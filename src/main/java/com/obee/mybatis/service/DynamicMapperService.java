package com.obee.mybatis.service;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 21:00
 */
@Service
public class DynamicMapperService {

    private final SqlSessionTemplate sqlSessionTemplate;
    private final ObjectMapper objectMapper; // 用于 Map -> Bean 转换

    public DynamicMapperService(SqlSessionTemplate sqlSessionTemplate, ObjectMapper objectMapper) {
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 无需 Mapper 接口，直接查库
     */
    public <T> List<T> selectSqlList(String sqlTemplate, Map<String, Object> parameter, Class<T> entityClass) {
        // 1. 确保实体类已注册到 MP (核心修改点)
        registerEntityIfNeed(entityClass);

        // 2. 准备参数
        Map<String, Object> map = parameter == null ? new HashMap<>() : new HashMap<>(parameter);
        map.put("sql", sqlTemplate);

        // 3. 执行 SQL
        // 注意：因为没有 UserMapper.xml，我们无法使用 MyBatis 的自动 Bean 映射
        // 我们利用一个通用的 <script>${sql}</script> 执行器，返回 Map 列表
        List<Map<String, Object>> rawList = executeRawSql(map);

        // 4. 手动转换结果 (Map -> User)
        return convertToBeanList(rawList, entityClass);
    }

    /**
     * 核心黑科技：运行时手动注册 Entity 的元数据
     * 解决 "MP缓存中未找到该实体" 的问题
     */
    private synchronized void registerEntityIfNeed(Class<?> entityClass) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        if (tableInfo == null) {
            Configuration configuration = sqlSessionTemplate.getConfiguration();

            // 创建一个临时的构建助手，命名空间随便起，因为我们不真正使用 Mapper 接口调用
            String dummyNamespace = entityClass.getName();
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
            assistant.setCurrentNamespace(dummyNamespace);

            // 触发 MP 的解析逻辑
            TableInfoHelper.initTableInfo(assistant, entityClass);

            // 再次检查
            Assert.notNull(TableInfoHelper.getTableInfo(entityClass), "实体注册失败: " + entityClass.getName());
        }
    }

    /**
     * 执行原生 SQL，返回 Map 列表
     * 需要在项目中定义一个通用的 Statement，或者在代码里动态生成 MappedStatement (太复杂，推荐用 XML 定义一个通用的)
     */
    private List<Map<String, Object>> executeRawSql(Map<String, Object> params) {
        // 这里我们需要一个通用的查询入口
        // 建议在 resources/mapper/CommonMapper.xml 中定义一个 ID 为 "dynamicSelect" 的查询
        // 或者使用下面的代码动态创建一个 MappedStatement (如果不想写 XML)

        // 简单方式：假设你有一个 CommonMapper 接口或者在 XML 里写死了一个通用查询
        // return sqlSessionTemplate.selectList("com.example.CommonMapper.dynamicSelect", params);

        // 纯代码方式（不需要 XML）：使用 SqlRunner 或者下面这个简单技巧
        // 为了方便，我们复用 SqlSession 的 selectList，但 ID 指向一个必定存在的通用查询
        // 如果你完全不想写任何 Mapper，这里推荐使用 SqlRunner (需要开启配置)

        // 这里演示最稳健的方案：必须配合一个 DynamicSqlMapper 接口
        return sqlSessionTemplate.selectList("com.obee.mybatis.mapper.DynamicSqlMapper.selectRaw", params);
    }

    private <T> List<T> convertToBeanList(List<Map<String, Object>> rawList, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        if (rawList != null) {
            for (Map<String, Object> map : rawList) {
                // Jackson 的 convertValue 非常强大，能处理驼峰、类型转换
                result.add(objectMapper.convertValue(map, clazz));
            }
        }
        return result;
    }

}
