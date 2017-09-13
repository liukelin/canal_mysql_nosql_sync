#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @author: liukelin  314566990@qq.com

'''

 消费 数据 写入到redis

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
 ·将数据写入到redis
  这里的demo是将表数据映射到 redis hash 结构，key=db:table:primary_id
  body={
	    "binlog": "mysql-bin.000009:1235",
	    "db": "test",
	    "table": "users",
	    "eventType": "UPDATE",
	    "before": {
	        "uid": "8",
	        "username": "duobao153713223"
	    },
	    "after": {
	        "uid": "8",
	        "username": "duobao153713223"
	    },
	    "time": "2016-08-22 17:47:25"
	}
'''
def set_data(body):
	if not body or body=='':
		return False
	data = json.loads(body)
	if isinstance(data, (dict)) == False:
		return False


	if data.has_key('eventType') and data.has_key('after') and data.has_key('db') and data.has_key('table'):
		redis_cache_map = config.get('redis_cache_map')

		if redis_cache_map.has_key(data['db']) and redis_cache_map[data['db']].has_key(data['table']):
			key = "%s_%s_%s" %(data['db'], data['table'], redis_cache_map[data['db']][data['table']])
		else:
			return False

		if data.get('eventType') in ['UPDATE', 'INSERT', 'DELETE'] and isinstance(data['after'], (dict)):
			
			if not redisConn:
				conn_redis()

			if data.get('eventType')=='INSERT':

				redisConn.hmset(key, data['after'])

			else if data.get('eventType')=='UPDATE':

				redisConn.hmset(key, data['after'])

			else if data.get('eventType')=='DELETE':

				redisConn.del(key)

			# try:
			# 	redisConn.hmset(key, data['after'])
			# except:
			# 	conn_redis()
			# 	redisConn.hmset(key, data['after'])
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












