define([
	"dojo/_base/declare",
	"dojo/_base/lang",
	"dojo/_base/connect",	
	"ecm/model/Request",
	"ecm/widget/dialog/BaseDialog",
	"dijit/form/Button",
	"ecm/widget/Select",
	"dojo/_base/array",	
	"dojo/json",	
	"dojo/dom",	
	"dojo/dom-style",
	"dojo/dom-geometry",		
	"dojox/grid/DataGrid",
	"dojox/timing",	
	"dojo/data/ItemFileWriteStore", 
	"dojo/data/ObjectStore",
	"dojo/store/Memory",	
	"ecm/model/Desktop",
	"dojo/text!./templates/SolDocDevolucionesProvDialog.html"
	],
	function (declare, lang, connect, Request, BaseDialog, Button, Select, array, json, dom, domStyle, domGeom, DataGrid, timing, ItemFileWriteStore, ObjectStore, Memory, Desktop, template) {
	
	/**
	 * @name importSaadPluginDojo.SolDocDevolucionesProvDialog
	 * @class
	 * @augments ecm.widget.dialog.BaseDialog
	 */
	return declare("importSaadPluginDojo.SolDocDevolucionesProvDialog", [BaseDialog], {
	/** @lends importSaadPluginDojo.SolDocDevolucionesProvDialog.prototype */

		//needed to load from template
		contentString: template,
		widgetsInTemplate: true,
		gridDevoluciones: null,
		gridFacturas: null,
		gridPagos: null,
		gridSaldoPendiente: null,
		gridSaldoPendienteFacturas: null,
		repositoryId: null,
		context: null,	
		proveedores: null,
		clientes: null,
		clientesSaldoPendiente: null,
		empresasSaldoPendiente: null,
		comprobantePagoCliente: null,
		comprobantePagoProveedor: null,
		tipoOperacion: 0,
		maxGridDescLength: 30,
		beenMinimized: false,
		ticker: null,

		postCreate: function() {
			this.inherited(arguments);
			this.setResizable(false);
			this.setMaximized(true);
			this.setTitle("Devoluciones de Proveedores");
			this.setIntroText("Registro de pagos asociados una devolución solicitada a un proveedor.");
			this.deleteButton = this.addButton("Eliminar", "onDelete", false, false);
			this.resetButton = this.addButton("Limpiar", "onReset", false, false);
			this.sendButton = this.addButton("Solicitar Devolución", "onSend", false, false);						
			this.saveButton = this.addButton("Guardar", "onSave", false, true); // main button
		},			
		
		showDialog: function() {
			this.onLoad();
			//this.startTicker(600000); // 10 minutes			
			this.inherited("show", []);		
		},

		hide: function() {
			this.stopTicker();
			this.proveedores.destroy();
			this.proveedores = null;
			this.clientes.destroy();
			this.clientes = null;	
			this.clientesSaldoPendiente.destroy();
			this.clientesSaldoPendiente = null;	
			this.empresasSaldoPendiente.destroy();
			this.empresasSaldoPendiente = null;				
			this.gridDevoluciones.destroy();
			this.gridDevoluciones = null;			
			this.gridFacturas.destroy();
			this.gridFacturas = null;
			this.gridPagos.destroy();
			this.gridPagos = null;	
			this.gridSaldoPendiente.destroy();
			this.gridSaldoPendiente = null;	
			this.gridSaldoPendienteFacturas.destroy();
			this.gridSaldoPendienteFacturas = null;											
			this.inherited("hide", []);		
		},		

		resize: function() {
			this.inherited(arguments);

			var size = domGeom.getContentBox(this.domNode);
			var gridDevolucionesHeight = size.h - 420;
			var gridFacturasHeight = size.h - 520;
			var gridPagosHeight = size.h - 620;
			var gridDSaldoPendienteHeight = size.h - 750;
			var gridSaldoPendienteFacturasHeight = (gridDSaldoPendienteHeight / 1.5);								
			if (this.isMaximized()) {
				var tabContentPaneHeight = gridDevolucionesHeight + 150;
				gridDevolucionesHeight += 70; // if maximized, no header is included
				gridFacturasHeight += 70;
				gridPagosHeight += 70;
				gridDSaldoPendienteHeight += 70;
				gridSaldoPendienteFacturasHeight += 70;
	    		if (this.beenMinimized) {
	    			this.tabConteinerPane.set("style", "width: 100%; height: " + tabContentPaneHeight + "px; background-color:#FFFFFF;");
	    			this.tabConteinerPane.resize();
	    		}				
			} else {
				var tabContentPaneHeight = gridDevolucionesHeight + 80;
	    		this.tabConteinerPane.set("style", "width: 100%; height: " + tabContentPaneHeight + "px; background-color:#FFFFFF;");
	    		this.tabConteinerPane.resize();
	    		this.beenMinimized = true;				
			}				
			domStyle.set(this.gridDevolucionesDiv, "height", gridDevolucionesHeight + "px");	
    		domStyle.set(this.gridFacturasDiv, "height", gridFacturasHeight + "px");
    		domStyle.set(this.gridPagosDiv, "height", gridPagosHeight + "px");	
    		domStyle.set(this.gridSaldoPendienteDiv, "height", gridDSaldoPendienteHeight + "px");	
    		domStyle.set(this.gridSaldoPendienteFacturasDiv, "height", gridSaldoPendienteFacturasHeight + "px");			


			if (this.gridDevoluciones != null) {
		    	this.gridDevoluciones.resize();
		    	this.gridDevoluciones.update();		
	    	}
			if (this.gridFacturas != null) {
		    	this.gridFacturas.resize();
		    	this.gridFacturas.update();		
	    	}	    	
			if (this.gridPagos != null) {
		    	this.gridPagos.resize();
		    	this.gridPagos.update();		
	    	}
			if (this.gridSaldoPendiente != null) {
		    	this.gridSaldoPendiente.resize();
		    	this.gridSaldoPendiente.update();		
	    	}
			if (this.gridSaldoPendienteFacturas != null) {
		    	this.gridSaldoPendienteFacturas.resize();
		    	this.gridSaldoPendienteFacturas.update();		
	    	}		    			    		    	
		},	

		startTicker: function(interval) {
			this.ticker = new dojox.timing.Timer();
			this.ticker.setInterval(interval);

			connect.connect(this.ticker, "onTick", lang.hitch(this, function() {
				this.sendDummyRequest();
			}));	
			
			this.ticker.start();
		},

		stopTicker: function() {
			if (this.ticker != null)
				this.ticker.stop();
		},					
		
		setRepositoryId: function(repositoryId) {
			this.repositoryId = repositoryId;
		},		
		
		setContext: function(context) {
			this.context = context;
		},	
		
		isValid: function() {
			var valid = this._fileInput;
			// This test works for both HTML5 and non-HTML5 browsers. 
			valid = valid && (this._fileInput.value) && (this._fileInput.value.length > 0);
			return valid;
		},	
		
		checkFileButtons: function() {
			this.addFileButton.set("disabled", (this.isValid() ? false : true));
			this.deleteFileButton.set("disabled", (this.comprobantePagoProveedor == null ? true : false));
			domStyle.set(this.comprobantePagoProveedorPane, "display", (this.comprobantePagoProveedor == null ? "none" : "block"));
		},			
									
		onLoad: function() {	

			this.checkFileButtons();
			
			this.onReset();
			this.loadProveedores();
			
			// Grid Devoluciones Solicitadas al Proveedor
			this.resetGridDevolucionesValues();
			
			// Grid Facturas Asociadas a la Devolucion
			this.resetGridFacturasValues();
					
			// Clientes Devolucion
			if (this.clientes != null) {
				this.clientes.destroy();
				this.clientes = null;					
			}
			
			var store = new Memory({
				data:  [{ "id": "", "label": "--SELECCIONAR--"}]
			});							
		    
			var os = new ObjectStore({ objectStore: store });
			this.clientes = new Select({
				id: "clientes",
				required: false,
		        store: os
		    });								
			
			this.clientes.placeAt(this.clientesDiv);			
			this.clientes.startup();	
			
			connect.connect(this.clientes, "onChange", lang.hitch(this, function() {
				this.onClienteChange();	
			}));	
			
			// Grid Pagos a Asociados con la Devolucion
			this.resetGridPagosValues();	

			// Grid Devoluciones con Saldo Pendiente
			this.resetGridSaldoPendiente();

			// Grid Facturas Asociadas a las Devoluciones con Saldo Pendiente
			this.resetGridSaldoPendienteFacturas();				

			// Load Clientes Filtro Saldo Pendiente
			this.loadClientes();

			// Load Empresas Filtro Saldo Pendiente
			if (this.empresasSaldoPendiente != null) {
				this.empresasSaldoPendiente.destroy();
				this.empresasSaldoPendiente = null;					
			}	

			var store = new Memory({
				data:  [{ "id": "", "label": "--SELECCIONAR--"}]
			});							
			    
			var os = new ObjectStore({ objectStore: store });
			this.empresasSaldoPendiente = new Select({
				id: "empresasSaldoPendiente",
				required: false,
		        store: os
		    });								
			
			this.empresasSaldoPendiente.placeAt(this.empresasSaldoPendienteDiv);			
			this.empresasSaldoPendiente.startup();			
			
		},
		
		onGridDevolucionesItemSelect: function() {
			var items = this.gridDevoluciones.selection.getSelected();
			if (items.length < 1)
				return;
			
			var devolucion = json.parse(this.gridDevoluciones.store.getValue(items[0], 'datos'));

			// Load facturas grid
			this.resetGridFacturasValues();
			array.forEach(devolucion.facturas, lang.hitch(this, function(factura) {
				this.gridFacturas.store.newItem(factura);
			}));	
			this.gridFacturas.render();
			
			// Set pago details
			this.fechaPagoCliente.innerHTML = this.formatDate(devolucion.pago.fechaPago);
			this.importePagoCliente.innerHTML = this.formatCurrency(devolucion.pago.importe);
			this.metodoPagoCliente.innerHTML = devolucion.pago.metodoPago;
			this.bancoPagoCliente.innerHTML = (devolucion.pago.banco == null ? "" : devolucion.pago.banco);
			this.referenciaPagoCliente.innerHTML = devolucion.pago.referencia;
			if (devolucion.pago.contentSize != null) {
				domStyle.set(this.comprobantePagoClientePane, "display", "block");
				this.comprobantePagoCliente = { "id": devolucion.pago.id, "name": devolucion.pago.documentTitle};
			} else {
				domStyle.set(this.comprobantePagoClientePane, "display", "none");
				this.comprobantePagoCliente = null;	
			}
			
			// Load importes
			var importeDevolucionProveedorProveedor = parseFloat(this.gridDevoluciones.store.getValue(items[0], 'devolucionSolicitadaProveedor'));
			this.importeDevolucionProveedor.innerHTML = this.formatCurrency(importeDevolucionProveedorProveedor);

			// Set opcion de devolucion directa al cliente del proveedor
			var devolucionDirecta = this.gridDevoluciones.store.getValue(items[0], 'devolucionDirecta');
			this.devolucionDirectaProveedor.setValue(devolucionDirecta);
			this.onDevolucionDirectaChange();
		},

		onGridSaldoPendienteItemSelect: function() {
			var items = this.gridSaldoPendiente.selection.getSelected();
			if (items.length < 1)
				return;
			
			var devolucion = json.parse(this.gridSaldoPendiente.store.getValue(items[0], 'datos'));
			
			// Load facturas asociadas grid
			this.resetGridSaldoPendienteFacturas();
			array.forEach(devolucion.facturas, lang.hitch(this, function(factura) {
				this.gridSaldoPendienteFacturas.store.newItem(factura);
			}));	
			this.gridSaldoPendienteFacturas.render();

			this.setTotalSaldoPendiente();		
		},

		sendDummyRequest: function() {

			var params = {};			
			Request.invokePluginService("ImportSaadPlugin", "configService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {	

						})
					}); 								
		},				
						
		loadProveedores: function() {							
			
			var params = {};
			params.method = "getSolDocProveedores";
			params.repositoryid = this.repositoryId;
			params.context = json.stringify(this.context);
			
			Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";													
								return;
							}
							
							var items = [];
							items.push({ "id": "", "label": "--SELECCIONAR--" });
							array.forEach(response.proveedores, lang.hitch(this, function(item) {
								items.push({ "id": item.id, "label": item.proveedorNombre });
							}));								
							
							var store = new Memory({
								data: items
							});							
						    
							var os = new ObjectStore({ objectStore: store });
							this.proveedores = new Select({
								id: "proveedores",
								required: false,
						        store: os
						    });								
							
							this.proveedores.placeAt(this.proveedoresDiv);			
							this.proveedores.startup();
							
							connect.connect(this.proveedores, "onChange", lang.hitch(this, function() {
								this.onProveedorChange();	
							}));					
													
						})
					}); 			
		},		
						
		onProveedorChange: function() {
			this.loadClientesDevolucion();
			this.loadSaldoProveedor();
			this.loadDevoluciones();
			this.loadEmpresas();
			this.onResetSaldoPendiente();		
		},
		
		onClienteChange: function() {
			var filter = "";
			var cliente = this.getSelectedCliente();
		
			if (cliente == null) {
				this.gridDevoluciones.filter({cliente: '*'});
				return;
			}
			
			filter = cliente.label;
			/*
			if (cliente.label.length > this.maxGridDescLength)
				filter = cliente.label.substr(0,this.maxGridDescLength) + "...";			
			*/
			
			this.gridDevoluciones.filter({cliente: filter});
		},
		
		onMetodoPagoChange: function() {
			domStyle.set(this.metodoPagoTransferenciaPane, "display", "none");
			domStyle.set(this.metodoPagoChequePane, "display", "none");
			domStyle.set(this.metodoEntregaEfectivoPane, "display", "none");
			domStyle.set(this.metodoPagoEfectivoPane, "display", "none");
			domStyle.set(this.metodoPagoTarjetaPane, "display", "none");
			var metodoPago = parseInt(this.metodoPagoProveedor.getValue());
			switch(metodoPago) {
				case 2: // transferencia
					domStyle.set(this.metodoPagoTransferenciaPane, "display", "block");
					break;
				case 1: // cheque
					domStyle.set(this.metodoPagoChequePane, "display", "block");
					break;
				case 0: // entrega en efectivo
					domStyle.set(this.metodoEntregaEfectivoPane, "display", "block");
					break;					
				case 3: // deposito en efectivo
					domStyle.set(this.metodoPagoEfectivoPane, "display", "block");
					break;	
				case 4: // pago a tarjeta
					domStyle.set(this.metodoPagoTarjetaPane, "display", "block");
					break;						
			}
		},			
		
		loadSaldoProveedor: function() {
			
			this.saldoProveedor.innerHTML = "0.00";
			var proveedor = this.getSelectedProveedor();
			
			if (proveedor == null)
				return;
						
			var params = {};
			params.method = "getSaldoProveedor";
			params.repositoryid = this.repositoryId;
			params.context = json.stringify(this.context);
			params.proveedor = json.stringify(proveedor);
			
			Request.invokePluginService("ImportSaadPlugin", "ImportSaadPagosService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";													
								return;
							}
							
							this.saldoProveedor.innerHTML = this.formatCurrency(response.saldo);						
						})
					}); 			
		},
		
		loadClientesDevolucion: function() {
					
			var proveedor = this.getSelectedProveedor();
			
			if (proveedor == null) {
				var items = [{ "id": "", "label": "--SELECCIONAR--"}];
				var store = new Memory({
					data: items
				});							
				var os = new ObjectStore({ objectStore: store });
				this.clientes.setStore(os);	
				return;
			}
						
			var params = {};
			params.method = "getClientesDevolucion";
			params.repositoryid = this.repositoryId;
			params.context = json.stringify(this.context);
			params.proveedor = json.stringify(proveedor);
			params.tipo = 0; // devolucion del proveedor
			
			Request.invokePluginService("ImportSaadPlugin", "ImportSaadPagosService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";													
								return;
							}
							
							var items = [];
							items.push({ "id": "", "label": "--SELECCIONAR--" });
							array.forEach(response.clientes, lang.hitch(this, function(item) {
								items.push(item);
							}));								
							
							var store = new Memory({
								data: items
							});							
						    
							var os = new ObjectStore({ objectStore: store });
							this.clientes.setStore(os);				
													
						})
					}); 			
		},

		loadClientes: function() {		

			// Inicializa select en caso de no existir
			if (this.clientesSaldoPendiente == null) {
				var items = [{ "id": "", "label": "--SELECCIONAR--"}];
												
				var store = new Memory({
					data: items
				});							
			    
				var os = new ObjectStore({ objectStore: store });
				this.clientesSaldoPendiente = new Select({
					id: "clientesSaldoPendiente",
					required: false,
			        store: os
			    });								
				
				this.clientesSaldoPendiente.placeAt(this.clientesSaldoPendienteDiv);			
				this.clientesSaldoPendiente.startup();	
			}							
			
			var params = {};
			params.method = "getSolDocClientes";
			params.repositoryid = this.repositoryId;
			params.context = json.stringify(this.context);
			
			Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";													
								return;
							}
							
							var items = [];
							items.push({ "id": "", "label": "--SELECCIONAR--", "rfc": "", "direccionFiscal": "", "porcentajeComisionProveedor": 0, "porcentajeComisionDistribuidor": 0});
							array.forEach(response.clientes, lang.hitch(this, function(item) {
								items.push({ "id": item.id, "label": item.razonSocial, "rfc": item.rfc, "direccionFiscal": item.direccionFiscal, "porcentajeComisionProveedor": item.porcentajeComisionProveedor, "porcentajeComisionDistribuidor": item.porcentajeComisionDistribuidor});
							}));

							var store = new Memory({
								data: items
							});							
						    
							var os = new ObjectStore({ objectStore: store });
							this.clientesSaldoPendiente.setStore(os);																	
							
							var store = new Memory({
								data: items
							});							
							
						})
					}); 			
		},		

		loadEmpresas: function() {

			var proveedor = this.getSelectedProveedor();

			if (proveedor == null) {
				var items = [{ "id": "", "label": "--SELECCIONAR--"}];
				var store = new Memory({
					data: items
				});							
				var os = new ObjectStore({ objectStore: store });
				this.empresasSaldoPendiente.setStore(os);	
				return;
			}		
			
			var params = {};
			params.method = "getSolDocEmpresas";
			params.repositoryid = this.repositoryId;
			params.proveedor = proveedor.label;
			params.context = json.stringify(this.context);
			
			Request.invokePluginService("ImportSaadPlugin", "ImportSaadPluginService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";													
								return;
							}
							
							var items = [];
							items.push({ "id": "", "label": "--SELECCIONAR--"});
							array.forEach(response.empresas, lang.hitch(this, function(item) {
								items.push({ "id": item.id, "label": item.name});
							}));								
							
							var store = new Memory({
								data: items
							});							
						    
							var os = new ObjectStore({ objectStore: store });
							this.empresasSaldoPendiente.setStore(os);									
							
						})
					}); 			
		},			
		
		loadDevoluciones: function() {

			this.resetGridDevolucionesValues();
			this.resetGridFacturasValues();			
			
			var proveedor = this.getSelectedProveedor();
			
			if (proveedor == null) {
				return;
			}

			var params = {};
			params.method = "searchDevoluciones";
			params.repositoryid = this.repositoryId;
			params.context = json.stringify(this.context);
			params.proveedor = json.stringify(proveedor);
			params.tipo = 0; // devolucion proveedor
			
			Request.invokePluginService("ImportSaadPlugin", "ImportSaadPagosService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";													
								return;
							}
							
							/*
							array.forEach(response.devoluciones, lang.hitch(this, function(devolucion) {
								if (devolucion.cliente.length > this.maxGridDescLength)
									devolucion.cliente = devolucion.cliente.substr(0,this.maxGridDescLength) + "...";
								if (devolucion.empresa.length > this.maxGridDescLength)
									devolucion.empresa = devolucion.empresa.substr(0,this.maxGridDescLength) + "...";							
							}));
							*/

							array.forEach(response.devoluciones, lang.hitch(this, function(devolucion) {
								this.gridDevoluciones.store.newItem(devolucion);
							}));	
							this.gridDevoluciones.render();										
													
						})
					});			
		},

		resetGridSaldoPendiente: function() {

			this.totalSaldoPendiente.innerHTML = "0.00";

			if (this.gridSaldoPendiente != null) {
				this.gridSaldoPendiente.destroy();
				this.gridSaldoPendiente = null;				
			}	
			
			var gridStore = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});				
		
			var gridLayout = [
				{name: 'Id', field: 'id', hidden: true},
				{name: 'Fecha', field: 'fechaSolicitud', width: '13%', editable: false, formatter: this.formatDate},
				{name: 'Importe', field: 'devolucionSolicitadaProveedor', width: '13%', editable: false, formatter: this.formatCurrency},
				{name: 'Saldo', field: 'saldo', width: '13%', editable: false, formatter: this.formatCurrency},
				{name: 'Cliente', field: 'cliente', width: '30%', editable: false},
				{name: 'Empresa', field: 'empresa', width: '30%', editable: false}				
			];
			
			this.gridSaldoPendiente = new DataGrid({
				id: 'gridSaldoPendiente',
				store: gridStore,
				structure: gridLayout,
				selectionMode: 'multi',
				sortInfo: 2, // asc by fecha solicitud
				autoHeight: true
			});

			this.gridSaldoPendiente.placeAt(this.gridSaldoPendienteDiv);			
			this.gridSaldoPendiente.startup();	

			connect.connect(this.gridSaldoPendiente, "onClick", lang.hitch(this, function() {
				this.onGridSaldoPendienteItemSelect();
			}));				
			
			connect.connect(this.gridSaldoPendiente, "onStyleRow", lang.hitch(this, function(row) {
				var item = this.gridSaldoPendiente.getItem( row.index );
				if ( item == null ) return;
				if (row.odd) row.customStyles += 'background-color:#F7F6F6;';
			}));				
			
			if (this.gridSaldoPendiente == null)
				return;
			
			var gridStore = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});				

		},

		resetGridSaldoPendienteFacturas: function() {

			if (this.gridSaldoPendienteFacturas != null) {
				this.gridSaldoPendienteFacturas.destroy();
				this.gridSaldoPendienteFacturas = null;				
			}	
			
			var gridStore = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});				
		
			var gridLayout = [
				{name: 'Id', field: 'id', hidden: true},
				{name: 'Folio', field: 'folio', width: '15%', editable: false},
				{name: 'Fecha Factura', field: 'fechaFactura', width: '15%', editable: false, formatter: this.formatDate},
				{name: 'Número Factura', field: 'numeroFactura', width: '15%', editable: false},
				{name: 'Importe Factura', field: 'importe', width: '15%', editable: false, formatter: this.formatCurrency},
				{name: '%', field: 'porcentajeComisionProveedor', width: '5%', editable: false, formatter: this.formatCurrency},
				{name: 'Comisión Proveedor', field: 'montoComisionProveedor', width: '15%', editable: false, formatter: this.formatCurrency},
				{name: '%', field: 'porcentajeComisionDistribuidor', width: '5%', editable: false, formatter: this.formatCurrency},
				{name: 'Comisión Distribuidor', field: 'montoComisionDistribuidor', width: '15%', editable: false, formatter: this.formatCurrency}									
			];
			
			this.gridSaldoPendienteFacturas = new DataGrid({
				id: 'gridSaldoPendienteFacturas',
				store: gridStore,
				structure: gridLayout,
				selectionMode: 'single',
				sortInfo: 2, // asc by folio
				autoHeight: true
			});

			this.gridSaldoPendienteFacturas.placeAt(this.gridSaldoPendienteFacturasDiv);			
			this.gridSaldoPendienteFacturas.startup();	
			
			connect.connect(this.gridSaldoPendienteFacturas, "onStyleRow", lang.hitch(this, function(row) {
				var item = this.gridSaldoPendienteFacturas.getItem( row.index );
				if ( item == null ) return;
				if (row.odd) row.customStyles += 'background-color:#F7F6F6;';
			}));	

		},		
				
		resetGridDevolucionesValues: function() {
			
			this.importeDevolucionProveedor.innerHTML = "0.00";

			if (this.gridDevoluciones != null) {
				this.gridDevoluciones.destroy();
				this.gridDevoluciones = null;				
			}	
			
			var gridStore = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});				
		
			var gridLayout = [
				{name: 'Id', field: 'id', hidden: true},
				{name: 'Fecha', field: 'fechaSolicitud', width: '20%', editable: false, formatter: this.formatDate},
				{name: 'Importe', field: 'devolucionSolicitadaProveedor', width: '20%', editable: false, formatter: this.formatCurrency},
				{name: 'Cliente', field: 'cliente', width: '30%', editable: false},
				{name: 'Empresa', field: 'empresa', width: '30%', editable: false}				
			];
			
			this.gridDevoluciones = new DataGrid({
				id: 'gridDevoluciones',
				store: gridStore,
				structure: gridLayout,
				selectionMode: 'single',
				sortInfo: 2, // asc by fecha solicitud
				autoHeight: true
			});

			this.gridDevoluciones.placeAt(this.gridDevolucionesDiv);			
			this.gridDevoluciones.startup();	
			
			connect.connect(this.gridDevoluciones, "onClick", lang.hitch(this, function() {
				this.onGridDevolucionesItemSelect();
			}));				
			
			connect.connect(this.gridDevoluciones, "onStyleRow", lang.hitch(this, function(row) {
				var item = this.gridDevoluciones.getItem( row.index );
				if ( item == null ) return;
				if (row.odd) row.customStyles += 'background-color:#F7F6F6;';
			}));				

		},					
				
		resetGridFacturasValues: function() {

			if (this.gridFacturas != null) {
				this.gridFacturas.destroy();
				this.gridFacturas = null;				
			}	
			
			var gridStore = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});				
		
			var gridLayout = [
				{name: 'Id', field: 'id', hidden: true},
				{name: 'Folio', field: 'folio', width: '15%', editable: false},
				{name: 'Fecha Factura', field: 'fechaFactura', width: '15%', editable: false, formatter: this.formatDate},
				{name: 'Número Factura', field: 'numeroFactura', width: '15%', editable: false},
				{name: 'Importe Factura', field: 'importe', width: '15%', editable: false, formatter: this.formatCurrency},
				{name: '%', field: 'porcentajeComisionProveedor', width: '5%', editable: false, formatter: this.formatCurrency},
				{name: 'Comisión Proveedor', field: 'montoComisionProveedor', width: '15%', editable: false, formatter: this.formatCurrency},
				{name: '%', field: 'porcentajeComisionDistribuidor', width: '5%', editable: false, formatter: this.formatCurrency},
				{name: 'Comisión Distribuidor', field: 'montoComisionDistribuidor', width: '15%', editable: false, formatter: this.formatCurrency}									
			];
			
			this.gridFacturas = new DataGrid({
				id: 'gridFacturas',
				store: gridStore,
				structure: gridLayout,
				selectionMode: 'single',
				sortInfo: 2, // asc by folio
				autoHeight: true
			});

			this.gridFacturas.placeAt(this.gridFacturasDiv);			
			this.gridFacturas.startup();	
					
			connect.connect(this.gridFacturas, "onStyleRow", lang.hitch(this, function(row) {
				var item = this.gridFacturas.getItem( row.index );
				if ( item == null ) return;
				if (row.odd) row.customStyles += 'background-color:#F7F6F6;';
			}));				

		},	
		
		resetGridPagosValues: function() {
			
			this.importeTotalPagos.innerHTML = "0.00";
			
			if (this.gridPagos != null) {
				this.gridPagos.destroy();
				this.gridPagos = null;				
			}	
			
			var gridStore = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});				
		
			var gridLayout = [
			  	{name: 'Id', field: 'id', hidden: true},
			  	{name: 'Fecha de Pago', field: 'fechaPago', width: '25%', editable: false, formatter: this.formatDate},
			  	{name: 'Importe', field: 'importe', width: '25%', editable: false, formatter: this.formatCurrency},			
			  	{name: 'Método de Pago', field: 'metodoPago', width: '25%', editable: false, formatter: this.formatMetodoPago},
			  	{name: 'Referencia', field: 'referencia', width: '25%', editable: false},
			  	{name: 'Ver', field: 'view', width: '5%', editable: false, formatter: this.formatViewFileButton}
			];
			
			this.gridPagos = new DataGrid({
				id: 'gridPagos',
				store: gridStore,
				structure: gridLayout,
				selectionMode: 'multi',
				sortInfo: 2, // asc by fechaPago
				autoHeight: true,
				formatterScope: this
			});

			this.gridPagos.placeAt(this.gridPagosDiv);			
			this.gridPagos.startup();	
			
			connect.connect(this.gridPagos, "onStyleRow", lang.hitch(this, function(row) {
				var item = this.gridPagos.getItem( row.index );
				if ( item == null ) return;
				if (row.odd) row.customStyles += 'background-color:#F7F6F6;';
			}));	

		},			
						
		onReset: function() {
			this.onAfterSave();
			if (this.proveedores != null) {
				this.saldoProveedor.innerHTML = "0.00";
				this.proveedores.reset();
			}
			if (this.clientes != null)
				this.clientes.reset();	
			this.tipoOperacion = 0;
			this.message.innerHTML = "";
			this.progressBar.style.display = "none";					
		},

		onResetSaldoPendiente: function() {
			this.resetGridSaldoPendiente();
			this.resetGridSaldoPendienteFacturas();	
			this.setTotalSaldoPendiente();
		},
		
		onAfterSave: function() {
			this.onPagoAfterAction();
			this.fechaPagoCliente.innerHTML = "";	
			this.importePagoCliente.innerHTML = "";	
			this.metodoPagoCliente.innerHTML = "";	
			this.bancoPagoCliente.innerHTML = "";	
			this.referenciaPagoCliente.innerHTML = "";	
			this.resetGridDevolucionesValues();
			this.resetGridFacturasValues();
			this.resetGridPagosValues();
			this.resetGridSaldoPendiente();
			this.resetGridSaldoPendienteFacturas();
			this.setTotalSaldoPendiente();
			this.comprobantePagoCliente = null;
			this.observaciones.reset();
			if (this.clientesSaldoPendiente != null)
				this.clientesSaldoPendiente.reset();
			if (this.empresasSaldoPendiente != null)
				this.empresasSaldoPendiente.reset();
			this.fechaSolicitudDesde.reset();
			this.fechaSolicitudHasta.reset();
			this.devolucionDirectaProveedor.reset();
			this.omitirEnvioCorreo.reset();
			this.omitirEnvioCorreo.set("disabled", true);			
			this.importeDevolucionProveedor.innerHTML = "0.00";
			domStyle.set(this.confirmacionPane, "display", "none");
			domStyle.set(this.comprobantePagoClientePane, "display", "none");			
		},
		
		onPagoAfterAction: function() {			
			this.fechaPagoProveedor.setValue(new Date());	
			this.importePagoProveedor.reset();
			this.metodoPagoProveedor.reset();
			this.bancoChequePagoProveedor.reset();
			this.bancoDepositoPagoProveedor.reset();
			this.bancoTarjetaPagoProveedor.reset();
			this.referenciaTransferenciaPagoProveedor.reset();
			this.referenciaDepositoPagoProveedor.reset();
			this.referenciaTarjetaPagoProveedor.reset();
			this.nombrePersonaPagoProveedor.reset();
			this.onMetodoPagoChange();
			this.onFileDelete();	
			this.setTotalPagos();
		},
		
		setTotalPagos: function() {
			var importeTotalPagos = 0;
			if (this.gridPagos != null) {
				for (var i = 0; i < this.gridPagos.rowCount; i++) {
					var item = this.gridPagos.getItem(i);
					importeTotalPagos += parseFloat(this.gridPagos.store.getValue(item, 'importe'));
				}
			}
			this.importeTotalPagos.innerHTML = this.formatCurrency(importeTotalPagos);		
		},

		setTotalSaldoPendiente: function() {
			var totalSaldoPendiente = 0;
			if (this.gridSaldoPendiente != null) {
				var items = this.gridSaldoPendiente.selection.getSelected();
				array.forEach(items, lang.hitch(this, function(item) {
					totalSaldoPendiente += parseFloat(this.gridSaldoPendiente.store.getValue(item, 'saldo'));
				}));
			}
			this.totalSaldoPendiente.innerHTML = this.formatCurrency(totalSaldoPendiente);	
		},			
		
		validaDatosComprobante: function() {
			
			// Ocultar panel de confirmacion
			domStyle.set(this.confirmacionPane, "display", "none");
			this.confirmationMessage.innerHTML = "";
			this.message.innerHTML = "";			
			
			if (!this.fechaPagoProveedor.isValid() || !this.importePagoProveedor.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "La fecha o el importe del comprobante de pago no son inválidos.";
				return false;	
			}		
			
			var metodoPago = parseInt(this.metodoPagoProveedor.getValue());
			switch(metodoPago) {
				case 2: // transferencia
					if (!this.referenciaTransferenciaPagoProveedor.isValid()) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Número de referencia inválida.";
						return false;						
					}
					break;
				case 1: // cheque
					if (!this.numeroChequePagoProveedor.isValid() || !this.bancoChequePagoProveedor.isValid()) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Banco o número de cheque inválido.";
						return false;						
					}
					break;
				case 0: // entrega en efectivo
					if (!this.nombrePersonaPagoProveedor.isValid()) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Nombre de la persona que entrega el efectivo inválido.";
						return false;						
					}
					break;					
				case 3: // deposito en efectivo
					if (!this.referenciaDepositoPagoProveedor.isValid() || !this.bancoDepositoPagoProveedor.isValid()) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Banco o número de referencia inválido.";
						return false;					
					}
					break;		
				case 4: // pago a tarjeta
					if (!this.referenciaTarjetaPagoProveedor.isValid() || !this.bancoTarjetaPagoProveedor.isValid()) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Banco o número de tarjeta inválido.";
						return false;					
					}
					break;						
			}
			
			return true;
		},	
		
		validaDevolucionSeleccionada: function() {
			
			// Ocultar panel de confirmacion
			domStyle.set(this.confirmacionPane, "display", "none");
			this.confirmationMessage.innerHTML = "";
			this.message.innerHTML = "";					

			// Valida seleccion de devolucion
			var items = this.gridDevoluciones.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "Es nesesario seleccionar al menos una devolución.";
				return false;				
			}
			
			return true;
			
		},
		
		validaGuardar: function() {
			
			// Ocultar panel de confirmacion
			domStyle.set(this.confirmacionPane, "display", "none");
			this.confirmationMessage.innerHTML = "";
			this.message.innerHTML = "";			
			
			// Valida seleccion del proveedor
			if (this.proveedores.getValue() == "") {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "Es necesario seleccionar un proveedor.";
				return false;				
			}

			// Si no existe una devolucion seleccionada se asume que se registrara un pago(s) asociados a una devolucion con saldo pendiente
			if (this.gridDevoluciones.selection.getSelected().length == 0) 
			{
				if (this.gridPagos.rowCount == 0 || this.gridSaldoPendiente.selection.getSelected().length == 0) {
					domStyle.set(this.message, "color", "#6B0A0A");
					this.message.innerHTML = "Al no seleccionar una devolución se asume que se asociará un pago a una devolución con saldo pendiente, por lo tanto, es necesario registrar por lo menos un pago y seleccionar por lo menos una devolución con saldo pendiente.";
					return false;					
				}
				// Valida que el registro del pago vaya de acurdo a los saldos pendientes seleccionados
				var totalPagos = this.parseFloat(this.importeTotalPagos.innerHTML);
				var totalSaldoPendiente = this.parseFloat(this.totalSaldoPendiente.innerHTML);
				if (totalPagos > 0) { // pago del cliente al distribuidor
					if (totalSaldoPendiente > 0) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "El importe total de los pagos corresponde a un pago del distribuidor al proveedor, por lo tanto, las devoluciones con saldo pendiente a seleecionar deben de tener saldo en contra.";
						return false;	
					}
					if (totalPagos > (totalSaldoPendiente * -1)) {	
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "El importe total de los pagos no puede ser mayor al total de saldo pendiente seleccionado.";
						return false;									
					}
				} else { // pago del distribuidor al cliente
					if (totalSaldoPendiente < 0) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "El importe total de los pagos corresponde a un pago del proveedor al distribuidor, por lo tanto, las devoluciones con saldo pendiente a seleecionar deben de tener saldo a favor.";
						return false;	
					}
					if (totalPagos < (totalSaldoPendiente * -1)) {	
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "El importe total de los pagos no puede ser mayor al total de saldo pendiente seleccionado.";
						return false;									
					}
				}
				// Para el pago de devoluciones con saldo pendiente se valida todas las devoluciones selccionadas sean del mismo proveedor, cliente y empresa
				var items = this.gridSaldoPendiente.selection.getSelected();
				var isValid = true;
				var firstProveedor = null;
				var firstCliente = null;
				var firstEmpresa = null;				
				array.forEach(items, lang.hitch(this, function(item) {
					if (firstProveedor != null && firstCliente != null && firstEmpresa != null) {
						if (isValid && (this.gridSaldoPendiente.store.getValue(item, 'proveedor') != firstProveedor || this.gridSaldoPendiente.store.getValue(item, 'cliente') != firstCliente || this.gridSaldoPendiente.store.getValue(item, 'empresa') != firstEmpresa))
							isValid = false;
					} else {	
						firstProveedor = this.gridSaldoPendiente.store.getValue(item, 'proveedor');
						firstCliente = this.gridSaldoPendiente.store.getValue(item, 'cliente');
						firstEmpresa = this.gridSaldoPendiente.store.getValue(item, 'empresa');
					}
				}));
				if (!isValid) {
					domStyle.set(this.message, "color", "#6B0A0A");
					this.message.innerHTML = "Para asociar pagos a devoluciones con saldo pendientes, todas las devoluciones seleccionadas deben de corresponder al mismo proveedor, cliente y empresa.";
					return false;
				}									
			}									
			else
			{						

				// Valida seleccion de devolucion
				var items = this.gridDevoluciones.selection.getSelected();
				if (items.length <= 0) {
					domStyle.set(this.message, "color", "#6B0A0A");
					this.message.innerHTML = "Es nesesario seleccionar una devolución para asociar el comprobante de pago.";
					return false;				
				}

				// Valida ingreso de pago o seleccion de devolucion con saldo pendiente
				if (this.gridPagos.rowCount == 0 && this.gridSaldoPendiente.selection.getSelected().length == 0) {
					domStyle.set(this.message, "color", "#6B0A0A");
					this.message.innerHTML = "Es nesesario ingresar al menos un registro de pago o seleccionar una devolución con saldo pendiente.";
					return false;					
				}

				// Valida importe de la devolucion vs importe de los comprobantes de pago 
				/*
				if (this.parseFloat(this.importeDevolucionProveedor.innerHTML) != this.parseFloat(this.importeTotalPagos.innerHTML)) {
					domStyle.set(this.message, "color", "#6B0A0A");
					this.message.innerHTML = "El importe de la devolución seleccionada no corresponde al importe de los comprobantes de pago a registrados.";
					return false;					
				}	
				*/

			}
						
			return true;
		},
		
		onCancelar: function() {
			// Ocular panel de confirmacion
			domStyle.set(this.confirmacionPane, "display", "none");
			this.confirmationMessage.innerHTML = "";
		},
		
		onConfirmar: function() {
			
			switch (this.tipoOperacion) {
			
				case 1: // guardar
					
					if (!this.validaGuardar())
						return;		
										
					domStyle.set(this.message, "color", "#000253");
					this.message.innerHTML = "Asociando pagos a devolución...";
					this.progressBar.style.display = "block";			

					var params = {};
					params.method = "savePagosDeProveedor";
					params.repositoryid = this.repositoryId;
					params.context = json.stringify(this.context);

					// Tipo de Transaccion
					if (this.gridDevoluciones.selection.getSelected().length > 0)
						params.tipo	= 0; // registro de devolucion de proveedor
					else
						params.tipo = 1; // registro de pagos a devoluciones con saldo pendiente					

					// Pagos
					var pagos = [];
					for (var i = 0; i < this.gridPagos.rowCount; i++) {
						var item = this.gridPagos.getItem(i);
						var pago = {"id": this.gridPagos.store.getValue(item, 'id')};
						pagos.push(pago);
					}	
					params.pagos = json.stringify(pagos);	

					// Devoluciones con Saldo Pendiente por aplicar
					var devolucionesSaldoPendiente = [];
					var items = this.gridSaldoPendiente.selection.getSelected();
					array.forEach(items, lang.hitch(this, function(item) {
						var devolucion = {"id": this.gridSaldoPendiente.store.getValue(item, 'id')};
						devolucionesSaldoPendiente.push(devolucion);
					}));	
					params.devolucionesSaldoPendiente = json.stringify(devolucionesSaldoPendiente);	

					if (params.tipo == 0) { // registro de devolucion de proveedor					

					// Get Saldos de la Devolucion
					params.saldoProveedor = this.getSaldoDevolucionProveedor();
					if (this.devolucionDirectaProveedor.getValue() === "true")
						params.saldoDistribuidor = this.getSaldoDevolucionDistribuidor();

						// ¿Devolucion directa al cliente?
						var devolucionDirecta = {
							"aplica": (this.devolucionDirectaProveedor.getValue() === "true"),
							"omitirNotificacion": (this.omitirEnvioCorreo.getValue() === "true"),
							"observaciones": this.observaciones.getValue()
						};
						params.devolucionDirecta = json.stringify(devolucionDirecta);		
						
						// Devolucion
						var items = this.gridDevoluciones.selection.getSelected();
						var devolucion = {"id": this.gridDevoluciones.store.getValue(items[0], 'id')};
						params.devolucion = json.stringify(devolucion);	

					} else if (params.tipo = 1) { // registro de pagos a devoluciones con saldo pendiente

						var totalPagos = this.parseFloat(this.importeTotalPagos.innerHTML);
						var totalSaldoPendiente = this.parseFloat(this.totalSaldoPendiente.innerHTML);
						params.diferenciaSaldoPendiente = totalPagos - (totalSaldoPendiente * -1);

					}						

					Request.invokePluginService("ImportSaadPlugin", "ImportSaadPagosService",
							{	
								requestParams: params,
								requestCompleteCallback: lang.hitch(this, function(response) {	
									
									if (response.status == 1) { // error en transaccion
										domStyle.set(this.message, "color", "#6B0A0A");
										this.message.innerHTML = response.error;
										this.progressBar.style.display = "none";						
										return;
									}
									
									if (response.status == 2) // error en envio de notificacion
									{
										domStyle.set(this.message, "color", "#000253");
										this.message.innerHTML = "Los pagos se asociaron correctamente, sin embargo, ocurrió un error al intentar enviar la notificación de devolución al cliente. " + response.error;
										this.progressBar.style.display = "none";
									} 
									else
									{
										domStyle.set(this.message, "color", "#000253");
										this.message.innerHTML = "Los pagos se asociaron correctamente a la devolución del proveedor.";
										this.progressBar.style.display = "none";
									}													
									
									this.onAfterSave();
									this.loadSaldoProveedor();
									this.loadDevoluciones();
								})
							}); 
					break;
					
				case 2: // enviar solicitud de devolucion
					
					if (!this.validaDevolucionSeleccionada())
						return;	
					
					domStyle.set(this.message, "color", "#000253");
					this.message.innerHTML = "Enviando correo de solicitud de devolución...";
					this.progressBar.style.display = "block";			
			
					var params = {};
					params.method = "notificaDevolucionProveedor";
					params.repositoryid = this.repositoryId;
					params.context = json.stringify(this.context);
					
					// Devolucion
					var items = this.gridDevoluciones.selection.getSelected();
					var devolucion = {"id": this.gridDevoluciones.store.getValue(items[0], 'id')};
					params.devolucion = json.stringify(devolucion);
					
					// Observaciones
					params.observaciones = this.observaciones.getValue();

					Request.invokePluginService("ImportSaadPlugin", "ImportSaadPagosService",
							{	
								requestParams: params,
								requestCompleteCallback: lang.hitch(this, function(response) {	
									
									if (response.error != null) {
										domStyle.set(this.message, "color", "#6B0A0A");
										this.message.innerHTML = response.error;
										this.progressBar.style.display = "none";						
										return;
									}
									
									domStyle.set(this.message, "color", "#000253");
									this.message.innerHTML = "La notificación se solicitud de devolución al proveedor ha sido enviada.";
									this.progressBar.style.display = "none";							
									
								})
							});
					
					break;
					
				case 3: // eliminar devolucion
					
					if (!this.validaDevolucionSeleccionada())
						return;		
					
					domStyle.set(this.message, "color", "#000253");
					this.message.innerHTML = "Eliminando devolución...";
					this.progressBar.style.display = "block";			
			
					var params = {};
					params.method = "deleteDevolucion";
					params.repositoryid = this.repositoryId;
					params.context = json.stringify(this.context);
					params.tipo = 0; // proveedor
					
					// Devolucion
					var items = this.gridDevoluciones.selection.getSelected();
					var devolucion = {"id": this.gridDevoluciones.store.getValue(items[0], 'id')};
					params.devolucion = json.stringify(devolucion);

					Request.invokePluginService("ImportSaadPlugin", "ImportSaadPagosService",
							{	
								requestParams: params,
								requestCompleteCallback: lang.hitch(this, function(response) {	
									
									if (response.error != null) {
										domStyle.set(this.message, "color", "#6B0A0A");
										this.message.innerHTML = response.error;
										this.progressBar.style.display = "none";						
										return;
									}
									
									domStyle.set(this.message, "color", "#000253");
									this.message.innerHTML = "La devolución y el pago del cliente asociado han sido eliminados.";
									this.progressBar.style.display = "none";	
									
									this.onAfterSave();	
									this.loadSaldoProveedor();
									this.loadDevoluciones();									
									
								})
							});					
					
					break;					
			
			}
			    	
		},		

		getSaldoDevolucionProveedor: function() {
			var saldoDevolucion = 0;
			var montoDevolucion = this.parseFloat(this.importeDevolucionProveedor.innerHTML);
			var montoPagos = this.parseFloat(this.importeTotalPagos.innerHTML);
			var montoSaldoPendiente = this.parseFloat(this.totalSaldoPendiente.innerHTML);
			saldoDevolucion = montoPagos - montoSaldoPendiente;
			saldoDevolucion = saldoDevolucion - montoDevolucion;
			saldoDevolucion = saldoDevolucion * -1;
			return saldoDevolucion;
		},

		getSaldoDevolucionDistribuidor: function() {
			var saldoDevolucion = 0;
			var montoDevolucion = this.getMontoDevolucionDistribuidor();
			var montoPagos = this.parseFloat(this.importeTotalPagos.innerHTML);
			saldoDevolucion = montoPagos - montoDevolucion;
			saldoDevolucion = saldoDevolucion * -1;
			return saldoDevolucion;
		},	

		getMontoDevolucionDistribuidor: function() {
			var montoDevolucionDistribuidor = 0;
			var importeFacturas = 0;
			var montoComisionDistribuidor = 0;
			for (var i = 0; i < this.gridFacturas.rowCount; i++) {
				var item = this.gridFacturas.getItem(i);
				importeFacturas += parseFloat(this.gridFacturas.store.getValue(item, 'importe'));
				montoComisionDistribuidor += parseFloat(this.gridFacturas.store.getValue(item, 'montoComisionDistribuidor'));
			}
			montoDevolucionDistribuidor = importeFacturas - montoComisionDistribuidor;
			return montoDevolucionDistribuidor;	
		},					
		
		onSave: function() {
			
			this.tipoOperacion = 1; // guardar
				
			if (!this.validaGuardar())
				return false;

			var proveedor = this.getSelectedProveedor();

			// Mostrar panel de confirmacion
			this.confirmationMessage.innerHTML = "";

			// Si no existe una devolucion seleccionada se asume que se registrara un pago(s) asociados a una devolucion con saldo pendiente
			if (this.gridDevoluciones.selection.getSelected().length == 0) {	

				var totalPagos = this.parseFloat(this.importeTotalPagos.innerHTML);
				var totalSaldoPendiente = this.parseFloat(this.totalSaldoPendiente.innerHTML);
				var saldoPendiente = totalPagos - (totalSaldoPendiente * -1);
				if (saldoPendiente != 0)
					this.confirmationMessage.innerHTML += "La asociación de los pagos a la devolución con saldo pendiente dejará una saldo pendiente de <u>" + this.formatCurrency(saldoPendiente) + "</u> para el proveedor.<br>"						

				this.confirmationMessage.innerHTML += "¿Está seguro de aplicar los pagos a la devolución con saldo pendiente seleccionada?";				
			}
			else
			{				
				if (this.devolucionDirectaProveedor.getValue() === "true")
					this.confirmationMessage.innerHTML += "Ésta devolución será enviada directamente al cliente.<br>"			

				var saldoProveedor = this.getSaldoDevolucionProveedor();
				if (saldoProveedor != 0)
					this.confirmationMessage.innerHTML += "El registro de ésta devolución generará un saldo de <u>" + this.formatCurrency(saldoProveedor) + "</u> para el proveedor.<br>"

				if (this.devolucionDirectaProveedor.getValue() === "true") {
					var saldoDistribuidor = this.getSaldoDevolucionDistribuidor();
					if (saldoDistribuidor != 0)
						this.confirmationMessage.innerHTML += "El registro de ésta devolución generará un saldo de <u>" + this.formatCurrency(saldoDistribuidor) + "</u> para el cliente.<br>"		
				}

				this.confirmationMessage.innerHTML += "¿Está seguro de aplicar los pagos y/o saldos pendientes a la devolución seleccionada?";
			}

			domStyle.set(this.confirmacionPane, "display", "block");			
						
		},
		
		onSend: function() {
			
			this.tipoOperacion = 2; // enviar solicitud de devolucion
			
			if (!this.validaDevolucionSeleccionada())
				return false;

			// Mostrar panel de confirmacion
			this.confirmationMessage.innerHTML = "¿Está seguro de volver a solicitar la devolución seleccionada al proveedor?";
			domStyle.set(this.confirmacionPane, "display", "block");
			
		},
		
		onDelete: function() {
			
			this.tipoOperacion = 3; // eliminar devolucion
			
			if (!this.validaDevolucionSeleccionada())
				return false;
			
			// Mostrar panel de confirmacion
			this.confirmationMessage.innerHTML = "¿Está seguro de eliminar la devolución seleccionada y el pago del cliente asociado a la devolución?";
			domStyle.set(this.confirmacionPane, "display", "block");			
			
		},		
		
		onPagoProveedorAdd: function() {

			if (!this.validaDatosComprobante())
				return;
								
			domStyle.set(this.message, "color", "#000253");
			this.message.innerHTML = "Registrando Pago...";
			this.progressBar.style.display = "block";			

			var params = {};
			params.method = "addPago";
			params.tipo = 1;  // pago de proveedor
			params.repositoryid = this.repositoryId;
			params.context = json.stringify(this.context);
			var pago = {"fechaPago": this.getFormatedDateValue(this.fechaPagoProveedor.getValue()), "importe": parseFloat(this.importePagoProveedor.getValue()), "metodoPago": parseInt(this.metodoPagoProveedor.getValue()), comprobantePago: this.comprobantePagoProveedor};
			var metodoPago = parseInt(this.metodoPagoProveedor.getValue());
			switch(metodoPago) {
				case 2: // transferencia
					pago.referencia = this.referenciaTransferenciaPagoProveedor.getValue();
					break;
				case 1: // cheque
					pago.referencia = this.numeroChequePagoProveedor.getValue();
					pago.banco = parseInt(this.bancoChequePagoProveedor.getValue());
					break;
				case 0: // entrega en efectivo
					pago.referencia = this.nombrePersonaPagoProveedor.getValue();
					break;							
				case 3: // deposito en efectivo
					pago.referencia = this.referenciaDepositoPagoProveedor.getValue();
					pago.banco = parseInt(this.bancoDepositoPagoProveedor.getValue());
					break;	
				case 4: // pago a tarjeta
					pago.referencia = this.referenciaTarjetaPagoProveedor.getValue();
					pago.banco = parseInt(this.bancoTarjetaPagoProveedor.getValue());
					break;								
			}
			params.pago = json.stringify(pago);
			
			Request.invokePluginService("ImportSaadPlugin", "ImportSaadPagosService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {	
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";						
								return;
							}
							
							domStyle.set(this.message, "color", "#000253");
							this.message.innerHTML = "";
							this.progressBar.style.display = "none";							
							
							this.gridPagos.store.newItem(response.pago);
							this.gridPagos.render();
							
							this.onPagoAfterAction();								
						})
					}); 			
		},
		
		onPagoProveedorDelete: function() {
			
			var items = this.gridPagos.selection.getSelected();		
			if (items.length <= 0)
				return;		
			
			domStyle.set(this.message, "color", "#000253");
			this.message.innerHTML = "Eliminando Pagos...";
			this.progressBar.style.display = "block";						
					
			var pagos = [];
			array.forEach(items, lang.hitch(this, function(item) {
				var pago = {"id": this.gridPagos.store.getValue(item, 'id')};
				pagos.push(pago);
			}));						
			
			var params = {};
			params.method = "deletePago";
			params.repositoryid = this.repositoryId;
			params.context = json.stringify(this.context);
			params.pagos = json.stringify(pagos);
			
			Request.invokePluginService("ImportSaadPlugin", "ImportSaadPagosService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {	
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";						
								return;
							}
							
							domStyle.set(this.message, "color", "#000253");
							this.message.innerHTML = "";
							this.progressBar.style.display = "none";							
														
							
							array.forEach(items, lang.hitch(this, function(item) {
								this.gridPagos.store.deleteItem(item);
							}));		
							this.gridPagos.render();
							
							this.onPagoAfterAction();																
						})
					}); 			
			
		},
		
		onPagoProveedorView: function() {
			if (this.comprobantePagoProveedor != null)
				this.showDocument(this.comprobantePagoProveedor.id);
		},		
		
		onPagoClienteView: function() {
			if (this.comprobantePagoCliente != null)
				this.showDocument(this.comprobantePagoCliente.id);
		},
		
		onFileDelete: function() {
			this.fileName.innerHTML = "";
			this.comprobantePagoProveedor = null;			
			this.checkFileButtons();	
		},		
		
		onFileAdd: function() {
			
			domStyle.set(this.message, "color", "#000253");
			this.message.innerHTML = "Anexando comprobante...";
			this.progressBar.style.display = "block";				
						
			var callback = lang.hitch(this, this._onFileAddCompleted);
			
			var params = {};
			params.method = "addDocument";
			params.repositoryid = this.repositoryId;
			params.context = json.stringify(this.context);
			params.parentFolder = "/Temporal/Documentos";	
			
			// HTML5 browser
			if (this._fileInput.files) {
				var file = this._fileInput.files[0];
				params.mimetype = file.type;
				params.parm_part_filename = (file.fileName ? file.fileName : file.name)
				//params.max_file_size = this._maxFileSize.toString();

				var form = new FormData();
				form.append("file", file);
				
				Request.postFormToPluginService("ImportSaadPlugin", "ImportSaadPluginService", form, {
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
				params.action = "ImportSaadPluginService";
				
				Request.ieFileUploadServiceAPI("plugin", "", {requestParams: params, 
					requestCompleteCallback: callback
				}, this._fileInputForm);
			}			
			
		},
		
		_onFileAddCompleted: function(response) {				
		
			this._fileInput.value = "";
			
			if (response.error != null) {
				this.message.innerHTML = response.error;
				this.progressBar.style.display = "none";													
				return;				
			}
			
			this.comprobantePagoProveedor = { "id": response.document.id, "name": response.document.documentTitle};
			this.fileName.innerHTML = response.document.documentTitle;
									
			this.checkFileButtons();
			
			this.message.innerHTML = ""
			this.progressBar.style.display = "none";				
		},		
		
		showDocument: function(docId) {

			domStyle.set(this.message, "color", "#000253");
			this.message.innerHTML = "Abriendo Archivo...";
			this.progressBar.style.display = "block";
			
			var _repository = ecm.model.desktop.getRepository(ecm.model.desktop.defaultRepositoryId);
			if (!_repository.connected) {
				_repository.userId = this.context.usuario;
				_repository.logon(this.context.contrasena, lang.hitch(this, function(msg) {
					this._showItem(_repository, docId);
				}));						
			} else {
				this._showItem(_repository, docId);
			}

		},
		
		_showItem: function(repository, docId) {
			repository.retrieveItem(docId, lang.hitch(this, function(item) {
				var viewer = Desktop.getViewerForItem(item);
				if(!viewer) {
					domStyle.set(this.message, "color", "#6B0A0A");
					this.message.innerHTML = "No se pudo obtener el visor correspondiente del elemento seleccionado.";
					this.progressBar.style.display = "none";
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
				
				this.message.innerHTML = "";
				this.progressBar.style.display = "none";	
				
			}));
		},	

		onConsultarDevolucionesSaldo: function() {

			this.resetGridSaldoPendiente();

			var proveedor = this.getSelectedProveedor();
			
			if (proveedor == null) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "Es necesario seleccionar un proveedor";				
				return;
			}

			if (!this.fechaSolicitudDesde.isValid() || !this.fechaSolicitudHasta.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "Las fechas son inválidas";		
				return;			
			}

			domStyle.set(this.message, "color", "#000253");
			this.message.innerHTML = "Consultando devoluciones con saldo pendiente...";
			this.progressBar.style.display = "block";			

			var params = {};
			params.method = "searchDevolucionesSaldoPendiente";
			params.repositoryid = this.repositoryId;
			params.context = json.stringify(this.context);
			params.proveedor = json.stringify(proveedor);
			params.tipo = 0; // devolucion proveedor

			var cliente = this.getSelectedClienteSaldoPendiente();
			if (cliente != null)
				params.cliente = json.stringify(cliente);

			var empresa = this.getSelectedEmpresaSaldoPendiente();
			if (empresa != null)
				params.empresa = json.stringify(empresa);

			if (this.fechaSolicitudDesde.getValue() != "")
				params.fechaSolicitudDesde = this.getFormatedDateValue(this.fechaSolicitudDesde.getValue());

			if (this.fechaSolicitudHasta.getValue() != "")
				params.fechaSolicitudHasta = this.getFormatedDateValue(this.fechaSolicitudHasta.getValue());			
			
			Request.invokePluginService("ImportSaadPlugin", "ImportSaadPagosService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";													
								return;
							}

							if (response.devoluciones.length == 0) {
								domStyle.set(this.message, "color", "#6B0A0A");
								this.message.innerHTML = "No se encontraron devoluciones con saldo pendientes con los criterios seleccionados";			
								this.progressBar.style.display = "none";	
								return;								
							}

							/*
							var maxLength = 20;							
							
							array.forEach(response.devoluciones, lang.hitch(this, function(devolucion) {
								if (devolucion.cliente.length > maxLength)
									devolucion.cliente = devolucion.cliente.substr(0, maxLength) + "...";
								if (devolucion.empresa.length > maxLength)
									devolucion.empresa = devolucion.empresa.substr(0, maxLength) + "...";							
							}));
							*/		

							array.forEach(response.devoluciones, lang.hitch(this, function(devolucion) {
								this.gridSaldoPendiente.store.newItem(devolucion);
							}));	
							this.gridSaldoPendiente.render();												

							this.message.innerHTML = ""
							this.progressBar.style.display = "none";											
													
						})
					});				

		},

		onDevolucionDirectaChange: function() {
			if (this.devolucionDirectaProveedor.getValue() === "true") {
				this.omitirEnvioCorreo.set("disabled", false);
			} else {
				this.omitirEnvioCorreo.setValue(false);
				this.omitirEnvioCorreo.set("disabled", true);
			}
		},
				
		getSelectedCliente: function() {
			var cliente = null;
			array.forEach(this.clientes.options, lang.hitch(this, function(option) {
				if (option.selected && option.item.id != "")
					cliente = option.item;
			}));
			return cliente;
		},

		getSelectedClienteSaldoPendiente: function() {
			var cliente = null;
			array.forEach(this.clientesSaldoPendiente.options, lang.hitch(this, function(option) {
				if (option.selected && option.item.id != "")
					cliente = option.item;
			}));
			return cliente;
		},	

		getSelectedEmpresaSaldoPendiente: function() {
			var empresa = null;
			array.forEach(this.empresasSaldoPendiente.options, lang.hitch(this, function(option) {
				if (option.selected && option.item.id != "")
					empresa = option.item;
			}));
			return empresa;
		},				
		
		getSelectedProveedor: function() {
			var proveedor = null;
			array.forEach(this.proveedores.options, lang.hitch(this, function(option) {
				if (option.selected && option.item.id != "")
					proveedor = option.item;
			}));
			return proveedor;
		},		
		
		getFormatedDateValue: function(strDate) {
			if (strDate.length < 10)
				return strDate;
			else
				return strDate.substring(0,10);			
		},	
		
		getDataValue: function(dVal) {
	    	// expected: dd/MM/yyyy
	    	// convert to: yyyy-MM-dd	
	    	if (dVal == null || dVal.length == 0)
	    		return dVal;
	    	return dVal.substring(6,10) + "-" +  dVal.substring(3,5) + "-" + dVal.substring(0,2);  		
		},
		
	    parseFloat: function(str) {
	    	  var strVal = str.replace(new RegExp(',', 'g'), '');
	    	  return parseFloat(strVal);
	    },		
		
	    formatDate: function(dVal) {
	    	// expected: yyyy-MM-dd
	    	// convert to: dd/MM/yyyy
	    	if (dVal == null || dVal.length == 0)
	    		return dVal;
	    	return dVal.substring(8,10) + "/" + dVal.substring(5,7) + "/" + dVal.substring(0,4);  	
	    },
		
		formatCurrency: function(num) {
		    var p = num.toFixed(2).split(".");
		    return p[0].split("").reverse().reduce(function(acc, num, i, orig) {
		        return  num + (i && !(i % 3) ? "," : "") + acc;
		    }, "") + "." + p[1];
		},
		
		formatViewFileButton: function(value, index) {
			
			var item = this.gridPagos.getItem(index);
			if (this.gridPagos.store.getValue(item, 'contentSize') == null)
				return;
			
			button = new Button({
            	label: 'Ver',
                onClick: lang.hitch(this, function() {
        			this.showDocument(this.gridPagos.store.getValue(item, 'id'));
                })
	        });
		    return button;
		},  		
		
		formatMetodoPago: function(num) {
			var metodoPago = parseInt(num);
			switch(metodoPago) {
				case 2: // transferencia
					return "TRANSFERENCIA"
					break;
				case 1: // cheque
					return "CHEQUE"
					break;
				case 0: // entrega en efectivo
					return "ENTREGA EN EFECTIVO"
					break;					
				case 3: // deposito en efectivo
					return "DEPÓSITO EN EFECTIVO"
					break;
				case 4: // pago a tarjeta
					return "PAGO A TARJETA DE CRÉDITO"
					break;					
			}			
		}		
		
	});
});