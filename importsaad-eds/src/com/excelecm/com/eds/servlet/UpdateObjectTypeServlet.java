package com.excelecm.com.eds.servlet;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.excelecm.com.eds.services.ProveedoresService;
import com.excelecm.com.eds.services.ClientesService;
import com.excelecm.com.eds.services.ListasService;
import com.excelecm.com.eds.services.SolDocProveedores;
import com.excelecm.com.eds.services.SolDocEmpresas;
import com.excelecm.common.settings.ConfigurationSettings;
import com.excelecm.common.settings.NavigatorSettings;

/**
 * Servlet implementation class UpdateObjectTypeServlet
 */
@WebServlet("/type/*")
public class UpdateObjectTypeServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private static final String OS_KEY_GC = "GestionContenedores";
	private static final String OS_KEY_GF = "GestionFacturas";
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String objectType = request.getPathInfo().substring(1);
		
		// Get the request json
		InputStream requestInputStream = request.getInputStream();
		JSONObject jsonRequest = JSONObject.parse(requestInputStream);
		String requestMode = jsonRequest.get("requestMode").toString();
		JSONArray requestProperties = (JSONArray)jsonRequest.get("properties");
		JSONArray responseProperties = new JSONArray();
		JSONArray propertyData = getPropertyData(objectType, request.getLocale());
		
		// First, for initial object calls, fill in overrides of initial values
		if (requestMode.equals("initialNewObject")) {
			for (int i = 0; i < propertyData.size(); i++) {
				JSONObject overrideProperty = (JSONObject)propertyData.get(i);
				String overridePropertyName = overrideProperty.get("symbolicName").toString();
				if (overrideProperty.containsKey("initialValue")) {
					for (int j = 0; j < requestProperties.size(); j++) {
						JSONObject requestProperty = (JSONObject)requestProperties.get(j);
						String requestPropertyName = requestProperty.get("symbolicName").toString();
						if (overridePropertyName.equals(requestPropertyName)) {
							Object initialValue = overrideProperty.get("initialValue");
							requestProperty.put("value", initialValue);
						}
					}
				}
			}
		}
		
		// For both initial and in-progress calls, process the property data to add in choice lists and modified metadata
		for (int i = 0; i < propertyData.size(); i++) {
			JSONObject overrideProperty = (JSONObject)propertyData.get(i);
			String overridePropertyName = overrideProperty.get("symbolicName").toString();
			if (requestMode.equals("initialNewObject") || requestMode.equals("initialExistingObject") || requestMode.equals("inProgressChanges")) { 
				if (overrideProperty.containsKey("dependentOn")) {
					// perform dependent overrides (such as dependent choice lists) for inProgressChanges calls only
					// although they can be processed for initial calls, it will influence searches (narrowing the search choices)
					if (requestMode.equals("inProgressChanges")) {
					String dependentOn = overrideProperty.get("dependentOn").toString();
					String dependentValue = overrideProperty.get("dependentValue").toString();
					for (int j = 0; j < requestProperties.size(); j++) {
						JSONObject requestProperty = (JSONObject)requestProperties.get(j);
						String requestPropertyName = requestProperty.get("symbolicName").toString();
						Object value = requestProperty.get("value");
						if (requestPropertyName.equals(dependentOn) && dependentValue.equals(value)) {
							responseProperties.add(overrideProperty);
						}
					}
					}
				} else {
					// perform non-dependent overrides.  copy over the request property values to maintain
					// the current values
					if (!overrideProperty.containsKey("value")) {
						for (int j = 0; j < requestProperties.size(); j++) {
							JSONObject requestProperty = (JSONObject)requestProperties.get(j);
							String requestPropertyName = requestProperty.get("symbolicName").toString();
							if (requestPropertyName.equals(overridePropertyName)) {
								Object value = requestProperty.get("value");
								overrideProperty.put("value", value);
							}
						}
					}
					responseProperties.add(overrideProperty);
				}
			}
		}

		// Send the response json
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("properties", responseProperties);
		PrintWriter writer = response.getWriter();
		jsonResponse.serialize(writer);
	}
	
	private JSONArray getPropertyData(String objectType, Locale locale) throws IOException {
		
		// First look for a locale specific version of the property data.
		/*
		InputStream propertyDataStream = this.getServletContext().getClassLoader().getResourceAsStream(objectType.replace(' ', '_')+"_PropertyData_"+locale.toString()+".json");
		if (propertyDataStream == null) {
			// Look for a locale independent version of the property data
			propertyDataStream = this.getServletContext().getClassLoader().getResourceAsStream(objectType.replace(' ', '_')+"_PropertyData.json");
		}
		*/

		NavigatorSettings navigatorSettings = ConfigurationSettings.getInstance().getNavigatorSettings();
		
		// Load JSON Property Data
		InputStream propertyDataStream = null;
		JSONArray jsonPropertyData = new JSONArray();
		try {
			propertyDataStream = new FileInputStream(navigatorSettings.getEdsPath() + java.io.File.separator + objectType.replace(' ', '_') + "_" + locale.toString() + ".json");
			jsonPropertyData = JSONArray.parse(propertyDataStream);
		} catch (IOException ioe) {
			try {
				propertyDataStream = new FileInputStream(navigatorSettings.getEdsPath() + java.io.File.separator + objectType.replace(' ', '_') + ".json");
				jsonPropertyData = JSONArray.parse(propertyDataStream);
			} catch (IOException ioe2) {}
		} finally {
			if (propertyDataStream != null)
				propertyDataStream.close();
		}
		
		// Load JSON Object Stores Collection
		InputStream objectStoresStream = null;
		JSONObject jsonObjectStores = new JSONObject();
		try {
			objectStoresStream = new FileInputStream(navigatorSettings.getEdsPath() + java.io.File.separator + "ObjectStores.json");
			jsonObjectStores = JSONObject.parse(objectStoresStream);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			if (objectStoresStream != null)
				objectStoresStream.close();
		}		
		
		// Asigna catalogos externos
		try
		{
			String objType = objectType.replace(' ', '_');
			if (objType.equals("Registrar_Nuevo_Contenedor.Workflow.Registrar_Nuevo_Contenedor") || objType.equals("Actualizar_Datos_de_Contenedor.Workflow.Actualizar_Datos_de_Contenedor")) 
			{
				// Identifica particularidades
				String initialValue = null;
				if (objType.equals("Registrar_Nuevo_Contenedor.Workflow.Registrar_Nuevo_Contenedor"))
					initialValue = "ImportSaad";				
				
				// Lista de Clientes
				JSONArray clientes = ClientesService.getChoices(jsonObjectStores.get(OS_KEY_GC).toString());
				jsonPropertyData.add(ClientesService.getData("Cliente", clientes, initialValue, true));
				// Lista de Navieras y Agencias
				JSONArray proveedores = ProveedoresService.getChoices(jsonObjectStores.get(OS_KEY_GC).toString());				
				jsonPropertyData.add(ProveedoresService.getData("Naviera", proveedores, initialValue, true));
				jsonPropertyData.add(ProveedoresService.getData("AgenciaAduanal", proveedores, initialValue, true));
				// Lista de Importadoras
				JSONArray importadoras = ListasService.getChoices(jsonObjectStores.get(OS_KEY_GC).toString(), ListasService.TIPO_IMPORTADORA);
				jsonPropertyData.add(ListasService.getData("Importadora", "Nombre de la Importadora", importadoras, initialValue, true));
			}
			else if (objType.equals("Asociar_Comprobante_Contenedor.Workflow.Asociar_Comprobante_Contenedor")) 
			{				
				// Lista de Proveedores
				JSONArray proveedores = ProveedoresService.getChoices(jsonObjectStores.get(OS_KEY_GC).toString());	
				jsonPropertyData.add(ProveedoresService.getData("Proveedor", proveedores, null, true));
				// Lista de Conceptos de Comprobante
				JSONArray conceptosComprobante = ListasService.getChoices(jsonObjectStores.get(OS_KEY_GC).toString(), ListasService.TIPO_CONCEPTOSCOMPROBANTE);
				jsonPropertyData.add(ListasService.getData("ConceptoComprobante", "Concepto del Comprobante", conceptosComprobante, null, true));
			}
			else if (objType.equals("Asociar_Documentos_de_Origen.Workflow.Asociar_Documentos_de_Origen")) {
				// Lista de Tipo de Mercancias
				JSONArray tipoMercancia = ListasService.getChoices(jsonObjectStores.get(OS_KEY_GC).toString(), ListasService.TIPO_TIPOMERCANCIA);
				jsonPropertyData.add(ListasService.getData("TipoMercancia", "Tipo de Mercancía", tipoMercancia, null, false));				
			}
			else if (objType.equals("Contenedor") || objType.equals("GCDocumento") || objType.equals("FacturaComercial") || objType.equals("BillOfLoading") || objType.equals("ListaEmpaque") || objType.equals("Comprobante") || objType.equals("CuentaGastos") || objType.equals("PedimentoDesglosado") || objType.equals("AnexoCuentaGastos"))
			{
				// Lista de Clientes
				JSONArray clientes = ClientesService.getChoices(jsonObjectStores.get(OS_KEY_GC).toString());
				jsonPropertyData.add(ClientesService.getData("NombreCliente", clientes, null, false));
				// Lista de Navieras y Agencias
				JSONArray proveedores = ProveedoresService.getChoices(jsonObjectStores.get(OS_KEY_GC).toString());				
				jsonPropertyData.add(ProveedoresService.getData("NombreNaviera", proveedores, null, false));
				jsonPropertyData.add(ProveedoresService.getData("NombreAgenciaAduanal", proveedores, null, false));
				// Lista de Importadoras
				JSONArray importadoras = ListasService.getChoices(jsonObjectStores.get(OS_KEY_GC).toString(), ListasService.TIPO_IMPORTADORA);
				jsonPropertyData.add(ListasService.getData("Importadora", "Nombre de la Importadora", importadoras, null, false));
							
				if (objType.equals("Comprobante")) {
					// Lista de Proveedores	
					jsonPropertyData.add(ProveedoresService.getData("NombreProveedor", proveedores, null, false));
					// Lista de Conceptos de Comprobante
					JSONArray conceptosComprobante = ListasService.getChoices(jsonObjectStores.get(OS_KEY_GC).toString(), ListasService.TIPO_CONCEPTOSCOMPROBANTE);
					jsonPropertyData.add(ListasService.getData("ConceptoComprobante", "Concepto del comprobante", conceptosComprobante, null, false));
					
				} else if (objType.equals("BillOfLoading")) {
					// Lista de Tipos de Mercancia
					JSONArray tipoMercancias = ListasService.getChoices(jsonObjectStores.get(OS_KEY_GC).toString(), ListasService.TIPO_TIPOMERCANCIA);
					jsonPropertyData.add(ListasService.getData("TipoMercancia", "Tipo de Mercancía", tipoMercancias, null, false));
							
				} else if (objType.equals("AnexoCuentaGastos")) {
					// Lista de Tipos de Anexos Cuenta de Gastos
					JSONArray tiposAnexos = ListasService.getChoices(jsonObjectStores.get(OS_KEY_GC).toString(), ListasService.TIPO_TIPOANEXOCTAGASTOS);
					jsonPropertyData.add(ListasService.getData("TipoAnexoCuentaGastos", "Tipo de Anexo Cuenta de Gastos", tiposAnexos, null, false));
					
				}
			} 
			else if (objType.equals("SolDocCase") || objType.equals("SolDocPago") || objType.equals("SolDocDevolucion")) {
				// Lista de Proveedores
				JSONArray proveedores = SolDocProveedores.getChoices(jsonObjectStores.get(OS_KEY_GF).toString());
				jsonPropertyData.add(SolDocProveedores.getData("Proveedor", proveedores, null, false));		
				// Lista de Empresas
				JSONArray empresa = SolDocEmpresas.getChoices(jsonObjectStores.get(OS_KEY_GF).toString());
				jsonPropertyData.add(SolDocEmpresas.getData("Empresa", empresa, null, false));	
			}
		}
		catch (Exception e) 
		{
			throw new IOException("Ocurrió un error al momento de obtener los catálogos externos.", e);
		}
			
		return jsonPropertyData;
	}	

}
