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
#include <assert.h>
#include <common.h>
#include <stdio.h>
#include <stdlib.h>
#include "monitor/sdb/sdb.h"
#include "monitor/sdb/watchpoint.h"

void init_monitor(int, char *[]);
void am_init_monitor();
void engine_start();
int is_exit_status_bad();

void sdb_arthimetic_test() {
  FILE *fp = fopen("/home/trace/trace/learning/ysyx/ysyx-workbench/nemu/tools/gen-expr/log_1000", "r");

  assert(fp != NULL);
  char buf[65536+10];
  bool success;
  for (int i = 0; i < 800; i++) {
    // read oneline into the buf; 
    // will continue read the last line of the file
    fgets(buf, ARRLEN(buf), fp);
    // split line by spaces
    char* c_res = strtok(buf, " ");
    // parse the interger
    unsigned int res = strtol(c_res, NULL, 10);
    // remainging should be the expression
    char* c_expr = buf + strlen(c_res) + 1;
    // remove \n in the end of the line
    assert(*(c_expr + strlen(c_expr) - 1) == '\n');
    *(c_expr + strlen(c_expr) - 1) = '\0';
    unsigned int actual = expr(c_expr, &success);
    Check(res == actual, "failed on #%d, expression is %s, expected %u , but %u", i, c_expr, res, actual) ;
    printf("PASSED #%d\n", i);
  }

  
error:
  fclose(fp);
  return;

}

int main(int argc, char *argv[]) {
  /* Initialize the monitor. */
#ifdef CONFIG_TARGET_AM
  am_init_monitor();
#else
  init_monitor(argc, argv);
#endif

  // test expresion parser
  sdb_arthimetic_test();

  // test watch points
  // wp_test_bench();

  /* Start engine. */
  engine_start();

  return is_exit_status_bad();
}
