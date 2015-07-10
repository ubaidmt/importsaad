package com.excelecm.loader;

import java.util.Iterator;
import org.apache.log4j.Logger;

import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ContentEngineSettings;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Folder;

public class Catalogos {

	private static Logger log = Logger.getLogger(Catalogos.class);

	public static void main(String[] args) {
		new Catalogos().init();
	}
	
	@SuppressWarnings("unchecked")
	public void init() {
		
		CEService ceService = null;	
		int count = 0;
		
		try
		{
			log.debug("Estableciendo valores...");
			
			ContentEngineSettings ceSettings = new ContentEngineSettings();
			ceSettings.setUri(LoaderConstantes.ceUri);
			ceSettings.setUser(LoaderConstantes.ceUser);
			ceSettings.setPassword(LoaderConstantes.cePassword);
			ceSettings.setStanza(LoaderConstantes.ceStanza);
			
			ceService = new CEService(ceSettings);
			ceService.establishConnection();
			
			// Get all catalogos
		    SearchScope search = new SearchScope(ceService.getOS(LoaderConstantes.ceOS));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("Id, Activo");
		    sql.setFromClauseInitialValue("SolDocCatalogo", null, true);
		    
		    FolderSet folSet = (FolderSet) search.fetchObjects(sql, 50, null, true);
		    
		    for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) 
		    {
		    	Folder fol = it.next();
		    	com.filenet.api.property.Properties props = fol.getProperties();
		    	props.putObjectValue("Activo", true);
		    	fol.save(RefreshMode.REFRESH);
		    	count++;
		    }
			
			log.debug("Número de elementos actualizados: " + count);
			
		}
		catch (Exception e)
		{
			log.error(e);
		}
		finally
		{
			ceService.releaseConnection();
		}
	}

}
