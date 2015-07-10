package com.ibm.ecm.extension.service;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.upload.FormFile;

import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.util.UserContext;
import com.filenet.api.util.Id;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.collection.ContentElementList;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.RefreshMode;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.json.java.JSONObject;
import com.ibm.json.java.JSONArray;

public class CntContenedoresService extends PluginService {	
	
	@Override	
	public String getId() {
		return "CntContenedoresService";
	}	
	
	@Override	
	public void execute(PluginServiceCallbacks callbacks, HttpServletRequest request, HttpServletResponse response) throws Exception {

		String methodName = request.getParameter("method");
		if (methodName == null)
			throw new Exception("No se ha incluido el nombre del método a invocar.");
		
		callbacks.getLogger().logEntry(this, methodName, request);
			
		try {
	
			JSONObject result = new JSONObject();
			if (methodName.equals("searchContenedores"))
				result = searchContenedores(request, callbacks);
			else if (methodName.equals("addContenedor"))
				result = addContenedor(request, callbacks);
			else if (methodName.equals("updateContenedor"))
				result = updateContenedor(request, callbacks);
			else if (methodName.equals("deleteContenedor"))
				result = deleteContenedor(request, callbacks);
			else if (methodName.equals("addDocumento"))
				result = addDocumento(request, callbacks);
			else if (methodName.equals("updateDocumento"))
				result = updateDocumento(request, callbacks);			
			else if (methodName.equals("deleteDocumento"))
				result = deleteDocumento(request, callbacks);			
			else if (methodName.equals("searchDocumentos"))
				result = searchDocumentos(request, callbacks);
			else if (methodName.equals("asociaDocumento"))
				result = asociaDocumento(request, callbacks);
			else if (methodName.equals("fetchDocumentoEditable"))
				result = fetchDocumentoEditable(request, callbacks);			
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
	
	private JSONObject searchContenedores(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
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
			jsonArray = searchContenedores(objStore, criterio, maxResults);
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("contenedores", jsonArray);
		return jsonResponse;
	}
	
	private JSONObject addContenedor(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("contenedor"));
			
			// Crea nueva cotizacion asociada
			JSONObject cotizacionData = new JSONObject();
			cotizacionData.put("clienteId", jsonData.get("cliente"));
			cotizacionData.put("name", CntCotizacionesService.getNextReferencia(objStore));
			cotizacionData.put("contenedor", jsonData.get("name"));
			cotizacionData.put("monto", Double.valueOf(0));
			cotizacionData.put("mercancia", jsonData.get("mercancia") == null ? "" : jsonData.get("mercancia"));
			Document cotizacion = CntCotizacionesService.addCotizacion(objStore, cotizacionData, Integer.valueOf(0), Integer.valueOf(0));
			
			// Crea nuevo contenedor
			Folder contenedor = addContenedor(objStore, jsonData, cotizacion);
						
			// Response
			jsonData = getContenedorJson(contenedor);
			
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
		jsonResponse.put("contenedor", jsonData);
		return jsonResponse;
	}
	
	public static Folder addContenedor(ObjectStore objStore, JSONObject jsonData, Document cotizacion) throws Exception {
		// Get cliente
		Folder cliente = Factory.Folder.fetchInstance(objStore, new Id(jsonData.get("cliente").toString()), null);
				
		// Get naviera
		Document naviera = null; 
		if (jsonData.get("naviera") != null)
			naviera = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("naviera").toString()), null);
		
		// Get forwarder
		Document forwarder = null; 
		if (jsonData.get("forwarder") != null)
			forwarder = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("forwarder").toString()), null);			
						
		// Obten instancia del folder raiz para contenedores
		Folder parentFolder = getContenedoresFolder(objStore, cliente, new Date());				
			
		// Crea nuevo contenedor
		Folder contenedor = Factory.Folder.createInstance(objStore, "CntContenedor");
		contenedor.set_FolderName(jsonData.get("name").toString());
		contenedor.set_Parent(parentFolder);
		contenedor.set_InheritParentPermissions(Boolean.TRUE); // inherit permissions from parent folder
		
		com.filenet.api.property.Properties props = contenedor.getProperties();
		props.putObjectValue("FechaBase", ServiceUtil.getUTFCalendar(ServiceUtil.sdf.parse(jsonData.get("fechabase").toString())).getTime());
		props.putObjectValue("Pedimento", jsonData.get("pedimento") != null ? jsonData.get("pedimento").toString() : null);
		props.putObjectValue("Naviera", naviera);
		props.putObjectValue("Forwarder", forwarder);
		props.putObjectValue("EstadoContenedor", Integer.valueOf(0));
		props.putObjectValue("Semaforo", Integer.valueOf(0));
		props.putObjectValue("Mercancia", jsonData.get("mercancia").toString().equals("") ? null : jsonData.get("mercancia").toString());
		props.putObjectValue("ClbJSONData", jsonData.serialize().getBytes());
		props.putObjectValue("Cotizacion", cotizacion);

		// Save
		contenedor.save(RefreshMode.REFRESH);
		
		// Set contenedor en cotizacion asociada
		props = cotizacion.getProperties();
		props.putObjectValue("ContenedorA", contenedor);
		cotizacion.save(RefreshMode.REFRESH);
					
		return contenedor;
	}
	
	private JSONObject updateContenedor(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonData = JSONObject.parse(request.getParameter("contenedor"));

			// Get contenedor
			Folder contenedor = Factory.Folder.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
			com.filenet.api.property.Properties props = contenedor.getProperties();
			
			// Get cotizacion actualmente asociada
			Document cotizacionActual = null;
			if (props.getObjectValue("Cotizacion") != null)
				cotizacionActual = (Document) props.getEngineObjectValue("Cotizacion");
			
			// Get cotizacion asociada
			Document cotizacion = null;
			if (jsonData.get("cotizacion") != null)
				cotizacion = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("cotizacion").toString()), null);
			
			// Get naviera
			Document naviera = null; 
			if (jsonData.get("naviera") != null)
				naviera = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("naviera").toString()), null);
			
			// Get forwarder
			Document forwarder = null; 
			if (jsonData.get("forwarder") != null)
				forwarder = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("forwarder").toString()), null);
			
			// Get proveedor
			Document proveedor = null; 
			if (jsonData.get("proveedor") != null)
				proveedor = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("proveedor").toString()), null);
			
			// Get importadora
			Document importadora = null; 
			if (jsonData.get("importadora") != null)
				importadora = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("importadora").toString()), null);
			
			// Get puerto llegada
			Document puertollegada = null; 
			if (jsonData.get("puertollegada") != null)
				puertollegada = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("puertollegada").toString()), null);
			
			// Get puerto salida
			Document puertosalida = null; 
			if (jsonData.get("puertosalida") != null)
				puertosalida = Factory.Document.fetchInstance(objStore, new Id(jsonData.get("puertosalida").toString()), null);			
			
			// Get cliente
			Folder clienteActual = getContenedorCliente(contenedor);
			Folder cliente = Factory.Folder.fetchInstance(objStore, new Id(jsonData.get("cliente").toString()), null);			
			
			// Actualiza contenedor
			contenedor.set_FolderName(jsonData.get("name").toString());
			props.putObjectValue("FechaBase", ServiceUtil.getUTFCalendar(ServiceUtil.sdf.parse(jsonData.get("fechabase").toString())).getTime());
			props.putObjectValue("Pedimento", jsonData.get("pedimento") != null ? jsonData.get("pedimento").toString() : null);
			props.putObjectValue("Naviera", naviera);
			props.putObjectValue("Forwarder", forwarder);
			props.putObjectValue("EstadoContenedor", Integer.parseInt(jsonData.get("estado").toString()));
			props.putObjectValue("Semaforo",  Integer.parseInt(jsonData.get("semaforo").toString()));
			props.putObjectValue("Mercancia", jsonData.get("mercancia").toString().equals("") ? null : jsonData.get("mercancia").toString());
			props.putObjectValue("Proveedor", proveedor);
			props.putObjectValue("Importadora", importadora);
			props.putObjectValue("PuertoLlegada", puertollegada);
			props.putObjectValue("PuertoSalida", puertosalida);
			props.putObjectValue("ClbJSONData", jsonData.serialize().getBytes());	
			props.putObjectValue("Cotizacion", cotizacion);

			// Save
			contenedor.save(RefreshMode.REFRESH);
			
			// Actualiza contendedor asociado en cotizaciones en caso de cambiar
			if ((cotizacionActual == null && cotizacion != null) || (cotizacionActual != null && cotizacion != null && !cotizacionActual.get_Id().equals(cotizacion.get_Id()))) {
				// remueve contenedor de cotizacion anterior
				if (cotizacionActual != null) {
					props = cotizacionActual.getProperties();
					props.putObjectValue("ContenedorA", null);
					cotizacionActual.save(RefreshMode.REFRESH);
				}
				// establece contenedor en nueva cotizacion
				if (cotizacion != null) {
					props = cotizacion.getProperties();
					props.putObjectValue("ContenedorA", contenedor);
					cotizacion.save(RefreshMode.REFRESH);
				}				
			}
									
			// Mueve contenedor en caso de cambiar cliente
			actualizaCliente(objStore, contenedor, clienteActual, cliente);
			
			// ***** Sincronizacion de cotizacion asociada *****
			if (cotizacion != null) {
				// Sincroniza datos de cotizacion original asociada
				sincronizaDatosCotizacion(objStore, cotizacion, clienteActual, cliente, jsonData);
		    	// Sincroniza datos de cotizacion final asociada
			    JSONObject criterio = new JSONObject();
			    criterio.put("nameequals", cotizacion.get_Name());
			    criterio.put("tipo", Integer.valueOf(1)); // cotizacion final
				JSONArray jsonArray = CntCotizacionesService.searchCotizaciones(objStore, criterio, Integer.valueOf(1));	
				if (jsonArray.size() > 0) {
					JSONObject jsonCotizacion = (JSONObject) jsonArray.get(0);
					cotizacion = Factory.Document.fetchInstance(objStore, new Id(jsonCotizacion.get("id").toString()), null);
					sincronizaDatosCotizacion(objStore, cotizacion, clienteActual, cliente, jsonData);
				}	
			}
						
			// Response
			jsonData = getContenedorJson(contenedor);				
			
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
		jsonResponse.put("contenedor", jsonData);
		return jsonResponse;
	}
	
	public static void actualizaCliente(ObjectStore objStore, Folder contenedor, Folder clienteActual, Folder cliente) throws Exception {
		// Mueve contenedor en caso de cambiar cliente
		if (!clienteActual.get_Id().equals(cliente.get_Id())) {	
			Folder oldParent = contenedor.get_Parent();
			// Obten instancia del folder raiz para contenedores
			Folder parentFolder = getContenedoresFolder(objStore, cliente, contenedor.getProperties().getDateTimeValue("DateCreated"));		
			contenedor.set_Parent(parentFolder);
			contenedor.set_InheritParentPermissions(Boolean.TRUE); // inherit permissions from parent folder
			contenedor.save(RefreshMode.REFRESH);	
			// Elimina la estructura de folders sin uso del cliente anteriormente asociado
			ServiceUtil.deleteParentsRecursively(oldParent, clienteActual.get_FolderName());
		}		
	}
	
	private static void sincronizaDatosCotizacion(ObjectStore objStore, Document cotizacion, Folder clienteActual, Folder cliente, JSONObject jsonData) throws Exception {
		CntCotizacionesService.actualizaCliente(objStore, cotizacion, clienteActual, cliente);
		com.filenet.api.property.Properties props = cotizacion.getProperties();
		props.putObjectValue("Contenedor", jsonData.get("name").toString());
		props.putObjectValue("Mercancia", jsonData.get("mercancia").toString().equals("") ? null : jsonData.get("mercancia").toString());
    	byte[] data = props.getBinaryValue("ClbJSONData");
    	JSONObject cotizacionJsonData = JSONObject.parse(new String(data));
    	cotizacionJsonData.put("contenedor", jsonData.get("name").toString());
    	cotizacionJsonData.put("clienteId", cliente.get_Id().toString());
    	cotizacionJsonData.put("mercancia", jsonData.get("mercancia").toString().equals("") ? null : jsonData.get("mercancia").toString());
    	props.putObjectValue("ClbJSONData", cotizacionJsonData.serialize().getBytes());
    	cotizacion.save(RefreshMode.REFRESH);
	}
	
	private JSONObject deleteContenedor(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		JSONArray jsonArray = new JSONArray();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get datos
			jsonArray = JSONArray.parse(request.getParameter("contenedores"));
			
			for (Object obj : jsonArray) {
				JSONObject jsonData = (JSONObject) obj;
				Folder contenedor = Factory.Folder.fetchInstance(objStore, new Id(jsonData.get("id").toString()), null);
				Folder parent = contenedor.get_Parent();
				Folder clienteActual = getContenedorCliente(contenedor);	
				ServiceUtil.deleteRecursively(contenedor);
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

	private JSONObject asociaDocumento(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		String error = null;
		JSONObject jsonDoc = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore os = ServiceUtil.getP8Connection(request, callbacks);
			
			// Fetch contenedor
			Folder contenedor = Factory.Folder.fetchInstance(os, new Id(request.getParameter("contenedor")), null);
			
			// Fetch documento fuente
			Document doc = Factory.Document.fetchInstance(os, new Id(request.getParameter("documento")), null);
			
			// En caso de corresponder a un document editable inlcuira el PDF asociado
			Document pdf = null;
			if (request.getParameter("pdf") != null)
				pdf = Factory.Document.fetchInstance(os, new Id(request.getParameter("pdf")), null);
			
			// Asocia documento
			doc = createDocumento(os, contenedor, doc.get_Name(), doc.accessContentStream(0), doc.get_MimeType(), Integer.parseInt(request.getParameter("tipo")), pdf);
			jsonDoc = CntContenedoresService.getDocumentoJson(doc);			
			
		} catch (Exception e) {
			error = e.getMessage();
			jsonDoc = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("documento", jsonDoc);
		return jsonResponse;				
	}
	
	@SuppressWarnings("rawtypes")
	private JSONObject addDocumento(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {

		String error = null;
		JSONObject jsonDoc = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore os = ServiceUtil.getP8Connection(request, callbacks);		
						
			// Get form file params
			FormFile uploadFile = callbacks.getRequestUploadFile();
    		String docName = request.getParameter("parm_part_filename");
    		String mimeType = request.getParameter("mimetype");
    		long maxFileSize = -1;
    		if (request.getParameter("max_file_size") != null)
    			maxFileSize = Long.parseLong(request.getParameter("max_file_size"));
    	    
			if (uploadFile == null) {
				ActionForm form = callbacks.getRequestActionForm();
				if (form != null && form.getMultipartRequestHandler() != null) {
					Map fileElements = form.getMultipartRequestHandler().getFileElements();
					Iterator it = fileElements.values().iterator();
					while (it.hasNext()) {
						uploadFile = (FormFile) it.next();
						if (uploadFile != null)
							break;
					}
				}
			} 
			
			InputStream is;
			int contentLen;			
			
			if (uploadFile != null) {
				is = uploadFile.getInputStream();
				contentLen = uploadFile.getFileSize();
			} else {
				is = request.getInputStream();
				contentLen = request.getContentLength();
			}
			
			if (maxFileSize > 0 && contentLen > maxFileSize)
				throw new Exception("El tamaño del archivo es mayor al permitido");
			
			if (is != null) {
				Folder contenedor = Factory.Folder.fetchInstance(os, new Id(request.getParameter("contenedor")), null);
				Document doc = createDocumento(os, contenedor, docName, is, mimeType, Integer.parseInt(request.getParameter("tipo")), null);
				jsonDoc = CntContenedoresService.getDocumentoJson(doc);
			}
			
		} catch (Exception e) {
			error = e.getMessage();
			jsonDoc = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("documento", jsonDoc);
		return jsonResponse;				
	}
	
	@SuppressWarnings("unchecked")
	private Document createDocumento(ObjectStore os, Folder contenedor, String docName, InputStream is, String mimeType, int tipo, Document pdf) throws Exception {
		Document doc = Factory.Document.createInstance(os, pdf != null  ? "CntEditable" : "CntDocumento");
		
		ContentElementList contentList = Factory.ContentElement.createList();
		ContentTransfer contentTransfer = Factory.ContentTransfer.createInstance();
		contentTransfer.setCaptureSource(is);
		contentTransfer.set_RetrievalName(docName);
		contentTransfer.set_ContentType(mimeType);
		contentList.add(contentTransfer);
		doc.set_ContentElements(contentList);
		
		doc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
		
		com.filenet.api.property.Properties props = doc.getProperties();
		props.putObjectValue("DocumentTitle", docName);
		props.putObjectValue("TipoDocumento",  tipo);
		if (pdf != null) props.putObjectValue("PDF", pdf);

		doc.set_MimeType(mimeType);	
		doc.set_SecurityFolder(contenedor); // inherit permissions from parent folder
		doc.save(RefreshMode.REFRESH);	
		
		ReferentialContainmentRelationship rel = contenedor.file(doc, AutoUniqueName.AUTO_UNIQUE, null, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
		rel.save(RefreshMode.NO_REFRESH);
		
		return doc;
	}
	
	private JSONObject updateDocumento(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {

		String error = null;
		JSONObject jsonDoc = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore os = ServiceUtil.getP8Connection(request, callbacks);		
			
			// Get datos
			jsonDoc = JSONObject.parse(request.getParameter("documento"));
			
			// Get documento
			Document doc = Factory.Document.fetchInstance(os, new Id(jsonDoc.get("id").toString()), null);
			com.filenet.api.property.Properties properties = doc.getProperties();
			
			// Update props
			properties.putValue("TipoDocumento",  Integer.parseInt(jsonDoc.get("tipo").toString()));
			doc.save(RefreshMode.REFRESH);
			
			// Return
			jsonDoc = CntContenedoresService.getDocumentoJson(doc);
						
		} catch (Exception e) {
			error = e.getMessage();
			jsonDoc = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("documento", jsonDoc);
		return jsonResponse;						
	}	
	
	private JSONObject deleteDocumento(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {

		String error = null;
		JSONArray jsonArray = new JSONArray();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore os = ServiceUtil.getP8Connection(request, callbacks);		
			
			// Get datos
			jsonArray = JSONArray.parse(request.getParameter("documentos"));
			
			for (Object obj : jsonArray) {
				JSONObject jsonData = (JSONObject) obj;
				Document doc = Factory.Document.fetchInstance(os, new Id(jsonData.get("id").toString()), null);
				// Elimina documento editable asociado en caso de existir
				Document editable = fetchDocumentoEditable(os, doc);
				if (editable != null) {
					editable.delete();
					editable.save(RefreshMode.REFRESH);					
				}
				// Elimina documento fuente
				doc.delete();
				doc.save(RefreshMode.REFRESH);
			}				
						
		} catch (Exception e) {
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
	
	private JSONObject searchDocumentos(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		JSONArray jsonArray = new JSONArray();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get criterio
		    JSONObject criterio = JSONObject.parse(request.getParameter("criterio"));	
		    
		    // Maxresults
		    int maxResults = Integer.parseInt(request.getParameter("maxResults"));
			
			// Get documentos asociados a contenedor
			jsonArray = searchDocumentos(objStore, criterio, maxResults);			
			
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
		jsonResponse.put("documentos", jsonArray);
		return jsonResponse;			
	}	
	
	private static JSONObject getContenedorJson(Folder contenedor) throws Exception {		
    	// Get cliente del contenedor
    	Folder cliente = getContenedorCliente(contenedor);
    	// Get Data
    	com.filenet.api.property.Properties props = contenedor.getProperties();
    	byte[] data = props.getBinaryValue("ClbJSONData");
    	JSONObject jsonData = JSONObject.parse(new String(data));
    	// Properties
    	JSONObject jsonObject = new JSONObject();
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", ServiceUtil.sdf.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("FolderName"));
    	jsonObject.put("fechabase", ServiceUtil.sdf.format(ServiceUtil.getUTFCalendar(props.getDateTimeValue("FechaBase")).getTime()));
    	jsonObject.put("pedimento", props.getObjectValue("Pedimento") != null ? props.getStringValue("Pedimento") : null);
    	jsonObject.put("cliente", cliente.get_FolderName());
    	jsonObject.put("clienteId", cliente.get_Id().toString());
    	if (props.getObjectValue("Naviera") != null) {
    		Document pedimento = (Document) props.getEngineObjectValue("Naviera");
        	jsonObject.put("naviera", pedimento.get_Name());
        	jsonObject.put("navieraId", pedimento.get_Id().toString());
    	} else {
        	jsonObject.put("naviera", null);
        	jsonObject.put("navieraId", null);    		
    	}
    	if (props.getObjectValue("Forwarder") != null) {
    		Document forwarder = (Document) props.getEngineObjectValue("Forwarder");
        	jsonObject.put("forwarder", forwarder.get_Name());
        	jsonObject.put("forwarderId", forwarder.get_Id().toString());
    	} else {
        	jsonObject.put("forwarder", null);
        	jsonObject.put("forwarderId", null);    		
    	}    	
    	jsonObject.put("mercancia", props.getStringValue("Mercancia"));
    	jsonObject.put("estado", props.getInteger32Value("EstadoContenedor"));
    	jsonObject.put("semaforo", props.getInteger32Value("Semaforo"));
    	jsonObject.put("datos", jsonData.toString());
    	jsonObject.put("cotizacion", props.getObjectValue("Cotizacion") != null ? ((Document)props.getEngineObjectValue("Cotizacion")).get_Id().toString() : null);
    	jsonObject.put("proveedor", props.getObjectValue("Proveedor") != null ? ((Document)props.getEngineObjectValue("Proveedor")).get_Id().toString() : null);
    	jsonObject.put("importadora", props.getObjectValue("Importadora") != null ? ((Document)props.getEngineObjectValue("Importadora")).get_Id().toString() : null);
    	jsonObject.put("puertollegada", props.getObjectValue("PuertoLlegada") != null ? ((Document)props.getEngineObjectValue("PuertoLlegada")).get_Id().toString() : null);
    	jsonObject.put("puertosalida", props.getObjectValue("PuertoSalida") != null ? ((Document)props.getEngineObjectValue("PuertoSalida")).get_Id().toString() : null);
    	return jsonObject;
	}
	
	private static Folder getContenedorCliente(Folder contenedor) throws Exception {
		Folder fol = contenedor;
		String className = "";
		while (fol != null && !className.equals("CntCliente")) {
			fol = fol.get_Parent();
			if (fol != null)
				className = fol.getClassName();
		}
		return fol;		
	}		
	
	private static Folder getContenedoresFolder(ObjectStore os, Folder cliente, Date date) throws Exception {
		String[] meses = new String[]{"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		Folder parent = Factory.Folder.fetchInstance(os, "/Importaciones/" + cliente.get_FolderName(), null);
		Folder contenedoresFolder = ServiceUtil.getFolder(os, parent, "Contenedores", "Folder");
		Folder anioFolder = ServiceUtil.getFolder(os, contenedoresFolder, Integer.toString(cal.get(Calendar.YEAR)), "Folder");
		Folder mesFolder = ServiceUtil.getFolder(os, anioFolder, meses[cal.get(Calendar.MONTH)], "Folder");
		
		return mesFolder;
	}
	
	@SuppressWarnings("unchecked")
	private static JSONArray searchDocumentos(ObjectStore objStore, JSONObject criterio, int maxResults) throws Exception {
		JSONArray jsonArray = new JSONArray();
		
		// Get contenedor
		Folder contenedor = Factory.Folder.fetchInstance(objStore, new Id(criterio.get("contenedor").toString()), null);
		
	    SearchScope search = new SearchScope(objStore);
	    SearchSQL sql = new SearchSQL();
	    sql.setSelectList("This, Id, DocumentTitle, DateCreated, TipoDocumento");
	    sql.setFromClauseInitialValue("CntDocumento", null, false);
	    
	    // Build where statement
	    StringBuffer whereStatement = new StringBuffer();
	    whereStatement.append("isCurrentVersion = TRUE AND This INFOLDER '" + contenedor.get_PathName() + "'");	    
	    if (criterio.get("tipo") != null)
	    	whereStatement.append(" AND TipoDocumento = " + criterio.get("tipo").toString());
	    
	    sql.setWhereClause(whereStatement.toString());
	    if (maxResults > 0)
	    	sql.setMaxRecords(maxResults);
	    
	    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, null, null, true);
	    for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
	    {
	    	Document doc = it.next();
	    	JSONObject jsonObject = getDocumentoJson(doc);
			// add element
			jsonArray.add(jsonObject);	
	    }
		
		return jsonArray;
	}
	
	private JSONObject fetchDocumentoEditable(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonDocumento = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get documento
		    Document doc = Factory.Document.fetchInstance(objStore, new Id(request.getParameter("documento")), null);
		    
		    // Localiz documento editable asociado
		    Document editable = fetchDocumentoEditable(objStore, doc);
		    
		    if (editable != null)
		    	jsonDocumento = getDocumentoJson(editable);
		    else
		    	jsonDocumento = getDocumentoJson(doc);
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonDocumento = new JSONObject();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("documento", jsonDocumento);
		return jsonResponse;
	}	
	
	private static Document fetchDocumentoEditable(ObjectStore os, Document doc) throws Exception {
	    SearchScope search = new SearchScope(os);
	    SearchSQL sql = new SearchSQL();
	    sql.setSelectList("Id, DocumentTitle, DateCreated, TipoDocumento");
	    sql.setFromClauseInitialValue("CntEditable", null, false);
	    sql.setWhereClause("isCurrentVersion = TRUE AND PDF = " + doc.get_Id());
	    sql.setMaxRecords(1);
	    
	    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, null, null, true);
	    if (docSet.isEmpty())
	    	return null;
	    
	    return (Document) docSet.iterator().next();
	}		
	
	@SuppressWarnings("unchecked")
	public static JSONArray searchContenedores(ObjectStore objStore, JSONObject criterio, int maxResults) throws Exception {
			
		JSONArray jsonArray = new JSONArray();
		
	    SearchScope search = new SearchScope(objStore);
	    SearchSQL sql = new SearchSQL();
	    sql.setSelectList("This, Id, Parent, DateCreated, FolderName, FechaBase, Pedimento, Naviera, Forwarder, EstadoContenedor, Semaforo, ClbJSONData, Mercancia, Cotizacion, Proveedor, Importadora, PuertoLlegada, PuertoSalida");
	    sql.setFromClauseInitialValue("CntContenedor", null, false);			
		
	    // Build where statement
	    StringBuffer whereStatement = new StringBuffer();
	    whereStatement.append("FolderName IS NOT NULL");
	    if (criterio.get("id") != null)
	    	whereStatement.append(" AND Id = " + criterio.get("id").toString());
	    if (criterio.get("exactname") != null)
	    	whereStatement.append(" AND FolderName = '" + criterio.get("exactname").toString() + "'");	    
	    if (criterio.get("name") != null)
	    	whereStatement.append(" AND FolderName LIKE '%" + criterio.get("name").toString() + "%'");
	    if (criterio.get("exactpedimento") != null)
	    	whereStatement.append(" AND Pedimento = '" + criterio.get("exactpedimento").toString() + "'");	    	    
	    if (criterio.get("pedimento") != null)
	    	whereStatement.append(" AND Pedimento LIKE '%" + criterio.get("name").toString() + "%'");
	    if (criterio.get("mercancia") != null)
	    	whereStatement.append(" AND Mercancia LIKE '%" + criterio.get("mercancia").toString() + "%'");	    
	    if (criterio.get("creaciondesde") != null)
	    	whereStatement.append(" AND DateCreated >= " + ServiceUtil.convertLocalTimeToUTC(ServiceUtil.ldf.parse(criterio.get("creaciondesde").toString() + " 00:00:00")));
	    if (criterio.get("creacionhasta") != null)
	    	whereStatement.append(" AND DateCreated <= " + ServiceUtil.convertLocalTimeToUTC(ServiceUtil.ldf.parse(criterio.get("creacionhasta").toString() + " 23:59:59")));
	    if (criterio.get("basedesde") != null)
	    	whereStatement.append(" AND FechaBase >= " + ServiceUtil.convertLocalTimeToUTC(ServiceUtil.ldf.parse(criterio.get("basedesde").toString() + " 00:00:00")));
	    if (criterio.get("basehasta") != null)
	    	whereStatement.append(" AND FechaBase <= " + ServiceUtil.convertLocalTimeToUTC(ServiceUtil.ldf.parse(criterio.get("basehasta").toString() + " 23:59:59")));	    
	    if (criterio.get("naviera") != null)
	    	whereStatement.append(" AND Naviera = " + criterio.get("naviera").toString());
	    if (criterio.get("forwarder") != null)
	    	whereStatement.append(" AND Forwarder = " + criterio.get("forwarder").toString());
	    if (criterio.get("estado") != null) {
	    	if (criterio.get("estado").toString().equals("X")) // en progreso
	    		whereStatement.append(" AND EstadoContenedor <> 99");
	    	else
	    		whereStatement.append(" AND EstadoContenedor = " + criterio.get("estado").toString());
	    }	    
	    if (criterio.get("semaforo") != null) {
	    	if (criterio.get("semaforo").toString().equals("X")) // alertados
	    		whereStatement.append(" AND Semaforo >= 1 AND Semaforo <= 2");
	    	else
	    		whereStatement.append(" AND Semaforo = " + criterio.get("semaforo").toString());
	    }
	    if (criterio.get("cliente") != null) {
	    	Folder cliente = Factory.Folder.fetchInstance(objStore, new Id(criterio.get("cliente").toString()), null);
		    whereStatement.append(" AND This INSUBFOLDER '/Importaciones/" + cliente.get_FolderName() + "'");
	    }
	    if (criterio.get("infolder") != null)
		    whereStatement.append(" AND This INFOLDER '" + criterio.get("infolder") + "'");
	    if (criterio.get("cotizacion") != null) {
	    	if (Id.isId(criterio.get("cotizacion").toString()))
	    		whereStatement.append(" AND Cotizacion = " + criterio.get("cotizacion").toString());
	    	else
	    		whereStatement.append(" AND Cotizacion " + (Boolean.parseBoolean(criterio.get("cotizacion").toString()) ? "IS NOT NULL" : "IS NULL"));
	    }
	    if (criterio.get("proveedor") != null)
	    	whereStatement.append(" AND Proveedor = " + criterio.get("proveedor").toString());
	    if (criterio.get("importadora") != null)
	    	whereStatement.append(" AND Importadora = " + criterio.get("importadora").toString());
	    if (criterio.get("puertollegada") != null)
	    	whereStatement.append(" AND PuertoLlegada = " + criterio.get("puertollegada").toString());
	    if (criterio.get("puertosalida") != null)
	    	whereStatement.append(" AND PuertoSalida = " + criterio.get("puertosalida").toString());	    

	    sql.setWhereClause(whereStatement.toString());
	    if (maxResults > 0)
	    	sql.setMaxRecords(maxResults);
	    sql.setOrderByClause("DateCreated DESC");
	    
	    FolderSet folSet = (FolderSet) search.fetchObjects(sql, null, null, true);
	    for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) 
	    {
	    	Folder fol = it.next();
	    	JSONObject jsonObject = getContenedorJson(fol);
			// add element
			jsonArray.add(jsonObject);					
	    }
		    
		return jsonArray;
	}
	
	public static JSONObject getDocumentoJson(Document doc) throws Exception {		
    	// Properties
		com.filenet.api.property.Properties props = doc.getProperties();
    	JSONObject jsonObject = new JSONObject();
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", ServiceUtil.sdf.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("DocumentTitle"));
    	jsonObject.put("tipo", props.getInteger32Value("TipoDocumento"));
    	return jsonObject;
	}	
	
}
