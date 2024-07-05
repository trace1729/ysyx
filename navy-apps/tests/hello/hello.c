#include <unistd.h>
#include <stdio.h>

int main() {
  void* a = sbrk(0);
  return 0;
}
