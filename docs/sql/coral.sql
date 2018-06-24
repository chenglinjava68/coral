# Create Database
# ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS coral_db DEFAULT CHARACTER SET = utf8mb4;

Use coral_db;

# Dump of table tb_segment
# ------------------------------------------------------------
DROP TABLE IF EXISTS `tb_segment`;

create table `tb_segment`(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_tag` varchar(128) NOT NULL COMMENT '业务标识',
  `max_id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '表示该biz_tag目前所被分配的ID号段的最大值',
  `step` int(10) NOT NULL DEFAULT '1000' COMMENT '表示每次分配的号段长度'
  `desc` varchar(256) NOT NULL DEFAULT 'default' COMMENT '应用名',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `INX_biz_tag` (`biz_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用表';
