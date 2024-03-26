#include <proc.h>
#include <elf.h>
#include <stdint.h>
#include <string.h>

#ifdef __LP64__
# define Elf_Ehdr Elf64_Ehdr
# define Elf_Phdr Elf64_Phdr
#else
# define Elf_Ehdr Elf32_Ehdr
# define Elf_Phdr Elf32_Phdr
#endif

#define ELF_MAGIC 0x464C457FU

#if defined(__ISA_AM_NATIVE__)
 #define EXPECTED_TYPE EM_X86_64
#elif defined(__ISA_RISCV32__)
 #define EXPECTED_TYPE EM_RISCV
#endif


size_t ramdisk_read(void *buf, size_t offset, size_t len);
size_t ramdisk_write(const void *buf, size_t offset, size_t len);

 /* e_phentsize */
 /*        This member holds the size in bytes of one entry  in  the  file's  program */
 /*        header table; all entries are the same size. */

// hdr -> section header table/ program header table

 uintptr_t loader(PCB *pcb, const char *filename) {
  // 读取文件
  Elf_Ehdr e_hdr;
  // load elf-hdr
  size_t size = ramdisk_read(&e_hdr, 0, sizeof(e_hdr));
  // check size
  assert(size == sizeof(e_hdr));
  // check elf header magic number
  assert(*(uint32_t*)e_hdr.e_ident == ELF_MAGIC);
  // begin iterate through header table
  printf("program header table:\n");
  assert(e_hdr.e_machine == EXPECTED_TYPE);

  for (int i = 0; i < e_hdr.e_phnum; i++) {
    Elf_Phdr p_hdr;
    size = ramdisk_read(&p_hdr, e_hdr.e_phoff + i * e_hdr.e_phentsize, e_hdr.e_phentsize);
    assert(size == e_hdr.e_phentsize);

    // load segment only when segment type is PT_LOAD
    if (p_hdr.p_type != PT_LOAD) {
      continue;
    }
    printf("p_vaddr: %d, p_paddr: %d, Filesize: %x, Memsize:%x\n",  p_hdr.p_vaddr, p_hdr.p_paddr, p_hdr.p_filesz, p_hdr.p_memsz);
  
    // 将程序从 elf 文件拷贝到 对应的物理地址 (这里物理地址和虚拟地址是一样的)
    ramdisk_read((void *)(uintptr_t)(p_hdr.p_paddr), p_hdr.p_offset, p_hdr.p_filesz);
    // 将多余的空间设置为0
    memset((void *)(uintptr_t)(&p_hdr.p_paddr + p_hdr.p_filesz), 0, p_hdr.p_memsz - p_hdr.p_filesz);

  }

  return e_hdr.e_entry;
}

void naive_uload(PCB *pcb, const char *filename) {
  uintptr_t entry = loader(pcb, filename);
  Log("Jump to entry = %p", entry);
  ((void(*)())entry) ();
}

