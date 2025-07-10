#!/bin/bash

# 设置基础变量
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="${SCRIPT_DIR}/logs"
RESULT_DIR="src/main/resources/test/test_results"
API_BASE="http://localhost:8080"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../../../.." && pwd)"
JAR_FILE="${PROJECT_ROOT}/target/dynamic-plugin-framework-1.0.0.jar"

# 创建日志目录
mkdir -p "${LOG_DIR}"
mkdir -p "${RESULT_DIR}"

# 日志文件
LOG_FILE="${LOG_DIR}/test_env_$(date +%Y%m%d_%H%M%S).log"

# 记录日志的函数
log() {
  echo "[$(date +"%Y-%m-%d %H:%M:%S")] $1" | tee -a "${LOG_FILE}"
}

# 检查Java是否安装
check_java() {
  if ! command -v java &> /dev/null; then
    log "错误: Java未安装，请安装JDK 17或更高版本"
    exit 1
  fi
  
  java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
  if [ "$java_version" -lt 17 ]; then
    log "错误: Java版本过低，需要JDK 17或更高版本，当前版本: $java_version"
    exit 1
  fi
  
  log "Java版本检查通过: $(java -version 2>&1 | head -1)"
}

# 检查JAR文件是否存在
check_jar() {
  if [ ! -f "$JAR_FILE" ]; then
    log "JAR文件不存在: $JAR_FILE"
    log "尝试构建项目..."
    
    cd "$PROJECT_ROOT" || exit 1
    mvn clean package -DskipTests
    
    if [ ! -f "$JAR_FILE" ]; then
      log "构建失败，无法找到JAR文件: $JAR_FILE"
      exit 1
    fi
    
    log "项目构建成功"
  else
    log "找到JAR文件: $JAR_FILE"
  fi
}

# 启动测试环境
start_test_env() {
  log "开始启动测试环境"
  
  # 检查是否已有实例在运行
  if netstat -tln | grep -q ':8081'; then
    log "警告: 端口8081已被占用，可能已有实例在运行"
    log "尝试关闭已有实例..."
    
    # 查找并关闭已有实例
    pid=$(lsof -t -i:8081)
    if [ -n "$pid" ]; then
      log "关闭进程ID: $pid"
      kill -15 $pid
      sleep 5
    fi
  fi
  
  # 启动应用
  log "启动应用..."
  cd "$PROJECT_ROOT" || exit 1
  nohup java -jar "$JAR_FILE" --spring.profiles.active=test > "${LOG_DIR}/application.log" 2>&1 &
  
  # 等待应用启动
  log "等待应用启动..."
  for i in {1..30}; do
    if curl -s -o /dev/null -w "%{http_code}" "${API_BASE}/api/common/health" | grep -q "200"; then
      log "应用启动成功"
      return 0
    fi
    log "等待应用启动... $i/30"
    sleep 2
  done
  
  log "错误: 应用启动超时"
  return 1
}

# 主函数
main() {
  log "开始初始化测试环境"
  
  # 检查依赖
  check_java
  check_jar
  
  # 启动测试环境
  start_test_env
  if [ $? -ne 0 ]; then
    log "测试环境启动失败"
    exit 1
  fi
  
  log "测试环境启动成功，可以开始执行测试"
  log "使用以下命令执行所有测试: sh run_all_tests.sh"
}

# 执行主函数
main 