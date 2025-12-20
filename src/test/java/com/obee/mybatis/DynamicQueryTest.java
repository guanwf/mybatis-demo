package com.obee.mybatis;

import com.obee.mybatis.model.User;
import com.obee.mybatis.service.DynamicQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class DynamicQueryTest {

	@Autowired
	DynamicQueryService dynamicQueryService;

	@Test
	void contextLoads() {
		// 假设 User 实体对应 sys_user 表

		// 1. 定义 SQL（注意：这里用了 #{age} 参数绑定，比直接拼字符串安全）
		String sql = "SELECT * FROM sys_user WHERE age > #{age} AND status = #{status}";

		// 2. 准备参数
		Map<String, Object> params = new HashMap<>();
		params.put("age", 18);
		params.put("status", 1);

		// 3. 执行
		// 结果自动映射为 List<User>
		List<User> users = dynamicQueryService.selectSqlList(sql, params, User.class);

	}

}
