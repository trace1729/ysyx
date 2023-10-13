# Bug Recording

- 如何在 代码里 里设置 top.v 生成的双控开关, 如何加载 元器件。

- 为什么 git 提交不会自动同步到 tracer 分支

    由于使用zsh运行的makefile，zsh中的配置文件没有 $(NEMU) 环境变量的定义, 所以调用 git-commit 函数失效

- 运行 NVBoard 后一段时间，程序无响应是正常现象吗

- 需要研究 nvboard 中的 Makefile, 再尝试将我们的双控开关接入到 NVboard
    实例工程有几大模块
    - 15-8: 流水灯
    - 0-7 : 开关控制亮灭
    - 数码管
    - VGA 显示屏

    constr/top.nxdc 这个文件是引脚的描述文件，之后基于这个脚本文件，生成对应的 cpp 代码

    Makefile 使用 `verilator` 和 cpp 源文件、verilog 源文件、生成的 auto_bind.cpp 以及 nvboard 的静态链接库文件 生成
    可执行文件

    cpp 文件主要是加载 auto_bind.cpp 里的函数

- 绑定引脚成功，但是led灯的输出不受开关的控制
