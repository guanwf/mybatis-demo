package com.obee.mybatis.config;

import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.obee.mybatis.support.SelectSqlListMethod;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.sf.jsqlparser.expression.Expression;
import java.util.List;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 20:02
 */
@Configuration
public class MybatisPlusConfig {

//    @Bean
    public DefaultSqlInjector sqlInjector() {
        return new DefaultSqlInjector() {
            @Override
            public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
                // 获取父类原有方法（BaseMapper 中的方法）
                List<AbstractMethod> methodList = super.getMethodList(mapperClass, tableInfo);
                // 添加我们的自定义方法
                methodList.add(new SelectSqlListMethod());
                return methodList;
            }
        };
    }

//    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加多租户插件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            // 1. 指定需要添加条件的列名
            @Override
            public String getTenantIdColumn() {
                return "tid";
            }

            // 2. 指定具体的条件值 (例如从 ThreadLocal/Session 中获取当前用户的 tid)
            @Override
            public Expression getTenantId() {
                // 假设当前 tid 为 2
                return new LongValue(2);
            }

            // 3. (可选) 指定哪些表不需要添加条件
            @Override
            public boolean ignoreTable(String tableName) {
                return "sys_admin".equalsIgnoreCase(tableName);
            }
        }));

        return interceptor;
    }

}
