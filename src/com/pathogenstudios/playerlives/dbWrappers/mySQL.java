package com.pathogenstudios.playerlives.dbWrappers;

import com.pathogenstudios.generic.Log;
import com.pathogenstudios.playerlives.dbWrapper;
import com.pathogenstudios.playerlives.playerLives;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;

public class mySQL extends dbWrapper
{
 private Connection con = null;
 private Statement db = null;
 public mySQL(playerLives parent)
 {
  super(parent);
  
  Log.d("Starting MySQL Database Engine...");
  
  //Make MySQL connection:
  try
  {
   new com.mysql.jdbc.Driver();
   con = DriverManager.getConnection("jdbc:mysql://"+parent.conf.dbHost+":"+parent.conf.dbPort+"/"+parent.conf.dbDatabase,parent.conf.dbUser,parent.conf.dbPassword);
   db = con.createStatement();
  }
  catch (SQLException e)
  {
   Log.e("Error initialzing MySQL.");
   e.printStackTrace();
   if (db!=null) {try{db.close();}catch (SQLException ex) {}}
   if (con!=null) {try{con.close();}catch (SQLException ex) {}}
   con = null;
   db = null;
   return;
  }
  
  //Check if we need to init the table:
  Log.d("Checking if table needs to be made...");
  String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = '"+parent.conf.dbDatabase+"' AND table_name = '"+parent.conf.dbTable+"'";
  ResultSet result = null;
  try
  {
   result = db.executeQuery(query);
   if (!result.first())//Table does not exist
   {
    result.close();
    Log.d("Creating table...");
    query = "CREATE TABLE `"+parent.conf.dbTable+"` (`name` VARCHAR(255) NOT NULL,`lives` INT NOT NULL,PRIMARY KEY (`name`))";
    try {db.execute(query);}
    catch (SQLException e)
    {
     Log.e("Error creating MySQL table.");
     Log.e("QUERY: "+query);
     e.printStackTrace();
    }
   }
  }
  catch (SQLException e)
  {
   Log.e("Error checking MySQL table.");
   Log.e("QUERY: "+query);
   e.printStackTrace();
  }
  if (result!=null) {try{result.close();}catch (SQLException ex) {ex.printStackTrace();}}
  Log.d("Done starting MySQL!");
 }
 public boolean isActive() {return con!=null && db!=null;}
 
 public void close()
 {
  Log.d("Closing MySQL connection...");
  if (db!=null) {try{db.close();}catch (SQLException ex) {ex.printStackTrace();}}
  if (db!=null) {try{con.close();}catch (SQLException ex) {ex.printStackTrace();}}
 }
 
 public void load() {}//Nothing to do.
 public void save() {}//Nothing to do.
 
 public boolean addPlayer(String player, int lives)
 {
  String query = "INSERT INTO `"+parent.conf.dbTable+"` (`name`,`lives`) VALUES ('"+player+"', '"+lives+"');";
  try
  {
   db.execute(query);
   return true;
  }
  catch (SQLException e)
  {
   Log.d("Error adding new player to database.");
   Log.d("QUERY: \""+query+"\"");
   e.printStackTrace();
   return false;
  }
 }
 
 public boolean exists(String player)
 {
  String query = "SELECT `lives` FROM `"+parent.conf.dbTable+"` WHERE `name`='"+player+"' LIMIT 1";
  ResultSet result = null;
  try
  {
   result = db.executeQuery(query);
   boolean ret = result.first();
   result.close();
   return ret;
  }
  catch (SQLException e)
  {
   Log.d("Error checking if player exists in the database.");
   Log.d("QUERY: \""+query+"\"");
   e.printStackTrace();
   if (result!=null) {try{result.close();}catch (SQLException ex) {ex.printStackTrace();}}
   return false;
  }
 }
 
 public int get(String player)
 {
  String query = "SELECT `lives` FROM `"+parent.conf.dbTable+"` WHERE `name`='"+player+"' LIMIT 1";
  ResultSet result = null;
  try
  {
   result = db.executeQuery(query);
   int ret = -1;
   if (result.first())//If the player exists at all
   {
    ret = result.getInt(1);
    if (ret<=0 && parent.conf.infiniteLives) {ret = 1;}
   }
   result.close();
   return ret;
  }
  catch (SQLException e)
  {
   Log.d("Error checking player's number of lives.");
   Log.d("QUERY: \""+query+"\"");
   e.printStackTrace();
   if (result!=null) {try{result.close();}catch (SQLException ex) {ex.printStackTrace();}}
   return -1;
  }
 }
 
 public boolean set(String player, int lives)
 {
  String query = "UPDATE `"+parent.conf.dbTable+"` SET `lives` = '"+lives+"' WHERE `name` = '"+player+"' LIMIT 1";
  try {return db.execute(query);}
  catch (SQLException e)
  {
   Log.d("Error setting player's lives.");
   Log.d("QUERY: \""+query+"\"");
   e.printStackTrace();
   return false;
  }
 }
 
 public boolean give(String player, int lives)//MySQL optimized give function
 {
  String query = "UPDATE `"+parent.conf.dbTable+"` SET `lives` = `lives`+'"+lives+"' WHERE `name` = '"+player+"' LIMIT 1";
  try {return db.execute(query);}
  catch (SQLException e)
  {
   Log.d("Error incrementing/decrementing player's lives.");
   Log.d("QUERY: \""+query+"\"");
   e.printStackTrace();
   return false;
  }
 }
}
