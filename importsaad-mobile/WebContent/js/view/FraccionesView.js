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
		reloadFraccionesButton: null,
		fraccionesList: null,
		fraccionesHeading: null,
		cotizadorView: null,
		isLoaded: false,
		// Create a template string for a fraccion ListItem
		fraccionViewItemTemplateString:	'',
		// init variables and handlers
		startup: function() {
			this.inherited(arguments);
			// retain widgets references
			this.reloadFraccionesButton = registry.byId("reloadFraccionesButton");
			this.fraccionesHeading = registry.byId("fraccionesHeading");			
			this.fraccionesList = registry.byId("fraccionesList");
			this.progressIndicator = ProgressIndicator.getInstance();
			this.cotizadorView = registry.byId("cotizadorView");
			this.reloadFraccionesButton.on("click", lang.hitch(this, this.loadFracciones));
			// handler to get notified before a transition to the current view starts
			this.on("beforeTransitionIn", lang.hitch(this, function () {
			}));		
		},
		load: function() {
			if (!this.isLoaded)
				this.loadFracciones();
			this.isLoaded = true;
		},
		loadFracciones: function() {
			registry.byId("fraccionesFilterBox").reset(); // reset filter box
			// remove all list items
			this.fraccionesList.destroyDescendants();
			// reset scroll to make sure progress indicator is visible
			this.scrollTo({x:0,y:0});
			// add progress indicator
			this.fraccionesHeading.set('label',"cargando...");
			this.fraccionesList.domNode.appendChild(this.progressIndicator.domNode);
			this.progressIndicator.start();
			// search fracciones
			var requestURL = "Dispatcher?method=searchFracciones&os=" + settings.osname;
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
				this.onResponse(response.fracciones);
			})), lang.hitch(this, function(err) {	
				this.onError(err);				
			});					
		},
		//  response handler
		onResponse: function(fracciones) {
			// remove progress indicator
			this.progressIndicator.stop();
			this.fraccionesList.destroyDescendants();
			// restore the title
			this.fraccionesHeading.set('label','Fracciones');
			// populate data
			var items = [];
			array.forEach(fracciones, lang.hitch(this, function (fraccion) {
				items.push({label: fraccion.name + " (" + this.formatUnidad(fraccion.unidad) + ")", unidad: fraccion.unidad, precio: fraccion.precio, moveTo: "cotizadorView"});
			}));
			var data = {items: items};
	        // store for the dojox/mobile/EdgeToEdgeStoreList
	        var store = new Memory({idProperty:"label", data: data});
	        ready(function() {
                var listWidget = new declare([EdgeToEdgeStoreList, FilteredListMixin]) (
                		{filterBoxRef: 'fraccionesFilterBox', placeHolder: 'Buscar', store: store}
                	);
                listWidget.placeAt(registry.byId("fraccionesList").containerNode);
                listWidget.startup();     
                // handle click event
                listWidget.on("click", lang.hitch(this, function (evt) {
                	var result = store.query({label: evt.target.innerHTML});
					// update cotizacion view before transitioning to it
                	registry.byId("cotizadorView").setFraccion(result[0]);
                	registry.byId("cotizadorView").updateCotizacion();
    			}));
	        });
		},
		// error handler
		onError: function(error) {
			// remove progress indicator
			this.progressIndicator.stop();
			this.fraccionesList.destroyDescendants();
			// display error message
			this.fraccionesHeading.set('label',error);
		},
		formatUnidad: function(num) {
			if (num == null)
				return "";
			switch(num) {
				case 0:
					return "Kg";
					break;
				case 1:
					return "M";
					break;
				case 2:
					return "M2";
					break;					
				case 3:
					return "Pza";
					break;			
			}			
		},		
		// Pushes data into a template - primitive
		substitute: function(template,obj) {
			return template.replace(/\$\{([^\s\:\}]+)(?:\:([^\s\:\}]+))?\}/g, function(match,key){
				return obj[key];
			});
		}		
	});
});