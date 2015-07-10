define([
		"dojo/_base/declare",
		"dojo/_base/lang",	
		"dojo/_base/array",	
		"dojo/on",
		"dojo/json",
    	"dijit/Dialog",
        "dijit/_TemplatedMixin",
    	"dijit/_WidgetsInTemplateMixin",
		"dojo/text!./templates/SolDocMetodosPagoDialog.html"
	],
	function(declare, lang, array, on, json, Dialog, _TemplatedMixin, _WidgetsInTemplateMixin, template) {
	/**
	 * @name importSaadPluginDojo.dialog.SolDocMetodosPagoDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments dijit.Dialog
	 */
	return declare("importSaadPluginDojo.dialog.SolDocMetodosPagoDialog", [ 
	       Dialog, 
	       _TemplatedMixin,
	       _WidgetsInTemplateMixin 
	], {
	/** @lends importSaadPluginDojo.dialog.SolDocMetodosPagoDialog.prototype */	

		templateString: template,
		widgetsInTemplate: true,	
		title: null,

		postCreate: function() {
			this.inherited(arguments);			
		},	
		
		setData: function(data) {
			this.data = data;
		},
		
		loadData: function() {
	  		for (key in this.data) {
	  			if (key == "01") this.efectivo.set("checked", true);
	  			if (key == "02") this.chequeNominativo.set("checked", true);
	  			if (key == "03") this.transferenciaElectronica.set("checked", true);
	  			if (key == "04") this.tarjetaCredito.set("checked", true);
	  			if (key == "05") this.monederoElectronico.set("checked", true);
	  			if (key == "06") this.dineroElectronico.set("checked", true);
	  			if (key == "08") this.valesDespensa.set("checked", true);
	  			if (key == "28") this.tarjetaDebito.set("checked", true);
	  			if (key == "29") this.tarjetaServicio.set("checked", true);
	  			if (key == "99") this.otros.set("checked", true);
	  		}	
		},
		
		getData: function() {
			var data = {};
			if (this.efectivo.getValue() === "true") data["01"] = "Efectivo";
			if (this.chequeNominativo.getValue() === "true") data["02"] = "Cheque nominativo";
			if (this.transferenciaElectronica.getValue() === "true") data["03"] = "Transferencia electrónica de fondos";
			if (this.tarjetaCredito.getValue() === "true") data["04"] = "Tarjeta de credito";
			if (this.monederoElectronico.getValue() === "true") data["05"] = "Monedero electrónico";
			if (this.dineroElectronico.getValue() === "true") data["06"] = "Dinero electrónico";
			if (this.valesDespensa.getValue() === "true") data["08"] = "Vales de despensa";
			if (this.tarjetaDebito.getValue() === "true") data["28"] = "Tarjeta de débito";
			if (this.tarjetaServicio.getValue() === "true") data["29"] = "Tarjeta de servicio";
			if (this.otros.getValue() === "true") data["99"] = "Otros";
			return data;
		},			
		
		show: function() {		
			this.inherited(arguments);
			
			this.loadData();
			
			on(this.closeButton, "click", lang.hitch(this, function() {
				this.hide();
			}));
			
			on(this.cleanButton, "click", lang.hitch(this, function() {
				this.onClean();
			}));			
			
			on(this.confirmButton, "click", lang.hitch(this, function() {
				this.onBeforeConfirm();
			}));			
		},
		
		hide: function() {
			this.inherited(arguments);
		},
		
		onClean: function() {
			this.efectivo.reset();
			this.chequeNominativo.reset();
			this.transferenciaElectronica.reset();
			this.tarjetaCredito.reset();
			this.monederoElectronico.reset();
			this.dineroElectronico.reset();
			this.valesDespensa.reset();
			this.tarjetaDebito.reset();
			this.tarjetaServicio.reset();
			this.otros.reset();
		},
		
		onBeforeConfirm: function() {
			// validaciones
			this.onConfirm()
		},
		
		onConfirm: function(callback) {			
			if (lang.isFunction(callback))
				callback();
		}		
			
	});
});
