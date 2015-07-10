package com.importsaad.job.impl;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.Iterator;
import java.util.Date;
import java.text.SimpleDateFormat;
import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ContentEngineSettings;
import com.excelecm.common.settings.ConfigurationSettings;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.core.Document;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.constants.RefreshMode;

public class LimpiaTemporalJobImpl {
	
	private long docEliminados = 0;
	private String osName;
	private int addDays = 0;
	
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
		    SearchScope search = new SearchScope(ceService.getOS(getOsName()));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id");
		    sql.setFromClauseInitialValue("Document ", null, true);
		    
		    StringBuffer sb = new StringBuffer();
		    if (addDays != 0) {
		    	Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		    	cal.setTime(new Date());
		    	cal.add(Calendar.DATE, addDays);
		    	sb.append("DateCreated <= " + Date2ISO(cal.getTime()) + " AND ");
		    }
		    sb.append("This INFOLDER '/Temporal/Documentos'");
		    
		    sql.setWhereClause(sb.toString());	
		    
		    IndependentObjectSet objSet = search.fetchObjects(sql, null, null, true);
    		
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
	
	public void setAddDays(int addDays) {
		this.addDays = addDays;
	}		
	
	public long getDocEliminados() {
		return docEliminados;
	}
	
    private String Date2ISO(Date date) {
        String timePattern = "yyyyMMdd'T'HHmmssz";
        SimpleDateFormat formatter = new SimpleDateFormat(timePattern);
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        formatter.setTimeZone(timeZone);
        String f =  formatter.format(date);
        return f.replaceAll("UTC","Z");
    }

}
