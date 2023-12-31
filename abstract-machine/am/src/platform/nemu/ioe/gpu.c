#include <am.h>
#include <nemu.h>

#define SYNC_ADDR (VGACTL_ADDR + 4)

void __am_gpu_init() {
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  uint32_t vgactl = inl(VGACTL_ADDR);
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = vgactl >> 16, .height = (vgactl & 0xFFFF),
    .vmemsz = 0
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  if (ctl->pixels != NULL) {
    uint32_t* fb = (uint32_t*)(uintptr_t)FB_ADDR;
    for (int j = 0; j < ctl->h; j++) {
      for (int i = 0; i < ctl->w; i++) {
        int v_i = ctl->x + i;
        int v_j = ctl->y + j;
        fb[v_i + v_j * 400] = ((uint32_t*)ctl->pixels)[i + j * ctl->w];
      }
    }
  }
  if (ctl->sync) {
    outl(SYNC_ADDR, 1);
  } 
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
