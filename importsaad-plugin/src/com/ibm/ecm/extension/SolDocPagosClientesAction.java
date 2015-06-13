package com.ibm.ecm.extension;

import java.util.Locale;
import com.ibm.ecm.extension.PluginAction;

public class SolDocPagosClientesAction extends PluginAction {
	
	@Override
	public String getId() {
		return "SolDocPagosClientesAction";
	}

	@Override
	public String getActionFunction() {
		return "solDocPagosClientesAction";
	}
	
	@Override
	public String getIcon() {
		return null;
	}	

	@Override
	public String getName(Locale locale) {
		return "Registrar Pagos de Clientes";
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

