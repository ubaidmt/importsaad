package com.ibm.ecm.extension;

import java.io.Writer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;

public class ConfigService extends PluginService {
	
	@Override
	public String getId() {
		return "configService";
	}
  
	@Override
	public void execute(PluginServiceCallbacks callbacks, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String configuration = callbacks.loadConfiguration();
		if (configuration == null) configuration = "{\"configuration\": null }";		
		Writer w = response.getWriter();
		w.write(configuration);
		w.flush();		
	}
}
