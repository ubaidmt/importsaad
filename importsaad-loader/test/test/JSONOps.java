package test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ContentEngineSettings;
import com.filenet.api.util.Id;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.core.Factory;
import com.filenet.api.core.IndependentObject;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class JSONOps {
	
	public static void main(String[] args) {
		
		JSONOps jsonOps = new JSONOps();
		CEService ceService = null;	
		
		try {
		
			ceService = jsonOps.getService();
			//jsonOps.setData(ceService, "ImportSaadOS");
			//jsonOps.getData(ceService, "ImportSaadOS");
			//jsonOps.getReportesData(ceService, "ImportSaadOS");
			jsonOps.setSecurityTemplate(ceService, "DefaultOS");
		
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			jsonOps.logOff(ceService);
		}
	}
	
	public CEService getService() {
		
		ContentEngineSettings ceSettings = new ContentEngineSettings();
		ceSettings.setUri("http://p8server:9080/wsi/FNCEWS40MTOM/");
		ceSettings.setUser("operaciones");
		ceSettings.setPassword("filenet");
		ceSettings.setStanza("FileNetP8WSI");
		
		CEService ceService = new CEService(ceSettings);
		ceService.establishConnection();	
		
		return ceService;
		
	}
	
	public void logOff(CEService ceService) {
		
		try {
		
			if (ceService != null)
				ceService.releaseConnection();
		
		} catch (Exception e) {}
	}
	
	@SuppressWarnings("unchecked")
	public void setData(CEService ceService, String osName) throws Exception {
		
		String sql = "SELECT FolderName, ClbJSONData FROM EDS WITH EXCLUDESUBCLASSES";
		IndependentObjectSet edsSet = ceService.fetchObjects(sql, osName, 0);	
		
		for (Iterator<IndependentObject> it = edsSet.iterator(); it.hasNext(); ) 
		{
			IndependentObject edsObj = it.next();
			
			JSONArray jsonArray = new JSONArray();
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("symbolicName", "NumeroContenedor");
			jsonObject.put("required", true);
			jsonObject.put("displayMode", "readonly");
			jsonArray.add(jsonObject);
			Map<String, Object> props = new HashMap<String, Object>();
			props.put("ClbJSONData", jsonArray.serialize().getBytes());	
	
			ceService.saveObjectProperties(edsObj, props);
			System.out.println("JSONData Set");
		}	    		
		
	}
	
	@SuppressWarnings("unchecked")
	public void getData(CEService ceService, String osName) throws Exception {
		
		String sql = "SELECT FolderName, ClbJSONData FROM EDS WITH EXCLUDESUBCLASSES";
		RepositoryRowSet edsSet = ceService.fetchRows(sql, osName, 0);		
		
		for (Iterator<RepositoryRow> it = edsSet.iterator(); it.hasNext(); ) 
		{
			RepositoryRow edsRecord = it.next();
			String edsName = edsRecord.getProperties().getStringValue("FolderName");
			System.out.println(edsName);
			byte[] edsData= edsRecord.getProperties().getBinaryValue("ClbJSONData");
			if (edsData != null) {
				JSONArray jsonArray;
				try {
					jsonArray = JSONArray.parse(new String(edsData));
					System.out.println(jsonArray.toString());
				} catch (java.io.IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}	    		
		
	}
	
	@SuppressWarnings("unchecked")
	public void getReportesData(CEService ceService, String osName) throws Exception {
		
		String sql = "SELECT Id, ClbJSONData FROM ReportTemplate WITH EXCLUDESUBCLASSES";
		RepositoryRowSet rowSet = ceService.fetchRows(sql, osName, 0);		
		
		for (Iterator<RepositoryRow> it = rowSet.iterator(); it.hasNext(); ) 
		{
			RepositoryRow reporte = it.next();
			byte[] reporteData= reporte.getProperties().getBinaryValue("ClbJSONData");
			if (reporteData != null) {
				JSONObject jsonObject;
				try {
					jsonObject = JSONObject.parse(new String(reporteData));
					System.out.println(jsonObject.get("reporte").toString());
				} catch (java.io.IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}	    		
		
	}
	
	public void setSecurityTemplate(CEService ceService, String osName) throws Exception {
		
		com.filenet.api.core.Folder fol = Factory.Folder.fetchInstance(ceService.getOS(osName), "{40D5934B-0000-CC13-B888-9BDA5B38832F}", null);
		fol.applySecurityTemplate(new Id("{E42F9064-2173-4FB1-8DCB-2617F2550F43}"));
		fol.save(RefreshMode.REFRESH);
		
	}

}
