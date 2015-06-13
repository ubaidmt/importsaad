package com.excelecm.loader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.csvreader.CsvReader;
import com.excelecm.common.service.CEService;
import com.excelecm.common.settings.ContentEngineSettings;
import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.constants.RefreshMode;

public class Listas {

	private static Logger log = Logger.getLogger(Listas.class);
	private static String osParentFolder = LoaderConstantes.osRootFolder + "/Listas";
	private final static int LISTA_IMPORTADORAS = 0;
	private final static int LISTA_CONCEPTOCOMPROBANTES = 1;
	private final static int LISTA_TIPOMERCANCIAS = 2;
	private final static int LISTA_TIPOANEXOSCTAGASTOS = 3;

	public static void main(String[] args) {
		
		CEService ceService = null;	
		
		ContentEngineSettings ceSettings = new ContentEngineSettings();
		ceSettings.setUri(LoaderConstantes.ceUri);
		ceSettings.setUser(LoaderConstantes.ceUser);
		ceSettings.setPassword(LoaderConstantes.cePassword);
		ceSettings.setStanza(LoaderConstantes.ceStanza);
		
		ceService = new CEService(ceSettings);
		ceService.establishConnection();		
		
		try
		{			
			Listas lista = new Listas();
			// Eliminar Listas
			lista.deleteAllListas(ceService, LoaderConstantes.ceOS);
			// Crear Listas
			lista.loadLista(ceService, LoaderConstantes.ceOS, "Importadoras.csv", Listas.LISTA_IMPORTADORAS);
			lista.loadLista(ceService, LoaderConstantes.ceOS, "ConceptoComprobantes.csv", Listas.LISTA_CONCEPTOCOMPROBANTES);
			lista.loadLista(ceService, LoaderConstantes.ceOS, "TipoMercancias.csv", Listas.LISTA_TIPOMERCANCIAS);
			lista.loadLista(ceService, LoaderConstantes.ceOS, "TipoAnexosCtaGastos.csv", Listas.LISTA_TIPOANEXOSCTAGASTOS);

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
	
	private static ClassLoader getClassLoader() {
		return Listas.class.getClassLoader();
	}	
	
	@SuppressWarnings("unchecked")
	public void deleteAllListas(CEService ceService, String os) throws Exception {
		int numElementos = 0;
		
		log.debug("Eliminando listas...");
		IndependentObjectSet objSet = ceService.fetchObjects("Lista", null, false, os, 0);
	    for (Iterator<Document> it = objSet.iterator(); it.hasNext(); ) 
	    {
	    	Document doc = it.next();
	    	doc.delete();
	    	doc.save(RefreshMode.REFRESH); 	
	    	numElementos++;
	    }
	    log.debug("Listas eliminadas. Número de elementos eliminados: " + numElementos);
	}
	
	public void loadLista(CEService ceService, String os, String csvFile, int tipoLista) throws Exception {
		
		CsvReader lista = null;
		int numElementos = 0;
		
		try
		{
			log.debug("Creando lista de " + csvFile + "...");
			java.net.URL resource = getClassLoader().getResource(csvFile);
			lista = new CsvReader(new InputStreamReader(new FileInputStream(resource.getPath()), "UTF-8"));
			
			// Get Root Folder
			Folder rootFolder = Factory.Folder.fetchInstance(ceService.getOS(os), osParentFolder, null);
			
			while (lista.readRecord())
			{
				String nombre = lista.get(0).trim();
				Map<String, Object> props = new HashMap<String, Object>();
				props.put("TipoLista", tipoLista);
				props.put("Nombre", nombre);
				
				Document doc = (Document) ceService.createDocumentInstance(os, null, null, null, "Lista", nombre, props, rootFolder.get_PathName());
				doc.set_SecurityFolder(rootFolder);
				doc.save(RefreshMode.REFRESH);
				numElementos++;
				log.debug(nombre);
			}
			
			log.debug("Creación de lista finalizada. Número de elementos creados: " + numElementos);
			
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			lista.close();
		}
	}	
	
}
