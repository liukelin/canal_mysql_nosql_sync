Canal MySql RabbitMQ Redis 的nosql同步方案 （多读、nosql延时不严格 需求）
1.mysql主从配置
2.对从库 mysql binlog(row) parser 这一步交给canal
3.MQ对解析后binlog增量数据的推送
4.对MQ数据的消费（接收+数据解析，考虑数据消费速度）
5.数据写入/修改到nosql （redis的主从）
mysql->binlog->MQ->redis （nosql数据仅用脚本维护）
用户->webserver->redis->返回数据 （完全避免用户直接读取mysql）


Mysql Redis/memcached nosql的缓存方案 （多读写需求）
1.对数据在mysql的hash分布(db/table/分区)，每个hash为节点
2.mysql主从
3.对数据在nosql的hash分布(多实例、DB)，每个hash为节点
4.数据震荡处理 （多层hash替代节点。。。）
5.恢复节点数据


.....end 期待后续。。。


