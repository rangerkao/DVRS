package dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import bean.QosBean;

public class QosDao extends BaseDao {

	public QosDao() throws Exception {
		super();
	}

	//�d�ߦC��
	public List<QosBean> queryQosList() throws SQLException{
		sql=
				"SELECT A.PROVISIONID,A.IMSI,A.MSISDN,A.PLAN,A.ACTION,A.RESPONSE_CODE,A.RESULT_CODE,to_char(A.CERATE_TIME,'yyyyMMdd hh24:mi:ss') ctime "
				+ "FROM QOS_PROVISION_LOG A "
				+ "ORDER BY A.CERATE_TIME DESC";

		List<QosBean> list=new ArrayList<QosBean>();
		
			Statement st = conn.createStatement();
			ResultSet rs=st.executeQuery(sql);
			System.out.println("Execute SQL :"+sql);
			while(rs.next()){
				QosBean qosdata =new QosBean();
				String rc = rs.getString("RESULT_CODE");
				if(rc!=null ){
					if(rc.contains("RETURN_CODE=0")){
						rc="成功";
					}
				}else{
					rc="";
				}
				
				String rc2 = rs.getString("RESPONSE_CODE");
				if(rc2!=null ){
					if(rc2.contains("200")){
						rc2="正常";
					}
				}else{
					rc2="";
				}
				qosdata.setProvisionID(rs.getInt("PROVISIONID"));
				qosdata.setImsi(rs.getString("IMSI"));
				qosdata.setMsisdn(rs.getString("MSISDN"));
				qosdata.setPlan(rs.getString("PLAN"));
				qosdata.setAction(rs.getString("ACTION"));
				qosdata.setResultCode(rc);
				qosdata.setReturnCode(rc2);
				qosdata.setCreateTime(rs.getString("ctime"));
				list.add(qosdata);
			}
			st.close();
			rs.close();
			
		return list;
		
	}
	
	//�d�ߦC��
		public List<QosBean> queryQosList(String imsi,String msisdn) throws SQLException{
			sql=
					"SELECT A.PROVISIONID,A.IMSI,A.MSISDN,A.PLAN,A.ACTION,A.RESPONSE_CODE,A.RESULT_CODE,to_char(A.CERATE_TIME,'yyyyMMdd hh24:mi:ss') ctime "
					+ "FROM QOS_PROVISION_LOG A "
					+ "WHERE 1=1 "
					+ (imsi!=null && !"".equals(imsi) ? "AND A.IMSI like '"+imsi+"'" : "")
					+ (msisdn!=null && !"".equals(msisdn) ? "AND A.MSISDN like '"+msisdn+"'" : "")
					+ " ORDER BY A.CERATE_TIME DESC";;

			List<QosBean> list=new ArrayList<QosBean>();
			
			Statement st = conn.createStatement();
			ResultSet rs=st.executeQuery(sql);
			System.out.println("Execute SQL :"+sql);
			while(rs.next()){
				QosBean qosdata =new QosBean();
				
				String rc = rs.getString("RESULT_CODE");
				if(rc!=null ){
					if(rc.contains("RETURN_CODE=0")){
						rc="成功";
					}
				}else{
					rc="";
				}
				
				String rc2 = rs.getString("RESPONSE_CODE");
				if(rc2!=null ){
					if(rc2.contains("200")){
						rc2="正常";
					}
				}else{
					rc2="";
				}
				
				qosdata.setProvisionID(rs.getInt("PROVISIONID"));
				qosdata.setImsi(rs.getString("IMSI"));
				qosdata.setMsisdn(rs.getString("MSISDN"));
				qosdata.setPlan(rs.getString("PLAN"));
				qosdata.setAction(rs.getString("ACTION"));
				qosdata.setResultCode(rc);
				qosdata.setReturnCode(rc2);
				qosdata.setCreateTime(rs.getString("ctime"));
				list.add(qosdata);
			}
			st.close();
			rs.close();
			
			return list;
			
		}
	
}
