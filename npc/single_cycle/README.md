# 贡献指南

本项目是一个单周期CPU的实现，使用的是 chisel 语言。如果你对本项目感兴趣，欢迎参与贡献。

## 如何为处理器添加指令

- 在 `src/controlLogic.scala` 为增加的指令添加控制逻辑
- 在 `src/ImmGen.scala` 为增加的指令添加立即数生成逻辑
- 在 `src/CPU.scala` 为增加的指令添加数据通路逻辑
