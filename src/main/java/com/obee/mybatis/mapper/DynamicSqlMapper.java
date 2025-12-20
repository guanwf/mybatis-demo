package com.obee.mybatis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 21:01
 * 全局唯一的通用 Mapper，用于执行任意 SQL
 */
@Mapper
public interface DynamicSqlMapper {
    // ${sql} 直接执行传入的 SQL
    @Select("<script>${sql}</script>")
    List<Map<String, Object>> selectRaw(@Param("sql") String sql);
}
