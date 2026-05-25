# 社区生鲜电商 AI 智能体

这是一个面向社区生鲜电商平台的 AI 客服智能体项目，基于 `Spring Boot + Spring AI + DeepSeek` 构建。

本项目聚焦一个清晰的业务目标：

`自然语言 -> 知识检索 -> 工具调用 -> 真实电商业务流程执行`

它不是一个普通的聊天机器人演示项目。  
项目将大语言模型接入了真实业务能力，包括商品查询、订单查询、购物车操作、地址管理和订单提交等。

## 功能特性

- 使用 `Spring AI 1.1.6` 稳定版本
- `Tool Calling` 与真实业务模块打通
- 支持流式聊天响应
- 支持多轮会话记忆，并按用户隔离会话上下文
- 支持平台规则与客服知识的 RAG 检索增强
- 可选接入 Milvus 向量检索
- 对购物车、订单、地址等操作进行了权限校验
- 订单提交流程保证事务一致性

## 主要场景

当前智能体支持以下能力：

1. 商品查询
2. 热销商品推荐
3. 订单列表查询
4. 购物车查看 / 新增 / 修改 / 删除
5. 地址查看 / 新增 / 删除
6. 提交订单
7. 基于知识库回答订单状态、售后规则、平台政策等问题

## 技术栈

### 后端

- Java 17
- Spring Boot 3.5
- Spring AI 1.1.6
- MyBatis / MyBatis Plus
- MySQL
- Redis
- WebSocket
- SSE

### AI / Agent

- DeepSeek Chat Model
- Spring AI Tool Calling
- Chat Memory
- RAG Retrieval
- Prompt Engineering

### 存储 / 基础设施

- 阿里云 OSS
- 可选向量存储
- 可选 Milvus 集成

## 项目结构

### AI 入口

- `src/main/java/cn/yangeit/controller/customer/OpenAiController.java`

### 工具调用

- `src/main/java/cn/yangeit/config/ReservTools.java`

### RAG 服务

- `src/main/java/cn/yangeit/service/RagKnowledgeService.java`

### 安全配置

- `src/main/java/com/example/common/config/JwtInterceptor.java`
- `src/main/java/com/example/common/config/JwtWebConfig.java`

## Tool Calling 能力

当前智能体暴露了以下工具函数：

- `searchGoods`
- `getTop5Goods`
- `getOrderList`
- `getCartList`
- `getAddressList`
- `addToCart`
- `updateCartNum`
- `deleteCart`
- `addAddress`
- `deleteAddress`
- `submitOrder`

这些工具直接操作真实业务数据，而不是返回模拟文本。

## RAG

项目已经具备可运行的 RAG 检索增强能力。

### 当前支持的 RAG 模式

1. `keyword`
   轻量级兜底方案，不依赖外部向量数据库即可工作。

2. `simple`
   使用 Spring AI 的内存向量检索能力。

3. `milvus`
   在启用并完成配置后，使用 Milvus 作为向量存储。

### 知识库文件

- `src/main/resources/rag/customer-service-faq.txt`
- `src/main/resources/rag/terms-of-service.txt`

### RAG 配置项

关键环境变量：

- `APP_RAG_ENABLED`
- `APP_RAG_VECTOR_ENABLED`
- `APP_RAG_VECTOR_PROVIDER`
- `APP_RAG_TOP_K`
- `APP_RAG_KEYWORD_LIMIT`

### Milvus 配置

如果需要启用 Milvus 向量检索，请配置：

- `APP_RAG_VECTOR_PROVIDER=milvus`
- `APP_RAG_MILVUS_URI`
或
- `APP_RAG_MILVUS_HOST`
- `APP_RAG_MILVUS_PORT`

可选 Milvus 配置项：

- `APP_RAG_MILVUS_TOKEN`
- `APP_RAG_MILVUS_DATABASE_NAME`
- `APP_RAG_MILVUS_COLLECTION_NAME`
- `APP_RAG_MILVUS_INDEX_TYPE`
- `APP_RAG_MILVUS_METRIC_TYPE`
- `APP_RAG_MILVUS_INDEX_PARAMETERS`
- `APP_RAG_MILVUS_INITIALIZE_SCHEMA`
- `APP_RAG_MILVUS_SEARCH_PARAMS_JSON`
- `APP_RAG_EMBEDDING_DIMENSION`

## 安全性与工程化

当前版本已经包含以下强化措施：

- AI 接口要求登录后访问
- 购物车、地址、订单等操作绑定当前用户
- 敏感业务操作进行了权限校验
- 订单提交流程具备事务保障
- 密钥等敏感配置通过环境变量外置
- CORS 支持按需配置

## 本地运行

### 环境要求

- JDK 17
- Maven 3.9+
- MySQL
- Redis

### 编译

```powershell
$env:JAVA_HOME='D:\jdk'
$env:Path="D:\jdk\bin;$env:Path"
mvn -DskipTests compile
```

### 启动

```powershell
$env:JAVA_HOME='D:\jdk'
$env:Path="D:\jdk\bin;$env:Path"
mvn spring-boot:run
```

## 配置说明

主配置文件：

- `src/main/resources/application.yml`

建议配置的环境变量：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_DATABASE`
- `REDIS_PASSWORD`
- `APP_JWT_SECRET`
- `SPRING_AI_OPENAI_API_KEY`
- `SPRING_AI_OPENAI_BASE_URL`
- `SPRING_AI_OPENAI_CHAT_MODEL`
- `ALIYUN_OSS_ENDPOINT`
- `ALIYUN_OSS_ACCESS_KEY_ID`
- `ALIYUN_OSS_ACCESS_KEY_SECRET`
- `ALIYUN_OSS_BUCKET_NAME`

## 路线图

- [x] 升级 Spring AI 到稳定版本 1.1.6
- [x] 将 Tool Calling 与电商业务模块集成
- [x] 增加可运行的 RAG 能力
- [x] 增加可选的 Milvus 向量检索支持
- [x] 强化敏感操作的权限控制
