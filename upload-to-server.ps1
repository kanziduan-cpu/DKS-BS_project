# Cloud Server API 服务自动部署脚本
# 使用方法：右键 -> 使用 PowerShell 运行

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "云端 API 服务自动部署脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "服务器信息:" -ForegroundColor Yellow
Write-Host "  公网IP: 120.55.113.226" -ForegroundColor White
Write-Host "  API端口: 3000" -ForegroundColor White
Write-Host "  MQTT端口: 1884" -ForegroundColor White
Write-Host ""

# 上传项目
Write-Host "[1/5] 正在上传项目到服务器..." -ForegroundColor Yellow
Write-Host "源路径: c:\Users\TSBJ\Documents\BS_project\cloud-server" -ForegroundColor Gray
Write-Host "目标: root@120.55.113.226:/opt/warehouse-monitor/" -ForegroundColor Gray
Write-Host ""

try {
    scp -o StrictHostKeyChecking=no -r "c:\Users\TSBJ\Documents\BS_project\cloud-server" root@120.55.113.226:/opt/warehouse-monitor/

    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ 上传完成！" -ForegroundColor Green
        Write-Host ""

        # 检查 Node.js 环境
        Write-Host "[2/5] 检查 Node.js 环境..." -ForegroundColor Yellow
        $nodeVersion = ssh -o StrictHostKeyChecking=no root@120.55.113.226 "node --version 2>&1"

        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ Node.js 版本: $nodeVersion" -ForegroundColor Green
            Write-Host ""
        } else {
            Write-Host "✗ Node.js 未安装，请先安装 Node.js" -ForegroundColor Red
            Write-Host ""
            Write-Host "安装命令:" -ForegroundColor White
            Write-Host "  curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -" -ForegroundColor Gray
            Write-Host "  sudo apt install -y nodejs" -ForegroundColor Gray
            Write-Host ""
            $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
            exit 1
        }

        # 安装依赖
        Write-Host "[3/5] 正在安装 Node.js 依赖..." -ForegroundColor Yellow
        ssh -o StrictHostKeyChecking=no root@120.55.113.226 "cd /opt/warehouse-monitor/cloud-server && npm install --production"

        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ 依赖安装完成！" -ForegroundColor Green
            Write-Host ""

            # 安装 PM2
            Write-Host "[4/5] 正在安装 PM2 进程管理器..." -ForegroundColor Yellow
            ssh -o StrictHostKeyChecking=no root@120.55.113.226 "npm list -g pm2 || npm install -g pm2"

            if ($LASTEXITCODE -eq 0) {
                Write-Host "✓ PM2 已安装！" -ForegroundColor Green
                Write-Host ""

                # 检查是否已有配置文件
                Write-Host "[5/5] 检查配置文件..." -ForegroundColor Yellow
                $configExists = ssh -o StrictHostKeyChecking=no root@120.55.113.226 "test -f /opt/warehouse-monitor/cloud-server/.env && echo 'exists' || echo 'not_exists'"

                if ($configExists -eq "not_exists") {
                    ssh -o StrictHostKeyChecking=no root@120.55.113.226 "cd /opt/warehouse-monitor/cloud-server && cp .env.example .env 2>/dev/null || echo 'PORT=3000' > .env && echo 'NODE_ENV=production' >> .env && echo 'MQTT_HOST=localhost' >> .env && echo 'MQTT_PORT=1884' >> .env"
                    Write-Host "✓ 配置文件已创建" -ForegroundColor Green
                } else {
                    Write-Host "✓ 配置文件已存在" -ForegroundColor Green
                }
                Write-Host ""

                Write-Host "========================================" -ForegroundColor Cyan
                Write-Host "部署完成！" -ForegroundColor Green
                Write-Host "========================================" -ForegroundColor Cyan
                Write-Host ""
                Write-Host "接下来您需要手动执行以下操作：" -ForegroundColor Yellow
                Write-Host ""
                Write-Host "1. 登录服务器:" -ForegroundColor White
                Write-Host "   ssh root@120.55.113.226" -ForegroundColor Gray
                Write-Host ""
                Write-Host "2. 进入项目目录:" -ForegroundColor White
                Write-Host "   cd /opt/warehouse-monitor/cloud-server" -ForegroundColor Gray
                Write-Host ""
                Write-Host "3. 编辑配置文件（如果需要）:" -ForegroundColor White
                Write-Host "   nano .env" -ForegroundColor Gray
                Write-Host ""
                Write-Host "4. 启动 API 服务:" -ForegroundColor White
                Write-Host "   pm2 start server.js --name warehouse-api" -ForegroundColor Gray
                Write-Host ""
                Write-Host "5. 查看服务状态:" -ForegroundColor White
                Write-Host "   pm2 status" -ForegroundColor Gray
                Write-Host ""
                Write-Host "6. 查看日志:" -ForegroundColor White
                Write-Host "   pm2 logs warehouse-api" -ForegroundColor Gray
                Write-Host ""
                Write-Host "7. 设置开机自启:" -ForegroundColor White
                Write-Host "   pm2 startup" -ForegroundColor Gray
                Write-Host "   pm2 save" -ForegroundColor Gray
                Write-Host ""
                Write-Host "测试 API 服务:" -ForegroundColor Yellow
                Write-Host "   curl http://120.55.113.226:3000/api/" -ForegroundColor Gray
                Write-Host ""
                Write-Host "详细文档请查看: 云端服务器部署操作手册.md" -ForegroundColor Cyan
            } else {
                Write-Host "✗ PM2 安装失败" -ForegroundColor Red
            }
        } else {
            Write-Host "✗ 依赖安装失败" -ForegroundColor Red
        }
    } else {
        Write-Host "✗ 上传失败，请检查服务器连接" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 发生错误: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "按任意键退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
