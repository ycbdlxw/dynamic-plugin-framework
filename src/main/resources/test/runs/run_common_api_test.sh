#!/bin/bash
API_BASE="http://localhost:8081"
SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CURL_FILE="${SCRIPT_DIR}/test_common_api.curl"
RESULT_DIR="${SCRIPT_DIR}/test_results"
mkdir -p "${RESULT_DIR}"
echo "=== 执行测试用例: ${CURL_FILE} ==="
curl -s -X POST "${API_BASE}/api/test/execute?scriptPath=${CURL_FILE}&resultDir=${RESULT_DIR}&useCurrentDir=true"
echo "=== 用例执行完成，结果请查看: ${RESULT_DIR} ===" 