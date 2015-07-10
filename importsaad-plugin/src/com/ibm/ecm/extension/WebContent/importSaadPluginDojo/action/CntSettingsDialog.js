define([
		"dojo/_base/declare",
		"dojo/_base/lang",	
		"dojo/json",
		"dojo/html",
		"dojo/dom-style",
    	"ecm/model/Request",	   	
		"dijit/_TemplatedMixin",
		"dijit/_WidgetsInTemplateMixin",
		"dijit/Dialog",		
		"dojo/text!./templates/CntSettingsDialog.html"
	],
	function(declare, 
			lang, 
			json, 
			html, 
			domStyle, 
			Request, 
			_TemplatedMixin, 
			_WidgetsInTemplateMixin, 
			Dialog, 
			template) {

	/**
	 * @name importSaadPluginDojo.action.CntSettingsDialog
	 * @class Provides a dialog whose main content is an html page displayed in an iframe.  
	 * @augments dijit.Dialog
	 */
	return declare("importSaadPluginDojo.action.CntSettingsDialog", [ Dialog, _TemplatedMixin, _WidgetsInTemplateMixin ], {
	/** @lends importSaadPluginDojo.action.CntSettingsDialog.prototype */	

		templateString: template,
		widgetsInTemplate: true,		
		
		showDialog: function() {
			this.onLoad();
			this.inherited("show", []);		
		},
		
		onClose: function() {
			this.inherited("hide", []);		
		},
		
		setConfig: function(config) {
			this.config = config;
		},		
		
		onLoad: function() {
			
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Cargando...");
			domStyle.set(this.progressBar, "display", "block");		
			
			var params = {};
			params.method = "getCntSettings";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			
			Request.invokePluginService("ImportSaadPlugin", "SettingsService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								html.set(this.message, response.error);
								domStyle.set(this.progressBar, "display", "none");																					
								return;
							}
							
							// cotizador
							this.tipoCambio.setValue(response.settings.tipoCambio);
							this.IGI.setValue(response.settings.IGI);
							this.DTA.setValue(response.settings.DTA);
							this.PREV.setValue(response.settings.PREV);
							this.CNT.setValue(response.settings.CNT);
							this.ajusteCotizacion.setValue(response.settings.ajusteCotizacion);
							// base ETA
							this.baseLlegadaPuerto.setValue(response.settings.eta.base["0"]);
							this.baseProformaImpuestos.setValue(response.settings.eta.base["1"]);
							this.baseSolicitudImpuestosCliente.setValue(response.settings.eta.base["2"]);
							this.basePagoImpuestos.setValue(response.settings.eta.base["3"]);
							this.baseRevalidacion.setValue(response.settings.eta.base["4"]);
							this.baseSalidaPuerto.setValue(response.settings.eta.base["5"]);
							this.baseProgFerrocarril.setValue(response.settings.eta.base["6"]);
							this.baseProgEntregaBodega.setValue(response.settings.eta.base["7"]);
							this.baseLlegadaAlmacenLocal.setValue(response.settings.eta.base["8"]);
							this.baseSalidaAlmacenLocal.setValue(response.settings.eta.base["9"]);
							this.baseEntregaCliente.setValue(response.settings.eta.base["10"]);
							this.baseDemoras.setValue(response.settings.eta.base["11"]);								
							// dias adicionales base ETA
							this.diasLlegadaPuerto.setValue(response.settings.eta.dias["0"]);
							this.diasProformaImpuestos.setValue(response.settings.eta.dias["1"]);
							this.diasSolicitudImpuestosCliente.setValue(response.settings.eta.dias["2"]);							
							this.diasPagoImpuestos.setValue(response.settings.eta.dias["3"]);
							this.diasRevalidacion.setValue(response.settings.eta.dias["4"]);
							this.diasSalidaPuerto.setValue(response.settings.eta.dias["5"]);
							this.diasProgFerrocarril.setValue(response.settings.eta.dias["6"]);
							this.diasProgEntregaBodega.setValue(response.settings.eta.dias["7"]);
							this.diasLlegadaAlmacenLocal.setValue(response.settings.eta.dias["8"]);
							this.diasSalidaAlmacenLocal.setValue(response.settings.eta.dias["9"]);
							this.diasEntregaCliente.setValue(response.settings.eta.dias["10"]);
							this.diasDemoras.setValue(response.settings.eta.dias["11"]);
							// dias previos alerta
							this.previoLlegadaPuerto.setValue(response.settings.eta.previo["0"]);
							this.previoProformaImpuestos.setValue(response.settings.eta.previo["1"]);
							this.previoSolicitudImpuestosCliente.setValue(response.settings.eta.previo["2"]);														
							this.previoPagoImpuestos.setValue(response.settings.eta.previo["3"]);
							this.previoRevalidacion.setValue(response.settings.eta.previo["4"]);
							this.previoSalidaPuerto.setValue(response.settings.eta.previo["5"]);
							this.previoProgFerrocarril.setValue(response.settings.eta.previo["6"]);
							this.previoProgEntregaBodega.setValue(response.settings.eta.previo["7"]);							
							this.previoLlegadaAlmacenLocal.setValue(response.settings.eta.previo["8"]);
							this.previoSalidaAlmacenLocal.setValue(response.settings.eta.previo["9"]);
							this.previoEntregaCliente.setValue(response.settings.eta.previo["10"]);
							this.previoDemoras.setValue(response.settings.eta.previo["11"]);									
							
							html.set(this.message, "");
							domStyle.set(this.progressBar, "display", "none");
							
						})
					}); 				
		},	
		
		onSave: function() {			
			
			if (!this.tipoCambio.isValid() || !this.IGI.isValid() || !this.DTA.isValid() || !this.PREV.isValid() || !this.CNT.isValid()) {			
				this.message.innerHTML = "Datos inválidos en parámetros de cotización.";
				return;	
			}
			
			if (!this.diasLlegadaPuerto.isValid() || !this.diasRevalidacion.isValid() || !this.diasPagoImpuestos.isValid() || !this.diasSalidaPuerto.isValid() || !this.diasProgFerrocarril.isValid() || !this.diasLlegadaAlmacenLocal.isValid() || !this.diasSalidaAlmacenLocal.isValid() || !this.diasProgEntregaBodega.isValid() || !this.diasEntregaCliente.isValid() || !this.diasDemoras.isValid()) {			
				this.message.innerHTML = "Datos inválidos en parámetros de dias adicionales a ETA.";
				return;	
			}
			
			if (!this.previoLlegadaPuerto.isValid() || !this.previoRevalidacion.isValid() || !this.previoPagoImpuestos.isValid() || !this.previoSalidaPuerto.isValid() || !this.previoProgFerrocarril.isValid() || !this.previoLlegadaAlmacenLocal.isValid() || !this.previoSalidaAlmacenLocal.isValid() || !this.previoProgEntregaBodega.isValid() || !this.previoEntregaCliente.isValid() || !this.previoDemoras.isValid()) {			
				this.message.innerHTML = "Datos inválidos en parámetros de dias previo para alerta.";
				return;	
			}						
			
			domStyle.set(this.message, "color", "#000253");
			html.set(this.message, "Actualizando...");
			domStyle.set(this.progressBar, "display", "block");	
			
			var settings = {};
			// cotizador
			settings.tipoCambio = parseFloat(this.tipoCambio.getValue());
			settings.IGI = parseFloat(this.IGI.getValue());
			settings.DTA = parseFloat(this.DTA.getValue());
			settings.PREV = parseFloat(this.PREV.getValue());
			settings.CNT = parseFloat(this.CNT.getValue());
			settings.ajusteCotizacion = parseFloat(this.ajusteCotizacion.getValue());
			// ETA
			settings.eta = {};			
			// base ETA
			settings.eta.base = {};
			settings.eta.base["0"] = parseInt(this.baseLlegadaPuerto.getValue());
			settings.eta.base["1"] = parseInt(this.baseProformaImpuestos.getValue());
			settings.eta.base["2"] = parseInt(this.baseSolicitudImpuestosCliente.getValue());
			settings.eta.base["3"] = parseInt(this.basePagoImpuestos.getValue());
			settings.eta.base["4"] = parseInt(this.baseRevalidacion.getValue());
			settings.eta.base["5"] = parseInt(this.baseSalidaPuerto.getValue());
			settings.eta.base["6"] = parseInt(this.baseProgFerrocarril.getValue());
			settings.eta.base["7"] = parseInt(this.baseProgEntregaBodega.getValue());
			settings.eta.base["8"] = parseInt(this.baseLlegadaAlmacenLocal.getValue());
			settings.eta.base["9"] = parseInt(this.baseSalidaAlmacenLocal.getValue());
			settings.eta.base["10"] = parseInt(this.baseEntregaCliente.getValue());
			settings.eta.base["11"] = parseInt(this.baseDemoras.getValue());
			// dias adicionales base ETA
			settings.eta.dias = {};
			settings.eta.dias["0"] = parseInt(this.diasLlegadaPuerto.getValue());
			settings.eta.dias["1"] = parseInt(this.diasProformaImpuestos.getValue());
			settings.eta.dias["2"] = parseInt(this.diasSolicitudImpuestosCliente.getValue());			
			settings.eta.dias["3"] = parseInt(this.diasPagoImpuestos.getValue());
			settings.eta.dias["4"] = parseInt(this.diasRevalidacion.getValue());
			settings.eta.dias["5"] = parseInt(this.diasSalidaPuerto.getValue());
			settings.eta.dias["6"] = parseInt(this.diasProgFerrocarril.getValue());
			settings.eta.dias["7"] = parseInt(this.diasProgEntregaBodega.getValue());
			settings.eta.dias["8"] = parseInt(this.diasLlegadaAlmacenLocal.getValue());
			settings.eta.dias["9"] = parseInt(this.diasSalidaAlmacenLocal.getValue());
			settings.eta.dias["10"] = parseInt(this.diasEntregaCliente.getValue());
			settings.eta.dias["11"] = parseInt(this.diasDemoras.getValue());
			// dias previos de alerta
			settings.eta.previo = {};
			settings.eta.previo["0"] = parseInt(this.previoLlegadaPuerto.getValue());
			settings.eta.previo["1"] = parseInt(this.previoProformaImpuestos.getValue());
			settings.eta.previo["2"] = parseInt(this.previoSolicitudImpuestosCliente.getValue());					
			settings.eta.previo["3"] = parseInt(this.previoPagoImpuestos.getValue());
			settings.eta.previo["4"] = parseInt(this.previoRevalidacion.getValue());
			settings.eta.previo["5"] = parseInt(this.previoSalidaPuerto.getValue());
			settings.eta.previo["6"] = parseInt(this.previoProgFerrocarril.getValue());
			settings.eta.previo["7"] = parseInt(this.previoProgEntregaBodega.getValue());
			settings.eta.previo["8"] = parseInt(this.previoLlegadaAlmacenLocal.getValue());
			settings.eta.previo["9"] = parseInt(this.previoSalidaAlmacenLocal.getValue());
			settings.eta.previo["10"] = parseInt(this.previoEntregaCliente.getValue());
			settings.eta.previo["11"] = parseInt(this.previoDemoras.getValue());			
			
			var params = {};
			params.method = "updateCntSettings";
			params.repositoryid = ecm.model.desktop.defaultRepositoryId;
			params.context = json.stringify(this.config.context);
			params.settings = json.stringify(settings);
			
			Request.invokePluginService("ImportSaadPlugin", "SettingsService",
					{	
						requestParams: params,
						requestCompleteCallback: lang.hitch(this, function(response) {
							
							if (response.error != null) {
								domStyle.set(this.message, "color", "#6B0A0A");
								html.set(this.message, response.error);
								domStyle.set(this.progressBar, "display", "none");																					
								return;
							}
								
							html.set(this.message, "");
							domStyle.set(this.progressBar, "display", "none");
							
						})
					}); 							
		}
		
	});
});
