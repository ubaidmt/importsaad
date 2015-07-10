define([
	"dojo/_base/declare",
	"dojo/_base/lang",
	"dojo/_base/array",
	"dijit/registry",
	"dojo/on",
	"dojo/keys",		
	"dojo/query",
	"dojo/dom",
	"dojo/dom-style",
	"dojo/dom-class",
	"dojo/html",
	"dojo/json",
	"dojo/request/xhr",
	"dojox/widget/Standby",
	"dojo/store/Memory",
	"dojo/data/ObjectStore",    	
	"ecm/widget/Select",	
	"ecm/widget/FilteringSelect",	
	"ecm/widget/dialog/LoginDialog",
	"ecm/model/Request",	
    "dijit/tree/ObjectStoreModel",
	"dijit/Tree",
	"dojox/grid/DataGrid",
	"dojo/data/ItemFileWriteStore",
	"ecm/model/Desktop",	
	"ecm/widget/layout/_LaunchBarPane",	
	"dojo/text!./templates/ReportesPane.html"
],

function(declare,
		lang,
		array,
		registry,
		on,
		keys,		
		query,
		dom,
		domStyle,
		domClass,
		html,
		json,
		xhr,
		Standby,
		Memory,
		ObjectStore,
		Select,
		FilteringSelect,		
		LoginDialog,
		Request,
		ObjectStoreModel,
		Tree,
		DataGrid,
		ItemFileWriteStore,		
		Desktop,
		_LaunchBarPane,		
		template) {

	/**
	 * @name importSaadPluginDojo.feature.ReportesPane
	 * @class Reportes Panel
	 * @augments ecm.widget.layout._LaunchBarPane
	 */
	return declare("importSaadPluginDojo.feature.ReportesPane", [
		_LaunchBarPane
	], {
		/** @lends importSaadPluginDojo.feature.ReportesPane.protocompany */

		templateString: template,
		widgetsInTemplate: true,

		postCreate: function() {
			this.logEntry("postCreate");
			this.inherited(arguments);
			this.registry = require("dijit/registry");
			
			on(this.limpiaResultadosButton, "click", lang.hitch(this, function() {		
				this.destroyGrid();
			}));
			on(this.ejecutaReporteButton, "click", lang.hitch(this, function() {
				this.doReporte();
			}));			
			
			this.logExit("postCreate");
		},	

		loadContent: function() {
				
			// cuando existe un cambio de usuario en la misma sesion del navegador, es necesario reiniciar el arbol y el grid para considerar los nuevos permisos
			if (this._inituser != ecm.model.desktop.userId) {
				this.destroyTree();
				this.destroyGrid();
				this.initTree();				
				this.loadCatalogos();
				this.initGrid();				
				this._inituser = ecm.model.desktop.userId;		
			}
			html.set(this.informationMessage, "Selección de reportes para: <b>" + ecm.model.desktop.userId + "</b>");
		},

		showCriteriosPane: function(rubro) {
			domStyle.set(this.criteriosPaneGF, "display", "none");
			domStyle.set(this.criteriosPaneDefault, "display", "block");
			
			if (this.tree == null || rubro == null)
				return;

			if (rubro.name == "Gestión de Facturas") {
				domStyle.set(this.criteriosPaneDefault, "display", "none");
				domStyle.set(this.criteriosPaneGF, "display", "block");				
			}
		},
		
		initGrid: function() {	

			html.set(this.mensajeContenidoGrid, 'No existen reportes para el rubro seleccionado');
			domStyle.set(this.reportesGrid, "height", "40px;");	
		
			if (this.grid == null)
			{
				var gridStore = new ItemFileWriteStore({
					data: {
						identifier: "id",
						items: []
					}
				});				
			
				var gridLayout = [
					{name: 'Id', field: 'id', hidden: true},
					{name: 'Nombre', field: 'name', width: '20%', editable: false},
					{name: 'Descripción', field: 'descripcion', width: '80%', editable: false}
				];
				
				this.grid = new DataGrid({
					id: 'gridReportes',
					store: gridStore,
					structure: gridLayout,
					selectionMode: 'single',
					sortInfo: 2, // asc by nombre
					autoHeight: true
				});

				this.grid.placeAt(this.reportesGrid);			
				this.grid.startup();
				
				on(this.grid, "click", lang.hitch(this, function() {
					this.onGridItemSelect(this.grid);
				}));
				on( this.grid, "stylerow", lang.hitch(this, function(row) {
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

				this.initParametrosData();				
				
			}		
		},
		
		initTree: function() {	
			if (this.tree == null)
			{
				var requestParams = {};
				requestParams.method = "getReportesTree";
				requestParams.repositoryid = ecm.model.desktop.defaultRepositoryId;
				Request.invokePluginService("ImportSaadPlugin", "ReportesService",
					{
						requestParams: requestParams,
						requestCompleteCallback: lang.hitch(this, function(response) {	// success

							if (response.error != null) {
								this.message.innerHTML = response.error;
								return;
							}
											
							this.treeStore = new Memory({
								data: response.results,
								getChildren: function(object){
									return this.query({parent: object.id});
								}
							});

							// Create the model
							var treeModel = new ObjectStoreModel({
								store: this.treeStore,
								query: {id: 'root'}
							});

							// Create the Tree.
							this.tree = new Tree({
								model: treeModel,
								showRoot: false,
								autoExpand: false
							});		

							this.tree.placeAt(this.reportesTree);
							this.tree.startup();
							
							on(this.tree, "click", lang.hitch(this, function(item) {
								this.onTreeItemSelect(item, this.grid);
							}));								
							
						})
					});			
			}
		},

		onTreeItemSelect: function(item, grid) {		
			this.loadGridData(item.rubroid, grid);	
			this.showCriteriosPane(item);
		},
		
		onGridItemSelect: function(grid) {
			this.initParametrosData();
			
			var items = grid.selection.getSelected();
			array.forEach(items, function(selectedItem) {
				if (selectedItem.condiciones != null && selectedItem.condiciones != '') {
					var condiciones = json.parse(selectedItem.condiciones, true);
					array.forEach(condiciones, function(condicion) {
						var widget = registry.byId(condicion.valor);
						if (widget != null) {
							var pointer = dom.byId(condicion.valor+"Pointer");
							if (pointer != null) {
								if ("requerido" in condicion || condicion.requerido) { // requerido default: false
									domClass.replace(pointer, "mandatoryReportField");
									widget.set("required", true);
									if (widget instanceof ecm.widget.DatePicker)
										widget.set("value", new Date());
								} else {
									domClass.replace(pointer, "optionalReportField");
									widget.set("required", false);				
								}
							}
						}
					});
				}
			});
			
		},		
		
		getSelectedItem: function() {	
			var item = null;
			var items = this.grid.selection.getSelected();		
			array.forEach(items, function(selectedItem) {
				item = selectedItem;								
			});		
			return item;		
		},
		
		destroyTree: function() {
			if (this.tree != null) {
				this.tree.destroyRecursive();
				this.tree = null;
			}		
		},
		
		loadGridData: function(rubroid, grid) {
			html.set(this.mensajeContenidoGrid, "");
			domStyle.set(this.reportesGrid, "height", "100%");
				
			var params = {};
			params.method = "getReportesDetalle";
			params.rubroid = rubroid;
			params.userid = ecm.model.desktop.userId;
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			Request.invokePluginService("ImportSaadPlugin", "ReportesService",
				{
					requestParams: params,
					requestCompleteCallback: lang.hitch(this, function(response) {	// success
				
							this.message.innerHTML = "";							

							if (response.error != null) {
								this.message.innerHTML = response.error;
							}
														
							if (response.results.length > 0) 
							{							
								var gridStore = new ItemFileWriteStore({
									data: {
										identifier: "id",
										items: response.results
									}
								});	
								grid.store = gridStore;
								grid.render();
							} 
							else 
							{
								this.destroyGrid();
							}
						
					})
				});				
		},
		
		destroyGrid: function() {	
			this.showCriteriosPane(null);
			if (this.grid != null) {
				this.grid.destroy();
				this.grid = null;
			}
			this.initGrid();
		},
		
		initParametrosData: function() {

			this.message.innerHTML = "";
			this.formatoSalida.reset();
			
			var widgetList = query('div[widgetid^="param"]'); // validation texts, date pickers, range bound tet box
            array.forEach(widgetList, function(widget) {
				widgetName = widget.id;
				if (widgetName.indexOf("widget_") != -1)
					widgetName = widgetName.substring(7,widgetName.length);	
				if (registry.byId(widgetName) != null) {
					registry.byId(widgetName).required = false;
					if (dom.byId(widgetName+"Pointer") != null)
						domClass.replace(dom.byId(widgetName+"Pointer"), "clearReportField");
					if (registry.byId(widgetName) instanceof ecm.widget.DatePicker || registry.byId(widgetName) instanceof ecm.widget.ValidationTextBox || registry.byId(widgetName) instanceof ecm.widget.RangeBoundTextBox) {
						registry.byId(widgetName).reset();
						registry.byId(widgetName).set("disabled", false);
					} if (registry.byId(widgetName) instanceof ecm.widget.FilteringSelect) { 
						registry.byId(widgetName).set('item', registry.byId(widgetName).store.get(""));
						registry.byId(widgetName).set("disabled", false);
					}
				}
			});	
            
		},
		
		getParametrosString: function(parametrosStr) {
			
			if (parametrosStr == null || parametrosStr == '')
				return '';
			
			var parametros = json.parse(parametrosStr, true);			
			var out = "";
			array.forEach(parametros, lang.hitch(this, function(parametro) {
				out += parametro.nombre;
				out += "=";
				if (!("placeholder" in parametro) || parametro.placeholder) { // placeholder default: true
					var widget = registry.byId(parametro.valor);
					if (widget != null) {
						if (widget instanceof ecm.widget.DatePicker) {	
							var strDate = widget.getValue();			
							if (strDate.length >= 10) {
								out += strDate.substring(8,10);
								out += "/" + strDate.substring(5,7);
								out += "/" + strDate.substring(0,4);
							}
						} else if (widget instanceof ecm.widget.FilteringSelect) {
							var obj = this.getFilteringSelectItem(widget);
							if (obj != null)
								out += obj.name;
						} else if (widget instanceof ecm.widget.Select) {
							out += this.getSelectSelectedLabel(widget);							
						} else
							out += widget.getValue();
					}					
				} else {
					out += parametro.valor;
				}
				out += "|";
			}));
			
			// remove last separator character
			if (out.length > 0)
				out = out.substring(0, out.length-1);
			
			//console.log("Parametros: " + out);
			return out;
		},
		
		getCondicionesString: function(condicionesStr) {
			
			if (condicionesStr == null || condicionesStr == '')
				return '';
			
			var condiciones = json.parse(condicionesStr, true);			
			var out = "";
			array.forEach(condiciones, lang.hitch(this, function(condicion) {
				out += condicion.nombre;
				out += "=";
				if (!("placeholder" in condicion) || condicion.placeholder) { // placeholder default: true
					var widget = registry.byId(condicion.valor);
					if (widget != null) {
						if (widget instanceof ecm.widget.DatePicker) {	
							var strDate = widget.getValue();			
							if (strDate.length >= 10) {
								out += strDate.substring(8,10);
								out += "/" + strDate.substring(5,7);
								out += "/" + strDate.substring(0,4);
							}
						} else if (widget instanceof ecm.widget.FilteringSelect) {
							var obj = this.getFilteringSelectItem(widget);
							if (obj != null)
								out += obj.id;
						} else if (widget instanceof ecm.widget.Select) {
							out += widget.getValue();						
						} else
							out += widget.getValue();
					}
				} else {
					out += condicion.valor;
				}				
				out += "|";
			}));
			
			// remove last separator character
			if (out.length > 0)
				out = out.substring(0, out.length-1);
			
			//console.log("Condiciones: " + out);
			return out;
		},
		
		replaceAll: function( text, find, replace ) {
			while (text.toString().indexOf(find) != -1)
				text = text.toString().replace(find,replace);
			return text;
		},		

		loadCatalogos: function() {
			
			if (this.proveedores == null) {
				
				var store = new Memory({
					data: []
				});
			
				this.proveedores = new FilteringSelect({
					id: "paramProveedor",
					value: "",
					autoComplete: true,
					pageSize: 30,
					style: "width: 200px;",
					required: false,
			        store: store,
			        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
			    });
				
				this.proveedores.placeAt(this.proveedoresDiv);			
				this.proveedores.startup();
			}		
			
			var params = {};
			params.method = "getSolDocProveedores";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			Request.invokePluginService("ImportSaadPlugin", "ReportesService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							var items = [];
							var initItem = {"id": "", "name": "-- SELECCIONAR --"};
							items.push(initItem);		
							array.forEach(response.proveedores, function(row) {							
								items.push({"id": row.id, "name": row.proveedorNombre});				
							});
							
							var store = new Memory({
								data: items
							});							
							
							this.proveedores.set('store', store);
							this.proveedores.set('item', this.proveedores.store.get(""));
							
						})
					}); 

			if (this.empresas == null)
			{
				var store = new Memory({
					data: []
				});
			
				this.empresas = new FilteringSelect({
					id: "paramEmpresa",
					value: "",
					autoComplete: true,
					pageSize: 30,
					style: "width: 200px;",
					required: false,
			        store: store,
			        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
			    });
				
				this.empresas.placeAt(this.empresasDiv);			
				this.empresas.startup();
			}
								
			var requestParams = {};
			requestParams.method = "getSolDocEmpresas";
			requestParams.repositoryid = ecm.model.desktop.defaultRepositoryId;
			Request.invokePluginService("ImportSaadPlugin", "ReportesService",
				{
					requestParams: requestParams,
					requestCompleteCallback: lang.hitch(this, function(response) {	// success
						
						var items = [];
						var initItem = {"id": "", "name": "-- SELECCIONAR --"};
						items.push(initItem);		
						array.forEach(response.empresas, function(row) {							
							items.push({"id": row.id, "name": row.name});				
						});
						
						var store = new Memory({
							data: items
						});							
						
						this.empresas.set('store', store);
						this.empresas.set('item', this.empresas.store.get(""));									
						 
						this.empresas.placeAt(this.empresasDiv);			
						this.empresas.startup();									
						
					})
				});					

			if (this.clientes == null)
			{
				var store = new Memory({
					data: []
				});
			
				this.clientes = new FilteringSelect({
					id: "paramCliente",
					value: "",
					autoComplete: true,
					pageSize: 30,
					style: "width: 200px;",
					required: false,
			        store: store,
			        fetchProperties: { sort: [{attribute:"name", descending: false}] } 
			    });
				
				this.clientes.placeAt(this.clientesDiv);			
				this.clientes.startup();				
			}
			
			var requestParams = {};
			requestParams.method = "getSolDocClientes";
			requestParams.repositoryid = ecm.model.desktop.defaultRepositoryId;
			Request.invokePluginService("ImportSaadPlugin", "ReportesService",
				{
					requestParams: requestParams,
					requestCompleteCallback: lang.hitch(this, function(response) {	// success
						
						var items = [];
						var initItem = {"id": "", "name": "-- SELECCIONAR --"};
						items.push(initItem);		
						array.forEach(response.clientes, function(row) {							
							items.push({"id": row.id, "name": row.razonSocial});				
						});
						
						var store = new Memory({
							data: items
						});							
						
						this.clientes.set('store', store);
						this.clientes.set('item', this.clientes.store.get(""));																	
					})
				});												
		},		
		
		getSelectSelectedLabel: function(obj) {
			var label = "";
			var value = obj.get('value');
			array.forEach(obj.options, function(option) {
				if (option.value.toString() == value) {
					label = option.label.toString();
					return;
				}
			});
			return label;
		},
		
		getFilteringSelectItem: function(obj) {
			var item = null;
			var value = obj.get('value');
			if (value != "")
				var item = obj.store.get(value);
			return item;
		},		
		
		setReporteProgreso: function(enProgreso) {
			if (enProgreso) {
				this.ejecutaReporteButton.set('disabled', true);
				registry.byId('standByWidget').show();
			} else {
				this.ejecutaReporteButton.set('disabled', false);
				registry.byId('standByWidget').hide();					
			}
		},

		doReporte: function() {
			this.logEntry("doReporte");
			html.set(this.message, "");
			
			// Validacion de seleccion de reporte
			var selectedItem = this.getSelectedItem();		
			if (selectedItem == null) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Favor de seleccionar un reporte de la lista de resultados.");
				return;
			}			

			// Validacion de integridad de condiciones requeridad para el reporte seleccionado
			var validConditions = true;
			var widgetList = query('div[widgetid^="param"]');	// text boxes, date pickers
            array.forEach(widgetList, lang.hitch(this, function(widget) {
				if (validConditions) {							
					widgetName = widget.id;
					if (widgetName.indexOf("widget_") != -1)
						widgetName = widgetName.substring(7,widgetName.length);	
					if (registry.byId(widgetName) != null) {
						if (registry.byId(widgetName) instanceof ecm.widget.DatePicker || registry.byId(widgetName) instanceof ecm.widget.ValidationTextBox || registry.byId(widgetName) instanceof ecm.widget.RangeBoundTextBox) {
							if (!registry.byId(widgetName).isValid())
								validConditions = false;
						} else if (registry.byId(widgetName) instanceof ecm.widget.FilteringSelect) {
							if (!registry.byId(widgetName).isValid())
								validConditions = false;
							else if (registry.byId(widgetName).required && this.getFilteringSelectItem(registry.byId(widgetName)) == null)
								validConditions = false;							
						}
					}
				}
			}));	
				
			if (!validConditions) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Existen parámetros requeridos para el reporte seleccionado que son inválidos, favor de corregirlos.");
				return;
			}
			
			// Valida rango de fechas
			/*
			if (!this.validaRangoFechas())
				return;
			*/

			// Set mensaje estatus
			this.setReporteProgreso(true);
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Generando Reporte...");
			
			// Timeout de 1 segundo para presentar correctamente widget de Standby
			setTimeout(lang.hitch(this, function() {
				
				// request a enviar
				var serviceRequest = "/excelecm-rest-broker-common/jaxrs/reportsmodule/getReporteComplejo";				
				
				serviceRequest += "?tipoSalida=" + this.formatoSalida.get('value');
				if (selectedItem.clase != null)
					serviceRequest += "&claseImpl=" + selectedItem.clase;
				if (selectedItem.plantilla != null)
					serviceRequest += "&nombrePlantilla=" + selectedItem.plantilla;
				if (selectedItem.parametros != null)
					serviceRequest += "&params=" + this.getParametrosString(selectedItem.parametros);
				if (selectedItem.condiciones != null)
					serviceRequest += "&condiciones=" + this.getCondicionesString(selectedItem.condiciones);
				
				serviceRequest += "&os=" + ecm.model.desktop.getRepository(ecm.model.desktop.defaultRepositoryId).objectStoreName;				
							
				// invoca servicio de reportes					
				//console.log(serviceRequest);
				
				xhr(serviceRequest, {
					method: "GET",
					handleAs: "json",
					preventCache: true,
					sync : false, 
					timeout: 0, // no timeout
					headers: { "Content-Type": "application/json"}
				}).then(lang.hitch(this, function(data) {	
						
					this.setReporteProgreso(false);	
										
					if (data.status == 1) {
						domStyle.set(this.message, "color", "#6B0A0A");
						html.set(this.message, "Ocurrió un error al momento de generar el reporte: " + data.desc);
					} else if (data.desc == null) {
						domStyle.set(this.message, "color", "#6B0A0A");
						html.set(this.message, "No es posible abrir el reporte ya que no fue identificado el nombre de manera correcta, favor de consultar a su administrador.");						
					} else if (data.count == 0) {		
						domStyle.set(this.message, "color", "#6B0A0A");
						html.set(this.message, "El reporte no contiene resultados bajo los criterios especificados.");				
					} else {
						if (this.isValidId(data.desc)) {
							// show p8 document
							this.showDocument(data.desc);
							html.set(this.message, "");						
						} else {
							// download reporte
							var frmReporte = dom.byId("formDownloadReporte");
							dom.byId("reporte").value = data.desc;
							frmReporte.submit();	
							html.set(this.message, "");						
						}					
					}	
					
					
				})), lang.hitch(this, function(err) {	
					
					this.setReporteProgreso(false);	
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "Ocurrió un error al momento de generar el reporte: " + err);
					
				});				
								
			}), 1000);
			
			this.logExit("doReporte");
		},
		
		validaRangoFechas: function() {
					
			// si las fechas de registro son establecidas como obligatorias
			if (this.paramFechaSolicitudDesde.required && this.paramFechaSolicitudHasta.required) {
				var dateRangeMaxDays = 31; // 1 mes
				if (!this.isMaxDateRangeValid(this.paramFechaSolicitudDesde, this.paramFechaSolicitudHasta, dateRangeMaxDays)) {
					domStyle.set(this.message, "color", "#6B0A0A");
					html.set(this.message, "El rango establecido entre las fechas de registro es mayor al permitido. Máximo permitido " + dateRangeMaxDays + " dias.");
					return false;					
				}
			}			
			
			return true;
		},
		
		isValidId: function(val) {
		    var pattern = new RegExp("^({)([A-Z0-9]{8})(-)([A-Z0-9]{4})(-)([A-Z0-9]{4})(-)([A-Z0-9]{4})(-)([A-Z0-9]{12})(})$");
			return pattern.test(val);
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
		
		isMaxDateRangeValid: function(dateWidget1, dateWidget2, maxDaysDiff) {
			var strDate = dateWidget1.getValue();
			var out = "";
			if (strDate.length >= 10) {
				out += strDate.substring(5,7);
				out += "/" + strDate.substring(8,10);
				out += "/" + strDate.substring(0,4);
			}
			var date1 = new Date(out);

			strDate = dateWidget2.getValue();
			out = "";
			if (strDate.length >= 10) {
				out += strDate.substring(5,7);
				out += "/" + strDate.substring(8,10);
				out += "/" + strDate.substring(0,4);
			}
			var date2 = new Date(out);
			
			if (maxDaysDiff >= this.daysDiff(date1, date2))
				return true;
			else
				return false;
		},
		
		daysDiff: function(date1, date2) {
			var timeDiff = Math.abs(date2.getTime() - date1.getTime());
			var diffDays = Math.ceil(timeDiff / (1000 * 3600 * 24)); 
			return diffDays;			
		}		
	});
});
