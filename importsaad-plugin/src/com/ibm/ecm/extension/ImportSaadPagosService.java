package com.ibm.ecm.extension;

import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.json.java.JSONObject;

import com.ibm.ecm.extension.service.SolDocPagosService;

public class ImportSaadPagosService extends PluginService {
	
	@Override	
	public String getId() {
		return "ImportSaadPagosService";
	}	
	
	@Override	
	public void execute(PluginServiceCallbacks callbacks, HttpServletRequest request, HttpServletResponse response) throws Exception {

		String methodName = request.getParameter("method");
		if (methodName == null)
			throw new Exception("No se ha incluido el nombre del método a invocar.");
		
		callbacks.getLogger().logEntry(this, methodName, request);
			
		try {
	
			JSONObject result = new JSONObject();
			if (methodName.equals("getEmpresas"))
				result = SolDocPagosService.getEmpresas(request, callbacks);
			else if (methodName.equals("searchFacturas"))
				result = SolDocPagosService.searchFacturas(request, callbacks);	
			else if (methodName.equals("searchDevoluciones"))
				result = SolDocPagosService.searchDevoluciones(request, callbacks);			
			else if (methodName.equals("savePagosDeCliente"))
				result = SolDocPagosService.savePagosDeCliente(request, callbacks);
			else if (methodName.equals("savePagosACliente"))
				result = SolDocPagosService.savePagosACliente(request, callbacks);			
			else if (methodName.equals("savePagosDeProveedor"))
				result = SolDocPagosService.savePagosDeProveedor(request, callbacks);			
			else if (methodName.equals("getSaldoFacturas"))
				result = SolDocPagosService.getSaldoFacturas(request, callbacks);
			else if (methodName.equals("getSaldoProveedor"))
				result = SolDocPagosService.getSaldoProveedor(request, callbacks);
			else if (methodName.equals("getSaldoCliente"))
				result = SolDocPagosService.getSaldoCliente(request, callbacks);				
			else if (methodName.equals("getClientesDevolucion"))
				result = SolDocPagosService.getClientesDevolucion(request, callbacks);
			else if (methodName.equals("addPago"))
				result = SolDocPagosService.addPago(request, callbacks);
			else if (methodName.equals("deletePago"))
				result = SolDocPagosService.deletePago(request, callbacks);		
			else if (methodName.equals("notificaDevolucionProveedor"))
				result = SolDocPagosService.notificaDevolucionProveedor(request, callbacks);
			else if (methodName.equals("notificaDevolucionCliente"))
				result = SolDocPagosService.notificaDevolucionCliente(request, callbacks);			
			else if (methodName.equals("deleteDevolucion"))
				result = SolDocPagosService.deleteDevolucion(request, callbacks);	
			else if (methodName.equals("searchDevolucionesSaldoPendiente"))
				result = SolDocPagosService.searchDevolucionesSaldoPendiente(request, callbacks);
			else
				throw new Exception("No se identificó el método incluido en el servicio.");

			// Send the response json
			PrintWriter writer = response.getWriter();
			result.serialize(writer);		
			
		} catch (Exception e) {
			callbacks.getLogger().logError(this, methodName, request, e);
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("error", "Ocurrió un error al momento de invocar un servicio. " + e.getMessage());
			PrintWriter writer = response.getWriter();
			jsonResponse.serialize(writer);			
			
		} finally {
			callbacks.getLogger().logExit(this, methodName, request);
		}		
	}

}
