package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 20:01
 */
public class DeleteBySql extends AbstractMethod {
    public static final String METHOD_NAME = "deleteBySql";

    public DeleteBySql() { super(METHOD_NAME); }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 直接执行传入的 SQL
        String sqlTemplate = "<script>${sql}</script>";
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sqlTemplate, modelClass);
        return this.addDeleteMappedStatement(mapperClass, METHOD_NAME, sqlSource);
    }

}
