#!/bin/bash
# 文件名: runners/run_user_crud.sh
# 描述: 执行用户CRUD测试
# 作者: 系统开发组
# 日期: 2025-06-21
set -euo pipefail

API_BASE="http://localhost:8080"
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TEST_SCRIPT="${SCRIPT_DIR}/test_user_crud.curl"
RESULT_DIR="${SCRIPT_DIR}/test_results"

# 确保结果目录存在
mkdir -p "${RESULT_DIR}"

echo "=== 执行测试脚本: ${TEST_SCRIPT} ==="
curl -s -X POST "${API_BASE}/api/test/execute?scriptPath=${TEST_SCRIPT}&resultDir=${RESULT_DIR}&useCurrentDir=true" | jq .

echo "=== 测试完成，结果保存在: ${RESULT_DIR} ===" 