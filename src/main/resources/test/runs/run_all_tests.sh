#!/bin/bash

# 设置基础变量
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="${SCRIPT_DIR}/logs"
RESULT_DIR="src/main/resources/test/test_results"
API_BASE="http://localhost:8081"

# 创建日志目录
mkdir -p "${LOG_DIR}"
mkdir -p "${RESULT_DIR}"

# 日志文件
LOG_FILE="${LOG_DIR}/all_tests_$(date +%Y%m%d_%H%M%S).log"

# 记录日志的函数
log() {
  echo "[$(date +"%Y-%m-%d %H:%M:%S")] $1" | tee -a "${LOG_FILE}"
}

# 执行测试的函数
run_test() {
  local test_name="$1"
  local script_path="$2"
  
  log "开始执行测试: ${test_name}"
  
  if [ ! -f "${script_path}" ]; then
    log "测试脚本不存在: ${script_path}"
    return 1
  fi
  
  # 执行测试脚本
  bash "${script_path}"
  local status=$?
  
  if [ $status -eq 0 ]; then
    log "测试完成: ${test_name} - 成功"
  else
    log "测试完成: ${test_name} - 失败 (退出码: ${status})"
  fi
  
  return $status
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
  
  # 定义所有测试
  declare -a tests=(
    "基础测试:${SCRIPT_DIR}/run_basic_test.sh"
    "登录测试:${SCRIPT_DIR}/run_login_test.sh"
    "通用API测试:${SCRIPT_DIR}/run_common_api_test.sh"
    "用户查询测试:${SCRIPT_DIR}/run_user_query_test.sh"
    "新用户测试:${SCRIPT_DIR}/run_new_user_test.sh"
    "WHERE构建器测试:${SCRIPT_DIR}/run_where_builder_test.sh"
    "数据预处理测试:${SCRIPT_DIR}/run_data_preprocessor_test.sh"
    "过滤规则测试:${SCRIPT_DIR}/run_filter_rule_test.sh"
    "命令执行器测试:${SCRIPT_DIR}/run_command_executor_test.sh"
    "插件管理测试:${SCRIPT_DIR}/run_plugin_management_test.sh"
  )
  
  # 执行所有测试
  local success_count=0
  local fail_count=0
  local total_count=${#tests[@]}
  
  for test in "${tests[@]}"; do
    IFS=":" read -r name script <<< "$test"
    run_test "$name" "$script"
    if [ $? -eq 0 ]; then
      ((success_count++))
    else
      ((fail_count++))
    fi
    log "------------------------------------------------"
  done
  
  # 输出测试结果摘要
  log "测试执行完成"
  log "总测试数: ${total_count}"
  log "成功测试数: ${success_count}"
  log "失败测试数: ${fail_count}"
  
  if [ $fail_count -gt 0 ]; then
    log "测试结果: 失败"
    exit 1
  else
    log "测试结果: 成功"
    exit 0
  fi
}

# 执行主函数
main 