define([
	"dojo/_base/declare",
	"dojox/mobile/ScrollableView",
    "dojo/dom",
	"dijit/registry",
	"dojo/on",
	"dojo/_base/lang",
	"dojo/_base/array",
], function(declare, ScrollableView, dom, registry, on, lang, array){
	return declare([ScrollableView], {
		fraccion: null,
		cleanDatosCotizacionButton: null,
		settingsCotizacionButton: null,
		fraccionesView: null,
		// Create a template string for a value ListItem
		cotizacionViewItemTemplateString:
			'<div data-mobile-layout="left" class="c1ScrollPane mblListItemLayoutLeft">${title}</div>' +
			'<div data-mobile-layout="right" class="c3ScrollPane c3aScrollPane mblListItemLayoutRight">${value}</div>' +
			'<div style="display: inline;" class="mblListItemLabel"></div>',
		cotizacionViewTemplateListItems: [
            {"title": "MIN", "value": 0},
		    {"title": "UNI", "value": 0},
		    {"title": "USD", "value": 0},
		    {"title": "ADU", "value": 0},
		    {"title": "IGI", "value": 0},
		    {"title": "DTA", "value": 0},
		    {"title": "IVA", "value": 0},
		    {"title": "PREV", "value": 0},
		    {"title": "CNT", "value": 0},
		    {"title": "IMP", "value": 0},
		    {"title": "COT", "value": 0}
		],
		startup: function () {
			this.inherited(arguments);
			this.fraccionesView = registry.byId("fraccionesView");
			this.cleanDatosCotizacionButton = registry.byId("cleanDatosCotizacionButton");
			this.settingsCotizacionButton = registry.byId("settingsCotizacionButton");
			this.cleanDatosCotizacionButton.on("click", lang.hitch(this, this.resetCotizacion));
			registry.byId("selectFraccionButton").on("click", lang.hitch(this, function () {
				this.performTransition("fraccionesView");
			}));						
			this.settingsCotizacionButton.on("click", lang.hitch(this, function () {
				this.performTransition("cotizadorSettingsView");
			}));			
			// handlers para actualizar cotizqcion
			registry.byId("aumento").on("change,keyup", lang.hitch(this, function () {
				this.updateCotizacion();
			}));			
			registry.byId("ancho").on("change,keyup", lang.hitch(this, function () {
				this.updateCotizacion();
			}));
			registry.byId("metros").on("statechanged", lang.hitch(this, function () {
				this.updateCotizacion();
			}));			
			registry.byId("cantidad").on("change,keyup", lang.hitch(this, function () {
				this.updateCotizacion();
			}));
			registry.byId("flete").on("change,keyup", lang.hitch(this, function () {
				this.updateCotizacion();
			}));			
			registry.byId("tipoCambio").on("change,keyup", lang.hitch(this, function () {
				this.updateCotizacion();
			}));	
			registry.byId("incremento").on("change,keyup", lang.hitch(this, function () {
				this.updateCotizacion();
			}));			
			// handler
			registry.byId("doneCotizadorButton").on("click", lang.hitch(this, function () {
				// we are done with cotizacion: transition to FeedView
				this.performTransition("mainView");
			}));
			this.on("beforeTransitionOut", lang.hitch(this, function () {
			}));
		},
		setFraccion: function(fraccion) {
			this.fraccion = fraccion;
			dom.byId("fraccion").innerHTML = "&nbsp;&nbsp;" + this.fraccion.label;
		},
		resetCotizacion: function() {
			this.fraccion = null;
			dom.byId("fraccion").innerHTML = "";
			registry.byId("metros").reset();
			registry.byId("aumento").reset();
			registry.byId("ancho").reset();
			registry.byId("cantidad").reset();
			registry.byId("flete").reset();
			registry.byId("tipoCambio").set('value', settings.cotizador.tc);
			this.updateCotizacion();
		},
		setTipoCambio: function(value) {
			registry.byId("tipoCambio").set('value', value);			
		},
		updateCotizacion: function() {
			var cotizacionItems = this.getCotizacionItems();
			var index = 0;
			array.forEach(registry.byId("cotizacionList").getChildren(), lang.hitch(this, function(listItem) {
				listItem.containerNode.innerHTML = this.substitute(this.cotizacionViewItemTemplateString, {
					title: cotizacionItems[index].title,
					value: "$ " + this.formatCurrency(cotizacionItems[index].value)
				});
				index++;
	    	})); 			
		},
		getCotizacionItems: function() {
			var preciominimo = 0;
			var aumento = 0;
			var ancho = 1;
			var anchoMetros = 1;
			var cantidad = 1;
			var flete = 0;
			var incremento = 0;
			var tipocambio = settings.cotizador.tc;
			
			if (this.fraccion != null) 
				preciominimo = parseFloat(this.fraccion.precio);
			if (this.isNumber(registry.byId("aumento").get('value')))
				aumento = parseFloat(registry.byId("aumento").get('value'));
			if (this.isNumber(registry.byId("ancho").get('value'))) {
				ancho = parseFloat(registry.byId("ancho").get('value'));
				anchoMetros = ancho;
				// si la medida se indica en pulgadas, se realiza la conversion a metros
				if (registry.byId("metros").get("value") == "off")
					anchoMetros = ancho * 0.0254;				
			}
			if (this.isNumber(registry.byId("cantidad").get('value')))
				cantidad = parseFloat(registry.byId("cantidad").get('value'));
			if (this.isNumber(registry.byId("flete").get('value')))
				flete = parseFloat(registry.byId("flete").get('value'));
			if (this.isNumber(registry.byId("tipoCambio").get('value')))
				tipocambio = parseFloat(registry.byId("tipoCambio").get('value'));
			if (this.isNumber(registry.byId("incremento").get('value')))
				incremento = parseFloat(registry.byId("incremento").get('value'));			
			var preciounitario = (preciominimo + aumento) * anchoMetros;
			preciounitario = Math.round(preciounitario *100) / 100; // round to two decimals
			var totalusd = preciounitario * cantidad;
			var valoraduanal = (totalusd + flete) * tipocambio;
			var igi = valoraduanal * settings.cotizador.igi;
			var dta = valoraduanal * settings.cotizador.dta;
			var iva = (valoraduanal * settings.pteiva) + (igi * settings.pteiva) + (dta * settings.pteiva);
			var prev = settings.cotizador.prev;
			var cnt = settings.cotizador.cnt;
			var totalimpuestos = igi + dta + iva + prev + cnt;		
			var totalcotizacion = totalimpuestos + incremento;

			var index = 0;
			array.forEach(this.cotizacionViewTemplateListItems, lang.hitch(this, function(item) {
				switch (index) {
					case 0: // precio minimo
						item.value = preciominimo;
						break;
					case 1: // precio unitario
						item.value = preciounitario;
						break;					
					case 2: // total usd
						item.value = totalusd;
						break;
					case 3: // valor aduanal
						item.value = valoraduanal;
						break;
					case 4: // igi
						item.value = igi;
						break;
					case 5: // dta
						item.value = dta;
						break;
					case 6: // iva
						item.value = iva;
						break;
					case 7: // prev
						item.value = prev;
						break;
					case 8: // cnt
						item.value = cnt;
						break;
					case 9: // total impuestos
						item.value = totalimpuestos;
						break;
					case 10: // total cotizacion
						item.value = totalcotizacion;
						break;								
					default:
						item.value = 0;
						break;	
				}
				index++;
	    	}));
			return this.cotizacionViewTemplateListItems;
		},		
		// Pushes data into a template - primitive
		substitute: function(template,obj) {
			return template.replace(/\$\{([^\s\:\}]+)(?:\:([^\s\:\}]+))?\}/g, function(match,key){
				return obj[key];
			});
		},
		formatCurrency: function(num) {
		    var p = num.toFixed(2).split(".");
		    return p[0].split("").reverse().reduce(function(acc, num, i, orig) {
		        return  num + (i && !(i % 3) ? "," : "") + acc;
		    }, "") + "." + p[1];
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