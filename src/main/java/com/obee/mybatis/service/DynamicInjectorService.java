package com.obee.mybatis.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.obee.mybatis.model.GlobalDummy;
import com.obee.mybatis.model.PageRequest;
import com.obee.mybatis.model.PageResult;
import com.obee.mybatis.support.*;
import com.obee.mybatis.utils.SqlSafeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.session.SqlSession;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 21:31
 */
@Service
@Slf4j
public class DynamicInjectorService {

    @Autowired
    private ObjectMapper objectMapper; // 需注入 Jackson

    private final SqlSessionTemplate sqlSessionTemplate;

    private final SqlSessionFactory sqlSessionFactory; // <--- 新增注入

    // 缓存已注册的实体，防止重复注入报错
    private final Map<Class<?>, Boolean> injectedCache = new ConcurrentHashMap<>();

    // 建议分片大小：500-1000
    private static final int BATCH_SIZE = 1000;

    // 标记全局方法是否已注册
    private volatile boolean isGlobalInjected = false;

    public DynamicInjectorService(SqlSessionTemplate sqlSessionTemplate, SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    // =========================================================
    // 1. 根据 ID 查询 (返回单条)
    // =========================================================
    public <T> T selectById(Class<T> entityClass, Object id) {
        Assert.notNull(entityClass, "实体类型不能为空");
        Assert.notNull(id, "ID不能为空");

        injectIfNeed(entityClass);

        String statementId = entityClass.getName() + "." + SelectByIdEntity.METHOD_NAME;
        log.debug("[Select]ById:Entity={},ID={}", entityClass.getSimpleName(), id);

        return sqlSessionTemplate.selectOne(statementId, id);
    }

    // =========================================================
    // 2. 根据 Map 条件查询 (List)
    // =========================================================
    public <T> List<T> selectList(Class<T> entityClass, Map<String, Object> params) {
        return executeSelectByMap(entityClass, params);
    }

    // 3. 根据 Map 条件查询 (One)
    public <T> T selectOne(Class<T> entityClass, Map<String, Object> params) {
        List<T> list = executeSelectByMap(entityClass, params);
        return handleSelectOne(list);
    }

    private <T> List<T> executeSelectByMap(Class<T> entityClass, Map<String, Object> params) {
        Assert.notNull(entityClass, "实体类型不能为空");
        injectIfNeed(entityClass);

        Map<String, Object> context = new HashMap<>();
        context.put("params", params); // 对应 collection="params"

        String statementId = entityClass.getName() + "." + SelectByMapEntity.METHOD_NAME;
        log.debug("[Select]ByMap:Entity={},Params={}", entityClass.getSimpleName(), params);

        return sqlSessionTemplate.selectList(statementId, context);
    }

    // =========================================================
    // 4. 根据 WhereSql 自定义条件查询 (List)
    // =========================================================
    public <T> List<T> selectList(Class<T> entityClass, String whereSql) {
        return executeSelectByWhereSql(entityClass, whereSql);
    }

    // 5. 根据 WhereSql 自定义条件查询 (One)
    public <T> T selectOne(Class<T> entityClass, String whereSql) {
        List<T> list = executeSelectByWhereSql(entityClass, whereSql);
        return handleSelectOne(list);
    }

    /**
     * 单条+纯SQL
     *
     * @param mySQL
     * @param params
     * @param <T>
     * @return
     */
    public <T> T selectOneSQL(Class<T> entityClass, String mySQL, Map<String, Object> params) {
        List<T> list = executeSelectBySql(entityClass, mySQL, params);
        return handleSelectOne(list);
    }

    private <T> List<T> executeSelectByWhereSql(Class<T> entityClass, String whereSql) {
        Assert.notNull(entityClass, "实体类型不能为空");
        // 安全检查
        SqlSafeUtil.checkWhereClause(whereSql);

        injectIfNeed(entityClass);

        Map<String, Object> context = new HashMap<>();
        context.put("whereSql", whereSql);

        String statementId = entityClass.getName() + "." + SelectByWhereSqlEntity.METHOD_NAME;
        log.debug("[Select] ByWhereSql: Entity={}, Where={}", entityClass.getSimpleName(), whereSql);

        return sqlSessionTemplate.selectList(statementId, context);
    }

    // =========================================================
    // 6. 根据 原生 SQL 查询 (List)
    // =========================================================
    public <T> List<T> selectListBySql(Class<T> entityClass, String sql, Map<String, Object> params) {
        return executeSelectBySql(entityClass, sql, params);
    }

    public <T> List<Map> selectListBySql(String sql, Map<String, Object> params) {
        return executeSelectBySql(sql, params);
    }


    // 7. 根据 原生 SQL 查询 (One)
    public <T> T selectOneBySql(Class<T> entityClass, String sql, Map<String, Object> params) {
        List<T> list = executeSelectBySql(entityClass, sql, params);
        return handleSelectOne(list);
    }

    public Map selectOneSQL(String sql, Map<String, Object> params) {
        List<Map> list = executeSelectBySql(sql, params);
        return handleSelectOne(list);
    }

    /**
     * 根据sql查询结果转换成targetClass的类
     * @param targetClass
     * @param mySQL
     * @param params
     * @return
     * @param <T>
     */
    public <T> T selectOneBySqlWithConvert(Class<T> targetClass, String mySQL, Map<String, Object> params) {
        // 1. 调用全局 Map 查询 (SelectPageBySqlGlobal 或类似的全局方法)
        // 这里假设你有一个返回 List<Map> 的底层方法
        List<Map<String, Object>> mapList = executeSelectBySql(mySQL, params);

        Map<String, Object> resultMap = handleSelectOne(mapList);
        if (resultMap == null) {
            return null;
        }

        // 2. 手动转换: Map -> Bean
        return objectMapper.convertValue(resultMap, targetClass);
    }

    /**
     * 根据sql查询结果转换成targetClass的类
     * 先查 Map，再转 Bean
     *
     * @param targetClass
     * @param mySQL
     * @param params
     * @param <T>
     * @return
     */
    public <T> List<T> selectListBySqlWithConvert(Class<T> targetClass, String mySQL, Map<String, Object> params) {

        // 1. 调用全局 Map 查询 (SelectPageBySqlGlobal 或类似的全局方法)
        // 这里假设你有一个返回 List<Map> 的底层方法
        List<Map<String, Object>> mapList = executeSelectBySql(mySQL, params);

        Map<String, Object> resultMap = handleSelectOne(mapList);
        if (resultMap == null) {
            return null;
        }

        // 2. 手动转换: Map -> Bean
//        return objectMapper.convertValue(resultMap, targetClass);
        return convertMapListToBeanList(mapList, targetClass);
    }

    /**
     * 辅助方法：List<Map> -> List<Bean>
     */
    private <T> List<T> convertMapListToBeanList(List<Map<String, Object>> rawList, Class<T> clazz) {
        if (rawList == null || rawList.isEmpty()) {
            return new ArrayList<>();
        }

        List<T> resultList = new ArrayList<>(rawList.size());
        for (Map<String, Object> map : rawList) {
            try {
                // 利用 Jackson 的 convertValue 强转
                // 它能自动处理 int -> Long, String -> Date 等类型转换
                T bean = objectMapper.convertValue(map, clazz);
                resultList.add(bean);
            } catch (Exception e) {
                log.error("Map 转 Bean 失败: {}", e.getMessage());
                throw new RuntimeException("数据转换异常", e);
            }
        }
        return resultList;
    }

    /**
     * Bean+SQL
     *
     * @param entityClass
     * @param sql
     * @param params
     * @param <T>
     * @return
     */
    private <T> List<T> executeSelectBySql(Class<T> entityClass, String sql, Map<String, Object> params) {
        Assert.notNull(entityClass, "实体类型不能为空");
        // 安全检查
        SqlSafeUtil.checkSelectSql(sql);

        injectIfNeed(entityClass);

        Map<String, Object> context = params == null ? new HashMap<>() : new HashMap<>(params);
        context.put("sql", sql);

        String statementId = entityClass.getName() + "." + SelectBySqlEntity.METHOD_NAME;
        log.debug("[Select]BySql:Entity={},SQL={}", entityClass.getSimpleName(), sql);

        return sqlSessionTemplate.selectList(statementId, context);
    }


    /**
     * 纯SQL
     *
     * @param sql
     * @param params
     * @param <T>
     * @return
     */
    private <T> List<T> executeSelectBySql(String sql, Map<String, Object> params) {
        // 安全检查
        SqlSafeUtil.checkSelectSql(sql);

        injectGlobalMapperIfNeed();

        Map<String, Object> context = params == null ? new HashMap<>() : new HashMap<>(params);
        context.put("sql", sql);

        String statementId = GlobalDummy.class.getName() + "." + SelectBySql.METHOD_NAME;

        return sqlSessionTemplate.selectList(statementId, context);
    }

    // =========================================================
    // 辅助: 处理单条记录返回
    // =========================================================
    private <T> T handleSelectOne(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            log.warn("selectOne查询到了多条数据，仅返回第一条.Size={}", list.size());
            // 或者抛出异常: throw new TooManyResultsException("Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
        }
        return list.get(0);
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
     * 分页查询 - 完全通过 SQL
     *
     * @param entityClass
     * @param req
     * @param sql
     * @param params
     * @param <T>
     * @return
     */
    public <T> PageResult<T> selectPageBySql(Class<T> entityClass, PageRequest req, String sql, Map<String, Object> params) {
        Assert.notNull(entityClass, "实体类型不能为空");

        SqlSafeUtil.checkSelectSql(sql);

        injectIfNeed(entityClass);

        // 1. 转换入参
        IPage<T> mpPage = toMpPage(req);

        // 2. 构造 Context
        Map<String, Object> context = params == null ? new HashMap<>() : new HashMap<>(params);
        context.put("sql", sql);
        context.put("page", mpPage); // MP 插件识别点

        // 3. 执行
        String statementId = entityClass.getName() + "." + SelectBySqlEntity.METHOD_NAME;
        List<T> list = sqlSessionTemplate.selectList(statementId, context);

        mpPage.setRecords(list);

        // 4. 转换出参
        return toPageResult(mpPage);
    }

    public <T> PageResult<T> selectPageBySql(String sql, PageRequest req) {
        return this.selectPageBySql(sql, req, null);
    }

    public <T> PageResult<T> selectPageBySql(String sql, PageRequest req, Map<String, Object> params) {

        SqlSafeUtil.checkSelectSql(sql);

        injectGlobalMapperIfNeed();

        // 1. 转换入参
        IPage<T> mpPage = toMpPage(req);

        // 2. 构造 Context
        Map<String, Object> context = params == null ? new HashMap<>() : new HashMap<>(params);
        context.put("sql", sql);
        context.put("page", mpPage); // MP 插件识别点

        // 3. 执行
        String statementId = GlobalDummy.class.getName() + "." + SelectBySql.METHOD_NAME;
        List<T> list = sqlSessionTemplate.selectList(statementId, context);

        mpPage.setRecords(list);

        // 4. 转换出参
        return toPageResult(mpPage);
    }

    private <T> PageResult<T> executeSelectPageByMap(Class<T> entityClass, PageRequest req, Map<String, Object> params) {
        // 1. 转换入参 (PageRequest -> MP Page)
        IPage<T> mpPage = toMpPage(req);

        // 2. 构造 Context
        Map<String, Object> context = new HashMap<>();
        if (params != null && !params.isEmpty()) {
            context.put("params", params); // 对应 XML collection="params"
        }
        context.put("page", mpPage); // MP 插件识别点

        // 3. 执行
        String statementId = entityClass.getName() + "." + SelectByMapEntity.METHOD_NAME;
        List<T> list = sqlSessionTemplate.selectList(statementId, context);

        mpPage.setRecords(list);
        // 4. 转换出参 (MP Page -> PageResult)
        // MP 会自动把结果 list 塞回 mpPage 中，并计算 total
        return toPageResult(mpPage);
    }

    /**
     * 分页查询 - 通过 Map (Key为数据库列名)
     *
     * @param entityClass
     * @param pageRequest
     * @param params
     * @param <T>
     * @return
     */
    public <T> PageResult<T> selectPage(Class<T> entityClass, PageRequest pageRequest, Map<String, Object> params) {
        Assert.notNull(entityClass, "实体类型不能为空");
        injectIfNeed(entityClass);
        return executeSelectPageByMap(entityClass, pageRequest, params);
    }

    /**
     * 分页查询 - 通过 Bean (自动提取非空字段作为条件)
     *
     * @param entity
     * @param pageRequest
     * @param <T>
     * @return
     */
    public <T> PageResult<T> selectPage(T entity, PageRequest pageRequest) {
        Assert.notNull(entity, "查询实体不能为空");
        Class<?> entityClass = entity.getClass();

        // 1. 确保注入
        injectIfNeed(entityClass);

        // 2. 将 Bean 转为 Map<列名, 值>
        Map<String, Object> columnMap = convertBeanToColumnMap(entity);

        // 3. 复用 ByMap 的逻辑
        return executeSelectPageByMap((Class<T>) entityClass, pageRequest, columnMap);
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

            // 3. 注入 UpdateBySql (直接SQL) <-- 新增
            if (!configuration.hasStatement(namespace + "." + UpdateBySql.METHOD_NAME)) {
                new UpdateBySql().inject(assistant, entityClass, entityClass, tableInfo);
            }

            // 4. 注入 UpdateWithWhereEntity (Bean更新) <-- 新增
            if (!configuration.hasStatement(namespace + "." + UpdateWithWhereEntity.METHOD_NAME)) {
                // 注意：如果没有主键，这个方法内部会返回 null，不会注册成功，要容错
                try {
                    new UpdateWithWhereEntity().inject(assistant, entityClass, entityClass, tableInfo);
                } catch (Exception e) {
                    log.warn("UpdateEntity 注入失败 (可能缺少主键): {}", entityClass.getName());
                }
            }

            if (!configuration.hasStatement(namespace + "." + UpdateByIdEntity.METHOD_NAME)) {
                try {
                    new UpdateByIdEntity().inject(assistant, entityClass, entityClass, tableInfo);
                } catch (Exception e) {
                    log.warn("UpdateEntity 注入失败 (可能缺少主键): {}", entityClass.getName());
                }
            }

            // 注入 UpdateWithWhereSqlEntity <--- 新增
            if (!configuration.hasStatement(namespace + "." + UpdateWithWhereSqlEntity.METHOD_NAME)) {
                new UpdateWithWhereSqlEntity().inject(assistant, entityClass, entityClass, tableInfo);
            }

            // 1. DeleteByIdEntity
            if (!configuration.hasStatement(namespace + "." + DeleteByIdEntity.METHOD_NAME)) {
                new DeleteByIdEntity().inject(assistant, entityClass, entityClass, tableInfo);
            }
            // 2. DeleteWithWhereEntity
            if (!configuration.hasStatement(namespace + "." + DeleteWithWhereEntity.METHOD_NAME)) {
                new DeleteWithWhereEntity().inject(assistant, entityClass, entityClass, tableInfo);
            }
            // 3. DeleteWithWhereSqlEntity
            if (!configuration.hasStatement(namespace + "." + DeleteWithWhereSqlEntity.METHOD_NAME)) {
                new DeleteWithWhereSqlEntity().inject(assistant, entityClass, entityClass, tableInfo);
            }


            // 1. SelectByIdEntity
            if (!configuration.hasStatement(namespace + "." + SelectByIdEntity.METHOD_NAME)) {
                new SelectByIdEntity().inject(assistant, entityClass, entityClass, tableInfo);
            }
            // 2. SelectByMapEntity
            if (!configuration.hasStatement(namespace + "." + SelectByMapEntity.METHOD_NAME)) {
                new SelectByMapEntity().inject(assistant, entityClass, entityClass, tableInfo);
            }
            // 3. SelectByWhereSqlEntity
            if (!configuration.hasStatement(namespace + "." + SelectByWhereSqlEntity.METHOD_NAME)) {
                new SelectByWhereSqlEntity().inject(assistant, entityClass, entityClass, tableInfo);
            }
            // 4. SelectBySqlEntity
            if (!configuration.hasStatement(namespace + "." + SelectBySqlEntity.METHOD_NAME)) {
                new SelectBySqlEntity().inject(assistant, entityClass, entityClass, tableInfo);
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

    @Transactional(rollbackFor = Exception.class)
    public int insertBySql(String sql) {
        // 安全检查
        SqlSafeUtil.checkInsertSql(sql);

        injectGlobalMapperIfNeed();

        Map<String, Object> map = new HashMap<>();
        map.put("sql", sql);

        log.info("[Insert]原生SQL插入:SQL={}", sql, map);

        String statementId = GlobalDummy.class.getName() + "." + InsertBySql.METHOD_NAME;
        return sqlSessionTemplate.insert(statementId, map);
    }

    /**
     * 根据参数值
     *
     * @param sql
     * @param params
     * @return
     */
    public int insertBySql(String sql, Map<String, Object> params) {
        // 安全检查
        SqlSafeUtil.checkInsertSql(sql);

        injectGlobalMapperIfNeed();

        Map<String, Object> map = params == null ? new HashMap<>() : new HashMap<>(params);
        map.put("sql", sql);

        log.info("[Insert]原生SQL插入:SQL={},Params={}", sql, params);

        String statementId = GlobalDummy.class.getName() + "." + InsertBySql.METHOD_NAME;
        return sqlSessionTemplate.insert(statementId, map);
    }

    /**
     * 批量插入 (带分片处理，性能最优)
     *
     * @param entityList 实体列表
     * @return 成功插入的总条数
     */
    @Transactional(rollbackFor = Exception.class) // 开启事务，保证要么全成，要么全败
    public <T> int insert(List<T> entityList) {
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

            log.debug("Batch insert progress: {}/{}", end, size);
        }

        return totalRows;
    }

    /**
     * JDBC 原生批量插入 (Oracle/全数据库兼容, 高性能)
     * 原理: 开启 ExecutorType.BATCH，复用预编译语句
     * <p>
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

    /**
     * 执行原生 Update/Delete SQL
     *
     * @param sqlTemplate SQL 语句 (支持 #{param})
     * @param params      参数
     * @param entityClass 任意实体类 (用于挂载 Mapper)
     * @return 影响行数
     */
//    @Transactional(rollbackFor = Exception.class)
//    public int updateBySql(String sqlTemplate, Map<String, Object> params, Class<?> entityClass) {
//        injectIfNeed(entityClass);
//
//        Map<String, Object> map = params == null ? new HashMap<>() : new HashMap<>(params);
//        map.put("sql", sqlTemplate);
//
//        String statementId = entityClass.getName() + "." + UpdateBySql.METHOD_NAME;
//        return sqlSessionTemplate.update(statementId, map);
//    }

    /**
     * 根据 ID 更新实体 (只更新非空字段)
     *
     * @param entity 实体对象 (必须包含主键值)
     * @return 影响行数
     */
//    @Transactional(rollbackFor = Exception.class)
//    public <T> int updateById(T entity) {
//        Assert.notNull(entity, "Entity cannot be null");
//        Class<?> entityClass = entity.getClass();
//
//        injectIfNeed(entityClass);
//
//        String statementId = entityClass.getName() + "." + UpdateEntity.METHOD_NAME;
//
//        // MyBatis 会自动提取 entity 中的主键作为 WHERE 条件
//        // 提取 entity 中的非空字段作为 SET 内容
//
//        return sqlSessionTemplate.update(statementId, entity);
//    }

    /**
     * 自定义条件更新
     *
     * @param entity 用于生成 SET 语句 (非空字段会被更新)
     * @param params 用于生成 WHERE 语句 (key为列名, value为值)
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> int update(T entity, Map<String, Object> params) {
        Assert.notNull(entity, "更新实体 entity 不能为空");
        Class<?> entityClass = entity.getClass();

        // 1. 安全检查: 禁止无条件更新 (防止全表更新事故)
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("必须指定params更新条件，禁止全表更新！");
        }

        // 2. 确保已注入
        injectIfNeed(entityClass);

        // 3. 构造参数上下文
        // 因为 Mapper 接收到了两个对象，我们需要把它们包起来
        Map<String, Object> context = new HashMap<>();
        context.put("et", entity);    // 对应 AbstractMethod 里的 "et"
        context.put("params", params); // 对应 AbstractMethod 里的 collection="params"

        String statementId = entityClass.getName() + "." + UpdateWithWhereEntity.METHOD_NAME;

        // 4. 执行更新
        return sqlSessionTemplate.update(statementId, context);
    }

    /**
     * 注入全局通用的 MappedStatement
     */
    private void injectGlobalMapperIfNeed() {
        if (isGlobalInjected) return;

        synchronized (this) {
            if (isGlobalInjected) return;

            Class<?> entityClass = GlobalDummy.class;
            String namespace = entityClass.getName();
            Configuration configuration = sqlSessionTemplate.getConfiguration();

            // 只有当容器里没有这个方法时才注入
            if (!configuration.hasStatement(namespace + "." + UpdateBySql.METHOD_NAME)) {
                log.info(">>>>>> 初始化全局 SQL 执行器: {}", namespace);

                MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace.replace('.', '/') + ".java (Global)");
                assistant.setCurrentNamespace(namespace);

                // UpdateBySql 不需要 TableInfo (因为它直接执行 ${sql})，所以 TableInfo 传 null 即可
                // 如果你的 UpdateBySql 实现里用到了 tableInfo (比如 getTableName)，这里就需要想办法
                // 但根据上一轮的 UpdateBySql 实现，它只用了 ${sql}，所以传 null 是安全的
                new UpdateBySql().inject(assistant, entityClass, entityClass, null);
            }

            if (!configuration.hasStatement(namespace + "." + DeleteBySql.METHOD_NAME)) {
                MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace.replace('.', '/') + ".java (Global)");
                assistant.setCurrentNamespace(namespace);
                new DeleteBySql().inject(assistant, entityClass, entityClass, null);
            }

            if (!configuration.hasStatement(namespace + "." + InsertBySql.METHOD_NAME)) {
                MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace.replace('.', '/') + ".java (Global)");
                assistant.setCurrentNamespace(namespace);
                new InsertBySql().inject(assistant, entityClass, entityClass, null);
            }

            if (!configuration.hasStatement(namespace + "." + SelectBySql.METHOD_NAME)) {
                MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace.replace('.', '/') + ".java (Global)");
                assistant.setCurrentNamespace(namespace);
                new SelectBySql().inject(assistant, entityClass, entityClass, null);
            }

            isGlobalInjected = true;
        }
    }

    /**
     * 优化的直接 SQL 更新方法
     * 1. 无需传入 Class
     * 2. 增加 SQL 安全检查
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateBySql(String sqlTemplate, Map<String, Object> params) {
        // 1. 安全检查
        SqlSafeUtil.checkSql(sqlTemplate);

        // 2. 确保全局 Mapper 已注入 (只执行一次)
        injectGlobalMapperIfNeed();

        // 3. 准备参数
        Map<String, Object> map = params == null ? new HashMap<>() : new HashMap<>(params);
        map.put("sql", sqlTemplate);

        // 4. 使用全局 ID 执行
        String statementId = GlobalDummy.class.getName() + "." + UpdateBySql.METHOD_NAME;
        return sqlSessionTemplate.update(statementId, map);
    }

    /**
     * 根据 ID 更新实体 (严格模式)
     * 1. 检查实体是否有 @TableId 注解
     * 2. 检查实体 ID 值是否为 null
     * 3. 只更新非空字段
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateById(Object entity) {
        Assert.notNull(entity, "更新对象不能为空");
        Class<?> entityClass = entity.getClass();

        // 1. 确保已注入
        injectIfNeed(entityClass);

        // 2. 获取元数据进行检查
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);

        // 校验 A: 检查是否定义了主键
        if (tableInfo == null || !tableInfo.havePK()) {
            throw new IllegalArgumentException("操作失败: 实体类 " + entityClass.getSimpleName() + " 未定义主键 (@TableId)");
        }

        // ------------------ 修改开始 ------------------
        // 校验 B: 检查传入对象的 ID 值是否为空
        // 使用 MyBatis 原生工具 SystemMetaObject 获取属性值，兼容性最好
        MetaObject metaObject = SystemMetaObject.forObject(entity);
        // tableInfo.getKeyProperty() 获取的是主键的属性名 (如 "userId")
        // metaObject.getValue(...) 会自动调用 getUserId() 方法
        Object idVal = metaObject.getValue(tableInfo.getKeyProperty());
        // ------------------ 修改结束 ------------------

        // 校验 B: 检查传入对象的 ID 值是否为空
        // 利用 MP 的 ReflectionKit 反射获取主键值
        //tableInfo.havePK(): 这是 MyBatis-Plus 解析后的元数据，判断类上有没有 @TableId。
//        Object idVal = ReflectionKit.getMethodValue(entity, tableInfo.getKeyProperty());
        if (idVal == null) {
            throw new IllegalArgumentException("操作失败: 更新操作的主键值不能为 NULL (字段: " + tableInfo.getKeyProperty() + ")");
        }

        // 3. 执行更新
        String statementId = entityClass.getName() + "." + UpdateByIdEntity.METHOD_NAME;

        return sqlSessionTemplate.update(statementId, entity);
    }


    /**
     * 自定义 SQL 条件更新
     *
     * @param entity   用于生成 SET 语句 (更新的内容)
     * @param whereSql 自定义 WHERE 条件 (例如 "age > 10 AND status = 1")
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> int update(T entity, String whereSql) {
        Assert.notNull(entity, "更新实体 entity 不能为空");
        // 1. 安全检查: 禁止空条件 (防止全表更新)
        if (whereSql == null || whereSql.trim().isEmpty()) {
            throw new IllegalArgumentException("必须指定 whereSql 条件，禁止全表更新！");
        }

        // 简单防注入/防误操作检查 (根据需求可选)
        // 这里的校验逻辑比较简单，如果 whereSql 包含 ; 或者是 DROP 等关键字应该拦截
        if (whereSql.contains(";") || whereSql.toUpperCase().contains("DROP ")) {
            throw new IllegalArgumentException("检测到非法 SQL 关键字");
        }

        try {
            SqlSafeUtil.checkWhereClauseStrict(whereSql);
        } catch (IllegalArgumentException e) {
            log.error("SQL 安全拦截: {}", e.getMessage());
            throw e; // 抛出异常中断执行
        }

        Class<?> entityClass = entity.getClass();

        // 2. 确保已注入
        injectIfNeed(entityClass);

        // 3. 构造参数上下文
        Map<String, Object> context = new HashMap<>();
        context.put("et", entity);        // 实体 -> SET 用
        context.put("whereSql", whereSql); // 字符串 -> WHERE 用

        String statementId = entityClass.getName() + "." + UpdateWithWhereSqlEntity.METHOD_NAME;

        // 4. 执行更新
        return sqlSessionTemplate.update(statementId, context);
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteById(Object entity) {
        Assert.notNull(entity, "删除对象不能为空");
        Class<?> entityClass = entity.getClass();

        injectIfNeed(entityClass);
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);

        // 校验: 是否有主键注解
        if (tableInfo == null || !tableInfo.havePK()) {
            throw new IllegalArgumentException("操作失败:实体" + entityClass.getSimpleName() + "未定义主键(@TableId)");
        }

        // 校验: ID 值是否为空 (使用 SystemMetaObject)
        MetaObject metaObject = SystemMetaObject.forObject(entity);
        Object idVal = metaObject.getValue(tableInfo.getKeyProperty());
        if (idVal == null) {
            throw new IllegalArgumentException("操作失败:删除操作的主键值不能为NULL");
        }

        log.info("[Delete]根据ID删除:Entity={},ID={}", entityClass.getSimpleName(), idVal);

        String statementId = entityClass.getName() + "." + DeleteByIdEntity.METHOD_NAME;
        return sqlSessionTemplate.delete(statementId, entity); // 这里的 parameter 直接传 entity，MyBatis 会去取属性
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteBySql(String sql, Map<String, Object> params) {
        // 安全检查
        SqlSafeUtil.checkSql(2, sql); // 确保包含 DELETE 且有 WHERE

        injectGlobalMapperIfNeed();

        Map<String, Object> map = params == null ? new HashMap<>() : new HashMap<>(params);
        map.put("sql", sql);

        log.info("[Delete]原生SQL删除:SQL={},Params={}", sql, params);

        String statementId = GlobalDummy.class.getName() + "." + DeleteBySql.METHOD_NAME;
        return sqlSessionTemplate.delete(statementId, map);
    }

    @Transactional(rollbackFor = Exception.class)
    public <T> int delete(Class<T> entityClass, Map<String, Object> params) {
        Assert.notNull(entityClass, "实体类型不能为空");

        // 全表防护
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("操作失败:必须提供params删除条件禁止全表删除！");
        }

        injectIfNeed(entityClass);

        Map<String, Object> context = new HashMap<>();
        context.put("params", params); // 对应 XML 中的 collection="params"

        log.info("[Delete]Map条件删除:Entity={},Params={}", entityClass.getSimpleName(), params);

        String statementId = entityClass.getName() + "." + DeleteWithWhereEntity.METHOD_NAME;
        return sqlSessionTemplate.delete(statementId, context);
    }

    /**
     * @param entityClass
     * @param whereSql    id=1111 and flag=0
     * @param <T>
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> int delete(Class<T> entityClass, String whereSql) {
        Assert.notNull(entityClass, "实体类型不能为空");

        // 安全检查 & 全表防护
        try {
            SqlSafeUtil.checkWhereClause(2, whereSql);
        } catch (IllegalArgumentException e) {
            log.error("SQL安全拦截:{}", e.getMessage());
            throw e;
        }

        injectIfNeed(entityClass);

        Map<String, Object> context = new HashMap<>();
        context.put("whereSql", whereSql);

        log.info("[Delete]String条件删除:Entity={},Where={}", entityClass.getSimpleName(), whereSql);

        String statementId = entityClass.getName() + "." + DeleteWithWhereSqlEntity.METHOD_NAME;
        return sqlSessionTemplate.delete(statementId, context);
    }


    /**
     * Bean -> Map<Column, Value>
     * 只提取非空字段，且 Key 转换为数据库列名 (解决驼峰/下划线不一致问题)
     */
    private Map<String, Object> convertBeanToColumnMap(Object entity) {
        Map<String, Object> map = new HashMap<>();
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entity.getClass());
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        // 1. 处理普通字段
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            Object value = metaObject.getValue(fieldInfo.getProperty());
            if (value != null) {
                map.put(fieldInfo.getColumn(), value); // Key 使用数据库列名
            }
        }
        // 2. 处理主键
        if (tableInfo.havePK()) {
            Object pkVal = metaObject.getValue(tableInfo.getKeyProperty());
            if (pkVal != null) {
                map.put(tableInfo.getKeyColumn(), pkVal);
            }
        }
        return map;
    }

    /**
     * 入参转换: Custom Request -> MP Page
     */
    private <T> IPage<T> toMpPage(PageRequest req) {
        if (req == null) {
            return new Page<>(1, 10);
        }
        return new Page<>(req.getCurrent(), req.getSize());
    }

    /**
     * 出参转换: MP Page -> Custom Result
     */
    private <T> PageResult<T> toPageResult(IPage<T> mpPage) {
        return new PageResult<>(
                mpPage.getRecords(),
                mpPage.getTotal(),
                mpPage.getCurrent(),
                mpPage.getSize(),
                mpPage.getPages()
        );
    }

}
