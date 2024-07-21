#include <common.h>

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
  return 0;
}

size_t fb_write(const void *buf, size_t offset, size_t len) {
  return 0;
}

void init_device() {
  Log("Initializing devices...");
  ioe_init();
}
