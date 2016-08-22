Canal MySql RabbitMQ Redis/memcached/mongodb 的nosql同步 （多读、nosql延时不严格 需求）

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


部署(详情查看wiki):

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
	
	canal server 模拟mysql从库并向mysql发送dump命令获取mysql binlog数据。
	
	1.下载解压项目，这里提供了1.0.22版本:canal.deployer-1.0.22.tar.gz(https://github.com/liukelin/canal_mysql_nosql_sync/files/426724/canal.deployer-1.0.22.tar.gz) 可从下载最新版本：https://github.com/alibaba/canal/releases
	
	2.配置项目：
		# 公共配置
		$ sudo vim conf/canal.properties
			
			canal.port= 11111 # canal server 运行端口，保证该端口为占用状态，或者使用其他未占用端口
		
		保存退出。
		
		# 实例配置
		$ sudo vim conf/example/instance.properties
			
			# position info
			canal.instance.master.address = 127.0.0.1:3306  # mysql连接
			
			canal.instance.dbUsername = canal  		# mysql账号
			canal.instance.dbPassword = canal		# 密码
			canal.instance.defaultDatabaseName = test	# 需要同步的库名
			canal.instance.connectionCharset = UTF-8	# mysql编码
		
		保存退出。
		
		更多配置查看：http://agapple.iteye.com/blog/1831873
		
	3.启动：
		$ sh bin/startup.sh
		
	日志文件：$ less logs/canal/canal.log	   # canal server端运行日志
		  $ less logs/example/example.log  # canal client端连接日志
		  $ logs/example/meta.log 	   # 实例binlog 读取记录文件

canal client 配置启动：
	
	canal client将从canal server获取的binlog数据最终以json行格式保存到指定文件。
	
	1.下载解压项目，这里自己写了个基于1.0.22版本的项目:canal_client1.0.22.zip(https://github.com/liukelin/canal_mysql_nosql_sync/files/426769/canal_client1.0.22.zip), 源码查看：
		
	2.基本配置
		
		$vim conf/canal.properties
		
		# cancal server host， canal server的连接IP
		canal.server.host = 127.0.0.1
		
		# cancal server port，canal server的连接端口 
		canal.server.port = 11111
		
		# 实例 默认 example
		canal.server.instance = example
		
		# 每次获取binlog数据 行数
		canal.batchsize = 1000
		
		# 每次获取等待时间单位/ms
		canal.sleep = 1000
		
		# 数据保存路径 ，自行指定
		canal.binlog.dir = /home/deploy/log/db_data
		
		保存退出。
	
	3.启动：
	
		$ sh start_canal_client.sh
		

最终结果：
 /home/deploy/log/db_data/binlog_xxxx.log
 
 {"binlog":"mysql-bin.000008:26280","db":"duobao","table":"orders_code","eventType":"INSERT","before":"","after":{"code":"10000027","code_id":"339","create_time":"2016-08-21 16:48:46","orders_id":"145","orders_no":"20160821164844108919","period_id":"17","uid":"1","user":"123"},"time":"2016-08-22 09:53:28"}

消费数据：
	如：client1和client2 需要消费这些数据， 他们得到的数据一样
	开始考虑直接用队列：
	队列数据： [A, B, C, D] 
	client1 ：
	           消费进程1：获取AB
	           消费进程2：获取CD
	
	client2 ：
	           消费进程1：获取AB
	           消费进程2：获取CD
	
	这样的话，如果使用rabbitMQ 就必须给每个 client 提供独立的队列。并独立消费
	使用kafka，利用他的分组group,每个client 为一个组，这样就可保证，数据给每个组一致。
	


		
.....end 后续。。。


