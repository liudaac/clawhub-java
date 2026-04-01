# ClawHub Java版变更摘要

**变更日期**: 2026-04-02  
**原版参考**: commit 3080567 → 86259ee

---

## 快速概览

| 阶段 | 功能 | 状态 |
|------|------|------|
| Phase 1 | 安全扫描状态解析重构 | ✅ 完成 |
| Phase 2 | VT扫描增强（重试+降级） | ✅ 完成 |
| Phase 3 | 回扫优先级队列 | ✅ 完成 |
| Phase 4 | CLI GitHub源支持 | ✅ 完成 |
| Phase 5 | GitHub Actions工作流 | ✅ 完成 |

---

## 文件变更清单

### 新增文件 (5个)

```
backend/src/main/java/clawhub/service/PackageSecurityService.java
backend/src/test/java/clawhub/service/PackageSecurityServiceTest.java
backend/src/test/java/clawhub/service/VirusTotalServiceTest.java
backend/src/test/java/clawhub/service/ScheduledTaskServiceTest.java
cli/src/main/java/clawhub/service/GitHubSourceService.java
.github/workflows/package-publish.yml
.github/workflows/example-publish.yml
```

### 修改文件 (8个)

```
backend/src/main/java/clawhub/service/PackageSecurityScanService.java
backend/src/main/java/clawhub/service/VirusTotalService.java
backend/src/main/java/clawhub/service/ScheduledTaskService.java
backend/src/main/java/clawhub/controller/PackageController.java
backend/src/main/java/clawhub/service/PackageReleaseService.java
backend/src/main/java/clawhub/service/StorageService.java
backend/src/main/java/clawhub/repository/PackageReleaseRepository.java
cli/src/main/java/clawhub/commands/PackagesPublishCommand.java
```

---

## 关键功能

### 1. 安全扫描状态解析 (PackageSecurityService)

```java
// 综合裁决扫描状态
ScanStatus status = packageSecurityService.resolvePackageReleaseScanStatus(release);

// 检查是否阻止公开访问（仅阻止MALICIOUS）
boolean blocked = packageSecurityService.isPackageBlockedFromPublic(status);
```

### 2. VT扫描重试机制

```java
// 调度扫描（带30秒初始延迟）
virusTotalService.schedulePackageScan(releaseId, packageId);

// 最多3次重试，30秒间隔
```

### 3. 无恶意引擎降级

```java
// 当VT无恶意引擎命中时，根据其他扫描结果降级
Optional<VtAnalysis> analysis = virusTotalService.buildUndetectedFallbackAnalysis(
    release, pkg, stats
);
```

### 4. 回扫优先级

```java
// 优先扫描最近发布的，再处理积压
List<PackageRelease> batch = scheduledTaskService.getBackfillBatch(50, true);
```

### 5. CLI GitHub源

```bash
# 从GitHub发布
clawhub package publish owner/repo --family skill --version 1.0.0

# 预览模式
clawhub package publish ./my-package --dry-run --json
```

### 6. GitHub Actions

```yaml
jobs:
  publish:
    uses: ./.github/workflows/package-publish.yml
    with:
      source: 'owner/repo'
      dry_run: false
    secrets:
      clawhub_token: ${{ secrets.CLAWHUB_TOKEN }}
```

---

## 测试

```bash
# 运行所有测试
mvn test

# 预期结果: 30个测试通过
```

---

## 验证清单

- [ ] `mvn compile` 编译成功
- [ ] `mvn test` 所有测试通过
- [ ] 下载接口拦截MALICIOUS包
- [ ] PENDING状态的包可以下载
- [ ] CLI支持GitHub源格式
- [ ] GitHub Actions工作流可复用

---

## 详细文档

- [完整变更日志](CHANGELOG-2026-04-02.md)
- [迭代计划](../.openclaw/workspace/analysis/ITERATION_PLAN.md)
- [差距分析](../.openclaw/workspace/analysis/clawhub-update-analysis-2026-04-01.md)
