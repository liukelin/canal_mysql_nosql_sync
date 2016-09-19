#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @author: liukelin  314566990@qq.com
'''
 消费 rabbitmq binlog 数据 同步到redis

'''

import os
import config
import pika
import redis
import json


redisConn=False
def conn_redis():
	pool = redis.ConnectionPool(host=redis_host, port=redis_port, db=0)
	redisConn = redis.Redis(ConnectionPool=pool)
	print(redisConn, redisConn.ping())

'''
 获取rabbitmq队列数据
'''
def get_mq():
	
	credentials = pika.PlainCredentials(config.rabbitmq_user, config.rabbitmq_pass) # 远程访问禁止使用 guest账号
	#这里可以连接远程IP，请记得打开远程端口
	parameters = pika.ConnectionParameters(config.rabbitmq_host, config.rabbitmq_port,'/',credentials) 
	# 建立连接
	connection = pika.BlockingConnection(parameters) # 本机 parameters可直接写localhost
	# 创建channel
	channel = connection.channel()

	channel.queue_declare(queue=queue_name, durable=True) # durable队列持久化（需生产端配合设置）
	print(' [*] Waiting for messages. To exit press CTRL+C')

	def callback(ch, method, properties, body):

		# redis
		ack = sync_redis(body)

	    ch.basic_ack(delivery_tag = method.delivery_tag) # ACK确认

	channel.basic_qos(prefetch_count=1) # 允许暂留Unacked数量，（数据未basic_ack前 数据保存在Unacked允许的最大值）

	channel.basic_consume(callback, 
							queue=queue_name,
							#no_ack=True,      #失效basic_ack
						)

	channel.start_consuming()


'''
 写入到redis
'''
def sync_redis(body):
	
	data = json.loads(body)
	if isinstance(data, (dict)) == False:
		return False

	if data.has_key('eventType'):
		if data['eventType'] == 'UPDATE':

		elif data['eventType'] == 'INSERT':

		elif data['eventType'] == 'DELETE':

		else:
			pass

	else:



	try:
		checkConn = conn_redis.ping()
		if checkConn:
			pass
		else:
			conn_redis()
	except:
		conn_redis()

'''
{"binlog":"mysql-bin.000027:258",
"db":"duobao",
"table":"category",
"eventType":"UPDATE",
"before":{"icon":"http://duobao.qianlu.com/static/icon/0008.png","id":"4","name":"????","order":"3","path":"/","status":"0"},
"after":{"icon":"http://duobao.qianlu.com/static/icon/0008.png","id":"4","name":"????","order":"4","path":"/","status":"0"},"time":"2016-09-09 10:43:48"}

'''











