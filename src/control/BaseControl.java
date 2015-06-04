package control;

import java.util.Properties;


public class BaseControl {
	Properties props = null;
	BaseControl(){
		props=cache.CacheAction.getProperties();
	}

}
