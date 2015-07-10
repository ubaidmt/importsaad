package com.ibm.ecm.extension.action;

import java.util.Locale;

import com.ibm.ecm.extension.PluginAction;

public class SolDocCopiarSolicitudAction extends PluginAction {
	
	@Override
	public String getId() {
		return "SolDocCopiarSolicitudAction";
	}

	@Override
	public String getActionFunction() {
		return "solDocCopiarSolicitudAction";
	}
	
	@Override
	public String getIcon() {
		return null;
	}	

	@Override
	public String getName(Locale locale) {
		return "Copiar";
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
		return "importSaadPluginDojo.model.SolDocCopiarSolicitudModel";
	}			
	
}

