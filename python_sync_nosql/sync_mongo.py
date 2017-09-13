#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @author: liukelin  314566990@qq.com

'''

 消费 数据 写入到mongodb

'''
import os
import config
import json
import pymongo
from pymongo import MongoClient

client=False
def Conn():
    client = MongoClient(config.mongo_host, config.mongo_port)
    print(client)

'''
 ·将数据写入到 mongo
  这里的demo是将表数据映射到 mongodb 结构
  db    => db
  table => 集合
  column=> coll

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
        
        if not client:
            Conn()

        mongo_cache_map = config.mongo_cache_map
        db = data.get('db')
        table = data.get('table')

        # 指定数据库(db)
        dbc = client.db
        # 指定集合(表)
        posts = dbc.table

        if not posts:
            return False

        if data.get('eventType') in ['UPDATE', 'INSERT', 'DELETE'] and isinstance(data.get('after'), (dict)):
            
            coll = '';  # 唯一字段名
            pid = 0;    # 值
            # 获取更新条件唯一值
            if mongo_cache_map.get(db) and mongo_cache_map.get(db).get(table):
                coll = mongo_cache_map.get(db).get(table)
                pid = data.get(coll)

            else:
                return False

            if data.get('eventType')=='INSERT':
                posts.insert( data.get('after') )
                # posts.save(data.get('table'))

            elif data.get('eventType')=='UPDATE':
                posts.update( { coll:pid } , {'$set': data.get('after') } )

            elif data.get('eventType')=='DELETE':
                posts.remove( { coll:pid } )

            # try:
            #   redisConn.hmset(key, data['after'])
            # except:
            #   conn_redis()
            #   redisConn.hmset(key, data['after'])
            return True

    return False

'''
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
body = json.dumps(body)
set_data(body)
'''






