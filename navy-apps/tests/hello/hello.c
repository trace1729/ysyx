#include <unistd.h>
#include <stdio.h>

int main() {
  void* a = sbrk(0);
  write(1, "hello\n", 6);
  return 0;
}
