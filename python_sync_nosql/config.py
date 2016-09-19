#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# rabbitmq connect
rabbitmq_host = '192.168.179.184'
rabbitmq_port = 5672
# rabbitmq 远程访问禁止使用 guest账号
rabbitmq_user = 'test'    
rabbitmq_pass = '123456'
rabbitmq_queue_name = 'binlog_test'


#binlog_dir = '/Users/liukelin/Desktop/canal-otter-mycat-cobar/canal_object/data' # 文件路径
#binlog_file = '' # 开始文件
#binlog_prefix = 'binlog_' # 文件前缀


# redis
redis_host = '127.0.0.1'
redis_port = 6379
