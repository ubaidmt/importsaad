require(["dojo/_base/declare",
         "dojo/_base/lang", 
         "dojo/_base/array",	
     	 "ecm/model/Request",
     	 "ecm/model/Desktop",
     	 "importSaadPluginDojo/MessagesDialog",
     	 "importSaadPluginDojo/ImportSaadDecorator",
     	 "importSaadPluginDojo/SolDocSettingsDialog",
     	 "importSaadPluginDojo/SolDocProveedoresDialog",
     	 "importSaadPluginDojo/SolDocEmpresasDialog",
     	 "importSaadPluginDojo/SolDocClientesDialog",
     	 "importSaadPluginDojo/SolDocSolicitaFacturaDialog",
     	 "importSaadPluginDojo/SolDocEditarFacturaDialog",
     	 "importSaadPluginDojo/SolDocPagosClientesDialog",
     	 "importSaadPluginDojo/SolDocDevolucionesProvDialog",
     	 "importSaadPluginDojo/SolDocDevolucionesCliDialog",
     	 "importSaadPluginDojo/SolDocVersionarDialog"], 
function(declare, lang, array, Request, Desktop, MssagesDialog, ImportSaadDecorator, SolDocSettingsDialog, SolDocProveedoresDialog, SolDocEmpresasDialog, SolDocClientesDialog, SolDocSolicitaFacturaDialog, SolDocEditarFacturaDialog, SolDocPagosClientesDialog, SolDocDevolucionesProvDialog, SolDocDevolucionesCliDialog, SolDocVersionarDialog) {
	
	lang.setObject("importSaadRegistraContenedorAction", function (repository, items) {
		
		if (ecm.model.desktop.defaultRepositoryId == null) {
			var messagesDialog = new MessagesDialog();
			messagesDialog.showMessage("Registro de Nuevo Contenedor", "El id del repositorio o el escritorio por defecto no fue identificado, por lo tanto, no es posible registrar un nuevo contenedor.", "<img src='/importsaad-cdn/images/common/actions/warning.png' width='30px' height='30px'>", 500);
			return;
		}
		
		var params = {};
		params.method = "getWorkflowVWVersion";
		params.repositoryid = ecm.model.desktop.defaultRepositoryId;
		params.nombreworkflow = "Registrar Nuevo Contenedor";
		Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService", 
			{
				requestParams: params,
				requestCompleteCallback: function(response) {	// success
								
					if (response.vwversion == null) {
						var messagesDialog = new MessagesDialog();
						messagesDialog.showMessage("Registro de Nuevo Contenedor", "El proceso para registrar un nuevo contenedor no fué identificado correctamente.<br>Posiblemente no tenga los permisos necesarios para realizar esta acción.<br><br>", "<img src='/importsaad-cdn/images/common/actions/warning.png' width='30px' height='30px'>", 500);
						return;		
					}
					
					var url = "gc_registracontenedorprocessor.jsp?workflowVersion="+encodeURIComponent(response.vwversion)+"&subject=&attachmentId=&propertyMap=&isoRegion=undefined&repositoryId="+ecm.model.desktop.defaultRepositoryId+"&desktopId="+ecm.model.desktop.id;
					var w = 1000;
					var h = 700;
					var left = (screen.width - w) / 2;
					var top = (screen.height - h) / 2;		
					window.open(url, '_blank', 'width='+w+',height='+h+',top='+top+',left='+left);					
					
		    	}
			});  
		
	});
	
	lang.setObject("solDocSettingsAction", function (repository, items) {
		var solDocSettingsDialog = new SolDocSettingsDialog();
		solDocSettingsDialog.setRepositoryId(ecm.model.desktop.defaultRepositoryId);		
		solDocSettingsDialog.showDialog();							
	});	
	
	lang.setObject("solDocProveedoresAction", function (repository, items) {					
		var solDocProveedoresDialog = new SolDocProveedoresDialog();
		solDocProveedoresDialog.setContext(getContext());
		solDocProveedoresDialog.setRepositoryId(ecm.model.desktop.defaultRepositoryId);		
		solDocProveedoresDialog.showDialog();							
	});		

	lang.setObject("solDocEmpresasAction", function (repository, items) {	
		var solDocEmpresasDialog = new SolDocEmpresasDialog();
		solDocEmpresasDialog.setRepositoryId(ecm.model.desktop.defaultRepositoryId);
		solDocEmpresasDialog.setContext(getContext());
		solDocEmpresasDialog.showDialog();
	});		
	
	lang.setObject("solDocClientesAction", function (repository, items) {					
		var solDocClientesDialog = new SolDocClientesDialog();
		solDocClientesDialog.setRepositoryId(ecm.model.desktop.defaultRepositoryId);
		solDocClientesDialog.setContext(getContext());
		solDocClientesDialog.showDialog();							
	});		
	
	lang.setObject("solDocSolicitaFacturaAction", function (repository, items) {
		var solDocSolicitaFacturaDialog = new SolDocSolicitaFacturaDialog();
		solDocSolicitaFacturaDialog.setRepositoryId(ecm.model.desktop.defaultRepositoryId);
		solDocSolicitaFacturaDialog.setContext(getContext());
		solDocSolicitaFacturaDialog.setCalculoIVA(getCalculoIVA());
		solDocSolicitaFacturaDialog.showDialog();													
	});
	
	lang.setObject("solDocCopiarSolicitudAction", function (repository, items) {
		var solDocSolicitaFacturaDialog = new SolDocSolicitaFacturaDialog();
		solDocSolicitaFacturaDialog.setRepositoryId(ecm.model.desktop.defaultRepositoryId);
		solDocSolicitaFacturaDialog.setContext(getContext());
		solDocSolicitaFacturaDialog.setCalculoIVA(getCalculoIVA());
		solDocSolicitaFacturaDialog.setItems(items);
		solDocSolicitaFacturaDialog.showDialog();													
	});		
	
	lang.setObject("solDocEditarFacturaAcion", function (repository, items) {
		var solDocEditarFacturaDialog = new SolDocEditarFacturaDialog();
		solDocEditarFacturaDialog.setItems(items);
		solDocEditarFacturaDialog.setContext(getContext());
		solDocEditarFacturaDialog.setCalculoIVA(getCalculoIVA());
		solDocEditarFacturaDialog.showDialog();													
	});	
	
	lang.setObject("solDocPagosClientesAction", function (repository, items) {
		var solDocPagosClientesDialog = new SolDocPagosClientesDialog();
		solDocPagosClientesDialog.setRepositoryId(ecm.model.desktop.defaultRepositoryId);
		solDocPagosClientesDialog.setContext(getContext());
		solDocPagosClientesDialog.showDialog();							
	});	
	
	lang.setObject("solDocDevolucionesProvAction", function (repository, items) {
		var solDocDevolucionesProvDialog = new SolDocDevolucionesProvDialog();
		solDocDevolucionesProvDialog.setRepositoryId(ecm.model.desktop.defaultRepositoryId);
		solDocDevolucionesProvDialog.setContext(getContext());
		solDocDevolucionesProvDialog.showDialog();						
	});		
	
	lang.setObject("solDocDevolucionesCliAction", function (repository, items) {
		var solDocDevolucionesCliDialog = new SolDocDevolucionesCliDialog();
		solDocDevolucionesCliDialog.setRepositoryId(ecm.model.desktop.defaultRepositoryId);
		solDocDevolucionesCliDialog.setContext(getContext());
		solDocDevolucionesCliDialog.showDialog();							
	});	
	
	lang.setObject("solDocVersionarAction", function (repository, items) {
		var solDocVersionarDialog = new SolDocVersionarDialog();
		solDocVersionarDialog.setItems(items);
		solDocVersionarDialog.setContext(getContext());
		solDocVersionarDialog.showDialog();								
	});

	function getContext() {	
		var params = {};
		var response = Request.invokeSynchronousPluginService("ImportSaadPlugin", "configService", params);

		var context = {};		
		context.objectStoreName = ecm.model.desktop.getRepository(ecm.model.desktop.defaultRepositoryId).objectStoreName;
		context.serverName = ecm.model.desktop.getRepository(ecm.model.desktop.defaultRepositoryId).serverName;
					
		if (response.configuration != null) {
			var configitems = response.configuration;
			array.forEach(configitems, lang.hitch(this, function(item) {							
				if (item.name == "usuario") {
					context.usuario = item.value;
				} else if (item.name == "contrasena") {
					context.contrasena = item.value;
				}
			}));
		}

		return context;
	}

	function getCalculoIVA() {
		var params = {};
		params.method = "getSolDocSettings";
		params.repositoryid = ecm.model.desktop.defaultRepositoryId;	
		var response = Request.invokeSynchronousPluginService("ImportSaadPlugin", "ImportSaadPluginService", params);
		return parseFloat(response.settings.calculoIVA);
	}
	
});

