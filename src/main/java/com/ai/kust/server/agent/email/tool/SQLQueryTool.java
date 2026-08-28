package com.ai.kust.server.agent.email.tool;

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
public class SQLQueryTool {

    private final JdbcTemplate jdbcTemplate;

    @Tool(description = """
            1）根据用户的姓名，查询users表中的用户信息
            2）返回用户姓名，用户邮箱地址，和手机号码
            3)如果查询不到用户信息，返回用户信息为空，返回一个空列表
            """)
    public List<Map<String, Object>> queryUser(@ToolParam(description = "用户姓名") String userName){
        log.info("正在查询用户信息：用户姓名：{},",userName);
        String sql = """
                SELECT email, phone FROM users WHERE username = ?
                """;
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, userName);
        if (result.isEmpty()) {
            log.warn("用户查询为空，用户姓名：{}", userName);
        }else {
            log.info("已查询到用户，用户姓名为：{}", userName);
        }
        return result;
    }
}
