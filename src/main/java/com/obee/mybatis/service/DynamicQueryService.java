package com.obee.mybatis.service;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.obee.mybatis.support.SelectSqlListMethod;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 20:04
 */
@Service
public class DynamicQueryService {
    private final SqlSessionTemplate sqlSessionTemplate;

    public DynamicQueryService(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    /**
     * 万能查询方法
     *
     * @param sqlTemplate SQL 语句 (支持 #{param})
     * @param parameter   参数 Map
     * @param entityClass 实体类 Class (用于定位 Mapper 和 结果映射)
     * @return 实体列表
     */
    public <T> List<T> selectSqlList(String sqlTemplate, Map<String, Object> parameter, Class<T> entityClass) {
        // 1. 获取 MP 的元数据
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);

        Assert.notNull(tableInfo, "MP缓存中未找到该实体，请检查: 1.是否有对应的Mapper接口 2.是否被@MapperScan扫描到 -> " + entityClass.getName());

        // 2. 准备参数 Map (必须包含 key 为 "sql" 的项)
        Map<String, Object> map = parameter == null ? new HashMap<>() : new HashMap<>(parameter);
        map.put("sql", sqlTemplate);

        // 3. 构建 MappedStatement ID
        // 格式：com.example.mapper.UserMapper.selectSqlList
        String statementId = tableInfo.getCurrentNamespace() + "." + SelectSqlListMethod.METHOD_NAME;

        // 4. 执行底层查询
        // MyBatis 会自动将 map 中的其他 key (如 #{name}) 匹配到 SQL 中
        return sqlSessionTemplate.selectList(statementId, map);
    }

}
