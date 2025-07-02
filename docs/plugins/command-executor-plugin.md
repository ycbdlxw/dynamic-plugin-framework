# CommandExecutor 插件

跨平台命令行执行能力，支持在 **Windows / Linux / macOS** 上统一调用 Shell / CMD 命令。

## 插件元信息

| 配置项 | 值 |
| ------ | ------------------------------------------------ |
| plugin_name | CommandExecutor |
| class_name | com.ycbd.demo.plugin.commandexecutor.CommandExecutorPlugin |
| description | 跨平台命令行执行插件 |
| is_active | true *(已在 `data.sql` 中默认激活)* |

## 组件一览

| 组件 | 说明 |
| ------------------------------ | -------------------------------- |
| `CommandExecutionService` | 核心服务。根据系统类型组装 `ProcessBuilder`，返回 `exitCode / stdout / stderr` |
| `CommandExecutorController` | REST 接口 `/api/command/execute` |

## API 说明

### POST /api/command/execute

| 参数 | 类型 | 必填 | 说明 |
| ------ | ---- | ---- | -------------------------------- |
| command | string | ✔ | 要执行的完整命令，如 `ls -l` / `echo hello` |

响应体 (`ApiResponse.data`)：

```json
{
  "exitCode": 0,
  "stdout": "hello\n",
  "stderr": ""
}
```

> 若 `exitCode` ≠ 0，表示执行错误，可查看 `stderr` 获取原因。

## 安全
- 接口未加入白名单，需要正常携带 JWT `Authorization: Bearer <token>`。

## 测试脚本 `test_command_executor.sh`

```bash
#!/bin/bash

# 登录获取 JWT Token
curl -X POST 'http://localhost:8080/api/core/login' -H 'Content-Type: application/json' -d '{"username":"admin","password":"ycbd1234"}'

# 加载插件（若已加载会返回已加载提示）
curl -X POST 'http://localhost:8080/api/system/plugins/load?pluginName=CommandExecutor' -H 'Authorization: Bearer your-token-here'

# 执行示例命令
echo "执行 echo hello："
curl -X POST 'http://localhost:8080/api/command/execute' -H 'Content-Type: application/json' -H 'Authorization: Bearer your-token-here' -d '{"command":"echo hello"}'
```

> 登录返回的 token 请替换到 `your-token-here` 位置即可。 