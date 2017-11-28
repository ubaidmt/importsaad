package com.ibm.ecm.extension.util.map;

import java.util.HashMap;

public class MetodoPagoMap extends HashMap<String, String> {
	
	private static final long serialVersionUID = 1L;

	public MetodoPagoMap() {
        this.put("PUE", "Pago en una sola exhibición (PUE)");
        this.put("PPD", "Pago en parcialidades o diferido (PPD)");
    }

}
