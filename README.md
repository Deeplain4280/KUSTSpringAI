# Spring AI 工具调用项目（Agent智能体项目）
#### 版本说明：Spring Boot4.0 + Spring AI 2.0 ， JDK必须使用 JDK25


## 第一天：项目配置解析

### 1.SpringBoot 依赖

- 创建项目时，就已经自动帮我们导入依赖


    <!-- Spring Boot 的父级依赖（做spring Boot开发。必须加载这个依赖）-->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.1</version>  <!-- 版本号 -->
        <relativePath/>
    </parent>

    <!-- JDK 版本号 -->
    <properties>
        <java.version>25</java.version>
    </properties>

    <!-- 依赖存储位置（MySQL  Rediscover Email） -->
    <dependencies>
        <!-- Spring Boot Web开发依赖（Spring + MVC ）  -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc-test</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

- 导入SpringAI + OpenAI相关依赖


    <!-- Spring AI 的版本号管理，只能管理 Spring  AI的版本号，
    Spring Boot 版本和其他版本需要自己处理  -->
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

        <!--OpenAI 依赖： 导入第三方大模型（云服务器） -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>

1) 在 .properties 配置文件中，配置openai云服务器（阿里云百炼平台）


        # api 密钥
        spring.ai.openai.api-key=sk-ws-HG8pzLTD1LqHzPeaSZeKk-
        # 服务器地址
        spring.ai.openai.base-url=https://llm-p5xgvi9zm7z9yrev.cn-beijing.maas.aliyuncs.com/compatible-mode/v1
        # 模型名称
        #spring.ai.openai.chat.options.model=qwen3.7-plus-2026-05-26
        spring.ai.openai.chat.model=qwen3.7-plus-2026-05-26
        # 模型温度（越高越发散（结果越不准确），越低越收敛（越准确））
        spring.ai.openai.chat.temperature=0.7
        # 一次性返回的最大token（这 是手背 、 这是  脚背 ，你 是 我的 宝贝）
        spring.ai.openai.chat.max-tokens=4096
        # 思考功能
        spring.ai.openai.chat.extra-body=true
        #spring.ai.openai.chat.options.enable-thinking=true


2) demo案例代码
- 在com.ai.kust中新建一个 Java Package server，继续在 server 包中新建一个 Java Package chat ，
- chat 用来写聊天模型, 在 chat 包中新建一个 Java Package controller，controller用来控制数据流向和接收
- controller 包我们称为控制层（作用：控制数据流向（流向 service 层， 将数据发送给前端））
- 在 controller包中新建一个 Java Class ChatDemo.java


        @RestController  // TODO：等同于 @ResponseBody + @Controller 注解，将数据以JSON发送给前端，不让返回值经过试图解析器（解析HTML代码的容器）
        //@ResponseBody
        //@Controller
        // TODO： @ResponseBody：将数据以JSON格式返回给前端（html页面），@Controller ：将Java来标注为一个控制器类（控制器数据请求和转发）
        // TODO：请求地址，启动父服务器输入本地地址：127.0.0.1:服务器端口号8080/请求地址
        @RequestMapping(value = "/api")    // TODO：请求：127.0.0.1:8080/api
        public class ChatDemo {
        
            /*
            * 假设返回一个 问答内容
            * 访问: 启动服务器，127.0.0.1:8080/api/say
            * */
            @GetMapping(value = "/say")  // TODO: GET、POST、PATCH、DELETE ，对应的请求方式就是 xxxMapping(“/请求地址”)
            public String say(){
                return "你好，我是你的智能助手YOYO，请问有什么可以帮你？";
            }


3) 案例代码

- 基础对话

    
      /*
        * 创建一个聊天客户端，用来接收用户输入指令或者用户聊天内容
        * 需要经过大模型，实现聊天内容
        * */
          // TODO：第一步，注入聊天大模型，通过声明聊天变量（Spring AI ChatClient）注入大模型
          private final ChatClient chatClient;
    
      // TODO：通过构造器注入聊天模型/客户端,通过 ChatClient 的 Builder 方法，创建聊天客户端（创建一个Java对象）
      public ChatDemo(ChatClient.Builder chatBuilderClient){
      this.chatClient = chatBuilderClient.build();      // this：表示调用类变量
      }
    
      // TODO : 定义一个方法，实现基础对话   127.0.0.1:8080/api/chat?userInput=你好，你是谁？
      @RequestMapping(value = "/chat", method = RequestMethod.GET)  // TODO : 等同于 GetMapping(value="/chat")
      public String generation(String userInput){      // userInput ：用户输入的信息
      ChatClient.ChatClientRequestSpec prompt = chatClient.prompt();  // TODO: 创建请求构造器
      ChatClient.ChatClientRequestSpec user = prompt.user(userInput);  // TODO : 将用户的信息发送给请求构造器，用户对话传递给构造器方法
      //System.out.println(user);
      ChatClient.CallResponseSpec call = user.call(); //TODO: 将用户信息发送给大模型
      //System.out.println(call);
      // TODO : 等待大模型返回信息
      String content = call.content();    // TODO : 接收大模型的返回信息（大模型传递给用户信息）
      System.out.println(content);
      return content;
    
            //return this.chatClient.prompt().user(userInput).call().content();
      }


- 设置提示词


    /*设置提示词：设置模型角色*/
    @GetMapping("/chat/role")
    // 127.0.0.1:8080/api/chat/role?userInput=你是谁，我不喜欢你&role=你是人工智能中的刘亦菲，一个知心大姐姐
    // 127.0.0.1:8080/api/chat/role?userInput=你是谁，我不喜欢你&role=你是毒舌
    public String generatePrompt(String userInput, String role){   // userInput：接收用户信息   role：接收提示词
        ChatClient.ChatClientRequestSpec prompt = chatClient.prompt();   // 创建请求
        ChatClient.ChatClientRequestSpec system = prompt.system(role);  // 设置系统提示词（提示词工程）
        ChatClient.ChatClientRequestSpec user = system.user(userInput);  // 接收用户信息
        ChatClient.CallResponseSpec call = user.call(); // 将用户的信息发送给大模型
        String content = call.content();  // 接收大模型的返回值
        return content;
    }


## 第二天:逻辑分离

将控制层（controller） 与 业务逻辑（service）进行分割

- 配置日志打印路径
    - 在 properties 配置文件文件中添加下面配置


    # 添加日志打印 logging.level.路径地址
    logging.level.com.ai.kust.server=DEBUG

1) 配置默认模型角色（提示词）

在com.ai.kust 中新建一个Java Package config（配置包），继续在 config 包中新建一个 Java Class ChatClientConfig.java


      /*
      * 这是配置类文件，使用 @Configuration 注解，进行标注，在启动项目就会自动加载 配置类 中的配置信息
      * 将配置信息加载到 SpringApplication 中（IOC容器）
      * 配置类需要实现 WebMvcConfigurer 接口
      * */
        @Configuration
        public class ChatClientConfig implements WebMvcConfigurer {
    
        /*
        * 写一个Java方法，配置默认默认角色（提示词），将这个方法 通过 Spring Bean 注册为全局方法
        * */
          @Bean("chatClient")
          public ChatClient chatClient(ChatClient.Builder builder){
          return builder.defaultSystem("""
          你是一个专业、幽默风趣的智能助手，是人工智能中的刘亦菲，是一个知心大姐姐，偶尔爱说土味情话。
          请使用幽默风趣、可爱乖巧的语气回答用户内容，偶尔会生气！！！
          """).build();
          }
      }

2) 配置AI专用线程池


    @Bean("aiExecutor")
    public ThreadPoolTaskExecutor aiExecutor(){
        // 获取自己电脑的CPU核心数
        int cpuCores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(); // 创建任务线程池对象
        // 核心线程数 = cpu核数 * 2
        // 网络等待时，耗时最长
        executor.setCorePoolSize(cpuCores * 2);
        // 创建的最大的线程数量，防止创建的线程过多导致系统宕机
        executor.setMaxPoolSize(50);
        //设置等待队列（200次等待）
        executor.setQueueCapacity(200);
        // 线程名称前缀
        executor.setThreadNamePrefix("ai-call-");
        // 当线程池（50）和 队列数量（200） 已经创建完成，满了的时候，让调用者自己创建线程
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 当任务完成时，销毁线程池 shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 如果 线程池请求超时，超时60秒强制结束
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }


3) 统一封装返回值

在 com.ai.kust 中新建一个 Java package common ，继续在这个包中 新建 Java Package result， 在这个包中新建一个Java Eumn ResultCode.java
用来存储状态码和状态信息


      /*
      * 统一状态码和状态信息
      * 设计原则：遵循HTTP协议，符合设计风格
      * AI相关的状态码： 1xxxx
      * 用户业务相关的状态：2xx  3xx  4xx  5xx
      * */
        @Getter   // 等同于 get 方法，用来获取对应的变量值
        @AllArgsConstructor   // 全参数构造器
        public enum ResultCode {
        SUCCESS(200, "操作成功", 200),  // 200 自定义状态码，“操作成功” 自定义的提示信息， 200 HTTPState协议自带的
        CREATED(201, "创建成功", 201),
        BAD_REQUEST(400, "请求参数错误", 400),
        NOT_FOUND(404, "请求资源不存在", 404),
        INTERNAL_ERROR(500,"系统内部错误，请稍后重试", 500);
        // 业务状态码
        private final int code;
        // 业务返回的提示信息
        private final String message;
        // HTTPStatus状态码
        private final int HTTPStatus;
        // TODO： 等同于 注解 @AllArgsConstructor（构造器的作用：创建 Java 对象的 ）
        /*    ResultCode(int code, String message, int HTTPStatus) {
        this.code = code;
        this.message = message;
        this.HTTPStatus = HTTPStatus;
        }*/
        // 自定义业务状态码根据业务码查找提示信息
        public static ResultCode formCode(int code){
        for (ResultCode rc: values()){
        if (code == rc.HTTPStatus) return rc;
        }
        return null;
        }
    }

在 result 文件夹中，新建一个 Java Class Result.java，用来统一封装返回值


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Data     //  包含了 @Getter +  @Setter  +  @NoArgsConstructor +  @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)  // u序列化时自动忽略空字段, 不返回空字段
    public class Result<T> {  // T 占位符。表示传递的数据等类型未知（T ->String   Object  Int）
    
        private Integer code;  // Integer 是 int 的包装类（int 基本数据类型  Integer 对象）
        private String message;
        private T data;         // T 表示范型
        /*
        *   成功返回值时，成功时，返回空字符
        * */
        public static <T> Result<T> success(){
            return null;
        }
        /*
        * 成功返回时，返回数据
        * */
        public static <T> Result<T> success(T data){
            Result<T> r = new Result<>();
            r.setCode(ResultCode.SUCCESS.getCode());  // 获取 枚举类型 中 SUCCESS 中定义 code 和 message
            r.setMessage(ResultCode.SUCCESS.getMessage());
            r.setData(data);
            return r;
        }
        /*
        * 失败的返回值
        * */
        public static <T> Result<T> fail(ResultCode resultCode){
            Result<T> r = new Result<>();
            r.setCode(resultCode.getCode());
            r.setMessage(resultCode.getMessage());
            return r;
        }
        /*
        * 动态显示错误信息
        * */
        public static <T> Result<T> fail(ResultCode resultCode, String detail){ // detail 失败原因
            Result<T> r = new Result<>();
            r.setCode(resultCode.getCode());
            r.setMessage(detail);
            return r;
        }
    }


在 common 包中新建一个全局异常处理包 Java Package exception，然后在这个包中新建一个Java class BusinessException.java


在 exception 中新建一个Java class GlobalExceptionHandler.java (拦截器--拦截异常信息，处理完后将异常信息传递给BusinessExceptpion)


## 第三天：代码分离

- 将控制层和服务层进行代码抽离，控制器用来控制数据流向，服务层用来写代码业务逻辑！

1. 服务层
- 在 chat 中 新建一个 服务层 java package servcie，继续在 service 包中新建Java interface ChatService.java



- 在 service 包中，新建一个 Java package impl，实现接口中的抽象方法，在 impl 包中，新建 Java Class ChatServiceImpl.java
这个Java类，用来实现接口中的方法.


- 在controller包中 新建一个 Java Class ChatController.java 用来做数据转发


2.流式输出配置自己的，需要配置独立的异步请求线程池

- 在 ChatClientConfig.java 中配置独立线程池


    /*
    * 配置Spring MVC支持异步请求
    * 需要配置独立的SSE（Servet-Sent-Events）
    * */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 指定MVC线程池
        configurer.setTaskExecutor(aiExecutor());
        // 配置超市时间，避免僵尸一直占用线程池，配置60秒，避免线程一直占用资源
        configurer.setDefaultTimeout(60000L);
    }


## 第四天：记忆功能

- 大模型（LLM-大语言模型）是一个无状态（一次对话一个线程，无法识别上下文的对话内容）的问答系统
  - redis记忆功能，开源的redis不支持 LTE redis-stack （使用 docker 部署私有 redis（支持redis-stack版本），Linux 部署 redis-stack 版本） 
  - 使用 JDBC 做记忆功能（使用MySQL作为记忆功能），redis做短期记忆（内存记忆），MySQL做长期记忆直到用户自己删除（redis+mysql）

1. 使用MySQL做记忆功能

- 在 pom.xml 文件中导入jdbc依赖和连接数据库的依赖

        <!-- jdbc 记忆以来（MySQL记忆功能的依赖） -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-chat-memory-repository-jdbc</artifactId>
        </dependency>
        <!-- 连接MySQL数据库的依赖 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>


2. 配置数据库连接
- 在properties配置数据库连接和记忆模型


    # 数据库连接驱动
    spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
    # 数据库连接地址和服务器地址
    spring.datasource.url=jdbc:mysql://127.0.0.1:3306/kust_spring_ai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    # 数据库用户名
    spring.datasource.username=root
    # 数据库密码
    spring.datasource.password=minseung.yun
    # 开启MySQL数据库记忆功能
    spring.ai.chat.memory.repository.jdbc.initialize-schema=always
    # 记忆功能日志打印
    logging.level.org.springframework.ai.autoconfigure.chat.memory=DEBUG

3. 配置文件中，配置记忆功能的上下文窗口

- 在 config 文件夹中，新建一个配置类 Java class AIMemoryConfig.java

4. 项目代码（chat中的代码，只是demo案例代码； common  config属于项目架构）

- 在server Java包中新建一个 Java package qwen
- 在 qwen 包中，新建一个 Java 包 service，新建一个 Java interface QwenService.java

- 在service 这个包中，新建一个 Java package impl，然后在 impl 中新建一个Java class QwenServiceImpl.java 用来实现接口中的抽象方法

- 在 qwen 中新建一个 Java package controller，然后在这个新建的包中新建一个 Java class QwenController.java

4. 获取QQ邮箱授权码

- 获取qq邮箱授权码
- 添加 pom.xml 邮件发送的依赖


        <!-- 邮箱发送依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>


- 添加配置文件


    # qq邮箱服务器地址
    spring.mail.host=smtp.qq.com
    # qq 邮箱的端口号
    spring.mail.port=587
    # 自己的QQ邮箱（需要改为自己的QQ邮箱）
    spring.mail.username=jaeeun_humg@foxmail.com
    # QQ 邮箱的授权码（刚刚让大家保存起来）
    spring.mail.password=unhsyukzsjikbifi
    # 允许使用密钥进行远程调用，需要进行身份验证（检测邮箱和密钥）
    spring.mail.properties.mail.smtp.auth=true
    # 允许远程调过程中使用加密传输
    spring.mail.properties.mail.smtp.starttls.enable=true
    # 与服务器邮箱连接超时时间（）
    spring.mail.properties.mail.smtp.connectiontimeout=5000
    # 服务器响应超市时间
    spring.mail.properties.mail.smtp.timeout=5000
    # 写入服务器超时时间
    spring.mail.properties.mail.smtp.writetimeout=5000