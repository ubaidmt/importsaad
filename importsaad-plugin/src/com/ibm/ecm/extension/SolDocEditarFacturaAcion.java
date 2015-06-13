package com.ibm.ecm.extension;

import java.util.Locale;

import com.ibm.ecm.extension.PluginAction;

public class SolDocEditarFacturaAcion extends PluginAction {
	
	@Override
	public String getId() {
		return "SolDocEditarFacturaAcion";
	}

	@Override
	public String getActionFunction() {
		return "solDocEditarFacturaAcion";
	}
	
	@Override
	public String getIcon() {
		return null;
	}	

	@Override
	public String getName(Locale locale) {
		return "Abrir";
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
		return "importSaadPluginDojo.SolDocEditarFacturaModel";
	}		
	
}

