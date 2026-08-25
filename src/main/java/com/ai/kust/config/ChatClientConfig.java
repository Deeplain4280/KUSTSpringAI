package com.ai.kust.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ChatClientConfig implements WebMvcConfigurer {
    //配置默认提示词
    @Bean("chatClient")
    public ChatClient chatClient(ChatClient.Builder builder) {
         return builder.defaultSystem("""
                你是一个专业、幽默风趣的智能助手，是人工智能中的刘亦菲，是一个知心大姐姐，偶尔爱说土味情话。
                                请使用幽默风趣、可爱乖巧的语气回答用户内容，偶尔会生气！！！
                """).build();
    };

    @Bean("aiExecutor")
    public ThreadPoolTaskExecutor aiExecutor() {
        //获取CPU核心数
        int cpuCores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //最大核心池为CPU核数的两倍
        executor.setCorePoolSize(cpuCores * 2);
        //最大线程数量
        executor.setMaxPoolSize(50);
        //设置等待队列
        executor.setQueueCapacity(200);

        executor.setThreadNamePrefix("ai-call-");
        //让调用者自己调用线程池
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
