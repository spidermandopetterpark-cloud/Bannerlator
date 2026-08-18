#include "SystemSettings.h"

#include <fstream>
#include <string>

namespace TitanPC {

uint64_t SystemSettings::memorySizeMB = 2048;

uint64_t SystemSettings::swapSizeMB = 2048;

uint64_t SystemSettings::getAvailableMemoryMB() {

    std::ifstream file("/proc/meminfo");

    if (!file.is_open())
        return 0;

    std::string name;
    uint64_t value;
    std::string unit;

    while (file >> name >> value >> unit) {

        if (name == "MemAvailable:") {

            return value / 1024;
        }
    }

    return 0;
}

bool SystemSettings::setMemorySizeMB(
        uint64_t memoryMB) {

    if (memoryMB < 512)
        return false;

    memorySizeMB = memoryMB;

    return true;
}

bool SystemSettings::setSwapSizeMB(
        uint64_t swapMB) {

    swapSizeMB = swapMB;

    return true;
}

uint64_t SystemSettings::getMemorySizeMB() {

    return memorySizeMB;
}

uint64_t SystemSettings::getSwapSizeMB() {

    return swapSizeMB;
}

}
