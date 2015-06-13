package com.importsaad.jasper.gc;

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
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.core.Folder;
import com.filenet.api.core.Document;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class ResumenContable implements IReportsCMCustomSearch {
	
	private final static SimpleDateFormat shortDateFormat = new SimpleDateFormat("dd/MM/yyyy");	
	private final static SimpleDateFormat longDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
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
		sql.setSelectList("ContainedDocuments, NumeroContenedor, DateCreated, Importadora, EstadoContenedor, FechaRegistro, FechaEntregaCliente, NombreCliente, NombreNaviera, NombreAgenciaAduanal, Importadora, CierreContable");
		sql.setFromClauseInitialValue(claseContenedor, null, false);		
		StringBuffer whereStatement = new StringBuffer();
		whereStatement.append("This INSUBFOLDER '/Contenedores'");		
		
		if (!CommonUtils.isEmpty(condiciones.get("NumeroContenedor")))
			whereStatement.append(" AND c.NumeroContenedor LIKE '%" + condiciones.get("NumeroContenedor").toString() + "%'");
		if (!CommonUtils.isEmpty(condiciones.get("FechaCreacionDesde")))
			whereStatement.append(" AND c.DateCreated >= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaCreacionDesde").toString() + " 00:00:00")));
		if (!CommonUtils.isEmpty(condiciones.get("FechaCreacionHasta")))
			whereStatement.append(" AND c.DateCreated <= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaCreacionHasta").toString() + " 00:00:00")));
		if (!CommonUtils.isEmpty(condiciones.get("FechaRegistroDesde")))
			whereStatement.append(" AND c.FechaRegistro >= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaRegistroDesde").toString() + " 00:00:00")));
		if (!CommonUtils.isEmpty(condiciones.get("FechaRegistroHasta")))
			whereStatement.append(" AND c.FechaRegistro <= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaRegistroHasta").toString() + " 00:00:00")));
		if (!CommonUtils.isEmpty(condiciones.get("FechaEntregaClienteDesde")))
			whereStatement.append(" AND c.FechaEntregaCliente >= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaEntregaClienteDesde").toString() + " 00:00:00")));			
		if (!CommonUtils.isEmpty(condiciones.get("FechaEntregaClienteHasta")))
			whereStatement.append(" AND c.FechaEntregaCliente <= " + DateUtils.convertLocalTimeToUTC(longDateFormat.parse(condiciones.get("FechaEntregaClienteHasta").toString() + " 00:00:00")));			
		if (!CommonUtils.isEmpty(condiciones.get("EstadoContenedor")))
			whereStatement.append(" AND c.EstadoContenedor = " + condiciones.get("EstadoContenedor").toString());
		if (!CommonUtils.isEmpty(condiciones.get("CierreContable")))
			whereStatement.append(" AND c.CierreContable = " + condiciones.get("CierreContable").toString());
		if (!CommonUtils.isEmpty(condiciones.get("NombreCliente")))
			whereStatement.append(" AND c.NombreCliente = '" + condiciones.get("NombreCliente").toString() + "'");
		if (!CommonUtils.isEmpty(condiciones.get("NombreNaviera")))
			whereStatement.append(" AND c.NombreNaviera = '" + condiciones.get("NombreNaviera").toString() + "'");
		if (!CommonUtils.isEmpty(condiciones.get("NombreAgenciaAduanal")))
			whereStatement.append(" AND c.NombreAgenciaAduanal = '" + condiciones.get("NombreAgenciaAduanal").toString() + "'");
		if (!CommonUtils.isEmpty(condiciones.get("Importadora")))
			whereStatement.append(" AND c.Importadora = '" + condiciones.get("Importadora").toString() + "'");		
		
		sql.setWhereClause(whereStatement.toString());
		
	    // performance settings
		sql.setMaxRecords(maxRecords);
		sql.setTimeLimit(timeLimit);			
		
		// Execute Query
		FolderSet folSet = (FolderSet) search.fetchObjects(sql, 50, null, true);
		
		for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) 
		{
			Folder contenedor = it.next();
			com.filenet.api.property.Properties contenedorProps = contenedor.getProperties();
			java.util.Properties props = new java.util.Properties();
			String numContenedor = contenedorProps.getStringValue("NumeroContenedor");
			
			// Datos Generales del Contenedor
			props.setProperty("NumeroContenedor", numContenedor);	
			props.setProperty("ClaseDocumento", contenedor.getClassName());
			props.setProperty("NombreCliente", contenedorProps.getStringValue("NombreCliente"));
			props.setProperty("NombreNaviera", contenedorProps.getStringValue("NombreNaviera"));
			props.setProperty("NombreAgenciaAduanal", contenedorProps.getStringValue("NombreAgenciaAduanal"));
			props.setProperty("Importadora", contenedorProps.getStringValue("Importadora"));
			props.setProperty("EstadoContenedor", Integer.toString(contenedorProps.getInteger32Value("EstadoContenedor")));
			props.setProperty("CierreContable", Boolean.toString(contenedorProps.getBooleanValue("CierreContable")));
    		resultado.add(props);
			
			DocumentSet docSet = contenedor.get_ContainedDocuments();
			for (Iterator<Document> it2 = docSet.iterator(); it2.hasNext(); ) 
			{
				Document doc = it2.next();
				com.filenet.api.property.Properties docProps = doc.getProperties();
				props = new java.util.Properties();
				
				// Set propiedades generales
				props.setProperty("NumeroContenedor", numContenedor);
				props.setProperty("ClaseDocumento", doc.getClassName());
				
				// Datos de la Cuenta de Gastos
				if (doc.getClassName().equals("CuentaGastos"))
				{
					props.setProperty("NumeroFactura", docProps.getObjectValue("NumeroFactura") == null ? "" : docProps.getStringValue("NumeroFactura"));
					props.setProperty("FechaFactura", docProps.getObjectValue("FechaFactura") == null ? "" : shortDateFormat.format(docProps.getDateTimeValue("FechaFactura")));
					props.setProperty("NumeroPedimento", docProps.getObjectValue("NumeroPedimento") == null ? "" : docProps.getStringValue("NumeroPedimento"));
					props.setProperty("Gastos", docProps.getObjectValue("Gastos") == null ? "" : Double.toString(docProps.getFloat64Value("Gastos")));
					props.setProperty("Honorarios", docProps.getObjectValue("Honorarios") == null ? "" : Double.toString(docProps.getFloat64Value("Honorarios")));
					props.setProperty("IVA", docProps.getObjectValue("IVA") == null ? "" : Double.toString(docProps.getFloat64Value("IVA")));
					props.setProperty("Impuestos", docProps.getObjectValue("Impuestos") == null ? "" : Double.toString(docProps.getFloat64Value("Impuestos")));
					props.setProperty("Anticipo", docProps.getObjectValue("Anticipo") == null ? "" : Double.toString(docProps.getFloat64Value("Anticipo")));
					props.setProperty("Saldo", docProps.getObjectValue("Saldo") == null ? "" : Double.toString(docProps.getFloat64Value("Saldo")));
					props.setProperty("ValorAduanal", docProps.getObjectValue("ValorAduanal") == null ? "" : Double.toString(docProps.getFloat64Value("ValorAduanal")));
					props.setProperty("TipoCambio", docProps.getObjectValue("TipoCambio") == null ? "" : Double.toString(docProps.getFloat64Value("TipoCambio")));
					resultado.add(props);		
				}
				// Datos del Pedimento Desglosado
				else if (doc.getClassName().equals("PedimentoDesglosado"))
				{
	    	    	double desgloseMonto = 0;
	    	    	byte[] data = docProps.getBinaryValue("ClbJSONData");
	    	    	if (data != null) {
		    			JSONArray jsonArray = JSONArray.parse(new String(data));		
		    	    	for (Object obj : jsonArray) {
		    	    		JSONObject jsonObj = (JSONObject) obj;
		    	    		desgloseMonto += Double.parseDouble(jsonObj.get("monto").toString());
		    	    	}
	    	    	}
					props.setProperty("DesgloseImpuestos", Double.toString(desgloseMonto));
					resultado.add(props);		
				}
				// Datos de Comprobantes que afectan a la Cuenta de Gastos
				else if (doc.getClassName().equals("Comprobante"))
				{
					props.setProperty("ConceptoComprobante", docProps.getObjectValue("ConceptoComprobante") == null ? "" : docProps.getStringValue("ConceptoComprobante"));
					props.setProperty("TipoComprobante", docProps.getObjectValue("TipoComprobante") == null ? "" : Integer.toString(docProps.getInteger32Value("TipoComprobante")));
					props.setProperty("AfectaCuentaGastos", docProps.getObjectValue("AfectaCuentaGastos") == null ? "true" : Boolean.toString(docProps.getBooleanValue("AfectaCuentaGastos")));
					props.setProperty("MontoTotal", docProps.getObjectValue("MontoTotal") == null ? "" : Double.toString(docProps.getFloat64Value("MontoTotal")));
					resultado.add(props);		
				}
				// Datos de la Factura Comercial
				else if (doc.getClassName().equals("FacturaComercial"))
				{
					props.setProperty("NumeroFactura", docProps.getObjectValue("NumeroFactura") == null ? "" : docProps.getStringValue("NumeroFactura"));
					props.setProperty("FechaFactura", docProps.getObjectValue("FechaFactura") == null ? "" : shortDateFormat.format(docProps.getDateTimeValue("FechaFactura")));
					props.setProperty("IDFiscal", docProps.getObjectValue("IDFiscal") == null ? "" : docProps.getStringValue("IDFiscal"));
					props.setProperty("MontoTotal", docProps.getObjectValue("MontoTotal") == null ? "" : Double.toString(docProps.getFloat64Value("MontoTotal")));
					resultado.add(props);		
				}
				// Datos del Bill Of Loading
				else if (doc.getClassName().equals("BillOfLoading"))
				{
					props.setProperty("NumeroBL", docProps.getObjectValue("NumeroBL") == null ? "" : docProps.getStringValue("NumeroBL"));
					props.setProperty("NumeroBultos", docProps.getObjectValue("NumeroBultos") == null ? "" : Integer.toString(docProps.getInteger32Value("NumeroBultos")));
					props.setProperty("TipoMercancia", docProps.getObjectValue("TipoMercancia") == null ? "" : docProps.getStringValue("TipoMercancia"));
					resultado.add(props);		
				}	
				// Datos de Anexos Cuenta de Gastos
				else if (doc.getClassName().equals("AnexoCuentaGastos"))
				{
					props.setProperty("TipoAnexoCuentaGastos", docProps.getObjectValue("TipoAnexoCuentaGastos") == null ? "" : docProps.getStringValue("TipoAnexoCuentaGastos"));
					resultado.add(props);					
				}
				// Otras Clases Documentales
				else
				{
					resultado.add(props);	
				}
				
			}
		}
		
		return resultado;		
		
	}	
	
}
