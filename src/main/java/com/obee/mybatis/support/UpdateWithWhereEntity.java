package com.obee.mybatis.support;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 17:19
 */
public class UpdateWithWhereEntity extends AbstractMethod {
    public static final String METHOD_NAME = "updateWithWhere";

    public UpdateWithWhereEntity() {
        super(METHOD_NAME);
    }



/*
    <script>UPDATE demo <set>
<if test="e['demoid'] != null">demoid=#{etdemoid},</if>
<if test="e['demoname'] != null">demoname=#{etdemoname},</if>
<if test="e['flag'] != null">flag=#{etflag},</if>
<if test="e['creater'] != null">creater=#{etcreater},</if>
<if test="e['createtime'] != null">createtime=#{etcreatetime},</if>
<if test="e['laster'] != null">laster=#{etlaster},</if>
<if test="e['lasttime'] != null">lasttime=#{etlasttime},</if>
<if test="e['remark'] != null">remark=#{etremark},</if>
</set> <where><foreach collection="params" index="key" item="val" separator=" AND ">${key} = #{val}</foreach></where></script>
    */

    /*
        <script>
                UPDATE demo
    <set>
      <if test="et.demoid != null">demoid=#{et.demoid},</if>
      <if test="et.demoname != null">demoname=#{et.demoname},</if>
      <if test="et.flag != null">flag=#{et.flag},</if>
      <if test="et.remark != null">remark=#{et.remark},</if>
    </set>
    <where>
      <foreach collection="params" index="key" item="val" separator=" AND ">
        ${key} = #{val}
      </foreach>
    </where>
    </script>

        */
//    @Override
    public MappedStatement injectMappedStatement2(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // 1. 生成 SET 语句
        // 关键点：传入 "et" 作为前缀。生成的 SQL 类似于: name = #{et.name}, age = #{et.age}
        // convertSet 会自动处理末尾逗号
        String sqlSet = SqlScriptUtils.convertSet(tableInfo.getAllSqlSet(true, "et"));

        // 2. 生成 WHERE 语句
        // 遍历 params Map。index 是 key (列名), item 是 value (值)
        // ${key} : 列名直接拼接 (注意安全)
        // #{val} : 值使用预编译参数
        String sqlWhere = "<foreach collection=\"params\" index=\"key\" item=\"val\" separator=\" AND \">"
                + "${key} = #{val}"
                + "</foreach>";

        // 3. 拼接完整 SQL
        // 格式: <script>UPDATE table SET ... <where> ... </where></script>
        // 使用 <where> 标签可以自动处理第一个 AND
        String sql = String.format("<script>UPDATE %s %s <where>%s</where></script>",
                tableInfo.getTableName(),
                sqlSet,
                sqlWhere);

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);

        return this.addUpdateMappedStatement(mapperClass, modelClass, METHOD_NAME, sqlSource);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        // =========================================================
        // 修复方案：手动拼接 SET 语句，不依赖 getAllSqlSet 方法
        // 目标格式：<if test="et.prop != null">col=#{et.prop},</if>
        // =========================================================

        StringBuilder setScript = new StringBuilder();
        setScript.append("<set>"); // 开启 set 标签，自动去除末尾逗号

        // 1. 遍历所有普通字段
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            // 排除逻辑删除字段（通常逻辑删除字段不应该被 update 更新，除非你特意需要）
            if (!fieldInfo.isLogicDelete()) {
                // 拼接 <if test="et.属性名 != null">列名=#{et.属性名},</if>
                setScript.append("<if test=\"et.").append(fieldInfo.getProperty()).append(" != null\">");
                setScript.append(fieldInfo.getColumn()).append("=#{et.").append(fieldInfo.getProperty()).append("},");
                setScript.append("</if>");
                setScript.append("\n"); // 加个换行，生成的 SQL 好看点
            }
        }

        // 2. 注意：通常不更新主键 ID，所以这里不把 ID 加入 SET 中
        // 如果你的业务允许修改 ID，需要在这里额外处理 tableInfo.getKeyProperty()

        setScript.append("</set>");
        String sqlSet = setScript.toString();


        // =========================================================
        // 生成 WHERE 语句 (保持不变)
        // =========================================================
        String sqlWhere = "<foreach collection=\"params\" index=\"key\" item=\"val\" separator=\" AND \">"
                + "${key} = #{val}"
                + "</foreach>";


        // =========================================================
        // 拼接完整 SQL
        // =========================================================
        String sql = String.format("<script>UPDATE %s %s <where>%s</where></script>",
                tableInfo.getTableName(),
                sqlSet,
                sqlWhere);

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);

        return this.addUpdateMappedStatement(mapperClass, modelClass, METHOD_NAME, sqlSource);
    }

}
