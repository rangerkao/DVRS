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
		// TODO Auto-generated constructor stub
	}

	//查詢列表
	public List<QosBean> queryQosList() throws SQLException{
		sql=
				"SELECT A.PROVISIONID,A.IMSI,A.MSISDN,A.PLAN,A.ACTION,A.RESPONSE_CODE,A.RESULT_CODE,to_char(A.CERATE_TIME,'yyyyMMdd hh24:mi:ss') ctime "
				+ "FROM QOS_PROVISION_LOG A ";

		List<QosBean> list=new ArrayList<QosBean>();
		
			Statement st = conn.createStatement();
			ResultSet rs=st.executeQuery(sql);
			System.out.println("Execute SQL :"+sql);
			while(rs.next()){
				QosBean qosdata =new QosBean();
				qosdata.setProvisionID(rs.getInt("PROVISIONID"));
				qosdata.setImsi(rs.getString("IMSI"));
				qosdata.setMsisdn(rs.getString("MSISDN"));
				qosdata.setPlan(rs.getString("PLAN"));
				qosdata.setAction(rs.getString("ACTION"));
				qosdata.setResultCode((rs.getString("RESULT_CODE").contains("RETURN_CODE=0")?"成功":rs.getString("RESULT_CODE")));
				qosdata.setReturnCode(("200".equals(rs.getString("RESPONSE_CODE"))?"正常":rs.getString("RESPONSE_CODE")));
				qosdata.setCreateTime(rs.getString("ctime"));
				list.add(qosdata);
			}
			st.close();
			rs.close();
		
		closeConnect();
		
		return list;
		
	}
	
	//查詢列表
		public List<QosBean> queryQosList(String imsi,String msisdn) throws SQLException{
			sql=
					"SELECT A.PROVISIONID,A.IMSI,A.MSISDN,A.PLAN,A.ACTION,A.RESPONSE_CODE,A.RESULT_CODE,to_char(A.CERATE_TIME,'yyyyMMdd hh24:mi:ss') ctime "
					+ "FROM QOS_PROVISION_LOG A "
					+ "WHERE 1=1 "
					+ (imsi!=null && !"".equals(imsi) ? "AND A.IMSI like '"+imsi+"'" : "")
					+ (msisdn!=null && !"".equals(msisdn) ? "AND A.MSISDN like '"+msisdn+"'" : "");

			List<QosBean> list=new ArrayList<QosBean>();
			
			Statement st = conn.createStatement();
			ResultSet rs=st.executeQuery(sql);
			System.out.println("Execute SQL :"+sql);
			while(rs.next()){
				QosBean qosdata =new QosBean();
				qosdata.setProvisionID(rs.getInt("PROVISIONID"));
				qosdata.setImsi(rs.getString("IMSI"));
				qosdata.setMsisdn(rs.getString("MSISDN"));
				qosdata.setPlan(rs.getString("PLAN"));
				qosdata.setAction(rs.getString("ACTION"));
				qosdata.setResultCode((rs.getString("RESULT_CODE").contains("RETURN_CODE=0")?"成功":rs.getString("RESULT_CODE")));
				qosdata.setReturnCode(("200".equals(rs.getString("RESPONSE_CODE"))?"正常":rs.getString("RESPONSE_CODE")));
				qosdata.setCreateTime(rs.getString("ctime"));
				list.add(qosdata);
			}
			st.close();
			rs.close();
			
			closeConnect();
			
			return list;
			
		}
	
}
