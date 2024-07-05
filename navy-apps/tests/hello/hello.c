#include <unistd.h>
#include <stdio.h>

char buf[200];
int main() {
  
  setbuffer(stdout, buf, 100);
  printf("hello world");

  return 0;
}
