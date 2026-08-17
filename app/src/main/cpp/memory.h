#pragma once

#include <stdint.h>

namespace Memory {

uint64_t GetTotalRAM();
uint64_t GetAvailableRAM();

uint64_t GetTotalSwap();
uint64_t GetAvailableSwap();

uint64_t GetTotalCombinedMemory();
uint64_t GetAvailableCombinedMemory();

uint64_t GetTotalCombinedMemoryGB();

}
