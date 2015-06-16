define([
	"dojo/_base/declare",
	"dojo/_base/lang",
	"dojo/_base/array",
	"dijit/registry",
	"dojo/_base/connect",
	"dojo/query",
	"dojo/dom",
	"dojo/dom-style",
	"dojo/html",
	"dojo/json",
	"dojo/request/xhr",
	"dojox/widget/Standby",
	"dojo/store/Memory",
	"ecm/model/Request",	
    "dijit/tree/ObjectStoreModel",
	"dijit/Tree",
	"dojox/grid/DataGrid",
	"dojo/data/ItemFileWriteStore",
	"dojox/grid/cells/dijit",
	"ecm/widget/layout/_LaunchBarPane",	
	"dojo/text!./templates/ImportSaadReportesPane.html"
],

function(declare,
		lang,
		array,
		registry,
		connect,
		query,
		dom,
		domStyle,
		html,
		json,
		xhr,
		Standby,
		Memory,
		Request,
		ObjectStoreModel,
		Tree,
		DataGrid,
		ItemFileWriteStore,		
		cells,
		_LaunchBarPane,		
		template) {

	/**
	 * @name importSaadPluginDojo.ImportSaadReportesPane
	 * @class Reportes Panel
	 * @augments ecm.widget.layout._LaunchBarPane
	 */
	return declare("importSaadPluginDojo.ImportSaadReportesPane", [
		_LaunchBarPane
	], {
		/** @lends importSaadPluginDojo.ImportSaadReportesPane.protocompany */

		templateString: template,
		widgetsInTemplate: true,
		registry: null,
		_tree: null,
		_treeStore: null,
		_grid: null,
		_clientesSelect: null,
		_navierasSelect: null,
		_agenciasSelect: null,
		_importadorasSelect: null,
		proveedoresGF: null,
		_inituser: null,

		postCreate: function() {
			this.logEntry("postCreate");
			this.inherited(arguments);
			this.registry = require("dijit/registry");
			
			this._inituser = ecm.model.desktop.userId;

			this.initTree();
			
			connect.connect(this.limpiaResultadosButton, "onClick", lang.hitch(this, function() {		
				this.destroyGrid();
			}));
			connect.connect(this.ejecutaReporteButton, "onClick", lang.hitch(this, function() {
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
				this._inituser = ecm.model.desktop.userId;		
			}
			
			html.set(dom.byId("informtionMessage"), "Selección de reportes para: <b>" + ecm.model.desktop.userId + "</b>");
			this.loadCatalogosGC();
			this.loadCatalogosGF();
			this.initGrid();

		},

		showCriteriosPane: function(rubro) {
			domStyle.set(this.criteriosPaneGC, "display", "none");
			domStyle.set(this.criteriosPaneGF, "display", "none");
			domStyle.set(this.criteriosPaneDefault, "display", "block");
			
			if (this._tree == null || rubro == null)
				return;

			if (rubro.name == "Gestión de Contenedores") {
				domStyle.set(this.criteriosPaneDefault, "display", "none");
				domStyle.set(this.criteriosPaneGF, "display", "none");
				domStyle.set(this.criteriosPaneGC, "display", "block");
			} else if(rubro.name == "Gestión de Facturas") {
				domStyle.set(this.criteriosPaneDefault, "display", "none");
				domStyle.set(this.criteriosPaneGF, "display", "block");
				domStyle.set(this.criteriosPaneGC, "display", "none");				
			}
		},
		
		initGrid: function() {	

			html.set(dom.byId("mensajeContenidoGrid"), 'No existen reportes para el rubro seleccionado');
			dom.byId("reportesGrid").style.height = "40px";	
		
			if (this._grid == null)
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
				
				this._grid = new DataGrid({
					id: 'gridReportes',
					store: gridStore,
					structure: gridLayout,
					selectionMode: 'single',
					sortInfo: 2, // asc by nombre
					autoHeight: true
				});

				this._grid.placeAt(dom.byId("reportesGrid"));			
				this._grid.startup();
				
				connect.connect(this._grid, "onClick", lang.hitch(this, function() {
					this.onGridItemSelect(this._grid);
				}));
				connect.connect( this._grid, "onStyleRow", lang.hitch(this, function(row) {
					var item = this._grid.getItem( row.index );
					if ( item == null ) return;
					if (row.odd) row.customStyles += 'background-color:#F7F6F6;';
				}));

				this.initParametrosData();				
				
			}		
		},
		
		initTree: function() {	
			if (this._tree == null)
			{
				var requestParams = {};
				requestParams.method = "getReportesTree";
				requestParams.repositoryid = ecm.model.desktop.defaultRepositoryId;
				Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
					{
						requestParams: requestParams,
						requestCompleteCallback: lang.hitch(this, function(response) {	// success

							if (response.error != null) {
								this.message.innerHTML = response.error;
								return;
							}
											
							this._treeStore = new Memory({
								data: response.results,
								getChildren: function(object){
									return this.query({parent: object.id});
								}
							});

							// Create the model
							var treeModel = new ObjectStoreModel({
								store: this._treeStore,
								query: {id: 'root'}
							});

							// Create the Tree.
							this._tree = new Tree({
								model: treeModel,
								showRoot: false,
								autoExpand: false
							});		

							this._tree.placeAt(this.reportesTree);
							this._tree.startup();
							
							connect.connect(this._tree, "onClick", lang.hitch(this, function(item) {
								this.onTreeItemSelect(item, this._grid);
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
						if (registry.byId(condicion.valor) != null) {
							var mark  = "/importsaad-cdn/images/pointers/green.png";
							if (condicion.requerido) {
								mark = "/importsaad-cdn/images/pointers/red.png";					
								registry.byId(condicion.valor).required = true;
							}
							dom.byId(condicion.valor+"Pointer").style.display = "block";
							dom.byId(condicion.valor+"Pointer").src = mark;		
						}
					});
				}
			});
			
		},
		
		getSelectedItem: function() {	
			var item = null;
			var items = this._grid.selection.getSelected();		
			array.forEach(items, function(selectedItem) {
				item = selectedItem;								
			});		
			return item;		
		},
		
		destroyTree: function() {
			if (this._tree != null) {
				this._tree.destroyRecursive();
				this._tree = null;
			}		
		},
		
		loadGridData: function(rubroid, grid) {
			html.set(dom.byId("mensajeContenidoGrid"), '');
			dom.byId("reportesGrid").style.height = "100%";
				
			var requestParams = {};
			requestParams.method = "getReportesDetalle";
			requestParams.rubroid = rubroid;
			requestParams.repositoryid = ecm.model.desktop.defaultRepositoryId;
			Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
				{
					requestParams: requestParams,
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
			if (this._grid != null) {
				this._grid.destroy();
				this._grid = null;
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
						dom.byId(widgetName+"Pointer").style.display = "none";
					if (registry.byId(widgetName) instanceof ecm.widget.DatePicker || registry.byId(widgetName) instanceof ecm.widget.ValidationTextBox || registry.byId(widgetName) instanceof ecm.widget.RangeBoundTextBox)
						registry.byId(widgetName).reset();					
				}
			});	
			widgetList = query('table[widgetid^="param"]'); // selects
            array.forEach(widgetList, function(widget) {		
				widgetName = widget.id;		
				if (registry.byId(widgetName) != null) {
					registry.byId(widgetName).required = false;
					if (dom.byId(widgetName+"Pointer") != null)
						dom.byId(widgetName+"Pointer").style.display = "none";				
					if (registry.byId(widgetName) instanceof ecm.widget.Select)
						registry.byId(widgetName).reset();
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
				if (parametro.placeholder) {
					if (registry.byId(parametro.valor) != null) {
						var widget = registry.byId(parametro.valor);
						if (widget instanceof ecm.widget.DatePicker) {	
							var strDate = widget.getValue();			
							if (strDate.length >= 10) {
								out += strDate.substring(8,10);
								out += "/" + strDate.substring(5,7);
								out += "/" + strDate.substring(0,4);
							}
						} else if (widget instanceof ecm.widget.Select) {
							var label = this.getSelectedLabel(widget);
							if (label != null)							
								out += label;
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
			
			console.log("Parametros: " + out);
			return out;
		},

		getSelectedLabel: function(widget) {
			var value = null;
			array.forEach(widget.options, lang.hitch(this, function(option) {
				if (option.selected && option.value != "")
					value = option.label;
			}));
			return value;
		},
		
		getCondicionesString: function(condicionesStr) {
			
			if (condicionesStr == null || condicionesStr == '')
				return '';
			
			var condiciones = json.parse(condicionesStr, true);
			var out = "";
			array.forEach(condiciones, lang.hitch(this, function(condicion) {
				out += condicion.nombre;
				out += "=";
				if (registry.byId(condicion.valor) != null) {
					var widget = registry.byId(condicion.valor);
					if (widget instanceof ecm.widget.DatePicker) {	
						var strDate = widget.getValue();			
						if (strDate.length >= 10) {
							out += strDate.substring(8,10);
							out += "/" + strDate.substring(5,7);
							out += "/" + strDate.substring(0,4);
						}
					} else if (widget instanceof ecm.widget.Select)
						out += widget.getValue();
					else
						out += widget.getValue();
				}
				out += "|";
			}));
			
			// remove last separator character
			if (out.length > 0)
				out = out.substring(0, out.length-1);
			
			console.log("Condiciones: " + out);
			return out;
		},
		
		replaceAll: function( text, find, replace ) {
			while (text.toString().indexOf(find) != -1)
				text = text.toString().replace(find,replace);
			return text;
		},

		loadCatalogosGC: function() {
			if (this._clientesSelect == null)
			{
				var requestParams = {};
				requestParams.method = "getClientes";
				requestParams.repositoryid = ecm.model.desktop.defaultRepositoryId;
				Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
					{
						requestParams: requestParams,
						requestCompleteCallback: lang.hitch(this, function(response) {	// success

							if (response.error != null) {
								this.message.innerHTML = error;				
								return;
							}
							
							var jsonData = [
								{"id": "", "name": "-- SELECCIONAR --"}
							];
							
							array.forEach(response.results, function(row) {
								jsonObj = json.parse('{"id": "' + row.name + '","name": "' + row.name + '"}', true);							
								jsonData.push(jsonObj);				
							});

							var clientesStore = new ItemFileWriteStore({
								data: {
									identifier: "id",
									label: "name",
									items: jsonData
								}
							});	
							
							this._clientesSelect = new ecm.widget.Select({
								id: "paramNombreCliente",
								name: "paramNombreCliente",
								store: clientesStore
							 }, "paramNombreCliente");
							 
							this._clientesSelect.set('style', {width: '60%'});
							this._clientesSelect.placeAt(dom.byId("clientesSelect"));			
							this._clientesSelect.startup();							
							
						})
					});					
			}

			if (this._navierasSelect == null && this._agenciasSelect == null)
			{
				var requestParams = {};
				requestParams.method = "getProveedores";
				requestParams.repositoryid = ecm.model.desktop.defaultRepositoryId;
				Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
					{
						requestParams: requestParams,
						requestCompleteCallback: lang.hitch(this, function(response) {	// success

							if (response.error != null) {
								this.message.innerHTML = error;				
								return;
							}
							
							var jsonData = [
								{"id": "", "name": "-- SELECCIONAR --"}
							];
							
							array.forEach(response.results, function(row) {
								jsonObj = json.parse('{"id": "' + row.name + '","name": "' + row.name + '"}', true);							
								jsonData.push(jsonObj);				
							});

							var proveedoresStore = new ItemFileWriteStore({
								data: {
									identifier: "id",
									label: "name",
									items: jsonData
								}
							});	
							
							this._navierasSelect = new ecm.widget.Select({
								id: "paramNombreNaviera",
								name: "paramNombreNaviera",
								store: proveedoresStore
							 }, "paramNombreNaviera");
							
							this._agenciasSelect = new ecm.widget.Select({
								id: "paramNombreAgenciaAduanal",
								name: "paramNombreAgenciaAduanal",
								store: proveedoresStore
							 }, "paramNombreAgenciaAduanal");
							 							
							 
							this._navierasSelect.set('style', {width: '60%'});
							this._navierasSelect.placeAt(dom.byId("navieraSelect"));			
							this._navierasSelect.startup();	

							this._agenciasSelect.set('style', {width: '60%'});
							this._agenciasSelect.placeAt(dom.byId("agenciaAduanalSelect"));			
							this._agenciasSelect.startup();									
							
						})
					});					
			}	
			
			if (this._importadorasSelect == null)
			{
				var requestParams = {};
				requestParams.method = "getLista";
				requestParams.repositoryid = ecm.model.desktop.defaultRepositoryId;
				requestParams.tipolista = 0; // Importadoras
				Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
					{
						requestParams: requestParams,
						requestCompleteCallback: lang.hitch(this, function(response) {	// success

							if (response.error != null) {
								this.message.innerHTML = error;				
								return;
							}
							
							var jsonData = [
								{"id": "", "name": "-- SELECCIONAR --"}
							];
							
							array.forEach(response.results, function(row) {
								jsonObj = json.parse('{"id": "' + row.name + '","name": "' + row.name + '"}', true);							
								jsonData.push(jsonObj);				
							});

							var importadorasStore = new ItemFileWriteStore({
								data: {
									identifier: "id",
									label: "name",
									items: jsonData
								}
							});	
							
							this._importadorasSelect = new ecm.widget.Select({
								id: "paramImportadora",
								name: "paramImportadora",
								store: importadorasStore
							 }, "paramImportadora");		
							 
							this._importadorasSelect.placeAt(dom.byId("importadoraSelect"));			
							this._importadorasSelect.startup();									
							
						})
					});					
			}										
		},			

		loadCatalogosGF: function() {
			if (this.proveedoresGF == null)
			{
				var requestParams = {};
				requestParams.method = "getSolDocProveedores";
				requestParams.repositoryid = ecm.model.desktop.defaultRepositoryId;
				Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
					{
						requestParams: requestParams,
						requestCompleteCallback: lang.hitch(this, function(response) {	// success

							if (response.error != null) {
								this.message.innerHTML = error;				
								return;
							}
							
							var jsonData = [
								{"id": "", "name": "-- SELECCIONAR --"}
							];
							
							array.forEach(response.proveedores, function(row) {
								jsonObj = json.parse('{"id": "' + row.id + '","name": "' + row.proveedorNombre + '"}', true);							
								jsonData.push(jsonObj);				
							});

							var store = new ItemFileWriteStore({
								data: {
									identifier: "id",
									label: "name",
									items: jsonData
								}
							});	
							
							this.proveedoresGF = new ecm.widget.Select({
								id: "paramProveedor",
								name: "paramProveedor",
								store: store
							 }, "paramProveedor");		
							 
							this.proveedoresGF.placeAt(dom.byId("proveedoresGFSelect"));			
							this.proveedoresGF.startup();									
							
						})
					});					
			}	

			if (this.empresasGF == null)
			{
				var requestParams = {};
				requestParams.method = "getSolDocEmpresas";
				requestParams.repositoryid = ecm.model.desktop.defaultRepositoryId;
				Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
					{
						requestParams: requestParams,
						requestCompleteCallback: lang.hitch(this, function(response) {	// success

							if (response.error != null) {
								this.message.innerHTML = error;				
								return;
							}
							
							var jsonData = [
								{"id": "", "name": "-- SELECCIONAR --"}
							];
							
							array.forEach(response.empresas, function(row) {
								jsonObj = json.parse('{"id": "' + row.id + '","name": "' + row.name + '"}', true);							
								jsonData.push(jsonObj);				
							});

							var store = new ItemFileWriteStore({
								data: {
									identifier: "id",
									label: "name",
									items: jsonData
								}
							});	
							
							this.empresasGF = new ecm.widget.Select({
								id: "paramEmpresa",
								name: "paramEmpresa",
								store: store
							 }, "paramEmpresa");		
							 
							this.empresasGF.placeAt(dom.byId("empresasGFSelect"));			
							this.empresasGF.startup();									
							
						})
					});					
			}

			if (this.clientesGF == null)
			{
				var requestParams = {};
				requestParams.method = "getSolDocClientes";
				requestParams.repositoryid = ecm.model.desktop.defaultRepositoryId;
				Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
					{
						requestParams: requestParams,
						requestCompleteCallback: lang.hitch(this, function(response) {	// success

							if (response.error != null) {
								this.message.innerHTML = error;				
								return;
							}
							
							var jsonData = [
								{"id": "", "name": "-- SELECCIONAR --"}
							];
							
							array.forEach(response.clientes, function(row) {
								jsonObj = json.parse('{"id": "' + row.razonSocial + '","name": "' + row.razonSocial + '"}', true);							
								jsonData.push(jsonObj);				
							});

							var store = new ItemFileWriteStore({
								data: {
									identifier: "id",
									label: "name",
									items: jsonData
								}
							});	
							
							this.clientesGF = new ecm.widget.Select({
								id: "paramCliente",
								name: "paramCliente",
								store: store
							 }, "paramCliente");		
							 
							this.clientesGF.placeAt(dom.byId("clientesGFSelect"));			
							this.clientesGF.startup();									
							
						})
					});					
			}										
		},		
		
		setReporteProgreso: function(enProgreso) {
			if (enProgreso) {
				this.ejecutaReporteButton.set('disabled', true);
	     		this.registry.byId('standByWidget').show();
				this.message.style.color = "#267DC5";
			} else {
				this.ejecutaReporteButton.set('disabled', false);
				this.registry.byId('standByWidget').hide();					
				this.message.style.color = "#970303";					
			}
		},

		doReporte: function() {
			this.logEntry("doReporte");
			
			this.message.innerHTML = "";
			
			// Validacion de seleccion de reporte
			var selectedItem = this.getSelectedItem();		
			if (selectedItem == null) {
				this.message.innerHTML = "Favor de seleccionar un reporte de la lista de resultados.";
				return;
			}			

			// Validacion de integridad de condiciones requeridad para el reporte seleccionado
			var validConditions = true;
			var widgetList = query('div[widgetid^="param"]');	// text boxes, date pickers
            array.forEach(widgetList, function(widget) {	
				if (validConditions) {							
					widgetName = widget.id;	
					if (widgetName.indexOf("widget_") != -1)
						widgetName = widgetName.substring(7,widgetName.length);	
					if (registry.byId(widgetName) != null) {
						if (registry.byId(widgetName) instanceof ecm.widget.DatePicker || registry.byId(widgetName) instanceof ecm.widget.ValidationTextBox || registry.byId(widgetName) instanceof ecm.widget.RangeBoundTextBox) {
							if (!registry.byId(widgetName).isValid())
								validConditions = false;
						}
					}
				}
			});	
			widgetList = query('table[widgetid^="param"]'); // selects
            array.forEach(widgetList, function(widget) {	
				if (validConditions) {			
					widgetName = widget.id;	
					if (registry.byId(widgetName) != null) {
						if (registry.byId(widgetName) instanceof ecm.widget.Select) {
							if (!registry.byId(widgetName).isValid())
								validConditions = false;
						}
					}
				}
			});		
			
			if (!validConditions) {
				this.message.innerHTML = "Existen parámetros requeridos para el reporte seleccionado que son inválidos, favor de corregirlos.";
				return;
			}
			
			// Valida que la diferencia de dias entre fechas de creacion no sea mayor a 1 mes
			if (!this.isMaxDateRangeValid(this.paramFechaSolicitudDesde, this.paramFechaSolicitudHasta, 30)) {
				html.set(this.message, "El rango establecido en las fechas de registro inicial y final debe de ser menor o igual a 30 días naturales.");
				return;				
			}			
			
			// Set mensaje estatus
			this.setReporteProgreso(true);
			this.message.innerHTML = "Generando Reporte...";
			
			// Timeout de 1 segundo para presentar correctamente widget de Standby
			setTimeout(lang.hitch(this, function() {
				
				// request a enviar
				var serviceRequest = "/excelecm-rest-broker-common/jaxrs/reportsmodule/getReporteComplejo";				
				
				serviceRequest += "?tipoSalida=" + registry.byId("formatoSalida").get('value');
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
				this.logDebug("doReporte", "url: " + serviceRequest);
				
				xhr(serviceRequest, {
					method: "GET",
					handleAs: "json",
					preventCache: true,
					sync : false, 
					timeout: 10000, // 10 secs
					headers: { "Content-Type": "application/json"}
				}).then(lang.hitch(this, function(data) {	
					
					this.setReporteProgreso(false);	
					
					if (data.status == 1) {	
						this.message.innerHTML = "Ocurrió un error al momento de generar el reporte: " + data.desc;						
					} else if (data.desc == null) {
						this.message.innerHTML = "No es posible abrir el reporte ya que no fue identificado el nombre de manera correcta, favor de consultar a su administrador.";						
					} else if (data.count == 0) {		
						this.message.innerHTML = "El reporte no contiene resultados bajo los criterios especificados.";				
					} else {
						// download reporte
						var frmReporte = dom.byId("formDownloadReporte");
						dom.byId("reporte").value = data.desc;
						frmReporte.submit();		
						this.message.innerHTML = "";							
					}						
					
				})), lang.hitch(this, function(err) {	
					
					this.setReporteProgreso(false);		
					this.message.innerHTML = "Ocurrió un error al momento de generar el reporte: " + err;
					
				});				
								
			}), 1000);
			
			this.logExit("doReporte");
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
			
			if (maxDaysDiff > this.daysDiff(date1, date2))
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
