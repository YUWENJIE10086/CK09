-- 烤房分配记录表
CREATE TABLE IF NOT EXISTS `barn_assignment` (
  `assignment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分配ID',
  `barn_id` bigint NOT NULL COMMENT '烤房ID',
  `project_code` varchar(64) DEFAULT NULL COMMENT '项目编号',
  `barn_name` varchar(128) DEFAULT NULL COMMENT '烤房名称',
  `farmer_id` bigint DEFAULT NULL COMMENT '烟农ID',
  `farmer_name` varchar(64) DEFAULT NULL COMMENT '烟农姓名',
  `farmer_phone` varchar(20) DEFAULT NULL COMMENT '烟农电话',
  `assign_status` varchar(20) DEFAULT '已分配' COMMENT '分配状态：已分配/使用中/已归还/已撤销',
  `assign_date` varchar(20) DEFAULT NULL COMMENT '分配日期',
  `return_date` varchar(20) DEFAULT NULL COMMENT '归还日期',
  `assign_remark` varchar(500) DEFAULT NULL COMMENT '分配备注',
  `assign_by` varchar(64) DEFAULT NULL COMMENT '分配人',
  `assign_time` datetime DEFAULT NULL COMMENT '分配时间',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`assignment_id`),
  KEY `idx_barn_id` (`barn_id`),
  KEY `idx_farmer_id` (`farmer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烤房分配记录表';
