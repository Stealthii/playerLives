package com.pathogenstudios.playerlives.dbWrappers;

import com.pathogenstudios.generic.Log;
import com.pathogenstudios.playerlives.PropertyHandler;
import com.pathogenstudios.playerlives.DbWrapper;
import com.pathogenstudios.playerlives.PlayerLives;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class Flatfile extends DbWrapper {
	private PropertyHandler livesDb = null;
	private HashMap<String, Integer> livesList;
	private Timer autoSaveTimer = null;
	private boolean isDirty = true;

	public Flatfile(PlayerLives parent) {
		super(parent);
		load();

		Log.d(Integer.toString(parent.conf.flatFileAutosaveInterval));
		if (parent.conf.flatFileAutosaveInterval > 0) {
			Log.d("Setting up flatfile autosave...");
			autoSaveTimer = new Timer();
			scheduleAutoSave();
		}
	}

	public boolean isActive() {
		return livesDb != null;
	}

	public void load() {
		livesDb = new PropertyHandler(parent.getDataFolder() + File.separator + "livesDb.properties");//Created after the dir is made if it does not exist yet.

		Log.d("Load flatfile lives database...");
		livesList = new HashMap<String, Integer>();
		try {
			ArrayList<String> livesPlayerList = livesDb.getKeys();
			for (int i = 0; i < livesPlayerList.size(); i++) {
				String key = livesPlayerList.get(i);
				int val = livesDb.getInt(key);
				if (parent.conf.verbose) {
					System.out.println(key + " = " + val);
				}
				livesList.put(key, val);
			}
		}
		catch (Exception e) {
			Log.e("COULD NOT LOAD LIVES FLATFILE DATABASE!");
			e.printStackTrace();
		}
		livesDb.save();
	}

	public void save() {
		Log.d("Saving lives db...");
		livesDb.load();
		Iterator<String> itr = livesList.keySet().iterator();
		while (itr.hasNext()) {
			String key = itr.next();
			livesDb.setInt(key, livesList.get(key));
		}
		livesDb.save();
		isDirty = false;
	}

	class AutoSaveTask extends TimerTask {
		public void run() {
			if (isDirty == true) {
				Log.m("Flatfile database autosaving...");
				save();
			}
			scheduleAutoSave();
		}
	}

	private void scheduleAutoSave() {
		autoSaveTimer.schedule(new AutoSaveTask(), parent.conf.flatFileAutosaveInterval * 1000);
	}

	public void close() {
		if (autoSaveTimer != null) {
			autoSaveTimer.cancel();
		}
		super.close();
	}

	public boolean addPlayer(String player, int lives) {
		livesList.put(player, lives);
		return true;
	}

	public boolean exists(String player) {
		return livesList.containsKey(player);
	}

	public int get(String player) {
		int ret = -1;

		if (exists(player)) {
			ret = livesList.get(player);
		}
		else {
			ret = -1;
		}

		if (ret <= 0 && parent.conf.infiniteLives) {
			ret = 1;
		}

		return ret;
	}

	public boolean set(String player, int lives) {
		if (!exists(player)) {
			return false;
		}
		if (lives < 0) {
			lives = 0;
		}
		livesList.put(player, lives);
		isDirty = true;
		return true;
	}
}
