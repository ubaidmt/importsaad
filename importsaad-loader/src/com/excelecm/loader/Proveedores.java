package com.excelecm.loader;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.csvreader.CsvReader;
import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ContentEngineSettings;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;

public class Proveedores {
	
	private static Logger log = Logger.getLogger(Proveedores.class);
	private static String csvFile = "Proveedores.csv";
	private static String osParentFolder = LoaderConstantes.osRootFolder + "/Proveedores";

	public static void main(String[] args) {
		new Proveedores().loadProveedores();
	}
	
	private static ClassLoader getClassLoader() {
		return Listas.class.getClassLoader();
	}	
	
	@SuppressWarnings("unchecked")
	public void deleteAllProveedores(CEService ceService, String os) throws Exception {
		int numElementos = 0;
		
		log.debug("Eliminando proveedores...");
		IndependentObjectSet objSet = ceService.fetchObjects("Proveedor", null, false, os, 0);
	    for (Iterator<Document> it = objSet.iterator(); it.hasNext(); ) 
	    {
	    	Document doc = it.next();
	    	doc.delete();
	    	doc.save(RefreshMode.REFRESH); 	
	    	numElementos++;
	    }
	    log.debug("Proveedores eliminados. Número de elementos eliminados: " + numElementos);
	}	
	
	public void loadProveedores() {
		
		CsvReader proveedores = null;
		CEService ceService = null;	
		int numProvedores = 0;
		
		try
		{
			log.debug("Creando proveedores...");
			
			java.net.URL resource = getClassLoader().getResource(csvFile);	
			proveedores = new CsvReader(new InputStreamReader(new FileInputStream(resource.getPath()), "UTF-8"));
			
			ContentEngineSettings ceSettings = new ContentEngineSettings();
			ceSettings.setUri(LoaderConstantes.ceUri);
			ceSettings.setUser(LoaderConstantes.ceUser);
			ceSettings.setPassword(LoaderConstantes.cePassword);
			ceSettings.setStanza(LoaderConstantes.ceStanza);
			
			ceService = new CEService(ceSettings);
			ceService.establishConnection();
			
			// Get Root Folder
			Folder rootFolder = Factory.Folder.fetchInstance(ceService.getOS(LoaderConstantes.ceOS), osParentFolder, null);
			
			// Limpiar Proveedores
			deleteAllProveedores(ceService, LoaderConstantes.ceOS);
			
			while (proveedores.readRecord())
			{
				String proveedorCodigo = proveedores.get(0).toUpperCase().trim();
				String proveedorNombre = proveedores.get(1).toUpperCase().trim() + " (" + proveedorCodigo + ")";

				Map<String, Object> props = new HashMap<String, Object>();
				props.put("Codigo", proveedorCodigo);
				props.put("Nombre", proveedorNombre);
				
				Document doc = (Document) ceService.createDocumentInstance(LoaderConstantes.ceOS, null, null, null, "Proveedor", proveedorNombre, props, rootFolder.get_PathName());
				doc.set_SecurityFolder(rootFolder);
				doc.save(RefreshMode.REFRESH);				
				numProvedores++;
				log.debug("Provedor " + proveedorNombre + " creado.");
			}
			
			log.debug("Creación de provedores finalizada. Número de proveedores creados: " + numProvedores);
			
		}
		catch (Exception e)
		{
			log.error(e);
		}
		finally
		{
			ceService.releaseConnection();
			proveedores.close();
		}
	}

}
