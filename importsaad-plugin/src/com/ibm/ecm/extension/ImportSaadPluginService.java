package com.ibm.ecm.extension;

import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.json.java.JSONObject;

import com.ibm.ecm.extension.service.ImportSaadContenedorService;
import com.ibm.ecm.extension.service.ImportSaadReportesService;
import com.ibm.ecm.extension.service.ImportSaadCatalogosService;
import com.ibm.ecm.extension.service.SolDocService;
import com.ibm.ecm.extension.service.ContentService;

public class ImportSaadPluginService extends PluginService {
	
	@Override	
	public String getId() {
		return "ImportSaadPluginService";
	}	
	
	@Override	
	public void execute(PluginServiceCallbacks callbacks, HttpServletRequest request, HttpServletResponse response) throws Exception {

		String methodName = request.getParameter("method");
		if (methodName == null)
			throw new Exception("No se ha incluido el nombre del método a invocar.");
		
		callbacks.getLogger().logEntry(this, methodName, request);
			
		try {
	
			JSONObject result = new JSONObject();
			if (methodName.equals("getDatosContenedor"))
				result = ImportSaadContenedorService.getDatosContenedor(request, callbacks);
			else if (methodName.equals("getWorkflowVWVersion"))
				result = ImportSaadContenedorService.getVWVersion(request, callbacks);
			else if (methodName.equals("getDocumentosContenedor"))
				result = ImportSaadContenedorService.getDocumentosContenedor(request, callbacks);
			else if (methodName.equals("getDetalleFacturaComercial"))
				result = ImportSaadContenedorService.getDetalleFacturaComercial(request, callbacks);
			else if (methodName.equals("setDetallePedimentoDesglosado"))
				result = ImportSaadContenedorService.setDetallePedimentoDesglosado(request, callbacks);
			else if (methodName.equals("setDatosFacturaComercial"))
				result = ImportSaadContenedorService.setDatosFacturaComercial(request, callbacks);
			else if (methodName.equals("setDatosCuantaGastos"))
				result = ImportSaadContenedorService.setDatosCuantaGastos(request, callbacks);
			else if (methodName.equals("getDetalleCuentaGastos"))
				result = ImportSaadContenedorService.getDetalleCuentaGastos(request, callbacks);
			else if (methodName.equals("getDetallePedimentoDesglosado"))	
				result = ImportSaadContenedorService.getDetallePedimentoDesglosado(request, callbacks);
			else if (methodName.equals("getDetalleComprobantes"))		
				result = ImportSaadContenedorService.getDetalleComprobantes(request, callbacks);
			else if (methodName.equals("getReportesTree"))		
				result = ImportSaadReportesService.getReportesTree(request, callbacks);
			else if (methodName.equals("getReportesDetalle"))		
				result = ImportSaadReportesService.getReportesDetalle(request, callbacks);
			else if (methodName.equals("getClientes"))
				result = ImportSaadCatalogosService.getClientes(request, callbacks);
			else if (methodName.equals("getProveedores"))
				result = ImportSaadCatalogosService.getProveedores(request, callbacks);
			else if (methodName.equals("getLista"))
				result = ImportSaadCatalogosService.getLista(request, callbacks);		
			else if (methodName.equals("getSolDocSettings"))
				result = SolDocService.getSettings(request, callbacks);		
			else if (methodName.equals("updateSolDocSettings"))
				result = SolDocService.updateSettings(request, callbacks);
			else if (methodName.equals("getSolDocProveedores"))
				result = SolDocService.getProveedores(request, callbacks);
			else if (methodName.equals("updateSolDocProveedores"))
				result = SolDocService.updateProveedores(request, callbacks);
			else if (methodName.equals("deleteSolDocProveedores"))
				result = SolDocService.deleteProveedores(request, callbacks);
			else if (methodName.equals("addSolDocProveedores"))
				result = SolDocService.addProveedor(request, callbacks);
			else if (methodName.equals("addSolDocEmpresas"))
				result = SolDocService.addEmpresa(request, callbacks);	
			else if (methodName.equals("updateSolDocEmpresas"))
				result = SolDocService.updateEmpresas(request, callbacks);			
			else if (methodName.equals("deleteSolDocEmpresas"))
				result = SolDocService.deleteEmpresas(request, callbacks);			
			else if (methodName.equals("getSolDocClientes"))
				result = SolDocService.getClientes(request, callbacks);
			else if (methodName.equals("addSolDocClientes"))
				result = SolDocService.addCliente(request, callbacks);	
			else if (methodName.equals("updateSolDocClientes"))
				result = SolDocService.updateCliente(request, callbacks);
			else if (methodName.equals("deleteSolDocClientes"))
				result = SolDocService.deleteClientes(request, callbacks);		
			else if (methodName.equals("addSolDocSolicitudFactura"))
				result = SolDocService.addSolicitudFactura(request, callbacks);
			else if (methodName.equals("sendSolDocSoliciudFactura"))
				result = SolDocService.sendSolicitudFactura(request, callbacks);
			else if (methodName.equals("getSolDocDatosFactura"))
				result = SolDocService.getDatosFactura(request, callbacks);
			else if (methodName.equals("addDocument"))
				result = ContentService.addDocument(request, callbacks);
			else if (methodName.equals("getDocument"))
				result = ContentService.getDocument(request, callbacks);
			else if (methodName.equals("updateDocument"))
				result = ContentService.updateDocument(request, callbacks);			
			else if (methodName.equals("updateSolDocSolicitudFactura"))
				result = SolDocService.updateFactura(request, callbacks);
			else if (methodName.equals("sendSolDocFactura"))
				result = SolDocService.sendFactura(request, callbacks);
			else if (methodName.equals("getSolDocCurrentDocId"))
				result = SolDocService.getCurrentDocumentId(request, callbacks);
			else if (methodName.equals("deleteSolDocFactura"))
				result = SolDocService.deleteFactura(request, callbacks);	
			else if (methodName.equals("getSolDocEmpresas"))
				result = SolDocService.getEmpresas(request, callbacks);
			else if (methodName.equals("depurarSolDocCatalogos"))
				result = SolDocService.depurarCatalogos(request, callbacks);			
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
