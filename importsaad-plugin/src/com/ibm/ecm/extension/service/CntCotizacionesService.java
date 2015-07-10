package com.ibm.ecm.extension.service;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.util.UserContext;
import com.filenet.api.util.Id;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.collection.ContentElementList;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.RefreshMode;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.json.java.JSONObject;
import com.ibm.json.java.JSONArray;

public class CntCotizacionesService extends PluginService {	
	
	@Override	
	public String getId() {
		return "CntCotizacionesService";
	}	
	
	@Override	
	public void execute(PluginServiceCallbacks callbacks, HttpServletRequest request, HttpServletResponse response) throws Exception {

		String methodName = request.getParameter("method");
		if (methodName == null)
			throw new Exception("No se ha incluido el nombre del método a invocar.");
		
		callbacks.getLogger().logEntry(this, methodName, request);
			
		try {
	
			JSONObject result = new JSONObject();
			if (methodName.equals("searchCotizaciones"))
				result = searchCotizaciones(request, callbacks);
			else if (methodName.equals("addCotizacion"))
				result = addCotizacion(request, callbacks);
			else if (methodName.equals("updateCotizacion"))
				result = updateCotizacion(request, callbacks);
			else if (methodName.equals("deleteCotizacion"))
				result = deleteCotizacion(request, callbacks);
			else if (methodName.equals("asociaCotizacionPDF"))
				result = asociaCotizacionPDF(request, callbacks);
			else if (methodName.equals("getNextFolioCotizacion"))
				result = getNextFolioCotizacion(request, callbacks);			
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
	
	private JSONObject searchCotizaciones(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get criterio
		    JSONObject criterio = JSONObject.parse(request.getParameter("criterio"));	
		    
		    // Maxresults
		    int maxResults = Integer.parseInt(request.getParameter("maxResults"));
			
		    // Search cotizaciones
			jsonArray = searchCotizaciones(objStore, criterio, maxResults);
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("cotizaciones", jsonArray);
		return jsonResponse;
	}
	
	private JSONObject addCotizacion(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("cotizacion"));
			
			// Tipo de cotizacion
			int tipo = Integer.parseInt(request.getParameter("tipo"));		
			
			// crea nueva cotizacion
			Document cotizacion = addCotizacion(objStore, jsonData, tipo, Integer.valueOf(1));
			
			// Response
			jsonData = getCotizacionJson(cotizacion);				
			
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
		jsonResponse.put("cotizacion", jsonData);
		return jsonResponse;
	}
	
	public static Document addCotizacion(ObjectStore objStore, JSONObject jsonData, int tipo, int estado) throws Exception {			
		
		// Get cliente
		Folder cliente = Factory.Folder.fetchInstance(objStore, new Id(jsonData.get("clienteId").toString()), null);
						
		// Obten instancia del folder raiz para cotizaciones
		Folder parentFolder = getCotizacionesFolder(objStore, cliente, new Date());
		
		// Si corresponde a una cotizacion final, se asegura que el parent folder sea el mismo que el de la cotizacion original (en caso de actualizar la cotizacion en un mes distinto en el cual fue orignalmente creada)
		if (tipo == 1) {
		    JSONObject criterio = new JSONObject();
		    // Search cotizacion original por referencia
		    criterio.put("nameequals", jsonData.get("name").toString());
		    criterio.put("tipo", 0);
		    JSONArray jsonArray = searchCotizaciones(objStore, criterio, 1);
		    if (!jsonArray.isEmpty()) {
		    	JSONObject jsonCotizacion = (JSONObject) jsonArray.get(0);
		    	Document original = Factory.Document.fetchInstance(objStore, new Id(jsonCotizacion.get("id").toString()), null);
		    	parentFolder = (Folder) original.get_FoldersFiledIn().iterator().next();
		    }
		}
			
		// Crea nueva cotizacion
		Document cotizacion = Factory.Document.createInstance(objStore, "CntCotizacion");
		cotizacion.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
		
		com.filenet.api.property.Properties props = cotizacion.getProperties();
		props.putObjectValue("DocumentTitle", jsonData.get("name").toString());
		props.putObjectValue("FechaCreacion", ServiceUtil.getUTFCalendar(new Date()).getTime());
		props.putObjectValue("Contenedor", jsonData.get("contenedor").toString().equals("") ? null : jsonData.get("contenedor").toString());
		props.putObjectValue("MontoTotal", Double.parseDouble(jsonData.get("monto").toString()));
		props.putObjectValue("Mercancia", jsonData.get("mercancia").toString().equals("") ? null : jsonData.get("mercancia").toString());
		props.putObjectValue("ClbJSONData", jsonData.serialize().getBytes());
		props.putObjectValue("TipoCotizacion", tipo);
		props.putObjectValue("EstadoCotizacion", estado);

		cotizacion.save(RefreshMode.REFRESH);
		
		// File fraccion
		ReferentialContainmentRelationship rel = parentFolder.file(cotizacion, AutoUniqueName.AUTO_UNIQUE, null, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
		rel.save(RefreshMode.NO_REFRESH);
		
		return cotizacion;
	}
	
	private JSONObject updateCotizacion(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("cotizacion"));

			// Tipo de cotizacion
			int tipo = Integer.parseInt(request.getParameter("tipo"));
			
			// Get cotizacion
			Document cotizacion = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);

			// Actualiza cotizacion
			com.filenet.api.property.Properties props = cotizacion.getProperties();
			props.putObjectValue("DocumentTitle", jsonData.get("name").toString());
			props.putObjectValue("FechaCreacion", ServiceUtil.getUTFCalendar(new Date()).getTime());
			props.putObjectValue("Contenedor", jsonData.get("contenedor").toString().equals("") ? null : jsonData.get("contenedor").toString());
			props.putObjectValue("MontoTotal", Double.parseDouble(jsonData.get("monto").toString()));
			props.putObjectValue("Mercancia", jsonData.get("mercancia").toString().equals("") ? null : jsonData.get("mercancia").toString());
			props.putObjectValue("ClbJSONData", jsonData.serialize().getBytes());
			props.putObjectValue("TipoCotizacion", tipo);
			props.putObjectValue("EstadoCotizacion", Integer.valueOf(1)); // realizada

			cotizacion.save(RefreshMode.REFRESH);
			
			// Mueve cotizacion en caso de cambiar cliente
			Folder clienteActual = getCotizacionCliente(cotizacion);
			Folder cliente = Factory.Folder.fetchInstance(objStore, new Id(jsonData.get("clienteId").toString()), null);
			actualizaCliente(objStore, cotizacion, clienteActual, cliente);

			// Contenedor asociado
			if (jsonData.get("contenedorasociado") != null) {
				// Si no corresponde a un Id valido, se trata de una solicitud para creacion de un nuevo contenedor asociado
				if (!Id.isId(jsonData.get("contenedorasociado").toString())) 
				{				
					JSONObject jsonContenedor = new JSONObject();
					jsonContenedor.put("name", jsonData.get("contenedorasociado"));
					jsonContenedor.put("fechabase", ServiceUtil.sdf.format(new Date()));
					jsonContenedor.put("cliente", jsonData.get("clienteId"));
					jsonContenedor.put("mercancia", jsonData.get("mercancia") == null ? "" : jsonData.get("mercancia"));
					cotizacion = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
					CntContenedoresService.addContenedor(objStore, jsonContenedor, cotizacion);
				}
				else
				{
					// ***** Sincronizacion de contenedor asociado *****
					Folder contenedor = Factory.Folder.fetchInstance(objStore, new Id(jsonData.get("contenedorasociado").toString()), null);
					sincronizaDatosContenedor(objStore, contenedor, clienteActual, cliente, jsonData);
				}
			
			}
						
			// Response
			cotizacion = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
			jsonData = getCotizacionJson(cotizacion);				
			
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
		jsonResponse.put("cotizacion", jsonData);
		return jsonResponse;
	}
	
	private static void sincronizaDatosContenedor(ObjectStore objStore, Folder contenedor, Folder clienteActual, Folder cliente, JSONObject jsonData) throws Exception {
		CntContenedoresService.actualizaCliente(objStore, contenedor, clienteActual, cliente);
		com.filenet.api.property.Properties props = contenedor.getProperties();
		props.putObjectValue("FolderName", jsonData.get("contenedor").toString());
		props.putObjectValue("Mercancia", jsonData.get("mercancia").toString().equals("") ? null : jsonData.get("mercancia").toString());
    	byte[] data = props.getBinaryValue("ClbJSONData");
    	JSONObject cotizacionJsonData = JSONObject.parse(new String(data));
    	cotizacionJsonData.put("name", jsonData.get("contenedor").toString());
    	cotizacionJsonData.put("cliente", cliente.get_Id().toString());
    	cotizacionJsonData.put("mercancia", jsonData.get("mercancia").toString().equals("") ? null : jsonData.get("mercancia").toString());
    	props.putObjectValue("ClbJSONData", cotizacionJsonData.serialize().getBytes());
    	contenedor.save(RefreshMode.REFRESH);
	}	
	
	public static void actualizaCliente(ObjectStore objStore, Document cotizacion, Folder clienteActual, Folder cliente) throws Exception {
		// Mueve cotizacion en caso de cambiar cliente
		if (!clienteActual.get_Id().equals(cliente.get_Id())) {
			com.filenet.api.property.Properties props = cotizacion.getProperties();
			// Get pdf
			Document pdf = null;
			if (props.getObjectValue("PDF") != null) pdf = (Document) props.getEngineObjectValue("PDF");				
			// Unfile de cliente anterior
			Folder oldParent = (Folder) cotizacion.get_FoldersFiledIn().iterator().next();
			ReferentialContainmentRelationship rel = oldParent.unfile(cotizacion);
			rel.save(RefreshMode.NO_REFRESH);
			if (pdf != null) {
				rel = oldParent.unfile(pdf);
				rel.save(RefreshMode.NO_REFRESH);					
			}
			ServiceUtil.deleteParentsRecursively(oldParent, clienteActual.get_FolderName());
			// File en cliente actualizado
			Folder newParent = getCotizacionesFolder(objStore, cliente, props.getDateTimeValue("DateCreated"));				
			rel = newParent.file(cotizacion, AutoUniqueName.AUTO_UNIQUE, null, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
			rel.save(RefreshMode.NO_REFRESH);
			if (pdf != null) {
				rel = newParent.file(pdf, AutoUniqueName.AUTO_UNIQUE, null, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
				rel.save(RefreshMode.NO_REFRESH);					
			}				
		}			
	}
	
	private JSONObject deleteCotizacion(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		JSONArray jsonArray = new JSONArray();

		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonArray = JSONArray.parse(request.getParameter("cotizaciones"));
			
			for (Object obj : jsonArray) {
				JSONObject jsonData = (JSONObject) obj;
				Document cotizacion = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
				Folder clienteActual = getCotizacionCliente(cotizacion);				
				Folder parent = (Folder) cotizacion.get_FoldersFiledIn().iterator().next();
				com.filenet.api.property.Properties props = cotizacion.getProperties();
				
				// Remueve PDF en caso de existir
				if (props.getObjectValue("PDF") != null) {
					Document pdf = (Document) props.getEngineObjectValue("PDF");
					pdf.delete();
					pdf.save(RefreshMode.REFRESH);					
				}
				
				// Remueve cotizacion
				cotizacion.delete();
				cotizacion.save(RefreshMode.REFRESH);
				ServiceUtil.deleteParentsRecursively(parent, clienteActual.get_FolderName());
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
	private JSONObject asociaCotizacionPDF(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get cotizacion
			Document cotizacion = Factory.Document.fetchInstance(objStore, new Id(request.getParameter("cotizacion")), null);
			com.filenet.api.property.Properties cotizacionProps = cotizacion.getProperties();
			
			// Get cotizacion parent
			Folder parentFolder = (Folder) cotizacion.get_FoldersFiledIn().iterator().next();
			
			// Get security folder
			Folder securityFolder = Factory.Folder.fetchInstance(objStore, "/Importaciones", null);
			
			// Get new pdf
			Document pdf = Factory.Document.fetchInstance(objStore, new Id(request.getParameter("pdf")), null);
	    	ContentElementList cel = pdf.get_ContentElements();
	    	ContentTransfer newPDFCT = (ContentTransfer)cel.get(0);			
			
			// Get old pdf
			Document oldPDF = null;
			if (cotizacionProps.getObjectValue("PDF") != null)
				oldPDF = (Document) cotizacionProps.getEngineObjectValue("PDF");
			
			// Crea cotizacion pdf
			Document cotizacionPDF = Factory.Document.createInstance(objStore, "CntCotizacionPDF");
			cotizacionPDF.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
			
			ContentElementList contentList = Factory.ContentElement.createList();
			ContentTransfer contentTransfer = Factory.ContentTransfer.createInstance();
			contentTransfer.setCaptureSource(newPDFCT.accessContentStream());
			contentTransfer.set_RetrievalName(newPDFCT.get_RetrievalName());
			contentTransfer.set_ContentType(newPDFCT.get_ContentType());
			contentList.add(contentTransfer);
			cotizacionPDF.set_ContentElements(contentList);			
			
			com.filenet.api.property.Properties props = cotizacionPDF.getProperties();
			props.putObjectValue("DocumentTitle", cotizacionProps.getObjectValue("DocumentTitle"));
			props.putObjectValue("FechaCreacion", cotizacionProps.getObjectValue("FechaCreacion"));
			props.putObjectValue("Contenedor", cotizacionProps.getObjectValue("Contenedor"));
			props.putObjectValue("MontoTotal", cotizacionProps.getObjectValue("MontoTotal"));
			props.putObjectValue("ClbJSONData", cotizacionProps.getObjectValue("ClbJSONData"));
			props.putObjectValue("TipoCotizacion", cotizacionProps.getObjectValue("TipoCotizacion"));

			cotizacionPDF.set_MimeType(pdf.get_MimeType());
			cotizacionPDF.set_SecurityFolder(securityFolder); // inherit permissions from security folder
			cotizacionPDF.save(RefreshMode.REFRESH);
			
			// File new pdf
			ReferentialContainmentRelationship rel = parentFolder.file(cotizacionPDF, AutoUniqueName.AUTO_UNIQUE, null, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
			rel.save(RefreshMode.NO_REFRESH);	
			
			// Asocia pdf a cotizacion
			cotizacionProps.putObjectValue("PDF", cotizacionPDF);
			cotizacion.save(RefreshMode.REFRESH);
			
			// Remueve pdf anterior
			if (oldPDF != null) {
				oldPDF.delete();
				oldPDF.save(RefreshMode.REFRESH);
			}
			
			// Response
			jsonData = getCotizacionJson(cotizacion);				
			
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
		jsonResponse.put("cotizacion", jsonData);
		return jsonResponse;
	}	
	
	private JSONObject getNextFolioCotizacion(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		String folio = null;
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get referencia cotizacion
			folio = getNextReferencia(objStore);
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("folio", folio);
		return jsonResponse;
	}
	
	public static String getNextReferencia(ObjectStore objStore) throws Exception {
		// Get next consuecutivo cotizacion
		int consecutivo = SettingsService.getNextConsecutivoCotizacion(objStore);
		String referencia = String.format("%09d", consecutivo); // nine-digit string
		referencia = new StringBuilder(referencia).insert(6, ",").insert(3, ",").toString(); // set referencia format as ###,###,###
		return referencia;
	}
	
	private static JSONObject getCotizacionJson(Document cotizacion) throws Exception {		
    	// Get cliente de la cotizacion
    	Folder cliente = getCotizacionCliente(cotizacion);
    	// Get Data
    	com.filenet.api.property.Properties props = cotizacion.getProperties();
    	byte[] data = props.getBinaryValue("ClbJSONData");
    	JSONObject jsonData = JSONObject.parse(new String(data));
    	// Properties
    	JSONObject jsonObject = new JSONObject();
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", ServiceUtil.sdf.format(ServiceUtil.getUTFCalendar(props.getDateTimeValue("FechaCreacion")).getTime()));
    	jsonObject.put("name", props.getStringValue("DocumentTitle"));
    	jsonObject.put("contenedor", props.getStringValue("Contenedor"));
    	jsonObject.put("mercancia", props.getStringValue("Mercancia"));
    	jsonObject.put("monto", props.getFloat64Value("MontoTotal"));
    	jsonObject.put("cliente", cliente.get_FolderName());
    	jsonObject.put("clienteId", cliente.get_Id().toString());
    	jsonObject.put("tipo", props.getInteger32Value("TipoCotizacion"));
    	jsonObject.put("pdf", props.getObjectValue("PDF") != null ? ((Document)props.getEngineObjectValue("PDF")).get_Id().toString() : null);
    	jsonObject.put("datos", jsonData.toString());
    	jsonObject.put("estado", props.getInteger32Value("EstadoCotizacion"));
    	jsonObject.put("contenedorobj", props.getObjectValue("ContenedorA") != null ? ((Folder)props.getEngineObjectValue("ContenedorA")).get_Id().toString() : null);
    	return jsonObject;
	}		
	
	private static Folder getCotizacionCliente(Document cotizacion) throws Exception {
		Folder fol = null;
		FolderSet folSet = cotizacion.get_FoldersFiledIn();
		if (!folSet.isEmpty()) {
			fol = (Folder) folSet.iterator().next();
			String className = "";
			while (fol != null && !className.equals("CntCliente")) {
				fol = fol.get_Parent();
				if (fol != null)
					className = fol.getClassName();
			}
		}
		return fol;
	}
	
	private static Folder getCotizacionesFolder(ObjectStore os, Folder cliente, Date date) throws Exception {
		String[] meses = new String[]{"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		Folder parent = Factory.Folder.fetchInstance(os, "/Importaciones/" + cliente.get_FolderName(), null);
		Folder cotizacionesFolder = ServiceUtil.getFolder(os, parent, "Cotizaciones", "Folder");
		Folder anioFolder = ServiceUtil.getFolder(os, cotizacionesFolder, Integer.toString(cal.get(Calendar.YEAR)), "Folder");
		Folder mesFolder = ServiceUtil.getFolder(os, anioFolder, meses[cal.get(Calendar.MONTH)], "Folder");
		
		return mesFolder;
	}		
	
	@SuppressWarnings("unchecked")
	public static JSONArray searchCotizaciones(ObjectStore objStore, JSONObject criterio, int maxResults) throws Exception {
			
		JSONArray jsonArray = new JSONArray();
		
	    SearchScope search = new SearchScope(objStore);
	    SearchSQL sql = new SearchSQL();
	    sql.setSelectList("This, Id, FoldersFiledIn, FechaCreacion, DocumentTitle, Contenedor, MontoTotal, PDF, ClbJSONData, Mercancia, TipoCotizacion, EstadoCotizacion, ContenedorA");
	    sql.setFromClauseInitialValue("CntCotizacion", null, false);			
		
	    // Build where statement
	    StringBuffer whereStatement = new StringBuffer();
	    whereStatement.append("isCurrentVersion = TRUE");
	    if (criterio.get("id") != null)
	    	whereStatement.append(" AND Id = " + criterio.get("id").toString());
	    if (criterio.get("nameequals") != null)
	    	whereStatement.append(" AND DocumentTitle = '" + criterio.get("nameequals").toString() + "'");	    
	    if (criterio.get("namecontains") != null)
	    	whereStatement.append(" AND DocumentTitle LIKE '%" + criterio.get("namecontains").toString() + "%'");
	    if (criterio.get("contenedor") != null)
	    	whereStatement.append(" AND Contenedor LIKE '%" + criterio.get("contenedor").toString() + "%'");
	    if (criterio.get("mercancia") != null)
	    	whereStatement.append(" AND Mercancia LIKE '%" + criterio.get("mercancia").toString() + "%'");	    
	    if (criterio.get("datecreateddesde") != null)
	    	whereStatement.append(" AND FechaCreacion >= " + ServiceUtil.convertLocalTimeToUTC(ServiceUtil.ldf.parse(criterio.get("datecreateddesde").toString() + " 00:00:00")));
	    if (criterio.get("datecreatedhasta") != null)
	    	whereStatement.append(" AND FechaCreacion <= " + ServiceUtil.convertLocalTimeToUTC(ServiceUtil.ldf.parse(criterio.get("datecreatedhasta").toString() + " 23:59:59")));	    
	    if (criterio.get("montodesde") != null)
		    whereStatement.append(" AND MontoTotal >= " + criterio.get("montodesde").toString());
	    if (criterio.get("montohasta") != null)
		    whereStatement.append(" AND MontoTotal <= " + criterio.get("montohasta").toString());
	    if (criterio.get("tipo") != null)
		    whereStatement.append(" AND TipoCotizacion = " + criterio.get("tipo").toString());	    
	    if (criterio.get("cliente") != null) {
	    	Folder cliente = Factory.Folder.fetchInstance(objStore, new Id(criterio.get("cliente").toString()), null);
		    whereStatement.append(" AND This INSUBFOLDER '/Importaciones/" + cliente.get_FolderName() + "'");
	    }
	    if (criterio.get("estado") != null)
		    whereStatement.append(" AND EstadoCotizacion = " + criterio.get("estado").toString());	    
	    if (criterio.get("contenedorobj") != null) {
	    	if (Id.isId(criterio.get("contenedorobj").toString()))
	    		whereStatement.append(" AND ContenedorA = " + criterio.get("contenedorobj").toString());
	    	else
	    		whereStatement.append(" AND ContenedorA " + (Boolean.parseBoolean(criterio.get("contenedorobj").toString()) ? "IS NOT NULL" : "IS NULL"));
	    }
	    
	    sql.setWhereClause(whereStatement.toString());
	    if (maxResults > 0)
	    	sql.setMaxRecords(maxResults);
	    sql.setOrderByClause("FechaCreacion DESC");
	    
	    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, null, null, true);
	    for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
	    {
	    	Document doc = it.next();
	    	JSONObject jsonObject = getCotizacionJson(doc);
			// add element
			jsonArray.add(jsonObject);					
	    }
		    
		return jsonArray;
	}		
	
}
