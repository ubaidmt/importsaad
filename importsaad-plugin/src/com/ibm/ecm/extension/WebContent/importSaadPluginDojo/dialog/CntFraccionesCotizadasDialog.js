define([
		"dojo/_base/declare",
		"dojo/_base/lang",	
		"dojo/_base/array",	
		"dojo/on",
		"dojo/json",
		"dojo/html",		
		"dojo/dom-style",
		"dojo/dom-geometry", 	
    	"dojox/grid/EnhancedGrid",    	
    	"dojo/data/ItemFileWriteStore",
    	"dojox/grid/enhanced/plugins/Pagination",
    	"dojox/grid/enhanced/plugins/Filter",    	
    	"dijit/Dialog",
        "dijit/_TemplatedMixin",
    	"dijit/_WidgetsInTemplateMixin",
		"dojo/text!./templates/CntFraccionesCotizadasDialog.html"
	],
	function(declare, 
			lang, 
			array, 
			on, 
			json, 
			html, 
			domStyle,
			domGeom, 
			EnhancedGrid,
			ItemFileWriteStore,
			Pagination,
			Filter,			
			Dialog,
			_TemplatedMixin,
			_WidgetsInTemplateMixin,
			template) {
	/**
	 * @name importSaadPluginDojo.dialog.CntFraccionesCotizadasDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments dijit.Dialog
	 */
	return declare("importSaadPluginDojo.dialog.CntFraccionesCotizadasDialog", [ 
	       Dialog, 
	       _TemplatedMixin,
	       _WidgetsInTemplateMixin 
	], {
	/** @lends importSaadPluginDojo.dialog.CntFraccionesCotizadasDialog.prototype */	

		templateString: template,
		widgetsInTemplate: true,	
		title: null,

		postCreate: function() {
			this.inherited(arguments);			
		},	
		
		setConfig: function(config) {
			this.config = config;
		},	
		
		setCotizacion: function(cotizacion) {
			this.cotizacion = cotizacion;
		},
		
		show: function() {		
			this.inherited(arguments);
			
			html.set(this.info, "Fracciones incluidas en la cotización <b>" + this.cotizacion.name + "</b>");
			this.initGrid();
			this.loadCotizacion();
			
			on(this.cancelButton, "click", lang.hitch(this, function() {
				this.hide();
			}));
			
			on(this.confirmButton, "click", lang.hitch(this, function() {
				this.onBeforeConfirm();
			}));			
			
			on(this.selectAllFraccionesButton, "click", lang.hitch(this, function() {
				if (this.grid != null) {
					this.grid.store.fetch({ 
						onItem: lang.hitch(this, function(item) {
							this.grid.selection.setSelected(item, true);
						})
					});
					this.updateTotales();
				}
			}));
			
			on(this.unSelectAllFraccionesButton, "click", lang.hitch(this, function() {
				if (this.grid != null) {
					this.grid.selection.clear();
					this.updateTotales();
				}
			}));			
			
			on(this.filtro, "keyup", lang.hitch(this, function(evt) {
		  		if (this.grid == null)
					return;
		  		if (this.filtro.getValue() != "")
		  			this.grid.filter({"fraccion": "*" + this.filtro.getValue() + "*"});
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
				html.set(this.message, "Para confirmar es necesario seleccionar al menos una fracción.");
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
					identifier: "fraccion",
					items: []
				}
			});				
			
			var gridLayout = [
				{name: 'Fracción', field: 'fraccion', width: '15%', editable: false, noresize: true},
				{name: 'Precio Mínimo (USD)', field: 'precioMinimo', width: '15%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true},
				{name: 'Aumento (USD)', field: 'aumento', width: '15%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true},
				{name: 'Ancho', field: 'ancho', width: '15%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true},
				{name: 'Precio Unitario (USD)', field: 'precioUnitario', width: '15%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true},
				{name: 'Cantidad', field: 'cantidad', width: '15%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true},
				{name: 'Total (USD)', field: 'total', width: '15%', editable: false, datatype: 'number', formatter: this.formatCurrency, noresize: true}
			];						
			
			this.grid = new EnhancedGrid({
				store: this.gridStore,
				structure: gridLayout,
				selectionMode: 'multi',
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
			
			on(this.grid, "click", lang.hitch(this, function() {
				this.onGridItemSelect();			
			}));						
			
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
		
		onGridItemSelect: function() {
			this.updateTotales();		
		},		
		
		loadCotizacion: function() {
			var datos = json.parse(this.cotizacion.datos);			
			var store = new ItemFileWriteStore({
				data: {
					identifier: "fraccion",
					items: datos.fracciones
				}
			});										
			this.grid.setStore(store);
			this.grid.firstPage();
		},
		
		updateTotales: function() {
			var cantidad = 0;
			var total = 0;
			if (this.grid != null) {
				var items = this.grid.selection.getSelected();
				array.forEach(items, lang.hitch(this, function(item) {
					cantidad += parseFloat(item.cantidad.toString());
					total += parseFloat(item.total.toString());
				}));			
			}
			html.set(this.cantidad, this.formatCurrency(cantidad));
			html.set(this.total, this.formatCurrency(total));
		},
		
		formatCurrency: function(num) {
		    var p = num.toFixed(2).split(".");
		    return p[0].split("").reverse().reduce(function(acc, num, i, orig) {
		        return  num + (i && !(i % 3) ? "," : "") + acc;
		    }, "") + "." + p[1];
		}		
			
	});
});
