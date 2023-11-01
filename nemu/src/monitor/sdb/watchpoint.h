
#ifndef __WATCHPOINT_H__
#define __WATCHPOINT_H__

#include <common.h>
#define MAX_EXP_LEN 1000

typedef struct watchpoint {
  int NO;
  char exp[MAX_EXP_LEN];
  unsigned int res;
  struct watchpoint *next;

  /* TODO: Add more members if necessary */

} WP;

WP* new_wp();
void free_wp(int NO);
bool watchpoint_stop();
void watchpoint_display();

#endif
