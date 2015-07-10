package com.ibm.ecm.extension;

import java.util.Locale;
import com.ibm.ecm.extension.PluginFeature;

public class ReportesFeature extends PluginFeature {

	@Override
	public String getId() {
		return "ImportSaadReportesFeature";
	}

	@Override
	public String getName(Locale locale) {
		return "Reportes";		
	}

	@Override
	public String getDescription(Locale locale) {
		return "Reportes";
	}
	
	@Override
	public String getIconUrl() {
		return "reportesLaunchIcon";
	}
	
	@Override
	public String getSvgFilePath() {
		return "WebContent/images/importSaadPlugin.svg";
	}	

	@Override
	public String getFeatureIconTooltipText(Locale locale) {
		return "Reportes";	
	}

	@Override
	public String getPopupWindowTooltipText(Locale locale) {
		return null;
	}

	@Override
	public String getContentClass() {
		return "importSaadPluginDojo.feature.ReportesPane";
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
