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

public class ListasService {
	
	public final static int TIPO_IMPORTADORA = 0;
	public final static int TIPO_CONCEPTOSCOMPROBANTE = 1;
	public final static int TIPO_TIPOMERCANCIA = 2;
	public final static int TIPO_TIPOANEXOCTAGASTOS = 3;
	
	public final static String SELECT_OPTION = "-- SELECCIONA --";
	
	public static JSONObject getData(String propertyName, String propertyDescription, JSONArray choices, String initialValue, boolean isRequired) throws Exception {
		
		JSONObject result = new JSONObject();
		JSONObject jsonObj = new JSONObject();		
		
		try 
		{
	    	jsonObj = new JSONObject();
	    	jsonObj.put("displayName", propertyDescription);
	    	jsonObj.put("choices", choices);
	    	
	    	result = new JSONObject();
	    	result.put("symbolicName", propertyName);
	    	result.put("choiceList", jsonObj);
	    	result.put("hasDependentProperties", false);
	    	
	    	if (!CommonUtils.isEmtpy(initialValue))	    	
	    		result.put("value", initialValue);
        	if (isRequired)
        		result.put("required", true);    	
	    	
        	result.put("customValidationError", "Este dato es obligatorio");
	    	
	    	return result;
		}
		catch (Exception e) 
		{
    		throw e;
    	}	
	}		
	
	@SuppressWarnings("unchecked")
	public static JSONArray getChoices(String osName, int tipoLista) throws Exception {
		
		CEService ceService = null;
		JSONArray choices = new JSONArray();		
		JSONObject jsonObj = new JSONObject();		
		
		try 
		{
			ContentEngineSettings ceSettings = ConfigurationSettings.getInstance().getCESettings();
			ceService = new CEService(ceSettings);
			ceService.establishConnection();  	
    		
			String sql = "SELECT Nombre FROM Lista WITH EXCLUDESUBCLASSES WHERE TipoLista = " + tipoLista + " AND IsCurrentVersion = True AND This INFOLDER '/Catálogos/Listas' ORDER BY Nombre";
    		RepositoryRowSet listaSet = ceService.fetchRows(sql, osName, 0);
    		
    		for (Iterator<RepositoryRow> it = listaSet.iterator(); it.hasNext(); ) 
    		{
    			RepositoryRow lista = it.next();
	    		jsonObj = new JSONObject();
	    		String nombre = lista.getProperties().getStringValue("Nombre");
	    		jsonObj.put("value", nombre);
	    		jsonObj.put("displayName", nombre);
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
	
	public static JSONArray addSelectChoice(JSONArray choices) throws Exception {		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("value", SELECT_OPTION);
		jsonObj.put("displayName", SELECT_OPTION);
		choices.add(0, jsonObj);
		return choices;
	}

}
