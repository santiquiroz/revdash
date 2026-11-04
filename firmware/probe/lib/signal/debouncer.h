#pragma once

#include <cstdint>
#include <optional>

namespace rdp {

class EdgeDebouncer {
 public:
  EdgeDebouncer(uint32_t stableMs, bool initial);
  std::optional<bool> update(uint32_t nowMs, bool raw);
  bool level() const;

 private:
  uint32_t stableMs_;
  bool stable_;
  bool hasCandidate_;
  bool candidate_;
  uint32_t candidateSince_;
};

}  // namespace rdp
