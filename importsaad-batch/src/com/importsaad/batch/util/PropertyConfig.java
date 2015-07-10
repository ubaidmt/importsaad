package com.importsaad.batch.util;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyConfig {
	
	private static final PropertyConfig singleton = new PropertyConfig();
	private static final String FILENAME = "config.properties";
	private Properties properties = new Properties();
	
    public static PropertyConfig getInstance() {
        return singleton;
    }		
	
	private PropertyConfig() {
		try 
		{
			properties.load(getStream(FILENAME, null));
		} 
		catch (IOException ioe) 
		{
			properties = null;
		}
	}
	
	public Properties getPropertiesResource() {
		return properties;		
	}
	
	private static ClassLoader getClassLoader() {
		return PropertyConfig.class.getClassLoader();
	}
	
	private static InputStream getStream(String resource, String directory) {
		InputStream is = getClassLoader().getResourceAsStream(resource);
		if (is == null) {
			if (directory != null)
				is = getClassLoader().getResourceAsStream(directory + '/' + resource);
			else
				is = getClassLoader().getResourceAsStream(resource);
		}
		return is;
	}	

}
