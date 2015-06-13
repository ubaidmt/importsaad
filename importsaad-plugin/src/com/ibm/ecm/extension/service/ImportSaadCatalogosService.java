package com.ibm.ecm.extension.service;

import java.util.Iterator;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.util.UserContext;
import com.filenet.api.collection.RepositoryRowSet;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class ImportSaadCatalogosService {
	
	@SuppressWarnings("unchecked")
	public static JSONObject getClientes(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		JSONArray jsonResults = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		
		try {
			
			String repositoryId = request.getParameter("repositoryid");
			Subject subject = callbacks.getP8Subject(repositoryId);
			UserContext.get().pushSubject(subject);
			
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(repositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Nombre");
		    sql.setFromClauseInitialValue("Cliente", null, false);
		    sql.setWhereClause("IsCurrentVersion = True AND This INFOLDER '/Catálogos/Clientes'");
		    sql.setOrderByClause("Nombre");
		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) {
				jsonObj = new JSONObject();
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	jsonObj.put("id", props.getStringValue("Nombre"));
				jsonObj.put("name", props.getStringValue("Nombre"));
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
	public static JSONObject getProveedores(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		JSONArray jsonResults = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		
		try {
			
			String repositoryId = request.getParameter("repositoryid");
			Subject subject = callbacks.getP8Subject(repositoryId);
			UserContext.get().pushSubject(subject);
			
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(repositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Nombre");
		    sql.setFromClauseInitialValue("Proveedor", null, false);
		    sql.setWhereClause("IsCurrentVersion = True AND This INFOLDER '/Catálogos/Proveedores'");
		    sql.setOrderByClause("Nombre");
		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) {
				jsonObj = new JSONObject();
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	jsonObj.put("id", props.getStringValue("Nombre"));
				jsonObj.put("name", props.getStringValue("Nombre"));
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
	public static JSONObject getLista(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		JSONArray jsonResults = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		
		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String tipoLista = request.getParameter("tipolista");
			Subject subject = callbacks.getP8Subject(repositoryId);
			UserContext.get().pushSubject(subject);
			
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(repositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Nombre");
		    sql.setFromClauseInitialValue("Lista", null, false);
		    sql.setWhereClause("TipoLista = " + tipoLista + " AND IsCurrentVersion = True AND This INFOLDER '/Catálogos/Listas'");
		    sql.setOrderByClause("Nombre");
		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) {
				jsonObj = new JSONObject();
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	jsonObj.put("id", props.getStringValue("Nombre"));
				jsonObj.put("name", props.getStringValue("Nombre"));
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

}
