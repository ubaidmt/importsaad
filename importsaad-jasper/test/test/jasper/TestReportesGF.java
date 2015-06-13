package test.jasper;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ConfigurationSettings;
import com.excelecm.common.settings.ContentEngineSettings;
import com.importsaad.jasper.gf.DetalleFacturas;
import com.importsaad.jasper.gf.DetallePagos;
import com.importsaad.jasper.gf.DetalleDevoluciones;

public class TestReportesGF {

	private CEService ceService = null;

	private static final String objectstore = "ImportSaadOS";
	//private static final String objectstore = "GestionDocsOS";

	@Before
	public void setUp() throws Exception {
		ContentEngineSettings ceSettings = ConfigurationSettings.getInstance().getCESettings();
		ceService = new CEService(ceSettings);
		ceService.establishConnection(); 	
	}
	
	@After
	public void tearDown() throws Exception {
		ceService.releaseConnection();		
	}	
	
	@Test 
	public void testDetalleFacturas() {
		
		List<Properties> resultado = null;
		
		try {
			
			Map<String, Object> condiciones = new HashMap<String, Object>();
			
			DetalleFacturas customReport = new DetalleFacturas();
			resultado = customReport.doExecute(ceService, null, objectstore, null, null, condiciones);
			
			for (int i=0; i<resultado.size(); i++) {
				System.out.println("--- Registro " + (i+1) + " ---");
				java.util.Properties props = (java.util.Properties) resultado.get(i);
				Enumeration <?> keys = props.keys();	
                while (keys.hasMoreElements()) {
                	String key = (String) keys.nextElement();
                	System.out.println(key+"="+props.getProperty(key));
                }
			}			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}	
	
	@Test 
	public void testDetallePagos() {
		
		List<Properties> resultado = null;
		
		try {
			
			Map<String, Object> condiciones = new HashMap<String, Object>();
			
			DetallePagos customReport = new DetallePagos();
			condiciones.put("TipoPago", "0");
			
			resultado = customReport.doExecute(ceService, null, objectstore, null, null, condiciones);
			
			for (int i=0; i<resultado.size(); i++) {
				System.out.println("--- Registro " + (i+1) + " ---");
				java.util.Properties props = (java.util.Properties) resultado.get(i);
				Enumeration <?> keys = props.keys();	
                while (keys.hasMoreElements()) {
                	String key = (String) keys.nextElement();
                	System.out.println(key+"="+props.getProperty(key));
                }
			}			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@Test 
	public void testDetalleDevoluciones() {
		
		List<Properties> resultado = null;
		
		try {
			
			Map<String, Object> condiciones = new HashMap<String, Object>();
			
			DetalleDevoluciones customReport = new DetalleDevoluciones();
			condiciones.put("EstadoSaldo", "0");
			condiciones.put("EstadoDevolucion", "1");
			
			resultado = customReport.doExecute(ceService, null, objectstore, null, null, condiciones);
			
			for (int i=0; i<resultado.size(); i++) {
				System.out.println("--- Registro " + (i+1) + " ---");
				java.util.Properties props = (java.util.Properties) resultado.get(i);
				Enumeration <?> keys = props.keys();	
                while (keys.hasMoreElements()) {
                	String key = (String) keys.nextElement();
                	System.out.println(key+"="+props.getProperty(key));
                }
			}			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}	

}
