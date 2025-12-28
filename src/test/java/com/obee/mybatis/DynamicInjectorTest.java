package com.obee.mybatis;

import com.obee.mybatis.model.Demo;
import com.obee.mybatis.model.PageRequest;
import com.obee.mybatis.model.PageResult;
import com.obee.mybatis.utils.MapBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.obee.mybatis.service.DynamicInjectorService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
        //select
        long start = System.currentTimeMillis();
        String sql1 = "SELECT * FROM demo WHERE id = 1";
        List<Demo> list1 = dynamicInjectorService.selectSqlList(sql1, null, Demo.class);
        long end = System.currentTimeMillis();
        log.info("Result Count: {}, Time Cost: {}ms", list1.size(), (end - start));
        log.info("{}", list1);

        // 第二次调用：直接走缓存，性能极快
        String sql2 = "SELECT * FROM demo WHERE flag > #{flag}";
        Map<String, Object> params = Map.of("flag", 1);
        List<Demo> list2 = dynamicInjectorService.selectSqlList(sql2, params, Demo.class);
        log.info("{}", list2);

    }

    @Test
    public void TestAdd() {
        //add
        Demo demo = new Demo();
//        demo.setId(1L);
        demo.setDemoid("1");
        demo.setDemoname("1111");
        demo.setFlag(10);
        demo.setCreater("demo");
        demo.setCreatetime(LocalDateTime.now());
        demo.setLaster("demo");
        demo.setRemark("test");

        log.info("{}", demo);
        int rowEffect = dynamicInjectorService.insert(demo);
        log.info("roweffect={}", rowEffect);
    }

    @Test
    public void TestInsertBySql(){
        String mySql="insert into demo(id,demoid,demoname,flag,creater,createtime,laster)"
                +" values(111,'111','222',11,'test',now(),'test')";
        int rowEffect = dynamicInjectorService.insertBySql(mySql);
        log.info("roweffect={}", rowEffect);
    }

    @Test
    public void TestInsertBySqlParams(){
        String mySql="insert into demo(id,demoid,demoname,flag,creater,createtime,laster)"
                +" values(#{id},#{demoid},#{demoname},#{flag},#{creater},#{createtime},#{laster})";

        Map<String,Object> map=new HashMap<>();
        map.put("id","3333");
        map.put("demoid","33");
        map.put("demoname","3333");
        map.put("flag","30");
        map.put("creater","33");
        map.put("createtime",LocalDateTime.now());
        map.put("laster","last");
        int rowEffect = dynamicInjectorService.insertBySql(mySql,map);
        log.info("roweffect={}", rowEffect);
    }

    @Test
    public void TestUpdateById() {
        Demo demo = new Demo();
        demo.setId(2004812455125958657L);
        demo.setDemoid("1");
        demo.setRemark("update");

        int rowEffect = dynamicInjectorService.updateById(demo);
        log.info("roweffect={}", rowEffect);
    }

    @Test
    public void TestUpdateByParam() {
        Demo demo = new Demo();
        demo.setId(2004812455125958657L);
        demo.setDemoid("1");
        demo.setRemark("update");

        HashMap<String, Object> param = new HashMap<>();
        param.put("id", "2004812455125958657");
        param.put("demoid", "111");

        int rowEffect = dynamicInjectorService.update(demo,param);
        log.info("roweffect={}", rowEffect);
    }

    @Test
    public void TestUpdateByWhereSQL() {
        Demo demo = new Demo();
        demo.setId(2004812455125958657L);
        demo.setDemoid("1");
        demo.setRemark("update");

        String whereSQL="flag>=10";

        int rowEffect = dynamicInjectorService.update(demo,whereSQL);
        log.info("roweffect={}", rowEffect);
    }

    @Test
    public void TestUpdateSql() {
        String mySQL = "UPDATE demo SET demoid=#{demoid}, remark=#{remark} WHERE id=#{id}";
        HashMap<String, Object> param = new HashMap<>();
        param.put("id", "2004812455125958657");
        param.put("demoid", "111");
        param.put("remark", "TestUpdateSql");

//        int rowEffect = dynamicInjectorService.updateBySql(mySQL, param, Demo.class);
        int rowEffect = dynamicInjectorService.updateBySql(mySQL, param);
        log.info("roweffect={}", rowEffect);
    }

    @Test
    public void testBatchPerformance() {
        List<Demo> demos = new ArrayList<>();

        // 构造 5000 条数据
        for (int i = 0; i < 10; i++) {
            Demo user = new Demo();
            user.setDemoid(Integer.toString(i));
            user.setDemoname(Integer.toString(i));
            user.setFlag(1);
            user.setCreater("patch");
            user.setLaster("patch");
            user.setCreatetime(LocalDateTime.now());
            demos.add(user);
        }

        long start = System.currentTimeMillis();

        // 执行批量插入
        int rows = dynamicInjectorService.insert(demos);

        long end = System.currentTimeMillis();

        log.info("成功插入: " + rows + " 行");
        log.info("耗时: " + (end - start) + " ms");

    }

    @Test
    public void TestDeleteById(){

        Demo demo = new Demo();
        demo.setId(2004832441475239947L);
        demo.setDemoid("1");
        demo.setRemark("update");

        int rowEffect = dynamicInjectorService.deleteById(demo);
        log.info("roweffect={}", rowEffect);

    }

    @Test
    public void TestDeleteBySQL(){
        String mySQL="DELETE FROM demo WHERE id = #{id}";
        Map<String,Object> params=new HashMap<>();
        params.put("id","2004832441475239946");
        int rowEffect = dynamicInjectorService.deleteBySql(mySQL,params);
        log.info("roweffect={}", rowEffect);
    }

    @Test
    public void TestDeleteBySQLEntity(){
        int rowEffect = dynamicInjectorService.delete(Demo.class,"id=2004832441475239946 and flag=10");
        log.info("roweffect={}", rowEffect);
    }

    @Test
    public void TestDeleteByParamsEntity(){
        Map<String,Object> params=new HashMap<>();
        params.put("id","2004832441475239946");
        params.put("flag","10");

        int rowEffect = dynamicInjectorService.delete(Demo.class,params);
        log.info("roweffect={}", rowEffect);
    }

    //select
    @Test
    public void TestSelectOne(){
        Demo demo=dynamicInjectorService.selectById(Demo.class,3333L);
        log.info("{}",demo);
    }

    @Test
    public void TestSelectByMap(){
        Demo demo=dynamicInjectorService.selectOne(Demo.class,MapBuilder.of("id","3333").put("flag",30).build());
        log.info("{}",demo);
    }

    @Test
    public void TestSelectListByMap(){
        List<Demo> demos=dynamicInjectorService.selectList(Demo.class,MapBuilder.of("id","3333").put("flag",30).build());
        log.info("{}",demos);
    }

    @Test
    public void TestSelectListByWhere(){
        String mysql="select * from demo where flag = 30 AND id = #{id}";
        List<Demo> demos=dynamicInjectorService.selectListBySql(Demo.class,mysql,MapBuilder.of("id","3333").build());
        log.info("{}",demos);
    }

    @Test
    public void TestSelectPageByMap(){
        PageRequest pageRequest=new PageRequest(2,2);
        PageResult<Demo> demos=dynamicInjectorService.selectPage(Demo.class,pageRequest,MapBuilder.of("flag",1).build());
        log.info("getPages>{}",demos.getPages());
        log.info("getCurrent>{}",demos.getCurrent());
        log.info("getTotal>{}",demos.getTotal());
        log.info("getSize>{}",demos.getSize());
        log.info("getRecords>{}",demos.getRecords());
    }

    @Test
    public void TestSelectPageBySql(){
        PageRequest pageRequest=new PageRequest(2,2);
        String mySQL="select * from demo where flag=#{flag}";
        PageResult<Demo> demos=dynamicInjectorService.selectPageBySql(Demo.class,pageRequest,mySQL,MapBuilder.of("flag",1).build());
        log.info("getPages>{}",demos.getPages());
        log.info("getCurrent>{}",demos.getCurrent());
        log.info("getTotal>{}",demos.getTotal());
        log.info("getSize>{}",demos.getSize());
        log.info("getRecords>{}",demos.getRecords());
    }

    @Test
    public void TestSelectPageBySql2(){
        PageRequest pageRequest=new PageRequest(2,2);
        String mySQL="select * from demo where flag=#{flag}";
        PageResult<Demo> demos=dynamicInjectorService.selectPageBySql(mySQL,pageRequest,MapBuilder.of("flag",1).build());
        log.info("getPages>{}",demos.getPages());
        log.info("getCurrent>{}",demos.getCurrent());
        log.info("getTotal>{}",demos.getTotal());
        log.info("getSize>{}",demos.getSize());
        log.info("getRecords>{}",demos.getRecords());
    }

    @Test
    public void TestselectListBySql() {
        String mySQL = "select * from demo where flag=#{flag}";
        List<Demo> demos=dynamicInjectorService.selectListBySql(Demo.class,mySQL,MapBuilder.of("flag",1).build());
        log.info("{}",demos);
    }

    @Test
    public void TestselectListBySqlMap() {
        String mySQL = "select * from demo where flag=#{flag}";
        List<Map> demos=dynamicInjectorService.selectListBySql(mySQL,MapBuilder.of("flag",1).build());
        log.info("{}",demos);
    }

    @Test
    public void TestSelectOneBySql() {
        String mySQL = "select * from demo where id=#{id} and flag=#{flag}";
        Demo demos=dynamicInjectorService.selectOneSQL(Demo.class,mySQL,MapBuilder.of("flag",1).put("id","2004832441475239938").build());
        log.info("{}",demos);
    }

    @Test
    public void TestSelectOneBySqlMap() {
        String mySQL = "select * from demo where id=#{id} and flag=#{flag}";
        Map demos=dynamicInjectorService.selectOneSQL(mySQL,MapBuilder.of("flag",1).put("id","2004832441475239938").build());
        log.info("{}",demos);
    }



    @Test
    public void TestSelectOneBySqlWithConvert() {
        String mySQL = "select * from demo where id=#{id} and flag=#{flag}";
        Demo demos=dynamicInjectorService.selectOneBySqlWithConvert(Demo.class,mySQL,MapBuilder.of("flag",1).put("id","2004832441475239938").build());
        log.info("{}",demos);
    }

    @Test
    public void TestSelectListBySqlWithConvert() {
        String mySQL = "select * from demo where id=#{id} and flag=#{flag}";
        List<Demo> demos=dynamicInjectorService.selectListBySqlWithConvert(Demo.class,mySQL,MapBuilder.of("flag",1).put("id","2004832441475239938").build());
        log.info("{}",demos);
    }

}
