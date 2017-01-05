    canal client端封装

    获取canal server 的binlog

            ↓

    将binlog 数据组装成json格式

            ↓

    使用MQ传输 （rabbitmq、redis、kafka）

