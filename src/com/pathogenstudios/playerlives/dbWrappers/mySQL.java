package com.pathogenstudios.playerlives.dbWrappers;

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
  
  //Make MySQL connection:
  try
  {
   new com.mysql.jdbc.Driver();
   con = DriverManager.getConnection("jdbc:mysql://"+parent.conf.dbHost+":"+parent.conf.dbPort+"/"+parent.conf.dbDatabase,parent.conf.dbUser,parent.conf.dbPassword);
   db = con.createStatement();
  }
  catch (SQLException e)
  {
   System.err.println("["+playerLives.pluginName+"] Error initialzing MySQL.");
   e.printStackTrace();
   if (db!=null) {try{db.close();}catch (SQLException ex) {}}
   if (con!=null) {try{con.close();}catch (SQLException ex) {}}
   con = null;
   db = null;
   return;
  }
  
  //Check if we need to init the table:
  String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = '"+parent.conf.dbDatabase+"' AND table_name = '"+parent.conf.dbTable+"'";
  ResultSet result = null;
  try
  {
   result = db.executeQuery(query);
   if (!result.first())//Table does not exist
   {
    result.close();
    query = "CREATE TABLE `"+parent.conf.dbTable+"` (`name` VARCHAR(255) NOT NULL,`lives` INT NOT NULL,PRIMARY KEY (`name`))";
    try {db.execute(query);}
    catch (SQLException e)
    {
     System.err.println("["+playerLives.pluginName+"] Error creating MySQL table.");
     System.err.println("["+playerLives.pluginName+"] QUERY: "+query);
     e.printStackTrace();
    }
   }
  }
  catch (SQLException e)
  {
   System.err.println("["+playerLives.pluginName+"] Error checking MySQL table.");
   System.err.println("["+playerLives.pluginName+"] QUERY: "+query);
   e.printStackTrace();
  }
  if (result!=null) {try{result.close();}catch (SQLException ex) {ex.printStackTrace();}}
 }
 
 public void close()
 {
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
   System.err.println("["+playerLives.pluginName+"] Error adding new player to database.");
   System.err.println("["+playerLives.pluginName+"] QUERY: \""+query+"\"");
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
   System.err.println("["+playerLives.pluginName+"] Error checking if player exists in the database.");
   System.err.println("["+playerLives.pluginName+"] QUERY: \""+query+"\"");
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
    ret = result.getInt(0);
    if (ret<=0 && parent.conf.infiniteLives) {ret = 1;}
   }
   result.close();
   return ret;
  }
  catch (SQLException e)
  {
   System.err.println("["+playerLives.pluginName+"] Error checking player's number of lives.");
   System.err.println("["+playerLives.pluginName+"] QUERY: \""+query+"\"");
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
   System.err.println("["+playerLives.pluginName+"] Error setting player's lives.");
   System.err.println("["+playerLives.pluginName+"] QUERY: \""+query+"\"");
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
   System.err.println("["+playerLives.pluginName+"] Error incrementing/decrementing player's lives.");
   System.err.println("["+playerLives.pluginName+"] QUERY: \""+query+"\"");
   e.printStackTrace();
   return false;
  }
 }
}
