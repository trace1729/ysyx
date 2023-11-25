/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include "debug.h"
#include "macro.h"
#include "sdb.h"
#include "watchpoint.h"
#include <assert.h>
#include <stdio.h>

#define NR_WP 32

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;


// setup #id for each watch point node, 
// and link the (next) field of wp node to the following node
// the next field of the last wp node points to NULL

// head points to the first wp node in use
// free_ points to the free wp nodes
void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);
  }

  head = NULL;
  free_ = wp_pool;
}

WP* new_wp() {
  assert(free_ != NULL);
  WP* backup = head;
  head = free_;
  free_ = free_->next;
  head->next = backup;
  return head;
}

void free_wp(int NO) {

  Check(head != NULL, "watchpoint does not exists"); 

  WP* wp, *backup;
  // free head
  if (head->NO == NO) {
    backup = free_;
    free_ = head;
    head = head->next;
    free_->next = backup;
    return;
  } 

  // If thereis only one wp node, and does not match, report an error
  Check(head->next != NULL, "watchpoint does not exists");

  // 2..remaining
  for(wp = head; wp->next != NULL; wp = wp->next) {
    // what success goes wrong
    if (wp->next->NO == NO) {
      WP* backup = free_;
      free_ = wp->next;
      assert(wp->next != NULL);
      wp->next = wp->next->next;
      free_->next = backup;
      break;
    }
  }

  Check(free_->NO == NO, "watchpoint does not exists");
  return;
error:
  return;
}

bool watchpoint_stop()
{
  bool success = true;
  WP* wp;
  for(wp = head; wp != NULL; wp = wp->next) {
    // expr
    unsigned int n = expr(wp->exp, &success);
    if (!success || wp->res != n) {
      /* Log("\ntriggering watchpoints id: %10d, expr: %10s, res_prev: %10u, res_now: %10u", \
          wp->NO, wp->exp, wp->res, n);*/ 
      wp->res = n;
      break;
    }
  }

  // execute expression failed
  if (!success) return true;
  
  // if wp != null, means expression changes
  if (wp != NULL) return true;

  return false;
}


void watchpoint_display() {
  WP* wp;
  if (head == NULL) {
    printf("No watch point set\n");
  } else {
    printf("%10s%10s%10s\n", "num", "what", "value");
  }

  for(wp = head; wp != NULL; wp = wp->next) {
    // what success goes wrong
    printf("%10d%10s 0x%02x\n", wp->NO, wp->exp, wp->res);
  }

  // for testing purpost only
  int cnt = 0;
  for (wp = free_; wp != NULL; wp = wp->next) {
    cnt++;
  }
  printf("free node cnt: %d\n", cnt);
}

const char* test_expr[] = {
  "1 + 2*$ra",
  "1 + 2*$a0",
  "1 + 2*$a1",
  "1 + 2*$a2",
  "1 + 2*$a3",
  "1 + 2*$a4",
  "1 + 2*$a5",
  "1 + 2*$a6",
};

#define TEST_LEN ARRLEN(test_expr)

void wp_test_bench()
{

  watchpoint_display();
  assert(free_ == wp_pool);

  for (int i = 0; i < TEST_LEN; i++) {
    WP* wp = new_wp();
    mempcpy(wp->exp, test_expr[i], strlen(test_expr[i]));
    wp->exp[strlen(test_expr[i])] = '\0';
    watchpoint_display();
  }

  // free node in order;
  for (int i = TEST_LEN - 1; i >= 0; i--) {
    free_wp(i);
    watchpoint_display();
  }

  assert(head == NULL);
}


