package com.pathogenstudios.playerlives.dbWrappers;

import com.pathogenstudios.playerlives.PropertyHandler;
import com.pathogenstudios.playerlives.dbWrapper;
import com.pathogenstudios.playerlives.playerLives;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class flatfile extends dbWrapper
{
 private PropertyHandler livesDb=null;
 private HashMap<String,Integer> livesList;
 
 public flatfile(playerLives parent)
 {
  super(parent);
  load();
 }
 
 public void load()
 {
  livesDb = new PropertyHandler(parent.getDataFolder()+File.separator+"livesDb.properties");//Created after the dir is made if it does not exist yet.
  
  if (parent.conf.verbose) {System.out.println("["+playerLives.pluginName+"] Load flatfile lives database...");}
  livesList = new HashMap<String,Integer>();
  try
  {
   ArrayList<String> livesPlayerList = livesDb.getKeys();
   for(int i = 0;i<livesPlayerList.size();i++)
   {
    String key = livesPlayerList.get(i);
    int val = livesDb.getInt(key);
    if (parent.conf.verbose) {System.out.println(key+" = "+val);}
    livesList.put(key,val);
   }
  }
  catch (Exception e)
  {
   System.err.println("["+playerLives.pluginName+"] COULD NOT LOAD LIVES FLATFILE DATABASE!");
   e.printStackTrace();
  }
  livesDb.save();
 }
 
 public void save()
 {
  if (parent.conf.verbose) {System.out.println("["+playerLives.pluginName+"] Saving lives db...");}
  livesDb.load();
  Iterator<String> itr = livesList.keySet().iterator();
  while(itr.hasNext())
  {
   String key=itr.next();
   livesDb.setInt(key,livesList.get(key));
  }
  livesDb.save();
 }
 
 public boolean addPlayer(String player,int lives) {livesList.put(player,lives);return true;}
 public boolean exists(String player) {return livesList.containsKey(player);}
 
 public int get(String player)
  {
  if (exists(player))
  {
   int ret = livesList.get(player);
   if (ret<=0 && parent.conf.infiniteLives)
   {return 1;}//Never return <0 if infiniteLives is on
   else
   {return ret;}
  }
  else {return -1;}
 }
 
 public boolean set(String player,int lives)
 {
  if (!exists(player)) {return false;}
  if (lives<0) {lives = 0;}
  livesList.put(player,lives);
  return true;
 }
}
