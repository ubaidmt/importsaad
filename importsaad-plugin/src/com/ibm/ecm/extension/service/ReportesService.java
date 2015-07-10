package com.ibm.ecm.extension.service;

import java.io.PrintWriter;
import java.util.Iterator;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ecm.extension.PluginService;
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

public class ReportesService  extends PluginService {
	
	@Override	
	public String getId() {
		return "ReportesService";
	}	
	
	@Override	
	public void execute(PluginServiceCallbacks callbacks, HttpServletRequest request, HttpServletResponse response) throws Exception {

		String methodName = request.getParameter("method");
		if (methodName == null)
			throw new Exception("No se ha incluido el nombre del método a invocar.");
		
		callbacks.getLogger().logEntry(this, methodName, request);
			
		try {
	
			JSONObject result = new JSONObject();
			if (methodName.equals("getReportesTree"))		
				result = getReportesTree(request, callbacks);
			else if (methodName.equals("getReportesDetalle"))		
				result = getReportesDetalle(request, callbacks);			
			else
				throw new Exception("No se identificó el método incluido en el servicio.");

			// Send the response json
			PrintWriter writer = response.getWriter();
			result.serialize(writer);		
			
		} catch (Exception e) {
			callbacks.getLogger().logError(this, methodName, request, e);
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("error", "Ocurrió un error al momento de invocar un servicio. " + e.getMessage());
			PrintWriter writer = response.getWriter();
			jsonResponse.serialize(writer);			
			
		} finally {
			callbacks.getLogger().logExit(this, methodName, request);
		}		
	}
	
	@SuppressWarnings("unchecked")
	private static JSONObject getReportesTree(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
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
		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, null, null, true);
		    
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
	private static JSONObject getReportesDetalle(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		JSONArray jsonResults = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		
		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String rubroid = request.getParameter("rubroid");
			String userid = request.getParameter("userid");
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
		    		byte[] reporteData = reporte.getProperties().getBinaryValue("ClbJSONData");
					if (reporteData != null) {
						JSONObject jsonReporte = JSONObject.parse(new String(reporteData));
						// seguridad del reporte
						if (jsonReporte.containsKey("security")) {
							JSONObject jsonSecurity = (JSONObject) jsonReporte.get("security");
							if (jsonSecurity.containsKey(userid)) {
								boolean isallowed = (Boolean) jsonSecurity.get(userid);
								if (!isallowed) 
									continue; // el usuario no tiene acceso al reporte
							} else if (jsonSecurity.containsKey("everyoneelse")) {
								boolean isallowed = (Boolean) jsonSecurity.get("everyoneelse");
								if (!isallowed) 
									continue; // el resto de los usuarios no tienen acceso al reporte								
							} else {
								continue; // no se encuentra definido en la seguridad el usuario ni el elemento "everyoneelse"
							}
						}
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
