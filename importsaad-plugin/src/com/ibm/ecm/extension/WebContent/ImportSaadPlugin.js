require(["dojo/_base/declare",
         "dojo/_base/lang", 
     	 "ecm/model/Desktop",
     	 "importSaadPluginDojo/util/Contexto",
     	 "importSaadPluginDojo/action/SolDocSettingsDialog",
     	 "importSaadPluginDojo/action/SolDocProveedoresDialog",
     	 "importSaadPluginDojo/action/SolDocEmpresasDialog",
     	 "importSaadPluginDojo/action/SolDocClientesDialog",
     	 "importSaadPluginDojo/action/SolDocSolicitaFacturaDialog",
     	 "importSaadPluginDojo/action/SolDocEditarFacturaDialog",
     	 "importSaadPluginDojo/action/SolDocPagosClientesDialog",
     	 "importSaadPluginDojo/action/SolDocDevolucionesProvDialog",
     	 "importSaadPluginDojo/action/SolDocDevolucionesCliDialog",
     	 "importSaadPluginDojo/action/SolDocVersionarDialog",
     	 "importSaadPluginDojo/action/CntSettingsDialog",
     	 "importSaadPluginDojo/action/CntFraccionesDialog",
     	 "importSaadPluginDojo/action/CntClientesDialog",
     	 "importSaadPluginDojo/action/CntNavierasDialog",
     	 "importSaadPluginDojo/action/CntForwardersDialog",
     	 "importSaadPluginDojo/action/CntProveedoresDialog",
     	 "importSaadPluginDojo/action/CntImportadorasDialog",
     	 "importSaadPluginDojo/action/CntPuertosDialog"], 
function(declare, 
		lang, 
		Desktop, 
		Contexto,
		SolDocSettingsDialog, 
		SolDocProveedoresDialog, 
		SolDocEmpresasDialog, 
		SolDocClientesDialog, 
		SolDocSolicitaFacturaDialog, 
		SolDocEditarFacturaDialog, 
		SolDocPagosClientesDialog, 
		SolDocDevolucionesProvDialog, 
		SolDocDevolucionesCliDialog, 
		SolDocVersionarDialog,
		CntSettingsDialog,
		CntFraccionesDialog,
		CntClientesDialog,
		CntNavierasDialog,
		CntForwardersDialog,
		CntProveedoresDialog,
		CntImportadorasDialog,
		CntPuertosDialog) {
	
	lang.setObject("solDocSettingsAction", function (repository, items) {
		var solDocSettingsDialog = new SolDocSettingsDialog();
		solDocSettingsDialog.setConfig(Contexto.getConfig());
		solDocSettingsDialog.show();							
	});	
	
	lang.setObject("solDocProveedoresAction", function (repository, items) {					
		var solDocProveedoresDialog = new SolDocProveedoresDialog();
		solDocProveedoresDialog.setConfig(Contexto.getConfig());
		solDocProveedoresDialog.showDialog();							
	});		

	lang.setObject("solDocEmpresasAction", function (repository, items) {	
		var solDocEmpresasDialog = new SolDocEmpresasDialog();
		solDocEmpresasDialog.setConfig(Contexto.getConfig());
		solDocEmpresasDialog.showDialog();
	});		
	
	lang.setObject("solDocClientesAction", function (repository, items) {					
		var solDocClientesDialog = new SolDocClientesDialog();
		solDocClientesDialog.setConfig(Contexto.getConfig());
		solDocClientesDialog.showDialog();							
	});		
	
	lang.setObject("solDocSolicitaFacturaAction", function (repository, items) {
		var solDocSolicitaFacturaDialog = new SolDocSolicitaFacturaDialog();
		solDocSolicitaFacturaDialog.setConfig(Contexto.getConfig());
		solDocSolicitaFacturaDialog.showDialog();													
	});
	
	lang.setObject("solDocCopiarSolicitudAction", function (repository, items) {
		var solDocSolicitaFacturaDialog = new SolDocSolicitaFacturaDialog();
		solDocSolicitaFacturaDialog.setConfig(Contexto.getConfig());
		solDocSolicitaFacturaDialog.setItems(items);
		solDocSolicitaFacturaDialog.showDialog();													
	});		
	
	lang.setObject("solDocEditarFacturaAcion", function (repository, items) {
		var solDocEditarFacturaDialog = new SolDocEditarFacturaDialog();
		solDocEditarFacturaDialog.setItems(items);
		solDocEditarFacturaDialog.setConfig(Contexto.getConfig());
		solDocEditarFacturaDialog.showDialog();													
	});	
	
	lang.setObject("solDocPagosClientesAction", function (repository, items) {
		var solDocPagosClientesDialog = new SolDocPagosClientesDialog();
		solDocPagosClientesDialog.setConfig(Contexto.getConfig());
		solDocPagosClientesDialog.showDialog();							
	});	
	
	lang.setObject("solDocDevolucionesProvAction", function (repository, items) {
		var solDocDevolucionesProvDialog = new SolDocDevolucionesProvDialog();
		solDocDevolucionesProvDialog.setConfig(Contexto.getConfig());
		solDocDevolucionesProvDialog.showDialog();						
	});		
	
	lang.setObject("solDocDevolucionesCliAction", function (repository, items) {
		var solDocDevolucionesCliDialog = new SolDocDevolucionesCliDialog();
		solDocDevolucionesCliDialog.setConfig(Contexto.getConfig());
		solDocDevolucionesCliDialog.showDialog();							
	});	
	
	lang.setObject("solDocVersionarAction", function (repository, items) {
		var solDocVersionarDialog = new SolDocVersionarDialog();
		solDocVersionarDialog.setItems(items);
		solDocVersionarDialog.setConfig(Contexto.getConfig());
		solDocVersionarDialog.showDialog();								
	});
	
	lang.setObject("cntSettingsAction", function (repository, items) {
		var cntSettingsDialog = new CntSettingsDialog();
		cntSettingsDialog.setConfig(Contexto.getConfig());
		cntSettingsDialog.showDialog();							
	});		
	
	lang.setObject("cntFraccionesAction", function (repository, items) {
		var cntFraccionesDialog = new CntFraccionesDialog();
		cntFraccionesDialog.setConfig(Contexto.getConfig());
		cntFraccionesDialog.show();								
	});	
	
	lang.setObject("cntClientesAction", function (repository, items) {
		var cntClientesDialog = new CntClientesDialog();
		cntClientesDialog.setConfig(Contexto.getConfig());
		cntClientesDialog.show();								
	});	
	
	lang.setObject("cntNavierasAction", function (repository, items) {
		var cntNavierasDialog = new CntNavierasDialog();
		cntNavierasDialog.setConfig(Contexto.getConfig());
		cntNavierasDialog.show();								
	});
	
	lang.setObject("cntForwardersAction", function (repository, items) {
		var cntForwardersDialog = new CntForwardersDialog();
		cntForwardersDialog.setConfig(Contexto.getConfig());
		cntForwardersDialog.show();								
	});
	
	lang.setObject("cntProveedoresAction", function (repository, items) {
		var cntProveedoresDialog = new CntProveedoresDialog();
		cntProveedoresDialog.setConfig(Contexto.getConfig());
		cntProveedoresDialog.show();								
	});	
	
	lang.setObject("cntImportadorasAction", function (repository, items) {
		var cntImportadorasDialog = new CntImportadorasDialog();
		cntImportadorasDialog.setConfig(Contexto.getConfig());
		cntImportadorasDialog.show();								
	});	
	
	lang.setObject("cntPuertosAction", function (repository, items) {
		var cntPuertosDialog = new CntPuertosDialog();
		cntPuertosDialog.setConfig(Contexto.getConfig());
		cntPuertosDialog.show();								
	});		
	
});