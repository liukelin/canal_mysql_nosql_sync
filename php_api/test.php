<?php
defined('__URL__') or define('__URL__',dirname(__FILE__).DIRECTORY_SEPARATOR);     //项目路径
include_once(__URL__.'config.php');
global $config;
$config = $conf;
$r = $config['redis']['tasks'];

$redis = new Redis();
$redis->connect($config['redis']['tasks']['host'],$config['redis']['tasks']['port'],$config['redis']['tasks']['db']);
// $redis->set('k',1);
// $redis->zadd('celery_startup_zset' , microtime(true)*10000, 'basdad');

function test($a, $b){
	return $a;
}
echo microtime(true)*10000;




echo call_user_func("test", "loveapple", "Using class name.");