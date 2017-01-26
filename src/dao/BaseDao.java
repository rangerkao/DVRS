package dao;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class BaseDao {

	protected String sql="";
	protected static Properties props =null;

	public BaseDao() throws Exception{

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
	
	public Connection getConn1() throws ClassNotFoundException, SQLException {
		return cache.CacheAction.getConn1();
	}
	
	public Connection getConn2() throws ClassNotFoundException, SQLException {
		return cache.CacheAction.getConn2();
	}

}
