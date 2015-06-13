package com.ibm.ecm.extension.service;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.Document;
import com.filenet.api.core.IndependentObject;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.UserContext;
import com.filenet.api.constants.RefreshMode;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class ImportSaadContenedorService {
	
	private final static SimpleDateFormat shortFormat = new SimpleDateFormat("dd/MM/yyyy");
	
	@SuppressWarnings("unchecked")
	public static JSONObject getDatosContenedor(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		
		try {
			
			String pRepositoryId = request.getParameter("repositoryid");
			String pNumeroContenedor = request.getParameter("numerocontenedor");
			
			Subject subject = callbacks.getP8Subject(pRepositoryId);
			UserContext.get().pushSubject(subject);
			
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(pRepositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("NumeroContenedor, FechaRegistro, NombreCliente, NombreNaviera, NombreAgenciaAduanal, Importadora");
		    sql.setFromClauseInitialValue("Contenedor ", null, false);
		    sql.setWhereClause("NumeroContenedor = '" + pNumeroContenedor + "' AND This INSUBFOLDER '/Contenedores'");
		    sql.setMaxRecords(1);
		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
		    
		    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		    {
		    	jsonObject = new JSONObject();
		    	RepositoryRow row = it.next();
		    	com.filenet.api.property.Properties props = row.getProperties();
				String numeroContenedor = props.getStringValue("NumeroContenedor");
				String nombreCliente = props.getStringValue("NombreCliente");
				String nombreNaviera = props.getStringValue("NombreNaviera");
				String nombreAgenciaAduanal = props.getStringValue("NombreAgenciaAduanal");
				String importadora = props.getStringValue("Importadora");
				Date fechaRegistro = props.getDateTimeValue("FechaRegistro");
				cal.setTime(fechaRegistro);
				jsonObject.put("numero", numeroContenedor);
				jsonObject.put("cliente", nombreCliente);
				jsonObject.put("naviera", nombreNaviera);
				jsonObject.put("agenciaaduanal", nombreAgenciaAduanal);
				jsonObject.put("importadora", importadora);
				jsonObject.put("fecharegistro", shortFormat.format(cal.getTime()));
				// add element
				jsonArray.add(jsonObject);
		    }
		    
		} catch (Exception e) {
			status = 1;
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("status", status);
		jsonResponse.put("error", error);
		jsonResponse.put("contenedores", jsonArray);
		return jsonResponse;
	}	
	
	public static JSONObject getVWVersion(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String vwVersion = null;
		
		try {
			
			String pRepositoryId = request.getParameter("repositoryid");
			String pNombreWorkflow = request.getParameter("nombreworkflow");
			
			Subject subject = callbacks.getP8Subject(pRepositoryId);
			UserContext.get().pushSubject(subject);
			
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(pRepositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("VWVersion");
		    sql.setFromClauseInitialValue("WorkflowDefinition ", null, false);
		    sql.setWhereClause("DocumentTitle = '" + pNombreWorkflow + "' AND IsCurrentVersion = True");
		    sql.setMaxRecords(1);
		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
		    
		    if (rowSet.iterator().hasNext()) {
		    	RepositoryRow row = (RepositoryRow) rowSet.iterator().next();
		    	com.filenet.api.property.Properties props = row.getProperties();
				vwVersion = props.getStringValue("VWVersion");	    	
		    }
		    		    
		} catch (Exception e) {
			
		} finally {
			UserContext.get().popSubject();
		}		
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("vwversion", vwVersion);		
		return jsonResponse;		
		
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getDocumentosContenedor(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;

		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		
		try {
			
			String pRepositoryId = request.getParameter("repositoryid");
			String pContenedorId = request.getParameter("contenedorid");
			
			Subject subject = callbacks.getP8Subject(pRepositoryId);
			UserContext.get().pushSubject(subject);
			
			Folder contenedor = Factory.Folder.fetchInstance(callbacks.getP8ObjectStore(pRepositoryId), pContenedorId, null);
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(pRepositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, Name, DocumentTitle, TipoComprobante, ConceptoComprobante, TipoAnexoCuentaGastos");
		    sql.setFromClauseInitialValue("GCDocumento ", null, true);
		    sql.setWhereClause("IsCurrentVersion = True AND This INFOLDER '" + contenedor.get_PathName() + "'");
		    
		    IndependentObjectSet objSet = search.fetchObjects(sql, 50, null, true);
    		
    		for (Iterator<IndependentObject> it = objSet.iterator(); it.hasNext(); ) 
    		{
    			jsonObject = new JSONObject();
    			IndependentObject obj = it.next();
    			if (obj instanceof Document) {
    				Document doc = (Document) obj;
    				String docClass = doc.getClassName();
    				int docTipoComprobante = -1;
    				String docTipoDocumento = null;
    				com.filenet.api.property.Properties props = doc.getProperties();
    				if (docClass.equals("Comprobante")) {
    					docTipoComprobante = props.getInteger32Value("TipoComprobante");
    					docTipoDocumento = props.getStringValue("ConceptoComprobante");
    				} else if (docClass.equals("AnexoCuentaGastos")) {
    					docTipoDocumento = props.getStringValue("TipoAnexoCuentaGastos");
    				}
    	    		jsonObject.put("id", doc.get_Id().toString());
    	    		jsonObject.put("docTitle", doc.get_Name());
    	    		jsonObject.put("docClass", docClass);
    	    		jsonObject.put("tipoDocumento", docTipoDocumento);
    	    		if (docTipoComprobante == -1)
    	    			jsonObject.put("tipoComprobante", null);    	    		
    	    		else
    	    			jsonObject.put("tipoComprobante", docTipoComprobante);
        			// Add    	    		
    	    		jsonArray.add(jsonObject);	    				

    			}
    		}
					    
		} catch (Exception e) {
			status = 1;
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("status", status);
		jsonResponse.put("error", error);
		jsonResponse.put("docs", jsonArray);
		return jsonResponse;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getDetalleFacturaComercial(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;
		final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

		JSONObject jsonObject = new JSONObject();
		
		try {
			
			String pRepositoryId = request.getParameter("repositoryid");
			String pContenedorId = request.getParameter("contenedorid");
			
			Subject subject = callbacks.getP8Subject(pRepositoryId);
			UserContext.get().pushSubject(subject);
						
			Folder contenedor = Factory.Folder.fetchInstance(callbacks.getP8ObjectStore(pRepositoryId), pContenedorId, null);
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(pRepositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("FechaFactura, MontoTotal, IDFiscal, NumeroFactura");
		    sql.setFromClauseInitialValue("FacturaComercial ", null, false);
		    sql.setWhereClause("IsCurrentVersion = True AND This INFOLDER '" + contenedor.get_PathName() + "'");
		    sql.setMaxRecords(1);
		    
		    RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
    		
		    Iterator<RepositoryRow> it = rowSet.iterator();
		    if (it.hasNext()) {
    			RepositoryRow row = it.next();
    			com.filenet.api.property.Properties props = row.getProperties();
    			Date fechaFactura = props.getDateTimeValue("FechaFactura");
    			if (fechaFactura != null)
    				jsonObject.put("fecha", sdf.format(fechaFactura));
    			else
    				jsonObject.put("fecha", null);
    			jsonObject.put("monto", props.getFloat64Value("MontoTotal"));
    			jsonObject.put("idfiscal", props.getStringValue("IDFiscal"));
    			jsonObject.put("numero", props.getStringValue("NumeroFactura"));		    	
		    }	
					    
		} catch (Exception e) {
			status = 1;
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("status", status);
		jsonResponse.put("error", error);
		jsonResponse.put("fecha", (jsonObject.containsKey("fecha") ? jsonObject.get("fecha") : null));
		jsonResponse.put("monto", (jsonObject.containsKey("monto") ? jsonObject.get("monto") : 0));
		jsonResponse.put("idfiscal", (jsonObject.containsKey("idfiscal") ? jsonObject.get("idfiscal") : null));
		jsonResponse.put("proveedor", (jsonObject.containsKey("proveedor") ? jsonObject.get("proveedor") : null));
		jsonResponse.put("numero", (jsonObject.containsKey("numero") ? jsonObject.get("numero") : null));
		return jsonResponse;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject getDetalleCuentaGastos(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		JSONObject jsonObject = new JSONObject();
		
		try {
			
			String pRepositoryId = request.getParameter("repositoryid");
			String pContenedorId = request.getParameter("contenedorid");
			
			Subject subject = callbacks.getP8Subject(pRepositoryId);
			UserContext.get().pushSubject(subject);
			
			Folder contenedor = Factory.Folder.fetchInstance(callbacks.getP8ObjectStore(pRepositoryId), pContenedorId, null);
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(pRepositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("FechaFactura, NumeroFactura, Anticipo, Gastos, Honorarios, Impuestos, IVA, Saldo, TipoCambio, ValorAduanal, NumeroPedimento, ClbJSONData");
		    sql.setFromClauseInitialValue("CuentaGastos ", null, false);
		    sql.setWhereClause("IsCurrentVersion = True AND This INFOLDER '" + contenedor.get_PathName() + "'");
		    sql.setMaxRecords(1);			

		    RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
    		
		    Iterator<RepositoryRow> it = rowSet.iterator();
		    if (it.hasNext()) {
    			RepositoryRow row = it.next();
    			com.filenet.api.property.Properties props = row.getProperties();
    			Date fechaFactura = props.getDateTimeValue("FechaFactura");
    			if (fechaFactura != null)
    				jsonObject.put("fecha", sdf.format(fechaFactura));
    			else
    				jsonObject.put("fecha", null);
    			jsonObject.put("numero", props.getStringValue("NumeroFactura"));
    			jsonObject.put("anticipo", props.getFloat64Value("Anticipo"));
    			jsonObject.put("gastos", props.getFloat64Value("Gastos"));
    			jsonObject.put("honorarios", props.getFloat64Value("Honorarios"));
    			jsonObject.put("impuestos", props.getFloat64Value("Impuestos"));
    			jsonObject.put("iva", props.getFloat64Value("IVA"));
    			jsonObject.put("saldo", props.getFloat64Value("Saldo"));
    			jsonObject.put("tipocambio", props.getFloat64Value("TipoCambio"));
    			jsonObject.put("valoraduanal", props.getFloat64Value("ValorAduanal"));
    			jsonObject.put("pedimento", props.getStringValue("NumeroPedimento"));
    			
    	    	byte[] data = props.getBinaryValue("ClbJSONData");
    	    	if (data != null)
    	    		jsonObject.put("datos", JSONObject.parse(new String(data)));    	
		    }	
					    
		} catch (Exception e) {
			status = 1;
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("status", status);
		jsonResponse.put("error", error);	
		jsonResponse.put("fecha", (jsonObject.containsKey("fecha") ? jsonObject.get("fecha") : null));
		jsonResponse.put("numero", (jsonObject.containsKey("numero") ? jsonObject.get("numero") : null));
		jsonResponse.put("anticipo", (jsonObject.containsKey("anticipo") ? jsonObject.get("anticipo") : 0));
		jsonResponse.put("gastos", (jsonObject.containsKey("gastos") ? jsonObject.get("gastos") : 0));
		jsonResponse.put("honorarios", (jsonObject.containsKey("honorarios") ? jsonObject.get("honorarios") : 0));
		jsonResponse.put("impuestos", (jsonObject.containsKey("impuestos") ? jsonObject.get("impuestos") : 0));
		jsonResponse.put("iva", (jsonObject.containsKey("iva") ? jsonObject.get("iva") : 0));
		jsonResponse.put("saldo", (jsonObject.containsKey("saldo") ? jsonObject.get("saldo") : 0));
		jsonResponse.put("tipocambio", (jsonObject.containsKey("tipocambio") ? jsonObject.get("tipocambio") : 0));
		jsonResponse.put("valoraduanal", (jsonObject.containsKey("valoraduanal") ? jsonObject.get("valoraduanal") : 0));
		jsonResponse.put("pedimento", (jsonObject.containsKey("pedimento") ? jsonObject.get("pedimento") : null));
		jsonResponse.put("tipomercancia", (jsonObject.containsKey("tipomercancia") ? jsonObject.get("tipomercancia") : null));
		jsonResponse.put("datos", (jsonObject.containsKey("datos") ? jsonObject.get("datos") : null));
		return jsonResponse;
	}	
	
	@SuppressWarnings("unchecked")
	public static JSONObject getDetallePedimentoDesglosado(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;

		JSONArray jsonArray = new JSONArray();
		
		try {
			
			String pRepositoryId = request.getParameter("repositoryid");
			String pContenedorId = request.getParameter("contenedorid");
			
			Subject subject = callbacks.getP8Subject(pRepositoryId);
			UserContext.get().pushSubject(subject);

			Folder contenedor = Factory.Folder.fetchInstance(callbacks.getP8ObjectStore(pRepositoryId), pContenedorId, null);
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(pRepositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("This, IsCurrentVersion, ClbJSONData");
		    sql.setFromClauseInitialValue("PedimentoDesglosado ", null, false);
		    sql.setWhereClause("IsCurrentVersion = True AND This INFOLDER '" + contenedor.get_PathName() + "'");
		    sql.setMaxRecords(1);				
			
		    RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
    		
		    Iterator<RepositoryRow> it = rowSet.iterator();
		    if (it.hasNext()) {
    			RepositoryRow row = it.next();
    			com.filenet.api.property.Properties props = row.getProperties();
    	    	byte[] data = props.getBinaryValue("ClbJSONData");
    	    	if (data != null)
    	    		jsonArray = JSONArray.parse(new String(data));    	
		    }	
					    
		} catch (Exception e) {
			status = 1;
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("status", status);
		jsonResponse.put("error", error);
		jsonResponse.put("datos", jsonArray);
		return jsonResponse;
	}	
	
	@SuppressWarnings("unchecked")
	public static JSONObject getDetalleComprobantes(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		
		try {
			
			String pRepositoryId = request.getParameter("repositoryid");
			String pContenedorId = request.getParameter("contenedorid");
			
			Subject subject = callbacks.getP8Subject(pRepositoryId);
			UserContext.get().pushSubject(subject);

			Folder contenedor = Factory.Folder.fetchInstance(callbacks.getP8ObjectStore(pRepositoryId), pContenedorId, null);
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(pRepositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("FechaComprobante, MontoTotal, TipoComprobante, ConceptoComprobante, NombreProveedor, AfectaCuentaGastos");
		    sql.setFromClauseInitialValue("Comprobante ", null, false);
		    sql.setWhereClause("IsCurrentVersion = True AND This INFOLDER '" + contenedor.get_PathName() + "'");		

		    RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
    		
    		for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
    		{
    			jsonObject = new JSONObject();
    			RepositoryRow row = it.next();
    			com.filenet.api.property.Properties props = row.getProperties();
    			Date fechaComprobante = props.getDateTimeValue("FechaComprobante");
    			if (fechaComprobante != null)
    				jsonObject.put("fecha", sdf.format(fechaComprobante));
    			else
    				jsonObject.put("fecha", null);
    			jsonObject.put("monto", props.getFloat64Value("MontoTotal"));
    			jsonObject.put("tipo", props.getInteger32Value("TipoComprobante"));
    			jsonObject.put("concepto", props.getStringValue("ConceptoComprobante"));
    			jsonObject.put("proveedor", props.getStringValue("NombreProveedor"));
    			jsonObject.put("afectactagastos", props.getBooleanValue("AfectaCuentaGastos"));
    			// Add    	    		
    			jsonArray.add(jsonObject);	    				

    		}	
					    
		} catch (Exception e) {
			status = 1;
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("status", status);
		jsonResponse.put("error", error);
		jsonResponse.put("comprobantes", jsonArray);
		return jsonResponse;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject setDetallePedimentoDesglosado(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;		
		
		try {
			
			String pRepositoryId = request.getParameter("repositoryid");
			String pContenedorId = request.getParameter("contenedorid");
			String pDatos = request.getParameter("datos");
			JSONArray jsonData = JSONArray.parse(pDatos);    	
			
			Subject subject = callbacks.getP8Subject(pRepositoryId);
			UserContext.get().pushSubject(subject);	
			
			Folder contenedor = Factory.Folder.fetchInstance(callbacks.getP8ObjectStore(pRepositoryId), pContenedorId, null);
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(pRepositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("This, IsCurrentVersion, ClbJSONData");
		    sql.setFromClauseInitialValue("PedimentoDesglosado ", null, false);
		    sql.setWhereClause("IsCurrentVersion = True AND This INFOLDER '" + contenedor.get_PathName() + "'");
		    sql.setMaxRecords(1);				
			
		    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, 50, null, true);
    		
		    Iterator<Document> it = docSet.iterator();
		    if (it.hasNext()) {
		    	Document doc = it.next();
    			com.filenet.api.property.Properties props = doc.getProperties();
    			props.putObjectValue("ClbJSONData", jsonData.serialize().getBytes());
    			doc.save(RefreshMode.REFRESH);	    	    	
		    }				
			
		} catch (Exception e) {
			status = 1;
			error = e.getMessage();
			
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
	public static JSONObject setDatosFacturaComercial(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;		
		
		try {
			
			String pRepositoryId = request.getParameter("repositoryid");
			String pContenedorId = request.getParameter("contenedorid");
			String pDatos = request.getParameter("datos");
			JSONObject jsonData = JSONObject.parse(pDatos);    	
			
			Subject subject = callbacks.getP8Subject(pRepositoryId);
			UserContext.get().pushSubject(subject);	
			
			Folder contenedor = Factory.Folder.fetchInstance(callbacks.getP8ObjectStore(pRepositoryId), pContenedorId, null);
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(pRepositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("*");
		    sql.setFromClauseInitialValue("FacturaComercial ", null, false);
		    sql.setWhereClause("IsCurrentVersion = True AND This INFOLDER '" + contenedor.get_PathName() + "'");
		    sql.setMaxRecords(1);				
			
		    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, 50, null, true);
    		
		    Iterator<Document> it = docSet.iterator();
		    if (it.hasNext()) {
		    	Document doc = it.next();
    			com.filenet.api.property.Properties props = doc.getProperties();
    			props.putObjectValue("MontoTotal", Double.parseDouble(jsonData.get("valorcomercial").toString()));
    			doc.save(RefreshMode.REFRESH);	    	    	
		    }				
			
		} catch (Exception e) {
			status = 1;
			error = e.getMessage();
			
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
	public static JSONObject setDatosCuantaGastos(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		int status = 0;
		String error = null;	
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		try {
			
			String pRepositoryId = request.getParameter("repositoryid");
			String pContenedorId = request.getParameter("contenedorid");
			String pDatos = request.getParameter("datos");
			JSONObject jsonData = JSONObject.parse(pDatos);    	
			
			Subject subject = callbacks.getP8Subject(pRepositoryId);
			UserContext.get().pushSubject(subject);	
			
			Folder contenedor = Factory.Folder.fetchInstance(callbacks.getP8ObjectStore(pRepositoryId), pContenedorId, null);
		    SearchScope search = new SearchScope(callbacks.getP8ObjectStore(pRepositoryId));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("*");
		    sql.setFromClauseInitialValue("CuentaGastos ", null, false);
		    sql.setWhereClause("IsCurrentVersion = True AND This INFOLDER '" + contenedor.get_PathName() + "'");
		    sql.setMaxRecords(1);				
			
		    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, 50, null, true);
    		
		    Iterator<Document> it = docSet.iterator();
		    if (it.hasNext()) {
		    	Document doc = it.next();
    			com.filenet.api.property.Properties props = doc.getProperties();
    			props.putObjectValue("ValorAduanal", Double.parseDouble(jsonData.get("valoraduanal").toString()));
    			props.putObjectValue("Gastos", Double.parseDouble(jsonData.get("gastos").toString()));
    			props.putObjectValue("Honorarios", Double.parseDouble(jsonData.get("honorarios").toString()));
    			props.putObjectValue("IVA", Double.parseDouble(jsonData.get("iva").toString()));
    			props.putObjectValue("Anticipo", Double.parseDouble(jsonData.get("anticipo").toString()));
    			props.putObjectValue("Saldo", Double.parseDouble(jsonData.get("saldo").toString()));
    			props.putObjectValue("Impuestos", Double.parseDouble(jsonData.get("impuestos").toString()));
    			props.putObjectValue("TipoCambio", Double.parseDouble(jsonData.get("tipocambio").toString()));
    			props.putObjectValue("FechaFactura", sdf.parse((String)jsonData.get("fechafactura")));
    			props.putObjectValue("NumeroFactura", (String)jsonData.get("numerofactura"));
    			props.putObjectValue("NumeroPedimento", (String)jsonData.get("numeropedimento"));
    			props.putObjectValue("ClbJSONData", JSONObject.parse(jsonData.get("datos").toString()).serialize().getBytes());
    			doc.save(RefreshMode.REFRESH);	    	    	
		    }
			
		} catch (Exception e) {
			status = 1;
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("status", status);
		jsonResponse.put("error", error);
		return jsonResponse;			
		
	}		
	
}
