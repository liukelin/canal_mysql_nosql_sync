/**
 * canal client  
 * 从canal server 获取 binlog，并写入文件
 * @date 2016-08-13
 * @author liukelin
 * @email 314566990@qq.com
 */
package canal.client;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map; 
import java.util.HashMap;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry.Column;
import com.alibaba.otter.canal.protocol.CanalEntry.Entry;
import com.alibaba.otter.canal.protocol.CanalEntry.EntryType;
import com.alibaba.otter.canal.protocol.CanalEntry.EventType;
import com.alibaba.otter.canal.protocol.CanalEntry.RowChange;
import com.alibaba.otter.canal.protocol.CanalEntry.RowData;
import com.alibaba.otter.canal.protocol.Message;
import com.alibaba.fastjson.JSON; //alibaba的FastJson(高性能JSON开发包)

//读取 Properties
import java.util.Properties;
import java.io.InputStream;   
import java.io.IOException;
import java.io.FileInputStream;
//import java.io.UnsupportedEncodingException;

//时间
import java.util.Date;
import java.util.concurrent.TimeoutException;
import java.text.SimpleDateFormat;

//写入文件
import java.io.FileWriter;
//url encode
//import java.net.URLEncoder;

//rabbitmq(rabbitmq-client.jar)
//import com.rabbitmq.client.ConnectionFactory;
//import com.rabbitmq.client.Connection;
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.MessageProperties;

public class CanalClientTest {
	
	private static String path = CanalClientTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	public static String canal_print = "0";
	
	//canal server
	public static String host = "127.0.0.1";
	public static int port = 11111;
	public static String instance = "example";
	public static int batchSize = 1000;     //每次获取数据数量
	public static int sleep = 1000; 		//无数据时等待时间
	
	//file
	public static String canal_binlog_filename = "h"; //保存文件名
	public static String data_dir = "data"; //数据保存路径
	
	//mq
	public static String canal_mq; // redis/rabbitmq/kafka
	
	// rabbitmq
	public static String rabbitmq_host = "127.0.0.1";
	public static String rabbitmq_port = "5672";
	public static String rabbitmq_user = "";
	public static String rabbitmq_pass = "";
	public static String rabbitmq_queuename = "canal_binlog_data"; //队列名称
	public static String rabbitmq_ack = "false"; //ack
	public static String rabbitmq_durable = "false"; //队列持久
	public static Map<String,String> rabbitmq_conf;
	
	// redis
	public static String redis_host = "127.0.0.1";
	public static String redis_port = "5672";
	public static String redis_user = "";
	public static String redis_pass = "";
	public static String redis_queuename = "canal_binlog_data"; //队列名称
	public static Map<String,String> redis_conf;
	
	public static void main(String args[]) {
		String conf_path = path.substring(0, path.lastIndexOf("/")) + "/conf/canal.properties";
		//String host = AddressUtils.getHostIp()
        
        System.out.println("#=====canal client====================\r\n#=====2016====================\r\n" +
        				   "#=====liukelin====================\r\n" +
        				   "#=====conf:"+conf_path);
        
        //读取配置
        try {
        	Properties prop = new Properties();
            InputStream in = new FileInputStream(conf_path);
//            InputStream in = new FileInputStream("/Users/liukelin/Desktop/canal-otter-mycat-cobar/canal_object/conf/canal.properties");
            
        	prop.load(in);
            String conf_host = prop.getProperty("canal.server.host");   
            String conf_port = prop.getProperty("canal.server.port");
            String conf_instance = prop.getProperty("canal.server.instance");
            
            String conf_batchsize = prop.getProperty("canal.batchsize");
            String conf_sleep = prop.getProperty("canal.sleep");
            String conf_dir = prop.getProperty("canal.binlog.dir");
            String conf_filename = prop.getProperty("canal.binlog.filename");
            String conf_print = prop.getProperty("canal.print");
            
            String conf_mq = prop.getProperty("canal.mq");
            
            String conf_rabbitmq_host = prop.getProperty("rabbitmq.host");
        	String conf_rabbitmq_port = prop.getProperty("rabbitmq.port");
        	String conf_rabbitmq_user = prop.getProperty("rabbitmq.user");
        	String conf_rabbitmq_pass = prop.getProperty("rabbitmq.pass");
        	String conf_rabbitmq_queuename = prop.getProperty("rabbitmq.queuename");
        	String conf_rabbitmq_ack = prop.getProperty("rabbitmq.ack");
        	String conf_rabbitmq_durable = prop.getProperty("rabbitmq.durable");
        	
        	String conf_redis_host = prop.getProperty("redis.host");
        	String conf_redis_port = prop.getProperty("redis.port");
        	String conf_redis_user = prop.getProperty("redis.user");
        	String conf_redis_pass = prop.getProperty("redis.pass");
        	String conf_redis_queuename = prop.getProperty("redis.queuename");
            
        	if ( conf_host!= null && conf_host!=""){
        		host = conf_host.trim();
            }
        	if ( conf_port!= null && conf_port!=""){
        		port = Integer.parseInt(conf_port.trim());
            }

            if ( conf_instance!= null && conf_instance!=""){
            	instance = conf_instance.trim();
            }
            if ( conf_batchsize!= null && conf_batchsize!=""){
            	batchSize = Integer.parseInt(conf_batchsize.trim());
            }
            if (conf_sleep!= null && conf_sleep!=""){
            	sleep = Integer.parseInt(conf_sleep.trim());
            }
            if (conf_dir!= null && conf_dir!=""){
            	data_dir = conf_dir.trim();
            }
            if (conf_filename!= null && conf_filename!=""){
            	canal_binlog_filename = conf_filename.trim();
            }
            if (conf_print!= null && conf_print!=""){
            	canal_print = conf_print.trim();
            }
            if (conf_mq!= null && conf_mq!=""){
            	canal_mq = conf_mq.trim();
            }
            
            if (conf_rabbitmq_host!= null && conf_rabbitmq_host!=""){
            	rabbitmq_host = conf_rabbitmq_host.trim();
            }
            if (conf_rabbitmq_port!= null && conf_rabbitmq_port!=""){
            	rabbitmq_port = conf_rabbitmq_port.trim();
            }
            if (conf_rabbitmq_user!= null && conf_rabbitmq_user!=""){
            	rabbitmq_user = conf_rabbitmq_user.trim();
            }
            if (conf_rabbitmq_pass!= null && conf_rabbitmq_pass!=""){
            	rabbitmq_pass = conf_rabbitmq_pass.trim();
            }
            if (conf_rabbitmq_queuename!= null && conf_rabbitmq_queuename!=""){
            	rabbitmq_queuename = conf_rabbitmq_queuename.trim();
            }
            if (conf_rabbitmq_ack!= null && conf_rabbitmq_ack!=""){
            	rabbitmq_ack = conf_rabbitmq_ack.trim();
            }
            if (conf_rabbitmq_durable!= null && conf_rabbitmq_durable!=""){
            	rabbitmq_durable = conf_rabbitmq_durable.trim();
            }
            
            if (conf_redis_port!= null && conf_redis_port!=""){
            	redis_host = conf_redis_host.trim();
	        }
	        if (conf_redis_port!= null && conf_redis_port!=""){
	        	redis_port = conf_redis_port.trim();
	        }
	        if (conf_redis_user!= null && conf_redis_user!=""){
	        	redis_user = conf_redis_user.trim();
	        }
	        if (conf_redis_pass!= null && conf_redis_pass!=""){
	        	redis_pass = conf_redis_pass.trim();
	        }
	        if (conf_redis_queuename!= null && conf_redis_queuename!=""){
	        	redis_queuename = conf_redis_queuename.trim();
	        }
            
            rabbitmq_conf = new HashMap<String, String>();
            rabbitmq_conf.put("rabbitmq_host", rabbitmq_host);
            rabbitmq_conf.put("rabbitmq_port", rabbitmq_port);
            rabbitmq_conf.put("rabbitmq_user", rabbitmq_user);
            rabbitmq_conf.put("rabbitmq_pass", rabbitmq_pass);
            rabbitmq_conf.put("rabbitmq_queuename", rabbitmq_queuename);
            rabbitmq_conf.put("rabbitmq_ack", rabbitmq_ack);
            rabbitmq_conf.put("rabbitmq_durable", rabbitmq_durable);
            
            redis_conf = new HashMap<String, String>();
            redis_conf.put("host", redis_host);
            redis_conf.put("port", redis_port);
            redis_conf.put("user", redis_user);
            redis_conf.put("pass", redis_pass);
            redis_conf.put("queuename", redis_queuename);
       
            
            System.out.println("#=====host:"+host+":"+port+ "\r\n#=====instance:"+instance+"\r\n");

        } catch (IOException e) {   
            e.printStackTrace();
            System.out.println("#=====load conf/canal.properties error!");
        }

        // 创建链接   (example 为server conf/example配置)
        CanalConnector connector = CanalConnectors.newSingleConnector(new InetSocketAddress(host, port), instance, "", "");  
        //int emptyCount = 0;  
        try {  
            connector.connect();  
            connector.subscribe(".*\\..*");  
            connector.rollback();
            
            System.out.println("connect success!\r\n startup...");
            
            while (true){
                Message message = connector.getWithoutAck(batchSize); // 获取指定数量的数据  
                long batchId = message.getId();  
                int size = message.getEntries().size();  
                if (batchId == -1 || size == 0) {
                    //System.out.println("empty count : " + emptyCount);  
                    try {  
                        Thread.sleep(sleep); // 等待时间
                    } catch (InterruptedException e) {  
                    	
                    }  
                } else {
                    //System.out.printf("message[batchId=%s,size=%s] \n", batchId, size);  
                    printEntry(message.getEntries());  
                }  
  
                connector.ack(batchId); // 提交确认  
                // connector.rollback(batchId); // 处理失败, 回滚数据  
            }
            //System.out.println("empty too many times, exit");
        } finally {
        	System.out.println("connect error!");
            connector.disconnect();  
        }  
    }  
  
    private static void printEntry(List<Entry> entrys) {
    	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStr = df.format(new Date());
    	
        ArrayList<String> dataArray = new ArrayList<String> ();
    	
        //循环每行binlog
        for (Entry entry : entrys) {
            if (entry.getEntryType() == EntryType.TRANSACTIONBEGIN || entry.getEntryType() == EntryType.TRANSACTIONEND) {  
                continue;  
            }  
  
            RowChange rowChage = null;  
            try {  
                rowChage = RowChange.parseFrom(entry.getStoreValue());  
            } catch (Exception e) {  
                throw new RuntimeException("ERROR ## parser of eromanga-event has an error , data:" + entry.toString(),e);  
            }  
            
            //单条 binlog sql
            EventType eventType = rowChage.getEventType();
            /**
            System.out.println(String.format("================> binlog[%s:%s] , name[%s,%s] , eventType : %s",  
                                             entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),  
                                             entry.getHeader().getSchemaName(), entry.getHeader().getTableName(),  
                                             eventType)); **/
            
            String header_str = "{\"binlog\":\"" + entry.getHeader().getLogfileName()+ ":" + entry.getHeader().getLogfileOffset() + "\"," +
            					"\"db\":\"" + entry.getHeader().getSchemaName() + "\"," +
            					"\"table\":\"" + entry.getHeader().getTableName() + "\",";
            //受影响 数据行
            for (RowData rowData : rowChage.getRowDatasList()) {
            	String row_str = "\"eventType\":\"" + eventType +"\",";
            	String before = "\"\"";
            	String after = "\"\"";
            	
            	//获取字段值 
                if (eventType == EventType.DELETE) {  
                	before = printColumn(rowData.getBeforeColumnsList());  
                } else if (eventType == EventType.INSERT) {  
                	after = printColumn(rowData.getAfterColumnsList());  
                } else {  //update
                    //System.out.println("-------> before");  
                    before = printColumn(rowData.getBeforeColumnsList());  
                    //System.out.println("-------> after");  
                    after = printColumn(rowData.getAfterColumnsList());  
                }
                
                String row_data = header_str + row_str + "\"before\":" +before + ",\"after\":" + after + ",\"time\":\"" + timeStr +"\"}";
                dataArray.add(row_data);   
                save_data_logs(row_data);
                //System.out.println(row_data);
            }  
        }
        
        // ArrayList<String>  TO String[]
        String[] strArr = dataArray.toArray(new String[]{});
        try {
        	if(canal_mq.equals("rabbitmq")){
        		rabbitmq r = new rabbitmq();
        		r.push_rabbitmq(rabbitmq_conf,strArr);
        		//push_rabbitmq(strArr);
        	}else if(canal_mq.equals("redis")){
        		redis r = new redis();
        		r.push_redis(redis_conf, strArr);
        	}else if(canal_mq.equals("kafka")){
        		
        	}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("push "+ canal_mq +" error!");
		}
        dataArray = null;
    }  
    
    // 获取字段 变更  (1、使用map转换为json。 2、使用urlencode。  避免拼接json错误)
    private static String printColumn(List<Column> columns) {
    	//String column_str = "";
    	Map<String, String> column_map = new HashMap<String, String>();
        for (Column column : columns) {
        	String column_name = column.getName();
        	String column_value = column.getValue();
			
			/**
			String a = "";
			String b = "";
			String c = "";
			try {
				column_value = new String(column_value.getBytes(),"UTF-8");
				a=new String(column.getValue().getBytes("Shift-JIS"),"GBK");
				b= new String(column.getValue().getBytes("Shift_JIS"), "GB2312");
				c= new String(column.getValue().getBytes("ISO-8859-1"), "UTF-8");
				
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				//column_value = column.getValue();
			}
			System.out.println("column_value:" + column_value + " : "+getEncoding(column_value)+
					", a:"+ a+" : " +getEncoding(a) +
					", b:"+ b+" : " +getEncoding(b) +
					", c:"+ c+" : " +getEncoding(c));
			**/
        	column_map.put(column_name, column_value);
            //System.out.println(column.getName() + " : " + column.getValue() + " update=" + column.getUpdated());  
        }
        return JSON.toJSONString(column_map);
    }
    
    //save data file
    private static void save_data_logs(String row_data){
    	if (canal_print.equals("true")){
    		System.out.println(row_data);
    	}
        
    	String ts = "yyyyMMdd";
        if (canal_binlog_filename.equals("y")){
        	ts = "yyyy";
        }else if (canal_binlog_filename.equals("m")){
        	ts = "yyyyMM";
        }else if (canal_binlog_filename.equals("d")){
        	ts = "yyyyMMdd";
        }else if (canal_binlog_filename.equals("h")){
        	ts = "yyyyMMddHH";
        }else if (canal_binlog_filename.equals("i")){
        	ts = "yyyyMMddHHmm";
        }else{
        	
        }
        SimpleDateFormat df2 = new SimpleDateFormat(ts);
        String timeStr2 = df2.format(new Date());
    	String filename = data_dir + "/binlog_" + timeStr2 + ".log";
    	
    	FileWriter writer;
        try {
            writer = new FileWriter(filename, true);
            writer.write(row_data + "\r\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("write file error!");
        }
    }
    
    /**
    // 将信息push 到 rabbitmq
    private static void push_rabbitmq(String[] argv) throws java.io.IOException{
    	ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitmq_host);
        factory.setPort(Integer.parseInt(rabbitmq_port));
        factory.setUsername(rabbitmq_user);
        factory.setPassword(rabbitmq_pass);
        
        Boolean durable = false;
        if(rabbitmq_durable.equals("true")) {durable=true;}
        
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
                    argv[i].getBytes());
        }

        try {
			channel.close();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        connection.close();
    }**/
    
    
    //check String type
    public static String getEncoding(String str) {
		String[] array = { "Shift_JIS", "GB2312", "ISO-8859-1", "UTF-8", "GBK", "ASCII", "Big5", "Unicode" };
		for (int i = 0; i < array.length; i++) {
			try {
				if (str.equals(new String(str.getBytes(array[i]), array[i]))) {
					String s = array[i];
					if (s.equals("Shift_JIS")) {
						return "unknow";
					}
					return s;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return "other";
	}
    
    
    
}


/**
 * 多线程包concurrent ， Executor 多线程操作
 */


