package com.ibm.ecm.extension.service;

import java.util.Iterator;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.Folder;
import com.filenet.api.core.Document;
import com.filenet.api.core.VersionSeries;
import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.util.UserContext;
import com.filenet.api.util.Id;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.collection.RepositoryRowSet;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.json.java.JSONObject;
import com.ibm.json.java.JSONArray;

public class CntCatalogosService extends PluginService {	
	
	@Override	
	public String getId() {
		return "CntCatalogosService";
	}	
	
	@Override	
	public void execute(PluginServiceCallbacks callbacks, HttpServletRequest request, HttpServletResponse response) throws Exception {

		String methodName = request.getParameter("method");
		if (methodName == null)
			throw new Exception("No se ha incluido el nombre del método a invocar.");
		
		callbacks.getLogger().logEntry(this, methodName, request);
			
		try {
	
			JSONObject result = new JSONObject();
			if (methodName.equals("searchFracciones"))
				result = searchFracciones(request, callbacks);	
			else if (methodName.equals("addFraccion"))
				result = addFraccion(request, callbacks);
			else if (methodName.equals("updateFraccion"))
				result = updateFraccion(request, callbacks);			
			else if (methodName.equals("deleteFraccion"))
				result = deleteFraccion(request, callbacks);
			else if (methodName.equals("searchClientes"))
				result = searchClientes(request, callbacks);
			else if (methodName.equals("addCliente"))
				result = addCliente(request, callbacks);
			else if (methodName.equals("updateCliente"))
				result = updateCliente(request, callbacks);
			else if (methodName.equals("deleteCliente"))
				result = deleteCliente(request, callbacks);
			else if (methodName.equals("searchNavieras"))
				result = searchNavieras(request, callbacks);
			else if (methodName.equals("addNaviera"))
				result = addNaviera(request, callbacks);
			else if (methodName.equals("updateNaviera"))
				result = updateNaviera(request, callbacks);
			else if (methodName.equals("deleteNaviera"))
				result = deleteNaviera(request, callbacks);
			else if (methodName.equals("searchForwarders"))
				result = searchForwarders(request, callbacks);
			else if (methodName.equals("addForwarder"))
				result = addForwarder(request, callbacks);
			else if (methodName.equals("updateForwarder"))
				result = updateForwarder(request, callbacks);
			else if (methodName.equals("deleteForwarder"))
				result = deleteForwarder(request, callbacks);
			else if (methodName.equals("searchProveedores"))
				result = searchProveedores(request, callbacks);
			else if (methodName.equals("addProveedor"))
				result = addProveedor(request, callbacks);
			else if (methodName.equals("updateProveedor"))
				result = updateProveedor(request, callbacks);
			else if (methodName.equals("deleteProveedor"))
				result = deleteProveedor(request, callbacks);
			else if (methodName.equals("searchImportadoras"))
				result = searchImportadoras(request, callbacks);
			else if (methodName.equals("addImportadora"))
				result = addImportadora(request, callbacks);
			else if (methodName.equals("updateImportadora"))
				result = updateImportadora(request, callbacks);
			else if (methodName.equals("deleteImportadora"))
				result = deleteImportadora(request, callbacks);		
			else if (methodName.equals("searchPuertos"))
				result = searchPuertos(request, callbacks);
			else if (methodName.equals("addPuerto"))
				result = addPuerto(request, callbacks);
			else if (methodName.equals("updatePuerto"))
				result = updatePuerto(request, callbacks);
			else if (methodName.equals("deletePuerto"))
				result = deletePuerto(request, callbacks);				
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
	private JSONObject searchFracciones(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);

		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, DateCreated, DocumentTitle, ClbJSONData");
		    sql.setFromClauseInitialValue("CntFraccion", null, false);			
			
		    // Build where statement
		    JSONObject criterio = JSONObject.parse(request.getParameter("criterio"));
		    StringBuffer whereStatement = new StringBuffer();
		    whereStatement.append("isCurrentVersion = TRUE");	
		    if (criterio.get("id") != null)
		    	whereStatement.append(" AND Id = " + criterio.get("id").toString());		    
		    if (criterio.get("name") != null)
		    	whereStatement.append(" AND DocumentTitle = '" + criterio.get("name").toString() + "'");
		    
		    sql.setWhereClause(whereStatement.toString());
		    if (Integer.parseInt(request.getParameter("maxResults")) > 0)
		    	sql.setMaxRecords(Integer.parseInt(request.getParameter("maxResults")));
		    
		    RepositoryRowSet rowSet = (RepositoryRowSet) search.fetchRows(sql, null, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	jsonObject = getFraccionJson(props);
				// add element
				jsonArray.add(jsonObject);					
		    }
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("fracciones", jsonArray);
		return jsonResponse;
	}	
	
	private JSONObject addFraccion(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("fraccion"));
							
			// Obten instancia del folder raiz para fracciones
			Folder parentFolder = Factory.Folder.getInstance(objStore, "Folder", "/Fracciones");				
				
			// Crea nueva fraccion
			Document fraccion = Factory.Document.createInstance(objStore, "CntFraccion");
			fraccion.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
			
			com.filenet.api.property.Properties props = fraccion.getProperties();
			props.putObjectValue("DocumentTitle", jsonData.get("name").toString());			
			props.putValue("ClbJSONData", jsonData.serialize().getBytes());

			fraccion.set_SecurityFolder(parentFolder); // inherit permissions from parent folder
			fraccion.save(RefreshMode.REFRESH);
			
			// File fraccion
			ReferentialContainmentRelationship rel = parentFolder.file(fraccion, AutoUniqueName.AUTO_UNIQUE, null, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
			rel.save(RefreshMode.NO_REFRESH);	
			
			// Response
			props = fraccion.getProperties();
			jsonData = getFraccionJson(props);			
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("fraccion", jsonData);
		return jsonResponse;			
	}	
	
	private JSONObject updateFraccion(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		JSONObject jsonData = new JSONObject();		

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("fraccion"));
			Document fraccion = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
			
			// Update props
			com.filenet.api.property.Properties props = fraccion.getProperties();
			props.putObjectValue("DocumentTitle", jsonData.get("name").toString());			
			props.putValue("ClbJSONData", jsonData.serialize().getBytes());
			fraccion.save(RefreshMode.REFRESH);				
			
			// Response
			props = fraccion.getProperties();
			jsonData = getFraccionJson(props);					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("fraccion", jsonData);
		return jsonResponse;			
	}		
	
	private JSONObject deleteFraccion(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		JSONArray jsonArray = new JSONArray();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonArray = JSONArray.parse(request.getParameter("fracciones"));
			
			for (Object obj : jsonArray) {
				JSONObject jsonData = (JSONObject) obj;
				Document fraccion = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
				VersionSeries vs = fraccion.get_VersionSeries();
				vs.delete();
				vs.save(RefreshMode.REFRESH);				
			}					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("count", jsonArray.size());
		return jsonResponse;			
	}	
	
	@SuppressWarnings("unchecked")
	private JSONObject searchClientes(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);

		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, DateCreated, FolderName, Activo");
		    sql.setFromClauseInitialValue("CntCliente", null, false);			
			
		    // Build where statement
		    JSONObject criterio = JSONObject.parse(request.getParameter("criterio"));
		    StringBuffer whereStatement = new StringBuffer();
		    whereStatement.append("FolderName is NOT NULL");	
		    if (criterio.get("id") != null)
		    	whereStatement.append(" AND Id = " + criterio.get("id").toString());		    
		    if (criterio.get("name") != null)
		    	whereStatement.append(" AND FolderName = '" + criterio.get("name").toString() + "'");
		    if (criterio.get("activo") != null)
		    	whereStatement.append(" AND Activo = " + (Boolean.parseBoolean(criterio.get("activo").toString()) ? "TRUE" : "FALSE"));		    
		    
		    sql.setWhereClause(whereStatement.toString());
		    if (Integer.parseInt(request.getParameter("maxResults")) > 0)
		    	sql.setMaxRecords(Integer.parseInt(request.getParameter("maxResults")));
		    
		    RepositoryRowSet rowSet = (RepositoryRowSet) search.fetchRows(sql, null, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	jsonObject = getClienteJson(props);
				// add element
				jsonArray.add(jsonObject);					
		    }
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("clientes", jsonArray);
		return jsonResponse;
	}	
	
	private JSONObject addCliente(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("cliente"));
							
			// Obten instancia del folder raiz
			Folder parentFolder = Factory.Folder.getInstance(objStore, "Folder", "/Importaciones");				
				
			// Crea nuevo cliente
			Folder cliente = Factory.Folder.createInstance(objStore, "CntCliente");
			cliente.set_FolderName(jsonData.get("name").toString());
			cliente.set_Parent(parentFolder); 
			cliente.set_InheritParentPermissions(Boolean.TRUE); // inherit permissions from parent folder
			
			com.filenet.api.property.Properties props = cliente.getProperties();			
			props.putValue("Activo", Boolean.parseBoolean(jsonData.get("activo").toString()));

			cliente.save(RefreshMode.REFRESH);
			
			// Response
			props = cliente.getProperties();
			jsonData = getClienteJson(props);			
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("cliente", jsonData);
		return jsonResponse;			
	}	
	
	private JSONObject updateCliente(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		JSONObject jsonData = new JSONObject();		

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("cliente"));
			Folder cliente = Factory.Folder.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
			cliente.set_FolderName(jsonData.get("name").toString());
			
			// Update props
			com.filenet.api.property.Properties props = cliente.getProperties();		
			props.putValue("Activo", Boolean.parseBoolean(jsonData.get("activo").toString()));
			cliente.save(RefreshMode.REFRESH);				
			
			// Response
			props = cliente.getProperties();
			jsonData = getClienteJson(props);					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("cliente", jsonData);
		return jsonResponse;			
	}
	
	private JSONObject deleteCliente(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;
		JSONArray jsonArray = new JSONArray();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonArray = JSONArray.parse(request.getParameter("clientes"));
			
			for (Object obj : jsonArray) {
				JSONObject jsonData = (JSONObject) obj;
				Folder cliente = Factory.Folder.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
				// el cliente se elimina unicamente en caso de no tener cotizaciones ni contenedores asociados
				JSONObject criterio = new JSONObject();
				criterio.put("cliente", cliente.get_Id().toString());
				boolean sinCotizaciones = CntCotizacionesService.searchCotizaciones(objStore, criterio, 1).isEmpty();
				boolean sinContenedores = CntContenedoresService.searchContenedores(objStore, criterio, 1).isEmpty();
				if (sinCotizaciones && sinContenedores) 
					ServiceUtil.deleteRecursively(cliente);
				else
					jsonArray.remove(obj);
			}					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("count", jsonArray.size());
		return jsonResponse;			
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject searchNavieras(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);

		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, DateCreated, DocumentTitle, Activo, ClbJSONData");
		    sql.setFromClauseInitialValue("CntNaviera", null, false);			
			
		    // Build where statement
		    JSONObject criterio = JSONObject.parse(request.getParameter("criterio"));
		    StringBuffer whereStatement = new StringBuffer();
		    whereStatement.append("isCurrentVersion = TRUE");	
		    if (criterio.get("id") != null)
		    	whereStatement.append(" AND Id = " + criterio.get("id").toString());		    
		    if (criterio.get("name") != null)
		    	whereStatement.append(" AND DocumentTitle = '" + criterio.get("name").toString() + "'");
		    if (criterio.get("activo") != null)
		    	whereStatement.append(" AND Activo = " + (Boolean.parseBoolean(criterio.get("activo").toString()) ? "TRUE" : "FALSE"));		    
		    
		    sql.setWhereClause(whereStatement.toString());
		    if (Integer.parseInt(request.getParameter("maxResults")) > 0)
		    	sql.setMaxRecords(Integer.parseInt(request.getParameter("maxResults")));
		    
		    RepositoryRowSet rowSet = (RepositoryRowSet) search.fetchRows(sql, null, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	jsonObject = getNavieraJson(props);
				// add element
				jsonArray.add(jsonObject);					
		    }
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("navieras", jsonArray);
		return jsonResponse;
	}	
	
	private JSONObject addNaviera(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("naviera"));
							
			// Obten instancia del folder raiz
			Folder parentFolder = Factory.Folder.getInstance(objStore, "Folder", "/Navieras");				
				
			// Crea nueva naviera
			Document naviera = Factory.Document.createInstance(objStore, "CntNaviera");
			naviera.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
			
			com.filenet.api.property.Properties props = naviera.getProperties();
			props.putObjectValue("DocumentTitle", jsonData.get("name").toString());
			props.putObjectValue("Activo", Boolean.parseBoolean(jsonData.get("activo").toString()));
			props.putValue("ClbJSONData", jsonData.serialize().getBytes());

			naviera.set_SecurityFolder(parentFolder); // inherit permissions from parent folder
			naviera.save(RefreshMode.REFRESH);
			
			// File naviera
			ReferentialContainmentRelationship rel = parentFolder.file(naviera, AutoUniqueName.AUTO_UNIQUE, null, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
			rel.save(RefreshMode.NO_REFRESH);	
			
			// Response
			props = naviera.getProperties();
			jsonData = getNavieraJson(props);			
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("naviera", jsonData);
		return jsonResponse;			
	}	
	
	private JSONObject updateNaviera(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		JSONObject jsonData = new JSONObject();		

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("naviera"));
			Document naviera = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
			
			// Update props
			com.filenet.api.property.Properties props = naviera.getProperties();
			props.putObjectValue("DocumentTitle", jsonData.get("name").toString());			
			props.putObjectValue("Activo", Boolean.parseBoolean(jsonData.get("activo").toString()));
			props.putValue("ClbJSONData", jsonData.serialize().getBytes());
			naviera.save(RefreshMode.REFRESH);				
			
			// Response
			props = naviera.getProperties();
			jsonData = getNavieraJson(props);					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("naviera", jsonData);
		return jsonResponse;			
	}
	
	private JSONObject deleteNaviera(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;
		JSONArray jsonArray = new JSONArray();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonArray = JSONArray.parse(request.getParameter("navieras"));
			
			for (Object obj : jsonArray) {
				JSONObject jsonData = (JSONObject) obj;
				Document naviera = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
				// la naviera se elimina unicamente en caso de no tener contenedores asociados
				JSONObject criterio = new JSONObject();
				criterio.put("naviera", naviera.get_Id().toString());
				if (CntContenedoresService.searchContenedores(objStore, criterio, 1).isEmpty()) {
					naviera.delete();
					naviera.save(RefreshMode.REFRESH);
				}
				else
					jsonArray.remove(obj);			
			}					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("count", jsonArray.size());
		return jsonResponse;			
	}	
	
	@SuppressWarnings("unchecked")
	private JSONObject searchForwarders(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);

		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, DateCreated, DocumentTitle, Activo");
		    sql.setFromClauseInitialValue("CntForwarder", null, false);			
			
		    // Build where statement
		    JSONObject criterio = JSONObject.parse(request.getParameter("criterio"));
		    StringBuffer whereStatement = new StringBuffer();
		    whereStatement.append("isCurrentVersion = TRUE");	
		    if (criterio.get("id") != null)
		    	whereStatement.append(" AND Id = " + criterio.get("id").toString());		    
		    if (criterio.get("name") != null)
		    	whereStatement.append(" AND DocumentTitle = '" + criterio.get("name").toString() + "'");
		    if (criterio.get("activo") != null)
		    	whereStatement.append(" AND Activo = " + (Boolean.parseBoolean(criterio.get("activo").toString()) ? "TRUE" : "FALSE"));		    
		    
		    sql.setWhereClause(whereStatement.toString());
		    if (Integer.parseInt(request.getParameter("maxResults")) > 0)
		    	sql.setMaxRecords(Integer.parseInt(request.getParameter("maxResults")));
		    
		    RepositoryRowSet rowSet = (RepositoryRowSet) search.fetchRows(sql, null, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	jsonObject = getForwarderJson(props);
				// add element
				jsonArray.add(jsonObject);					
		    }
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("forwarders", jsonArray);
		return jsonResponse;
	}
	
	private JSONObject addForwarder(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("forwarder"));
							
			// Obten instancia del folder raiz
			Folder parentFolder = Factory.Folder.getInstance(objStore, "Folder", "/Forwarders");				
				
			// Crea nueva forwarder
			Document forwarder = Factory.Document.createInstance(objStore, "CntForwarder");
			forwarder.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
			
			com.filenet.api.property.Properties props = forwarder.getProperties();
			props.putObjectValue("DocumentTitle", jsonData.get("name").toString());
			props.putObjectValue("Activo", Boolean.parseBoolean(jsonData.get("activo").toString()));

			forwarder.set_SecurityFolder(parentFolder); // inherit permissions from parent folder
			forwarder.save(RefreshMode.REFRESH);
			
			// File naviera
			ReferentialContainmentRelationship rel = parentFolder.file(forwarder, AutoUniqueName.AUTO_UNIQUE, null, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
			rel.save(RefreshMode.NO_REFRESH);	
			
			// Response
			props = forwarder.getProperties();
			jsonData = getForwarderJson(props);			
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("forwarder", jsonData);
		return jsonResponse;			
	}	
	
	private JSONObject updateForwarder(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		JSONObject jsonData = new JSONObject();		

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("forwarder"));
			Document forwarder = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
			
			// Update props
			com.filenet.api.property.Properties props = forwarder.getProperties();
			props.putObjectValue("DocumentTitle", jsonData.get("name").toString());			
			props.putObjectValue("Activo", Boolean.parseBoolean(jsonData.get("activo").toString()));
			forwarder.save(RefreshMode.REFRESH);				
			
			// Response
			props = forwarder.getProperties();
			jsonData = getNavieraJson(props);					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("forwarder", jsonData);
		return jsonResponse;			
	}
	
	private JSONObject deleteForwarder(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;
		JSONArray jsonArray = new JSONArray();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonArray = JSONArray.parse(request.getParameter("forwarders"));
			
			for (Object obj : jsonArray) {
				JSONObject jsonData = (JSONObject) obj;
				Document forwarder = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
				// el forwarder se elimina unicamente en caso de no tener contenedores asociados
				JSONObject criterio = new JSONObject();
				criterio.put("forwarder", forwarder.get_Id().toString());
				if (CntContenedoresService.searchContenedores(objStore, criterio, 1).isEmpty()) {
					forwarder.delete();
					forwarder.save(RefreshMode.REFRESH);
				}
				else
					jsonArray.remove(obj);			
			}					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("count", jsonArray.size());
		return jsonResponse;			
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject searchProveedores(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);

		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, DateCreated, DocumentTitle, Activo, ClbJSONData");
		    sql.setFromClauseInitialValue("CntProveedor", null, false);			
			
		    // Build where statement
		    JSONObject criterio = JSONObject.parse(request.getParameter("criterio"));
		    StringBuffer whereStatement = new StringBuffer();
		    whereStatement.append("isCurrentVersion = TRUE");	
		    if (criterio.get("id") != null)
		    	whereStatement.append(" AND Id = " + criterio.get("id").toString());		    
		    if (criterio.get("name") != null)
		    	whereStatement.append(" AND DocumentTitle = '" + criterio.get("name").toString() + "'");
		    if (criterio.get("activo") != null)
		    	whereStatement.append(" AND Activo = " + (Boolean.parseBoolean(criterio.get("activo").toString()) ? "TRUE" : "FALSE"));		    
		    
		    sql.setWhereClause(whereStatement.toString());
		    if (Integer.parseInt(request.getParameter("maxResults")) > 0)
		    	sql.setMaxRecords(Integer.parseInt(request.getParameter("maxResults")));
		    
		    RepositoryRowSet rowSet = (RepositoryRowSet) search.fetchRows(sql, null, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	jsonObject = getNavieraJson(props);
				// add element
				jsonArray.add(jsonObject);					
		    }
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("proveedores", jsonArray);
		return jsonResponse;
	}
	
	private JSONObject addProveedor(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("proveedor"));
							
			// Obten instancia del folder raiz
			Folder parentFolder = Factory.Folder.getInstance(objStore, "Folder", "/ProveedoresCnt");				
				
			// Crea nuevo proveedor
			Document proveedor = Factory.Document.createInstance(objStore, "CntProveedor");
			proveedor.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
			
			com.filenet.api.property.Properties props = proveedor.getProperties();
			props.putObjectValue("DocumentTitle", jsonData.get("name").toString());
			props.putObjectValue("Activo", Boolean.parseBoolean(jsonData.get("activo").toString()));
			props.putValue("ClbJSONData", jsonData.serialize().getBytes());

			proveedor.set_SecurityFolder(parentFolder); // inherit permissions from parent folder
			proveedor.save(RefreshMode.REFRESH);
			
			// File
			ReferentialContainmentRelationship rel = parentFolder.file(proveedor, AutoUniqueName.AUTO_UNIQUE, null, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
			rel.save(RefreshMode.NO_REFRESH);	
			
			// Response
			props = proveedor.getProperties();
			jsonData = getProveedorJson(props);			
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("proveedor", jsonData);
		return jsonResponse;			
	}	
	
	private JSONObject updateProveedor(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		JSONObject jsonData = new JSONObject();		

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("proveedor"));
			Document proveedor = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
			
			// Update props
			com.filenet.api.property.Properties props = proveedor.getProperties();
			props.putObjectValue("DocumentTitle", jsonData.get("name").toString());			
			props.putObjectValue("Activo", Boolean.parseBoolean(jsonData.get("activo").toString()));
			props.putValue("ClbJSONData", jsonData.serialize().getBytes());
			proveedor.save(RefreshMode.REFRESH);				
			
			// Response
			props = proveedor.getProperties();
			jsonData = getProveedorJson(props);					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("proveedor", jsonData);
		return jsonResponse;			
	}
	
	private JSONObject deleteProveedor(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;
		JSONArray jsonArray = new JSONArray();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonArray = JSONArray.parse(request.getParameter("proveedores"));
			
			for (Object obj : jsonArray) {
				JSONObject jsonData = (JSONObject) obj;
				Document proveedor = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
				// el proveeedor se elimina unicamente en caso de no tener contenedores asociados
				JSONObject criterio = new JSONObject();
				criterio.put("proveedor", proveedor.get_Id().toString());
				if (CntContenedoresService.searchContenedores(objStore, criterio, 1).isEmpty()) {
					proveedor.delete();
					proveedor.save(RefreshMode.REFRESH);
				}
				else
					jsonArray.remove(obj);			
			}					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("count", jsonArray.size());
		return jsonResponse;			
	}		
	
	@SuppressWarnings("unchecked")
	private JSONObject searchImportadoras(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);

		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, DateCreated, DocumentTitle, Activo, ClbJSONData");
		    sql.setFromClauseInitialValue("CntImportadora", null, false);			
			
		    // Build where statement
		    JSONObject criterio = JSONObject.parse(request.getParameter("criterio"));
		    StringBuffer whereStatement = new StringBuffer();
		    whereStatement.append("isCurrentVersion = TRUE");	
		    if (criterio.get("id") != null)
		    	whereStatement.append(" AND Id = " + criterio.get("id").toString());		    
		    if (criterio.get("name") != null)
		    	whereStatement.append(" AND DocumentTitle = '" + criterio.get("name").toString() + "'");
		    if (criterio.get("activo") != null)
		    	whereStatement.append(" AND Activo = " + (Boolean.parseBoolean(criterio.get("activo").toString()) ? "TRUE" : "FALSE"));		    
		    
		    sql.setWhereClause(whereStatement.toString());
		    if (Integer.parseInt(request.getParameter("maxResults")) > 0)
		    	sql.setMaxRecords(Integer.parseInt(request.getParameter("maxResults")));
		    
		    RepositoryRowSet rowSet = (RepositoryRowSet) search.fetchRows(sql, null, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	jsonObject = getImportadoraJson(props);
				// add element
				jsonArray.add(jsonObject);					
		    }
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("importadoras", jsonArray);
		return jsonResponse;
	}
	
	private JSONObject addImportadora(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("importadora"));
							
			// Obten instancia del folder raiz
			Folder parentFolder = Factory.Folder.getInstance(objStore, "Folder", "/Importadoras");				
				
			// Crea nueva importadora
			Document importadora = Factory.Document.createInstance(objStore, "CntImportadora");
			importadora.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
			
			com.filenet.api.property.Properties props = importadora.getProperties();
			props.putObjectValue("DocumentTitle", jsonData.get("name").toString());
			props.putObjectValue("Activo", Boolean.parseBoolean(jsonData.get("activo").toString()));
			props.putValue("ClbJSONData", jsonData.serialize().getBytes());

			importadora.set_SecurityFolder(parentFolder); // inherit permissions from parent folder
			importadora.save(RefreshMode.REFRESH);
			
			// File
			ReferentialContainmentRelationship rel = parentFolder.file(importadora, AutoUniqueName.AUTO_UNIQUE, null, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
			rel.save(RefreshMode.NO_REFRESH);	
			
			// Response
			props = importadora.getProperties();
			jsonData = getImportadoraJson(props);			
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("importadora", jsonData);
		return jsonResponse;			
	}	
	
	private JSONObject updateImportadora(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		JSONObject jsonData = new JSONObject();		

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("importadora"));
			Document importadora = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
			
			// Update props
			com.filenet.api.property.Properties props = importadora.getProperties();
			props.putObjectValue("DocumentTitle", jsonData.get("name").toString());			
			props.putObjectValue("Activo", Boolean.parseBoolean(jsonData.get("activo").toString()));
			props.putValue("ClbJSONData", jsonData.serialize().getBytes());
			importadora.save(RefreshMode.REFRESH);				
			
			// Response
			props = importadora.getProperties();
			jsonData = getImportadoraJson(props);					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("importadora", jsonData);
		return jsonResponse;			
	}
	
	private JSONObject deleteImportadora(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;
		JSONArray jsonArray = new JSONArray();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonArray = JSONArray.parse(request.getParameter("importadoras"));
			
			for (Object obj : jsonArray) {
				JSONObject jsonData = (JSONObject) obj;
				Document importadora = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
				// la importadora se elimina unicamente en caso de no tener contenedores asociados
				JSONObject criterio = new JSONObject();
				criterio.put("importadora", importadora.get_Id().toString());
				if (CntContenedoresService.searchContenedores(objStore, criterio, 1).isEmpty()) {
					importadora.delete();
					importadora.save(RefreshMode.REFRESH);
				}
				else
					jsonArray.remove(obj);			
			}					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("count", jsonArray.size());
		return jsonResponse;			
	}			
	
	@SuppressWarnings("unchecked")
	private JSONObject searchPuertos(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);

		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, DateCreated, DocumentTitle, Activo");
		    sql.setFromClauseInitialValue("CntPuerto", null, false);			
			
		    // Build where statement
		    JSONObject criterio = JSONObject.parse(request.getParameter("criterio"));
		    StringBuffer whereStatement = new StringBuffer();
		    whereStatement.append("isCurrentVersion = TRUE");	
		    if (criterio.get("id") != null)
		    	whereStatement.append(" AND Id = " + criterio.get("id").toString());		    
		    if (criterio.get("name") != null)
		    	whereStatement.append(" AND DocumentTitle = '" + criterio.get("name").toString() + "'");
		    if (criterio.get("activo") != null)
		    	whereStatement.append(" AND Activo = " + (Boolean.parseBoolean(criterio.get("activo").toString()) ? "TRUE" : "FALSE"));		    
		    
		    sql.setWhereClause(whereStatement.toString());
		    if (Integer.parseInt(request.getParameter("maxResults")) > 0)
		    	sql.setMaxRecords(Integer.parseInt(request.getParameter("maxResults")));
		    
		    RepositoryRowSet rowSet = (RepositoryRowSet) search.fetchRows(sql, null, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	jsonObject = getPuertoJson(props);
				// add element
				jsonArray.add(jsonObject);					
		    }
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("puertos", jsonArray);
		return jsonResponse;
	}
	
	private JSONObject addPuerto(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("puerto"));
							
			// Obten instancia del folder raiz
			Folder parentFolder = Factory.Folder.getInstance(objStore, "Folder", "/Puertos");				
				
			// Crea nueva importadora
			Document puerto = Factory.Document.createInstance(objStore, "CntPuerto");
			puerto.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
			
			com.filenet.api.property.Properties props = puerto.getProperties();
			props.putObjectValue("DocumentTitle", jsonData.get("name").toString());
			props.putObjectValue("Activo", Boolean.parseBoolean(jsonData.get("activo").toString()));

			puerto.set_SecurityFolder(parentFolder); // inherit permissions from parent folder
			puerto.save(RefreshMode.REFRESH);
			
			// File
			ReferentialContainmentRelationship rel = parentFolder.file(puerto, AutoUniqueName.AUTO_UNIQUE, null, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
			rel.save(RefreshMode.NO_REFRESH);	
			
			// Response
			props = puerto.getProperties();
			jsonData = getPuertoJson(props);			
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("puerto", jsonData);
		return jsonResponse;			
	}	
	
	private JSONObject updatePuerto(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		JSONObject jsonData = new JSONObject();		

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("puerto"));
			Document puerto = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
			
			// Update props
			com.filenet.api.property.Properties props = puerto.getProperties();
			props.putObjectValue("DocumentTitle", jsonData.get("name").toString());			
			props.putObjectValue("Activo", Boolean.parseBoolean(jsonData.get("activo").toString()));
			puerto.save(RefreshMode.REFRESH);				
			
			// Response
			props = puerto.getProperties();
			jsonData = getPuertoJson(props);					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("puerto", jsonData);
		return jsonResponse;			
	}
	
	private JSONObject deletePuerto(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;
		JSONArray jsonArray = new JSONArray();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonArray = JSONArray.parse(request.getParameter("puertos"));
			
			for (Object obj : jsonArray) {
				JSONObject jsonData = (JSONObject) obj;
				Document puerto = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
				// el puerto se elimina unicamente en caso de no tener contenedores asociados
				JSONObject criterio = new JSONObject();
				criterio.put("puertollegada", puerto.get_Id().toString());
				if (CntContenedoresService.searchContenedores(objStore, criterio, 1).isEmpty()) {
					criterio = new JSONObject();
					criterio.put("puertosalida", puerto.get_Id().toString());
					if (CntContenedoresService.searchContenedores(objStore, criterio, 1).isEmpty()) {					
						puerto.delete();
						puerto.save(RefreshMode.REFRESH);
					}
				}
				else
					jsonArray.remove(obj);			
			}					
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("count", jsonArray.size());
		return jsonResponse;			
	}				
	
	private JSONObject getFraccionJson(com.filenet.api.property.Properties props) throws Exception {
    	// Get Data
    	byte[] data = props.getBinaryValue("ClbJSONData");
    	JSONObject jsonObject = JSONObject.parse(new String(data));	
    	// Properties
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", ServiceUtil.sdf.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("DocumentTitle"));    	
    	return jsonObject;
	}
	
	private JSONObject getClienteJson(com.filenet.api.property.Properties props) throws Exception {
    	JSONObject jsonObject = new JSONObject();	
    	// Properties
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", ServiceUtil.sdf.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("FolderName"));
    	jsonObject.put("activo", props.getBooleanValue("Activo"));    	
    	return jsonObject;
	}	
	
	private JSONObject getNavieraJson(com.filenet.api.property.Properties props) throws Exception {
    	// Get Data
    	byte[] data = props.getBinaryValue("ClbJSONData");
    	JSONObject jsonObject = JSONObject.parse(new String(data));	
    	// Properties
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", ServiceUtil.sdf.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("DocumentTitle"));
    	jsonObject.put("activo", props.getBooleanValue("Activo"));
    	return jsonObject;
	}	
	
	private JSONObject getForwarderJson(com.filenet.api.property.Properties props) throws Exception {
		JSONObject jsonObject = new JSONObject();	
    	// Properties
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", ServiceUtil.sdf.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("DocumentTitle"));
    	jsonObject.put("activo", props.getBooleanValue("Activo"));
    	return jsonObject;
	}	
	
	private JSONObject getProveedorJson(com.filenet.api.property.Properties props) throws Exception {
    	// Get Data
    	byte[] data = props.getBinaryValue("ClbJSONData");
    	JSONObject jsonObject = JSONObject.parse(new String(data));	
    	// Properties
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", ServiceUtil.sdf.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("DocumentTitle"));
    	jsonObject.put("activo", props.getBooleanValue("Activo"));
    	return jsonObject;
	}	
	
	private JSONObject getImportadoraJson(com.filenet.api.property.Properties props) throws Exception {
    	// Get Data
    	byte[] data = props.getBinaryValue("ClbJSONData");
    	JSONObject jsonObject = JSONObject.parse(new String(data));	
    	// Properties
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", ServiceUtil.sdf.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("DocumentTitle"));
    	jsonObject.put("activo", props.getBooleanValue("Activo"));
    	return jsonObject;
	}	
	
	private JSONObject getPuertoJson(com.filenet.api.property.Properties props) throws Exception {
		JSONObject jsonObject = new JSONObject();	
    	// Properties
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", ServiceUtil.sdf.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("DocumentTitle"));
    	jsonObject.put("activo", props.getBooleanValue("Activo"));
    	return jsonObject;
	}		
	
}
