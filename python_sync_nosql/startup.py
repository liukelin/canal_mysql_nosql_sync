#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @author: liukelin  314566990@qq.com

'''
  将数据写入到 redis
  startup

'''
import os
import config
import pika
import get_rabbitmq

print(' ====liukelin==== ')
print(' [*] Waiting for messages. To exit press CTRL+C')
get_rabbitmq.get_mq()
