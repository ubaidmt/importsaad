package com.ibm.ecm.extension.util.map;

import java.util.HashMap;

public class UsoCFDIMap extends HashMap<String, String> {
	
	private static final long serialVersionUID = 1L;

	public UsoCFDIMap() {
        this.put("P01", "Por definir (P01)");  
        this.put("G01", "Adquisición de mercancias (G01)");
        this.put("G02", "Devoluciones, descuentos o bonificaciones (F02)");
        this.put("G03", "Gastos en general (G03)");
        this.put("I01", "Construcciones (I01)");
        this.put("I02", "Mobilario y equipo de oficina por inversiones (I02)");
        this.put("I03", "Equipo de transporte (I03)");
        this.put("I04", "Equipo de computo y accesorios (I04)");
        this.put("I05", "Dados, troqueles, moldes, matrices y herramental (I05)");
        this.put("I06", "Comunicaciones telefónicas (I06)");
        this.put("I07", "Comunicaciones satelitales (I07)");
        this.put("I08", "Otra maquinaria y equipo (I08)");
        this.put("D01", "Honorarios médicos, dentales y gastos hospitalarios (D01)");
        this.put("D02", "Gastos médicos por incapacidad o discapacidad (D02)");
        this.put("D03", "Gastos funerales (D03)");
        this.put("D04", "Donativos (D04)");
        this.put("D05", "Intereses reales efectivamente pagados por créditos hipotecarios (D05)");
        this.put("D06", "Aportaciones voluntarias al sar (D06)");
        this.put("D07", "Primas por seguros de gastos médicos (D07)");
        this.put("D08", "Gastos de transportación escolar obligatoria (D08)");
        this.put("D09", "Depósitos en cuentas para el ahorro, primas que tengan como base planes de pensiones (D09)");
        this.put("D10", "Pagos por servicios educativos (D10)");          
    }

}
