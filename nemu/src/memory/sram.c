#include <isa.h>

static uint8_t sram[CONFIG_SRAM_SIZE] PG_ALIGN = {};
uint8_t* copy_to_sram(paddr_t paddr) { return sram + paddr - CONFIG_SRAM_BASE;}
