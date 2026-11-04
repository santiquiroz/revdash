#include "inputs.h"

#include "debouncer.h"

#include "config.h"
#include "rdp_frame.h"

namespace {

QueueHandle_t gQueue = nullptr;
std::atomic<uint32_t>* gDropped = nullptr;

void emitAllLevels(rdp::EdgeDebouncer** deb, uint32_t now) {
  for (uint8_t ch = 0; ch < rdp::kNumChannels; ++ch) {
    rdp::rdpEnqueue(gQueue, gDropped, rdp::formatEdge(now, ch, deb[ch]->level()));
  }
}

void sampleAll(rdp::EdgeDebouncer** deb, uint32_t now) {
  for (uint8_t ch = 0; ch < rdp::kNumChannels; ++ch) {
    bool raw = digitalRead(rdp::kDigitalPins[ch]) == HIGH;
    auto edge = deb[ch]->update(now, raw);
    if (edge.has_value()) {
      rdp::rdpEnqueue(gQueue, gDropped, rdp::formatEdge(now, ch, *edge));
    }
  }
}

void inputTask(void* arg) {
  rdp::EdgeDebouncer** deb = static_cast<rdp::EdgeDebouncer**>(arg);
  TickType_t last = xTaskGetTickCount();
  uint32_t lastLevelRefresh = millis();
  for (;;) {
    uint32_t now = millis();
    sampleAll(deb, now);
    if (now - lastLevelRefresh >= rdp::kLevelRefreshMs) {
      emitAllLevels(deb, now);
      lastLevelRefresh = now;
    }
    vTaskDelayUntil(&last, pdMS_TO_TICKS(1));
  }
}

}  // namespace

void inputsBegin(QueueHandle_t queue, std::atomic<uint32_t>* dropped) {
  gQueue = queue;
  gDropped = dropped;
  static rdp::EdgeDebouncer* deb[rdp::kNumChannels];
  for (uint8_t ch = 0; ch < rdp::kNumChannels; ++ch) {
    pinMode(rdp::kDigitalPins[ch], INPUT);
    deb[ch] = new rdp::EdgeDebouncer(rdp::kDebounceMs, digitalRead(rdp::kDigitalPins[ch]) == HIGH);
  }
  emitAllLevels(deb, millis());
  xTaskCreate(inputTask, "inputs", 2048, deb, 1, nullptr);
}
