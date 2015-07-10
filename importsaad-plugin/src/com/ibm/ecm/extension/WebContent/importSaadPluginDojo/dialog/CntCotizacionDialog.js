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
    	"dojox/grid/EnhancedGrid",
    	"dojox/grid/enhanced/plugins/Pagination",
    	"dojo/store/Memory",
    	"dojo/data/ItemFileWriteStore", 
    	"ecm/widget/dialog/YesNoCancelDialog",
    	"importSaadPluginDojo/dialog/CntBusquedaFraccionesDialog",
    	"ecm/model/Desktop",
		"ecm/widget/dialog/BaseDialog",	
		"dojo/text!./templates/CntCotizacionDialog.html"
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
			EnhancedGrid, 
			Pagination, 
			Memory,
			ItemFileWriteStore, 
			YesNoCancelDialog,
			CntBusquedaFraccionesDialog,
			Desktop,
			BaseDialog, 
			template) {
	/**
	 * @name importSaadPluginDojo.dialog.CntCotizacionDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments ecm.widget.dialog.BaseDialog
	 */
	return declare("importSaadPluginDojo.dialog.CntCotizacionDialog", [ BaseDialog ], {
	/** @lends importSaadPluginDojo.dialog.CntCotizacionDialog.prototype */	

		contentString: template,
		widgetsInTemplate: true,
		title: null,
		isCopy: false,
		contenedorasociado: null,

		postCreate: function() {
			this.inherited(arguments);
			this.setResizable(false);
			this.setMaximized(true);
			//this.setSize(900, 780);
			this.setTitle(this.title);
			this.refreshButton = this.addButton("Refrescar", "onRefresh", false, false);
			this.saveButton = this.addButton("Guardar", "onBeforeSave", false, true);
			this.cancelButton.set("label", "Cerrar");
		},				
		
		show: function() {		
			this.inherited(arguments);
			this.initGrid();
			this.setDefaultSettings();
			this.loadClientes();
			this.loadCotizacion();
			this.initEvents();
		},
		
		hide: function() {
			if (this.grid != null)
				this.grid.destroy();
			registry.byId("medidaMetros").destroy();
			registry.byId("medidaPulgadas").destroy();			
			this.inherited(arguments);	
		},
		
		initEvents: function() {
			on(this.searchIcon, "click", lang.hitch(this, function() {
				this.onBuscaFraccion();
			}));
			on(this.aumento, "keyup,change", lang.hitch(this, function() {
				this.updateDatosFraccion();
			}));
			on(this.ancho, "keyup,change", lang.hitch(this, function() {
				this.updateDatosFraccion();
			}));
			on(this.medida, "change", lang.hitch(this, function() {
				this.updateDatosFraccion();
			}));
			on(this.ajuste, "keyup,change", lang.hitch(this, function() {
				this.updateDatosFraccion();
			}));				
			on(this.cantidad, "keyup,change", lang.hitch(this, function() {
				this.updateDatosFraccion();
			}));
			on(this.flete, "keyup,change", lang.hitch(this, function() {
				this.updateTotalCotizacion();
			}));
			on(this.tipoCambio, "keyup,change", lang.hitch(this, function() {
				this.updateTotalCotizacion();
			}));	
			on(this.incremento, "keyup,change", lang.hitch(this, function() {
				this.updateTotalCotizacion();
			}));		
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
		
		setCotizacion: function(cotizacion) {
			this.cotizacion = cotizacion;
		},
		
		setIsCopy: function(isCopy) {
			this.isCopy = isCopy;
		},
		
		initGrid: function() {
			
			var gridStore = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});				
			
			var gridLayout = [
				{name: 'Fracción', field: 'fraccion', width: '15%', editable: false, noresize: true},
				{name: 'Precio Mínimo (USD)', field: 'precioMinimo', width: '15%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true},
				{name: 'Aumento (USD)', field: 'aumento', width: '15%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true},
				{name: 'Ancho', field: 'ancho', width: '15%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true},
				{name: 'Precio Unitario (USD)', field: 'precioUnitario', width: '15%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true},
				{name: 'Cantidad', field: 'cantidad', width: '15%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true},
				{name: 'Total (USD)', field: 'total', width: '15%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true}
			];							
			
			this.grid = new EnhancedGrid({
				store: gridStore,
				structure: gridLayout,
				selectionMode: 'multi',
				sortInfo: 1, // asc by fraccion
				autoHeight: false,
				formatterScope: this,
				queryOptions: {ignoreCase: true}							
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
		
		loadCotizacion: function() {
			// si es una nueva cotizacion
			if (this.cotizacion == null) {
				this.loadReferencia();
				return;
			}
			
			// get datos
			var datos = json.parse(this.cotizacion.datos.toString());
			// datos generales
			if ("tipoCambio" in datos) this.tipoCambio.setValue(datos.tipoCambio.toString()); // backward compatibility
			if ("flete" in datos) this.flete.setValue(datos.flete.toString()); // backward compatibility
			if ("incremento" in datos) this.incremento.setValue(datos.incremento.toString()); 
			this.clientes.set("item", this.clientes.store.get(this.cotizacion.clienteId.toString()));
			this.referencia.setValue(datos.name.toString());
			this.contenedor.setValue(datos.contenedor.toString());
			this.mercancia.setValue(datos.mercancia.toString());
			if ("ETA" in datos) this.ETA.setValue(datos.ETA.toString());
			if ("observaciones" in datos) this.observaciones.setValue(datos.observaciones.toString()); // backward compatibility
			// fracciones cotizadas
			array.forEach(datos.fracciones, lang.hitch(this, function(fraccion) {	
				// add new element to grid
				fraccion.id = dojox.uuid.generateRandomUuid();
				if ("totalUSD" in fraccion) fraccion.total = fraccion.totalUSD; // backward compatibility
				this.grid.store.newItem(fraccion);
				this.grid.store.save();
			}));	
			
			// si la cotizacion corresponde a una final (en caso de una copia), se reajustan los valores de la cotizacion orignal
			if (parseInt(this.cotizacion.tipo.toString()) == 1) {
				this.grid.store.fetch({
					onItem: lang.hitch(this, function(item) {
						var cotizacionOriginal = json.parse(item.cotizacionOriginal.toString());
						// Update grid store						
				  		for (key in cotizacionOriginal) {
				  			this.grid.store.setValue(item, key, cotizacionOriginal[key]);				  			
				  		}
					})
				});				
			}
			
			// carga contendor asociado
			if (!this.isCopy)
				this.loadContenedorAsociado(this.cotizacion.contenedorobj);				
			
			// si es una copia, genera nueva referencia y limpia cotizacion para tratarla como una nueva
			if (this.isCopy) {
				this.loadReferencia();
				this.cotizacion = null;	
			}
			
			// set total cotizacion
			this.updateTotalCotizacion();				
		},
		
		loadReferencia: function() {			
			if (this.folio != null) {
				this.referencia.setValue(this.folio);
				return;
			}
			
			var params = {};
			params.method = "getNextFolioCotizacion";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			
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
							
							this.folio = response.folio;
							this.referencia.setValue(this.folio);
						})
					}); 			
		},
		
		loadClientes: function() {
			
			if (this.clientes != null)
				return;
		
			this.clientes = new FilteringSelect({
				value: "",
				autoComplete: true,
				pageSize: 30,
				style: "width: 150px;",
				required: true,
		        store: this.clientesStore,
		        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
		    });
			
			this.clientes.placeAt(this.clientesDiv);			
			this.clientes.startup();	
			this.clientes.set("item", this.clientes.store.get(""));				
		},			
		
		onGridItemSelect: function() {
			var items = this.grid.selection.getSelected();
			if (items.length > 0) {
				var item = items[items.length-1]; // select last
				this.fraccion.setValue(item.fraccion.toString());
				this.unidadComercial = parseInt(item.unidadComercial.toString());
				html.set(this.precioMinimo, "$ " + this.formatCurrency(parseFloat(item.precioMinimo.toString())));
				this.ancho.setValue(item.ancho.toString());
				this.ancho.set("disabled", !(parseInt(item.unidadComercial.toString()) == 2)); // el ancho unicamente aplica para M2
				if ("medida" in item && item.medida.toString() == "in")
					registry.byId("medidaPulgadas").set("checked", true);
				else
					registry.byId("medidaMetros").set("checked", true);	// default
				registry.byId("medidaMetros").set("disabled", !(parseInt(item.unidadComercial.toString()) == 2));
				registry.byId("medidaPulgadas").set("disabled", !(parseInt(item.unidadComercial.toString()) == 2));
				this.aumento.setValue(item.aumento.toString());
				this.cantidad.setValue(item.cantidad.toString());
				if ("ajuste" in item) this.ajuste.setValue(item.ajuste.toString()); // backward compatibility
				if ("observaciones" in item) this.observaciones_fraccion.setValue(item.observaciones.toString()); // backward compatibility
				this.updateDatosFraccion();
			}		
		},
		
		onRefresh: function() {
			this.resetValues();
			this.loadCotizacion();
		},
		
		onAdd: function() {
			// data validation
			if (!this.fraccion.isValid() || !this.aumento.isValid() || !this.ancho.isValid() || !this.cantidad.isValid() || !this.flete.isValid() || !this.tipoCambio.isValid() || !this.ajuste.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Favor de ingresar datos válidos.");
				domStyle.set(this.progressBar, "display", "none");				
				return;					
			}
			
			// get fraccion cotizada
			var fraccion = this.getDatosFraccion();

			// add new element to grid
			fraccion.id = dojox.uuid.generateRandomUuid();
			this.grid.store.newItem(fraccion);
			this.grid.store.save();
			
			// set total cotizacion
			this.updateTotalCotizacion();
			
			// clean datos de fraccion cotizada
			this.onClean();			
		},

		onUpdate: function() {
			
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar una fracción existente en la cotización.");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}
			var item = items[items.length-1]; // select
			
			// data validation
			if (!this.fraccion.isValid() || !this.aumento.isValid() || !this.ancho.isValid() || !this.cantidad.isValid() || !this.flete.isValid() || !this.tipoCambio.isValid() || !this.ajuste.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Favor de ingresar datos válidos.");
				domStyle.set(this.progressBar, "display", "none");				
				return;					
			}			
						
			// get fraccion cotizada
			var fraccion = this.getDatosFraccion();

			// Update grid store						
	  		for (key in fraccion) {
	  			this.grid.store.setValue(item, key, fraccion[key]);
	  		}
			
			// set total cotizacion
			this.updateTotalCotizacion();
			
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");				
		},		
		
		onDelete: function() {
			var items = this.grid.selection.getSelected();
			var removedItems = [];
			array.forEach(items, lang.hitch(this, function(item) {							
				removedItems.push({"id": item.id.toString()});
			}));
			this.removeGridItemsFromStore(removedItems);
			
			// set total cotizacion
			this.updateTotalCotizacion();
			
			// clean datos de fraccion cotizada
			this.onClean();
		},
		
		onClean: function() {
			this.clearGridSelection();
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");			
		},				
		
		onBeforeSave: function() {
			var count = 0;
			this.grid.store.fetch({
				onItem: lang.hitch(this, function() {
					count++;
				})
			});
			if (count == 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Se requiere al menos una fracción cotizada.");
				domStyle.set(this.progressBar, "display", "none");				
				return;					
			}
			var cliente = this.getFilteringSelectItem(this.clientes);
			if (cliente == null || !this.referencia.isValid() || !this.incremento.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Datos inválidos de la cotización general.");
				domStyle.set(this.progressBar, "display", "none");				
				return;				
			}
			
			// valida que no exista un cotizacion con la misma referencia
			var criterio = {};
			criterio.nameequals = this.referencia.getValue();
			criterio.tipo = 0; // original
			this.searchCotizaciones(criterio, 50, lang.hitch(this, function(cotizaciones) {
				var existe = false;
				if (cotizaciones.length > 0 && this.cotizacion == null) { // nueva cotizacion
					existe = true;
				} else if (cotizaciones.length > 0 && this.cotizacion != null) { // actualizacion de cotizacion
					array.some(cotizaciones, lang.hitch(this, function(cotizacion) {
						if (cotizacion.id.toString() != this.cotizacion.id.toString()) {
							existe = true;
							return false; // exit loop
						}
					}));						
				}
				
				if (existe) {
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "Ya existe una cotización con la misma referencia.");
					domStyle.set(this.progressBar, "display", "none");				
					return;							
				}
				
				// save cotizacion
				this.save();

			}))				
		},
		
		save: function() {
			
			// get datos cotizacion
			var cotizacion = this.getDatosCotizacion();
			
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Guardando cotización...");
			domStyle.set(this.progressBar, "display", "block");				
			
			// add cotizacion
			if (this.cotizacion == null) 
			{
				this.addCotizacion(cotizacion, 0, lang.hitch(this, function(cotizacion) {  	
					this.asociaCotizacionPDF(cotizacion, lang.hitch(this, function(cotizacion) {
						// cotizacion final
						var cotizacionfinal = this.getDatosCotizacionFinal(cotizacion);
						this.addCotizacion(cotizacionfinal, 1, lang.hitch(this, function(cotizacionfinal) {
							this.asociaCotizacionPDF(cotizacionfinal, lang.hitch(this, function(cotizacionfinal) {
								this.onAfterSave(cotizacion);
							}))	
						}))	
					}))	
				}))		
			} 
			else // update cotizacion
			{
				// En caso de actualizar la referencia de la cotizacion, eliminar la cotizacion final asociada con la referencia actual
				if (this.cotizacion.name.toString() != cotizacion.name.toString()) {
					var criterio = {};
					criterio.nameequals = this.cotizacion.name.toString();
					criterio.tipo = 1; // cotizacion final
					this.searchCotizaciones(criterio, 1, lang.hitch(this, function(cotizaciones) {												
						this.deleteCotizaciones(cotizaciones);
					}))							
				} 
				
				// cotizacion original
				this.updateCotizacion(cotizacion, 0, lang.hitch(this, function(cotizacion) {  	
					this.asociaCotizacionPDF(cotizacion, lang.hitch(this, function(cotizacion) {
						// cotizacion final
						var cotizacionfinal = this.getDatosCotizacionFinal(cotizacion);							
						// valida si ya existe la cotizacion final asociada
						var criterio = {};
						criterio.nameequals = cotizacionfinal.name;
						criterio.tipo = 1; // cotizacion final
						this.searchCotizaciones(criterio, 1, lang.hitch(this, function(cotizaciones) {
							if (cotizaciones.length == 0) { // crea cotizacion final
								this.addCotizacion(cotizacionfinal, 1, lang.hitch(this, function(cotizacionfinal) {
									this.asociaCotizacionPDF(cotizacionfinal, lang.hitch(this, function(cotizacionfinal) {
										this.onAfterSave(cotizacion);
									}))										
								}))										
							} else { // update cotizacion final
								cotizacionfinal.id = cotizaciones[0].id.toString(); // set cotizacion final id
								this.updateCotizacion(cotizacionfinal, 1, lang.hitch(this, function(cotizacionfinal) {
									this.asociaCotizacionPDF(cotizacionfinal, lang.hitch(this, function(cotizacionfinal) {
										this.onAfterSave(cotizacion);
									}))																			
								}))																			
							}
						}))																				
					}))	
				}))							
			}			
		},
		
		addCotizacion: function(cotizacion, tipo, callback) {
			var params = {};
			params.method = "addCotizacion";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.cotizacion = json.stringify(cotizacion);
			params.tipo = tipo;
			
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
								callback(response.cotizacion);																														
						})
					}); 							
		},
		
		updateCotizacion: function(cotizacion, tipo, callback) {
			var params = {};
			params.method = "updateCotizacion";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.cotizacion = json.stringify(cotizacion);
			params.tipo = tipo;
			
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
								callback(response.cotizacion);								
						})
					}); 				
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
		
		asociaCotizacionPDF: function(cotizacion, callback) {
			this.generaCotizacionReporte(cotizacion, 0, lang.hitch(this, function(reporte) {
				if (this.isValidId(reporte)) {
					// asocia PDF a cotizacion		
					var params = {};
					params.method = "asociaCotizacionPDF";
					params.repositoryid = ecm.model.desktop.defaultRepositoryId;
					params.context = json.stringify(this.config.context);
					params.cotizacion = cotizacion.id;
					params.pdf = reporte;
					
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
										callback(response.cotizacion);																					
								})
							}); 
				}
			}))					
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
	    
		isValidId: function(val) {
		    var pattern = new RegExp("^({)([A-Z0-9]{8})(-)([A-Z0-9]{4})(-)([A-Z0-9]{4})(-)([A-Z0-9]{4})(-)([A-Z0-9]{12})(})$");
			return pattern.test(val);
		},	    
	    
		getDatosCotizacionFinal: function(cotizacion) {
			var datos = json.parse(cotizacion.datos.toString());
			var fracciones = [];
			array.forEach(datos.fracciones, lang.hitch(this, function(fraccion) {
				var item = lang.clone(fraccion);
				var datosCotizacionFinal = json.parse(fraccion.cotizacionFinal.toString());
		  		for (key in datosCotizacionFinal) {
		  			item[key] = datosCotizacionFinal[key]; 
		  		}									
				fracciones.push(item);
			}));
			var cotizacionfinal = lang.clone(datos);
			var datosCotizacionFinal = json.parse(datos.cotizacionFinal.toString());
			for (key in datosCotizacionFinal) {
				cotizacionfinal[key] = datosCotizacionFinal[key];
	  		}	
			// remueve solicitud de creacion de contenedor asociado en caso de existir
			delete cotizacionfinal.contenedorasociado;			
			// set fracciones
			cotizacionfinal.fracciones = fracciones;
			return cotizacionfinal;
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
		
		onSave: function(callback) {
			if (lang.isFunction(callback))
				callback();				
		},

		updateDatosFraccion: function() {
			var preciominimo = this.parseFloat(this.precioMinimo.innerHTML);
			var aumento = 0;
			var ancho = 1;
			var anchoMetros = 1;
			var cantidad = 1;
			var cantidadAjustada = 1;
			
			if (this.aumento.isValid())
				aumento = parseFloat(this.aumento.getValue());
			if (this.ancho.isValid()) {
				ancho = parseFloat(this.ancho.getValue());
				anchoMetros = ancho;
				// si la medida se indica en pulgadas, se realiza la conversion a metros
				if (registry.byId("medidaPulgadas").getValue() == "in")
					anchoMetros = ancho * 0.0254;
			}
			if (this.cantidad.isValid())
				cantidad = parseFloat(this.cantidad.getValue());
			if (this.cantidad.isValid() && this.ajuste.isValid())
				cantidadAjustada = this.getCantidadAjustada(parseFloat(this.cantidad.getValue()), parseFloat(this.ajuste.getValue()));
			 
			// precio unitario
			var preciounitario = (preciominimo + aumento) * anchoMetros;
			preciounitario = Math.round(preciounitario * 100) / 100; // round to two decimals
			// total
			var total = preciounitario * cantidad;
			var total_final = preciounitario * cantidadAjustada;
			
			// set datos
			html.set(this.cantidadAjustada, this.formatCurrency(cantidadAjustada));
			html.set(this.precioUnitario, "$ " + this.formatCurrency(preciounitario));
			html.set(this.total_fraccion, "$ " + this.formatCurrency(total));
			html.set(this.total_fraccion_final, "$ " + this.formatCurrency(total_final));
		},		
		
		updateTotalCotizacion: function() {		
			var incremento = 0;
			var cantidad_original = 0;
			var cantidad_final = 0;
			var total_original = 0;
			var total_final = 0;
						
			if (this.incremento.isValid())
				incremento = parseFloat(this.incremento.getValue());		
			
			// total cotizacion (considera todos las fracciones incluidas en la cotizacion)
			var numfracciones = 0;
			this.grid.store.fetch({
				onItem: lang.hitch(this, function(item) {
					// datos orignales
					cantidad_original += parseFloat(item.cantidad.toString());
					total_original += parseFloat(item.total.toString());
					// datos finales
					if ("cotizacionFinal" in item) { // backward compatibility
						var datos = json.parse(item.cotizacionFinal.toString());
						total_final += parseFloat(datos.total.toString());
						cantidad_final += parseFloat(datos.cantidad.toString());						
					}
					// numero de fracciones
					numfracciones++;
				})
			});		
			
			// set resumen cotizacion original
			var resumen = this.getResumenCotizacion(total_original, numfracciones);
			html.set(this.cantidadtotal, this.formatCurrency(cantidad_original));
			html.set(this.total, "$ " + this.formatCurrency(total_original));
			html.set(this.valorAduanal, "$ " + this.formatCurrency(resumen.valoraduanal));
			html.set(this.IGI, "$ " + this.formatCurrency(resumen.igi));
			html.set(this.DTA, "$ " + this.formatCurrency(resumen.dta));
			html.set(this.IVA, "$ " + this.formatCurrency(resumen.iva));
			html.set(this.PREV, "$ " + this.formatCurrency(resumen.prev));
			html.set(this.CNT, "$ " + this.formatCurrency(resumen.cnt));
			html.set(this.totalImpuestos, "$ " + this.formatCurrency(resumen.totalimpuestos));				
			html.set(this.totalCotizacion, "$ " + this.formatCurrency(resumen.totalimpuestos + incremento));
			
			// set resumen cotizacion final
			var resumen = this.getResumenCotizacion(total_final, numfracciones);
			html.set(this.cantidadtotal_final, this.formatCurrency(cantidad_final));
			html.set(this.total_final, "$ " + this.formatCurrency(total_final));
			html.set(this.valorAduanal_final, "$ " + this.formatCurrency(resumen.valoraduanal));
			html.set(this.IGI_final, "$ " + this.formatCurrency(resumen.igi));
			html.set(this.DTA_final, "$ " + this.formatCurrency(resumen.dta));
			html.set(this.IVA_final, "$ " + this.formatCurrency(resumen.iva));
			html.set(this.PREV_final, "$ " + this.formatCurrency(resumen.prev));
			html.set(this.CNT_final, "$ " + this.formatCurrency(resumen.cnt));
			html.set(this.totalImpuestos_final, "$ " + this.formatCurrency(resumen.totalimpuestos));				
			html.set(this.totalCotizacion_final, "$ " + this.formatCurrency(resumen.totalimpuestos + incremento));
		},	
		
		getResumenCotizacion: function(total, numfracciones) {
			var flete = 0;
			var tipocambio = this.settings.tipoCambio;
			
			if (this.flete.isValid())
				flete = parseFloat(this.flete.getValue());
			if (this.tipoCambio.isValid())
				tipocambio = parseFloat(this.tipoCambio.getValue());			
			
			var resumen = {};
			resumen.total = total;
			resumen.valoraduanal = (resumen.total + flete) * tipocambio;
			resumen.igi = resumen.valoraduanal * this.settings.IGI;
			resumen.dta = resumen.valoraduanal * this.settings.DTA;
			resumen.iva = (resumen.valoraduanal * parseFloat(this.config.pteiva)) + (resumen.igi * parseFloat(this.config.pteiva)) + (resumen.dta * parseFloat(this.config.pteiva));
			resumen.prev = this.settings.PREV * numfracciones;
			resumen.cnt = this.settings.CNT * numfracciones;
			resumen.totalimpuestos = resumen.igi + resumen.dta + resumen.iva + resumen.prev + resumen.cnt;
			
			return resumen;
		},
		
		getCantidadAjustada: function(cantidad, ajuste) {
			var cantidadAjustada = 1;			
			cantidadAjustada = cantidad - ((ajuste * cantidad) / 100);
			return cantidadAjustada;
		},
		
		setDefaultReferencia: function() {
			var referencia = "";
			// por defecto, la referencia de la cotizacion esta compuesta por las fracciones incluidas
			this.grid.store.fetch({
				onItem: lang.hitch(this, function(item) {
					referencia += item.fraccion.toString() + ",";
				})
			});		
			referencia = referencia.length > 0 ? referencia.slice(0, -1) : "";	// remueve ultima coma						
			this.referencia.setValue(referencia);
		},
		
		setDefaultSettings: function() {
			this.tipoCambio.setValue(this.settings.tipoCambio);
			this.ajuste.setValue(this.settings.ajusteCotizacion);
		},		
		
		onBuscaFraccion: function() {
			var dialog = new CntBusquedaFraccionesDialog({
				title: "Consulta de Fracciones",
				onConfirm: lang.hitch(this, function() {
					var items = dialog.grid.selection.getSelected();
					dialog.hide();
					if (items.length > 0) {
						var item = items[items.length-1]; // select last
						this.fraccion.setValue(item.name.toString());
						this.unidadComercial = parseInt(parseInt(item.unidad.toString()));
						html.set(this.precioMinimo, "$ " + this.formatCurrency(parseFloat(item.precio.toString())));
						this.ancho.reset();
						this.ancho.set("disabled", !(this.unidadComercial == 2)); // el ancho unicamente aplica para M2
						registry.byId("medidaMetros").set("checked", true);	// default
						registry.byId("medidaMetros").set("disabled", !(this.unidadComercial == 2));
						registry.byId("medidaPulgadas").set("disabled", !(this.unidadComercial == 2));
						this.updateDatosFraccion();
					}
				})
			});
			dialog.setConfig(this.config);
			dialog.show();			
		},
		
		getDatosFraccion: function() {
			var item = {};
			item.fraccion = this.fraccion.getValue();
			item.unidadComercial = this.unidadComercial;
			item.aumento = parseFloat(this.aumento.getValue());
			item.ancho = parseFloat(this.ancho.getValue());
			item.medida = registry.byId("medidaMetros").getValue() == "m" ? registry.byId("medidaMetros").getValue() : "in";
			item.cantidad = parseFloat(this.cantidad.getValue());
			item.ajuste = parseFloat(this.ajuste.getValue());
			item.precioMinimo = this.parseFloat(this.precioMinimo.innerHTML);
			item.precioUnitario = this.parseFloat(this.precioUnitario.innerHTML);
			item.total = this.parseFloat(this.total_fraccion.innerHTML);
			item.observaciones = this.observaciones_fraccion.getValue();

			// cotizacion final
			var cotizacionFinal = {};
			cotizacionFinal.cantidad = this.parseFloat(this.cantidadAjustada.innerHTML);
			cotizacionFinal.total = this.parseFloat(this.total_fraccion_final.innerHTML);
			item.cotizacionFinal = json.stringify(cotizacionFinal);
			
			// cotizacion original (se guardan los valores que se afectan de la cotizacion orignal para futura recuperacion)
			var cotizacionOriginal = {};
			cotizacionOriginal.cantidad = parseFloat(this.cantidad.getValue());
			cotizacionOriginal.total = this.parseFloat(this.total_fraccion.innerHTML);
			item.cotizacionOriginal = json.stringify(cotizacionOriginal);
			
			return item;
		},
		
		getDatosCotizacion: function() {
			var fracciones = [];
			this.grid.store.fetch({
				onItem: lang.hitch(this, function(item) {
					var fraccion = {};
					fraccion.fraccion = item.fraccion.toString();
					fraccion.unidadComercial = parseInt(item.unidadComercial.toString());
					fraccion.aumento = parseFloat(item.aumento.toString());
					fraccion.ancho = parseFloat(item.ancho.toString());
					fraccion.medida = ("medida" in item) ? item.medida.toString() : "m";			
					fraccion.cantidad = parseFloat(item.cantidad.toString());
					fraccion.ajuste = parseFloat(item.ajuste.toString());
					fraccion.precioMinimo = parseFloat(item.precioMinimo.toString());
					fraccion.precioUnitario = parseFloat(item.precioUnitario.toString());
					fraccion.total = parseFloat(item.total.toString());
					fraccion.observaciones = item.observaciones.toString();
					fraccion.cotizacionFinal = item.cotizacionFinal.toString();
					fraccion.cotizacionOriginal = item.cotizacionOriginal.toString();	
					fracciones.push(fraccion);
				})
			});
			var cliente = this.getFilteringSelectItem(this.clientes);			
			var cotizacion = {};
			cotizacion.flete = parseFloat(this.flete.toString());
			cotizacion.tipoCambio = parseFloat(this.tipoCambio.toString());			
			if (this.cotizacion != null) cotizacion.id = this.cotizacion.id.toString();
			cotizacion.incremento = parseFloat(this.incremento.getValue());
			cotizacion.clienteId = cliente.id.toString();
			cotizacion.name = this.referencia.getValue();
			cotizacion.contenedor = this.contenedor.getValue();
			cotizacion.mercancia = this.mercancia.getValue();
			cotizacion.ETA = this.ETA.getValue();
			cotizacion.observaciones = this.observaciones.getValue();
			cotizacion.cantidad = this.parseFloat(this.cantidadtotal.innerHTML);
			cotizacion.total = this.parseFloat(this.total.innerHTML);
			cotizacion.valorAduanal = this.parseFloat(this.valorAduanal.innerHTML);
			cotizacion.monto = this.parseFloat(this.totalCotizacion.innerHTML);
			var impuestos = {};
			impuestos.IGI = this.parseFloat(this.IGI.innerHTML);
			impuestos.DTA = this.parseFloat(this.DTA.innerHTML);
			impuestos.IVA = this.parseFloat(this.IVA.innerHTML);
			impuestos.PREV = this.parseFloat(this.PREV.innerHTML);
			impuestos.CNT = this.parseFloat(this.CNT.innerHTML);
			impuestos.total = this.parseFloat(this.totalImpuestos.innerHTML);	
			cotizacion.impuestos = impuestos;
			
			// contenedor asociado
			cotizacion.contenedorasociado = this.contenedorasociado;
			
			// fraciones
			cotizacion.fracciones = fracciones;
			
			// cotizacion final
			var cotizacionFinal = {};
			cotizacionFinal.cantidad = this.parseFloat(this.cantidadtotal_final.innerHTML);
			cotizacionFinal.total = this.parseFloat(this.total_final.innerHTML);
			cotizacionFinal.valorAduanal = this.parseFloat(this.valorAduanal_final.innerHTML);
			cotizacionFinal.monto = this.parseFloat(this.totalCotizacion_final.innerHTML);
			var impuestos = {};
			impuestos.IGI = this.parseFloat(this.IGI_final.innerHTML);
			impuestos.DTA = this.parseFloat(this.DTA_final.innerHTML);
			impuestos.IVA = this.parseFloat(this.IVA_final.innerHTML);
			impuestos.PREV = this.parseFloat(this.PREV_final.innerHTML);
			impuestos.CNT = this.parseFloat(this.CNT_final.innerHTML);
			impuestos.total = this.parseFloat(this.totalImpuestos_final.innerHTML);			
			cotizacionFinal.impuestos = impuestos;
			cotizacion.cotizacionFinal = json.stringify(cotizacionFinal);
			
			return cotizacion;
		},	
		
		loadContenedorAsociado: function(contenedorId) {
			this.contenedorasociado = null;
			if (contenedorId == null || contenedorId.toString() == "") {
				this.contenedorCrearButton.set("disabled", false);
				return;
			}
			var criterio = {};
			criterio.id = contenedorId.toString();
			this.searchContenedores(criterio, 1, lang.hitch(this, function(contenedores) {  
				if (contenedores.length == 0) {
					this.contenedorCrearButton.set("disabled", false);
					return;
				}
				this.setContenedorAsociado(contenedores[0]);
			}))					
		},
		
		setContenedorAsociado: function(contenedor) {
			this.contenedorasociado = contenedor.id.toString();
			var viewContenedorLinkId = "viewContenedorDialogLink" + contenedor.id.toString();
			domConstruct.destroy(viewContenedorLinkId); // dom destroy
			html.set(this.contenedorReferencia, '<a href="#" id="' + viewContenedorLinkId + '" title="Abrir Contenedor" style="font-family:arial;font-size:12px;font-weight:normal;color:#14469C;">' + contenedor.name.toString() + '</a>');
			on(dom.byId(viewContenedorLinkId), "click", lang.hitch(this, function() {
				this.onOpenContenedor(contenedor);
			}));			
		},			
		
		onCrearContenedor: function() {
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");	
			
			var cliente = this.getFilteringSelectItem(this.clientes);

			// valida datos de la cotizacion actualmente guardada
			if (cliente == null || this.contenedor.getValue() == "") {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Datos generales del contenedor inválidos.");
				domStyle.set(this.progressBar, "display", "none");				
				return;					
			}
			
			// valida que el numero de contenedor no exista en el mismo nivel
			var criterio = {};
			criterio.exactname = this.contenedor.getValue();
			criterio.infolder = "/Importaciones/" + cliente.name + "/Contenedores/" + new Date().getFullYear() + "/" + this.getMesLetra(new Date().getMonth());
			this.searchContenedores(criterio, 1, lang.hitch(this, function(contenedores) {
				if (contenedores.length > 0) {
					domStyle.set(this.message, "color", "#000253");
					html.set(this.message, "Ya existe el número de contenedor " + this.contenedor.getValue() + " asignado al cliente " + cliente.name + " en el mismo mes de alta del contenedor");
					domStyle.set(this.progressBar, "display", "none");	
					return;
				}
				
				// set nuevo contenedor para ser creado y asociado
				html.set(this.contenedorReferencia, this.contenedor.getValue());
				this.contenedorasociado = this.contenedor.getValue();
				
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
			this.clearGridSelection();
			this.clearGridData(this.grid);
			this.setDefaultSettings();
			if (this.clientes != null) this.clientes.set("item", this.clientes.store.get(""));
			this.incremento.reset();
			this.referencia.reset();
			this.contenedor.reset();
			this.mercancia.reset();
			this.ETA.reset();
			this.observaciones.reset();
			// resumen cotizacion original
			html.set(this.cantidadtotal, "0.00");
			html.set(this.total, "$ 0.00");
			html.set(this.valorAduanal, "$ 0.00");
			html.set(this.IGI, "$ 0.00");
			html.set(this.DTA, "$ 0.00");
			html.set(this.IVA, "$ 0.00");
			html.set(this.PREV, "$ 0.00");
			html.set(this.CNT, "$ 0.00");
			html.set(this.totalImpuestos, "$ 0.00");		
			html.set(this.totalCotizacion, "$ 0.00");
			// resumen cotizacion final
			html.set(this.cantidadtotal_final, "0.00");
			html.set(this.total_final, "$ 0.00");
			html.set(this.valorAduanal_final, "$ 0.00");
			html.set(this.IGI_final, "$ 0.00");
			html.set(this.DTA_final, "$ 0.00");
			html.set(this.IVA_final, "$ 0.00");
			html.set(this.PREV_final, "$ 0.00");
			html.set(this.CNT_final, "$ 0.00");
			html.set(this.totalImpuestos_final, "$ 0.00");		
			html.set(this.totalCotizacion_final, "$ 0.00");	
			html.set(this.contenedorReferencia, "");
			this.contenedorasociado = null;
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");				
		},
		
		clearGridSelection: function() {
			if (this.grid != null) this.grid.selection.clear();	
			this.fraccion.reset();
			this.unidadComercial = null;
			html.set(this.precioMinimo, "$ 0.00");
			html.set(this.precioUnitario, "$ 0.00");
			this.aumento.reset();
			this.ancho.reset();
			this.ancho.set("disabled", true);
			registry.byId("medidaMetros").set("checked", true);
			registry.byId("medidaMetros").set("disabled", true);
			registry.byId("medidaPulgadas").set("disabled", true);
			this.cantidad.reset();
			html.set(this.cantidadAjustada, "0.00");
			html.set(this.total_fraccion, "$ 0.00");
			html.set(this.total_fraccion_final, "$ 0.00");
			this.observaciones_fraccion.reset();
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
		
	    getMesLetra: function(index) {
	    	var meses = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"];
	    	return meses[index];
	    },
		
	    parseFloat: function(str) {
	    	var strVal = str.replace(/[,$]/g,''); 
	    	return parseFloat(strVal);
	    },			

		formatCurrency: function(num) {
		    var p = num.toFixed(2).split(".");
		    return p[0].split("").reverse().reduce(function(acc, num, i, orig) {
		        return  num + (i && !(i % 3) ? "," : "") + acc;
		    }, "") + "." + p[1];
		},		
		
		getFilteringSelectItem: function(obj) {
			var item = null;
			var value = obj.get('value');
			if (value != "")
				var item = obj.store.get(value);
			return item;
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
		}		
			
	});
});
