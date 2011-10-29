package com.pathogenstudios.playerlives.dbWrappers;

import com.pathogenstudios.generic.Log;
import com.pathogenstudios.playerlives.DbWrapper;
import com.pathogenstudios.playerlives.PlayerLives;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;

public class MySQL extends DbWrapper {
	private Connection con = null;
	private Statement db = null;

	public MySQL(PlayerLives parent) {
		super(parent);
		open(true);
	}

	public void open() {
		open(false);
	}

	public void open(boolean firstTime) {
		Log.d(firstTime ? "Starting MySQL Database Engine..." : "Reconnecting to lost MySQL Database server...");

		//Make MySQL connection:
		try {
			new com.mysql.jdbc.Driver();
			con = DriverManager.getConnection("jdbc:mysql://" + parent.conf.dbHost + ":" + parent.conf.dbPort + "/" + parent.conf.dbDatabase, parent.conf.dbUser, parent.conf.dbPassword);
			db = con.createStatement();
		}
		catch (SQLException e) {
			Log.e("Error initialzing MySQL.");
			e.printStackTrace();
			if (db != null) {
				try {
					db.close();
				}
				catch (SQLException ex) {}
			}
			if (con != null) {
				try {
					con.close();
				}
				catch (SQLException ex) {}
			}
			con = null;
			db = null;
			return;
		}

		//Check if we need to init the table:
		Log.d("Checking if table needs to be made...");
		String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = '" + parent.conf.dbDatabase + "' AND table_name = '" + parent.conf.dbTable + "'";
		ResultSet result = null;
		try {
			result = db.executeQuery(query);
			if (!result.first())//Table does not exist
			{
				result.close();
				Log.d("Creating table...");
				query = "CREATE TABLE `" + parent.conf.dbTable + "` (`name` VARCHAR(255) NOT NULL,`lives` INT NOT NULL,PRIMARY KEY (`name`))";
				try {
					db.execute(query);
				}
				catch (SQLException e) {
					Log.e("Error creating MySQL table.");
					Log.e("QUERY: " + query);
					e.printStackTrace();
				}
			}
		}
		catch (SQLException e) {
			Log.e("Error checking MySQL table.");
			Log.e("QUERY: " + query);
			e.printStackTrace();
		}
		if (result != null) {
			try {
				result.close();
			}
			catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
		Log.d("Done starting MySQL!");
	}

	public void close() {
		Log.d("Closing MySQL connection...");
		if (db != null) {
			try {
				db.close();
			}
			catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
		if (con != null) {
			try {
				con.close();
			}
			catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
	}

	public boolean isActive() {
		return con != null && db != null;
	}//TODO: Update this to have similar behavior to checkConnection? Need to check where this is used.

	public void checkConnection() {
		boolean thereIsntAConnection = false;

		try {
			if ((db == null || db.isClosed()) || (con == null || con.isClosed())) {
				thereIsntAConnection = true;
			}
		}
		catch (SQLException ex) {
			Log.d("Error checking MySQL connection state.");
			ex.printStackTrace();
			thereIsntAConnection = true;
		}

		if (thereIsntAConnection) {
			Log.e("Connection to MySQL server lost, attempting to reconnect...");
			open();
		}
	}

	public void load() {
	}//Nothing to do.

	public void save() {
	}//Nothing to do.

	public boolean addPlayer(String player, int lives) {
		checkConnection();

		String query = "INSERT INTO `" + parent.conf.dbTable + "` (`name`,`lives`) VALUES ('" + player + "', '" + lives + "');";
		try {
			db.execute(query);
			return true;
		}
		catch (SQLException e) {
			Log.d("Error adding new player to database.");
			Log.d("QUERY: \"" + query + "\"");
			e.printStackTrace();
			return false;
		}
	}

	public boolean exists(String player) {
		checkConnection();

		String query = "SELECT `lives` FROM `" + parent.conf.dbTable + "` WHERE `name`='" + player + "' LIMIT 1";
		ResultSet result = null;
		try {
			result = db.executeQuery(query);
			boolean ret = result.first();
			result.close();
			return ret;
		}
		catch (SQLException e) {
			Log.d("Error checking if player exists in the database.");
			Log.d("QUERY: \"" + query + "\"");
			e.printStackTrace();
			if (result != null) {
				try {
					result.close();
				}
				catch (SQLException ex) {
					ex.printStackTrace();
				}
			}
			return false;
		}
	}

	public int get(String player) {
		checkConnection();

		String query = "SELECT `lives` FROM `" + parent.conf.dbTable + "` WHERE `name`='" + player + "' LIMIT 1";
		ResultSet result = null;
		int ret = -1;
		try {
			result = db.executeQuery(query);
			if (result.first())//If the player exists at all
			{
				ret = result.getInt(1);
			}
			result.close();
		}
		catch (SQLException e) {
			Log.d("Error checking player's number of lives.");
			Log.d("QUERY: \"" + query + "\"");
			e.printStackTrace();
			if (result != null) {
				try {
					result.close();
				}
				catch (SQLException ex) {
					ex.printStackTrace();
				}
			}
			ret = -1;
		}

		if (ret <= 0 && parent.conf.infiniteLives) {
			ret = 1;
		}

		return ret;
	}

	public boolean set(String player, int lives) {
		checkConnection();

		String query = "UPDATE `" + parent.conf.dbTable + "` SET `lives` = '" + lives + "' WHERE `name` = '" + player + "' LIMIT 1";
		boolean ret = false;
		try {
			ret = db.execute(query);
		}
		catch (SQLException e) {
			Log.d("Error setting player's lives.");
			Log.d("QUERY: \"" + query + "\"");
			e.printStackTrace();
			ret = false;
		}
		parent.onNumLivesChange(player);
		return ret;
	}

	public boolean give(String player, int lives)//MySQL optimized give function
	{
		checkConnection();

		String query = "UPDATE `" + parent.conf.dbTable + "` SET `lives` = `lives`+'" + lives + "' WHERE `name` = '" + player + "' LIMIT 1";
		boolean ret = false;
		try {
			ret = db.execute(query);
		}
		catch (SQLException e) {
			Log.d("Error incrementing/decrementing player's lives.");
			Log.d("QUERY: \"" + query + "\"");
			e.printStackTrace();
			ret = false;
		}
		parent.onNumLivesChange(player);
		return ret;
	}
}
