#!/bin/bash

# 字段映射测试脚本
# 作者：Auto
# 日期：$(date +%Y-%m-%d)

API_BASE="http://localhost:8080"
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RESULT_DIR="src/main/resources/test/test_results"
LOG_DIR="$(dirname "$0")/logs"

mkdir -p "${RESULT_DIR}" "${LOG_DIR}"
LOG_FILE="${LOG_DIR}/field_mapping_test_$(date +%Y%m%d_%H%M%S).log"

log(){ echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "${LOG_FILE}"; }

main(){
  log "开始执行字段映射测试"
  # 健康检查
  HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "${API_BASE}/api/common/health")
  if [ "$HEALTH" != "200" ]; then
     log "服务未启动，退出"
     exit 1
  fi
  log "健康检查通过"

  # 调用统一测试执行端点
  curl -s -X POST "${API_BASE}/api/test/execute?scriptPath=${SCRIPT_DIR}/test_field_mapping.curl&resultDir=${RESULT_DIR}&useCurrentDir=true"
  log "字段映射测试完成，结果保存在 ${RESULT_DIR}"
}

main 