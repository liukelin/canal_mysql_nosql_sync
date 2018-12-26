


下图是最基本的web服务器的结构图。
![image](https://github.com/liukelin/canal_mysql_nosql_sync/raw/master/img/system-image.png)

基于 Canal 的 MySql RabbitMQ Redis/memcached/mongodb 的nosql同步 （多读、nosql延时不严格 需求）

	1.mysql主从配置

	2.对mysql binlog(row) parser 这一步交给canal

	3.MQ对解析后binlog增量数据的推送

	4.对MQ数据的消费（接收+数据解析，考虑消费速度，MQ队列的阻塞）

	5.数据写入/修改到nosql （redis的主从/hash分片）

	6.保证对应关系的简单性：一个mysql表对应一个 redis实例（redis单线程，多实例保证分流不阻塞），关联关系数据交给接口业务

	数据：mysql->binlog->MQ->redis(不过期、关闭RDB、AOF保证读写性能) （nosql数据仅用crontab脚本维护）

	请求：http->webserver->redis(有数据)->返回数据 （完全避免用户直接读取mysql）

	                    ->redis(无数据)->返回空
	
	7.可将它视为一个触发器，binlog为记录触发事件，canal的作用是将事件实时通知出来，并将binlog解析成了所有语言可读的工具。
	在事件传输的各个环节 提高 可用性 和 扩展性 （加入MQ等方法）最终提高系统的稳定。



传统 Mysql Redis/memcached nosql的缓存 （业务同步）
        从cache读取数据->

	1.对数据在mysql的hash算法分布(db/table/分区)，每个hash为节点（nosql数据全部失效时候，可保证mysql各节点可支持直接读取的性能）

	2.mysql主从

	3.nosql数据的hash算法分布(多实例、DB)，每个hash为节点

	4.nosql数据震荡处理 （当某节点挂了寻找替代节点算法（多层hash替代节点）。。。）

	5.恢复节点数据

	6.请求：http->webserver->【对key计算一致性hash节点】->connect对应的redis实例
	                                                    
	                                                    ->1.redis(有数据)-> 返回数据

				                            ->2.redis(无数据)-> mysql (并写入数据redis) -> 返回数据

		                                            ->3.redis节点挂掉-> 业务寻址hash替代节点 
		                                                                      -> 3.1 redis(有数据) -> 返回数据

										      -> 3.2 redis(无数据) -> mysql(并写入数据redis) -> 返回数据


 ![image](https://github.com/liukelin/canal_mysql_nosql_sync/raw/master/img/canal-mysql-nosql.png)
 
 
为什么要使用消息队列（MQ）进行binlog传输:
	
	1.增加缓冲，binlog生产端（canal client）只负责生产而不需要考虑消费端的消费能力, 不等待阻塞。
	
	2.binlog 消费端: 可实时根据MQ消息的堆积情况，动态 增加/减少 消费端的数量，达到合理的资源利用和消费	
	
	
部署:

	阿里canal纯java开发，所以要先安装java环境

安装jdk(推荐jdk1.8):
	安装过程参考网上资料，（注意环境变量配置）

mysql配置：
	1.编辑mysql配置文件
		$ sudo vim /etc/my.cnf
		
		[mysqld]  
		log-bin=mysql-bin #binlog文件名（也可以使用绝对路径）
		binlog-format=ROW #选择row模式  
		server_id=1 	  #实例唯一ID，不能和canal的slaveId重复
	
	保存并退出，并重启mysql
		$ sudo service mysql restart
	
	2.创建 mysql账号密码（账号密码自定 权限自定）
	
		CREATE USER canal IDENTIFIED BY 'canal';    
		GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';  
		-- GRANT ALL PRIVILEGES ON *.* TO 'canal'@'%' ;  
		FLUSH PRIVILEGES;

 canal server 配置启动：
	
	canal server 模拟mysql从库并向mysql发送dump命令获取mysql binlog数据。
	
	1.下载解压项目，这里提供了1.0.22版本:
	[canal.deployer-1.0.22.tar.gz](https://github.com/liukelin/canal_mysql_nosql_sync/releases) 
	可从阿里项目下载最新版本 deployer ：
	[https://github.com/alibaba/canal/releases](https://github.com/alibaba/canal/releases)
	
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
		
		更多配置查看:
		[http://agapple.iteye.com/blog/1831873](http://agapple.iteye.com/blog/1831873)
		
	3.启动：
		$ sh bin/startup.sh
		
	日志文件： $ less logs/canal/canal.log	 # canal server端运行日志
		  $ less logs/example/example.log   # canal client端连接日志
		  $ logs/example/meta.log 	    # 实例binlog 读取记录文件（记录变更位置，默认为新增变更(tail)）

 canal client 配置启动：
	
	canal client将从canal server获取的binlog数据最终以json行格式保存到指定文件(也可省略这步，直接发送到MQ)。
	
	binlog生产端和消费端的之间，增加MQ作为缓冲，增加容错度和动态扩展性
	
	1.下载解压项目，这里自己写了个基于1.0.22版本的项目:
	[canal_client1.0.22.zip](https://github.com/liukelin/canal_mysql_nosql_sync/releases)
	
	源码查看: [canal-client](https://github.com/liukelin/canal_mysql_nosql_sync/tree/master/canal-client)
		
	2.基本配置
		
		$vim conf/canal.properties
		
		# cancal server host， 上面 canal server的IP
		canal.server.host = 127.0.0.1
		
		# cancal server port，上面 canal server的启动端口 
		canal.server.port = 11111
		
		# 数据保存路径 ，自行指定
		canal.binlog.dir = db_data
		
		# 可选rabbitmq/redis/kafka 作为队列（这里使用 rabbitmq 作为队列传输）
		canal.mq = rabbitmq 

		###### rabbitmq 基本配置 #####
		rabbitmq.host = 127.0.0.1
		rabbitmq.port = 5672
		rabbitmq.user = test
		rabbitmq.pass = 123456

		
		保存退出。
	
	3.启动canal client：
	
		$ sh start_canal_client.sh
		

修改mysql数据触发。

最终结果：
		
	eventType ：操作类型（UPDATE/INSERTDELETE）
	
	db：   涉及库
	
	table: 涉及表
	
	before:变更前数据
	
	after: 变更后数据
	
	time:  操作时间
	

        $less db_data/binlog_xxxx.log
 
        {"binlog":"mysql-bin.000009:1235","db":"test","table":"users","eventType":"UPDATE","before":{"uid":"8","username":"duobao153713223"},"after":{"uid":"8","username":"duobao153713223"},"time":"2016-08-22 17:47:25"}

        {"binlog":"mysql-bin.000009:1533","db":"test","table":"users","eventType":"DELETE","before":"","after":{"uid":"8","username":"duobao153713223"},"time":"2016-08-22 17:48:09"}

        {"binlog":"mysql-bin.000009:1790","db":"test","table":"users","eventType":"INSERT","before":"","after":{"uid":"9","username":"test2"},"time":"2016-08-22 17:48:45"}

消费数据demo：（这里使用python3 消费rabbitmq同步到redis 作为案例，实际可根据业务需求，因为此时所需要的数据已是通用的json格式，无限可能）
        
	流程 ：file数据-> MQ -> nosql
	
	MQ: rabbitMQ
	
	语言：python3
	
	NoSql: redis
	
	
	多项目订阅需求，如：client1和client2 需要消费这些数据， 他们得到的数据一样
	开始考虑直接用队列：
	队列数据： [A, B, C, D] 
	client1 ：
	           消费进程1：获取AB
	           消费进程2：获取CD
	
	client2 ：
	           消费进程1：获取AB
	           消费进程2：获取CD
	
	这样的话，如果使用rabbitMQ 就必须给每个 client 提供独立的队列。并独立消费
	1、使用kafka，利用他的分组group,每个client 为一个组，这样就可保证，数据给每个组一致。
	2、对每个项目需求开独立的 canal server instance 和 canal client实例
	

	配置：
	   语言：python3
	   pip：pika redis
	   
	   项目代码： python_sync_nosql
	   修改配置文件config.py
	   	# 最终存储数据redis
		redis_host = '127.0.0.1'
		redis_port = 6379
		
		###### rabbitmq 基本配置 #####
		rabbitmq_host = '127.0.0.1'
		rabbitmq_port = 5672
		rabbitmq_user = 'test'    
		rabbitmq_pass = '123456'
		
		# 设置对每个table存储使用的key字段
		redis_cache_map = {
			# db
			'test':{
				# table ： kid
				'users':'uid', 
		  	}
		}
	 
	 运行脚本：
	 	$ python3 startup.py
	 

数据最终存储为Redis 的 hash结构，key为 db_table_id
![image](https://github.com/liukelin/canal_mysql_nosql_sync/raw/master/img/redis-hash.png)

同步到MongoDB同理
	
	  这里的demo是将表数据映射到 mongodb 结构
	  db    => db
	  table => 集合
	  column=> coll
![image](https://github.com/liukelin/canal_mysql_nosql_sync/raw/master/img/mongo.png)


    
## 目录结构

~~~

├─canal-client/         封装的canal client客户端 和 消息队列MQ 项目
│  ├─src/           	项目代码
│  ├─lib/           	jar包依赖
│  ├─conf/            	配置文件
│  ├─canal_client.jar   启动jar
│  └─start_canal_client.sh     启动文件
│  
├─python_sync_nosql/        	消费MQ binlog数据, 将数据写入到NoSql demo
│  ├─queue_rabbitmq.py 		rabbitmq 消费端
│  ├─sync_redis.py 		写入到redis
│  ├─sync_mongo.py 		写入到mongo
│  ├─config.py 			配置
│  └─startup.py         	启动入口
└─

~~~

	总结：
	
	1.使用MQ作为传输，可提高容错度，并且可以起到一个消费速度的缓冲，在程序上加上对队列积压数据的监控，可实时增加或减少MQ消费进程的数量。
	
	2.为了提高binlog数据的可靠消费，建议使用带有ACK功能的MQ 做为消息队列使用
	
	3.为了避免多进程对MQ消费速度的时序先后不可控，建议binlog数据只作为触发条件（使用id从mysql获取最新数据）作为数据使用，而不作为具体数据使用。
	
	4. 接下来我会继续完善otter的实际案例 ...


<h1>资源下载</h1>

 canal server 服务端deployer： [https://github.com/alibaba/canal/releases/tag/canal-1.0.22](https://github.com/alibaba/canal/releases/tag/canal-1.0.22)

 
 canal client 客户端： [https://github.com/liukelin/canal_mysql_nosql_sync/releases/tag/1.0.22.2](https://github.com/liukelin/canal_mysql_nosql_sync/releases/tag/1.0.22.2)

 阿里canal项目原始地址：[https://github.com/alibaba/canal](https://github.com/alibaba/canal)
 
 数据消费写入nosql例子: python_sync_nosql 这里是消费rabbitmq数据最终同步到redis
 
 其他说明：[使用canal和canal_mysql_nosql_sync同步mysql数据](https://www.aliyun.com/jiaocheng/1114823.html?spm=5176.100033.2.18.hXjlSb)

使用golang语言开发的版本：[https://github.com/liukelin/bubod](https://github.com/liukelin/bubod)


