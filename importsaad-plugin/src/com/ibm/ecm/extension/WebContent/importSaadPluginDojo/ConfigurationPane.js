define([
		"dojo/_base/declare",
		"dojo/_base/lang",
		"dojo/_base/array",	
		"dojo/json",
		"dijit/_TemplatedMixin",
		"dijit/_WidgetsInTemplateMixin",
		"dojo/data/ItemFileWriteStore",		
		"ecm/widget/admin/PluginConfigurationPane",
		"dojo/text!./templates/ConfigurationPane.html"
	],
	function(declare, lang, array, json, _TemplatedMixin, _WidgetsInTemplateMixin, ItemFileWriteStore, PluginConfigurationPane, template) {

		/**
		 * @name importSaadPluginDojo.ConfigurationPane
		 * @augments ecm.widget.admin.PluginConfigurationPane
		 */
		return declare("importSaadPluginDojo.ConfigurationPane", [ PluginConfigurationPane, _TemplatedMixin, _WidgetsInTemplateMixin], {
		/** @lends importSaadPluginDojo.ConfigurationPane.prototype */

		templateString: template,
		widgetsInTemplate: true,
	
		load: function(callback) {		
			
			if (this.configurationString) 
			{		
				var jsonObj = json.parse(this.configurationString);
				if (jsonObj == null || jsonObj == '')
					return;	
				
				var items = jsonObj.configuration;
				array.forEach(items, lang.hitch(this, function(item) {		  				
					 if (item.name == "usuario")
						this.usuario.setValue(item.value);	
					else if (item.name == "contrasena")
						this.contrasena.setValue(item.value);	
					else if (item.name == "administradoresGF") {
						var value = "";
						array.forEach(item.value, lang.hitch(this, function(user) {
							value += user.username + ",";
						}));
						if (value.length > 0)
							value = value.substring(0, value.length - 1);
						this.administradoresGF.setValue(value);
					}
				}));						
			}		
		},
			
		onSave: function() {
			
			// convertir a json array los administradores gestion de facturas
			var jsonAdminsGF = new Array();		
			var users = this.administradoresGF.getValue().split(",");
			array.forEach(users, lang.hitch(this, function(user) {				
				jsonAdminsGF.push({"username": user});				
			}));	
			
			var configData = [
			    {"name": "usuario", "value": this.usuario.getValue()},
			    {"name": "contrasena", "value": this.contrasena.getValue()},
			    {"name": "administradoresGF", "value": jsonAdminsGF}
			];
							
			this.configurationString = json.stringify({"configuration": configData});	
			this.onSaveNeeded(true);
		},
		
		validate: function() {
			return true;
		}
		
	});
});
