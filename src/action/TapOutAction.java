package action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import bean.TabOutData;
import control.TapOutControl;

public class TapOutAction extends BaseAction{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TapOutAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	String phonenumber;
	String type;
	String from;
	String to;
	
	
	//Excel 使用
	
	private InputStream excelStream;  //輸出變量
	private String excelFileName; //下載文件名稱

	String param;
	String tapOut_colHead;
	String tapOut_reportName;
	
	TapOutControl tapOutControl= new TapOutControl();
	
	public String queryTapOutData() {
		try {
			List<TabOutData> list = tapOutControl.queryTapOutData(from, to, phonenumber, type);
			result = makeResult(list, null);
			actionLogControl.loggerAction(super.getUser().getAccount(), "TapOut", "Query","from:"+from+" to:"+to+" phonenumber:"+phonenumber+ " type:"+type, SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter s = new StringWriter();
			e.printStackTrace(new PrintWriter(s));
			result = makeResult(null, s.toString());
		}
		
		return SUCCESS;
	}
	
	
	public String createExcel(){
			
		
		try {  
			//第一步，創建webbook文件
            HSSFWorkbook wb = new HSSFWorkbook();  

            //第二步，添加sheet
            HSSFSheet sheet = wb.createSheet("表格1");  
            //第三步添加表頭第0行
            HSSFRow row = sheet.createRow(0);  
            //第四步，設定樣式
            HSSFCellStyle style = wb.createCellStyle();  
            style.setAlignment(HSSFCellStyle.ALIGN_CENTER);  
            //第五部，建立單元格
            HSSFCell cell;  
  
            if(tapOut_colHead == null)
            	return null;
            
            System.out.println("colHead"+tapOut_colHead);
            JSONArray ja = new JSONArray(java.net.URLDecoder.decode(tapOut_colHead,"UTF-8"));
            
            for(int i = 0 ; i < ja.length() ; i++){
    			JSONObject jo = ja.getJSONObject(i);
    			
    			String col=jo.getString("name");
    			
    			if(!"button".equals(col)){
    				cell = row.createCell(i);  
                    cell.setCellValue(col);  
                    cell.setCellStyle(style); 
    			}
    		}		

  
            //第六部，寫入內容
            JSONObject joparam = new JSONObject(java.net.URLDecoder.decode(param,"UTF-8"));
            List<TabOutData> list = tapOutControl.queryTapOutData(joparam.getString("from"),joparam.getString("to"),joparam.getString("phonenumber"),joparam.getString("type"));
            
            
            JSONArray ja2 = (JSONArray) JSONObject.wrap(list);
            
            for (int i = 0; i < ja2.length(); i++) {  
            	JSONObject jo =ja2.getJSONObject(i);
                row = sheet.createRow(i+1);  
                
                for(int j = 0 ; j < ja.length() ; j++){

        			JSONObject jo2 = ja.getJSONObject(j);
        			String col=jo2.getString("name");
        			if(!"button".equals(col)){
        				row.createCell(j).setCellValue(jo.get(jo2.getString("col")).toString()); 
        			}
        		}	
            }  

            //第七步，放置串流  
            ByteArrayOutputStream os = new ByteArrayOutputStream();  
            wb.write(os);  
            byte[] fileContent = os.toByteArray();  
            ByteArrayInputStream is = new ByteArrayInputStream(fileContent);  
  
            excelStream = is;             //文件流  
            
            //reportName=java.net.URLDecoder.decode(reportName,"UTF-8");
            
            if(tapOut_reportName==null || "".equals(tapOut_reportName))
            	tapOut_reportName="report";            	
            
            excelFileName = tapOut_reportName+".xls"; //文件名稱
        }  
        catch(Exception e) {  
            e.printStackTrace();  
        }  
  
        return SUCCESS;  
	}

	public InputStream getExcelStream() {
		return excelStream;
	}

	public void setExcelStream(InputStream excelStream) {
		this.excelStream = excelStream;
	}

	public String getExcelFileName() {
		return excelFileName;
	}

	public void setExcelFileName(String excelFileName) {
		this.excelFileName = excelFileName;
	}



	public String getParam() {
		return param;
	}


	public void setParam(String param) {
		this.param = param;
	}




	public String getTapOut_colHead() {
		return tapOut_colHead;
	}


	public void setTapOut_colHead(String tapOut_colHead) {
		this.tapOut_colHead = tapOut_colHead;
	}


	public String getTapOut_reportName() {
		return tapOut_reportName;
	}


	public void setTapOut_reportName(String tapOut_reportName) {
		this.tapOut_reportName = tapOut_reportName;
	}


	public String getPhonenumber() {
		return phonenumber;
	}

	public void setPhonenumber(String phonenumber) {
		this.phonenumber = phonenumber;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	
	
}
