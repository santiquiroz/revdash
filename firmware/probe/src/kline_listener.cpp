#include "kline_listener.h"

#include "config.h"
#include "rdp_frame.h"

namespace {

QueueHandle_t gQueue = nullptr;
std::atomic<uint32_t>* gDropped = nullptr;

void flush(uint8_t* buf, uint8_t* len) {
  if (*len == 0) {
    return;
  }
  rdp::rdpEnqueue(gQueue, gDropped, rdp::formatKline(millis(), buf, *len));
  *len = 0;
}

void klineTask(void*) {
  uint8_t buf[rdp::kKlineMaxBytes];
  uint8_t len = 0;
  uint32_t lastByte = millis();
  for (;;) {
    while (Serial2.available() > 0) {
      if (len >= rdp::kKlineMaxBytes) {
        flush(buf, &len);
      }
      buf[len++] = static_cast<uint8_t>(Serial2.read());
      lastByte = millis();
    }
    if (len > 0 && millis() - lastByte >= rdp::kKlineSilenceMs) {
      flush(buf, &len);
    }
    vTaskDelay(pdMS_TO_TICKS(1));
  }
}

}  // namespace

void klineBegin(QueueHandle_t queue, std::atomic<uint32_t>* dropped) {
  if (!rdp::kKlineEnabled) {
    return;
  }
  gQueue = queue;
  gDropped = dropped;
  Serial2.begin(rdp::kKlineBaud, SERIAL_8N1, rdp::kKlineRxPin, -1);
  xTaskCreate(klineTask, "kline", 2048, nullptr, 1, nullptr);
}
