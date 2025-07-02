#!/bin/bash

# WHERE子句构建优化测试主脚本
# 作者：ycbd
# 日期：$(date +%Y-%m-%d)

# 设置变量
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEST_DIR="src/main/resources/test"
RESULT_DIR="$TEST_DIR/test_results"
SUMMARY_FILE="$RESULT_DIR/${TIMESTAMP}_test_summary.txt"

# 创建结果目录
mkdir -p $RESULT_DIR

# 确保脚本可执行
chmod +x $TEST_DIR/runs/test_where_builder.sh
chmod +x $TEST_DIR/runs/capture_sql_log.sh

# 记录测试开始
echo "=== WHERE子句构建优化测试总结 ===" > $SUMMARY_FILE
echo "测试时间: $(date)" >> $SUMMARY_FILE
echo "================================" >> $SUMMARY_FILE

# 检查应用是否运行
echo "检查应用是否运行..."
if ! curl -s http://localhost:8080/api/health > /dev/null; then
  echo "应用未运行，请先启动应用" | tee -a $SUMMARY_FILE
  exit 1
fi

echo "应用已运行，开始测试..." | tee -a $SUMMARY_FILE

# 启动SQL日志捕获（后台运行）
echo "启动SQL日志捕获..." | tee -a $SUMMARY_FILE
$TEST_DIR/runs/capture_sql_log.sh &
SQL_LOG_PID=$!

# 等待SQL日志捕获启动
sleep 2

# 运行测试
echo "运行WHERE子句构建测试..." | tee -a $SUMMARY_FILE
$TEST_DIR/runs/test_where_builder.sh | tee -a $SUMMARY_FILE

# 停止SQL日志捕获
echo "停止SQL日志捕获..." | tee -a $SUMMARY_FILE
kill $SQL_LOG_PID 2>/dev/null

# 等待日志完成写入
sleep 2

# 查找最新的测试结果文件
WHERE_RESULT=$(ls -t $RESULT_DIR/*where_builder_result.txt | head -1)
SQL_LOG=$(ls -t $RESULT_DIR/*sql_log.txt | head -1)

# 分析测试结果
echo -e "\n分析测试结果..." | tee -a $SUMMARY_FILE

# 检查测试成功率
TOTAL_TESTS=$(grep -c "状态:" $WHERE_RESULT)
SUCCESS_TESTS=$(grep -c "状态: 成功" $WHERE_RESULT)
FAILURE_TESTS=$(grep -c "状态: 失败" $WHERE_RESULT)

echo "测试总数: $TOTAL_TESTS" | tee -a $SUMMARY_FILE
echo "成功测试: $SUCCESS_TESTS" | tee -a $SUMMARY_FILE
echo "失败测试: $FAILURE_TESTS" | tee -a $SUMMARY_FILE

# 提取SQL语句进行分析
echo -e "\nSQL语句分析:" | tee -a $SUMMARY_FILE

# 提取WHERE子句
grep -A 3 "WHERE" $SQL_LOG 2>/dev/null | grep -v "\-\-" >> $SUMMARY_FILE

# 检查是否有错误
if grep -q "Exception\|Error" $WHERE_RESULT; then
  echo -e "\n发现错误:" | tee -a $SUMMARY_FILE
  grep -A 3 "Exception\|Error" $WHERE_RESULT | grep -v "\-\-" >> $SUMMARY_FILE
fi

# 总结
echo -e "\n测试总结:" | tee -a $SUMMARY_FILE
if [ $SUCCESS_TESTS -eq $TOTAL_TESTS ]; then
  echo "所有测试均成功通过，WHERE子句构建优化有效且未影响现有业务流程。" | tee -a $SUMMARY_FILE
else
  echo "有 $FAILURE_TESTS 个测试失败，请检查测试结果和SQL日志以确定问题。" | tee -a $SUMMARY_FILE
fi

echo -e "\n详细结果文件:" | tee -a $SUMMARY_FILE
echo "测试结果: $WHERE_RESULT" | tee -a $SUMMARY_FILE
echo "SQL日志: $SQL_LOG" | tee -a $SUMMARY_FILE
echo "总结报告: $SUMMARY_FILE" | tee -a $SUMMARY_FILE

echo "测试完成！" 