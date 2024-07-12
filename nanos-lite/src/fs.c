#include <fs.h>

typedef size_t (*ReadFn)(void *buf, size_t offset, size_t len);
typedef size_t (*WriteFn)(const void *buf, size_t offset, size_t len);

typedef struct {
  char *name;
  size_t size;
  size_t disk_offset;
  ReadFn read;
  WriteFn write;
} Finfo;

enum { FD_STDIN, FD_STDOUT, FD_STDERR, FD_FB };

size_t invalid_read(void *buf, size_t offset, size_t len) {
  panic("should not reach here");
  return 0;
}

size_t invalid_write(const void *buf, size_t offset, size_t len) {
  panic("should not reach here");
  return 0;
}

/* This is the information about all files in disk. */
static Finfo file_table[] __attribute__((used)) = {
    [FD_STDIN] = {"stdin", 0, 0, invalid_read, invalid_write},
    [FD_STDOUT] = {"stdout", 0, 0, invalid_read, invalid_write},
    [FD_STDERR] = {"stderr", 0, 0, invalid_read, invalid_write},
#include "files.h"
};

#define FILE_NUM (sizeof(file_table) / sizeof(file_table[0]))

size_t file_offset_array[FILE_NUM];

size_t ramdisk_read(void *buf, size_t offset, size_t len);
size_t ramdisk_write(const void *buf, size_t offset, size_t len);

void init_fs() {
  // TODO: initialize the size of /dev/fb
}

int fs_open(const char *pathname, int flags, int mode) {

  // ignoring flag and mode
  int fd = -1;
  for (int i = 0; i < FILE_NUM; i++) {
    if (strcmp(file_table[i].name, pathname) == 0) {
      fd = i;
    }
  }
  assert(fd != -1);
  // do initialization for file_offset_array
  file_offset_array[fd] = file_table[fd].disk_offset;
  return fd;
}

size_t fs_read(int fd, void *buf, size_t len) {
  size_t offset = file_offset_array[fd];
  size_t actual = ramdisk_read(buf, offset, len);
  file_offset_array[fd] += actual;
  return actual;
}
size_t fs_write(int fd, const void *buf, size_t len) {
  size_t offset = file_offset_array[fd];
  size_t actual = ramdisk_write(buf, offset, len);
  file_offset_array[fd] += actual;
  return actual;
}

size_t fs_lseek(int fd, size_t offset, int whence) {

  switch (whence) {
  case SEEK_CUR:
    assert(file_offset_array[fd] + offset < file_table[fd].size + file_table[fd].disk_offset);
    file_offset_array[fd] += offset;
    break;
  case SEEK_SET:
    assert(offset < file_table[fd].size);
    file_offset_array[fd] = file_table[fd].disk_offset + offset;
    break;
  case SEEK_END:
    assert(offset < file_table[fd].size);
    file_offset_array[fd] = file_table[fd].disk_offset + file_table[fd].size - offset;
    break;
  }
  return file_offset_array[fd];
}

int fs_close(int fd) { return 0; }
