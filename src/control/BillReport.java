package control;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;










import java.util.regex.Pattern;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import bill.bean.BillData;
import bill.bean.Charge;
import bill.bean.ChargeDetail;
import bill.bean.Invoice;
import bill.bean.InvoiceDetail;
import bill.bean.Usage;
import bill.bean.UsageDetail;

public class BillReport{
	
	
	private static String FileName;
	private static final String filePath =BillReport.class.getClassLoader().getResource("").toString().replace("file:/", "")+ "source/";
	
	
	public BillData process(String fileName){
		
		FileName=fileName;
		
		//FileName="85266400998.txt";
		
		System.out.println("filePath:"+filePath+FileName);
		
		BufferedReader reader = null;
		
		String str = null;
		String[] data;
		
		BillData result=new BillData();
		
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath+FileName))); // 指定讀取文件的編碼格式，以免出現中文亂碼
			
			
			while ((str = reader.readLine()) != null) {
				data=str.split("\t");
				String s= data[0];
				
				List<String> list=new ArrayList<String>();
				for (int i = 0; i < data.length; i++) {
					list.add(data[i]);
				}

				if("I".equalsIgnoreCase(s)){
					result.setI(new Invoice(data));
				}else if("J".equalsIgnoreCase(s)){
					list.add(3, null);//OrderSequence null
					result.getJ().add(new InvoiceDetail(list));
				}else if("C".equalsIgnoreCase(s)){
					result.setC(new Charge(data));
				}else if("D".equalsIgnoreCase(s)){
					list.add(3,null);//CategorySequence null
					result.getD().add(new ChargeDetail(list));
				}else if("U".equalsIgnoreCase(s)){
					result.setU(new Usage(data));
				}else if("R".equalsIgnoreCase(s)){
					result.getR().add(new UsageDetail(list));
				}

			//System.out.println(str);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
			
			reader.close();
			
			} catch (IOException e) {
			
			e.printStackTrace();
			
			}
		}
		System.out.println("read file "+fileName+" finish!");
		
		
		
		creatPDF(result);
		
		
		return result;
	}
	
	private void creatPDF(BillData data){
		System.out.println("Start to create bill PDF!");
		
		String templateName="bill2.jrxml";
		String PDFPath=filePath+FileName.replace("txt", "pdf");
		
		try {
			System.out.println("read file convert to jasperFile!");
			String jasperFile=JasperCompileManager.compileReportToFile(filePath+templateName);
			System.out.println("convert to jasperFile finished!");
			System.out.println("");
			
			//參數設置
			Map<String,Object> map=new HashMap<String,Object>();
			//地址
			map.put("address for",
					data.getI().getPostalCode()+"\n"
					+data.getI().getBillingAddressLine1()+"\n"
					+data.getI().getBillingAddressLine2()+"\n"
					+"\n"
					+data.getI().getAddressee()+"  先生/小姐");
			
			map.put("address for1", data.getI().getPostalCode());
			map.put("address for2", data.getI().getBillingAddressLine1());
			map.put("address for3", data.getI().getBillingAddressLine2());
			map.put("address for4", data.getI().getAddressee()+"  先生/小姐");
			
			//資料
			map.put("Statement for", data.getI().getCustomerName());
			map.put("Account Number", data.getI().getAccountName());
			map.put("Billing Period", data.getI().getCycleBeginDate()+"~"+data.getI().getCycleEndDate());
			map.put("Currency", "HKD");
			
			//封面項目
			map.put("Previous Balance", new Double(data.getI().getAccountBalance()));
			map.put("Payment Received", new Double(data.getI().getPaymentPosted()));
			map.put("Balance", data.getJ());
			map.put("applied date", data.getI().getDueDate());
			
			//無法利用subreport回傳，在java中計算
			Double currentTotal=0D;
			for(InvoiceDetail j:data.getJ()){
				if(Pattern.matches("^\\d+(.\\d+)?", j.getAmount()))//判對是否為浮點數型態
					currentTotal=currentTotal+Double.parseDouble(j.getAmount());
			}
			map.put("currentTotal", currentTotal);
	
			
			map.put("D", data.getD());
			map.put("R", data.getR());

			List<UsageDetail> R1=new ArrayList<UsageDetail>();	
			List<UsageDetail> R2=new ArrayList<UsageDetail>();
			List<UsageDetail> R3=new ArrayList<UsageDetail>();
			
			Double tR1=0D;
			Double tR2=0D;
			Double tR3=0D;
			
			for(UsageDetail u : data.getR()){
				if("Voice Usage Charges".equalsIgnoreCase(u.getChargeItemName())){
					R1.add(u);	
					tR1=Double.parseDouble(u.getSubTotalCharges());
				}else if("SMS Charges".equalsIgnoreCase(u.getChargeItemName())){
					R2.add(u);
					tR2=Double.parseDouble(u.getSubTotalCharges());
				}else if("GPRS Usage Charges".equalsIgnoreCase(u.getChargeItemName())){
					R3.add(u);
					tR3=Double.parseDouble(u.getSubTotalCharges());
				}
			}
			map.put("R1", data.getR());
			map.put("R2", data.getR());
			map.put("R3", data.getR());
			
			map.put("Total Usage Charges",tR1+tR2+tR3 );
			
			
			map.put("SUBREPORT_DIR", filePath);
			float a=new java.lang.Float(0.00);
			
			
			List list=new ArrayList();			
			
			System.out.println("jasperFile convert to jrprintFile!");
			String jrprintFile=JasperFillManager.fillReportToFile(jasperFile,map,new JREmptyDataSource());
			//String jrprintFile=JasperFillManager.fillReportToFile(jasperFile,null,new JREmptyDataSource());
			//String jrprintFile=JasperFillManager.fillReportToFile(jasperFile,map,new JRBeanCollectionDataSource(data.getD()));
			System.out.println("convert to jrprintFile finished!");
			System.out.println("");
			
			System.out.println("Creating PDF file at "+PDFPath+"!");
			JasperExportManager.exportReportToPdfFile(jrprintFile,PDFPath);
			
		} catch (JRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("read file convert to jasperFile failt!");
		}
		
		
	}
	
}
