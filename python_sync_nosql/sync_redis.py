#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @author: liukelin  314566990@qq.com

'''

 消费 数据 写入到redis

 没用上 conn_redis 重用。。。以后再补了
'''
import os
import config
import redis
import json


# redisConn=False
def conn_redis():
	conf = {
		    "host": config.redis_host,
		    "port": config.redis_port,
		    "db": 0
	    }
	# pool = redis.ConnectionPool(**conf)
	# redisConn = redis.Redis(ConnectionPool=pool)
	redisConn = redis.Redis(**conf)
	# print(redisConn, redisConn.ping())
	return redisConn

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
def set_data(body, redisConn=None):
	if not body or body=='':
		return False

	try:
		# 如果是bytes
		body = str(body, encoding = "utf-8")
	except:
		pass

	# 这个位置粗略的处理了下单引号json 实际可以再做处理
	# 有可能是单引号json
	body = body.replace("'", "\"")

	data = json.loads(body)
	if isinstance(data, (dict)) == False:
		return False
	print(data)

	if 'eventType' in data and 'after' in data and 'db' in data and 'table' in data:

		redis_cache_map = config.redis_cache_map
		db = data.get('db')
		table = data.get('table')


		if redis_cache_map.get(db) and redis_cache_map.get(db).get(table):
			key = "%s_%s_%s" %(db, table, data.get('after').get(redis_cache_map.get(db).get(table)) )
		else:
			return False

		if data.get('eventType') in ['UPDATE', 'INSERT', 'DELETE'] and data.get('after') and isinstance(data.get('after'), (dict)):

			if not redisConn:
				redisConn = conn_redis()

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

if __name__=="__main__":
	strs = """{'binlog': 'mysql-bin.000007:5078', 'db': 'zhou', 'table': 'uc_admin', 'eventType': 'UPDATE', 'before': {'create_id': '', 'create_time': '0000-00-00 00:00:00.000', 'id': '9', 'language_code': '', 'password': 'e10adc3949ba59abbe56e057f20f883e', 'status': '1', 'update_id': '', 'update_time': '0000-00-00 00:00:00.000', 'username': 'aa'}, 'after': {'create_id': '', 'create_time': '0000-00-00 00:00:00.000', 'id': '9', 'language_code': '', 'password': 'e10adc3949ba59abbe56e057f20f883e', 'status': '1', 'update_id': '', 'update_time': '0000-00-00 00:00:00.000', 'username': 'aab'}, 'time': '2017-11-07 21:24:56'}"""
	set_data(strs)
	# conn_redis()

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












