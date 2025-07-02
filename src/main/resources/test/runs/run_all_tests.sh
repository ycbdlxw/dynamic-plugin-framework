#!/bin/bash

# 批量测试执行脚本
# 创建日期: 2023-07-01
# 创建人: 系统管理员

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 设置测试环境变量
export TEST_HOST="localhost:8080"
export TEST_OUTPUT_DIR="../test_results"
export TEST_TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
export TEST_ALL_RESULT_FILE="${TEST_OUTPUT_DIR}/${TEST_TIMESTAMP}_all_results.txt"

# 创建结果目录
mkdir -p ${TEST_OUTPUT_DIR}

# 打印测试信息
echo -e "${YELLOW}开始执行所有测试...${NC}"
echo -e "${YELLOW}测试时间: $(date)${NC}"
echo -e "${YELLOW}测试结果将保存到: ${TEST_ALL_RESULT_FILE}${NC}\n"

# 开始记录测试结果
echo "所有测试结果汇总" > ${TEST_ALL_RESULT_FILE}
echo "测试时间: $(date)" >> ${TEST_ALL_RESULT_FILE}
echo "----------------------------------------" >> ${TEST_ALL_RESULT_FILE}

# 获取当前目录下的所有测试脚本
TEST_SCRIPTS=$(ls run_*_test.sh | grep -v "run_all_tests.sh")

# 统计变量
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 执行每个测试脚本
for script in ${TEST_SCRIPTS}; do
    echo -e "\n${YELLOW}执行测试脚本: ${script}${NC}"
    echo "执行测试脚本: ${script}" >> ${TEST_ALL_RESULT_FILE}
    echo "----------------------------------------" >> ${TEST_ALL_RESULT_FILE}
    
    # 执行测试脚本并捕获输出
    TEST_OUTPUT=$(bash ${script})
    TEST_EXIT_CODE=$?
    
    # 将测试输出写入汇总结果文件
    echo "${TEST_OUTPUT}" >> ${TEST_ALL_RESULT_FILE}
    echo "----------------------------------------" >> ${TEST_ALL_RESULT_FILE}
    
    # 统计测试结果
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ ${TEST_EXIT_CODE} -eq 0 ]; then
        echo -e "${GREEN}测试脚本 ${script} 执行成功${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}测试脚本 ${script} 执行失败${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
done

# 测试总结
echo -e "\n${YELLOW}所有测试执行完成！${NC}"
echo -e "${YELLOW}总测试数: ${TOTAL_TESTS}${NC}"
echo -e "${GREEN}通过测试数: ${PASSED_TESTS}${NC}"
echo -e "${RED}失败测试数: ${FAILED_TESTS}${NC}"

# 将测试统计写入汇总结果文件
echo -e "\n测试统计" >> ${TEST_ALL_RESULT_FILE}
echo "总测试数: ${TOTAL_TESTS}" >> ${TEST_ALL_RESULT_FILE}
echo "通过测试数: ${PASSED_TESTS}" >> ${TEST_ALL_RESULT_FILE}
echo "失败测试数: ${FAILED_TESTS}" >> ${TEST_ALL_RESULT_FILE}

echo -e "${YELLOW}测试结果已保存到: ${TEST_ALL_RESULT_FILE}${NC}" 