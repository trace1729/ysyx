#include <common.h>
#include <debug.h>

extern uint64_t g_nr_guest_inst;

#ifndef CONFIG_TARGET_AM
#include <elf.h>


static FILE *fp = NULL;
static char e_strtab[1024];
static Elf32_Sym e_symbols[256];
static int e_symnum;

void get_function_symbol_by_address(uint32_t addr, char *buf) {

  for (int i = 0; i < e_symnum; i++) {
    // 类型是 func 的 symbol
    if ((e_symbols[i].st_info & STT_FUNC) == 0) {
      continue;
    }
    if (addr >= e_symbols[i].st_value && addr < (e_symbols[i].st_value + e_symbols[i].st_size)) {
      uint32_t nameoff = e_symbols[i].st_name;
      strcpy(buf, e_strtab + nameoff);
      // printf("%s\n", e_strtab + nameoff);
    }
  }
  return;
}

void init_elf(const char* elf_file) {
  Elf32_Ehdr e_hdr;
  Elf32_Shdr e_sections[256];

  fp = fopen(elf_file, "rb");
  Check(fp != NULL, "open %s failed", elf_file);
  size_t size = fread(&e_hdr, sizeof(e_hdr), 1, fp);
  Check(size == 1, "expected read %u, but %lu", 1, size);
  uint16_t e_shnum = e_hdr.e_shnum;
  uint16_t e_shentsize = e_hdr.e_shentsize;
  uint32_t e_shoff = e_hdr.e_shoff;

  /* read section headers */
  // go to the start of the file
  rewind(fp);
  // go to section header;
  fseek(fp, e_shoff, SEEK_SET);
  // read section headers;
  size = fread(e_sections, e_shentsize, e_shnum, fp);
  Check(size == e_shnum, "expected read %u, but %lu", e_shnum, size);

  /* find symtab and strtab */
  uint32_t sym_idx = 0;
  uint32_t str_idx = 0;
  for (unsigned i = 0; i < e_shnum; i++) {
    if (!sym_idx && e_sections[i].sh_type == SHT_SYMTAB) {
      sym_idx = i;
    }
    if (!str_idx && e_sections[i].sh_type == SHT_STRTAB) {
      str_idx = i;
    }
  }

  /* read strtab to str buffer */
  rewind(fp);
  fseek(fp, e_sections[str_idx].sh_offset, SEEK_SET);
  size = fread(e_strtab, 1, e_sections[str_idx].sh_size, fp);
  Check(size == e_sections[str_idx].sh_size, "expected read %u, but %lu", e_sections[str_idx].sh_size, size);

  /* read symbol table to symbols array */
  uint32_t sh_size = e_sections[sym_idx].sh_size;
  uint32_t sh_entsize = e_sections[sym_idx].sh_entsize;
  e_symnum = sh_size / sh_entsize;

  rewind(fp);
  fseek(fp, e_sections[sym_idx].sh_offset, SEEK_SET);
  size = fread(e_symbols, sh_entsize, e_symnum, fp);
  Check(size == e_symnum, "expected read %u, but %lu", e_symnum, size);

  /* close elf file */
  fclose(fp);
  return;

  /* test how to find symbol by address */
  // get_function_symbol_by_address(fp, 0x80000028u);

error:
  fclose(fp);
} 

#endif
