---
name: preview-cycle
description: 启动或重启当前 workspace 的静态网页原型预览。Use when the user says 启动预览、重启预览、跑一下预览、open preview、restart preview。Do not use for Node vite/npm dev server 项目或 Android Gradle 项目。
---

# Preview Cycle

一键启停并重验当前 workspace 的静态网页原型预览。适用于零依赖 HTML + CSS + 原生 JS 项目（如本仓库的 `memento-prototype/`），用 PowerShell + .NET `HttpListener` 跑服务，**不依赖 Python / Node / npm**。

## 流程

1. **停旧进程**
   - 如果之前有后台跑的预览服务（command_id 在上下文里，比如上次调用返回的 shell_command_id），先 `StopCommand`。
   - 如果没有已知 command_id 但怀疑端口被占，先查端口：`netstat -ano | findstr ":PORT"`，再 `Stop-Process`。
   - 默认端口 **8000**；冲突时选 8001、8080、3000 依次尝试。

2. **确认入口目录**
   - 默认在 workspace 根目录找 `index.html`；如果有子目录叫 `prototype` / `preview` / `web` / `demo` / `public`，优先用那个。
   - 如果用户明确指定了入口文件，以用户为准。

3. **生成（或复用）`_preview.ps1`**
   - 路径固定：`{workspace}/_preview.ps1`。
   - 模板内容如下（每次写都用这份，不要内联 PowerShell 命令——转义坑太多）：

```powershell
param([int]$Port=8000)
$Root = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "memento-prototype"
Add-Type -AssemblyName System.Web
$L = New-Object System.Net.HttpListener
$L.Prefixes.Add("http://localhost:$Port/")
$L.Start()
Write-Host "Serving $Root at http://localhost:$Port/" -ForegroundColor Green
$M = @{'.html'='text/html; charset=utf-8';'.css'='text/css; charset=utf-8';'.js'='application/javascript; charset=utf-8';'.json'='application/json; charset=utf-8';'.svg'='image/svg+xml';'.png'='image/png';'.jpg'='image/jpeg';'.ico'='image/x-icon';'.txt'='text/plain; charset=utf-8'}
try {
  while ($L.IsListening) {
    $C = $L.GetContext()
    try {
      $u = [System.Web.HttpUtility]::UrlDecode($C.Request.Url.AbsolutePath.TrimStart('/'))
      if ([string]::IsNullOrWhiteSpace($u)) { $u = 'index.html' }
      $p = Join-Path $Root $u
      if (Test-Path $p -PathType Container) { $p = Join-Path $p 'index.html' }
      if (-not (Test-Path $p -PathType Leaf)) {
        $b = [System.Text.Encoding]::UTF8.GetBytes("Not Found: $u")
        $C.Response.StatusCode = 404; $C.Response.OutputStream.Write($b,0,$b.Length); $C.Response.OutputStream.Close(); continue
      }
      $e = [System.IO.Path]::GetExtension($p).ToLower()
      $t = if ($M.ContainsKey($e)) { $M[$e] } else { 'application/octet-stream' }
      $b = [System.IO.File]::ReadAllBytes($p)
      $C.Response.ContentType = $t; $C.Response.ContentLength64 = $b.Length
      $C.Response.OutputStream.Write($b,0,$b.Length); $C.Response.OutputStream.Close()
      Write-Host "200 $u ($($b.Length) B)" -ForegroundColor Cyan
    } catch { try { $C.Response.OutputStream.Close() } catch {} }
  }
} finally { $L.Stop() }
```

   - **改 `$Root`**：替换成入口目录相对路径（比如 `prototype` 或 `.`）。

4. **启动**
   - 用 `Shell`，**必须显式传 `cwd: workspace 根目录`**，`run_in_background: true`，`timeout: 5000`。
   - 命令：`powershell -NoProfile -ExecutionPolicy Bypass -File "_preview.ps1" -Port PORT`。
   - 记录返回的 `shell_command_id`，后面停止要用到。

5. **验证**
   - 启动后等 2 秒，用 `Invoke-WebRequest -Uri "http://localhost:PORT/" -UseBasicParsing -TimeoutSec 5` 验证。
   - 检查 `StatusCode == 200` 且 `Content.Length > 0`。
   - 验证失败时：查后台日志 `Read` 输出文件 → 看是端口冲突（换端口重跑）还是路径问题（改 `$Root`）。

6. **打开预览**
   - 用 `OpenPreview` 工具，传 `preview_url` 和后台 `command_id`。
   - 简短汇报：**地址 / 后台进程 id / 验证结果**。

## 注意事项

- `_preview.ps1` 是临时脚本，应该在仓库 `.gitignore` 里有 `_preview.ps1`；如果没有，提示用户加上。
- 沙箱可能阻止往 `.git/index.lock` 写入，但不影响读 `.git` 目录和运行脚本；真要改 git 才需要 `dangerouslyDisableSandbox: true`。
- PowerShell 内联字符串里 `$($var.Length)` 会被误判成外部命令，**绝对不要内联写服务器脚本**——写入文件再 `-File` 启动才稳。
- 如果用户指定了新的入口目录（不是 `memento-prototype`），记得改模板里的 `$Root`。
