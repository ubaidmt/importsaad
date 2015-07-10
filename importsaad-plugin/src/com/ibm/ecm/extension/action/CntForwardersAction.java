package com.ibm.ecm.extension.action;

import java.util.Locale;

import com.ibm.ecm.extension.PluginAction;

public class CntForwardersAction extends PluginAction {
	
	@Override
	public String getId() {
		return "CntForwardersAction";
	}

	@Override
	public String getActionFunction() {
		return "cntForwardersAction";
	}
	
	@Override
	public String getIcon() {
		return null;
	}	

	@Override
	public String getName(Locale locale) {
		return "Forwarders";
	}
	
	@Override
	public String getPrivilege() {
		return "";
	}	

	@Override
	public String getServerTypes() {
		return "p8";
	}

	@Override
	public boolean isMultiDoc() {
		return true;
	}

	@Override
	public boolean isGlobal() {
		return true;
	}
	
}
