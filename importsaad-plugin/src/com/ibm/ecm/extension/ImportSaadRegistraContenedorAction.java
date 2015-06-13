package com.ibm.ecm.extension;

import java.util.Locale;
import com.ibm.ecm.extension.PluginAction;

public class ImportSaadRegistraContenedorAction extends PluginAction {
	
	@Override
	public String getId() {
		return "ImportSaadRegistraContenedorAction";
	}

	@Override
	public String getActionFunction() {
		return "importSaadRegistraContenedorAction";
	}
	
	@Override
	public String getIcon() {
		return null;
	}	

	@Override
	public String getName(Locale locale) {
			return "Registrar Nuevo Contenedor";
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
