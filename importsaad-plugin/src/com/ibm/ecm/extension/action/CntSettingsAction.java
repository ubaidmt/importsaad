package com.ibm.ecm.extension.action;

import java.util.Locale;
import com.ibm.ecm.extension.PluginAction;

public class CntSettingsAction extends PluginAction {
	
	@Override
	public String getId() {
		return "CntSettingsAction";
	}

	@Override
	public String getActionFunction() {
		return "cntSettingsAction";
	}
	
	@Override
	public String getIcon() {
		return null;
	}	

	@Override
	public String getName(Locale locale) {
		return "Gestión de Contenedores";
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
