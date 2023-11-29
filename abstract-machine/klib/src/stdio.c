#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...) {
  panic("Not implemented");
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  panic("Not implemented");
}

int dectostr(char *out, int n, int idx) {
  if (n == 0) {
	  out[idx++] = '0';
	  return idx;
  }
  long num = n;
  long long num = n;
  if (n < 0) {
    out[idx++] = '-';
    num = -(long)n;
    num = -num;
  }
  int len = -1;
  char buf[32];
  while (num) {
    buf[++len] = '0' + num % 10;
    num /= 10;
  }
  for (; len >= 0; len--, idx++) {
    out[idx] = buf[len];
  }
  return idx;
}

int sprintf(char *out, const char *fmt, ...) {
  va_list ap;
  int cnt = 0;
  bool format = false;
  int d;
  char* s;
  va_start(ap, fmt);
  while(*fmt) {
    switch (*fmt) {
      case '%': format = true; break;
      case 'd': if (format) { d = va_arg(ap, int)  ; cnt = dectostr(out, d, cnt);} break;
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
  va_end(ap);
  out[cnt] = '\0';
  return cnt - 1;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
