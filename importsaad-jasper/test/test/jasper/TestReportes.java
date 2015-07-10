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
import com.importsaad.jasper.gf.Facturas;
import com.importsaad.jasper.gf.Pagos;
import com.importsaad.jasper.gf.Devoluciones;
import com.importsaad.jasper.gc.Fracciones;
import com.importsaad.jasper.gc.Cotizacion;
import com.importsaad.jasper.gc.FraccionesCotizadas;

public class TestReportes {

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
			
			Facturas customReport = new Facturas();
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
			
			Pagos customReport = new Pagos();
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
			
			Devoluciones customReport = new Devoluciones();
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
	
	@Test 
	public void testFracciones() {
		
		List<Properties> resultado = null;
		
		try {
			
			Map<String, Object> condiciones = new HashMap<String, Object>();
			
			Fracciones customReport = new Fracciones();
			
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
	public void testCotizacion() {
		
		List<Properties> resultado = null;
		
		try {
			
			Map<String, Object> condiciones = new HashMap<String, Object>();
			condiciones.put("Id", "{1034D158-0000-C02D-9186-9BC0617F9F56}");
			
			Cotizacion customReport = new Cotizacion();
			
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
	public void testFraccionesCotizas() {
		
		List<Properties> resultado = null;
		
		try {
			
			Map<String, Object> condiciones = new HashMap<String, Object>();
			condiciones.put("Id", "{C0F21A59-0000-C335-972B-4B1063D3EA3A}");
			//condiciones.put("Fracciones", "5106.20.01");
			
			FraccionesCotizadas customReport = new FraccionesCotizadas();
			
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
