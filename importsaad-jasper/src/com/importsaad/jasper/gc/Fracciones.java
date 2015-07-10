package com.importsaad.jasper.gc;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.excelecm.common.service.CEService;
import com.excelecm.common.service.PEService;
import com.excelecm.jasper.module.IReportsCMCustomSearch;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.ibm.json.java.JSONObject;

public class Fracciones implements IReportsCMCustomSearch {
	
	private final static DateFormat sdf = new SimpleDateFormat("yyyyMMdd");	
	
	@SuppressWarnings("unchecked")
	public List<Properties> doExecute(CEService ceService, PEService peService, String objectStoreName, String rosterName, String eventLogName, Map<String, Object> condiciones) throws Exception {
		
		List<Properties> resultado = new ArrayList<Properties>();
		
		// Build Query
		SearchScope search = new SearchScope(ceService.getOS(objectStoreName));
		SearchSQL sql = new SearchSQL();	
	    sql.setSelectList("Id, DateCreated, DocumentTitle, ClbJSONData");
	    sql.setFromClauseInitialValue("CntFraccion", null, false);
	    
		StringBuffer whereStatement = new StringBuffer();
		whereStatement.append("isCurrentVersion = TRUE");
		sql.setWhereClause(whereStatement.toString());		
		
		// Execute Query
	    RepositoryRowSet rowSet = (RepositoryRowSet) search.fetchRows(sql, null, null, true);
	    
	    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		{
	    	RepositoryRow row = it.next();
			com.filenet.api.property.Properties rowProps = row.getProperties();
			java.util.Properties props = new java.util.Properties();
			
	    	// Get Data
	    	byte[] data = rowProps.getBinaryValue("ClbJSONData");
	    	JSONObject jsonData = JSONObject.parse(new String(data));	
	    	
			// Property Values
			props.setProperty("DateCreated", sdf.format(rowProps.getDateTimeValue("DateCreated")));
			props.setProperty("Fraccion", rowProps.getStringValue("DocumentTitle"));
			props.setProperty("Descripcion", jsonData.get("descripcion").toString());
			props.setProperty("Unidad", jsonData.get("unidad").toString());
			props.setProperty("Precio", jsonData.get("precio").toString());
	    	
    		resultado.add(props);			
					
		}
		
		return resultado;
	}

}
