package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 21:24
 */
public class SelectByMapEntity extends AbstractMethod {

    public static final String METHOD_NAME = "selectByMapEntity";

    public SelectByMapEntity() {
        super(METHOD_NAME);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // SELECT col... FROM table
        String sqlSelect = sqlSelectColumns(tableInfo, false);

        // WHERE key = #{val}
        String sqlWhere = "<foreach collection=\"params\" index=\"key\" item=\"val\" separator=\" AND \">"
                + "${key} = #{val}"
                + "</foreach>";

        String sql = String.format("<script>SELECT %s FROM %s <where>%s</where></script>",
                sqlSelect,
                tableInfo.getTableName(),
                sqlWhere);

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        return this.addSelectMappedStatementForTable(mapperClass, METHOD_NAME, sqlSource, tableInfo);
    }
}
