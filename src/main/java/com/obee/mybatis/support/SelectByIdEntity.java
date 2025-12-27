package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 21:23
 */
public class SelectByIdEntity extends AbstractMethod {
    public static final String METHOD_NAME = "selectByIdEntity";

    public SelectByIdEntity() {
        super(METHOD_NAME);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        if (!tableInfo.havePK()) return null;

        // SELECT col1, col2... FROM table WHERE id = #{id}
        String sql = String.format("<script>SELECT %s FROM %s WHERE %s=#{%s}</script>",
                sqlSelectColumns(tableInfo, false),
                tableInfo.getTableName(),
                tableInfo.getKeyColumn(),
                tableInfo.getKeyProperty());

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        // addSelectMappedStatementForTable 会自动关联 tableInfo 中的 ResultMap
        return this.addSelectMappedStatementForTable(mapperClass, METHOD_NAME, sqlSource, tableInfo);
    }
}
