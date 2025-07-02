#!/bin/bash

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 设置测试环境变量
export TEST_HOST="localhost:8080"
export TEST_OUTPUT_DIR="test_results"
export TEST_TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
export TEST_RESULT_FILE="${TEST_OUTPUT_DIR}/${TEST_TIMESTAMP}_result.txt"

# 创建结果目录
mkdir -p ${TEST_OUTPUT_DIR}

# 打印测试信息
echo -e "${YELLOW}开始执行 CommonController API 测试${NC}"
echo -e "${YELLOW}测试时间: $(date)${NC}"
echo -e "${YELLOW}测试结果将保存到: ${TEST_RESULT_FILE}${NC}\n"

# 获取认证令牌
echo -e "${YELLOW}获取认证令牌...${NC}"
TOKEN_RESPONSE=$(curl -s -X POST "http://${TEST_HOST}/api/core/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"ycbd1234"}')

# 提取令牌
TOKEN=$(echo $TOKEN_RESPONSE | grep -o '"token":"[^"]*' | sed 's/"token":"//')

if [ -z "$TOKEN" ]; then
  echo -e "${RED}无法获取认证令牌，测试将继续但认证相关测试可能失败${NC}"
else
  echo -e "${GREEN}成功获取认证令牌${NC}"
fi

# 开始记录测试结果
echo "CommonController API 测试结果" > ${TEST_RESULT_FILE}
echo "测试时间: $(date)" >> ${TEST_RESULT_FILE}
echo "----------------------------------------" >> ${TEST_RESULT_FILE}

# 1. 健康检查测试
echo -e "\n${YELLOW}执行健康检查测试...${NC}"
HEALTH_RESULT=$(curl -s -X GET "http://${TEST_HOST}/api/common/health" \
  -H "Content-Type: application/json")

echo "1. 健康检查测试结果:" >> ${TEST_RESULT_FILE}
echo "${HEALTH_RESULT}" >> ${TEST_RESULT_FILE}
echo "----------------------------------------" >> ${TEST_RESULT_FILE}

if [[ $HEALTH_RESULT == *"Service is running"* ]]; then
  echo -e "${GREEN}健康检查测试通过${NC}"
else
  echo -e "${RED}健康检查测试失败${NC}"
fi

# 2. 获取列表测试
echo -e "\n${YELLOW}执行获取列表测试...${NC}"
LIST_RESULT=$(curl -s -X GET "http://${TEST_HOST}/api/common/list?targetTable=sys_user&pageIndex=0&pageSize=10" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}")

echo "2. 获取列表测试结果:" >> ${TEST_RESULT_FILE}
echo "${LIST_RESULT}" >> ${TEST_RESULT_FILE}
echo "----------------------------------------" >> ${TEST_RESULT_FILE}

if [[ $LIST_RESULT == *"\"code\":200"* ]]; then
  echo -e "${GREEN}获取列表测试通过${NC}"
else
  echo -e "${RED}获取列表测试失败${NC}"
fi

# 3. 保存数据测试
echo -e "\n${YELLOW}执行保存数据测试...${NC}"
SAVE_RESULT=$(curl -s -X POST "http://${TEST_HOST}/api/common/save?targetTable=sys_user" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "username": "testuser_'${TEST_TIMESTAMP}'",
    "password": "password123",
    "real_name": "Test User",
    "email": "test@example.com",
    "status": 1
  }')

echo "3. 保存数据测试结果:" >> ${TEST_RESULT_FILE}
echo "${SAVE_RESULT}" >> ${TEST_RESULT_FILE}
echo "----------------------------------------" >> ${TEST_RESULT_FILE}

if [[ $SAVE_RESULT == *"\"code\":200"* ]]; then
  echo -e "${GREEN}保存数据测试通过${NC}"
  # 提取新创建的用户ID
  NEW_USER_ID=$(echo $SAVE_RESULT | grep -o '"id":[0-9]*' | sed 's/"id"://')
  echo "新创建的用户ID: ${NEW_USER_ID}"
else
  echo -e "${RED}保存数据测试失败${NC}"
  NEW_USER_ID=0
fi

# 4. 更新数据测试
if [ $NEW_USER_ID -gt 0 ]; then
  echo -e "\n${YELLOW}执行更新数据测试...${NC}"
  UPDATE_RESULT=$(curl -s -X POST "http://${TEST_HOST}/api/common/save?targetTable=sys_user" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${TOKEN}" \
    -d '{
      "id": '${NEW_USER_ID}',
      "email": "updated@example.com",
      "status": 0
    }')

  echo "4. 更新数据测试结果:" >> ${TEST_RESULT_FILE}
  echo "${UPDATE_RESULT}" >> ${TEST_RESULT_FILE}
  echo "----------------------------------------" >> ${TEST_RESULT_FILE}

  if [[ $UPDATE_RESULT == *"\"code\":200"* ]]; then
    echo -e "${GREEN}更新数据测试通过${NC}"
  else
    echo -e "${RED}更新数据测试失败${NC}"
  fi

  # 5. 删除数据测试
  echo -e "\n${YELLOW}执行删除数据测试...${NC}"
  DELETE_RESULT=$(curl -s -X POST "http://${TEST_HOST}/api/common/delete?targetTable=sys_user&id=${NEW_USER_ID}" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${TOKEN}")

  echo "5. 删除数据测试结果:" >> ${TEST_RESULT_FILE}
  echo "${DELETE_RESULT}" >> ${TEST_RESULT_FILE}
  echo "----------------------------------------" >> ${TEST_RESULT_FILE}

  if [[ $DELETE_RESULT == *"\"code\":200"* ]]; then
    echo -e "${GREEN}删除数据测试通过${NC}"
  else
    echo -e "${RED}删除数据测试失败${NC}"
  fi
else
  echo -e "${RED}跳过更新和删除测试，因为未能创建新用户${NC}"
fi

# 6. 未授权测试
echo -e "\n${YELLOW}执行未授权测试...${NC}"
UNAUTH_RESULT=$(curl -s -X GET "http://${TEST_HOST}/api/common/list?targetTable=sys_user" \
  -H "Content-Type: application/json")

echo "6. 未授权测试结果:" >> ${TEST_RESULT_FILE}
echo "${UNAUTH_RESULT}" >> ${TEST_RESULT_FILE}
echo "----------------------------------------" >> ${TEST_RESULT_FILE}

if [[ $UNAUTH_RESULT == *"\"code\":401"* ]]; then
  echo -e "${GREEN}未授权测试通过${NC}"
else
  echo -e "${RED}未授权测试失败${NC}"
fi

# 测试总结
echo -e "\n${YELLOW}测试完成！${NC}"
echo -e "${YELLOW}测试结果已保存到: ${TEST_RESULT_FILE}${NC}"
echo -e "${YELLOW}请查看测试结果文件获取详细信息${NC}" 