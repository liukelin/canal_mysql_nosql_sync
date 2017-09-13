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
    print(redisConn, redisConn.ping())

'''
 ·将数据写入到 mongo
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
        
        if not client:
            Conn()

        # 指定数据库(db)
        db = client.(data.get('db'))

        # 指定集合(表)
        posts = db.(data.get('table'))

        if not posts:
            return False

        if data.get('eventType') in ['UPDATE', 'INSERT', 'DELETE'] and isinstance(data['after'], (dict)):
            
            coll = '';
            pid = 0;
            # 获取更新条件唯一值
            if mongo_cache_map.get(data.get('db')) and mongo_cache_map.get(data.get('db')).get(data.get('table')):
                coll = mongo_cache_map.get(data.get('db')).get(data.get('table'))
                pid = data.get(coll)

            else:
                return False

            if data.get('eventType')=='INSERT':

                posts.insert(data['after'])
                # posts.save(data.get('table'))

            else if data.get('eventType')=='UPDATE':

                collection.update( { coll:pid } , {'$set': data['after'] })

            else if data.get('eventType')=='DELETE':

                posts.remove( { coll:pid } )

            # try:
            #   redisConn.hmset(key, data['after'])
            # except:
            #   conn_redis()
            #   redisConn.hmset(key, data['after'])
            return True

    return False








