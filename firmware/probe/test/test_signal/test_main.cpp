#include <unity.h>

#include "debouncer.h"
#include "frequency.h"

void setUp() {}
void tearDown() {}

void test_turn_signal_333ms_pulses_pass() {
  rdp::EdgeDebouncer d(5, false);
  TEST_ASSERT_FALSE(d.update(0, true).has_value());
  auto on = d.update(6, true);  TEST_ASSERT_TRUE(on.has_value()); TEST_ASSERT_TRUE(*on);
  TEST_ASSERT_FALSE(d.update(333, false).has_value());
  auto off = d.update(339, false); TEST_ASSERT_TRUE(off.has_value()); TEST_ASSERT_FALSE(*off);
}
void test_alternating_noise_never_emits() {
  rdp::EdgeDebouncer d(5, false);
  int emitted = 0;
  for (uint32_t t = 0; t < 1000; t += 1) if (d.update(t, t % 4 < 2).has_value()) emitted++;
  TEST_ASSERT_EQUAL(0, emitted);
}
void test_level_returns_initial_value() {
  const rdp::EdgeDebouncer low(5, false);
  const rdp::EdgeDebouncer high(5, true);
  TEST_ASSERT_FALSE(low.level());
  TEST_ASSERT_TRUE(high.level());
}
void test_level_changes_only_after_confirmed_edge() {
  rdp::EdgeDebouncer d(5, false);
  TEST_ASSERT_FALSE(d.update(0, true).has_value());
  TEST_ASSERT_FALSE(d.level());
  TEST_ASSERT_FALSE(d.update(4, false).has_value());
  TEST_ASSERT_FALSE(d.level());
  TEST_ASSERT_FALSE(d.update(10, true).has_value());
  TEST_ASSERT_FALSE(d.update(14, true).has_value());
  TEST_ASSERT_FALSE(d.level());
  auto on = d.update(15, true);
  TEST_ASSERT_TRUE(on.has_value());
  TEST_ASSERT_TRUE(*on);
  TEST_ASSERT_TRUE(d.level());
  TEST_ASSERT_FALSE(d.update(20, false).has_value());
  TEST_ASSERT_TRUE(d.level());
  TEST_ASSERT_FALSE(d.update(24, true).has_value());
  TEST_ASSERT_TRUE(d.level());
  TEST_ASSERT_FALSE(d.update(30, false).has_value());
  TEST_ASSERT_FALSE(d.update(34, false).has_value());
  TEST_ASSERT_TRUE(d.level());
  auto off = d.update(35, false);
  TEST_ASSERT_TRUE(off.has_value());
  TEST_ASSERT_FALSE(*off);
  TEST_ASSERT_FALSE(d.level());
}
void test_frequency_values() {
  TEST_ASSERT_EQUAL_UINT32(0, rdp::mhzFromCount(0, 250));
  TEST_ASSERT_EQUAL_UINT32(104000, rdp::mhzFromCount(26, 250));
  TEST_ASSERT_EQUAL_UINT32(0, rdp::mhzFromCount(10, 0));
  TEST_ASSERT_EQUAL_UINT32(400000000, rdp::mhzFromCount(100000, 250));
}

int main(int, char**) {
  UNITY_BEGIN();
  RUN_TEST(test_turn_signal_333ms_pulses_pass);
  RUN_TEST(test_alternating_noise_never_emits);
  RUN_TEST(test_level_returns_initial_value);
  RUN_TEST(test_level_changes_only_after_confirmed_edge);
  RUN_TEST(test_frequency_values);
  return UNITY_END();
}
