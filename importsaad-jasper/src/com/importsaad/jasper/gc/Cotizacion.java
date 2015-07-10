package com.importsaad.jasper.gc;

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
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class Cotizacion implements IReportsCMCustomSearch {
	
	@SuppressWarnings("unchecked")
	public List<Properties> doExecute(CEService ceService, PEService peService, String objectStoreName, String rosterName, String eventLogName, Map<String, Object> condiciones) throws Exception {
		
		List<Properties> resultado = new ArrayList<Properties>();
		
		// Build Query
		SearchScope search = new SearchScope(ceService.getOS(objectStoreName));
		SearchSQL sql = new SearchSQL();	
	    sql.setSelectList("Id, ClbJSONData");
	    sql.setFromClauseInitialValue("CntCotizacion", null, false);
	    
		StringBuffer whereStatement = new StringBuffer();
		whereStatement.append("isCurrentVersion = TRUE");
		whereStatement.append(" AND Id = " + condiciones.get("Id").toString());
		sql.setWhereClause(whereStatement.toString());		
		
		sql.setMaxRecords(1);
		
		// Execute Query
		RepositoryRowSet rowSet = (RepositoryRowSet) search.fetchRows(sql, null, null, true);
	    
	    for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		{
	    	RepositoryRow row = it.next();
	    	com.filenet.api.property.Properties rowProps = row.getProperties();
			
	    	// Get Data
	    	byte[] data = rowProps.getBinaryValue("ClbJSONData");
	    	JSONObject jsonData = JSONObject.parse(new String(data));
	    	
	    	JSONArray fraccionesCotizadas = (JSONArray) jsonData.get("fracciones");
	    	for (Object obj : fraccionesCotizadas)
	    	{
	    		JSONObject fraccionCotizada = (JSONObject) obj;
				java.util.Properties props = new java.util.Properties();
				
				// si la medida esta en pulgadas, se realiza la conversion a metros
				double ancho = Double.parseDouble(fraccionCotizada.get("ancho").toString());
				if (fraccionCotizada.containsKey("medida") && fraccionCotizada.get("medida").toString().equals("in"))
					ancho = ancho * 0.0254;	

				// Property Values
				props.setProperty("Fraccion", fraccionCotizada.get("fraccion").toString());
				props.setProperty("UnidadComercial", fraccionCotizada.get("unidadComercial").toString());
				props.setProperty("PrecioMinimo", fraccionCotizada.get("precioMinimo").toString());
				props.setProperty("Aumento", fraccionCotizada.get("aumento").toString());
				props.setProperty("Ancho", Double.toString(ancho));
				props.setProperty("PrecioUnitario", fraccionCotizada.get("precioUnitario").toString());
				props.setProperty("Cantidad", fraccionCotizada.get("cantidad").toString());
				props.setProperty("Total", fraccionCotizada.get("total").toString());
				props.setProperty("Observaciones", fraccionCotizada.get("observaciones").toString());
	
	    		resultado.add(props);
	    	}
		}
		
		return resultado;
	}

}
