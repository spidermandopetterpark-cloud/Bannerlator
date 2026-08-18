#ifndef TITANPC_SYSTEM_SETTINGS_H
#define TITANPC_SYSTEM_SETTINGS_H

#include <cstdint>

namespace TitanPC {

class SystemSettings {

public:

    static uint64_t getAvailableMemoryMB();

    static bool setMemorySizeMB(
            uint64_t memoryMB
    );

    static bool setSwapSizeMB(
            uint64_t swapMB
    );

    static uint64_t getMemorySizeMB();

    static uint64_t getSwapSizeMB();

private:

    static uint64_t memorySizeMB;
    static uint64_t swapSizeMB;
};

}

#endif
