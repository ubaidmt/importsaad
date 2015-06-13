package com.importsaad.jasper.gf;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import com.filenet.api.collection.FolderSet;
import com.filenet.api.core.Folder;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.ibm.json.java.JSONObject;

public class DetalleFacturas implements IReportsCMCustomSearch {
	
	private final static SimpleDateFormat shortDateFormat = new SimpleDateFormat("dd/MM/yyyy");	
	private final static SimpleDateFormat longDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	private final static int maxRecords = 5000;
	private final static int timeLimit = 180;	
	
	@SuppressWarnings("unchecked")
	public List<Properties> doExecute(CEService ceService, PEService peService, String objectStoreName, String rosterName, String eventLogName, Map<String, Object> condiciones) throws Exception {
		
		List<Properties> resultado = new ArrayList<Properties>();
		shortDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		// Build Query
		SearchScope search = new SearchScope(ceService.getOS(objectStoreName));
		SearchSQL sql = new SearchSQL();	
		sql.setSelectList("This, FolderName, DateCreated, NumeroFactura, FechaFactura, MontoTotal, Proveedor, Empresa, ClbJSONData, EstadoCFDI");
		sql.setFromClauseInitialValue("SolDocCase", null, false);		
		StringBuffer whereStatement = new StringBuffer();
		
		if (!CommonUtils.isEmpty(condiciones.get("Cliente")))
			whereStatement.append("This INSUBFOLDER '/Facturas/" + condiciones.get("Cliente").toString() + "'");		
		else
			whereStatement.append("This INSUBFOLDER '/Facturas'");			
		
		if (!CommonUtils.isEmpty(condiciones.get("NumeroFolio")))
			whereStatement.append(" AND FolderName LIKE '%" + condiciones.get("NumeroFolio").toString() + "%'");
		if (!CommonUtils.isEmpty(condiciones.get("NumeroFactura")))
			whereStatement.append(" AND NumeroFactura LIKE '%" + condiciones.get("NumeroFactura").toString() + "%'");
		if (!CommonUtils.isEmpty(condiciones.get("FechaSolicitudDesde")))
			whereStatement.append(" AND DateCreated >= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaSolicitudDesde").toString() + " 00:00:00")));
		if (!CommonUtils.isEmpty(condiciones.get("FechaSolicitudHasta")))
			whereStatement.append(" AND DateCreated <= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaSolicitudHasta").toString() + " 00:00:00")));		
		if (!CommonUtils.isEmpty(condiciones.get("FechaFacturaDesde")))
			whereStatement.append(" AND FechaFactura >= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaFacturaDesde").toString() + " 00:00:00")));
		if (!CommonUtils.isEmpty(condiciones.get("FechaFacturaHasta")))
			whereStatement.append(" AND FechaFactura <= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaFacturaHasta").toString() + " 00:00:00")));	
		if (!CommonUtils.isEmpty(condiciones.get("ImporteDesde")))
			whereStatement.append(" AND MontoTotal >= " + condiciones.get("ImporteDesde").toString());
		if (!CommonUtils.isEmpty(condiciones.get("ImporteHasta")))
			whereStatement.append(" AND MontoTotal <= " + condiciones.get("ImporteHasta").toString());
		if (!CommonUtils.isEmpty(condiciones.get("Proveedor")))
			whereStatement.append(" AND Proveedor = " + condiciones.get("Proveedor").toString());		
		if (!CommonUtils.isEmpty(condiciones.get("Empresa")))
			whereStatement.append(" AND Empresa = " + condiciones.get("Empresa").toString());			
		if (!CommonUtils.isEmpty(condiciones.get("EstadoFactura")))
			whereStatement.append(" AND EstadoCFDI = " + condiciones.get("EstadoFactura").toString());

		sql.setWhereClause(whereStatement.toString());	
		
	    // performance settings
		sql.setMaxRecords(maxRecords);
		sql.setTimeLimit(timeLimit);			
		
		// Execute Query
		FolderSet folSet = (FolderSet) search.fetchObjects(sql, 50, null, true);		
		
		for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) 
		{
			Folder solicitud = it.next();
			com.filenet.api.property.Properties solicitudProps = solicitud.getProperties();
			java.util.Properties props = new java.util.Properties();
			
			// Property Values
			props.setProperty("Folio", solicitudProps.getStringValue("FolderName"));	
			props.setProperty("DateCreated", shortDateFormat.format(solicitudProps.getDateTimeValue("DateCreated")));
			props.setProperty("FechaFactura", (solicitudProps.getObjectValue("FechaFactura") == null ? "" : shortDateFormat.format(solicitudProps.getDateTimeValue("FechaFactura"))));
			props.setProperty("NumeroFactura", (solicitudProps.getObjectValue("NumeroFactura") == null ? "" : solicitudProps.getStringValue("NumeroFactura")));
			props.setProperty("Importe", Double.toString(solicitudProps.getFloat64Value("MontoTotal")));
			props.setProperty("Proveedor", (solicitudProps.getEngineObjectValue("Proveedor") == null ? "" : ((Folder)solicitudProps.getEngineObjectValue("Proveedor")).get_FolderName()));
			props.setProperty("Cliente", getCliente(((Folder)solicitudProps.getEngineObjectValue("This"))).get_FolderName());
			props.setProperty("Empresa", (solicitudProps.getObjectValue("Empresa") == null ? "" : ((Folder)solicitudProps.getEngineObjectValue("Empresa")).get_FolderName()));
			props.setProperty("Estado", Integer.toString(solicitudProps.getInteger32Value("EstadoCFDI")));
			
			// Binary Values
			byte[] data = solicitudProps.getBinaryValue("ClbJSONData");
			JSONObject jsonObj = JSONObject.parse(new String(data));	
			props.setProperty("PorcentajeComisionProveedor", jsonObj.get("porcentajeComisionProveedor").toString());
			props.setProperty("MontoComisionProveedor", jsonObj.get("montoComisionProveedor").toString());		
			props.setProperty("PorcentajeComisionDistribuidor", jsonObj.get("porcentajeComisionDistribuidor").toString());
			props.setProperty("MontoComisionDistribuidor", jsonObj.get("montoComisionDistribuidor").toString());
			
    		resultado.add(props);			
					
		}
		
		return resultado;
	}
	
	private static Folder getCliente(Folder factura) throws Exception {
		Folder cliente = factura;
		while (!cliente.getClassName().equals("SolDocCliente"))
			cliente = cliente.get_Parent();
		return cliente;
	}	

}
