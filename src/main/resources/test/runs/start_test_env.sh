#!/bin/bash

# 启动测试环境脚本
# 作者：ycbd
# 日期：$(date +%Y-%m-%d)

# 设置变量
PROJECT_ROOT=$(pwd)
TEST_CONFIG="$PROJECT_ROOT/src/main/resources/test/application-test.properties"
JAR_FILE="$PROJECT_ROOT/target/dynamic-plugin-framework-1.0.0.jar"
LOG_FILE="$PROJECT_ROOT/logs/app-test.log"

# 创建日志目录
mkdir -p $PROJECT_ROOT/logs

# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
  echo "JAR文件不存在，尝试构建项目..."
  mvn clean package -DskipTests
  
  if [ ! -f "$JAR_FILE" ]; then
    echo "构建失败，无法找到JAR文件: $JAR_FILE"
    exit 1
  fi
fi

# 停止已运行的实例
echo "检查是否有已运行的应用实例..."
PID=$(pgrep -f "java.*dynamic-plugin-framework")
if [ ! -z "$PID" ]; then
  echo "发现运行中的实例(PID: $PID)，正在停止..."
  kill $PID
  sleep 5
  
  # 检查是否成功停止
  if ps -p $PID > /dev/null; then
    echo "无法停止应用实例，请手动终止进程 $PID"
    exit 1
  fi
fi

# 启动应用
echo "启动测试环境..."
echo "使用配置: $TEST_CONFIG"
echo "日志将写入: $LOG_FILE"

java -jar $JAR_FILE --spring.config.location=file:$TEST_CONFIG > $LOG_FILE 2>&1 &

# 等待应用启动
echo "等待应用启动..."
for i in {1..30}; do
  if curl -s http://localhost:8080/api/health > /dev/null; then
    echo "应用已成功启动！"
    echo "测试环境就绪，可以运行测试脚本: ./src/main/resources/test/runs/run_where_builder_test.sh"
    exit 0
  fi
  echo -n "."
  sleep 1
done

echo "应用启动超时，请检查日志: $LOG_FILE"
exit 1 