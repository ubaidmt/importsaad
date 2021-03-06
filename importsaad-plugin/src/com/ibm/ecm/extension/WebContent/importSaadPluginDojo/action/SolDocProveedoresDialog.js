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
		"dojo/text!./templates/SolDocProveedoresDialog.html"
	],
	function(declare, lang, connect, registry, array, json, domStyle, domGeom, Request, DataGrid, ItemFileWriteStore, BaseDialog, template) {

	/**
	 * @name importSaadPluginDojo.action.SolDocProveedoresDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments ecm.widget.dialog.BaseDialog
	 */
	return declare("importSaadPluginDojo.action.SolDocProveedoresDialog", [ BaseDialog ], {
	/** @lends importSaadPluginDojo.action.SolDocProveedoresDialog.prototype */	

		contentString: template,
		widgetsInTemplate: true,		
		grid: null,
		repositoryId: null,
		context: null,	

		postCreate: function() {
			this.inherited(arguments);
			this.setResizable(false);
			this.setMaximized(false);
			this.setSize(800, 600);
			this.setTitle("Administración de Proveedores");
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
			var gridHeight = size.h - 340;
			if (this.isMaximized())
				gridHeight += 70; // if maximized, no header is included
			domStyle.set(this.proveedoresGrid, "height", gridHeight + "px");	

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
			params.method = "getSolDocProveedores";
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
									items: response.proveedores
								}
							});				
						
							var gridLayout = [
								{name: 'Id', field: 'id', hidden: true},
								{name: 'Nombre del Proveedor', field: 'proveedorNombre', width: '50%', editable: false},
								{name: 'Contacto Nombre', field: 'contactoNombre', width: '25%', editable: false},
								{name: 'Correo Principal', field: 'contactoMailTo', width: '25%', editable: false}
							];
							
							this.grid = new DataGrid({
								id: 'grid',
								store: gridStore,
								structure: gridLayout,
								selectionMode: 'multi',
								sortInfo: 2, // asc by Nombre
								autoHeight: true
							});

							this.grid.placeAt(this.proveedoresGrid);			
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
				this.proveedorNombre.setValue(item.proveedorNombre.toString());
				this.contactoNombre.setValue(item.contactoNombre.toString());
				this.contactoMailTo.setValue(item.contactoMailTo.toString());
				this.contactoMailCc.setValue(item.contactoMailCc.toString());
				this.contactoTelefono.setValue(item.contactoTelefono.toString());	
				this.activo.set("checked", (item.activo.toString() === "true"));
			}		
		},
		
		onUpdate: function() {
			
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				this.message.innerHTML = "Es necesario seleccionar un proveedor.";
				return;				
			}	
			var item = items[items.length-1]; // select last
			
			if (!this.proveedorNombre.isValid() || !this.contactoNombre.isValid() || !this.contactoMailTo.isValid()) {			
				this.message.innerHTML = "Datos inválidos.";
				return;	
			}		
			
			this.message.innerHTML = "Actualizando...";
			this.progressBar.style.display = "block";			
			
			var proveedor = {"id": this.grid.store.getValue(item, 'id'), "proveedorNombre": this.proveedorNombre.getValue(), "contactoNombre": this.contactoNombre.getValue(), "contactoMailTo": this.contactoMailTo.getValue(), "contactoMailCc": this.contactoMailCc.getValue(), "contactoTelefono": this.contactoTelefono.getValue(), "activo": (this.activo.getValue() === "true")};
			
			var params = {};
			params.method = "updateSolDocProveedores";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.proveedor = json.stringify(proveedor);
			
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
				this.message.innerHTML = "Es necesario seleccionar un proveedor.";
				return;				
			}			
			
			var jsonData = new Array();
			array.forEach(items, lang.hitch(this, function(item) {
				var servicio = {"id": this.grid.store.getValue(item, 'id')};
				jsonData.push(servicio);
			}));
			
			this.message.innerHTML = "Eliminando...";
			this.progressBar.style.display = "block";
			
			var params = {};
			params.method = "deleteSolDocProveedores";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.proveedores = json.stringify(jsonData);
			
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
			
			if (!this.proveedorNombre.isValid() || !this.contactoNombre.isValid() || !this.contactoMailTo.isValid()) {
				this.message.innerHTML = "Datos inválidos.";
				return;	
			}			

			this.message.innerHTML = "Creando...";
			this.progressBar.style.display = "block";
			
			var params = {};
			params.method = "addSolDocProveedores";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			var proveedor = {"proveedorNombre": this.proveedorNombre.getValue(), "contactoNombre": this.contactoNombre.getValue(), "contactoMailTo": this.contactoMailTo.getValue(), "contactoMailCc": this.contactoMailCc.getValue(), "contactoTelefono": this.contactoTelefono.getValue(), "activo": (this.activo.getValue() === "true")};				
			params.proveedor = json.stringify(proveedor);
			
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
			params.context = json.stringify(this.context);
			params.tipo = 2; // proveedores
			
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
			this.proveedorNombre.reset();
			this.contactoNombre.reset();
			this.contactoMailTo.reset();
			this.contactoMailCc.reset();
			this.contactoTelefono.reset();
			this.activo.reset();
			this.message.innerHTML = "";
			this.progressBar.style.display = "none";	
		}	
			
	});
});
