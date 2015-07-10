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
		reloadNavierasButton: null,
		navierasList: null,
		navierasHeading: null,
		contenedoresSettingsView: null,
		isLoaded: false,
		// Create a template string for a cliente ListItem
		clienteViewItemTemplateString:	'',
		// init variables and handlers
		startup: function() {
			this.inherited(arguments);
			// retain widgets references
			this.reloadNavierasButton = registry.byId("reloadNavierasButton");
			this.navierasHeading = registry.byId("navierasHeading");			
			this.navierasList = registry.byId("navierasList");
			this.progressIndicator = ProgressIndicator.getInstance();
			this.contenedoresSettingsView = registry.byId("contenedoresSettingsView");
			this.reloadNavierasButton.on("click", lang.hitch(this, this.loadNavieras));
			// handler to get notified before a transition to the current view starts
			this.on("beforeTransitionIn", lang.hitch(this, function () {
			}));		
		},
		load: function() {
			if (!this.isLoaded)
				this.loadNavieras();
			this.isLoaded = true;
		},
		loadNavieras: function() {
			registry.byId("navierasFilterBox").reset(); // reset filter box
			// remove all list items
			this.navierasList.destroyDescendants();
			// reset scroll to make sure progress indicator is visible
			this.scrollTo({x:0,y:0});
			// add progress indicator
			this.navierasHeading.set('label',"cargando...");
			this.navierasList.domNode.appendChild(this.progressIndicator.domNode);
			this.progressIndicator.start();
			// search navieras
			var criterio = {
				"activo": true,
			};
			var requestURL = "Dispatcher?method=searchNavieras&criterio=" + json.stringify(criterio) + "&maxResults=0&os=" + settings.osname;
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
				this.onResponse(response.navieras);
			})), lang.hitch(this, function(err) {	
				this.onError(err);				
			});					
		},
		//  response handler
		onResponse: function(navieras) {
			// remove progress indicator
			this.progressIndicator.stop();
			this.navierasList.destroyDescendants();
			// restore the title
			this.navierasHeading.set('label','Navieras');
			// populate data
			var items = [];
			array.forEach(navieras, lang.hitch(this, function (cliente) {
				items.push({label: cliente.name, id: cliente.id, moveTo: "contenedoresSettingsView"});
			}));
			var data = {items: items};
	        // store for the dojox/mobile/EdgeToEdgeStoreList
	        var store = new Memory({idProperty:"label", data: data});
	        ready(function() {
                var listWidget = new declare([EdgeToEdgeStoreList, FilteredListMixin]) (
                		{filterBoxRef: 'navierasFilterBox', placeHolder: 'Buscar', store: store}
                	);
                listWidget.placeAt(registry.byId("navierasList").containerNode);
                listWidget.startup();     
                // handle click event
                listWidget.on("click", lang.hitch(this, function (evt) {
                	var result = store.query({label: evt.target.innerHTML});
					// set cliente before transitioning to it
                	registry.byId("contenedoresSettingsView").setNaviera(result[0]);
    			}));
	        });
		},
		// error handler
		onError: function(error) {
			// remove progress indicator
			this.progressIndicator.stop();
			this.navierasList.destroyDescendants();
			// display error message
			this.navierasHeading.set('label',error);
		}		
	});
});