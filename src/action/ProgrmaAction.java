package action;

import java.io.IOException;
import java.sql.SQLException;

import control.ProgramControl;

public class ProgrmaAction extends BaseAction {

	public ProgrmaAction() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String filename;

	private ProgramControl programControl=new ProgramControl();
	
	public String execute() throws SQLException {
		try {
			result = programControl.execute(filename);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result=e.getMessage();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result=e.getMessage();
		}
		try {
			actionLogControl.loggerAction(super.getUser().getAccount(), "Program", "execute","filename¡G"+filename, result);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception:"+e.getMessage());
		}
		return SUCCESS;
	}
	
	//******************************************************//
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	

	
}
