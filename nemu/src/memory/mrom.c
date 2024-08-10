#include <isa.h>

static uint8_t mrom[CONFIG_MROM_SIZE] PG_ALIGN = {};
uint8_t* copy_to_mrom(paddr_t paddr) { 
  return mrom + paddr - CONFIG_MROM_BASE; 
}
void test_mrom()
{
  for (int i = 0; i < 100; i++) {
    printf("%x ", mrom[i]);
  }
}
