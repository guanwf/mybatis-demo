package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import java.util.Map;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 21:26
 */
public class SelectBySql extends AbstractMethod {
    public static final String METHOD_NAME = "selectBySql";

    public SelectBySql() { super(METHOD_NAME); }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 直接执行 ${sql}
        // 注意：addSelectMappedStatementForTable 会强制结果类型为 modelClass
        String sqlTemplate = "<script>${sql}</script>";

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sqlTemplate, modelClass);
        return this.addSelectMappedStatementForOther(mapperClass, METHOD_NAME, sqlSource, Map.class);
    }

}
