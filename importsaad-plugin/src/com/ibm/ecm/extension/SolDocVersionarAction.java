package com.ibm.ecm.extension;

import java.util.Locale;

import com.ibm.ecm.extension.PluginAction;

public class SolDocVersionarAction extends PluginAction {
	
	@Override
	public String getId() {
		return "SolDocVersionarAction";
	}

	@Override
	public String getActionFunction() {
		return "solDocVersionarAction";
	}
	
	@Override
	public String getIcon() {
		return null;
	}	

	@Override
	public String getName(Locale locale) {
		return "Actualizar";
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
		return false;
	}

	@Override
	public boolean isGlobal() {
		return false;
	}
	
	@Override
	public String getActionModelClass() {
		return "importSaadPluginDojo.SolDocVersionarModel";
	}			
	
}

