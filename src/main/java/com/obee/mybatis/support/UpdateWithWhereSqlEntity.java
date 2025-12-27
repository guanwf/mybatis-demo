package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 18:20
 */
public class UpdateWithWhereSqlEntity extends AbstractMethod {

    public static final String METHOD_NAME = "updateWithWhereSql";

    public UpdateWithWhereSqlEntity() {
        super(METHOD_NAME);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 1. 生成 SET 语句 (使用手动拼接，确保稳健性)
        // 格式: <if test="et.prop != null">col=#{et.prop},</if>
        StringBuilder setScript = new StringBuilder();
        setScript.append("<set>");
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            if (!fieldInfo.isLogicDelete()) {
                setScript.append("<if test=\"et.").append(fieldInfo.getProperty()).append(" != null\">");
                setScript.append(fieldInfo.getColumn()).append("=#{et.").append(fieldInfo.getProperty()).append("},");
                setScript.append("</if>\n");
            }
        }
        setScript.append("</set>");
        String sqlSet = setScript.toString();

        // 2. 生成 WHERE 语句
        // 直接接收 whereSql 字符串
        // <where> 标签会自动添加 WHERE 关键字
        // ${whereSql} 表示直接拼接字符串 (注意：调用方要注意 SQL 注入风险)
        String sqlWhere = "<where>${whereSql}</where>";

        // 3. 拼接完整 SQL
        String sql = String.format("<script>UPDATE %s %s %s</script>",
                tableInfo.getTableName(),
                sqlSet,
                sqlWhere);

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);

        return this.addUpdateMappedStatement(mapperClass, modelClass, METHOD_NAME, sqlSource);
    }
}
