package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 15:22
 */
public class UpdateBySql extends AbstractMethod {
    // 方法名
    public static final String METHOD_NAME = "updateBySql";

    public UpdateBySql() {
        super(METHOD_NAME);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 1. 定义模板：直接执行传入的 SQL
        // <script> 标签用于支持动态 SQL 标签
        String sqlTemplate = "<script>${sql}</script>";

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sqlTemplate, modelClass);

        // 2. 注册为 Update 类型的 Statement (返回影响行数)
        return this.addUpdateMappedStatement(mapperClass, modelClass, METHOD_NAME, sqlSource);
    }

}
