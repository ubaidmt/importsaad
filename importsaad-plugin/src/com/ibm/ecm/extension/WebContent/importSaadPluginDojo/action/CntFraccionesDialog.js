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
		"dojo/text!./templates/CntFraccionesDialog.html"
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
	 * @name importSaadPluginDojo.action.CntFraccionesDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments ecm.widget.dialog.BaseDialog
	 */
	return declare("importSaadPluginDojo.action.CntFraccionesDialog", [ BaseDialog ], {
	/** @lends importSaadPluginDojo.action.CntFraccionesDialog.prototype */	

		contentString: template,
		widgetsInTemplate: true,	

		postCreate: function() {
			this.inherited(arguments);
			this.setResizable(false);
			this.setMaximized(false);
			this.setSize(700, 800);
			this.setTitle("Administración de Fracciones Aracelarias");
			this.exportButton = this.addButton("Exportar", "onExport", false, false);
			this.deleteButton = this.addButton("Eliminar", "onDelete", false, false);
			this.updateButton = this.addButton("Actualizar", "onUpdate", false, false);
			this.refreshButton = this.addButton("Refrescar", "onRefresh", false, false);
			this.addButton = this.addButton("Agregar", "onAdd", false, true);
			this.cancelButton.set("label", "Cerrar");
		},				
		
		show: function() {		
			this.inherited(arguments);
			this.initGrid();
			this.loadFracciones();
			
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
				{name: 'Fracción', field: 'name', width: '20%', editable: false, noresize: true},
				{name: 'Descripción', field: 'descripcion', width: '50%', editable: false, formatter: this.formatDescripcion, noresize: true},
				{name: 'Unidad', field: 'unidad', width: '15%', editable: false, formatter: this.formatUnidadComercial, noresize: true},
				{name: 'Precio', field: 'precio', width: '15%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true}
			];							
			
			this.grid = new EnhancedGrid({
				store: this.gridStore,
				structure: gridLayout,
				selectionMode: 'multi',
				sortInfo: 1, // asc by fraccion
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
		
		searchFracciones: function(criterio, maxResults, callback) {			
																
			var params = {};
			params.method = "searchFracciones";
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
								callback(response.fracciones);	
														
						})
					}); 		
		},		
		
		loadFracciones: function() {
			
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Consultando fracciones...");
			domStyle.set(this.progressBar, "display", "block");
			
			var criterio = {};
			this.searchFracciones(criterio, 0, lang.hitch(this, function(fracciones) {  			
			
				var store = new ItemFileWriteStore({
					data: {
						identifier: "id",
						items: fracciones
					}
				});										
				
				this.grid.setStore(store);
				this.grid.firstPage();

				if (fracciones.length == 1) { // select row if single result
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
				this.fraccion.setValue(item.name.toString());
				this.descripcion.setValue(item.descripcion.toString());
				this.unidad.setValue(item.unidad.toString());
				this.precio.setValue(item.precio.toString());
			}		
		},
		
		onRefresh: function() {
			this.resetValues();
			this.loadFracciones();
		},
		
		onUpdate: function() {	
			
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar una fracción.");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}
			var item = items[items.length-1]; // select 
			
			if (!this.fraccion.isValid() || !this.precio.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Favor de ingresar datos válidos.");
				domStyle.set(this.progressBar, "display", "none");				
				return;	
			}
			
			// valida que la fraccion a actualizar no exista previamente creada
			var criterio = {};
			criterio.name = this.fraccion.getValue();
			this.searchFracciones(criterio, 1, lang.hitch(this, function(fracciones) {  				
				
				if (fracciones.length > 0 && fracciones[0].id.toString() != item.id.toString()) {
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "La fracción " + this.fraccion.getValue() + " ya existe.");
					domStyle.set(this.progressBar, "display", "none");				
					return;	
				}
				
				domStyle.set(this.message, "color", "#000253");
				html.set(this.message, "Actualizando fracción seleccionada...");
				domStyle.set(this.progressBar, "display", "block");
				
				var params = {};
				params.method = "updateFraccion";
				params.repositoryid = ecm.model.desktop.defaultRepositoryId;
				params.context = json.stringify(this.config.context);
				var fraccion = {"id": item.id.toString(), "name": this.fraccion.getValue(), "descripcion": this.descripcion.getValue(), "unidad": parseInt(this.unidad.getValue()), "precio": parseFloat(this.precio.getValue())};
				params.fraccion = json.stringify(fraccion);
				
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
								this.grid.store.setValue(item, 'name', response.fraccion.name);
								this.grid.store.setValue(item, 'descripcion', response.fraccion.descripcion);
								this.grid.store.setValue(item, 'unidad', response.fraccion.unidad);
								this.grid.store.setValue(item, 'precio', response.fraccion.precio);												
								
								domStyle.set(this.message, "color", "#000253");
								html.set(this.message, "La fracción " + response.fraccion.name + " ha sido actualizada.");
								domStyle.set(this.progressBar, "display", "none");
								
							})
						}); 				
			}))					
		},
		
		onDelete: function() {
			
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Es necesario seleccionar por lo menos una fracción.");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}
						
			var confirmDialog = new YesNoCancelDialog({
				title: "Eliminar Fracciones",
				text: "¿Está seguro de eliminar las <b>" + items.length + "</b> fracciones seleccionadas?",
				onYes: lang.hitch(this, function() {
					confirmDialog.hide();
					
					domStyle.set(this.message, "color", "#000253");
					html.set(this.message, "Eliminando las fracciones seleccionadas...");
					domStyle.set(this.progressBar, "display", "block");
					
					var fracciones = [];
					array.forEach(items, lang.hitch(this, function(item) {							
						var fraccion = {"id": item.id.toString()};
						fracciones.push(fraccion);
					}));					

					var params = {};
					params.method = "deleteFraccion";
					params.repositoryid = ecm.model.desktop.defaultRepositoryId;
					params.context = json.stringify(this.config.context);
					params.fracciones = json.stringify(fracciones);
					
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
									html.set(this.message, "Las <b>" + fracciones.length + "</b> fracciones seleccionadas han sido eliminadas.");
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
			
			if (!this.fraccion.isValid() || !this.precio.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Favor de ingresar datos válidos.");
				domStyle.set(this.progressBar, "display", "none");				
				return;	
			}
			
			// valida que la fraccion no exista previamente creada	
			var criterio = {};
			criterio.name = this.fraccion.getValue();
			this.searchFracciones(criterio, 1, lang.hitch(this, function(fracciones) {  
				
				if (fracciones.length > 0) {
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "La fracción " + this.fraccion.getValue() + " ya existe.");
					domStyle.set(this.progressBar, "display", "none");				
					return;	
				}
				
				domStyle.set(this.message, "color", "#000253");
				html.set(this.message, "Agregando nueva fracción...");
				domStyle.set(this.progressBar, "display", "block");	
				
				var params = {};
				params.method = "addFraccion";
				params.repositoryid = ecm.model.desktop.defaultRepositoryId;
				params.context = json.stringify(this.config.context);
				var fraccion = {"name": this.fraccion.getValue(), "descripcion": this.descripcion.getValue(), "unidad": parseInt(this.unidad.getValue()), "precio": parseFloat(this.precio.getValue())};
				params.fraccion = json.stringify(fraccion);
				
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
								this.grid.store.newItem(response.fraccion);
								this.grid.store.save();
								
								domStyle.set(this.message, "color", "#000253");
								html.set(this.message, "La nueva fracción " + response.fraccion.name + " ha sido creada.");
								domStyle.set(this.progressBar, "display", "none");
								
							})
						}); 				
			}))			
		},
		
		onExport: function() {
			
			// request a enviar
			var serviceRequest = "/excelecm-rest-broker-common/jaxrs/reportsmodule/getReporteComplejo";				
			serviceRequest += "?tipoSalida=1"; // excel
			serviceRequest += "&claseImpl=com.importsaad.jasper.gc.Fracciones";
			serviceRequest += "&nombrePlantilla=gc/DetalleFracciones";
			serviceRequest += "&params=";
			serviceRequest += "&condiciones=";
			serviceRequest += "&nombreArchivo=Fracciones Arancelarias";
			serviceRequest += "&os=" + ecm.model.desktop.getRepository(ecm.model.desktop.defaultRepositoryId).objectStoreName;
			
			//console.log(serviceRequest);
						
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Exportando fracciones arancelarias...");
			domStyle.set(this.progressBar, "display", "block");			
			
			// Timeout de 1 segundo para presentar correctamente progress status
			setTimeout(lang.hitch(this, function() {
										
				xhr(serviceRequest, {
					method: "GET",
					handleAs: "json",
					preventCache: true,
					sync : false, 
					timeout: 10000, // 10 secs
					headers: { "Content-Type": "application/json"}
				}).then(lang.hitch(this, function(data) {	
									
					var mensaje = "";
					if (data.status == 1) {	
						mensaje = "Ocurrió un error al momento de generar el reporte: " + data.desc;						
					} else if (data.desc == null) {
						mensaje = "No es posible abrir el reporte ya que no fue identificado el nombre de manera correcta, favor de consultar a su administrador.";						
					} else if (data.count == 0) {		
						mensaje = "El reporte no contiene resultados bajo los criterios especificados.";				
					}
					
					if (this.isValidId(data.desc)) {
						// show p8 document
						this.showDocument(data.desc);
						html.set(this.message, "");						
					}
					
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, mensaje);
					domStyle.set(this.progressBar, "display", "none")				
					
				})), lang.hitch(this, function(err) {	
					
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "Ocurrió un error al momento de generar el reporte: " + err);
					domStyle.set(this.progressBar, "display", "none")				
					
				});	
			
			}), 1000);			
		},
		
		isValidId: function(val) {
		    var pattern = new RegExp("^({)([A-Z0-9]{8})(-)([A-Z0-9]{4})(-)([A-Z0-9]{4})(-)([A-Z0-9]{4})(-)([A-Z0-9]{12})(})$");
			return pattern.test(val);
		},
		
		showDocument: function(docId) {
			var repository = ecm.model.desktop.getRepository(ecm.model.desktop.defaultRepositoryId);
			this.showItem(repository, docId);
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
			this.fraccion.reset();
			this.descripcion.reset();
			this.unidad.reset();
			this.precio.reset();
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
		
		formatCurrency: function(num) {
		    var p = num.toFixed(2).split(".");
		    return p[0].split("").reverse().reduce(function(acc, num, i, orig) {
		        return  num + (i && !(i % 3) ? "," : "") + acc;
		    }, "") + "." + p[1];
		},
		
		formatDescripcion: function(value) {
			if (value == null)
				return "";		
	  		var maxLen = this.isMaximized() ? 130 : 50;
	  		return (value.length > maxLen ? value.substr(0, maxLen) + "..." : value);
		}		
			
	});
});
