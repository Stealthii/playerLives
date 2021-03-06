package com.pathogenstudios.playerlives;

//~--- JDK imports ------------------------------------------------------------

//Blatant steal from the permissions plugin!
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Permissions 2.x
 * Copyright (C) 2011  Matt 'The Yeti' Burnett <admin@theyeticave.net>
 * Original Credit & Copyright (C) 2010 Nijikokun <nijikokun@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Permissions Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Permissions Public License for more details.
 *
 * You should have received a copy of the GNU Permissions Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * PropertyHandler
 *
 * Reads & Writes properties files.
 *
 * @author Nijiko
 */
public final class PropertyHandler {
    private static final Logger log = Logger.getLogger("Minecraft");
    private String              fileName;
    private Properties          properties;

    public PropertyHandler(String fileName) {
        this.fileName   = fileName;
        this.properties = new Properties();

        File file = new File(fileName);

        if (file.exists()) {
            load();
        } else {
            save();    // This fails when the directory does not exist!
        }
    }

    public void load() {
        try {
            this.properties.load(new FileInputStream(this.fileName));
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Unable to load " + this.fileName, ex);
        }
    }

    public void save() {
        try {
            this.properties.store(new FileOutputStream(this.fileName), "Minecraft Properties File");
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Unable to save " + this.fileName, ex);
        }
    }

    public Map<String, String> returnMap() throws Exception {
        Map<String, String> map    = new HashMap<String, String>();
        BufferedReader      reader = new BufferedReader(new FileReader(this.fileName));
        String              line;

        while ((line = reader.readLine()) != null) {
            if (line.trim().length() == 0) {
                continue;
            }

            if (line.charAt(0) == '#') {
                continue;
            }

            int    delimPosition = line.indexOf('=');
            String key           = line.substring(0, delimPosition).trim();
            String value         = line.substring(delimPosition + 1).trim();

            map.put(key, value);
        }

        reader.close();

        return map;
    }

    // Added by Pathogen_David
    public ArrayList<String> getKeys() throws Exception {
        ArrayList<String> map    = new ArrayList<String>();
        BufferedReader    reader = new BufferedReader(new FileReader(this.fileName));
        String            line;

        while ((line = reader.readLine()) != null) {
            if (line.trim().length() == 0) {
                continue;
            }

            if (line.charAt(0) == '#') {
                continue;
            }

            int    delimPosition = line.indexOf('=');
            String key           = line.substring(0, delimPosition).trim();

            // String value = line.substring(delimPosition + 1).trim();
            map.add(key);
        }

        reader.close();

        return map;
    }

    public void removeKey(String key) {
        this.properties.remove(key);
        save();
    }

    public boolean keyExists(String key) {
        return this.properties.containsKey(key);
    }

    public String getString(String key) {
        if (this.properties.containsKey(key)) {
            return this.properties.getProperty(key);
        }

        return "";
    }

    public String getString(String key, String value) {
        if (this.properties.containsKey(key)) {
            return this.properties.getProperty(key);
        }

        setString(key, value);

        return value;
    }

    public void setString(String key, String value) {
        this.properties.setProperty(key, value);
        save();
    }

    public int getInt(String key) {
        if (this.properties.containsKey(key)) {
            return Integer.parseInt(this.properties.getProperty(key));
        }

        return 0;
    }

    public int getInt(String key, int value) {
        if (this.properties.containsKey(key)) {
            return Integer.parseInt(this.properties.getProperty(key));
        }

        setInt(key, value);

        return value;
    }

    public void setInt(String key, int value) {
        this.properties.setProperty(key, String.valueOf(value));
        save();
    }

    public double getDouble(String key) {
        if (this.properties.containsKey(key)) {
            return Double.parseDouble(this.properties.getProperty(key));
        }

        return 0;
    }

    public double getDouble(String key, double value) {
        if (this.properties.containsKey(key)) {
            return Double.parseDouble(this.properties.getProperty(key));
        }

        setDouble(key, value);

        return value;
    }

    public void setDouble(String key, double value) {
        this.properties.setProperty(key, String.valueOf(value));
        save();
    }

    public long getLong(String key) {
        if (this.properties.containsKey(key)) {
            return Long.parseLong(this.properties.getProperty(key));
        }

        return 0;
    }

    public long getLong(String key, long value) {
        if (this.properties.containsKey(key)) {
            return Long.parseLong(this.properties.getProperty(key));
        }

        setLong(key, value);

        return value;
    }

    public void setLong(String key, long value) {
        this.properties.setProperty(key, String.valueOf(value));
        save();
    }

    public boolean getBoolean(String key) {
        if (this.properties.containsKey(key)) {
            return Boolean.parseBoolean(this.properties.getProperty(key));
        }

        return false;
    }

    public boolean getBoolean(String key, boolean value) {
        if (this.properties.containsKey(key)) {
            return Boolean.parseBoolean(this.properties.getProperty(key));
        }

        setBoolean(key, value);

        return value;
    }

    public void setBoolean(String key, boolean value) {
        this.properties.setProperty(key, String.valueOf(value));
        save();
    }
}
