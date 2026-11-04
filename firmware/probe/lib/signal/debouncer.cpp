#include "debouncer.h"

namespace rdp {

EdgeDebouncer::EdgeDebouncer(uint32_t stableMs, bool initial)
    : stableMs_(stableMs), stable_(initial), hasCandidate_(false), candidate_(false), candidateSince_(0) {}

std::optional<bool> EdgeDebouncer::update(uint32_t nowMs, bool raw) {
  if (raw == stable_) {
    hasCandidate_ = false;
    return std::nullopt;
  }
  if (!hasCandidate_ || candidate_ != raw) {
    hasCandidate_ = true;
    candidate_ = raw;
    candidateSince_ = nowMs;
  }
  if (nowMs - candidateSince_ >= stableMs_) {
    stable_ = candidate_;
    hasCandidate_ = false;
    return stable_;
  }
  return std::nullopt;
}

bool EdgeDebouncer::level() const {
  return stable_;
}

}  // namespace rdp
