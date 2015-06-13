package com.excelecm.loader;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.csvreader.CsvReader;
import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ContentEngineSettings;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;

public class Clientes {

	private static Logger log = Logger.getLogger(Clientes.class);
	private static String csvFile = "Clientes.csv";
	private static String osParentFolder = LoaderConstantes.osRootFolder + "/Clientes";

	public static void main(String[] args) {
		new Clientes().loadClientes();
	}
	
	private static ClassLoader getClassLoader() {
		return Listas.class.getClassLoader();
	}		
	
	@SuppressWarnings("unchecked")
	public void deleteAllClientes(CEService ceService, String os) throws Exception {
		int numElementos = 0;
		
		log.debug("Eliminando clientes...");
		IndependentObjectSet objSet = ceService.fetchObjects("Cliente", null, false, os, 0);
	    for (Iterator<Document> it = objSet.iterator(); it.hasNext(); ) 
	    {
	    	Document doc = it.next();
	    	doc.delete();
	    	doc.save(RefreshMode.REFRESH); 	
	    	numElementos++;
	    }
	    log.debug("Clientes eliminados. Número de elementos eliminados: " + numElementos);
	}		
	
	public void loadClientes() {
		
		CsvReader clientes = null;
		CEService ceService = null;	
		int numClientes = 0;
		
		try
		{
			log.debug("Creando clientes...");
			
			java.net.URL resource = getClassLoader().getResource(csvFile);	
			clientes = new CsvReader(new InputStreamReader(new FileInputStream(resource.getPath()), "UTF-8"));
			
			ContentEngineSettings ceSettings = new ContentEngineSettings();
			ceSettings.setUri(LoaderConstantes.ceUri);
			ceSettings.setUser(LoaderConstantes.ceUser);
			ceSettings.setPassword(LoaderConstantes.cePassword);
			ceSettings.setStanza(LoaderConstantes.ceStanza);
			
			ceService = new CEService(ceSettings);
			ceService.establishConnection();
			
			// Get Root Folder
			Folder rootFolder = Factory.Folder.fetchInstance(ceService.getOS(LoaderConstantes.ceOS), osParentFolder, null);
			
			// Limpiar Clientes
			deleteAllClientes(ceService, LoaderConstantes.ceOS);			
			
			while (clientes.readRecord())
			{
				String clienteCodigo = clientes.get(0).toUpperCase().trim();
				String clienteNombre = clientes.get(1).toUpperCase().trim() + " (" + clienteCodigo + ")";
				String clienteRFC = clientes.get(2).toUpperCase();

				Map<String, Object> props = new HashMap<String, Object>();
				props.put("Codigo", clienteCodigo);
				props.put("Nombre", clienteNombre);
				props.put("RFC", clienteRFC);
				
				Document doc = (Document) ceService.createDocumentInstance(LoaderConstantes.ceOS, null, null, null, "Cliente", clienteNombre, props, rootFolder.get_PathName());
				doc.set_SecurityFolder(rootFolder);
				doc.save(RefreshMode.REFRESH);
				numClientes++;
				log.debug("Cliente " + clienteNombre + " creado.");
			}
			
			log.debug("Creación de clientes finalizada. Número de clientes creados: " + numClientes);
			
		}
		catch (Exception e)
		{
			log.error(e);
		}
		finally
		{
			ceService.releaseConnection();
			clientes.close();
		}
	}

}
