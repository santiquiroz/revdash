#pragma once

#include <cstdint>
#include <string>

namespace rdp {

std::string formatHello(uint32_t ms, const char* fwVersion);
std::string formatStatus(uint32_t ms, uint32_t dropped);
std::string formatEdge(uint32_t ms, uint8_t ch, bool level);
std::string formatFrequency(uint32_t ms, uint8_t ch, uint32_t milliHz);
std::string formatCan(uint32_t ms, uint32_t id, bool extended, const uint8_t* data, uint8_t len);
std::string formatKline(uint32_t ms, const uint8_t* data, uint8_t len);

}  // namespace rdp
