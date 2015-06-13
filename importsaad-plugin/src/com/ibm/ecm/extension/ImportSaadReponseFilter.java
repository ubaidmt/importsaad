package com.ibm.ecm.extension;

import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletRequest;
import com.ibm.ecm.extension.PluginResponseFilter;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.ecm.json.JSONResultSetColumn;
import com.ibm.ecm.json.JSONResultSetResponse;
import com.ibm.ecm.json.JSONResultSetRow;
import com.ibm.json.java.JSONObject;

public class ImportSaadReponseFilter extends PluginResponseFilter {
	
	private final static DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private final static DecimalFormat decimalformat = new DecimalFormat("0.00");
	
	private static Calendar calStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	private static Calendar calEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	
	
	@Override
	public String[] getFilteredServices() {
		return new String[] { "/p8/search" };
	}

	@Override
	public void filter(String serverType, PluginServiceCallbacks callbacks, HttpServletRequest request, JSONObject jsonResponse) throws Exception {
	
		//String desktopId = request.getParameter("desktop");
		JSONResultSetRow row;
				
		if (!(jsonResponse instanceof JSONResultSetResponse))
			return;

		JSONResultSetResponse jsonResultSetResponse = (JSONResultSetResponse) jsonResponse;
		
		if (jsonResultSetResponse.getRowCount() == 0)
			return;
		
		// Las columnas adicionales de SLA son desplegadas cuando existe una carpeta de Contenedor en la lista de resultados
		boolean showSLAColumns = false;
		for (int i = 0; i < jsonResultSetResponse.getRowCount(); i++) 
		{
			row = jsonResultSetResponse.getRow(i);
			String template = (String) row.get("template"); // class name
			if (template != null && template.equals("Contenedor")) {
				showSLAColumns = true;
				break;
			}
		}
		
		if (!showSLAColumns)
			return;
		
		// Establece Timezone para dateformat
		dateformat.setCalendar(calStart);
		dateformat.setCalendar(calEnd);
					
		// Crea columnas adicionales para mostrar estatus SLA
		JSONResultSetColumn columnSLALlegadaPuerto = new JSONResultSetColumn("Estado Llegada a Puerto", "150px", "SLALlegadaPuerto", null, true);
		JSONResultSetColumn columnSLATerminoTransito = new JSONResultSetColumn("Estado Término de Tránsito", "160px", "SLATerminoTransito", null, true);
		JSONResultSetColumn columnSLAEntregaCliente = new JSONResultSetColumn("Estado Entrega a Cliente", "150px", "SLAEntregaCliente", null, true);

		jsonResultSetResponse.addColumn(columnSLALlegadaPuerto);
		jsonResultSetResponse.addColumn(columnSLATerminoTransito);
		jsonResultSetResponse.addColumn(columnSLAEntregaCliente);
		
		for (int i = 0; i < jsonResultSetResponse.getRowCount(); i++) 
		{	
			row = jsonResultSetResponse.getRow(i);
			String sFechaRegistro = (String)row.getAttributeValue("FechaRegistro");
			String sFechaSalidaOrigen = (String)row.getAttributeValue("FechaSalidaOrigen");
			String sFechaLlegadaPuerto = (String)row.getAttributeValue("FechaLlegadaPuerto");
			String sFechaTerminoTransito = (String)row.getAttributeValue("FechaTerminoTransito");
			String sFechaEntregaCliente = (String)row.getAttributeValue("FechaEntregaCliente");
			
			// SLA Llegada a Puerto
			if (sFechaSalidaOrigen != null && sFechaLlegadaPuerto == null)
				row.addAttribute("SLALlegadaPuerto", getDiffDays(sFechaSalidaOrigen, null), JSONResultSetRow.TYPE_DOUBLE, null, null);
			else if (sFechaRegistro != null && sFechaLlegadaPuerto == null)
				row.addAttribute("SLALlegadaPuerto", getDiffDays(sFechaRegistro, null), JSONResultSetRow.TYPE_DOUBLE, null, null);
			else if (sFechaSalidaOrigen != null && sFechaLlegadaPuerto != null)
				row.addAttribute("SLALlegadaPuerto", getDiffDays(sFechaSalidaOrigen, sFechaLlegadaPuerto), JSONResultSetRow.TYPE_DOUBLE, null, null);
			else if (sFechaRegistro != null && sFechaLlegadaPuerto != null)
				row.addAttribute("SLALlegadaPuerto", getDiffDays(sFechaRegistro, sFechaLlegadaPuerto), JSONResultSetRow.TYPE_DOUBLE, null, null);

			// SLA Termino de Transito
			if (sFechaLlegadaPuerto != null && sFechaTerminoTransito == null)
				row.addAttribute("SLATerminoTransito", getDiffDays(sFechaLlegadaPuerto, null), JSONResultSetRow.TYPE_DOUBLE, null, null);
			else if (sFechaLlegadaPuerto != null && sFechaTerminoTransito != null)
				row.addAttribute("SLATerminoTransito", getDiffDays(sFechaLlegadaPuerto, sFechaTerminoTransito), JSONResultSetRow.TYPE_DOUBLE, null, null);
		
			// SLA Entrega a Cliente
			if (sFechaTerminoTransito != null && sFechaEntregaCliente == null)
				row.addAttribute("SLAEntregaCliente", getDiffDays(sFechaTerminoTransito, null), JSONResultSetRow.TYPE_DOUBLE, null, null);
			else if (sFechaTerminoTransito != null && sFechaEntregaCliente != null)
				row.addAttribute("SLAEntregaCliente", getDiffDays(sFechaTerminoTransito, sFechaEntregaCliente), JSONResultSetRow.TYPE_DOUBLE, null, null);
		}
		
		for (int i = 0; i < jsonResultSetResponse.getColumnCount(); i++) {
			JSONResultSetColumn column = jsonResultSetResponse.getColumn(i);
			String columnName = (String) column.get("field");
			if (columnName != null) {
				if (columnName.equals("EstadoContenedor"))
					column.put("decorator", "estadoContendorDecorator");
				else if (columnName.equals("SLALlegadaPuerto"))
					column.put("decorator", "slaLlegadaPuertoDecorator");
				else if (columnName.equals("SLATerminoTransito"))
					column.put("decorator", "slaTerminoTransitoDecorator");
				else if (columnName.equals("SLAEntregaCliente"))
					column.put("decorator", "slaEntregaClienteDecorator");					
			}
		}			

	}
	
	private double getDiffDays(String sFechaStart, String sFechaEnd) throws Exception {
		Date dStart = new Date();
		Date dEnd = new Date();
		if (sFechaStart != null)
			dStart = dateformat.parse(sFechaStart);
		if (sFechaEnd != null)
			dEnd = dateformat.parse(sFechaEnd);
		calEnd.setTime(dEnd);
		calStart.setTime(dStart);
		long startTime = calStart.getTimeInMillis();
		long endTime = calEnd.getTimeInMillis();
		double diffTime = endTime - startTime;
		diffTime = diffTime / (1000 * 60 * 60 * 24);
		return Double.parseDouble(decimalformat.format(diffTime));
	}

}
