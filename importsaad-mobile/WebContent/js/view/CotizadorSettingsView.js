define([
	"dojo/_base/declare",
	"dojox/mobile/ScrollableView",
	"dijit/registry",
	"dojo/on",
	"dojo/_base/lang"
], function(declare, ScrollableView, registry, on, lang){
	return declare([ScrollableView], {
		cotizadorView: null,
		stnPteIVA: 0,
		stnTC: 0,
		stnIGI: 0,
		stnDTA: 0,
		stnPREV: 0,
		stnCNT: 0,
		originalSettings: null,
		startup: function () {
			this.inherited(arguments);
			this.cotizadorView = registry.byId("cotizadorView");
			this.stnPteIVA = registry.byId("stnPteIVA");
			this.stnTC = registry.byId("stnTC");
			this.stnIGI = registry.byId("stnIGI");
			this.stnDTA = registry.byId("stnDTA");
			this.stnPREV = registry.byId("stnPREV");
			this.stnCNT = registry.byId("stnCNT");
			// handler to update search query parameters when done button is selected
			registry.byId("doneCotizadorSettingsButton").on("click", lang.hitch(this, function () {
				this.performTransition("cotizadorView");
				// update cotizador settings
				if (this.isNumber(this.stnPteIVA.get('value')))				
					settings.pteiva = parseFloat(this.stnPteIVA.get('value'));
				if (this.isNumber(this.stnTC.get('value')))				
					settings.cotizador.tc = parseFloat(this.stnTC.get('value'));
				if (this.isNumber(this.stnIGI.get('value')))				
					settings.cotizador.igi = parseFloat(this.stnIGI.get('value'));
				if (this.isNumber(this.stnDTA.get('value')))				
					settings.cotizador.dta = parseFloat(this.stnDTA.get('value'));				
				if (this.isNumber(this.stnPREV.get('value')))				
					settings.cotizador.prev = parseFloat(this.stnPREV.get('value'));
				if (this.isNumber(this.stnCNT.get('value')))				
					settings.cotizador.cnt = parseFloat(this.stnCNT.get('value'));
				// update cotizacion based on new settings
				this.cotizadorView.setTipoCambio(settings.cotizador.tc);
				this.cotizadorView.updateCotizacion();
			}));
			registry.byId("restoreCotizadorSettingsButton").on("click", lang.hitch(this, function () {
				// restore original settings
				settings.cotizador = lang.clone(this.originalSettings);
				this.loadValues();
			}));			
			// handler to get notified before a transition to the current view starts
			this.on("beforeTransitionIn", lang.hitch(this, function () {
				this.loadValues();
			}));
		},
		loadValues: function() {
			this.stnPteIVA.set('value', settings.pteiva);
			this.stnTC.set('value', settings.cotizador.tc);
			this.stnIGI.set('value', settings.cotizador.igi);
			this.stnDTA.set('value', settings.cotizador.dta);
			this.stnPREV.set('value', settings.cotizador.prev);
			this.stnCNT.set('value', settings.cotizador.cnt);
		},
		saveOriginalSettings: function(settings) {
			this.originalSettings = lang.clone(settings);
		},
		isNumber: function(value) {
		    if ((undefined === value) || (null === value) || ('' === value))
		        return false;
		    if (typeof value == 'number')
		        return true;
		    return !isNaN(value - 0);
		}
	});
});