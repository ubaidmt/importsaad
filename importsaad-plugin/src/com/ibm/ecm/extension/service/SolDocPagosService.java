package com.ibm.ecm.extension.service;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.Map;
import java.util.HashMap;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;

import com.filenet.api.admin.Choice;
import com.filenet.api.admin.ClassDefinition;
import com.filenet.api.admin.PropertyDefinition;
import com.filenet.api.collection.ContentElementList;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.collection.PropertyDefinitionList;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.FilteredPropertyType;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.VersionSeries;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.Document;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.property.PropertyFilter;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.ecm.extension.service.mail.MailService;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class SolDocPagosService {
	
	private static final String stanza = "FileNetP8";
	private final static SimpleDateFormat longDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	private final static DecimalFormat df = new DecimalFormat("$#,##0.00");
	private final static String devolucionesReadOnlyPolicy = "{2080EE0E-21AE-4D83-847C-F6E8E1CDF364}";
	private final static String pagoslReadOnlyPolicy = "{A629E2CF-F9C7-43F9-89F3-19D017314E55}";
	
	@SuppressWarnings("unchecked")
	public static JSONObject getEmpresas(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		ObjectStore objStore = null;
		JSONArray empresas = new JSONArray();

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			JSONObject jsonCliente = JSONObject.parse(request.getParameter("cliente"));
			int estado = Integer.parseInt(request.getParameter("estado"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}
			
			// Get folder cliente
			Folder cliente = null;
			try {
				cliente = Factory.Folder.fetchInstance(objStore, jsonCliente.get("id").toString(), null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El cliente seleccionado no fue localizado.");		
			}			
			
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("DISTINCT Empresa");
		    sql.setFromClauseInitialValue("SolDocCase", null, false);	    
		    sql.setWhereClause("This INSUBFOLDER '" + cliente.get_PathName() + "' AND Empresa IS NOT NULL AND EstadoCFDI = " + estado);

		    // performance settings
		    JSONObject jsonSettings = SolDocService.getSettingsObject(objStore);
			sql.setTimeLimit(Integer.parseInt(jsonSettings.get("timeLimit").toString()));		  
		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	JSONObject jsonData = new JSONObject();
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties properties = row.getProperties();
		    	Folder empresa = (Folder) properties.getEngineObjectValue("Empresa");
		    	jsonData.put("id", empresa.get_Id().toString());
		    	jsonData.put("label", empresa.get_FolderName());
		    	empresas.add(jsonData);
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
    	jsonResponse.put("empresas", empresas);
		return jsonResponse;		
		
	}	
	
	@SuppressWarnings("unchecked")
	public static JSONObject getClientesDevolucion(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		ObjectStore objStore = null;
		JSONArray clientes = new JSONArray();

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			int tipo = Integer.parseInt(request.getParameter("tipo"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}		
			
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("FoldersFiledIn");
		    sql.setFromClauseInitialValue("SolDocDevolucion", null, false);
		    
		    StringBuffer whereStatement = new StringBuffer();
		    whereStatement.append("This INSUBFOLDER '/Facturas' AND Pendiente = TRUE AND isCurrentVersion = TRUE AND TipoDevolucion = " + tipo);
		    
		    if (tipo == 0) { // devolucion del proveedor
		    	JSONObject jsonProveedor = JSONObject.parse(request.getParameter("proveedor"));
		    	whereStatement.append(" AND Proveedor = " + jsonProveedor.get("id").toString());
		    }    
		    sql.setWhereClause(whereStatement.toString());

		    // performance settings
		    JSONObject jsonSettings = SolDocService.getSettingsObject(objStore);
			sql.setMaxRecords(Integer.parseInt(jsonSettings.get("maxRecords").toString()));
			sql.setTimeLimit(Integer.parseInt(jsonSettings.get("timeLimit").toString()));			
		    		    
		    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, 50, null, true);

		    for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
		    {
		    	Document devolucion = it.next();
		    	FolderSet folSet = devolucion.get_FoldersFiledIn();
		    	for (Iterator<Folder> it2 = folSet.iterator(); it2.hasNext(); ) 
		    	{
		    		JSONObject jsonData = new JSONObject();
		    		Folder cliente = getCliente(it2.next());
			    	jsonData.put("id", cliente.get_Id().toString());
			    	jsonData.put("label", cliente.get_FolderName());
		    		if (!clientes.contains(jsonData))
		    			clientes.add(jsonData);
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
    	jsonResponse.put("clientes", clientes);
		return jsonResponse;		
		
	}		
	
	@SuppressWarnings("unchecked")
	public static JSONObject getSaldoFacturas(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		ObjectStore objStore = null;
		double saldo = 0;

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			JSONObject jsonCliente = JSONObject.parse(request.getParameter("cliente"));
			int estado = Integer.parseInt(request.getParameter("estado"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}
			
			// Get folder cliente
			Folder cliente = null;
			try {
				cliente = Factory.Folder.fetchInstance(objStore, jsonCliente.get("id").toString(), null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El cliente seleccionado no fue localizado.");		
			}			
			
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("MontoTotal");
		    sql.setFromClauseInitialValue("SolDocCase", null, false);	    
		    sql.setWhereClause("This INSUBFOLDER '" + cliente.get_PathName() + "' AND EstadoCFDI = " + estado);

		    // performance settings
		    JSONObject jsonSettings = SolDocService.getSettingsObject(objStore);
			sql.setMaxRecords(Integer.parseInt(jsonSettings.get("maxRecords").toString()));
			sql.setTimeLimit(Integer.parseInt(jsonSettings.get("timeLimit").toString()));		    
		    		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties properties = row.getProperties();
		    	saldo += properties.getFloat64Value("MontoTotal");
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
    	jsonResponse.put("saldo", saldo);
		return jsonResponse;		
		
	}	
	
	@SuppressWarnings("unchecked")
	public static JSONObject getSaldoProveedor(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		ObjectStore objStore = null;
		double saldo = 0;
		double saldo1 = 0;
		double saldo2 = 0;

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			JSONObject jsonProveedor = JSONObject.parse(request.getParameter("proveedor"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}		
			
			// Get devoluciones pendientes
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, FoldersFiledIn");
		    sql.setFromClauseInitialValue("SolDocDevolucion", null, false);	    
		    sql.setWhereClause("This INSUBFOLDER '/Facturas' AND Proveedor = " + jsonProveedor.get("id").toString() + " AND TipoDevolucion = 0 AND isCurrentVersion = TRUE AND Pendiente = TRUE");

		    // performance settings
		    JSONObject jsonSettings = SolDocService.getSettingsObject(objStore);
			sql.setMaxRecords(Integer.parseInt(jsonSettings.get("maxRecords").toString()));
			sql.setTimeLimit(Integer.parseInt(jsonSettings.get("timeLimit").toString()));		    
		    		    
		    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, 50, null, true);
		    
		    for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
		    {
		    	Document doc = it.next();
		    	JSONObject jsonDevolucion = getDatosDevolucionProveedor(objStore, doc);
		    	saldo1 += Double.parseDouble(jsonDevolucion.get("devolucionSolicitadaProveedor").toString());
		    }
		    
		    // Get devoluciones pagadas con saldo pendiente
		    search = new SearchScope(objStore);
		    sql = new SearchSQL();
		    sql.setSelectList("Id, Saldo");
		    sql.setFromClauseInitialValue("SolDocDevolucion", null, false);		    
		    sql.setWhereClause("This INSUBFOLDER '/Facturas' AND Proveedor = " + jsonProveedor.get("id").toString() + " AND TipoDevolucion = 0 AND isCurrentVersion = TRUE AND Pendiente = FALSE AND Saldo <> 0");

		    // performance settings
			sql.setMaxRecords(Integer.parseInt(jsonSettings.get("maxRecords").toString()));
			sql.setTimeLimit(Integer.parseInt(jsonSettings.get("timeLimit").toString()));		    
		    		    
		    docSet = (DocumentSet) search.fetchObjects(sql, 50, null, true);
		    
		    for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
		    {
		    	Document doc = it.next();
		    	saldo2 += doc.getProperties().getFloat64Value("Saldo");
		    }
		    
			saldo = saldo1 + saldo2;	    
			
		} catch (Exception e) {
			error = e.getMessage();
			e.printStackTrace();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
    	jsonResponse.put("saldo", saldo);
		return jsonResponse;		
		
	}	
	
	@SuppressWarnings("unchecked")
	public static JSONObject getSaldoCliente(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		ObjectStore objStore = null;
		double saldo = 0;
		double saldo1 = 0;
		double saldo2 = 0;

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			JSONObject jsonCliente = JSONObject.parse(request.getParameter("cliente"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}	
			
			// Get folder cliente
			Folder cliente = null;
			try {
				cliente = Factory.Folder.fetchInstance(objStore, jsonCliente.get("id").toString(), null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El cliente seleccionado no fue localizado.");		
			}				
			
			// Get Saldo Devoluciones Pendientes
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, FoldersFiledIn");
		    sql.setFromClauseInitialValue("SolDocDevolucion", null, false);		    
		    sql.setWhereClause("This INSUBFOLDER '" + cliente.get_PathName() + "' AND TipoDevolucion = 1 AND Pendiente = TRUE AND isCurrentVersion = TRUE");

		    // performance settings
		    JSONObject jsonSettings = SolDocService.getSettingsObject(objStore);
			sql.setMaxRecords(Integer.parseInt(jsonSettings.get("maxRecords").toString()));
			sql.setTimeLimit(Integer.parseInt(jsonSettings.get("timeLimit").toString()));		    
		    		    
		    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, 50, null, true);
		    
		    for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
		    {
		    	Document doc = it.next();
		    	JSONObject jsonDevolucion = getDatosDevolucionCliente(objStore, doc);
		    	saldo1 += Double.parseDouble(jsonDevolucion.get("devolucionSolicitadaDistribuidor").toString());
		    }
		    
		    // Get devoluciones pagadas con saldo pendiente
		    search = new SearchScope(objStore);
		    sql = new SearchSQL();
		    sql.setSelectList("Id, FoldersFiledIn, Saldo");
		    sql.setFromClauseInitialValue("SolDocDevolucion", null, false);	    
		    sql.setWhereClause("This INSUBFOLDER '" + cliente.get_PathName() + "' AND TipoDevolucion = 1 AND isCurrentVersion = TRUE AND Pendiente = FALSE AND Saldo <> 0");

		    // performance settings
			sql.setMaxRecords(Integer.parseInt(jsonSettings.get("maxRecords").toString()));
			sql.setTimeLimit(Integer.parseInt(jsonSettings.get("timeLimit").toString()));		    
		    		    
		    docSet = (DocumentSet) search.fetchObjects(sql, 50, null, true);
		    
		    for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
		    {
		    	Document doc = it.next();
		    	saldo2 += doc.getProperties().getFloat64Value("Saldo");
		    }
		    
		    saldo = saldo1 + saldo2;		    
			
		} catch (Exception e) {
			error = e.getMessage();
			e.printStackTrace();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
    	jsonResponse.put("saldo", saldo);
		return jsonResponse;		
		
	}			
	
	@SuppressWarnings("unchecked")
	public static JSONObject searchFacturas(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		ObjectStore objStore = null;
		JSONArray facturas = new JSONArray();

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			JSONObject jsonCliente = JSONObject.parse(request.getParameter("cliente"));
			JSONObject jsonEmpresa = JSONObject.parse(request.getParameter("empresa"));
			int estado = Integer.parseInt(request.getParameter("estado"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}
			
			// Get folder cliente
			Folder cliente = null;
			try {
				cliente = Factory.Folder.fetchInstance(objStore, jsonCliente.get("id").toString(), null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El cliente seleccionado no fue localizado.");		
			}			
			
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, FolderName, MontoTotal, FechaFactura, NumeroFactura, ClbJSONData");
		    sql.setFromClauseInitialValue("SolDocCase", null, false);	    
		    sql.setWhereClause("This INSUBFOLDER '" + cliente.get_PathName() + "' AND Empresa = " + jsonEmpresa.get("id").toString() + " AND EstadoCFDI = " + estado);

		    // performance settings
		    JSONObject jsonSettings = SolDocService.getSettingsObject(objStore);
			sql.setMaxRecords(Integer.parseInt(jsonSettings.get("maxRecords").toString()));
			sql.setTimeLimit(Integer.parseInt(jsonSettings.get("timeLimit").toString()));		    
		    		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	JSONObject jsonFactura = new JSONObject();
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties properties = row.getProperties();
		    	jsonFactura.put("id", properties.getIdValue("id").toString());
		    	jsonFactura.put("folio", properties.getStringValue("FolderName"));
		    	jsonFactura.put("fechaFactura", (properties.getObjectValue("FechaFactura") == null ? null : sdf.format(getUTFCalendar(properties.getDateTimeValue("FechaFactura")).getTime())));
		    	jsonFactura.put("numeroFactura", properties.getStringValue("NumeroFactura"));
		    	jsonFactura.put("importe", properties.getFloat64Value("MontoTotal"));
		    	
		    	byte[] data = properties.getBinaryValue("ClbJSONData");
		    	JSONObject jsonData = JSONObject.parse(new String(data));		    	
		    	jsonFactura.put("porcentajeComisionProveedor", jsonData.get("porcentajeComisionProveedor"));
		    	jsonFactura.put("montoComisionProveedor", jsonData.get("montoComisionProveedor"));
		    	jsonFactura.put("porcentajeComisionDistribuidor", jsonData.get("porcentajeComisionDistribuidor"));
		    	jsonFactura.put("montoComisionDistribuidor", jsonData.get("montoComisionDistribuidor"));		    	
		    	
		    	facturas.add(jsonFactura);
		    }
			
		} catch (Exception e) {
			facturas = new JSONArray();
			error = e.getMessage();
			e.printStackTrace();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
    	jsonResponse.put("facturas", facturas);
		return jsonResponse;		
		
	}		
	
	@SuppressWarnings("unchecked")
	public static JSONObject searchDevoluciones(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		ObjectStore objStore = null;
		JSONArray devoluciones = new JSONArray();
		JSONObject jsonDevolucion = new JSONObject();

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			int tipo = Integer.parseInt(request.getParameter("tipo"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}
			
			Folder cliente = null;
									
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, DateCreated, FoldersFiledIn");
		    sql.setFromClauseInitialValue("SolDocDevolucion", null, false);
		    StringBuffer whereStatement = new StringBuffer();
		    whereStatement.append("Pendiente = TRUE AND isCurrentVersion = TRUE AND TipoDevolucion = " + tipo);
			if (request.getParameter("proveedor") != null) {
				JSONObject jsonProveedor = JSONObject.parse(request.getParameter("proveedor"));
				whereStatement.append(" AND Proveedor = " + jsonProveedor.get("id").toString());
			}
		    if (request.getParameter("cliente") != null) {
		    	JSONObject jsonCliente = JSONObject.parse(request.getParameter("cliente"));
				try {
					cliente = Factory.Folder.fetchInstance(objStore, jsonCliente.get("id").toString(), null);	
				} catch (EngineRuntimeException ere) {
					throw new RuntimeException ("El cliente seleccionado no fue localizado.");		
				}			    	
		    	whereStatement.append(" AND This INSUBFOLDER '" + cliente.get_PathName() + "'");
		    } else {
		    	whereStatement.append(" AND This INSUBFOLDER '/Facturas'");
		    }		    
		    sql.setWhereClause(whereStatement.toString());

		    // performance settings
		    JSONObject jsonSettings = SolDocService.getSettingsObject(objStore);
			sql.setMaxRecords(Integer.parseInt(jsonSettings.get("maxRecords").toString()));
			sql.setTimeLimit(Integer.parseInt(jsonSettings.get("timeLimit").toString()));		    
		    		    
		    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, 50, null, true);
		    
		    for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
		    {
		    	JSONObject jsonData = new JSONObject();
		    	Document devolucion = it.next();
		    	switch (tipo) {
		    		case 0: // devolucion del proveedor
		    			jsonDevolucion = getDatosDevolucionProveedor(objStore, devolucion);
		    			break;
		    		case 1: // devolucion para el cliente
		    			jsonDevolucion = getDatosDevolucionCliente(objStore, devolucion);
		    			break;		    			
		    	}
				JSONArray jsonFacturas = (JSONArray) jsonDevolucion.get("facturas");		    	
				Folder firstFactura = Factory.Folder.fetchInstance(objStore, ((JSONObject) jsonFacturas.get(0)).get("id").toString(), null);
				com.filenet.api.property.Properties props = firstFactura.getProperties();
				cliente = getCliente(firstFactura);
				jsonData.put("id", devolucion.get_Id().toString());
				jsonData.put("proveedor", ((Folder) props.getEngineObjectValue("Proveedor")).get_FolderName());
		    	jsonData.put("empresa", ((Folder) props.getEngineObjectValue("Empresa")).get_FolderName());
		    	jsonData.put("cliente", cliente.get_FolderName());
		    	jsonData.put("fechaSolicitud", sdf.format(getUTFCalendar(props.getDateTimeValue("DateCreated")).getTime()));
		    	jsonData.put("devolucionSolicitadaProveedor", jsonDevolucion.get("devolucionSolicitadaProveedor"));
		    	jsonData.put("devolucionSolicitadaDistribuidor", jsonDevolucion.get("devolucionSolicitadaDistribuidor"));
		    	byte[] data = cliente.getProperties().getBinaryValue("ClbJSONData");
		    	JSONObject jsonCliente = JSONObject.parse(new String(data));
		    	jsonData.put("devolucionDirecta", (jsonCliente.containsKey("devolucionDirectaProveedor") ? Boolean.parseBoolean(jsonCliente.get("devolucionDirectaProveedor").toString()) : false));
		    	jsonData.put("datos", jsonDevolucion.toString());
		    	devoluciones.add(jsonData);
		    }
			
		} catch (Exception e) {
			devoluciones = new JSONArray();
			error = e.getMessage();
			e.printStackTrace();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
    	jsonResponse.put("devoluciones", devoluciones);
		return jsonResponse;		
		
	}	
	
	@SuppressWarnings("unchecked")
	public static JSONObject searchDevolucionesSaldoPendiente(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		ObjectStore objStore = null;
		JSONArray devoluciones = new JSONArray();
		JSONObject jsonDevolucion = new JSONObject();

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			int tipo = Integer.parseInt(request.getParameter("tipo"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}
			
			Folder cliente = null;
									
		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, DateCreated, Saldo, FoldersFiledIn");
		    sql.setFromClauseInitialValue("SolDocDevolucion", null, false);
		    StringBuffer whereStatement = new StringBuffer();
		    whereStatement.append("Saldo <> 0 AND isCurrentVersion = TRUE AND TipoDevolucion = " + tipo);
			if (request.getParameter("proveedor") != null) {
				JSONObject jsonProveedor = JSONObject.parse(request.getParameter("proveedor"));
				whereStatement.append(" AND Proveedor = " + jsonProveedor.get("id").toString());
			}
		    if (request.getParameter("cliente") != null) {
		    	JSONObject jsonCliente = JSONObject.parse(request.getParameter("cliente"));
				try {
					cliente = Factory.Folder.fetchInstance(objStore, jsonCliente.get("id").toString(), null);	
				} catch (EngineRuntimeException ere) {
					throw new RuntimeException ("El cliente seleccionado no fue localizado.");		
				}			    	
		    	whereStatement.append(" AND This INSUBFOLDER '" + cliente.get_PathName() + "'");
		    } else {
		    	whereStatement.append(" AND This INSUBFOLDER '/Facturas'");
		    }
			if (request.getParameter("empresa") != null) {
				JSONObject jsonEmpresa = JSONObject.parse(request.getParameter("empresa"));
				whereStatement.append(" AND Empresa = " + jsonEmpresa.get("id").toString());
			}	
			if (request.getParameter("fechaSolicitudDesde") != null)
				whereStatement.append(" AND DateCreated >= " + convertLocalTimeToUTC(longDateFormat.parse(request.getParameter("fechaSolicitudDesde").toString() + " 00:00:00")));
			if (request.getParameter("fechaSolicitudHasta") != null)
				whereStatement.append(" AND DateCreated <= " + convertLocalTimeToUTC(longDateFormat.parse(request.getParameter("fechaSolicitudHasta").toString() + " 00:00:00")));
				
		    sql.setWhereClause(whereStatement.toString());

		    // performance settings
		    JSONObject jsonSettings = SolDocService.getSettingsObject(objStore);
			sql.setMaxRecords(Integer.parseInt(jsonSettings.get("maxRecords").toString()));
			sql.setTimeLimit(Integer.parseInt(jsonSettings.get("timeLimit").toString()));		    
		    		    
		    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, 50, null, true);
		    
		    for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
		    {
		    	JSONObject jsonData = new JSONObject();
		    	Document devolucion = it.next();
		    	switch (tipo) {
		    		case 0: // devolucion del proveedor
		    			jsonDevolucion = getDatosDevolucionProveedor(objStore, devolucion);
		    			break;
		    		case 1: // devolucion para el cliente
		    			jsonDevolucion = getDatosDevolucionCliente(objStore, devolucion);
		    			break;		    			
		    	}
				JSONArray jsonFacturas = (JSONArray) jsonDevolucion.get("facturas");		    	
				Folder firstFactura = Factory.Folder.fetchInstance(objStore, ((JSONObject) jsonFacturas.get(0)).get("id").toString(), null);
				com.filenet.api.property.Properties props = firstFactura.getProperties();
				jsonData.put("id", devolucion.get_Id().toString());
				jsonData.put("proveedor", ((Folder) props.getEngineObjectValue("Proveedor")).get_FolderName());
		    	jsonData.put("empresa", ((Folder) props.getEngineObjectValue("Empresa")).get_FolderName());
		    	jsonData.put("cliente", getCliente(firstFactura).get_FolderName());
		    	jsonData.put("fechaSolicitud", sdf.format(getUTFCalendar(props.getDateTimeValue("DateCreated")).getTime()));
		    	jsonData.put("devolucionSolicitadaProveedor", jsonDevolucion.get("devolucionSolicitadaProveedor"));
		    	jsonData.put("devolucionSolicitadaDistribuidor", jsonDevolucion.get("devolucionSolicitadaDistribuidor"));
		    	jsonData.put("saldo", devolucion.getProperties().getFloat64Value("Saldo"));
		    	jsonData.put("datos", jsonDevolucion.toString());
		    	devoluciones.add(jsonData);
		    }
			
		} catch (Exception e) {
			devoluciones = new JSONArray();
			error = e.getMessage();
			e.printStackTrace();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
    	jsonResponse.put("devoluciones", devoluciones);
		return jsonResponse;		
		
	}	
	
	public static JSONObject savePagosDeCliente(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;	
		ObjectStore objStore = null;

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			JSONArray jsonFacturas = JSONArray.parse(request.getParameter("facturas"));
			JSONObject jsonPago = JSONObject.parse(request.getParameter("pago"));
			JSONObject datos = JSONObject.parse(request.getParameter("datos"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}
			
			// Get temp folder
			Folder temp = null;
			try {
				temp = Factory.Folder.fetchInstance(objStore, "/Temporal/Documentos", null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El folder temporal no fue localizado.");		
			}			
						
			// Add pago
			Document pago = null;
			if (jsonPago.get("comprobantePago") != null) {
				pago = Factory.Document.fetchInstance(objStore, ((JSONObject)jsonPago.get("comprobantePago")).get("id").toString(), null);
				pago.changeClass("SolDocPago");
				pago.save(RefreshMode.REFRESH);					
			} else {
				pago = Factory.Document.createInstance(callbacks.getP8ObjectStore(repositoryId), "SolDocPago");
				pago.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
			}				
			
			com.filenet.api.property.Properties props = pago.getProperties();
			props.putObjectValue("DocumentTitle", "Pago de Cliente");
			props.putObjectValue("MontoTotal", Double.parseDouble(jsonPago.get("importe").toString()));
			props.putObjectValue("FechaPago", getUTFCalendar(sdf.parse(jsonPago.get("fechaPago").toString())).getTime());
			props.putObjectValue("MetodoPago", Integer.parseInt(jsonPago.get("metodoPago").toString()));
			props.putObjectValue("Referencia", jsonPago.get("referencia").toString());
			props.putObjectValue("TipoPago", 0); // pago de cliente 
			if (jsonPago.containsKey("banco"))
				props.putObjectValue("Banco", Integer.parseInt(jsonPago.get("banco").toString()));
			else
				props.putObjectValue("Banco", null);			
					
			pago.applySecurityTemplate(new Id(pagoslReadOnlyPolicy));
			pago.save(RefreshMode.REFRESH);
			
			// Unfile comprobante de pago de temporal
			if (jsonPago.get("comprobantePago") != null) {
				ReferentialContainmentRelationship rel = temp.unfile(pago);
				rel.save(RefreshMode.NO_REFRESH);				
			}
			
			// Crea Solicitud de Devolucion de Proveedor
			Document devolucionProveedor = Factory.Document.createInstance(objStore, "SolDocDevolucion");
			devolucionProveedor.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
			props = devolucionProveedor.getProperties();
			props.putObjectValue("DocumentTitle", "Solicitud de Devolución al Proveedor");
			devolucionProveedor.save(RefreshMode.REFRESH);			
			
			// Asociacion de pagos con facturas
			for (Object objFactura : jsonFacturas)
			{
				JSONObject jsonFactura = (JSONObject) objFactura;
				
				// Get factura
				Folder factura = null;
				try {
					factura = Factory.Folder.fetchInstance(objStore, jsonFactura.get("id").toString(), null);
				} catch (EngineRuntimeException ere) {
					throw new RuntimeException ("La factura no pudo ser localizada.");		
				}
				
				// File Pago
				ReferentialContainmentRelationship rel = factura.file(pago, AutoUniqueName.AUTO_UNIQUE, pago.get_Name(), DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
				rel.save(RefreshMode.NO_REFRESH);					
				
				// File Devolucion Proveedor
				rel = factura.file(devolucionProveedor, AutoUniqueName.AUTO_UNIQUE, devolucionProveedor.get_Name(), DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
				rel.save(RefreshMode.NO_REFRESH);					
				
				props = factura.getProperties();
				props.putObjectValue("EstadoCFDI", 1); // Pagada por Cliente
				factura.save(RefreshMode.REFRESH);
			}
			
			// Actualiza Solicitud Devolucion Proveedor Properties
			props = devolucionProveedor.getProperties();	
			JSONObject jsonDevolucion = getDatosDevolucionProveedor(objStore, devolucionProveedor);
			jsonFacturas = (JSONArray) jsonDevolucion.get("facturas");
			Folder firstFactura = Factory.Folder.fetchInstance(objStore, ((JSONObject) jsonFacturas.get(0)).get("id").toString(), null);		
			props.putObjectValue("Proveedor", firstFactura.getProperties().getEngineObjectValue("Proveedor"));
			props.putObjectValue("Empresa", firstFactura.getProperties().getEngineObjectValue("Empresa"));
			props.putObjectValue("MontoTotal", Double.parseDouble(jsonDevolucion.get("devolucionSolicitadaProveedor").toString()));
			props.putObjectValue("Pendiente", true);
			props.putObjectValue("TipoDevolucion", 0); // Devolucion Proveedor
			devolucionProveedor.applySecurityTemplate(new Id(devolucionesReadOnlyPolicy));
			devolucionProveedor.save(RefreshMode.REFRESH);
			
			// Props Adicionales Pago
			props = pago.getProperties();
			props.putObjectValue("Proveedor", firstFactura.getProperties().getEngineObjectValue("Proveedor"));
			props.putObjectValue("Empresa", firstFactura.getProperties().getEngineObjectValue("Empresa"));
			pago.save(RefreshMode.REFRESH);			

			// Envia notificacion de solicitud de devolucion a proveedor
			if (!(Boolean)datos.get("omitirEnvioCorreo")) {
				try 
				{
					notificaSolicitudDevolucion(objStore, devolucionProveedor, datos.get("observaciones").toString());
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
		return jsonResponse;		
		
	}	
	
	@SuppressWarnings("unchecked")
	public static JSONObject savePagosDeProveedor(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;	
		ObjectStore objStore = null;
		Map<Id, Document> pagosClonadosMap = new HashMap<Id, Document>();
		List<Document> pagosProcesados = new ArrayList<Document>();

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			JSONArray jsonPagos = JSONArray.parse(request.getParameter("pagos"));
			JSONArray jsonDevolucionesSaldoPendiente = JSONArray.parse(request.getParameter("devolucionesSaldoPendiente"));	
			int tipoTransaccion = Integer.parseInt(request.getParameter("tipo"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}	
			
			// Get temp folder
			Folder temp = null;
			try {
				temp = Factory.Folder.fetchInstance(objStore, "/Temporal/Documentos", null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El folder temporal no fue localizado.");		
			}
			
			switch (tipoTransaccion) {
			
				case 0: // registro de devolucion de proveedor
				
					JSONObject devolucion = JSONObject.parse(request.getParameter("devolucion"));
					JSONObject jsonDevolucionDirecta = JSONObject.parse(request.getParameter("devolucionDirecta"));						
					
					// Get solicitud de devolucion del proveedor
					Document devolucionProveedor = null;
					try {
						devolucionProveedor = Factory.Document.fetchInstance(objStore, devolucion.get("id").toString(), null);
					} catch (EngineRuntimeException ere) {
						throw new RuntimeException ("La devolucion del proveedor no pudo ser localizada.");		
					}	
					
					com.filenet.api.property.Properties props;

					// Crea Solicitud de Devolucion a Cliente
					Document devolucionCliente = Factory.Document.createInstance(objStore, "SolDocDevolucion");
					devolucionCliente.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
					props = devolucionCliente.getProperties();
					props.putObjectValue("DocumentTitle", "Solicitud de Devolución para el Cliente");
					devolucionCliente.save(RefreshMode.REFRESH);
								
					// Facturas donde se encuentra actualmente asociada la solicitud de devolucion del proveedor
					FolderSet folSet = devolucionProveedor.get_FoldersFiledIn();
					for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) 
					{
						Folder factura = it.next();

						// Asociacion de pagos a factura
						for (Object obj : jsonPagos)
						{
							JSONObject jsonPago = (JSONObject) obj;
							
							// Get pago
							Document pago = null;
							try {
								pago = Factory.Document.fetchInstance(objStore, jsonPago.get("id").toString(), null);
							} catch (EngineRuntimeException ere) {
								throw new RuntimeException ("El pago no pudo ser localizado.");		
							}
							
							if (!pagosProcesados.contains(pago)) {	
								// Props Adicionales Pago
								props = pago.getProperties();
								props.putObjectValue("Proveedor", factura.getProperties().getEngineObjectValue("Proveedor"));
								props.putObjectValue("Empresa", factura.getProperties().getEngineObjectValue("Empresa"));
								pago.save(RefreshMode.REFRESH);						
								
								// Unfile pago de temporal
								ReferentialContainmentRelationship rel = temp.unfile(pago);
								rel.save(RefreshMode.NO_REFRESH);		
							}
							
							// File Pago
							ReferentialContainmentRelationship rel = factura.file(pago, AutoUniqueName.AUTO_UNIQUE, pago.get_Name(), DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
							rel.save(RefreshMode.NO_REFRESH);
							
							// Set Pago Procesado
							pagosProcesados.add(pago);

							// Si es devolucion directa, clona pago a cliente
							if (Boolean.parseBoolean(jsonDevolucionDirecta.get("aplica").toString())) {
								Document pagoClonado = null;
								if (pagosClonadosMap.containsKey(pago.get_Id())) {
									pagoClonado = pagosClonadosMap.get(pago.get_Id());
								} else {
									Map<String, Object> propsMap = new HashMap<String, Object>();
									pago = Factory.Document.fetchInstance(objStore, jsonPago.get("id").toString(), null);
									props = pago.getProperties();
									propsMap.put("MontoTotal", props.getObjectValue("MontoTotal"));
									propsMap.put("FechaPago", props.getObjectValue("FechaPago"));
									propsMap.put("MetodoPago", props.getObjectValue("MetodoPago"));
									propsMap.put("Referencia", props.getObjectValue("Referencia"));
									propsMap.put("TipoPago", 2); // Pago a Cliente
									propsMap.put("Banco", props.getObjectValue("Banco"));
									propsMap.put("Proveedor", props.getObjectValue("Proveedor"));
									propsMap.put("Empresa", props.getObjectValue("Empresa"));					
									pagoClonado = cloneDocument(objStore, pago.getClassName(), pago, "Pago a Cliente", propsMap, pagoslReadOnlyPolicy);
									pagosClonadosMap.put(pago.get_Id(), pagoClonado);
								}
								// File Pago Clonado
								rel = factura.file(pagoClonado, AutoUniqueName.AUTO_UNIQUE, pagoClonado.get_Name(), DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
								rel.save(RefreshMode.NO_REFRESH);						
							}
			
						}	
						
						// File Devolucion Cliente
						ReferentialContainmentRelationship rel = factura.file(devolucionCliente, AutoUniqueName.AUTO_UNIQUE, devolucionCliente.get_Name(), DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
						rel.save(RefreshMode.NO_REFRESH);
						
						props = factura.getProperties();
						if (Boolean.parseBoolean(jsonDevolucionDirecta.get("aplica").toString()))
							props.putObjectValue("EstadoCFDI", 3); // Devualta a Cliente
						else
							props.putObjectValue("EstadoCFDI", 2); // Devuelta por Proveedor
						factura.save(RefreshMode.REFRESH);				
					}
					
					// Actualiza Estatus Devolucion Proveedor
					props = devolucionProveedor.getProperties();	
					props.putObjectValue("Pendiente", false);
					props.putObjectValue("Saldo", Double.parseDouble(request.getParameter("saldoProveedor")));
					devolucionProveedor.save(RefreshMode.REFRESH);
					
					// Actualiza Solicitud Devolucion Cliente Properties
					props = devolucionCliente.getProperties();
					JSONObject jsonDevolucion = getDatosDevolucionCliente(objStore, devolucionCliente);
					JSONArray jsonFacturas = (JSONArray) jsonDevolucion.get("facturas");
					Folder firstFactura = Factory.Folder.fetchInstance(objStore, ((JSONObject) jsonFacturas.get(0)).get("id").toString(), null);		
					props.putObjectValue("Proveedor", firstFactura.getProperties().getEngineObjectValue("Proveedor"));
					props.putObjectValue("Empresa", firstFactura.getProperties().getEngineObjectValue("Empresa"));
					props.putObjectValue("MontoTotal", Double.parseDouble(jsonDevolucion.get("devolucionSolicitadaDistribuidor").toString()));
					if (Boolean.parseBoolean(jsonDevolucionDirecta.get("aplica").toString())) {
						props.putObjectValue("Pendiente", false);
						props.putObjectValue("Saldo", Double.parseDouble(request.getParameter("saldoDistribuidor")));
					} else {
						props.putObjectValue("Pendiente", true);
					}
					props.putObjectValue("TipoDevolucion", 1); // Devolucion Cliente
					devolucionCliente.applySecurityTemplate(new Id(devolucionesReadOnlyPolicy));
					devolucionCliente.save(RefreshMode.REFRESH);
					
					// Actualiza Devoluciones de Proveedores con Saldo Pendiente Aplicadas
					for (Object obj : jsonDevolucionesSaldoPendiente)
					{
						JSONObject jsonDevolucionSaldoPendiente = (JSONObject) obj;
						
						// Get devolucion con saldo pendiente
						Document devolucionSaldoPendiente = null;
						try {
							devolucionSaldoPendiente = Factory.Document.fetchInstance(objStore, jsonDevolucionSaldoPendiente.get("id").toString(), null);
						} catch (EngineRuntimeException ere) {
							throw new RuntimeException ("La devolucion con saldo pendiente no pudo ser localizado.");		
						}
						
						props = devolucionSaldoPendiente.getProperties();
						props.putObjectValue("Saldo", Double.parseDouble("0"));
						devolucionSaldoPendiente.save(RefreshMode.REFRESH);
					}
					
					// Envia notificacion de devolucion a cliente
					if (Boolean.parseBoolean(jsonDevolucionDirecta.get("aplica").toString()) && !Boolean.parseBoolean(jsonDevolucionDirecta.get("omitirNotificacion").toString())) {
						try 
						{
							notificaDevolucionCliente(objStore, devolucionCliente, jsonDevolucionDirecta.get("observaciones").toString());
						}
						catch (Exception e) 
						{
							status = 2; // error en notificacion
							error = e.getMessage();
							e.printStackTrace();
						}
					}								
				
					break;
					
				case 1: // registro de pagos a devoluciones con saldo pendiente
					
					double diferenciaSaldoPendiente = Double.parseDouble(request.getParameter("diferenciaSaldoPendiente"));
							
					// Actualiza saldo pendiente en devoluciones
					Document devolucionSaldoPendiente = null;
					for (Object obj : jsonDevolucionesSaldoPendiente)
					{
						JSONObject jsonDevolucionSaldoPendiente = (JSONObject) obj;
						
						// Get devolucion con saldo pendiente
						try {
							devolucionSaldoPendiente = Factory.Document.fetchInstance(objStore, jsonDevolucionSaldoPendiente.get("id").toString(), null);
						} catch (EngineRuntimeException ere) {
							throw new RuntimeException ("La devolucion con saldo pendiente no pudo ser localizado.");		
						}
						
						props = devolucionSaldoPendiente.getProperties();
						
						if (diferenciaSaldoPendiente > 0 && devolucionSaldoPendiente.getProperties().getFloat64Value("Saldo") > 0) {
							props.putObjectValue("Saldo", diferenciaSaldoPendiente);
							devolucionSaldoPendiente.save(RefreshMode.REFRESH);
							diferenciaSaldoPendiente = 0;
						} else if (diferenciaSaldoPendiente < 0 && devolucionSaldoPendiente.getProperties().getFloat64Value("Saldo") < 0) {
							props.putObjectValue("Saldo", diferenciaSaldoPendiente);
							devolucionSaldoPendiente.save(RefreshMode.REFRESH);	
							diferenciaSaldoPendiente = 0;
						} else {
							props.putObjectValue("Saldo", Double.parseDouble("0"));
							devolucionSaldoPendiente.save(RefreshMode.REFRESH);							
						}
						
						// Facturas donde se encuentra asociada la devolucion con saldo pendiente
						folSet = devolucionSaldoPendiente.get_FoldersFiledIn();
						for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) 
						{
							Folder factura = it.next();

							// Asociacion de pagos a factura
							for (Object obj2 : jsonPagos)
							{
								JSONObject jsonPago = (JSONObject) obj2;
								
								// Get pago
								Document pago = null;
								try {
									pago = Factory.Document.fetchInstance(objStore, jsonPago.get("id").toString(), null);
								} catch (EngineRuntimeException ere) {
									throw new RuntimeException ("El pago no pudo ser localizado.");		
								}
								
								if (!pagosProcesados.contains(pago)) {	
									// Props Adicionales Pago
									props = pago.getProperties();
									props.putObjectValue("DocumentTitle", "Pago a Saldo Pendiente");
									props.putObjectValue("TipoPago", 3); // Pago a Saldo Pendiente
									props.putObjectValue("Proveedor", factura.getProperties().getEngineObjectValue("Proveedor"));
									props.putObjectValue("Empresa", factura.getProperties().getEngineObjectValue("Empresa"));
									pago.save(RefreshMode.REFRESH);					
									
									// Unfile pago de temporal
									ReferentialContainmentRelationship rel = temp.unfile(pago);
									rel.save(RefreshMode.NO_REFRESH);
								}
								
								// File Pago
								ReferentialContainmentRelationship rel = factura.file(pago, AutoUniqueName.AUTO_UNIQUE, pago.get_Name(), DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
								rel.save(RefreshMode.NO_REFRESH);	
								
								// Set Pago Procesado
								pagosProcesados.add(pago);					
							}
						}
					}
					
					break;					

			}
			

		} catch (Exception e) {
			status = 1;
			error = e.getMessage();
			e.printStackTrace();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("status", status);
		jsonResponse.put("error", error);
		return jsonResponse;		
		
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject savePagosACliente(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;	
		ObjectStore objStore = null;
		List<Document> pagosProcesados = new ArrayList<Document>();

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			JSONArray jsonPagos = JSONArray.parse(request.getParameter("pagos"));
			JSONArray jsonDevolucionesSaldoPendiente = JSONArray.parse(request.getParameter("devolucionesSaldoPendiente"));
			int tipoTransaccion = Integer.parseInt(request.getParameter("tipo"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}	
			
			// Get temp folder
			Folder temp = null;
			try {
				temp = Factory.Folder.fetchInstance(objStore, "/Temporal/Documentos", null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El folder temporal no fue localizado.");		
			}			
			
			switch (tipoTransaccion) {
				
				case 0: // registro de devolucion a cliente
					
					JSONObject devolucion = JSONObject.parse(request.getParameter("devolucion"));
					JSONObject datos = JSONObject.parse(request.getParameter("datos"));					

					// Get solicitud de devolucion al cliente
					Document devolucionCliente = null;
					try {
						devolucionCliente = Factory.Document.fetchInstance(objStore, devolucion.get("id").toString(), null);
					} catch (EngineRuntimeException ere) {
						throw new RuntimeException ("La devolucion del cliente no pudo ser localizada.");		
					}				
					
					// Facturas donde se encuentra actualmente asociada la solicitud de devolucion al cliente
					FolderSet folSet = devolucionCliente.get_FoldersFiledIn();
					for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) 
					{
						Folder factura = it.next();

						// Asociacion de pagos a factura
						for (Object obj : jsonPagos)
						{
							JSONObject jsonPago = (JSONObject) obj;
							
							// Get pago
							Document pago = null;
							try {
								pago = Factory.Document.fetchInstance(objStore, jsonPago.get("id").toString(), null);
							} catch (EngineRuntimeException ere) {
								throw new RuntimeException ("El pago no pudo ser localizado.");		
							}
							
							if (!pagosProcesados.contains(pago)) {	
								// Props Adicionales Pago
								com.filenet.api.property.Properties props = pago.getProperties();
								props.putObjectValue("Proveedor", factura.getProperties().getEngineObjectValue("Proveedor"));
								props.putObjectValue("Empresa", factura.getProperties().getEngineObjectValue("Empresa"));
								pago.save(RefreshMode.REFRESH);					
								
								// Unfile pago de temporal
								ReferentialContainmentRelationship rel = temp.unfile(pago);
								rel.save(RefreshMode.NO_REFRESH);
							}
							
							// File Pago
							ReferentialContainmentRelationship rel = factura.file(pago, AutoUniqueName.AUTO_UNIQUE, pago.get_Name(), DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
							rel.save(RefreshMode.NO_REFRESH);	
							
							// Set Pago Procesado
							pagosProcesados.add(pago);					
						}	
						
						com.filenet.api.property.Properties props = factura.getProperties();
						props.putObjectValue("EstadoCFDI", 3); // Devuelta al Cliente
						factura.save(RefreshMode.REFRESH);				
					}
				
					// Actualiza Estatus Devolucion Cliente
					com.filenet.api.property.Properties props = devolucionCliente.getProperties();			
					props.putObjectValue("Pendiente", false);
					props.putObjectValue("Saldo", Double.parseDouble(request.getParameter("saldoDistribuidor")));
					devolucionCliente.save(RefreshMode.REFRESH);
					
					// Actualiza Devoluciones de Proveedores con Saldo Pendiente Aplicadas
					for (Object obj : jsonDevolucionesSaldoPendiente)
					{
						JSONObject jsonDevolucionSaldoPendiente = (JSONObject) obj;
						
						// Get devolucion con saldo pendiente
						Document devolucionSaldoPendiente = null;
						try {
							devolucionSaldoPendiente = Factory.Document.fetchInstance(objStore, jsonDevolucionSaldoPendiente.get("id").toString(), null);
						} catch (EngineRuntimeException ere) {
							throw new RuntimeException ("La devolucion con saldo pendiente no pudo ser localizado.");		
						}
						
						props = devolucionSaldoPendiente.getProperties();
						props.putObjectValue("Saldo", Double.parseDouble("0"));
						devolucionSaldoPendiente.save(RefreshMode.REFRESH);
					}			
					
					// Envia notificacion de devolucion a cliente
					if (!(Boolean)datos.get("omitirEnvioCorreo")) {
						try 
						{
							notificaDevolucionCliente(objStore, devolucionCliente, datos.get("observaciones").toString());
						}
						catch (Exception e) 
						{
							status = 2; // error en notificacion
							error = e.getMessage();
							e.printStackTrace();
						}
					}						
					
					break;
					
				case 1: // registro de pagos a devoluciones con saldo pendiente
					
					double diferenciaSaldoPendiente = Double.parseDouble(request.getParameter("diferenciaSaldoPendiente"));
							
					// Actualiza saldo pendiente en devoluciones
					Document devolucionSaldoPendiente = null;
					for (Object obj : jsonDevolucionesSaldoPendiente)
					{
						JSONObject jsonDevolucionSaldoPendiente = (JSONObject) obj;
						
						// Get devolucion con saldo pendiente
						try {
							devolucionSaldoPendiente = Factory.Document.fetchInstance(objStore, jsonDevolucionSaldoPendiente.get("id").toString(), null);
						} catch (EngineRuntimeException ere) {
							throw new RuntimeException ("La devolucion con saldo pendiente no pudo ser localizado.");		
						}
						
						props = devolucionSaldoPendiente.getProperties();
						
						if (diferenciaSaldoPendiente > 0 && devolucionSaldoPendiente.getProperties().getFloat64Value("Saldo") > 0) {
							props.putObjectValue("Saldo", diferenciaSaldoPendiente);
							devolucionSaldoPendiente.save(RefreshMode.REFRESH);
							diferenciaSaldoPendiente = 0;
						} else if (diferenciaSaldoPendiente < 0 && devolucionSaldoPendiente.getProperties().getFloat64Value("Saldo") < 0) {
							props.putObjectValue("Saldo", diferenciaSaldoPendiente);
							devolucionSaldoPendiente.save(RefreshMode.REFRESH);	
							diferenciaSaldoPendiente = 0;
						} else {
							props.putObjectValue("Saldo", Double.parseDouble("0"));
							devolucionSaldoPendiente.save(RefreshMode.REFRESH);							
						}
						
						// Facturas donde se encuentra asociada la devolucion con saldo pendiente
						folSet = devolucionSaldoPendiente.get_FoldersFiledIn();
						for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) 
						{
							Folder factura = it.next();

							// Asociacion de pagos a factura
							for (Object obj2 : jsonPagos)
							{
								JSONObject jsonPago = (JSONObject) obj2;
								
								// Get pago
								Document pago = null;
								try {
									pago = Factory.Document.fetchInstance(objStore, jsonPago.get("id").toString(), null);
								} catch (EngineRuntimeException ere) {
									throw new RuntimeException ("El pago no pudo ser localizado.");		
								}
								
								if (!pagosProcesados.contains(pago)) {	
									// Props Adicionales Pago
									props = pago.getProperties();
									props.putObjectValue("DocumentTitle", "Pago a Saldo Pendiente");
									props.putObjectValue("TipoPago", 3); // Pago a Saldo Pendiente
									props.putObjectValue("Proveedor", factura.getProperties().getEngineObjectValue("Proveedor"));
									props.putObjectValue("Empresa", factura.getProperties().getEngineObjectValue("Empresa"));
									pago.save(RefreshMode.REFRESH);					
									
									// Unfile pago de temporal
									ReferentialContainmentRelationship rel = temp.unfile(pago);
									rel.save(RefreshMode.NO_REFRESH);
								}
								
								// File Pago
								ReferentialContainmentRelationship rel = factura.file(pago, AutoUniqueName.AUTO_UNIQUE, pago.get_Name(), DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
								rel.save(RefreshMode.NO_REFRESH);	
								
								// Set Pago Procesado
								pagosProcesados.add(pago);					
							}
						}
					}
					
					break;
				
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
		return jsonResponse;		
		
	}	
	
	@SuppressWarnings("unchecked")
	public static JSONObject deleteDevolucion(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		ObjectStore objStore = null;

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			JSONObject jsonDevolucion = JSONObject.parse(request.getParameter("devolucion"));
			int tipo = Integer.parseInt(request.getParameter("tipo"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}
			
			// Get Devolucion
			Document devolucion = Factory.Document.fetchInstance(objStore, jsonDevolucion.get("id").toString(), null);

			// Eliminar los pagos asociados a la devolucion
			switch (tipo) {
			
				case 0: // devolucion del proveedor
				
					// elimina pago del cliente asociado a la devolucion del proveedor
					JSONObject jsonData = getDatosDevolucionProveedor(objStore, devolucion);
			    	JSONObject jsonPagoCliente = (JSONObject) jsonData.get("pago");		
			    	Document pago = Factory.Document.fetchInstance(objStore, jsonPagoCliente.get("id").toString(), null);
					VersionSeries vs = pago.get_VersionSeries();
					vs.delete();
					vs.save(RefreshMode.REFRESH);	
					
					// actualiza estatus de las facturas asociadas a la devolucion del proveedor
					JSONArray jsonFacturas = (JSONArray) jsonData.get("facturas");
			    	for (Object obj : jsonFacturas) {
			    		JSONObject jsonFactura = (JSONObject) obj;
			    		Folder factura = Factory.Folder.fetchInstance(objStore, jsonFactura.get("id").toString(), null);
			    		com.filenet.api.property.Properties props = factura.getProperties();
			    		props.putObjectValue("EstadoCFDI", 0); // Pendiente por Pagar
			    		factura.save(RefreshMode.REFRESH);
			    	}
					
					break;
					
				case 1: // devolucion para el cliente
					
					// elimina pagos del proveedor asociados a la devolucion del cliente
					jsonData = getDatosDevolucionCliente(objStore, devolucion);
			    	JSONArray jsonPagosProveedor = (JSONArray) jsonData.get("pagosproveedor");
			    	for (Object obj : jsonPagosProveedor) {
			    		JSONObject jsonPagoProveedor = (JSONObject) obj;
			    		pago = Factory.Document.fetchInstance(objStore, jsonPagoProveedor.get("id").toString(), null);
						vs = pago.get_VersionSeries();
						vs.delete();
						vs.save(RefreshMode.REFRESH);				    		
			    	};
			    	
					// actualiza estatus de las facturas asociadas a la devolucion del proveedor
					jsonFacturas = (JSONArray) jsonData.get("facturas");
			    	for (Object obj : jsonFacturas) {
			    		JSONObject jsonFactura = (JSONObject) obj;
			    		Folder factura = Factory.Folder.fetchInstance(objStore, jsonFactura.get("id").toString(), null);
			    		com.filenet.api.property.Properties props = factura.getProperties();
			    		props.putObjectValue("EstadoCFDI", 1); // Pagada por Cliente
			    		factura.save(RefreshMode.REFRESH);
			    	}
			    	
			    	// actualiza el estatus de la devolucion del proveedor			    	
			    	for (Object obj : jsonFacturas) {
			    		JSONObject jsonFactura = (JSONObject) obj;
			    		Folder factura = Factory.Folder.fetchInstance(objStore, jsonFactura.get("id").toString(), null);
			    		DocumentSet docSet = factura.get_ContainedDocuments();
			    		for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) {
			    			Document doc = it.next();
			    			com.filenet.api.property.Properties props = doc.getProperties();
			    			if (doc.getClassName().equals("SolDocDevolucion") && props.getInteger32Value("TipoDevolucion") == 0 && !props.getBooleanValue("Pendiente")) { // devolucion proveedor
					    		props.putObjectValue("Pendiente", true);
					    		doc.save(RefreshMode.REFRESH);
			    			}
			    		}
			    	}
			    	
					break;					
			
			}
			
			// Elimina devolucion
			VersionSeries vs = devolucion.get_VersionSeries();
			vs.delete();
			vs.save(RefreshMode.REFRESH);
			
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
	
	public static JSONObject notificaDevolucionProveedor(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;	
		ObjectStore objStore = null;

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			JSONObject devolucion = JSONObject.parse(request.getParameter("devolucion"));
			String observaciones = request.getParameter("observaciones");
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}	
			
			// Get solicitud de devolucion del proveedor
			Document devolucionProveedor = null;
			try {
				devolucionProveedor = Factory.Document.fetchInstance(objStore, devolucion.get("id").toString(), null);
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("La devolucion del proveedor no pudo ser localizada.");		
			}
			
			// Envia notificacion de solicitud de devolucion a proveedor
			try 
			{
				notificaSolicitudDevolucion(objStore, devolucionProveedor, observaciones);
			}
			catch (Exception e) 
			{
				status = 2; // error en notificacion
				error = e.getMessage();
				e.printStackTrace();
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
		return jsonResponse;		
		
	}	
	
	@SuppressWarnings("unchecked")
	public static JSONObject notificaDevolucionCliente(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;	
		ObjectStore objStore = null;

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			JSONArray jsonSolicitudes = JSONArray.parse(request.getParameter("solicitudes"));
			String observaciones = request.getParameter("observaciones");
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}	
			
			for (Object obj : jsonSolicitudes) {
				JSONObject jsonSolicitud = (JSONObject) obj;
				Folder solicitud = Factory.Folder.fetchInstance(objStore, jsonSolicitud.get("id").toString(), null);
				DocumentSet docSet = solicitud.get_ContainedDocuments();
				for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) {
					Document doc = it.next();
					com.filenet.api.property.Properties props = doc.getProperties();
					if (doc.getClassName().equals("SolDocDevolucion") && props.getInteger32Value("TipoDevolucion") == 1) { // devolucion al cliente
						// Envia notificacion de devolucion a cliente
						try 
						{
							notificaDevolucionCliente(objStore, doc, observaciones);
						}
						catch (Exception e) 
						{
							status = 2; // error en notificacion
							error = e.getMessage();
							e.printStackTrace();
						}
						break;
					}
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
		return jsonResponse;		
		
	}	
			
	public static JSONObject addPago(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		ObjectStore objStore = null;
		JSONObject pago = new JSONObject();

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			pago = JSONObject.parse(request.getParameter("pago"));
			int tipo = Integer.parseInt(request.getParameter("tipo"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}
									
			// Get temp folder
			Folder temp = null;
			try {
				temp = Factory.Folder.fetchInstance(objStore, "/Temporal/Documentos", null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El folder temporal no fue localizado.");		
			}			
			
			Document docPago = null;
					
			if (pago.get("comprobantePago") != null) {
				docPago = Factory.Document.fetchInstance(objStore, ((JSONObject)pago.get("comprobantePago")).get("id").toString(), null);
				docPago.changeClass("SolDocPago");
				docPago.save(RefreshMode.REFRESH);					
			} else {
				docPago = Factory.Document.createInstance(callbacks.getP8ObjectStore(repositoryId), "SolDocPago");
				docPago.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
			}				
			
			com.filenet.api.property.Properties props = docPago.getProperties();
			String docTitle = "";
			switch(tipo) {
				case 1: // pago de proveedor
					docTitle = "Pago de Proveedor";
					break;
				case 2: // pago a cliente
					docTitle = "Pago a Cliente";
					break;							
			}
			props.putObjectValue("DocumentTitle", docTitle);
			props.putObjectValue("MontoTotal", Double.parseDouble(pago.get("importe").toString()));
			props.putObjectValue("FechaPago", getUTFCalendar(sdf.parse(pago.get("fechaPago").toString())).getTime());
			props.putObjectValue("MetodoPago", Integer.parseInt(pago.get("metodoPago").toString()));
			props.putObjectValue("Referencia", pago.get("referencia").toString());
			props.putObjectValue("TipoPago", tipo); 
			if (pago.containsKey("banco"))
				props.putObjectValue("Banco", Integer.parseInt(pago.get("banco").toString()));
			else
				props.putObjectValue("Banco", null);
					
			docPago.applySecurityTemplate(new Id(pagoslReadOnlyPolicy));
			docPago.save(RefreshMode.REFRESH);
			
			if (pago.get("comprobantePago") == null) {
				ReferentialContainmentRelationship rel = temp.file(docPago, AutoUniqueName.AUTO_UNIQUE, docPago.get_Name(), DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
				rel.save(RefreshMode.NO_REFRESH);
			}
			
			// Additional properties set to response
			pago.put("id", docPago.get_Id().toString());
			pago.put("contentSize", docPago.get_ContentSize());
			pago.put("documentTitle", docPago.get_Name());				

			
		} catch (Exception e) {
			error = e.getMessage();
			e.printStackTrace();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("pago", pago);
		return jsonResponse;		
		
	}		
	
	public static JSONObject deletePago(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		ObjectStore objStore = null;

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			JSONArray pagos = JSONArray.parse(request.getParameter("pagos"));
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    objStore = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				objStore = callbacks.getP8ObjectStore(repositoryId);
			}
					
			for (Object obj : pagos)
			{
				JSONObject pago = (JSONObject) obj;
				Document docPago = Factory.Document.fetchInstance(objStore, pago.get("id").toString(), null);
				VersionSeries vs = docPago.get_VersionSeries();
				vs.delete();
				vs.save(RefreshMode.REFRESH);
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
	
	private static void notificaSolicitudDevolucion(ObjectStore os, Document devolucion, String observaciones) throws Exception {
				
		// Get copia oculta de configuracion general
		Document settings = Factory.Document.fetchInstance(os, "/Settings/SolDocSettings", null);
		byte[] data = settings.getProperties().getBinaryValue("ClbJSONData");		
		JSONObject jsonSettings = JSONObject.parse(new String(data));

		// Get datos devolucion
		JSONObject jsonDevolucion = getDatosDevolucionProveedor(os, devolucion);			
		JSONArray jsonFacturas = (JSONArray) jsonDevolucion.get("facturas");
		JSONObject jsonPago = (JSONObject) jsonDevolucion.get("pago");
		
		Folder firstFactura = Factory.Folder.fetchInstance(os, ((JSONObject) jsonFacturas.get(0)).get("id").toString(), null);
		com.filenet.api.property.Properties props = firstFactura.getProperties();
		
		Folder cliente = getCliente(firstFactura);
		Folder empresa = (Folder) props.getEngineObjectValue("Empresa");
		Folder proveedor = (Folder) props.getEngineObjectValue("Proveedor");
		
		// Get datos proveedor
    	data = proveedor.getProperties().getBinaryValue("ClbJSONData");
    	JSONObject jsonDatosProveedor = JSONObject.parse(new String(data));	

		// Set parametros para notificacion
		String to = jsonDatosProveedor.get("contactoMailTo").toString();
		String cc = jsonDatosProveedor.get("contactoMailCc").toString();
		String alias = jsonSettings.get("emailAlias").toString();		
		String bcc = jsonSettings.get("emailBcc").toString();
		String subject = "Solicitud de Devolucion";
		List<String> templateNames = new ArrayList<String>();
		templateNames.add("contactoNombre");
		templateNames.add("devolucionSolicitada");
		templateNames.add("empresa");
		templateNames.add("cliente");
		templateNames.add("fechaPago");
		templateNames.add("importePago");
		templateNames.add("metodoPago");
		templateNames.add("referenciaPago");
		templateNames.add("bancoPago");
		templateNames.add("facturas");
		templateNames.add("observaciones");
		List<String> templateValues = new ArrayList<String>();
		templateValues.add(StringEscapeUtils.escapeHtml(jsonDatosProveedor.get("contactoNombre").toString()));
		templateValues.add(df.format(Double.parseDouble(jsonDevolucion.get("devolucionSolicitadaProveedor").toString())));
		templateValues.add(StringEscapeUtils.escapeHtml(empresa.get_FolderName()));
		templateValues.add(StringEscapeUtils.escapeHtml(cliente.get_FolderName()));
		templateValues.add(jsonPago.get("fechaPago").toString());
		templateValues.add(df.format(Double.parseDouble(jsonPago.get("importe").toString())));
		templateValues.add(StringEscapeUtils.escapeHtml(jsonPago.get("metodoPago").toString()));
		templateValues.add(StringEscapeUtils.escapeHtml(jsonPago.get("referencia").toString()));
		templateValues.add((jsonPago.containsKey("banco") ? jsonPago.get("banco").toString() : ""));

		StringBuffer facturasBuffer = new StringBuffer();
		for (Object obj : jsonFacturas) {
			JSONObject jsonFactura = (JSONObject) obj;
			facturasBuffer.append("<tr>");
			facturasBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + jsonFactura.get("folio").toString() + "</td>");
			facturasBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + (jsonFactura.get("fechaFactura") == null ? "" : jsonFactura.get("fechaFactura").toString()) + "</td>");
			facturasBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + (jsonFactura.get("numeroFactura") == null ? "" : jsonFactura.get("numeroFactura").toString()) + "</td>");
			facturasBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + df.format(Double.parseDouble(jsonFactura.get("importe").toString())) + "</td>");
			facturasBuffer.append("</tr>");			
		}
		templateValues.add(facturasBuffer.toString());
		templateValues.add(StringEscapeUtils.escapeHtml(observaciones));	

		// Get plantilla de notificacion
		Document template = Factory.Document.fetchInstance(os, "/Plantillas/Notificacion Solicitud de Devolucion", null);			
		
		// Se anexa el archivo de comprobante de pago en caso de existir 	
		List<Document> atts = new ArrayList<Document>();
		Document comprobantePago = Factory.Document.fetchInstance(os, jsonPago.get("id").toString(), null);
		if (comprobantePago.get_ContentSize() != null)
			atts.add(comprobantePago);
				
		// Envia notificacion
		MailService mailService = new MailService(jsonSettings);
		mailService.sendTemplateMail(to, cc, bcc, alias, subject, template, templateNames, templateValues, atts);		

	}
	
	@SuppressWarnings("unchecked")
	private static void notificaDevolucionCliente(ObjectStore os, Document devolucion, String observaciones) throws Exception {
		
		// Get copia oculta de configuracion general
		Document settings = Factory.Document.fetchInstance(os, "/Settings/SolDocSettings", null);
		byte[] data = settings.getProperties().getBinaryValue("ClbJSONData");		
		JSONObject jsonSettings = JSONObject.parse(new String(data));

		// Get datos devolucion
		JSONObject jsonDevolucion = getDatosDevolucionCliente(os, devolucion);			
		JSONArray jsonFacturas = (JSONArray) jsonDevolucion.get("facturas");
		
		// Get pagos a cliente
		double pagosCliente = 0;
		JSONArray jsonPagos = (JSONArray) jsonDevolucion.get("pagoscliente");
		for (Object obj : jsonPagos) {
			JSONObject jsonPago = (JSONObject) obj;
			pagosCliente += Double.parseDouble(jsonPago.get("importe").toString());
		}
		
		Folder firstFactura = Factory.Folder.fetchInstance(os, ((JSONObject) jsonFacturas.get(0)).get("id").toString(), null);
		com.filenet.api.property.Properties props = firstFactura.getProperties();
		
		Folder cliente = getCliente(firstFactura);
		Folder empresa = (Folder) props.getEngineObjectValue("Empresa");
		
		// Get datos cliente
    	data = cliente.getProperties().getBinaryValue("ClbJSONData");
    	JSONObject jsonDatosCliente = JSONObject.parse(new String(data));	

		// Set parametros para notificacion
		String to = jsonDatosCliente.get("contactoMailTo").toString();
		String cc = jsonDatosCliente.get("contactoMailCc").toString();
		String alias = jsonSettings.get("emailAlias").toString();		
		String bcc = jsonSettings.get("emailBcc").toString();
		String subject = "Notificacion de Devolucion";
		List<String> templateNames = new ArrayList<String>();
		templateNames.add("contactoNombre");
		templateNames.add("devolucionSolicitada");
		templateNames.add("razonSocial");
		templateNames.add("facturas");
		templateNames.add("pagos");
		templateNames.add("observaciones");
		List<String> templateValues = new ArrayList<String>();
		templateValues.add(StringEscapeUtils.escapeHtml(jsonDatosCliente.get("contactoNombre").toString()));
		templateValues.add(df.format(pagosCliente));
		templateValues.add(StringEscapeUtils.escapeHtml(empresa.get_FolderName()));
		// Facturas
		StringBuffer facturasBuffer = new StringBuffer();
		for (Object obj : jsonFacturas) {
			JSONObject jsonFactura = (JSONObject) obj;
			facturasBuffer.append("<tr>");
			facturasBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + jsonFactura.get("folio").toString() + "</td>");
			facturasBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + (jsonFactura.get("fechaFactura") == null ? "" : jsonFactura.get("fechaFactura").toString()) + "</td>");
			facturasBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + (jsonFactura.get("numeroFactura") == null ? "" : jsonFactura.get("numeroFactura").toString()) + "</td>");
			facturasBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + df.format(Double.parseDouble(jsonFactura.get("importe").toString())) + "</td>");			
			facturasBuffer.append("</tr>");			
		}
		templateValues.add(facturasBuffer.toString());
		// Pagos
		List<Document> atts = new ArrayList<Document>();
		StringBuffer pagosBuffer = new StringBuffer();
		DocumentSet docSet = firstFactura.get_ContainedDocuments();
		for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) {
			Document doc = it.next();
			props = doc.getProperties();
			if (doc.getClassName().equals("SolDocPago") && props.getInteger32Value("TipoPago") == 2) { // pago al cliente
				pagosBuffer.append("<tr>");
				pagosBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + sdf.format(getUTFCalendar(props.getDateTimeValue("FechaPago")).getTime()) + "</td>");
				pagosBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + df.format(props.getFloat64Value("MontoTotal")) + "</td>");
				pagosBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + getChoieListDisplayValue(os, "SolDocPago", "MetodoPago", props.getInteger32Value("MetodoPago")) + "</td>");
				if (props.getInteger32Value("Banco") != null)
					pagosBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + getChoieListDisplayValue(os, "SolDocPago", "Banco", props.getInteger32Value("Banco")) + "</td>");
				else
					pagosBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'></td>");
				pagosBuffer.append("<td style='background:#dcddc0; border-width: 1px; padding: 4px; border-style: solid;'>" + props.getStringValue("Referencia") + "</td>");
				pagosBuffer.append("</tr>");
				
				// Se anexan los archivos de comprobante de pago en caso de existir 	
				if (doc.get_ContentSize() != null)
					atts.add(doc);				
			}
		}
		templateValues.add(pagosBuffer.toString());				
		templateValues.add(StringEscapeUtils.escapeHtml(observaciones));	

		// Get plantilla de notificacion
		Document template = Factory.Document.fetchInstance(os, "/Plantillas/Notificacion de Devolucion a Cliente", null);
				
		// Envia notificacion
		MailService mailService = new MailService(jsonSettings);
		mailService.sendTemplateMail(to, cc, bcc, alias, subject, template, templateNames, templateValues, atts);		

	}	
	
	private static Folder getCliente(Folder factura) throws Exception {
		Folder cliente = factura;
		while (!cliente.getClassName().equals("SolDocCliente"))
			cliente = cliente.get_Parent();
		return cliente;
	}
	
	private static Calendar getUTFCalendar(Date dateObj) throws Exception {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(dateObj);	
		return cal;
	}	
	
	@SuppressWarnings("unchecked")
	private static String getChoieListDisplayValue(ObjectStore os, String className, String propName, int value) throws Exception {
	    PropertyFilter pf = new PropertyFilter();
	    pf.addIncludeType(0, null, Boolean.TRUE, FilteredPropertyType.ANY, null); 
	    ClassDefinition classDef = Factory.ClassDefinition.fetchInstance(os, className, pf);
	    PropertyDefinitionList propDefs = classDef.get_PropertyDefinitions();
	    for (Iterator<PropertyDefinition> it = propDefs.iterator(); it.hasNext(); ) {
	    	PropertyDefinition pd = it.next();
	    	if (pd.get_SymbolicName().equals(propName)) {
    	    	com.filenet.api.admin.ChoiceList clDef = pd.get_ChoiceList();
    	    	com.filenet.api.collection.ChoiceList cl = clDef.get_ChoiceValues();
    	    	for (Iterator<Choice> it2 = cl.iterator(); it2.hasNext(); ) {
    	    		Choice choice = it2.next();
    	    		if (choice.get_ChoiceIntegerValue().equals(value))
    	    			return choice.get_DisplayName();
    	    	}
	    	}
	    }
	    return null;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getDatosDevolucionProveedor(ObjectStore os, Document devolucion) throws Exception {

		JSONObject jsonResult = new JSONObject();
		double importeTotal = 0;
		double importeComisionDistribuidor = 0;
		double importeComisionProveedor = 0;
		double devolucionSolicitadaProveedor = 0;
		double devolucionSolicitadaDistribuidor = 0;
		JSONArray jsonFacturas = new JSONArray();
		JSONObject jsonPago = new JSONObject();	
		
		// Get facturas asociadas
		Folder factura = null;
		com.filenet.api.property.Properties props = null;
		FolderSet folSet = devolucion.get_FoldersFiledIn();
		for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) {
			factura = it.next();
			props = factura.getProperties();			
			JSONObject jsonFactura = new JSONObject();
			jsonFactura.put("id", factura.get_Id().toString());
			jsonFactura.put("folio", props.getStringValue("FolderName"));
	    	jsonFactura.put("fechaFactura", (props.getObjectValue("FechaFactura") == null ? null : sdf.format(getUTFCalendar(props.getDateTimeValue("FechaFactura")).getTime())));
	    	jsonFactura.put("numeroFactura", props.getStringValue("NumeroFactura"));
	    	jsonFactura.put("importe", props.getFloat64Value("MontoTotal"));
	    	byte[] data = props.getBinaryValue("ClbJSONData");
	    	JSONObject jsonData = JSONObject.parse(new String(data));		    	
	    	jsonFactura.put("porcentajeComisionProveedor", Double.parseDouble(jsonData.get("porcentajeComisionProveedor").toString()));
	    	jsonFactura.put("montoComisionProveedor", Double.parseDouble(jsonData.get("montoComisionProveedor").toString()));		    	
	    	jsonFactura.put("porcentajeComisionDistribuidor", Double.parseDouble(jsonData.get("porcentajeComisionDistribuidor").toString()));
	    	jsonFactura.put("montoComisionDistribuidor", Double.parseDouble(jsonData.get("montoComisionDistribuidor").toString()));	    	
	    	jsonFacturas.add(jsonFactura);
	    	importeTotal += props.getFloat64Value("MontoTotal");
	    	importeComisionDistribuidor += Double.parseDouble(jsonData.get("montoComisionDistribuidor").toString());
	    	importeComisionProveedor += Double.parseDouble(jsonData.get("montoComisionProveedor").toString());
		}
		devolucionSolicitadaDistribuidor = importeTotal - importeComisionDistribuidor;
		devolucionSolicitadaProveedor = importeTotal - importeComisionProveedor;
		
    	// Get pago del cliente
		DocumentSet docSet = factura.get_ContainedDocuments();
		for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) {
			Document doc = it.next();
			props = doc.getProperties();
			if (doc.getClassName().equals("SolDocPago") && props.getInteger32Value("TipoPago") == 0) { // pago de cliente
				jsonPago = new JSONObject();
				jsonPago.put("id", props.getIdValue("Id").toString());
				jsonPago.put("importe", props.getFloat64Value("MontoTotal"));
				jsonPago.put("fechaPago", sdf.format(getUTFCalendar(props.getDateTimeValue("FechaPago")).getTime()));
				jsonPago.put("metodoPago", getChoieListDisplayValue(os, "SolDocPago", "MetodoPago", props.getInteger32Value("MetodoPago")));
				if (props.getInteger32Value("Banco") != null)
					jsonPago.put("banco", getChoieListDisplayValue(os, "SolDocPago", "Banco", props.getInteger32Value("Banco")));
				jsonPago.put("referencia", props.getStringValue("Referencia"));
				jsonPago.put("contentSize", doc.get_ContentSize());
				jsonPago.put("documentTitle", doc.get_Name());						
				break;
			}
		}

		// Set resultado		
		jsonResult.put("facturas", jsonFacturas);
		jsonResult.put("pago", jsonPago);
		jsonResult.put("importeTotal", importeTotal);
		jsonResult.put("importeComisionProveedor", importeComisionProveedor);
		jsonResult.put("devolucionSolicitadaProveedor", devolucionSolicitadaProveedor);
		jsonResult.put("devolucionSolicitadaDistribuidor", devolucionSolicitadaDistribuidor);
		
		return jsonResult;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getDatosDevolucionCliente(ObjectStore os, Document devolucion) throws Exception {

		JSONObject jsonResult = new JSONObject();
		double importeTotal = 0;
		double importeComisionDistribuidor = 0;
		double importeComisionProveedor = 0;
		double devolucionSolicitadaProveedor = 0;
		double devolucionSolicitadaDistribuidor = 0;
		JSONArray jsonFacturas = new JSONArray();
		JSONArray jsonPagosProveedor = new JSONArray();
		JSONObject jsonPagoProveedor = new JSONObject();
		JSONArray jsonPagosACliente = new JSONArray();
		JSONObject jsonPagoACliente = new JSONObject();		
		JSONObject jsonPagoDeCliente = new JSONObject();
		
		// Get facturas asociadas
		Folder factura = null;
		com.filenet.api.property.Properties props = null;
		FolderSet folSet = devolucion.get_FoldersFiledIn();
		for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) {
			factura = it.next();
			props = factura.getProperties();			
			JSONObject jsonFactura = new JSONObject();
			jsonFactura.put("id", factura.get_Id().toString());
			jsonFactura.put("folio", props.getStringValue("FolderName"));
	    	jsonFactura.put("fechaFactura", (props.getObjectValue("FechaFactura") == null ? null : sdf.format(getUTFCalendar(props.getDateTimeValue("FechaFactura")).getTime())));
	    	jsonFactura.put("numeroFactura", props.getStringValue("NumeroFactura"));
	    	jsonFactura.put("importe", props.getFloat64Value("MontoTotal"));
	    	byte[] data = props.getBinaryValue("ClbJSONData");
	    	JSONObject jsonData = JSONObject.parse(new String(data));	
	    	jsonFactura.put("porcentajeComisionProveedor", Double.parseDouble(jsonData.get("porcentajeComisionProveedor").toString()));
	    	jsonFactura.put("montoComisionProveedor", Double.parseDouble(jsonData.get("montoComisionProveedor").toString()));		    	
	    	jsonFactura.put("porcentajeComisionDistribuidor", Double.parseDouble(jsonData.get("porcentajeComisionDistribuidor").toString()));
	    	jsonFactura.put("montoComisionDistribuidor", Double.parseDouble(jsonData.get("montoComisionDistribuidor").toString()));	    		    	
	    	jsonFacturas.add(jsonFactura);
	    	importeTotal += props.getFloat64Value("MontoTotal");
	    	importeComisionDistribuidor += Double.parseDouble(jsonData.get("montoComisionDistribuidor").toString());
	    	importeComisionProveedor += Double.parseDouble(jsonData.get("montoComisionProveedor").toString());
		}
		devolucionSolicitadaDistribuidor = importeTotal - importeComisionDistribuidor;
		devolucionSolicitadaProveedor = importeTotal - importeComisionProveedor;
		
    	// Get pago del cliente
		DocumentSet docSet = factura.get_ContainedDocuments();
		for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) {
			Document doc = it.next();
			props = doc.getProperties();
			if (doc.getClassName().equals("SolDocPago") && props.getInteger32Value("TipoPago") == 0) { // pago de cliente
				jsonPagoDeCliente = new JSONObject();
				jsonPagoDeCliente.put("id", props.getIdValue("Id").toString());
				jsonPagoDeCliente.put("importe", props.getFloat64Value("MontoTotal"));
				jsonPagoDeCliente.put("fechaPago", sdf.format(getUTFCalendar(props.getDateTimeValue("FechaPago")).getTime()));
				jsonPagoDeCliente.put("metodoPago", getChoieListDisplayValue(os, "SolDocPago", "MetodoPago", props.getInteger32Value("MetodoPago")));
				if (props.getInteger32Value("Banco") != null)
					jsonPagoDeCliente.put("banco", getChoieListDisplayValue(os, "SolDocPago", "Banco", props.getInteger32Value("Banco")));
				jsonPagoDeCliente.put("referencia", props.getStringValue("Referencia"));
				jsonPagoDeCliente.put("contentSize", doc.get_ContentSize());
				jsonPagoDeCliente.put("documentTitle", doc.get_Name());						
				break;
			}
		}
		
    	// Get pagos del proveedor
		docSet = factura.get_ContainedDocuments();
		for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) {
			Document doc = it.next();
			props = doc.getProperties();
			if (doc.getClassName().equals("SolDocPago") && props.getInteger32Value("TipoPago") == 1) { // pago de proveedor
				jsonPagoProveedor = new JSONObject();
				jsonPagoProveedor.put("id", props.getIdValue("Id").toString());
				jsonPagoProveedor.put("importe", props.getFloat64Value("MontoTotal"));
				jsonPagoProveedor.put("fechaPago", sdf.format(getUTFCalendar(props.getDateTimeValue("FechaPago")).getTime()));
				jsonPagoProveedor.put("metodoPago", getChoieListDisplayValue(os, "SolDocPago", "MetodoPago", props.getInteger32Value("MetodoPago")));
				if (props.getInteger32Value("Banco") != null)
					jsonPagoProveedor.put("banco", getChoieListDisplayValue(os, "SolDocPago", "Banco", props.getInteger32Value("Banco")));
				jsonPagoProveedor.put("referencia", props.getStringValue("Referencia"));
				jsonPagoProveedor.put("contentSize", doc.get_ContentSize());
				jsonPagoProveedor.put("documentTitle", doc.get_Name());
				jsonPagosProveedor.add(jsonPagoProveedor);
			}
		}	
		
    	// Get pagos al cliente
		docSet = factura.get_ContainedDocuments();
		for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) {
			Document doc = it.next();
			props = doc.getProperties();
			if (doc.getClassName().equals("SolDocPago") && props.getInteger32Value("TipoPago") == 2) { // pago a cliente
				jsonPagoACliente = new JSONObject();
				jsonPagoACliente.put("id", props.getIdValue("Id").toString());
				jsonPagoACliente.put("importe", props.getFloat64Value("MontoTotal"));
				jsonPagoACliente.put("fechaPago", sdf.format(getUTFCalendar(props.getDateTimeValue("FechaPago")).getTime()));
				jsonPagoACliente.put("metodoPago", getChoieListDisplayValue(os, "SolDocPago", "MetodoPago", props.getInteger32Value("MetodoPago")));
				if (props.getInteger32Value("Banco") != null)
					jsonPagoACliente.put("banco", getChoieListDisplayValue(os, "SolDocPago", "Banco", props.getInteger32Value("Banco")));
				jsonPagoACliente.put("referencia", props.getStringValue("Referencia"));
				jsonPagoACliente.put("contentSize", doc.get_ContentSize());
				jsonPagoACliente.put("documentTitle", doc.get_Name());
				jsonPagosACliente.add(jsonPagoACliente);
			}
		}		
		
		// Set resultado		
		jsonResult.put("facturas", jsonFacturas);
		jsonResult.put("pago", jsonPagoDeCliente);
		jsonResult.put("pagosproveedor", jsonPagosProveedor);
		jsonResult.put("pagoscliente", jsonPagosACliente);
		jsonResult.put("importeTotal", importeTotal);
		jsonResult.put("importeComisionDistribuidor", importeComisionDistribuidor);
		jsonResult.put("devolucionSolicitadaProveedor", devolucionSolicitadaProveedor);
		jsonResult.put("devolucionSolicitadaDistribuidor", devolucionSolicitadaDistribuidor);
		
		return jsonResult;
	}	
	
    private static String convertLocalTimeToUTC(Date localDate) throws Exception{     
        SimpleDateFormat utcFormat=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");   
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateUTCAsString = utcFormat.format(getDate(localDate,"yyyy-MM-dd HH:mm:ss z"));   
        return dateUTCAsString;   
    }
    
    private static Date getDate(Date date, String format) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.parse(sdf.format(date));
    }  
    
    @SuppressWarnings("unchecked")
	public static Document cloneDocument(ObjectStore os, String className, Document doc, String documentTitle, Map<String, Object> propsMap, String securityTemplateId) throws Exception {
		Document newDoc = Factory.Document.createInstance(os, className);
		
		if (!doc.get_ContentElements().isEmpty()) {
			ContentTransfer oriCT = (ContentTransfer) doc.get_ContentElements().get(0);
			ContentElementList contentList = Factory.ContentElement.createList();
			ContentTransfer contentTransfer = Factory.ContentTransfer.createInstance();
			contentTransfer.setCaptureSource(oriCT.accessContentStream());
			contentTransfer.set_RetrievalName(oriCT.get_RetrievalName());
			contentTransfer.set_ContentType(oriCT.get_ContentType());
			contentList.add(contentTransfer);
			newDoc.set_ContentElements(contentList);		
		}
		
		newDoc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);

		com.filenet.api.property.Properties props = newDoc.getProperties();
		if (documentTitle.isEmpty())
			props.putObjectValue("DocumentTitle", props.getObjectValue("DocumentTitle"));
		else
			props.putObjectValue("DocumentTitle", documentTitle);
		
		for (Iterator<String> it = propsMap.keySet().iterator(); it.hasNext(); ) {
			String key = it.next();
			props.putObjectValue(key, propsMap.get(key));
		}
			
		if (!securityTemplateId.isEmpty())
			newDoc.applySecurityTemplate(new Id(securityTemplateId));
		
		newDoc.save(RefreshMode.REFRESH);
		return newDoc;
    }
	
}
