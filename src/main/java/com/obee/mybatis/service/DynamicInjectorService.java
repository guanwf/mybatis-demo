package com.obee.mybatis.service;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.obee.mybatis.support.InsertEntity;
import com.obee.mybatis.support.SelectSqlList;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 21:31
 */
@Service
public class DynamicInjectorService {
    private final SqlSessionTemplate sqlSessionTemplate;
    // 缓存已注册的实体，防止重复注入报错
    private final Map<Class<?>, Boolean> injectedCache = new ConcurrentHashMap<>();

    public DynamicInjectorService(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    /**
     * 核心方法：传入 Entity，传入 SQL，直接查对象列表
     */
    public <T> List<T> selectSqlList(String sqlTemplate, Map<String, Object> parameter, Class<T> entityClass) {
        // 1. 关键：检查并执行动态注入
        // 如果这个实体没注册过，我们就现场给它造一个 Mapper 环境
        injectIfNeed(entityClass);

        // 2. 准备参数
        Map<String, Object> map = parameter == null ? new HashMap<>() : new HashMap<>(parameter);
        map.put("sql", sqlTemplate);

        // 3. 构造 Statement ID
        // 既然没有 Mapper 接口，我们就约定 Namespace 就是 Entity 的全类名
        String statementId = entityClass.getName() + "." + SelectSqlList.METHOD_NAME;

        // 4. 执行查询
        // 此时 MyBatis 里已经有了这个 ID，且 ResultMap 已经指向了 entityClass
        return sqlSessionTemplate.selectList(statementId, map);
    }

    /**
     * 运行时动态注入逻辑 (黑魔法)
     */
    private void injectIfNeed2(Class<?> entityClass) {
        if (injectedCache.containsKey(entityClass)) {
            return;
        }
        synchronized (this) {
            Configuration configuration = sqlSessionTemplate.getConfiguration();

            // A. 准备 Namespace
            // 我们没有 UserMapper 接口，所以直接用 Entity 类名作为 Namespace
            String currentNamespace = entityClass.getName();

            // B. 构造 Assistant
            // resource 参数只是为了报错时好定位，可以随便写一个字符串
            String resource = currentNamespace.replace('.', '/') + ".java (DynamicRuntime)";
            MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, resource);
            builderAssistant.setCurrentNamespace(currentNamespace);

            // C. 强制初始化 TableInfo (元数据)
            // 如果 TableInfo 已经存在（比如被其他方式加载过），initTableInfo 内部会跳过，是安全的
            TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
            if (tableInfo == null) {
                // 这里我们强行解析 User.class，建立表映射关系
                tableInfo = TableInfoHelper.initTableInfo(builderAssistant, entityClass);
            }
            Assert.notNull(tableInfo, "实体解析失败: " + entityClass.getName());

            // D. 检查是否已经注入过 (双重保险)
            // 防止 configuration.addMappedStatement 报 "already contains value" 异常
            String statementId = currentNamespace + "." + SelectSqlList.METHOD_NAME;
            if (configuration.hasStatement(statementId)) {
                injectedCache.put(entityClass, true);
                return;
            }

            // E. 核心：调用 AbstractMethod.inject()
            SelectSqlList method = new SelectSqlList();
            // 参数解释：
            // builderAssistant: 我们刚构造的，带着 Configuration 和 Namespace
            // mapperClass: 这里传 entityClass 充数，因为我们没有 Mapper 接口。
            //              MP 内部主要用它来获取泛型（这里不需要）或生成 ID（我们已经定好了 Namespace）。
            // modelClass:  真实的 Entity 类
            // tableInfo:   刚解析出来的表信息
            method.inject(builderAssistant, entityClass, entityClass, tableInfo);

            // F. 标记为已注册
            injectedCache.put(entityClass, true);
            System.out.println("动态注入成功: " + statementId);
        }

    }

    private void injectIfNeed(Class<?> entityClass) {
        if (injectedCache.containsKey(entityClass)) {
            return;
        }
        synchronized (this) {
            if (injectedCache.containsKey(entityClass)) {
                return;
            }

//            log.info(">>>>>> 动态注册 Mapper: {}", entityClass.getName());
            Configuration configuration = sqlSessionTemplate.getConfiguration();
            String namespace = entityClass.getName();

            // 构造 Assistant
            String resource = namespace.replace('.', '/') + ".java (Runtime)";
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, resource);
            assistant.setCurrentNamespace(namespace);

            // 解析元数据
            TableInfo tableInfo = TableInfoHelper.initTableInfo(assistant, entityClass);
            Assert.notNull(tableInfo, "实体解析失败: " + entityClass.getName());

            // 1. 注入查询方法 (SelectSqlList)
            if (!configuration.hasStatement(namespace + "." + SelectSqlList.METHOD_NAME)) {
                new SelectSqlList().inject(assistant, entityClass, entityClass, tableInfo);
            }

            // 2. 注入插入方法 (InsertEntity) <-- 新增
            if (!configuration.hasStatement(namespace + "." + InsertEntity.METHOD_NAME)) {
                new InsertEntity().inject(assistant, entityClass, entityClass, tableInfo);
            }

            injectedCache.put(entityClass, true);
        }
    }

    /**
     * 通用插入方法
     * @param entity 实体对象 (必须有 @TableName 注解)
     * @return 影响行数
     */
    public <T> int insert(T entity) {
        Class<?> entityClass = entity.getClass();

        // 1. 检查并注入
        injectIfNeed(entityClass);

        // 2. 构造 ID
        String statementId = entityClass.getName() + "." + InsertEntity.METHOD_NAME;

        // 3. 执行插入
        // MyBatis 会自动读取 entity 中的属性填充 #{property}
        return sqlSessionTemplate.insert(statementId, entity);
    }

}
