# Spring AI 工具调用项目（Agent智能体项目）
#### 版本说明：Spring Boot4.0 + Spring AI 2.0 ， JDK必须使用 JDK25


## 第一天：pom.xml配置文件

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


        @RestController  // TODO：@ResponseBody + @Controller 注解，将数据以JSON发送给前端，不让返回值经过试图解析器（解析HTML代码的容器）
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
        
        }
