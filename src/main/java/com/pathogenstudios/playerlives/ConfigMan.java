package com.pathogenstudios.playerlives;

import org.bukkit.util.config.Configuration;

import com.pathogenstudios.generic.*;

public class ConfigMan
{
 //Internals:
 PlayerLives parent;
 private Configuration conf;
 
 //Config Variables:
 public double lifeCost;//Cost to buy a new life (Economy)
 public double deathPunishmentCost;//Amount of money lost on death (Economy)
 public double minBalanceForPunishment;//Minimum balance needed to be charged for death (Economy)
 public int defaultLives;//Default number of lives a new player receives
 public boolean verbose;//Verbose logging mode
 public boolean infiniteLives;//Infinite lives mode - disabled buying mechanic.
 public int flatFileAutosaveInterval;//How many seconds to wait between flatfile database saves. 0 to disable autosave.
 
 public String dbDriver;//Database driver {flatfile,mysql,sqlite}
 public String dbHost;
 public String dbUser;
 public String dbPassword;
 public int dbPort;
 public String dbDatabase;
 public String dbTable;
 
 public ConfigMan(PlayerLives parent)
 {
  this.parent = parent;
  conf = parent.getConfiguration();
  load();
 }
 
 public void load()
 {
  //Get configuration
  Log.m("Loading config...");
  lifeCost = conf.getDouble("lifeCost",100.0);
  deathPunishmentCost = conf.getDouble("deathPunishmentCost",0.0);
  minBalanceForPunishment = conf.getDouble("minBalanceForPunishment",100.0);
  defaultLives = conf.getInt("defaultLives",3);
  verbose = conf.getBoolean("verbose",false);
  infiniteLives = conf.getBoolean("infiniteLives",false);
  flatFileAutosaveInterval = conf.getInt("flatFileAutosaveInterval",0);
  
  dbDriver = conf.getString("dbDriver","flatfile");
  dbHost = conf.getString("dbHost","localhost");
  dbUser = conf.getString("dbUser","root");
  dbPassword = conf.getString("dbPassword","");
  dbPort = conf.getInt("dbPort",3306);
  dbDatabase = conf.getString("dbDatabase","playerlives");
  dbTable = conf.getString("dbTable","playerlives");
  
  save();//Save immediately afterwards in case the user has upgraded - this adds the new values.
 }
 
 public void save()
 {
  Log.m("Saving config...");
  conf.load();
  conf.setProperty("lifeCost",lifeCost);
  conf.setProperty("deathPunishmentCost",deathPunishmentCost);
  conf.setProperty("minBalanceForPunishment",minBalanceForPunishment);
  conf.setProperty("defaultLives",defaultLives);
  conf.setProperty("verbose",verbose);
  conf.setProperty("infiniteLives",infiniteLives);
  conf.setProperty("flatFileAutosaveInterval",flatFileAutosaveInterval);
  
  conf.setProperty("dbDriver",dbDriver);
  conf.setProperty("dbHost",dbHost);
  conf.setProperty("dbUser",dbUser);
  conf.setProperty("dbPassword",dbPassword);
  conf.setProperty("dbPort",dbPort);
  conf.setProperty("dbDatabase",dbDatabase);
  conf.setProperty("dbTable",dbTable);
  
  conf.save();
 }
}
