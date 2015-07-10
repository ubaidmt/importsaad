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
	"dojo/text!./templates/SolDocPagosClientesDialog.html"
	],
	function (declare, lang, connect, Request, BaseDialog, Button, Select, array, json, dom, domStyle, domGeom, DataGrid, timing, ItemFileWriteStore, ObjectStore, Memory, Desktop, template) {
	
	/**
	 * @name importSaadPluginDojo.action.SolDocPagosClientesDialog
	 * @class
	 * @augments ecm.widget.dialog.BaseDialog
	 */
	return declare("importSaadPluginDojo.action.SolDocPagosClientesDialog", [BaseDialog], {
	/** @lends importSaadPluginDojo.action.SolDocPagosClientesDialog.prototype */

		//needed to load from template
		contentString: template,
		widgetsInTemplate: true,
		gridFacturas: null,		
		repositoryId: null,
		context: null,	
		clientes: null,
		comprobantePago: null,
		ticker: null,

		postCreate: function() {
			this.inherited(arguments);
			this.setResizable(false);
			this.setMaximized(true);
			this.setTitle("Pagos de Clientes");
			this.setIntroText("Registro de pagos a facturas solicitadas por los clientes.");
			this.resetButton = this.addButton("Limpiar", "onReset", false, false);						
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
			this.clientes.destroy();
			this.clientes = null;
			this.empresas.destroy();
			this.empresas = null;			
			this.gridFacturas.destroy();
			this.gridFacturas = null;				
			this.inherited("hide", []);		
		},

		resize: function() {
			this.inherited(arguments);

			var size = domGeom.getContentBox(this.domNode);
			var gridHeight = size.h - 480;
			if (this.isMaximized())
				gridHeight += 70; // if maximized, no header is included
			domStyle.set(this.gridFacturasDiv, "height", gridHeight + "px");	

			if (this.gridFacturas != null) {
		    	this.gridFacturas.resize();
		    	this.gridFacturas.update();		
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

		setConfig: function(config) {
			this.config = config;
		},
		
		isValid: function() {
			var valid = this._fileInput;
			// This test works for both HTML5 and non-HTML5 browsers. 
			valid = valid && (this._fileInput.value) && (this._fileInput.value.length > 0);
			return valid;
		},
		
		checkFileButtons: function() {
			this.addFileButton.set("disabled", (this.isValid() ? false : true));
			this.deleteFileButton.set("disabled", (this.comprobantePago == null ? true : false));
			domStyle.set(this.comprobanteFilePane, "display", (this.comprobantePago == null ? "none" : "block"));
		},		
									
		onLoad: function() {
			
			this.onReset();
			this.loadClientes();
			
			// Grid Facturas Pendientes por Pagar
			this.resetGridFacturasValues();			
								
			// Empresas
			if (this.empresas != null) {
				this.empresas.destroy();
				this.empresas = null;					
			}
			
			var store = new Memory({
				data:  [{ "id": "", "label": "--SELECCIONAR--"}]
			});							
		    
			var os = new ObjectStore({ objectStore: store });
			this.empresas = new Select({
				id: "empresas",
				required: false,
		        store: os
		    });								
			
			this.empresas.placeAt(this.empresasDiv);			
			this.empresas.startup();	
			
			connect.connect(this.empresas, "onChange", lang.hitch(this, function() {
				this.onEmpresaChange();	
			}));			
			
		},
		
		onGridFacturasItemSelect: function() {
			var items = this.gridFacturas.selection.getSelected();
			var importeTotalFacturas = 0;	
			var montoComisionProveedor = 0;
			var importeDevolucionProveedor = 0;
			array.forEach(items, lang.hitch(this, function(item) {
				importeTotalFacturas += parseFloat(this.gridFacturas.store.getValue(item, 'importe'));
				montoComisionProveedor += parseFloat(this.gridFacturas.store.getValue(item, 'montoComisionProveedor'));
			}));
			importeDevolucionProveedor = importeTotalFacturas - montoComisionProveedor;
			this.importeTotalFacturas.innerHTML = this.formatCurrency(importeTotalFacturas);
			this.importeDevolucion.innerHTML = this.formatCurrency(importeDevolucionProveedor);
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
				
		loadClientes: function() {							
			
			var params = {};
			params.method = "getSolDocClientes";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
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
							items.push({ "id": "", "label": "--SELECCIONAR--" });
							array.forEach(response.clientes, lang.hitch(this, function(item) {
								items.push({ "id": item.id, "label": item.razonSocial });
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
							
							connect.connect(this.clientes, "onChange", lang.hitch(this, function() {
								this.onClienteChange();	
							}));					
													
						})
					}); 			
		},
		
		onClienteChange: function() {
			this.loadSaldoCliente();
			this.resetGridFacturasValues();
			this.loadEmpresas();
		},
		
		onEmpresaChange: function() {
			this.loadFacturas();
		},
		
		loadSaldoCliente: function() {
			
			this.saldoCliente.innerHTML = "0.00";
			var cliente = this.getSelectedCliente();
			if (cliente == null)
				return;
						
			var params = {};
			params.method = "getSaldoFacturas";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.cliente = json.stringify(cliente);
			params.estado = 0; // pendiente de pago
			
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
							
							this.saldoCliente.innerHTML = this.formatCurrency(response.saldo);						
						})
					}); 			
		},
		
		loadEmpresas: function() {
					
			var cliente = this.getSelectedCliente();
			
			if (cliente == null) {
				var items = [{ "id": "", "label": "--SELECCIONAR--"}];
				var store = new Memory({
					data: items
				});							
				var os = new ObjectStore({ objectStore: store });
				this.empresas.setStore(os);	
				return;
			}
						
			var params = {};
			params.method = "getEmpresas";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.cliente = json.stringify(cliente);
			params.estado = 0; // pendiente por pagar
			
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
							
							var items = [];
							items.push({ "id": "", "label": "--SELECCIONAR--" });
							array.forEach(response.empresas, lang.hitch(this, function(item) {
								items.push(item);
							}));								
							
							var store = new Memory({
								data: items
							});							
						    
							var os = new ObjectStore({ objectStore: store });
							this.empresas.setStore(os);				
													
						})
					}); 			
		},
		
		loadFacturas: function() {
			
			var cliente = this.getSelectedCliente();
			var empresa = this.getSelectedEmpresa();

			this.resetGridFacturasValues();
			
			if (cliente == null || empresa == null) {
				return;
			}

			var params = {};
			params.method = "searchFacturas";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.cliente = json.stringify(cliente);
			params.empresa = json.stringify(empresa);
			params.estado = 0; // pendiente por pagar
			
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

							array.forEach(response.facturas, lang.hitch(this, function(factura) {
								this.gridFacturas.store.newItem(factura);
							}));	
							this.gridFacturas.render();								
													
						})
					});			
		},
		
		onMetodoPagoChange: function() {
			domStyle.set(this.metodoPagoTransferenciaPane, "display", "none");
			domStyle.set(this.metodoPagoChequePane, "display", "none");
			domStyle.set(this.metodoPagoEfectivoPane, "display", "none");
			var metodoPago = parseInt(this.metodoPago.getValue());
			switch(metodoPago) {
				case 2: // transferencia
					domStyle.set(this.metodoPagoTransferenciaPane, "display", "block");
					break;
				case 1: // cheque
					domStyle.set(this.metodoPagoChequePane, "display", "block");
					break;
				case 3: // deposito en efectivo
					domStyle.set(this.metodoPagoEfectivoPane, "display", "block");
					break;					
			}
		},			
				
		resetGridFacturasValues: function() {
			
			this.importeTotalFacturas.innerHTML = "0.00";
			this.importeDevolucion.innerHTML = "0.00";

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
				{name: '% Comisión Proveedor', field: 'porcentajeComisionProveedor', width: '15%', editable: false, formatter: this.formatCurrency},
				{name: 'Comisión Proveedor', field: 'montoComisionProveedor', width: '15%', editable: false, formatter: this.formatCurrency}
			];
			
			this.gridFacturas = new DataGrid({
				id: 'gridFacturas',
				store: gridStore,
				structure: gridLayout,
				selectionMode: 'multi',
				sortInfo: 2, // asc by folio
				autoHeight: true
			});

			this.gridFacturas.placeAt(this.gridFacturasDiv);			
			this.gridFacturas.startup();
			
			connect.connect(this.gridFacturas, "onClick", lang.hitch(this, function() {
				this.onGridFacturasItemSelect();
			}));			
			
			connect.connect(this.gridFacturas, "onStyleRow", lang.hitch(this, function(row) {
				var item = this.gridFacturas.getItem( row.index );
				if ( item == null ) return;
				if (row.odd) row.customStyles += 'background-color:#F7F6F6;';
			}));				

		},			
						
		onReset: function() {
			this.onAfterSave();
			if (this.clientes != null) {
				this.saldoCliente.innerHTML = "0.00";
				this.clientes.reset();
			}
			if (this.empresas != null)
				this.empresas.reset();			
			this.message.innerHTML = "";
			this.progressBar.style.display = "none";					
		},
		
		onAfterSave: function() {
			this.fechaPago.setValue(new Date());	
			this.importePago.reset();
			this.metodoPago.reset();
			this.bancoCheque.reset();
			this.bancoDeposito.reset();
			this.referenciaTransferencia.reset();
			this.referenciaDeposito.reset();
			this.onMetodoPagoChange();
			this.onFileDelete();
			domStyle.set(this.confirmacionPane, "display", "none");
			this.resetGridFacturasValues();
			this.observaciones.reset();
			this.omitirEnvioCorreo.reset();
		},
		
		validaGuardar: function() {
			
			// Ocultar panel de confirmacion
			domStyle.set(this.confirmacionPane, "display", "none");
			this.confirmationMessage.innerHTML = "";
			this.message.innerHTML = "";			
			
			// Valida seleccion del cliente
			if (this.clientes.getValue() == "") {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "Es necesario seleccionar un cliente.";
				return false;				
			}
			
			// Valida seleccion de la empresa
			if (this.empresas.getValue() == "") {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "Es necesario seleccionar una empresa.";
				return false;				
			}				

			// Valida seleccion de facturas
			var items = this.gridFacturas.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "Es nesesario seleccionar al menos una factura para asociar el comprobante de pago.";
				return;				
			}	
			
			// Valida fecha e importe pago
			if (!this.fechaPago.isValid() || !this.importePago.isValid()) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "La fecha o el importe del comprobante son inválidos.";
				return false;	
			}		
			
			// Valida metodo de pago
			var metodoPago = parseInt(this.metodoPago.getValue());
			switch(metodoPago) {
				case 2: // transferencia
					if (!this.referenciaTransferencia.isValid()) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Número de referencia inválida.";
						return false;						
					}
					break;
				case 1: // cheque
					if (!this.numeroCheque.isValid() || !this.bancoCheque.isValid()) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Banco o número de cheque inválido.";
						return false;						
					}
					break;
				case 3: // deposito en efectivo
					if (!this.referenciaDeposito.isValid() || !this.bancoDeposito.isValid()) {
						domStyle.set(this.message, "color", "#6B0A0A");
						this.message.innerHTML = "Banco o número de referencia inválido.";
						return false;					
					}
					break;					
			}			

			// Valida importe de las facturas vs importe del comprobante de pago 
			if (this.parseFloat(this.importePago.getValue()) != this.parseFloat(this.importeTotalFacturas.innerHTML)) {
				domStyle.set(this.message, "color", "#6B0A0A");
				this.message.innerHTML = "El importe de las facturas seleccionadas no corresponde al importe del comprobante de pago a registrar.";
				return false;					
			}
			
			return true;
		},
		
		onCancelar: function() {
			// Ocular panel de confirmacion
			domStyle.set(this.confirmacionPane, "display", "none");
			this.confirmationMessage.innerHTML = "";
		},
		
		onConfirmar: function() {
			    	
			if (!this.validaGuardar())
				return;		
								
			domStyle.set(this.message, "color", "#000253");
			this.message.innerHTML = "Asociando pago...";
			this.progressBar.style.display = "block";			
	
			var params = {};
			params.method = "savePagosDeCliente";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			
			// Pago	
			var pago = {"fechaPago": this.getFormatedDateValue(this.fechaPago.getValue()), "importe": parseFloat(this.importePago.getValue()), "metodoPago": parseInt(this.metodoPago.getValue()), comprobantePago: this.comprobantePago};
			var metodoPago = parseInt(this.metodoPago.getValue());
			switch(metodoPago) {
				case 2: // transferencia
					pago.referencia = this.referenciaTransferencia.getValue();
					break;
				case 1: // cheque
					pago.referencia = this.numeroCheque.getValue();
					pago.banco = parseInt(this.bancoCheque.getValue());
					break;
				case 0: // entrega en efectivo
					pago.referencia = this.nombrePersona.getValue();
					break;							
				case 3: // deposito en efectivo
					pago.referencia = this.referenciaDeposito.getValue();
					pago.banco = parseInt(this.bancoDeposito.getValue());
					break;					
			}	
			params.pago = json.stringify(pago);			
						
			// Facturas
			var items = this.gridFacturas.selection.getSelected();			
			var facturas = new Array();
			array.forEach(items, lang.hitch(this, function(item) {
				var factura = {"id": this.gridFacturas.store.getValue(item, 'id')};
				facturas.push(factura);
			}));	
			params.facturas = json.stringify(facturas);	
			
			// Datos Adicionales
			var datos = {
					"observaciones": this.observaciones.getValue(),
					"omitirEnvioCorreo": (this.omitirEnvioCorreo.getValue() === "true")
			};
			params.datos = json.stringify(datos);
			
			Request.invokePluginService("ImportSaadPlugin", "SolDocPagosService",
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
								this.message.innerHTML = "El pago se asoció correctamente, sin embargo, ocurrió un error al intentar enviar la solicitud de la devolución al proveedor. " + response.error;
								this.progressBar.style.display = "none";
							} 
							else
							{
								domStyle.set(this.message, "color", "#000253");
								this.message.innerHTML = "El pago se asoció correctamente y la solicitud de la devolución ha sido enviada al proveedor.";
								this.progressBar.style.display = "none";
							}
							
							this.onAfterSave();	
							this.loadFacturas();
						})
					}); 
		},		
		
		onSave: function() {
				
			if (!this.validaGuardar())
				return false;
			
			var cliente = this.getSelectedCliente();
					
			// Mostrar panel de confirmacion
			this.confirmationMessage.innerHTML = "¿Está seguro de asociar el comprobante de pago del cliente " + cliente.label + " a las facturas seleccionadas?";
			domStyle.set(this.confirmacionPane, "display", "block");			
						
		},
				
		getSelectedCliente: function() {
			var cliente = null;
			array.forEach(this.clientes.options, lang.hitch(this, function(option) {
				if (option.selected && option.item.id != "")
					cliente = option.item;
			}));
			return cliente;
		},
		
		getSelectedEmpresa: function() {
			var empresa = null;
			array.forEach(this.empresas.options, lang.hitch(this, function(option) {
				if (option.selected && option.item.id != "")
					empresa = option.item;
			}));
			return empresa;
		},		
		
		onFileDelete: function() {
			this.fileName.innerHTML = "";
			this.comprobantePago = null;			
			this.checkFileButtons();	
		},
		
		onFileAdd: function() {
			
			domStyle.set(this.message, "color", "#000253");
			this.message.innerHTML = "Anexando comprobante...";
			this.progressBar.style.display = "block";				
						
			var callback = lang.hitch(this, this._onFileAddCompleted);
			
			var params = {};
			params.method = "addDocument";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.parentFolder = "/Temporal/Documentos";	
			
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
			
			this.comprobantePago = { "id": response.document.id, "name": response.document.documentTitle};
			this.fileName.innerHTML = response.document.documentTitle;
									
			this.checkFileButtons();
			
			this.message.innerHTML = ""
			this.progressBar.style.display = "none";				
		},
		
		onFileView: function() {
			if (this.comprobantePago != null)
				this.showDocument(this.comprobantePago.id);
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
		}	
		
	});
});