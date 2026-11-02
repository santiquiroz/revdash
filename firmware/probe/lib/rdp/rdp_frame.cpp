#include "rdp_frame.h"

#include <cinttypes>
#include <cstdio>

namespace rdp {

std::string formatHello(uint32_t ms, const char* fwVersion) {
  char buf[64];
  snprintf(buf, sizeof(buf), "H,%" PRIu32 ",RDP1,%s", ms, fwVersion);
  return buf;
}

std::string formatStatus(uint32_t ms, uint32_t dropped) {
  char buf[48];
  snprintf(buf, sizeof(buf), "S,%" PRIu32 ",%" PRIu32, ms, dropped);
  return buf;
}

std::string formatEdge(uint32_t ms, uint8_t ch, bool level) {
  char buf[48];
  snprintf(buf, sizeof(buf), "E,%" PRIu32 ",%u,%d", ms, static_cast<unsigned>(ch), level ? 1 : 0);
  return buf;
}

std::string formatFrequency(uint32_t ms, uint8_t ch, uint32_t milliHz) {
  char buf[48];
  snprintf(buf, sizeof(buf), "F,%" PRIu32 ",%u,%" PRIu32, ms, static_cast<unsigned>(ch), milliHz);
  return buf;
}

std::string formatCan(uint32_t ms, uint32_t id, bool extended, const uint8_t* data, uint8_t len) {
  char head[48];
  char idBuf[16];
  snprintf(idBuf, sizeof(idBuf), "%" PRIX32, id);
  snprintf(head, sizeof(head), "C,%" PRIu32 ",%s,%c,", ms, idBuf, extended ? 'x' : 's');
  std::string out = head;
  for (uint8_t i = 0; i < len; ++i) {
    char b[4];
    snprintf(b, sizeof(b), "%02X", static_cast<unsigned>(data[i]));
    out += b;
  }
  return out;
}

std::string formatKline(uint32_t ms, const uint8_t* data, uint8_t len) {
  char head[32];
  snprintf(head, sizeof(head), "K,%" PRIu32 ",", ms);
  std::string out = head;
  for (uint8_t i = 0; i < len; ++i) {
    char b[4];
    snprintf(b, sizeof(b), "%02X", static_cast<unsigned>(data[i]));
    out += b;
  }
  return out;
}

}  // namespace rdp
