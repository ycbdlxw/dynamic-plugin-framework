# 测试服务说明文档

## 1. 测试服务概述

测试服务提供了一个自动化的接口测试框架,支持通过脚本文件批量执行HTTP接口测试。主要特性包括:

- 支持解析和执行curl命令
- 自动处理登录和token管理
- 生成详细的测试报告
- 支持多个测试用例的串行执行
- 支持自定义结果保存目录

## 2. 测试脚本编写指南

### 2.1 脚本格式

测试脚本使用 **`.curl` 扩展名**，文件中只能包含两类行：

1. 以 `#` 开头的注释
2. 以 `curl` 开头的单行 HTTP 请求

脚本中**禁止**出现 `echo`、`mkdir`、反斜杠续行符(`\`)等任何 Shell 语句，插件会在执行前进行严格校验，发现不符合规范的行会立刻返回错误并终止执行。

### 2.2 命令格式

```bash
# 命令说明注释
curl -X <METHOD> '<URL>' \
  -H '<HEADER_NAME>: <HEADER_VALUE>' \
  -d '<REQUEST_BODY>'
```

### 2.3 注意事项

1. 第一条命令通常为登录，用于自动提取 Token；后续使用 `Bearer your-token-here` 占位符。
2. 所有 `curl` 必须写成 *单行*，不允许使用 `\` 换行。
3. 若校验失败，接口将返回形如 `脚本第 12 行含有非允许内容: echo "Hello"` 的提示，并不会继续执行。

## 3. 测试接口说明

### 3.1 接口地址

```
POST /api/test/execute
```

### 3.2 请求参数

| 参数名       | 类型    | 必填 | 说明                                                |
| ------------ | ------- | ---- | --------------------------------------------------- |
| scriptPath   | String  | 是   | 测试脚本文件路径                                    |
| resultDir    | String  | 否   | 测试结果保存目录,为空时使用默认目录(test_results)   |
| useCurrentDir| Boolean | 否   | 是否使用脚本所在目录作为基准目录,默认为false        |

### 3.3 响应结果

```json
{
    "code": 200,
    "result": {
        "totalCommands": 3,
        "successCount": 2,
        "failureCount": 1,
        "commandResults": [
            {
                "commandName": "登录测试",
                "command": "curl ...",
                "success": true,
                "result": "...",
                "errorMessage": null
            }
        ],
        "resultFilePath": "test_results/20240321_123456_result.txt"
    },
    "message": null,
    "timestamp": 1642612345678
}
```

## 4. 测试结果说明

### 4.1 结果统计

- totalCommands: 总命令数
- successCount: 成功执行的命令数
- failureCount: 执行失败的命令数

### 4.2 命令结果

- commandName: 命令名称
- command: 实际执行的命令
- success: 是否执行成功
- result: 执行结果
- errorMessage: 错误信息(如果有)

### 4.3 结果文件

- 测试结果会保存到指定目录下(如未指定则使用test_results目录)
- 文件名格式: yyyyMMdd_HHmmss_result.txt
- 包含每个命令的执行结果和总体执行统计
- 如果指定的目录不存在,系统会自动创建

## 5. 示例说明

### 5.1 测试脚本示例

```bash
#!/bin/bash

# 登录测试
curl -X POST 'http://localhost:8080/api/common/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "password": "ycbd1234"
}'

# 测试新增用户
curl -X POST 'http://localhost:8080/api/common/save' \
  -H 'Authorization: Bearer your-token-here' \
  -H 'Content-Type: application/json' \
  -d '{
    "targetTable": "sys_user",
    "username": "test_user",
    "password": "123456",
    "real_name": "测试用户",
    "phone": "13800138000",
    "email": "test@example.com",
    "org_id": 1,
    "status": 1
}'

# 查询用户列表
curl -X GET "http://localhost:8080/api/common/list?targetTable=sys_user&username=test_user" \
  -H "Authorization: Bearer your-token-here"
```

### 5.2 执行测试

```bash
# 使用默认结果目录
curl -X POST 'http://localhost:8080/api/test/execute?scriptPath=src/main/resources/test/test_user_crud.curl'

# 指定结果目录（绝对路径）
curl -X POST 'http://localhost:8080/api/test/execute?scriptPath=src/main/resources/test/test_file_pipeline.curl&resultDir=/tmp/results'

# 使用脚本目录作为相对结果目录
curl -X POST 'http://localhost:8080/api/test/execute?scriptPath=src/main/resources/test/test_ai_processor.curl&resultDir=results&useCurrentDir=true'
```

### 5.3 测试结果示例

```
登录测试结果:
{"code":200,"result":{"token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."},"message":null,"timestamp":1642612345678}

新增用户结果:
{"code":200,"result":{"id":1},"message":null,"timestamp":1642612345679}

查询用户列表结果:
{"code":200,"result":{"items":[{"username":"test_user","real_name":"测试用户"}],"total":1},"message":null,"timestamp":1642612345680}

执行总结:
总命令数: 3
成功数: 3
失败数: 0
```