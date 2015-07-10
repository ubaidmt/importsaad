package com.importsaad.job.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.List;
import java.util.ArrayList;

import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ConfigurationSettings;
import com.excelecm.common.settings.ContentEngineSettings;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.Id;
import java.util.concurrent.TimeUnit;
import com.ibm.json.java.JSONObject;

public class UpdateSemaforosJobImpl {

	private String osName;
	private int addDays = 0;
	private long procesados = 0;
	private final static DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");
	private final static DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private final static String CntSettingsId = "{1542E1F2-5854-40ED-A3B9-75DB8D24EFF9}";
	
	private final static int SEMAFORO_VERDE = 0;
	private final static int SEMAFORO_AMBAR = 1;
	private final static int SEMAFORO_ROJO = 2;
	
	private JSONObject jsonSettings;
	
	@SuppressWarnings("unchecked")
	public void doExec() throws Exception {
		
		CEService ceService = null;

    	try 
    	{
    		// CE Settings
    		ContentEngineSettings ceSettings = ConfigurationSettings.getInstance().getCESettings();
    		
			// CE Connection
			ceService = new CEService(ceSettings);
			ceService.establishConnection();
			
			// Load Settings
			jsonSettings = getCntSettings(ceService.getOS(getOsName()));
			
			// Set Calendar
	    	Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	    	cal.setTime(new Date());
	    	cal.add(Calendar.DATE, addDays);
			
			// Get Contenedores En Progreso
	    	SearchScope search = new SearchScope(ceService.getOS(getOsName()));
		    SearchSQL sql = new SearchSQL();
		    sql.setSelectList("This, Id, Parent, DateCreated, FolderName, FechaBase, Pedimento, Naviera, Forwarder, EstadoContenedor, Semaforo, ClbJSONData");
		    sql.setFromClauseInitialValue("CntContenedor", null, false);
		    
		    // Build where statement
		    StringBuffer whereStatement = new StringBuffer();
		    whereStatement.append("FolderName IS NOT NULL");
		    whereStatement.append(" AND DateCreated >= " + convertLocalTimeToUTC(cal.getTime())); // contenedores con antiguedad de X dias
		    whereStatement.append(" AND EstadoContenedor <> 99"); // contenedores en progreso
		    
		    FolderSet folSet = (FolderSet) search.fetchObjects(sql, null, null, true);
		    List<Integer> semaforosEtapas = new ArrayList<Integer>();
		    for (Iterator<Folder> it = folSet.iterator(); it.hasNext(); ) 
		    {
		    	Folder contenedor = it.next();
		    	// Get Data
		    	com.filenet.api.property.Properties props = contenedor.getProperties();
		    	byte[] data = props.getBinaryValue("ClbJSONData");
		    	JSONObject jsonData = JSONObject.parse(new String(data));
		    	// Identifica semaforo de cada etapa
		    	if (jsonData.containsKey("etapas")) {
		    		JSONObject jsonEtapas = (JSONObject) jsonData.get("etapas");
		    		for (Object obj : jsonEtapas.keySet()) {
		    			String key = (String) obj;
		    			JSONObject jsonEstado = (JSONObject) jsonEtapas.get(key);
		    			String eta = jsonEstado.get("eta") != null ? jsonEstado.get("eta").toString() : null;
		    			String done = jsonEstado.get("done") != null ? jsonEstado.get("done").toString() : null;
		    			semaforosEtapas.add(getSemaforoEtapa(key, eta, done));
		    		}
		    	}
		    	// Identifica semaforo del contenedor
		    	int semaforoContenedor = getSemaforoContenedor(semaforosEtapas);
		    	props.putObjectValue("Semaforo", semaforoContenedor);
		    	contenedor.save(RefreshMode.REFRESH);
		    	procesados++;
		    }
    		
    	}
    	catch (Exception e)
    	{
    		throw e;
    	}
    	finally
    	{
    		if (ceService != null)
    			ceService.releaseConnection();	
    	}
		
	}	
	
	public String getOsName() {
		return osName;
	}

	public void setOsName(String osName) {
		this.osName = osName;
	}	
	
	public void setAddDays(int addDays) {
		this.addDays = addDays;
	}	
	
	public long getProcesados() {
		return procesados;
	}
	
	private String convertLocalTimeToUTC(Date localDate) throws Exception{     
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcFormat.format(localDate);
    }
	
	private int getSemaforoEtapa(String key, String eta, String completado) throws Exception {
		int semaforo = SEMAFORO_VERDE; // verde por defecto
		// si exite fecha de ETA definida
		if (eta != null) {
			// si no existe fecha de completado definida
			if (completado == null) {
				Date currentDate = isoFormat.parse(isoFormat.format(new Date())); // current date without time
				Date dateETA = isoFormat.parse(eta);
				int diasPrevio = Integer.parseInt(((JSONObject)((JSONObject)jsonSettings.get("eta")).get("previo")).get(key).toString());
				
				// set ETA dias previos
		    	Calendar calETA = Calendar.getInstance();
		    	calETA.setTime(dateETA);
		    	calETA.add(Calendar.DATE, diasPrevio * -1);
		    	
		    	// check semaforo
		    	if (daysDiff(currentDate, dateETA) < 0) // si la fecha de ETA es menor a la actual se marca como alerta roja
		    		semaforo = SEMAFORO_ROJO; // rojo
		    	else if (daysDiff(currentDate, calETA.getTime()) < 0) // si la fecha de ETA es menor a la actual mas dias de alerta previa se marca como alerta ambar
		    		semaforo = SEMAFORO_AMBAR; // ambar
			}
		}
		return semaforo;
	}
	
	private int getSemaforoContenedor(List<Integer> semaforosEtapas) throws Exception {
		// semaforo del contenedor
		int numRojos = 0;
		int numAmbars = 0;
		for (Integer semaforo : semaforosEtapas) {
			switch (semaforo) {
				case SEMAFORO_ROJO:
					numRojos++;
					break;
				case SEMAFORO_AMBAR:
					numAmbars++;
					break;					
			}
		}
		if (numRojos > 0)
			return SEMAFORO_ROJO;
		else if (numAmbars > 0)
			return SEMAFORO_AMBAR;
		else
			return SEMAFORO_VERDE;			
	}	
	
	private JSONObject getCntSettings(ObjectStore objStore) throws Exception {
		JSONObject jsonData = new JSONObject();
		
		// get settings object
		Document settings = Factory.Document.fetchInstance(objStore, new Id(CntSettingsId), null);
    	byte[] data = settings.getProperties().getBinaryValue("ClbJSONData");
    	if (data != null)
    		jsonData = JSONObject.parse(new String(data));
    	
    	return jsonData;
	}	
	
	private long daysDiff(Date date1, Date date2) {
		long diff = date2.getTime() - date1.getTime();
		return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS); 			
	}
	
}

