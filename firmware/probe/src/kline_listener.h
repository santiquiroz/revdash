#pragma once

#include <Arduino.h>

#include <atomic>
#include <cstdint>

void klineBegin(QueueHandle_t queue, std::atomic<uint32_t>* dropped);
