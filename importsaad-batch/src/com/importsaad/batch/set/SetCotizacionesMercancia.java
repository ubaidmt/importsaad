package com.importsaad.batch.set;

import java.util.Iterator;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import com.importsaad.batch.util.PropertyConfig;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.UserContext;
import com.ibm.json.java.JSONObject;

public class SetCotizacionesMercancia {
	
	private static Logger log = Logger.getLogger(SetCotizacionesMercancia.class);
	private ObjectStore os = null;

	public static void main(String[] args) {
		new SetCotizacionesMercancia().doExec();
	}
	
	@SuppressWarnings("unchecked")
	public void doExec() {
		
		int processed = 0;		
		
		try {		
		
			java.util.Properties configProps = PropertyConfig.getInstance().getPropertiesResource();
			
			// Open P8 connection
			Connection con = Factory.Connection.getConnection(configProps.getProperty("ce.uri"));
		    Subject subject = UserContext.createSubject(con, configProps.getProperty("ce.user"), configProps.getProperty("ce.password"), configProps.getProperty("ce.stanza"));
		    UserContext.get().pushSubject(subject); 
		    os = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), configProps.getProperty("ce.os"), null);
		    
			log.debug("Estableciendo mercancia en cotizaciones existentes...");
			
		    SearchScope search = new SearchScope(os);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("This, ClbJSONData, Mercancia");
		    sql.setFromClauseInitialValue("CntCotizacion", null, false);
		    sql.setWhereClause("isCurrentVersion = TRUE");
			
		    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, 50, null, true);
		    for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
		    {
		    	Document doc = it.next();

		    	// Get Data
		    	com.filenet.api.property.Properties props = doc.getProperties();
		    	byte[] data = props.getBinaryValue("ClbJSONData");
		    	JSONObject jsonData = JSONObject.parse(new String(data));
		    	
		    	if (jsonData.containsKey("mercancia")) {
		    		props.putObjectValue("Mercancia", jsonData.get("mercancia").toString());
		    		doc.save(RefreshMode.REFRESH);
					processed++;
		    	}
		    }
					    
			log.debug("Total de cotizaciones afectadas: " + processed);
		    
		} catch (Exception e) {
			log.error(e);
		} finally {
			UserContext.get().popSubject();
		}
	}
	
}
