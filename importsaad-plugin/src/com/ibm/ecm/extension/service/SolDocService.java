package com.ibm.ecm.extension.service;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Document;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.core.VersionSeries;
import com.filenet.api.util.UserContext;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.util.Id;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.ecm.extension.util.mail.MailService;

import org.apache.commons.lang.StringEscapeUtils;

import com.ibm.json.java.JSONObject;
import com.ibm.json.java.JSONArray;

public class SolDocService extends PluginService {

	private final static DecimalFormat df = new DecimalFormat("$#,##0.00");
	private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	private final static String generalReadOnlyPolicy = "{B605FB9F-A0F1-4E75-82E8-A6DC5F758139}";		
	
	@Override	
	public String getId() {
		return "SolDocService";
	}	
	
	@Override	
	public void execute(PluginServiceCallbacks callbacks, HttpServletRequest request, HttpServletResponse response) throws Exception {

		String methodName = request.getParameter("method");
		if (methodName == null)
			throw new Exception("No se ha incluido el nombre del método a invocar.");
		
		callbacks.getLogger().logEntry(this, methodName, request);
			
		try {
	
			JSONObject result = new JSONObject();
			if (methodName.equals("getSolDocProveedores"))
				result = getProveedores(request, callbacks);
			else if (methodName.equals("updateSolDocProveedores"))
				result = updateProveedores(request, callbacks);
			else if (methodName.equals("deleteSolDocProveedores"))
				result = deleteProveedores(request, callbacks);
			else if (methodName.equals("addSolDocProveedores"))
				result = addProveedor(request, callbacks);
			else if (methodName.equals("addSolDocEmpresas"))
				result = addEmpresa(request, callbacks);	
			else if (methodName.equals("updateSolDocEmpresas"))
				result = updateEmpresas(request, callbacks);			
			else if (methodName.equals("deleteSolDocEmpresas"))
				result = deleteEmpresas(request, callbacks);			
			else if (methodName.equals("getSolDocClientes"))
				result = getClientes(request, callbacks);
			else if (methodName.equals("addSolDocClientes"))
				result = addCliente(request, callbacks);	
			else if (methodName.equals("updateSolDocClientes"))
				result = updateCliente(request, callbacks);
			else if (methodName.equals("deleteSolDocClientes"))
				result = deleteClientes(request, callbacks);		
			else if (methodName.equals("addSolDocSolicitudFactura"))
				result = addSolicitudFactura(request, callbacks);
			else if (methodName.equals("sendSolDocSoliciudFactura"))
				result = sendSolicitudFactura(request, callbacks);
			else if (methodName.equals("getSolDocDatosFactura"))
				result = getDatosFactura(request, callbacks);
			else if (methodName.equals("addDocument"))
				result = ContentService.addDocument(request, callbacks);
			else if (methodName.equals("getDocument"))
				result = ContentService.getDocument(request, callbacks);
			else if (methodName.equals("updateDocument"))
				result = ContentService.updateDocument(request, callbacks);			
			else if (methodName.equals("updateSolDocSolicitudFactura"))
				result = updateFactura(request, callbacks);
			else if (methodName.equals("sendSolDocFactura"))
				result = sendFactura(request, callbacks);
			else if (methodName.equals("getSolDocCurrentDocId"))
				result = getCurrentDocumentId(request, callbacks);
			else if (methodName.equals("deleteSolDocFactura"))
				result = deleteFactura(request, callbacks);	
			else if (methodName.equals("getSolDocEmpresas"))
				result = getEmpresas(request, callbacks);
			else if (methodName.equals("depurarSolDocCatalogos"))
				result = depurarCatalogos(request, callbacks);			
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
	private static JSONObject getProveedores(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonResponse = new JSONObject();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, FolderName, ClbJSONData, Activo");
		    sql.setFromClauseInitialValue("SolDocProveedor", null, false);			    
		    StringBuffer whereClause = new StringBuffer();
		    whereClause.append("This INFOLDER '/Proveedores'");	
		    if (request.getParameter("activo") != null)
		    	whereClause.append(" AND Activo = " + (Integer.parseInt(request.getParameter("activo")) == 1 ? "TRUE" : "FALSE"));
		    
		    sql.setWhereClause(whereClause.toString());		    
		    
		    // performance settings
		    JSONObject jsonSettings = ServiceUtil.getSettingsObject(objStore);
			sql.setMaxRecords(Integer.parseInt(jsonSettings.get("maxRecords").toString()));
			sql.setTimeLimit(Integer.parseInt(jsonSettings.get("timeLimit").toString()));			    
		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, null, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	byte[] data = props.getBinaryValue("ClbJSONData");
		    	JSONObject jsonProveedor = JSONObject.parse(new String(data));	
		    	jsonProveedor.put("id", row.getProperties().getIdValue("Id").toString());
		    	jsonProveedor.put("activo", props.getBooleanValue("Activo"));
				// add element
				jsonArray.add(jsonProveedor);					
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
	
	@SuppressWarnings("unchecked")
	private static JSONObject getEmpresas(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonResponse = new JSONObject();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
				
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, FolderName, Activo");
		    sql.setFromClauseInitialValue("SolDocEmpresa", null, false);	
		    StringBuffer whereClause = new StringBuffer();
		    if (request.getParameter("proveedor") != null) {
		    	whereClause.append("This INSUBFOLDER '/Proveedores/" + request.getParameter("proveedor") + "'");	
		    } else {
		    	whereClause.append("This INSUBFOLDER '/Proveedores'");
		    }
		    if (request.getParameter("activo") != null)
		    	whereClause.append(" AND Activo = " + (Integer.parseInt(request.getParameter("activo")) == 1 ? "TRUE" : "FALSE"));
		    
		    sql.setWhereClause(whereClause.toString());
		    
		    // performance settings
		    JSONObject jsonSettings = ServiceUtil.getSettingsObject(objStore);
			sql.setMaxRecords(Integer.parseInt(jsonSettings.get("maxRecords").toString()));
			sql.setTimeLimit(Integer.parseInt(jsonSettings.get("timeLimit").toString()));					    
		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, null, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	RepositoryRow row = it.next();
		    	JSONObject jsonEmpresa = new JSONObject();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	jsonEmpresa.put("id", props.getIdValue("Id").toString());
		    	jsonEmpresa.put("name", props.getStringValue("FolderName"));
		    	jsonEmpresa.put("activo", props.getBooleanValue("Activo"));		
				// add element
				jsonArray.add(jsonEmpresa);					
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
		jsonResponse.put("empresas", jsonArray);
		return jsonResponse;
		
	}		
	
	private static JSONObject addProveedor(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			JSONObject jsonProveedor = JSONObject.parse(request.getParameter("proveedor"));	
			
			// Obten instancia del folder raiz para proveedores
			Folder parentFolder = null;
			try {
				parentFolder = Factory.Folder.fetchInstance(objStore, "/Proveedores", null);	
			} catch (EngineRuntimeException ere) {
				ere.printStackTrace();
				error = ere.getMessage();
			}	
			
			//Valida que el nombre del proveedor no exista
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, FolderName");
		    sql.setFromClauseInitialValue("SolDocProveedor", null, false);
		    sql.setWhereClause("Parent = " + parentFolder.get_Id() + " AND FolderName = '" + jsonProveedor.get("proveedorNombre").toString() + "'");
		    sql.setMaxRecords(1);
			
		    RepositoryRowSet rowSet = search.fetchRows(sql, null, null, true);
		    if (!rowSet.isEmpty())
		    	throw new RuntimeException("El proveedor " + jsonProveedor.get("proveedorNombre").toString() + " ya existe.");				
			
			if (parentFolder != null) {
				// Crea proveedor folder
				Folder proveedor = ServiceUtil.createFolder(objStore, "SolDocProveedor", jsonProveedor.get("proveedorNombre").toString(), parentFolder);				
				com.filenet.api.property.Properties properties = proveedor.getProperties();
				jsonProveedor.put("id", proveedor.get_Id().toString());
				properties.putObjectValue("ClbJSONData", jsonProveedor.serialize().getBytes());
				properties.putObjectValue("Activo", Boolean.parseBoolean(jsonProveedor.get("activo").toString()));
				proveedor.save(RefreshMode.REFRESH);		
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		return jsonResponse;			
	}	
	
	private static JSONObject updateProveedores(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			JSONObject jsonProveedor = JSONObject.parse(request.getParameter("proveedor"));			
			
			// Get proveedor
			Folder proveedor = Factory.Folder.fetchInstance(objStore, jsonProveedor.get("id").toString(), null);				
			
			// Obten instancia del folder raiz para proveedores
			Folder parentFolder = null;
			try {
				parentFolder = Factory.Folder.fetchInstance(objStore, "/Proveedores", null);	
			} catch (EngineRuntimeException ere) {
				ere.printStackTrace();
				error = ere.getMessage();
			}	
			
			//Valida que el nombre del proveedor no exista
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, FolderName");
		    sql.setFromClauseInitialValue("SolDocProveedor", null, false);
		    sql.setWhereClause("Parent = " + parentFolder.get_Id() + " AND FolderName = '" + jsonProveedor.get("proveedorNombre").toString() + "' AND Id <> " + proveedor.get_Id());
		    sql.setMaxRecords(1);
			
		    RepositoryRowSet rowSet = search.fetchRows(sql, null, null, true);
		    if (!rowSet.isEmpty())
		    	throw new RuntimeException("El proveedor " + jsonProveedor.get("proveedorNombre").toString() + " ya existe.");				

			com.filenet.api.property.Properties properties = proveedor.getProperties();
			properties.putObjectValue("FolderName", jsonProveedor.get("proveedorNombre").toString());
			properties.putObjectValue("ClbJSONData", jsonProveedor.serialize().getBytes());
			properties.putObjectValue("Activo", Boolean.parseBoolean(jsonProveedor.get("activo").toString()));
			proveedor.save(RefreshMode.REFRESH);			    
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		return jsonResponse;			
		
	}	
	
	private static JSONObject deleteProveedores(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;			

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			JSONArray jsonProveedores = JSONArray.parse(request.getParameter("proveedores"));
			
			for(Object obj : jsonProveedores) 
			{
				JSONObject jsonProveedor = (JSONObject) obj;
				Folder proveedor = Factory.Folder.fetchInstance(objStore, jsonProveedor.get("id").toString(), null);
				proveedor.delete();
				proveedor.save(RefreshMode.REFRESH);				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		return jsonResponse;				
	}
	
	private static JSONObject addEmpresa(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		try {
			
			String proveedorId = request.getParameter("proveedorid");	
			String nombreEmpresa = request.getParameter("empresa");
			boolean activo = Boolean.parseBoolean(request.getParameter("activo"));	
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);		
			
			// Obten instancia proveedor
			Folder proveedor = null;
			try {
				proveedor = Factory.Folder.fetchInstance(objStore, proveedorId, null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El folder del proveedor no fue localizado.");		
			}
			
			//Valida que el nombre de la empresa no exista para el proveedor seleccionado
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, FolderName");
		    sql.setFromClauseInitialValue("SolDocEmpresa", null, false);
		    sql.setWhereClause("Parent = " + proveedor.get_Id() + " AND FolderName = '" + nombreEmpresa + "'");
		    sql.setMaxRecords(1);
			
		    RepositoryRowSet rowSet = search.fetchRows(sql, null, null, true);
		    if (!rowSet.isEmpty())
		    	throw new RuntimeException("La empresa " + nombreEmpresa + " ya existe para el proveedor seleccionado.");			
			
			// Crea empresa folder
			Folder empresa = ServiceUtil.createFolder(objStore, "SolDocEmpresa", nombreEmpresa, proveedor);	
			com.filenet.api.property.Properties properties = empresa.getProperties();
			properties.putValue("Activo", activo);
			empresa.save(RefreshMode.REFRESH);		
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		return jsonResponse;			
	}
	
	private static JSONObject updateEmpresas(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	

		try {
			
			JSONObject jsonEmpresa = JSONObject.parse(request.getParameter("empresa"));
			String proveedorId = request.getParameter("proveedorid");	
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Obten instancia proveedor
			Folder proveedor = null;
			try {
				proveedor = Factory.Folder.fetchInstance(objStore, proveedorId, null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El folder del proveedor no fue localizado.");		
			}
			
			// Get empresa
			Folder empresa = Factory.Folder.fetchInstance(objStore, jsonEmpresa.get("id").toString(), null);
			
			// Valida que el nombre de la empresa no exista para el proveedor seleccionado
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, FolderName");
		    sql.setFromClauseInitialValue("SolDocEmpresa", null, false);
		    sql.setWhereClause("Parent = " + proveedor.get_Id() + " AND FolderName = '" + jsonEmpresa.get("empresaNombre").toString() + "' AND Id <> " + empresa.get_Id());
		    sql.setMaxRecords(1);
			
		    RepositoryRowSet rowSet = search.fetchRows(sql, null, null, true);
		    if (!rowSet.isEmpty())
		    	throw new RuntimeException("La empresa " + jsonEmpresa.get("empresaNombre").toString() + " ya existe para el proveedor seleccionado."); 			

			com.filenet.api.property.Properties properties = empresa.getProperties();
			properties.putValue("FolderName", jsonEmpresa.get("empresaNombre").toString());
			properties.putValue("Activo", Boolean.parseBoolean(jsonEmpresa.get("activo").toString()));
			empresa.save(RefreshMode.REFRESH);		
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		return jsonResponse;			
	}	
	
	private static JSONObject deleteEmpresas(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			JSONArray jsonEmpresas = JSONArray.parse(request.getParameter("empresas"));
			
			for(Object obj : jsonEmpresas) 
			{
				JSONObject jsonEmpresa = (JSONObject) obj;
				Folder empresa = Factory.Folder.fetchInstance(objStore, jsonEmpresa.get("id").toString(), null);
				
				// Valida que la empresa no se encuentre asociada a ninguna solicitud
			    SearchScope search = new SearchScope(objStore);
			    SearchSQL sql = new SearchSQL();
			    sql.setSelectList("Id");
			    sql.setFromClauseInitialValue("SolDocCase", null, false);
			    sql.setWhereClause("Empresa = " + empresa.get_Id().toString());
			    sql.setMaxRecords(1);
				
			    RepositoryRowSet rowSet = search.fetchRows(sql, null, null, true);
			    if (!rowSet.isEmpty())
			    	throw new RuntimeException ("La empresa " + empresa.get_FolderName() + " se encuentra asociada a una o más solicitudes");		

				empresa.delete();
				empresa.save(RefreshMode.REFRESH);				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		return jsonResponse;				
	}
	
	@SuppressWarnings("unchecked")
	private static JSONObject depurarCatalogos(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		int count = 0;

		try {
			
			int tipoCatalogo = Integer.parseInt(request.getParameter("tipo"));
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			SearchScope search;
			SearchSQL sql;
			FolderSet folSet;
			RepositoryRowSet rowSet;
			
			switch (tipoCatalogo) {
			
				case 1: // Empresas
					
					// Obtiene todas las empresas y en caso de no tener ninguna asociacion activa es eliminada
				    search = new SearchScope(objStore);
				    sql = new SearchSQL();
				    sql.setSelectList("Id");
				    sql.setFromClauseInitialValue("SolDocEmpresa", null, false);
				    		    
				    folSet = (FolderSet) search.fetchObjects(sql, null, null, true);
				    
				    for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) 
				    {
				    	Folder empresa = it.next();
				    	
						// Valida que la empresa no se encuentre asociada a ninguna solicitud
					    search = new SearchScope(objStore);
					    sql = new SearchSQL();
					    sql.setSelectList("Id");
					    sql.setFromClauseInitialValue("SolDocCase", null, false);
					    sql.setWhereClause("Empresa = " + empresa.get_Id().toString());
					    sql.setMaxRecords(1);
					    rowSet = search.fetchRows(sql, null, null, true);
					    if (!rowSet.isEmpty())
					    	continue;
					    					    
				    	empresa.delete();
				    	empresa.save(RefreshMode.REFRESH);
				    	count++;
				    }					
					
					break;
					
				case 2: // Proveedores
					
					// Obtiene todos los proveedores y en caso de no tener ninguna asociacion activa es eliminado
				    search = new SearchScope(objStore);
				    sql = new SearchSQL();
				    sql.setSelectList("Id");
				    sql.setFromClauseInitialValue("SolDocProveedor", null, false);
				    		    
				    folSet = (FolderSet) search.fetchObjects(sql, null, null, true);
				    
				    for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) 
				    {
				    	Folder proveedor = it.next();
				    	
						// Valida que la empresa no se encuentre asociada a ninguna solicitud
					    search = new SearchScope(objStore);
					    sql = new SearchSQL();
					    sql.setSelectList("Id");
					    sql.setFromClauseInitialValue("SolDocCase", null, false);
					    sql.setWhereClause("Proveedor = " + proveedor.get_Id().toString());
					    sql.setMaxRecords(1);						    
					    rowSet = search.fetchRows(sql, null, null, true);
					    if (!rowSet.isEmpty())
					    	continue;
				   					    
					    proveedor.delete();
					    proveedor.save(RefreshMode.REFRESH);
				    	count++;
				    }					
					
					break;		
					
				case 3: // Clientes
					
					// Obtiene todos los clientes y en caso de no tener ninguna asociacion activa es eliminado
				    search = new SearchScope(objStore);
				    sql = new SearchSQL();
				    sql.setSelectList("Id, PathName, SubFolders");
				    sql.setFromClauseInitialValue("SolDocCliente", null, false);
				    		    
				    folSet = (FolderSet) search.fetchObjects(sql, null, null, true);
				    
				    for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) 
				    {
				    	Folder cliente = it.next();
				    	
						// Valida que la empresa no se encuentre asociada a ninguna solicitud
					    search = new SearchScope(objStore);
					    sql = new SearchSQL();
					    sql.setSelectList("Id");
					    sql.setFromClauseInitialValue("SolDocCase", null, false);
					    sql.setWhereClause("This INSUBFOLDER '" + cliente.get_PathName() + "'");
					    sql.setMaxRecords(1);					    
					    rowSet = search.fetchRows(sql, null, null, true);
					    if (!rowSet.isEmpty())
					    	continue;
					    
						// Valida que la empresa no se contenga ningun documento asociado (incluyendo pagos y devoluciones)
					    search = new SearchScope(objStore);
					    sql = new SearchSQL();
					    sql.setSelectList("Id");
					    sql.setFromClauseInitialValue("Document", null, true);
					    sql.setWhereClause("This INSUBFOLDER '" + cliente.get_PathName() + "'");
					    sql.setMaxRecords(1);					    
					    rowSet = search.fetchRows(sql, null, null, true);	
					    if (!rowSet.isEmpty())
					    	continue;					    
					    			
					    ServiceUtil.deleteRecursively(cliente);
				    	count++;
				    }					
					
					break;						
			
			}
					
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("count", count);
		return jsonResponse;				
	}	
	
	@SuppressWarnings("unchecked")
	private static JSONObject getClientes(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonResponse = new JSONObject();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, FolderName, ClbJSONData, Activo");
		    sql.setFromClauseInitialValue("SolDocCliente", null, false);
		    StringBuffer whereClause = new StringBuffer();
		    whereClause.append("This INFOLDER '/Facturas'");
		    if (request.getParameter("activo") != null)
		    	whereClause.append(" AND Activo = " + (Integer.parseInt(request.getParameter("activo")) == 1 ? "TRUE" : "FALSE"));
		    
		    sql.setWhereClause(whereClause.toString());
		    
		    // performance settings
		    JSONObject jsonSettings = ServiceUtil.getSettingsObject(objStore);
			sql.setMaxRecords(Integer.parseInt(jsonSettings.get("maxRecords").toString()));
			sql.setTimeLimit(Integer.parseInt(jsonSettings.get("timeLimit").toString()));				    
		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, null, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
		    	byte[] data = props.getBinaryValue("ClbJSONData");
		    	JSONObject jsonCliente = JSONObject.parse(new String(data));
		    	jsonCliente.put("activo", props.getBooleanValue("Activo"));
				// add element
				jsonArray.add(jsonCliente);					
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
	
	private static JSONObject addCliente(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			JSONObject jsonCliente = JSONObject.parse(request.getParameter("cliente"));	
			
			// Obten instancia del folder raiz para clientes
			Folder parentFolder = null;
			try {
				parentFolder = Factory.Folder.fetchInstance(objStore, "/Facturas", null);	
			} catch (EngineRuntimeException ere) {
				ere.printStackTrace();
				error = ere.getMessage();
			}
			
			//Valida que el nombre del cliente no exista
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, FolderName");
		    sql.setFromClauseInitialValue("SolDocCliente", null, false);
		    sql.setWhereClause("Parent = " + parentFolder.get_Id() + " AND FolderName = '" + jsonCliente.get("razonSocial").toString() + "'");
		    sql.setMaxRecords(1);
			
		    RepositoryRowSet rowSet = search.fetchRows(sql, null, null, true);
		    if (!rowSet.isEmpty())
		    	throw new RuntimeException("El cliente " + jsonCliente.get("razonSocial").toString() + " ya existe.");				
			
			if (parentFolder != null) {
				// Crea cliente folder
				Folder cliente = ServiceUtil.createFolder(objStore, "SolDocCliente", jsonCliente.get("razonSocial").toString(), parentFolder);				
				com.filenet.api.property.Properties properties = cliente.getProperties();
				jsonCliente.put("id", cliente.get_Id().toString());
				properties.putObjectValue("ClbJSONData", jsonCliente.serialize().getBytes());
				properties.putObjectValue("Activo", Boolean.parseBoolean(jsonCliente.get("activo").toString()));
				cliente.save(RefreshMode.REFRESH);		
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		return jsonResponse;			
	}	
	
	private static JSONObject updateCliente(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			JSONObject jsonCliente = JSONObject.parse(request.getParameter("cliente"));
			
			// Obten instancia del folder raiz para clientes
			Folder parentFolder = null;
			try {
				parentFolder = Factory.Folder.fetchInstance(objStore, "/Facturas", null);	
			} catch (EngineRuntimeException ere) {
				ere.printStackTrace();
				error = ere.getMessage();
			}		
			
			// Get cliente
			Folder cliente = Factory.Folder.fetchInstance(objStore, jsonCliente.get("id").toString(), null);			
			
			//Valida que el nombre del cliente no exista
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, FolderName");
		    sql.setFromClauseInitialValue("SolDocCliente", null, false);
		    sql.setWhereClause("Parent = " + parentFolder.get_Id() + " AND FolderName = '" + jsonCliente.get("razonSocial").toString() + "' AND Id <> " + cliente.get_Id());
		    sql.setMaxRecords(1);
			
		    RepositoryRowSet rowSet = search.fetchRows(sql, null, null, true);
		    if (!rowSet.isEmpty())
		    	throw new RuntimeException("El cliente " + jsonCliente.get("razonSocial").toString() + " ya existe.");				

			com.filenet.api.property.Properties properties = cliente.getProperties();
			properties.putObjectValue("FolderName", jsonCliente.get("razonSocial").toString());
			properties.putObjectValue("ClbJSONData", jsonCliente.serialize().getBytes());
			properties.putObjectValue("Activo", Boolean.parseBoolean(jsonCliente.get("activo").toString()));
			cliente.save(RefreshMode.REFRESH);		    
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		return jsonResponse;			
		
	}		
	
	private static JSONObject deleteClientes(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			JSONArray jsonClientes = JSONArray.parse(request.getParameter("clientes"));
			
			for(Object obj : jsonClientes) 
			{
				JSONObject jsonCliente = (JSONObject) obj;
				Folder cliente = Factory.Folder.fetchInstance(objStore, jsonCliente.get("id").toString(), null);
				cliente.delete();
				cliente.save(RefreshMode.REFRESH);				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		return jsonResponse;				
	}
	
	private static JSONObject getCurrentDocumentId(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		JSONObject jsonObject = new JSONObject();

		try {
			
			String itemId = request.getParameter("itemid");
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			 
			Document doc = Factory.Document.fetchInstance(objStore, itemId, null);
			jsonObject.put("id", ((Document)doc.get_CurrentVersion()).get_Id().toString());
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
    		e.printStackTrace();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
    	jsonResponse.put("documento", jsonObject);
		return jsonResponse;		
		
	}				
	
	@SuppressWarnings("unchecked")
	private static JSONObject getDatosFactura(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		JSONArray jsonSolicitudes = new JSONArray();

		try {
			
			JSONArray jsonItems = JSONArray.parse(request.getParameter("items"));
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);

			for (Object obj : jsonItems) 
			{
				JSONObject jsonItem = (JSONObject) obj;	 
				JSONObject jsonSolicitud = new JSONObject();
				Folder solicitud = Factory.Folder.fetchInstance(objStore, jsonItem.get("id").toString(), null);
				com.filenet.api.property.Properties props = solicitud.getProperties();
				jsonSolicitud.put("id", solicitud.get_Id().toString());
				jsonSolicitud.put("numeroFactura", props.getStringValue("NumeroFactura"));
				jsonSolicitud.put("fechaFactura", null);
				if (props.getDateTimeValue("FechaFactura") != null) {
					jsonSolicitud.put("fechaFactura", sdf.format(ServiceUtil.getUTFCalendar(props.getDateTimeValue("FechaFactura")).getTime()));
				}
				jsonSolicitud.put("empresaNombre", null);
				if (props.getObjectValue("Empresa") != null) {
					Folder empresa = (Folder) props.getEngineObjectValue("Empresa");
					jsonSolicitud.put("empresaNombre", empresa.get_FolderName());
				}
				jsonSolicitud.put("estado", props.getInteger32Value("EstadoCFDI"));
		    	byte[] data = props.getBinaryValue("ClbJSONData");
		    	JSONObject jsonData = JSONObject.parse(new String(data));
				jsonSolicitud.put("datos", jsonData);
				
				jsonSolicitud.put("filePDF", null);
				jsonSolicitud.put("fileXML", null);
				DocumentSet docSet = solicitud.get_ContainedDocuments();
				for (Iterator<Document> it = docSet.iterator(); it.hasNext();) {
					Document doc = it.next();
					if (doc.getClassName().equals("SolDocCFDI") && doc.getProperties().getInteger32Value("TipoCFDI") == 0) { // PDF
						JSONObject jsonContent = new JSONObject();
						ContentTransfer ct = (ContentTransfer) doc.get_ContentElements().get(0);
						jsonContent.put("id", doc.get_Id().toString());
						jsonContent.put("name", ct.get_RetrievalName());
						jsonSolicitud.put("filePDF", jsonContent);						
					} else if (doc.getClassName().equals("SolDocCFDI") && doc.getProperties().getInteger32Value("TipoCFDI") == 1) { // XML
						JSONObject jsonContent = new JSONObject();
						ContentTransfer ct = (ContentTransfer) doc.get_ContentElements().get(0);
						jsonContent.put("id", doc.get_Id().toString());
						jsonContent.put("name", ct.get_RetrievalName());
						jsonSolicitud.put("fileXML", jsonContent);								
					}
				}

				jsonSolicitudes.add(jsonSolicitud);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
    	jsonResponse.put("solicitudes", jsonSolicitudes);
		return jsonResponse;		
		
	}				
	
	@SuppressWarnings("unchecked")
	private static JSONObject updateFactura(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		Folder solicitud = null;
		Document docPDF = null;
		Document docXML = null;
		
		try {
			
			String itemId = request.getParameter("itemid");
			JSONObject jsonData = JSONObject.parse(request.getParameter("datos"));
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
						
			// Get solicitud
			solicitud = Factory.Folder.fetchInstance(objStore, itemId, null);
			
			// Get estado actual de la factura
			int estadoSolicitud = solicitud.getProperties().getInteger32Value("EstadoCFDI");
			 		
			DocumentSet docSet;
							
			// Get folder proveedor
			Folder proveedor = null;
			try {
				proveedor = Factory.Folder.fetchInstance(objStore, "/Proveedores/" + jsonData.get("nombreProveedor").toString(), null);						
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El proveedor seleccionado no fue localizado.");	    		
			}	
			
			if (jsonData.get("filePDF") != null && jsonData.get("fileXML") != null)
			{
				ReferentialContainmentRelationship rel = null;
				
				// Get archivos CFDI
				docPDF = Factory.Document.fetchInstance(objStore, ((JSONObject)jsonData.get("filePDF")).get("id").toString(), null);
				docXML = Factory.Document.fetchInstance(objStore, ((JSONObject)jsonData.get("fileXML")).get("id").toString(), null);
				
				// Se identifica si los archivos CFDI son nuevos o no
				boolean newPDF = true;
				boolean newXML = true;
				docSet = solicitud.get_ContainedDocuments();
				for (Iterator<Document> it = docSet.iterator(); it.hasNext();) {
					Document doc = it.next();
					if (doc.getClassName().equals("SolDocCFDI")) {
						if (doc.getProperties().getInteger32Value("TipoCFDI") == 0) { // PDF
							if (doc.equals(docPDF))
								newPDF = false;
						} else if (doc.getProperties().getInteger32Value("TipoCFDI") == 1) { // XML
							if (doc.equals(docXML))
								newXML = false;							
						}
					}
				}
				
				// Se eliminan los archivos CFDI previamente asociados en caso de incluir nuevos CFDI
				docSet = solicitud.get_ContainedDocuments();
				for (Iterator<Document> it = docSet.iterator(); it.hasNext();) {
					Document doc = it.next();
					if (doc.getClassName().equals("SolDocCFDI")) {
						if (doc.getProperties().getInteger32Value("TipoCFDI") == 0 && newPDF) { // PDF
							doc.delete();
							doc.save(RefreshMode.REFRESH);
						} else if (doc.getProperties().getInteger32Value("TipoCFDI") == 1 && newXML) { // XML
							doc.delete();
							doc.save(RefreshMode.REFRESH);						
						}							
					}
				}			
				
				// Asociar archivo PDF
				if (newPDF) {
					// Unfile from temp
					FolderSet folSet = docPDF.get_FoldersFiledIn();
					for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) {
						Folder temp = it.next();
						rel = temp.unfile(docPDF);
						rel.save(RefreshMode.NO_REFRESH);
					}	
					
					// Asociar archivo CFDI a solicitud
					docPDF.changeClass("SolDocCFDI");
					docPDF.getProperties().putObjectValue("TipoCFDI", 0); // PDF
					docPDF.applySecurityTemplate(new Id(generalReadOnlyPolicy));
					docPDF.save(RefreshMode.REFRESH);	
					rel = solicitud.file(docPDF, AutoUniqueName.AUTO_UNIQUE, docPDF.get_Name(), DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
					rel.save(RefreshMode.NO_REFRESH);						
				}
				
				// Asociar archivo XML
				if (newXML) {
					// Unfile from temp
					FolderSet folSet = docXML.get_FoldersFiledIn();
					for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) {
						Folder temp = it.next();
						rel = temp.unfile(docXML);
						rel.save(RefreshMode.NO_REFRESH);
					}						
					
					// Asociar archivo CFDI a solicitud
					docXML = Factory.Document.fetchInstance(objStore, ((JSONObject)jsonData.get("fileXML")).get("id").toString(), null);
					docXML.changeClass("SolDocCFDI");
					docXML.getProperties().putObjectValue("TipoCFDI", 1); // XML
					docXML.applySecurityTemplate(new Id(generalReadOnlyPolicy));
					docXML.save(RefreshMode.REFRESH);
					rel = solicitud.file(docXML, AutoUniqueName.AUTO_UNIQUE, docXML.get_Name(), DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
					rel.save(RefreshMode.NO_REFRESH);						
				}
			} 
			else 
			{
				docSet = solicitud.get_ContainedDocuments();
				for (Iterator<Document> it = docSet.iterator(); it.hasNext();) {
					Document doc = it.next();
					if (doc.getClassName().equals("SolDocCFDI")) {
						doc.delete();
						doc.save(RefreshMode.REFRESH);
					}
				}				
			}
										
			// Update Properties
			com.filenet.api.property.Properties props = solicitud.getProperties();
			byte[] data = props.getBinaryValue("ClbJSONData");
			JSONObject jsonObj = JSONObject.parse(new String(data));
			
			if (estadoSolicitud == 0) { // Unicamente se actualiza o se crea la empresa si es igual a "Pendiente"
			
				// Se obtiene la empresa o se crea una nueva en caso de no obtenerla			
				Folder empresa = null;
				String nombreEmpresa = null;
				if (!jsonData.get("nombreEmpresa").equals("")) {
					nombreEmpresa = jsonData.get("nombreEmpresa").toString();
					try {
						empresa = Factory.Folder.fetchInstance(objStore, proveedor.get_PathName() + "/" + jsonData.get("nombreEmpresa").toString(), null);						
					} catch (EngineRuntimeException ere) {
						ere.printStackTrace();
					}
					
					if (empresa == null) {
						// Crea folder empresa
						empresa = ServiceUtil.createFolder(objStore, "SolDocEmpresa", jsonData.get("nombreEmpresa").toString(), proveedor);								
					}		
				}
	
				// Establece empresa
				props.putObjectValue("Empresa", empresa);
				jsonObj.put("nombreEmpresa", nombreEmpresa);
			
			}
					
			// Establece propiedades basado en archivos CFDI
			if (jsonData.get("filePDF") != null && jsonData.get("fileXML") != null)
			{				
				props.putObjectValue("NumeroFactura", jsonData.get("numeroFactura"));
				props.putObjectValue("FechaFactura", ServiceUtil.getUTFCalendar(sdf.parse(jsonData.get("fechaFactura").toString())).getTime());
				jsonObj.put("numeroFactura", jsonData.get("numeroFactura"));
				jsonObj.put("fechaFactura", jsonData.get("fechaFactura"));
			} 
			else 
			{
				props.putObjectValue("NumeroFactura", null);
				props.putObjectValue("FechaFactura", null);
				jsonObj.put("numeroFactura", null);
				jsonObj.put("fechaFactura", null);	
			}
			
			// Actualiza proveedor
			props.putObjectValue("Proveedor", proveedor);
			
			// Actualiza comisiones
			if (estadoSolicitud == 0) { // Se actualizan comisiones del proveedor unicamente si es igual a "Pendiente"
				jsonObj.put("porcentajeComisionProveedor", jsonData.get("porcentajeComisionProveedor"));
				jsonObj.put("montoComisionProveedor", jsonData.get("montoComisionProveedor"));
			}
			if (estadoSolicitud != 3) { // Se actualizan comisiones del distribuidor unicamente si no es "Entregada a Cliente"
				jsonObj.put("porcentajeComisionDistribuidor", jsonData.get("porcentajeComisionDistribuidor"));
				jsonObj.put("montoComisionDistribuidor", jsonData.get("montoComisionDistribuidor"));			
			}
			
			// Actualiza metodo de pago y numero de cuenta
			jsonObj.put("metodoPago", jsonData.get("metodoPago"));
			jsonObj.put("numeroCuenta", jsonData.get("numeroCuenta"));				
			
			// Actualiza conceptos
			jsonObj.put("conceptos", jsonData.get("conceptos"));
			
			props.putObjectValue("ClbJSONData", jsonObj.serialize().getBytes());				
			
			// Save
			solicitud.save(RefreshMode.REFRESH);	
						
			// Actualiza devolucion pendiente para el cliente de acuerdo a la comision del distribuidor actualizada
			if (estadoSolicitud != 3) { // Distinta a "Entregada a Cliente"
				docSet = solicitud.get_ContainedDocuments();
				for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) {
					Document doc = it.next();
					props = doc.getProperties();
					if (doc.getClassName().equals("SolDocDevolucion") && props.getInteger32Value("TipoDevolucion") == 1 && props.getBooleanValue("Pendiente")) { // Devolucion para el cliente pendiente
						JSONObject jsonDevolucion = SolDocPagosService.getDatosDevolucionCliente(objStore, doc);
						props.putObjectValue("MontoTotal", Double.parseDouble(jsonDevolucion.get("devolucionSolicitada").toString()));
						doc.save(RefreshMode.REFRESH);
						break;
					}
				}
			}
								
		} catch (Exception e) {
			error = e.getMessage();
			e.printStackTrace();			
			
		} finally {		
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		return jsonResponse;		
		
	}		
	
	private static JSONObject addSolicitudFactura(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0; // success
    	String error = null;	
    	String folio = null; 
		JSONArray jsonFolios = new JSONArray();

		try {
			
			JSONObject jsonData = JSONObject.parse(request.getParameter("datos"));
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);			
			
			// Get folder proveedor
			Folder proveedor = null;
			try {
				proveedor = Factory.Folder.fetchInstance(objStore, "/Proveedores/" + jsonData.get("nombreProveedor").toString(), null);						
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El proveedor seleccionado no fue localizado.");	    		
			}				
			
			// Se crea la nueva empresa en caso de que asi haya sido indicado
			Folder empresa = null;
			if ((Boolean)jsonData.get("nuevaEmpresa")) {		
				// Crea folder empresa
				empresa = ServiceUtil.createFolder(objStore, "SolDocEmpresa", jsonData.get("nombreEmpresa").toString(), proveedor);										
			} else if (!jsonData.get("nombreEmpresa").toString().isEmpty()) {
				empresa = Factory.Folder.fetchInstance(objStore, proveedor.get_PathName() + "/" + jsonData.get("nombreEmpresa").toString(), null);
			}
			
			// Get folder cliente
			Folder cliente = null;
			try {
				cliente = Factory.Folder.fetchInstance(objStore, "/Facturas/" + jsonData.get("razonSocial").toString(), null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El cliente seleccionado no fue localizado.");		
			}
			
			// Get year-month-based parent folder
			Folder parentFolder = getYearMonthParent(objStore, new Date(), cliente);
			
			// Se el numero de solicitudes en base al numero de copias solicitado
			Folder solicitud = null;
			for (int i = 0; i < (Long)jsonData.get("numeroCopias"); i++) 
			{
				JSONObject jsonFolio = new JSONObject();
				
				// Se obtiene el folio de la solucitud a crear
				folio = getNextFolio(objStore);	
				jsonFolio.put("value", folio);
				
				// Se crea la nueva solicitud de factura y se asocian las propiedades correspondientes
				solicitud = Factory.Folder.createInstance(objStore, "SolDocCase");
				solicitud.set_FolderName(folio);
				solicitud.set_Parent(parentFolder);
				
				com.filenet.api.property.Properties properties = solicitud.getProperties();
				properties.putObjectValue("FolderName", folio);
				properties.putObjectValue("Proveedor", proveedor);
				if (empresa != null)
					properties.putObjectValue("Empresa", empresa);
				properties.putObjectValue("MontoTotal", ServiceUtil.getDouble(jsonData.get("montoTotal")));
				properties.putObjectValue("NumeroFactura", null);
				properties.putObjectValue("FechaFactura", null);
				properties.putObjectValue("EstadoCFDI", 0); // Pendiente de Pago
				properties.putObjectValue("ClbJSONData", jsonData.serialize().getBytes());
								
				solicitud.save(RefreshMode.REFRESH);
						
				jsonFolios.add(jsonFolio);
			}
			
			// Se notifica por correo electronico al proveedor
			if (!(Boolean)jsonData.get("omitirEnvioCorreo")) {
				try 
				{
					List<String> folios = new ArrayList<String>();
					for (Object obj : jsonFolios) {
						JSONObject jsonFolio = (JSONObject) obj;
						folios.add(jsonFolio.get("value").toString());
					}
					// Notifica en base a ultima solicitud creada
					notificaSolicitudFactura(objStore, folios, solicitud, null, proveedor, false);
				}
				catch (Exception e) 
				{
					status = 2; // error en notificacion
					error = e.getMessage();
					e.printStackTrace();
				}
			}
			
			
		} catch (Exception e) {
    		status = 1; // error en transaccion
    		error = e.getMessage();
    		e.printStackTrace();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
    	jsonResponse.put("status", status);
		jsonResponse.put("error", error);
		jsonResponse.put("folios", jsonFolios);
		return jsonResponse;		
		
	}
	
	private static JSONObject sendSolicitudFactura(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
    	String error = null;	
    	int status = 0;
		JSONObject jsonObject = new JSONObject();
		JSONArray solicitudes = new JSONArray();

		try {
			
			JSONArray jsonSolicitudes = JSONArray.parse(request.getParameter("solicitudes"));
			String observaciones = request.getParameter("observaciones");
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			for (Object obj : jsonSolicitudes) 
			{
				try {
					status = 0;
					error = null;	
					JSONObject jsonSolicitud = (JSONObject) obj;
		    	    Folder solicitud = Factory.Folder.fetchInstance(objStore, jsonSolicitud.get("id").toString(), null);		    	    
		    	    Folder proveedor = (Folder) solicitud.getProperties().getObjectValue("Proveedor");
		    	    List<String> folios = new ArrayList<String>();
		    	    folios.add(solicitud.get_Name());	    	    
		    	    
		    	    notificaSolicitudFactura(objStore, folios, solicitud, observaciones, proveedor, true);
		    	    
				} catch (Exception e) {
					status = 1;
					e.printStackTrace();
					error = e.getMessage();
		    		
				} finally {
					jsonObject = new JSONObject();
		    	    jsonObject.put("status", status);
		    	    jsonObject.put("error", error);
		    	    solicitudes.add(jsonObject);
				}	
			}			

		} catch (Exception e) {
			e.printStackTrace();
    		error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("solicitudes", solicitudes);
		return jsonResponse;		
		
	}	
	
	@SuppressWarnings("unchecked")
	private static JSONObject deleteFactura(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
    	String error = null;	
		JSONObject jsonObject = new JSONObject();

		try {
			
			JSONArray jsonSolicitudes = JSONArray.parse(request.getParameter("solicitudes"));
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Validar que las facturas no tengan pagos asociados
			for (Object obj : jsonSolicitudes) 
			{
				JSONObject jsonSolicitud = (JSONObject) obj;
				Folder solicitud = Factory.Folder.fetchInstance(objStore, jsonSolicitud.get("id").toString(), null);		    	    
				DocumentSet docSet = solicitud.get_ContainedDocuments();
				for (Iterator<Document> it = docSet.iterator(); it.hasNext();) {
					Document doc = it.next();
					if (doc.getClassName().equals("SolDocPago"))
						throw new RuntimeException("La factura no puede ser eliminada ya que tiene pagos asociados.");
				}	
			}		
			
			// Delete documentos y solicitud
			for (Object obj : jsonSolicitudes) 
			{
				try {
					error = null;	
					JSONObject jsonSolicitud = (JSONObject) obj;
		    	    Folder solicitud = Factory.Folder.fetchInstance(objStore, jsonSolicitud.get("id").toString(), null);		    	    
					DocumentSet docSet = solicitud.get_ContainedDocuments();
					for (Iterator<Document> it = docSet.iterator(); it.hasNext();) {
						Document doc = it.next();
						VersionSeries vs = doc.get_VersionSeries();
						vs.delete();
						vs.save(RefreshMode.REFRESH);							
					}
					solicitud.delete();
					solicitud.save(RefreshMode.REFRESH);	
		    	    
				} catch (Exception e) {
					e.printStackTrace();
					error = e.getMessage();
		    		
				} finally {
					jsonObject = new JSONObject();
		    	    jsonObject.put("error", error);
				}	
			}			

		} catch (Exception e) {
			e.printStackTrace();
    		error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		return jsonResponse;		
		
	}	
	
	private static JSONObject sendFactura(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
    	String error = null;	
    	int status = 0;
		JSONObject jsonObject = new JSONObject();
		JSONArray solicitudes = new JSONArray();

		try {
			
			JSONArray jsonSolicitudes = JSONArray.parse(request.getParameter("solicitudes"));
			String observaciones = request.getParameter("observaciones");
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			for (Object obj : jsonSolicitudes) 
			{
				try {
					status = 0;
					error = null;	
					JSONObject jsonSolicitud = (JSONObject) obj;
		    	    Folder solicitud = Factory.Folder.fetchInstance(objStore, jsonSolicitud.get("id").toString(), null);
		        	byte[] data = solicitud.getProperties().getBinaryValue("ClbJSONData");
		        	JSONObject jsonData = JSONObject.parse(new String(data));
		        	Folder cliente = Factory.Folder.fetchInstance(objStore, "/Facturas/" + jsonData.get("razonSocial").toString(), null);
		    	    notificaEnvioFactura(objStore, solicitud, cliente, observaciones);
		    	    
				} catch (Exception e) {
					status = 1;
					e.printStackTrace();
					error = e.getMessage();
		    		
				} finally {
					jsonObject = new JSONObject();
		    	    jsonObject.put("status", status);
		    	    jsonObject.put("error", error);
		    	    solicitudes.add(jsonObject);
				}	
			}			

		} catch (Exception e) {
			e.printStackTrace();
    		error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("solicitudes", solicitudes);
		return jsonResponse;		
		
	}		

	private static synchronized String getNextFolio(ObjectStore os) throws Exception {
		Document settings = null;
		try {
			settings = Factory.Document.fetchInstance(os, "/Settings/SolDocSettings", null);
		} catch (EngineRuntimeException ere) {
			throw new RuntimeException ("No se encontró la instancia de la configuración general.");
		}
		
    	byte[] data = settings.getProperties().getBinaryValue("ClbJSONData");
    	if (data == null)
			throw new RuntimeException ("No se encontraron datos almacenados de la configuración general.");	    		
    
    	JSONObject jsonSettings = JSONObject.parse(new String(data));	
    	long folioConsecutivo = (Long) jsonSettings.get("folioConsecutivo");
    	jsonSettings.put("folioConsecutivo", folioConsecutivo + 1);
		com.filenet.api.property.Properties properties = settings.getProperties();
		properties.putValue("ClbJSONData", jsonSettings.serialize().getBytes());
		settings.save(RefreshMode.REFRESH);   
		
		String folio = String.format("%09d", folioConsecutivo); // nine-digit string
		folio = new StringBuilder(folio).insert(6, ",").insert(3, ",").toString(); // set folio format as ###,###,###		
		
		return folio;
	}
	
	private static void notificaSolicitudFactura(ObjectStore os, List<String> folios, Folder solicitud, String observaciones, Folder proveedor, boolean forceSingleCopy) throws Exception {
		
		// Get copia oculta de configuracion general
		Document settings = Factory.Document.fetchInstance(os, "/Settings/SolDocSettings", null);
		byte[] data = settings.getProperties().getBinaryValue("ClbJSONData");		
		JSONObject jsonSettings = JSONObject.parse(new String(data));
		
		// Get datos proveedor
    	data = proveedor.getProperties().getBinaryValue("ClbJSONData");
    	JSONObject jsonDatosProveedor = JSONObject.parse(new String(data));				
						
		// Get datos solicitud
    	data = solicitud.getProperties().getBinaryValue("ClbJSONData");
    	JSONObject jsonDatosSolicitud = JSONObject.parse(new String(data));		
		
		// Set parametros para notificacion
		String to = jsonDatosProveedor.get("contactoMailTo").toString();
		String cc = jsonDatosProveedor.get("contactoMailCc").toString();
		String alias = jsonSettings.get("emailAlias").toString();		
		String bcc = jsonSettings.get("emailBcc").toString();
		String subject = "Solicitud de Factura";
		List<String> templateNames = new ArrayList<String>();
		templateNames.add("contactoNombre");
		templateNames.add("empresaSolicitada");
		templateNames.add("numeroCopias");
		templateNames.add("razonSocial");
		templateNames.add("rfc");
		templateNames.add("direccionFiscal");
		templateNames.add("metodoPago");
		templateNames.add("numeroCuenta");
		templateNames.add("subTotal");
		templateNames.add("iva");
		templateNames.add("montoTotal");
		templateNames.add("folios");
		templateNames.add("conceptos");
		templateNames.add("observaciones");
		List<String> templateValues = new ArrayList<String>();
		templateValues.add(StringEscapeUtils.escapeHtml(jsonDatosProveedor.get("contactoNombre").toString()));
		//templateValues.add((!jsonDatosSolicitud.containsKey("empresaSolicitada") ? "" : jsonDatosSolicitud.get("empresaSolicitada").toString()));
		templateValues.add(jsonDatosSolicitud.get("nombreEmpresa") != null ? jsonDatosSolicitud.get("nombreEmpresa").toString(): "");
		templateValues.add((forceSingleCopy ? "1" : jsonDatosSolicitud.get("numeroCopias").toString()));
		templateValues.add(StringEscapeUtils.escapeHtml(jsonDatosSolicitud.get("razonSocial").toString()));
		templateValues.add(jsonDatosSolicitud.get("rfc").toString());
		templateValues.add(StringEscapeUtils.escapeHtml(jsonDatosSolicitud.get("direccionFiscal").toString()));
		templateValues.add(StringEscapeUtils.escapeHtml(jsonDatosSolicitud.containsKey("metodoPago") ? metodosPagoToString(JSONObject.parse(jsonDatosSolicitud.get("metodoPago").toString())) : ""));
		templateValues.add(StringEscapeUtils.escapeHtml(jsonDatosSolicitud.containsKey("numeroCuenta") ? jsonDatosSolicitud.get("numeroCuenta").toString() : ""));
		templateValues.add(df.format(jsonDatosSolicitud.get("subTotal")));
		templateValues.add(df.format(jsonDatosSolicitud.get("iva")));
		templateValues.add(df.format(jsonDatosSolicitud.get("montoTotal")));
		templateValues.add(folios.toString());
		StringBuffer conceptosBuffer = new StringBuffer();
		JSONArray jsonConceptos = (JSONArray) jsonDatosSolicitud.get("conceptos");
		for (Object obj : jsonConceptos) {
			JSONObject concepto = (JSONObject) obj;
			conceptosBuffer.append("<tr>");
			conceptosBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + StringEscapeUtils.escapeHtml(concepto.get("conceptoDescripcion").toString()) + "</td>");
			conceptosBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + StringEscapeUtils.escapeHtml(concepto.get("conceptoUnidad").toString()) + "</td>");
			conceptosBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + concepto.get("conceptoCantidad").toString() + "</td>");
			conceptosBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + df.format(concepto.get("conceptoPrecioUnitario")) + "</td>");
			conceptosBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + df.format(concepto.get("conceptoImporte")) + "</td>");
			conceptosBuffer.append("</tr>");			
		}
		templateValues.add(conceptosBuffer.toString());
		if (observaciones == null)
			observaciones = jsonDatosSolicitud.get("observaciones").toString();
		templateValues.add(StringEscapeUtils.escapeHtml(observaciones));	

		// Get plantilla de notificacion
		Document template = Factory.Document.fetchInstance(os, "/Plantillas/Notificacion Solicitud de Factura.html", null);			
		
		// Envia notificacion
		MailService mailService = new MailService(jsonSettings);
		mailService.sendTemplateMail(to, cc, bcc, alias, subject, template, templateNames, templateValues, new ArrayList<Document>());

	}
	
	private static String metodosPagoToString(JSONObject jsonObject) throws Exception {
		// convert to readable mode
		String value = "";
		for (Object obj : jsonObject.keySet()) {
			String key = (String) obj;
			value += jsonObject.get(key) + " (" + key + "), ";
		}
		if (value.length() > 0)
			value = value.substring(0, value.length() - 2);
		return value;
	}
	
	@SuppressWarnings("unchecked")
	private static void notificaEnvioFactura(ObjectStore os, Folder solicitud, Folder cliente, String observaciones) throws Exception {
		
		// Get copia oculta de configuracion general
		Document settings = Factory.Document.fetchInstance(os, "/Settings/SolDocSettings", null);
		byte[] data = settings.getProperties().getBinaryValue("ClbJSONData");		
		JSONObject jsonSettings = JSONObject.parse(new String(data));
		
		// Get datos cliente
    	data = cliente.getProperties().getBinaryValue("ClbJSONData");
    	JSONObject jsonDatosCliente = JSONObject.parse(new String(data));				
						
		// Get datos factura
    	com.filenet.api.property.Properties props = solicitud.getProperties();	
    	data = props.getBinaryValue("ClbJSONData");
    	JSONObject jsonDatosSolicitud = JSONObject.parse(new String(data));		
		
		// Set parametros para notificacion
		String to = jsonDatosCliente.get("contactoMailTo").toString();
		String cc = jsonDatosCliente.get("contactoMailCc").toString();
		String alias = jsonSettings.get("emailAlias").toString();		
		String bcc = jsonSettings.get("emailBcc").toString();
		String subject = "Envio de Factura";
		List<String> templateNames = new ArrayList<String>();
		templateNames.add("contactoNombre");
		templateNames.add("numeroFactura");
		templateNames.add("fechaFactura");
		templateNames.add("montoTotal");
		templateNames.add("folio");
		templateNames.add("observaciones");
		List<String> templateValues = new ArrayList<String>();
		templateValues.add(StringEscapeUtils.escapeHtml(jsonDatosCliente.get("contactoNombre").toString()));
		templateValues.add(jsonDatosSolicitud.get("numeroFactura").toString());
		templateValues.add(jsonDatosSolicitud.get("fechaFactura").toString());
		templateValues.add(df.format(jsonDatosSolicitud.get("montoTotal")));
		templateValues.add(solicitud.get_FolderName());
		templateValues.add(StringEscapeUtils.escapeHtml(observaciones));	
		
		// Get plantilla de notificacion
		Document template = Factory.Document.fetchInstance(os, "/Plantillas/Notificacion Envio de Factura", null);			
		
		// Se anexa el CFDI (PDF y XML)  como attachment		
		List<Document> atts = new ArrayList<Document>();
		DocumentSet docSet = solicitud.get_ContainedDocuments();
		for (Iterator<Document> it = docSet.iterator(); it.hasNext();) {
			Document doc = it.next();
			if (doc.getClassName().equals("SolDocCFDI"))
				atts.add(doc);				
		}		
		
		// Envia notificacion
		MailService mailService = new MailService(jsonSettings);
		mailService.sendTemplateMail(to, cc, bcc, alias, subject, template, templateNames, templateValues, atts);
	}	
	
	private static Folder getYearMonthParent(ObjectStore objStore, Date dtVal, Folder root) throws Exception {
		Folder anioFolder = null;
		Folder mesFolder = null;
		String[] meses = new String[]{"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
		Calendar cal = Calendar.getInstance();
		cal.setTime(dtVal);
		String anio = Integer.toString(cal.get(Calendar.YEAR));
		String mes = meses[cal.get(Calendar.MONTH)];
		
		try {
			anioFolder = Factory.Folder.fetchInstance(objStore, root.get_PathName() + "/" + anio, null);						
		} catch (EngineRuntimeException ere) {}	
		
		if (anioFolder == null) {
			// Crea anio folder
			anioFolder = ServiceUtil.createFolder(objStore, "SolDocEstructura", anio, root);		
		}
		
		try {
			mesFolder = Factory.Folder.fetchInstance(objStore, anioFolder.get_PathName() + "/" +  mes, null);						
		} catch (EngineRuntimeException ere) {}			
		
		if (mesFolder == null) {
			// Crea mes folder
			mesFolder = ServiceUtil.createFolder(objStore, "SolDocEstructura", mes, anioFolder);
		}
		
		return mesFolder;
	}	
	
}
