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
		"dojo/text!./templates/CntBusquedaCotizacionesDialog.html"
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
	 * @name importSaadPluginDojo.dialog.CntBusquedaCotizacionesDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments dijit.Dialog
	 */
	return declare("importSaadPluginDojo.dialog.CntBusquedaCotizacionesDialog", [ 
	       Dialog, 
	       _TemplatedMixin,
	       _WidgetsInTemplateMixin 
	], {
	/** @lends importSaadPluginDojo.dialog.CntBusquedaCotizacionesDialog.prototype */	

		templateString: template,
		widgetsInTemplate: true,	
		title: null,

		postCreate: function() {
			this.inherited(arguments);			
		},	
		
		setConfig: function(config) {
			this.config = config;
		},	
		
		setContenedor: function(contenedor) {
			this.contenedor = contenedor;
		},
		
		show: function() {		
			this.inherited(arguments);
			
			html.set(this.info, "Cotizaciones del cliente <b>" + this.contenedor.cliente.toString() + "</b> sin asociar.");
			this.initGrid();
			this.loadCotizaciones();
			
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
				html.set(this.message, "Para confirmar es necesario seleccionar una cotización.");
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
  				{name: 'Referencia', field: 'name', width: '20%', editable: false, noresize: true},
				{name: 'Contenedor', field: 'contenedor', width: '20%', editable: false, noresize: true},
				{name: 'Fecha', field: 'datecreated', width: '10%', editable: false, formatter: this.formatDate, noresize: true},
				{name: 'Cliente', field: 'cliente', width: '15%', editable: false, noresize: true},
				{name: 'Mercancia', field: 'mercancia', width: '15%', editable: false, noresize: true},
				{name: 'Total', field: 'monto', width: '10%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true}
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
		
		searchCotizaciones: function(criterio, maxResults, callback) {			
			
			var params = {};
			params.method = "searchCotizaciones";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.criterio = json.stringify(criterio);
			params.maxResults = maxResults;
			
			Request.invokePluginService("ImportSaadPlugin", "CntCotizacionesService",
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
								callback(response.cotizaciones);	
														
						})
					}); 		
		},		
		
		loadCotizaciones: function() {
			
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Consultando cotizaciones...");
			domStyle.set(this.progressBar, "display", "block");
			
			var criterio = {};
			criterio.cliente = this.contenedor.clienteId.toString();
			criterio.tipo = 0; // cotizaciones originales
			criterio.contenedorobj = false; // sin contenedor asociado
			this.searchCotizaciones(criterio, 0, lang.hitch(this, function(cotizaciones) {  			
			
				var store = new ItemFileWriteStore({
					data: {
						identifier: "id",
						items: cotizaciones
					}
				});										
				
				this.grid.setStore(store);
				this.grid.firstPage();
				
				html.set(this.message, "");
				domStyle.set(this.progressBar, "display", "none");					
				
			}))						
		},	
		
	    formatDate: function(strVal) {
	    	// expected: yyyy-MM-dd
	    	// convert to: dd/MM/yyyy
	    	if (strVal == null || strVal.length == 0)
	    		return strVal;
	    	return strVal.substring(8,10) + "/" + strVal.substring(5,7) + "/" + strVal.substring(0,4);  	
	    },
		
		formatCurrency: function(num) {
		    var p = num.toFixed(2).split(".");
		    return p[0].split("").reverse().reduce(function(acc, num, i, orig) {
		        return  num + (i && !(i % 3) ? "," : "") + acc;
		    }, "") + "." + p[1];
		}		
			
	});
});
