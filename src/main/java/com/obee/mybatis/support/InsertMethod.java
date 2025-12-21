package com.obee.mybatis.support;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 22:52
 *
 * SqlScriptUtils
 */
public class InsertMethod extends AbstractMethod{
    public static final String METHOD_NAME = "insertEntity";

/*
<script>
INSERT INTO sys_user
<trim prefix="(" suffix=")" suffixOverrides=",">
    <if test="id != null">id,</if>
    <if test="name != null">name,</if>
    <if test="age != null">age,</if>
</trim>
VALUES
<trim prefix="(" suffix=")" suffixOverrides=",">
    <if test="id != null">#{id},</if>
    <if test="name != null">#{name},</if>
    <if test="age != null">#{age},</if>
</trim>
</script>
*/

    public InsertMethod() {
        super(METHOD_NAME);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 1. 生成列名脚本: (id, name, age)
        // getAllInsertSqlColumnMaybeIf: 会自动生成 <if test="name != null">name,</if> 逻辑
        String columnScript = SqlScriptUtils.convertTrim(
                tableInfo.getKeyInsertSqlColumn(true, "", false) +
                        tableInfo.getAllInsertSqlColumnMaybeIf(null),
                "(", ")", null, ","
        );

        // 2. 生成值脚本: (#{id}, #{name}, #{age})
        String valuesScript = SqlScriptUtils.convertTrim(
                tableInfo.getKeyInsertSqlProperty(true, "", false) +
                        tableInfo.getAllInsertSqlPropertyMaybeIf(null),
                "(", ")", null, ","
        );

        // 3. 拼接完整 SQL
        // 格式: <script>INSERT INTO table_name (cols) VALUES (vals)</script>
        String sql = String.format("<script>INSERT INTO %s %s VALUES %s</script>",
                tableInfo.getTableName(),
                columnScript,
                valuesScript);

        // 4. 处理主键生成策略
        // 如果是 AUTO (自增)，需要使用 Jdbc3KeyGenerator 来回填 ID
        KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
        String keyProperty = null;
        String keyColumn = null;

        // 判断是否需要回填主键 (当类型为 AUTO 或者 INPUT 时)
        if (tableInfo.havePK()) {
            if (tableInfo.getIdType() == IdType.AUTO) {
                keyGenerator = Jdbc3KeyGenerator.INSTANCE;
                keyProperty = tableInfo.getKeyProperty();
                keyColumn = tableInfo.getKeyColumn();
            } else {
                // 如果是 ASSIGN_ID (雪花算法) 或 ASSIGN_UUID，MP 会在 insert 前自动填充，这里不需要 JDBC 回填
                if (null != tableInfo.getKeySequence()) {
                    keyGenerator = TableInfoHelper.genKeyGenerator(METHOD_NAME, tableInfo, builderAssistant);
                    keyProperty = tableInfo.getKeyProperty();
                    keyColumn = tableInfo.getKeyColumn();
                }
            }
        }

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);

        // 5. 注册
        return this.addInsertMappedStatement(mapperClass, modelClass, METHOD_NAME, sqlSource, keyGenerator, keyProperty, keyColumn);
    }
}
