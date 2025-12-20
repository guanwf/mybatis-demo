package com.obee.mybatis.service;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 22:13
 */
@Service
public class SpringJdbcDynamicService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SpringJdbcDynamicService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public <T> List<T> selectSqlList(String sql, Map<String, Object> params, Class<T> clazz) {
        // 1. 处理参数 (防止 null)
        Map<String, Object> paramMap = params == null ? Collections.emptyMap() : params;

        // 2. 执行查询
        // BeanPropertyRowMapper 是 Spring 提供的神器，自动根据列名映射到 Bean 属性
        // 注意：SQL 中的参数需要用 :name 的形式，而不是 MyBatis 的 #{name}
        return jdbcTemplate.query(
                sql,
                paramMap,
                new BeanPropertyRowMapper<>(clazz)
        );
    }
}
