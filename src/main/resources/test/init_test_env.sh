#!/bin/bash

# 设置基础变量
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="${SCRIPT_DIR}/logs"

# 创建日志目录
mkdir -p "${LOG_DIR}"

# 日志文件
LOG_FILE="${LOG_DIR}/init_$(date +%Y%m%d_%H%M%S).log"

# 记录日志的函数
log() {
  echo "[$(date +"%Y-%m-%d %H:%M:%S")] $1" | tee -a "${LOG_FILE}"
}

# 主函数
main() {
  log "开始初始化测试环境"
  
  # 检查应用是否运行
  log "检查应用是否运行"
  HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/common/health")
  if [ "$HEALTH_STATUS" == "200" ]; then
    log "应用已启动: $HEALTH_STATUS"
  else
    log "应用未启动，请先启动应用"
    exit 1
  fi

  # 登录获取管理员Token
  log "获取管理员Token"
  ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8080/api/core/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"ycbd1234"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

  if [ -z "$ADMIN_TOKEN" ]; then
    log "获取管理员Token失败，可能需要初始化角色数据"
    
    # 尝试执行SQL初始化脚本
    log "尝试通过API执行SQL初始化脚本"
    SQL_CONTENT=$(cat "${SCRIPT_DIR}/init_roles.sql")
    
    # 使用命令执行器插件执行SQL
    curl -s -X POST "http://localhost:8080/api/plugin/command-executor/execute-sql" \
      -H "Content-Type: application/json" \
      -d "{\"sql\":\"${SQL_CONTENT}\"}" > /dev/null
    
    log "SQL初始化完成，重新尝试登录"
    
    ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8080/api/core/login" \
      -H "Content-Type: application/json" \
      -d '{"username":"admin","password":"ycbd1234"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)
      
    if [ -z "$ADMIN_TOKEN" ]; then
      log "初始化后仍无法登录，请检查日志"
      exit 1
    fi
  fi

  log "管理员Token获取成功"
  log "测试环境初始化完成"
}

# 执行主函数
main 