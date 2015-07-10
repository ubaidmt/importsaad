define([
	"dojo/_base/declare",
	"dojo/_base/lang",
	"dojo/dom",
	"dojo/dom-construct",
	"dojo/on",
	"dojo/_base/array",
	"dojo/dom-style",
	"dojo/dom-geometry",
	"dojo/json",
	"dojo/request/xhr",
	"dojo/keys",
	"dojo/html",
	"dojo/data/ItemFileWriteStore",
	"dojo/store/Memory",
	"ecm/model/Request",
	"ecm/model/Desktop",
	"ecm/widget/FilteringSelect",
	"ecm/widget/dialog/YesNoCancelDialog",
	"ecm/widget/dialog/LoginDialog",
	"dojox/grid/EnhancedGrid",
	"dojox/grid/enhanced/plugins/Pagination",
	"dijit/layout/ContentPane",
	"importSaadPluginDojo/util/Contexto",
	"importSaadPluginDojo/dialog/CntCotizacionDialog",
	"importSaadPluginDojo/dialog/CntContenedorDialog",
	"ecm/widget/layout/_LaunchBarPane",
	"ecm/widget/layout/_RepositorySelectorMixin",
	"dojo/text!./templates/CotizacionesFeaturePane.html"
],

function(declare,
		lang,
		dom,
		domConstruct,
		on,
		array,
		domStyle,
		domGeom,
		json,
		xhr,
		keys,
		html,
		ItemFileWriteStore,
		Memory,
		Request,
		Desktop,
		FilteringSelect,
		YesNoCancelDialog,
		LoginDialog,
		EnhancedGrid,
		Pagination,	
		ContentPane,
		Contexto,
		CntCotizacionDialog,
		CntContenedorDialog,
		_LaunchBarPane,
		_RepositorySelectorMixin,
		template) {

	/**
	 * @name importSaadPluginDojo.feature.CotizacionesFeaturePane
	 * @class Provides a pane that demonstrates how to insert new features into the standard IBM Content Navigator layout.
	 * @augments ecm.widget.layout._LaunchBarPane
	 */
	return declare("importSaadPluginDojo.feature.CotizacionesFeaturePane", [
		_LaunchBarPane,
		_RepositorySelectorMixin
	], {
		/** @lends importSaadPluginDojo.feature.CotizacionesFeaturePane.prototype */

		templateString: template,
		widgetsInTemplate: true,

		postCreate: function() {
			this.inherited(arguments);
			this.setRepositoryTypes("p8");
			this.doRepositorySelectorConnections();			
		},
		
		/**
		 * Sets the repository being used for search.
		 * 
		 * @param repository
		 * 			An instance of {@link ecm.model.Repository}
		 */
		setRepository: function(repository) {
			this.repository = repository;
		},		
		
		/**
		 * Loads the content of the pane. This is a required method to insert a pane into the LaunchBarContainer.
		 */
		loadContent: function() {			
			if (this.currentUser != ecm.model.desktop.userId) {
				this.currentUser = ecm.model.desktop.userId;
				this.config = Contexto.getConfig();
				this.loadSettings();
				this.initEvents();
				this.initGrid();
				this.loadClientes();
				this.loadNavieras();
				this.loadForwarders();
				this.loadProveedores();
				this.loadImportadoras();
				this.loadPuertos();				
				this.referencia.focus();
			}
		},
		
		loadSettings: function() {
			
			var params = {};
			params.method = "getCntSettings";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			
			Request.invokePluginService("ImportSaadPlugin", "SettingsService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								html.set(this.message, response.error);
								domStyle.set(this.progressBar, "display", "none");																					
								return;
							}
							
							this.settings = response.settings;
						})
					}); 				
		},
		
		initEvents: function() {
			on(this.referencia, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
			     	case keys.ENTER:
			     		this.onSearch();
			     		break;
			    }
			}));
			on(this.contenedor, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
			     	case keys.ENTER:
			     		this.onSearch();
			     		break;
			    }
			}));
			on(this.mercancia, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
			     	case keys.ENTER:
			     		this.onSearch();
			     		break;
			    }
			}));				
			on(this.tipo, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
			     	case keys.ENTER:
			     		this.onSearch();
			     		break;
			    }
			}));
			on(this.estado, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
			     	case keys.ENTER:
			     		this.onSearch();
			     		break;
			    }
			}));					
			on(this.maxResults, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
			     	case keys.ENTER:
			     		this.onSearch();
			     		break;
			    }
			}));				
			on(this.filtro, "keyup", lang.hitch(this, function() {
		  		if (this.grid == null)
					return;
		  		if (this.filtro.getValue() != "")
		  			this.grid.filter({"name": "*" + this.filtro.getValue() + "*"});
		  		else
		  			this.grid.filter(null);	
			}));			
		},		
		
		initGrid: function() {
			
			if (this.grid != null)
				return;
			
			var gridStore = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});				
		
			var gridLayout = [         
				{name: 'Referencia', field: 'name', width: '20%', editable: false, noresize: true},
				{name: 'Contenedor', field: 'contenedor', width: '20%', editable: false, noresize: true},
				{name: 'Fecha', field: 'datecreated', width: '10%', editable: false, formatter: this.formatDate, noresize: true},
				{name: 'Cliente', field: 'cliente', width: '15%', editable: false, noresize: true},
				{name: 'Mercancia', field: 'mercancia', width: '15%', editable: false, noresize: true},
				{name: 'Tipo', field: 'tipo', width: '10%', editable: false, datatype: 'number', formatter: this.formatTipoCotizacion, noresize: true},
				{name: 'Estado', field: 'estado', width: '7%', editable: false, datatype: 'number', formatter: this.formatSemaforo, noresize: true}
			];		
										
			this.grid = new EnhancedGrid({
				store: gridStore,
				structure: gridLayout,
				selectionMode: 'multi',
				sortInfo: -3, // desc by fecha
				autoHeight: true,
				formatterScope: this,
				queryOptions: {ignoreCase: true},
				singleClickEdit: false,
		        plugins: {
		            pagination: {
		                pageSizes: ["50","250","500"],
		                defaultPageSize: "50",
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
		},
		
		loadClientes: function() {
			
			if (this.clientes == null) {
				var store = new Memory({
					data: []
				});
			
				this.clientes = new FilteringSelect({
					value: "",
					autoComplete: true,
					pageSize: 30,
					style: "width: 150px;",
					required: false,
			        store: store,
			        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
			    });
				
				this.clientes.placeAt(this.clientesDiv);			
				this.clientes.startup();
				
				on(this.clientes, "keypress", lang.hitch(this, function(evt) {
				    switch(evt.charOrCode) {
				     	case keys.ENTER:
				     		this.onSearch();
				     		break;
				    }
				}));					
			}
			
			var criterio = {};
			criterio.activo = true; // solo clientes activos
			
			var params = {};
			params.method = "searchClientes";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.criterio = json.stringify(criterio);
			params.maxResults = 0;
			
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

							var store = new Memory({
								data: response.clientes
							});							
							
							this.clientes.set('store', store);
							this.clientes.reset();
														
						})
					}); 			
		},	
		
		loadNavieras: function() {
			var criterio = {};
			criterio.activo = true; // solo navieras activas
			
			var params = {};
			params.method = "searchNavieras";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.criterio = json.stringify(criterio);
			params.maxResults = 0;
			
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
							
							this.navierasStore = new Memory({
								data: response.navieras
							});									
						})
					}); 			
		},	
		
		loadForwarders: function() {
			var criterio = {};
			criterio.activo = true; // solo forwarders activas
			
			var params = {};
			params.method = "searchForwarders";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.criterio = json.stringify(criterio);
			params.maxResults = 0;
			
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
							
							this.forwardersStore = new Memory({
								data: response.forwarders
							});														
						})
					}); 			
		},	
		
		loadProveedores: function() {
			
			var criterio = {};
			criterio.activo = true; // solo activos
			
			var params = {};
			params.method = "searchProveedores";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.criterio = json.stringify(criterio);
			params.maxResults = 0;
			
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
							
							this.proveedoresStore = new Memory({
								data: response.proveedores
							});									
						})
					}); 			
		},
		
		loadImportadoras: function() {
			
			var criterio = {};
			criterio.activo = true; // solo activos
			
			var params = {};
			params.method = "searchImportadoras";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.criterio = json.stringify(criterio);
			params.maxResults = 0;
			
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
							
							this.importadorasStore = new Memory({
								data: response.importadoras
							});									
						})
					}); 			
		},

		loadPuertos: function() {
			
			var criterio = {};
			criterio.activo = true; // solo activos
			
			var params = {};
			params.method = "searchPuertos";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.criterio = json.stringify(criterio);
			params.maxResults = 0;
			
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
							
							this.puertosStore = new Memory({
								data: response.puertos
							});									
						})
					}); 			
		},					
		
		onSearch: function() {
			
			if (!this.maxResults.isValid())
				this.maxResults.reset();			
			
			var criterio = {};

			if (this.referencia.isValid() && this.referencia.getValue() != "") 
				criterio.namecontains = this.referencia.getValue();
			if (this.contenedor.isValid() && this.contenedor.getValue() != "") 
				criterio.contenedor = this.contenedor.getValue();
			if (this.mercancia.isValid() && this.mercancia.getValue() != "") 
				criterio.mercancia = this.mercancia.getValue();			
			var cliente = this.getFilteringSelectItem(this.clientes);
			if (cliente != null)
				criterio.cliente = cliente.id;							
			var tipo = this.getFilteringSelectItem(this.tipo);
			if (tipo != null)
				criterio.tipo = tipo.id;
			var estado = this.getFilteringSelectItem(this.estado);
			if (estado != null)
				criterio.estado = estado.id;								
			
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Consultando cotizaciones...");
			domStyle.set(this.progressBar, "display", "block");			
			
			this.searchCotizaciones(criterio, parseInt(this.maxResults.getValue()), lang.hitch(this, function(cotizaciones) {
				
				// Clear grid selection
				this.clearGridSelection();					
			
				var store = new ItemFileWriteStore({
					data: {
						identifier: "id",
						items: cotizaciones
					}
				});										
				
				this.grid.setStore(store);
				this.grid.firstPage();

				if (cotizaciones.length == 1) { // select row if single result
					this.grid.selection.setSelected(0, true);
					this.onGridItemSelect();
				}	
				
				html.set(this.message, "");
				domStyle.set(this.progressBar, "display", "none");					
				
			}))				
		},
		
		searchCotizaciones: function(criterio, maxResults, callback) {			
			
			var params = {};
			params.method = "searchCotizaciones";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.criterio = json.stringify(criterio);
			params.maxResults = maxResults;
			
			Request.invokePluginService("ImportSaadPlugin", "CntCotizacionesService",
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
								callback(response.cotizaciones);	
														
						})
					}); 		
		},	
		
		searchContenedores: function(criterio, maxResults, callback) {			
			
			var params = {};
			params.method = "searchContenedores";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.criterio = json.stringify(criterio);
			params.maxResults = maxResults;
			
			Request.invokePluginService("ImportSaadPlugin", "CntContenedoresService",
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
								callback(response.contenedores);	
														
						})
					}); 		
		},		
		
		disableActionButtons: function(item) {
			this.updateButton.set("disabled", parseInt(item.tipo.toString()) != 0);
		},		
		
		onAdd: function() {
			var dialog = new CntCotizacionDialog({
				title: "Nueva Cotización",
				onAfterSave: lang.hitch(this, function(cotizacion) {
					dialog.hide();
					// consulta y muestra cotizaciones con la misma referencia
					this.searchAndShowByReferencia(cotizacion);			
				})
			});
			
			dialog.setConfig(this.config);
			dialog.setSettings(this.settings);
			dialog.setClientesStore(this.clientes.store);
			dialog.show();				
		},
		
		onGridItemSelect: function() {
			html.set(this.detalleDiv, "");
			var items = this.grid.selection.getSelected();
			if (items.length > 0) {
				var item = items[items.length-1]; // select last
				this.setCotizacionDetalle(item);
				this.disableActionButtons(item);
			}				
		},
		
		onCopy: function() {			
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar una cotización.");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}			
			var item = items[items.length-1]; // select 

			var dialog = new CntCotizacionDialog({
				title: "Copia de Cotización",
				onAfterSave: lang.hitch(this, function(cotizacion) {
					dialog.hide();
					// consulta y muestra cotizaciones con la misma referencia
					this.searchAndShowByReferencia(cotizacion);											
				})
			});
			
			dialog.setConfig(this.config);
			dialog.setSettings(this.settings);
			dialog.setClientesStore(this.clientes.store);
			dialog.setCotizacion(item);
			dialog.setIsCopy(true);
			dialog.show();					
		},			
		
		onUpdate: function() {			
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar una cotización.");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}			
			var item = items[items.length-1]; // select 

			var dialog = new CntCotizacionDialog({
				title: "Actualización de Cotización",
				onAfterSave: lang.hitch(this, function(cotizacion) {
					dialog.hide();
					// consulta y muestra cotizaciones con la misma referencia
					this.searchAndShowByReferencia(cotizacion);	
				}),
				onOpenContenedor: lang.hitch(this, function(contenedor) {
					dialog.hide();
					// abre contenedor asociado
					this.openContenedorAsociado(contenedor);	
				})				
			});
			
			dialog.setConfig(this.config);
			dialog.setSettings(this.settings);
			dialog.setClientesStore(this.clientes.store);
			dialog.setCotizacion(item);
			dialog.show();					
		},
		
		openContenedorAsociado: function(contenedor) {
			var dialog = new CntContenedorDialog({
				title: "Actualización de Contenedor",
				onHide: lang.hitch(this, function() {									
				})
			});
			dialog.setConfig(this.config);
			dialog.setSettings(this.settings);
			dialog.setClientesStore(this.clientes.store);
			dialog.setNavierasStore(this.navierasStore);
			dialog.setForwardersStore(this.forwardersStore);
			dialog.setProveedoresStore(this.proveedoresStore);
			dialog.setImportadorasStore(this.importadorasStore);
			dialog.setPuertosStore(this.puertosStore);			
			dialog.setContenedorId(contenedor.id.toString());
			dialog.show();			
		},	
		
		searchAndShowByReferencia: function(cotizacion) {
			var criterio = {};
			criterio.nameequals = cotizacion.name;
			this.searchCotizaciones(criterio, 50, lang.hitch(this, function(cotizaciones) {  			
			
				var store = new ItemFileWriteStore({
					data: {
						identifier: "id",
						items: cotizaciones
					}
				});										
				
				this.grid.setStore(store);
				this.grid.firstPage();

				if (cotizaciones.length > 0) { // select first row
					this.grid.selection.setSelected(0, true);
					this.onGridItemSelect();
				}	
				
				html.set(this.message, "");
				domStyle.set(this.progressBar, "display", "none");					
				
			}))				
		},
		
		onOpen: function() {
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar una cotización.");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}			
			var item = items[items.length-1]; // select 
			
			if (this.isValidId(item.pdf.toString())) {
				// show p8 document
				this.showDocument(item.pdf.toString());
				html.set(this.message, "");						
			}			
		},	
		
		onDownload: function() {
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar una cotización.");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}			
			var item = items[items.length-1]; // select
			
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Descargando cotización...");
			domStyle.set(this.progressBar, "display", "block");			

			this.generaCotizacionReporte(item, 2, lang.hitch(this, function(reporte) {
				if (this.isValidId(reporte)) {		
					this.downloadDocument(reporte);
					html.set(this.message, "");
					domStyle.set(this.progressBar, "display", "none");						
				}
			}))								
		},
		
		onDelete: function() {
			
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar por lo menos una cotización.");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}			
			
			var confirmDialog = new YesNoCancelDialog({
				title: "Eliminar Cotizaciones",
				text: "¿Está seguro de eliminar las <b>" + items.length + "</b> cotizaciones seleccionadas?",
				onYes: lang.hitch(this, function() {
					confirmDialog.hide();
					
					domStyle.set(this.message, "color", "#000253");
					html.set(this.message, "Eliminando las cotizaciones seleccionadas...");
					domStyle.set(this.progressBar, "display", "block");
					
					// Clear grid selection
					this.clearGridSelection();	
					
					var cotizaciones = [];
					array.forEach(items, lang.hitch(this, function(item) {							
						var cotizacion = {"id": item.id.toString()};
						cotizaciones.push(cotizacion);
					}));
					
					this.deleteCotizaciones(cotizaciones, lang.hitch(this, function(count) {
						// Update grid store
						var removedItems = [];
						array.forEach(items, lang.hitch(this, function(item) {							
							removedItems.push({"id": item.id.toString()});
						}));
						this.removeGridItemsFromStore(removedItems);									
						
						domStyle.set(this.message, "color", "#000253");
						html.set(this.message, "Las <b>" + items.length + "</b> cotizaciones seleccionadas han sido eliminadas.");
						domStyle.set(this.progressBar, "display", "none");	
					}))						
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
		
		deleteCotizaciones: function(cotizaciones, callback) {
			var params = {};
			params.method = "deleteCotizacion";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.cotizaciones = json.stringify(cotizaciones);
			
			Request.invokePluginService("ImportSaadPlugin", "CntCotizacionesService",
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
								callback(response.count);							
						})
					}); 			
		},
		
		onClean: function() {
			this.referencia.reset();
			this.contenedor.reset();
			this.mercancia.reset();
			if (this.clientes != null) this.clientes.reset();			
			this.tipo.reset();
			this.estado.reset();
			this.filtro.reset();
			this.clearGridSelection();	
			this.clearGridData(this.grid);
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");
			this.referencia.focus();
		},
		
		setCotizacionDetalle: function(item) {
			
			var datos = json.parse(item.datos.toString());
			var content = '';			
			
			if (!("impuestos" in datos)) {
				content += '<br>';
				content += '<table style="width: 100%;border-collapse: collapse;">';
				content += '<tr>';
				content += '<td style="width:100%;font-family:arial;font-size:12px;font-weight:normal;font-style:italic;color:black;">Detalle de cotizaci&oacute;n no disponible ...</td>';
				content += '</tr>';
				content += '</table>';
				html.set(this.detalleDiv, content);
				return;
			}
						
			// datos generales
			content += '<br>';
			content += '<table style="width: 100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width:100%;font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Datos generales de la cotizaci&oacute;n (' + this.formatTipoCotizacion(parseInt(item.tipo.toString())) + ')</td>';
			content += '</tr>';
			content += '</table>';			
			content += '<br>';
			content += '<table style="width: 100%;border-collapse: collapse;">';			
			content += '<tr>';
			content += '<td style="width: 50%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Cliente:</label></td>';
			content += '<td align="left" style="width: 50%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + item.cliente.toString() + '</div></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width: 50%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Fecha Cotizaci&oacute;n:</label></td>';
			content += '<td align="left" style="width: 50%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + this.formatDate(item.datecreated.toString()) + '</div></td>';
			content += '</tr>';			
			content += '<tr>';
			content += '<td style="width: 50%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Referencia:</label></td>';
			content += '<td align="left" style="width: 50%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + item.name.toString() + '</div></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width: 50%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Contenedor:</label></td>';
			content += '<td align="left" style="width: 50%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + datos.contenedor.toString() + '</div></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width: 50%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Mercanc&iacute;a:</label></td>';
			content += '<td align="left" style="width: 50%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + datos.mercancia.toString() + '</div></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width: 50%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">ETA:</label></td>';
			content += '<td align="left" style="width: 50%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + this.formatDate(datos.ETA.toString()) + '</div></td>';
			content += '</tr>';								
			content += '<tr>';
			content += '<td style="width: 50%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Incremento:</label></td>';
			content += '<td align="left" style="width: 50%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(datos.incremento) + '</div></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width: 50%;background-color:#14469C;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:white;">Total Cotizaci&oacute;n:</label></td>';
			content += '<td align="left" style="width: 50%;background-color:#14469C;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:white;">$ ' + this.formatCurrency(parseFloat(item.monto.toString())) + '</div></td>';
			content += '</tr>';
			content += '</table>';
			// calculo de impuestos
			content += '<br>';
			content += '<table style="width: 100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width:100%;font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">C&aacute;lculo de impuestos</td>';
			content += '</tr>';
			content += '</table>';	
			content += '<br>';			
			content += '<table style="width: 100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Cantidad:</label></td>';
			content += '<td align="left" style="width: 25%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + ("cantidad" in datos ? this.formatCurrency(datos.cantidad) : "") + '</div></td>';
			content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">IGI:</label></td>';
			content += '<td align="left" style="width: 25%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(datos.impuestos.IGI) + '</div></td>';								
			content += '</tr>';			
			content += '<tr>';
			content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Total (USD):</label></td>';
			content += '<td align="left" style="width: 25%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(datos.total) + '</div></td>';
			content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">DTA:</label></td>';
			content += '<td align="left" style="width: 25%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(datos.impuestos.DTA) + '</div></td>';								
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Flete:</label></td>';
			content += '<td align="left" style="width: 25%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(datos.flete) + '</div></td>';
			content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">IVA:</label></td>';
			content += '<td align="left" style="width: 25%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(datos.impuestos.IVA) + '</div></td>';						
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Total con Flete (USD):</label></td>';
			content += '<td align="left" style="width: 25%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(datos.total + datos.flete) + '</div></td>';
			content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">PREV:</label></td>';
			content += '<td align="left" style="width: 25%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(datos.impuestos.PREV) + '</div></td>';									
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Tipo de Cambio:</label></td>';
			content += '<td align="left" style="width: 25%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + datos.tipoCambio + '</div></td>';
			content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">CNT:</label></td>';
			content += '<td align="left" style="width: 25%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(datos.impuestos.CNT) + '</div></td>';								
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Valor Aduanal:</label></td>';
			content += '<td align="left" style="width: 25%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(datos.valorAduanal) + '</div></td>';
			content += '<td style="width: 25%;background-color:#14469C;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:white;">Total Impuestos:</label></td>';
			content += '<td align="left" style="width: 25%;background-color:#14469C;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:white;">$ ' + this.formatCurrency(datos.impuestos.total) + '</div></td>';								
			content += '</tr>';
			content += '</table>';				
			// observaciones cotizacion
			if (datos.observaciones.toString() != "") {
				content += '<br>';
				content += '<table style="width: 100%;border-collapse: collapse;">';
				content += '<tr>';
				content += '<td style="width:100%;font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Observaciones</td>';
				content += '</tr>';
				content += '<tr>';
				content += '<td align="left" style="width: 100%;"><div style="font-family:arial;font-size:12px;font-weight:normal;color:black;">' + datos.observaciones.toString().replace(/\n/g,'<br>') + '</div></td>';
				content += '</tr>';				
				content += '</table>';						
			}
			// contenedor asociado
			domConstruct.destroy("tblContenedor"); // dom destroy
			domConstruct.destroy("consultandoContenedorRow"); // dom destroy
			domConstruct.destroy("consultandoContenedorLabel"); // dom destroy
			content += '<br>';
			content += '<table id="tblContenedor" style="width: 100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width: 100%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Contenedor asociado</label></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td id="consultandoContenedorRow" align="center" style="width: 100%;"><label id="consultandoContenedorLabel" style="font-family:arial;font-size:12px;font-weight:normal;font-style:italic;color:black;">no existe contenedor asociado</label></td>';		
			content += '</tr>';				
			content += '</table>';			
			if (this.isValidId(item.contenedorobj)) {
				var criterio = {};
				criterio.id = item.contenedorobj.toString();
				this.searchContenedores(criterio, 1, lang.hitch(this, function(contenedores) {
					if (contenedores.length > 0) {
						domConstruct.destroy("consultandoContenedorRow"); // dom destroy
						domConstruct.destroy("consultandoContenedorLabel"); // dom destroy
						
						var contenedor = contenedores[0];
						var viewContenedorLinkId = "viewContenedorLink" + contenedor.id.toString();
						domConstruct.destroy(viewContenedorLinkId); // dom destroy				
						var row = '';
						row += '<tr>';
						row += '<td style="width: 100%;"><a href="#" id="' + viewContenedorLinkId + '" title="Abrir Contenedor" style="font-family:arial;font-size:12px;font-weight:normal;color:#14469C;">' + contenedor.name.toString() + '</a></td>';
						row += '</tr>';
						domConstruct.toDom(row);
						domConstruct.place(row, "tblContenedor");
						on(dom.byId(viewContenedorLinkId), "click", lang.hitch(this, function() {
							this.openContenedorAsociado(contenedor);
						}));						
					}							
				}))	
			}			
			// fracciones cotizadas
			content += '<br>';
			content += '<table style="width: 100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width:100%;font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Detalle de la cotizaci&aacute;n</td>';
			content += '</tr>';
			content += '</table>';				
			array.forEach(datos.fracciones, lang.hitch(this, function(fraccion) {
				
				// si la medida se esta en pulgadas, se realiza la conversion a metros
				var ancho = parseFloat(fraccion.ancho.toString());
				if ("medida" in fraccion && fraccion.medida.toString() == "in")
					ancho = ancho * 0.0254;
				
				content += '<br>';
				content += '<table style="width: 100%;border-collapse: collapse;">';		
				content += '<tr>';
				content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Fracci&oacute;n:</label></td>';
				content += '<td align="left" style="width: 25%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + fraccion.fraccion.toString() + ' (' + this.formatUnidadComercial(parseInt(fraccion.unidadComercial.toString())) + ')</div></td>';
				content += '<td colspan="2" style="width: 50%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Observaciones</label></td>';
				content += '</tr>';
				content += '<tr>';
				content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Precio M&iacute;nimo (USD):</label></td>';
				content += '<td align="left" style="width: 25%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(parseFloat(fraccion.precioMinimo.toString())) + '</div></td>';
				content += '<td colspan="2" rowspan="7" valign="top" style="width: 50%;"><div style="font-family:arial;font-size:12px;font-weight:normal;color:black;">' + fraccion.observaciones.toString() + '</div></td>';
				content += '</tr>';
				content += '<tr>';
				content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Aumento (USD):</label></td>';			
				content += '<td colspan="3" align="left" style="width: 75%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(parseFloat(fraccion.aumento.toString())) + '</div></td>';							
				content += '</tr>';
				content += '<tr>';
				content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Ancho:</label></td>';
				content += '<td colspan="3" align="left" style="width: 75%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + ancho + '</div></td>';							
				content += '</tr>';
				content += '<tr>';
				content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Precio Unitario (USD):</label></td>';
				content += '<td colspan="3" align="left" style="width: 75%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(parseFloat(fraccion.precioUnitario.toString())) + '</div></td>';											
				content += '</tr>';		
				content += '<tr>';
				content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Cantidad:</label></td>';
				content += '<td colspan="3" align="left" style="width: 75%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + this.formatCurrency(parseFloat(fraccion.cantidad.toString())) + '</div></td>';
				content += '</tr>';
				content += '<tr>';
				content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Ajuste Final:</label></td>';
				content += '<td colspan="3" align="left" style="width: 75%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + ("ajuste" in fraccion ? this.formatCurrency(parseFloat(fraccion.ajuste.toString())) : "0.00") + " %" + '</div></td>';
				content += '</tr>';
				content += '<tr>';
				content += '<td style="width: 25%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Total (USD):</label></td>';
				content += '<td colspan="3" align="left" style="width: 75%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">$ ' + this.formatCurrency(parseFloat(fraccion.total.toString())) + '</div></td>';											
				content += '</tr>';				
				content += '</table>';			
				content += '<hr style="border: 0; height: 1px; background-image: linear-gradient(to right, rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.75), rgba(0, 0, 0, 0));">';
			}));	
			
			html.set(this.detalleDiv, content);
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
		
		clearGridSelection: function() {
			if (this.grid != null) this.grid.selection.clear();
			html.set(this.detalleDiv, "");
			this.updateButton.set("disabled", false);			
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
		
		generaCotizacionReporte: function(cotizacion, salida, callback) {
			var datos = json.parse(cotizacion.datos.toString());
		
			// request a enviar
			var serviceRequest = "/excelecm-rest-broker-common/jaxrs/reportsmodule/getReporteComplejo";				
			serviceRequest += "?tipoSalida=" + salida;
			serviceRequest += "&claseImpl=com.importsaad.jasper.gc.Cotizacion";
			serviceRequest += "&nombrePlantilla=gc/Cotizacion";
			serviceRequest += "&params=Cliente=" + this.escapeJasperParameter(cotizacion.cliente.toString()) + "|DateCreated=" + this.escapeJasperParameter(cotizacion.datecreated.toString().replace(/[-]/g,'')) + "|Referencia=" + this.escapeJasperParameter(cotizacion.name.toString()) + "|Contenedor=" + this.escapeJasperParameter(datos.contenedor) + "|Mercancia=" + this.escapeJasperParameter(datos.mercancia) + "|ETA=" + this.escapeJasperParameter(datos.ETA.replace(/[-]/g,'')) + "|Incremento=" + datos.incremento + "|Monto=" + cotizacion.monto.toString() + "|Observaciones=" + this.escapeJasperParameter(datos.observaciones.toString()) + "|TipoCotizacion=" + this.formatTipoCotizacion(parseInt(cotizacion.tipo.toString())) + "|Cantidad=" + datos.cantidad.toString() + "|Total=" + datos.total.toString() + "|Flete=" + datos.flete.toString() + "|TipoCambio=" + datos.tipoCambio.toString() + "|ValorAduanal=" + datos.valorAduanal.toString() + "|IGI=" + datos.impuestos.IGI.toString() + "|DTA=" + datos.impuestos.DTA.toString() + "|IVA=" + datos.impuestos.IVA.toString() + "|PREV=" + datos.impuestos.PREV.toString() + "|CNT=" + datos.impuestos.CNT.toString() + "|TotalImpuestos=" + datos.impuestos.total.toString();
			serviceRequest += "&condiciones=Id=" + cotizacion.id.toString();
			serviceRequest += "&nombreArchivo=" + this.escapeJasperParameter(cotizacion.name.toString());
			serviceRequest += "&os=" + ecm.model.desktop.getRepository(ecm.model.desktop.defaultRepositoryId).objectStoreName;
			
			//console.log(serviceRequest);
										
			xhr(serviceRequest, {
				method: "GET",
				handleAs: "json",
				preventCache: true,
				sync : false, 
				timeout: 10000, // 10 secs
				headers: { "Content-Type": "application/json"}
			}).then(lang.hitch(this, function(data) {	
								
				if (data.status == 1) {
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "Ocurrió un error al momento de generar el reporte: " + data.desc);
					domStyle.set(this.progressBar, "display", "none");						
					return;						
				}
				
				if (lang.isFunction(callback))
					callback(data.desc);						

			})), lang.hitch(this, function(err) {	
				
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Ocurrió un error al momento de generar el reporte: " + err);
				domStyle.set(this.progressBar, "display", "none")
			
			});		
		},			
		
	    escapeJasperParameter: function(value) {
	    	return value.replace(/[|#&=]/g,''); // remove reserved parameters characters
	    },
		
		retrieveDefaultRepository: function(callback) {
			var repository = ecm.model.desktop.getRepository(ecm.model.desktop.defaultRepositoryId);
			if (!repository.connected) {
				var dialog = new LoginDialog({
					onConnected: lang.hitch(this, function() {
						if (lang.isFunction(callback))
							callback(repository);
					}),
					onLoginFailed: lang.hitch(this, function() {
					})
				});
				dialog.show();					
			} else {
				if (lang.isFunction(callback))
					callback(repository);				
			}
		},
		
		showDocument: function(docId) {
			this.retrieveDefaultRepository(lang.hitch(this, function(repository) {												
				this.showItem(repository, docId);
			}))				
		},
		
		showItem: function(repository, docId) {
			repository.retrieveItem(docId, lang.hitch(this, function(item) {
				var viewer = Desktop.getViewerForItem(item);
				if(!viewer) {
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "No se pudo obtener el visor correspondiente del elemento seleccionado");				
					return;
				} else {
					var viewerPopupQueue = [];
					if (viewer.launchInSeparateWindow) {
						var viewerUrl = viewer.getLaunchUrl(item);		
						var popupWindow = window.open("", "_blank");
						ecm.widget.viewer.model.ViewerItem.loadSecure(popupWindow, viewerUrl, true);
					} else {
						viewerPopupQueue.push(item);
					}
					if (viewerPopupQueue.length > 0)
						ecm.widget.dialog.contentViewerWindow.open(viewerPopupQueue);
				}
			}));
		},
		
		downloadDocument: function(docId) {
			this.retrieveDefaultRepository(lang.hitch(this, function(repository) {												
				this.downloadItem(repository, docId);
			}))							
		},
		
		downloadItem: function(repository, docId) {
			repository.retrieveItem(docId, lang.hitch(this, function(item) {
				var params = {
					repositoryId: item.repository.id,
					docid: item.id,
					template_name: item.template,
					disposition: "attachment",
					transform: "native",
					version: "released"
				};
				ecm.model.Request.setSecurityToken(params);
				var url = Request.getServiceRequestUrl("getDocument", item.repository.type, params);
				window.open(url, "_blank");
			}));			
		},
		
		getFilteringSelectItem: function(obj) {
			var item = null;
			var value = obj.get('value');
			if (value != "")
				var item = obj.store.get(value);
			return item;
		},
		
		formatUnidadComercial: function(num) {
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
		
		formatTipoCotizacion: function(num) {
			if (num == null)
				return "";
			switch(num) {
				case 0:
					return "ORIGINAL";
					break;
				case 1:
					return "FINAL";
					break;	
			}			
		},	
		
		formatEstadoCotizacion: function(num) {
			if (num == null)
				return "";
			switch(num) {
				case 0:
					return "PENDEINTE";
					break;
				case 1:
					return "REALIZADA";
					break;	
			}			
		},
		
		formatSemaforo: function(num) {
			if (num == null)
				return "";
			switch(num) {
				case 1:
					return "<div class='alertGreenIcon' style='width:15px;height:15px;'></div>";
					break;				
				case 0:
					return "<div class='alertRedIcon' style='width:15px;height:15px;'></div>";
					break;
			}			
		},			
		
		getFormatedDateValue: function(strDate) {
			if (strDate.length < 10)
				return strDate;
			else
				return strDate.substring(0,10);			
		},	
		
		isValidId: function(val) {
			if (val == null) return false;
		    var pattern = new RegExp("^({)([A-Z0-9]{8})(-)([A-Z0-9]{4})(-)([A-Z0-9]{4})(-)([A-Z0-9]{4})(-)([A-Z0-9]{12})(})$");
			return pattern.test(val);
		},			
		
	    formatDate: function(strVal) {
	    	// expected: yyyy-MM-dd
	    	// convert to: dd/MM/yyyy
	    	if (strVal == null || strVal.length == 0)
	    		return strVal;
	    	return strVal.substring(8,10) + "/" + strVal.substring(5,7) + "/" + strVal.substring(0,4);  	
	    },
	    
		formatInteger: function(num) {
			if (num == null)
				return "";
		    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
		},		    
		
		formatCurrency: function(num) {
		    var p = num.toFixed(2).split(".");
		    return p[0].split("").reverse().reduce(function(acc, num, i, orig) {
		        return  num + (i && !(i % 3) ? "," : "") + acc;
		    }, "") + "." + p[1];
		}
				
	});
});