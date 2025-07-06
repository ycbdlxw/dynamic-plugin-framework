#!/bin/bash

# 设置基础变量
API_BASE="http://localhost:8081"
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RESULT_DIR="${SCRIPT_DIR}/test_results"
LOG_DIR="./logs"

# 创建结果目录
mkdir -p "${RESULT_DIR}"
mkdir -p "${LOG_DIR}"

# 日志文件
LOG_FILE="${LOG_DIR}/test_$(date +%Y%m%d_%H%M%S).log"

# 记录日志的函数
log() {
  echo "[$(date +"%Y-%m-%d %H:%M:%S")] $1" | tee -a "${LOG_FILE}"
}

# 运行测试的函数
run_test() {
  local test_file=$1
  local test_name=$(basename "${test_file}" .curl)
  
  log "===== 开始执行测试: ${test_name} ====="
  
  # 获取一个临时token用于测试
  TOKEN=$(curl -s -X POST "${API_BASE}/api/core/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"ycbd1234"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)
  
  if [ -z "$TOKEN" ]; then
    log "获取Token失败，使用空token继续测试"
    TOKEN=""
  else
    log "成功获取Token"
  fi
  
  # 替换测试文件中的TOKEN占位符
  TMP_TEST_FILE="${RESULT_DIR}/tmp_${test_name}.curl"
  cat "${test_file}" | sed "s/{{TOKEN}}/${TOKEN}/g" | sed "s/localhost:8080/localhost:8081/g" > "${TMP_TEST_FILE}"
  
  # 执行测试
  log "执行测试文件: ${TMP_TEST_FILE}"
  bash "${TMP_TEST_FILE}" > "${RESULT_DIR}/${test_name}_result.txt" 2>&1
  
  # 检查结果
  if [ $? -eq 0 ]; then
    log "测试 ${test_name} 执行完成"
  else
    log "测试 ${test_name} 执行出错"
  fi
  
  log "===== 测试 ${test_name} 结束 ====="
  echo ""
}

# 主函数
main() {
  log "开始执行所有测试"
  
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

  # 运行基本测试
  run_test "${SCRIPT_DIR}/test_basic.curl"
  
  # 运行命令执行器测试
  run_test "${SCRIPT_DIR}/test_command_executor.curl"
  
  # 运行通用API测试
  run_test "${SCRIPT_DIR}/test_common_api.curl"
  
  # 运行WHERE构建器测试
  run_test "${SCRIPT_DIR}/test_where_builder.curl"
  
  log "所有测试执行完成，结果保存在 ${RESULT_DIR}"
}

# 执行主函数
main 