#!/bin/bash

# 测试WHERE子句构建优化的脚本
# 作者：ycbd
# 日期：$(date +%Y-%m-%d)

# 设置变量
API_BASE="http://localhost:8080"
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CURL_FILE="${SCRIPT_DIR}/test_where_builder.curl"
RESULT_DIR="${SCRIPT_DIR}/test_results"
mkdir -p "${RESULT_DIR}"
echo "=== 执行测试用例: ${CURL_FILE} ==="
curl -s -X POST "${API_BASE}/api/test/execute?scriptPath=${CURL_FILE}&resultDir=${RESULT_DIR}&useCurrentDir=true"
echo "=== 用例执行完成，结果请查看: ${RESULT_DIR} ==="

# 设置变量
HOST="http://localhost:8080"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULT_FILE="src/main/resources/test/test_results/${TIMESTAMP}_where_builder_result.txt"
TOKEN=""

# 创建结果目录
mkdir -p src/main/resources/test/test_results

# 登录获取token
echo "正在登录获取Token..."
TOKEN=$(curl -s -X POST "${HOST}/api/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "登录失败，无法获取Token"
  exit 1
fi

echo "Token获取成功: ${TOKEN:0:10}..."

# 记录测试开始
echo "=== WHERE子句构建优化测试 ===" > $RESULT_FILE
echo "测试时间: $(date)" >> $RESULT_FILE
echo "============================" >> $RESULT_FILE

# 执行测试并记录结果
run_test() {
  local test_name=$1
  local curl_cmd=$2
  
  echo -e "\n--- 测试: $test_name ---" >> $RESULT_FILE
  echo "命令: $curl_cmd" >> $RESULT_FILE
  
  # 替换命令中的{{TOKEN}}
  curl_cmd=${curl_cmd//\{\{TOKEN\}\}/$TOKEN}
  
  # 执行命令并记录结果
  echo "响应:" >> $RESULT_FILE
  eval $curl_cmd >> $RESULT_FILE 2>&1
  
  # 记录执行结果
  if [ $? -eq 0 ]; then
    echo "状态: 成功" >> $RESULT_FILE
  else
    echo "状态: 失败" >> $RESULT_FILE
  fi
}

# 从curl文件中读取测试命令
curl_file="src/main/resources/test/test_where_builder.curl"
test_count=0

echo "开始执行测试..."

while IFS= read -r line || [[ -n "$line" ]]; do
  # 跳过注释行和空行
  if [[ $line == \#* ]] || [[ -z "${line// }" ]]; then
    if [[ $line == \#* ]] && [[ $line != \#\#* ]]; then
      # 提取测试名称（去掉#号）
      test_name=${line#\# }
    fi
    continue
  fi
  
  # 处理多行curl命令
  if [[ $line == curl* ]]; then
    curl_cmd="$line"
    multiline=false
    
    # 如果命令以\结尾，表示是多行命令
    if [[ $line == *\\ ]]; then
      multiline=true
      curl_cmd=${curl_cmd%\\}
      while IFS= read -r next_line || [[ -n "$next_line" ]]; do
        if [[ -z "${next_line// }" ]]; then
          continue
        fi
        curl_cmd="$curl_cmd $next_line"
        if [[ $next_line != *\\ ]]; then
          break
        fi
        curl_cmd=${curl_cmd%\\}
      done
    fi
    
    # 执行测试
    test_count=$((test_count + 1))
    echo "执行测试 $test_count: $test_name"
    run_test "$test_name" "$curl_cmd"
  fi
done < "$curl_file"

# 记录测试结束
echo -e "\n============================" >> $RESULT_FILE
echo "测试完成，共执行 $test_count 个测试" >> $RESULT_FILE
echo "结果保存在: $RESULT_FILE" >> $RESULT_FILE

echo "测试完成，共执行 $test_count 个测试"
echo "结果保存在: $RESULT_FILE"

# 打印测试结果摘要
echo -e "\n测试结果摘要:"
grep -A 1 "状态:" $RESULT_FILE | grep -v "\-\-" 