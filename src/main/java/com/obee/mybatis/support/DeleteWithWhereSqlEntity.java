package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 20:03
 */
public class DeleteWithWhereSqlEntity extends AbstractMethod {
    public static final String METHOD_NAME = "deleteWithWhereSql";

    public DeleteWithWhereSqlEntity() { super(METHOD_NAME); }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // DELETE FROM table WHERE ${whereSql}
        String sql = String.format("<script>DELETE FROM %s <where>${whereSql}</where></script>",
                tableInfo.getTableName());

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        return this.addDeleteMappedStatement(mapperClass, METHOD_NAME, sqlSource);
    }
}
