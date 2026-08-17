#include "memory.h"

#include <sys/sysinfo.h>
#include <stdint.h>

namespace Memory {

static constexpr uint64_t GB = 1024ULL * 1024ULL * 1024ULL;

/*
 * RAM física total do sistema.
 */
uint64_t GetTotalRAM() {
    struct sysinfo info{};

    if (sysinfo(&info) != 0)
        return 0;

    return static_cast<uint64_t>(info.totalram) *
           static_cast<uint64_t>(info.mem_unit);
}

/*
 * RAM física disponível.
 */
uint64_t GetAvailableRAM() {
    struct sysinfo info{};

    if (sysinfo(&info) != 0)
        return 0;

    return static_cast<uint64_t>(info.freeram) *
           static_cast<uint64_t>(info.mem_unit);
}

/*
 * Swap total disponível no sistema.
 */
uint64_t GetTotalSwap() {
    struct sysinfo info{};

    if (sysinfo(&info) != 0)
        return 0;

    return static_cast<uint64_t>(info.totalswap) *
           static_cast<uint64_t>(info.mem_unit);
}

/*
 * Swap atualmente disponível.
 */
uint64_t GetAvailableSwap() {
    struct sysinfo info{};

    if (sysinfo(&info) != 0)
        return 0;

    return static_cast<uint64_t>(info.freeswap) *
           static_cast<uint64_t>(info.mem_unit);
}

/*
 * RAM + swap total.
 */
uint64_t GetTotalCombinedMemory() {
    return GetTotalRAM() + GetTotalSwap();
}

/*
 * RAM + swap disponível.
 */
uint64_t GetAvailableCombinedMemory() {
    return GetAvailableRAM() + GetAvailableSwap();
}

/*
 * Retorna o total combinado em GB.
 */
uint64_t GetTotalCombinedMemoryGB() {
    return GetTotalCombinedMemory() / GB;
}

} // namespace Memory
