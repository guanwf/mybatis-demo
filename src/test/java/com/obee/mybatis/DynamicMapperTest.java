package com.obee.mybatis;

import com.obee.mybatis.model.Goods;
import com.obee.mybatis.service.DynamicMapperService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 21:02
 */
@SpringBootTest
public class DynamicMapperTest {

    @Autowired
    private DynamicMapperService dynamicMapperService;

    @Test
    public void test() {
        // 1. User 类只需要有 @TableName 注解，不需要 Mapper 接口
        // 2. 第一次运行时，registerEntityIfNeed 会自动解析 User 类

        String sql = "select * from sys_user where age > #{age}";
        Map<String, Object> params = Map.of("age", 18);

        // 3. 直接调用，返回 List<User>
        List<Goods> users = dynamicMapperService.selectSqlList(sql, params, Goods.class);

        System.out.println(users);
    }

}
