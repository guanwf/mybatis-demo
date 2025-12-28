
CREATE TABLE `demo` (
`id` bigint(20) NOT NULL COMMENT 'Id',
`demoid` varchar(32) DEFAULT NULL COMMENT '角色编号',
`demoname` varchar(64) NOT NULL COMMENT '角色名称',
`flag` int(1) NOT NULL DEFAULT '0' COMMENT '状态',
`creater` varchar(64) NOT NULL COMMENT '建立人',
`createtime` datetime DEFAULT NULL COMMENT '建立时间',
`laster` varchar(64) NOT NULL COMMENT '修改人',
`lasttime` datetime DEFAULT NULL COMMENT '修改时间',
`remark` varchar(64) DEFAULT NULL COMMENT '备注',
PRIMARY KEY (`id`)
) DEFAULT CHARSET = utf8mb4 COMMENT = '测试表';
