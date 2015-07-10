package com.excelecm.loader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ContentEngineSettings;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.collection.AccessPermissionList;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Document;
import com.filenet.api.core.Folder;
import com.filenet.api.core.Containable;
import com.filenet.api.core.Factory;
import com.filenet.api.core.IndependentlyPersistableObject;
import com.filenet.api.constants.AccessType;
import com.filenet.api.security.AccessPermission;
import com.filenet.api.exception.EngineRuntimeException;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class ReportesLoader {
	
	private static Logger log = Logger.getLogger(ReportesLoader.class);
	
	public static void main(String[] args) {
		ReportesLoader loader = new ReportesLoader();
		CEService ceService = null;
		String rubroReporte;
		List<String> granteeNames = new ArrayList<String>();
		
		try {
		
			ceService = loader.logOn();
			
			// Gestion de Contenedores
//			rubroReporte = "Gestión de Contenedores";
//			granteeNames = new ArrayList<String>();
//			granteeNames.add("#AUTHENTICATED-USERS");
//			loader.deleteReportes(ceService, LoaderConstantes.ceOS, rubroReporte);
//			loader.creaContenedor(ceService, LoaderConstantes.ceOS, rubroReporte, granteeNames);
//			loader.creaReportes(ceService, LoaderConstantes.ceOS, "ReportesGC", rubroReporte, granteeNames);
			
			// Gestion de Facturas
			rubroReporte = "Gestión de Facturas";
			granteeNames = new ArrayList<String>();
			granteeNames.add("#AUTHENTICATED-USERS");
			loader.deleteReportes(ceService, LoaderConstantes.ceOS, rubroReporte);
			loader.creaContenedor(ceService, LoaderConstantes.ceOS, rubroReporte, granteeNames);
			loader.creaReportes(ceService, LoaderConstantes.ceOS, "ReportesGF", rubroReporte, granteeNames);
			
						
		
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			loader.logOff(ceService);
		}
	}
	
	public CEService logOn() throws Exception {
		
		ContentEngineSettings ceSettings = new ContentEngineSettings();
		ceSettings.setUri(LoaderConstantes.ceUri);
		ceSettings.setUser(LoaderConstantes.ceUser);
		ceSettings.setPassword(LoaderConstantes.cePassword);
		ceSettings.setStanza(LoaderConstantes.ceStanza);
		
		CEService ceService = new CEService(ceSettings);
		ceService.establishConnection();
		
		return ceService;
		
	}	
	
	@SuppressWarnings("unchecked")
	public void deleteReportes(CEService ceService, String os, String name) throws Exception {
		int numElementos = 0;
		
		log.debug("Eliminando reportes y contenedor para " + name + "...");
		String className = "ReportContainer";
		IndependentObjectSet objSet = ceService.fetchObjects(className, "FolderName = '" + name + "'", false, os, 0);
	    for (Iterator<Folder> it = objSet.iterator(); it.hasNext(); ) 
	    {
	    	Folder contenedor = it.next();
	    	DocumentSet reportes = contenedor.get_ContainedDocuments();
	    	for (Iterator<Document> it2 = reportes.iterator(); it2.hasNext(); )
	    	{
	    		Document doc = it2.next();
		    	doc.delete();
		    	doc.save(RefreshMode.REFRESH); 		
		    	numElementos++;
	    	}
	    	contenedor.delete();
	    	contenedor.save(RefreshMode.REFRESH); 	
	    }
	    log.debug("Instancias eliminadas. Número de reportes eliminados: " + numElementos);
	}		
	
	public void logOff(CEService ceService) {
		
		try {
		
			if (ceService != null)
				ceService.releaseConnection();
		
		} catch (Exception e) {}
	}
	
	public void creaContenedor(CEService ceService, String os, String name, List<String> granteeNames) throws Exception {

		log.debug("Creando contenedor de reportes para " + name + "...");	
		String className = "ReportContainer";
		Folder parentFolder = Factory.Folder.fetchInstance(ceService.fetchOS(os), LoaderConstantes.osRootFolderReportes, null);
		Containable contenedor = (Containable) ceService.createFolderInstance(os, parentFolder, className, name, new HashMap<String, Object>());
		if (!granteeNames.isEmpty())
			setObjectViewSecurity(contenedor, granteeNames);
		log.debug("Contenedor creado");	
	}
	
	public void creaReportes(CEService ceService, String os, String reportesResurce, String rubro, List<String> granteeNames) throws Exception {
		
		log.debug("Creando reportes para " + reportesResurce + "...");	
		String className = "ReportTemplate";
		InputStream is = getClass().getClassLoader().getResourceAsStream(reportesResurce + ".json");
		if (is == null)
			throw new java.io.IOException("Resorce " + reportesResurce  + ".json not found.");
		
		JSONArray jsonReportes = JSONArray.parse(is);
		JSONObject jsonObj;
		Map<String, Object> props;
		int numElementos = 0;
		
		for(Object jsonReporte : jsonReportes) {
			jsonObj = (JSONObject) jsonReporte;
			props = new HashMap<String, Object>();
			props.put("ClbJSONData", jsonObj.serialize().getBytes());
			try {
				Folder parentFolder = Factory.Folder.fetchInstance(ceService.fetchOS(os), LoaderConstantes.osRootFolderReportes + "/" + rubro, null);
				Containable reporte = (Containable) ceService.createDocumentInstance(os, null, null, null, className, jsonObj.get("nombre").toString(), props, parentFolder.get_PathName());
				if (!granteeNames.isEmpty())
					setObjectViewSecurity(reporte, granteeNames);				
				numElementos++;
			} catch (EngineRuntimeException ere) {}
		}
		
		log.debug("Reportes creados. Número de reportes creados: " + numElementos);
	}
	
	@SuppressWarnings("unchecked")
	public void setObjectViewSecurity(Containable obj, List<String> granteeNames) throws Exception {
		AccessPermissionList apl = null;
		
		apl = obj.get_Permissions();	
		
		for (String granteeName: granteeNames) {
			AccessPermission ap = Factory.AccessPermission.createInstance();
			ap.set_GranteeName(granteeName);			
			ap.set_AccessType(AccessType.ALLOW);
			ap.set_AccessMask(131201); // VIEW_AS_INT
			ap.set_InheritableDepth(0); // No inheritance
			apl.add(ap);			
		}
		
		obj.set_Permissions(apl);
		IndependentlyPersistableObject ipo = (IndependentlyPersistableObject) obj;
		ipo.save(RefreshMode.REFRESH); 		
	} 	

}
