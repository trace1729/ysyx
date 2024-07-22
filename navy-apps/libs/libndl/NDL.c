#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/time.h>

static int evtdev = -1;
static int fbdev = -1;
static int screen_w = 0, screen_h = 0;
static int fb_w = 0, fb_h = 0;

// return system time by milliseconds
uint32_t NDL_GetTicks() {
  struct timeval tv;
  struct timezone tz;
  gettimeofday(&tv, &tz);
  return tv.tv_usec / 1000;
}

int NDL_PollEvent(char *buf, int len) {
  int fd = open("/dev/events", O_RDONLY);
  size_t size = read(fd, buf, len);
  close(fd);
  return (size > 0);
}

void NDL_OpenCanvas(int *w, int *h) {
  // 这个 NWM_APP 的环境变量不存在
  if (getenv("NWM_APP")) {
    int fbctl = 4;
    fbdev = 5;
    screen_w = *w; screen_h = *h;
    char buf[64];
    int len = sprintf(buf, "%d %d", screen_w, screen_h);
    // let NWM resize the window and create the frame buffer
    write(fbctl, buf, len);
    while (1) {
      // 3 = evtdev 
      // 如果应用程序写回 mmap ok 说明创建程序成功
      int nread = read(3, buf, sizeof(buf) - 1);
      if (nread <= 0) continue;
      buf[nread] = '\0';
      if (strcmp(buf, "mmap ok") == 0) break;
    }
    close(fbctl);
  }

  // 记录画布大小
  // 若没有指定画布大小，那就默认是屏幕大小
  FILE* fd = fopen("/proc/dispinfo", "r");
  char buf[400];
  // TODO 没有实现 seek 偏移量
  fscanf(fd, "WIDTH: %d\nHEIGHT: %d\n", &fb_w, &fb_h);
  fclose(fd);

  screen_w = *w; screen_h = *h;
  if (*w == 0) screen_w = fb_w;
  if (*h == 0) screen_h = fb_h;

  printf("\n\n\nw = %d, h = %d\n\n\n", fb_h, fb_w);

}

void NDL_DrawRect(uint32_t *pixels, int x, int y, int w, int h) {


  FILE* fb = fopen("/dev/fb", "w");

  for (int i = 0; i < h; i++) {
    fseek(fb, ((y + i) * fb_w + x) * sizeof(uint32_t), SEEK_SET);
    fwrite(pixels, 4, w, fb);
    pixels += w;
  }

  fclose(fb);
}

void NDL_OpenAudio(int freq, int channels, int samples) {
}

void NDL_CloseAudio() {
}

int NDL_PlayAudio(void *buf, int len) {
  return 0;
}

int NDL_QueryAudio() {
  return 0;
}

int NDL_Init(uint32_t flags) {
  if (getenv("NWM_APP")) {
    evtdev = 3;
  }
  return 0;
}

void NDL_Quit() {
}
