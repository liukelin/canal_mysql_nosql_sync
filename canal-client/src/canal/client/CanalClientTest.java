/**
 * canal client  
 * 从canal server 获取 binlog，并写入文件
 * @date 2016-08-13
 * @author liukelin
 * @email 314566990@qq.com
 */
package canal.client;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map; //数组
import java.util.HashMap;// 数组

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
import java.io.UnsupportedEncodingException;
//import java.io.UnsupportedEncodingException;

//输出时间
import java.util.Date;
import java.text.SimpleDateFormat;

//写入文件
import java.io.FileWriter;
//url encode
//import java.net.URLEncoder;

public class CanalClientTest {
	
	private static String path = CanalClientTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	public static String canal_print = "0";
	public static String canal_binlog_filename = "h"; //保存文件名
	public static String data_dir = "data"; //数据保存路径
	
	public static void main(String args[]) {
		String conf_path = path.substring(0, path.lastIndexOf("/")) + "/conf/canal.properties";
		//String host = AddressUtils.getHostIp()
		String host = "127.0.0.1";
		int port = 11111;
		String instance = "example";
        int batchSize = 1000;  //每次获取数据数量
        int sleep = 1000; //无数据时等待时间
        
        System.out.println("#=====canal client====================\r\n#=====2016====================\r\n" +
        				   "#=====liukelin====================\r\n" +
        				   "#=====conf:"+conf_path);
        
        //读取配置
        try {
        	Properties prop = new Properties();
            InputStream in = new FileInputStream(conf_path);
//            InputStream in = new FileInputStream("/Users/liukelin/Desktop/canal-otter-mycat-cobar/canal_object/conf/canal.properties");
            
        	prop.load(in);
            host = prop.getProperty("canal.server.host").trim();   
            port = Integer.parseInt(prop.getProperty("canal.server.port").trim());
            String conf_instance = prop.getProperty("canal.server.instance").trim();
            
            String conf_batchsize = prop.getProperty("canal.batchsize").trim();
            String conf_sleep = prop.getProperty("canal.sleep").trim();
            String conf_dir = prop.getProperty("canal.binlog.dir").trim();
            String conf_filename = prop.getProperty("canal.binlog.filename").trim();
            String conf_print = prop.getProperty("canal.print").trim();
            
            if ( conf_instance!= null && conf_instance!=""){
            	instance = conf_instance;
            }
            if ( conf_batchsize!= null && conf_batchsize!=""){
            	batchSize = Integer.parseInt(conf_batchsize);
            }
            if (conf_sleep!= null && conf_sleep!=""){
            	sleep = Integer.parseInt(conf_sleep);
            }
            if (conf_dir!= null && conf_dir!=""){
            	data_dir = conf_dir;
            }
            if (conf_filename!= null && conf_filename!=""){
            	canal_binlog_filename = conf_filename;
            }
            if (conf_print!= null && conf_print!=""){
            	canal_print = conf_print;
            }
            
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
            //int totalEmtryCount = 120;  //120s无更新则退出 
            
            System.out.println("connect success!\r\n startup...");
            
//          while (emptyCount < totalEmtryCount) {
            while (true){
                Message message = connector.getWithoutAck(batchSize); // 获取指定数量的数据  
                long batchId = message.getId();  
                int size = message.getEntries().size();  
                if (batchId == -1 || size == 0) {  
                    //emptyCount++;  
                    //System.out.println("empty count : " + emptyCount);  
                    try {  
                        Thread.sleep(sleep); // 等待时间
                    } catch (InterruptedException e) {  
                    }  
                } else {  
                    //emptyCount = 0;  
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
        String timeStr = df.format(new Date()); // new Date()为获取当前系统时间
    	
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
                	after = printColumn(rowData.getBeforeColumnsList());  
                } else if (eventType == EventType.INSERT) {  
                	after = printColumn(rowData.getAfterColumnsList());  
                } else {  //update
                    //System.out.println("-------> before");  
                    before = printColumn(rowData.getBeforeColumnsList());  
                    //System.out.println("-------> after");  
                    after = printColumn(rowData.getAfterColumnsList());  
                }
                
                String row_data = header_str + row_str + "\"before\":" +before + ",\"after\":" + after + ",\"time\":\"" + timeStr +"\"}\r\n";
                save_data_logs(row_data);
                //System.out.println(row_data);
            }  
        }  
    }  
    
    // 获取字段 变更  (1、使用map转换为json。 2、使用urlencode。  避免拼接json错误)
    private static String printColumn(List<Column> columns) {
    	//String column_str = "";
    	Map<String, String> column_map = new HashMap<String, String>();
        for (Column column : columns) {
        	String column_name;
        	String column_value;
        	/**
        	try {
				column_name = URLEncoder.encode(column.getName(),"utf-8");
				column_value = URLEncoder.encode(column.getValue(),"utf-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				column_name = column.getName();
				column_value = column.getValue();
			}**/
        	column_name = column.getName();
			column_value = column.getValue();
			
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
			
        	column_map.put(column_name, column_value);

        	//column_str = column_str +"\""+ column_name + "\":\"" + column_value + "\",";
            //System.out.println(column.getName() + " : " + column.getValue() + " update=" + column.getUpdated());  
        }
        //return "{" + column_str.substring(0,column_str.length()-1) + "}"; //去除最后一个字符
        return JSON.toJSONString(column_map);        
    }
    
    //save data log
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
            writer.write(row_data);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("write file error!");
        }
    	
    }
    
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
