package com.ai.kust.server.agent.auth.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component//注入到SpringApplication IOC容器
public class EmailQueryTool {
    private final JdbcTemplate jdbcTemplate;

    @Tool(description = """
            1）根据输入的 邮箱地址查询 users 表中用户信息
            2）如果查询到的用户信息中的 status 值为0，那么就返回 用户账户异常，请联系管理员
            3）如果用户信息中的 status 值为1 ，就返回用户信息，并返回一个用户列表；
            4）如果根据邮箱查询不到用户信息，就返回一个空列表，然后返回该邮箱未注册，请注册后使用
            """)
    public List<Map<String, Object>> queryUser(@ToolParam(description = "用户姓名") String email){
        log.info("正在查询用户信息：用户邮箱：{},",email);
        String sql = """
                SELECT * FROM users WHERE email = ?
                """;
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, email);
        if (result.isEmpty()) {
            log.warn("用户查询为空，用户邮箱：{}", email);
        }else {
            log.info("已查询到{}用户，用户的信息：{}", email, result);
        }
        return result;
    }
}


