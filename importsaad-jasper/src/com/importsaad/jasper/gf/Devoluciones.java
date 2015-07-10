package com.importsaad.jasper.gf;

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
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.core.Folder;
import com.filenet.api.core.Document;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;

public class Devoluciones implements IReportsCMCustomSearch {
	
	private final static SimpleDateFormat shortDateFormat = new SimpleDateFormat("dd/MM/yyyy");	
	private final static SimpleDateFormat longDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	
	@SuppressWarnings({ "unchecked" })
	public List<Properties> doExecute(CEService ceService, PEService peService, String objectStoreName, String rosterName, String eventLogName, Map<String, Object> condiciones) throws Exception {
		
		List<Properties> resultado = new ArrayList<Properties>();
		
		// Build Query
		SearchScope search = new SearchScope(ceService.getOS(objectStoreName));
		SearchSQL sql = new SearchSQL();	
		sql.setSelectList("This, Proveedor, Empresa, DateCreated, MontoTotal, Saldo, TipoDevolucion, Pendiente, FoldersFiledIn");
		sql.setFromClauseInitialValue("SolDocDevolucion", null, false);		
		StringBuffer whereStatement = new StringBuffer();
		
		whereStatement.append("DateCreated >= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaSolicitudDesde").toString() + " 00:00:00")));
		whereStatement.append(" AND DateCreated <= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaSolicitudHasta").toString() + " 23:59:59")));
		whereStatement.append(" AND isCurrentVersion = TRUE");		
		
		if (!CommonUtils.isEmpty(condiciones.get("Cliente")))
			whereStatement.append(" AND This INSUBFOLDER '/Facturas/" + condiciones.get("Cliente").toString());		
		if (!CommonUtils.isEmpty(condiciones.get("TipoDevolucion")))
			whereStatement.append(" AND TipoDevolucion = " + condiciones.get("TipoDevolucion").toString());		
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
		if (!CommonUtils.isEmpty(condiciones.get("EstadoSaldo")))
			whereStatement.append(" AND Saldo " + (condiciones.get("EstadoSaldo").toString().equals("0") ? "<> 0" : "= 0"));	
		if (!CommonUtils.isEmpty(condiciones.get("EstadoDevolucion")))
			whereStatement.append(" AND Pendiente = " + (condiciones.get("EstadoDevolucion").toString().equals("0") ? "True" : "False"));			

		sql.setWhereClause(whereStatement.toString());		
		
		// Execute Query
		DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, null, null, true);		
		
		for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
		{
			Document pago = it.next();
			com.filenet.api.property.Properties devolucionProps = pago.getProperties();
			java.util.Properties props = new java.util.Properties();
				
			// Property Values	
			props.setProperty("TipoDevolucion", Integer.toString(devolucionProps.getInteger32Value("TipoDevolucion")));
			props.setProperty("DateCreated", shortDateFormat.format(devolucionProps.getDateTimeValue("DateCreated")));
			props.setProperty("Importe", Double.toString(devolucionProps.getFloat64Value("MontoTotal")));
			props.setProperty("Saldo", Double.toString(devolucionProps.getFloat64Value("Saldo")));
			props.setProperty("Proveedor", (devolucionProps.getEngineObjectValue("Proveedor") == null ? "" : ((Folder)devolucionProps.getEngineObjectValue("Proveedor")).get_FolderName()));
			Folder cliente = getCliente(((Document)devolucionProps.getEngineObjectValue("This")));
			props.setProperty("Cliente", (cliente == null ? "" : cliente.get_FolderName()));
			props.setProperty("Empresa", (devolucionProps.getObjectValue("Empresa") == null ? "" : ((Folder)devolucionProps.getEngineObjectValue("Empresa")).get_FolderName()));
			props.setProperty("Estado", (devolucionProps.getBooleanValue("Pendiente") ? "0" : "1"));
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

}
