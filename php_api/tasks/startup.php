<?php 
/**
 * 此文件用于cli模式运行
 * 
 * 基于redis的任务队列，类似于python celery，可用rabbitMQ代替redis消费更可靠。
 * liukelin 
 **/
defined('__URL__') or define('__URL__',dirname(__FILE__).DIRECTORY_SEPARATOR); 
include_once(__URL__.'../config.php');
global $config;
$config = $conf;


class celery{
	
	private $queue_key = 'celery_startup_zset';
	
	/**
	 * 将执行任务写入队列
	 * @param unknown $queue 方法名
	 * @param unknown $args	 参数
	 * @return boolean
	 */	
	public function apply_async($queue, $args=array()){
		global $config;
		
		if(empty($queue)){
			return false;
		}
		$key = microtime(true)*10000;
		$arr = array(
			'fun'=>$queue,
			'args'=>$args,
			'key'=>$key,
		);
		$data = json_encode($arr);

		# push redis zset
		$r = $config['redis']['tasks'];
		$redis = new Redis();
		$redis->connect($r['host'],$r['port'],$r['db']);
		$redis->zadd($this->queue_key , $key, $data);
		return true;
	}
	
	/**
	 * 获取执行队列数据
	 */
	public function get_queue_data(){
		global $config;
		$r = $config['redis']['tasks'];
		$redis = new Redis();
		$redis->connect($r['host'],$r['port'],$r['db']);
		
		$redis->mutil();
		$redis->watch($this->queue_key);
		$json = $redis->zrange($this->queue_key , 0, 0);
		if(!empty($json)){
			$redis->zrem($this->queue_key, $json);
		}
		$redis->exec();
		
		$data = json_decode($json,true);
		
		//执行
		$ret = $this->start_up($data['fun'], $data['args']);
		if (!$ret) {
			#消费失败 数据回归队列
			$redis->zadd($this->queue_key , $data['key'], $json);
		}
		return $ret;
	}
	
	/**
	 * 执行方法脚本
	 * @param unknown $queue 方法名
	 * @param unknown $args  方法参数
	 * @return mixed|boolean 
	 */
	public function start_up($queue, $args=array()){		
		try {
			return call_user_func_array($queue, $args);
		}catch(Exception $e){
			return false;
		}
	}
}

//run 
//开启进程数量
$processNo = 5;

$on_out = system('ps -ef | grep "startup.php" | grep -v "grep" | wc -l',$out);
if (intval(trim($on_out)) > ($processNo+1)){
	exit('error on start!');
}

$st = new celery();
$st->get_queue_data();












