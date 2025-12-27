package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 19:59
 */
public class DeleteByIdEntity extends AbstractMethod {
    public static final String METHOD_NAME = "deleteByIdEntity";

    public DeleteByIdEntity() { super(METHOD_NAME); }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 1. 必须有主键
        if (!tableInfo.havePK()) return null;

        // 2. 生成 SQL: DELETE FROM table WHERE id = #{id}
        String sql = String.format("<script>DELETE FROM %s WHERE %s = #{%s}</script>",
                tableInfo.getTableName(),
                tableInfo.getKeyColumn(),
                tableInfo.getKeyProperty());

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        return this.addDeleteMappedStatement(mapperClass, METHOD_NAME, sqlSource);
    }

}
