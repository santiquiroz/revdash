#pragma once

#include <Arduino.h>

#include <atomic>
#include <cstdint>
#include <cstring>
#include <string>

namespace rdp {

constexpr const char* kFwVersion = "0.1.0";
constexpr uint8_t kNumChannels = 8;
constexpr int kDigitalPins[kNumChannels] = {34, 35, 36, 39, 32, 33, 25, 26};
constexpr uint8_t kPulseChannels[] = {0, 1};
constexpr uint8_t kNumPulseChannels = sizeof(kPulseChannels) / sizeof(kPulseChannels[0]);
constexpr uint32_t kDebounceMs = 5;
constexpr uint32_t kFrequencyWindowMs = 250;
constexpr uint32_t kStatusPeriodMs = 5000;

constexpr bool kCanEnabled = true;
constexpr int kCanRxPin = 22;
constexpr int kCanTxPin = 21;
constexpr uint32_t kCanBitrates[] = {500000, 250000};
constexpr uint8_t kNumCanBitrates = sizeof(kCanBitrates) / sizeof(kCanBitrates[0]);
constexpr uint32_t kCanDetectWindowMs = 3000;

constexpr bool kKlineEnabled = true;
constexpr int kKlineRxPin = 16;
constexpr uint32_t kKlineBaud = 10400;
constexpr uint8_t kKlineMaxBytes = 16;
constexpr uint32_t kKlineSilenceMs = 20;

constexpr int kSelftestPwmPin = 23;
constexpr uint32_t kSelftestHz = 100;

constexpr size_t kLineLen = 64;
constexpr uint16_t kLineQueueCapacity = 512;

struct RdpLine {
  char text[kLineLen];
};

inline void rdpEnqueue(QueueHandle_t queue, std::atomic<uint32_t>* dropped, const std::string& s) {
  RdpLine line;
  strncpy(line.text, s.c_str(), kLineLen - 1);
  line.text[kLineLen - 1] = '\0';
  if (xQueueSend(queue, &line, 0) != pdTRUE) {
    (*dropped)++;
  }
}

}  // namespace rdp
