#include "pulse_counter.h"

#include <driver/pcnt.h>

#include "config.h"
#include "frequency.h"
#include "rdp_frame.h"

namespace {

QueueHandle_t gQueue = nullptr;
std::atomic<uint32_t>* gDropped = nullptr;

void setupUnit(pcnt_unit_t unit, int pin) {
  pcnt_config_t cfg = {};
  cfg.pulse_gpio_num = pin;
  cfg.ctrl_gpio_num = PCNT_PIN_NOT_USED;
  cfg.channel = PCNT_CHANNEL_0;
  cfg.unit = unit;
  cfg.pos_mode = PCNT_COUNT_INC;
  cfg.neg_mode = PCNT_COUNT_DIS;
  cfg.lctrl_mode = PCNT_MODE_KEEP;
  cfg.hctrl_mode = PCNT_MODE_KEEP;
  cfg.counter_h_lim = 32767;
  cfg.counter_l_lim = 0;
  pcnt_unit_config(&cfg);
  pcnt_set_filter_value(unit, 80);
  pcnt_filter_enable(unit);
  pcnt_counter_clear(unit);
  pcnt_counter_resume(unit);
}

void readAndEnqueue(pcnt_unit_t unit, uint8_t ch, uint32_t now) {
  int16_t count = 0;
  pcnt_get_counter_value(unit, &count);
  pcnt_counter_clear(unit);
  rdp::rdpEnqueue(gQueue, gDropped,
                  rdp::formatFrequency(now, ch, rdp::mhzFromCount(static_cast<uint32_t>(count), rdp::kFrequencyWindowMs)));
}

void pulseTask(void*) {
  TickType_t last = xTaskGetTickCount();
  for (;;) {
    vTaskDelayUntil(&last, pdMS_TO_TICKS(rdp::kFrequencyWindowMs));
    uint32_t now = millis();
    for (uint8_t i = 0; i < rdp::kNumPulseChannels; ++i) {
      readAndEnqueue(static_cast<pcnt_unit_t>(i), rdp::kPulseChannels[i], now);
    }
  }
}

}  // namespace

void pulseBegin(QueueHandle_t queue, std::atomic<uint32_t>* dropped) {
  gQueue = queue;
  gDropped = dropped;
  for (uint8_t i = 0; i < rdp::kNumPulseChannels; ++i) {
    setupUnit(static_cast<pcnt_unit_t>(i), rdp::kDigitalPins[rdp::kPulseChannels[i]]);
  }
  xTaskCreate(pulseTask, "pulse", 2048, nullptr, 1, nullptr);
}
