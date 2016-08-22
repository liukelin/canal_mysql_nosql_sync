<?php

$conf = array(

	'redis'=>array(
		'num'=> 4,
		'default' => array('host' => '127.0.0.1', 'port'=>'6379', 'db'=>'0'),
		'tasks'=> array('host' => '127.0.0.1', 'port'=>'6379', 'db'=>'1'), # 用于任务队列redis
		'0' => array('host' => '127.0.0.1', 'port'=>'6379', 'db'=>'1'),
		'1' => array('host' => '127.0.0.1', 'port'=>'6379', 'db'=>'2'),
		'2' => array('host' => '127.0.0.1', 'port'=>'6379', 'db'=>'3'),
		'3' => array('host' => '127.0.0.1', 'port'=>'6379', 'db'=>'4'),
	),

	'mysql'=>array(
		'num'=> 4,
		'default' => array(
			'host'=>'127.0.0.1', 
			'port'=>3306, 
			'user'=>'root', 
			'passwd'=>'123456', 
			'db'=>'k_kelin',
			'charset'=>'utf8'
			),
		'0' => array(
			'host'=>'127.0.0.1', 
			'port'=>3306, 
			'user'=>'root', 
			'passwd'=>'123456', 
			'db'=>'k_kelin',
			'charset'=>'utf8'
			),
		'1' => array(
			'host'=>'127.0.0.1', 
			'port'=>3306, 
			'user'=>'root', 
			'passwd'=>'123456', 
			'db'=>'k_kelin',
			'charset'=>'utf8'
			),
		'2' => array(
			'host'=>'127.0.0.1', 
			'port'=>3306, 
			'user'=>'root', 
			'passwd'=>'123456', 
			'db'=>'k_kelin',
			'charset'=>'utf8'
			),
		'3' => array(
			'host'=>'127.0.0.1', 
			'port'=>3306, 
			'user'=>'root', 
			'passwd'=>'123456', 
			'db'=>'k_kelin',
			'charset'=>'utf8'
			),
	),

);


?>