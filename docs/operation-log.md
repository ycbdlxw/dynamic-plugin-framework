## 测试脚本 `test_operation_log.sh`

```bash
#!/bin/bash

# 登录获取 JWT Token（响应 JSON 中包含 token 字段）
curl -X POST 'http://localhost:8080/api/core/login' \
     -H 'Content-Type: application/json' \
     -d '{"username":"admin","password":"ycbd1234"}'

# 调用任意受保护接口，触发 system_log 写入
curl -X GET 'http://localhost:8080/api/common/list?targetTable=sys_user' \
     -H 'Authorization: Bearer your-token-here'

# 查询 system_log 表，验证刚才的日志
curl -X GET 'http://localhost:8080/api/common/list?targetTable=system_log&method=GET' \
     -H 'Authorization: Bearer your-token-here'
```

> 将 `your-token-here` 替换为登录返回的 JWT 即可。