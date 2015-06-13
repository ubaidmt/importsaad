package com.ibm.ecm.extension;

import java.util.Locale;

import com.ibm.ecm.extension.Plugin;
import com.ibm.ecm.extension.PluginFeature;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginAction;
import com.ibm.ecm.extension.PluginResponseFilter;

public class ImportSaadPlugin extends Plugin {
		
		@Override
		public String getId() {
			return "ImportSaadPlugin";
		}

		@Override
		public String getName(Locale locale) {
			return "ImportSaad Plugin";
		}
		
		@Override
		public String getScript() {
			return "ImportSaadPlugin.js";
		}

		@Override
		public String getDebugScript() {
			return "ImportSaadPlugin.js";
		}		
		
		@Override
		public String getVersion() {
			return "2.0.3";
		}

		@Override
		public String getCopyright() {
			return "Copyright ImportSaad S.A. de C.V.";
		}
		
		@Override
		public String getCSSFileName() {
			return "ImportSaadPlugin.css";
		}
		
		@Override
		public String getDebugCSSFileName() {
			return "ImportSaadPlugin.css";
		}		

		@Override
		public String getDojoModule() {
			return "importSaadPluginDojo";
		}
		
		@Override
		public String getConfigurationDijitClass() {
			return "importSaadPluginDojo.ConfigurationPane";
		}		
		
		@Override
		public PluginAction[] getActions() {
			return new PluginAction[] { 
					new ImportSaadRegistraContenedorAction(),
					new SolDocSettingsAction(),
					new SolDocProveedoresAction(),
					new SolDocClientesAction(),
					new SolDocSolicitaFacturaAction(),
					new SolDocPagosClientesAction(),
					new SolDocEditarFacturaAcion(),
					new SolDocCopiarSolicitudAction(),
					new SolDocDevolucionesProvAction(),
					new SolDocDevolucionesCliAction(),
					new SolDocVersionarAction(),
					new SolDocEmpresasAction()
					};
		}			
		
		@Override
		public PluginService[] getServices() {
			return new PluginService[] { 
					new ImportSaadPluginService(),
					new ConfigService(),
					new ImportSaadPagosService()
					};
		}	
		
		@Override
		public PluginResponseFilter[] getResponseFilters() {
			return new PluginResponseFilter[] { 
					new ImportSaadReponseFilter() 
					};
		}		
		
		@Override
		public PluginFeature[] getFeatures() {
			return new PluginFeature[] { 
					new ImportSaadReportesFeature() 
					};
		}			

}

