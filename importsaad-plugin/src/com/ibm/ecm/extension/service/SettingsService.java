package com.ibm.ecm.extension.service;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.json.java.JSONObject;

public class SettingsService extends PluginService {
	
	private static final String SolDocSettingsId = "{B057C949-0000-CC1B-8D2B-5FDD8EADA15B}";
	private static final String CntSettingsId = "{1542E1F2-5854-40ED-A3B9-75DB8D24EFF9}";
	private static final String FolioCotizacionesId = "{1F83A436-8280-446A-AED1-0EA230031761}";
	
	@Override	
	public String getId() {
		return "SettingsService";
	}	
	
	@Override	
	public void execute(PluginServiceCallbacks callbacks, HttpServletRequest request, HttpServletResponse response) throws Exception {

		String methodName = request.getParameter("method");
		if (methodName == null)
			throw new Exception("No se ha incluido el nombre del método a invocar.");
		
		callbacks.getLogger().logEntry(this, methodName, request);
			
		try {
	
			JSONObject result = new JSONObject();
			if (methodName.equals("getSolDocSettings"))
				result = getSolDocSettings(request, callbacks);
			else if (methodName.equals("updateSolDocSettings"))
				result = updateSolDocSettings(request, callbacks);
			else if (methodName.equals("getCntSettings"))
				result = getCntSettings(request, callbacks);
			else if (methodName.equals("updateCntSettings"))
				result = updateCntSettings(request, callbacks);			
			else
				throw new Exception("No se identificó el método incluido en el servicio.");

			// Send back JSON response
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
	
	private JSONObject getSolDocSettings(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get settings object
	    	jsonData = getSolDocSettings(objStore);

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
	
	private JSONObject getCntSettings(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonData = new JSONObject();
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
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
	
	private JSONObject updateSolDocSettings(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get data
			JSONObject jsonData = JSONObject.parse(request.getParameter("settings").toString());
			
			// Update settings object
			updateSolDocSettingsObject(objStore, jsonData);
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		return jsonResponse;
	}
	
	private JSONObject updateCntSettings(HttpServletRequest request, PluginServiceCallbacks callbacks) throws Exception {
		
		String error = null;		
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			// Get p8 connection based on context
			ObjectStore objStore = ServiceUtil.getP8Connection(request, callbacks);
			
			// Get data
			JSONObject jsonData = JSONObject.parse(request.getParameter("settings").toString()); 
			
			// Update settings object
			updateCntSettingsObject(objStore, jsonData);
			
		} catch (Exception e) {
			e.printStackTrace();
			error = e.getMessage();
			
		} finally {
			UserContext.get().popSubject();
		}
				
		// Result
		jsonResponse.put("error", error);
		return jsonResponse;
	}	
		
	public static JSONObject getSolDocSettings(ObjectStore objStore) throws Exception {
		JSONObject jsonData = new JSONObject();
		
		// get settings object
		Document settings = Factory.Document.fetchInstance(objStore, new Id(SolDocSettingsId), null);
    	byte[] data = settings.getProperties().getBinaryValue("ClbJSONData");
    	if (data != null)
    		jsonData = JSONObject.parse(new String(data));
    	
    	return jsonData;
	}
	
	public static JSONObject getCntSettings(ObjectStore objStore) throws Exception {
		JSONObject jsonData = new JSONObject();
		
		// get settings object
		Document settings = Factory.Document.fetchInstance(objStore, new Id(CntSettingsId), null);
    	byte[] data = settings.getProperties().getBinaryValue("ClbJSONData");
    	if (data != null)
    		jsonData = JSONObject.parse(new String(data));
    	
    	return jsonData;
	}
	
	public synchronized static void updateSolDocSettingsObject(ObjectStore objStore, JSONObject jsonData) throws Exception {
		
		// get settings object
		Document settings = Factory.Document.fetchInstance(objStore, new Id(SolDocSettingsId), null);
		com.filenet.api.property.Properties props = settings.getProperties();

		// update settings object
		props.putObjectValue("ClbJSONData", jsonData.serialize().getBytes());
		settings.save(RefreshMode.REFRESH);	
	}
	
	public synchronized static void updateCntSettingsObject(ObjectStore objStore, JSONObject jsonData) throws Exception {
		
		// get settings object
		Document settings = Factory.Document.fetchInstance(objStore, new Id(CntSettingsId), null);
		com.filenet.api.property.Properties props = settings.getProperties();

		// update settings object
		props.putObjectValue("ClbJSONData", jsonData.serialize().getBytes());
		settings.save(RefreshMode.REFRESH);	
	}
	
	public synchronized static int getNextConsecutivoCotizacion(ObjectStore objStore) throws Exception {
		
		// get folio consecutivo object
		Document folio = Factory.Document.fetchInstance(objStore, new Id(FolioCotizacionesId), null);
		com.filenet.api.property.Properties props = folio.getProperties();	
		int consecutivo = props.getInteger32Value("Consecutivo");
		props.putObjectValue("Consecutivo", (consecutivo + 1));	
		folio.save(RefreshMode.REFRESH);
		
		return consecutivo;
	}	
	
}
