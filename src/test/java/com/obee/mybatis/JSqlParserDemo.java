//package com.obee.mybatis;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.test.context.SpringBootTest;
//import com.github.jsqlparser.JSQLParserException;
//import com.github.jsqlparser.expression.Expression;
//import com.github.jsqlparser.parser.CCJSqlParserUtil;
//import com.github.jsqlparser.statement.Statement;
//import com.github.jsqlparser.statement.select.Select;
//import com.github.jsqlparser.util.TablesNamesFinder;
//import com.github.jsqlparser.util.expression.ExpressionBuilder;
//
///**
// * @description:
// * @author: Guanwf
// * @date: 2025/12/28 11:55
// */
//@SpringBootTest
//@Slf4j
//public class JSqlParserDemo {
//
//    public void Test01() {
//        // 1. 待解析的 SQL
//        String sql = "SELECT id, name FROM user WHERE age > 18";
//
//        // 2. 解析 SQL 为 AST
//        Statement statement = CCJSqlParserUtil.parse(sql);
//        Select select = (Select) statement;
//
//        // 3. 分析：提取 SQL 中的表名
//        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
//        System.out.println("涉及表名：" + tablesNamesFinder.getTableList(select)); // 输出 [user]
//
//        // 4. 修改：添加 WHERE 条件（age < 30）
//        Expression originalWhere = select.getSelectBody().getWhere();
//        Expression newWhere = ExpressionBuilder.create(originalWhere).and("age < 30").build();
//        select.getSelectBody().setWhere(newWhere);
//
//        // 5. 生成：将修改后的 AST 还原为 SQL
//        String modifiedSql = select.toString();
//        System.out.println("修改后 SQL：" + modifiedSql); // 输出 SELECT id, name FROM user WHERE age > 18 AND age < 30
//
//    }
//}
