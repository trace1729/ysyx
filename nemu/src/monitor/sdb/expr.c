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
#include <isa.h>

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>
#include <string.h>

enum {
  TK_NOTYPE = 256, TK_EQ, TK_NUM

};

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  {"[0-9]+", TK_NUM},    // spaces
  {" +", TK_NOTYPE},    // spaces
  {"\\+", '+'},         // plus
  {"-", '-'},         // minus
  {"\\*", '*'},         // multiple
  {"/", '/'},         // div
  {"==", TK_EQ},        // equal
  {"\\(", '('},        // equal
  {"\\)", ')'},        // equal
};

static int priority[] __attribute__((used)) = {
  ['+'] = 0,
  ['-'] = 0,
  ['*'] = 1,
  ['/'] = 1,
  ['('] = 2,
  [')'] = 2,
};

#define NR_REGEX ARRLEN(rules)
#define BAD_EXPRESSION -1

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex() {
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i ++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

static Token tokens[32] __attribute__((used)) = {};
static int nr_token __attribute__((used))  = 0;

static bool make_token(char *e) {
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0') {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i ++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
            i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */


        switch (rules[i].token_type) {
          case '+':
          case '-':
          case '*':
          case '/':
          case '(':
          case ')':
            break;
          case TK_EQ:
            break;
          case TK_NUM:
            substr_len = substr_len > 31? 31: substr_len; // truncate to 32 bits
            mempcpy(tokens[nr_token].str, substr_start, substr_len);
            tokens[nr_token].str[substr_len] = '\0';
            Log("copy to tokens %s", tokens[nr_token].str);
            break;
          default:
            break;
        }
        if (rules[i].token_type != TK_NOTYPE) {
          tokens[nr_token++].type = rules[i].token_type;
        }
        break; // found a match, break
      }
    }

    if (i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }

  return true;
}
static bool is_arithmatic(int type) {
  return type == '+' || \
               type == '-' || \
               type == '*' || \
               type == '/';

}

static bool check_parentheses(int l, int r) {
  if (tokens[l].type != '(' || tokens[r].type != ')') {
    return false;
  }

  int stack = 1; // the first symbol must be a ( 

  for (int i = l + 1; i <= r; i ++) {
    // give the responsibility to function eval
    if (stack < 0)
      return false;
    // stack == 0 means the leftmost brackets is canceled along the way.
    if (stack == 0 && i != r) 
      return false;

    if (tokens[i].type == '(') 
      stack++; 
    else if (tokens[i].type == ')') 
      stack--;
  }

  return stack == 0;
}

int find_prime_operator(int l, int r) {
  
  int stack = 0;
  char prime_op = BAD_EXPRESSION;

  for (int i = l; i <= r; i ++) {
    if (stack < 0) return BAD_EXPRESSION;

    if (tokens[i].type == '(') stack++;
    else if (tokens[i].type == ')') stack--;
    else if (is_arithmatic(tokens[i].type) && !stack){
      if (prime_op == BAD_EXPRESSION) {
        prime_op = i;
      } else {
        prime_op = priority[(int)prime_op] >= priority[tokens[i].type]? i: prime_op;
      }
    }
  }
  return prime_op;
}

uint32_t eval(int l, int r) {
  Log("call eval(%d, %d)", l, r);
  if (l > r) {
    return BAD_EXPRESSION;
  }

  if (l == r) {
    // may be inlegal input
    Log("evaluate %s", tokens[l].str);
    return strtol(tokens[l].str, NULL, 10);
  } else if (check_parentheses(l, r)){
    Log("removeing brackets");
    // if vaild, drop brackets directly
    return eval(l + 1, r - 1);

  } else {
    
    // find prime operator (idx)
    int prime_op = find_prime_operator(l, r);
    
    Check(prime_op != BAD_EXPRESSION, "eval: Wrong prime_operator!");

    uint32_t val_l = eval(l, prime_op - 1);
    uint32_t val_r = eval(prime_op + 1, r);

    Log("%d %c %d", val_l, tokens[prime_op].type, val_r);

    Check(val_l != BAD_EXPRESSION, "eval: wrong operand l");
    Check(val_r != BAD_EXPRESSION, "eval: wrong operand r");

    switch (tokens[prime_op].type) {
      case '+':return val_l + val_r;
      case '-':return val_l - val_r;
      case '*':return val_l * val_r;
      case '/':return val_l / val_r;
      default:return BAD_EXPRESSION;
    }
  }
error:
  return BAD_EXPRESSION;
}

word_t expr(char *e, bool *success) {
  if (!make_token(e)) {
    *success = false;
    return 0;
  }

  return eval(0, nr_token - 1);
}
