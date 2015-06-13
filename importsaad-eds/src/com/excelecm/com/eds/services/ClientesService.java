package com.excelecm.com.eds.services;

import java.util.Iterator;
import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ConfigurationSettings;
import com.excelecm.common.settings.ContentEngineSettings;
import com.excelecm.common.util.CommonUtils;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.query.RepositoryRow;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class ClientesService {

	public static JSONObject getData(String propertyName, JSONArray choices, String initialValue, boolean isRequired) throws Exception {
		
		JSONObject result = new JSONObject();
		JSONObject jsonObj = new JSONObject();		
		
		try 
		{
	    	jsonObj = new JSONObject();
	    	jsonObj.put("displayName", "Nombre de Cliente");
	    	jsonObj.put("choices", choices);
	    	
	    	result = new JSONObject();
	    	result.put("symbolicName", propertyName);
	    	result.put("choiceList", jsonObj);
	    	result.put("hasDependentProperties", false);
	    	
	    	if (!CommonUtils.isEmtpy(initialValue))	    	
	    		result.put("value", initialValue);
        	if (isRequired)
        		result.put("required", true);
        	result.put("customValidationError", "El Nombre de Cliente es obligatorio");
	    	
	    	return result;
		}
		catch (Exception e) 
		{
    		throw e;
    	}	
	}		
	
	@SuppressWarnings("unchecked")
	public static JSONArray getChoices(String osName) throws Exception {
		
		CEService ceService = null;
		JSONArray choices = new JSONArray();		
		JSONObject jsonObj = new JSONObject();		
		
		try 
		{
			ContentEngineSettings ceSettings = ConfigurationSettings.getInstance().getCESettings();
			ceService = new CEService(ceSettings);
			ceService.establishConnection();  	
    		
			String sql = "SELECT Nombre FROM Cliente WITH EXCLUDESUBCLASSES WHERE IsCurrentVersion = True AND This INFOLDER '/Catálogos/Clientes' ORDER BY Nombre";
    		RepositoryRowSet clientesSet = ceService.fetchRows(sql, osName, 0);
    		
    		for (Iterator<RepositoryRow> it = clientesSet.iterator(); it.hasNext(); ) 
    		{
    			RepositoryRow cliente = it.next();
	    		jsonObj = new JSONObject();
	    		String nombreCliente = cliente.getProperties().getStringValue("Nombre");
	    		jsonObj.put("value", nombreCliente);
	    		jsonObj.put("displayName", nombreCliente);
	    		choices.add(jsonObj);    			
    		}	
    		
	    	return choices;
		}
		catch (Exception e) 
		{
    		throw e;	
    	} 
		finally 
		{
			ceService.releaseConnection();
    	}    		
		
	}

}