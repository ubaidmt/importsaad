package com.ibm.ecm.extension;

import java.util.Locale;
import com.ibm.ecm.extension.PluginFeature;

public class ContenedoresFeature extends PluginFeature {

	@Override
	public String getId() {
		return "ImportSaadContenedoresFeature";
	}

	@Override
	public String getName(Locale locale) {
		return "Contenedores";		
	}

	@Override
	public String getDescription(Locale locale) {
		return "Contenedores";
	}
	
	@Override
	public String getIconUrl() {
		return "contenedoresLaunchIcon";
	}
	
	@Override
	public String getSvgFilePath() {
		return "WebContent/images/importSaadPlugin.svg";
	}	

	@Override
	public String getFeatureIconTooltipText(Locale locale) {
		return "Contenedores";	
	}

	@Override
	public String getPopupWindowTooltipText(Locale locale) {
		return null;
	}

	@Override
	public String getContentClass() {
		return "importSaadPluginDojo.feature.ContenedoresFeaturePane";
	}

	@Override
	public String getPopupWindowClass() {
		return null;
	}

	@Override
	public boolean isPreLoad() {
		return false;
	}

	@Override
	public String getHelpContext() {
		return null;
	}
	
	@Override
	public String getConfigurationDijitClass() {
		return null;
	}	

}
