package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 16:46
 */
public class UpdateByIdEntity extends AbstractMethod {

    public static final String METHOD_NAME = "updateById";

    public UpdateByIdEntity() {
        super(METHOD_NAME);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 1. 如果实体没有定义主键 (@TableId)，直接跳过，不注入
        // 这一步是底层的防御
        if (!tableInfo.havePK()) {
            return null;
        }

        // 2. 生成 SET 语句 (使用 convertSet 自动去除末尾逗号)
        // tableInfo.getAllSqlSet(true, null) 会生成 <if test="col!=null">col=#{col},</if>
        String sqlSet = SqlScriptUtils.convertSet(tableInfo.getAllSqlSet(true, null));

        // 3. 生成 WHERE 语句 (通过主键更新)
        String sqlWhere = tableInfo.getKeyColumn() + "= #{" + tableInfo.getKeyProperty() + "}";

        // 4. 拼接 SQL
        String sql = String.format("<script>UPDATE %s %s WHERE %s</script>",
                tableInfo.getTableName(),
                sqlSet,
                sqlWhere);

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);

        // 5. 注册
        return this.addUpdateMappedStatement(mapperClass, modelClass, METHOD_NAME, sqlSource);
    }
}
