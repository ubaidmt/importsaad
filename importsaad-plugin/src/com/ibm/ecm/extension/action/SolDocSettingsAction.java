package com.ibm.ecm.extension.action;

import java.util.Locale;
import com.ibm.ecm.extension.PluginAction;

public class SolDocSettingsAction extends PluginAction {
	
	@Override
	public String getId() {
		return "SolDocSettingsAction";
	}

	@Override
	public String getActionFunction() {
		return "solDocSettingsAction";
	}
	
	@Override
	public String getIcon() {
		return null;
	}	

	@Override
	public String getName(Locale locale) {
		return "Gestión de Facturas";
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
