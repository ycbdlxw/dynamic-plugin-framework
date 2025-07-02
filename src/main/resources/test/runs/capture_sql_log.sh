#!/bin/bash

# SQL日志捕获脚本
# 作者：ycbd
# 日期：$(date +%Y-%m-%d)

# 设置变量
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="src/main/resources/test/test_results/${TIMESTAMP}_sql_log.txt"
SPRING_LOG="./logs/sql-test.log"

# 创建结果目录
mkdir -p src/main/resources/test/test_results

# 记录开始
echo "=== SQL日志捕获 ===" > $LOG_FILE
echo "开始时间: $(date)" >> $LOG_FILE
echo "=================" >> $LOG_FILE

# 检查应用是否正在运行
if ! curl -s http://localhost:8080/api/health > /dev/null; then
  echo "应用未运行，无法捕获SQL日志" | tee -a $LOG_FILE
  exit 1
fi

echo "应用正在运行" | tee -a $LOG_FILE

# 清空现有日志
if [ -f "$SPRING_LOG" ]; then
  echo "清空现有SQL日志..." | tee -a $LOG_FILE
  cp /dev/null $SPRING_LOG
fi

# 启动日志捕获
echo "开始捕获SQL日志..." | tee -a $LOG_FILE
echo "日志将保存到: $LOG_FILE" | tee -a $LOG_FILE
echo "按Ctrl+C停止捕获" | tee -a $LOG_FILE

# 主循环 - 持续监控日志文件
trap "echo '停止捕获SQL日志'; exit 0" INT
tail -f $SPRING_LOG | grep --line-buffered "SQL\|Preparing\|Parameters" | while read -r line; do
  echo "$line" >> $LOG_FILE
done 