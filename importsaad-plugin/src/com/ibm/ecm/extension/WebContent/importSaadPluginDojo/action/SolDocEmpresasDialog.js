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
    	"ecm/widget/Select",
		"dojo/data/ObjectStore",
		"dojo/store/Memory",    	
		"ecm/widget/dialog/BaseDialog",	
		"dojo/text!./templates/SolDocEmpresasDialog.html"
	],
	function(declare, lang, connect, registry, array, json, domStyle, domGeom, Request, DataGrid, ItemFileWriteStore, Select, ObjectStore, Memory, BaseDialog, template) {

	/**
	 * @name importSaadPluginDojo.action.SolDocEmpresasDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments ecm.widget.dialog.BaseDialog
	 */
	return declare("importSaadPluginDojo.action.SolDocEmpresasDialog", [ BaseDialog ], {
	/** @lends importSaadPluginDojo.action.SolDocEmpresasDialog.prototype */	

		contentString: template,
		widgetsInTemplate: true,		
		grid: null,
		repositoryId: null,
		proveedores: null,
		context: null,	

		postCreate: function() {
			this.inherited(arguments);
			this.setResizable(false);
			this.setMaximized(false);
			this.setSize(800, 800);
			this.setTitle("Administración de Empresas");
			this.depurarButton = this.addButton("Depurar", "onDepurar", false, false);
			this.deleteButton = this.addButton("Eliminar", "onDelete", false, false);
			this.updateButton = this.addButton("Actualizar", "onUpdate", false, false);
			this.cleanButton = this.addButton("Refrescar", "onClean", false, false);
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
			this.proveedores.destroy();
			this.proveedores = null;
			this.inherited("hide", []);		
		},

		resize: function() {
			this.inherited(arguments);

			var size = domGeom.getContentBox(this.domNode);
			var gridHeight = size.h - 260;
			if (this.isMaximized())
				gridHeight += 70; // if maximized, no header is included
			domStyle.set(this.gridEmpresasDiv, "height", gridHeight + "px");	

			if (this.grid != null) {
		    	this.grid.resize();
		    	this.grid.update();		
	    	}
		},			

		setConfig: function(config) {
			this.config = config;
		},				
		
		onLoad: function() {
						
			this.loadProveedores();			
			this.resetGrid();	
					
		},	

		loadProveedores: function() {				
			
			var params = {};
			params.method = "getSolDocProveedores";
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
							items.push({ "id": "", "label": "--SELECCIONAR--"});
							array.forEach(response.proveedores, lang.hitch(this, function(item) {
								items.push({ "id": item.id, "label": item.proveedorNombre});
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
								this.onProveedoresChange();
							}));									
							
						})
					}); 			
		},	

		onProveedoresChange: function() {

			this.resetGrid();			

			var proveedor = this.getSelectedProveedor();
			if (proveedor == null) {
				return;
			}

			var params = {};
			params.method = "getSolDocEmpresas";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.proveedor = proveedor.label;
			
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

							array.forEach(response.empresas, lang.hitch(this, function(empresa) {
								this.grid.store.newItem(empresa);
							}));	
							this.grid.render();														
							
						})
					}); 										

		},

		getSelectedProveedor: function() {
			var proveedor = null;
			array.forEach(this.proveedores.options, lang.hitch(this, function(option) {
				if (option.selected && option.item.id != "")
					proveedor = option.item;
			}));
			return proveedor;
		},	

		resetGrid: function() {
			
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
				{name: 'Id', field: 'id', hidden: true},
				{name: 'Nombre de la Empresa', field: 'name', width: '50%', editable: false}
			];
			
			this.grid = new DataGrid({
				id: 'grid',
				store: gridStore,
				structure: gridLayout,
				selectionMode: 'multi',
				sortInfo: 2, // asc by Nombre
				autoHeight: true
			});

			this.grid.placeAt(this.gridEmpresasDiv);			
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

		},				
		
		onGridItemSelect: function(t) {
			var items = t.grid.selection.getSelected();
			if (items.length > 0) {
				var item = items[items.length-1]; // select last
				this.empresaNombre.setValue(item.name.toString());
				this.activo.set("checked", (item.activo.toString() === "true"));
			}
		},
		
		onUpdate: function() {

			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				this.message.innerHTML = "Es necesario seleccionar una empresa.";
				return;				
			}
			var item = items[items.length-1]; // select last
			
			var proveedor = this.getSelectedProveedor();
			if (proveedor == null) {
				this.message.innerHTML = "Proveedor inválido.";
				return;	
			}			
			
			if (!this.empresaNombre.isValid()) {		
				this.message.innerHTML = "Datos inválidos.";
				return;	
			}

			this.message.innerHTML = "Actualizando...";
			this.progressBar.style.display = "block";			
			
			var empresa = {"id": item.id.toString(), "empresaNombre": this.empresaNombre.getValue(), "activo": (this.activo.getValue() === "true")};
			
			var params = {};
			params.method = "updateSolDocEmpresas";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.empresa = json.stringify(empresa);
			params.proveedorid = proveedor.id;
			
			Request.invokePluginService("ImportSaadPlugin", "SolDocService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";						
								return;
							}	

							this.empresaNombre.reset();
							this.message.innerHTML = "";
							this.progressBar.style.display = "none";								
								
							this.onProveedoresChange();				
							
						})
					}); 			
									
		},
		
		onDelete: function() {	

			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				this.message.innerHTML = "Es necesario seleccionar una empresa.";
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
			params.method = "deleteSolDocEmpresas";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.empresas = json.stringify(jsonData);
			
			Request.invokePluginService("ImportSaadPlugin", "SolDocService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";						
								return;
							}	

							this.empresaNombre.reset();
							this.message.innerHTML = "";
							this.progressBar.style.display = "none";								
								
							this.onProveedoresChange();								
							
						})
					}); 		
		
		},
		
		onAdd: function() {

			var proveedor = this.getSelectedProveedor();
			if (proveedor == null) {
				this.message.innerHTML = "Proveedor inválido.";
				return;	
			}			

			if (!this.empresaNombre.isValid()) {
				this.message.innerHTML = "Datos inválidos.";
				return;	
			}
			
			this.message.innerHTML = "Creando...";
			this.progressBar.style.display = "block";
			
			var params = {};
			params.method = "addSolDocEmpresas";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.proveedorid = proveedor.id;
			params.empresa = this.empresaNombre.getValue();
			params.activo = (this.activo.getValue() === "true");
			
			Request.invokePluginService("ImportSaadPlugin", "SolDocService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";						
								return;
							}	

							this.empresaNombre.reset();
							this.message.innerHTML = "";
							this.progressBar.style.display = "none";							
								
							this.onProveedoresChange();				
							
						})
					}); 			
				
		},

		onDepurar: function() {

			this.onClean();

			this.message.innerHTML = "Depurando catálogo...";
			this.progressBar.style.display = "block";
			
			var params = {};
			params.method = "depurarSolDocCatalogos";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.tipo = 1; // empresas
			
			Request.invokePluginService("ImportSaadPlugin", "SolDocService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								this.message.innerHTML = response.error;
								this.progressBar.style.display = "none";						
								return;
							}	

							this.message.innerHTML = "Número de elementos depurados: " + response.count;
							this.progressBar.style.display = "none";
							
						})
					}); 				
		},
		
		onClean: function() {
			this.empresaNombre.reset();
			this.activo.reset();
			this.proveedores.reset();
			this.resetGrid();
			this.message.innerHTML = "";
			this.progressBar.style.display = "none";	
		}	
			
	});
});
