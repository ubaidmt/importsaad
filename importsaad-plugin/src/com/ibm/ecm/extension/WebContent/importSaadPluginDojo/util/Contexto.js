define([
    "dojo/_base/declare",
	"dojo/_base/lang",
	"dojo/_base/array",
	"dojo/json",
	"ecm/model/Request"
],
function(declare, 
		lang,
		array,
		json,
		Request) {
	
	var Contexto = declare("importSaadPluginDojo.util.Contexto", null, {	
	
    	config: {},
    	
        constructor: function(args) {
        	declare.safeMixin(this, args);
        	this.loadConfig();
        },
        
		loadConfig: function() {
			var params = {};
			var response = Request.invokeSynchronousPluginService("ImportSaadPlugin", "configService", params);
			
			// context (integrated logon)
			var context = {};
			context.objectStoreName = ecm.model.desktop.getRepository(ecm.model.desktop.defaultRepositoryId).objectStoreName;
			context.serverName = ecm.model.desktop.getRepository(ecm.model.desktop.defaultRepositoryId).serverName;			
	
			if (response.configuration != null) {
				var configitems = response.configuration;
				array.forEach(configitems, lang.hitch(this, function(item) {
					// parametros de aplicacion
					if (item.name == "pteiva")
						this.config.pteiva = item.value;						
					// permisos
					else if (item.name == "administradoresGF")
						this.config.administradoresGF = item.value;	
				}));
			}
			// set context json
			this.config.context = context;
		}        
        
    });	

	Contexto.getConfig = function() {
		var contexto = new Contexto();
		return contexto.config;
	};	
	
	return Contexto;
});