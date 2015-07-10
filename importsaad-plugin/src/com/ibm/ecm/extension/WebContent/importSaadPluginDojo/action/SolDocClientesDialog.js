define([
		"dojo/_base/declare",
		"dojo/_base/lang",	
		"dojo/_base/connect",
		"dijit/registry",
		"dojo/_base/array",	
		"dojo/json",	
		"dojo/dom-style",
		"dojo/dom-geometry",
    	"ecm/model/Request",	
    	"dojox/grid/DataGrid",
    	"dojo/data/ItemFileWriteStore",    	
		"ecm/widget/dialog/BaseDialog",	
		"dojo/text!./templates/SolDocClientesDialog.html"
	],
	function(declare, lang, connect, registry, array, json, domStyle, domGeom, Request, DataGrid, ItemFileWriteStore, BaseDialog, template) {

	/**
	 * @name importSaadPluginDojo.action.SolDocClientesDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments ecm.widget.dialog.BaseDialog
	 */
	return declare("importSaadPluginDojo.action.SolDocClientesDialog", [ BaseDialog ], {
	/** @lends importSaadPluginDojo.action.SolDocClientesDialog.prototype */	

		contentString: template,
		widgetsInTemplate: true,		
		grid: null,
		repositoryId: null,
		context: null,	

		postCreate: function() {
			this.inherited(arguments);
			this.setResizable(false);
			this.setMaximized(false);
			this.setSize(800, 800);
			this.setTitle("Administración de Clientes");
			this.depurarButton = this.addButton("Depurar", "onDepurar", false, false);
			this.deleteButton = this.addButton("Eliminar", "onDelete", false, false);
			this.updateButton = this.addButton("Actualizar", "onUpdate", false, false);
			this.refreshButton = this.addButton("Refrescar", "onLoad", false, false);
			this.addButton = this.addButton("Agregar", "onAdd", false, true);
			this.cancelButton.set("label", "Cerrar");
		},				
		
		showDialog: function() {
			this.onLoad();
			this.inherited("show", []);		
		},
		
		hide: function() {
			this.grid.destroy();
			this.grid = null;
			this.inherited("hide", []);		
		},

		resize: function() {
			this.inherited(arguments);

			var size = domGeom.getContentBox(this.domNode);
			var gridHeight = size.h - 530;
			if (this.isMaximized())
				gridHeight += 70; // if maximized, no header is included
			domStyle.set(this.clientesGrid, "height", gridHeight + "px");	

			if (this.grid != null) {
		    	this.grid.resize();
		    	this.grid.update();		
	    	}    	
		},			
		
		setConfig: function(config) {
			this.config = config;
		},				
		
		onLoad: function() {
			
			this.message.innerHTML = "Cargando...";
			this.progressBar.style.display = "block";			
			
			if (this.grid != null) {
				this.grid.destroy();
				this.grid = null;				
			}		
			
			var params = {};
			params.method = "getSolDocClientes";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			
			Request.invokePluginService("ImportSaadPlugin", "SolDocService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";													
								return;
							}
						
							var gridStore = new ItemFileWriteStore({
								data: {
									identifier: "id",
									items: response.clientes
								}
							});				
						
							var gridLayout = [
								{name: 'Id', field: 'id', hidden: true},
								{name: 'Razón Social', field: 'razonSocial', width: '75%', editable: false},
								{name: 'RFC', field: 'rfc', width: '25%', editable: false}
							];
							
							this.grid = new DataGrid({
								id: 'grid',
								store: gridStore,
								structure: gridLayout,
								selectionMode: 'multi',
								sortInfo: 2, // asc by Nombre
								autoHeight: true
							});

							this.grid.placeAt(this.clientesGrid);			
							this.grid.startup();
							
							connect.connect(this.grid, "onClick", lang.hitch(this, function() {
								var t = this;
								this.onGridItemSelect(t);
							}));
							
							connect.connect( this.grid, "onStyleRow", lang.hitch(this, function(row) {
								var item = this.grid.getItem( row.index );
								if ( item == null ) return;
								if (row.odd) row.customStyles += 'background-color:#F7F6F6;';
							}));	
							
							this.resetValues();							
							
						})
					}); 		
					
		},	
		
		onGridItemSelect: function(t) {
			var items = t.grid.selection.getSelected();
			if (items.length > 0) {
				var item = items[items.length-1]; // select last
				this.razonSocial.setValue(item.razonSocial.toString());
				this.rfc.setValue(item.rfc.toString());
				this.direccionFiscal.setValue(item.direccionFiscal.toString());
				this.contactoNombre.setValue(item.contactoNombre.toString());
				this.contactoMailTo.setValue(item.contactoMailTo.toString());
				this.contactoMailCc.setValue(item.contactoMailCc.toString());
				this.contactoTelefono.setValue(item.contactoTelefono.toString());
				this.devolucionDirectaProveedor.setValue("devolucionDirectaProveedor" in item ? item.devolucionDirectaProveedor[0] : false);
				this.porcentajeComisionProveedor.setValue(item.porcentajeComisionProveedor.toString());
				this.porcentajeComisionDistribuidor.setValue(item.porcentajeComisionDistribuidor.toString());
				this.numeroCuenta.setValue("numeroCuenta" in item ? item.numeroCuenta.toString() : "");
				this.activo.set("checked", (item.activo.toString() === "true"));
			}		
		},
		
		onUpdate: function() {
			
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				this.message.innerHTML = "Es necesario seleccionar un cliente.";
				return;				
			}
			var item = items[items.length-1]; // select last
			
			if (!this.razonSocial.isValid() || !this.rfc.isValid() || !this.direccionFiscal.isValid() || !this.contactoNombre.isValid() || !this.contactoMailTo.isValid() || !this.porcentajeComisionProveedor.isValid() || !this.porcentajeComisionDistribuidor.isValid() || !this.numeroCuenta.isValid()) {
				this.message.innerHTML = "Datos inválidos.";
				return;	
			}		
			
			this.message.innerHTML = "Actualizando...";
			this.progressBar.style.display = "block";
			
			var cliente = {"id": item.id.toString(), "razonSocial": this.razonSocial.getValue(), "rfc": this.rfc.getValue(), "direccionFiscal": this.direccionFiscal.getValue(), "contactoNombre": this.contactoNombre.getValue(), "contactoMailTo": this.contactoMailTo.getValue(), "contactoMailCc": this.contactoMailCc.getValue(), "contactoTelefono": this.contactoTelefono.getValue(), "porcentajeComisionProveedor": parseFloat(this.porcentajeComisionProveedor.getValue()), "porcentajeComisionDistribuidor": parseFloat(this.porcentajeComisionDistribuidor.getValue()), "devolucionDirectaProveedor": (this.devolucionDirectaProveedor.getValue() === "true"), "numeroCuenta": this.numeroCuenta.getValue(), "activo": (this.activo.getValue() === "true")};	
			
			var params = {};
			params.method = "updateSolDocClientes";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.cliente = json.stringify(cliente);
			
			Request.invokePluginService("ImportSaadPlugin", "SolDocService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";						
								return;
							}	
								
							this.onLoad();				
							
						})
					}); 							
		},
		
		onDelete: function() {		

			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				this.message.innerHTML = "Es necesario seleccionar un cliente.";
				return;				
			}
			
			var jsonData = new Array();
			array.forEach(items, lang.hitch(this, function(item) {
				var cliente = {"id": item.id.toString()};
				jsonData.push(cliente);
			}));
			
			this.message.innerHTML = "Eliminando...";
			this.progressBar.style.display = "block";
			
			var params = {};
			params.method = "deleteSolDocClientes";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.clientes = json.stringify(jsonData);
			
			Request.invokePluginService("ImportSaadPlugin", "SolDocService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";						
								return;
							}	
								
							this.onLoad();								
							
						})
					}); 			
		},
		
		onAdd: function() {
			
			if (!this.razonSocial.isValid() || !this.rfc.isValid() || !this.direccionFiscal.isValid() || !this.contactoNombre.isValid() || !this.contactoMailTo.isValid() || !this.porcentajeComisionProveedor.isValid() || !this.porcentajeComisionDistribuidor.isValid() || !this.numeroCuenta.isValid()) {			
				this.message.innerHTML = "Datos inválidos.";
				return;	
			}

			this.message.innerHTML = "Creando...";
			this.progressBar.style.display = "block";
			
			var params = {};
			params.method = "addSolDocClientes";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			var cliente = {"razonSocial": this.razonSocial.getValue(), "rfc": this.rfc.getValue(), "direccionFiscal": this.direccionFiscal.getValue(), "contactoNombre": this.contactoNombre.getValue(), "contactoMailTo": this.contactoMailTo.getValue(), "contactoMailCc": this.contactoMailCc.getValue(), "contactoTelefono": this.contactoTelefono.getValue(), "porcentajeComisionProveedor": parseFloat(this.porcentajeComisionProveedor.getValue()), "porcentajeComisionDistribuidor": parseFloat(this.porcentajeComisionDistribuidor.getValue()), "devolucionDirectaProveedor": (this.devolucionDirectaProveedor.getValue() === "true"), "numeroCuenta": this.numeroCuenta.getValue(), "activo": (this.activo.getValue() === "true")};
			params.cliente = json.stringify(cliente);
			
			Request.invokePluginService("ImportSaadPlugin", "SolDocService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";						
								return;
							}	
								
							this.onLoad();				
							
						})
					}); 				
		},	

		onDepurar: function() {

			this.message.innerHTML = "Depurando catálogo...";
			this.progressBar.style.display = "block";
			
			var params = {};
			params.method = "depurarSolDocCatalogos";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.tipo = 3; // clientes
			
			Request.invokePluginService("ImportSaadPlugin", "SolDocService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";						
								return;
							}	
							
							this.onLoad();
							
						})
					}); 				
		},			
		
		resetValues: function() {
			this.razonSocial.reset();
			this.rfc.reset();
			this.direccionFiscal.reset();
			this.contactoNombre.reset();
			this.contactoMailTo.reset();
			this.contactoMailCc.reset();
			this.contactoTelefono.reset();
			this.porcentajeComisionProveedor.reset();
			this.porcentajeComisionDistribuidor.reset();
			this.numeroCuenta.reset();
			this.activo.reset();
			this.devolucionDirectaProveedor.reset();
			this.message.innerHTML = "";
			this.progressBar.style.display = "none";					
		}   
			
	});
});
