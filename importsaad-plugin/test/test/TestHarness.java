package test;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import javax.security.auth.Subject;

import com.filenet.api.collection.DocumentSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.Document;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.UserContext;
import com.ibm.json.java.JSONObject;
import com.ibm.ecm.extension.service.ContentService;
import com.ibm.ecm.extension.util.cipher.CipherUtil;
import com.ibm.ecm.extension.util.mail.MailService;
import com.ibm.json.java.JSONArray;

public class TestHarness {
	
	private static final String server = "http://p8server:9080/wsi/FNCEWS40MTOM/";
	//private static final String server = "http://zeus.excelecm.com:9081/wsi/FNCEWS40MTOM/";
	private static final String user = "p8admin";
	private static final String password = "filenet";
	private static final String stanza = "FileNetP8WSI";
	private static final String osName = "ImportSaadOS";
	//private static final String osName = "GestionDocsOS";
	private static final String stringKey = "pxtHnTBIdxE0vOvuQ5fAEQ=="; // AES-compliant string key		

	public static void main(String[] args) {
		TestHarness th = new TestHarness();
		th.testJsonToString();
	}
	
	public void getBinaryData() {
		try
		{
    		Connection con = Factory.Connection.getConnection(server);
    	    Subject subject = UserContext.createSubject(con, user, password, stanza);
    	    UserContext.get().pushSubject(subject); 
    	    ObjectStore os = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), osName, null);	
    	       	    
    	    Document doc = Factory.Document.fetchInstance(os, "{801A594A-0000-C62E-B24B-11FD3EC8B990}", null);
    	    JSONObject jsonDevolucion = com.ibm.ecm.extension.service.SolDocPagosService.getDatosDevolucionCliente(os, doc);
	    	System.out.println(jsonDevolucion.toString());
			
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			UserContext.get().popSubject();
		}	
	}
	
	public void getPropertyDefinition() {
		try
		{
    		Connection con = Factory.Connection.getConnection(server);
    	    Subject subject = UserContext.createSubject(con, user, password, stanza);
    	    UserContext.get().pushSubject(subject); 
    	    ObjectStore os = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), osName, null);	
    	    
    	    List<String> propNames = new ArrayList<String>();
    	    propNames.add("Proveedor");
    	    propNames.add("Empresa");
    	      	    
    	    JSONArray prosArray = ContentService.getPropertyDefinition(os, "SolDocPago", propNames);
    	    System.out.println(prosArray.toString());
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			UserContext.get().popSubject();
		}	
	}
	
	@SuppressWarnings("unchecked")
	public void setSaldoDevoluciones() {
		try
		{
    		Connection con = Factory.Connection.getConnection(server);
    	    Subject subject = UserContext.createSubject(con, user, password, stanza);
    	    UserContext.get().pushSubject(subject); 
    	    ObjectStore os = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), osName, null);	
    	    
		    SearchScope search = new SearchScope(os);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, Saldo");
		    sql.setFromClauseInitialValue("SolDocDevolucion", null, false);
		    sql.setWhereClause("This INSUBFOLDER '/Facturas' AND isCurrentVersion = TRUE AND Saldo IS NULL");
		    		    
		    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, 50, null, true);
		    
		    for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
		    {
		    	Document doc = it.next();
		    	doc.getProperties().putObjectValue("Saldo", Double.parseDouble("0"));
		    	doc.save(RefreshMode.REFRESH);
		    }
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			UserContext.get().popSubject();
		}		
	}	
	
	@SuppressWarnings("rawtypes")
	public void updatePropertyDefinition() {
		
		try
		{
    		Connection con = Factory.Connection.getConnection(server);
    	    Subject subject = UserContext.createSubject(con, user, password, stanza);
    	    UserContext.get().pushSubject(subject); 
    	    ObjectStore os = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), osName, null);	
    	    
    	    // Construct property filter to ensure PropertyDefinitions property of CD is returned as evaluated
    	    com.filenet.api.property.PropertyFilter pf = new com.filenet.api.property.PropertyFilter();
    	    pf.addIncludeType(0, null, Boolean.TRUE, com.filenet.api.constants.FilteredPropertyType.ANY, null); 

    	    // Fetch selected class definition from the server
    	    com.filenet.api.admin.ClassDefinition objClassDef = Factory.ClassDefinition.fetchInstance(os, "SolDocPago", pf);                       

    	    String objPropDefSymbolicName;   
    	                     
    	    // Get PropertyDefinitions property from the property cache                     
    	    com.filenet.api.collection.PropertyDefinitionList objPropDefs = objClassDef.get_PropertyDefinitions();   
    	                                            
    	    Iterator iter = objPropDefs.iterator();
    	    com.filenet.api.admin.PropertyDefinition objPropDef = null;
    	              
    	    // Loop until property definition found
    	    while (iter.hasNext())
    	    {                                               
    	       objPropDef = (com.filenet.api.admin.PropertyDefinition) iter.next();
    	                     
    	       // Get SymbolicName property from the property cache
    	       objPropDefSymbolicName = objPropDef.get_SymbolicName();

    	       if (objPropDefSymbolicName.equalsIgnoreCase("MontoTotal"))
    	       {
    	          // PropertyDefinition object found
    	          System.out.println("Property definition selected: " + objPropDefSymbolicName);
    	          com.filenet.api.property.Property prop = objPropDef.getProperties().get("PropertyMinimumFloat64");
    	          System.out.println("PropertyMinimumFloat64: " + prop.getFloat64Value());
    	          
    	          /*
    	          com.filenet.api.property.Properties props = objPropDef.getProperties();
    	          props.putObjectValue("PropertyMinimumFloat64", null);
    	          objClassDef.save(com.filenet.api.constants.RefreshMode.REFRESH);
    	          */
    	          
    	          break;
    	       }
    	    }    	    
		
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			UserContext.get().popSubject();
		}	    	    
    	    
	}
	
	public void testCipher() {
		
		String mensaje = "filenet";

		try {
			System.out.println("Mensaje: " + mensaje);
			javax.crypto.SecretKey key = CipherUtil.getSecretKey(stringKey);
			System.out.println("stringkey: " + CipherUtil.getStringKey(key));
			String encrypted = CipherUtil.encrypt(mensaje, key);
			System.out.println("encrypted: " + encrypted);
			String decrypted = CipherUtil.decrypt(encrypted, key);
			System.out.println("decrypted: " + decrypted);
		} catch (Exception e) {
			e.printStackTrace();
		}    	
	}	
	
	public void testJsonToString() {
		try
		{
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("01", "Efectivo");
			jsonObject.put("03", "Transferencia de fondos electrónicos");
			
			// convert to readable mode
			String value = "";
			for (Object obj : jsonObject.keySet()) {
				String key = (String) obj;
				value += jsonObject.get(key) + " (" + key + "), ";
			}
			if (value.length() > 0)
				value = value.substring(0, value.length() - 2);
			System.out.println(value);
			
		} catch (Exception e) {
			e.printStackTrace();
		}   
	}
	
	public void sendMail() {

		try
		{
			// yahoo mail
//			String emailHost = "smtp.mail.yahoo.com";
//			long emailPort = 587;
//			String emailFrom = "almaceneseloverol@yahoo.com";
//			String emailAlias = "Almacenes el Overol";
//			String emailUser = "almaceneseloverol@yahoo.com";
//			String emailPassword = "denver1013";
			
//			// hotmail
//			String emailHost = "smtp.live.com";
//			long emailPort = 587;
//			String emailFrom = "solucionas@hotmail.com";
//			String emailAlias = "Administrador de Documentos";
//			String emailUser = "solucionas@hotmail.com";
//			String emailPassword = "oso@02175";			
			
			// gmail
//			String emailHost = "smtp.gmail.com";
//			long emailPort = 587;
//			String emailFrom = "juansaad78@gmail.com";
//			String emailAlias = "Juan Saad";
//			String emailUser = "juansaad78@gmail.com";
//			String emailPassword = "denver1013";
			
			// aws
			String emailHost = "email-smtp.us-east-1.amazonaws.com";
			long emailPort = 587;			
			String emailFrom = "info@excelecm.com";
			String emailAlias = "ExcelECM Technology";	
			String emailUser = "AKIAI2NMQEQ6UTALKDPQ";
			String emailPassword = "Av7Z4PqdF4pAVAg/GCtlZvmFWOspuNK6dRfn9Beksc0r";

			// email to
			String emailTo = "juansaad78@gmail.com";
			
			JSONObject mailSettings = new JSONObject();
			mailSettings.put("starttls", Boolean.TRUE);
			mailSettings.put("emailHost", emailHost);
			mailSettings.put("emailPort", emailPort);
			mailSettings.put("emailFrom", emailFrom);
			mailSettings.put("emailAlias", emailAlias);
			mailSettings.put("emailUser", emailUser);
			mailSettings.put("emailPassword", emailPassword);
			MailService mailService = new MailService(mailSettings);

			System.out.println("Enviado correo de " + emailFrom + " para " + emailTo + " ...");
			String body = "<html><body>Prueba de correo...</body></html>";
			mailService.sendEmail(emailTo, null, null, null, "Prueba", body, new ArrayList<String>(), new ArrayList<String>());
			System.out.println("Mail enviado!!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}	

}
