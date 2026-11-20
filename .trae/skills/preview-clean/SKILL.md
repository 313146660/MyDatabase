---
name: preview-clean
description: 停止正在后台跑的静态预览服务并清理临时脚本。Use when the user says 停止预览、停预览、清理临时脚本、关预览。Do not use for 代码编译产物或 node_modules 清理。
---

# Preview Clean

两步：停后台服务 → 删临时脚本。**只做预览相关的清理，别动别的。**

## 流程

1. **停后台服务**
   - 从上下文里找到之前 `Shell` run_in_background=true 启动预览时返回的 `shell_command_id`。
   - 如果有多个，全部 `StopCommand`。
   - 如果没有 command_id 但 `_preview.ps1` 存在，先查 8000/8001/8080 端口：`netstat -ano | findstr ":PORT"`，拿 PID 后 `Stop-Process -Id PID -Force`。
   - 停完后等 1 秒，再查端口确认已释放。

2. **删临时脚本**
   - 检查 `{workspace}/_preview.ps1` 是否存在。
   - 用 `DeleteFile` 删除；如果 `.gitignore` 里没 `_preview.ps1`，提醒用户加上（但不要自动加——clean 不是改仓库规则的场合）。
   - 不要删 `web/` 或其他源目录，那些是真项目文件。

3. **简短汇报**
   - 报告：停了哪些 command_id / PID，删了哪些文件。
   - 如果什么都没找到，如实说"当前没有预览服务在跑，也没找到临时脚本"，别瞎编。

## 注意事项

- 不要停不是预览服务的进程。比如用户有 node/npm dev server 在跑，那不是这个 skill 的范畴。
- 不要清理 `.gradle/`、`build/`、`node_modules/`、`.git/` 等其他目录。
- 如果有用户之前说过 `_preview.ps1` 想保留当正式工具用，那就别删——先问一声。
