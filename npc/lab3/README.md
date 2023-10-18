## What Have You Learnt ?

- 如何比较两个有符号数的大小关系。

    $\text{less} = \text{OF} \oplus \text{results[} n - 1\text{]}$

- 设计 Soc 过程中，所使用到的软件和技术是什么。
- 什么是硬件编程，什么是软件编程。
    - 硬件编程：通过 硬件描述语言(HDL)来定义硬件结构。(比如使用 vivado 编写 verilog)
    - 软件编程：通过 高级语言生成 .hex 文件，加载到硬件的 ROM 中，操控硬件完成任务 (比如使用 keil 写 c 语言)
    Modelsim 仿真软件。可将编写的 hex 文件内容加载到 HDL 所描述的硬件中去。
- 熟悉 verilog 语法, 更深刻的理解了 硬件描述语言的含意

- 目前编写的 alu 效率太低，考虑之后进行优化
