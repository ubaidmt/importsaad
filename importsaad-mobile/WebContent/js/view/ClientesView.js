define([
	"dojo/_base/declare",
    "dojo/ready",
    "dijit/registry",
	"dojo/store/Memory",
    "dojox/mobile/EdgeToEdgeStoreList",
    "dojox/mobile/FilteredListMixin",
    "dojox/mobile/SearchBox",
    "dojox/mobile/ScrollableView",
	"dojo/_base/lang",
	"dojo/_base/array",
	"dojo/dom-class",
	"dojo/json",
	"dojo/request/xhr",
	"dojox/mobile/ProgressIndicator"
], function(declare, ready, registry, Memory, EdgeToEdgeStoreList, FilteredListMixin, SearchBox, ScrollableView, lang, array, domClass, json, xhr, ProgressIndicator){
	return declare([ScrollableView], {
		reloadClientesButton: null,
		clientesList: null,
		clientesHeading: null,
		contenedoresSettingsView: null,
		isLoaded: false,
		// Create a template string for a cliente ListItem
		clienteViewItemTemplateString:	'',
		// init variables and handlers
		startup: function() {
			this.inherited(arguments);
			// retain widgets references
			this.reloadClientesButton = registry.byId("reloadClientesButton");
			this.clientesHeading = registry.byId("clientesHeading");			
			this.clientesList = registry.byId("clientesList");
			this.progressIndicator = ProgressIndicator.getInstance();
			this.contenedoresSettingsView = registry.byId("contenedoresSettingsView");
			this.reloadClientesButton.on("click", lang.hitch(this, this.loadClientes));
			// handler to get notified before a transition to the current view starts
			this.on("beforeTransitionIn", lang.hitch(this, function () {
			}));		
		},
		load: function() {
			if (!this.isLoaded)
				this.loadClientes();
			this.isLoaded = true;
		},
		loadClientes: function() {
			registry.byId("clientesFilterBox").reset(); // reset filter box
			// remove all list items
			this.clientesList.destroyDescendants();
			// reset scroll to make sure progress indicator is visible
			this.scrollTo({x:0,y:0});
			// add progress indicator
			this.clientesHeading.set('label',"cargando...");
			this.clientesList.domNode.appendChild(this.progressIndicator.domNode);
			this.progressIndicator.start();
			// search clientes
			var criterio = {
				"activo": true,
			};
			var requestURL = "Dispatcher?method=searchClientes&criterio=" + json.stringify(criterio) + "&maxResults=0&os=" + settings.osname;
			xhr(requestURL, {
				method: "GET",
				handleAs: "json",
				preventCache: true,
				sync : false, 
				timeout: 10000, // 10 seconds
				headers: { "Content-Type": "application/json"}
			}).then(lang.hitch(this, function(response) {	
				if (response.error != null) {
					this.onError(response.error);
					return;
				}
				this.onResponse(response.clientes);
			})), lang.hitch(this, function(err) {	
				this.onError(err);				
			});					
		},
		//  response handler
		onResponse: function(clientes) {
			// remove progress indicator
			this.progressIndicator.stop();
			this.clientesList.destroyDescendants();
			// restore the title
			this.clientesHeading.set('label','Clientes');
			// populate data
			var items = [];
			array.forEach(clientes, lang.hitch(this, function (cliente) {
				items.push({label: cliente.name, id: cliente.id, moveTo: "contenedoresSettingsView"});
			}));
			var data = {items: items};
	        // store for the dojox/mobile/EdgeToEdgeStoreList
	        var store = new Memory({idProperty:"label", data: data});
	        ready(function() {
                var listWidget = new declare([EdgeToEdgeStoreList, FilteredListMixin]) (
                		{filterBoxRef: 'clientesFilterBox', placeHolder: 'Buscar', store: store}
                	);
                listWidget.placeAt(registry.byId("clientesList").containerNode);
                listWidget.startup();     
                // handle click event
                listWidget.on("click", lang.hitch(this, function (evt) {
                	var result = store.query({label: evt.target.innerHTML});
					// set cliente before transitioning to it
                	registry.byId("contenedoresSettingsView").setCliente(result[0]);
    			}));
	        });
		},
		// error handler
		onError: function(error) {
			// remove progress indicator
			this.progressIndicator.stop();
			this.clientesList.destroyDescendants();
			// display error message
			this.clientesHeading.set('label',error);
		}		
	});
});