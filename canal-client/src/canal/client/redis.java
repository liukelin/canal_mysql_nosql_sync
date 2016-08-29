/**
 * @author liukelin
 * rpush / lpop 
 */
package canal.client;

import java.util.Map;

import com.rabbitmq.client.MessageProperties;

import redis.clients.jedis.Jedis;

public class redis {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//连接本地的 Redis 服务
	    Jedis jedis = new Jedis("192.168.179.184");
	    System.out.println("Connection to server sucessfully");
	    //查看服务是否运行
	    System.out.println("Server is running: "+jedis.ping());
	}
	
	//
	public void push_redis(Map<String, String> conf, String[] argv) throws java.io.IOException {
		String host = conf.get("host");
    	int port = Integer.parseInt(conf.get("port"));
    	String user = conf.get("user");
    	String pass = conf.get("pass");
    	String queuename = conf.get("queuename");
    	
    	Jedis jedis = new Jedis(host, port);
        for(int i=0;i<argv.length;i++){
        	jedis.rpush(queuename, argv[i]);
        }
        
	}

}
