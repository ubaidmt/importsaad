package com.excelecm.loader;

import org.apache.log4j.Logger;
import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ContentEngineSettings;

public class TestConnection {

	private static Logger log = Logger.getLogger(TestConnection.class);
	
	public static void main(String[] args) {

		CEService ceService = null;	
		
		try
		{
			log.debug("Conectando a CE...");
			
			ContentEngineSettings ceSettings = new ContentEngineSettings();
			ceSettings.setUri(LoaderConstantes.ceUri);
			ceSettings.setUser(LoaderConstantes.ceUser);
			ceSettings.setPassword(LoaderConstantes.cePassword);
			ceSettings.setStanza(LoaderConstantes.ceStanza);
			
			ceService = new CEService(ceSettings);
			ceService.establishConnection();
			com.filenet.api.security.Realm realm = ceService.fetchMyRealm();
			
			log.debug("Successfully connected to Realm: " + realm.get_Name());
			
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
