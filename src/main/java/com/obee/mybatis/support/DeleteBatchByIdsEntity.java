package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @description:
 * @author: Guanwf
 * @date: 2026/1/31 16:41
 */


/**
 * 批量 ID 删除注入器
 * SQL: DELETE FROM table WHERE id IN (val1, val2, ...)
 */
public class DeleteBatchByIdsEntity extends AbstractMethod {
    public static final String METHOD_NAME = "deleteBatchByIds";

    public DeleteBatchByIdsEntity() { super(METHOD_NAME); }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        if (!tableInfo.havePK()) return null;

        // 生成 IN 语句
        // collection="list" 对应 Service 层传入的 ID 列表
        String sql = String.format("<script>DELETE FROM %s WHERE %s IN (<foreach collection=\"list\" item=\"id\" separator=\",\">#{id}</foreach>)</script>",
                tableInfo.getTableName(),
                tableInfo.getKeyColumn());

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        return this.addDeleteMappedStatement(mapperClass, METHOD_NAME, sqlSource);
    }

}