package com.obee.mybatis.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/27 14:50
 */
@Data
public class Demo implements Serializable {

    @TableId(type = IdType.ASSIGN_ID) // 雪花算法ID
    private Long id;

    private String demoid;
    private String demoname;
    private Integer flag;
    private String creater;
    private LocalDateTime createtime;

    private String laster;
    private LocalDateTime lasttime;

    private String remark;
}
