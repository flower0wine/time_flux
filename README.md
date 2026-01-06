# 🔹 Time_Flux

**Time_Flux** is a modern automation toolkit built to help individuals and teams streamline repetitive tasks, optimize workflows, and reclaim valuable time. Whether you're scheduling jobs, orchestrating processes, or syncing systems, Time_Flux simplifies automation with clarity and power.

---

## 🚀 Key Features

- **广告跳过**: 基于无障碍服务的智能广告跳过功能，自动识别并点击"跳过"按钮
- **Task Automation:** Easily define and run automated tasks with minimal configuration.
- **Workflow Orchestration:** Link multiple jobs into efficient, repeatable flows.
- **Modular & Extensible:** Add plugins or custom integrations as your needs grow.
- **Smart Scheduling:** Flexible timing options including intervals, triggers, and queues.

---

## 📱 广告跳过功能

Time Flux 现已支持自动跳过应用开屏广告！

### 特性

- ✅ 智能识别常见广告按钮（跳过、关闭、×等）
- ✅ 支持自定义规则配置
- ✅ 记录跳过历史
- ✅ 无需 Root 权限
- ✅ 隐私安全，数据本地存储

### 快速开始

1. 打开 Time Flux 应用
2. 切换到"广告跳过"标签页
3. 点击"打开无障碍设置"启用服务
4. 完成！应用会自动跳过广告

详细使用指南请查看 [AD_SKIP_GUIDE.md](./AD_SKIP_GUIDE.md)

---

## 🧠 Why Time_Flux?

In today's fast-paced environment, repetitive manual work drains productivity. Time_Flux lets you focus on what truly matters — by managing the minutiae for you.

---

## 🛠️ 技术栈

- **Kotlin Multiplatform**: 跨平台共享代码
- **Jetpack Compose**: 现代化 Android UI
- **AccessibilityService**: 无障碍服务实现自动化
- **Coroutines**: 异步处理

---

## 📦 项目结构

```
Time_Flux/
├── androidApp/          # Android 应用
│   └── src/
│       └── main/
│           ├── java/    # Kotlin 源代码
│           └── res/     # 资源文件
├── shared/              # 共享模块
│   └── src/
│       ├── commonMain/  # 跨平台代码
│       └── androidMain/ # Android 特定实现
├── iosApp/              # iOS 应用
└── AD_SKIP_GUIDE.md     # 广告跳过功能详细指南
```

---

## 🔧 构建与运行

### 前置要求

- JDK 17 或更高版本
- Android Studio Hedgehog 或更高版本
- Android SDK 33 或更高版本

### 构建

```bash
# 构建 Android 应用
./gradlew :androidApp:assembleDebug

# 运行 Lint 检查
./gradlew lint

# 运行测试
./gradlew test
```

### 安装

```bash
# 安装到连接的设备
./gradlew :androidApp:installDebug
```

---

## 📄 许可证

本项目采用开源许可证。

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！
