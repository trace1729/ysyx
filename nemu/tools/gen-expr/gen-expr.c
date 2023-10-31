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

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>

// this should be enough
static char buf[65536] = {};
static unsigned head = 0;
static char code_buf[65536 + 128] __attribute__((used)) = {}; // a little larger than `buf`
static char *code_format __attribute__((used))=
"#include <stdio.h>\n"
"int main() { "
"  unsigned result = %s; "
"  printf(\"%%u\", result); "
"  return 0; "
"}";
/* static char* type_unsigned = "(unsigned int)"; */

#define MAXLEN 9
#define MAXDEPTH 6
#define MAXSPACELEN 4
#define DEBUG(format, ...) printf("debug: " format "\n", ## __VA_ARGS__)

static void gen_num() {
  // whethet to insert spaces
  int rand_space_len = 0;
  int space_generate = rand() % 2;
  if (space_generate == 1) {
    rand_space_len = rand() % MAXSPACELEN + 1;
  }
  for (int i = 0; i < rand_space_len; i++) {
    buf[head++] = ' ';
  }

  // generate random number
  int rand_len = rand() % MAXLEN + 1;
  buf[head++] = '1' + rand() % 9;
  for (int i = 1; i < rand_len; i++) {
    buf[head++] = '0' + rand() % 10;
  }
  buf[head++] = 'u';
}

static void gen_rand_op() {
  switch (rand() % 4) {
    case 0: buf[head++] = '+'; break;
    case 1: buf[head++] = '-'; break;
    case 2: buf[head++] = '*'; break;
    case 3: buf[head++] = '/'; break;
  }
}

__attribute__((used))
static void gen_rand_expr(int depth) {
//DEBUG("gen_rand_expr: depth %d", depth);
  
  int choice;
  if (depth > MAXDEPTH) {
    choice = rand() % 2;
  } else {
    choice = rand() % 3;
  }

  switch (choice) {
    case 0: gen_num(); break;
    case 1: 
            buf[head++] = '(';
            gen_rand_expr(depth);
            buf[head++] = ')';
            break;
    default:
            if (depth > MAXDEPTH)
              break;
            gen_rand_expr(depth + 1);
            gen_rand_op();
            gen_rand_expr(depth + 1);
            break;
  }
}

int main(int argc, char *argv[]) {
  int seed = time(0);
  srand(seed);
  int loop = 1;
  if (argc > 1) {
    sscanf(argv[1], "%d", &loop);
  }
  int i;
//DEBUG("generate %d expr", loop);
  for (i = 0; i < loop; i ++) {
    gen_rand_expr(0);
    buf[head] = '\0';
//DEBUG("generate #%d expr: %s", i, buf);
    head = 0;
    sprintf(code_buf, code_format, buf);

    FILE *fp = fopen("/tmp/.code.c", "w");
    assert(fp != NULL);
    fputs(code_buf, fp);
    fclose(fp);

    int ret = system("gcc -O2 -Wall -Werror /tmp/.code.c -o /tmp/.expr");
    if (ret != 0) continue;

    fp = popen("/tmp/.expr", "r");
    assert(fp != NULL);

    int result;
    ret = fscanf(fp, "%d", &result);
    pclose(fp);

    printf("%u %s\n", result, buf);
  }
  return 0;
}
