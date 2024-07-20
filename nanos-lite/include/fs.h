#ifndef __FS_H__
#define __FS_H__

#include <stdio.h>
#include <common.h>

#ifndef SEEK_SET
enum {SEEK_SET, SEEK_CUR, SEEK_END};
#endif

int fs_open(const char *pathname, int flags, int mode);
size_t fs_write(int fd, const void *buf, size_t len);
size_t fs_read(int fd, void *buf, size_t len);
int fs_close(int fd);

off_t fs_lseek(int fd, off_t offset, int whence);
const char* get_filename_by_fd(int fd);

#endif
