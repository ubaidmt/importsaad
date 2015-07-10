package com.excelecm.com.eds.services;

import java.util.Iterator;

import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ConfigurationSettings;
import com.excelecm.common.settings.ContentEngineSettings;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.query.RepositoryRow;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class SolDocClientes {
	
	public static JSONObject getData(String propertyName, JSONArray choices, Object initialValue, boolean isRequired) throws Exception {
		
		JSONObject result = new JSONObject();
		JSONObject jsonObj = new JSONObject();		
		
		try 
		{
	    	jsonObj = new JSONObject();
	    	jsonObj.put("displayName", "Cliente");
	    	jsonObj.put("choices", choices);
	    	
	    	result = new JSONObject();
	    	result.put("symbolicName", propertyName);
	    	result.put("choiceList", jsonObj);
	    	result.put("hasDependentProperties", false);
	    	
	    	if (initialValue != null)	    	
	    		result.put("value", initialValue);
        	if (isRequired)
        		result.put("required", true);
        	result.put("customValidationError", "El Cliente es obligatorio");
	    	
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
			    		
			String sql = "SELECT Id, FolderName FROM SolDocCliente WITH EXCLUDESUBCLASSES WHERE This INFOLDER '/Facturas'";
    		RepositoryRowSet proveedoresSet = ceService.fetchRows(sql, osName, 0);
    		
    		for (Iterator<RepositoryRow> it = proveedoresSet.iterator(); it.hasNext(); ) 
    		{
    			RepositoryRow proveedor = it.next();
	    		jsonObj = new JSONObject();
	    		jsonObj.put("value", proveedor.getProperties().getIdValue("Id").toString());
	    		jsonObj.put("displayName", proveedor.getProperties().getStringValue("FolderName"));
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
