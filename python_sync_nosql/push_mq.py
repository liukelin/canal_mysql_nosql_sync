#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#  rabitmq client 生产队列数据
#  将file 数据添加到 mq 

# @author: liukelin  314566990@qq.com

import pika
import os
import config

BASE_DIR = os.path.dirname(os.path.realpath(__file__))

'''
	get file
	读取不断增长的多个文件

'''
def get_file():

	# get file 
	binlogDirs = []
	binlog_dir = config.binlog_dir
	binlog_prefix = config.binlog_prefix
	binlog_file = config.binlog_file

	if not binlog_dir:
		return  

	listdir = os.listdir(binlog_dir)
	for line in listdir:
		if binlog_prefix : 
			if line.startswith(binlog_prefix):
				binlogDirs.append(line)
		else:
			binlogDirs.append(line)

	if len(binlogDirs)==0:
		return 

	# 获取开始标记
	tag = log_tag('get')


	binlogDirs.sort()

	# push_mq
	for i in binlogDirs:
		file = open(binlog_dir + '/' + i)

		connection, channel, queue_name = conn_mq()
		while 1:
			line = file.readline()
			if not line:
				break
			# print(line)
			channel.basic_publish(exchange='',routing_key=queue_name, body=line)
		connection.close()
	return


'''
	文件读取标记
	set、get
	2016-12-22 23:34:23 | binog_222.log | 23234
'''
def log_tag(action, data=''):
	dir_ = BASE_DIR + '/meta.log'
	if action=='set':
		if data:
			f=open(dir_,'w') # 避免竞争可用a+写入
			f.write(data)
			f.close()
			return True
	else:
		text = ''
		f = open(dir_,'r')
		try:
			text = f.read()
		finally:
			f.close()

		con = text.split('|')
		for i in range(0,len(con)):
			con[i] = con[i].replace(' ', '')
		return con
	return False


# push MQ
def conn_mq():
	
	queue_name = config.rabbitmq_queue_name if config.rabbitmq_queue_name !='' else 'binlog_test'

	credentials = pika.PlainCredentials(config.rabbitmq_user, config.rabbitmq_pass) # 远程访问禁止使用 guest账号
	# 这里可以连接远程IP，请记得打开远程端口
	parameters = pika.ConnectionParameters(config.rabbitmq_host, config.rabbitmq_port,'/',credentials) 
	# 建立连接
	connection = pika.BlockingConnection(parameters) # 本机 parameters可直接写localhost
	
	# channel
	channel = connection.channel()  

	# 是确保队列存在 / 创建queue
	channel.queue_declare(queue=queue_name)

	# 发送消息到指定队列
	# channel.basic_publish(exchange='',routing_key=queue_name,body='test')

	# connection.close()
	return connection, channel, queue_name


if __name__ == "__main__":
	log_tag('')
