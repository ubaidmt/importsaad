package com.importsaad.job.impl;

import java.util.Iterator;

import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ConfigurationSettings;
import com.excelecm.common.settings.ContentEngineSettings;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Document;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;

public class LimpiaUnfiledJobImpl {

	private String osName;
	private long docEliminados = 0;
	
	@SuppressWarnings("unchecked")
	public void doExec() throws Exception {
		
		CEService ceService = null;

    	try 
    	{
    		// CE Settings
    		ContentEngineSettings ceSettings = ConfigurationSettings.getInstance().getCESettings();
    		
			// CE Connection
			ceService = new CEService(ceSettings);
			ceService.establishConnection();
			
			// Perform CE Query
			StringBuffer sqlStatement = new StringBuffer();
			sqlStatement.append("SELECT d.This, d.Id ");
			sqlStatement.append("FROM Document d WITH INCLUDESUBCLASSES LEFT JOIN ReferentialContainmentRelationship r ON d.This = r.Head ");
			sqlStatement.append("WHERE r.Head IS NULL AND IsCurrentVersion = TRUE");
		    SearchScope search = new SearchScope(ceService.getOS(getOsName()));
		    SearchSQL sql = new SearchSQL(sqlStatement.toString());
		    
		    IndependentObjectSet objSet = search.fetchObjects(sql, 50, null, true);
    		
    		for (Iterator<Document> it = objSet.iterator(); it.hasNext(); ) 
    		{
    			Document doc = it.next();
    			doc.delete();
    			doc.save(RefreshMode.NO_REFRESH);
    			docEliminados++;
		    }
    		
    	}
    	catch (Exception e)
    	{
    		throw e;
    	}
    	finally
    	{
    		if (ceService != null)
    			ceService.releaseConnection();	
    	}
		
	}	
	
	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}	
	
	public long getDocEliminados() {
		return docEliminados;
	}
	
}

