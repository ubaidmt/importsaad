package com.ibm.ecm.extension.service;

import java.util.Iterator;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.util.UserContext;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.collection.RepositoryRowSet;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class ImportSaadReportesService {
	
	@SuppressWarnings("unchecked")
	public static JSONObject getReportesTree(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		JSONArray jsonResults = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		
		try {
			
			String repositoryId = request.getParameter("repositoryid");
			Subject subject = callbacks.getP8Subject(repositoryId);
			UserContext.get().pushSubject(subject);
			
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(repositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, FolderName");
		    sql.setFromClauseInitialValue("ReportContainer", null, false);
		    sql.setOrderByClause("FolderName");
		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
		    
		    // Add Root Element
		    jsonObj = new JSONObject();
	    	jsonObj.put("id", "root");
			jsonObj.put("name", "Reportes");
			jsonResults.add(jsonObj);

		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) {
				jsonObj = new JSONObject();
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	jsonObj.put("id", props.getStringValue("FolderName"));
				jsonObj.put("name", props.getStringValue("FolderName"));
				jsonObj.put("parent", "root"); // todos al mismo nivel debajo de Root
				jsonObj.put("rubroid", props.getIdValue("Id").toString());
				jsonResults.add(jsonObj);
		    }
		
		} catch (Exception e) {
			throw e;
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Results
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("results", jsonResults);		
		return jsonResponse;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getReportesDetalle(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		JSONArray jsonResults = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		
		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String rubroid = request.getParameter("rubroid");			
			Subject subject = callbacks.getP8Subject(repositoryId);
			UserContext.get().pushSubject(subject);
			
			Folder reportContainer = null;
			try {
				reportContainer = Factory.Folder.fetchInstance(callbacks.getP8ObjectStore(repositoryId), rubroid, null);
			} catch (EngineRuntimeException ere) {}			
			
			if (reportContainer != null) {
		    	DocumentSet reportes = reportContainer.get_ContainedDocuments();
		    	for (Iterator<Document> it = reportes.iterator(); it.hasNext(); )
		    	{
		    		Document reporte = it.next();
		    		byte[] reporteData= reporte.getProperties().getBinaryValue("ClbJSONData");
					if (reporteData != null) {
						JSONObject jsonReporte = JSONObject.parse(new String(reporteData));
						jsonObj = new JSONObject();
						jsonObj.put("id", jsonReporte.get("id"));
						jsonObj.put("name", reporte.get_Name());
						jsonObj.put("descripcion", jsonReporte.get("descripcion"));
						jsonObj.put("clase", jsonReporte.get("clase"));
						jsonObj.put("plantilla", jsonReporte.get("plantilla"));
						jsonObj.put("tipo", jsonReporte.get("tipo"));
						jsonObj.put("condiciones", jsonReporte.get("condiciones").toString());
						jsonObj.put("parametros", jsonReporte.get("parametros").toString());
						jsonResults.add(jsonObj);	
					}		    		
		    	}			
			}
		    
		} catch (Exception e) {
			throw e;
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Results
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("results", jsonResults);		
		return jsonResponse;	    
		
	}

}
