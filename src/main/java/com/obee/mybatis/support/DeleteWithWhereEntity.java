package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 20:02
 */
public class DeleteWithWhereEntity extends AbstractMethod {
    public static final String METHOD_NAME = "deleteWithWhere";

    public DeleteWithWhereEntity() { super(METHOD_NAME); }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 生成 WHERE 语句: key = #{val}
        String sqlWhere = "<foreach collection=\"params\" index=\"key\" item=\"val\" separator=\" AND \">"
                + "${key} = #{val}"
                + "</foreach>";

        // DELETE FROM table <where>...</where>
        String sql = String.format("<script>DELETE FROM %s <where>%s</where></script>",
                tableInfo.getTableName(),
                sqlWhere);

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        return this.addDeleteMappedStatement(mapperClass, METHOD_NAME, sqlSource);
    }
}
