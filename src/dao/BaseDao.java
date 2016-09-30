package dao;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class BaseDao {

	protected String sql="";
	protected static Properties props =null;
	protected Connection conn=null;
	protected Connection conn2=null;

	public BaseDao() throws Exception{
		createConnection();
	}
	
	public String nvl(String msg,String s){
		if(msg==null)
			msg = s;
		return msg;
	}
	
	public String convertString(String msg,String sCharset,String dCharset) throws UnsupportedEncodingException{
		
		if(msg==null)
			msg="";
		
		return sCharset==null? new String(msg.getBytes(),dCharset):new String(msg.getBytes(sCharset),dCharset);
	}
	
	public void createConnection() throws Exception{
		
		
		conn=cache.CacheAction.getConnect1();
		conn2=cache.CacheAction.getConnect2();
		
	}
	public void closeConnection() throws SQLException{
		/*conn.close();
		conn2.close();
		System.out.println("Close connect!");*/
	}	
}
