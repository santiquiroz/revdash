#include "selftest.h"

#include <Arduino.h>

#include "config.h"

namespace {
constexpr uint8_t kSelftestChannel = 0;
constexpr uint8_t kSelftestResolution = 8;
}  // namespace

void selftestBegin() {
#ifdef RDP_SELFTEST
  ledcSetup(kSelftestChannel, rdp::kSelftestHz, kSelftestResolution);
  ledcAttachPin(rdp::kSelftestPwmPin, kSelftestChannel);
  ledcWrite(kSelftestChannel, 128);
#endif
}
