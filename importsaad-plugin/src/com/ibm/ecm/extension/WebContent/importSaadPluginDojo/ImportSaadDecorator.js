define(["dojo/_base/lang"], function(lang) {
	lang.setObject("estadoContendorDecorator", function(data, rowId, rowIndex) {	
		if (data == null || data == '')
			return data;
		
		if (data == 0)
			return "En Progreso"
		else if (data == 1)
			return "Entregado"
		else
			return data;
	});	
	
	lang.setObject("slaLlegadaPuertoDecorator", function(data, rowId, rowIndex) {
		if (data == null || data == '')
			return data;
		
		var daysAlert = 20;
		var daysWarning = 18;
		
		if (data >= daysAlert)
			return "<img src='/importsaad-cdn/images/sla/red.png'>&nbsp;&nbsp;" + data;
		else if (data >= daysWarning)
			return "&nbsp;&nbsp;<img src='/importsaad-cdn/images/sla/amber.png'>&nbsp;&nbsp;" + data;
		else
			return "&nbsp;&nbsp;<img src='/importsaad-cdn/images/sla/green.png'>&nbsp;&nbsp;" + data;	
	});	
	
	lang.setObject("slaTerminoTransitoDecorator", function(data, rowId, rowIndex) {	
		if (data == null || data == '')
			return data;
		
		var daysAlert = 8;
		var daysWarning = 5;	
		
		if (data >= daysAlert)
			return "<img src='/importsaad-cdn/images/sla/red.png'>&nbsp;&nbsp;" + data;
		else if (data >= daysWarning)
			return "<img src='/importsaad-cdn/images/sla/amber.png'>&nbsp;&nbsp;" + data;
		else
			return "<img src='/importsaad-cdn/images/sla/green.png'>&nbsp;&nbsp;" + data;	
	});	
	
	lang.setObject("slaEntregaClienteDecorator", function(data, rowId, rowIndex) {
		if (data == null || data == '')
			return data;

		var daysAlert = 8;
		var daysWarning = 5;
				
		if (data >= daysAlert)
			return "<img src='/importsaad-cdn/images/sla/red.png'>&nbsp;&nbsp;" + data;
		else if (data >= daysWarning)
			return "<img src='/importsaad-cdn/images/sla/amber.png'>&nbsp;&nbsp;" + data;
		else
			return "<img src='/importsaad-cdn/images/sla/green.png'>&nbsp;&nbsp;" + data;
	});		
});
