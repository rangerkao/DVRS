package action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;



public class Excel extends BaseAction{
	public Excel() throws Exception {
		super();
	}
	private InputStream excelStream;  //��X�ܶq
	private String excelFileName; //�U�����W��

	String dataList;
	String colHead;
	String reportName;
	
	public String createExcel(){
			
		
		try {  
			//�Ĥ@�B�A�Ы�webbook���
            HSSFWorkbook wb = new HSSFWorkbook();  

            //�ĤG�B�A�K�[sheet
            HSSFSheet sheet = wb.createSheet("���1");  
            //�ĤT�B�K�[���Y��0��
            HSSFRow row = sheet.createRow(0);  
            //�ĥ|�B�A�]�w�˦�
            HSSFCellStyle style = wb.createCellStyle();  
            style.setAlignment(HSSFCellStyle.ALIGN_CENTER);  
            //�Ĥ����A�إ߳椸��
            HSSFCell cell;  
  
            
            JSONArray ja = new JSONArray(java.net.URLDecoder.decode(colHead,"UTF-8"));
            
            for(int i = 0 ; i < ja.length() ; i++){
    			JSONObject jo = ja.getJSONObject(i);
    			
    			String col=jo.getString("name");
    			
    			if(!"button".equals(col)){
    				cell = row.createCell(i);  
                    cell.setCellValue(col);  
                    cell.setCellStyle(style); 
    			}
    		}		

  
            //�Ĥ����A�g�J���e
            JSONArray ja2 = new JSONArray(java.net.URLDecoder.decode(dataList,"UTF-8"));
            
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

            //�ĤC�B�A��m��y  
            ByteArrayOutputStream os = new ByteArrayOutputStream();  
            wb.write(os);  
            byte[] fileContent = os.toByteArray();  
            ByteArrayInputStream is = new ByteArrayInputStream(fileContent);  
  
            excelStream = is;             //���y  
            
            //reportName=java.net.URLDecoder.decode(reportName,"UTF-8");
            
            if(reportName==null || "".equals(reportName))
            	reportName="report";            	
            
            excelFileName = reportName+".xls"; //���W��
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

	public String getDataList() {
		return dataList;
	}

	public void setDataList(String dataList) {
		this.dataList = dataList;
	}

	public String getColHead() {
		return colHead;
	}

	public void setColHead(String colHead) {
		this.colHead = colHead;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}


}
