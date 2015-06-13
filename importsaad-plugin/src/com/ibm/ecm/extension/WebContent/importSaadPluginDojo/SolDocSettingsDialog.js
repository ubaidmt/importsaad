define([
		"dojo/_base/declare",
		"dojo/_base/lang",	
		"dojo/_base/connect",
		"dijit/registry",
		"dojo/_base/array",	
		"dojo/json",
		"dijit/_TemplatedMixin",
		"dijit/_WidgetsInTemplateMixin",
    	"ecm/model/Request",	   	
		"dijit/Dialog",		
		"dojo/text!./templates/SolDocSettingsDialog.html"
	],
	function(declare, lang, connect, registry, array, json, _TemplatedMixin, _WidgetsInTemplateMixin, Request, Dialog, template) {

	/**
	 * @name importSaadPluginDojo.SolDocSettingsDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments dijit.Dialog
	 */
	return declare("importSaadPluginDojo.SolDocSettingsDialog", [ Dialog, _TemplatedMixin, _WidgetsInTemplateMixin], {
	/** @lends importSaadPluginDojo.SolDocSettingsDialog.prototype */	

		templateString: template,
		widgetsInTemplate: true,		
		repositoryId: null,
		
		showDialog: function() {
			this.onLoad();
			this.inherited("show", []);		
		},
		
		onClose: function() {
			this.inherited("hide", []);		
		},
		
		setRepositoryId: function(repositoryId) {
			this.repositoryId = repositoryId;
		},
		
		onLoad: function() {
			
			this.message.innerHTML = "Cargando...";
			this.progressBar.style.display = "block";				
			
			var params = {};
			params.method = "getSolDocSettings";
			params.repositoryid = this.repositoryId;
			
			Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";													
								return;
							} else if (response.settings == null) {		
								this.clearMessage();						
								return;								
							}
							
							this.emailFrom.setValue(response.settings.emailFrom);
							this.emailAlias.setValue(response.settings.emailAlias);
							this.emailBcc.setValue(response.settings.emailBcc);
							this.emailPassword.setValue(response.settings.emailPassword);
							this.starttls.setValue(response.settings.starttls);
							this.emailHost.setValue(response.settings.emailHost);
							this.emailPort.setValue(response.settings.emailPort);
							this.maxRecords.setValue(response.settings.maxRecords);
							this.timeLimit.setValue(response.settings.timeLimit);
							this.calculoIVA.setValue(response.settings.calculoIVA);		
							this.folioConsecutivo.setValue(response.settings.folioConsecutivo);		
							
							this.clearMessage();
							
						})
					}); 		
					
		},	
		
		onSave: function() {			
			
			if (!this.emailFrom.isValid() || !this.emailPassword.isValid() || !this.emailHost.isValid() || !this.emailPort.isValid() || !this.folioConsecutivo.isValid()) {			
				this.message.innerHTML = "Datos inválidos.";
				return;	
			}
			
			this.message.innerHTML = "Actualizando...";
			this.progressBar.style.display = "block";	
			
			var jsonSettings = 	{
					"emailFrom": this.emailFrom.getValue(),
					"emailAlias": this.emailAlias.getValue(),
					"emailBcc": this.emailBcc.getValue(),
					"emailPassword": this.emailPassword.getValue(),
					"starttls": (this.starttls.getValue() === "true"),
					"emailHost": this.emailHost.getValue(),
					"emailPort": parseInt(this.emailPort.getValue()),
					"maxRecords": parseInt(this.maxRecords.getValue()),
					"timeLimit": parseInt(this.timeLimit.getValue()),				
					"calculoIVA": parseFloat(this.calculoIVA.getValue()),
					"folioConsecutivo": parseInt(this.folioConsecutivo.getValue())
					};
			
			var params = {};
			params.method = "updateSolDocSettings";
			params.repositoryid = this.repositoryId;
			params.settings = json.stringify(jsonSettings);
			
			Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";						
								return;
							}	
								
							this.clearMessage();
							
						})
					}); 							
		},
		
		clearMessage: function() {		
			this.message.innerHTML = "";
			this.progressBar.style.display = "none";	
		}   
			
	});
});
