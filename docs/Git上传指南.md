# Git 仓库上传指南

## ✅ 已完成的工作

1. ✅ 初始化 Git 仓库
2. ✅ 配置 .gitignore（排除不需要的文件）
3. ✅ 添加所有文件到暂存区
4. ✅ 创建第一次提交
5. ✅ 本地 Git 状态正常

## 📋 当前状态

```
提交: 29afddf Initial commit - Warehouse Monitoring System
文件: 57 个文件，11744 行代码
远程仓库: https://github.com/kanziduan-cpu/BS_project.git
```

## 🔧 推送到 GitHub（三种方法）

### 方法一：使用 GitHub CLI（推荐）

```bash
# 1. 安装 GitHub CLI（如果未安装）
# Windows: 下载安装 https://cli.github.com/

# 2. 登录 GitHub
gh auth login

# 3. 创建新仓库并推送
gh repo create BS_project --public --source=. --push
```

### 方法二：在 GitHub 网页创建仓库

1. **创建新仓库**
   - 访问 https://github.com/new
   - 仓库名称: `BS_project` 或 `warehouse-monitor-system`
   - 描述: 地下仓库环境监测调控系统
   - 选择 Public 或 Private
   - **不要**勾选 "Initialize this repository with a README"
   - 点击 "Create repository"

2. **推送代码**
   ```bash
   # 方法 A: 如果新仓库名相同
   git push -u origin main

   # 方法 B: 如果仓库名不同，先删除旧的远程地址
   git remote remove origin
   git remote add origin https://github.com/你的用户名/新仓库名.git
   git push -u origin main
   ```

### 方法三：使用 SSH（需要配置 SSH 密钥）

```bash
# 1. 生成 SSH 密钥（如果还没有）
ssh-keygen -t ed25519 -C "your_email@example.com"

# 2. 将公钥添加到 GitHub
# 复制 ~/.ssh/id_ed25519.pub 的内容
# 访问 https://github.com/settings/keys 添加 SSH key

# 3. 使用 SSH URL 推送
git remote set-url origin git@github.com:kanziduan-cpu/BS_project.git
git push -u origin main
```

## 🔐 如果遇到认证问题

### 使用 Personal Access Token

1. **生成 Token**
   - 访问 https://github.com/settings/tokens
   - 点击 "Generate new token (classic)"
   - 勾选权限: `repo` (全部)
   - 生成并复制 token

2. **使用 Token 推送**
   ```bash
   git push https://your_token@github.com/kanziduan-cpu/BS_project.git main
   ```

### 配置 Git 凭证助手（避免每次输入密码）

```bash
# Windows
git config --global credential.helper manager-core

# Linux/Mac
git config --global credential.helper store

# 然后再次推送，输入一次密码后会保存
git push -u origin main
```

## 📝 推送成功后的操作

### 1. 设置仓库信息

在 GitHub 仓库页面：
- 添加 README（如果没有）
- 设置仓库描述
- 添加 Topics 标签: `iot`, `mqtt`, `warehouse`, `monitoring`, `android`, `nodejs`

### 2. 配置 GitHub Pages（可选）

如果要展示项目文档：

```bash
# 创建 gh-pages 分支
git checkout --orphan gh-pages
git rm -rf .

# 创建索引文件
echo "# 地下仓库环境监测调控系统" > index.html
git add index.html
git commit -m "Add GitHub Pages"
git push origin gh-pages
```

### 3. 添加 Git 忽略规则（如果需要）

```bash
# 编辑 .gitignore
nano .gitignore

# 添加需要忽略的文件
# 然后提交
git add .gitignore
git commit -m "Update .gitignore"
git push
```

## 🔍 常见问题

### 问题 1: Repository not found

**原因**：仓库不存在或无权限

**解决**：
- 检查仓库名称是否正确
- 确认仓库是否已创建
- 检查是否有访问权限

### 问题 2: Authentication failed

**原因**：认证失败

**解决**：
- 使用 Personal Access Token
- 或配置 SSH 密钥
- 或使用 GitHub CLI

### 问题 3: ! [rejected] main -> main (fetch first)

**原因**：远程仓库已有内容

**解决**：
```bash
git pull origin main --allow-unrelated-histories
git push -u origin main
```

### 问题 4: remote: Permission denied

**原因**：没有推送权限

**解决**：
- 确认你是仓库所有者或有推送权限
- 检查 Git 配置的用户名和邮箱

## 🎯 推荐做法

### 每次提交前检查状态

```bash
# 查看状态
git status

# 查看差异
git diff

# 查看暂存的文件
git diff --cached
```

### 规范的提交信息

```bash
# feat: 新功能
git commit -m "feat: add dual storage support"

# fix: 修复 bug
git commit -m "fix: resolve MQTT connection issue"

# docs: 文档更新
git commit -m "docs: update deployment guide"

# style: 代码格式调整
git commit -m "style: format code with prettier"

# refactor: 重构
git commit -m "refactor: optimize storage manager"

# test: 测试相关
git commit -m "test: add unit tests for API"
```

### 创建 .gitignore 后添加新文件

```bash
# 如果之前已经添加了应该忽略的文件
git rm -r --cached node_modules/
git commit -m "chore: remove node_modules from git"
git push
```

## 📞 获取帮助

- GitHub 文档: https://docs.github.com
- Git 文档: https://git-scm.com/doc
- 或者直接问 AI 助手

## ✅ 完成检查清单

- [ ] Git 仓库已初始化
- [ ] .gitignore 已配置
- [ ] 第一次提交已创建
- [ ] 远程仓库已创建
- [ ] 认证已配置（Token/SSH/CLI）
- [ ] 代码已推送到 GitHub
- [ ] 仓库信息已完善（描述、Topics）
- [ ] （可选）GitHub Pages 已配置

---

**现在选择一个方法，把你的代码推送到 GitHub 吧！** 🚀
