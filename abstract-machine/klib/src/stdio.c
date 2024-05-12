#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#define BUFSIZE 4000
#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

#define BUFSIZE 4000
#define INT_MAX 2147483647u
#define ULONG_MAX 18446744073709551615u

#define MAX(a,b) ((a)>(b) ? (a) : (b))
#define MIN(a,b) ((a)<(b) ? (a) : (b))
// flag specifier
#define SPACE ' '
#define ALT_FORM   (1U<<('#'- SPACE))
#define ZERO_PAD   (1U<<('0'- SPACE))
#define LEFT_ADJ   (1U<<('-'- SPACE))
#define FLAGMASK (ALT_FORM|ZERO_PAD|LEFT_ADJ)
#define TOFLAG(f) (1U << ((unsigned)f - SPACE))
#define ISALPHA(a) ((a >= 'A') && (a <= 'z'))
#define ISDIGIT(a) ((a >= '0') && (a <= '9'))
#define ISFLAG(f) ((f&FLAGMASK) != 0)

/* typedef unsigned long int uintmax_t; */
/* typedef unsigned long int	uintptr_t; */
typedef unsigned int size_t;

union arg
{
	uintmax_t i;
	long double f;
	void *p;
};

// state machine 
enum {
  BARE, LPRE, LLPRE,
  STOP,
  PTR, INT, UINT, CHAR, UIPTR,
  LONG, ULONG,
  LLONG, ULLONG,
  MAXSTATE
};

// 使用 C 语言的数组特性，为数组的某一个特定单元赋值
// define a state machine function
#define S(x) [(x)-'A']
int vsprintf(char* buffer, const char* fmt, va_list ap);

// state transition table
const char states[]['z' - 'A' + 1] = {
  {
    S('d') = INT, S('u') = UINT, S('s') = PTR,
    S('c') = CHAR, S('p')= UIPTR,
    S('l') = LPRE
  },
  {
    S('d') = LONG, S('u') = ULONG, S('x') = ULONG,
    S('l') = LLPRE
  },
  {
    S('d') = LLONG, S('u') = ULLONG, S('x') = ULLONG,
  }
};

static int getint(char **s) {
	int i;
	for (i=0; ISDIGIT(**s); (*s)++) {
		if (i > INT_MAX/10U || **s-'0' > INT_MAX-10*i) i = -1;
		else i = 10*i + (**s-'0');
	}
	return i;
}

static void pop_arg(union arg *arg, int type, va_list ap)
{
	switch (type) {
	       case PTR:	arg->p = va_arg(ap, void *);
	break; case INT:	arg->i = va_arg(ap, int);
	break; case UINT:	arg->i = va_arg(ap, unsigned int);
	break; case LONG:	arg->i = va_arg(ap, long);
	break; case CHAR:	arg->i = (signed char)va_arg(ap, int);
	break; case ULONG:	arg->i = va_arg(ap, unsigned long);
	break; case ULLONG:	arg->i = va_arg(ap, unsigned long long);
	break; case UIPTR:	arg->i = (uintptr_t)va_arg(ap, void *);
	}
}
    
static const char xdigits[16] = {
	"0123456789ABCDEF"
};

// 以16进制输出
static char *fmt_x(uintmax_t x, char *s, int lower)
{
  // 32 刚好是大小写字母之间的差值
	for (; x; x>>=4) *--s = xdigits[(x&15)]|lower;
	return s;
}

static char *fmt_u(uintmax_t x, char *s)
{
	unsigned long y;
	for (   ; x>ULONG_MAX; x/=10) *--s = '0' + x%10;
	for (y=x;           y; y/=10) *--s = '0' + y%10;
	return s;
}

static void out(char** buffer, const char *s, size_t l)
{
  memcpy((void*)(*buffer), s, l);
  (*buffer) += l;
}

// 填充字符处理 
static void pad(char** buffer, char c, int w, int l, int fl)
{
	char pad[256];
  // 如果 fl 的 ZERO_PAD 标志位为1，那不输出
	if (fl & (LEFT_ADJ | ZERO_PAD) || l >= w) return;
	l = w - l;
	memset(pad, c, l>sizeof pad ? sizeof pad : l);
	for (; l >= sizeof pad; l -= sizeof pad) {
    memcpy(*buffer, pad, sizeof pad);
    (*buffer) += sizeof pad;
  }
  memcpy(*buffer, pad, l);
  (*buffer) += l;
}

int printf(const char *restrict fmt, ...)
{
  char buffer[BUFSIZE];
	int ret;
	va_list ap;
	va_start(ap, fmt);
	ret = vsprintf(buffer, fmt, ap);
	va_end(ap);
  for (int i = 0; i < ret; i++) {
    putch(buffer[i]);
  }
	return ret;
}

int sprintf(char *out, const char *fmt, ...) {
	va_list args;
	va_start(args, fmt);
	int size = vsprintf(out, fmt, args);
	va_end(args);
	return size;
}

int vsprintf(char* buffer, const char* fmt, va_list ap) {
  
  char *start, *end, *str=(char*)fmt;
  unsigned flag = 0;
  int w = 0, p = 0;
  union arg arg;

  unsigned st = BARE;
  /* unsigned ps = BARE; // ps for length specifier, and st for actual type */
  int cnt = 0, len = 0;

  char buf[sizeof(uintmax_t) * 3];
  const char* prefix;

  int type, pl; // pl for place_holder

  for ( ; ; ) {

    // 更新计数器
    cnt += len;
    if (*str == 0) break;
    
    start = str;
    // 扫描到第一个%
    while (str[0] && str[0] != '%') str++;
    end = str;

    // 扫描到第二个%
    while (str[0] == '%' && str[1] == '%') {
      str += 2;
      end ++;
    }

    len = end - start;

    // 将文字部分全部输出
    if (end - start > 0) {
      out(&buffer, start, len);
      continue;
    }

    str += 1; // 跳过%

    // 以下是对%后字符的解析
    // 首先读取 flag
    while (!ISALPHA(*str) && ISFLAG(TOFLAG(*str))) {
      flag |= TOFLAG(*str);
      str++;
    }

    // 下面读取指定的
    w = getint(&str);
    if (w < 0) goto error;
  
    // 使用状态机读取 输出类型
		st=0;
		do {
			if (!ISALPHA(*str)) goto error;
			/* ps=st; */
			st=states[st]S(*str++);
		} while (st-1<STOP);
		if (!st) goto error;
    
    // 根据类型从 va_list 取参数
    pop_arg(&arg, st, ap);

    // 准备将参数转化为字符串
    end = buf + sizeof(buf);
		prefix = "-+   0X0x";
    pl = 0;
    type = str[-1]; // type stores the conversion
    
    switch(type) {
      case 'p':
        p = MAX(p, 2*sizeof(void*));
        type = 'x';
        flag |= ALT_FORM;
      case 'x': case 'X':
        // x -> 32, X -> 0
			  start = fmt_x(arg.i, end, type & 32);
        if (arg.i != 0 && (flag&ALT_FORM) != 0) {
          // 跳转到 prefix 第7位(0X)
          prefix += (type >> 4);
          pl = 2;
        }
        break;
      case 'd': case 'i':
        pl = 1; // 负号
        if (arg.i>INT_MAX) {
          arg.i=-arg.i;
        } else {
          pl = 0;
        }
      case 'u':
        start = fmt_u(arg.i, end);
        // 如果 arg.i 为0，此时 z - a为0, 需要保证输出宽度至少为1
        p = MAX(p, end - start + !arg.i);
        break;
      case 'c':
        // 因为是倒序填充，所以需要减去宽度
        p = 1;
        start = end - p;
        *start = arg.i;
        break;
      case 's':
        putch(st==0);
        putch('\n');
        start = arg.p ? arg.p : "(null)";
        end = start + strlen(start);
        p = start - end;
        break;
    }
    
    // p 是输出宽度，z 指向buf的最后一位，a指向buf的最开始一位
    // 如果值为0，那么 end - start 为0，但是至少需要输出一位0
    p = MAX(p, end - start);
		if (p > INT_MAX-pl) goto error;

    // if length is greater than specified width, then padding 
    // is unnessary
    w = MAX(w, pl + p);
		if (w > INT_MAX-cnt) goto error;

    // 正常情况下，使用空格进行填充
		pad(&buffer, ' ', w, pl+p, flag);
    // 输出前缀
		out(&buffer, prefix, pl);
    // 有 zero flag 用 '0' 填充
    // why does not print 0?
		pad(&buffer, '0', w, pl+p, flag^ZERO_PAD);
    // 当 z-a 为0时，打印输出一个0
		pad(&buffer, '0', p, end - start, 0);
    // 输出实际内容
		out(&buffer, start, end - start);
    // 处理左对齐
		pad(&buffer, ' ', w, pl+p, flag^LEFT_ADJ);
    // w 是总输出长度
		len = w;
  }
  
  return cnt;
error:
  // panic("Overflow");
  return -1;
}
int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("not implemented");
}

#endif
