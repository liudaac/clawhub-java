# ClawHub Java版变更日志

## 版本：原版同步迭代 (2026-04-02)

基于原版 ClawHub (commit 3080567 → 86259ee) 的更新，同步关键改进到 Java 版。

---

## 概述

本次迭代将原版 ClawHub 的关键安全扫描和 CLI 改进同步到 Java 版，包括：

1. **安全扫描状态解析重构** - 集中化状态裁决逻辑
2. **VT扫描增强** - 重试机制和无恶意引擎降级处理
3. **回扫优先级队列** - 优先扫描最近发布的包
4. **CLI GitHub源支持** - 支持从GitHub直接发布
5. **GitHub Actions工作流** - 可复用的包发布工作流

---

## 详细变更

### 🔐 Phase 1: 安全扫描核心逻辑对齐

#### 新增文件

**`backend/src/main/java/clawhub/service/PackageSecurityService.java`**
- 集中化安全扫描状态解析服务
- 关键方法：
  - `resolvePackageReleaseScanStatus()` - 综合裁决静态扫描、VT扫描、验证状态
  - `isPackageBlockedFromPublic()` - 仅阻止 MALICIOUS（关键变更：不再阻止 PENDING）
  - `getPackageDownloadSecurityBlock()` - 下载安全拦截
  - `syncLatestPackageVerification()` - 验证状态同步

**`backend/src/test/java/clawhub/service/PackageSecurityServiceTest.java`**
- 9个单元测试用例
- 覆盖状态解析优先级、下载拦截、验证同步

#### 修改文件

**`backend/src/main/java/clawhub/service/PackageSecurityScanService.java`**
- 注入 `PackageSecurityService`
- 使用新的状态解析逻辑
- 更新裁决流程

**`backend/src/main/java/clawhub/controller/PackageController.java`**
- 添加 `/api/v1/packages/{name}/versions/{version}/download` 端点
- 集成下载安全检查

**`backend/src/main/java/clawhub/service/PackageReleaseService.java`**
- 添加 `findReleaseEntity()` 方法

**`backend/src/main/java/clawhub/service/StorageService.java`**
- 添加 `getPackageDownloadUrl()` 方法

#### 关键变更

| 变更项 | 之前 | 之后 |
|--------|------|------|
| 公开访问阻止 | 阻止 MALICIOUS, PENDING | 仅阻止 MALICIOUS |
| 状态解析 | 分散在各服务 | 集中到 PackageSecurityService |
| 下载检查 | 无统一拦截 | 统一安全拦截 |

---

### 🔍 Phase 2: VT扫描增强

#### 修改文件

**`backend/src/main/java/clawhub/service/VirusTotalService.java`**
- 新增常量：
  - `PACKAGE_SCAN_MAX_ATTEMPTS = 3` - 最大重试次数
  - `PACKAGE_SCAN_RETRY_DELAY_MS = 30000` - 重试延迟30秒
  - `INITIAL_PACKAGE_VT_SCAN_DELAY_MS = 30000` - 初始延迟30秒
- 新增方法：
  - `schedulePackageScan()` - 带初始延迟的扫描调度
  - `performPackageScan()` - 支持重试的扫描执行
  - `buildUndetectedFallbackAnalysis()` - 无恶意引擎降级处理
  - `buildAnalysisFromVtResult()` - 从VT响应构建分析
  - `VtAnalysisStats` - VT统计记录类

**`backend/src/main/java/clawhub/service/PackageSecurityScanService.java`**
- 更新VT扫描调用，使用新的异步调度方法

#### 新增文件

**`backend/src/test/java/clawhub/service/VirusTotalServiceTest.java`**
- 12个单元测试用例
- 覆盖降级处理、扫描调度、VT响应解析

#### 关键变更

| 变更项 | 之前 | 之后 |
|--------|------|------|
| 扫描执行 | 立即执行 | 延迟30秒后执行 |
| 文件缺失 | 直接失败 | 自动重试（最多3次） |
| 无恶意引擎 | 保持PENDING | 降级为CLEAN（条件满足时） |

#### 降级处理条件

无恶意引擎降级为CLEAN需要满足：
1. 包类型非 SKILL
2. 验证层级为 source-linked/provenance-verified/rebuild-verified
3. LLM扫描结果为 clean
4. 静态扫描结果非 malicious
5. VT统计：malicious=0, suspicious=0
6. 至少有一个引擎响应（harmless>0 或 undetected>0）

---

### 🔄 Phase 3: 回扫优先级队列

#### 修改文件

**`backend/src/main/java/clawhub/repository/PackageReleaseRepository.java`**
- 新增查询方法：
  - `findByScanStatusPendingOrderByCreatedAtAsc()` - 按PENDING状态查询
  - `findTopNByOrderByCreatedAtDesc()` - 查询最近发布的
  - `findBackfillBatchOrderByCreatedAtDesc()` - 回扫批次查询

**`backend/src/main/java/clawhub/service/ScheduledTaskService.java`**
- 注入 `PackageReleaseRepository` 和 `PackageSecurityScanService`
- 新增任务：
  - `processPackageScanBackfill()` - 每30秒执行的回扫任务
  - `getBackfillBatch()` - 获取回扫批次（支持优先模式）
  - `processPackageScanBackfillInternal()` - 处理回扫逻辑

#### 新增文件

**`backend/src/test/java/clawhub/service/ScheduledTaskServiceTest.java`**
- 9个单元测试用例
- 覆盖优先扫描、去重逻辑、批次限制、跳过逻辑

#### 关键变更

| 变更项 | 之前 | 之后 |
|--------|------|------|
| 回扫顺序 | 按创建时间升序 | 优先最近发布的，再处理积压 |
| 批次大小 | 无限制 | 限制1-200 |
| 执行频率 | 每分钟 | 每30秒 |

#### 回扫逻辑

```
获取回扫批次(prioritizeRecent=true):
  1. 获取最近发布的包 (batchSize * 2)
  2. 获取积压的包 (batchSize * 3)
  3. 合并并去重
  4. 限制为 batchSize
```

---

### 💻 Phase 4: CLI GitHub源支持

#### 新增文件

**`cli/src/main/java/clawhub/service/GitHubSourceService.java`**
- 支持从GitHub获取源码
- 关键方法：
  - `resolveSourceInput()` - 解析多种GitHub源格式
  - `fetchGitHubSource()` - 下载并解压GitHub源码
  - `normalizeGitHubRepo()` - 标准化仓库地址
  - `resolveLocalGitInfo()` - 解析本地git信息
- 支持格式：
  - `owner/repo`
  - `owner/repo@ref`
  - `https://github.com/owner/repo`
  - `https://github.com/owner/repo/tree/main/subdir`

#### 修改文件

**`cli/src/main/java/clawhub/commands/PackagesPublishCommand.java`**
- 支持GitHub源输入（自动检测）
- 新增选项：
  - `--dry-run` - 预览发布内容
  - `--json` - JSON格式输出
  - `--owner` - 指定所有者
- 自动清理临时目录

#### 关键变更

| 变更项 | 之前 | 之后 |
|--------|------|------|
| 源支持 | 仅本地路径 | 本地路径 + GitHub源 |
| 预览模式 | 无 | --dry-run |
| 输出格式 | 文本 | 文本 + JSON |

#### 使用示例

```bash
# 从本地路径发布
clawhub package publish ./my-package --family skill --version 1.0.0

# 从GitHub发布
clawhub package publish owner/repo --family skill --version 1.0.0

# 指定分支
clawhub package publish owner/repo@main --family skill --version 1.0.0

# 预览模式
clawhub package publish ./my-package --dry-run --json
```

---

### 🔧 Phase 5: GitHub Actions工作流

#### 新增文件

**`.github/workflows/package-publish.yml`**
- 可复用的包发布工作流
- 输入参数：
  - `source` - 包源（owner/repo、GitHub URL或本地路径）
  - `ref` - Git引用
  - `dry_run` - 预览模式（默认true）
  - `json` - JSON输出（默认true）
  - `registry` - ClawHub注册表URL
  - `owner` - 所有者handle
  - `version` - 版本覆盖
  - `tags` - 标签（默认latest）
- 输出：
  - `publish_json` - 发布JSON输出
  - `release_id` - 发布版本ID
- 特性：
  - 输入验证（dry_run=false时需要clawhub_token）
  - Java 21环境
  - Maven缓存
  - 15分钟超时
  - Job Summary

**`.github/workflows/example-publish.yml`**
- 示例工作流，展示4种使用场景：
  - dry-run模式
  - 正式发布
  - 从其他仓库发布
  - 手动触发

#### 使用示例

```yaml
# 从当前仓库发布（dry-run）
jobs:
  publish:
    uses: ./.github/workflows/package-publish.yml
    with:
      dry_run: true
      json: true
    secrets:
      clawhub_token: ${{ secrets.CLAWHUB_TOKEN }}

# 从其他仓库发布
jobs:
  publish:
    uses: ./.github/workflows/package-publish.yml
    with:
      source: 'openclaw/openclaw'
      ref: 'main'
      dry_run: false
      owner: 'openclaw'
    secrets:
      clawhub_token: ${{ secrets.CLAWHUB_TOKEN }}
```

---

## 测试覆盖

### 单元测试

| 测试类 | 测试数 | 覆盖率 |
|--------|--------|--------|
| PackageSecurityServiceTest | 9 | 状态解析、下载拦截、验证同步 |
| VirusTotalServiceTest | 12 | 降级处理、扫描调度、VT响应 |
| ScheduledTaskServiceTest | 9 | 优先扫描、去重、批次限制 |
| **总计** | **30** | - |

### 测试执行

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=PackageSecurityServiceTest
mvn test -Dtest=VirusTotalServiceTest
mvn test -Dtest=ScheduledTaskServiceTest
```

---

## 迁移指南

### 数据库迁移

无需数据库迁移，所有变更向后兼容。

### 配置更新

**新增配置项（backend/application.yml）:**

```yaml
# VirusTotal配置（已存在）
virustotal:
  api:
    key: ${VIRUSTOTAL_API_KEY:}
    url: https://www.virustotal.com/api/v3
  enabled: true

# 扫描调度配置（新增）
scan:
  backfill:
    enabled: true
    interval: 30000  # 30秒
    batch-size: 50
    prioritize-recent: true
```

### API变更

**新增端点:**

```
GET /api/v1/packages/{name}/versions/{version}/download
```

**响应:**
- 200 OK - 下载授权，返回下载URL
- 403 Forbidden - 包被标记为MALICIOUS

---

## 已知问题

暂无已知问题。

---

## 后续计划

1. **性能优化** - 添加扫描结果缓存
2. **监控告警** - 扫描失败率监控
3. **批量操作** - 支持批量扫描
4. **审计日志** - 记录扫描操作

---

## 相关提交

- 原版参考: 3080567 → 86259ee
- 关键提交:
  - fc4f864 - refactor: centralize package scan state resolution
  - 2186c41 - fix: prioritize recent package vt backfills
  - a8a687e - [codex] streamline plugin publish flow

---

## 贡献者

- 变更实施: OpenClaw Agent
- 代码审查: 待进行
- 测试验证: 待进行

---

*变更日期: 2026-04-02*
*版本: 1.0.0-sync*