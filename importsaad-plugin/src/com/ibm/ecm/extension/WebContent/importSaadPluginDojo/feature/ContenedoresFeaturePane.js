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
	"importSaadPluginDojo/dialog/CntContenedorDialog",
	"importSaadPluginDojo/dialog/CntCotizacionDialog",
	"ecm/widget/layout/_LaunchBarPane",
	"ecm/widget/layout/_RepositorySelectorMixin",
	"dojo/text!./templates/ContenedoresFeaturePane.html"
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
		CntContenedorDialog,
		CntCotizacionDialog,
		_LaunchBarPane,
		_RepositorySelectorMixin,
		template) {

	/**
	 * @name importSaadPluginDojo.feature.ContenedoresFeaturePane
	 * @class Provides a pane that demonstrates how to insert new features into the standard IBM Content Navigator layout.
	 * @augments ecm.widget.layout._LaunchBarPane
	 */
	return declare("importSaadPluginDojo.feature.ContenedoresFeaturePane", [
		_LaunchBarPane,
		_RepositorySelectorMixin
	], {
		/** @lends importSaadPluginDojo.feature.ContenedoresFeaturePane.prototype */

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
				this.contenedor.focus();
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
			on(this.contenedor, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
			     	case keys.ENTER:
			     		this.onSearch();
			     		break;
			    }
			}));
			on(this.pedimento, "keypress", lang.hitch(this, function(evt) {
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
			on(this.creacionDesde, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
			     	case keys.ENTER:
			     		this.onSearch();
			     		break;
			    }
			}));
			on(this.creacionHasta, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
			     	case keys.ENTER:
			     		this.onSearch();
			     		break;
			    }
			}));
			on(this.baseDesde, "keypress", lang.hitch(this, function(evt) {
			    switch(evt.charOrCode) {
			     	case keys.ENTER:
			     		this.onSearch();
			     		break;
			    }
			}));
			on(this.baseHasta, "keypress", lang.hitch(this, function(evt) {
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
				{name: 'Contenedor', field: 'name', width: '14%', editable: false, noresize: true},
				{name: 'Fecha', field: 'fechabase', width: '8%', editable: false, formatter: this.formatDate, noresize: true},
				{name: 'Pedimento', field: 'pedimento', width: '9%', editable: false, noresize: true},
				{name: 'Cliente', field: 'cliente', width: '13%', editable: false, noresize: true},
				{name: 'Naviera', field: 'naviera', width: '13%', editable: false, noresize: true},
				{name: 'Forwarder', field: 'forwarder', width: '13%', editable: false, noresize: true},
				{name: 'Mercancia', field: 'mercancia', width: '13%', editable: false, noresize: true},
				{name: 'Etapa', field: 'estado', width: '16%', editable: false, formatter: this.formatEstado, noresize: true},
				{name: 'Sem', field: 'semaforo', width: '5%', editable: false, formatter: this.formatSemaforo, noresize: true}
			];		
										
			this.grid = new EnhancedGrid({
				store: gridStore,
				structure: gridLayout,
				selectionMode: 'multi',
				sortInfo: -2, // desc by fecha
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
					style: "width: 160px;",
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
			
			if (this.navieras == null) {
				var store = new Memory({
					data: []
				});
			
				this.navieras = new FilteringSelect({
					value: "",
					autoComplete: true,
					pageSize: 30,
					style: "width: 160px;",
					required: false,
			        store: store,
			        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
			    });
				
				this.navieras.placeAt(this.navierasDiv);			
				this.navieras.startup();
				
				on(this.navieras, "keypress", lang.hitch(this, function(evt) {
				    switch(evt.charOrCode) {
				     	case keys.ENTER:
				     		this.onSearch();
				     		break;
				    }
				}));					
			}
			
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
							
							var store = new Memory({
								data: response.navieras
							});							
							
							this.navieras.set('store', store);
							this.navieras.reset();
														
						})
					}); 			
		},	
		
		loadForwarders: function() {
			
			if (this.forwarders == null) {
				var store = new Memory({
					data: []
				});
			
				this.forwarders = new FilteringSelect({
					value: "",
					autoComplete: true,
					pageSize: 30,
					style: "width: 160px;",
					required: false,
			        store: store,
			        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
			    });
				
				this.forwarders.placeAt(this.forwardersDiv);			
				this.forwarders.startup();
				
				on(this.forwarders, "keypress", lang.hitch(this, function(evt) {
				    switch(evt.charOrCode) {
				     	case keys.ENTER:
				     		this.onSearch();
				     		break;
				    }
				}));					
			}
			
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
							
							var store = new Memory({
								data: response.forwarders
							});							
							
							this.forwarders.set('store', store);
							this.forwarders.reset();
														
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

			if (this.contenedor.isValid() && this.contenedor.getValue() != "") 
				criterio.name = this.contenedor.getValue();	
			if (this.pedimento.isValid() && this.pedimento.getValue() != "") 
				criterio.pedimento = this.pedimento.getValue();
			if (this.mercancia.isValid() && this.mercancia.getValue() != "") 
				criterio.mercancia = this.mercancia.getValue();					
			var cliente = this.getFilteringSelectItem(this.clientes);
			if (cliente != null)
				criterio.cliente = cliente.id;
			var naviera = this.getFilteringSelectItem(this.navieras);
			if (naviera != null)
				criterio.naviera = naviera.id;
			var forwarder = this.getFilteringSelectItem(this.forwarders);
			if (forwarder != null)
				criterio.forwarder = forwarder.id;			
			if (this.creacionDesde.isValid() && this.creacionDesde.getValue() != "") 
				criterio.creaciondesde = this.getFormatedDateValue(this.creacionDesde.getValue());
			if (this.creacionHasta.isValid() && this.creacionHasta.getValue() != "") 
				criterio.creacionhasta = this.getFormatedDateValue(this.creacionHasta.getValue());
			if (this.baseDesde.isValid() && this.baseDesde.getValue() != "") 
				criterio.basedesde = this.getFormatedDateValue(this.baseDesde.getValue());
			if (this.baseHasta.isValid() && this.baseHasta.getValue() != "") 
				criterio.basehasta = this.getFormatedDateValue(this.baseHasta.getValue());			
			var estado = this.getFilteringSelectItem(this.estados);
			if (estado != null)
				criterio.estado = estado.id;
			var semaforo = this.getFilteringSelectItem(this.semaforos);
			if (semaforo != null)
				criterio.semaforo = semaforo.id;
			
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Consultando contenedores...");
			domStyle.set(this.progressBar, "display", "block");			
			
			this.searchContenedores(criterio, parseInt(this.maxResults.getValue()), lang.hitch(this, function(contenedores) {
				
				// Clear grid selection
				this.clearGridSelection();					
			
				var store = new ItemFileWriteStore({
					data: {
						identifier: "id",
						items: contenedores
					}
				});										
				
				this.grid.setStore(store);
				this.grid.firstPage();

				if (contenedores.length == 1) { // select row if single result
					this.grid.selection.setSelected(0, true);
					this.onGridItemSelect();
				}	
				
				html.set(this.message, "");
				domStyle.set(this.progressBar, "display", "none");					
				
			}))				
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

		onGridItemSelect: function() {
			var items = this.grid.selection.getSelected();
			if (items.length > 0) {
				var item = items[items.length-1]; // select last
				this.showContenedorDetalle(item);
			}				
		},		
		
		onAdd: function() {
			var dialog = new CntContenedorDialog({
				title: "Nuevo Contenedor",
				onHide: lang.hitch(this, function() {
					// en caso de haber creado un nuevo contenedor se muestra en la consulta
					if (dialog.contenedor != null && dialog.updated) {
						domStyle.set(this.message, "color", "#000253");
						html.set(this.message, "Consultando contenedores...");
						domStyle.set(this.progressBar, "display", "block");		
						
						var criterio = {};
						criterio.id = dialog.contenedor.id;
						this.searchContenedores(criterio, 1, lang.hitch(this, function(contenedores) {
							
							// Clear grid selection
							this.clearGridSelection();					
						
							var store = new ItemFileWriteStore({
								data: {
									identifier: "id",
									items: contenedores
								}
							});										
							
							this.grid.setStore(store);
							this.grid.firstPage();

							if (contenedores.length == 1) { // select row if single result
								this.grid.selection.setSelected(0, true);
								this.onGridItemSelect();
							}	
							
							html.set(this.message, "");
							domStyle.set(this.progressBar, "display", "none");					
							
						}))							
					}
					// en caso de abrir la cotizacion asociada
					else if (dialog.cotizacionasociada != null && dialog.opencotizacionasociada) {
						this.openCotizacionAsociada(dialog.cotizacionasociada);							
					}					
				})
			});
			dialog.setConfig(this.config);
			dialog.setSettings(this.settings);
			dialog.setClientesStore(this.clientes.store);
			dialog.setNavierasStore(this.navieras.store);
			dialog.setForwardersStore(this.forwarders.store);
			dialog.setProveedoresStore(this.proveedoresStore);
			dialog.setImportadorasStore(this.importadorasStore);
			dialog.setPuertosStore(this.puertosStore);	
			dialog.show();						
		},
				
		onUpdate: function() {			
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar un contenedor");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}			
			var item = items[items.length-1]; // select 
			
			var dialog = new CntContenedorDialog({
				title: "Actualización de Contenedor",
				onHide: lang.hitch(this, function() {
					// en caso de haber actualizado el contenedor se muestra en la consulta
					if (dialog.contenedor != null && dialog.updated) {
						domStyle.set(this.message, "color", "#000253");
						html.set(this.message, "Consultando contenedores...");
						domStyle.set(this.progressBar, "display", "block");		
						
						var criterio = {};
						criterio.id = dialog.contenedor.id;
						this.searchContenedores(criterio, 1, lang.hitch(this, function(contenedores) {
							
							// Clear grid selection
							this.clearGridSelection();					
						
							var store = new ItemFileWriteStore({
								data: {
									identifier: "id",
									items: contenedores
								}
							});										
							
							this.grid.setStore(store);
							this.grid.firstPage();

							if (contenedores.length == 1) { // select row if single result
								this.grid.selection.setSelected(0, true);
								this.onGridItemSelect();
							}	
							
							html.set(this.message, "");
							domStyle.set(this.progressBar, "display", "none");					
							
						}))	
					}
					// en caso de abrir la cotizacion asociada
					else if (dialog.cotizacionasociada != null && dialog.opencotizacionasociada) {
						this.openCotizacionAsociada(dialog.cotizacionasociada);							
					}										
				})
			});
			dialog.setConfig(this.config);
			dialog.setSettings(this.settings);
			dialog.setClientesStore(this.clientes.store);
			dialog.setNavierasStore(this.navieras.store);
			dialog.setForwardersStore(this.forwarders.store);
			dialog.setProveedoresStore(this.proveedoresStore);
			dialog.setImportadorasStore(this.importadorasStore);
			dialog.setPuertosStore(this.puertosStore);			
			dialog.setContenedorId(item.id.toString());
			dialog.show();									
		},
		
		openCotizacionAsociada: function(cotizacion) {
			var dialog = new CntCotizacionDialog({
				title: "Actualización de Cotización",
				onAfterSave: lang.hitch(this, function(cotizacion) {
					dialog.hide();
				}),
				onOpenContenedor: lang.hitch(this, function(contenedor) {
					dialog.hide();	
				})					
			});
			dialog.setConfig(this.config);
			dialog.setSettings(this.settings);
			dialog.setClientesStore(this.clientes.store);
			dialog.setCotizacion(cotizacion);
			dialog.show();			
		},		
		
		onDelete: function() {
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar por lo menos un contenedor");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}	
			
			var confirmDialog = new YesNoCancelDialog({
				title: "Eliminar Contenedores",
				text: "¿Está seguro de eliminar los <b>" + items.length + "</b> contenedores seleccionados?",
				onYes: lang.hitch(this, function() {
					confirmDialog.hide();
					
					domStyle.set(this.message, "color", "#000253");
					html.set(this.message, "Eliminando los contenedores seleccionadas...");
					domStyle.set(this.progressBar, "display", "block");
					
					// Clear grid selection
					this.clearGridSelection();	
					
					var contenedores = [];
					array.forEach(items, lang.hitch(this, function(item) {							
						var contenedor = {"id": item.id.toString()};
						contenedores.push(contenedor);
					}));					

					var params = {};
					params.method = "deleteContenedor";
					params.repositoryid = ecm.model.desktop.defaultRepositoryId;
					params.context = json.stringify(this.config.context);
					params.contenedores = json.stringify(contenedores);
					
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

									// Update grid store
									var removedItems = [];
									array.forEach(items, lang.hitch(this, function(item) {							
										removedItems.push({"id": item.id.toString()});
									}));
									this.removeGridItemsFromStore(removedItems);									
																	
									domStyle.set(this.message, "color", "#000253");
									html.set(this.message, "Los <b>" + items.length + "</b> contenedores seleccionados han sido eliminados");
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
		
		onClean: function() {
			this.contenedor.reset();
			this.pedimento.reset();
			this.mercancia.reset();
			if (this.clientes != null) this.clientes.reset();
			if (this.navieras != null) this.navieras.reset();
			if (this.forwarders != null) this.forwarders.reset();
			this.creacionDesde.reset();
			this.creacionHasta.reset();
			this.baseDesde.reset();
			this.baseHasta.reset();			
			this.estados.reset();
			this.semaforos.reset();
			this.filtro.reset();
			this.clearGridSelection();	
			this.clearGridData(this.grid);
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");
			this.contenedor.focus();
		},
		
		showContenedorDetalle: function(item) {
			var datos = json.parse(item.datos.toString());
			var content = '';

			// datos generales
			content += '<br>';
			content += '<table style="width: 100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width:100%;font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Datos generales</td>';
			content += '</tr>';
			content += '</table>';			
			content += '<table style="width: 100%;border-collapse: collapse;">';			
			content += '<tr>';
			content += '<td style="width: 30%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Contenedor:</label></td>';
			content += '<td align="left" style="width: 70%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + item.name.toString() + '</div></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width: 30%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Cliente:</label></td>';
			content += '<td align="left" style="width: 70%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + item.cliente.toString() + '</div></td>';
			content += '</tr>';			
			content += '<tr>';
			content += '<td style="width: 30%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Fecha base:</label></td>';
			content += '<td align="left" style="width: 70%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + this.formatDate(item.fechabase.toString()) + '</div></td>';
			content += '</tr>';			
			content += '<tr>';
			content += '<td style="width: 30%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Fecha creaci&oacute;n:</label></td>';
			content += '<td align="left" style="width: 70%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + this.formatDate(item.datecreated.toString()) + '</div></td>';
			content += '</tr>';			
			content += '<tr>';
			content += '<td style="width: 30%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Pedimento:</label></td>';
			content += '<td align="left" style="width: 70%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + item.pedimento.toString() + '</div></td>';
			content += '</tr>';
			content += '</tr>';
			content += '<td style="width: 30%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Mercancia:</label></td>';
			content += '<td align="left" style="width: 70%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + item.mercancia.toString() + '</div></td>';
			content += '</tr>';			
			content += '<tr>';
			content += '<td style="width: 30%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Naviera:</label></td>';
			content += '<td align="left" style="width: 70%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + item.naviera.toString() + '</div></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width: 30%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Forwarder:</label></td>';
			content += '<td align="left" style="width: 70%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + item.forwarder.toString() + '</div></td>';			
			content += '</table>';		
			// estado actual			
			content += '<br>';
			content += '<table style="width: 100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width:100%;font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Estado actual</td>';
			content += '</tr>';
			content += '</table>';			
			content += '<table style="width: 100%;border-collapse: collapse;">';			
			content += '<tr>';
			content += '<td style="width: 30%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Etapa:</label></td>';
			content += '<td align="left" style="width: 70%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + this.formatEstado(parseInt(item.estado.toString())) + '</div></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width: 30%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">Sem&aacute;foro:</label></td>';
			content += '<td align="left" style="width: 70%;"><div style="font-family:arial;font-size:12px;font-weight:bold;color:black;">' + this.formatSemaforo(parseInt(item.semaforo.toString())) + '</div></td>';
			content += '</tr>';			
			content += '</table>';	
			// cotizacion asociada
			domConstruct.destroy("tblCotizacion"); // dom destroy
			domConstruct.destroy("consultandoCotizacionRow"); // dom destroy
			domConstruct.destroy("consultandoCotizacionLabel"); // dom destroy
			content += '<br>';
			content += '<table id="tblCotizacion" style="width: 100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width: 100%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Cotizaci&oacute;n asociada</label></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td id="consultandoCotizacionRow" align="center" style="width: 100%;"><label id="consultandoCotizacionLabel" style="font-family:arial;font-size:12px;font-weight:normal;font-style:italic;color:black;">no existe cotizaci&oacute;n asociada</label></td>';		
			content += '</tr>';				
			content += '</table>';			
			if (this.isValidId(item.cotizacion)) {
				var criterio = {};
				criterio.id = item.cotizacion.toString();
				this.searchCotizaciones(criterio, 1, lang.hitch(this, function(cotizaciones) {
					if (cotizaciones.length > 0) {
						domConstruct.destroy("consultandoCotizacionRow"); // dom destroy
						domConstruct.destroy("consultandoCotizacionLabel"); // dom destroy
						
						var cotizacion = cotizaciones[0];
						var viewCotizacionLinkId = "viewCotizacionLink" + cotizacion.id.toString();
						domConstruct.destroy(viewCotizacionLinkId); // dom destroy				
						var row = '';
						row += '<tr>';
						row += '<td style="width: 100%;"><a href="#" id="' + viewCotizacionLinkId + '" title="Abrir Cotizaci&oacute;n" style="font-family:arial;font-size:12px;font-weight:normal;color:#14469C;">' + cotizacion.name.toString() + '</a></td>';
						row += '</tr>';
						domConstruct.toDom(row);
						domConstruct.place(row, "tblCotizacion");
						on(dom.byId(viewCotizacionLinkId), "click", lang.hitch(this, function() {
							this.openCotizacionAsociada(cotizacion);
						}));						
					}							
				}))	
			}
			// observaciones
			if ("observaciones" in datos && datos.observaciones != "") {
				content += '<br>';
				content += '<table style="width: 100%;border-collapse: collapse;">';
				content += '<tr>';
				content += '<td style="width:100%;font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Observaciones</td>';
				content += '</tr>';
				content += '</table>';			
				content += '<table style="width: 100%;border-collapse: collapse;">';			
				content += '<tr>';
				content += '<td align="left" style="width: 100%;"><div style="font-family:arial;font-size:12px;font-weight:normal;color:black;">' + datos.observaciones.replace(/\n/g,'<br>') + '</div></td>';
				content += '</tr>';	
				content += '</table>';				
			}			
			// checklist
			content += '<br>';
			content += '<table style="width: 100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width: 50%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Nombre de actividad</label></td>';
			content += '<td align="left" style="width: 20%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">ETA</label></td>';
			content += '<td align="left" style="width: 20%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Completado</label></td>';
			content += '<td align="left" style="width: 10%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:bold;color:#14469C;"></label></td>';			
			content += '</tr>';
			var actividades = ["Llegada a puerto", "Proforma de impuestos", "Solicitud de impuestos a cliente", "Pago de impuestos", "Revalidaci&oacute;n", "Salida de puerto", "Programaci&oacute;n a ferrocarril", "Programaci&oacute;n de entrega a bodega", "Llegada a almac&eacute;n local", "Salida de almac&eacute;n local", "Entrega a cliente", "Demoras"];
			for (var i = 0; i < actividades.length; i++) {
				content += '<tr>';
				content += '<td style="width: 50%;border-top: 1px solid #14469C;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:normal;color:black;">' + actividades[i] + '</label></td>';
				content += '<td align="left" style="width: 20%;border-top: 1px solid #14469C;"><div style="font-family:arial;font-size:12px;font-weight:normal;color:black;">' + this.formatDate(datos.etapas[i.toString()].eta) + '</div></td>';
				content += '<td align="left" style="width: 20%;border-top: 1px solid #14469C;"><div style="font-family:arial;font-size:12px;font-weight:normal;color:black;">' + this.formatDate(datos.etapas[i.toString()].done) + '</div></td>';
				content += '<td align="left" style="width: 10%;border-top: 1px solid #14469C;"><div style="font-family:arial;font-size:12px;font-weight:normal;color:black;">' + this.formatSemaforo(this.getSemaforoEtapa(i, datos.etapas[i.toString()].eta, datos.etapas[i.toString()].done)) + '</div></td>';			
				content += '</tr>';					
			}
			content += '<tr>';
			content += '<td colspan="4" style="width: 100%;border-top: 1px solid #14469C;"><div></div></td>';			
			content += '</tr>';				
			content += '</table>';	
			// documentos	
			domConstruct.destroy("tblDocumentos"); // dom destroy
			domConstruct.destroy("consultandoDocsRow"); // dom destroy
			domConstruct.destroy("consultandoDocsLabel"); // dom destroy
			content += '<br>';
			content += '<table id="tblDocumentos" style="width: 100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width: 40%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Nombre de documento</label></td>';
			content += '<td align="left" style="width: 20%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Creaci&oacute;n</label></td>';
			content += '<td align="left" style="width: 30%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:bold;color:#14469C;">Tipo</label></td>';
			content += '<td align="left" style="width: 10%;"><label style="color:#970303;"></label><label style="font-family:arial;font-size:12px;font-weight:bold;color:#14469C;"></label></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td id="consultandoDocsRow" colspan="4" align="center" style="width: 100%;"><label id="consultandoDocsLabel" style="font-family:arial;font-size:12px;font-weight:normal;font-style:italic;color:black;">consultando documentos asociados...</label></td>';		
			content += '</tr>';				
			content += '</table>';
			var criterio = {};
			criterio.contenedor = item.id.toString();
			this.searchDocumentos(criterio, 0, lang.hitch(this, function(documentos) {
				if (documentos.length > 0) {
					domConstruct.destroy("consultandoDocsRow"); // dom destroy
					domConstruct.destroy("consultandoDocsLabel"); // dom destroy
				} else {
					html.set(dom.byId("consultandoDocsLabel"), "no existen documentos asociados");
				}
				var index = 0;
				array.forEach(documentos, lang.hitch(this, function(documento) {
					var viewDocumentLinkId = "viewDocumentLink_" + index;
					var downloadDocumentIconId = "downloadDocumentIcon_" + index;
					domConstruct.destroy(viewDocumentLinkId); // dom destroy
					domConstruct.destroy(downloadDocumentIconId); // dom destroy
					var row = '';
					row += '<tr>';
					row += '<td style="width: 40%;"><a href="#" id="' + viewDocumentLinkId + '" title="Ver Documento" style="font-family:arial;font-size:12px;font-weight:normal;color:#14469C;">' + documento.name.toString() + '</a></td>';
					row += '<td align="left" style="width: 20%;"><div style="font-family:arial;font-size:12px;font-weight:normal;color:black;">' + this.formatDate(documento.datecreated.toString()) + '</div></td>';
					row += '<td align="left" style="width: 30%;"><div style="font-family:arial;font-size:12px;font-weight:normal;color:black;">' + this.formatTipoDocumento(parseInt(documento.tipo.toString())) + '</div></td>';
					row += '<td align="left" style="width: 10%;"><div id="' + downloadDocumentIconId + '" title="Descargar Documento" class="downloadIcon" style="width:15px;height:15px;"></div></td>';
					row += '</tr>';
					domConstruct.toDom(row);
					domConstruct.place(row, "tblDocumentos");
					on(dom.byId(viewDocumentLinkId), "click", lang.hitch(this, function() {
						this.onDocumentView(documento.id.toString());
					}));
					on(dom.byId(downloadDocumentIconId), "click", lang.hitch(this, function() {
						this.onDocumentDownload(documento.id.toString());
					}));					
					index++;
				}));				
			}))			
			
			html.set(this.detalleDiv, content);
		},
		
		onDocumentView: function(docId) {
			// show p8 document
			this.showDocument(docId);
		},	
		
		onDocumentDownload: function(docId) {
			// localiza el documento editable asociado al documento seleccionado, en caso de no exisitir considera el documento fuente
			this.fetchDocumentoEditable(docId, lang.hitch(this, function(documento) {
				this.downloadDocument(documento.id);
			}))				
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
		
		searchDocumentos: function(criterio, maxResults, callback) {			
			var params = {};
			params.method = "searchDocumentos";
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
								callback(response.documentos);			
						})
					}); 		
		},
		
		fetchDocumentoEditable: function(docId, callback) {	
			var params = {};
			params.method = "fetchDocumentoEditable";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.documento = docId;
			
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
								callback(response.documento);																										
						})
					}); 			
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
		
		getSemaforoEtapa: function(index, eta, completado) {
			var semaforo = 0; // verde por defecto
			var key = index.toString();
			// si exite fecha de ETA definida
			if (eta != null) {
				// si no existe fecha de completado definida
				if (completado == null) {
					var dateCurrent = new Date(this.getStringDateValue(new Date(), 0));
					var dateETA = new Date(this.getFormatedDateValue(eta));
					var dateETAPrevio = lang.clone(dateETA);
					var diasPrevio = parseInt(this.settings.eta.previo[key]);
					dateETAPrevio.setDate(dateETAPrevio.getDate() - diasPrevio);
					if (this.daysDiff(dateCurrent, dateETA) < 0) // si la fecha de ETA es menor a la actual se marca como alerta roja
						semaforo = 2; // rojo
					else if (this.daysDiff(dateCurrent, dateETAPrevio) < 0) // si la fecha de ETA es menor a la actual mas dias de alerta previa se marca como alerta ambar
						semaforo = 1; // ambar
				}
			}
			return semaforo;
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
		
		getFilteringSelectItem: function(obj) {
			var item = null;
			var value = obj.get('value');
			if (value != "")
				var item = obj.store.get(value);
			return item;
		},
		
		formatEstado: function(num) {
			if (num == null)
				return "";
			switch(num) {
				case 0:
					return "EN TRASLADO MARITIMO";
					break;
				case 1:
					return "EN PUERTO";
					break;				
				case 2:
					return "EN TRASLADO TERRESTRE";
					break;
				case 3:
					return "EN ALMACEN LOCAL";
					break;			
				case 4:
					return "EN ENTREGA A CLIENTE";
					break;
				case 5:
					return "EN RETORNO DE VACIO";
					break;
				case 99:
					return "CONCLUIDO";
					break;						
			}			
		},
		
	    formatTipoDocumento: function(num) {
			if (num == null)
				return "";
			switch(num) {
				case 0:
					return "Bill of Lading";
					break;
				case 1:
					return "Factura Comercial";
					break;
				case 2:
					return "Lista de Empaque";
					break;					
				case 3:
					return "Pedimento";
					break;
				case 4:
					return "Carta Técnica";
					break;			
				case 5:
					return "Contrato a Proveedor";
					break;			
				case 6:
					return "Notificación de Arribo";
					break;			
				case 7:
					return "Documentación a Ferrocarril";
					break;			
				case 8:
					return "EIR";
					break;
				case 99:
					return "Otro";
					break;							
			}			
		},	 		
		
		formatSemaforo: function(num) {
			if (num == null)
				return "";
			switch(num) {
				case 0:
					return "<div class='alertGreenIcon' style='width:15px;height:15px;'></div>";
					break;
				case 1:
					return "<div class='alertAmberIcon' style='width:15px;height:15px;'></div>";
					break;				
				case 2:
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

		getStringDateValue: function(dVal, tipo) {
			var formatedDate;
			switch (tipo) {
				case 0: // convert to: yyyy-MM-dd
					formatedDate = dVal.getFullYear() + "-" + this.lpad((dVal.getMonth() + 1).toString(), 2, '0') + "-" + this.lpad(dVal.getDate().toString(), 2, '0');
					break;
				case 1: // convert to: dd/MM/yyyy
					formatedDate = this.lpad(dVal.getDate().toString(), 2, '0') + "/" + this.lpad((dVal.getMonth() + 1).toString(), 2, '0') + "/" + dVal.getFullYear();
					break;				
			}
			return formatedDate;	
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
	    		return "";
	    	return strVal.substring(8,10) + "/" + strVal.substring(5,7) + "/" + strVal.substring(0,4);  	
	    },
	    
		daysDiff: function(date1, date2) {
			var timeDiff = date2.getTime() - date1.getTime();
			var diffDays = Math.ceil(timeDiff / (1000 * 3600 * 24)); 
			return diffDays;			
		},	
		
		lpad: function(val, len, padding) {
	       	while (val.length < len)
	       		val = padding + val;
	       	return val;
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