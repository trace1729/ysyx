#include <stdio.h>
#include <sys/time.h>
#include <NDL.h>

int main() {
  NDL_Init(0);
  int sec = 1;
  int i = 0;
  while (1) {
    while ((NDL_GetTicks() / 500) == sec) {
      printf("%d half sec\n", sec++);
    }
  }
  NDL_Quit();
  return 0;
}
