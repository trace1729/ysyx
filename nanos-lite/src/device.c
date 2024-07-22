#include <common.h>
#include <stdio.h>

#if defined(MULTIPROGRAM) && !defined(TIME_SHARING)
# define MULTIPROGRAM_YIELD() yield()
#else
# define MULTIPROGRAM_YIELD()
#endif

#define NAME(key) \
  [AM_KEY_##key] = #key,

static const char *keyname[256] __attribute__((used)) = {
  [AM_KEY_NONE] = "NONE",
  AM_KEYS(NAME)
};

size_t serial_write(const void *buf, size_t offset, size_t len) {
  for (int i = 0; i < len; i++) {
    putch(((uint8_t*)buf)[i]);
  }
  return len;
}

size_t events_read(void *buf, size_t offset, size_t len) {
  bool has_kbd  = io_read(AM_INPUT_CONFIG).present;
  size_t size = 0;
  if (has_kbd) {
      AM_INPUT_KEYBRD_T ev = io_read(AM_INPUT_KEYBRD);
      if (ev.keycode == AM_KEY_NONE) {
        return 0;
      }
      if (ev.keydown) {
        memcpy(buf, "kd ", 3);
      } else {
        memcpy(buf, "ku ", 3);
      }
      size += 3;
      memcpy((char* )buf + size, keyname[ev.keycode], strlen(keyname[ev.keycode]));
      size += strlen(keyname[ev.keycode]);
      ((char*)buf)[size] = '\0';
      return size;
  }
  return 0;
}

size_t dispinfo_read(void *buf, size_t offset, size_t len) {
  size_t size = 0;
  int width = io_read(AM_GPU_CONFIG).width;
  int height = io_read(AM_GPU_CONFIG).height;
  char* proc_dispinfo = (char* )buf;
  sprintf(proc_dispinfo, "WIDTH: %d\nHEIGHT: %d\n", width, height);
  char* dispinfo = (char*) proc_dispinfo;
  while (*(char*)dispinfo != '\n') dispinfo++, size++; 
  dispinfo++;
  while (*(char*)dispinfo != '\n') dispinfo++, size++;
  dispinfo++;
  size++;
  *dispinfo = '\0';
  return size;
}

size_t fb_write(const void *buf, size_t offset, size_t len) {

  int width = io_read(AM_GPU_CONFIG).width;
  offset /= sizeof(uint32_t);
  len /= sizeof(uint32_t);
  int x = offset % width;
  int y = offset / width;

  // Log("fb_write: ");
  // printf("offset: %d, (%d, %d) %d\n", offset, x, y, len);
  /* uint32_t* pixel = (uint32_t *)buf;  */
  /* for (int i = 0; i < len; i++) { */
  /*   printf("%p", (void*)(uintptr_t)pixel[i]); */
  /* } */

  io_write(AM_GPU_FBDRAW, x, y, (char* )buf, len, 1, false);
  io_write(AM_GPU_FBDRAW, 0, 0, NULL, 0, 0, true);
  return len * sizeof(uint32_t);
}

void init_device() {
  Log("Initializing devices...");
  ioe_init();
}
