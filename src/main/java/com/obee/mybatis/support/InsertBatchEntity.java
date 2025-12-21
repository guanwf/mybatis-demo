package com.obee.mybatis.support;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import java.util.List;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/21 21:18
 */
public class InsertBatchEntity extends AbstractMethod{
    public static final String METHOD_NAME = "insertBatch";

    public InsertBatchEntity() {
        super(METHOD_NAME);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
        String keyProperty = null;
        String keyColumn = null;

        // 1. 准备列名和属性名构建器
        StringBuilder columnScript = new StringBuilder();
        StringBuilder valueScript = new StringBuilder();

        // 2. 处理主键 (如果不是自增，则需要插入主键列)
        if (tableInfo.havePK()) {
            if (tableInfo.getIdType() == IdType.AUTO) {
                // 自增主键，SQL 中不写列名，但在 Mybatis 配置中开启回填
                keyGenerator = Jdbc3KeyGenerator.INSTANCE;
                keyProperty = tableInfo.getKeyProperty();
                keyColumn = tableInfo.getKeyColumn();
            } else {
                // 非自增（如 input, assign_id），需要插入主键列
                columnScript.append(tableInfo.getKeyColumn()).append(",");
                valueScript.append("#{item.").append(tableInfo.getKeyProperty()).append("},");
            }
        }

        // 3. 处理普通字段
        List<TableFieldInfo> fieldList = tableInfo.getFieldList();
        for (TableFieldInfo field : fieldList) {
            columnScript.append(field.getColumn()).append(",");
            valueScript.append("#{item.").append(field.getProperty()).append("},");
        }

        // 4. 移除最后一个逗号
        String cols = columnScript.substring(0, columnScript.length() - 1);
        String vals = valueScript.substring(0, valueScript.length() - 1);

        // 5. 拼接最终 SQL
        // 格式: INSERT INTO table (col1, col2) VALUES
        // <foreach collection="list" item="item" separator=",">
        //   (#{item.col1}, #{item.col2})
        // </foreach>
        String sql = String.format(
                "<script>INSERT INTO %s (%s) VALUES <foreach collection=\"list\" item=\"item\" separator=\",\">(%s)</foreach></script>",
                tableInfo.getTableName(),
                cols,
                vals
        );

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);

        // 6. 注册方法
        return this.addInsertMappedStatement(mapperClass, modelClass, METHOD_NAME, sqlSource, keyGenerator, keyProperty, keyColumn);
    }
}
