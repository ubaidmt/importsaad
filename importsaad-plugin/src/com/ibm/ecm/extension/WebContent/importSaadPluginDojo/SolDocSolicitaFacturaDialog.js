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
	"dojo/text!./templates/SolDocSolicitaFacturaDialog.html"
	],
	function (declare, lang, connect, Request, BaseDialog, Button, Select, array, json, dom, domStyle, domGeom, DataGrid, timing, ItemFileWriteStore, ObjectStore, Memory, Desktop, template) {
	
	/**
	 * @name importSaadPluginDojo.SolDocSolicitaFacturaDialog
	 * @class
	 * @augments ecm.widget.dialog.BaseDialog
	 */
	return declare("importSaadPluginDojo.SolDocSolicitaFacturaDialog", [BaseDialog], {
	/** @lends importSaadPluginDojo.SolDocSolicitaFacturaDialog.prototype */

		//needed to load from template
		contentString: template,
		widgetsInTemplate: true,
		grid: null,	
		repositoryId: null,
		isAdminUser: false,	
		context: null,	
		proveedores: null,
		clientes: null,
		empresas: null,	
		items: null,
		gridId: 1,
		calculoIVA: 0,
		ticker: null,

		postCreate: function() {
			this.inherited(arguments);
			this.setResizable(false);
			this.setMaximized(true);
			//this.setSize(800, 800); // in case setMaximized is set to false
			this.setTitle("Solcitud de Nueva Factura");
			this.setIntroText("Envio de solicitd al proveedor para una nueva factura.");
			this.resetButton = this.addButton("Limpiar", "onReset", false, false);
			this.crearButtonButton = this.addButton("Crear", "onCrear", false, true);
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
			this.empresas.destroy();
			this.empresas = null;	
			this.grid.destroy();
			this.grid = null;
			this.inherited("hide", []);		
		},

		resize: function() {
			this.inherited(arguments);

			var size = domGeom.getContentBox(this.domNode);
			var gridHeight = size.h - 640;
			if (this.isMaximized())
				gridHeight += 70; // if maximized, no header is included
			domStyle.set(this.gridDiv, "height", gridHeight + "px");	

			if (this.grid != null) {
		    	this.grid.resize();
		    	this.grid.update();		
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
		
		setIsAdminUser: function(value) {
			this.isAdminUser = value;
		},
		
		setItems: function(items) {
			this.items = items;
		},		
		
		setContext: function(context) {
			this.context = context;
		},	

		setCalculoIVA: function(calculoIVA) {
			this.calculoIVA = calculoIVA;
		},
				
		onLoad: function() {

			this.resetGridValues();
			this.loadProveedores();								
			this.setMontosFactura();
			this.setMontosComision();
			
			//if (this.isAdminUser) {
				domStyle.set(this.comisionesPane, "display", "block");
				this.porcentajeComisionProveedor.required = true;
				this.porcentajeComisionDistribuidor.required = true;
			//}			
			
			connect.connect(this.nuevaEmpresa, "onChange", lang.hitch(this, function() {
				this.onNuevaEmpresaChange();
			}));
			
			connect.connect(this.porcentajeComisionProveedor, "onChange", lang.hitch(this, function() {
				this.setMontosComision();
			}));
			
			connect.connect(this.porcentajeComisionDistribuidor, "onChange", lang.hitch(this, function() {
				this.setMontosComision();
			}));		
						
		},
		
		loadDatosSolicitud: function() {
			
			var params = {};
			params.method = "getSolDocDatosFactura";
			params.repositoryid = this.items[0].repository.id;
			params.context = json.stringify(this.context);
			var items = [{"id": this.items[0].id.split(",")[2]}];
			params.items = json.stringify(items);				
			
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
							
							var solicitud = response.solicitudes[0];
									
							this.proveedores.setValue(solicitud.datos.nombreProveedor);
							this.empresaSolicitada.setValue(("empresaSolicitada" in solicitud.datos ? solicitud.datos.empresaSolicitada : ""));
							this.clientes.setValue(solicitud.datos.razonSocial);	
							this.porcentajeComisionProveedor.setValue(solicitud.datos.porcentajeComisionProveedor);
							this.porcentajeComisionDistribuidor.setValue(solicitud.datos.porcentajeComisionDistribuidor);
							this.loadConceptos(solicitud.datos.conceptos);
							this.setMontosFactura();
							this.setMontosComision();
							
						})
					}); 			
		},
		
		loadConceptos: function(conceptos) {
			array.forEach(conceptos, lang.hitch(this, function(concepto) {
				this.grid.store.newItem(concepto);
			}));	
			this.grid.render();	
			this.gridId = this.getCurrentGridId();
		},	

		getCurrentGridId: function() {
			var lastGridId = 0;
			for (var i = 0; i < this.grid.rowCount; i++) {
				var item = this.grid.getItem(i);
				var currentGridId = parseInt(this.grid.store.getValue(item, 'id'));
				if (currentGridId > lastGridId)
					lastGridId = currentGridId;
			}
			return (lastGridId + 1);			
		},			
		
		setMontosComision: function() {
			var subTotal = this.parseFloat(this.subTotal.innerHTML);
			
			if (this.porcentajeComisionProveedor.isValid()) {
				var montoComision = (parseFloat(this.porcentajeComisionProveedor.getValue()) * .01) * subTotal;
				this.montoComisionProveedor.innerHTML = this.formatCurrency(montoComision);
			}
			if (this.porcentajeComisionDistribuidor.isValid()) {
				var montoComision = (parseFloat(this.porcentajeComisionDistribuidor.getValue()) * .01) * subTotal;
				this.montoComisionDistribuidor.innerHTML = this.formatCurrency(montoComision);			
			}
		},
		
		onNuevaEmpresaChange: function() {
			if (this.nuevaEmpresa.getValue()) {
				domStyle.set(this.nombreEmpresaPane, "display", "block");
				if (this.empresas != null) {
					this.empresas.setValue("");
					this.empresas.readOnly = true;
					this.empresaNombre.required = true;
				}
			} else {
				domStyle.set(this.nombreEmpresaPane, "display", "none");
				if (this.empresas != null) {
					this.empresas.readOnly = false;
					this.empresaNombre.required = false;
					this.empresaNombre.reset();
				}					
			}			
		},
		
		onGridItemSelect: function() {
			var items = this.grid.selection.getSelected();
			if (items.length > 0) {
				var item = items[items.length-1]; // select last
				this.conceptoDescripcion.setValue(item.conceptoDescripcion.toString());
				this.conceptoUnidad.setValue(item.conceptoUnidad.toString());
				this.conceptoCantidad.setValue(item.conceptoCantidad.toString());
				this.conceptoPrecioUnitario.setValue(item.conceptoPrecioUnitario.toString());					
			}		
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
							items.push({ "id": "", "label": "--SELECCIONAR--"});
							array.forEach(response.proveedores, lang.hitch(this, function(item) {
								items.push({ "id": item.proveedorNombre, "label": item.proveedorNombre});
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
							this.loadEmpresas();
							this.loadClientes();
							
							connect.connect(this.proveedores, "onChange", lang.hitch(this, function() {
								this.onProveedoresChange();
							}));									
							
						})
					}); 			
		},	
		
		onProveedoresChange: function() {
			// Load empresas del proveedor
			//this.loadEmpresas();
		},		
		
		loadClientes: function() {							
			
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
								items.push({ "id": item.razonSocial, "label": item.razonSocial, "rfc": item.rfc, "direccionFiscal": item.direccionFiscal, "porcentajeComisionProveedor": item.porcentajeComisionProveedor, "porcentajeComisionDistribuidor": item.porcentajeComisionDistribuidor});
							}));								
							
							var store = new Memory({
								data: items
							});							
						    
							var os = new ObjectStore({ objectStore: store });
							this.clientes = new Select({
								id: "clientes",
								required: false,
						        store: os
						    });								
							
							this.clientes.placeAt(this.clientesDiv);			
							this.clientes.startup();
							this.setDatosCliente();
							
							connect.connect(this.clientes, "onChange", lang.hitch(this, function() {
								this.setDatosCliente();	
							}));					
							
							// Precarga datos de la solicitud en caso de ser invocada por accion "Copiar"
							if (this.items != null)
								this.loadDatosSolicitud();							
							
						})
					}); 			
		},	
		
		setDatosCliente: function() {
				
			// Set datos cliente
			this.porcentajeComisionProveedor.reset();
			this.porcentajeComisionDistribuidor.reset();
			array.forEach(this.clientes.options, lang.hitch(this, function(option) {
				if (option.selected) {
					this.rfc.innerHTML = option.item.rfc;
					this.direccionFiscal.innerHTML = option.item.direccionFiscal;					
					this.porcentajeComisionDistribuidor.setValue(option.item.porcentajeComisionDistribuidor);
					this.porcentajeComisionProveedor.setValue(option.item.porcentajeComisionProveedor);
				}
			}));			
						
		},
		
		loadEmpresas: function() {
			
			// Inicializa select en caso de no existir
			if (this.empresas == null) {
				var items = [{ "id": "", "label": "--SELECCIONAR--"}];
												
				var store = new Memory({
					data: items
				});							
			    
				var os = new ObjectStore({ objectStore: store });
				this.empresas = new Select({
					id: "empresas",
					required: false,
			        store: os
			    });								
				
				this.empresas.placeAt(this.empresasDiv);			
				this.empresas.startup();	
				return;
			}
			
			var params = {};
			params.method = "getSolDocEmpresas";
			params.repositoryid = this.repositoryId;
			params.proveedor = this.proveedores.getValue();
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
								items.push({ "id": item.empresaNombre, "label": item.empresaNombre});
							}));								
							
							var store = new Memory({
								data: items
							});							
						    
							var os = new ObjectStore({ objectStore: store });
							this.empresas.setStore(os);									
							
						})
					}); 			
		},	
		
		onConceptoAdd: function() {
			
			if (!this.conceptoDescripcion.isValid() || !this.conceptoUnidad.isValid() || !this.conceptoCantidad.isValid() || !this.conceptoPrecioUnitario.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "Datos inválidos.";
				return;	
			}
			
			var precioUnitario = parseFloat(this.conceptoPrecioUnitario.getValue());
			var cantidad = parseFloat(this.conceptoCantidad.getValue());
			var conceptoImporte = precioUnitario * cantidad;
			
			var concepto = {"id": this.gridId++, "conceptoDescripcion": this.conceptoDescripcion.getValue(), "conceptoUnidad": this.conceptoUnidad.getValue(), "conceptoCantidad": cantidad, "conceptoPrecioUnitario": precioUnitario, "conceptoImporte": conceptoImporte};
			this.grid.store.newItem(concepto);
			this.grid.render();
			this.setMontosFactura();
			this.resetConceptosValues();
			
		},
		
		getConceptosItems: function() {
			var jsonConceptos = [];				
			for (var i = 0; i < this.grid.rowCount; i++) {
				var item = this.grid.getItem(i);
				var concepto = {"id": this.grid.store.getValue(item, "id"), "conceptoDescripcion": this.grid.store.getValue(item, "conceptoDescripcion"), "conceptoUnidad": this.grid.store.getValue(item, "conceptoUnidad"), "conceptoCantidad": this.grid.store.getValue(item, "conceptoCantidad"), "conceptoPrecioUnitario": this.grid.store.getValue(item, "conceptoPrecioUnitario"), "conceptoImporte": this.grid.store.getValue(item, "conceptoImporte")};
				jsonConceptos.push(concepto);
			}
			return jsonConceptos;
		},		
		
		onConceptoUpdate: function() {
			
			if (!this.conceptoDescripcion.isValid() || !this.conceptoUnidad.isValid() || !this.conceptoCantidad.isValid() || !this.conceptoPrecioUnitario.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "Datos inválidos.";
				return;	
			}		
			
			var precioUnitario = parseFloat(this.conceptoPrecioUnitario.getValue());
			var cantidad = parseFloat(this.conceptoCantidad.getValue());
			var conceptoImporte = precioUnitario * cantidad;			
			
			var items = this.grid.selection.getSelected();		
			if (items.length <= 0)
				return;
			
			array.forEach(items, lang.hitch(this, function(item) {
				this.grid.store.setValue(item, "conceptoDescripcion", this.conceptoDescripcion.getValue());
				this.grid.store.setValue(item, "conceptoUnidad", this.conceptoUnidad.getValue());
				this.grid.store.setValue(item, "conceptoCantidad", cantidad);
				this.grid.store.setValue(item, "conceptoPrecioUnitario", precioUnitario);
				this.grid.store.setValue(item, "conceptoImporte", conceptoImporte);
			}));
			
			this.grid.render();	
			this.setMontosFactura();
			this.resetConceptosValues();
			
		},
		
		onConceptoDelete: function() {

			var items = this.grid.selection.getSelected();		
			if (items.length <= 0)
				return;
			
			array.forEach(items, lang.hitch(this, function(item) {
				this.grid.store.deleteItem(item);
			}));
			
			this.grid.render();
			this.setMontosFactura();
			this.resetConceptosValues();
			
		},
		
		resetGridValues: function() {

			if (this.grid != null) {
				this.grid.destroy();
				this.grid = null;				
			}
			
			var gridStore = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});				
		
			var gridLayout = [
				{name: 'Id', field: 'id', width: '3%', editable: false},
				{name: 'Descripción', field: 'conceptoDescripcion', width: '40%', editable: false},
				{name: 'Unidad', field: 'conceptoUnidad', width: '15%', editable: false},			
				{name: 'Cantidad', field: 'conceptoCantidad', width: '15%', editable: false},
				{name: 'Precio Unitario', field: 'conceptoPrecioUnitario', width: '15%', editable: false, formatter: this.formatCurrency},
				{name: 'Importe', field: 'conceptoImporte', width: '15%', editable: false, formatter: this.formatCurrency}
			];
			
			this.grid = new DataGrid({
				id: 'grid',
				store: gridStore,
				structure: gridLayout,
				selectionMode: 'multi',
				sortInfo: 2, // asc by conceptoDescripcion
				autoHeight: true
			});

			this.grid.placeAt(this.gridDiv);			
			this.grid.startup();
			
			connect.connect(this.grid, "onClick", lang.hitch(this, function() {
				this.onGridItemSelect();
			}));
			
			connect.connect(this.grid, "onStyleRow", lang.hitch(this, function(row) {
				var item = this.grid.getItem( row.index );
				if ( item == null ) return;
				if (row.odd) row.customStyles += 'background-color:#F7F6F6;';
			}));			

			this.gridId = 1;
			this.setMontosFactura();
		},
		
		validaDatosEnvio: function() {
			
			// Ocular panel de confirmacion
			domStyle.set(this.confirmacionPane, "display", "none");	
			
			this.message.innerHTML = "";			
			
			if (this.proveedores.getValue() == "" || this.clientes.getValue() == "" || !this.porcentajeComisionProveedor.isValid() || !this.porcentajeComisionDistribuidor.isValid() || !this.numeroCopias.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "Datos inválidos.";
				return false;
			}
			
			if(this.nuevaEmpresa.getValue() && !this.empresaNombre.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "Es necesario ingresar el nombre de la nueva empresa.";
				return false;				
			}
			
			if (this.grid.rowCount < 1) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "Es necesario ingresar al menos un concepto.";
				return false;				
			}			
			
			// En caso de crear una nueva empresa, valda que el nombre no exista
			var empresaValida = true;
			if (this.nuevaEmpresa.getValue()) {
				array.forEach(this.empresas.options, lang.hitch(this, function(option) {
					if (this.empresaNombre.getValue() == option.item.id) {
						empresaValida = false;
						return false;
					}
				}));				
			}
			
			if (!empresaValida) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "La empresa a ser creada ya se encuentra asignada al proveedor.";
				return false;
			}	
			
			return true;
			
		},
		
		onConfirmar: function() {
									
			if (!this.validaDatosEnvio())
				return;
			
			domStyle.set(this.message, "color", "#000253");
			this.message.innerHTML = "Enviando Solicitud...";
			this.progressBar.style.display = "block";				
			
			var jsonData = {
					"nombreProveedor": this.proveedores.getValue(),
					"nuevaEmpresa": (this.nuevaEmpresa.getValue() === "true"),
					"nombreEmpresa": (this.nuevaEmpresa.getValue() ? this.empresaNombre.getValue() : this.empresas.getValue()),
					"empresaSolicitada": this.empresaSolicitada.getValue(),
					"razonSocial": this.clientes.getValue(),
					"rfc": this.rfc.innerHTML,
					"direccionFiscal": this.direccionFiscal.innerHTML,
					"subTotal": this.parseFloat(this.subTotal.innerHTML),
					"iva": this.parseFloat(this.iva.innerHTML),
					"montoTotal": this.parseFloat(this.montoTotal.innerHTML),
					"porcentajeComisionProveedor": parseFloat(this.porcentajeComisionProveedor.getValue()),
					"montoComisionProveedor": this.parseFloat(this.montoComisionProveedor.innerHTML),
					"porcentajeComisionDistribuidor": parseFloat(this.porcentajeComisionDistribuidor.getValue()),
					"montoComisionDistribuidor": this.parseFloat(this.montoComisionDistribuidor.innerHTML),
					"observaciones": this.observaciones.getValue(),
					"numeroCopias": parseInt(this.numeroCopias.getValue()),
					"conceptos": this.getConceptosItems(),
					"omitirEnvioCorreo": (this.omitirEnvioCorreo.getValue() === "true")
				};
			
			var params = {};
			params.method = "addSolDocSolicitudFactura";
			params.repositoryid = this.repositoryId;
			params.context = json.stringify(this.context);
			params.datos = json.stringify(jsonData);
			
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
														
							var message;
							
							if (response.status == 1) { // error en transaccion
								domStyle.set(this.message, "color", "#6B0A0A");
								message = "Ocurrió un error al momento de enviar la solicitud de la factura(s). " + response.error;
							} else if (response.status == 2) { // error en notificacion
								domStyle.set(this.message, "color", "#000253");
								if (response.folios.length > 1) {
									message = "Se crearon existosamente las solicitudes de la facturas con los siguientes folios:<br>";
									array.forEach(response.folios, lang.hitch(this, function(folio) {
										message += "[" + folio.value + "] ";
									}));
								} else {
									message = "Se creó existosamente la solicitud de la factura con el siguiente folio:<br>" + response.folios[0].value;
									if (this.omitirEnvioCorreo.getValue() === "true")
										message += "<br>La notificación al proveedor fue omitida";
								}
								message += "<br>Sin embargo, ocurrió un error al momento de enviar la notificación por correo electrónico al proveedor: " + response.error;
							} else { // success
								domStyle.set(this.message, "color", "#000253");
								if (response.folios.length > 1) {
									message = "Se crearon existosamente las solicitudes de la facturas con los siguientes folios:<br>";
									array.forEach(response.folios, lang.hitch(this, function(folio) {
										message += folio.value + " ";
									}));
								} else {
									message = "Se creó existosamente la solicitud de la factura con el siguiente folio:<br>" + response.folios[0].value;
								}
								if (this.omitirEnvioCorreo.getValue() === "true")
									message += "<br>La notificación al proveedor fue omitida";								
							}
							
							this.onReset();	
							this.message.innerHTML = message;
							this.progressBar.style.display = "none";							
				
						})
					}); 			
			
		},
		
		onCancelar: function() {
			// Ocular panel de confirmacion
			domStyle.set(this.confirmacionPane, "display", "none");
		},		
		
		onCrear: function() {
			
			if (!this.validaDatosEnvio())
				return;
			
			// Mostrar panel de confirmacion
			domStyle.set(this.confirmacionPane, "display", "block");
			
		},	
		
		onReset: function() {
			this.resetGridValues();
			this.resetConceptosValues();			
			this.proveedores.setValue("");
			this.clientes.setValue("");
			this.empresaSolicitada.reset();
			this.nuevaEmpresa.setValue(false);
			this.empresaNombre.reset();
			this.observaciones.reset();	
			this.numeroCopias.reset();
			this.omitirEnvioCorreo.reset();
			domStyle.set(this.confirmacionPane, "display", "none");
			domStyle.set(this.progressBar, "display", "none");	
			this.message.innerHTML = "";			
		},	
		
		setMontosFactura: function() {
			
			var subtotal = 0;
			var iva = 0;		
			var montoTotal = 0;
			for (var i = 0; i < this.grid.rowCount; i++) {
				var item = this.grid.getItem(i);
				subtotal += this.grid.store.getValue(item, 'conceptoImporte');
			}
			
			if (subtotal > 0) {
				iva = subtotal * this.calculoIVA;	
				montoTotal = subtotal + iva;
			}
				
			this.subTotal.innerHTML = this.formatCurrency(subtotal);
			this.iva.innerHTML = this.formatCurrency(iva);
			this.montoTotal.innerHTML = this.formatCurrency(montoTotal);
			
			this.setMontosComision();
		},
		
		resetConceptosValues: function() {
			this.conceptoDescripcion.reset();
			this.conceptoUnidad.reset();
			this.conceptoCantidad.reset();
			this.conceptoPrecioUnitario.reset();
			this.progressBar.style.display = "none";
			this.message.innerHTML = "";			
		},
		
		formatCurrency: function(num) {
		    var p = num.toFixed(2).split(".");
		    return p[0].split("").reverse().reduce(function(acc, num, i, orig) {
		        return  num + (i && !(i % 3) ? "," : "") + acc;
		    }, "") + "." + p[1];
		},
		
	    parseFloat: function(str) {
	    	  var strVal = str.replace(new RegExp(',', 'g'), '');
	    	  return parseFloat(strVal);
	    }
		
	});
});