define([
    "dojo/_base/declare", 
    "dojo/_base/lang", 	
    "ecm/model/Action"],
function(declare, lang, Action) {

	return declare("importSaadPluginDojo.SolDocVersionarModel", [ Action ], {
	
		isEnabled: function(repository, listType, items, teamspace, resultSet) {
			
			if (items.length > 1)
				return false;
			
			for (i = 0; i < items.length; i++) { 
				if (items[i].template != "SolDocPago")
					return false;									
			}			
										
			return true;	
		},
	
		isVisible: function(repository, listType) {
			return this.inherited(arguments);			
		}
	});
	
});
