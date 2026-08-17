#include "win-memory.h"

#include <stdint.h>

uint64_t wine_get_total_memory(void)
{
    return WINE_MEMORY_12GB;
}

uint64_t wine_get_available_memory(void)
{
    /*
     * Não declare os 12 GB como totalmente livres.
     * O valor disponível deve representar uma estimativa realista
     * da memória que o processo consegue utilizar.
     */
    return WINE_MEMORY_12GB;
}
