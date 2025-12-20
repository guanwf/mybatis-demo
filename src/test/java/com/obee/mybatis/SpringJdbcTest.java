package com.obee.mybatis;

import com.obee.mybatis.model.User;
import com.obee.mybatis.service.SpringJdbcDynamicService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 22:15
 */

@SpringBootTest
public class SpringJdbcTest {

    @Autowired
    SpringJdbcDynamicService springJdbcDynamicService;

    @Test
    public void Test(){

        String sql = "SELECT * FROM sys_user WHERE age > #{age} AND status = #{status}";

        // 2. 准备参数
        Map<String, Object> params = new HashMap<>();
        params.put("age", 18);
        params.put("status", 1);

        // 3. 执行
        // 结果自动映射为 List<User>
        List<User> users = springJdbcDynamicService.selectSqlList(sql, params, User.class);

    }

}
