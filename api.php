<?php
/**
* API
*
**/

include_once('config.php');

// foreach ($_REQUEST as $k=>$v){
//     $$k = $v;
// }

switch($_REQUEST['action']){

	case 'get':
		$infoArr = array('action','id'); //允许参数
		filter_($infoArr);

		break;

	case 'set':
		$infoArr = array('action','val');
		filter_($infoArr);

		break;

	default:
	    exit('no action!');
	    break;
}
exit();

//insert data for mysql
function set_data(){
	
}

//get data for redis
function get_data(){
	$redis = new redis();  
	$result = $redis->connect('127.0.0.1', 6379);
}

//参数限制
function filter_($infoArr=array()){
    $infoArr[] = 'callback';
    foreach ($_REQUEST as $k=>$v){
        if(!in_array($k,$infoArr)){
            unset($$k);
            $$k = null;
            $_REQUEST[$k] = null;
            $_POST[$k]    = null;
            $_GET[$k]     = null;
        }
    }
}
 
//返回值处理
function exit_($infoArr){
    exit(empty($_REQUEST['callback'])?json_encode($infoArr):$_REQUEST['callback'].'('.json_encode($infoArr).')');
}

//hash 
//一般情况而言需要做字符串到十进制数字的转换可以用 crc32() 函数 ...
//这个函数的优点是快 ... 在我的知识范围内这应该是 php 内置最快的哈希函数 ...
//缺点是操作系统依赖 ... 不同操作系统的 crc32() 函数会产生不一样的值 ...
// base_convert() 这个函数的好处是返回 string ... 也就不存在溢出的问题了 ...
function generate_hash($str, $count){
	return base_convert( md5($str), 16, 10 ) % $count;
}

?>