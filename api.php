<?php
/**
* API
*
**/
header('Content-type:application/json; charset=utf-8');
date_default_timezone_set('PRC');

defined('__URL__') or define('__URL__',dirname(__FILE__).DIRECTORY_SEPARATOR);     //项目路径
defined('LOGDIR') or define('LOGDIR', '/Logs');
defined('STATUS_SUCCESS') or define('STATUS_SUCCESS', 0);
defined('STATUS_FAIL') or define('STATUS_FAIL',-1);
defined('T_PFLOG') or define('T_PFLOG','api_logs');
defined('DEBUG') or define('DEBUG',false);

include_once(__URL__.'config.php');
global $config;
$config = $conf;

if (DEBUG == true) {
    error_reporting(E_ALL);
    ini_set('display_errors', 1);
}else {
    error_reporting(0);
}

foreach ($_REQUEST as $k=>$v){
    $$k = mysql_escape_string($v);
}
switch($action){

	case 'get': // 获取数据
		$infoArr = array('action','id');
		filter_($infoArr);

		if(empty($id)){
			exit_(array('code'=>STATUS_FAIL,'msg'=>'not id.'));
		}

		get_data($id);
		exit_(array('code'=>STATUS_SUCCESS,'msg'=>'success'));

		break;

	case 'set': //新增数据
		$infoArr = array('action','val');
		filter_($infoArr);

		set_data($val);

		break;

	default:
	    exit('no action!');
	    break;
}
exit();



//get data for mysql
function get_mysql_data(){

}

//insert data for mysql
function set_mysql_data(){
	
}

//get data for redis
function get_redis_data($id){
	if(empty($id)) return null;

	$conf = generate_hash($id, $conf['redis']['num']);
	$redis = new redis();  
	$result = $redis->connect('127.0.0.1', 6379);
}

//set data for redis
function set_redis_data($data){

}

/**
 * hash 计算
 * $str   计算字符
 * $count 节点数
 * redis/mysql
 * return hash 值
	//一般情况而言需要做字符串到十进制数字的转换可以用 crc32() 函数 ...
	//这个函数的优点是快 ... 在我的知识范围内这应该是 php 内置最快的哈希函数 ...
	//缺点是操作系统依赖 ... 不同操作系统的 crc32() 函数会产生不一样的值 ...
	//base_convert() 这个函数的好处是返回 string ... 也就不存在溢出的问题了 ...
*/
function generate_hash($str, $count, $type='redis'){
	return base_convert( md5($str), 16, 10 ) % $count;
}

/**
 * 参数限制
 * $infoArr HTTP 允许参数名
 */
function filter_($infoArr=array()){
    $infoArr[] = 'callback';
    foreach ($_REQUEST as $k=>$v){
        if(!in_array($k,$infoArr)){
            unset($$k);
            $$k = null;
            $_REQUEST[$k] = null;
            $_POST[$k]    = null;
            $_GET[$k]     = null;
        }else{

        }
    }
}
 
//返回值处理
function exit_($infoArr){
    exit(empty($_REQUEST['callback'])?json_encode($infoArr):$_REQUEST['callback'].'('.json_encode($infoArr).')');
}

?>