define([
		"dojo/_base/declare",
		"dojo/_base/lang",	
		"dojo/_base/array",	
		"dojo/on",
		"dojo/json",
		"dijit/Dialog",
	    "dijit/_TemplatedMixin",
		"dijit/_WidgetsInTemplateMixin",
		"dojo/text!./templates/SolDocFormaPagoDialog.html"
	],
	function(declare, lang, array, on, json, Dialog, _TemplatedMixin, _WidgetsInTemplateMixin, template) {
	/**
	 * @name importSaadPluginDojo.dialog.SolDocFormaPagoDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments dijit.Dialog
	 */
	return declare("importSaadPluginDojo.dialog.SolDocFormaPagoDialog", [ 
	       Dialog, 
	       _TemplatedMixin,
	       _WidgetsInTemplateMixin 
	], {
	/** @lends importSaadPluginDojo.dialog.SolDocFormaPagoDialog.prototype */	

		templateString: template,
		widgetsInTemplate: true,	
		title: null,

		postCreate: function() {
			this.inherited(arguments);
			this.initEvents();
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
		},
		
		hide: function() {
			this.inherited(arguments);
		},
		
		initEvents: function() {
			on(this.closeButton, "click", lang.hitch(this, function() {
				this.hide();
			}));
			
			on(this.cleanButton, "click", lang.hitch(this, function() {
				this.onClean();
			}));			
			
			on(this.confirmButton, "click", lang.hitch(this, function() {
				this.onBeforeConfirm();
			}));		
			
			on(this.efectivo, "change", lang.hitch(this, function() {
				if (this.efectivo.getValue() === "true") this.resetValues("efectivo");
			}));
			on(this.chequeNominativo, "change", lang.hitch(this, function() {
				if (this.chequeNominativo.getValue() === "true") this.resetValues("chequeNominativo");
			}));	
			on(this.transferenciaElectronica, "change", lang.hitch(this, function() {
				if (this.transferenciaElectronica.getValue() === "true") this.resetValues("transferenciaElectronica");
			}));
			on(this.tarjetaCredito, "change", lang.hitch(this, function() {
				if (this.tarjetaCredito.getValue() === "true") this.resetValues("tarjetaCredito");
			}));
			on(this.monederoElectronico, "change", lang.hitch(this, function() {
				if (this.monederoElectronico.getValue() === "true") this.resetValues("monederoElectronico");
			}));
			on(this.dineroElectronico, "change", lang.hitch(this, function() {
				if (this.dineroElectronico.getValue() === "true") this.resetValues("dineroElectronico");
			}));
			on(this.valesDespensa, "change", lang.hitch(this, function() {
				if (this.valesDespensa.getValue() === "true") this.resetValues("valesDespensa");
			}));
			on(this.tarjetaDebito, "change", lang.hitch(this, function() {
				if (this.tarjetaDebito.getValue() === "true") this.resetValues("tarjetaDebito");
			}));
			on(this.tarjetaServicio, "change", lang.hitch(this, function() {
				if (this.tarjetaServicio.getValue() === "true") this.resetValues("tarjetaServicio");
			}));
			on(this.otros, "change", lang.hitch(this, function() {
				if (this.otros.getValue() === "true") this.resetValues("otros");
			}));
		},		
		
		onClean: function() {
			this.resetValues(null);
		},
		
		resetValues: function(source) {
			if (source == null || source != "efectivo") this.efectivo.reset();
			if (source == null || source != "chequeNominativo") this.chequeNominativo.reset();
			if (source == null || source != "transferenciaElectronica") this.transferenciaElectronica.reset();
			if (source == null || source != "tarjetaCredito") this.tarjetaCredito.reset();
			if (source == null || source != "monederoElectronico") this.monederoElectronico.reset();
			if (source == null || source != "dineroElectronico") this.dineroElectronico.reset();
			if (source == null || source != "valesDespensa") this.valesDespensa.reset();
			if (source == null || source != "tarjetaDebito") this.tarjetaDebito.reset();
			if (source == null || source != "tarjetaServicio") this.tarjetaServicio.reset();
			if (source == null || source != "otros") this.otros.reset();
			html.set(this.message, "");
			domStyle.set(this.progressBar, "display", "none");				
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
