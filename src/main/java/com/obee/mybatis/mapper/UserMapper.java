package com.obee.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.obee.mybatis.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/20 20:20
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
