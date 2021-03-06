define([
		"dojo/_base/declare",
		"dojo/_base/lang",	
		"dojo/on",
		"dojo/keys",
		"dojo/_base/array",	
		"dojo/html",
		"dojo/json",	
		"dojo/request/xhr",		
		"dojo/dom-style",
		"dojo/dom-geometry",
    	"ecm/model/Request",	
    	"dojox/grid/EnhancedGrid",
    	"dojox/grid/enhanced/plugins/Pagination",
    	"dojo/data/ItemFileWriteStore", 
    	"ecm/widget/dialog/YesNoCancelDialog",
    	"ecm/model/Desktop",
		"ecm/widget/dialog/BaseDialog",	
		"dojo/text!./templates/CntClientesDialog.html"
	],
	function(declare, 
			lang, 
			on, 
			keys, 
			array, 
			html, 
			json,
			xhr,
			domStyle, 
			domGeom, 
			Request, 
			EnhancedGrid, 
			Pagination, 
			ItemFileWriteStore, 
			YesNoCancelDialog,
			Desktop,
			BaseDialog, 
			template) {
	/**
	 * @name importSaadPluginDojo.action.CntClientesDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments ecm.widget.dialog.BaseDialog
	 */
	return declare("importSaadPluginDojo.action.CntClientesDialog", [ BaseDialog ], {
	/** @lends importSaadPluginDojo.action.CntClientesDialog.prototype */	

		contentString: template,
		widgetsInTemplate: true,	

		postCreate: function() {
			this.inherited(arguments);
			this.setResizable(false);
			this.setMaximized(false);
			this.setSize(700, 800);
			this.setTitle("Administración de Clientes");
			this.deleteButton = this.addButton("Eliminar", "onDelete", false, false);
			this.updateButton = this.addButton("Actualizar", "onUpdate", false, false);
			this.refreshButton = this.addButton("Refrescar", "onRefresh", false, false);
			this.addButton = this.addButton("Agregar", "onAdd", false, true);
			this.cancelButton.set("label", "Cerrar");
		},				
		
		show: function() {		
			this.inherited(arguments);
			this.initGrid();
			this.loadClientes();
			
			on(this.filtro, "keyup", lang.hitch(this, function(evt) {
		  		if (this.grid == null)
					return;
		  		if (this.filtro.getValue() != "")
		  			this.grid.filter({"name": "*" + this.filtro.getValue() + "*"});
		  		else
		  			this.grid.filter(null);	  							
			}));			
		
		},
		
		hide: function() {
			if (this.grid != null)
				this.grid.destroy();
			this.inherited(arguments);	
		},

		resize: function() {
			this.inherited(arguments)
			var size = domGeom.getContentBox(this.gridArea.domNode);
			if (this.grid != null)
				this.grid.resize({ w: size.w, h: size.h });
		},			

		setConfig: function(config) {
			this.config = config;
		},	
		
		initGrid: function() {
			
			var gridStore = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});				
			
			var gridLayout = [
				{name: 'Cliente', field: 'name', width: '80%', editable: false, noresize: true},
				{name: 'Activo', field: 'activo', width: '20%', editable: false, formatter: this.formatBoolean, noresize: true}
			];							
			
			this.grid = new EnhancedGrid({
				store: this.gridStore,
				structure: gridLayout,
				selectionMode: 'multi',
				sortInfo: 1, // asc by cliente
				autoHeight: false,
				formatterScope: this,
				queryOptions: {ignoreCase: true},
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
	
			// ajustar la altura del grid a la altura del contentpane
			var size = domGeom.getContentBox(this.gridArea.domNode);
	  		var gridHeight = size.h;
			domStyle.set(this.gridDiv, "height", gridHeight + "px");			
	
			this.grid.placeAt(this.gridDiv);			
			this.grid.startup();
			
			on(this.grid, "click", lang.hitch(this, function() {
				this.onGridItemSelect();
			}));			
			
			on(this.grid, "stylerow", lang.hitch(this, function(row) {
				var item = this.grid.getItem( row.index );
				if ( item == null ) return;
				if (row.odd) row.customStyles += 'background-color:#F7F6F6;';
			}));		
			
			on(this.grid, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
			     	case keys.BACKSPACE:
			     		evt.preventDefault();
			     		break;
			    }
			}));	
			
			on(window, "resize", lang.hitch(this, function() {
				var size = domGeom.getContentBox(this.gridArea.domNode);
				this.grid.resize({ w: size.w, h: size.h });
			})); 
		},
		
		searchClientes: function(criterio, maxResults, callback) {			
																
			var params = {};
			params.method = "searchClientes";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.criterio = json.stringify(criterio);
			params.maxResults = maxResults;
			
			Request.invokePluginService("ImportSaadPlugin", "CntCatalogosService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								html.set(this.message, response.error);
								domStyle.set(this.progressBar, "display", "none");
								return;
							}							
							
							if (lang.isFunction(callback))
								callback(response.clientes);	
														
						})
					}); 		
		},		
		
		loadClientes: function() {
			
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Consultando clientes...");
			domStyle.set(this.progressBar, "display", "block");
			
			var criterio = {};
			this.searchClientes(criterio, 0, lang.hitch(this, function(clientes) {  			
			
				var store = new ItemFileWriteStore({
					data: {
						identifier: "id",
						items: clientes
					}
				});										
				
				this.grid.setStore(store);
				this.grid.firstPage();

				if (clientes.length == 1) { // select row if single result
					this.grid.selection.setSelected(0, true);
					this.onGridItemSelect();
				}	
				
				html.set(this.message, "");
				domStyle.set(this.progressBar, "display", "none");					
				
			}))						
		},			
		
		onGridItemSelect: function() {
			var items = this.grid.selection.getSelected();
			if (items.length > 0) {
				var item = items[items.length-1]; // select last
				this.name.setValue(item.name.toString());
				this.activo.setValue(item.activo.toString() === "true");
			}		
		},
		
		onRefresh: function() {
			this.resetValues();
			this.loadClientes();
		},
		
		onUpdate: function() {	
			
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar un cliente.");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}
			var item = items[items.length-1]; // select 
			
			if (!this.name.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Favor de ingresar datos válidos.");
				domStyle.set(this.progressBar, "display", "none");				
				return;	
			}
			
			// valida que el cliente a actualizar no exista previamente creado
			var criterio = {};
			criterio.name = this.name.getValue();
			this.searchClientes(criterio, 1, lang.hitch(this, function(clientes) {  				
				
				if (clientes.length > 0 && clientes[0].id.toString() != item.id.toString()) {
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "El cliente " + this.name.getValue() + " ya existe.");
					domStyle.set(this.progressBar, "display", "none");				
					return;	
				}
				
				domStyle.set(this.message, "color", "#000253");
				html.set(this.message, "Actualizando cliente seleccionado...");
				domStyle.set(this.progressBar, "display", "block");
				
				var params = {};
				params.method = "updateCliente";
				params.repositoryid = ecm.model.desktop.defaultRepositoryId;
				params.context = json.stringify(this.config.context);
				var cliente = {"id": item.id.toString(), "name": this.name.getValue(), "activo": this.activo.getValue().toString() === "true"};
				params.cliente = json.stringify(cliente);
				
				Request.invokePluginService("ImportSaadPlugin", "CntCatalogosService",
						{	
							requestParams: params,
							requestCompleteCallback: lang.hitch(this, function(response) {
								
								if (response.error != null) {
									domStyle.set(this.message, "color", "#6B0A0A");
									html.set(this.message, response.error);
									domStyle.set(this.progressBar, "display", "none");							
									return;
								}
								
								// Update grid store						
								this.grid.store.setValue(item, 'name', response.cliente.name);
								this.grid.store.setValue(item, 'activo', response.cliente.activo);												
								
								domStyle.set(this.message, "color", "#000253");
								html.set(this.message, "El cliente " + response.cliente.name + " ha sido actualizado.");
								domStyle.set(this.progressBar, "display", "none");
								
							})
						}); 				
			}))					
		},
		
		onDelete: function() {
			
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar por lo menos un cliente.");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}
						
			var confirmDialog = new YesNoCancelDialog({
				title: "Eliminar Clientes",
				text: "¿Está seguro de eliminar los <b>" + items.length + "</b> clientes seleccionados?<br><br>Serán eliminados únicamente los clientes que no tengan cotizaciones ni contenedores asociados.",
				onYes: lang.hitch(this, function() {
					confirmDialog.hide();
					
					domStyle.set(this.message, "color", "#000253");
					html.set(this.message, "Eliminando los clientes seleccionados...");
					domStyle.set(this.progressBar, "display", "block");
					
					var clientes = [];
					array.forEach(items, lang.hitch(this, function(item) {							
						var cliente = {"id": item.id.toString()};
						clientes.push(cliente);
					}));					

					var params = {};
					params.method = "deleteCliente";
					params.repositoryid = ecm.model.desktop.defaultRepositoryId;
					params.context = json.stringify(this.config.context);
					params.clientes = json.stringify(clientes);
					
					Request.invokePluginService("ImportSaadPlugin", "CntCatalogosService",
							{	
								requestParams: params,
								requestCompleteCallback: lang.hitch(this, function(response) {
									
									if (response.error != null) {
										domStyle.set(this.message, "color", "#6B0A0A");
										html.set(this.message, response.error);
										domStyle.set(this.progressBar, "display", "none");
										return;
									}
									
									// Clear grid selection
									this.clearGridSelection();									
									
									// Update grid store
									this.removeGridItemsFromStore(items);
									
									domStyle.set(this.message, "color", "#000253");
									html.set(this.message, "Se eliminaron " + response.count + " de los " + clientes.length + " clientes seleccionados.");
									domStyle.set(this.progressBar, "display", "none");									
									
								})
							}); 					
				}),
				onNo: lang.hitch(this, function() {
					confirmDialog.hide();
				}),
				onShow: lang.hitch(this, function() {
				}),
				onHide: lang.hitch(this, function() {
				})
			});
			
			confirmDialog.show();					
		},
		
		onAdd: function() {	
			
			if (!this.name.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Favor de ingresar datos válidos.");
				domStyle.set(this.progressBar, "display", "none");				
				return;	
			}
			
			// valida que el ecliente no exista previamente creado	
			var criterio = {};
			criterio.name = this.name.getValue();
			this.searchClientes(criterio, 1, lang.hitch(this, function(clientes) {  
				
				if (clientes.length > 0) {
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "El cliente " + this.name.getValue() + " ya existe.");
					domStyle.set(this.progressBar, "display", "none");				
					return;	
				}
				
				domStyle.set(this.message, "color", "#000253");
				html.set(this.message, "Agregando nuevo cliente...");
				domStyle.set(this.progressBar, "display", "block");	
				
				var params = {};
				params.method = "addCliente";
				params.repositoryid = ecm.model.desktop.defaultRepositoryId;
				params.context = json.stringify(this.config.context);
				var cliente = {"name": this.name.getValue(), "activo": this.activo.getValue().toString() === "true"};
				params.cliente = json.stringify(cliente);
				
				Request.invokePluginService("ImportSaadPlugin", "CntCatalogosService",
						{	
							requestParams: params,
							requestCompleteCallback: lang.hitch(this, function(response) {
								
								if (response.error != null) {
									domStyle.set(this.message, "color", "#6B0A0A");
									html.set(this.message, response.error);
									domStyle.set(this.progressBar, "display", "none");							
									return;
								}	
								
								// Clear grid selection
								this.clearGridSelection();
									
								// Add new element to grid
								this.grid.store.newItem(response.cliente);
								this.grid.store.save();
								
								domStyle.set(this.message, "color", "#000253");
								html.set(this.message, "El nuevo cliente " + response.cliente.name + " ha sido creado.");
								domStyle.set(this.progressBar, "display", "none");
								
							})
						}); 				
			}))			
		},
		
		removeGridItemsFromStore: function(removedItems) {
			var remainingItems = [];
			this.grid.store.fetch({ 
				onItem: lang.hitch(this, function(item) {
					var exist = false;
					array.forEach(removedItems, lang.hitch(this, function(removedItem) {
						if (item.id.toString() == removedItem.id.toString())
							exist = true;
					}));
					if (!exist) remainingItems.push(item);
				})
			});				
			
			// update grid store
			var store = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: remainingItems
				}
			});					
			
			// set store
			this.grid.setStore(store);			
		},			
		
		resetValues: function() {
			this.clearGridSelection(this.grid);
			this.clearGridData(this.grid);
			this.setGridPageSize(this.grid, 50);
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");						
		},
		
		setGridPageSize: function(grid, pageSize) {
			if (grid != null) {
				grid.currentPageSize(pageSize);
				grid.resize();
				grid.update();
			}
		},
		
		clearGridSelection: function(grid) {
			if (grid != null)
				grid.selection.clear();
			this.name.reset();
			this.activo.reset();
			this.filtro.reset();
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
		},	
		
		formatBoolean: function(val) {
			return val ? "SI" : "NO";			
		}
			
	});
});
