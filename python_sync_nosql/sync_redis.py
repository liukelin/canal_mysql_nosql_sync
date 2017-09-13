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


	if 'eventType' in data and 'after' in data and 'db' in data and 'table' in data:

		redis_cache_map = config.redis_cache_map
		db = data.get('db')
		table = data.get('table')

		if mongo_cache_map.get(db) and mongo_cache_map.get(db).get(table):
			key = "%s_%s_%s" %(db, table, data.get(redis_cache_map.get(db).get(table)) )
		else:
			return False

		if data.get('eventType') in ['UPDATE', 'INSERT', 'DELETE'] and isinstance(data.get('after'), (dict)):

			if not redisConn:
				conn_redis()

			if data.get('eventType')=='INSERT':

				redisConn.hmset(key, data.get('after'))

			elif data.get('eventType')=='UPDATE':

				redisConn.hmset(key, data.get('after'))

			elif data.get('eventType')=='DELETE':

				redisConn.delete(key)

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












