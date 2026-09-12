# 耕耘 · 工资计算

Android 工资计算工具，初版。

## 功能

1. **选择月份** — 左右切换年月
2. **白班 / 夜班配置** — 自定义时间段（支持跨天夜班）
3. **日历点选** — 每天可设：不上班 / 上班 / 加班；白班或夜班；是否节假日
4. **时薪配置** — 正班、工作日加班、周末加班、节假日加班
5. **增减项目** — 岗位津贴、效益奖、五险一金、水电费等（收入/支出）
6. **夜班津贴** — 按天配置（如每天 10 元）
7. 本月汇总：正班工时/工资、加班、夜班津贴、增减项、预计到手

数据仅保存在本机 DataStore，不上传。

## 构建

- JDK 17
- Android SDK 35
- 签名与 CI 参考 `wallpaper_app`：
  - Secrets：`RELEASE_KEYSTORE_BASE64`、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_ALIAS`、`RELEASE_KEY_PASSWORD`
  - 提交说明含「发布」或推送 `v*` tag 会触发正式 Release 构建

```bash
./gradlew assembleDebug
./gradlew assembleRelease   # 需 keystore.properties 或 CI Secrets
```

## 图标

简单线条：小人面前放着碗（要饭风格），绿色背景。

## 包名

`com.kers.gengyun`
