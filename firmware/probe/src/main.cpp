#include <Arduino.h>

#include <atomic>

#include "can_listener.h"
#include "config.h"
#include "inputs.h"
#include "kline_listener.h"
#include "pulse_counter.h"
#include "rdp_frame.h"
#include "selftest.h"

namespace {
QueueHandle_t gQueue = nullptr;
std::atomic<uint32_t> gDropped{0};
uint32_t gLastStatus = 0;
}  // namespace

void setup() {
  Serial.begin(921600);
  gQueue = xQueueCreate(rdp::kLineQueueCapacity, sizeof(rdp::RdpLine));
  rdp::rdpEnqueue(gQueue, &gDropped, rdp::formatHello(millis(), rdp::kFwVersion));
  inputsBegin(gQueue, &gDropped);
  pulseBegin(gQueue, &gDropped);
  canBegin(gQueue, &gDropped);
  klineBegin(gQueue, &gDropped);
#ifdef RDP_SELFTEST
  selftestBegin();
#endif
  gLastStatus = millis();
}

void loop() {
  rdp::RdpLine line;
  if (xQueueReceive(gQueue, &line, pdMS_TO_TICKS(10)) == pdTRUE) {
    Serial.println(line.text);
  }
  if (millis() - gLastStatus >= rdp::kStatusPeriodMs) {
    gLastStatus = millis();
    Serial.println(rdp::formatStatus(gLastStatus, gDropped.load()).c_str());
  }
}
