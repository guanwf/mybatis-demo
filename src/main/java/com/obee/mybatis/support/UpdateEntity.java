package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 15:23
 *
 * 暂时没使用
 *
 */
@Deprecated
public class UpdateEntity extends AbstractMethod {
    public static final String METHOD_NAME = "updateEntity";

    public UpdateEntity() {
        super(METHOD_NAME);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 1. 检查是否有主键，没有主键无法通过 Bean 更新
        if (!tableInfo.havePK()) {
            return null;
        }

        // 2. 生成 SET 语句脚本
        // getAllSqlSet: MP 提供的工具，会自动生成 <if test="name!=null">name=#{name},</if>
        // true: 表示忽略逻辑删除字段
        // null: 前缀
//        String sqlSet = "SET " + tableInfo.getAllSqlSet(true, null);

        // 2. 核心修复点：使用 convertSet 包裹
        // tableInfo.getAllSqlSet(true, null): 生成 <if test="name!=null">name=#{name},</if>...
        // SqlScriptUtils.convertSet(...): 会在外面加上 <set>...</set> 标签
        // <set> 标签的作用就是：如果内容以逗号结尾，自动去除逗号
        String sqlSet = SqlScriptUtils.convertSet(tableInfo.getAllSqlSet(true, null));

        // 3. 生成 WHERE 语句脚本 (根据主键更新)
        // 格式: id = #{id}
        String sqlWhere = tableInfo.getKeyColumn() + "= #{" + tableInfo.getKeyProperty() + "}";

//        UPDATE("update", "根据 whereEntity 条件，更新记录", "<script>\nUPDATE %s %s %s %s\n</script>"),
        // 4. 拼接完整 SQL
        // UPDATE table_name SET ... WHERE id = ...
        String sql = String.format("<script>UPDATE %s %s WHERE %s</script>",
                tableInfo.getTableName(),
                sqlSet,
                sqlWhere);

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);

        // 5. 注册
        return this.addUpdateMappedStatement(mapperClass, modelClass, METHOD_NAME, sqlSource);
    }

//    SqlMethod sqlMethod = SqlMethod.UPDATE;
//    String sql = String.format(sqlMethod.getSql(), tableInfo.getTableName(), this.sqlSet(true, true, tableInfo, true, "et", "et."), this.sqlWhereEntityWrapper(true, tableInfo), this.sqlComment());
//    SqlSource sqlSource = super.createSqlSource(this.configuration, sql, modelClass);
//    return this.addUpdateMappedStatement(mapperClass, modelClass, this.methodName, sqlSource);

}
