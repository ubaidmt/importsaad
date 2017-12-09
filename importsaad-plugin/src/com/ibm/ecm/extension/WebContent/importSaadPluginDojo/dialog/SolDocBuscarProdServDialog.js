define([
	"dojo/_base/declare",
	"dojo/_base/lang",	
	"dojo/on",
	"dojo/aspect",
	"dojo/keys",			
	"dojo/html",
	"dojo/_base/array",	
	"dojo/json",
	"dojo/dom-style",
	"dojo/dom-geometry",
    	"ecm/model/Request",
    	"dojox/grid/EnhancedGrid",
    	"dojox/grid/enhanced/plugins/Pagination",
    	"dojo/data/ItemFileWriteStore",
	"ecm/widget/dialog/BaseDialog",	
	"dojo/text!./templates/SolDocBuscarProdServDialog.html"
],
	
function(declare, 
		lang,  
		on, 
		aspect,
		keys,
		html,
		array, 
		json, 
		domStyle, 
		domGeom, 
		Request, 
		EnhancedGrid, 
		Pagination, 
		ItemFileWriteStore,  
		BaseDialog, 
		template) {

	/**
	 * @name importSaadPluginDojo.dialog.SolDocBuscarProdServDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments ecm.widget.dialog.BaseDialog
	 */
	return declare("importSaadPluginDojo.dialog.SolDocBuscarProdServDialog", [ BaseDialog ], {
	/** @lends importSaadPluginDojo.dialog.SolDocBuscarProdServDialog.prototype */	

		contentString: template,
		widgetsInTemplate: true,		

		postCreate: function() {
			this.inherited(arguments);
			this.setResizable(false);
			this.setMaximized(false);
			this.setSize(600, 800); // in case setMaximized is set to false		
			this.setTitle("Búsqueda de Productos y Servicios");
			this.cleanButton = this.addButton("Limpiar", "onClean", false, false);
			this.confirmButton = this.addButton("Confirmar", "onBeforeConfirm", true, false);
			this.searchButton = this.addButton("Consultar", "onSearch", false, false);
			this.cancelButton.set("label", "Cerrar");
			this.initGrid();
			this.initEvents();
		},				
		
		show: function() {
			this.inherited(arguments);
			setTimeout(lang.hitch(this, function() {
				this.clave.focus();
			}), 500);						
		},
		
		hide: function() {
			this.inherited(arguments);
		},
		
		setConfig: function(config) {
			this.config = config;
		},

		resize: function() {
			this.inherited(arguments);
		},
		
		initEvents: function() {
			on(this.maxResults, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
		     	case keys.ENTER:
		     		this.onSearch();
		     		break;
			    }				
			}));							
			on(this.clave, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
		     	case keys.ENTER:
		     		this.onSearch();
		     		break;
			    }
			}));					
			on(this.name, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
		     	case keys.ENTER:
		     		this.onSearch();
		     		break;
			    }
			}));			
			aspect.after(this.gridArea, "resize", lang.hitch(this, function() {
	            this.adjustGridSize();
			}));
		},		
		
		adjustGridSize: function() {
			if (this.grid != null) {
				var size = domGeom.getContentBox(this.gridArea.domNode);
				this.grid.resize({ w: size.w, h: size.h });
			}
		},
		
		initGrid: function() {
			if (this.grid != null)
				return;
			
			var store = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});				
		
			var layout = [
				{name: 'Clave', field: 'clave', width: '15%', editable: false, noresize: true},
				{name: 'Descripcion', field: 'name', width: '85%', editable: false, noresize: true}
			];
			
  			this.grid = new EnhancedGrid({
  				store: store,
  				structure: layout,
  				selectionMode: 'single',
  				sortInfo: 1, // asc by name
				autoHeight: false,
				formatterScope: this,
				queryOptions: {ignoreCase: true},				
				singleClickEdit: false,
		        plugins: {
		            pagination: {
		            		pageSizes: ["50","250","500"],
		                defaultPageSize: 50,
		                description: true,
		                sizeSwitch: true,
		                pageStepper: true,
		                gotoButton: true,
		                maxPageStep: 4,
		                position: "bottom"
		            }		            
		        }				
  			});	

			this.grid.placeAt(this.gridDiv);			
			this.grid.startup();
			
			on(this.grid, "click", lang.hitch(this, function() {
				this.onGridItemSelect();
			}));		
			
			on(this.grid, "dblclick", lang.hitch(this, function() {
				this.onBeforeConfirm();
			}));							
			
			on(this.grid, "stylerow", lang.hitch(this, function(row) {
				var item = this.grid.getItem( row.index );
				if ( item == null ) return;
				if (row.odd) row.customStyles += 'background-color:#F7F7F7;';
			}));	
			
			on(this.grid, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
			     	case keys.BACKSPACE:
			     		evt.preventDefault();
			     		break;
			    }
			}));				 		
		},
		
		onBeforeConfirm: function() {			
			this.onConfirm();
		},
		
		onConfirm: function(callback) {			
			if (lang.isFunction(callback))
				callback();
		},
		
		onSearch: function() {
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");
			
			if (!this.maxResults.isValid())
				this.maxResults.reset();	
			
			var criterio = {};

			if (this.clave.getValue() != "" && this.clave.isValid()) 
				criterio.claveLike = this.clave.getValue();
			if (this.name.getValue() != "" && this.name.isValid()) 
				criterio.nameLike = this.name.getValue();				
			
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Consultando productos y servicios ...");
			domStyle.set(this.progressBar, "display", "block");
			
			// Clear grid selection
			this.clearGridSelection();				
			
			var params = {};
			params.method = "searchSolDocProdServ";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.criterio = json.stringify(criterio);
			params.maxResults = this.maxResults.getValue();
			
			Request.invokePluginService("ImportSaadPlugin", "SolDocService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								html.set(this.message, response.error);
								domStyle.set(this.progressBar, "display", "none");								
								return;
							}
									
							var results = response.results;				
							var store = new ItemFileWriteStore({
								data: {
									identifier: "id",
									items: results
								}
							});	
							
							this.grid.setStore(store);
							this.grid.firstPage();		
							
							if (results.length == 0) {
								domStyle.set(this.message, "color", "#000253");
								html.set(this.message, "No existen productos y servicios con el criterio de consulta indicado");
								domStyle.set(this.progressBar, "display", "none");	
								return;
							}				
							
							html.set(this.message, "");
							domStyle.set(this.progressBar, "display", "none");							
							
						})
					}); 							
		},
		
		onClean: function() {	
			this.clearGridSelection();
			this.clearGridData(this.grid);
			this.name.reset();
			this.clave.reset();
			this.clave.focus();
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");	
		},
		
		onGridItemSelect: function() {
			var items = this.grid.selection.getSelected();
			if (items.length > 0) {
				this.confirmButton.set("disabled", false);
			}		
		},	
		
		clearGridSelection: function() {
			if (this.grid != null) this.grid.selection.clear();
			this.confirmButton.set("disabled", true);
		},
		
		clearGridData: function(grid) {			
			var store = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});					
			if (grid != null)
				grid.setStore(store);			
		}		
	});
});
