#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @author: liukelin  314566990@qq.com

'''
  将数据写入到 nosql
  startup

'''
import os
import config
import pika
import queue_rabbitmq

print(' ====liukelin==== ')
print(' [*] Waiting for messages. To exit press CTRL+C')
get_rabbitmq.consumer_data()
