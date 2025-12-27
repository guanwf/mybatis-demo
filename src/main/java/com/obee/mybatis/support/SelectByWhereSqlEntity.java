package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 21:26
 */
public class SelectByWhereSqlEntity extends AbstractMethod {
    public static final String METHOD_NAME = "selectByWhereSqlEntity";

    public SelectByWhereSqlEntity() { super(METHOD_NAME); }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        String sqlSelect = sqlSelectColumns(tableInfo, false);

        // WHERE ${whereSql}
        String sql = String.format("<script>SELECT %s FROM %s <where>${whereSql}</where></script>",
                sqlSelect,
                tableInfo.getTableName());

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        return this.addSelectMappedStatementForTable(mapperClass, METHOD_NAME, sqlSource, tableInfo);
    }

}
