#include "frequency.h"

namespace rdp {

uint32_t mhzFromCount(uint32_t pulses, uint32_t windowMs) {
  if (windowMs == 0) {
    return 0;
  }
  return static_cast<uint32_t>(static_cast<uint64_t>(pulses) * 1000000ULL / windowMs);
}

}  // namespace rdp
