define([
		"dojo/_base/declare",
		"dojo/_base/lang",	
		"dojo/json",
		"dojo/html",
		"dojo/dom-style",
    	"ecm/model/Request",	   	
    	"ecm/widget/dialog/BaseDialog",	
		"dojo/text!./templates/SolDocSettingsDialog.html"
	],
	function(declare, 
			lang, 
			json, 
			html, 
			domStyle, 
			Request, 
			BaseDialog, 
			template) {

	/**
	 * @name importSaadPluginDojo.action.SolDocSettingsDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments dijit.Dialog
	 */
	return declare("importSaadPluginDojo.action.SolDocSettingsDialog", [ BaseDialog ], {
	/** @lends importSaadPluginDojo.action.SolDocSettingsDialog.prototype */	

		contentString: template,
		widgetsInTemplate: true,
		title: "Configuración Gestión de Facturas",
		
		postCreate: function() {
			this.inherited(arguments);
			this.setResizable(false);
			this.setMaximized(false);
			this.setSize(800, 530);
			this.refreshButton = this.addButton("Refrescar", "onLoad", false, false);
			this.saveButton = this.addButton("Guardar", "onSave", false, false);
			this.cancelButton.set("label", "Cerrar");							
		},			
		
		show: function() {
			this.inherited(arguments);
			this.onLoad();
		},
		
		destroy: function() {
			this.inherited(arguments);
		},				
		
		hide: function() {
			this.inherited(arguments);
		},			
		
		setConfig: function(config) {
			this.config = config;
		},		
		
		onLoad: function() {
			
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Cargando...");
			domStyle.set(this.progressBar, "display", "block");		
			
			var params = {};
			params.method = "getSolDocSettings";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			
			Request.invokePluginService("ImportSaadPlugin", "SettingsService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								html.set(this.message, response.error);
								domStyle.set(this.progressBar, "display", "none");																					
								return;
							}
							
							this.emailFrom.setValue(response.settings.emailFrom);
							this.emailAlias.setValue(response.settings.emailAlias);
							this.emailBcc.setValue(response.settings.emailBcc);
							this.emailUser.setValue("emailUser" in response.settings ? response.settings.emailUser : "");
							this.emailPassword.setValue(response.settings.emailPassword);
							this.starttls.setValue(response.settings.starttls);
							this.emailHost.setValue(response.settings.emailHost);
							this.emailPort.setValue(response.settings.emailPort);
							this.maxRecords.setValue(response.settings.maxRecords);
							this.timeLimit.setValue(response.settings.timeLimit);
							this.folioConsecutivo.setValue(response.settings.folioConsecutivo);		
							
							html.set(this.message, "");
							domStyle.set(this.progressBar, "display", "none");
							
						})
					}); 				
		},	
		
		onSave: function() {			
			
			if (!this.emailFrom.isValid() || !this.emailUser.isValid() || !this.emailPassword.isValid() || !this.emailHost.isValid() || !this.emailPort.isValid() || !this.folioConsecutivo.isValid()) {			
				this.message.innerHTML = "Datos inválidos.";
				return;	
			}
			
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Actualizando...");
			domStyle.set(this.progressBar, "display", "block");	
			
			var jsonSettings = {};
			jsonSettings.emailFrom = this.emailFrom.getValue();
			jsonSettings.emailAlias = this.emailAlias.getValue();
			jsonSettings.emailBcc = this.emailBcc.getValue();
			jsonSettings.emailUser = this.emailUser.getValue();
			jsonSettings.emailPassword = this.emailPassword.getValue();
			jsonSettings.starttls = this.starttls.getValue() === "true";
			jsonSettings.emailHost = this.emailHost.getValue();
			jsonSettings.emailPort = parseInt(this.emailPort.getValue());
			jsonSettings.maxRecords = parseInt(this.maxRecords.getValue());
			jsonSettings.timeLimit = parseInt(this.timeLimit.getValue());			
			jsonSettings.folioConsecutivo = parseInt(this.folioConsecutivo.getValue());
			
			var params = {};
			params.method = "updateSolDocSettings";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.settings = json.stringify(jsonSettings);
			
			Request.invokePluginService("ImportSaadPlugin", "SettingsService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								html.set(this.message, response.error);
								domStyle.set(this.progressBar, "display", "none");																					
								return;
							}
								
							html.set(this.message, "");
							domStyle.set(this.progressBar, "display", "none");
							
						})
					}); 							
		}
		
	});
});
