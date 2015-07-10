define([
	"dojo/_base/declare",
	"dojox/mobile/ScrollableView",
	"dijit/registry",
	"dojo/_base/lang",
	"dojo/_base/array",
	"dojo/dom-style",
	"dojo/dom-class",
	"dojo/dom",
	"dojo/dom-construct",
	"dojo/json",
	"dojo/html",
	"dojox/mobile/ListItem",
	"dojo/request/xhr",
	"dojox/mobile/ProgressIndicator"
], function(declare, ScrollableView, registry, lang, array, domStyle, domClass, dom, domConstruct, json, html, ListItem, xhr, ProgressIndicator){
	return declare([ScrollableView], {
		settings: null,
		reloadContenedoresButton: null,
		mainViewHeading: null,
		contenedoresList: null,
		cotizadorView: null,
		cotizadorSettingsView: null,
		detailsContainer:null,
		progressIndicator: null,
		// Create a template string for a contenedor ListItem
		contenedoresListItemTemplateString:
			'<div class="contenedroSummary">' +
				'<div class="contenedorTitle">${contenedor}</div>' +
				'<div class="cliente troncatedText">${cliente}</div>' +				
			'</div><div class="summaryClear"></div>',			
		// init variables and handlers
		startup: function() {
			this.inherited(arguments);			
			// retain widgets references
			this.reloadContenedoresButton = registry.byId("reloadContenedoresButton");
			this.mainViewHeading = registry.byId("mainViewHeading");
			this.detailsContainer = registry.byId("detailsContainer");
			this.contenedoresList = registry.byId("contenedoresList");
			this.cotizadorView = registry.byId("cotizadorView");
			this.cotizadorSettingsView = registry.byId("cotizadorSettingsView");
			this.progressIndicator = ProgressIndicator.getInstance();
			// add click handler to the button that call refresh
			this.reloadContenedoresButton.on("click", lang.hitch(this, this.loadContenedores) );
			this.loadMobileSettings(lang.hitch(this, function(mblsettings) {
				// add main settings
				settings.osname = mblsettings.osname;
				settings.pteiva = mblsettings.pteiva;	
				this.loadCntSettings(lang.hitch(this, function(cntsettings) {
					this.settings = cntsettings;
					// add cotizador settings
					settings.cotizador = {};
					settings.cotizador.tc = cntsettings.tipoCambio;
					settings.cotizador.igi = cntsettings.IGI;
					settings.cotizador.dta = cntsettings.DTA;
					settings.cotizador.prev = cntsettings.PREV;
					settings.cotizador.cnt = cntsettings.CNT;				
					// save original cotizador settings for further restore
					this.cotizadorSettingsView.saveOriginalSettings(settings.cotizador);
					// reset cotizador based on settings
					this.cotizadorView.resetCotizacion();
					// load contenedores
					this.loadContenedores();
				}))					
			}))					
		},
		loadContenedores: function() {
			// remove all list items
			this.contenedoresList.destroyDescendants();
			// reset scroll to make sur progress indicator is visible
			this.scrollTo({x:0,y:0});
			// add progress indicator
			this.mainViewHeading.set('label',"cargando...");
			this.contenedoresList.domNode.appendChild(this.progressIndicator.domNode);
			this.progressIndicator.start();
			// request contenedores list
			var requestURL = "Dispatcher?method=searchContenedores&criterio=" + json.stringify(settings.critero) + "&maxResults=" + settings.critero.maxResults + "&os=" + settings.osname;
			xhr(requestURL, {
				method: "GET",
				handleAs: "json",
				preventCache: true,
				sync : false, 
				timeout: 10000, // 10 seconds
				headers: { "Content-Type": "application/json"}
			}).then(lang.hitch(this, function(response) {	
				if (response.error != null) {
					this.progressIndicator.stop();
					this.mainViewHeading.set('label',response.error);
					return;
				}
				// remove progress indicator
				this.progressIndicator.stop();
				this.contenedoresList.destroyDescendants();
				// restore the title
				this.mainViewHeading.set('label','Contenedores');
				// populate the list
				array.forEach(response.contenedores, lang.hitch(this, function (contenedor) {
					// Create a new ListItem at the end of the list
					var listItem = new ListItem({}).placeAt(this.contenedoresList, "last");
					// set custom style
					domClass.add(listItem.domNode, "contenedorListItem");
					// create and insert content from template and JSON response
					listItem.containerNode.innerHTML = this.substitute(this.contenedoresListItemTemplateString, {
						contenedor: contenedor.name,
						fechabase: this.formatDate(contenedor.fechabase),
						cliente: contenedor.cliente
					});
					domStyle.set(listItem.domNode, "background-color", this.getSemaforoBackgroundColor(contenedor.semaforo)); // semaforo
					listItem.onClick = lang.hitch(this, function(){
						// update details view before transitioning to it
						this.detailsContainer.domNode.innerHTML = this.getContenedorDetalle(contenedor);
						listItem.set("transition","slide");
						listItem.transitionTo("details");
					});
					listItem.set("moveTo","#");
				}));				
			})), lang.hitch(this, function(err) {
				this.progressIndicator.stop();
				this.mainViewHeading.set('label',err);				
			});				
		},	
		loadDocumentos: function(critero, maxResults, callback) {
			// set loading title
			var requestURL = "Dispatcher?method=searchDocumentos&criterio=" + json.stringify(critero) + "&maxResults=" + maxResults + "&os=" + settings.osname;
			xhr(requestURL, {
				method: "GET",
				handleAs: "json",
				preventCache: true,
				sync : false, 
				timeout: 10000, // 10 seconds
				headers: { "Content-Type": "application/json"}
			}).then(lang.hitch(this, function(response) {
				if (response.error != null) {
					this.mainViewHeading.set('label',response.error);
					return;
				}
				if (lang.isFunction(callback))
					callback(response.documentos);					
			})), lang.hitch(this, function(err) {
				this.mainViewHeading.set('label',err);				
			});										
		},		
		loadMobileSettings: function(callback) {
			// set loading title
			var requestURL = "Dispatcher?method=getMobileSettings";
			xhr(requestURL, {
				method: "GET",
				handleAs: "json",
				preventCache: true,
				sync : false, 
				timeout: 10000, // 10 seconds
				headers: { "Content-Type": "application/json"}
			}).then(lang.hitch(this, function(response) {
				if (response.error != null) {
					this.mainViewHeading.set('label',response.error);
					return;
				}
				if (lang.isFunction(callback))
					callback(response.settings);					
			})), lang.hitch(this, function(err) {
				this.mainViewHeading.set('label',err);				
			});										
		},
		loadCntSettings: function(callback) {
			// set loading title
			var requestURL = "Dispatcher?method=getCntSettings&os=" + settings.osname;
			xhr(requestURL, {
				method: "GET",
				handleAs: "json",
				preventCache: true,
				sync : false, 
				timeout: 10000, // 10 seconds
				headers: { "Content-Type": "application/json"}
			}).then(lang.hitch(this, function(response) {	
				if (response.error != null) {
					this.mainViewHeading.set('label',response.error);
					return;
				}
				if (lang.isFunction(callback))
					callback(response.settings);		
			})), lang.hitch(this, function(err) {
				this.mainViewHeading.set('label',err);				
			});										
		},	
		getContenedorDetalle: function(contenedor) {
			var datos = json.parse(contenedor.datos.toString());
			var content = '';			
			// datos generales
			content += '<table style="width:100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width:100%;font-size:0.7em;font-weight:bold;color:#14469C;">Datos generales</td>';
			content += '</tr>';
			content += '</table>';			
			content += '<table style="width:100%;border-collapse: collapse;">';			
			content += '<tr>';
			content += '<td style="width:30%;"><div style="font-size:0.7em;font-weight:normal;color:black;">Contenedor</div></td>';
			content += '<td align="left" style="width:70%;"><div style="font-size:0.7em;font-weight:bold;color:black;">' + contenedor.name + '</div></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width:30%;"><div style="font-size:0.7em;font-weight:normal;color:black;">Fecha base</div></td>';
			content += '<td align="left" style="width:70%;"><div style="font-size:0.7em;font-weight:bold;color:black;">' + this.formatDate(contenedor.fechabase) + '</div></td>';
			content += '</tr>';			
			content += '<tr>';
			content += '<td style="width:30%;"><div style="font-size:0.7em;font-weight:normal;color:black;">Fecha creaci&oacute;n</div></td>';
			content += '<td align="left" style="width:70%;"><div style="font-size:0.7em;font-weight:bold;color:black;">' + this.formatDate(contenedor.datecreated) + '</div></td>';
			content += '</tr>';			
			content += '<tr>';
			content += '<td style="width:30%;"><div style="font-size:0.7em;font-weight:normal;color:black;">Pedimento</div></td>';
			content += '<td align="left" style="width:70%;"><div style="font-size:0.7em;font-weight:bold;color:black;">' + (contenedor.pedimiento != null ? contenedor.pedimento : "") + '</div></td>';
			content += '</tr>';
			content += '</tr>';
			content += '<td style="width:30%;"><div style="font-size:0.7em;font-weight:normal;color:black;">Mercancia</div></td>';
			content += '<td align="left" style="width:70%;"><div style="font-size:0.7em;font-weight:bold;color:black;">' + (contenedor.mercancia != null ? contenedor.mercancia : "") + '</div></td>';
			content += '</tr>';			
			content += '<tr>';
			content += '<td style="width:30%;"><div style="font-size:0.7em;font-weight:normal;color:black;">Cliente</div></td>';
			content += '<td align="left" style="width:70%;"><div style="font-size:0.7em;font-weight:bold;color:black;">' + contenedor.cliente + '</div></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width:30%;"><div style="font-size:0.7em;font-weight:normal;color:black;">Naviera</div></td>';
			content += '<td align="left" style="width:70%;"><div style="font-size:0.7em;font-weight:bold;color:black;">' + (contenedor.naviera != null ? contenedor.naviera : "") + '</div></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width:30%;"><div style="font-size:0.7em;font-weight:normal;color:black;">Forwarder</div></td>';
			content += '<td align="left" style="width:70%;"><div style="font-size:0.7em;font-weight:bold;color:black;">' + (contenedor.forwarder != null ? contenedor.forwarder : "") + '</div></td>';
			content += '</tr>';								
			content += '</table>';
			// observaciones
			if ("observaciones" in datos && datos.observaciones != "") {
				content += '<br>';
				content += '<table style="width:100%;border-collapse: collapse;">';
				content += '<tr>';
				content += '<td style="width:100%;font-size:0.7em;font-weight:bold;color:#14469C;">Observaciones</td>';
				content += '</tr>';
				content += '</table>';			
				content += '<table style="width:100%;border-collapse: collapse;">';			
				content += '<tr>';
				content += '<td align="left" style="width:100%;"><div style="font-size:0.7em;font-weight:normal;color:black;">' + datos.observaciones.replace(/\n/g,'<br>') + '</div></td>';
				content += '</tr>';	
				content += '</table>';				
			}
			// estado actual			
			content += '<br>';
			content += '<table style="width:100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width:100%;font-size:0.7em;font-weight:bold;color:#14469C;">Estado actual</td>';
			content += '</tr>';
			content += '</table>';			
			content += '<table style="width: 100%;border-collapse: collapse;">';			
			content += '<tr>';
			content += '<td style="width:30%;"><div style="font-size:0.7em;font-weight:normal;color:black;">Etapa</div></td>';
			content += '<td align="left" style="width:70%;"><div style="font-size:0.7em;font-weight:bold;color:black;">' + this.formatEstado(contenedor.estado) + '</div></td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td style="width:30%;"><div style="font-size:0.7em;font-weight:normal;color:black;">Sem&aacute;foro</div></td>';
			content += '<td align="left" style="width:70%;"><img src="' + this.formatSemaforo(contenedor.semaforo) + '" width="15px" height="15px"/></td>';
			content += '</tr>';			
			content += '</table>';
			// checklist
			content += '<br>';
			content += '<table style="width: 100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width:50%;font-size:0.7em;font-weight:bold;color:#14469C;">Actividad</td>';
			content += '<td style="width:20%;font-size:0.7em;font-weight:bold;color:#14469C;">ETA</td>';
			content += '<td style="width:20%;font-size:0.7em;font-weight:bold;color:#14469C;">Completado</td>';
			content += '<td style="width:10%;font-size:0.7em;font-weight:bold;color:#14469C;"><div></div></td>';			
			content += '</tr>';
			var actividades = ["Llegada puerto", "Proforma imp", "Solicitud imp", "Pago imp", "Revalidaci&oacute;n", "Salida puerto", "Prog ferrocarril", "Prog entrega", "Llegada alm loc", "Salida alm loc", "Entrega cliente", "Demoras"];
			for (var i = 0; i < actividades.length; i++) {
				content += '<tr>';
				content += '<td style="width: 50%;border-top: 1px solid #14469C;"><div style="font-size:0.7em;font-weight:normal;color:black;">' + actividades[i] + '</div></td>';
				content += '<td align="left" style="width: 20%;border-top: 1px solid #14469C;"><div style="font-size:0.7em;font-weight:normal;color:black;">' + this.formatDate(datos.etapas[i.toString()].eta) + '</div></td>';
				content += '<td align="left" style="width: 20%;border-top: 1px solid #14469C;"><div style="font-size:0.7em;font-weight:normal;color:black;">' + this.formatDate(datos.etapas[i.toString()].done) + '</div></td>';
				content += '<td align="left" style="width: 10%;border-top: 1px solid #14469C;"><img src="' + this.formatSemaforo(this.getSemaforoEtapa(i, datos.etapas[i.toString()].eta, datos.etapas[i.toString()].done)) + '" width="15px" height="15px"/></td>';			
				content += '</tr>';					
			}
			content += '<tr>';
			content += '<td colspan="4" style="width: 100%;border-top: 1px solid #14469C;"><div></div></td>';			
			content += '</tr>';				
			content += '</table>';	
			// documentos	
			domConstruct.destroy("tblDocumentos"); // dom destroy
			domConstruct.destroy("consultandoDocsRow"); // dom destroy
			domConstruct.destroy("consultandoDocsLabel"); // dom destroy
			content += '<br>';
			content += '<table id="tblDocumentos" style="width: 100%;border-collapse: collapse;">';
			content += '<tr>';
			content += '<td style="width:50%;font-size:0.7em;font-weight:bold;color:#14469C;">Nombre</td>';
			content += '<td style="width:50%;font-size:0.7em;font-weight:bold;color:#14469C;">Tipo</td>';
			content += '</tr>';
			content += '<tr>';
			content += '<td id="consultandoDocsRow" colspan="4" align="center" style="width: 100%;"><label id="consultandoDocsLabel" style="font-family:arial;font-size:12px;font-weight:normal;font-style:italic;color:black;">consultando documentos asociados...</label></td>';		
			content += '</tr>';				
			content += '</table>';
			var criterio = {
				"contenedor": contenedor.id
			};
			this.loadDocumentos(criterio, 0, lang.hitch(this, function(documentos) {
				if (documentos.length > 0) {
					domConstruct.destroy("consultandoDocsRow"); // dom destroy
					domConstruct.destroy("consultandoDocsLabel"); // dom destroy
				} else {
					html.set(dom.byId("consultandoDocsLabel"), "no existen documentos asociados");
				}
				array.forEach(documentos, lang.hitch(this, function(documento) {
					var row = '';
					row += '<tr>';
					row += '<td style="width: 50%;"><a href="' + documento.link.toString() + '" target="_blank" title="Ver Documento" style="font-size:0.7em;font-weight:normal;color:#14469C;">' + documento.name.toString() + '</a></td>';
					row += '<td align="left" style="width: 35%;"><div style="font-size:0.7em;font-weight:normal;color:black;">' + this.formatTipoDocumento(parseInt(documento.tipo.toString())) + '</div></td>';
					row += '</tr>';
					domConstruct.toDom(row);
					domConstruct.place(row, "tblDocumentos");
				}));				
			}))					
			
			return content;
		},
		// Pushes data into a template - primitive
		substitute: function(template,obj) {
			return template.replace(/\$\{([^\s\:\}]+)(?:\:([^\s\:\}]+))?\}/g, function(match,key){
				return obj[key];
			});
		},
	    formatDate: function(strVal) {
	    	// expected: yyyy-MM-dd
	    	// convert to: dd/MM/yyyy
	    	if (strVal == null || strVal.length == 0)
	    		return "";
	    	return strVal.substring(8,10) + "/" + strVal.substring(5,7) + "/" + strVal.substring(0,4);  	
	    },
	    getSemaforoBackgroundColor: function(num) {
			if (num == null)
				return "";
			switch(num) {
				case 0:
					return "#D1E7DB";
					break;
				case 1:
					return "#F2EDCE";
					break;				
				case 2:
					return "#EBCDCD";
					break;
			}		    	
	    },
		formatSemaforo: function(num) {
			if (num == null)
				return "";
			switch(num) {
				case 0:
					return "images/alert-green.png";
					break;
				case 1:
					return "images/alert-amber.png";
					break;				
				case 2:
					return "images/alert-red.png";
					break;
			}			
		},
		formatEstado: function(num) {
			if (num == null)
				return "";
			switch(num) {
				case 0:
					return "EN TRASLADO MARITIMO";
					break;
				case 1:
					return "EN PUERTO";
					break;				
				case 2:
					return "EN TRASLADO TERRESTRE";
					break;
				case 3:
					return "EN ALMACEN LOCAL";
					break;			
				case 4:
					return "EN ENTREGA A CLIENTE";
					break;
				case 5:
					return "EN RETORNO DE VACIO";
					break;
				case 99:
					return "CONCLUIDO";
					break;						
			}			
		}, 
	    formatTipoDocumento: function(num) {
			if (num == null)
				return "";
			switch(num) {
				case 0:
					return "Bill of Lading";
					break;
				case 1:
					return "Factura Comercial";
					break;
				case 2:
					return "Lista de Empaque";
					break;					
				case 3:
					return "Pedimento";
					break;
				case 4:
					return "Carta Técnica";
					break;			
				case 5:
					return "Contrato a Proveedor";
					break;			
				case 6:
					return "Notificación de Arribo";
					break;			
				case 7:
					return "Documentación a Ferrocarril";
					break;			
				case 8:
					return "EIR";
					break;
				case 99:
					return "Otro";
					break;							
			}			
		},			
		getFormatedDateValue: function(strDate) {
			if (strDate.length < 10)
				return strDate;
			else
				return strDate.substring(0,10);			
		},	
		getStringDateValue: function(dVal, tipo) {
			var formatedDate;
			switch (tipo) {
				case 0: // convert to: yyyy-MM-dd
					formatedDate = dVal.getFullYear() + "-" + this.lpad((dVal.getMonth() + 1).toString(), 2, '0') + "-" + this.lpad(dVal.getDate().toString(), 2, '0');
					break;
				case 1: // convert to: dd/MM/yyyy
					formatedDate = this.lpad(dVal.getDate().toString(), 2, '0') + "/" + this.lpad((dVal.getMonth() + 1).toString(), 2, '0') + "/" + dVal.getFullYear();
					break;				
			}
			return formatedDate;	
		},		
		daysDiff: function(date1, date2) {
			var timeDiff = date2.getTime() - date1.getTime();
			var diffDays = Math.ceil(timeDiff / (1000 * 3600 * 24)); 
			return diffDays;			
		},	
		lpad: function(val, len, padding) {
	       	while (val.length < len)
	       		val = padding + val;
	       	return val;
	   	},			
		getSemaforoEtapa: function(index, eta, completado) {
			var semaforo = 0; // verde por defecto
			var key = index.toString();
			// si exite fecha de ETA definida
			if (eta != null) {
				// si no existe fecha de completado definida
				if (completado == null) {
					var dateCurrent = new Date(this.getStringDateValue(new Date(), 0));
					var dateETA = new Date(this.getFormatedDateValue(eta));
					var dateETAPrevio = lang.clone(dateETA);
					var diasPrevio = parseInt(this.settings.eta.previo[key]);
					dateETAPrevio.setDate(dateETAPrevio.getDate() - diasPrevio);
					if (this.daysDiff(dateCurrent, dateETA) < 0) // si la fecha de ETA es menor a la actual se marca como alerta roja
						semaforo = 2; // rojo
					else if (this.daysDiff(dateCurrent, dateETAPrevio) < 0) // si la fecha de ETA es menor a la actual mas dias de alerta previa se marca como alerta ambar
						semaforo = 1; // ambar
				}
			}
			return semaforo;
		}		
	});
});