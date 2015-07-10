package com.ibm.ecm.extension.util.cipher;

import java.util.Map;
import java.util.HashMap;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class ConfigUtil {
	
	private static ConfigUtil singleton = null;
	private JSONArray jsonConfig = new JSONArray();
	private Map<String, String> mapConfig = new HashMap<String, String>();
	private final static String stringKey = "pxtHnTBIdxE0vOvuQ5fAEQ=="; // AES-compliant string key	
	private final static String KEY_CONTRASENA = "contrasena";
	private final static int DECRYPT_MAX_RETRIES = 3;
	
    public static ConfigUtil getInstance(PluginServiceCallbacks callbacks) {
    	if(singleton == null) 
    		singleton = new ConfigUtil(callbacks);
        return singleton;
    }		
    
	private ConfigUtil(PluginServiceCallbacks callbacks) {
		loadConfiguration(callbacks);
	}
	
	private void loadConfiguration(PluginServiceCallbacks callbacks) {
		try
		{
			String strconfig = callbacks.loadConfiguration();
			if (strconfig != null) {
				JSONObject data = JSONObject.parse(strconfig);
				if (data.containsKey("configuration")) {
					jsonConfig = (JSONArray) data.get("configuration");
					for (Object obj : jsonConfig) {
						JSONObject jsonObj = (JSONObject) obj;
						if (jsonObj.containsKey("name") && jsonObj.containsKey("value")) {
							// decrypt contrasena 
							if (jsonObj.get("name").toString().equals(KEY_CONTRASENA)) {
								String contrasena = null;
								int retry = 0;
								while (contrasena == null && retry < DECRYPT_MAX_RETRIES) {
									contrasena = CipherUtil.decrypt(jsonObj.get("value").toString(), CipherUtil.getSecretKey(stringKey)); // decrypted password
									retry++;
								}
								// set decrypted contrasena
								mapConfig.put(KEY_CONTRASENA, contrasena);								
							} else {
								// set map value
								mapConfig.put(jsonObj.get("name").toString(), jsonObj.get("value").toString());								
							}
						}
					}					
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public JSONArray getConfigurationJson() {
		return jsonConfig;
	}
	
	public Map<String, String> getConfigurationMap() {
		return mapConfig;
	}

}
