package com.obee.mybatis.utils;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.expression.Expression;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 16:12
 */
public class SqlSafeUtil {

    // 禁止的高危关键字 (不区分大小写)
    // (?i) 表示忽略大小写
//    private static final Pattern DANGEROUS_KEYWORDS = Pattern.compile(
//            "(?i)\\b(DROP|TRUNCATE|ALTER|GRANT|REVOKE|Create|DESC)\\b"
//    );

    // 必须包含 WHERE (防止全表更新/删除)
    // 简单的检查，防止低级错误
    private static final Pattern WHERE_PATTERN = Pattern.compile("(?i)\\bWHERE\\b");

    // 1. 堆叠查询和注释符检查 (最常见的注入手段)
    // 匹配分号 ; 或 注释符 --, /*, */, #
    private static final Pattern SQL_INJECTION_CHARS = Pattern.compile(";|--|/\\*|\\*/|#");

    // 2. 高危操作关键字 (仅限 WHERE 子句中不应出现的词)
    // 忽略大小写，匹配单词边界
    // 注意：这里没有禁用 SELECT (允许子查询)，也没有禁用 UPDATE (因为有时 update_time 这种字段名很常见)
    // 但禁止了修改结构的命令和 UNION (防止数据泄露)
    private static final Pattern DANGEROUS_KEYWORDS = Pattern.compile(
            "(?i)\\b(DROP|TRUNCATE|ALTER|GRANT|REVOKE|INSERT|DELETE|UNION|SHUTDOWN|EXEC|Create|DESC)\\b"
    );

    private static final Pattern DANGEROUS_KEYWORDS2 = Pattern.compile(
            "(?i)\\b(DROP|TRUNCATE|ALTER|GRANT|REVOKE|INSERT|UNION|SHUTDOWN|EXEC|Create|DESC)\\b"
    );

    private static final Pattern DANGEROUS_KEYWORDS_INSERT = Pattern.compile(
            "(?i)\\b(DROP|TRUNCATE|ALTER|GRANT|REVOKE|DELETE|UNION|SHUTDOWN|EXEC|Create|DESC)\\b"
    );

    // 用于匹配 MyBatis 占位符 #{...} 和 ${...}
    private static final Pattern MYBATIS_PLACEHOLDER = Pattern.compile("[#$]\\{[^}]+\\}");

    public static void checkSql(String sql) {
        checkSql(1, sql);
    }

    /**
     * SQL 安全检查
     */
    public static void checkSql(int type, String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new IllegalArgumentException("SQL语句不能为空");
        }

        // 1. 检查高危操作
        if (type == 2) {
            //delete
            if (DANGEROUS_KEYWORDS2.matcher(sql).find()) {
                throw new IllegalArgumentException("检测到非法/高危SQL关键字，操作被拒绝:" + sql);
            }
        } else {
            if (DANGEROUS_KEYWORDS.matcher(sql).find()) {
                throw new IllegalArgumentException("检测到非法/高危SQL关键字，操作被拒绝:" + sql);
            }
        }


        // 2. 检查 SQL 类型 (仅允许 UPDATE 或 DELETE)
        String upperSql = sql.trim().toUpperCase();
        boolean isUpdate = upperSql.startsWith("UPDATE");
        boolean isDelete = upperSql.startsWith("DELETE");

        if (!isUpdate && !isDelete) {
            throw new IllegalArgumentException("通用SQL执行只允许UPDATE或DELETE语句");
        }

        // 3. 强制检查 WHERE 条件 (防止全表事故)
        // 注意：这只是个简单的正则，复杂的 SQL (如子查询) 可能会误判，生产环境建议用 JSqlParser
        if (!WHERE_PATTERN.matcher(sql).find()) {
            throw new IllegalArgumentException("为了安全起见，UPDATE/DELETE语句必须包含WHERE条件");
        }

        // 4. 简单的注入检查 (禁止分号，防止堆叠查询)
        if (sql.contains(";")) {
            throw new IllegalArgumentException("SQL语句不允许包含分号(;)");
        }
    }

    public static void checkInsertSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new IllegalArgumentException("SQL语句不能为空");
        }

        // 1. 检查高危操作
        if (DANGEROUS_KEYWORDS_INSERT.matcher(sql).find()) {
            throw new IllegalArgumentException("检测到非法/高危SQL关键字，操作被拒绝:" + sql);
        }

        // 2. 检查 SQL 类型 (仅允许 UPDATE 或 DELETE)
        String upperSql = sql.trim().toUpperCase();
        boolean isInsert = upperSql.startsWith("INSERT");

        if (!isInsert) {
            throw new IllegalArgumentException("通用SQL执行只允许Insert语句");
        }

        // 3. 强制检查 WHERE 条件 (防止全表事故)
        // 注意：这只是个简单的正则，复杂的 SQL (如子查询) 可能会误判，生产环境建议用 JSqlParser
//        if (!WHERE_PATTERN.matcher(sql).find()) {
//            throw new IllegalArgumentException("为了安全起见，UPDATE/DELETE语句必须包含WHERE条件");
//        }

        // 4. 简单的注入检查 (禁止分号，防止堆叠查询)
        if (sql.contains(";")) {
            throw new IllegalArgumentException("SQL语句不允许包含分号(;)");
        }
    }

    public static void checkWhereClause(String whereSql) {
        checkWhereClause(1, whereSql);
    }

    /**
     * 校验 Where 条件片段的安全性
     *
     * @param whereSql 例如 "id = 1 AND status = 0"
     */
    public static void checkWhereClause(int type, String whereSql) {
        // 1. 基础非空检查 (防止全表更新)
        if (!StringUtils.hasText(whereSql)) {
            throw new IllegalArgumentException("Where条件不能为空，禁止全表更新！");
        }

        // 2. 注入字符检查
        if (SQL_INJECTION_CHARS.matcher(whereSql).find()) {
            throw new IllegalArgumentException("Where条件包含非法字符(分号/注释符)，已拦截:" + whereSql);
        }

        // 3. 高危关键字检查
        if (type == 2) {
            if (DANGEROUS_KEYWORDS2.matcher(whereSql).find()) {
                throw new IllegalArgumentException("Where条件包含高危关键字，已拦截:" + whereSql);
            }
        } else {
            if (DANGEROUS_KEYWORDS.matcher(whereSql).find()) {
                throw new IllegalArgumentException("Where条件包含高危关键字，已拦截:" + whereSql);
            }
        }


        // 4. (可选) 简单的平衡性检查
        // 比如检查括号是否成对出现，防止简单的语法错误
        int openParenthesis = StringUtils.countOccurrencesOf(whereSql, "(");
        int closeParenthesis = StringUtils.countOccurrencesOf(whereSql, ")");
        if (openParenthesis != closeParenthesis) {
            throw new IllegalArgumentException("Where条件语法错误：括号不匹配");
        }
    }

    public static void checkWhereClauseStrict(String whereSql) {
        if (!StringUtils.hasText(whereSql)) {
            throw new IllegalArgumentException("Where条件不能为空");
        }

        // 构造一个假的 SQL 进行解析验证
        String dummySql = "SELECT * FROM dummy WHERE " + whereSql;

        try {
            // 尝试解析。如果 whereSql 语法不对（比如括号不匹配，关键字错误），这里直接抛异常
            CCJSqlParserUtil.parse(dummySql);
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException("Where条件SQL语法错误:" + e.getCause().getMessage());
        }

        // 如果能解析通过，说明语法结构是合法的 SQL 表达式
        // 可以在这里进一步遍历 Expression 树，检查是否有子查询等复杂操作（视需求而定）
    }

    /**
     * 检查 SELECT SQL 合法性
     */
    public static void checkSelectSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new IllegalArgumentException("SQL 语句不能为空");
        }

        String upper = sql.trim().toUpperCase();
        if (!upper.startsWith("SELECT")) {
            throw new IllegalArgumentException("selectBySql 仅支持 SELECT 语句");
        }

        // 检查高危关键字 (DROP/TRUNCATE/UPDATE/DELETE/GRANT)
        // 允许子查询，但禁止修改数据的操作
        if (DANGEROUS_KEYWORDS.matcher(sql).find()) {
            throw new IllegalArgumentException("检测到非法/高危 SQL 关键字，操作被拒绝");
        }

        if (sql.contains(";")) {
            throw new IllegalArgumentException("SQL 语句不允许包含分号 (;)");
        }

        // =========================================================
        // 5. 【核心升级】使用 JSqlParser 进行严格语法检查
        // =========================================================
        try {
            // A. 预处理：因为 SQL 包含 #{id} 这种非标准 SQL 语法，解析器不认识
            // 我们将所有的 #{xxx} 或 ${xxx} 替换为 "?" (标准占位符) 或 "0" (数字字面量)
            // 这样只检查 SQL 结构，不关心参数
            String mockSql = MYBATIS_PLACEHOLDER.matcher(sql).replaceAll("?");

            // B. 尝试解析
            // 如果 SQL 写成了 "select * fromd demo"，这里会直接抛出 JSQLParserException
            CCJSqlParserUtil.parse(mockSql);

        } catch (JSQLParserException e) {
            // 解析失败，说明语法有错
            throw new IllegalArgumentException("SQL语法错误，请检查拼写:" + e.getCause().getMessage());
        }

    }

}
