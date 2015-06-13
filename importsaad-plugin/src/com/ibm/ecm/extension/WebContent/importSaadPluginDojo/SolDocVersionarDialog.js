define([
	"dojo/_base/declare",
	"dojo/_base/lang",	
	"ecm/model/Request",
	"dijit/Dialog",
	"dojo/json",
	"dojo/dom",
	"dojo/dom-style",
	"dijit/_TemplatedMixin",
	"dijit/_WidgetsInTemplateMixin",	
	"ecm/model/Desktop",
	"dojo/text!./templates/SolDocVersionarDialog.html"
	],
	function (declare, lang, Request, Dialog, json, dom, domStyle, _TemplatedMixin, _WidgetsInTemplateMixin, Desktop, template) {
	
	/**
	 * @name importSaadPluginDojo.SolDocVersionarDialog
	 * @class
	 * @augments dijit.Dialog
	 */
	return declare("importSaadPluginDojo.SolDocVersionarDialog", [Dialog, _TemplatedMixin, _WidgetsInTemplateMixin], {
	/** @lends importSaadPluginDojo.SolDocVersionarDialog.prototype */

		//needed to load from template
		templateString: template,
		widgetsInTemplate: true,
		items: null,
		context: null,		
		
		showDialog: function() {
			this.saveButton.set("disabled",true);
			this.onLoad();		
			this.inherited("show", []);		
		},
		
		setItems: function(items) {
			this.items = items;
		},
		
		setContext: function(context) {
			this.context = context;
		},			
		
		onCancel: function() {							
			this.inherited("hide", []);		
		},	

		isValid: function() {
			var valid = this._fileInput;
			// This test works for both HTML5 and non-HTML5 browsers. 
			valid = valid && (this._fileInput.value) && (this._fileInput.value.length > 0);
			return valid;
		},
		
		onFileInputChange: function() {
			this.saveButton.set("disabled",(this.isValid() ? false : true));
		},			
		
		onLoad: function() {	
												
		},	
		
		onSave: function() {		

			domStyle.set(this.message, "color", "#000253");
			this.message.innerHTML = "Actualizando documento...";
			this.progressBar.style.display = "block";
						
			var callback = lang.hitch(this, this._onFileAddCompleted);
			
			var params = {};
			params.method = "addDocument";
			params.repositoryid = this.items[0].repository.id;
			params.context = json.stringify(this.context);
			params.parentFolder = "/Temporal/Documentos";	
						
			// HTML5 browser
			if (this._fileInput.files) {
				var file = this._fileInput.files[0];
				params.mimetype = file.type;
				params.parm_part_filename = (file.fileName ? file.fileName : file.name)
				//params.max_file_size = this._maxFileSize.toString();

				var form = new FormData();
				form.append("file", file);
				
				Request.postFormToPluginService("ImportSaadPlugin", "ImportSaadPluginService", form, {
					requestParams: params,
					requestCompleteCallback: callback
				});	
				
			} else { // Non-HTML5 browser
				var fileName = this._fileInput.value;
				if (fileName && fileName.length > 0) {
					var i = fileName.lastIndexOf("\\");
					if (i != -1) {
						fileName = fileName.substr(i + 1);
					}
				}
				params.parm_part_filename = fileName;
				//params.max_file_size = this._maxFileSize.toString();
				
				// MIME type is not available, must be determined at the server.
				params.plugin = "ImportSaadPlugin";
				params.action = "ImportSaadPluginService";
				
				Request.ieFileUploadServiceAPI("plugin", "", {requestParams: params, 
					requestCompleteCallback: callback
				}, this._fileInputForm);
			}
		},
		
		_onFileAddCompleted: function(response) {
			
			this._fileInput.value = "";
			
			if (response.error != null) {
				this.message.innerHTML = response.error;
				this.progressBar.style.display = "none";													
				return;				
			}

			var params = {};
			params.method = "updateDocument";
			params.repositoryid = this.items[0].repository.id;
			params.context = json.stringify(this.context);
			params.itemid = this.items[0].id.split(",")[2];
			params.documentid = response.document.id;

			var propsToSync = [
				{"value": "Proveedor"},
				{"value": "Empresa"}
			];			
			params.propsToSync = json.stringify(propsToSync);
			
			Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";													
								return;
							}
																												
							this.onCancel();						
									
						})
					}); 			
		
		}
		
	});
});