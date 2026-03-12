# 推送代码到 GitHub - 立即执行

## 第一步：生成 GitHub Token

1. 访问：https://github.com/settings/tokens
2. 点击右上角 **"Generate new token"** → **"Generate new token (classic)"**
3. 填写信息：
   - Note: `DKS-BS_project push`
   - Expiration: 选择 90 days 或 No expiration
   - **勾选权限**：
     - ☑️ `repo` (勾选整个 repo 部分)
     - ☑️ `workflow` (如果需要)
4. 点击底部 **"Generate token"**
5. **复制生成的 token**（只显示一次！）

## 第二步：使用 Token 推送

打开终端/PowerShell，执行以下命令：

```bash
cd c:\Users\TSBJ\Documents\BS_project

# 将 YOUR_TOKEN_HERE 替换为你刚才复制的 token
git push https://YOUR_TOKEN_HERE@github.com/kanziduan-cpu/DKS-BS_project.git main
```

**示例**（假设 token 是 `ghp_xxxxxxxxxxxx`）：
```bash
git push https://ghp_xxxxxxxxxxxx@github.com/kanziduan-cpu/DKS-BS_project.git main
```

## 第三步：验证推送成功

```bash
# 查看远程分支
git branch -r

# 查看提交历史
git log --oneline

# 查看状态
git status
```

---

## 常见问题

### Q: 提示 "Permission denied"
A: 检查：
1. Token 是否正确复制
2. 是否勾选了 `repo` 权限
3. 仓库是否允许推送

### Q: 提示 "Authentication failed"
A: Token 可能过期或无效，重新生成一个。

### Q: 想保存 Token 避免每次输入
A: 配置凭证助手：
```bash
git config --global credential.helper store
```

---

## 推送成功后

1. 访问：https://github.com/kanziduan-cpu/DKS-BS_project
2. 查看你的代码
3. 完善仓库信息（描述、Topics）

---

**现在就按照上面的步骤操作吧！** 🚀
