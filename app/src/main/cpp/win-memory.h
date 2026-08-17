#pragma once

#include <stdint.h>

#define WINE_MEMORY_12GB (12ULL * 1024ULL * 1024ULL * 1024ULL)

uint64_t wine_get_total_memory(void);
uint64_t wine_get_available_memory(void);
