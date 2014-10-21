package action;

import control.ProgramControl;

public class ProgrmaAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String filename;

	private ProgramControl programControl=new ProgramControl();
	
	public String execute(){
		result = programControl.execute(filename);
		return SUCCESS;
	}
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	

	
}
