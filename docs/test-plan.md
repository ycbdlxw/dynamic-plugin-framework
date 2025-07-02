# 测试规划文档

## 1. 测试目标

本测试规划旨在规范动态插件框架的测试过程，确保系统功能正确、稳定且符合设计要求。测试将覆盖以下方面：

1. API接口功能测试
2. 插件功能测试
3. 安全认证测试
4. 性能测试

## 2. 测试文件命名规范

### 2.1 curl测试文件

curl测试文件应按以下格式命名：
```
test_<模块名>_<功能点>.curl
```

示例：
- test_common_api.curl - 通用API测试
- test_user_crud.curl - 用户CRUD操作测试
- test_plugin_command_executor.curl - 命令执行器插件测试

### 2.2 执行脚本文件

执行脚本文件应按以下格式命名：
```
run_<模块名>_<功能点>_test.sh
```

示例：
- run_common_api_test.sh - 执行通用API测试的脚本
- run_user_crud_test.sh - 执行用户CRUD操作测试的脚本
- run_plugin_command_executor_test.sh - 执行命令执行器插件测试的脚本

## 3. 测试文件目录结构

测试文件应按以下目录结构组织：

```
src/main/resources/test/           # 测试资源根目录
├── runs/                          # 测试执行脚本目录
│   ├── run_common_api_test.sh     # 通用API测试执行脚本
│   └── ...                        # 其他测试执行脚本
├── test_results/                  # 测试结果存放目录
├── test_common_api.curl           # 通用API测试用例
└── ...                            # 其他测试用例
```

## 4. curl测试文件编写规范

### 4.1 文件格式

curl测试文件必须遵循以下格式规范：

1. 每行必须以`echo`、`#`或`curl`开头
2. 所有命令必须是单行格式，不允许使用反斜杠`\`换行
3. 文件头部使用`#`注释说明测试目的和使用方法
4. 每个测试用例前使用`#`注释说明测试内容
5. 使用`echo`命令输出测试步骤信息
6. 使用`curl`命令执行API请求

### 4.2 基本结构

```
# 模块名称测试文件
# 使用方法: sh run_模块名_test.sh
# 创建日期: YYYY-MM-DD
# 创建人: 作者名

# 1. 测试用例名称
echo "执行测试用例名称..."
curl -X METHOD "URL" -H "Header1: Value1" -H "Header2: Value2" -d '{"key1":"value1","key2":"value2"}' -w "\n\nStatus: %{http_code}\nTime: %{time_total}s\n\n"

# 2. 测试用例名称
echo "执行测试用例名称..."
curl -X METHOD "URL" -H "Header1: Value1" -H "Header2: Value2" -d '{"key1":"value1","key2":"value2"}' -w "\n\nStatus: %{http_code}\nTime: %{time_total}s\n\n"
```

### 4.3 必要参数

每个curl命令应包含以下参数：

1. `-X METHOD` - 指定HTTP方法（GET, POST, PUT, DELETE等）
2. `-H "Content-Type: application/json"` - 指定内容类型
3. `-w "\n\nStatus: %{http_code}\nTime: %{time_total}s\n\n"` - 输出HTTP状态码和响应时间

### 4.4 认证相关

对于需要认证的API，应包含Authorization头：

```
-H "Authorization: Bearer ${TOKEN}"
```

## 5. 执行脚本编写规范

### 5.1 文件格式

执行脚本文件必须遵循以下格式规范：

1. 文件头部必须包含shebang行 `#!/bin/bash`
2. 文件头部必须包含脚本说明注释
3. 必须设置必要的环境变量
4. 必须创建测试结果目录
5. 必须捕获并分析测试结果
6. 必须将测试结果保存到文件

### 5.2 基本结构

```bash
#!/bin/bash

# 模块名称测试执行脚本
# 创建日期: YYYY-MM-DD
# 创建人: 作者名

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 设置测试环境变量
export TEST_HOST="localhost:8080"
export TEST_OUTPUT_DIR="test_results"
export TEST_TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
export TEST_RESULT_FILE="${TEST_OUTPUT_DIR}/${TEST_TIMESTAMP}_result.txt"

# 创建结果目录
mkdir -p ${TEST_OUTPUT_DIR}

# 打印测试信息
echo -e "${YELLOW}开始执行测试...${NC}"
echo -e "${YELLOW}测试时间: $(date)${NC}"
echo -e "${YELLOW}测试结果将保存到: ${TEST_RESULT_FILE}${NC}\n"

# 获取认证令牌（如果需要）
echo -e "${YELLOW}获取认证令牌...${NC}"
TOKEN_RESPONSE=$(curl -s -X POST "http://${TEST_HOST}/api/core/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}')

TOKEN=$(echo $TOKEN_RESPONSE | grep -o '"token":"[^"]*' | sed 's/"token":"//')

if [ -z "$TOKEN" ]; then
  echo -e "${RED}无法获取认证令牌，测试将继续但认证相关测试可能失败${NC}"
else
  echo -e "${GREEN}成功获取认证令牌${NC}"
fi

# 开始记录测试结果
echo "测试结果" > ${TEST_RESULT_FILE}
echo "测试时间: $(date)" >> ${TEST_RESULT_FILE}
echo "----------------------------------------" >> ${TEST_RESULT_FILE}

# 执行测试用例
source ../test_模块名.curl

# 测试总结
echo -e "\n${YELLOW}测试完成！${NC}"
echo -e "${YELLOW}测试结果已保存到: ${TEST_RESULT_FILE}${NC}"
```

### 5.3 结果分析

执行脚本应对每个测试用例的结果进行分析，并输出通过/失败信息：

```bash
if [[ $RESULT == *"\"code\":200"* ]]; then
  echo -e "${GREEN}测试通过${NC}"
else
  echo -e "${RED}测试失败${NC}"
fi
```

## 6. 测试用例设计指南

### 6.1 API测试用例

API测试应覆盖以下场景：

1. 正常情况 - 使用有效参数测试API的正常功能
2. 边界条件 - 测试参数边界值
3. 异常情况 - 测试无效参数、权限不足等异常情况
4. 安全测试 - 测试未授权访问、越权访问等安全问题

### 6.2 插件测试用例

插件测试应覆盖以下场景：

1. 插件加载 - 测试插件的加载和初始化
2. 插件功能 - 测试插件提供的各项功能
3. 插件卸载 - 测试插件的卸载和资源释放
4. 异常处理 - 测试插件在异常情况下的行为

## 7. 测试执行流程

### 7.1 单个测试执行

执行单个测试的步骤：

1. 进入测试目录：`cd src/main/resources/test/runs`
2. 执行测试脚本：`sh run_模块名_test.sh`
3. 查看测试结果：`cat ../test_results/YYYYMMDD_HHMMSS_result.txt`

### 7.2 批量测试执行

执行所有测试的步骤：

1. 进入测试目录：`cd src/main/resources/test/runs`
2. 执行批量测试脚本：`sh run_all_tests.sh`
3. 查看测试结果：`cat ../test_results/YYYYMMDD_HHMMSS_all_results.txt`

## 8. 测试结果格式

测试结果文件应包含以下内容：

1. 测试执行时间
2. 测试环境信息
3. 每个测试用例的请求和响应
4. 每个测试用例的通过/失败状态
5. 测试总结（总用例数、通过数、失败数）

## 9. 测试模板

### 9.1 curl测试文件模板

```
# 模块名称测试文件
# 使用方法: sh run_模块名_test.sh
# 创建日期: YYYY-MM-DD
# 创建人: 作者名

# 1. 测试用例名称
echo "执行测试用例名称..."
curl -X GET "http://${TEST_HOST}/api/endpoint" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKEN}" -w "\n\nStatus: %{http_code}\nTime: %{time_total}s\n\n"

# 2. 测试用例名称
echo "执行测试用例名称..."
curl -X POST "http://${TEST_HOST}/api/endpoint" -H "Content-Type: application/json" -H "Authorization: Bearer ${TOKEN}" -d '{"key":"value"}' -w "\n\nStatus: %{http_code}\nTime: %{time_total}s\n\n"
```

### 9.2 执行脚本模板

```bash
#!/bin/bash

# 模块名称测试执行脚本
# 创建日期: YYYY-MM-DD
# 创建人: 作者名

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 设置测试环境变量
export TEST_HOST="localhost:8080"
export TEST_OUTPUT_DIR="test_results"
export TEST_TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
export TEST_RESULT_FILE="${TEST_OUTPUT_DIR}/${TEST_TIMESTAMP}_result.txt"

# 创建结果目录
mkdir -p ${TEST_OUTPUT_DIR}

# 打印测试信息
echo -e "${YELLOW}开始执行模块名称测试${NC}"
echo -e "${YELLOW}测试时间: $(date)${NC}"
echo -e "${YELLOW}测试结果将保存到: ${TEST_RESULT_FILE}${NC}\n"

# 获取认证令牌
echo -e "${YELLOW}获取认证令牌...${NC}"
TOKEN_RESPONSE=$(curl -s -X POST "http://${TEST_HOST}/api/core/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}')

TOKEN=$(echo $TOKEN_RESPONSE | grep -o '"token":"[^"]*' | sed 's/"token":"//')

if [ -z "$TOKEN" ]; then
  echo -e "${RED}无法获取认证令牌，测试将继续但认证相关测试可能失败${NC}"
else
  echo -e "${GREEN}成功获取认证令牌${NC}"
fi

# 开始记录测试结果
echo "模块名称测试结果" > ${TEST_RESULT_FILE}
echo "测试时间: $(date)" >> ${TEST_RESULT_FILE}
echo "----------------------------------------" >> ${TEST_RESULT_FILE}

# 执行测试用例
source ../test_模块名.curl

# 测试总结
echo -e "\n${YELLOW}测试完成！${NC}"
echo -e "${YELLOW}测试结果已保存到: ${TEST_RESULT_FILE}${NC}"
```

## 10. 测试维护

### 10.1 测试更新

在以下情况下，需要更新测试用例：

1. API接口变更
2. 业务逻辑变更
3. 插件功能变更
4. 安全策略变更

### 10.2 测试结果分析

定期分析测试结果，关注以下方面：

1. 失败的测试用例
2. 响应时间异常的API
3. 错误率高的功能
4. 安全漏洞

### 10.3 测试报告

每次测试执行后，应生成测试报告，包含以下内容：

1. 测试概述（测试范围、时间、环境）
2. 测试结果统计（总用例数、通过数、失败数）
3. 失败用例分析
4. 性能分析
5. 改进建议 