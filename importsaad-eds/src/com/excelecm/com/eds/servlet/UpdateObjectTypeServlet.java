package com.excelecm.com.eds.servlet;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.excelecm.com.eds.services.SolDocProveedores;
import com.excelecm.com.eds.services.SolDocEmpresas;
import com.excelecm.com.eds.services.SolDocClientes;
import com.excelecm.com.eds.services.CntNavieras;
import com.excelecm.com.eds.services.CntForwarders;
import com.excelecm.common.settings.ConfigurationSettings;
import com.excelecm.common.settings.NavigatorSettings;

/**
 * Servlet implementation class UpdateObjectTypeServlet
 */
@WebServlet("/type/*")
public class UpdateObjectTypeServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private static final String OS_KEY_GF = "GestionFacturas";
	private static final String OS_KEY_GC = "GestionContenedores";
	
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
		JSONArray propertyData = getPropertyData(objectType, requestProperties);
		
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
	
	private JSONArray getPropertyData(String objectType, JSONArray requestProperties) throws IOException {

		NavigatorSettings navigatorSettings = ConfigurationSettings.getInstance().getNavigatorSettings();
		
		// Load JSON Object Stores Collection
		InputStream objectStoresStream = null;
		JSONObject jsonObjectStores = new JSONObject();
		try {
			objectStoresStream = new FileInputStream(navigatorSettings.getEdsPath() + java.io.File.separator + "ObjectStores.json");
			jsonObjectStores = JSONObject.parse(objectStoresStream);
		} catch (Exception e) {
			throw new IOException("Ocurrió un error al momento de intentar leer el archivo ObjectStores.json.", e);
		} finally {
			if (objectStoresStream != null) objectStoresStream.close();
		}			

		// Load JSON Property Data
		InputStream propertyDataStream = null;
		JSONArray jsonPropertyData = new JSONArray();
		try {
			propertyDataStream = new FileInputStream(navigatorSettings.getEdsPath() + java.io.File.separator + objectType.replace(' ', '_') + ".json");
			jsonPropertyData = JSONArray.parse(propertyDataStream);
		} catch (Exception e) {
			// no se localizo el archivo .json predefinido, continua con la carga de catalogos...
		} finally {
			if (propertyDataStream != null) propertyDataStream.close();
		}	
		
		// Asigna catalogos externos
		try
		{
			String objType = objectType.replace(' ', '_');
			Object getPropertyValue = null;
			if (objType.equals("SolDocCase") || objType.equals("SolDocPago") || objType.equals("SolDocDevolucion")) 
			{
				// Lista de Proveedores
				getPropertyValue = getPropertyValue("Proveedor", true, requestProperties);
				JSONArray proveedores = SolDocProveedores.getChoices(jsonObjectStores.get(OS_KEY_GF).toString());
				jsonPropertyData.add(SolDocProveedores.getData("Proveedor", proveedores, getPropertyValue, false));		
				// Lista de Empresas
				getPropertyValue = getPropertyValue("Empresa", true, requestProperties);
				JSONArray empresa = SolDocEmpresas.getChoices(jsonObjectStores.get(OS_KEY_GF).toString());
				jsonPropertyData.add(SolDocEmpresas.getData("Empresa", empresa, getPropertyValue, false));
				// Lista de Clientes
				getPropertyValue = getPropertyValue("Cliente", true, requestProperties);
				JSONArray cliente = SolDocEmpresas.getChoices(jsonObjectStores.get(OS_KEY_GF).toString());
				jsonPropertyData.add(SolDocClientes.getData("Cliente", cliente, getPropertyValue, false));				
			}
			else if (objType.equals("CntContenedor"))
			{
				// Lista de Navieras
				getPropertyValue = getPropertyValue("Naviera", true, requestProperties);
				JSONArray navieras = CntNavieras.getChoices(jsonObjectStores.get(OS_KEY_GC).toString());
				jsonPropertyData.add(CntNavieras.getData("Naviera", navieras, getPropertyValue, false));
				// Lista de Forwarders
				getPropertyValue = getPropertyValue("Forwarder", true, requestProperties);
				JSONArray forwarders = CntForwarders.getChoices(jsonObjectStores.get(OS_KEY_GC).toString());
				jsonPropertyData.add(CntForwarders.getData("Forwarder", forwarders, getPropertyValue, false));				
			}
		}
		catch (Exception e) 
		{
			throw new IOException("Ocurrió un error al momento de obtener los catálogos externos.", e);
		}
			
		return jsonPropertyData;
	}	
	
	private static Object getPropertyValue(String symbolicName, boolean isObjectType, JSONArray requestProperties) {
		Object propertyValue = null;
		
		try
		{
			for (int j = 0; j < requestProperties.size(); j++) {
				JSONObject requestProperty = (JSONObject)requestProperties.get(j);
				String requestPropertySymbolicName = requestProperty.get("symbolicName").toString();
				if (requestPropertySymbolicName.contains("[")) { // child component index.. ignore
					requestPropertySymbolicName = requestPropertySymbolicName.substring(0,requestPropertySymbolicName.indexOf("["));
				}
				if (symbolicName.equals(requestPropertySymbolicName)) {
					propertyValue = requestProperty.get("value");
					if (propertyValue != null && isObjectType)
						propertyValue = propertyValue.toString().split(",")[2]; // object-type value
					break;
				}
			}
		}
		catch (Exception e)
		{
			// error al obtener property value, salir...
		}
		
		return propertyValue;
	}	

}
