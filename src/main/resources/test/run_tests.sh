#!/bin/bash

# 设置基础变量
API_BASE="http://localhost:8080"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULT_DIR="src/main/resources/test/test_results"
LOG_DIR="${SCRIPT_DIR}/logs"

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
  
  # 执行测试
  log "执行测试文件: ${test_file}"
  curl -s -X POST "${API_BASE}/api/test/execute?scriptPath=${test_file}&resultDir=${RESULT_DIR}&useCurrentDir=true"
  
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

  # 运行登录测试
  run_test "${SCRIPT_DIR}/test_login.curl"
  
  # 运行新用户测试
  run_test "${SCRIPT_DIR}/test_new_user.curl"
  
  # 运行用户查询测试
  run_test "${SCRIPT_DIR}/test_user_query.curl"
  
  # 运行数据预处理器测试
  run_test "${SCRIPT_DIR}/test_data_preprocessor.curl"
  
  # 运行过滤规则测试
  run_test "${SCRIPT_DIR}/test_filter_rule.curl"
  
  # 运行通用API测试
  run_test "${SCRIPT_DIR}/test_common_api.curl"
  
  log "所有测试执行完成，结果保存在 ${RESULT_DIR}"
}

# 执行主函数
main 