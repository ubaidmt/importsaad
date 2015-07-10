package com.ibm.ecm.extension.util.sax;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class Factura extends DefaultHandler {
	
	private String nombreEmisor = null;
	private String rfcEmisor = null;
	private String rfcReceptor = null;
	private String folio = null;
	private String fecha = null;
	private double total = 0;
	
	public Factura() {
	}
	
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		
		for (int i = 0; i < atts.getLength(); i++) {
			if (atts.getQName(i).equalsIgnoreCase("nombre") && qName.equalsIgnoreCase("cfdi:Emisor")) {
				nombreEmisor = atts.getValue(i);
			} else if (atts.getQName(i).equalsIgnoreCase("rfc") && qName.equalsIgnoreCase("cfdi:Emisor")) {
				rfcEmisor = atts.getValue(i);
			} else if (atts.getQName(i).equalsIgnoreCase("rfc") && qName.equalsIgnoreCase("cfdi:Receptor")) {
				rfcReceptor = atts.getValue(i);				
			} else if (atts.getQName(i).equalsIgnoreCase("folio") && qName.equalsIgnoreCase("cfdi:Comprobante"))
				folio = atts.getValue(i);
			else if (atts.getQName(i).equalsIgnoreCase("fecha") && qName.equalsIgnoreCase("cfdi:Comprobante"))
				fecha = atts.getValue(i);	
			else if (atts.getQName(i).equalsIgnoreCase("total") && qName.equalsIgnoreCase("cfdi:Comprobante"))
				total = Double.parseDouble(atts.getValue(i));				
		}
		
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
	}
	
	public String getNombreEmisor() {
		return nombreEmisor;
	}
	
	public String getRFCEmisor() {
		return rfcEmisor;
	}	
	
	public String getRFCReceptor() {
		return rfcReceptor;
	}		
	
	public String getFolio() {
		return folio;
	}
	
	public String getFecha() {
		return fecha;
	}	
	
	public double getTotal() {
		return total;
	}	

}
