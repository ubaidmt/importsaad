package com.importsaad.jasper.gc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import com.excelecm.common.service.CEService;
import com.excelecm.common.service.PEService;
import com.excelecm.common.util.CommonUtils;
import com.excelecm.common.util.DateUtils;
import com.excelecm.jasper.module.IReportsCMCustomSearch;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;

public class EstadoContenedor implements IReportsCMCustomSearch {
	
	private final static SimpleDateFormat shortDateFormat = new SimpleDateFormat("dd/MM/yyyy");	
	private final static SimpleDateFormat longDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");	
	private Calendar calStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	private Calendar calEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC"));	
	private final static String claseContenedor = "Contenedor";
	private final static int maxRecords = 5000;
	private final static int timeLimit = 180;	
	
	@SuppressWarnings("unchecked")
	public List<Properties> doExecute(CEService ceService, PEService peService, String objectStoreName, String rosterName, String eventLogName, Map<String, Object> condiciones) throws Exception {
		
		List<Properties> resultado = new ArrayList<Properties>();
		shortDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));	
		
		// Build Query
		SearchScope search = new SearchScope(ceService.getOS(objectStoreName));
		SearchSQL sql = new SearchSQL();
		sql.setSelectList("NumeroContenedor, NombreCliente, NombreNaviera, NombreAgenciaAduanal, Importadora, EstadoContenedor, CierreContable, DateCreated, FechaRegistro, FechaSalidaOrigen, FechaLlegadaPuerto, FechaTerminoTransito, FechaEntregaCliente");
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

		for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) {
			// Get Row
			RepositoryRow row = it.next();
			java.util.Properties props = new java.util.Properties();			
	    	com.filenet.api.property.Properties rowProps = row.getProperties();
	    	
	    	for (Iterator<com.filenet.api.property.Property> propsIt = rowProps.iterator(); propsIt.hasNext(); )  {
	    		com.filenet.api.property.Property prop = propsIt.next();
	    		if (prop.getObjectValue() instanceof java.util.Date)
	    			props.setProperty(prop.getPropertyName(), prop.getObjectValue() == null ? "" : shortDateFormat.format(prop.getDateTimeValue()));
	    		else if (prop.getObjectValue() instanceof java.lang.Integer)
	    			props.setProperty(prop.getPropertyName(), prop.getObjectValue() == null ? "" : prop.getInteger32Value().toString());
	    		else if (prop.getObjectValue() instanceof java.lang.Boolean)
	    			props.setProperty(prop.getPropertyName(), prop.getObjectValue() == null ? "" : prop.getBooleanValue().toString());
	    		else if (prop.getObjectValue() instanceof java.lang.Double)
	    			props.setProperty(prop.getPropertyName(), prop.getObjectValue() == null ? "" : prop.getFloat64Value().toString());		    		
	    		else
	    			props.setProperty(prop.getPropertyName(), prop.getObjectValue() == null ? "" : prop.getStringValue());	    		
	    	}   	

			// SLA Llegada a Puerto
			if (rowProps.getDateTimeValue("FechaSalidaOrigen") != null && rowProps.getDateTimeValue("FechaLlegadaPuerto") == null)
				props.setProperty("SLALLegadaPuerto", Double.toString(getDiffDays(rowProps.getDateTimeValue("FechaSalidaOrigen"), new Date())));
			else if (rowProps.getDateTimeValue("FechaRegistro") != null && rowProps.getDateTimeValue("FechaLlegadaPuerto") == null)
				props.setProperty("SLALLegadaPuerto", Double.toString(getDiffDays(rowProps.getDateTimeValue("FechaRegistro"), new Date())));
			else if (rowProps.getDateTimeValue("FechaSalidaOrigen") != null && rowProps.getDateTimeValue("FechaLlegadaPuerto") != null)
				props.setProperty("SLALLegadaPuerto", Double.toString(getDiffDays(rowProps.getDateTimeValue("FechaSalidaOrigen"), rowProps.getDateTimeValue("FechaLlegadaPuerto"))));
			else if (rowProps.getDateTimeValue("FechaRegistro") != null && rowProps.getDateTimeValue("FechaLlegadaPuerto") != null)
				props.setProperty("SLALLegadaPuerto", Double.toString(getDiffDays(rowProps.getDateTimeValue("FechaRegistro"), rowProps.getDateTimeValue("FechaLlegadaPuerto"))));
			// SLA Termino de Transito
			if (rowProps.getDateTimeValue("FechaLlegadaPuerto") != null && rowProps.getDateTimeValue("FechaTerminoTransito") == null)
				props.setProperty("SLATerminoTransito", Double.toString(getDiffDays(rowProps.getDateTimeValue("FechaLlegadaPuerto"), new Date())));
			else if (rowProps.getDateTimeValue("FechaLlegadaPuerto") != null && rowProps.getDateTimeValue("FechaTerminoTransito") != null)
				props.setProperty("SLATerminoTransito", Double.toString(getDiffDays(rowProps.getDateTimeValue("FechaLlegadaPuerto"), rowProps.getDateTimeValue("FechaTerminoTransito"))));
			// SLA Entrega a Cliente
			if (rowProps.getDateTimeValue("FechaTerminoTransito") != null && rowProps.getDateTimeValue("FechaEntregaCliente") == null)
				props.setProperty("SLAEntregaCliente", Double.toString(getDiffDays(rowProps.getDateTimeValue("FechaTerminoTransito"), new Date())));
			else if (rowProps.getDateTimeValue("FechaTerminoTransito") != null && rowProps.getDateTimeValue("FechaEntregaCliente") != null)
				props.setProperty("SLAEntregaCliente", Double.toString(getDiffDays(rowProps.getDateTimeValue("FechaTerminoTransito"), rowProps.getDateTimeValue("FechaEntregaCliente"))));
			
	    	// Add Properties to Result
    		resultado.add(props);	    		
		}
		
		return resultado;		
				
	}
	
	private double getDiffDays(Date dStart, Date dEnd) throws Exception {
		calEnd.setTime(dEnd);
		calStart.setTime(dStart);
		long startTime = calStart.getTimeInMillis();
		long endTime = calEnd.getTimeInMillis();
		double diffTime = endTime - startTime;
		return diffTime / (1000 * 60 * 60 * 24);	
	}	

}
