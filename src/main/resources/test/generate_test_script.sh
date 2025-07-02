#!/bin/bash

# 测试脚本生成工具
# 创建日期: 2023-07-01
# 创建人: 系统管理员

# 检查参数
if [ $# -lt 2 ]; then
    echo "用法: $0 <模块名> <功能点>"
    echo "例如: $0 common api"
    exit 1
fi

MODULE_NAME=$1
FEATURE_NAME=$2
CURRENT_DATE=$(date +"%Y-%m-%d")
CURRENT_USER=$(whoami)

# 创建目录
mkdir -p runs
mkdir -p test_results

# 生成curl测试文件
CURL_FILE="test_${MODULE_NAME}_${FEATURE_NAME}.curl"
echo "生成curl测试文件: ${CURL_FILE}"

cat > ${CURL_FILE} << EOF
# ${MODULE_NAME^} ${FEATURE_NAME^} 测试文件
# 使用方法: sh run_${MODULE_NAME}_${FEATURE_NAME}_test.sh
# 创建日期: ${CURRENT_DATE}
# 创建人: ${CURRENT_USER}

# 1. 测试用例示例
echo "执行测试用例示例..."
curl -X GET "http://\${TEST_HOST}/api/${MODULE_NAME}/endpoint" -H "Content-Type: application/json" -H "Authorization: Bearer \${TOKEN}" -w "\n\nStatus: %{http_code}\nTime: %{time_total}s\n\n"

# 2. 另一个测试用例示例
echo "执行另一个测试用例示例..."
curl -X POST "http://\${TEST_HOST}/api/${MODULE_NAME}/endpoint" -H "Content-Type: application/json" -H "Authorization: Bearer \${TOKEN}" -d '{"key":"value"}' -w "\n\nStatus: %{http_code}\nTime: %{time_total}s\n\n"
EOF

# 生成执行脚本
SH_FILE="runs/run_${MODULE_NAME}_${FEATURE_NAME}_test.sh"
echo "生成执行脚本: ${SH_FILE}"

cat > ${SH_FILE} << EOF
#!/bin/bash

# ${MODULE_NAME^} ${FEATURE_NAME^} 测试执行脚本
# 创建日期: ${CURRENT_DATE}
# 创建人: ${CURRENT_USER}

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 设置测试环境变量
export TEST_HOST="localhost:8080"
export TEST_OUTPUT_DIR="../test_results"
export TEST_TIMESTAMP=\$(date +"%Y%m%d_%H%M%S")
export TEST_RESULT_FILE="\${TEST_OUTPUT_DIR}/\${TEST_TIMESTAMP}_${MODULE_NAME}_${FEATURE_NAME}_result.txt"

# 创建结果目录
mkdir -p \${TEST_OUTPUT_DIR}

# 打印测试信息
echo -e "\${YELLOW}开始执行 ${MODULE_NAME^} ${FEATURE_NAME^} 测试\${NC}"
echo -e "\${YELLOW}测试时间: \$(date)\${NC}"
echo -e "\${YELLOW}测试结果将保存到: \${TEST_RESULT_FILE}\${NC}\n"

# 获取认证令牌
echo -e "\${YELLOW}获取认证令牌...\${NC}"
TOKEN_RESPONSE=\$(curl -s -X POST "http://\${TEST_HOST}/api/core/login" \\
  -H "Content-Type: application/json" \\
  -d '{"username":"admin","password":"password"}')

TOKEN=\$(echo \$TOKEN_RESPONSE | grep -o '"token":"[^"]*' | sed 's/"token":"//')

if [ -z "\$TOKEN" ]; then
  echo -e "\${RED}无法获取认证令牌，测试将继续但认证相关测试可能失败\${NC}"
else
  echo -e "\${GREEN}成功获取认证令牌\${NC}"
fi

# 开始记录测试结果
echo "${MODULE_NAME^} ${FEATURE_NAME^} 测试结果" > \${TEST_RESULT_FILE}
echo "测试时间: \$(date)" >> \${TEST_RESULT_FILE}
echo "----------------------------------------" >> \${TEST_RESULT_FILE}

# 执行测试用例
source ../test_${MODULE_NAME}_${FEATURE_NAME}.curl

# 测试总结
echo -e "\n\${YELLOW}测试完成！\${NC}"
echo -e "\${YELLOW}测试结果已保存到: \${TEST_RESULT_FILE}\${NC}"
EOF

# 设置执行权限
chmod +x ${SH_FILE}

echo "测试脚本生成完成！"
echo "curl测试文件: ${CURL_FILE}"
echo "执行脚本: ${SH_FILE}"
echo ""
echo "使用方法:"
echo "1. 编辑 ${CURL_FILE} 添加实际测试用例"
echo "2. 执行测试: cd runs && sh run_${MODULE_NAME}_${FEATURE_NAME}_test.sh" 