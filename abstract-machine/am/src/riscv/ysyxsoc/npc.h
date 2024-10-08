#ifndef NPC_H__
#define NPC_H__

# define npc_trap(code) asm volatile("mv a0, %0; ebreak" : :"r"(code))

# define DEVICE_BASE 0xa0000000
#define MMIO_BASE 0xa0000000

#define SERIAL_PORT     (0x10000000)
#define UART_FCR        (SERIAL_PORT + 2)
#define UART_LCR        (SERIAL_PORT + 3)
#define UART_LSR        (SERIAL_PORT + 5)
#define UART_DIV        (SERIAL_PORT)

#define KBD_ADDR        (DEVICE_BASE + 0x0000060)
#define RTC_ADDR        (DEVICE_BASE + 0x0000048)
#define VGACTL_ADDR     (DEVICE_BASE + 0x0000100)
#define AUDIO_ADDR      (DEVICE_BASE + 0x0000200)
#define DISK_ADDR       (DEVICE_BASE + 0x0000300)
#define FB_ADDR         (MMIO_BASE   + 0x1000000)
#define AUDIO_SBUF_ADDR (MMIO_BASE   + 0x1200000)


#endif
