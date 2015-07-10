package com.importsaad.servlet;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.io.IOException;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.filenet.api.collection.DocumentSet;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;

import com.excelecm.common.settings.ConfigurationSettings;
import com.excelecm.common.settings.ContentEngineSettings;
import com.excelecm.common.util.PropertyConfig;
import com.excelecm.common.util.CommonUtils;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

/**
 * Servlet implementation class Dispatcher
 */
@WebServlet("/Dispatcher")
public class Dispatcher extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final String CntSettingsId = "{1542E1F2-5854-40ED-A3B9-75DB8D24EFF9}";
	private static final DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static final DateFormat longFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");  

    public Dispatcher() {
        super();
    }
    
    public void init() throws ServletException {
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doProcess(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doProcess(request, response);
	}
	
	protected void doProcess(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		request.setCharacterEncoding("UTF-8");
		String methodName = request.getParameter("method");
		if (methodName == null)
			throw new ServletException("No se ha incluido el nombre del metodo a invocar.");	
		
		try {
			
			JSONObject result = new JSONObject();
			if (methodName.equals("searchFracciones"))
				result = searchFracciones(request);
			else if (methodName.equals("searchContenedores"))
				result = searchContenedores(request);
			else if (methodName.equals("searchClientes"))
				result = searchClientes(request);
			else if (methodName.equals("searchNavieras"))
				result = searchNavieras(request);		
			else if (methodName.equals("searchForwarders"))
				result = searchForwarders(request);					
			else if (methodName.equals("searchDocumentos"))
				result = searchDocumentos(request);			
			else if (methodName.equals("getCntSettings"))
				result = getCntSettings(request);			
			else if (methodName.equals("getMobileSettings"))
				result = getMobileSettings(request);
			else
				throw new Exception("No se identifico el método incluido en el servicio.");

			// Send back JSON response
			PrintWriter writer = response.getWriter();
			response.setCharacterEncoding("UTF-8"); 
			result.serialize(writer);
			
		} catch (Exception e) {
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("error", "Ocurrio un error al momento de invocar un servicio. " + e.getMessage());
			PrintWriter writer = response.getWriter();
			response.setCharacterEncoding("UTF-8"); 
			jsonResponse.serialize(writer);
		}			
		
	}	
	
	@SuppressWarnings("unchecked")
	private JSONObject searchFracciones(HttpServletRequest request) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = getP8Connection(request);

		    SearchScope search = new SearchScope(objStore);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, DateCreated, DocumentTitle, ClbJSONData");
		    sql.setFromClauseInitialValue("CntFraccion", null, false);
		    sql.setOrderByClause("DocumentTitle");

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
	
	private JSONObject searchContenedores(HttpServletRequest request) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = getP8Connection(request);
			
			// Search criteria
			JSONObject criterio = JSONObject.parse(request.getParameter("criterio"));
			
			// Max results
			int maxResults = Integer.parseInt(request.getParameter("maxResults"));

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
	
	@SuppressWarnings("unchecked")
	private JSONArray searchContenedores(ObjectStore objStore, JSONObject criterio, int maxResults) throws Exception {
			
		JSONArray jsonArray = new JSONArray();
		
	    SearchScope search = new SearchScope(objStore);
	    SearchSQL sql = new SearchSQL();
	    sql.setSelectList("This, Id, Parent, DateCreated, FolderName, FechaBase, Pedimento, Naviera, Forwarder, EstadoContenedor, Semaforo, ClbJSONData, Mercancia");
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
	    if (criterio.get("creaciondesde") != null)
	    	whereStatement.append(" AND DateCreated >= " + convertLocalTimeToUTC(longFormat.parse(criterio.get("creaciondesde").toString() + " 00:00:00")));
	    if (criterio.get("creacionhasta") != null)
	    	whereStatement.append(" AND DateCreated <= " + convertLocalTimeToUTC(longFormat.parse(criterio.get("creacionhasta").toString() + " 23:59:59")));
	    if (criterio.get("basedesde") != null)
	    	whereStatement.append(" AND FechaBase >= " + convertLocalTimeToUTC(longFormat.parse(criterio.get("basedesde").toString() + " 00:00:00")));
	    if (criterio.get("basehasta") != null)
	    	whereStatement.append(" AND FechaBase <= " + convertLocalTimeToUTC(longFormat.parse(criterio.get("basehasta").toString() + " 23:59:59")));	    
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
	
	@SuppressWarnings("unchecked")
	private JSONObject searchClientes(HttpServletRequest request) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = getP8Connection(request);

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
	
	@SuppressWarnings("unchecked")
	private JSONObject searchNavieras(HttpServletRequest request) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = getP8Connection(request);

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
	
	@SuppressWarnings("unchecked")
	private JSONObject searchForwarders(HttpServletRequest request) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = getP8Connection(request);

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
	
	private JSONObject searchDocumentos(HttpServletRequest request) throws Exception {
		
		String error = null;		
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = getP8Connection(request);
			
			// Search criteria
			JSONObject criterio = JSONObject.parse(request.getParameter("criterio"));
			
			// Max results
			int maxResults = Integer.parseInt(request.getParameter("maxResults"));
			
			// Mobile settings
			JSONObject settings = getMobileSettingsJson();

			jsonArray = searchDocumentos(objStore, criterio, maxResults, settings);
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonArray = new JSONArray();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("documentos", jsonArray);
		return jsonResponse;
	}	
	
	@SuppressWarnings("unchecked")
	private JSONArray searchDocumentos(ObjectStore objStore, JSONObject criterio, int maxResults, JSONObject settings) throws Exception {
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
	    	JSONObject jsonObject = getDocumentoJson(doc, settings);
			// add element
			jsonArray.add(jsonObject);	
	    }
		
		return jsonArray;
	}	
		
	private JSONObject getCntSettings(HttpServletRequest request) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = getP8Connection(request);
			
			// Get settings object
	    	jsonData = getCntSettings(objStore);

		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("settings", jsonData);
		return jsonResponse;
	}		
	
	private JSONObject getMobileSettings(HttpServletRequest request) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get settings object
	    	jsonData = getMobileSettingsJson();

		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			jsonData = new JSONObject();
		}
				
		// Result
		jsonResponse.put("error", error);
		jsonResponse.put("settings", jsonData);
		return jsonResponse;
	}
	
	private JSONObject getMobileSettingsJson() throws Exception {
		java.util.Properties props = PropertyConfig.getInstance().getPropertiesResource();
		
		JSONObject jsonSettings = new JSONObject();
		jsonSettings.put("context", getPropertyValue(props.getProperty("mobile.context"), ""));
		jsonSettings.put("osname", getPropertyValue(props.getProperty("mobile.osname"), ""));
		jsonSettings.put("repository", getPropertyValue(props.getProperty("mobile.repository"), ""));
		jsonSettings.put("desktop", getPropertyValue(props.getProperty("mobile.desktop"), ""));
		jsonSettings.put("pteiva", Double.parseDouble(getPropertyValue(props.getProperty("mobile.cotizador.pteiva"), "0")));
		
    	return jsonSettings;
	}
	
	private JSONObject getCntSettings(ObjectStore objStore) throws Exception {
		JSONObject jsonData = new JSONObject();
		
		// get settings object
		Document settings = Factory.Document.fetchInstance(objStore, new Id(CntSettingsId), null);
    	byte[] data = settings.getProperties().getBinaryValue("ClbJSONData");
    	if (data != null)
    		jsonData = JSONObject.parse(new String(data));
    	
    	return jsonData;
	}		
	
	private String getPropertyValue(String propValue, String defaultValue ) {
		try {
			if (CommonUtils.isEmtpy(propValue))
				return defaultValue;
			else
				return propValue;
		} catch (Exception e) {
			return defaultValue;
		}
	}	
	
	private ObjectStore getP8Connection(HttpServletRequest request) throws Exception {
		ContentEngineSettings ceSettings = ConfigurationSettings.getInstance().getCESettings();
		Connection con = Factory.Connection.getConnection(ceSettings.getUri());
	    Subject subject = UserContext.createSubject(con, ceSettings.getUser(), ceSettings.getPassword(), ceSettings.getStanza());
	    UserContext.get().pushSubject(subject); 
	    ObjectStore os = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), request.getParameter("os"), null);
		return os;
	}
	
	private String convertLocalTimeToUTC(Date localDate) throws Exception{     
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcFormat.format(localDate);
    } 
	
	private Calendar getUTFCalendar(Date dateObj) throws Exception {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(dateObj);	
		return cal;
	}	
	
	private Folder getContenedorCliente(Folder contenedor) throws Exception {
		Folder fol = contenedor;
		String className = "";
		while (fol != null && !className.equals("CntCliente")) {
			fol = fol.get_Parent();
			if (fol != null)
				className = fol.getClassName();
		}
		return fol;		
	}
	
	private JSONObject getContenedorJson(Folder contenedor) throws Exception {		
    	// Get cliente del contenedor
    	Folder cliente = getContenedorCliente(contenedor);
    	// Get Data
    	com.filenet.api.property.Properties props = contenedor.getProperties();
    	byte[] data = props.getBinaryValue("ClbJSONData");
    	JSONObject jsonData = JSONObject.parse(new String(data));
    	// Properties
    	JSONObject jsonObject = new JSONObject();
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated",isoFormat.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("FolderName"));
    	jsonObject.put("fechabase",isoFormat.format(getUTFCalendar(props.getDateTimeValue("FechaBase")).getTime()));
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
    	return jsonObject;
	}	
	
	private JSONObject getClienteJson(com.filenet.api.property.Properties props) throws Exception {
    	JSONObject jsonObject = new JSONObject();	
    	// Properties
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", isoFormat.format(props.getDateTimeValue("DateCreated")));
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
    	jsonObject.put("datecreated", isoFormat.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("DocumentTitle"));
    	jsonObject.put("activo", props.getBooleanValue("Activo"));
    	return jsonObject;
	}	
	
	private JSONObject getForwarderJson(com.filenet.api.property.Properties props) throws Exception {
		JSONObject jsonObject = new JSONObject();	
    	// Properties
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", isoFormat.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("DocumentTitle"));
    	jsonObject.put("activo", props.getBooleanValue("Activo"));
    	return jsonObject;
	}
	
	private JSONObject getFraccionJson(com.filenet.api.property.Properties props) throws Exception {
    	// Get Data
    	byte[] data = props.getBinaryValue("ClbJSONData");
    	JSONObject jsonObject = JSONObject.parse(new String(data));	
    	// Properties
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", isoFormat.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("DocumentTitle"));    	
    	return jsonObject;
	}
	
	private JSONObject getDocumentoJson(Document doc, JSONObject settings) throws Exception {		
    	// Properties
		com.filenet.api.property.Properties props = doc.getProperties();
    	JSONObject jsonObject = new JSONObject();
    	jsonObject.put("id", props.getIdValue("Id").toString());
    	jsonObject.put("datecreated", isoFormat.format(props.getDateTimeValue("DateCreated")));
    	jsonObject.put("name", props.getStringValue("DocumentTitle"));
    	jsonObject.put("tipo", props.getInteger32Value("TipoDocumento"));
    	// doc external link
    	StringBuffer docLink = new StringBuffer();
    	docLink.append(settings.get("context").toString());
    	docLink.append("navigator/bookmark.jsp");
    	docLink.append("?repositoryType=p8");
    	docLink.append("&template_name=CntDocumento");
    	docLink.append("&version=released");
    	docLink.append("&desktop=" + settings.get("desktop").toString());
    	docLink.append("&docid=" + props.getIdValue("Id").toString());
    	jsonObject.put("link", docLink.toString());
    	return jsonObject;
	}	
	
}
