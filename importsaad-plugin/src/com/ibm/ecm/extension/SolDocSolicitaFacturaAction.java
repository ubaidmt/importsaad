package com.ibm.ecm.extension;

import java.util.Locale;
import com.ibm.ecm.extension.PluginAction;

public class SolDocSolicitaFacturaAction extends PluginAction {
	
	@Override
	public String getId() {
		return "SolDocSolicitaFacturaAction";
	}

	@Override
	public String getActionFunction() {
		return "solDocSolicitaFacturaAction";
	}
	
	@Override
	public String getIcon() {
		return null;
	}	

	@Override
	public String getName(Locale locale) {
		return "Solicitar Nueva Factura";
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

