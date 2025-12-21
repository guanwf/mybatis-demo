package com.obee.mybatis;

import com.obee.mybatis.model.Goods;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.obee.mybatis.service.DynamicInjectorService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 21:32
 */
@SpringBootTest
@Slf4j
public class DynamicInjectorTest {

    @Autowired
    private DynamicInjectorService dynamicInjectorService;

    @Test
    public void Test01() {

        long start = System.currentTimeMillis();
        String sql1 = "SELECT * FROM sys_user WHERE id = 1";
        List<Goods> list1 = dynamicInjectorService.selectSqlList(sql1, null, Goods.class);
        long end = System.currentTimeMillis();
//        log.info("Result Count: {}, Time Cost: {}ms", list1.size(), (end - start));


        // 第二次调用：直接走缓存，性能极快
        String sql2 = "SELECT * FROM sys_user WHERE age > #{age}";
        Map<String, Object> params = Map.of("age", 10);
        List<Goods> list2 = dynamicInjectorService.selectSqlList(sql2, params, Goods.class);

        System.out.println(list1);
        System.out.println(list2);

    }

    @Test
    public void Test02(){
        Goods goods = new Goods();
        goods.setStatus(11);
        goods.setAge(25);

        dynamicInjectorService.insert(goods);
    }

    public void testBatchPerformance() {
        List<Goods> userList = new ArrayList<>();

        // 构造 5000 条数据
        for (int i = 0; i < 5000; i++) {
            Goods user = new Goods();
            user.setStatus(i);
            user.setAge(i % 100);
            userList.add(user);
        }

        long start = System.currentTimeMillis();

        // 执行批量插入
        int rows = dynamicInjectorService.insertBatch(userList);

        long end = System.currentTimeMillis();

        System.out.println("成功插入: " + rows + " 行");
        System.out.println("耗时: " + (end - start) + " ms");
    }

}
