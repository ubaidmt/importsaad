package com.importsaad.jasper.gc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
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

public class FraccionesCotizadas implements IReportsCMCustomSearch {
	
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
		
		// fracciones consideradas
		List<String> fracciones = new ArrayList<String>();
		if (condiciones.get("Fracciones") != null)
		fracciones = Arrays.asList(condiciones.get("Fracciones").toString().split(","));
	    
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
	    		JSONObject fraccion = (JSONObject) obj;
				java.util.Properties props = new java.util.Properties();
				
				String medida = "KG";
				double ancho = Double.parseDouble(fraccion.get("ancho").toString());
				if (fraccion.containsKey("medida") && fraccion.get("medida").toString().equals("in")) {
					 medida = "IN";
					 ancho = ancho * 0.0254;
				}
				
				// si la fraccion cotizada se encuentra considerada
				if (fracciones.isEmpty() || fracciones.contains(fraccion.get("fraccion").toString())) {
					props.setProperty("Fraccion", fraccion.get("fraccion").toString());
					props.setProperty("Descripcion", "FABRIC");
					props.setProperty("UnidadComercial", fraccion.get("unidadComercial").toString());
					props.setProperty("PrecioMinimo", fraccion.get("precioMinimo").toString());
					props.setProperty("Aumento", fraccion.get("aumento").toString());
					props.setProperty("Ancho", Double.toString(ancho));					
					props.setProperty("Cantidad", fraccion.get("cantidad").toString());
					props.setProperty("Medida", medida);
					props.setProperty("Unitario", fraccion.get("precioUnitario").toString());
		    		resultado.add(props);
				}
	    	}
		}
		
		return resultado;
	}

}
