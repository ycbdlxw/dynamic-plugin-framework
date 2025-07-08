#!/bin/bash

# 测试脚本生成工具
# 使用方法: ./generate_test_script.sh <模块名> <功能点>
# 例如: ./generate_test_script.sh user login

# 检查参数
if [ $# -lt 2 ]; then
  echo "错误: 参数不足"
  echo "使用方法: ./generate_test_script.sh <模块名> <功能点>"
  echo "例如: ./generate_test_script.sh user login"
  exit 1
fi

# 设置变量
MODULE_NAME="$1"
FEATURE_NAME="$2"
CURRENT_DATE=$(date +%Y-%m-%d)
CURRENT_USER=$(whoami)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TEST_FILE_NAME="test_${MODULE_NAME}_${FEATURE_NAME}.curl"
TEST_FILE_PATH="${SCRIPT_DIR}/${TEST_FILE_NAME}"
RUN_SCRIPT_NAME="run_${MODULE_NAME}_${FEATURE_NAME}_test.sh"
RUN_SCRIPT_PATH="${SCRIPT_DIR}/runs/${RUN_SCRIPT_NAME}"

# 创建目录
mkdir -p "${SCRIPT_DIR}/runs/logs"
mkdir -p "${SCRIPT_DIR}/test_results"

# 检查文件是否已存在
if [ -f "$TEST_FILE_PATH" ]; then
  echo "警告: 测试文件已存在: $TEST_FILE_PATH"
  read -p "是否覆盖? (y/n): " OVERWRITE
  if [ "$OVERWRITE" != "y" ]; then
    echo "操作已取消"
    exit 0
  fi
fi

if [ -f "$RUN_SCRIPT_PATH" ]; then
  echo "警告: 执行脚本已存在: $RUN_SCRIPT_PATH"
  read -p "是否覆盖? (y/n): " OVERWRITE
  if [ "$OVERWRITE" != "y" ]; then
    echo "操作已取消"
    exit 0
  fi
fi

# 创建测试文件
cat > "$TEST_FILE_PATH" << EOF
# ${MODULE_NAME} ${FEATURE_NAME} 测试文件
# 使用方法: sh ${RUN_SCRIPT_NAME}
# 创建日期: ${CURRENT_DATE}
# 创建人: ${CURRENT_USER}

# 获取Token
echo "获取管理员Token..."
curl -s -X POST "http://localhost:8081/api/core/login" -H "Content-Type: application/json" -d '{"username":"admin","password":"ycbd1234"}' > token_response.txt
TOKEN=$(cat token_response.txt | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 检查Token
echo "检查Token是否获取成功..."
if [ -z "$TOKEN" ]; then
  echo "获取Token失败，请检查登录接口"
  exit 1
fi
echo "Token获取成功"

# 1. 测试示例
echo "测试1: 示例测试"
curl -X GET "http://localhost:8081/api/common/health" -H "Authorization: Bearer $TOKEN" -w "\n\nStatus: %{http_code}\nTime: %{time_total}s\n\n"

# 2. 测试示例2
echo "测试2: 示例测试2"
curl -X GET "http://localhost:8081/api/common/health" -H "Authorization: Bearer $TOKEN" -w "\n\nStatus: %{http_code}\nTime: %{time_total}s\n\n"

# 清理临时文件
rm -f token_response.txt
EOF

# 创建执行脚本
cat > "$RUN_SCRIPT_PATH" << EOF
#!/bin/bash

# 设置基础变量
API_BASE="http://localhost:8081"
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RESULT_DIR="${SCRIPT_DIR}/test_results"
LOG_DIR="./logs"
CURL_FILE="${SCRIPT_DIR}/${TEST_FILE_NAME}"

# 创建结果目录
mkdir -p "${RESULT_DIR}"
mkdir -p "${LOG_DIR}"

# 日志文件
LOG_FILE="${LOG_DIR}/${MODULE_NAME}_${FEATURE_NAME}_test_$(date +%Y%m%d_%H%M%S).log"

# 记录日志的函数
log() {
  echo "[$LOG_FILE]" "$1" | tee -a "$LOG_FILE"
}

# 主函数
main() {
  log "开始执行${MODULE_NAME} ${FEATURE_NAME}测试"
  
  # 健康检查
  log "执行健康检查"
  HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${API_BASE}/api/common/health")
  if [ "$HEALTH_STATUS" == "200" ]; then
    log "健康检查通过: $HEALTH_STATUS"
  else
    log "健康检查失败: $HEALTH_STATUS"
    log "服务可能未启动，退出测试"
    exit 1
  fi

  # 检查测试脚本是否存在
  if [ ! -f "$CURL_FILE" ]; then
    log "测试脚本不存在: $CURL_FILE"
    log "退出测试"
    exit 1
  fi

  # 执行${MODULE_NAME} ${FEATURE_NAME}测试
  log "执行${MODULE_NAME} ${FEATURE_NAME}测试"
  curl -s -X POST "${API_BASE}/api/test/execute?scriptPath=${CURL_FILE}&resultDir=${RESULT_DIR}&useCurrentDir=true"
  
  log "${MODULE_NAME} ${FEATURE_NAME}测试执行完成，结果保存在 ${RESULT_DIR}"
}

# 执行主函数
main
EOF

# 设置执行权限
chmod +x "$TEST_FILE_PATH"
chmod +x "$RUN_SCRIPT_PATH"

echo "测试脚本创建成功:"
echo "- 测试文件: $TEST_FILE_PATH"
echo "- 执行脚本: $RUN_SCRIPT_PATH"
echo ""
echo "请编辑测试文件，添加实际的测试用例。"
echo "执行测试: sh $RUN_SCRIPT_PATH" 