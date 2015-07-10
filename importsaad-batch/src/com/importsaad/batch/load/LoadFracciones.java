package com.importsaad.batch.load;

import java.io.InputStream;
import java.util.Iterator;

import javax.security.auth.Subject;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.importsaad.batch.util.PropertyConfig;
import com.filenet.api.collection.DocumentSet;
import com.filenet.api.collection.FolderSet;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.core.VersionSeries;
import com.filenet.api.util.UserContext;

import com.ibm.json.java.JSONObject;

public class LoadFracciones {
	
	private static Logger log = Logger.getLogger(LoadFracciones.class);
	private ObjectStore os = null;
	private final static String resource = "xlsx/Fracciones.xlsx";

	public static void main(String[] args) {
		new LoadFracciones().doExec();
	}
	
	public void doExec() {
		
		int processed = 0;
		InputStream is = null;
		XSSFWorkbook workbook = null;		
		
		try {		
		
			java.util.Properties configProps = PropertyConfig.getInstance().getPropertiesResource();
			
			// Open P8 connection
			Connection con = Factory.Connection.getConnection(configProps.getProperty("ce.uri"));
		    Subject subject = UserContext.createSubject(con, configProps.getProperty("ce.user"), configProps.getProperty("ce.password"), configProps.getProperty("ce.stanza"));
		    UserContext.get().pushSubject(subject); 
		    os = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), configProps.getProperty("ce.os"), null);
		    
			log.debug("Eliminando fracciones arancelarias existentes...");
			
			// delete previous objects
			Folder parentFolder = Factory.Folder.fetchInstance(os, "/Fracciones", null);
			deleteRecursively(parentFolder, Boolean.FALSE);
			
			log.debug("Cargando nuevas fracciones arancelarias...");
			
			// Get excel workbook
			is = this.getClass().getClassLoader().getResourceAsStream(resource);
			if (is == null)
				throw new java.io.IOException("Resource " + resource  + " not found.");
			
			workbook = new XSSFWorkbook(is);
			XSSFSheet sheet = workbook.getSheetAt(0);			
		    
		    // Parse workbook rows
		    for (Iterator<Row> rowIterator = sheet.iterator(); rowIterator.hasNext(); ) 
		    {
		    	Row row = rowIterator.next();
		    	
		    	// Get datos
	    		String name = getCellStringValue(row.getCell(0)).trim();
	    		String descripcion = getCellStringValue(row.getCell(1)).trim();
	    		int unidad = formatUnidadComercial(getCellStringValue(row.getCell(2)).trim());
	    		double precio = Double.parseDouble(getCellStringValue(row.getCell(3)).trim());

	    		JSONObject jsonData = new JSONObject();
	    		jsonData.put("name", name);
	    		jsonData.put("descripcion", descripcion);
	    		jsonData.put("unidad", unidad);
	    		jsonData.put("precio", precio);
	    		
				// Crea nueva fraccion
				Document fraccion = Factory.Document.createInstance(os, "CntFraccion");
				fraccion.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
				
				com.filenet.api.property.Properties props = fraccion.getProperties();
				props.putObjectValue("DocumentTitle", name);			
				props.putValue("ClbJSONData", jsonData.serialize().getBytes());
							
				fraccion.set_SecurityFolder(parentFolder); // inherit permissions from parent folder
				fraccion.save(RefreshMode.REFRESH);
				
				// File fraccion
				ReferentialContainmentRelationship rel = parentFolder.file(fraccion, AutoUniqueName.AUTO_UNIQUE, null, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
				rel.save(RefreshMode.NO_REFRESH);	
				
				processed++;
		    }
		    
			log.debug("Total de fracciones cargadas: " + processed);
		    
		} catch (Exception e) {
			log.error(e);
		} finally {
			UserContext.get().popSubject();
			try {
	    		if (is != null) is.close();
	    		if (workbook != null) workbook.close();
			} catch (java.io.IOException ioe) {}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void deleteRecursively(Folder folder, boolean deleteFolders) throws Exception {
		FolderSet folSet = folder.get_SubFolders();
		for (Iterator<Folder> itFol = folSet.iterator(); itFol.hasNext(); ) 
		{
			Folder subFolder = itFol.next();
			deleteRecursively(subFolder, deleteFolders);
		}
		DocumentSet docSet = folder.get_ContainedDocuments();
		for (Iterator<Document> itDoc = docSet.iterator(); itDoc.hasNext(); ) 
		{
	    	Document doc = itDoc.next();
	    	VersionSeries vs = doc.get_VersionSeries();
	    	vs.delete();
	    	vs.save(RefreshMode.REFRESH);			
		}
		if (deleteFolders) {
			folder.delete();
			folder.save(RefreshMode.REFRESH);
		}
	}
	
	private int formatUnidadComercial(String value) {
		int unidad = 0;
		if (value.equalsIgnoreCase("Kg"))
			unidad = 0;
		else if (value.equalsIgnoreCase("M"))
			unidad = 1;
		else if (value.equalsIgnoreCase("M2"))
			unidad = 2;
		else if (value.equalsIgnoreCase("Pza"))
			unidad = 3;				
		return unidad;
	}
	
	private static String getCellStringValue(Cell cell) {
		if (cell == null)
			return "";
		
        switch(cell.getCellType()) {
	        case Cell.CELL_TYPE_BOOLEAN:
	        	return Boolean.toString((Boolean)cell.getBooleanCellValue());
	        case Cell.CELL_TYPE_NUMERIC:
	        	return NumberToTextConverter.toText(cell.getNumericCellValue());
	        case Cell.CELL_TYPE_STRING:
	        	return cell.getStringCellValue().trim();
        }		
        return "";
	}			    

}
