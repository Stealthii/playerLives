/*
playerLives
Created by Pathogen David
http://www.pathogenstudios.com/

Special thanks to msm595 for his NoDrop plugin as it saved me a lot of time with figuring out stuff in Bukkit.
(And it is loosly based on it.)
https://github.com/msm595/NoDrop

TODO: Add permissions support
TODO: Add buy support!
*/
package com.pathogenstudios.playerlives;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

//Method for temporarily storing inventory data
class inventoryStore
{
 private ItemStack[] contents;
 private ItemStack helmet;
 private ItemStack chestplate;
 private ItemStack leggings;
 private ItemStack boots;
 private boolean isRespawned = false;//Used by onMove to allow the inventory to come back...
 
 public void copy(PlayerInventory inv)
 {
  System.out.println("Saving inventory...");
  contents = inv.getContents().clone();
  helmet = inv.getHelmet();
  chestplate = inv.getChestplate();
  leggings = inv.getLeggings();
  boots = inv.getBoots();
 }
 
 public void paste(PlayerInventory inv)
 {
  System.out.println("Restoring inventory...");
  inv.setContents(contents);
  if (helmet!=null && helmet.getTypeId()!=0)         {inv.setHelmet(helmet);}
  if (chestplate!=null && chestplate.getTypeId()!=0) {inv.setChestplate(chestplate);}
  if (leggings!=null && leggings.getTypeId()!=0)     {inv.setLeggings(leggings);}
  if (boots!=null && boots.getTypeId()!=0)           {inv.setBoots(boots);}
 }
 
 public void setIsRespawned(boolean isRespawned) {this.isRespawned = isRespawned;}
 public boolean isRespawned() {return isRespawned;}
}

//Main class
public class playerLives extends JavaPlugin
{ 
 plPlayerListener playerListener = new plPlayerListener(this);
 plEntityListener entityListener = new plEntityListener(this);
 PluginManager pluginMan;
 private Configuration conf;
 //private Configuration livesDb;
 private PropertyHandler livesDb;
 private HashMap<String,Integer> livesList;
 private int defaultLives;
 
 private HashMap<Player,inventoryStore> invStore = new HashMap<Player,inventoryStore>();
 
 //Constructor/Destrctor:
 public void onEnable()
 {
  System.out.println("Loading Pathogen playerLives...");
  
  conf = this.getConfiguration();
  livesDb = new PropertyHandler(getDataFolder().getAbsolutePath()+File.separator+"livesDb.properties");
  //new Configuration(new File(getDataFolder().getAbsolutePath()+File.separator+"livesDb.yml"));
  pluginMan = getServer().getPluginManager();
  
  //Create initial configuration
  if (getDataFolder().mkdir())
  {
   conf.load();
   conf.setProperty("lifeCost",100);//iConomy setting... (UNIMPLEMENTED)
   conf.setProperty("defaultLives",3);
   conf.save();
   
   livesDb.load();
   livesDb.save();
  }
  
  //Get configuration
  System.out.println("Load config...");
  defaultLives = conf.getInt("defaultLives",3);
  
  //Store all lives in memory...
  System.out.println("Load lives database...");
  livesList = new HashMap<String,Integer>();
  try
  {
   ArrayList<String> livesPlayerList = livesDb.getKeys();
   for(int i = 0;i<livesPlayerList.size();i++)
   {
    String key = livesPlayerList.get(i);
    int val = livesDb.getInt(key);
    System.out.println(key+" = "+val);
    livesList.put(key,val);
    subtractLives(key,1);
   }
  }
  catch (Exception e)
  {
   System.err.println("COULD NOT LOAD LIVES DATABASE!");
   e.printStackTrace();
  }
  
  pluginMan.registerEvent(Event.Type.ENTITY_DAMAGE,entityListener,Event.Priority.Normal,this);
  pluginMan.registerEvent(Event.Type.ENTITY_DEATH,entityListener,Event.Priority.Normal,this);
  pluginMan.registerEvent(Event.Type.PLAYER_JOIN,playerListener,Event.Priority.Normal,this);
  pluginMan.registerEvent(Event.Type.PLAYER_MOVE,playerListener,Event.Priority.Normal,this);
  pluginMan.registerEvent(Event.Type.PLAYER_RESPAWN,playerListener,Event.Priority.Normal,this);
  
  System.out.println("Done loading Pathogen playerLives.");
 }
 
 public void onDisable()
 {
  System.out.println("Unloading Pathogen playerLives...");
  
  System.out.println("Saving lives db one last time...");
  livesDb.load();
  Iterator<String> itr = livesList.keySet().iterator();
  while(itr.hasNext())
  {
   String key=itr.next();
   livesDb.setInt(key,livesList.get(key));
  }
  livesDb.save();
 }
 
 //Event Callbacks:
 public void onDamage(EntityDamageEvent e)
 {
  Player player = (Player)e.getEntity();
  String playerName = player.getName();
  
  if (player.getHealth()<1) {return;}//Player is already dead
  
  //If they will be dying at the end of this event, store their stuff!
  if (player.getHealth()-e.getDamage()<1)
  {
   if (getLives(playerName)>=1)
   {
    System.out.println("Player "+playerName+" is gonna die! Save their stuff!");
    inventoryStore newStore = new inventoryStore();
    newStore.copy(player.getInventory());
    invStore.put(player,newStore);
    //Will subtract a life during the "onDeath" event.
   }
   else
   {
    System.out.println("Player "+playerName+" is gonna die! They are out of lives so their stuff will not be saved.");
   }
  }
 }
 
 public void onDeath(EntityDeathEvent e)
 {
  Player player = (Player)e.getEntity();
  String playerName = player.getName();
  if (getLives(playerName)>=1)
  {
   System.out.println("Supressing drops for "+playerName);
   for(int i=0;i<e.getDrops().size();i++) {e.getDrops().remove(i);i--;}
   subtractLives(playerName,1);//Do the subtraction of a life.
  }
  else
  {
   System.out.println("Player "+playerName+" is out of lives, drops will not be surpressed.");
  }
 }
 
 public void onRespawn(PlayerRespawnEvent e)
 {
  //We can not give them back their stuff yet, just mark the entry as respawned and handle it in onMove...
  //We have to check the respawn because the entity can keep falling after death.
  Player player = e.getPlayer();
  if (invStore.containsKey(player))
  {
   invStore.get(player).setIsRespawned(true);
   int lives = getLives(player.getName());
   if (lives==1)
   {player.sendMessage("Welcome back! You only have one more life! Be careful!");}
   else if (lives==0)
   {player.sendMessage("Welcome back! You have no lives left! Be careful!");}
   else
   {player.sendMessage("Welcome back! You have "+lives+" lives left.");}
  }
  else
  {
   player.sendMessage("Welcome back! It looks like you were out of lives.");
   player.sendMessage("Your stuff did not come back with you.");
   player.sendMessage("However, it might still be where you died!");
  }
 }
 
 public void onMove(PlayerMoveEvent e)
 {
  Player player = e.getPlayer();
  
  //Give them their stuff back (if they just respawned and have logged stuff)
  if (invStore.containsKey(player) && invStore.get(player).isRespawned())
  {
   invStore.get(player).paste(player.getInventory());
   invStore.remove(player);
  }
 }
 
 public void onJoin(PlayerJoinEvent e)
 {
  System.out.println("Player joined! '"+e.getPlayer().getName()+"'");
  String playerName = e.getPlayer().getName();
  if (playerExists(playerName))
  {
   System.out.println("Recognized player! They have "+getLives(playerName)+" lives!");
  }
  else
  {
   System.out.println("Unrecognized player! Giving them "+defaultLives+" lives!");
   livesList.put(playerName,defaultLives);
  }
 }
 
 //Handle commands
 public boolean onCommand(CommandSender sender,Command command,String label,String[] args)
 {
  Player player = null;
  String playerName = "";
  String commandName = command.getName().toLowerCase();
  if (sender instanceof Player)
  {
   player = (Player)sender;
   playerName = player.getName();
  }
  
  if (commandName.compareToIgnoreCase("lives") == 0)//Check the current number of lives for a person... /lives [playername]
  {
   String messagePrefix = "You have";
   String targetName = playerName;
   if (args.length>0)
   {
    targetName = args[0];
   }
   else if (player==null) {return false;}//Console does not have lives!
   
   if (targetName!=playerName) {messagePrefix = targetName+" has";}
   
   if (playerExists(targetName))
   {
    sender.sendMessage(messagePrefix+" "+getLives(targetName)+" lives.");
   }
   else {sender.sendMessage("No player named "+targetName+".");}
   return true;
  }
  else if ((commandName.compareToIgnoreCase("givelives") == 0 || commandName.compareToIgnoreCase("takelives") == 0 || commandName.compareToIgnoreCase("setlives") == 0) && sender.isOp())//Give lives to someone /givelives [playername] [lives]
  {
   String targetName = playerName;
   Integer count = 1;
   if (commandName.compareToIgnoreCase("setlives") == 0) {count = defaultLives;}
   String messagePrefix = "You now have";
   
   for (int i = 0;i<args.length;i++)
   {
    if (i>1) {break;}//We only have two possible arguments...
    try {count = Integer.parseInt(args[i]);}//Is it a number? If so, it must be the count...
    catch (Exception e) {targetName = args[i];}//If not, it is probably the player name
   }
   
   if (commandName.compareToIgnoreCase("takelives") == 0) {count=-count;}
   if (targetName!=playerName) {messagePrefix=targetName+" now has";}
   
   if (playerExists(targetName))
   {
    if (commandName.compareToIgnoreCase("setlives") == 0)
    {setLives(targetName,count);}
    else
    {addLives(targetName,count);}
    
    sender.sendMessage(messagePrefix+" "+livesList.get(targetName)+" lives.");
   }
   else {sender.sendMessage("No player named "+targetName+".");}
   return true;
  }
  
  return false;
 }
 
 //Lives manipulation
 boolean playerExists(String player) {return livesList.containsKey(player);}
 
 int getLives(String player)
 {
  if (playerExists(player)) {return livesList.get(player);}else{return -1;}
 }
 
 void setLives(String player,int newValue)
 {
  if (!playerExists(player)) {return;}
  if (newValue<0) {newValue = 0;}
  livesList.put(player,newValue);
  
  //TODO: Save here?
 }
 
 void addLives(String player,int lives) {setLives(player,getLives(player)+lives);}
 void subtractLives(String player,int lives) {addLives(player,-lives);}
}
