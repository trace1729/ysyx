#include <klib.h>
#include <klib-macros.h>
#include <stdint.h>
#include <string.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

size_t strlen(const char *s) {
	size_t len = 0;
	while(*(s + len) != '\0') len++;
	return len;
}

size_t strnlen(const char *s, size_t maxlen) {
	size_t len = strlen(s);
	return len > maxlen? maxlen: len;
}

char *strcpy(char *dst, const char *src) {
	stpcpy(dst, src);
	return dst;
}

char *stpcpy(char *dst, const char *src) {
	char* p;
	p = mempcpy(dst, src, strlen(src));
	*p = '\0';
	return p;
}

char *strncpy(char *dst, const char *src, size_t n) {
	stpncpy(dst, src, n);
	return dst;
}

char *stpncpy(char *dst, const char *src, size_t n) {
	memset(dst, '\0', n);
	return mempcpy(dst, src, strnlen(src, n));
}

char *strcat(char *dst, const char *src) {
	stpcpy(dst + strlen(dst), src);
	return dst;
}

int strcmp(const char *s1, const char *s2) {
	while (*s1 && *s2) {
		if (*s1 != *s2) break;
		s1++; 
		s2++;
	}
	return *s1 - *s2;
}

int strncmp(const char *s1, const char *s2, size_t n) {
	unsigned i = 0;
	for (; i < n; i++) {
		if (*s1 != *s2) return *s1 - *s2;
		s1++; 
        s2++;
	}
	return 0;
}

void *memset(void *s, int c, size_t n) {
	unsigned i = 0;
	char* src = s;
	for (; i < n ; i++) {
		*src++ = c;
	}
	return s;
}

void *memmove(void *dst, const void *src, size_t n) {
	int i = 0;
	const char* s = src;
	char* d = dst;

	// src ------- dst ------- src + n
	if (src < dst && src + n > dst) {
		i = n;
		d += n;
		s += n;
		while (i > 0) {
			*d = *s;
			d--; s--;
			i--;
		}
	} else {
		while (i < n) {
			*d = *s;
			d++; s++;
			i++;
		}
	}
	return dst;
}

void* mempcpy(void *out, const void *in, size_t n) {
	unsigned i = 0;
	const char* src = in;
	char* dst = out;
	while (i < n) {
		*dst = *src;
		dst++; src++;
		i++;
	}
	return dst;
}

void *memcpy(void *out, const void *in, size_t n) {
	mempcpy(out, in, n);
    return out;
}

int memcmp(const void *s1, const void *s2, size_t n) {
	const unsigned char* st1 = s1, *st2 = s2;
	unsigned i = 0;
	for (; i < n; i++) {
		if (*st1 != *st2) return *st1 - *st2;
		st1++; st2++;
	}
	return 0;
}

#endif
