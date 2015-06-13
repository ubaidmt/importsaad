package com.importsaad.jasper.gc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.excelecm.common.service.CEService;
import com.excelecm.common.service.PEService;
import com.excelecm.common.util.CommonUtils;
import com.excelecm.common.util.DateUtils;
import com.excelecm.jasper.module.IReportsCMCustomSearch;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class DesgloseGastos implements IReportsCMCustomSearch {
			
	private final static SimpleDateFormat longDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	private final static String claseContenedor = "Contenedor";
	private final static int maxRecords = 5000;
	private final static int timeLimit = 180;	
	
	@SuppressWarnings("unchecked")
	public List<Properties> doExecute(CEService ceService, PEService peService, String objectStoreName, String rosterName, String eventLogName, Map<String, Object> condiciones) throws Exception {
		
		List<Properties> resultado = new ArrayList<Properties>();	
		
		// Build Query
		SearchScope search = new SearchScope(ceService.getOS(objectStoreName));
		SearchSQL sql = new SearchSQL();
		sql.setSelectList("PathName, NumeroContenedor, DateCreated, Importadora, EstadoContenedor, FechaRegistro, FechaEntregaCliente, NombreCliente, NombreNaviera, NombreAgenciaAduanal, Importadora, CierreContable");
		sql.setFromClauseInitialValue(claseContenedor, null, false);		
		StringBuffer whereStatement = new StringBuffer();
		whereStatement.append("This INSUBFOLDER '/Contenedores'");		
		
		if (!CommonUtils.isEmpty(condiciones.get("NumeroContenedor")))
			whereStatement.append(" AND NumeroContenedor LIKE '%" + condiciones.get("NumeroContenedor").toString() + "%'");
		if (!CommonUtils.isEmpty(condiciones.get("FechaCreacionDesde")))
			whereStatement.append(" AND DateCreated >= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaCreacionDesde").toString() + " 00:00:00")));
		if (!CommonUtils.isEmpty(condiciones.get("FechaCreacionHasta")))
			whereStatement.append(" AND DateCreated <= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaCreacionHasta").toString() + " 00:00:00")));
		if (!CommonUtils.isEmpty(condiciones.get("FechaRegistroDesde")))
			whereStatement.append(" AND FechaRegistro >= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaRegistroDesde").toString() + " 00:00:00")));
		if (!CommonUtils.isEmpty(condiciones.get("FechaRegistroHasta")))
			whereStatement.append(" AND FechaRegistro <= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaRegistroHasta").toString() + " 00:00:00")));
		if (!CommonUtils.isEmpty(condiciones.get("FechaEntregaClienteDesde")))
			whereStatement.append(" AND FechaEntregaCliente >= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaEntregaClienteDesde").toString() + " 00:00:00")));			
		if (!CommonUtils.isEmpty(condiciones.get("FechaEntregaClienteHasta")))
			whereStatement.append(" AND FechaEntregaCliente <= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaEntregaClienteHasta").toString() + " 00:00:00")));			
		if (!CommonUtils.isEmpty(condiciones.get("EstadoContenedor")))
			whereStatement.append(" AND EstadoContenedor = " + condiciones.get("EstadoContenedor").toString());
		if (!CommonUtils.isEmpty(condiciones.get("CierreContable")))
			whereStatement.append(" AND CierreContable = " + condiciones.get("CierreContable").toString());
		if (!CommonUtils.isEmpty(condiciones.get("NombreCliente")))
			whereStatement.append(" AND NombreCliente = '" + condiciones.get("NombreCliente").toString() + "'");
		if (!CommonUtils.isEmpty(condiciones.get("NombreNaviera")))
			whereStatement.append(" AND NombreNaviera = '" + condiciones.get("NombreNaviera").toString() + "'");
		if (!CommonUtils.isEmpty(condiciones.get("NombreAgenciaAduanal")))
			whereStatement.append(" AND NombreAgenciaAduanal = '" + condiciones.get("NombreAgenciaAduanal").toString() + "'");
		if (!CommonUtils.isEmpty(condiciones.get("Importadora")))
			whereStatement.append(" AND Importadora = '" + condiciones.get("Importadora").toString() + "'");

		sql.setWhereClause(whereStatement.toString());
		
	    // performance settings
		sql.setMaxRecords(maxRecords);
		sql.setTimeLimit(timeLimit);			
		
		// Execute Query
		RepositoryRowSet rowSet = search.fetchRows(sql, 50, null, true);
		
		for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		{
			RepositoryRow row = it.next();
			com.filenet.api.property.Properties rowProps = row.getProperties();
			String sqlStm = "SELECT d.Id, d.ClbJSONData FROM CuentaGastos d WITH EXCLUDESUBCLASSES INNER JOIN ReferentialContainmentRelationship r ON d.This = r.Head WHERE r.Tail = OBJECT('" + rowProps.getStringValue("PathName") + "') AND d.IsCurrentVersion = True";
			RepositoryRowSet rowSet2 = search.fetchRows(new SearchSQL(sqlStm), 50, null, true);
			for (Iterator<RepositoryRow> it2 = rowSet2.iterator(); it2.hasNext(); ) 
			{
				RepositoryRow row2 = it2.next();
				com.filenet.api.property.Properties row2Props = row2.getProperties();
				
		    	byte[] data = row2Props.getBinaryValue("ClbJSONData"); 
		    	if (data != null) {
		    		JSONObject jsonObj = JSONObject.parse(new String(data));
		    		if (!jsonObj.containsKey("desgloseGastos"))
		    			continue;
		    		
		    		JSONArray jsonArray = (JSONArray) jsonObj.get("desgloseGastos");
			    	for (Object obj : jsonArray) 
			    	{
			    		java.util.Properties props = new java.util.Properties();
			    		
						// Datos Contenedor
						props.setProperty("NumeroContenedor", rowProps.getStringValue("NumeroContenedor"));
						props.setProperty("NombreCliente", rowProps.getStringValue("NombreCliente"));
						props.setProperty("NombreNaviera", rowProps.getStringValue("NombreNaviera"));
						props.setProperty("NombreAgenciaAduanal", rowProps.getStringValue("NombreAgenciaAduanal"));
						props.setProperty("Importadora", rowProps.getStringValue("Importadora"));
						props.setProperty("EstadoContenedor", rowProps.getInteger32Value("EstadoContenedor").toString());
						props.setProperty("CierreContable", rowProps.getBooleanValue("CierreContable").toString());	
						
			    		// Datos Desglose Gastos
						jsonObj = (JSONObject) obj;
						props.setProperty("TipoGasto", jsonObj.get("gasto").toString());
						props.setProperty("MontoGasto", jsonObj.get("monto").toString());
			    		
						// Add Properties to Result				
			    		resultado.add(props);
			    	}	    		
		    	}
			}
		}			

		return resultado;		
	
	}

}
