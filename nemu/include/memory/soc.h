#ifndef __MEMORY_SOC_H__
#define __MEMORY_SOC_H__

#include <common.h>

uint8_t* copy_to_mrom(paddr_t paddr);
uint8_t* copy_to_sram(paddr_t paddr); 
void test_mrom();

#endif
