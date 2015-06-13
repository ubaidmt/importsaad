package test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import javax.security.auth.Subject;

import com.filenet.api.collection.DocumentSet;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.Document;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;
import com.ibm.json.java.JSONObject;
import com.ibm.ecm.extension.service.ContentService;
import com.ibm.ecm.extension.service.SolDocService;
import com.ibm.ecm.extension.service.SolDocPagosService;
import com.ibm.json.java.JSONArray;

public class TestHarness {
	
	private static final String server = "http://p8server:9080/wsi/FNCEWS40MTOM/";
	//private static final String server = "http://zeus.excelecm.com:9081/wsi/FNCEWS40MTOM/";
	private static final String user = "p8admin";
	private static final String password = "filenet";
	private static final String stanza = "FileNetP8WSI";
	private static final String osName = "ImportSaadOS";
	//private static final String osName = "GestionDocsOS";

	public static void main(String[] args) {
		TestHarness th = new TestHarness();
		th.updatePropertyDefinition();
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
	
	public void deleteEstructuraSubCarpetas() {
		try
		{
    		Connection con = Factory.Connection.getConnection(server);
    	    Subject subject = UserContext.createSubject(con, user, password, stanza);
    	    UserContext.get().pushSubject(subject); 
    	    ObjectStore os = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), osName, null);	
    	    
    	    Folder cliente = Factory.Folder.fetchInstance(os, "/Facturas/EXCELECM TECHNOLOGY S.A. DE C.V. 2", null);
    	    SolDocService.eliminaEstructuraSubCarpetas(os, cliente);  	    
		    cliente.delete();
		    cliente.save(RefreshMode.REFRESH);
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
	
	public void cloneDocument() {
		Map<Id, Document> pagosClonadosMap = new HashMap<Id, Document>();
		
		try
		{
    		Connection con = Factory.Connection.getConnection(server);
    	    Subject subject = UserContext.createSubject(con, user, password, stanza);
    	    UserContext.get().pushSubject(subject); 
    	    ObjectStore os = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), osName, null);	
    	    
			// Get temp folder
			Folder temp = null;
			try {
				temp = Factory.Folder.fetchInstance(os, "/Temporal/Documentos", null);	
			} catch (EngineRuntimeException ere) {
				throw new RuntimeException ("El folder temporal no fue localizado.");		
			}    	    
    	    
			Map<String, Object> propsMap = new HashMap<String, Object>();
			Document pago = Factory.Document.fetchInstance(os, "{B026054B-0000-C910-9087-C8347CB7F128}", null);
			com.filenet.api.property.Properties props = pago.getProperties();
			propsMap.put("MontoTotal", props.getObjectValue("MontoTotal"));
			propsMap.put("FechaPago", props.getObjectValue("FechaPago"));
			propsMap.put("MetodoPago", props.getObjectValue("MetodoPago"));
			propsMap.put("Referencia", props.getObjectValue("Referencia"));
			propsMap.put("TipoPago", 2); // Pago a Cliente
			propsMap.put("Banco", props.getObjectValue("Banco"));
			propsMap.put("Proveedor", props.getObjectValue("Proveedor"));
			propsMap.put("Empresa", props.getObjectValue("Empresa"));					
			Document pagoClonado = SolDocPagosService.cloneDocument(os, pago.getClassName(), pago, "Pago a Cliente", propsMap, "{A629E2CF-F9C7-43F9-89F3-19D017314E55}");
			pagosClonadosMap.put(pago.get_Id(), pagoClonado);	
			
			ReferentialContainmentRelationship rel = temp.file(pagoClonado, AutoUniqueName.AUTO_UNIQUE, pagoClonado.get_Name(), DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
			rel.save(RefreshMode.NO_REFRESH);				
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

}
