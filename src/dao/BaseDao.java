package dao;

import java.sql.Connection;

import program.IJatool;
import program.Jatool;


public class BaseDao {

	protected String sql="";
	protected Connection conn=null;
	protected Connection conn2=null;
	protected IJatool tool= new Jatool();
	
	BaseDao() throws Exception{
		conn=cache.CacheAction.getConnect1();
		conn2=cache.CacheAction.getConnect2();
	}
	
	
	
	
}
