package com.obee.mybatis.service;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.obee.mybatis.support.InsertBatchEntity;
import com.obee.mybatis.support.InsertMethod;
import com.obee.mybatis.support.SelectSqlListMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.ibatis.session.SqlSession;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 21:31
 */
@Service
@Slf4j
public class DynamicInjectorService {
    private final SqlSessionTemplate sqlSessionTemplate;

    private final SqlSessionFactory sqlSessionFactory; // <--- 新增注入

    // 缓存已注册的实体，防止重复注入报错
    private final Map<Class<?>, Boolean> injectedCache = new ConcurrentHashMap<>();

    // 建议分片大小：500-1000
    private static final int BATCH_SIZE = 1000;

    public DynamicInjectorService(SqlSessionTemplate sqlSessionTemplate,SqlSessionFactory  sqlSessionFactory) {
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.sqlSessionFactory=sqlSessionFactory;
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
        String statementId = entityClass.getName() + "." + SelectSqlListMethod.METHOD_NAME;

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
            String statementId = currentNamespace + "." + SelectSqlListMethod.METHOD_NAME;
            if (configuration.hasStatement(statementId)) {
                injectedCache.put(entityClass, true);
                return;
            }

            // E. 核心：调用 AbstractMethod.inject()
            SelectSqlListMethod method = new SelectSqlListMethod();
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
            if (!configuration.hasStatement(namespace + "." + SelectSqlListMethod.METHOD_NAME)) {
                new SelectSqlListMethod().inject(assistant, entityClass, entityClass, tableInfo);
            }

            // 2. 注入插入方法 (InsertEntity) <-- 新增
            if (!configuration.hasStatement(namespace + "." + InsertMethod.METHOD_NAME)) {
                new InsertMethod().inject(assistant, entityClass, entityClass, tableInfo);
            }

            // 3. 注入 InsertBatchEntity <--- 新增
            if (!configuration.hasStatement(namespace + "." + InsertBatchEntity.METHOD_NAME)) {
                new InsertBatchEntity().inject(assistant, entityClass, entityClass, tableInfo);
            }

            injectedCache.put(entityClass, true);
        }
    }

    /**
     * 通用插入方法
     *
     * @param entity 实体对象 (必须有 @TableName 注解)
     * @return 影响行数
     */
    public <T> int insert(T entity) {
        Class<?> entityClass = entity.getClass();

        // 1. 检查并注入
        injectIfNeed(entityClass);

        // 2. 构造 ID
        String statementId = entityClass.getName() + "." + InsertMethod.METHOD_NAME;

        // 3. 执行插入
        // MyBatis 会自动读取 entity 中的属性填充 #{property}
        return sqlSessionTemplate.insert(statementId, entity);
    }

    /**
     * 批量插入 (带分片处理，性能最优)
     *
     * @param entityList 实体列表
     * @return 成功插入的总条数
     */
    @Transactional(rollbackFor = Exception.class) // 开启事务，保证要么全成，要么全败
    public <T> int insertBatch(List<T> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return 0;
        }

        Class<?> entityClass = entityList.get(0).getClass();

        // 1. 检查并注入
        injectIfNeed(entityClass);

        String statementId = entityClass.getName() + "." + InsertBatchEntity.METHOD_NAME;
        int totalRows = 0;

        // 2. 分片处理 (Performance Consideration)
        // 避免一次性拼接过长的 SQL 导致 MySQL 报错 "Packet for query is too large"
        int size = entityList.size();
        for (int i = 0; i < size; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, size);
            List<T> subList = entityList.subList(i, end);

            // 执行分片插入
            // 注意：参数必须包装在一个 Map 或者 Collection 中，MyBatis 才能识别 collection="list"
            // SqlSessionTemplate.insert 默认如果传 List，会自动把参数名设为 "list"
            totalRows += sqlSessionTemplate.insert(statementId, subList);

//            log.debug("Batch insert progress: {}/{}", end, size);
        }

        return totalRows;
    }

    /**
     * JDBC 原生批量插入 (Oracle/全数据库兼容, 高性能)
     * 原理: 开启 ExecutorType.BATCH，复用预编译语句
     *
     * INSERT INTO sys_user (id, name) VALUES (?, ?)。
     *
     * @param entityList 实体列表
     * @return 成功提交的批次数量（注意：JDBC Batch 模式下通常很难精确返回具体的行数，通常返回批次执行结果）
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> void insertBatchJdbc(List<T> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return;
        }

        Class<?> entityClass = entityList.get(0).getClass();

        // 1. 确保"单条插入"的 MappedStatement 已经注入
        // JDBC Batch 复用的就是单条插入的 SQL
        injectIfNeed(entityClass);

        String statementId = entityClass.getName() + "." + InsertBatchEntity.METHOD_NAME;

        // 2. 开启批量 Session
        // openSession(ExecutorType.BATCH, false) -> false 表示不自动提交事务
        try (SqlSession batchSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            int size = entityList.size();
            for (int i = 0; i < size; i++) {
                // 3. 循环调用插入
                // 注意：这里看起来是循环，但因为是 BATCH 模式，MyBatis 并不会每条都发给数据库
                // 它会把参数暂存在本地，等待 flushStatements
                batchSession.insert(statementId, entityList.get(i));

                // 4. 分批提交 (防止内存溢出)
                if ((i + 1) % BATCH_SIZE == 0 || i == size - 1) {
                    batchSession.flushStatements();
                    // 对于非 Spring 管理的 session，需要手动 commit 确保数据落盘
                    // 但通常建议只 flush，最后统一 commit，或者配合外层 @Transactional
                }
            }

            // 5. 提交事务
            batchSession.commit();
            // 清理缓存
            batchSession.clearCache();
        } catch (Exception e) {
//            log.error("批量插入失败", e);
            throw new RuntimeException("批量插入失败", e);
        }
    }


}
