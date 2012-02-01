package com.pathogenstudios.generic;

//~--- non-JDK imports --------------------------------------------------------

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LangMan {
    private String                           defaultLanguage = "en";
    private Map<String, Map<String, String>> langDb;
    private JavaPlugin                       parent;

    public LangMan(JavaPlugin parent, String defaultLanguage) {
        Log.d("Creating language manager...");
        this.parent          = parent;
        this.defaultLanguage = defaultLanguage;

        // Set up the language map:
        langDb = new HashMap<String, Map<String, String>>();

        // Extract the default language file to the file system and then read it...
        PathogenUtil.extractResourceIfNotExist(defaultLanguage + ".yml",
                getLangFolder() + File.separator + defaultLanguage + ".yml");
        readLangFile(defaultLanguage);
    }

    public String getLangFolder() {
        return parent.getDataFolder() + File.separator + "lang";
    }

    public void readLangFile(String language) {
        String filename = getLangFolder() + File.separator + language + ".yml";

        try {
            Configuration config = new Configuration(new File(filename));

            config.load();

            List<String> keys = config.getKeys();

            if (!langDb.containsKey(language)) {
                langDb.put(language, new HashMap<String, String>());
            }

            Map<String, String> langMap = langDb.get(language);

            for (Iterator<String> it = keys.iterator(); it.hasNext(); ) {
                String key = it.next();

                Log.v("Adding " + language + "." + key + ";");
                langMap.put(key, config.getString(key));
            }

            Log.d("Done parsing localization file '" + filename + "' for language '" + language + "'");
        } catch (Exception ex) {
            Log.e("Could not load localizaion file '" + filename + "'");
        }
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public String get(String language, String key, String... vars) {
        Map<String, String> langMap;

        if (langDb.containsKey(language)) {
            langMap = langDb.get(language);
        } else {
            Log.w("Requested localized string in language '" + language + "' was not found.");
            langMap = langDb.get(defaultLanguage);

            if (langMap == null) {
                Log.e("Default language is not avaialable as fallback!");
            }
        }

        if (langMap == null) {
            return "LANG_" + language.toUpperCase() + "_NOTFOUND";
        } else {

            // Check if the key exists, if not, try the default language.
            if (!langMap.containsKey(key)) {
                langMap = langDb.get(defaultLanguage);

                if ((langMap == null) ||!langMap.containsKey(key)) {
                    Log.w("Requested localized string '" + key + "' in language '" + language
                          + "' was not found in the language " + ((langMap == null)
                            ? " and defaultLanguage is not loaded!"
                            : " or defaultLanguage!"));

                    return language.toUpperCase() + "_" + key.toUpperCase() + "_NOTFOUND";
                }
            }

            String localizedString = langMap.get(key);
            String varName         = "";

            for (String var : vars) {
                if (varName.isEmpty()) {
                    varName = var;
                } else {
                    Log.d("Replacing $" + varName + " with '" + var + "'");
                    localizedString = localizedString.replaceAll("\\$" + varName, var);
                    varName         = "";
                }
            }

            return localizedString;
        }
    }
}
