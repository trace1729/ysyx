#include <common.h>

static void statistic() {
/*   IFNDEF(CONFIG_TARGET_AM, setlocale(LC_NUMERIC, "")); */
/* #define NUMBERIC_FMT MUXDEF(CONFIG_TARGET_AM, "%", "%'") PRIu64 */
/*   Log("host time spent = " NUMBERIC_FMT " us", g_timer); */
/*   Log("total guest instructions = " NUMBERIC_FMT, g_nr_guest_inst); */
/*   if (g_timer > 0) Log("simulation frequency = " NUMBERIC_FMT " inst/s", g_nr_guest_inst * 1000000 / g_timer); */
  Log("failed");
}

void assert_fail_msg() {
  statistic();
}

void cpu_exec(uint64_t n) {
  printf("n=%lu, Sorry, instruction exection not implemented yet\n", n);
}

