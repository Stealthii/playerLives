/*
playerLives
Created by Pathogen David
http://www.pathogenstudios.com/
*/
package com.pathogenstudios.playerlives;

import java.util.HashMap;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import com.pathogenstudios.playerlives.dbWrappers.*;
import com.pathogenstudios.playerlives.econWrappers.*;


//Method for temporarily storing inventory data
class inventoryStore
{
 private ItemStack[] contents;
 private ItemStack helmet;
 private ItemStack chestplate;
 private ItemStack leggings;
 private ItemStack boots;
 private boolean isRespawned = false;//Used by onMove to allow the inventory to come back...
 private playerLives parent;
 
 public inventoryStore(playerLives parent) {this.parent = parent;}
 
 public void copy(PlayerInventory inv)
 {
  if (parent.conf.verbose) {System.out.println("["+playerLives.pluginName+"] Saving inventory...");}
  contents = inv.getContents().clone();
  helmet = inv.getHelmet();
  chestplate = inv.getChestplate();
  leggings = inv.getLeggings();
  boots = inv.getBoots();
 }
 
 public void paste(PlayerInventory inv)
 {
  if (parent.conf.verbose) {System.out.println("["+playerLives.pluginName+"] Restoring inventory...");}
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
 public static final String pluginName = "pathogenPlayerLives";
 
 //Listeners
 private plPlayerListener playerListener = new plPlayerListener(this);
 private plEntityListener entityListener = new plEntityListener(this);
 private plServerListener serverListener = new plServerListener(this);
 
 //Plugins
 private PluginManager pluginMan;
 private PermissionHandler permissionsPlugin = null;
 private econWrapper econ;
 private dbWrapper db;
 
 //Internal
 private HashMap<Player,inventoryStore> invStore = new HashMap<Player,inventoryStore>();
 
 //Configuration:
 public configMan conf;
 
 //Constructor/Destrctor:
 public void onEnable()
 {
  System.out.println("["+pluginName+"] Loading Pathogen playerLives...");
  
  pluginMan = getServer().getPluginManager();
  econ = new econWrapper();//Dummy wrapper until a compatible econ plugin is detected.
  
  //Make config folder if necessary...
  getDataFolder().mkdir();//boolean newInstall = 
  
  //Config Loading and such
  conf = new configMan(this);
  conf.load();//This will naturally populate with defaults when necessary.
  
  //Load Lives Db
  if (conf.dbDriver.toLowerCase()=="mysql") {db = new mySQL(this);}
  else {db = new flatfile(this);}
  
  //Register Events
  pluginMan.registerEvent(Event.Type.ENTITY_DAMAGE,entityListener,Event.Priority.Normal,this);
  pluginMan.registerEvent(Event.Type.ENTITY_DEATH,entityListener,Event.Priority.Normal,this);
  pluginMan.registerEvent(Event.Type.PLAYER_JOIN,playerListener,Event.Priority.Normal,this);
  pluginMan.registerEvent(Event.Type.PLAYER_MOVE,playerListener,Event.Priority.Normal,this);
  pluginMan.registerEvent(Event.Type.PLAYER_RESPAWN,playerListener,Event.Priority.Normal,this);
  pluginMan.registerEvent(Event.Type.PLUGIN_ENABLE,serverListener,Event.Priority.Monitor,this);
  
  System.out.println("["+pluginName+"] Done loading Pathogen playerLives.");
 }
 
 public void onDisable()
 {
  System.out.println("["+pluginName+"] Unloading Pathogen playerLives...");
  db.close();
  if (conf.verbose) {System.out.println("["+pluginName+"] I'm not even angry...");}
 }
 
 //Event Callbacks:
 public void onDamage(EntityDamageEvent e)
 {
  Player player = (Player)e.getEntity();
  String playerName = player.getName();
  
  if (!checkPermission(player,"canuse")) {player.sendMessage(accessDenied);return;}
  if (player.getHealth()<1) {return;}//Player is already dead
  
  //If they will be dying at the end of this event, store their stuff!
  if (player.getHealth()-e.getDamage()<1)
  {
   if (db.get(playerName)>=1)
   {
    if (conf.verbose) {System.out.println("["+pluginName+"] Player "+playerName+" is gonna die! Save their stuff!");}
    inventoryStore newStore = new inventoryStore(this);
    newStore.copy(player.getInventory());
    invStore.put(player,newStore);
    //Will subtract a life during the "onDeath" event.
   }
   else
   {
    if (conf.verbose) {System.out.println("["+pluginName+"] Player "+playerName+" is gonna die! They are out of lives so their stuff will not be saved.");}
   }
  }
 }
 
 public void onDeath(EntityDeathEvent e)
 {
  Player player = (Player)e.getEntity();
  String playerName = player.getName();
  
  if (!checkPermission(player,"canuse")) {player.sendMessage(accessDenied);return;}
  
  if (db.get(playerName)>=1)
  {
   //TODO: Fix issue with drops getting surpressed when killed with /kill-type admin commands.
   if (conf.verbose) {System.out.println("["+pluginName+"] Supressing drops for "+playerName);}
   for(int i=0;i<e.getDrops().size();i++) {e.getDrops().remove(i);i--;}
   if (!conf.infiniteLives) {db.take(playerName,1);}//Do the subtraction of a life.
  }
  else
  {
   if (conf.verbose) {System.out.println("["+pluginName+"] Player "+playerName+" is out of lives, drops will not be surpressed.");}
  }
 }
 
 public void onRespawn(PlayerRespawnEvent e)
 {
  //We can not give them back their stuff yet, just mark the entry as respawned and handle it in onMove...
  //We have to check the respawn because the entity can keep falling after death.
  Player player = e.getPlayer();
  
  if (!checkPermission(player,"canuse")) {player.sendMessage(accessDenied);return;}
  
  if (invStore.containsKey(player))
  {
   invStore.get(player).setIsRespawned(true);
   int lives = db.get(player.getName());
   if (conf.infiniteLives)
   {}//Don't display anything. pathogenPlayerLives is now a static game mechanic.
   else if (lives==1)
   {player.sendMessage("Welcome back! You only have one more life! Be careful!");}
   else if (lives==0)
   {player.sendMessage("Welcome back! You have no lives left! Be careful!");}
   else
   {player.sendMessage("Welcome back! You have "+lives+" lives left.");}
  }
  else
  {
   if (!conf.infiniteLives) {player.sendMessage("Welcome back! It looks like you were out of lives.");}//Ideally this does not happen, but lets at least try to avoid confusing the player.
   player.sendMessage("Your stuff did not come back with you.");
   player.sendMessage("However, it might still be where you died!");
  }
  
  //Death iConomy punishment:
  if (econ.isEnabled() && conf.deathPunishmentCost>0)
  {
   double oldBal = econ.getBalance(player);
   if (oldBal>=conf.minBalanceForPunishment)
   {
    double toTake = conf.deathPunishmentCost;
    if (oldBal-toTake<0) {toTake = oldBal;}//Don't know if iConomy allows debt, but we'll prevent it.
    econ.subBalance(player,toTake);
    player.sendMessage("You also lost "+econ.format(toTake)+" leaving you with "+econ.format(econ.getBalance(player))+".");
   }
  }
 }
 
 public void onMove(PlayerMoveEvent e)
 {
  Player player = e.getPlayer();
  
  //Give them their stuff back (if they just respawned and have logged stuff)
  if (invStore.containsKey(player) && invStore.get(player).isRespawned())
  {
   if (!checkPermission(player,"canuse")) {player.sendMessage(accessDenied);return;}
   invStore.get(player).paste(player.getInventory());
   invStore.remove(player);
  }
 }
 
 public void onJoin(PlayerJoinEvent e)
 {
  if (conf.verbose) {System.out.println("["+pluginName+"] Player joined! '"+e.getPlayer().getName()+"'");}
  Player player = e.getPlayer();
  if (db.exists(player))
  {
   if (conf.verbose) {System.out.println("["+pluginName+"] Recognized player! They have "+db.get(player.getName())+" lives!");}
  }
  else
  {
   if (conf.verbose) {System.out.println("["+pluginName+"] Unrecognized player! Giving them "+conf.defaultLives+" lives!");}
   db.addPlayer(player,conf.defaultLives);
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
   
   targetName = searchPlayer(targetName);
   if (targetName!=playerName)
   {
    messagePrefix = targetName+" has";
    if (!checkPermission(player,"checkothers")) {player.sendMessage(accessDenied);return true;}
   }
   else if (!checkPermission(player,"checkself")) {player.sendMessage(accessDenied);return true;}
   
   
   if (db.exists(targetName))
   {
    if (conf.infiniteLives)
    {sender.sendMessage(messagePrefix+" infinite lives.");}
    else
    {sender.sendMessage(messagePrefix+" "+db.get(targetName)+" lives.");}
   }
   else {sender.sendMessage("No player named "+targetName+".");}
   return true;
  }
  else if ((commandName.compareToIgnoreCase("givelives") == 0 || commandName.compareToIgnoreCase("takelives") == 0 || commandName.compareToIgnoreCase("setlives") == 0))//Give lives to someone /givelives [playername] [lives]
  {
   if (!checkPermission(player,"change")) {player.sendMessage(accessDenied);return true;}
   String targetName = playerName;
   Integer count = 1;
   if (commandName.compareToIgnoreCase("setlives") == 0) {count = conf.defaultLives;}
   String messagePrefix = "You now have";
   
   for (int i = 0;i<args.length;i++)
   {
    if (i>1) {break;}//We only have two possible arguments...
    try {count = Integer.parseInt(args[i]);}//Is it a number? If so, it must be the count...
    catch (Exception e) {targetName = args[i];}//If not, it is probably the player name
   }
   
   targetName = searchPlayer(targetName);
   if (commandName.compareToIgnoreCase("takelives") == 0) {count=-count;}
   if (targetName!=playerName) {messagePrefix=targetName+" now has";}
   
   if (db.exists(targetName))
   {
    if (commandName.compareToIgnoreCase("setlives") == 0)
    {db.set(targetName,count);}
    else
    {db.give(targetName,count);}
    
    sender.sendMessage(messagePrefix+" "+db.get(targetName)+" lives.");
   }
   else {sender.sendMessage("No player named "+targetName+".");}
   return true;
  }
  else if (commandName.compareToIgnoreCase("buylives") == 0)
  {
   if (!checkPermission(player,"buy")) {player.sendMessage(accessDenied);return true;}
   String targetName = playerName;
   Integer count = 1;
   
   if (!econ.isEnabled())
   {
    sender.sendMessage("Server needs an economy to enable buying lives!");
    return true;// << Technically it was handled...
   }
   
   if (args.length>=1)
   {
    try {count = Integer.parseInt(args[0]);}//Is it a number? If so, it must be the count...
    catch (Exception e)
    {
     sender.sendMessage("Expected a number for count.");
     return false;// << To avoid unexpected transactions, bail out. Use false so Bukkit will remind them of command usage.
    }
    
    if (count<1)
    {
     sender.sendMessage("Invalid count.");
     return false;
    }
   }
   
   if (db.exists(targetName))
   {
    //Holdings account = iConomy.getAccount(targetName).getHoldings();
    if (econ.getBalance(targetName)<conf.lifeCost*count)
    {
     sender.sendMessage("You do not have enough"+econ.getCurrency(true));
     sender.sendMessage("You need "+econ.format(conf.lifeCost*count)+(count>1?"to buy "+count+" lives.":" to buy a life."));
     return true;
    }
    
    econ.subBalance(targetName,conf.lifeCost*count);
    db.give(targetName,count);
    int numLives = db.get(targetName);
    sender.sendMessage("You now have "+numLives+(numLives==1?" life":" lives")+" and "+econ.format(econ.getBalance(targetName))+".");
   }
   else
   {
    sender.sendMessage("No player named "+targetName+".");
    System.err.print("["+pluginName+"] Player '"+targetName+"' does not exist!");
   }
   return true;
  }
  
  return false;
 }
 
 //Util
 boolean checkPermission(Player player,String node)
 {
  node = node.toLowerCase();//Just in case...
  if (permissionsPlugin!=null)
  {return permissionsPlugin.has(player,"playerlives."+node);}
  else
  {
   if (node=="canuse" || node=="checkself" || node=="buy")
   {return true;}//Things normal people can use.
   else//checkothers, change
   {return player.isOp();}//If not, assume op-only.
  }
 }
 final static String accessDenied = "You do not have access to that command.";
 
 public String searchPlayer(String targetName)
 {
  if (!db.exists(targetName))//Try to resolve the name to someone on the server...
  {
   List<Player> matches = getServer().matchPlayer(targetName);
   if (matches.size()>=1) {return matches.get(0).getName();}
  }
  return targetName;
 }
 
 //External Plugin Support
 public void onPluginEnable(PluginEnableEvent e)
 {
  //Economy Plugins
  if (!econ.isEnabled())
  {
   try
   {
    com.iConomy.iConomy iConomy5Plugin = (com.iConomy.iConomy)pluginMan.getPlugin("iConomy");
    if (iConomy5Plugin!=null && iConomy5Plugin.isEnabled())
    {
     System.out.println("["+playerLives.pluginName+"] Successfully linked with iConomy 5");
     econ = new iConomy5();
    }
   }
   catch (NoClassDefFoundError ex)
   {
    if (conf.verbose) {System.out.println("["+pluginName+"] Failed to link with iConomy. Trying iConomy 4...");}
    com.nijiko.coelho.iConomy.iConomy iConomy4Plugin = (com.nijiko.coelho.iConomy.iConomy)pluginMan.getPlugin("iConomy");
    
    if (iConomy4Plugin!=null && iConomy4Plugin.isEnabled())
    {
     System.out.println("["+playerLives.pluginName+"] Successfully linked with iConomy 4");
     econ = new iConomy4();
    }
    else if (conf.verbose) {System.err.println("["+pluginName+"] Failed to link with iConomy 4 and iConomy 5!");}
   }
  }
  
  //Permissions Plugins
  if (permissionsPlugin==null)
  {
   Permissions tempPerm = (Permissions)pluginMan.getPlugin("Permissions");
   
   if (tempPerm!=null)
   {
    permissionsPlugin = tempPerm.getHandler();
    System.out.println("["+pluginName+"] Successfully linked with Permissions");
   }
   else
   {permissionsPlugin = null;}
  }
 }
}
