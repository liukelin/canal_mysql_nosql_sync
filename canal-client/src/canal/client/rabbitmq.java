/**
 * rabbitmq  
 * 将数据写入到rabbitmq
 * @date 2016-08-27
 * @author liukelin
 * @email 314566990@qq.com
 */
package canal.client;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

//rabbitmq(rabbitmq-client.jar)
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;

public class rabbitmq {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	// 将信息push 到 rabbitmq
    public void push_rabbitmq(Map<String, String> conf, String[] argv) throws java.io.IOException{
    	String rabbitmq_host = conf.get("rabbitmq_host");
    	int rabbitmq_port = Integer.parseInt(conf.get("rabbitmq_port"));
    	String rabbitmq_user = conf.get("rabbitmq_user");
    	String rabbitmq_pass = conf.get("rabbitmq_pass");
    	String rabbitmq_queuename = conf.get("rabbitmq_queuename");
    	String rabbitmq_ack = conf.get("rabbitmq_ack");
    	String rabbitmq_durable = conf.get("rabbitmq_durable");
    	Boolean durable = false;
    	if(rabbitmq_durable.equals("true")) {durable=true;}
    	
    	ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitmq_host);
        factory.setPort(rabbitmq_port);
        factory.setUsername(rabbitmq_user);
        factory.setPassword(rabbitmq_pass);
        
        Connection connection = null;
		try {
			connection = factory.newConnection();
		} catch (TimeoutException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.out.println("connection rabbitmq error!");
		}
        Channel channel = connection.createChannel();
        channel.queueDeclare(rabbitmq_queuename, durable, false, false, null);

        //String message = getMessage(argv);
        for(int i=0;i<argv.length;i++){
        	channel.basicPublish( "", rabbitmq_queuename,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    argv[i].getBytes()
                    );
        }

        try {
			channel.close();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        connection.close();
    }

}
