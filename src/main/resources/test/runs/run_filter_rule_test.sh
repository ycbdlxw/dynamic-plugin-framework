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
LOG_FILE="${LOG_DIR}/filter_rule_test_$(date +%Y%m%d_%H%M%S).log"

# 记录日志的函数
log() {
  echo "[$(date +"%Y-%m-%d %H:%M:%S")] $1" | tee -a "${LOG_FILE}"
}

# 主函数
main() {
  log "开始执行过滤规则测试"
  
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

  # 执行过滤规则测试
  log "执行过滤规则测试"
  curl -s -X POST "${API_BASE}/api/test/execute?scriptPath=${SCRIPT_DIR}/test_filter_rule.curl&resultDir=${RESULT_DIR}&useCurrentDir=true"
  
  log "过滤规则测试执行完成，结果保存在 ${RESULT_DIR}"
}

# 执行主函数
main 