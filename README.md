Canal MySql RabbitMQ Redis 的nosql同步 （多读、nosql延时不严格 需求）

	1.mysql主从配置

	2.对从库 mysql binlog(row) parser 这一步交给canal

	3.MQ对解析后binlog增量数据的推送

	4.对MQ数据的消费（接收+数据解析，考虑消费速度，MQ队列的阻塞）

	5.数据写入/修改到nosql （redis的主从）

	6.保证对应关系的简单性：一个mysql表对应一个 redis实例（redis单线程，多实例保证分流不阻塞），关联关系数据交给接口业务

	数据：mysql->binlog->MQ->redis(不过期、关闭RDB、AOF保证读写性能) （nosql数据仅用crontab脚本维护）

	请求：http->webserver->redis(有数据)->返回数据 （完全避免用户直接读取mysql）

	                    ->redis(无数据)->返回空




Mysql Redis/memcached nosql的缓存 （多读写需求）

	1.对数据在mysql的hash算法分布(db/table/分区)，每个hash为节点（nosql数据全部失效时候，可保证mysql各节点可支持直接读取的性能）

	2.mysql主从

	3.nosql数据的hash算法分布(多实例、DB)，每个hash为节点

	4.nosql数据震荡处理 （当某节点挂了寻找替代节点算法（多层hash替代节点）。。。）

	5.恢复节点数据

	6.

	请求：http->webserver->【业务寻址hash节点】->1.redis(有数据)-> 返回数据

										    ->2.redis(无数据)-> mysql (并写入数据redis) -> 返回数据

										    ->3.redis节点挂掉-> 业务寻址hash替代节点 -> redis(有数据) -> 返回数据

										                                          -> redis(无数据) -> mysql (

										                                          并写入数据redis) -> 返回数据


===============部署===============
阿里canal纯java开发，所以要先安装java环境

安装jdk(推荐jdk1.8):
	安装过程参考网上资料，（注意环境变量配置）

mysql配置：
	1.编辑mysql配置文件
		$ sudo vim /etc/my.cnf
		
		[mysqld]  
		log-bin=mysql-bin # 
		binlog-format=ROW #选择row模式  
		server_id=1 	  #实例唯一ID，不能和canal的slaveId重复
	
	保存并退出，并重启mysql
		$ sudo service mysql restart
	
	2.创建 mysql账号密码（账号密码自定）
	
		CREATE USER canal IDENTIFIED BY 'canal';    
		GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';  
		-- GRANT ALL PRIVILEGES ON *.* TO 'canal'@'%' ;  
		FLUSH PRIVILEGES;

canal server 配置启动：
	
	1.下载解压项目，这里提供了1.0.22版本，可从下载最新版本：https://github.com/alibaba/canal/releases
	
	2.配置项目：
		# 公共配置
		$ sudo vim conf/canal.properties
			
			canal.port= 11111 # 保证该端口为占用状态，或者使用其他未占用端口
		
		保存退出。
		
		# 实例配置
		$ sudo vim conf/example/instance.properties
			
			# position info
			canal.instance.master.address = 127.0.0.1:3306 # mysql连接
			
			canal.instance.dbUsername = canal  		# mysql账号
			canal.instance.dbPassword = canal		# 密码
			canal.instance.defaultDatabaseName = duobao	# 需要同步的库名
			canal.instance.connectionCharset = UTF-8	# mysql编码
		
		保存退出。
		更多配置查看：http://agapple.iteye.com/blog/1831873
		
	3.启动：
		$ sh bin/startup.sh

canal client 配置启动：
	
	1.下载解压项目，这里提供了1.0.22版本，

		
.....end 后续。。。


