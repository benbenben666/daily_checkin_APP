# daily_checkin_APP

公司内部**任务发布与执行平台**。管理层发布任务，成员执行并提交成果，全过程留痕可查。

> ⚠️ 仓库名与 Java 包名（`com.ben.daily_check_in`）是早期遗留的"签到"语义，**本项目实际是任务管理系统**，与签到无关。

## 📖 先看这个

**完整设计文档：[`docs/开发文档.md`](docs/开发文档.md)**

里面有角色权限矩阵、全部业务规则、9 张表的结构与设计理由、核心流程 SQL、接口草案。

## ⚠️ 维护约定

**每一次改动 —— 功能新增、字段变更、流程调整、权限规则修改、技术栈更换 —— 都必须同步更新 [`docs/开发文档.md`](docs/开发文档.md)。**

两条硬规则：

1. **`backend/src/main/resources/db/schema.sql` 是表结构的唯一权威版本**
2. **表结构变更必须同时改两处** —— `schema.sql` 和开发文档 5.3，缺一不可

## 目录结构

```
daily_checkin_APP/
├── backend/          Spring Boot 4.1.1 + MyBatis + MySQL
│   └── src/main/resources/db/schema.sql    ← 建表脚本
├── frontend/         经典 uni-app（Vue3 + Vite）
│   └── frontend/     ← 前端工程根（src/ 下为源码）
└── docs/开发文档.md   ← 完整设计文档
```

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Spring Boot **4.1.1** · Java **21** · MyBatis · MySQL **8.0** · JWT（jjwt）· Lombok |
| 前端 | **经典 uni-app（Vue3 + JS）** · Vite · `npm run dev:h5` 跑浏览器 |
| 存储 | 阿里云 OSS（任务配图、完成提交图；直传签名已实现，OSS 配置为占位） |

## 快速开始

### 1. 建库建表

```powershell
mysql -u root -p < backend/src/main/resources/db/schema.sql
```

脚本会创建 `daily_check_in` 库和 9 张表（**开头有 DROP，会清空同名表**）。

### 2. 后端

用 IDEA 打开**仓库根目录**即可（根 `.idea/misc.xml` 已正确指向 `backend/pom.xml`，JDK 21、本地仓库 `D:\repo`、`D:\conf\settings.xml` 都已识别），然后：

```powershell
cd backend
.\mvnw spring-boot:run
```

服务启动在 `http://localhost:8080`。

数据库口令与 JWT 密钥已改成**环境变量写法**，默认值可直接跑（`DB_PASSWORD` 默认 `123456`）。部署时用环境变量覆盖，**不要把真实口令提交进仓库**：

```powershell
$env:DB_PASSWORD = "真实口令"
$env:JWT_SECRET  = "至少32位的随机字符串"
```

### 3. 前端

```powershell
cd frontend/frontend
npm install
npm run dev:h5
```

浏览器打开 `http://localhost:5173`。接口基地址在 `src/api/request.js` 的 `BASE_URL`（默认 `http://localhost:8080`，后端已开 CORS）。

## 预设管理员

管理员**无法通过注册获得**，只能在数据库里手工提升：

```sql
UPDATE `user` SET `system_role` = 'ADMIN' WHERE `phone` = '你的手机号';
```

## 两台电脑轮换开发的注意点

```powershell
git pull      # 开工前
git add -A && git commit -m "..." && git push    # 收工后
```

- 远程用 SSH，两台电脑各有一把密钥，互不复制
- `.idea/`、`target/`、`node_modules/` 都是本地的，不进 git
- ⚠️ **用 IDEA 的提交对话框时，一定要展开并勾选 `Unversioned Files`（未版本化文件）** —— 否则新建的文件不会进提交。本项目就踩过一次：`entity/`、`mapper/`、`controller/` 三个目录共 24 个文件漏提交，另一台电脑拉下来直接编译不过。提交前用 `git status` 复核一遍最稳。
