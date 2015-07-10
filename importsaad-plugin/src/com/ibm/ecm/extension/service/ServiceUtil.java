package com.ibm.ecm.extension.service;

import java.util.Map;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import com.filenet.api.collection.FolderSet;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Document;
import com.filenet.api.core.VersionSeries;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.util.UserContext;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.ecm.extension.util.cipher.ConfigUtil;
import com.ibm.json.java.JSONObject;

public class ServiceUtil {
	
	private static final String stanza = "FileNetP8";
	public final static DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	public final static DateFormat ldf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public final static DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");  
	
	public static JSONObject getSettingsObject(ObjectStore os) throws Exception {
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
		return jsonSettings;
	}		
	
	public static ObjectStore getP8Connection(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		ObjectStore os = null;
		String repositoryId = request.getParameter("repositoryid");		
		String context = request.getParameter("context");
		if (context != null) { // conexion por cuenta de servicio
			Map<String, String> configMap = ConfigUtil.getInstance(callbacks).getConfigurationMap();
			JSONObject jsonContext = JSONObject.parse(context);
    		Connection con = Factory.Connection.getConnection(jsonContext.get("serverName").toString());    		
    	    Subject subject = UserContext.createSubject(con, configMap.get("usuario"), configMap.get("contrasena"), stanza);
    	    UserContext.get().pushSubject(subject);
    	    os = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), jsonContext.get("objectStoreName").toString(), null);
		} else { // conexion por usuario activo
			Subject subject = callbacks.getP8Subject(repositoryId);
			UserContext.get().pushSubject(subject);			
			os = callbacks.getP8ObjectStore(repositoryId);
		}
		return os;
	}
	
	public static String convertLocalTimeToUTC(Date localDate) throws Exception{     
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcFormat.format(localDate);
    } 	
	
	public static double getDouble(Object num) throws Exception {
		double dbl = 0.00;
		if (num instanceof Integer)
			dbl = ((Integer) num).doubleValue();		
		if (num instanceof Long)
			dbl = ((Long) num).doubleValue();
		else
			dbl = (Double) num;
		return dbl;
	}
	
	public static synchronized Folder createFolder(ObjectStore objStore, String className, String folderName, Folder parent) throws Exception {
		Folder fol = Factory.Folder.createInstance(objStore, className);
		fol.set_Parent(parent);
		fol.set_FolderName(folderName);		
		fol.save(RefreshMode.REFRESH);	
		return fol;
	}
	
	public static Calendar getUTFCalendar(Date dateObj) throws Exception {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(dateObj);	
		return cal;
	}
	
	public synchronized static Folder getFolder(ObjectStore os, Folder parentFolder, String folderName, String className) throws Exception {
		Folder fol = null;
		try {
			fol = Factory.Folder.fetchInstance(os, parentFolder.get_PathName() + "/" + folderName, null);
		} catch (EngineRuntimeException ere) {}
		if (fol == null) {
			fol = Factory.Folder.createInstance(os, className);
			fol.set_FolderName(folderName);
			fol.set_Parent(parentFolder);
			fol.save(RefreshMode.REFRESH);
		}
		return fol;
	}		
	
	@SuppressWarnings("unchecked")
	public static void deleteRecursively(Folder folder) throws Exception {
		FolderSet folSet = folder.get_SubFolders();
		for (Iterator<Folder> itFol = folSet.iterator(); itFol.hasNext(); ) 
		{
			Folder subFolder = itFol.next();
			deleteRecursively(subFolder);
		}
		DocumentSet docSet = folder.get_ContainedDocuments();
		for (Iterator<Document> itDoc = docSet.iterator(); itDoc.hasNext(); ) 
		{
	    	Document doc = itDoc.next();
	    	VersionSeries vs = doc.get_VersionSeries();
	    	vs.delete();
	    	vs.save(RefreshMode.REFRESH);
		}
		folder.delete();
		folder.save(RefreshMode.REFRESH);
	}
	
	public static void deleteParentsRecursively(Folder folder, String doWhileFolder) throws Exception {
		if (folder.get_SubFolders().isEmpty() && folder.get_ContainedDocuments().isEmpty()) {
			Folder parent = folder.get_Parent();
			folder.delete();
			folder.save(RefreshMode.REFRESH);
			if (!parent.get_FolderName().equals(doWhileFolder))
				deleteParentsRecursively(parent, doWhileFolder);
		}
	}

}
