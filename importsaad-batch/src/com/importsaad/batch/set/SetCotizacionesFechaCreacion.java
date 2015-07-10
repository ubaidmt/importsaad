package com.importsaad.batch.set;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

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

public class SetCotizacionesFechaCreacion {
	
	private static Logger log = Logger.getLogger(SetCotizacionesFechaCreacion.class);
	private ObjectStore os = null;

	public static void main(String[] args) {
		new SetCotizacionesFechaCreacion().doExec();
	}
	
	@SuppressWarnings("unchecked")
	public void doExec() {
		
		int processed = 0;
		int batch = 500;
		
		try {		
		
			java.util.Properties configProps = PropertyConfig.getInstance().getPropertiesResource();
			
			// Open P8 connection
			Connection con = Factory.Connection.getConnection(configProps.getProperty("ce.uri"));
		    Subject subject = UserContext.createSubject(con, configProps.getProperty("ce.user"), configProps.getProperty("ce.password"), configProps.getProperty("ce.stanza"));
		    UserContext.get().pushSubject(subject); 
		    os = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), configProps.getProperty("ce.os"), null);
		    
			log.debug("Estableciendo fecha de creacion en cotizaciones existentes...");
			
		    SearchScope search = new SearchScope(os);
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("This, DateCreated, FechaCreacion");
		    sql.setFromClauseInitialValue("CntCotizacion", null, true);
		    sql.setWhereClause("isCurrentVersion = TRUE AND FechaCreacion IS NULL");
		    sql.setMaxRecords(batch);
			
		    DocumentSet docSet = (DocumentSet) search.fetchObjects(sql, 50, null, true);
		    for (Iterator<Document> it = docSet.iterator(); it.hasNext(); ) 
		    {
		    	Document doc = it.next();

		    	com.filenet.api.property.Properties props = doc.getProperties();
		    	props.putObjectValue("FechaCreacion", getUTFCalendar(props.getDateTimeValue("DateCreated")).getTime());
		    	doc.save(RefreshMode.REFRESH);
		    	processed++;
		    }
					    
			log.debug("Total de cotizaciones afectadas: " + processed);
		    
		} catch (Exception e) {
			log.error(e);
		} finally {
			UserContext.get().popSubject();
		}
	}
	
	private static Calendar getUTFCalendar(Date dateObj) throws Exception {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(dateObj);	
		return cal;
	}	
	
}
