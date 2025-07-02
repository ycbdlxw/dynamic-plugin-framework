# TestService 插件使用速查

本插件提供 `/api/test/execute` 接口，可批量执行位于 `src/main/resources/test/` 目录下的 `.curl` 测试命令文件并生成结果报告。

## 1. 基本调用
```bash
curl -X POST 'http://localhost:8080/api/test/execute?scriptPath=src/main/resources/test/test_user_crud.curl'
```

## 2. 指定结果目录
```bash
curl -X POST 'http://localhost:8080/api/test/execute?scriptPath=src/main/resources/test/test_file_pipeline.curl&resultDir=/tmp/test_results'
```

## 3. 使用脚本目录作为基准路径（相对结果目录）
```bash
curl -X POST 'http://localhost:8080/api/test/execute?scriptPath=src/main/resources/test/test_ai_processor.curl&resultDir=test_results&useCurrentDir=true'
```

## 4. 可用测试脚本一览
- test_file_pipeline.curl
- test_ai_processor.curl
- test_file_upload_ai.curl
- test_command_executor.curl
- test_user_crud.curl
- test_operation_log.curl
- test_file_watcher.curl
- test_remote_connection.curl
- test_lxw_media.curl

所有脚本遵循《testrules.mdc》：仅包含注释 `#` 与单行 `curl` 指令，测试服务会自动提取 Token 并替换 `your-token-here` 占位符。 