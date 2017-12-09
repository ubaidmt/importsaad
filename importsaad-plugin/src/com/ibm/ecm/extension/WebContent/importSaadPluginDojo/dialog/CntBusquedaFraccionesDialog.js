define([
		"dojo/_base/declare",
		"dojo/_base/lang",	
		"dojo/_base/connect",
		"dojo/_base/array",	
		"dijit/registry",
		"dojo/on",
		"dojo/json",
		"dojo/keys",
		"dojo/html",		
		"dojo/dom-style",
		"dojo/dom-geometry",
    	"ecm/model/Request",
    	"ecm/widget/FilteringSelect",
    	"dojo/store/Memory",  	
    	"dojox/grid/EnhancedGrid",    	
    	"dojo/data/ItemFileWriteStore",
    	"dojox/grid/enhanced/plugins/Pagination",
    	"dojox/grid/enhanced/plugins/Filter",    	
    	"dijit/Dialog",
        "dijit/_TemplatedMixin",
    	"dijit/_WidgetsInTemplateMixin",
		"dojo/text!./templates/CntBusquedaFraccionesDialog.html"
	],
	function(declare, 
			lang, 
			connect, 
			array, 
			registry,
			on, 
			json, 
			keys, 
			html, 
			domStyle,
			domGeom,
			Request, 
			FilteringSelect,
			Memory, 
			EnhancedGrid,
			ItemFileWriteStore,
			Pagination,
			Filter,			
			Dialog,
			_TemplatedMixin,
			_WidgetsInTemplateMixin,
			template) {
	/**
	 * @name importSaadPluginDojo.dialog.CntBusquedaFraccionesDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments dijit.Dialog
	 */
	return declare("importSaadPluginDojo.dialog.CntBusquedaFraccionesDialog", [ 
	       Dialog, 
	       _TemplatedMixin,
	       _WidgetsInTemplateMixin 
	], {
	/** @lends importSaadPluginDojo.dialog.CntBusquedaFraccionesDialog.prototype */	

		templateString: template,
		widgetsInTemplate: true,	
		title: null,

		postCreate: function() {
			this.inherited(arguments);			
		},	
		
		setConfig: function(config) {
			this.config = config;
		},		
		
		show: function() {		
			this.inherited(arguments);
			
			this.initGrid();
			this.loadFracciones();
			
			on(this.closeButton, "click", lang.hitch(this, function() {
				this.hide();
			}));
			
			on(this.confirmButton, "click", lang.hitch(this, function() {
				this.onBeforeConfirm();
			}));
			
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
			this.inherited(arguments);
		},
		
		onBeforeConfirm: function() {
			var items = this.grid.selection.getSelected();
			if (items.length <= 0) {
				domStyle.set(this.message, "color", "#6B0A0A");
				html.set(this.message, "Para confirmar es necesario seleccionar una fracción.");
				domStyle.set(this.progressBar, "display", "none");			
				return;				
			}		
			this.onConfirm()
		},
		
		onConfirm: function(callback) {			
			if (lang.isFunction(callback))
				callback();
		},		

		initGrid: function() {
			
			var gridStore = new ItemFileWriteStore({
				data: {
					identifier: "id",
					items: []
				}
			});				
			
			var gridLayout = [
				{name: 'Fracción', field: 'name', width: '15%', editable: false, noresize: true},
				{name: 'Descripción', field: 'descripcion', width: '65%', editable: false, formatter: this.formatDescripcion, noresize: true},
				{name: 'Unidad', field: 'unidad', width: '10%', editable: false, formatter: this.formatUnidadComercial, noresize: true},
				{name: 'Precio', field: 'precio', width: '10%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true}
			];							
			
			this.grid = new EnhancedGrid({
				store: this.gridStore,
				structure: gridLayout,
				selectionMode: 'single',
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
	  		var maxLen = 90;
	  		return (value.length > maxLen ? value.substr(0, maxLen) + "..." : value);
		}			
			
	});
});
