# TestService 脚本解析与执行流程（优化版）

本文档描述了 2025-07-07 对 `TestServicePlugin` 所做的脚本解析与执行逻辑精简/优化方案，以及整体业务流程图，方便后续维护与审阅。

## 1. 业务需求摘要

1. **脚本格式极简**：脚本中仅允许三类行首标记：`#`（注释）、`echo`（信息提示）与 `curl`（单行 HTTP 请求）。可选保留 `rm` 用于清理临时文件。
2. **Token 占位符约定**：如接口需鉴权，在脚本后续 `curl` 行中使用 `Bearer your-token-here`（或 `${TOKEN}`/`$TOKEN`）占位符；首条 `curl` 必须是登录接口，用于获取 Token。
3. **按需登录**：若脚本 **未** 出现以上占位符，则认为不需要 Token，所有 `curl` 直接顺序执行；若出现占位符，则执行登录获取 Token，失败立即终止脚本。
4. **即时执行/即时记录**：每条 `curl` 立刻执行、实时写入结果文件，不再拼接多行命令。
5. **错误快速失败**：登录失败或脚本出现非法行，立即记录错误并中断执行。

## 2. 关键逻辑步骤

1. 读取脚本，并检查是否包含 Token 占位符 (`requiresToken`)。
2. 逐行遍历脚本：
   - 空行跳过。
   - `#`：更新当前测试用例描述。
   - `echo`：仅用于可读性，记录/忽略。
   - `curl`：
     * 若 `requiresToken==true` 且当前为首条 `curl` ⇒ 作为登录请求执行；尝试从标准输出或重定向文件中提取 Token。
     * 提取失败 ⇒ 写入 "登录失败" 并终止脚本。
     * 其余 `curl` 行：若包含 Token 占位符则在执行前用已提取 Token 替换；然后执行并记录结果。
3. 循环结束后，输出汇总统计（总命令数 / 成功 / 失败）。

## 3. 业务流程图

```mermaid
flowchart TD
    A["Read script"] --> B{"Contains token placeholder?"}
    B --|No| C["No token required"]
    C --> D["Process lines"]

    B --|Yes| L["Token required"]
    L --> D

    subgraph PROC["Line-by-line processing"]
        D --> I{"Line type"}
        I --|Comment / echo / empty| D

        I --|curl| J{"First curl & need token?"}
        J --|Yes| K["Execute login"]
        K --> M{"Token extracted?"}
        M --|No| N["Fail & abort"]
        M --|Yes| O["Save token"]
        O --> D

        J --|No| P["Replace token & execute curl"]
        P --> Q["Record result"]
        Q --> D
    end
```

> **说明**：流程图中灰色路径（否分支）代表无需 Token 的简化路径；绿色路径（是分支）表示需要 Token 的完整流程。

## 4. 更新记录

| 日期 | 版本 | 说明 |
| ---- | ---- | ---- |
| 2025-07-07 | 1.0 | 初稿，新增登录判定逻辑、即时执行/记录、快速失败机制，并附流程图 | 