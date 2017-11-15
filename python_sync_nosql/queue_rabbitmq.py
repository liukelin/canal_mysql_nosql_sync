#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @author: liukelin  314566990@qq.com

'''

  读取 rabbitmq 数据

'''
import os
import config
import pika

if config.sync_db=='mongo':
	import sync_mongo as sync_data
else:
	import sync_redis as sync_data

'''
  消费 rabbitmq 队列数据
'''
def consumer_data():
	
	credentials = pika.PlainCredentials(config.rabbitmq_user, config.rabbitmq_pass) # 远程访问禁止使用 guest账号
	#这里可以连接远程IP，请记得打开远程端口
	parameters = pika.ConnectionParameters(config.rabbitmq_host, config.rabbitmq_port,'/',credentials) 
	# 建立连接
	connection = pika.BlockingConnection(parameters)
	
	channel = connection.channel()

	channel.queue_declare(queue=config.rabbitmq_queue_name, durable=True) # durable队列持久化（需生产端配合设置）
	

	def callback(ch, method, properties, body):

		# set data
		ack = sync_data.set_data(body) 

		# ACK 确认
		ch.basic_ack(delivery_tag = method.delivery_tag)



	channel.basic_qos(prefetch_count=1) # 允许暂留Unacked数量，（数据未basic_ack前 数据保存在Unacked允许的最大值）

	channel.basic_consume(callback, 
							queue=config.rabbitmq_queue_name,
							#no_ack=True,      #失效basic_ack
						)

	channel.start_consuming()
