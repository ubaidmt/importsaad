package com.importsaad.jasper.gf;

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
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.core.Folder;
import com.filenet.api.core.Document;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;

public class Pagos implements IReportsCMCustomSearch {
	
	private final static SimpleDateFormat shortDateFormat = new SimpleDateFormat("dd/MM/yyyy");	
	private final static SimpleDateFormat longDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	@SuppressWarnings({ "unchecked" })
	public List<Properties> doExecute(CEService ceService, PEService peService, String objectStoreName, String rosterName, String eventLogName, Map<String, Object> condiciones) throws Exception {
		
		List<Properties> resultado = new ArrayList<Properties>();
		
		// Build Query
		SearchScope search = new SearchScope(ceService.getOS(objectStoreName));
		SearchSQL sql = new SearchSQL();	
		sql.setSelectList("This, Proveedor, Empresa, DateCreated, FechaPago, MontoTotal, MetodoPago, Banco, Referencia, TipoPago, FoldersFiledIn");
		sql.setFromClauseInitialValue("SolDocPago", null, false);		
		StringBuffer whereStatement = new StringBuffer();
		
		whereStatement.append("DateCreated >= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaSolicitudDesde").toString() + " 00:00:00")));
		whereStatement.append(" AND DateCreated <= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaSolicitudHasta").toString() + " 23:59:59")));
		whereStatement.append(" AND isCurrentVersion = TRUE");		
		
		if (!CommonUtils.isEmpty(condiciones.get("Cliente")))
			whereStatement.append(" AND This INSUBFOLDER '/Facturas/" + condiciones.get("Cliente").toString());		
		if (!CommonUtils.isEmpty(condiciones.get("TipoPago")))
			whereStatement.append(" AND TipoPago = " + condiciones.get("TipoPago").toString());		
		if (!CommonUtils.isEmpty(condiciones.get("FechaPagoDesde")))
			whereStatement.append(" AND FechaPago >= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaPagoDesde").toString() + " 00:00:00")));
		if (!CommonUtils.isEmpty(condiciones.get("FechaPagoHasta")))
			whereStatement.append(" AND FechaPago <= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaPagoHasta").toString() + " 23:59:59")));	
		if (!CommonUtils.isEmpty(condiciones.get("ImporteDesde")))
			whereStatement.append(" AND MontoTotal >= " + condiciones.get("ImporteDesde").toString());
		if (!CommonUtils.isEmpty(condiciones.get("ImporteHasta")))
			whereStatement.append(" AND MontoTotal <= " + condiciones.get("ImporteHasta").toString());		
		if (!CommonUtils.isEmpty(condiciones.get("Proveedor")))
			whereStatement.append(" AND Proveedor = " + condiciones.get("Proveedor").toString());		
		if (!CommonUtils.isEmpty(condiciones.get("Empresa")))
			whereStatement.append(" AND Empresa = " + condiciones.get("Empresa").toString());

		sql.setWhereClause(whereStatement.toString());			
		
		// Execute Query
		DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, null, null, true);		
		
		for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
		{
			Document pago = it.next();
			com.filenet.api.property.Properties pagoProps = pago.getProperties();
			java.util.Properties props = new java.util.Properties();
				
			// Property Values	
			props.setProperty("TipoPago", Integer.toString(pagoProps.getInteger32Value("TipoPago")));
			props.setProperty("DateCreated", shortDateFormat.format(pagoProps.getDateTimeValue("DateCreated")));
			props.setProperty("FechaPago", (pagoProps.getObjectValue("FechaPago") == null ? "" : shortDateFormat.format(getUTFCalendar(pagoProps.getDateTimeValue("FechaPago")).getTime())));
			props.setProperty("Importe", Double.toString(pagoProps.getFloat64Value("MontoTotal")));
			props.setProperty("MetodoPago", Integer.toString(pagoProps.getInteger32Value("MetodoPago")));
			props.setProperty("Banco", (pagoProps.getInteger32Value("Banco") == null ? "" : Integer.toString(pagoProps.getInteger32Value("Banco"))));
			props.setProperty("Referencia", (pagoProps.getStringValue("Referencia") == null ? "" : pagoProps.getStringValue("Referencia")));
			props.setProperty("Proveedor", (pagoProps.getEngineObjectValue("Proveedor") == null ? "" : ((Folder)pagoProps.getEngineObjectValue("Proveedor")).get_FolderName()));
			Folder cliente = getCliente(((Document)pagoProps.getEngineObjectValue("This")));
			props.setProperty("Cliente", (cliente == null ? "" : cliente.get_FolderName()));
			props.setProperty("Empresa", (pagoProps.getObjectValue("Empresa") == null ? "" : ((Folder)pagoProps.getEngineObjectValue("Empresa")).get_FolderName()));
			props.setProperty("Folios", getFoliosAsociados(pago));
			
    		resultado.add(props);					
								
		}
		
		return resultado;
	}
	
	@SuppressWarnings("unchecked")
	private static String getFoliosAsociados(Document pago) throws Exception {
		FolderSet folSet = pago.get_FoldersFiledIn();
		String folios = "";
		for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) 
		{
			Folder solicitud = it.next();
			folios += solicitud.get_FolderName() + " ";
		}
		if (!folios.isEmpty())
			folios = folios.substring(0, folios.length() - 1);	
		return folios;
	}
	
	private static Folder getCliente(Document pago) throws Exception {
		if (pago.get_FoldersFiledIn().isEmpty())
			return null;
		
		Folder cliente = (Folder) pago.get_FoldersFiledIn().iterator().next();
		while (!cliente.getClassName().equals("SolDocCliente"))
			cliente = cliente.get_Parent();
		return cliente;
	}	
	
	private static Calendar getUTFCalendar(Date dateObj) throws Exception {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(dateObj);	
		return cal;
	}		

}
