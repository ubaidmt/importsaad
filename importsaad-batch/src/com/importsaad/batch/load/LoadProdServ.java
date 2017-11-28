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

public class LoadProdServ {
	
	private static Logger log = Logger.getLogger(LoadProdServ.class);
	private ObjectStore os = null;
	private final static String resource = "xlsx/ProdServ.xlsx";

	public static void main(String[] args) {
		new LoadProdServ().doExec();
	}
	
	public void doExec() {
		
		int processed = 0;
		int maxRecords = 0;
		InputStream is = null;
		XSSFWorkbook workbook = null;		
		
		try {		
		
			java.util.Properties configProps = PropertyConfig.getInstance().getPropertiesResource();
			
			// Open P8 connection
			Connection con = Factory.Connection.getConnection(configProps.getProperty("ce.uri"));
		    Subject subject = UserContext.createSubject(con, configProps.getProperty("ce.user"), configProps.getProperty("ce.password"), configProps.getProperty("ce.stanza"));
		    UserContext.get().pushSubject(subject); 
		    os = Factory.ObjectStore.fetchInstance(Factory.Domain.getInstance(con,null), configProps.getProperty("ce.os"), null);
		    
			log.debug("Eliminando prodserv existentes...");
			
			// delete previous objects
			Folder parentFolder = Factory.Folder.fetchInstance(os, "/ProdServ", null);
			deleteRecursively(parentFolder, Boolean.FALSE);
			
			log.debug("Cargando prodserv ...");
			
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
			    	String clave = getCellStringValue(row.getCell(0)).trim();
		    		String name = getCellStringValue(row.getCell(1)).trim().toUpperCase();
	    		
				// Crea documento
				Document doc = Factory.Document.createInstance(os, "SolDocProdServ");
				doc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
				
				com.filenet.api.property.Properties props = doc.getProperties();
				props.putObjectValue("DocumentTitle", name);
				props.putObjectValue("Clave", clave);			
							
				doc.set_SecurityFolder(parentFolder); // inherit permissions from parent folder
				doc.save(RefreshMode.REFRESH);
				
				// File doc
				ReferentialContainmentRelationship rel = parentFolder.file(doc, AutoUniqueName.AUTO_UNIQUE, null, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
				rel.save(RefreshMode.NO_REFRESH);	
				
				processed++;
				log.debug(clave + " - " + name + " (" + processed + ")");
				
	    			if (maxRecords > 0 && processed >= maxRecords)
	    				break; // exit loop
		    }
		    
			log.debug("Total de prodserv cargados: " + processed);
		    
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
