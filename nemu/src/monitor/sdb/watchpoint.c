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
  assert(head != NULL);
  WP* wp, *backup;
  // free head
  if (head->NO == NO) {
    backup = free_;
    free_ = head;
    head = head->next;
    free_->next = backup;
    return;
  } 
  // If thereis only one wp node, and do not match, report an error
  if (head->next == NULL) {
    Log("watch pointing not exists");
    return;
  }

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

  if (wp->next == NULL) {
    Log("watch pointing not exists");
  }
  
  return;
}

bool watchpoint_stop()
{
  bool success = true;
  WP* wp;
  for(wp = head; wp != NULL; wp = wp->next) {
    // what success goes wrong
    unsigned int n = expr(wp->exp, &success);
    if (!success || wp->res != n) {
      break;
    }
  }

  // execute expression failed
  if (!success) return true;
  
  // if wp != null, means expression changes
  if (!wp) return true;

  return false;
}


void watchpoint_display() {
  WP* wp;
  printf("%10s%10s", "num", "what");
  for(wp = head; wp != NULL; wp = wp->next) {
    // what success goes wrong
    printf("%10d%10s\n", wp->NO, wp->exp);
  }
}
