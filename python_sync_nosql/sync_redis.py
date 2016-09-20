#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @author: liukelin  314566990@qq.com

'''

 消费 rabbitmq 数据 同步到redis

'''
import os
import config
import redis
import json


redisConn=False
def conn_redis():
	pool = redis.ConnectionPool(host=redis_host, port=redis_port, db=0)
	redisConn = redis.Redis(ConnectionPool=pool)
	print(redisConn, redisConn.ping())

'''
 写入到redis
'''
def set_redis(body):
	if not body or body=='':
		return False
	data = json.loads(body)
	if isinstance(data, (dict)) == False:
		return False

	if data.has_key('eventType') and data.has_key('after') and data.has_key('db') and data.has_key('table'):

		if redis_cache_map.has_key(data['db']) and redis_cache_map[data['db']].has_key(data['table']):
			key = redis_cache_map[data['db']][data['table']]
		else:
			return False

		if data['eventType'] in ['UPDATE', 'INSERT', 'DELETE'] and isinstance(data['after'], (dict)):
			try:
				redisConn.hmset(key, data['after'])
			except:
				conn_redis()
				redisConn.hmset(key, data['after'])
			return True
	return False


'''
{
 "binlog":"mysql-bin.000027:258",
 "db":"test",
 "table":"user",
 "eventType":"UPDATE",
 "before":{"id":"4","name":"user1"},
 "after":{"id":"4","name":"user2"},
 "time":"2016-09-09 10:43:48"}

{
 "binlog":"mysql-bin.000027:258",
 "db":"test",
 "table":"user",
 "eventType":"INSERT",
 "after":{"id":"4","name":"user1"},
 "time":"2016-09-09 10:43:48"} 

{
 "binlog":"mysql-bin.000027:258",
 "db":"test",
 "table":"user",
 "eventType":"DELETE",
 "after":{"id":"4","name":"user2"},
 "time":"2016-09-09 10:43:48"}
'''












