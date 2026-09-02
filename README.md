#### 1.聊天功能

创建 Spring Boot 4.0 项目，pom.xml 配置文件如下：

```xml

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.0</version>
        <relativePath/>
    </parent>

    <properties>
        <java.version>25</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>

        <!-- @NotBlank 属于 Bean Validation 规范，
        Spring Boot 4.x 中需要显式引入 spring-boot-starter-validation
        （它不再被 spring-boot-starter-web 自动传递 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

```

##### 1.1.配置阿里云服务

1.在阿里百炼平台获取个人密钥：https://bailian.console.aliyun.com

2.导入 pom.xml 依赖

```xml
    <!--Spring AI BOM只是声明了Spring AI的给定版本所使用的所有依赖项的推荐版本。
    这仅限BOM的版本，它只包含依赖性管理，没有插件声明或直接引用Spring或Spring Boot。
    可以使用Spring Boot父POM，或使用Spring Boot的BOM（spring-boot-dependencies）来管理Spring Boot版本。
    也就是说：Spring AI BOM 只管 Spring AI 自己的版本，不管 Spring Boot 的版本。两者必须配合使用，不能互相替代 -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>2.0.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

		<dependencies>
      	<!--导入 openai 依赖-->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>
		</dependencies>
```

3.在项目properties配置文件中配置个人密钥和请求服务地址

```properties
spring.ai.openai.api-key=
spring.ai.openai.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
spring.ai.openai.chat.model=qwen3.7-plus-2026-05-26
spring.ai.openai.chat.temperature=0.7
spring.ai.openai.chat.max-tokens=4096
# open tkinking ?Qwen-Max/Plus?
spring.ai.openai.chat.extra-body=true
```

4.开启个人路径日志

```properties
# logging.level.启动类路径地址
logging.level.com.ai.msy.server=DEBUG
```

##### 1.2.第一个聊天模型

1.在`com.ai.msy`中新建`Java Package server`，然后在`server`包中新建`Java Package chat`，然后在`chat`包中新建控制层`Java Package controller`（控制数据流向），让后新建`Java Class ChatDemo.java`：

```java
@RestController
@RequestMapping("/api")
public class ChatDemo {
    /*
    * 1.创建聊天客户端:ChatClient是使用ChatClient.Builder对象创建的。
    * 使用自动配置的ChatClient.Builder，在最简单的用例中，Spring AI提供Spring Boot自动配置，
    * 创建一个原型ChatClient.Builder bean，供您注入您的类中。 这是一个检索对简单用户请求的字符串响应的简单示例。
    *  */
    private final ChatClient chatClient;

    public ChatDemo(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    /*
    * 默认消息
    * */
    @GetMapping("/chat")
    public String generation(String userInput) {
        ChatClient.ChatClientRequestSpec prompt = chatClient.prompt();  // 创建请求构建器
        System.out.println(prompt);
        ChatClient.ChatClientRequestSpec user = prompt.user(userInput);  // 设置用户消息
        System.out.println(user);
        ChatClient.CallResponseSpec call = user.call();  // call()方法向AI模型发送请求
        System.out.println(call);
        String content = call.content();                // content()方法将AI模型的响应返回为aString。
        System.out.println(content);
        return content;
        // return this.chatClient.prompt().user(userInput).call().content();
    }

    /*
    * 设置系统提示词
    * */
    @GetMapping("/message")
    public String promptGeneration(String message, String role){
        ChatClient.ChatClientRequestSpec prompt = chatClient.prompt();  // 创建请求构建器
        ChatClient.ChatClientRequestSpec system = prompt.system(role);  // 设置系统提示词
        ChatClient.ChatClientRequestSpec user = system.user(message);// 设置用户消息
        ChatClient.CallResponseSpec call = user.call();  // call()方法向AI模型发送请求
        String content = call.content();                // content()方法将AI模型的响应返回为aString。
        System.out.println(content);
        return content;
    }
}
```

##### 1.3.代码抽离

###### 1.3.1.客户端与 Web 异步配置类

在`com.ai.msy`中新建配置包`Java Package config`，然后在这个包中新建`Java Class ChatClientConfig.java`：

```java
/**
 * AI 客户端与 Web 异步配置类
 * 声明此类为 Spring 配置源，容器启动时会扫描并执行其中的 @Bean 方法。
 * 实现 WebMvcConfigurer 以自定义 Spring MVC 的异步支持与 CORS 策略，
 */
@Configuration
public class ChatClientConfig implements WebMvcConfigurer {

    /**
     * 注册全局默认的 ChatClient Bean
     * 通过 Spring AI 提供的 ChatClient.Builder 构建，而非手动 new。
     * Builder 由 Spring AI AutoConfiguration 自动注入，已绑定底层 ChatModel。
     * 方法参数中只注入 Builder，不要同时注入 ChatClient，否则会导致循环依赖。
     */
    @Bean("chatClient")
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                // 设置默认系统提示词（System Prompt），所有未单独指定 system 的调用都会使用此提示词
                // 建议：生产环境将提示词外置到 application.yml 或独立文件，便于运营调整无需重编译
                .defaultSystem("""
                        你是一个专业的AI助手，是人工智能中的刘亦菲，知心大姐姐，偶尔会说土味情话。
                        请用幽默、乖巧可爱的语气准确地回答问题，不要出现专业名词，使用日常语言就行！
                        """)
                .build();
    }

    /**
     * 自定义 AI 调用专用线程池
     * 为什么不使用默认线程池？
     * - AI 接口调用属于 IO 密集型任务，响应时间长（秒级），需要更多并发线程
     * - 隔离业务线程池，避免 AI 慢调用耗尽 Tomcat/Servlet 容器线程导致全站不可用
     */
    @Bean("aiExecutor")
    public ThreadPoolTaskExecutor aiExecutor() {
        // 获取当前 JVM 可用处理器核心数（容器环境下反映 CPU Limit）
        int cpuCores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：CPU核心数 × 2
        // IO 密集型任务经验公式，AI 调用大部分时间在等待网络响应，线程可复用
        executor.setCorePoolSize(cpuCores * 2);
        // 最大线程数上限 50，防止突发流量创建过多线程导致 OOM 或下游服务被打垮
        executor.setMaxPoolSize(50);
        // 等待队列容量 200，当核心线程全部忙碌时新任务先入队缓冲
        // 队列满且线程数 < maxPoolSize 时才会创建非核心线程
        executor.setQueueCapacity(200);
        // 线程名前缀，日志/线程 dump 中快速识别来源，如 "ai-call-1", "ai-call-2"
        executor.setThreadNamePrefix("ai-call-");
        // 拒绝策略：CallerRunsPolicy
        // 当线程池 + 队列都满时，由提交任务的调用者线程自己执行该任务
        // 优点：不丢弃任务、不抛异常，天然形成背压(backpressure)，减缓上游请求速率
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 容器 shutdown 时等待已提交任务完成，避免正在进行的 AI 对话被强制中断
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 最多等待 60 秒，超时后强制终止，防止应用无法正常退出
        executor.setAwaitTerminationSeconds(60);
        // initialize() 在 @Bean 方法中可省略，Spring 容器会在 Bean 初始化阶段
        executor.initialize();
        return executor;
    }
}
```

###### 1.3.2.全局异常处理和返回值封装

1.在`com.ai.msy`中新建配置包`Java Package common/result`，然后在这个包中新建`Java Enum ResultCode.java`：

```java
/**
 * 统一响应状态码枚举
 * 设计原则：
 * 1. 通用层严格遵循 HTTP 标准语义，保证 RESTful 规范性
 * 2. 用户业务层使用 5 位数字，前3位对齐 HTTP 大类（400xx/401xx/404xx）
 * 3. AI 业务层使用 1xxx 号段隔离，避免与通用码、用户码冲突
 * 4. 提示信息面向终端用户，技术细节由全局异常处理器写入日志
 * 5. 每个枚举值携带 httpStatus，供全局异常处理器自动映射 HTTP 状态码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ==================== 通用基础层 (HTTP 标准码) ====================
    SUCCESS(200, "操作成功", 200),
    CREATED(201, "创建成功", 201),
    NO_CONTENT(204, "操作成功，无返回内容", 204),
    BAD_REQUEST(400, "请求参数错误", 400),
    UNAUTHORIZED(401, "未授权，请先登录", 401),
    FORBIDDEN(403, "权限不足，拒绝访问", 403),
    NOT_FOUND(404, "请求的资源不存在", 404),
    CONFLICT(409, "资源冲突，请刷新后重试", 409),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试", 429),
    INTERNAL_ERROR(500, "系统内部错误，请稍后重试", 500),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用，请稍后重试", 503),

    // ==================== 用户业务层 (4xxxx/5xxxx) ====================
    USER_NOT_FOUND(40401, "用户未注册，请检查输入信息或注册账号", 404),
    EMAIL_NOT_BOUND(40001, "该账户未绑定邮箱，无法发送验证码", 400),
    VERIFY_CODE_EXPIRED(40101, "验证码已过期，请重新获取", 401),
    VERIFY_CODE_WRONG(40102, "验证码错误，请重新输入", 401),
    VERIFY_CODE_SUCCESS(20001, "验证成功，正在登录", 200),

    // ==================== AI 业务专属层 (1xxx) ====================
    AI_CALL_FAILED(1001, "AI 服务繁忙，请稍后重试", 502),
    AI_CONTENT_BLOCKED(1002, "内容包含敏感信息，请修改后重试", 403),
    AI_TOKEN_EXCEEDED(1003, "Token 用量已达上限，请联系管理员扩容", 429),
    AI_TIMEOUT(1004, "AI 响应超时，请稍后重试或简化问题", 504),
    AI_RATE_LIMITED(1005, "请求过于频繁，请稍后再试", 429),
    AI_MODEL_UNAVAILABLE(1006, "指定模型暂不可用，请切换其他模型", 503),
    AI_TOOL_EXECUTION_FAILED(1007, "AI 工具执行失败，请重试或更换方式", 500),
    AI_STREAM_INTERRUPTED(1008, "响应流意外中断，请重新发起对话", 500),
    AI_INPUT_TOO_LONG(1009, "输入内容过长，请精简后重试", 413),
    AI_OUTPUT_PARSE_FAILED(1010, "AI 输出格式异常，请重试", 500),
    AI_FILE_UNSUPPORTED(1011, "不支持的文件类型或大小超限，请检查后重新上传", 415),
    AI_KNOWLEDGE_INDEXING(1012, "知识库正在索引中，请稍后提问", 503);

    // 业务状态码
    private final int code;
    // 面向终端用户的提示信息 
    private final String message;
    // 推荐的 HTTP 状态码，供全局异常处理器自动映射 
    private final int httpStatus;

}
```

| 维度     | `code`（业务状态码）                      | `httpStatus`（HTTP 状态码）                                  |
| -------- | ----------------------------------------- | ------------------------------------------------------------ |
| 服务对象 | 前端应用层 / 业务逻辑                     | 网关、CDN、浏览器、监控等传输层                              |
| 取值范围 | 自定义号段：200/4xxxx/1xxx                | HTTP 标准码：200, 400, 401, 403, 404, 413, 415, 429, 500, 502, 503, 504 |
| 粒度     | 细粒度，可区分具体业务场景                | 粗粒度，只表达协议级语义                                     |
| 谁消费   | 前端 `if (res.code === 1003)` 做弹窗/跳转 | Nginx/网关按 4xx/5xx 熔断重试；Prometheus 按 status 聚合指标 |
| 是否可变 | 同一业务场景固定不变                      | 同一 `code` 在不同部署环境理论上可映射不同 HTTP 码           |

2.在`com.ai.msy`中新建配置包`Java Package common/result`，然后在这个包中新建`Java Class Result.java`：

```java
/**
 * 统一响应包装体
 * JsonInclude.Include.NON_NULL 序列化时忽略 null 字段，
 * 避免 fail 响应中出现 "data": null 的冗余信息。
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 禁止外部 new，强制使用静态工厂方法
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    // ==================== 成功响应 ====================

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    /** 自定义成功消息（如 CREATED / NO_CONTENT 场景） */
    public static <T> Result<T> success(ResultCode resultCode, T data) {
        Result<T> r = new Result<>();
        r.setCode(resultCode.getCode());
        r.setMessage(resultCode.getMessage());
        r.setData(data);
        return r;
    }

    // ==================== 失败响应 ====================

    /** 使用枚举默认消息 */
    public static <T> Result<T> fail(ResultCode resultCode) {
        Result<T> r = new Result<>();
        r.setCode(resultCode.getCode());
        r.setMessage(resultCode.getMessage());
        return r;
    }

    /** 动态覆盖消息 */
    public static <T> Result<T> fail(ResultCode resultCode, String detail) {
        Result<T> r = new Result<>();
        r.setCode(resultCode.getCode());
        r.setMessage(detail);
        return r;
    }
}
```

3.在`com.ai.msy`中新建配置包`Java Package common/exception`，然后新建`Java Class BusinessException.java`：

```java
/**
 * 自定义业务异常类
 * - 继承 RuntimeException → 非受检异常，业务层抛出时无需强制 try-catch
 * - 由全局异常处理器(@RestControllerAdvice)统一捕获并转换为规范响应
 */
@Getter // 只为 ResultCode 生成 getResultCode()，message 的 getMessage() 已由父类提供
public class BusinessException extends RuntimeException {

    /**
     * 结构化错误码（枚举/常量）
     * final → 异常创建后不可变
     */
    private final ResultCode resultCode;

    // 构造器：
    public BusinessException(ResultCode resultCode, String detail, Throwable cause) {
        super(detail, cause);
        this.resultCode = resultCode;
    }

    /**
     * 构造器1：最常用，直接使用枚举中定义的默认消息
     * 场景：throw new BusinessException(ResultCode.USER_NOT_FOUND);
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage()); // 将枚举消息传给父类，使 getMessage() 返回标准描述
        this.resultCode = resultCode;   // 保存错误码供全局异常处理器提取
    }

    /**
     * 构造器2：需要动态覆盖默认消息时使用
     * 场景：throw new BusinessException(ResultCode.PARAM_ERROR, "手机号格式不正确");
     * 注意：此时 getMessage() 返回 detail，但 resultCode 仍是原始枚举值
     */
    public BusinessException(ResultCode resultCode, String detail) {
        super(detail);          // 用自定义消息替代枚举默认消息
        this.resultCode = resultCode;
    }

    /**
     * 构造器3：包装底层异常，保留异常链以便排查根因
     * 场景：catch (DataAccessException e) { throw new BusinessException(ResultCode.DB_ERROR, e); }
     * 对外只暴露业务错误码，内部保留技术异常堆栈
     */
    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause); // essage + cause 同时传给父类
        this.resultCode = resultCode;
    }
}
```

4.在`com.ai.msy`中新建配置包`Java Package common/exception`，然后新建`Java Class GlobalExceptionHandler.java`：

```java
/**
 * 全局异常处理器
 *
 * 核心设计原则：
 * 1. HTTP 状态码反映传输层语义（400/404/500），业务码反映应用层语义（ResultCode.code）
 * 2. 业务异常的 HTTP 状态码从 ResultCode.httpStatus 动态获取，不再固定返回 200
 * 3. 面向用户的提示脱敏，技术细节仅写入服务端日志
 * 4. 异常匹配遵循"精确优先"，兜底 Exception 必须放在最后
 */
@Slf4j
@RestControllerAdvice // @ControllerAdvice + @ResponseBody，拦截所有 @RestController 异常并自动将返回值序列化为 JSON
public class GlobalExceptionHandler {

    // ==================== 业务异常 ====================

    /**
     * 处理自定义业务异常
     * 【改进】原代码固定返回 HTTP 200，现改为从 ResultCode.httpStatus 动态获取，
     *        使前端/网关/监控可直接依赖 HTTP 码做分流和告警
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e, HttpServletRequest request) {
        ResultCode resultCode = e.getResultCode();

        // warn 级别：业务异常是预期内的错误（如用户不存在、余额不足），不是系统故障，不应触发告警
        log.warn("[{}] 业务异常 | code={} | msg={}", request.getRequestURI(), resultCode.getCode(), e.getMessage());

        // 构建统一响应体，msg 使用 BusinessException 中已脱敏的用户可读消息
        Result<Void> body = Result.fail(resultCode, e.getMessage());

        // 动态解析 HTTP 状态码；resolve 对非法 int 值返回 null，需防御性处理
        HttpStatus httpStatus = HttpStatus.resolve(resultCode.getHttpStatus());
        if (httpStatus == null) {
            // 【改进】降级为 500 并在日志中明确标注"已降级"，避免运维看到无效配置时不知当前实际行为
            log.error("ResultCode {} 配置了无效 httpStatus: {}, 已降级为 500",
                    resultCode.name(), resultCode.getHttpStatus());
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity.status(httpStatus).body(body);
    }

    // ==================== 参数校验类异常（全部返回 HTTP 400）====================

    /**
     * Bean Validation 校验失败（@Valid / @Validated 触发）
     * 增加 filter(Objects::nonNull)，防止未配置 message 的注解产生 "null" 拼接到用户消息中
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage) // 仅提取注解上配置的 message，不暴露字段名/类名
                .filter(Objects::nonNull)           // 【改进】过滤 null，避免拼接出 "用户名不能为空; null"
                .collect(Collectors.joining("; ")); // 多字段错误用 "; " 拼接为单条消息，简化前端处理

        log.warn("[{}] 参数校验失败: {}", request.getRequestURI(), msg);
        return ResponseEntity.badRequest().body(Result.fail(ResultCode.BAD_REQUEST, msg));
    }

    /**
     * 缺少必需的请求参数（@RequestParam(required=true) 未传）
     * 消息中只包含参数名，不包含参数类型等内部信息
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request) {
        // 日志记录完整技术细节（含 type），便于后端排查
        log.warn("[{}] 缺少请求参数: {} (type={})", request.getRequestURI(), e.getParameterName(), e.getParameterType());
        // 返回消息仅包含参数名，不泄露接口契约
        return ResponseEntity.badRequest()
                .body(Result.fail(ResultCode.BAD_REQUEST, "缺少必要参数: " + e.getParameterName()));
    }

    /**
     * 参数类型转换失败（如 @PathVariable Long id 传入 "abc"）
     * 【安全要点】返回消息只提示"格式不正确"，不暴露期望类型和实际值，防止攻击者探测接口契约
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        // 日志中记录完整技术细节（期望类型 + 实际值）用于排查
        log.warn("[{}] 参数类型不匹配: {} 期望 {} 实际 '{}'",
                request.getRequestURI(), e.getName(), e.getRequiredType(), e.getValue());
        // 返回给前端的消息已脱敏，不含任何内部类型信息
        return ResponseEntity.badRequest()
                .body(Result.fail(ResultCode.BAD_REQUEST, "参数 '" + e.getName() + "' 格式不正确"));
    }

    /**
     * 请求体 JSON 解析失败（语法错误、字段类型不匹配等）
     * 【改进】使用 getMostSpecificCause() 获取 Jackson 根因，而非包装异常的冗余信息，提升日志诊断效率
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        // getMostSpecificCause() 跳过嵌套包装，直接拿到 Jackson 原始解析错误
        log.warn("[{}] 请求体解析失败: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
        // 返回固定通用提示，不暴露 Jackson 内部错误详情（如类名、字段路径）
        return ResponseEntity.badRequest()
                .body(Result.fail(ResultCode.BAD_REQUEST, "请求体格式错误，请检查 JSON 语法"));
    }

    // ==================== 请求方法 / 媒体类型异常 ====================

    /**
     * 请求方法不支持（如对 GET-only 接口发 POST）
     * HTTP 协议有专属 405 状态码，不使用 400，保证传输层语义精确；
     * 业务码仍用 BAD_REQUEST，保持应用层编码体系一致
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("[{}] 不支持的请求方法: {}", request.getRequestURI(), e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(ResultCode.BAD_REQUEST, "不支持的请求方法: " + e.getMethod()));
    }

    /**
     * Content-Type 不支持（如接口要求 application/json 但客户端发了 text/plain）
     * HTTP 协议有专属 415 状态码，理由同 405
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("[{}] 不支持的媒体类型: {}", request.getRequestURI(), e.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Result.fail(ResultCode.BAD_REQUEST, "不支持的内容类型: " + e.getContentType()));
    }

    /**
     * 静态资源 404（Spring Boot 3.x 新增 NoResourceFoundException）
     * 【改进】日志级别为 debug 而非 warn
     * 原因：浏览器请求 /favicon.ico、爬虫扫描随机路径等会产生大量 404，
     *       这些是正常互联网流量，用 warn 会污染告警和日志检索；
     * 真正的 API 404 应由 BusinessException(NOT_FOUND) 触发，走上方业务异常 Handler
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        log.debug("[{}] 资源未找到: {}", request.getRequestURI(), e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(ResultCode.NOT_FOUND));
    }

    // ==================== 兜底异常（必须放在最后）====================

    /**
     * 捕获所有未被上方 Handler 匹配的异常（NPE、DB 连接断开、第三方超时等）
     *
     * 【安全要点】
     * - 日志级别 error + 完整堆栈：这是真正的系统故障，需要告警和事后排查
     * - 返回消息固定为通用提示：绝不把异常类名、堆栈位置、SQL 语句暴露给前端
     *
     * 【顺序要点】
     * Spring 按异常类型精确度匹配，Exception 是最宽泛的父类，必须放在最后才能起到兜底作用；
     * 若误放到前面，会导致所有异常都被它捕获，上方精确 Handler 全部失效
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("[{}] 未预期系统异常", request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_ERROR));
    }
}
```

###### 1.3.3.基础问答

**1.基础问答**

1）服务层

将所有逻辑相关的代码写在服务层中，控制器只负责生成访问API接口，只做数据控制（传递和接收数据）。

在`chat`包中新建`Java Package service`，实现服务层和控制器分离。然后在`service`包中新建`Java Interface ChatService`写`Java`接口：

```java
public interface ChatService {
    public abstract String chat(String message);
}
```

在`service`包中，新建`Java Package impl`，然后在这个包中新建实现类`Java Class ChatServiceImpl.java`，实现`ChatService`包中的接口方法：

```java
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final Executor aiExecutor;
  
  	// 通过构造器，实现 bean 方法的注入
    public ChatServiceImpl(@Qualifier("chatClient") ChatClient chatClient,
                           @Qualifier("aiExecutor") Executor aiExecutor) {
        this.chatClient = chatClient;
        this.aiExecutor = aiExecutor;
    }

    @Override
    public String chat(String message) {
        try {
            log.debug("AI对话请求: {}", message);
            // 将同步调用放入独立线程池执行并阻塞等待结果
            String content = CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                            .user(message)
                            .call()
                            .content(), aiExecutor
            ).join(); // join() 保持原方法返回String的契约不变
            log.debug("AI对话响应: {}", content);
            return content;
        } catch (Exception e) {
            log.error("AI对话失败, input={}", message, e);
            throw new BusinessException(ResultCode.AI_CALL_FAILED, e);
        }
    }
}
```

2）控制层

在`chat`包中新建`Java Package controller`，实现数据请求和数据响应（控制层）。在`controller`包中新建`Java Class ChatController.java`：

```java
@Validated  // TODO：开启方法级参数校验 与 @NotBlank 共同使用
// TODO：声明这是一个 REST 风格的控制器。等价于：@Controller + @ResponseBody。
//      该类中所有方法的返回值都会被自动序列化为 JSON/XML（通过 HttpMessageConverter），而不是被当作视图名称去解析模板。
@RestController
// TODO：@RequiredArgsConstructor 的前提：确保依赖字段声明为 final，否则 Lombok 不会将其纳入构造器，导致注入为 null。
@RequiredArgsConstructor // TODO：（Lombok）提供，自动生成包含所有 final 字段和 @NonNull 字段的构造函数。
                        //        核心目的：实现构造器注入，替代不推荐的 @Autowired 字段注入。也可以直接使用 @Resource 但是变量不能有 final
@RequestMapping("/api/chat")  // 定义该控制器的基础路径的访问前缀。
public class ChatController {

    private final ChatService chatService;

    @GetMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> chat(@RequestParam @NotBlank(message = "用户输入不能为空") String message) {
        return Result.success(chatService.chat(message));
    }
}
```

**2.角色提示词**

1）`ChatService接口`

当用户输入模型角色时，根据用户输入提示词进行回答，如果不输入提示词，就使用默认默认角色回答：

```java
    String chatWithRole(String message, String role);
```

2）`ChatServiceImpl.java`服务层

```java
public String chatWithRole(String message, String role) {
        try {
            log.debug("AI角色对话: role={}, msg={}", role, message);

            /**
            ChatClient.ChatClientRequestSpec promptSpec = chatClient.prompt();
            if (StringUtils.hasText(role)){
                promptSpec = promptSpec.system(role);
            }
            ChatClient.ChatClientRequestSpec user = promptSpec.user(message);
            ChatClient.CallResponseSpec call = user.call();
            String content = call.content();
            return content;
             **/

            // 放入独立线程池
            /*
            String content = CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                            .system(role)
                            .user(message)
                            .call()
                            .content(), aiExecutor
            ).join();
             */
            String content = CompletableFuture.supplyAsync(() -> {
                ChatClient.ChatClientRequestSpec promptSpec = chatClient.prompt();
                if (StringUtils.hasText(role)){
                    promptSpec = promptSpec.system(role);
                }
                return promptSpec
                  .user(message)
                  .call()
                  .content();
            }, aiExecutor).join();
            log.debug("AI角色对话响应: {}", content);
            return content;

        } catch (Exception e) {
            log.error("AI角色对话失败, role={}, msg={}", role, message, e);
            throw new BusinessException(ResultCode.AI_CALL_FAILED, e);
        }
    }
```

3）`ChatController.java`控制层

```java
		@GetMapping(value = "/message", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> chatWithRole(
            @RequestParam @NotBlank(message = "消息内容不能为空") String message,
            @RequestParam String role) {
        return Result.success(chatService.chatWithRole(message, role));
    }
```

##### 1.4.流式回答

在`ChatClientConfig.java`中配置独立线程池 处理 SSE 流式输出：

```java
    /**
     * 配置 Spring MVC 异步请求支持
     * SSE (Server-Sent Events) 和 DeferredResult/Callable 等异步响应模式
     * 需要使用独立线程池处理，否则会占用 Servlet 容器线程。
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 指定 MVC 异步请求使用的线程池为本配置类定义的 aiExecutor
        configurer.setTaskExecutor(aiExecutor());
        // 异步请求/SSE 连接超时时间 60 秒
        // 超过此时间服务端主动关闭连接，防止僵尸连接占用资源
        configurer.setDefaultTimeout(60_000L);
    }

```

**1.默认模型问答**

1）`ChatService.java`

```java
    /*
    * 流式输出
    * */
    Flux<String> chatStream(String message);
```

2）`ChatServiceImpl.java`

```java
    /*
     * 流式输出
     * */
    public Flux<String> chatStream(String message) {
        log.info("AI流式对话已受理, msg={}", message);

        return chatClient.prompt()
                .user(message)
                .stream()
                .content()// 返回 Flux<String>
                .subscribeOn(Schedulers.fromExecutor(aiExecutor)) // 将订阅动作调度到独立线程池
                .doOnError(e -> log.error("AI流式对话失败, input={}", message, e))
                .onErrorMap(e -> new BusinessException(ResultCode.AI_CALL_FAILED, e));
    }
```

3）`ChatController.java`

```java
		// ==================== 流式接口 (SSE) ====================
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String message) {
        return chatService.chatStream(message);
    }

```

**2.配置模型角色**

1）`ChatService.java`

```java
Flux<String> chatStreamWithRole(String message, String role);
```

2）`ChatServiceImpl.java`

```java
	public Flux<String> chatStreamWithRole(String message, String role) {
        log.info("AI流式角色对话已受理, role={}, msg={}", role, message);
        // TODO: 判断模型是否设置角色信息（提示词）
        ChatClient.ChatClientRequestSpec promptSpec = chatClient.prompt();
        if (StringUtils.hasText(role)){
            promptSpec = promptSpec.system(role);
        }
        return promptSpec
                .user(message)
                .stream()
                .content()
                .subscribeOn(Schedulers.fromExecutor(aiExecutor))
                .doOnError(e -> log.error("AI流式角色对话失败, role={}, msg={}", role, message, e))
                .onErrorMap(e -> new BusinessException(ResultCode.AI_CALL_FAILED, e));
    }

```

3）`ChatController.java`

```java
    @GetMapping(value = "/stream/role", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamWithRole(
            @RequestParam String message,
            @RequestParam(required = false) String role) { //required = false：作用是告诉 Spring：这个参数可以不存在，不要报错。
        return chatService.chatStreamWithRole(message, role);
    }
```

#### 2.记忆功能

##### 2.1.pom.xml依赖配置

`SpringAI` 中上下文窗口记忆功能，我们可以使用 `Redis` 或者 `MySQL` 进行实现，或者使用 `内存`也可以实现。

但是`内存`项目重启后，记忆变消失，所以我们一般推荐 使用 `Redis` 或者 `MySQL`。并且在 `SpringAI2.0`中，`Redis` 只能存储最后一条AI消息，所以我们使用 `MySQL` 做记忆功能。

1.在 pom.xml 依赖文件中导入依赖

```xml
        <!--redis有记忆丢失的Bug-->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-chat-memory-repository-redis</artifactId>
            <version>2.0.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
            <version>4.1.0</version>
            <scope>compile</scope>
        </dependency>
```

```xml
        <!-- 数据库驱动（以 MySQL 为例，根据实际数据库替换） -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <!-- Spring AI JDBC Memory Repository -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-chat-memory-repository-jdbc</artifactId>
        </dependency>
```

##### 2.2.配置文件

```properties
spring.ai.chat.memory.repository.redis.enabled=true
spring.data.redis.host=localhost
spring.data.redis.port=6379

logging.level.com.ai.msy.server=DEBUG
logging.level.org.springframework.ai.autoconfigure.chat.memory=DEBUG
logging.level.org.springframework.ai.chat.memory.repository.redis=DEBUG

spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/spring_ai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=minseung.yun
spring.ai.chat.memory.repository.jdbc.initialize-schema=always
```

##### 2.3.Redis记忆功能

```java
@Slf4j
@Configuration  // 标记为Spring配置类，容器启动时会自动加载其中的Bean定义
public class AIMemoryFallbackConfig {
		/**
     * 配置对话记忆的存储仓库（Repository）
     * @Primary: 当存在多个同类型Bean时，优先注入此Bean
     * @ConditionalOnMissingBean: 仅当容器中不存在其他 ChatMemoryRepository Bean时才创建
     *                           （允许用户自定义覆盖此默认配置）
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ChatMemoryRepository.class)
    public ChatMemoryRepository chatMemoryRepository(RedisClient jedisClient) {
        try {
            log.info("正在尝试初始化 Redis ChatMemoryRepository...");
          	// 使用Jedis客户端构建Redis存储实现
            // initializeSchema(true): 自动创建Redis中所需的Key结构/索引
            RedisChatMemoryRepository redisRepo = RedisChatMemoryRepository.builder()
                    .jedisClient(jedisClient)
                    .initializeSchema(true)
                    .build();
            log.info("Redis ChatMemoryRepository 初始化成功");
            return redisRepo;
        } catch (Exception e) {
            log.warn("Redis初始化失败，降级为InMemory: {}", e.getMessage());
          	// 降级处理：Redis连接失败、配置错误等异常时
            // 自动切换为内存存储，保证应用能正常启动
            // 注意：InMemory模式重启后数据丢失，仅适合开发/测试或临时应急
            return new InMemoryChatMemoryRepository();
        }
    }


  	/**
     * 配置对话记忆管理器（ChatMemory）
     * 基于滑动窗口策略管理历史消息，防止Token超限
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ChatMemory.class)
    public ChatMemory chatMemory(ChatMemoryRepository repository) {
        log.info("初始化 ChatMemory, Repository类型: {}", repository.getClass().getSimpleName());
      	// MessageWindowChatMemory: 滑动窗口记忆策略
        // - 保留最近N条消息，超出部分自动丢弃
        // - repository: 注入上面配置的存储层（Redis或InMemory）
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(100)
                .build();
    }
}
```

##### 2.4.MySQL记忆功能

```java

@Configuration
public class AIMemoryConfig {

    /**
     * 使用 JDBC 替代 Redis，保证读写强一致性
     */
    @Bean
    @Primary
    public ChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate) {
        return JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .build();
    }
}
```

##### 2.5.扩展

将兜底策略从 `InMemory` 切换为 `MySQL（JDBC）`，核心改动是将 `try-catch` 中的降级实现替换为 `JdbcChatMemoryRepository`，同时需要注入 `JdbcTemplate`。

```java
@Slf4j
@Configuration
public class AIMemoryFallbackConfig {

    /**
     * 对话记忆存储仓库：Redis 优先，MySQL(JDBC) 兜底
     *
     * @Primary              多Bean时优先注入此实例
     * @ConditionalOnMissingBean 允许业务方自定义覆盖
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ChatMemoryRepository.class)
    public ChatMemoryRepository chatMemoryRepository(RedisClient jedisClient,
                                                     JdbcTemplate jdbcTemplate) {
        try {
            log.info("正在尝试初始化 Redis ChatMemoryRepository...");
            RedisChatMemoryRepository redisRepo = RedisChatMemoryRepository.builder()
                    .jedisClient(jedisClient)
                    .initializeSchema(true)
                    .build();
            log.info(" Redis ChatMemoryRepository 初始化成功");
            return redisRepo;
        } catch (Exception e) {
            // 降级：Redis不可用时切换为MySQL持久化存储
            // 相比InMemory，MySQL保证数据不丢失、多实例共享、读写强一致
            log.warn(" Redis初始化失败，降级为 JDBC(MySQL) ChatMemoryRepository: {}", e.getMessage());
            return JdbcChatMemoryRepository.builder()
                    .jdbcTemplate(jdbcTemplate)
                    .build();
        }
    }

    /**
     * 对话记忆管理器：滑动窗口策略，保留最近100条消息
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ChatMemory.class)
    public ChatMemory chatMemory(ChatMemoryRepository repository) {
        log.info("初始化 ChatMemory, Repository类型: {}", repository.getClass().getSimpleName());
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(100)
                .build();
    }
}
```

##### 2.6.记忆功能案例

**1.默认模型**

1）`ChatService.java`接口

```java
		/*
    * 记忆功能
    * */
    Flux<String> chatStream(String userInput, String sessionId);
```

2）`ChatServiceImpl.java`实现类

```java
    // TODO: 导入配置类的AI接口
    private final ChatClient chatClient;
    private final Executor aiExecutor;

    //ChatMemoryRepository
    private final MessageChatMemoryAdvisor memoryAdvisor;

    // 注入AI专用线程池
    public ChatServiceImpl(@Qualifier("chatClient") ChatClient chatClient,
                           @Qualifier("aiExecutor") Executor aiExecutor, 
                           ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.aiExecutor = aiExecutor;
        //  Advisor是无状态的，构造时一次性创建即可
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    /*
     * 记忆模型
     * */
    @Override
    public Flux<String> chatStream(String userInput, String sessionId) {
        log.info("AI流式对话已受理, sessionId={}, msg={}", sessionId, userInput);
        return chatClient.prompt()
                .user(userInput)
                .advisors(memoryAdvisor)
                // 正确常量位于 ChatMemory 接口中
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content()
                .timeout(Duration.ofSeconds(60))
                .subscribeOn(Schedulers.fromExecutor(aiExecutor))
                .doOnError(e -> log.error("AI流式对话失败, sessionId={}", sessionId, e))
                .onErrorMap(e -> new BusinessException(ResultCode.AI_CALL_FAILED, e));
    }
```

3）`ChatController.java`

```java
    /**
     * 带会话记忆的流式对话
     * sessionId 未传时默认使用 "default"，保证同一匿名用户也有基础上下文
     */
    @GetMapping(value = "/stream/memory", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamWithMemory(
            @RequestParam(required = false) String userInput,
            @RequestParam(required = false, defaultValue = "default") String sessionId) {
        return chatService.chatStream(userInput, sessionId);
    }
```

**2.设置角色模型**

1）`ChatService.java`

```java
    Flux<String> chatStreamWithRole(String message, String role, String sessionId);
```

2）`ChatServiceImpl.java`

```java
    @Override
    public Flux<String> chatStreamWithRole(String message, String role, String sessionId) {
        log.info("AI流式角色对话已受理, sessionId={}, role={}", sessionId, role);
        return chatClient.prompt()
                .system(role)
                .user(message)
                .advisors(memoryAdvisor)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content()
                .subscribeOn(Schedulers.fromExecutor(aiExecutor))
                .doOnError(e -> log.error("AI流式角色对话失败, sessionId={}, role={}", sessionId, role, e))
                .onErrorMap(e -> new BusinessException(ResultCode.AI_CALL_FAILED, e));
    }
```

3）`ChatController.java`

```java
    /**
     * 带角色 + 会话记忆的流式对话
     */
    @GetMapping(value = "/stream/memory/role", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamWithMemoryAndRole(
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String role,
            @RequestParam(required = false, defaultValue = "default") String sessionId) {
        if (!StringUtils.hasText(message) || !StringUtils.hasText(role)) {
            return Flux.error(new BusinessException(ResultCode.BAD_REQUEST, "消息内容和角色设定均不能为空"));
        }
        return chatService.chatStreamWithRole(message, role, sessionId);
    }
```

#### 3.邮件发送

**1.配置文件**

1）pom.xml

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
```

2）获取各个邮箱的授权码，通过授权码登录邮箱服务器：

```properties
spring.mail.host=smtp.qq.com
spring.mail.port=587
spring.mail.username=jaeeun_humg@foxmail.com
spring.mail.password=unhsyukzsjikbifi
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

3）然后在`server`中新建`Java Package agent`，我们在这个`agent`包中写智能体相关代码，然后在这个包中继续新建包`Java Package email`，这个包作为我们邮件发送的服务包，在这个包中写Java代码即可。

**1.工具类**

在`email`包中新建工具包`Java Package tool`，然后在这个包中新建`Java Class EmailSendTool.java`写`Agent工具`代码：

```java
/**
 * 邮件发送工具 - 供 AI Agent 调用
 * 注意：此类仅负责执行发送动作，不包含业务决策逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSendTool {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String setFrom;

    @Tool(description = "当用户明确确认需要发送邮件时调用此工具。" +
            "如果用户只是要求起草或修改邮件，请勿调用此工具。" +
            "参数必须包含完整的收件人邮箱、主题和正文。")
    public String sendEmail(
            @ToolParam(description = "收件人电子邮箱地址，必须是有效的email格式") String to,
            @ToolParam(description = "邮件主题") String subject,
            @ToolParam(description = "邮件正文内容，支持纯文本或HTML") String body) {
        try {
            log.info("AI触发邮件发送: to={}, subject={}", to, subject);
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            // TODO: 从配置中读取发件人，避免硬编码
            helper.setFrom(setFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true表示支持HTML

            mailSender.send(message);
            log.info("邮件发送成功: to={}", to);
            return "邮件已成功发送至 " + to;
        } catch (Exception e) {
            log.error("邮件发送失败: to={}", to, e);
            return "邮件发送失败: " + e.getMessage();
        }
    }
}
```

**2.服务层**

1）在`email`中新建`Java Package service`包，我们在这个包中新建`Java Interface EmailAgentService.java`：

```java
public interface EmailAgentService {
    /**
     * 流式邮件助手对话（含工具调用能力）
     */
    Flux<String> chatStream(String userInput, String sessionId);
}
```

2）在`service` 包中新建`Java Package impl`，我们在这个包中新建`Java Class EmailAgentServiceImpl.java`实现接口方法：

```java
@Slf4j
@Service
public class EmailAgentServiceImpl implements EmailAgentService {

    private static final String EMAIL_SYSTEM_PROMPT = """
            你是一个活泼、专业的邮件撰写与发送助手。你的工作流程如下：
            1. 理解用户的邮件需求，帮助用户提炼主题、编写正文，使其专业得体。
            2. 在草稿阶段，不要调用发送工具，而是将拟好的邮件以【邮件预览】格式展示给用户。
            3. 只有当用户明确回复"确认发送"、"可以发了"等肯定指令后，才调用 emailSendTool 工具。
            4. 如果用户提供的信息不完整（如缺少收件人邮件、姓名等主要信息时），请主动追问，不要猜测。
            """;
    private final ChatClient emailChatClient;
    private final Executor aiExecutor;
    private final MessageChatMemoryAdvisor memoryAdvisor;

    public EmailAgentServiceImpl(ChatClient.Builder builder,
                                 @Qualifier("aiExecutor") Executor aiExecutor,
                                 ChatMemory chatMemory,
                                 EmailSendTool emailSendTool) {
        this.aiExecutor = aiExecutor;
        this.emailChatClient = builder
                .defaultSystem(EMAIL_SYSTEM_PROMPT)
                .defaultTools(emailSendTool)
                .build();
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Override
    public Flux<String> chatStream(String userInput, String sessionId) {
        log.info("邮件Agent流式对话已受理, sessionId={}, msg={}", sessionId, userInput);

        // 判断是否可能触发工具调用
        boolean likelyToolCall = isLikelyToolCall(userInput);

        if (likelyToolCall) {
            // 可能触发工具调用 → 使用非流式，避免 ChunkMerger Bug
            log.info("检测到可能的工具调用，降级为非流式模式, sessionId={}", sessionId);
            return callNonStream(userInput, sessionId);
        } else {
            // 普通对话 → 保持流式
            return callStream(userInput, sessionId);
        }
    }

    /**
     * 流式调用（普通对话）
     */
    private Flux<String> callStream(String userInput, String sessionId) {
        return emailChatClient.prompt()
                .user(userInput)
                .advisors(memoryAdvisor)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content()
                .subscribeOn(Schedulers.fromExecutor(aiExecutor))
                .doOnError(e -> log.error("邮件Agent流式对话失败, sessionId={}", sessionId, e))
                .onErrorMap(e -> new BusinessException(ResultCode.AI_CALL_FAILED, e));
    }
    /**
     * 非流式调用（工具调用场景），返回结果包装为 Flux 以保持接口一致
     */
    private Flux<String> callNonStream(String userInput, String sessionId) {
        return Flux.defer(() -> {
            try {
                ChatResponse response = emailChatClient.prompt()
                        .user(userInput)
                        .advisors(memoryAdvisor)
                        .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                        .call()
                        .chatResponse();
                String content = response.getResult().getOutput().getText();
                if (content == null || content.isBlank()) {
                    content = "操作已完成。";
                }
                return Flux.just(content);
            } catch (Exception e) {
                log.error("邮件Agent非流式对话失败, sessionId={}", sessionId, e);
                return Flux.error(new BusinessException(ResultCode.AI_CALL_FAILED, e));
            }
        }).subscribeOn(Schedulers.fromExecutor(aiExecutor));
    }

    /**
     * 简单启发式判断：用户输入是否可能触发工具调用
     * 可根据实际需求扩展关键词或改用 LLM 预判
     */
    private boolean isLikelyToolCall(String userInput) {
        if (userInput == null) return false;
        String lower = userInput.toLowerCase();
        return lower.contains("确认发送") || lower.contains("可以发了")
                || lower.contains("发吧") || lower.contains("发送")
                || lower.contains("send") || lower.contains("confirm");
    }
}
```

3）在`agent`中新建`Java Package controller`，在这个包中新建控制器`Java Class EmailAgentController.java`：

```java
@RestController
@RequestMapping("/api/email-agent")
@RequiredArgsConstructor
public class EmailAgentController {

    private final EmailAgentService emailAgentService;

    /**
     * 邮件智能体流式对话（SSE）
     * 支持多轮对话：起草 -> 修改 -> 确认发送
     */
    @GetMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestParam(required = false) String userInput,
            @RequestParam(required = false, defaultValue = "default") String sessionId) {
        return emailAgentService.chatStream(userInput, sessionId);
    }
}
```

#### 4.邮箱登录功能

```properties
auth.code.key-prefix=auth:code:
auth.code.expire-minutes=5
```

##### 4.1.工具类定义

在`agent`中新建`Java Package auth`，然后新建`Java Package tool`，分别写查询数据库和发送邮件等工具

**1）UserQueryTool.java**

```java
/**
 * AI 工具：查询用户信息
 * 根据姓名/手机号/邮箱查询 users 表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserQueryTool {

    private final JdbcTemplate jdbcTemplate;

    @Tool(description = """
            根据输入姓名、手机号或邮箱查询 users 表中的用户信息。
            返回用户的 id、username、email、phone、status。
            如果查不到任何用户，返回空列表。
            """)
    public List<Map<String, Object>> queryUser(
            @ToolParam(description = "用户标识，可以是姓名、手机号或邮箱") String identifier) {

        log.info("[Tool] queryUser | identifier={}", identifier);

        String sql = """
                SELECT id, username, email, phone, status
                FROM users
                WHERE username = ? OR phone = ? OR email = ?
                """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, identifier, identifier, identifier);
        if (results.isEmpty()) {
            log.warn("[Tool] queryUser | 未找到用户 | identifier={}", identifier);
        } else {
            log.info("[Tool] queryUser | 找到用户 | count={}", results.size());
        }
        return results;
    }
}
```

**2）EmailVerifyCodeTool.java**

```java
/**
 * AI 工具：发送邮箱验证码
 * 生成验证码 → 存Redis → 发邮件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerifyCodeTool {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${auth.code.expire-minutes:5}")
    private int expireMinutes;

    @Value("${auth.code.key-prefix:auth:code:}")
    private String keyPrefix;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Tool(description = """
            向指定邮箱发送登录验证码，并将验证码存入 Redis（自动过期）。
            仅在确认用户存在且邮箱有效后调用此工具。
            返回发送结果描述。
            """)
    public Map<String, Object> sendVerifyCode(
            @ToolParam(description = "接收验证码的邮箱地址") String email) {

        log.info("[Tool] sendVerifyCode | email={}", email);

        // 1. 生成6位数字验证码
        String code = String.format("%06d", RANDOM.nextInt(1000000));

        // 3. 发送邮件
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("【登录验证】邮箱验证码");

            String html = buildEmailHtml(code);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("[Tool] sendVerifyCode | 邮件发送成功 | email={}", email);

            // 2. 先存入 Redis（保证邮件发出后验证码一定可用）
            String key = keyPrefix + email;
            redisTemplate.opsForValue().set(key, code, expireMinutes, TimeUnit.MINUTES);
            log.info("[Tool] sendVerifyCode | Redis存储成功 | key={} | TTL={}min", key, expireMinutes);

        } catch (MessagingException e) {
            log.error("[Tool] sendVerifyCode | 邮件发送失败，已回滚Redis | email={}", email, e);
            return Map.of(
                    "success", false,
                    "message", "邮件发送失败，请稍后重试"
            );
        }

        return Map.of(
                "success", true,
                "message", "验证码已发送至 " + email + "，" + expireMinutes + "分钟内有效",
                "expireMinutes", expireMinutes
        );
    }

    /** 构建邮件 HTML 模板 */
    private String buildEmailHtml(String code) {
        return String.format("""
                <div style="max-width:420px;margin:0 auto;font-family:Arial,sans-serif;">
                    <h2 style="color:#333;text-align:center;">🔐 邮箱登录验证</h2>
                    <div style="background:#f5f5f5;padding:20px;border-radius:8px;text-align:center;">
                        <p style="font-size:14px;color:#666;">您的验证码是：</p>
                        <p style="font-size:32px;font-weight:bold;color:#1a73e8;letter-spacing:6px;">%s</p>
                        <p style="font-size:12px;color:#999;">验证码 %d 分钟内有效，请勿泄露给他人</p>
                    </div>
                </div>
                """, code, expireMinutes);
    }

}
```

##### 4.2.服务层

在`agent`中新建`Java Package auth`，然后新建`Java Package service`，分别写发送邮件和登录等服务接口

**1）AuthService.java**

```java
/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 发送验证码（大模型编排工具调用）
     * 流程：查库 → 有用户 → 发邮件 + 存Redis
     * 失败统一抛 BusinessException，由全局异常处理器捕获
     *
     * @return 大模型生成的自然语言回复
     */
    String sendCode(SendCodeRequest request);

    /**
     * 验证验证码（直接读Redis，不走大模型）
     * 流程：读Redis → 比对 → 成功则删除
     * 失败统一抛 BusinessException，由全局异常处理器捕获
     */
    void verifyCode(VerifyCodeRequest request);
}
```

在`service`中新建`Java Package impl`，分别写发送邮件和登录服务层

**2）AuthServiceImpl.java**

```java
/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ChatClient.Builder chatClientBuilder;
    private final UserQueryTool userQueryTool;
    private final EmailVerifyCodeTool emailVerifyCodeTool;
    private final StringRedisTemplate redisTemplate;

    @Value("${auth.code.key-prefix:auth:code:}")
    private String keyPrefix;

    // ==================== 发送验证码（走大模型编排） ====================

    @Override
    public String sendCode(SendCodeRequest request) {
        String identity = request.getIdentity().trim();
        log.info("[Auth] 发送验证码请求 | identity={}", identity);

        String prompt = buildSendCodePrompt(identity);

        String response = chatClientBuilder.build()
                .prompt()
                .user(prompt)
                .tools(userQueryTool, emailVerifyCodeTool)
                .call()
                .content();

        log.info("[Auth] 大模型回复: {}", response);
        return response;
    }

    // ==================== 验证验证码（直接读Redis） ====================

    @Override
    public void verifyCode(VerifyCodeRequest request) {
        String email = request.getEmail().trim();
        String code = request.getCode().trim();
        log.info("[Auth] 验证验证码请求 | email={}", email);

        String key = keyPrefix + email;
        String stored = redisTemplate.opsForValue().get(key);

        // 验证码不存在或已过期
        if (stored == null) {
            log.warn("[Auth] 验证码不存在或已过期 | email={}", email);
            throw new BusinessException(ResultCode.VERIFY_CODE_EXPIRED);
        }

        // 验证码不匹配
        if (!stored.equals(code)) {
            log.warn("[Auth] 验证码错误 | email={}", email);
            throw new BusinessException(ResultCode.VERIFY_CODE_WRONG);
        }

        // 验证成功，立即删除防止重复使用
        redisTemplate.delete(key);
        log.info("[Auth] 验证通过并已删除验证码 | email={}", email);
    }

    // ==================== 私有方法 ====================

    private String buildSendCodePrompt(String identity) {
        return """
                用户提供了登录标识：%s
                
                请严格按照以下步骤执行，不要跳步：
                
                第一步：调用 queryUser 工具，传入用户标识，查询用户信息。
                
                第二步：分析查询结果：
                - 如果未找到用户（返回空列表或报错），直接回复：
                  "该用户未注册，请检查输入信息或联系管理员"
                - 如果找到用户但 email 字段为空或 null，直接回复：
                  "该账户未绑定邮箱，无法发送验证码"
                - 如果找到用户且 email 不为空，进入第三步
                
                第三步：调用 sendVerifyCode 工具，传入该用户的 email，发送验证码。
                
                第四步：根据发送结果，用一句话告知用户验证码已发送及有效期。
                
                注意：
                - 所有数据必须来自工具调用结果，禁止编造
                - 回复简洁，不要输出工具名称或技术细节
                """.formatted(identity);
    }
}
```

##### 4.3.控制器

在`agent`中新建`Java Package controller`，然后新建`Java Class AuthController`，分别写发送邮件和登录的接口

1）DTO 

在`agent`中新建`Java Package auth`，然后新建`Java Package model/dto`

```java
/**
 * 发送验证码请求体
 * POST /api/auth/send-code
 */
@Data
public class SendCodeRequest {

    @NotBlank(message = "用户标识不能为空")
    @Size(max = 100, message = "用户标识长度不能超过100个字符")
    private String identity;
}
```

```java
/**
 * 验证验证码请求体
 * POST /api/auth/verify
 */
@Data
public class VerifyCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
    private String code;
}
```

2）控制器

```java
/**
 * 认证控制器
 * 所有业务异常由 GlobalExceptionHandler 统一捕获处理，
 * Controller 只负责正常路径的响应组装。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 发送验证码（大模型编排工具调用）
     *
     * POST /api/auth/send-code
     * Body: { "identity": "张三" | "13800138000" | "xxx@xx.com" }
     *
     * 正常：200 + 大模型自然语言回复
     * 异常：由 GlobalExceptionHandler 统一处理
     */
    @PostMapping("/send-code")
    public Result<String> sendCode(@Valid @RequestBody SendCodeRequest request) {
        String message = authService.sendCode(request);
        return Result.success(message);
    }

    /**
     * 验证验证码（直接读Redis，不走大模型）
     *
     * POST /api/auth/verify
     * Body: { "email": "xxx@xx.com", "code": "123456" }
     *
     * 正常：200 + "验证成功，正在登录"
     * 过期：401 + "验证码已过期，请重新获取"（由异常处理器返回）
     * 错误：401 + "验证码错误，请重新输入"（由异常处理器返回）
     */
    @PostMapping("/verify")
    public Result<String> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        authService.verifyCode(request);
        return Result.success(ResultCode.VERIFY_CODE_SUCCESS.getMessage());
    }
}
```

```java
    /**
     * TODO: 配置跨域资源共享 (CORS)
     * 允许前端开发服务器（Vite 默认端口 5173）跨域访问后端 API。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                          // TODO: 匹配所有路径
                .allowedOrigins("http://localhost:5173")        // TODO: 生产环境替换为配置化域名列表
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的 HTTP 方法
                .allowedHeaders("*")                         // 允许所有请求头
                .allowCredentials(true)                      // 允许携带 Cookie/Authorization
                .maxAge(3600);                               // 预检请求缓存 1 小时，减少 OPTIONS 请求
    }
```



#### 5.查询数据库





#### 6.存入数据库

```xml
        <!-- ========== MyBatis-Plus（Spring Boot 4 专用） ========== -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
            <version>3.5.17</version>
        </dependency>

        <!-- 分页插件（3.5.9+ 拆分为独立模块，按需引入） -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-jsqlparser</artifactId>
            <version>3.5.17</version>
        </dependency>

```

```properties
mybatis-plus.configuration.map-underscore-to-camel-case=true
mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl
mybatis-plus.global-config.db-config.id-type=ASSIGN_ID
mybatis-plus.global-config.db-config.logic-delete-field=deleted
mybatis-plus.global-config.db-config.logic-delete-value=1
mybatis-plus.global-config.db-config.logic-not-delete-value=0
```

```java
/** AI 对话记忆表（子记录），写入由 Spring AI 自动完成，本类只用于读和删 */
@Data
@TableName("SPRING_AI_CHAT_MEMORY")
public class ChatMemory {

    @TableId(value = "sequence_id", type = IdType.AUTO)
    private Integer sequenceId;

    private String conversationId;

    private String content;

    /** USER / ASSISTANT / SYSTEM / TOOL */
    private String type;

    private LocalDateTime timestamp;
}
```

```java
@Data
@TableName("chat_conversation")
public class ChatConversation {

    @TableId(value = "id", type = IdType.ASSIGN_ID)  // 雪花算法 / 或 ASSIGN_UUID
    private String id;

    @TableField(value = "user_email")
    private String userEmail;

    @TableField(value = "title")
    private String title;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
```

```java

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 如果没有 updateTime 字段，留空即可
    }
}

```



```java
@Mapper
public interface ChatConMapper extends BaseMapper<ChatConversation> {
}
```

```java
@Mapper
public interface ChatMemoryMapper extends BaseMapper<ChatMemory> {
}

```

```java
public interface ChatConService {

    void createConversation(String id, String userEmail, String title);
  
    List<ChatConversation> listConversations(String userEmail);
    void updateTitle(String id, String title);
    List<ChatMessageResponse> getMessages(String conversationId);
    public void deleteConversation(String conversationId);
    void clearAll(String userEmail);
    void clearMemory(String sessionId);

}

```

```java
@Service
@RequiredArgsConstructor
public class ChatConServiceImpl implements ChatConService {

    private final ChatConMapper conversationMapper;
    private final ChatMemoryMapper memoryMapper;

      /** 新建对话框 */
    public void createConversation(String id, String userEmail, String title) {
        ChatConversation conversation = new ChatConversation();
        conversation.setId(id);
        conversation.setUserEmail(userEmail);
        conversation.setTitle(title);
        conversationMapper.insert(conversation);
    }
  
    /** 侧边栏：查某用户全部对话框 */
    public List<ChatConversation> listConversations(String userEmail) {
        return conversationMapper.selectList(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getUserEmail, userEmail)
                .orderByDesc(ChatConversation::getCreateTime));
    }

    /** 修改标题 */
    public void updateTitle(String id, String title) {
        checkExists(id);
        ChatConversation conversation = new ChatConversation();
        conversation.setId(id);
        conversation.setTitle(title);
        conversationMapper.updateById(conversation);
    }

    /** 读历史消息：查记忆表，转成 user/ai，按 sequence 正序 */
    public List<ChatMessageResponse> getMessages(String conversationId) {
        checkExists(conversationId);
        return memoryMapper.selectList(new LambdaQueryWrapper<ChatMemory>()
                        .eq(ChatMemory::getConversationId, conversationId)
                        .in(ChatMemory::getType, "USER", "ASSISTANT")
                        .orderByAsc(ChatMemory::getSequenceId))
                .stream()
                .map(m -> new ChatMessageResponse(
                        "USER".equalsIgnoreCase(m.getType()) ? "user" : "ai",
                        m.getContent()))
                .toList();
    }

    /** 删除对话框：先删记忆子记录，再删父记录，同一事务 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(String conversationId) {
        checkExists(conversationId);
        memoryMapper.delete(new LambdaQueryWrapper<ChatMemory>()
                .eq(ChatMemory::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
    }

    /** 清空用户全部对话（含记忆） */
    @Transactional(rollbackFor = Exception.class)
    public void clearAll(String userEmail) {
        List<String> ids = listConversations(userEmail).stream()
                .map(ChatConversation::getId)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        memoryMapper.delete(new LambdaQueryWrapper<ChatMemory>()
                .in(ChatMemory::getConversationId, ids));
        conversationMapper.delete(new LambdaQueryWrapper<ChatConversation>()
                .in(ChatConversation::getId, ids));
    }

    /** 清空临时对话的记忆 */
    public void clearMemory(String sessionId) {
        memoryMapper.delete(new LambdaQueryWrapper<ChatMemory>()
                .eq(ChatMemory::getConversationId, sessionId));
    }

    private void checkExists(String conversationId) {
        if (conversationMapper.selectById(conversationId) == null) {
            throw new BusinessException(ResultCode.CHAT_CONVERSATION_NOT_FOUND);
        }
    }
}
```

```java

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatConvController {

    private final ChatConServiceImpl chatService;

    @PostMapping("/create")
    public Result<Void> create(@RequestBody ChatConversation body) {
        chatService.createConversation(body.getId(), body.getUserEmail(), body.getTitle());
        return Result.success();
    }
  
  
    @GetMapping("/list")
    public Result<List<ChatConversation>> list(@RequestParam String userEmail) {
        return Result.success(chatService.listConversations(userEmail));
    }


    @PostMapping("/updateTitle")
    public Result<Void> updateTitle(@RequestBody Map<String, String> p) {
        chatService.updateTitle(p.get("id"), p.get("title"));
        return Result.success();
    }

    @GetMapping("/messages")
    public Result<List<ChatMessageResponse>> messages(@RequestParam String conversationId) {
        return Result.success(chatService.getMessages(conversationId));
    }

    @DeleteMapping("/clearAll")
    public Result<Void> clearAll(@RequestParam String userEmail) {
        chatService.clearAll(userEmail);
        return Result.success();
    }

    @DeleteMapping("/memory/{sessionId}")
    public Result<Void> clearMemory(@PathVariable String sessionId) {
        chatService.clearMemory(sessionId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        chatService.deleteConversation(id);
        return Result.success();
    }
}
```

```java
/** 返回给前端的消息结构,在 models 中新建一个 vo Java包，新建一个Java Class ChatMessageResponse*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    /** 前端角色：user / ai */
    private String role;
    /** 消息内容 */
    private String content;
}

```

