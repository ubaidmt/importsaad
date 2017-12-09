define([
	"dojo/_base/declare",
	"dojo/_base/lang",
	"dojo/on",
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
	"importSaadPluginDojo/dialog/SolDocFormaPagoDialog",
	"importSaadPluginDojo/dialog/SolDocBuscarProdServDialog",
	"importSaadPluginDojo/dialog/SolDocBuscarUniMedDialog",	
	"dojo/text!./templates/SolDocEditarFacturaDialog.html"
	],
	function (declare, lang, on, Request, BaseDialog, Button, Select, array, json, dom, domStyle, domGeom, DataGrid, timing, ItemFileWriteStore, ObjectStore, Memory, Desktop, SolDocFormaPagoDialog, SolDocBuscarProdServDialog, SolDocBuscarUniMedDialog, template) {
	
	/**
	 * @name importSaadPluginDojo.action.SolDocEditarFacturaDialog
	 * @class
	 * @augments ecm.widget.dialog.BaseDialog
	 */
	return declare("importSaadPluginDojo.action.SolDocEditarFacturaDialog", [BaseDialog], {
	/** @lends importSaadPluginDojo.action.SolDocEditarFacturaDialog.prototype */

		//needed to load from template
		contentString: template,
		widgetsInTemplate: true,
		grid: null,	
		items: null,
		isAdminUser: false,	
		context: null,		
		filePDF: null,
		fileXML: null,
		facturaCliente: null,
		facturaTotal: null,
		tipoOperacion: 0,
		empresas: null,
		gridId: 1,
		beenMinimized: false,
		ticker: null,
		
		postCreate: function() {
			this.inherited(arguments);
			this.setResizable(false);
			this.setMaximized(true);
			this.setTitle("Detalle de la Factura");
			this.setIntroText("Muestra el detalle de información sobre una factura.");
			this.deleteButton = this.addButton("Eliminar", "onDelete", false, false);
			this.notificaPagoButton = this.addButton("Notificar Devolución a Cliente", "onNotificaDevolucion", false, false);
			this.enviarButton = this.addButton("Enviar Factura a Cliente", "onEnviarFactura", false, false);
			this.solicitarButton = this.addButton("Solicitar Factura a Proveedor", "onSolicitarFactura", false, false);
			this.refreshButton = this.addButton("Refrescar", null, false, false);						
			this.saveButton = this.addButton("Guardar", "onSave", false, true); // main button
			this.cancelButton.set("label", "Cerrar");
		},	

		showDialog: function() {
			this.onLoad();	
			//this.startTicker(600000); // 10 minutes	
			this.inherited("show", []);		
		},

		hide: function() {
			this.stopTicker();
			this.grid.destroy();
			this.grid = null;		
			this.empresas.destroy();
			this.empresas = null;		
			this.inherited("hide", []);			
		},	

		resize: function() {
			this.inherited(arguments);

			var size = domGeom.getContentBox(this.domNode);
			var gridHeight = size.h - 640;
			if (this.isMaximized()) {
				var tabContentPaneHeight = gridHeight + 350;
				gridHeight += 70; // if maximized, no header is included
	    		if (this.beenMinimized) {
	    			this.tabConteinerPane.set("style", "width: 100%; height: " + tabContentPaneHeight + "px; background-color:#FFFFFF;");
	    			this.tabConteinerPane.resize();
	    		}				
			} else {
				var tabContentPaneHeight = gridHeight + 270;
	    		this.tabConteinerPane.set("style", "width: 100%; height: " + tabContentPaneHeight + "px; background-color:#FFFFFF;");
	    		this.tabConteinerPane.resize();
	    		this.beenMinimized = true;				
			}
			domStyle.set(this.gridDiv, "height", gridHeight + "px");	

			if (this.grid != null) {
		    	this.grid.resize();
		    	this.grid.update();		
	    	}
		},	

		startTicker: function(interval) {
			this.ticker = new dojox.timing.Timer();
			this.ticker.setInterval(interval);

			on(this.ticker, "tick", lang.hitch(this, function() {
				this.sendDummyRequest();
			}));	
			
			this.ticker.start();
		},

		stopTicker: function() {
			if (this.ticker != null)
				this.ticker.stop();
		},				
		
		setItems: function(items) {
			this.items = items;
		},
		
		setIsAdminUser: function(value) {
			this.isAdminUser = value;
		},	
		
		setConfig: function(config) {
			this.config = config;
		},				
		
		onLoad: function() {	
						
			this.tipoOperacion = 0;
			this.metodosPago = {};
			this.solicitarButton.set("disabled", true);
			this.enviarButton.set("disabled", true);
			this.notificaPagoButton.set("disabled", true);

			this.loadDatosFactura();
			
			domStyle.set(this.comisionesPane, "display", "block");
			this.porcentajeComisionProveedor.required = true;
			this.porcentajeComisionDistribuidor.required = true;	
			
			on(this.refreshButton, "click", lang.hitch(this, function() {
				this.onRefresh(true);
			}));			
			
			on(this.porcentajeComisionProveedor, "change", lang.hitch(this, function() {
				this.setMontosComision();
			}));
			
			on(this.porcentajeComisionDistribuidor, "change", lang.hitch(this, function() {
				this.setMontosComision();
			}));	
			
			on(this.radioPDF, "change", lang.hitch(this, function() {
				this.checkFileButtons();
			}));	
			
			on(this.radioXML, "change", lang.hitch(this, function() {
				this.checkFileButtons();
			}));
			
			on(this.searchProdServ, "click", lang.hitch(this, function() {
				this.onProdServSearch();
			}));
			
			on(this.searchUniMed, "click", lang.hitch(this, function() {
				this.onUniMedSearch();
			}));					
						
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
		
		loadDatosFactura: function() {
			var params = {};
			params.method = "getSolDocDatosFactura";
			params.repositoryid = this.items[0].repository.id;
			params.context = json.stringify(this.config.context);
			var items = [{"id": this.items[0].id.split(",")[2]}];
			params.items = json.stringify(items);				
			
			Request.invokePluginService("ImportSaadPlugin", "SolDocService",
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
							
							this.proveedor.innerHTML = solicitud.datos.nombreProveedor;
							this.empresaSolicitada.innerHTML = ("empresaSolicitada" in solicitud.datos ? solicitud.datos.empresaSolicitada : "");
							this.filePDF = solicitud.filePDF;
							this.fileXML = solicitud.fileXML;
							this.numeroFactura.innerHTML = solicitud.numeroFactura;
							this.fechaFactura.innerHTML= this.formatDate(solicitud.fechaFactura);
							this.cliente.innerHTML = solicitud.datos.razonSocial;
							this.rfc.innerHTML = solicitud.datos.rfc;
							this.direccionFiscal.innerHTML = solicitud.datos.direccionFiscal;
							this.estadoFactura.innerHTML = this.formatEstadoCFDI(solicitud.estado);
							this.loadConceptos(solicitud.datos.conceptos);
							
							this.metodoPago.setValue(solicitud.datos.metodoPago2);
							this.usoCFDI.setValue(solicitud.datos.usoCFDI);
							this.porcentajeComisionProveedor.setValue(solicitud.datos.porcentajeComisionProveedor);
							this.porcentajeComisionDistribuidor.setValue(solicitud.datos.porcentajeComisionDistribuidor);
							if ("metodoPago" in solicitud.datos) this.metodosPago = json.parse(solicitud.datos.metodoPago);
							this.setFormaPago();
							this.numeroCuenta.setValue("numeroCuenta" in solicitud.datos ? solicitud.datos.numeroCuenta : "");
							
							this.setMontosFactura();
							this.setMontosComision();
							
							this.pdfFileName.innerHTML = (this.filePDF != null ? this.filePDF.name : "");
							this.xmlFileName.innerHTML = (this.fileXML != null ? this.fileXML.name : "");
							this.checkFileButtons();
							this.checkMiscButtons(solicitud.estado);

							this.loadEmpresas(solicitud);
							
						})
					}); 				
		},

		loadEmpresas: function(solicitud) {
			
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
			}
			
			var params = {};
			params.method = "getSolDocEmpresas";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.proveedor = solicitud.datos.nombreProveedor;
			params.context = json.stringify(this.config.context);
			
			Request.invokePluginService("ImportSaadPlugin", "SolDocService",
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
								items.push({ "id": item.name, "label": item.name});
							}));								
							
							var store = new Memory({
								data: items
							});							
						    
							var os = new ObjectStore({ objectStore: store });
							this.empresas.setStore(os);	

							// Load nombre empresa
							if (solicitud.empresaNombre != null)
								this.empresas.setValue(solicitud.empresaNombre);								
							
						})
					}); 			
		},			
		
		checkMiscButtons: function(estado) {
			if (this.filePDF != null && this.fileXML != null) {
				this.enviarButton.set("disabled", false);			
			} else {
				this.solicitarButton.set("disabled", false);
			}
			if (estado == 3) { // devuelta a cliente
				this.notificaPagoButton.set("disabled", false);			
			}
		},
		
		loadConceptos: function(conceptos) {
			
			// create grid
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
				{name: 'Descripción', field: 'conceptoDescripcion', width: '35%', editable: false},
				{name: 'Clave', field: 'conceptoClave', width: '10%', editable: false},
				{name: 'ProdServ', field: 'conceptoProdServ', width: '15%', editable: false},
				{name: 'Unidad', field: 'conceptoUnidad', width: '10%', editable: false},
				{name: 'UniMed', field: 'conceptoUniMed', width: '10%', editable: false},
				{name: 'Cantidad', field: 'conceptoCantidad', width: '10%', editable: false},
				{name: 'Unitario', field: 'conceptoPrecioUnitario', width: '10%', editable: false, formatter: this.formatCurrency},
				{name: 'Importe', field: 'conceptoImporte', width: '10%', editable: false, formatter: this.formatCurrency}				
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

			// set events
			on(this.grid, "click", lang.hitch(this, function() {
				this.onGridItemSelect();
			}));			
			
			on(this.grid, "stylerow", lang.hitch(this, function(row) {
				var item = this.grid.getItem( row.index );
				if ( item == null ) return;
				if (row.odd) row.customStyles += 'background-color:#F7F6F6;';
			}));

			// load conceptos
			array.forEach(conceptos, lang.hitch(this, function(concepto) {
				this.grid.store.newItem(concepto);
			}));	
			this.grid.render();	
			this.gridId = this.getCurrentGridId();				

			this.setMontosConceptos();			
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

		setMontosFactura: function() {
			
			var subtotal = 0;
			var iva = 0;		
			var montoTotal = 0;
			for (var i = 0; i < this.grid.rowCount; i++) {
				var item = this.grid.getItem(i);
				subtotal += this.grid.store.getValue(item, 'conceptoImporte');
			}
			
			if (subtotal > 0) {
				iva = subtotal * parseFloat(this.config.pteiva);	
				montoTotal = subtotal + iva;
			}
				
			this.subTotal.innerHTML = this.formatCurrency(subtotal);
			this.iva.innerHTML = this.formatCurrency(iva);
			this.montoTotal.innerHTML = this.formatCurrency(montoTotal);
		},

		setMontosConceptos: function() {
			
			var subtotal = 0;
			var iva = 0;		
			var montoTotal = 0;
			for (var i = 0; i < this.grid.rowCount; i++) {
				var item = this.grid.getItem(i);
				subtotal += this.grid.store.getValue(item, 'conceptoImporte');
			}
			
			if (subtotal > 0) {
				iva = subtotal * 0.16;	
				montoTotal = subtotal + iva;
			}
				
			this.subTotalConceptos.innerHTML = this.formatCurrency(subtotal);
			this.ivaConceptos.innerHTML = this.formatCurrency(iva);
			this.montoTotalConceptos.innerHTML = this.formatCurrency(montoTotal);
		},	

		onGridItemSelect: function() {
			var items = this.grid.selection.getSelected();
			if (items.length > 0) {
				var item = items[items.length-1]; // select last
				this.conceptoDescripcion.setValue(item.conceptoDescripcion.toString());
				this.conceptoClave.setValue(item.conceptoClave.toString());
				this.conceptoProdServ.setValue(item.conceptoProdServ.toString());
				this.conceptoUnidad.setValue(item.conceptoUnidad.toString());
				this.conceptoUniMed.setValue(item.conceptoUniMed.toString());
				this.conceptoCantidad.setValue(item.conceptoCantidad.toString());
				this.conceptoPrecioUnitario.setValue(item.conceptoPrecioUnitario.toString());					
			}		
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
		
		validaDatos: function() {
			
			// Ocultar panel de confirmacion
			domStyle.set(this.confirmacionPane, "display", "none");
			this.confirmationMessage.innerHTML = "";
			this.message.innerHTML = "";	
			
			switch(this.tipoOperacion) {
				case 1: // guardar
					
					if (!this.numeroCuenta.isValid()) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Es necesario ingresar un número de cuenta válido.";
						return false;
					}					
					
					var numMetodosPago = 0;
			  		for (key in this.metodosPago) {
			  			numMetodosPago++;
			  			break;
			  		}
			  		if (numMetodosPago == 0) {		  		
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Es necesario indicar el método de pago.";
						return false;				
					}					

					// Validar que el monto total de los conceptos corresponda al monto total del a solictud
					if (this.montoTotalConceptos.innerHTML != this.montoTotal.innerHTML) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "El importe total de los conceptos no corresponde al importe total solicitado.";
						return false;								
					}
					
					// Se remueven ambos archivos de la factura
					if (this.filePDF == null && this.fileXML == null)
						return true;
									
					// Valida ambos archivos de facturas anexos
					if (this.filePDF == null || this.fileXML == null) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Es necesario ingresar ambos archivos de la factura.";
						return false;				
					}
					
					// Valida que ambos archivos sean del mismo par basado en nombre de archivo
					if (this.filePDF.name.substr(0, this.filePDF.name.lastIndexOf('.')) != this.fileXML.name.substr(0, this.fileXML.name.lastIndexOf('.'))) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Los archivos de la factura deben de incluir el mismo nombre.";
						return false;				
					}

					if (this.empresas.getValue() == "") {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Es necesario indicar la empresa emisora de la factura.";
						return false;
					}
					
					if (this.numeroFactura.innerHTML == "" || this.fechaFactura.innerHTML == "") {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Los datos de la factura no han sido identificados correctamente, validar que el archivo XML cargado sea un archivo CFDI válido.";
						return false;
					}		
					
					// Valida que el RFC de cliente asociado en la factura sea el mismo asociado a la solicitud
					if (this.facturaCliente != null && this.facturaCliente != this.rfc.innerHTML) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "El RFC del cliente asociado a la soliciud no coincide con el de la factura ingresada.";
						return false;								
					}
					
					// Valida que el Monto Total asociado en la factura sea el mismo asociado a la solicitud (sin considerar centavos)
					if (this.facturaTotal > 0 && this.trunc(this.facturaTotal) != this.trunc(this.parseFloat(this.montoTotal.innerHTML))) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "El monto total asociado a la soliciud no coincide con el de la factura ingresada.";
						return false;								
					}					
					
					break;
								
			}
		
			return true;
			
		},
		
		onConfirmar: function() {
									
			if (!this.validaDatos())
				return;
			
			domStyle.set(this.message, "color", "#000253");
			this.message.innerHTML = "Ejecutando petición...";
			this.progressBar.style.display = "block";
			
			switch(this.tipoOperacion) {
			    case 1: // guardar
			    	
					var jsonData = {
						"nombreProveedor": this.proveedor.innerHTML,
						"nombreEmpresa": this.empresas.getValue(),
						"numeroFactura": this.numeroFactura.innerHTML,
						"fechaFactura": this.getDataValue(this.fechaFactura.innerHTML),
						"porcentajeComisionProveedor": parseFloat(this.porcentajeComisionProveedor.getValue()),
						"montoComisionProveedor": this.parseFloat(this.montoComisionProveedor.innerHTML),
						"porcentajeComisionDistribuidor": parseFloat(this.porcentajeComisionDistribuidor.getValue()),
						"montoComisionDistribuidor": this.parseFloat(this.montoComisionDistribuidor.innerHTML),
						"metodoPago": json.stringify(this.metodosPago),
						"numeroCuenta": this.numeroCuenta.getValue(),
						"metodoPago2": this.metodoPago.getValue(),
						"usoCFDI": this.usoCFDI.getValue(),						
						"conceptos": this.getConceptos(),
						"filePDF": this.filePDF,
						"fileXML": this.fileXML					
					};
				
					var params = {};
					params.method = "updateSolDocSolicitudFactura";
					params.repositoryid = this.items[0].repository.id;
					params.context = json.stringify(this.config.context);
					params.itemid = this.items[0].id.split(",")[2];
					params.datos = json.stringify(jsonData);
					
					Request.invokePluginService("ImportSaadPlugin", "SolDocService",
							{	
								requestParams: params,
								requestCompleteCallback: lang.hitch(this, function(response) {
									
									if (response.error != null) {
										domStyle.set(this.message, "color", "#6B0A0A");
										this.message.innerHTML = response.error;
										this.progressBar.style.display = "none";													
										return;
									}
																
									var message = "Se actualizaron correctamente los datos y archivos de la factura.";
																
									domStyle.set(this.message, "color", "#000253");
									this.message.innerHTML = message;
									this.progressBar.style.display = "none";
									
									this.onRefresh(false);							
											
								})
							}); 	
			        break;
			        			    	
			    case 2: // solicitar factura	
			    	
					var params = {};
					params.method = "sendSolDocSoliciudFactura";
					params.repositoryid = this.items[0].repository.id;
					params.context = json.stringify(this.config.context);
					params.solicitudes = json.stringify([{"id": this.items[0].id.split(",")[2]}]);			
					params.observaciones = this.observaciones.getValue().trim();
					
					Request.invokePluginService("ImportSaadPlugin", "SolDocService",
							{	
								requestParams: params,
								requestCompleteCallback: lang.hitch(this, function(response) {
									
									if (response.error != null) {
										domStyle.set(this.message, "color", "#6B0A0A");
										this.message.innerHTML = response.error;
										this.progressBar.style.display = "none";													
										return;
									}
																
									var message = "La factura ha sido solicitada al proveedor.";
																
									domStyle.set(this.message, "color", "#000253");
									this.message.innerHTML = message;
									this.progressBar.style.display = "none";
									
									this.onRefresh(false);									
																
								})
							}); 			    	
			    	
			    	break;	
			    	
			    case 3: // enviar factura	
			    	
					var params = {};
					params.method = "sendSolDocFactura";
					params.repositoryid = this.items[0].repository.id;
					params.context = json.stringify(this.config.context);
					params.solicitudes = json.stringify([{"id": this.items[0].id.split(",")[2]}]);			
					params.observaciones = this.observaciones.getValue().trim();
					
					Request.invokePluginService("ImportSaadPlugin", "SolDocService",
							{	
								requestParams: params,
								requestCompleteCallback: lang.hitch(this, function(response) {
									
									if (response.error != null) {
										domStyle.set(this.message, "color", "#6B0A0A");
										this.message.innerHTML = response.error;
										this.progressBar.style.display = "none";													
										return;
									}
																
									var message = "La factura ha sido enviada al cliente.";
																
									domStyle.set(this.message, "color", "#000253");
									this.message.innerHTML = message;
									this.progressBar.style.display = "none";
									
									this.onRefresh(false);									
																
								})
							}); 			    	
			   
			    	break;	
			    	
			    	
			    case 4: // eliminar factura	
			    	
					var params = {};
					params.method = "deleteSolDocFactura";
					params.repositoryid = this.items[0].repository.id;
					params.context = json.stringify(this.config.context);
					params.solicitudes = json.stringify([{"id": this.items[0].id.split(",")[2]}]);			
					params.observaciones = this.observaciones.getValue().trim();
					
					Request.invokePluginService("ImportSaadPlugin", "SolDocService",
							{	
								requestParams: params,
								requestCompleteCallback: lang.hitch(this, function(response) {
									
									if (response.error != null) {
										domStyle.set(this.message, "color", "#6B0A0A");
										this.message.innerHTML = response.error;
										this.progressBar.style.display = "none";													
										return;
									}
									
									this.hide();									
																
								})
							}); 			    	
			   
			    	break;	
			    	
			    case 5: // enviar notificacion de devolucion a cliente
			    
					var params = {};
					params.method = "notificaDevolucionCliente";
					params.repositoryid = this.items[0].repository.id;
					params.context = json.stringify(this.config.context);
					params.solicitudes = json.stringify([{"id": this.items[0].id.split(",")[2]}]);			
					params.observaciones = this.observaciones.getValue().trim();
	
					Request.invokePluginService("ImportSaadPlugin", "SolDocPagosService",
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
									this.message.innerHTML = "La notificación de devolución al cliente ha sido enviada.";
									this.progressBar.style.display = "none";																
									
									this.onRefresh(false);	
								})
							}); 			    
			    
			    	break;
			}
	
		},
		
		onCancelar: function() {
			// Ocular panel de confirmacion
			domStyle.set(this.confirmacionPane, "display", "none");
			this.confirmationMessage.innerHTML = "";
		},		
		
		onSave: function() {
			
			this.tipoOperacion = 1; // guardar
			
			if (!this.validaDatos())
				return;
						
			// Mostrar panel de confirmacion
			this.confirmationMessage.innerHTML = "¿Está seguro de guardar los datos de la factura ingresados?";
			domStyle.set(this.confirmacionPane, "display", "block");
			
		},
		
		onDelete: function() {
			
			this.tipoOperacion = 4; // eliminar
						
			// Mostrar panel de confirmacion
			this.confirmationMessage.innerHTML = "¿Está seguro de eliminar la factura y todo su contenido?";
			domStyle.set(this.confirmacionPane, "display", "block");
			
		},			

		onRefresh: function(resetMessage) {
			if (resetMessage) {
				this.message.innerHTML = "";
				this.progressBar.style.display = "none";
			}			
			this.radioPDF.setValue("pdf");
			this._fileInput.value = "";
			this.addFileButton.set("disabled",true);
			domStyle.set(this.confirmacionPane, "display", "none");
			this.confirmationMessage.innerHTML = "";
			this.observaciones.reset();	
			this.resetConceptosValues();						
			this.onLoad();
		},	
				
		onSolicitarFactura: function() {
			
			this.tipoOperacion = 2; // solicitar factura
			
			if (!this.validaDatos())
				return;
						
			// Mostrar panel de confirmacion
			this.confirmationMessage.innerHTML = "¿Está seguro de solicitar la factura al proveedor?";
			domStyle.set(this.confirmacionPane, "display", "block");
		},
		
		onEnviarFactura: function() {
			this.tipoOperacion = 3; // enviar factura
			
			if (!this.validaDatos())
				return;
						
			// Mostrar panel de confirmacion
			this.confirmationMessage.innerHTML = "¿Está seguro de enviar la factura al cliente?";
			domStyle.set(this.confirmacionPane, "display", "block");
		},
		
		onNotificaDevolucion: function() {
			this.tipoOperacion = 5; // enviar notificacion de devolucion a cliente
			
			if (!this.validaDatos())
				return;
						
			// Mostrar panel de confirmacion
			this.confirmationMessage.innerHTML = "¿Está seguro de enviar la notificación de devolución al cliente?";
			domStyle.set(this.confirmacionPane, "display", "block");			
		},
		
		onFilesDelete: function() {
			this.pdfFileName.innerHTML = "";
			this.xmlFileName.innerHTML = "";
			this.filePDF = null;
			this.fileXML = null;
			this.facturaCliente = null;
			this.facturaTotal = 0;
			this.empresas.reset();
			this.numeroFactura.innerHTML = "";
			this.fechaFactura.innerHTML = "";			
			this.checkFileButtons();
		},
		
		onFileAdd: function() {
			
			domStyle.set(this.message, "color", "#000253");
			this.message.innerHTML = "Anexando factura...";
			this.progressBar.style.display = "block";				
						
			var callback = lang.hitch(this, this._onFileAddCompleted);
			
			var params = {};
			params.method = "addDocument";
			params.repositoryid = this.items[0].repository.id;
			params.context = json.stringify(this.config.context);
			params.parentFolder = "/Temporal/Documentos";	
			
			if (this.radioXML.getValue()) // parse values from xml
				params.parseFacturaXML = "true";
			
			// HTML5 browser
			if (this._fileInput.files) {
				var file = this._fileInput.files[0];
				params.mimetype = file.type;
				params.parm_part_filename = (file.fileName ? file.fileName : file.name)
				//params.max_file_size = this._maxFileSize.toString();

				var form = new FormData();
				form.append("file", file);
				
				Request.postFormToPluginService("ImportSaadPlugin", "SolDocService", form, {
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
				params.action = "SolDocService";
				
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
						
			if (this.radioPDF.getValue()) { // pdf
				this.filePDF = { "id": response.document.id, "name": response.document.documentTitle};
				this.pdfFileName.innerHTML = response.document.documentTitle;
			} else { // xml 
				this.fileXML = { "id": response.document.id, "name": response.document.documentTitle};
				this.xmlFileName.innerHTML = response.document.documentTitle;
				if (response.document.factura) {
					if (response.document.factura.rfcReceptor != null)
						this.facturaCliente = response.document.factura.rfcReceptor;
					if (response.document.factura.total != null)																	
						this.facturaTotal = response.document.factura.total;
					if (response.document.factura.nombreEmisor != null)
						this.setEmpresa(response.document.factura.nombreEmisor);
					if (response.document.factura.folio != null)
						this.numeroFactura.innerHTML = response.document.factura.folio;
					if (response.document.factura.fecha != null)
						this.fechaFactura.innerHTML = this.formatDate(response.document.factura.fecha);
				}
			}
			
			this.checkFileButtons();
			
			this.message.innerHTML = ""
			this.progressBar.style.display = "none";				
		
		},

		setEmpresa: function(nombreEmisor) {
			// Si el nombre del emisor es localizado en el select se agrega como nuevo
			var existe = false;
			array.forEach(this.empresas.options, lang.hitch(this, function(option) {
				if (nombreEmisor == option.item.id)
					existe = true;
			}));
			
			if (existe) {
				this.empresas.setValue(nombreEmisor);
				return;
			}

			var items = this.empresas.options;
			items.push({ "id": nombreEmisor, "label": nombreEmisor});							
			
			var store = new Memory({
				data: items
			});							
		    
			var os = new ObjectStore({ objectStore: store });
			this.empresas.setStore(os);	
			this.empresas.setValue(nombreEmisor);			
		},		
		
		isValid: function() {
			var valid = this._fileInput;
			// This test works for both HTML5 and non-HTML5 browsers. 
			valid = valid && (this._fileInput.value) && (this._fileInput.value.length > 0);
			return valid;
		},	
		
		checkFileButtons: function() {
			
			// Ocular panel de confirmacion
			domStyle.set(this.confirmacionPane, "display", "none");
			this.confirmationMessage.innerHTML = "";
			
			var addFileValid = this.isValid(); // default add file button validation
			
			if (addFileValid) {
				if (this.radioPDF.getValue()) { // pdf
					if (this._fileInput.value.substr(this._fileInput.value.lastIndexOf('.') + 1) != this.radioPDF.getValue())
						addFileValid = false;
				} else { // xml
					if (this._fileInput.value.substr(this._fileInput.value.lastIndexOf('.') + 1) != this.radioXML.getValue())
						addFileValid = false;					
				}
			}
			this.addFileButton.set("disabled", !addFileValid);
			this.deleteFileButton.set("disabled", (this.filePDF == null && this.fileXML == null ? true : false));		
			this.viewPDFButton.set("disabled", (this.filePDF == null ? true : false));
			this.viewXMLButton.set("disabled", (this.fileXML == null ? true : false));
		},
		
		onPDFView: function() {
			if (this.filePDF != null)
				this.showDocument(this.filePDF.id);
		},
		
		onXMLView: function() {
			if (this.fileXML != null)
				this.showDocument(this.fileXML.id);
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
		
		onProdServSearch: function() {
			var dialog = new SolDocBuscarProdServDialog({
				onConfirm: lang.hitch(this, function() {
					var items = dialog.grid.selection.getSelected();
					dialog.hide();
					if (items.length > 0) {
						var item = items[items.length-1]; // select last
						this.conceptoProdServ.setValue(item.clave.toString());
					}
				})
			});
			dialog.setConfig(this.config.context);
			dialog.show();					
		},
		
		onUniMedSearch: function() {
			var dialog = new SolDocBuscarUniMedDialog({
				onConfirm: lang.hitch(this, function() {
					var items = dialog.grid.selection.getSelected();
					dialog.hide();
					if (items.length > 0) {
						var item = items[items.length-1]; // select last
						this.conceptoUniMed.setValue(item.clave.toString());
					}
				})
			});
			dialog.setConfig(this.config.context);
			dialog.show();				
		},		
		
		onSelectFormaPago: function() {
			var dialog = new SolDocFormaPagoDialog({
				title: "Forma de Pago",
				onConfirm: lang.hitch(this, function() {
					this.metodosPago = dialog.getData();
					this.setFormaPago();
					dialog.hide();
				})
			});
			dialog.setData(this.metodosPago);
			dialog.show();					
		},
		
		setFormaPago: function() {
			var metodosPago = "";
			for (key in this.metodosPago) {
				metodosPago += this.metodosPago[key] + " (" + key + "), ";
			}
			if (metodosPago.length > 0) metodosPago = metodosPago.slice(0, -2); 
			this.formaPagoDisplay.innerHTML = metodosPago;			
		},

		onConceptoAdd: function() {
			
			if (!this.conceptoDescripcion.isValid() || !this.conceptoClave.isValid() || !this.conceptoProdServ.isValid() || !this.conceptoUnidad.isValid() || !this.conceptoUniMed.isValid() || !this.conceptoCantidad.isValid() || !this.conceptoPrecioUnitario.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "Datos inválidos.";
				return;	
			}
			
			var precioUnitario = parseFloat(this.conceptoPrecioUnitario.getValue());
			var cantidad = parseFloat(this.conceptoCantidad.getValue());
			var conceptoImporte = precioUnitario * cantidad;
			
			var concepto = {"id": this.gridId++, "conceptoDescripcion": this.conceptoDescripcion.getValue(), "conceptoClave": this.conceptoClave.getValue(), "conceptoProdServ": this.conceptoProdServ.getValue(), "conceptoUnidad": this.conceptoUnidad.getValue(), "conceptoUniMed": this.conceptoUniMed.getValue(), "conceptoCantidad": cantidad, "conceptoPrecioUnitario": precioUnitario, "conceptoImporte": conceptoImporte};
			this.grid.store.newItem(concepto);
			this.grid.render();
			this.resetConceptosValues();

			this.setMontosConceptos();
			
		},
		
		getConceptos: function() {
			var conceptos = [];
			this.grid.store.fetch({
				onItem: lang.hitch(this, function(item) {
					var concepto = {};
					concepto.id = item.id.toString();
					concepto.conceptoDescripcion = item.conceptoDescripcion.toString();
					concepto.conceptoClave = item.conceptoClave.toString();
					concepto.conceptoProdServ = item.conceptoProdServ.toString();
					concepto.conceptoUnidad = item.conceptoUnidad.toString();
					concepto.conceptoUniMed = item.conceptoUniMed.toString();
					concepto.conceptoCantidad = parseInt(item.conceptoCantidad.toString());
					concepto.conceptoPrecioUnitario = parseFloat(item.conceptoPrecioUnitario.toString());
					concepto.conceptoImporte = parseFloat(item.conceptoImporte.toString());
					conceptos.push(concepto);
				})
			});
			return conceptos;
		},		
		
		onConceptoUpdate: function() {
			
			if (!this.conceptoDescripcion.isValid() || !this.conceptoClave.isValid() || !this.conceptoProdServ.isValid() || !this.conceptoUnidad.isValid() || !this.conceptoUniMed.isValid() || !this.conceptoCantidad.isValid() || !this.conceptoPrecioUnitario.isValid()) {
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
				this.grid.store.setValue(item, "conceptoClave", this.conceptoClave.getValue());
				this.grid.store.setValue(item, "conceptoProdServ", this.conceptoProdServ.getValue());
				this.grid.store.setValue(item, "conceptoUnidad", this.conceptoUnidad.getValue());
				this.grid.store.setValue(item, "conceptoUniMed", this.conceptoUniMed.getValue());
				this.grid.store.setValue(item, "conceptoCantidad", cantidad);
				this.grid.store.setValue(item, "conceptoPrecioUnitario", precioUnitario);
				this.grid.store.setValue(item, "conceptoImporte", conceptoImporte);
			}));
			
			this.grid.render();	
			this.resetConceptosValues();

			this.setMontosConceptos();
			
		},
		
		onConceptoDelete: function() {

			var items = this.grid.selection.getSelected();		
			if (items.length <= 0)
				return;
			
			array.forEach(items, lang.hitch(this, function(item) {
				this.grid.store.deleteItem(item);
			}));
			
			this.grid.render();
			this.resetConceptosValues();

			this.setMontosConceptos();
		},	

		resetConceptosValues: function() {
			this.conceptoDescripcion.reset();
			this.conceptoClave.reset();
			this.conceptoProdServ.reset();
			this.conceptoUnidad.reset();
			this.conceptoUniMed.reset();
			this.conceptoCantidad.reset();
			this.conceptoPrecioUnitario.reset();
			this.progressBar.style.display = "none";
			this.message.innerHTML = "";			
		},		
		
		formatEstadoCFDI: function(num) {
			var estadoCFDI = parseInt(num);
			switch(estadoCFDI) {
				case 0:
					return "PENDIENTE POR PAGAR";
					break;
				case 1:
					return "PAGADA POR CLIENTE";
					break;
				case 2:
					return "DEVUELTA POR PROVEEDOR";
					break;					
				case 3:
					return "DEVUELTA A CLIENTE";
					break;				
			}			
		},	
		
		formatCurrency: function(num) {
		    var p = num.toFixed(2).split(".");
		    return p[0].split("").reverse().reduce(function(acc, num, i, orig) {
		        return  num + (i && !(i % 3) ? "," : "") + acc;
		    }, "") + "." + p[1];
		},
		
	    parseFloat: function(str) {
	    	var strVal = str.replace(/[,$]/g,''); 
	    	return parseFloat(strVal);
	    },
	    
		trunc: function(value) {
			var arr = (value + "").split(".");
			var intpart = parseInt(arr[0]);
			return intpart;
		},	    
	    
		getDataValue: function(dVal) {
	    	// expected: dd/MM/yyyy
	    	// convert to: yyyy-MM-dd	
	    	if (dVal == null || dVal.length == 0)
	    		return dVal;
	    	return dVal.substring(6,10) + "-" +  dVal.substring(3,5) + "-" + dVal.substring(0,2);  		
		},
		
	    formatDate: function(dVal) {
	    	// expected: yyyy-MM-dd
	    	// convert to: dd/MM/yyyy
	    	if (dVal == null || dVal.length == 0)
	    		return dVal;
	    	return dVal.substring(8,10) + "/" + dVal.substring(5,7) + "/" + dVal.substring(0,4);  	
	    }  		
		
	});
});