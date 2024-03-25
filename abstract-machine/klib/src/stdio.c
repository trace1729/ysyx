#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#define BUFSIZE 4000
#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...) {
	char out[BUFSIZE];
	va_list args;
	va_start(args, fmt);
	int size = vsprintf(out, fmt, args);
	va_end(args);
    putstr(out);
    if (size >= BUFSIZE) {
      panic("printf buffer overflow");
    }
	return size;
}

static char tochar(int num) {
  if (num >= 0 && num <= 9) return num + '0';
  else return 'A' + num - 10;
}

int int2str(char *out, int n, int idx, int base) {
  if (n == 0) {
	  out[idx++] = '0';
	  return idx;
  }
  long long num = n;
  if (base == 10 && n < 0) {
    out[idx++] = '-';
    num = -num;
  }
  if (base == 16) {
    out[idx++] = '0'; out[idx++] = 'x';
  }
  int len = -1;
  char buf[32];
  while (num) {
    buf[++len] = tochar(num % base);
    num /= base;
  }
  for (; len >= 0; len--, idx++) {
    out[idx] = buf[len];
  }
  return idx;
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  int cnt = 0;
  bool format = false;
  int d;
  char* s;
  char ch;
  while(*fmt) {
    switch (*fmt) {
      case '%': format = true; break;
      case 'd': if (format) { d = va_arg(ap, int)  ; cnt = int2str(out, d, cnt, 10);} break;
      case 'x': if (format) { d = va_arg(ap, int)  ; cnt = int2str(out, d, cnt, 16);} break;
      case 'c': if (format) { ch = (char)va_arg(ap, int); out[cnt++] = ch;} break;
      case 's': if (format) { s = va_arg(ap, char*); memcpy(out + cnt, s, strlen(s)) ; cnt += strlen(s);} break;
      default : break;
    }
    // 一直没有 %, format 一直为false, 默认打印字符
    // 第一次出现 %，*fmt == %, format 为 true, 不打印
    // 格式化输出后，*fmt != %, format 为 true, 不打印，将 format 置 false;
    if (!format) {
      out[cnt++] = *fmt;
    }
    if (format && *fmt != '%') {
      format = false;
    } 
    fmt++;
  }
  out[cnt] = '\0';
  return cnt - 1;
}


int sprintf(char *out, const char *fmt, ...) {
	va_list args;
	va_start(args, fmt);
	int size = vsprintf(out, fmt, args);
	va_end(args);
	return size;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
