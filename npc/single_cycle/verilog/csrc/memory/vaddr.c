/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

// #include <isa.h>
#include <memory/paddr.h>
#include <memory/host.h>

word_t vaddr_ifetch(vaddr_t addr, int len) {
#ifdef CONFIG_YSYXSOC
  char* copy_to_mrom(int32_t addr);
  return host_read((uint8_t*)copy_to_mrom(addr), 4);
#else
  return paddr_read(addr, len);
#endif

}

word_t vaddr_read(vaddr_t addr, int len) {
#ifdef CONFIG_YSYXSOC
  char* copy_to_mrom(int32_t addr);
  return host_read((uint8_t*)copy_to_mrom(addr), 4);
#else
  return paddr_read(addr, len);
#endif
}

void vaddr_write(vaddr_t addr, int len, word_t data) {
  return paddr_write(addr, len, data);
}
