Canal MySql RabbitMQ Redis 的nosql同步 （多读、nosql延时不严格 需求）

1.mysql主从配置

2.对从库 mysql binlog(row) parser 这一步交给canal

3.MQ对解析后binlog增量数据的推送

4.对MQ数据的消费（接收+数据解析，考虑消费速度，MQ队列的阻塞）

5.数据写入/修改到nosql （redis的主从）

数据：mysql->binlog->MQ->redis(不过期、关闭RDB、AOF) （nosql数据仅用crontab脚本维护）

请求：http->webserver->redis(有数据)->返回数据 （完全避免用户直接读取mysql）

                    ->redis(无数据)->返回空




Mysql Redis/memcached nosql的缓存 （多读写需求）

1.对数据在mysql的hash算法分布(db/table/分区)，每个hash为节点

2.mysql主从

3.nosql数据的hash算法分布(多实例、DB)，每个hash为节点

4.nosql数据震荡处理 （当某节点挂了寻找替代节点算法（多层hash替代节点）。。。）

5.恢复节点数据

请求：http->webserver->【业务寻址hash节点】->1.redis(有数据)-> 返回数据

									    ->2.redis(无数据)-> mysql (并写入数据redis) -> 返回数据

									    ->3.redis节点挂掉-> 业务寻址hash替代节点 -> redis(有数据) -> 返回数据

									                                          -> redis(无数据) -> mysql (

									                                          并写入数据redis) -> 返回数据

.....end 后续。。。


