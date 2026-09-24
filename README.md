# iven-infra

IVEN-CN 个人 Spring Boot 基础设施框架，抽自 BlueNet 项目，可复用于任意 Spring Boot 3 + Java 21 项目。

## 模块

| 模块 | 内容 | 开关 |
|---|---|---|
| `iven-starter-core` | 启动横幅（IVEN-CN logo）、`BizException` 业务异常基类 | `iven.banner.enabled` |
| `iven-starter-web` | 统一响应体 `ResponseMessage`/`PageDTO`、全局异常处理、请求日志拦截器、枚举三通道序列化（`ValueEnum`） | `iven.web.enabled` |
| `iven-starter-security` | JWT 签发/认证（载荷可自定义）、CSRF、Cookie、限流、OAuth state、访问级别门槛 | `iven.security.enabled` |
| `iven-starter-rbac` | `@RequiresPermission` 权限注解、启动扫描注册、`Role` 角色抽象（支持自定义角色） | `iven.rbac.enabled`（默认关闭） |
| `iven-starter-storage` | 预签名直传对象存储抽象，MinIO / 阿里云 OSS 双实现、魔数校验 | `iven.storage.enabled` + `iven.storage.provider` |

## 依赖关系

```
core ◀── web ◀── security ◀── rbac
  ▲            ▲
  └────────────┴── storage（仅依赖 core + spring-boot-starter）
```

## 快速开始

应用需实现的 SPI：

| SPI | 模块 | 用途 |
|---|---|---|
| `UserDetailsLoader` | security | token 主体ID → `SecurityPrincipal` |
| `PermissionRegistry` | rbac | 权限定义持久化 |
| `SuperAdminPolicy` | rbac | 超级管理员短路（可缺省） |

```xml
<dependency>
    <groupId>io.github.iven-cn</groupId>
    <artifactId>iven-starter-web</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

配置示例见各 starter 的 `*Properties` 类（前缀 `iven.*`）。
