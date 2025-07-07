#!/bin/bash

# 设置测试文件目录
TEST_DIR="$(cd "$(dirname "$0")" && pwd)"

# 替换所有curl文件中的端口
for file in ${TEST_DIR}/*.curl; do
  echo "修改文件: $file"
  sed -i '' 's/localhost:8081/localhost:8080/g' "$file"
done

echo "所有测试文件端口已更新为8080" 