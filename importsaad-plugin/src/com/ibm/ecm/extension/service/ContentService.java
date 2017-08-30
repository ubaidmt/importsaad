package com.ibm.ecm.extension.service;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.upload.FormFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.ecm.extension.util.sax.Factura;
import com.ibm.json.java.JSONObject;
import com.filenet.api.util.UserContext;
import com.filenet.api.admin.ClassDefinition;
import com.filenet.api.admin.PropertyDefinition;
import com.filenet.api.core.Connection;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Document;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.property.PropertyFilter;
import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.collection.ContentElementList;
import com.filenet.api.collection.PropertyDefinitionList;
import com.filenet.api.constants.FilteredPropertyType;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.constants.ReservationType;
import com.filenet.api.exception.EngineRuntimeException;

import com.ibm.json.java.JSONArray;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class ContentService {
	
	private static final String stanza = "FileNetP8";
	private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static JSONObject addDocument(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {

		String error = null;
		JSONObject jsonDoc = new JSONObject();
		ObjectStore os = null;
		
		try {
			
			// Get p8 connection based on context
			os = ServiceUtil.getP8Connection(request, callbacks);					
			
			// Get form file params
			FormFile uploadFile = callbacks.getRequestUploadFile();
    		String mimeType = request.getParameter("mimetype");
    		String docName = request.getParameter("parm_part_filename");
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
				Document doc = Factory.Document.createInstance(os, "Document");
				
				ContentElementList contentList = Factory.ContentElement.createList();
				ContentTransfer contentTransfer = Factory.ContentTransfer.createInstance();
				contentTransfer.setCaptureSource(is);
				contentTransfer.set_RetrievalName(docName);
				contentList.add(contentTransfer);
				doc.set_ContentElements(contentList);
				
				doc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
				
	    		Map<String, Object> props = new HashMap<String, Object>();
				com.filenet.api.property.Properties properties = doc.getProperties();
				properties.putValue("DocumentTitle", docName);
				if (!props.isEmpty()) {
					for (Map.Entry<String, Object> prop : props.entrySet()) {
						if (prop.getValue() != null)
							properties.putObjectValue(prop.getKey(), prop.getValue());			
					}
				}
				
				doc.set_MimeType(mimeType);	
				doc.save(RefreshMode.REFRESH);	
				
				String parentFolder = request.getParameter("parentFolder");
				if (parentFolder != null && !parentFolder.equals("")) {
					Folder folder = Factory.Folder.fetchInstance(os,parentFolder, null);
					ReferentialContainmentRelationship rel = folder.file(doc, AutoUniqueName.AUTO_UNIQUE, docName, 
							DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
					rel.save(RefreshMode.NO_REFRESH);				
				}					
				
				jsonDoc.put("id", doc.get_Id().toString());
				jsonDoc.put("dateCreated", sdf.format(getUTFCalendar(doc.get_DateCreated()).getTime()));
				jsonDoc.put("documentTitle", doc.get_Name());
				jsonDoc.put("size", doc.get_ContentSize());
				jsonDoc.put("mimeType", doc.get_MimeType());
				
				String parseFacturaXML = request.getParameter("parseFacturaXML");
				if (parseFacturaXML != null) {
					JSONObject jsonObject = new JSONObject();
					InputStream in = null;
					try {
						if (doc.get_ContentElements().size() > 0) {
							ContentTransfer ct = (ContentTransfer) doc.get_ContentElements().get(0);
							in = ct.accessContentStream();
							Factura factura = new Factura();
							SAXParserFactory factory = SAXParserFactory.newInstance();
							SAXParser saxParser = factory.newSAXParser();
							saxParser.parse(in, factura);		
							jsonObject.put("nombreEmisor", factura.getNombreEmisor());
							jsonObject.put("rfcEmisor", factura.getRFCEmisor());
							jsonObject.put("rfcReceptor", factura.getRFCReceptor());
							jsonObject.put("folio", factura.getFolio());
							jsonObject.put("fecha", factura.getFecha());
							jsonObject.put("total", factura.getTotal());
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						IOUtils.closeQuietly(in);
					}
					jsonDoc.put("factura", jsonObject);
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
		jsonResponse.put("document", jsonDoc);
		return jsonResponse;				
			
	}
	
	public static JSONObject getDocument(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {

		String error = null;
		JSONObject jsonObj = new JSONObject();
		FileOutputStream fos = null;
		ObjectStore os = null;
		
		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			
			if (context != null) { // conexion por usuario se servicio
				JSONObject jsonContext = JSONObject.parse(context);
	    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());
	    	    Subject subject = UserContext.createSubject(con, jsonContext.get("usuario").toString(), jsonContext.get("contrasena").toString(), stanza);
	    	    UserContext.get().pushSubject(subject); 
	    	    os = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
			} else { // conexion por usuario activo
				Subject subject = callbacks.getP8Subject(repositoryId);
				UserContext.get().pushSubject(subject);			
				os = callbacks.getP8ObjectStore(repositoryId);
			}  					
			
			String docId = request.getParameter("id");
			final String temporal = request.getParameter("temporal");
    	    
    	    Document doc = Factory.Document.fetchInstance(os, docId, null);
        	ContentElementList cel = doc.get_ContentElements();
        	ContentTransfer ct = (ContentTransfer)cel.get(0);
        	
    	    jsonObj.put("id", doc.get_Id().toString());
    	    jsonObj.put("name", doc.get_Name());
    	    jsonObj.put("mimeType", doc.get_MimeType());
    	    jsonObj.put("retrievalName", ct.get_RetrievalName());
    	    jsonObj.put("size", ct.get_ContentSize());
    	    
    	    // Write document to file system
    	    String filePath = (temporal + File.separator + ct.get_RetrievalName());
    	    fos = new FileOutputStream(filePath);
    	    IOUtils.copy(ct.accessContentStream(), fos);
    	    jsonObj.put("filePath", filePath);
    	    
    	    // Asynchronous thread to clean the temp directory
    		ExecutorService executorService = Executors.newSingleThreadExecutor();
    		executorService.execute(new Runnable() {
    		    public void run() {
    				Calendar cal = Calendar.getInstance();
    				cal.add(Calendar.DATE, -1); // 1-day-old files
    				File[] files = getFileList(new File(temporal), true, cal.getTime());
    				for (int i = 0; i < files.length; i++ )
    					FileUtils.deleteQuietly(files[i]);
    		    }
    		});
    		executorService.shutdown();    	    
			
		} catch (Exception e) {
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
    	    IOUtils.closeQuietly(fos);
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		jsonResponse.put("document", jsonObj);
		return jsonResponse;				
			
	}	
	
	private static Calendar getUTFCalendar(Date dateObj) throws Exception {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(dateObj);	
		return cal;
	}	
	
	private static File[] getFileList(File dir, final boolean includeDirs, final Date olderThan) {
		File[] files = dir.listFiles(new FileFilter()
		{
			public boolean accept(File file)
			{
				if (!includeDirs && file.isDirectory())
					return false;
				else if (olderThan == null)
					return true;
				else if (FileUtils.isFileOlder(file, olderThan))
					return true;
				else
					return false;
			}
		});
		return files;
	}		
	
	@SuppressWarnings("unchecked")
	public static JSONObject updateDocument(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;	
		ObjectStore objStore = null;
		Document doc = null;
		Document tmpDoc = null;

		try {
			
			String repositoryId = request.getParameter("repositoryid");
			String context = request.getParameter("context");
			String itemId = request.getParameter("itemid");
			String documentId = request.getParameter("documentid");
			JSONArray propsToSync = JSONArray.parse(request.getParameter("propsToSync"));
			
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
											
			// Get Current Document
			try {
				doc = Factory.Document.fetchInstance(objStore, itemId, null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El documento a ser versionado no fue localizado.");		
			}	
			
			// Get Temp Document Content
			try {
				tmpDoc = Factory.Document.fetchInstance(objStore, documentId, null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El nuevo contenido no fue localizado.");		
			}			
			
			// Build Properties Map to be sync
			com.filenet.api.property.Properties props = doc.getProperties();
			Map<String, Object> propsToSyncMap = new HashMap<String, Object>();
			JSONArray propArray = getPropertyDefinition(objStore, doc.getClassName(), convertToList(propsToSync));
			for (Object obj : propArray) {
				JSONObject jsonProp = (JSONObject) obj;
				propsToSyncMap.put(jsonProp.get("symbolicName").toString(), props.getObjectValue(jsonProp.get("symbolicName").toString()));
			}		

			// Check Out
			if (!doc.get_IsCurrentVersion())
				doc = (Document) doc.get_CurrentVersion();
			
			if (doc.get_IsReserved()) {
				Document reservation = (Document) doc.get_Reservation();
				doc.cancelCheckout();
				reservation.save(RefreshMode.REFRESH);				
			}
			
			doc.checkout(ReservationType.EXCLUSIVE, null, null, null);
			doc.save(RefreshMode.REFRESH);	
			
			// Check In
			doc = (Document) doc.get_Reservation();	
			
			if (tmpDoc.get_ContentElements().size() > 0) {
				ContentTransfer ct = (ContentTransfer) tmpDoc.get_ContentElements().get(0);
				ContentElementList contentList = Factory.ContentElement.createList();
				com.filenet.api.core.ContentTransfer contentTransfer = Factory.ContentTransfer.createInstance();
				contentTransfer.set_RetrievalName(ct.get_RetrievalName());
				contentTransfer.setCaptureSource(ct.accessContentStream());
				contentTransfer.set_ContentType(ct.get_ContentType());
				contentList.add(contentTransfer);
				doc.set_ContentElements(contentList);	
			}

			doc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);	
			
			// Since object-value properties are lost on new version
			// a synchronization of these properties has to be done
			props = doc.getProperties();
			for (Iterator<String> it = propsToSyncMap.keySet().iterator(); it.hasNext(); ) {
				String propName = it.next();
				if (props.isPropertyPresent(propName))
					props.putObjectValue(propName, propsToSyncMap.get(propName));
			}
			
			// Save Document				
			doc.save(RefreshMode.REFRESH);					

			
		} catch (Exception e) {
			
			// Cancel checkout
			if (doc != null) {
				if (doc.get_IsReserved()) {
					Document reservation = (Document) doc.get_Reservation();
					doc.cancelCheckout();
					reservation.save(RefreshMode.REFRESH);				
				}			
			}
				
			error = e.getMessage();
			e.printStackTrace();
			
		} finally {
			
			// Delete of temporary document
			if (tmpDoc != null){
				tmpDoc.delete();
				tmpDoc.save(RefreshMode.REFRESH);
			}
			
			UserContext.get().popSubject();
		}	
		
		// Result
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error", error);
		return jsonResponse;		
		
	}		
	
	@SuppressWarnings("unchecked")
	public static JSONArray getPropertyDefinition(ObjectStore os, String className, List<String> propNames) throws Exception {
		JSONArray propArray = new JSONArray();
		
		PropertyFilter pf = new PropertyFilter();
		pf.addIncludeType(0, null, Boolean.TRUE, FilteredPropertyType.ANY, null); 

		ClassDefinition objClassDef = Factory.ClassDefinition.fetchInstance(os, className, pf);                                         
		PropertyDefinitionList objPropDefs = objClassDef.get_PropertyDefinitions();   
		                                        
		for (Iterator<PropertyDefinition> it = objPropDefs.iterator(); it.hasNext(); ) {
			PropertyDefinition objPropDef = it.next();
			if (propNames.contains(objPropDef.get_SymbolicName())) {
				JSONObject prop = new JSONObject();
				prop.put("symbolicName", objPropDef.get_SymbolicName());
				prop.put("cardinality", objPropDef.get_Cardinality().getValue());
				propArray.add(prop);
			}
		}
		
		return propArray;
		
	}
	
	private static List<String> convertToList(JSONArray props) throws Exception {
		List<String> list = new ArrayList<String>();
		for (Object obj : props) {
			JSONObject jsonObj = (JSONObject) obj;
			if (jsonObj.containsKey("value")) {
			String value = jsonObj.get("value").toString();
			if (!value.isEmpty())
				list.add(value);
			}
		}
		return list;
	}
	
	public static ContentTransfer getContentTransfer(Document doc, int contentElement) throws Exception {
		ContentTransfer ct = null;
		ContentElementList cel = doc.get_ContentElements();
    		if (cel.size() >= contentElement)
    			ct = (ContentTransfer) cel.get(contentElement);
    		return ct;
	}		
	
}
