#include <unity.h>

#include "rdp_frame.h"

void setUp() {}
void tearDown() {}

void test_edge() { TEST_ASSERT_EQUAL_STRING("E,1234,3,1", rdp::formatEdge(1234, 3, true).c_str()); }
void test_frequency() { TEST_ASSERT_EQUAL_STRING("F,500,0,104000", rdp::formatFrequency(500, 0, 104000).c_str()); }
void test_can_std() { uint8_t d[] = {0x01, 0xAB}; TEST_ASSERT_EQUAL_STRING("C,10,1A0,s,01AB", rdp::formatCan(10, 0x1A0, false, d, 2).c_str()); }
void test_can_ext_empty() { TEST_ASSERT_EQUAL_STRING("C,10,18DAF110,x,", rdp::formatCan(10, 0x18DAF110, true, nullptr, 0).c_str()); }
void test_kline() { uint8_t d[] = {0xC1, 0x33}; TEST_ASSERT_EQUAL_STRING("K,7,C133", rdp::formatKline(7, d, 2).c_str()); }
void test_hello_status() {
  TEST_ASSERT_EQUAL_STRING("H,0,RDP1,0.1.0", rdp::formatHello(0, "0.1.0").c_str());
  TEST_ASSERT_EQUAL_STRING("S,5000,2", rdp::formatStatus(5000, 2).c_str());
}

int main(int, char**) {
  UNITY_BEGIN();
  RUN_TEST(test_edge);
  RUN_TEST(test_frequency);
  RUN_TEST(test_can_std);
  RUN_TEST(test_can_ext_empty);
  RUN_TEST(test_kline);
  RUN_TEST(test_hello_status);
  return UNITY_END();
}
