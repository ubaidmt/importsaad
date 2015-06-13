package test.jasper;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ContentEngineSettings;
import com.excelecm.common.settings.ConfigurationSettings;

import com.importsaad.jasper.gc.ComprobantesRegistrados;
import com.importsaad.jasper.gc.ResumenContable;
import com.importsaad.jasper.gc.EstadoContenedor;
import com.importsaad.jasper.gc.DesgloseImpuestos;
import com.importsaad.jasper.gc.DesgloseGastos;

public class TestReportesGC {
	
	private CEService ceService = null;

	private static final String objectstore = "ImportSaadOS";	

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
	public void testPagosRealizados() {
		
		List<Properties> resultado = null;
		
		try {
			
			Map<String, Object> condiciones = new HashMap<String, Object>();
			
			ComprobantesRegistrados customReport = new ComprobantesRegistrados();
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
	public void testResumenContable() {
		
		List<Properties> resultado = null;
		
		try {
			
			Map<String, Object> condiciones = new HashMap<String, Object>();
			//condiciones.put("NumeroContenedor", "12345CBA");
			
			ResumenContable customReport = new ResumenContable();
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
	public void testEstadoContenedor() {
		
		List<Properties> resultado = null;
		
		try {
			
			Map<String, Object> condiciones = new HashMap<String, Object>();
			
			EstadoContenedor customReport = new EstadoContenedor();
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
	public void testDesgloseImpuestos() {
		
		List<Properties> resultado = null;
		
		try {
			
			Map<String, Object> condiciones = new HashMap<String, Object>();
			
			DesgloseImpuestos customReport = new DesgloseImpuestos();
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
	public void testDesgloseGastos() {
		
		List<Properties> resultado = null;
		
		try {
			
			Map<String, Object> condiciones = new HashMap<String, Object>();
			
			DesgloseGastos customReport = new DesgloseGastos();
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
