#include "can_listener.h"

#include <driver/twai.h>

#include "config.h"
#include "rdp_frame.h"

namespace {

QueueHandle_t gQueue = nullptr;
std::atomic<uint32_t>* gDropped = nullptr;

twai_timing_config_t timingFor(uint32_t bitrate) {
  if (bitrate == 250000) {
    return TWAI_TIMING_CONFIG_250KBITS();
  }
  return TWAI_TIMING_CONFIG_500KBITS();
}

bool installAt(uint32_t bitrate) {
  twai_general_config_t general = TWAI_GENERAL_CONFIG_DEFAULT(
      static_cast<gpio_num_t>(rdp::kCanTxPin), static_cast<gpio_num_t>(rdp::kCanRxPin), TWAI_MODE_LISTEN_ONLY);
  twai_timing_config_t timing = timingFor(bitrate);
  twai_filter_config_t filter = TWAI_FILTER_CONFIG_ACCEPT_ALL();
  if (twai_driver_install(&general, &timing, &filter) != ESP_OK) {
    return false;
  }
  if (twai_start() != ESP_OK) {
    twai_driver_uninstall();
    return false;
  }
  return true;
}

void uninstall() {
  twai_stop();
  twai_driver_uninstall();
}

void enqueueFrame(const twai_message_t& msg);

bool waitForFrame(uint32_t timeoutMs) {
  twai_message_t msg;
  uint32_t start = millis();
  while (millis() - start < timeoutMs) {
    if (twai_receive(&msg, pdMS_TO_TICKS(100)) == ESP_OK) {
      enqueueFrame(msg);
      return true;
    }
  }
  return false;
}

bool isExtended(const twai_message_t& msg) {
  return (msg.flags & TWAI_MSG_FLAG_EXTD) != 0;
}

void enqueueFrame(const twai_message_t& msg) {
  rdp::rdpEnqueue(gQueue, gDropped,
                  rdp::formatCan(millis(), msg.identifier, isExtended(msg), msg.data, msg.data_length_code));
}

void receiveForever() {
  twai_message_t msg;
  for (;;) {
    if (twai_receive(&msg, portMAX_DELAY) == ESP_OK) {
      enqueueFrame(msg);
    }
  }
}

void canTask(void*) {
  uint8_t last = 0;
  for (uint8_t i = 0; i < rdp::kNumCanBitrates; ++i) {
    last = i;
    if (!installAt(rdp::kCanBitrates[i])) {
      continue;
    }
    if (waitForFrame(rdp::kCanDetectWindowMs)) {
      receiveForever();
    }
    uninstall();
  }
  if (installAt(rdp::kCanBitrates[last])) {
    receiveForever();
  }
  vTaskDelete(nullptr);
}

}  // namespace

void canBegin(QueueHandle_t queue, std::atomic<uint32_t>* dropped) {
  if (!rdp::kCanEnabled) {
    return;
  }
  gQueue = queue;
  gDropped = dropped;
  xTaskCreate(canTask, "can", 4096, nullptr, 1, nullptr);
}
