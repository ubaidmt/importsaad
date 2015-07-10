define([
		"dojo/_base/declare",
		"dojo/_base/lang",	
		"dijit/registry",
		"dojo/dom",
		"dojo/dom-construct",
		"dojo/dom-class",
		"dojo/on",
		"dojo/keys",
		"dojo/_base/array",	
		"dojo/html",
		"dojo/json",	
		"dojo/request/xhr",		
		"dojo/dom-style",
		"dojo/dom-geometry",
    	"ecm/model/Request",	
    	"ecm/widget/FilteringSelect",
    	"ecm/widget/dialog/YesNoCancelDialog",
    	"ecm/widget/dialog/LoginDialog",
    	"importSaadPluginDojo/dialog/CntBusquedaCotizacionesDialog",
    	"importSaadPluginDojo/dialog/CntFraccionesCotizadasDialog",
    	"dojox/grid/EnhancedGrid",
    	"dojox/grid/enhanced/plugins/Pagination",
    	"dojo/store/Memory",
    	"dojo/data/ItemFileWriteStore", 
    	"ecm/model/Desktop",
		"ecm/widget/dialog/BaseDialog",	
		"dojo/text!./templates/CntContenedorDialog.html"
	],
	function(declare, 
			lang, 
			registry,
			dom,
			domConstruct,
			domClass,
			on, 
			keys, 
			array, 
			html, 
			json,
			xhr,
			domStyle, 
			domGeom, 
			Request, 
			FilteringSelect,
			YesNoCancelDialog,
			LoginDialog,
			CntBusquedaCotizacionesDialog,
			CntFraccionesCotizadasDialog,
			EnhancedGrid, 
			Pagination, 
			Memory,
			ItemFileWriteStore, 
			Desktop,
			BaseDialog, 
			template) {
	/**
	 * @name importSaadPluginDojo.dialog.CntContenedorDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments ecm.widget.dialog.BaseDialog
	 */
	return declare("importSaadPluginDojo.dialog.CntContenedorDialog", [ BaseDialog ], {
	/** @lends importSaadPluginDojo.dialog.CntContenedorDialog.prototype */	

		contentString: template,
		widgetsInTemplate: true,
		title: null,
		numEtapas: 12,
		updated: false,
		cotizacionasociada: null,
		opencotizacionasociada: false,
		updateETADateHandlers: [],
		updateDoneDateHandlers: [],

		postCreate: function() {
			this.inherited(arguments);
			this.setResizable(false);
			this.setMaximized(true);
			//this.setSize(700, 620);
			this.setTitle(this.title);
			this.refreshButton = this.addButton("Refrescar", "onRefresh", false, false);
			this.saveButton = this.addButton("Guardar", "onBeforeSave", false, true);
			this.cancelButton.set("label", "Cerrar");
		},				
		
		show: function() {		
			this.inherited(arguments);
			this.loadClientes();
			this.loadNavieras();
			this.loadForwarders();
			this.loadProveedores();
			this.loadImportadoras();
			this.loadPuertosLlegada();
			this.loadPuertosSalida();
			this.initEvents();
			this.loadContenedor();
		},
		
		hide: function() {			
			if (this.grid != null)
				this.grid.destroy();
			for (var i = 0; i < this.numEtapas; i++) {
				domConstruct.destroy("lockETA_" + i);
				domConstruct.destroy("delETA_" + i);
				domConstruct.destroy("delDone_" + i);
				domConstruct.destroy("semaforoETA_" + i);
				if (registry.byId("dpETA_" + i) != null)
					registry.byId("dpETA_" + i).destroy();
				if (registry.byId("dpDone_" + i) != null) 
					registry.byId("dpDone_" + i).destroy();
			}			
			this.inherited(arguments);		
		},
		
		initEvents: function() {	
			this.tabContainer.watch("selectedChildWidget", lang.hitch(this, function(name, oldTab, newTab) {
				if (newTab.title == "Documentos") {
					this.initGridDocumentos();
					this.loadDocumentos();
					var size = domGeom.getContentBox(this.gridArea.domNode);
					this.grid.resize({ w: size.w, h: size.h });
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
			for (var i = 0; i < this.numEtapas; i++) {
				on(dom.byId("lockETA_" + i), "click", function() {
					var res = this.id.split("_");
					var index = parseInt(res[1]);					
					var locked = domClass.contains(this, "lockIcon");
					registry.byId("dpETA_" + index).set("disabled", !locked);
					domClass.replace(this, locked ? "unlockIcon" : "lockIcon", locked ? "lockIcon" : "unlockIcon");
				});				
				on(dom.byId("delETA_" + i), "click", function() {
					var res = this.id.split("_");
					var index = parseInt(res[1]);					
					if (registry.byId("dpETA_" + index).get("disabled") == false)
						registry.byId("dpETA_" + index).reset();
				});
				on(dom.byId("delDone_" + i), "click", function() {
					var res = this.id.split("_");
					var index = parseInt(res[1]);					
					registry.byId("dpDone_" + index).reset();
				});				
			}
			// init date handlers
			this.updateDateHandlers(0);
		},
		
		updateDateHandlers: function(action) {
			switch (action) {
				case 0: // init
					this.updateETADateHandlers["fechaBase"] = on.pausable(this.fechaBase, "change", lang.hitch(this, function() {
						if (this.contenedorId == null) // no aplica cuando el contenedor no ha sido creado
							return;						
						this.onUpdateCheckListDates("base_-1");
					}));										
					this.updateETADateHandlers["dpETA_0"] = on.pausable(registry.byId("dpETA_0"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpETA_0");
					}));
					this.updateDoneDateHandlers["dpDone_0"] = on.pausable(registry.byId("dpDone_0"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpDone_0");
					}));
					this.updateETADateHandlers["dpETA_1"] = on.pausable(registry.byId("dpETA_1"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpETA_1");
					}));
					this.updateDoneDateHandlers["dpDone_1"] = on.pausable(registry.byId("dpDone_1"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpDone_1");
					}));	
					this.updateETADateHandlers["dpETA_2"] = on.pausable(registry.byId("dpETA_2"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpETA_02");
					}));
					this.updateDoneDateHandlers["dpDone_2"] = on.pausable(registry.byId("dpDone_2"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpDone_2");
					}));	
					this.updateETADateHandlers["dpETA_3"] = on.pausable(registry.byId("dpETA_3"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpETA_3");
					}));
					this.updateDoneDateHandlers["dpDone_3"] = on.pausable(registry.byId("dpDone_3"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpDone_3");
					}));	
					this.updateETADateHandlers["dpETA_4"] = on.pausable(registry.byId("dpETA_4"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpETA_4");
					}));
					this.updateDoneDateHandlers["dpDone_4"] = on.pausable(registry.byId("dpDone_4"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpDone_4");
					}));	
					this.updateETADateHandlers["dpETA_5"] = on.pausable(registry.byId("dpETA_5"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpETA_5");
					}));
					this.updateDoneDateHandlers["dpDone_5"] = on.pausable(registry.byId("dpDone_5"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpDone_5");
					}));	
					this.updateETADateHandlers["dpETA_6"] = on.pausable(registry.byId("dpETA_6"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpETA_6");
					}));
					this.updateDoneDateHandlers["dpDone_6"] = on.pausable(registry.byId("dpDone_6"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpDone_6");
					}));	
					this.updateETADateHandlers["dpETA_7"] = on.pausable(registry.byId("dpETA_7"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpETA_7");
					}));
					this.updateDoneDateHandlers["dpDone_7"] = on.pausable(registry.byId("dpDone_7"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpDone_7");
					}));	
					this.updateETADateHandlers["dpETA_8"] = on.pausable(registry.byId("dpETA_8"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpETA_8");
					}));
					this.updateDoneDateHandlers["dpDone_8"] = on.pausable(registry.byId("dpDone_8"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpDone_8");
					}));	
					this.updateETADateHandlers["dpETA_9"] = on.pausable(registry.byId("dpETA_9"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpETA_9");
					}));
					this.updateDoneDateHandlers["dpDone_9"] = on.pausable(registry.byId("dpDone_9"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpDone_9");
					}));	
					this.updateETADateHandlers["dpETA_10"] = on.pausable(registry.byId("dpETA_10"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpETA_10");
					}));
					this.updateDoneDateHandlers["dpDone_10"] = on.pausable(registry.byId("dpDone_10"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpDone_10");
					}));	
					this.updateETADateHandlers["dpETA_11"] = on.pausable(registry.byId("dpETA_11"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpETA_11");
					}));
					this.updateDoneDateHandlers["dpDone_11"] = on.pausable(registry.byId("dpDone_11"), "change", lang.hitch(this, function() {
						this.onUpdateCheckListDates("dpDone_11");
					}));						
					break;
				case 1: // pause
					if ("fechaBase" in this.updateETADateHandlers) this.updateETADateHandlers["fechaBase"].pause();
					for (var i = 0; i < this.numEtapas; i++) {
						if ("dpETA_" + i in this.updateETADateHandlers) this.updateETADateHandlers["dpETA_" + i].pause();
						if ("dpDone_" + i in this.updateDoneDateHandlers) this.updateDoneDateHandlers["dpDone_" + i].pause();
					}	
					break;
				case 2: // resume
					for (var i = 0; i < this.numEtapas; i++) {
						if ("dpETA_" + i in this.updateETADateHandlers) this.updateETADateHandlers["dpETA_" + i].resume();
						if ("dpDone_" + i in this.updateDoneDateHandlers) this.updateDoneDateHandlers["dpDone_" + i].resume();
					}	
					break;
			}
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
		
		setSettings: function(settings) {
			this.settings = settings;
		},		
		
		setClientesStore: function(store) {
			this.clientesStore = store;
		},
		
		setNavierasStore: function(store) {
			this.navierasStore = store;
		},
		
		setForwardersStore: function(store) {
			this.forwardersStore = store;
		},
		
		setProveedoresStore: function(store) {
			this.proveedoresStore = store;
		},	
		
		setImportadorasStore: function(store) {
			this.importadorasStore = store;
		},	
		
		setPuertosStore: function(store) {
			this.puertosStore = store;
		},			
		
		setContenedorId: function(contenedorId) {
			this.contenedorId = contenedorId;
		},
		
		initGridDocumentos: function() {
			if (this.grid != null)
				return;
			
			var gridStore = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});				
			
			var gridLayout = [
			    {name: 'Nombre', field: 'name', width: '45%', editable: false, noresize: true},
			    {name: 'Creación', field: 'datecreated', width: '15%', editable: false, formatter: this.formatDate, noresize: true},
				{name: 'Tipo', field: 'tipo', width: '40%', editable: false, formatter: this.formatTipoDocumento, noresize: true}
			];							
			
			this.grid = new EnhancedGrid({
				store: gridStore,
				structure: gridLayout,
				selectionMode: 'multi',
				sortInfo: 1, // asc by name
				autoHeight: false,
				formatterScope: this,
				queryOptions: {ignoreCase: true},
				singleClickEdit: false,
		        plugins: {
		            pagination: {
		                pageSizes: ["10","25","50"],
		                defaultPageSize: "10",
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
		
		loadContenedor: function() {
			if (this.contenedorId == null)
				return;
			
			// enable additional tabs
			this.datosAdicionalesTab.set("disabled", false);
			this.checklistTab.set("disabled", false);
			this.documentosTab.set("disabled", false);
			
			// search contenedor
			var criterio = {};
			criterio.id = this.contenedorId;

			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Cargando datos del contenedor...");
			domStyle.set(this.progressBar, "display", "block");			
			
			this.searchContenedores(criterio, 1, lang.hitch(this, function(contenedores) {
				this.contenedor = contenedores[0];
				// datos generales
				this.numContenedor.setValue(this.contenedor.name);
				this.fechaBase.setValue(this.contenedor.fechabase);
				this.pedimento.setValue(this.contenedor.pedimento);
				this.clientes.setValue(this.contenedor.clienteId);
				if (this.contenedor.navieraId != null) this.navieras.setValue(this.contenedor.navieraId);
				if (this.contenedor.forwarderId != null) this.forwarders.setValue(this.contenedor.forwarderId);
				html.set(this.etapa, this.formatEstado(this.contenedor.estado));
				html.set(this.fechaCreacion, this.formatDate(this.contenedor.datecreated));
				this.mercancia.setValue(this.contenedor.mercancia);
		  		if (this.contenedor.proveedor != null) this.proveedores.setValue(this.contenedor.proveedor);
		  		if (this.contenedor.importadora != null) this.importadoras.setValue(this.contenedor.importadora);
		  		if (this.contenedor.puertollegada != null) this.puertosllegada.setValue(this.contenedor.puertollegada);
		  		if (this.contenedor.puertosalida != null) this.puertossalida.setValue(this.contenedor.puertosalida);
				this.loadSemaforoContenedor(this.contenedor.semaforo);
				this.loadCotizacionAsociada(this.contenedor.cotizacion);
				// get data
				var datos = json.parse(this.contenedor.datos);
				// observaciones
				if ("observaciones" in datos) this.observaciones.setValue(datos.observaciones);
				// pause date handlers
				this.updateDateHandlers(1);
	  			// load fechas etapas
		  		for (key in datos.etapas) {
		  			var etapa = lang.clone(datos.etapas[key]);
		  			registry.byId("dpETA_" + key).setValue(etapa.eta);
		  			registry.byId("dpETA_" + key).set("disabled", etapa.lock);
		  			domClass.replace(dom.byId("lockETA_" + key), etapa.lock ? "lockIcon" : "unlockIcon", etapa.lock ? "unlockIcon" : "lockIcon");
		  			registry.byId("dpDone_" + key).setValue(etapa.done);
		  			this.updateSemaforoEtapa(parseInt(key));		  			
		  		}
		  		if ("buque" in datos) this.buque.setValue(datos.buque);
		  		if ("incoterm" in datos) this.incoterm.setValue(datos.incoterm);
		  		if ("numerofactura" in datos) this.numeroFactura.setValue(datos.numerofactura);
		  		if ("fechafactura" in datos) this.fechaFactura.setValue(datos.fechafactura);
				html.set(this.message, "");
				domStyle.set(this.progressBar, "display", "none");
		  		// resume date handlers (1-second timeout to let date pickers update to be completed)
		  		setTimeout(lang.hitch(this, function() {
		  			this.updateDateHandlers(2);
				}), 1000);  				
			}))		
		},
		
		loadSemaforoContenedor: function(semaforo) {
			switch (semaforo) {			
				case 0: // verde
					domClass.replace(this.semaforoContenedor, "alertGreenIcon", "alertAmberIcon alertRedIcon");
					break;
				case 1: // ambar
					domClass.replace(this.semaforoContenedor, "alertAmberIcon", "alertGreenIcon alertRedIcon");
					break;
				case 2: // rojo
					domClass.replace(this.semaforoContenedor, "alertRedIcon", "alertGreenIcon alertAmberIcon");
					break;
			}			
		},
		
		getEstadoContenedor: function() {
			// identifica el indice de la primera actividad completada de abajo hacia arriba
			var index = -1;
			for (var i = (this.numEtapas - 1); i >= 0; i--) {
				key = i.toString();
				if (registry.byId("dpDone_" + key).isValid() && registry.byId("dpDone_" + key).getValue() != "") {
					index = i;
					break;
				}
			}
			if (index >= 11)
				return 99; // concluido			
			else if (index == 10)
				return 5; // en retorno de vacio			
			else if (index == 9)
				return 4; // en entreg a cliente			
			else if (index == 8)
				return 3; // en almacen local			
			else if (index >= 5 && index <= 7)
				return 2; // en traslado terrestre
			else if (index >= 0 && index <= 4)
				return 1; // en puerto
			else
				return 0; // en traslado maritimo		
		},
		
		loadClientes: function() {
			
			if (this.clientes != null)
				return;
		
			this.clientes = new FilteringSelect({
				value: "",
				autoComplete: true,
				pageSize: 30,
				style: "width: 250px;",
				required: true,
		        store: this.clientesStore,
		        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
		    });
			
			this.clientes.placeAt(this.clientesDiv);			
			this.clientes.startup();	
			this.clientes.reset();	
		},
		
		loadNavieras: function() {
			
			if (this.navieras != null)
				return;
		
			this.navieras = new FilteringSelect({
				value: "",
				autoComplete: true,
				pageSize: 30,
				style: "width: 250px;",
				required: false,
		        store: this.navierasStore,
		        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
		    });
			
			this.navieras.placeAt(this.navierasDiv);			
			this.navieras.startup();	
			this.navieras.reset();
		},	
		
		loadForwarders: function() {
			
			if (this.forwarders != null)
				return;
		
			this.forwarders = new FilteringSelect({
				value: "",
				autoComplete: true,
				pageSize: 30,
				style: "width: 250px;",
				required: false,
		        store: this.forwardersStore,
		        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
		    });
			
			this.forwarders.placeAt(this.forwardersDiv);			
			this.forwarders.startup();	
			this.forwarders.reset();	
		},
		
		loadProveedores: function() {
			
			if (this.proveedores != null)
				return;
		
			this.proveedores = new FilteringSelect({
				value: "",
				autoComplete: true,
				pageSize: 30,
				style: "width: 300px;",
				required: false,
		        store: this.proveedoresStore,
		        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
		    });
			
			this.proveedores.placeAt(this.proveedoresDiv);			
			this.proveedores.startup();	
			this.proveedores.reset();
		},	
		
		loadImportadoras: function() {
			
			if (this.importadoras != null)
				return;
		
			this.importadoras = new FilteringSelect({
				value: "",
				autoComplete: true,
				pageSize: 30,
				style: "width: 300px;",
				required: false,
		        store: this.importadorasStore,
		        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
		    });
			
			this.importadoras.placeAt(this.importadorasDiv);			
			this.importadoras.startup();	
			this.importadoras.reset();
		},	
		
		loadPuertosLlegada: function() {
			
			if (this.puertosllegada != null)
				return;
		
			this.puertosllegada = new FilteringSelect({
				value: "",
				autoComplete: true,
				pageSize: 30,
				style: "width: 300px;",
				required: false,
		        store: this.puertosStore,
		        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
		    });
			
			this.puertosllegada.placeAt(this.puertosLlegadaDiv);			
			this.puertosllegada.startup();	
			this.puertosllegada.reset();
		},	
		
		loadPuertosSalida: function() {
			
			if (this.puertossalida != null)
				return;
		
			this.puertossalida = new FilteringSelect({
				value: "",
				autoComplete: true,
				pageSize: 30,
				style: "width: 300px;",
				required: false,
		        store: this.puertosStore,
		        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
		    });
			
			this.puertossalida.placeAt(this.puertosSalidaDiv);			
			this.puertossalida.startup();	
			this.puertossalida.reset();
		},			
		
		onUpdateCheckListDates: function(source) {
			 // pause date handlers para evitar actualizacion en casada de manera automatica
			this.updateDateHandlers(1);	
			this.updateCheckListDate(source);					
		},
		
		updateCheckListDate: function(source) {			
			var res = source.split("_");
			var sourceindex = parseInt(res[1]);
			// se actualiza un nivel abajo del indice origen y el evento sobre los ETAs desata la actualizacion en cascada			
			var targetindex = sourceindex + 1;
			
			// actualiza semaforo etapa (indice distinto a fecha base)
  			if (sourceindex != -1)
  				this.updateSemaforoEtapa(sourceindex);			
			
			// en caso de llegar al numero maximo de etapas, se completa actualizacion en cascada
			if (targetindex >= this.numEtapas) {
				this.onEtapasETAUpdateCompleted();		
				return;
			}
			
  			var key = targetindex.toString();
  			var baseETA = parseInt(this.settings.eta.base[key]);
  			var diasETA = parseInt(this.settings.eta.dias[key]);
  			// en caso de tener activado el calculo de ETA y no encontrarse bloqueado el campo, se realiza el calculo de ETA
  			if (diasETA != -1 && registry.byId("dpETA_" + key).get("disabled") == false) {
  				var baseETADate = null;
  				if (baseETA == -1) {
  					if (this.fechaBase.isValid())
  						baseETADate = this.getDateObject(this.getFormatedDateValue(this.fechaBase.getValue())); // fecha base de contenedor
  					else
  						baseETADate = this.getDateObject(lang.clone(this.contenedor.fechabase)); // fecha base de contenedor
  				} else {
  					if (registry.byId("dpDone_" + baseETA).isValid() && registry.byId("dpDone_" + baseETA).getValue() != "") // fecha de completado
  						baseETADate = this.getDateObject(registry.byId("dpDone_" + baseETA).getValue());
  					else if (registry.byId("dpETA_" + baseETA).isValid() && registry.byId("dpETA_" + baseETA).getValue() != "") // fecha  predefinida
  						baseETADate = this.getDateObject(registry.byId("dpETA_" + baseETA).getValue());
  				}
  				// calcula ETA en caso de existir fecha base
  				if (baseETADate != null) {
  					// dias adicionales por naviera
  					var naviera = this.getFilteringSelectItem(this.navieras);  					
  					if (naviera != null && "etaDiasAdicionales_" + key in naviera)
  						diasETA += naviera["etaDiasAdicionales_" + key];
  					// set ETA
  					baseETADate.setDate(baseETADate.getDate() + diasETA);
  					registry.byId("dpETA_" + key).setValue(this.getStringDateValue(baseETADate, 0));
  				}
  			}

  			// se lanza trigger del siguiente indice para continuar con la actualizacion en cascada 
			this.updateCheckListDate(res[0] + "_" + targetindex);  				  				
		},
		
		onEtapasETAUpdateCompleted: function() {
			this.updateSemaforoContenedor();
  			this.updateEstadoContenedor();
	  		// resume date handlers (1-second timeout to let date pickers update to be completed)
	  		setTimeout(lang.hitch(this, function() {
	  			this.updateDateHandlers(2);
			}), 1000);
		},
		
		updateEstadoContenedor: function() {
			var estado = this.getEstadoContenedor();
			html.set(this.etapa, this.formatEstado(estado));
		},
		
		updateSemaforoEtapa: function(index) {
			var key = index.toString();
			var dpETA = registry.byId("dpETA_" + key);
			var dpCompletado = registry.byId("dpDone_" + key);
			var semaforoDom = dom.byId("semaforoETA_" + key);
			
			// default to verde
			domClass.replace(semaforoDom, "alertGreenIcon", "alertAmberIcon alertRedIcon");
		
			// si exite fecha de ETA definida
			if (dpETA.isValid() && dpETA.getValue() != "") {
				// si no existe fecha de completado definida
				if (!dpCompletado.isValid() || dpCompletado.getValue() == "") {
					var dateCurrent = this.getDateObject(this.getStringDateValue(new Date(), 0));
					var dateETA = this.getDateObject(this.getFormatedDateValue(dpETA.getValue()));	
					var dateETAPrevio = lang.clone(dateETA);
					var diasPrevio = parseInt(this.settings.eta.previo[key]);
					dateETAPrevio.setDate(dateETAPrevio.getDate() - diasPrevio);
					if (this.daysDiff(dateCurrent, dateETA) < 0) // si la fecha de ETA es menor a la actual se marca como alerta roja
						domClass.replace(semaforoDom, "alertRedIcon", "alertGreenIcon alertAmberIcon");
					else if (this.daysDiff(dateCurrent, dateETAPrevio) < 0) // si la fecha de ETA es menor a la actual mas dias de alerta previa se marca como alerta ambar
						domClass.replace(semaforoDom, "alertAmberIcon", "alertGreenIcon alertRedIcon");
				}
			}
			
			// semaforo del contenedor
			var numRojos = 0;
			var numAmbars = 0;
			for (var i = 0; i < this.numEtapas; i++) {
				key = i.toString();
				semaforoDom = dom.byId("semaforoETA_" + key);
				if (domClass.contains(semaforoDom, "alertRedIcon")) { // si existe al menos una alerta roja, el semaforo del contenedor se marca roja
					numRojos++;
					break;
				} else if (domClass.contains(semaforoDom, "alertAmberIcon")) { // si no existen alertas rojas y existe al menos una alerta ambar, el semaforo del contenedor se marca ambar
					numAmbars++;
				}
			}
			if (numRojos > 0)
				domClass.replace(this.semaforoContenedor, "alertRedIcon", "alertGreenIcon alertAmberIcon");
			else if (numAmbars > 0)
				domClass.replace(this.semaforoContenedor, "alertAmberIcon", "alertGreenIcon alertRedIcon");
			else
				domClass.replace(this.semaforoContenedor, "alertGreenIcon", "alertAmberIcon alertRedIcon");			
		},
		
		updateSemaforoContenedor: function() {			
			// semaforo del contenedor
			var numRojos = 0;
			var numAmbars = 0;
			for (var i = 0; i < this.numEtapas; i++) {
				var key = i.toString();
				var semaforoDom = dom.byId("semaforoETA_" + key);
				if (domClass.contains(semaforoDom, "alertRedIcon")) { // si existe al menos una alerta roja, el semaforo del contenedor se marca roja
					numRojos++;
					break;
				} else if (domClass.contains(semaforoDom, "alertAmberIcon")) { // si no existen alertas rojas y existe al menos una alerta ambar, el semaforo del contenedor se marca ambar
					numAmbars++;
				}
			}
			if (numRojos > 0)
				domClass.replace(this.semaforoContenedor, "alertRedIcon", "alertGreenIcon alertAmberIcon");
			else if (numAmbars > 0)
				domClass.replace(this.semaforoContenedor, "alertAmberIcon", "alertGreenIcon alertRedIcon");
			else
				domClass.replace(this.semaforoContenedor, "alertGreenIcon", "alertAmberIcon alertRedIcon");			
		},
		
		loadCotizacionAsociada: function(cotizacionId) {
			this.cotizacionReasociarButton.set("disabled", false);
			if (cotizacionId == null || cotizacionId.toString() == "")
				return;
			var criterio = {};
			criterio.id = cotizacionId.toString();
			this.searchCotizaciones(criterio, 1, lang.hitch(this, function(cotizaciones) {  
				if (cotizaciones.length == 0)
					return;
				this.setCotizacionAsociada(cotizaciones[0]);
			}))					
		},
		
		setCotizacionAsociada: function(cotizacion) {
			html.set(this.cotizacionReferencia, "");
			if (cotizacion == null)
				return;
			this.cotizacionasociada = cotizacion;
			var viewCotizacionLinkId = "viewCotizacionDialogLink" + cotizacion.id.toString();
			domConstruct.destroy(viewCotizacionLinkId); // dom destroy
			html.set(this.cotizacionReferencia, '<a href="#" id="' + viewCotizacionLinkId + '" title="Abrir Cotización" style="font-family:arial;font-size:12px;font-weight:normal;color:#14469C;">' + cotizacion.name.toString() + '</a>');
			on(dom.byId(viewCotizacionLinkId), "click", lang.hitch(this, function() {
				this.updated = false; // avoid contenedor details refresh
				this.opencotizacionasociada = true;
				this.hide();
			}));			
		},		
		
		onCotizacionReasociar: function() {
			var dialog = new CntBusquedaCotizacionesDialog({
				title: "Consulta de Cotizaciones",
				onConfirm: lang.hitch(this, function() {
					var items = dialog.grid.selection.getSelected();
					dialog.hide();
					if (items.length > 0) {
						var item = items[items.length-1]; // select last
						this.setCotizacionAsociada(item);
					}
				})
			});
			dialog.setConfig(this.config);
			dialog.setContenedor(this.contenedor);
			dialog.show();			
		},
		
		validateFileAddButton: function() {
			this.addFileButton.set("disabled", !this.isFileValid());
		},
		
		loadDocumentos: function() {
			
			if (this.grid == null || this.contenedor.id == null)
				return;
			
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Cargando documentos asociados al contenedor...");
			domStyle.set(this.progressBar, "display", "block");
			
			var criterio = {};
			criterio.contenedor = this.contenedorId; 
			
			this.searchDocumentos(criterio, 0, lang.hitch(this, function(documentos) {
				
				// Clear grid selection
				this.clearGridSelection();					
			
				var store = new ItemFileWriteStore({
					data: {
						identifier: "id",
						items: documentos
					}
				});										
				
				this.grid.setStore(store);
				this.grid.firstPage();			
						
				html.set(this.message, "");
				domStyle.set(this.progressBar, "display", "none");					
			}))				
		},
		
		onDocumentCreate: function() {
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");
			
			var tipo = parseInt(this.tipoDocumento.getValue());
			switch (tipo) {
				case 1: // factura comercial
					this.generaFacturaComercial(tipo);
					break;
				default: // documento no soportado
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "Documento no soportado para generación");
					domStyle.set(this.progressBar, "display", "none");										
					break;					
			}
		},
		
		generaFacturaComercial: function(tipo) {
			var datos = json.parse(this.contenedor.datos);
			// validar prerrequisitos paga generacion de factura comercial
			var fields = [];
			if (this.contenedor.proveedor == null) fields.push("Proveedor");
			if (this.contenedor.importadora == null) fields.push("Importadora");
			if (!("buque" in datos) || datos.buque == null) fields.push("Datos del Buque");
			if (this.contenedor.puertollegada == null) fields.push("Puerto de Llegada");
			if (this.contenedor.puertosalida == null) fields.push("Puerto de Salida");
			if (!("incoterm" in datos) || datos.incoterm == null) fields.push("Incoterm");
			if (!("numerofactura" in datos) || datos.numerofactura == null) fields.push("Número de Factura");
			if (!("fechafactura" in datos) || datos.fechafactura == null) fields.push("Fecha de Factura");
			if (this.contenedor.proveedor != null) {
				var proveedor = this.proveedores.store.get(this.contenedor.proveedor);
				if (proveedor["plantilla_" + tipo] == "") fields.push("Plantilla de Factura Comercial");
			}
			var criterio = null;
			if (this.cotizacionasociada != null) {
				criterio = {};
				criterio.nameequals = this.cotizacionasociada.name;
				criterio.tipo = 1; // cotizacion final
			}
			this.searchCotizaciones(criterio, 1, lang.hitch(this, function(cotizaciones) {  
				if (cotizaciones.length == 0) fields.push("Cotización Final Asociada");
				if (fields.length > 0) {
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "Datos faltantes: " + fields.toString());
					domStyle.set(this.progressBar, "display", "none");
					return;
				}							
				var cotizacion = cotizaciones[0];
				var proveedor = this.proveedores.store.get(this.contenedor.proveedor);
				var importadora = this.importadoras.store.get(this.contenedor.importadora);
				var puertollegada = this.puertosllegada.store.get(this.contenedor.puertollegada);
				var puertosalida = this.puertossalida.store.get(this.contenedor.puertosalida);
				var dialog = new CntFraccionesCotizadasDialog({
					title: "Selección de Fracciones",
					onConfirm: lang.hitch(this, function() {
						var items = dialog.grid.selection.getSelected();
						dialog.hide();
						var fracciones = [];
						array.forEach(items, lang.hitch(this, function(item) {
							fracciones.push(item.fraccion.toString());
						}));
						var claseimpl = "com.importsaad.jasper.gc.FraccionesCotizadas";
						var plantilla = "gc/facturas/" + proveedor["plantilla_" + tipo];
						var params = "Proveedor_Nombre=" + this.escapeJasperParameter(proveedor.name) + "|Proveedor_Direccion= " + this.escapeJasperParameter(proveedor.direccion) + "|Proveedor_Telefono= " + this.escapeJasperParameter(proveedor.telefono) + "|Proveedor_Fax= " + this.escapeJasperParameter(proveedor.fax) + "|Proveedor_TaxID= " + this.escapeJasperParameter(proveedor.taxID) + "|Proveedor_Telefono= " + this.escapeJasperParameter(proveedor.telefono) + "|Proveedor_Fax= " + this.escapeJasperParameter(proveedor.fax) + "|Numero_Factura= " + this.escapeJasperParameter(datos.numerofactura) + "|Fecha_Factura= " + this.getShortMonth(this.getDateObject(datos.fechafactura).getMonth()) + ". " + this.getDateObject(datos.fechafactura).getDate() + ", " + this.getDateObject(datos.fechafactura).getFullYear() + "|Importadora_Nombre=" + this.escapeJasperParameter(importadora.name) + "|Importadora_Direccion=" + this.escapeJasperParameter(importadora.direccion) + "|Importadora_RFC=" + this.escapeJasperParameter(importadora.rfc) + "|Importadora_Telefono=" + this.escapeJasperParameter(importadora.telefono) + "|Importadora_Email=" + this.escapeJasperParameter(importadora.email) + "|Buque_Nombre=" + this.escapeJasperParameter(datos.buque) + "|Puerto_Salida=" + this.escapeJasperParameter(puertosalida.name) + "|Puerto_Llegada=" + this.escapeJasperParameter(puertollegada.name) + "|Referencia=" + this.escapeJasperParameter(this.contenedor.name.toString()) + "|Incoterm=" + datos.incoterm;
						var condiciones = "Id=" + cotizacion.id.toString() + "|Fracciones=" + fracciones.toString();
						var archivo = "Factura Comercial";
						// genera documento pdf
						this.generaDocumento(claseimpl, plantilla, params, condiciones, archivo, 0, lang.hitch(this, function(docId) {
							if (this.isValidId(docId)) {
								this.asociaDocumento(this.contenedor, docId, tipo, null, lang.hitch(this, function(pdf) {
									// genera documento editable en word
									this.generaDocumento(claseimpl, plantilla, params, condiciones, archivo, 2, lang.hitch(this, function(docId) {
										if (this.isValidId(docId)) {
											this.asociaDocumento(this.contenedor, docId, tipo, pdf.id, lang.hitch(this, function(word) {
												// incluye documento pdf a grid de documentos
												this.grid.store.newItem(pdf);
												this.grid.store.save();
												this.updated = true; // contenedor modificado														
											}))												
										}
									}))													
								}))									
							}
						}))										
					})
				});
				dialog.setConfig(this.config);
				dialog.setCotizacion(cotizacion);
				dialog.show();	
			}))					
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
		
		asociaDocumento: function(contenedor, docId, tipo, pdfId, callback) {	
			var params = {};
			params.method = "asociaDocumento";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.contenedor = contenedor.id.toString();
			params.documento = docId;
			params.tipo = tipo;
			params.pdf = pdfId;
			
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
		
		generaDocumento: function(classeimpl, plantilla, params, condiciones, archivo, salida, callback) {
			// request a enviar
			var serviceRequest = "/excelecm-rest-broker-common/jaxrs/reportsmodule/getReporteComplejo";				
			serviceRequest += "?tipoSalida=" + salida;
			serviceRequest += "&claseImpl=" + classeimpl;
			serviceRequest += "&nombrePlantilla=" + plantilla;
			serviceRequest += "&params=" + params;
			serviceRequest += "&condiciones=" + condiciones;
			serviceRequest += "&nombreArchivo=" + this.escapeJasperParameter(archivo);
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
	    
		isValidId: function(val) {
			if (val == null) 
				return false;
		    var pattern = new RegExp("^({)([A-Z0-9]{8})(-)([A-Z0-9]{4})(-)([A-Z0-9]{4})(-)([A-Z0-9]{4})(-)([A-Z0-9]{12})(})$");
			return pattern.test(val);
		},		
		
		onDocumentAdd: function() {
			
			// valida que no exista el mismo tipo de documento asociado al contenedor (excepto para tipo de documento "Otro")
			var criterio = {};
			criterio.contenedor = this.contenedor.id;
			criterio.tipo = parseInt(this.tipoDocumento.getValue()) != 99 ? this.tipoDocumento.getValue() : -1;
			
			this.searchDocumentos(criterio, 1, lang.hitch(this, function(documentos) {
				/*
				if (documentos.length > 0) {
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "El documento " + this.tipoDocumento.attr("displayedValue") + " ya se encuentra asociado al contenedor");
					domStyle.set(this.progressBar, "display", "none");
					return;
				}
				*/
				
				domStyle.set(this.message, "color", "#000253");
				html.set(this.message, "Agregando nuevo documento...");
				domStyle.set(this.progressBar, "display", "block");						
							
				var callback = lang.hitch(this, this._onFileAddCompleted);
				
				var params = {};
				params.method = "addDocumento";
				params.repositoryid = ecm.model.desktop.defaultRepositoryId;
				params.context = json.stringify(this.config.context);
				params.tipo = this.tipoDocumento.getValue();
				params.contenedor = this.contenedor.id;
				
				// HTML5 browser
				if (this._fileInput.files) {
					var file = this._fileInput.files[0];
					params.mimetype = file.type;
					params.parm_part_filename = (file.fileName ? file.fileName : file.name)
					//params.max_file_size = this._maxFileSize.toString();

					var form = new FormData();
					form.append("file", file);
					
					Request.postFormToPluginService("ImportSaadPlugin", "CntContenedoresService", form, {
						requestParams: params,
						requestCompleteCallback: callback
					});	
					
				} else { // Non-HTML5 browser
					var fileName = this._fileInput.value;
					if (fileName && fileName.length > 0) {
						var i = fileName.lastIndexOf("\\");
						if (i != -1) {
							fileName = fileName.substr(i + 1);
						}
					}
					params.parm_part_filename = fileName;
					//params.max_file_size = this._maxFileSize.toString();
					
					// MIME type is not available, must be determined at the server.
					params.plugin = "ImportSaadPlugin";
					params.action = "CntContenedoresService";
					
					Request.ieFileUploadServiceAPI("plugin", "", {requestParams: params, 
						requestCompleteCallback: callback
					}, this._fileInputForm);
				}				
			}))				
		},
		
		_onFileAddCompleted: function(response) {
			
			this._fileInput.value = "";
			
			if (response.error != null) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, response.error);
				domStyle.set(this.progressBar, "display", "none");																	
				return;				
			}
			
			this.grid.store.newItem(response.documento);
			this.grid.store.save();
						
			this.validateFileAddButton();
			this.updated = true; // contenedor modificado
			
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");			
		
		},
		
		onDocumentUpdate: function() {
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar un documento");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}			
			var item = items[items.length-1]; // select last
			
			// valida que no exista el mismo tipo de documento asociado al contenedor (excepto para tipo de documento "Otro")
			var criterio = {};
			criterio.contenedor = this.contenedor.id;
			criterio.tipo = parseInt(this.tipoDocumento.getValue()) != 99 ? this.tipoDocumento.getValue() : -1;
			
			this.searchDocumentos(criterio, 1, lang.hitch(this, function(documentos) {
				/*
				if (documentos.length > 0 && documentos[0].id.toString() != item.id.toString()) {
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "El documento " + this.tipoDocumento.attr("displayedValue") + " ya se encuentra asociado al contenedor");
					domStyle.set(this.progressBar, "display", "none");
					return;
				}
				*/
				
				domStyle.set(this.message, "color", "#000253");
				html.set(this.message, "Actualizando documento...");
				domStyle.set(this.progressBar, "display", "block");	
				
				var documento = {};
				documento.id = item.id.toString();
				documento.tipo = parseInt(this.tipoDocumento.getValue());
				
				var params = {};
				params.method = "updateDocumento";
				params.repositoryid = ecm.model.desktop.defaultRepositoryId;
				params.context = json.stringify(this.config.context);
				params.documento = json.stringify(documento);
				
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
								
								// update store
								this.grid.store.setValue(item, "tipo", response.documento.tipo);
								this.updated = true; // contenedor modificado
								
								domStyle.set(this.message, "color", "#000253");
								html.set(this.message, "El documento seleccionado ha sido actualizado");
								domStyle.set(this.progressBar, "display", "none");									
								
							})
						}); 								
				
			}))					
		},
		
		onDocumentDelete: function() {
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar por lo menos un documento");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}
			
			var confirmDialog = new YesNoCancelDialog({
				title: "Eliminar Documentos",
				text: "¿Está seguro de eliminar los <b>" + items.length + "</b> documentos seleccionados?",
				onYes: lang.hitch(this, function() {
					confirmDialog.hide();
					
					domStyle.set(this.message, "color", "#000253");
					html.set(this.message, "Eliminando los documentos seleccionadas...");
					domStyle.set(this.progressBar, "display", "block");
					
					// Clear grid selection
					this.clearGridSelection();	
					
					var documentos = [];
					array.forEach(items, lang.hitch(this, function(item) {							
						var doc = {"id": item.id.toString()};
						documentos.push(doc);
					}));					

					var params = {};
					params.method = "deleteDocumento";
					params.repositoryid = ecm.model.desktop.defaultRepositoryId;
					params.context = json.stringify(this.config.context);
					params.documentos = json.stringify(documentos);
					
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
									array.forEach(items, lang.hitch(this, function(item) {
										this.grid.store.fetchItemByIdentity({ 
											identity: item.id.toString(),
											onItem: lang.hitch(this, function(currentitem) {
												if (currentitem != null) {
													this.grid.store.deleteItem(item);
													this.grid.store.save();
												}
											})
										});											
									}));	
									this.updated = true; // contenedor modificado
									
									domStyle.set(this.message, "color", "#000253");
									html.set(this.message, "Los <b>" + items.length + "</b> documentos seleccionados han sido eliminados");
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
		
		onDocumentView: function() {
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar un documento");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}			
			var item = items[items.length-1]; // select	
			
			// show p8 document
			this.showDocument(item.id.toString());			
		},
		
		onDocumentDownload: function() {
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar un documento");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}			
			var item = items[items.length-1]; // select	
			
			// localiza el documento editable asociado al documento seleccionado, en caso de no exisitir considera el documento fuente
			this.fetchDocumentoEditable(item.id.toString(), lang.hitch(this, function(documento) {
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
		
		isFileValid: function() {
			var valid = this._fileInput;
			// This test works for both HTML5 and non-HTML5 browsers. 
			valid = valid && (this._fileInput.value) && (this._fileInput.value.length > 0);
			return valid;
		},			
		
		onGridItemSelect: function() {
			var items = this.grid.selection.getSelected();
			if (items.length > 0) {
				var item = items[items.length-1]; // select last
				this.tipoDocumento.setValue(item.tipo.toString());
				this.updateDocumentButton.set("disabled", false);
				this.deleteDocumentButton.set("disabled", false);
				this.viewDocumentButton.set("disabled", false);
				this.downloadDocumentButton.set("disabled", false);
			}		
		},
		
		onClean: function() {
			this.clearGridSelection();
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");			
		},	
		
		onRefresh: function() {
			this.resetValues();
			this.loadContenedor();
			this.loadDocumentos();
		},		
		
		onBeforeSave: function() {
			// validacion de campos
			if (!this.fechaBase.isValid() || !this.numContenedor.isValid() || !this.pedimento.isValid() || !this.clientes.isValid() || !this.navieras.isValid() || !this.forwarders.isValid() || !this.mercancia.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Datos generales del contenedor inválidos.");
				domStyle.set(this.progressBar, "display", "none");				
				return;					
			}
			
			// get contenedor
			var contenedor = this.getDatosContenedor();
			var cliente = this.getFilteringSelectItem(this.clientes);			
			
			if (contenedor.id == null) // crea nuevo contenedor 
			{
				// valida que el numero de contenedor no exista en el mismo nivel
				var criterio = {};
				criterio.exactname = contenedor.name;
				criterio.infolder = "/Importaciones/" + cliente.name + "/Contenedores/" + new Date().getFullYear() + "/" + this.getMes(new Date().getMonth());
				this.searchContenedores(criterio, 1, lang.hitch(this, function(contenedores) {
					if (contenedores.length > 0) {
						domStyle.set(this.message, "color", "#000253");
						html.set(this.message, "Ya existe el número de contenedor " + contenedor.name + " asignado al cliente " + cliente.name + " en el mismo mes de alta del contenedor");
						domStyle.set(this.progressBar, "display", "none");	
						return;
					}
					
					// add new contenedor
					this.saveContenedor(contenedor, 0);					
					
				}))						
			} 
			else // actualiza contenedor 
			{
				// valida que el numero de contenedor no exista en el mismo nivel
				var criterio = {};
				criterio.exactname = contenedor.name;
				criterio.infolder = "/Importaciones/" + cliente.name + "/Contenedores/" + this.getDateObject(contenedor.datecreated).getFullYear() + "/" + this.getMes(this.getDateObject(contenedor.datecreated).getMonth());
				this.searchContenedores(criterio, 1, lang.hitch(this, function(contenedores) {
					if (contenedores.length > 0 && contenedores[0].id.toString() != contenedor.id.toString()) {
						domStyle.set(this.message, "color", "#000253");
						html.set(this.message, "Ya existe el número de contenedor " + contenedor.name + " asignado al cliente " + cliente.name + " en el mismo mes de alta del contenedor");
						domStyle.set(this.progressBar, "display", "none");	
						return;
					}	
					
					// update contenedor
					this.saveContenedor(contenedor, 1);						
					
				}))															
			}
		},
		
		saveContenedor: function(contenedor, action) {
			switch (action) {
				case 0: // add new contenedor
					domStyle.set(this.message, "color", "#000253");
					html.set(this.message, "Creando nuevo contenedor...");
					domStyle.set(this.progressBar, "display", "block");					
					
					this.addContenedor(contenedor, lang.hitch(this, function(contenedor) {
						// switch a modo de actualizacion
						this.contenedor = contenedor;
						this.contenedorId = this.contenedor.id;
						this.setTitle("Actualización de Contenedor");
						// enable additional tabs
						this.datosAdicionalesTab.set("disabled", false);
						this.checklistTab.set("disabled", false);
						this.documentosTab.set("disabled", false);
						// set datos generales del contenedor
						html.set(this.etapa, this.formatEstado(this.contenedor.estado));
						html.set(this.fechaCreacion, this.formatDate(this.contenedor.datecreated));
						this.loadSemaforoContenedor(this.contenedor.semaforo);
						this.loadCotizacionAsociada(this.contenedor.cotizacion);
						this.updated = true; // contenedor modificado

						domStyle.set(this.message, "color", "#000253");
						html.set(this.message, "El nuevo contenedor ha sido creado");
						domStyle.set(this.progressBar, "display", "none");											
					}))						
					break;
					
				case 1: // update contenedor
					domStyle.set(this.message, "color", "#000253");
					html.set(this.message, "Actualizando datos del contenedor...");
					domStyle.set(this.progressBar, "display", "block");
					
					this.updateContenedor(contenedor, lang.hitch(this, function(contenedor) {
						this.contenedor = contenedor;
						this.updated = true; // contenedor modificado
						domStyle.set(this.message, "color", "#000253");
						html.set(this.message, "Los datos del contenedor han sido actualizados");
						domStyle.set(this.progressBar, "display", "none");	
					}))						
					break;					
			}
		},
		
		getDatosContenedor: function() {
			var contenedor = {};
			// datos generales
			var cliente = this.getFilteringSelectItem(this.clientes);
			var naviera = this.getFilteringSelectItem(this.navieras);
			var forwarder = this.getFilteringSelectItem(this.forwarders);
			contenedor.id = this.contenedor != null ? this.contenedor.id : null;
			contenedor.name = this.numContenedor.getValue();
			contenedor.fechabase = this.getFormatedDateValue(this.fechaBase.getValue());
			contenedor.datecreated = this.contenedor != null ? this.contenedor.datecreated : null;
			contenedor.pedimento = this.pedimento.getValue() != "" ? this.pedimento.getValue() : null;
			contenedor.cliente = cliente.id;
			contenedor.naviera = naviera != null ? naviera.id : null;
			contenedor.forwarder = forwarder != null ? forwarder.id : null;
			contenedor.mercancia = this.mercancia.getValue();
			contenedor.observaciones = this.observaciones.getValue();
			// estado
			contenedor.estado = this.getEstadoContenedor();
			// semaforo
			var semaforo = 0; // default to verde
			if (domClass.contains(this.semaforoContenedor, "alertRedIcon"))
				semaforo = 2; // rojo
			else if (domClass.contains(this.semaforoContenedor, "alertAmberIcon"))
				semaforo = 1; // ambar			
			contenedor.semaforo = semaforo; 
			// etapas
			contenedor.etapas = {};
			for (var i = 0; i < this.numEtapas; i++) {
				var key = i.toString(); // la llave es el id de la etapa para mejor localizacion
				var value = {};
				var dpETA = registry.byId("dpETA_" + i);
				value.lock = dpETA.get("disabled");
				value.eta = dpETA.isValid() && dpETA.getValue() != "" ? this.getFormatedDateValue(dpETA.getValue()) : null;
				var dpDone = registry.byId("dpDone_" + i);
				value.done = dpDone.isValid() && dpDone.getValue() != "" ? this.getFormatedDateValue(dpDone.getValue()) : null;
				contenedor.etapas[key] = value;
			}
			// cotizacion asociada
			contenedor.cotizacion = this.cotizacionasociada != null ? this.cotizacionasociada.id.toString() : null;
			// datos adicionales
			var proveedor = this.getFilteringSelectItem(this.proveedores);
			var importadora = this.getFilteringSelectItem(this.importadoras);
			var puertollegada = this.getFilteringSelectItem(this.puertosllegada);
			var puertosalida = this.getFilteringSelectItem(this.puertossalida);
			contenedor.proveedor = proveedor != null ? proveedor.id : null;
			contenedor.importadora = importadora != null ? importadora.id : null;
			contenedor.puertollegada = puertollegada != null ? puertollegada.id : null;
			contenedor.puertosalida = puertosalida != null ? puertosalida.id : null;
			contenedor.buque = this.buque.getValue() != "" ? this.buque.getValue() : null;
			contenedor.incoterm = this.incoterm.getValue();
			contenedor.numerofactura = this.numeroFactura.getValue() != "" ? this.numeroFactura.getValue() : null;
			contenedor.fechafactura = this.fechaFactura.isValid() && this.fechaFactura.getValue() != "" ? this.getFormatedDateValue(this.fechaFactura.getValue()) : null;
			return contenedor;
		},
		
		addContenedor: function(contenedor, callback) {
			var params = {};
			params.method = "addContenedor";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.contenedor = json.stringify(contenedor);
			
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
								callback(response.contenedor);																														
						})
					}); 				
		},
		
		updateContenedor: function(contenedor, callback) {
			var params = {};
			params.method = "updateContenedor";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.contenedor = json.stringify(contenedor);
			
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
								callback(response.contenedor);																														
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
		
		searchCotizaciones: function(criterio, maxResults, callback) {
			
			if (criterio == null) {
				if (lang.isFunction(callback))
					callback([]);
				return;
			}
			
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
		
		resetValues: function() {
			this.numContenedor.reset();
			this.fechaBase.reset();
			this.pedimento.reset();
			if (this.clientes != null) this.clientes.reset();
			if (this.navieras != null) this.navieras.reset();
			if (this.forwarders != null) this.forwarders.reset();
			html.set(this.etapa, "");
			html.set(this.fechaCreacion, "");
			this.mercancia.reset();
			if (this.proveedores != null) this.proveedores.reset();
			if (this.importadoras != null) this.importadoras.reset();
			if (this.puertosllegada != null) this.puertosllegada.reset();
			if (this.puertossalida != null) this.puertossalida.reset();
	  		html.set(this.cotizacionReferencia, "");
			this.observaciones.reset();
	  		this.buque.reset();
	  		this.incoterm.reset();
	  		this.numeroFactura.reset();
	  		this.fechaFactura.reset();			
			this.clearGridSelection();
			this.clearGridData(this.grid);
			this.filtro.reset();
			this._fileInput.value = "";
			this.validateFileAddButton();
			this.tipoDocumento.reset();
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");						
		},
		
		clearGridSelection: function() {
			if (this.grid != null) this.grid.selection.clear();
			this.updateDocumentButton.set("disabled", true);
			this.deleteDocumentButton.set("disabled", true);
			this.viewDocumentButton.set("disabled", true);
			this.downloadDocumentButton.set("disabled", true);
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
		
		daysDiff: function(date1, date2) {
			var timeDiff = date2.getTime() - date1.getTime();
			var diffDays = Math.ceil(timeDiff / (1000 * 3600 * 24)); 
			return diffDays;			
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
		
		getDateObject: function(dVal) {
			// convert from "yyyy-mm-dd" to date object
	  		var date = new Date(
	  			parseInt(dVal.substring(0,4)),
	  			parseInt(dVal.substring(5,7)) - 1,
	  			parseInt(dVal.substring(8,10)),
	  		    0, 0, 0);
	  		return date;
		},
		
	    formatDate: function(dVal) {
	    	// expected: yyyy-MM-dd
	    	// convert to: dd/MM/yyyy
	    	if (dVal == null || dVal.length == 0)
	    		return dVal;
	    	return dVal.substring(8,10) + "/" + dVal.substring(5,7) + "/" + dVal.substring(0,4);  	
	    },
	    
	    getMes: function(index) {
	    	var meses = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"];
	    	return meses[index];
	    },
	    
	    getShortMonth: function(index) {
	    	var months = ["JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"];
	    	return months[index];
	    },	    
	    
		lpad: function(val, len, padding) {
	       	while (val.length < len)
	       		val = padding + val;
	       	return val;
	   	},	
	   	
	    parseFloat: function(str) {
	    	var strVal = str.replace(/[,$]/g,''); 
	    	return parseFloat(strVal);
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

		formatCurrency: function(num) {
		    var p = num.toFixed(2).split(".");
		    return p[0].split("").reverse().reduce(function(acc, num, i, orig) {
		        return  num + (i && !(i % 3) ? "," : "") + acc;
		    }, "") + "." + p[1];
		},	
		
		getFilteringSelectItem: function(obj, value) {
			var item = null;
			if (value != null)
				item = obj.store.get(value);
			return item;
		},	
		
		getFilteringSelectItem: function(obj) {
			var item = null;
			var value = obj.get('value');
			if (value != "")
				var item = obj.store.get(value);
			return item;
		},
		
		isNumber: function(value) {
		    if ((undefined === value) || (null === value) || ('' === value))
		        return false;
		    if (typeof value == 'number')
		        return true;
		    return !isNaN(value - 0);
		}		
			
	});
});
