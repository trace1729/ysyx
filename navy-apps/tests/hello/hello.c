#include <unistd.h>
#include <stdio.h>

int main() {
  void* a = sbrk(0);
  write(1, "hello", 5);
  return 0;
}
