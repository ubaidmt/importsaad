package com.ibm.ecm.extension;

import java.util.Locale;

import com.ibm.ecm.extension.Plugin;
import com.ibm.ecm.extension.PluginFeature;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.action.SolDocClientesAction;
import com.ibm.ecm.extension.action.SolDocCopiarSolicitudAction;
import com.ibm.ecm.extension.action.SolDocDevolucionesCliAction;
import com.ibm.ecm.extension.action.SolDocDevolucionesProvAction;
import com.ibm.ecm.extension.action.SolDocEditarFacturaAcion;
import com.ibm.ecm.extension.action.SolDocEmpresasAction;
import com.ibm.ecm.extension.action.SolDocPagosClientesAction;
import com.ibm.ecm.extension.action.SolDocProveedoresAction;
import com.ibm.ecm.extension.action.SolDocSettingsAction;
import com.ibm.ecm.extension.action.SolDocSolicitaFacturaAction;
import com.ibm.ecm.extension.action.SolDocVersionarAction;
import com.ibm.ecm.extension.action.CntSettingsAction;
import com.ibm.ecm.extension.action.CntFraccionesAction;
import com.ibm.ecm.extension.action.CntClientesAction;
import com.ibm.ecm.extension.action.CntNavierasAction;
import com.ibm.ecm.extension.action.CntForwardersAction;
import com.ibm.ecm.extension.action.CntProveedoresAction;
import com.ibm.ecm.extension.action.CntImportadorasAction;
import com.ibm.ecm.extension.action.CntPuertosAction;
import com.ibm.ecm.extension.service.ConfigService;
import com.ibm.ecm.extension.service.SettingsService;
import com.ibm.ecm.extension.service.ReportesService;
import com.ibm.ecm.extension.service.SolDocPagosService;
import com.ibm.ecm.extension.service.SolDocService;
import com.ibm.ecm.extension.service.CntCatalogosService;
import com.ibm.ecm.extension.service.CntCotizacionesService;
import com.ibm.ecm.extension.service.CntContenedoresService;
import com.ibm.ecm.extension.PluginAction;

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
			return "importSaadPluginDojo.config.ConfigurationPane";
		}		
		
		@Override
		public PluginAction[] getActions() {
			return new PluginAction[] { 
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
					new SolDocEmpresasAction(),
					new CntSettingsAction(),
					new CntFraccionesAction(),
					new CntClientesAction(),
					new CntNavierasAction(),
					new CntForwardersAction(),
					new CntProveedoresAction(),
					new CntImportadorasAction(),
					new CntPuertosAction()
					};
		}			
		
		@Override
		public PluginService[] getServices() {
			return new PluginService[] { 
					new SolDocService(),
					new SolDocPagosService(),
					new ReportesService(),
					new ConfigService(),
					new SettingsService(),
					new CntCatalogosService(),
					new CntCotizacionesService(),
					new CntContenedoresService()
					};
		}
		
		@Override
		public PluginFeature[] getFeatures() {
			return new PluginFeature[] { 
					new ReportesFeature(),
					new CotizacionesFeature(),
					new ContenedoresFeature()
					};
		}			

}

