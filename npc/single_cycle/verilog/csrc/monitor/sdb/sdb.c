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

#include <isa.h>
#include <cpu/cpu.h>
#include <readline/readline.h>
#include <readline/history.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <memory/vaddr.h>
#include "sdb.h"
#include "watchpoint.h"

static int is_batch_mode = false;

void init_regex();
void init_wp_pool();

/* We use the `readline' library to provide more flexibility to read from stdin. */
static char* rl_gets() {
  static char *line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(nemu) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}

static int cmd_c(char *args) {
  cpu_exec(-1);
  return 0;
}


static int cmd_q(char *args) {
  nemu_state.state = NEMU_QUIT;
  return -1;
}

static int cmd_d(char *args) {
  char *arg = strtok(NULL, " ");
  unsigned int id = strtoul(arg, NULL, 10);
  free_wp(id);
  return 0;
}

static int cmd_p(char *args) {
  bool success = true;
  unsigned int res = 0;
  if (args != NULL)
    res = expr(args, &success);
  printf("Dec: %u, Hex:0x%x\n", res, res);
  return success? 0: -1;
}

static int cmd_help(char *args);
static int cmd_info(char *args);
static int cmd_si(char *args);
static int cmd_x(char *args);
static int cmd_w(char* args);
enum {
  HELP=0, INFO, SI, C, X, Q, P, D
};

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  { "help", "Display information about all supported commands", cmd_help },
  { "info", "Display information about registers and watchpoints", cmd_info },
  { "si", "step [N] instructions exactly", cmd_si },
  { "c", "Continue the execution of the program", cmd_c },
  { "x", "scanning memory", cmd_x },
  { "q", "Exit NEMU", cmd_q },
  { "p", "eval expression", cmd_p },
  { "w", "add watchpoint", cmd_w },
  { "d", "delete watch points", cmd_d },

  /* TODO: Add more commands */

};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char *args) {
  /* extract the first argument */
  char *arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i ++) {
      printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
    }
  }
  else {
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

static int cmd_info(char* args)
{
  /* extract the first argument */
  char *arg = strtok(NULL, " ");

  if (arg == NULL) {
    printf("%s\n", cmd_table[INFO].description);
    return 0;
  }
  
  if (strlen(arg) != 1 || (arg[0] != 'r' && arg[0] != 'w')) {
    Log("unexpected argument %s found, argument can only be `w` or `r`", arg);
    return 0;
  }  

  switch (arg[0]) {
    case 'r': isa_reg_display(); break;
    case 'w': watchpoint_display(); break;
    default: break;
  }

  return 0;
}


static int cmd_si(char* args)
{
  int steps;
  char* arg = strtok(args, " ");

  if (arg == NULL) {
    steps = 1;
  } else {
    steps = strtol(arg, NULL, 10);
  }

  if (!steps) {
    Log("input argument %s contains invaild characters, please check", arg);
    return 0;
  }

  cpu_exec(steps);
  
  return 0;
}

static int cmd_x(char* args) {

  char* arg1 = strtok(args, " ");
  int N;
  uint32_t s;

  // check argument 1
  if (arg1 == NULL) {
    printf("%s", cmd_table[X].description);
    return 0;
  } else {
    N = strtol(arg1, NULL, 10);
  }

  if (!N) {
    Log("input argument %s contains invaild characters, please check", arg1);
    return 0;
  }

  char* arg2 = args + strlen(arg1) + 1;
  arg2 = strtok(arg2, " ");

  bool success = true;
  // check argument 2
  if (arg2 == NULL) {
    printf("%s", cmd_table[X].description);
    return 0;
  } else {
    s = expr(arg2, &success);
  }

  if (!success) {
    Log("expression evaluating error");
    return 0;
  }

  if (!s) {
    Log("input argument %s contains invaild characters, please check", arg2);
    return 0;
  }
  
  for (int i = 0; i < N; i ++, s += sizeof(vaddr_t)) {
    printf("0x%x ",vaddr_read(s, sizeof(vaddr_t)));
  }
  printf("\n");
  
  return 0;
}

static int cmd_w(char* args) {
  if (!args) return 0;
  
  bool success = true;
  char *arg = strtok(NULL, " ");
  WP* wp = new_wp();
  mempcpy(wp->exp, arg, strlen(arg));
  wp->exp[strlen(arg)] = '\0';
  wp->res = expr(wp->exp, &success);
  return success? 0 : -1;
}

void sdb_set_batch_mode() {
  is_batch_mode = true;
}

void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    return;
  }

  // read from std input (nemu) xxxx
  for (char *str; (str = rl_gets()) != NULL; ) {
    char *str_end = str + strlen(str); // '\0'

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) {char tmp[20] = "1"; cmd_si(tmp); continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1; // +1 means bypass space 
    if (args >= str_end) {
      args = NULL;
    }

#ifdef CONFIG_DEVICE
    extern void sdl_clear_event_queue();
    // sdl_clear_event_queue();
#endif

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { return; }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}

void init_sdb() {
  /* Compile the regular expressions. */
  init_regex();

  /* Initialize the watchpoint pool. */
  init_wp_pool();
}
