package com.pathogenstudios.generic;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class LangMan {
	private String defaultLanguage = "en";
	private JavaPlugin parent;
	
	private Map<String,Map<String,String>> langDb;
	
	public LangMan(JavaPlugin parent, String defaultLanguage) {
		this.defaultLanguage = defaultLanguage;
		
		//Set up the language map:
		langDb = new HashMap<String,Map<String,String>>();
		
		//Extract the default language file to the file system and then read it...
		PathogenUtil.extractResourceIfNotExist(defaultLanguage + ".yml", getLangFolder() + File.separator + defaultLanguage + ".yml");
		readLangFile(defaultLanguage);
	}
	
	public String getLangFolder() {return parent.getDataFolder() + File.separator + "lang";}
	
	public void readLangFile(String language) {
		String filename = getLangFolder() + File.separator + language + ".yml";
		
		try	{
			Configuration config = new Configuration(new File(filename));
			config.load();
			
			List<String> keys = config.getKeys();
			
			if (!langDb.containsKey(language)) {
				langDb.put(language, new HashMap<String,String>());
			}
			Map<String,String> langMap = langDb.get(language);
			
			for(Iterator<String> it = keys.iterator(); it.hasNext();) {
				String key = it.next();
				Log.d("Adding " + language + "." + key + "." + config.getString(key) + ";");
				langMap.put(key, config.getString(key));
			}
			
			Log.d("Done parsing localization file '" + filename + "' for language '" + language + "'");
		}
		catch (Exception ex) {
			Log.e("Could not load localizaion file '" + filename +"'");
		}
	}
}
