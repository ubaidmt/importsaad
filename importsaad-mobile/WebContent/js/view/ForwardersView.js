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
		reloadForwardersButton: null,
		forwardersList: null,
		forwardersHeading: null,
		contenedoresSettingsView: null,
		isLoaded: false,
		// Create a template string for a cliente ListItem
		clienteViewItemTemplateString:	'',
		// init variables and handlers
		startup: function() {
			this.inherited(arguments);
			// retain widgets references
			this.reloadForwardersButton = registry.byId("reloadForwardersButton");
			this.forwardersHeading = registry.byId("forwardersHeading");			
			this.forwardersList = registry.byId("forwardersList");
			this.progressIndicator = ProgressIndicator.getInstance();
			this.contenedoresSettingsView = registry.byId("contenedoresSettingsView");
			this.reloadForwardersButton.on("click", lang.hitch(this, this.loadForwarders));
			// handler to get notified before a transition to the current view starts
			this.on("beforeTransitionIn", lang.hitch(this, function () {
			}));		
		},
		load: function() {
			if (!this.isLoaded)
				this.loadForwarders();
			this.isLoaded = true;
		},
		loadForwarders: function() {
			registry.byId("forwardersFilterBox").reset(); // reset filter box
			// remove all list items
			this.forwardersList.destroyDescendants();
			// reset scroll to make sure progress indicator is visible
			this.scrollTo({x:0,y:0});
			// add progress indicator
			this.forwardersHeading.set('label',"cargando...");
			this.forwardersList.domNode.appendChild(this.progressIndicator.domNode);
			this.progressIndicator.start();
			// search forwarders
			var criterio = {
				"activo": true,
			};
			var requestURL = "Dispatcher?method=searchForwarders&criterio=" + json.stringify(criterio) + "&maxResults=0&os=" + settings.osname;
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
				this.onResponse(response.forwarders);
			})), lang.hitch(this, function(err) {	
				this.onError(err);				
			});					
		},
		//  response handler
		onResponse: function(forwarders) {
			// remove progress indicator
			this.progressIndicator.stop();
			this.forwardersList.destroyDescendants();
			// restore the title
			this.forwardersHeading.set('label','Forwarders');
			// populate data
			var items = [];
			array.forEach(forwarders, lang.hitch(this, function (cliente) {
				items.push({label: cliente.name, id: cliente.id, moveTo: "contenedoresSettingsView"});
			}));
			var data = {items: items};
	        // store for the dojox/mobile/EdgeToEdgeStoreList
	        var store = new Memory({idProperty:"label", data: data});
	        ready(function() {
                var listWidget = new declare([EdgeToEdgeStoreList, FilteredListMixin]) (
                		{filterBoxRef: 'forwardersFilterBox', placeHolder: 'Buscar', store: store}
                	);
                listWidget.placeAt(registry.byId("forwardersList").containerNode);
                listWidget.startup();     
                // handle click event
                listWidget.on("click", lang.hitch(this, function (evt) {
                	var result = store.query({label: evt.target.innerHTML});
					// set cliente before transitioning to it
                	registry.byId("contenedoresSettingsView").setForwarder(result[0]);
    			}));
	        });
		},
		// error handler
		onError: function(error) {
			// remove progress indicator
			this.progressIndicator.stop();
			this.forwardersList.destroyDescendants();
			// display error message
			this.forwardersHeading.set('label',error);
		}		
	});
});