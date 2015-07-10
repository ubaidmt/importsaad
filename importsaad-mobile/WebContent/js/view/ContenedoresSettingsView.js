define([
	"dojo/_base/declare",
	"dojox/mobile/ScrollableView",
	"dijit/registry",
	"dojo/on",
	"dojo/_base/lang",
	"dojo/dom"
], function(declare, ScrollableView, registry, on, lang, dom){
	return declare([ScrollableView], {
		mainView: null,
		name: null,
		cliente: null,
		naviera: null,
		forwarder: null,
		estado: null,
		semaforo: null,
		maxResults: null,
		startup: function () {
			this.inherited(arguments);
			this.mainView = registry.byId("mainView");
			this.name = registry.byId("name");
			this.naviera = registry.byId("naviera");
			this.forwarder = registry.byId("forwarder");
			this.estado = registry.byId("estado");
			this.maxResults = registry.byId("maxResults");
			// handler to record the selected etapa
			this.on("[name=estado]:click", lang.hitch(this, function (e) {
				this.estado = e.target.value;
			}));						
			// handler to record the selected semoforo
			this.on("[name=semaforo]:click", lang.hitch(this, function (e) {
				this.semaforo = e.target.value;
			}));			
			registry.byId("selectClienteButton").on("click", lang.hitch(this, function () {
				this.performTransition("clientesView");
			}));						
			registry.byId("selectNavieraButton").on("click", lang.hitch(this, function () {
				this.performTransition("navierasView");
			}));		
			registry.byId("selectForwarderButton").on("click", lang.hitch(this, function () {
				this.performTransition("forwardersView");
			}));					
			// handler to reset settings
			registry.byId("resetSettingsButton").on("click", lang.hitch(this, function () {
				this.name.reset();
				this.setCliente(null);
				this.setNaviera(null);
				this.setForwarder(null);	
				this.setEstado(null);
				this.setSemaforo(null);
				this.maxResults.reset();
			}));
			// handler to update search query parameters when done button is selected
			registry.byId("doneContenedoresButton").on("click", lang.hitch(this, function () {
				// we are done with the settings: transition to main view
				this.performTransition("mainView");
				// check if anything changed in the setting view
				if (this.getName() !== settings.critero.name ||
					this.getCliente() !== settings.critero.cliente ||
					this.getNaviera() !== settings.critero.naviera ||
					this.getForwarder() !== settings.critero.forwarder ||
					this.getEstado() !== settings.critero.estado ||
					this.getSemaforo() !== settings.critero.semaforo ||
					this.getMaxResults() !== settings.critero.maxResults) {
					// update critero
					settings.critero.name = this.getName();
					settings.critero.cliente = this.getCliente();
					settings.critero.naviera = this.getNaviera();
					settings.critero.forwarder = this.getForwarder();
					settings.critero.estado = this.getEstado();
					settings.critero.semaforo = this.getSemaforo();
					settings.critero.maxResults = this.getMaxResults();
					// force main view list refresh
					this.mainView.loadContenedores();
				}
			}));
		},
		setName: function (name) {
			this.name.set('value', name);
		},
		getName: function () {
			return this.name.get('value') == null || this.name.get('value') == "" ? null : this.name.get('value');
		},
		setCliente: function(cliente) {
			this.cliente = cliente;
			dom.byId("cliente").innerHTML = this.cliente == null ? "" : "&nbsp;&nbsp;" + this.cliente.label;
		},
		getCliente: function() {
			return this.cliente == null ? null : this.cliente.id;
		},		
		setNaviera: function(naviera) {
			this.naviera = naviera;
			dom.byId("naviera").innerHTML = this.naviera == null ? "" : "&nbsp;&nbsp;" + this.naviera.label;
		},
		getNaviera: function () {
			return this.naviera == null ? null : this.naviera.id;
		},		
		setForwarder: function(forwarder) {
			this.forwarder = forwarder;
			dom.byId("forwarder").innerHTML = this.forwarder == null ? "" : "&nbsp;&nbsp;" + this.forwarder.label;
		},
		getForwarder: function () {
			return this.forwarder == null ? null : this.forwarder.id;
		},
		setEstado: function (estado) {
			if (estado == null) {
				registry.byId("estadoAll").set('checked', true);
				this.estado = null;
			} else {
				this.estado = estado;
			}	
		},
		getEstado: function () {
			return this.estado == null || this.estado == "ALL" ? null : this.estado;
		},
		setSemaforo: function (semaforo) {
			if (semaforo == null) {
				registry.byId("semaforoAll").set('checked', true);
				this.semaforo = null;
			} else {
				this.semaforo = semaforo;
			}	
		},
		getSemaforo: function () {
			return this.semaforo == null || this.semaforo == "ALL" ? null : this.semaforo;
		},				
		setMaxResults: function (maxResults) {
			this.maxResults.set('value', maxResults);
		},
		getMaxResults: function () {
			return this.isNumber(this.maxResults.get('value')) ? parseInt(this.maxResults.get('value')) : 50;
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