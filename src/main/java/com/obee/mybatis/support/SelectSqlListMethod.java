package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;


/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 19:49
 */
public class SelectSqlListMethod extends AbstractMethod {

    public static final String METHOD_NAME = "selectSqlList";

    public SelectSqlListMethod() {
        super(METHOD_NAME);
    }

    protected SelectSqlListMethod(String methodName) {
        super(methodName);
    }


    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 核心：定义接受 ${sql} 的模板
        // 注意：<script> 是必须的，否则 MyBatis 可能不会将其识别为动态 SQL
        String sqlTemplate = "<script>${sql}</script>";

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sqlTemplate, modelClass);

        // 注册查询方法，并绑定返回值类型为 Entity (modelClass)
        return this.addSelectMappedStatementForTable(mapperClass, METHOD_NAME, sqlSource, tableInfo);
    }
}
