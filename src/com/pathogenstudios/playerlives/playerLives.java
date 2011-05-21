/*
playerLives
Created by Pathogen David
http://www.pathogenstudios.com/
*/
package com.pathogenstudios.playerlives;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

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
  if (parent.verbose) {System.out.println("["+playerLives.pluginName+"] Saving inventory...");}
  contents = inv.getContents().clone();
  helmet = inv.getHelmet();
  chestplate = inv.getChestplate();
  leggings = inv.getLeggings();
  boots = inv.getBoots();
 }
 
 public void paste(PlayerInventory inv)
 {
  if (parent.verbose) {System.out.println("["+playerLives.pluginName+"] Restoring inventory...");}
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
 
 //Internal
 private Configuration conf;
 private PropertyHandler livesDb;
 private HashMap<String,Integer> livesList;
 private HashMap<Player,inventoryStore> invStore = new HashMap<Player,inventoryStore>();
 
 //Configuration:
 private int defaultLives;
 private double lifeCost;
 private double deathPunishmentCost;
 private double minBalanceForPunishment;
 public boolean verbose;
 private boolean infiniteLives;
 
 //Constructor/Destrctor:
 public void onEnable()
 {
  System.out.println("["+pluginName+"] Loading Pathogen playerLives...");
  
  conf = this.getConfiguration();
  pluginMan = getServer().getPluginManager();
  econ = new econWrapper(pluginMan);
  
  //Create initial configuration
  if (getDataFolder().mkdir())
  {
   System.out.println("["+pluginName+"] Config does not exist, populating with default...");
   conf.load();
   conf.setProperty("lifeCost",100.0);//Cost to buy a new life (iConomy)
   conf.setProperty("deathPunishmentCost",0.0);//Punishment for dying (iConomy)
   conf.setProperty("minBalanceForPunishment",100.0);//Allows you to not punish poor people
   conf.setProperty("defaultLives",3);
   conf.setProperty("verbose",false);
   conf.setProperty("infiniteLives",false);
   conf.save();
   
   livesDb = new PropertyHandler(getDataFolder()+File.separator+"livesDb.properties");
   livesDb.load();
   livesDb.save();
   System.out.println("["+pluginName+"] Config populated.");
  }
  else
  {
   livesDb = new PropertyHandler(getDataFolder()+File.separator+"livesDb.properties");//Created after the dir is made if it does not exist yet.
  }
  
  //Get configuration
  System.out.println("["+pluginName+"] Load config...");
  lifeCost = conf.getDouble("lifeCost",100.0);
  deathPunishmentCost = conf.getDouble("deathPunishmentCost",0.0);
  minBalanceForPunishment = conf.getDouble("minBalanceForPunishment",100.0);
  defaultLives = conf.getInt("defaultLives",3);
  verbose = conf.getBoolean("verbose",false);
  infiniteLives = conf.getBoolean("infiniteLives",false);
  
  //Store all lives in memory...
  if (verbose) {System.out.println("["+pluginName+"] Load lives database...");}
  livesList = new HashMap<String,Integer>();
  try
  {
   ArrayList<String> livesPlayerList = livesDb.getKeys();
   for(int i = 0;i<livesPlayerList.size();i++)
   {
    String key = livesPlayerList.get(i);
    int val = livesDb.getInt(key);
    if (verbose) {System.out.println(key+" = "+val);}
    livesList.put(key,val);
   }
  }
  catch (Exception e)
  {
   System.err.println("["+pluginName+"] COULD NOT LOAD LIVES DATABASE!");
   e.printStackTrace();
  }
  
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
  
  if (verbose) {System.out.println("["+pluginName+"] Saving lives db one last time...");}
  livesDb.load();
  Iterator<String> itr = livesList.keySet().iterator();
  while(itr.hasNext())
  {
   String key=itr.next();
   livesDb.setInt(key,livesList.get(key));
  }
  livesDb.save();
  if (verbose) {System.out.println("["+pluginName+"] I'm not even angry...");}
 }
 
 //Event Callbacks:
 public void onDamage(EntityDamageEvent e)
 {
  Player player = (Player)e.getEntity();
  String playerName = player.getName();
  
  if (!checkPermission(player,"canUse")) {player.sendMessage(accessDenied);return;}
  if (player.getHealth()<1) {return;}//Player is already dead
  
  //If they will be dying at the end of this event, store their stuff!
  if (player.getHealth()-e.getDamage()<1)
  {
   if (getLives(playerName)>=1)
   {
    if (verbose) {System.out.println("["+pluginName+"] Player "+playerName+" is gonna die! Save their stuff!");}
    inventoryStore newStore = new inventoryStore(this);
    newStore.copy(player.getInventory());
    invStore.put(player,newStore);
    //Will subtract a life during the "onDeath" event.
   }
   else
   {
    if (verbose) {System.out.println("["+pluginName+"] Player "+playerName+" is gonna die! They are out of lives so their stuff will not be saved.");}
   }
  }
 }
 
 public void onDeath(EntityDeathEvent e)
 {
  Player player = (Player)e.getEntity();
  String playerName = player.getName();
  
  if (!checkPermission(player,"canUse")) {player.sendMessage(accessDenied);return;}
  
  if (getLives(playerName)>=1)
  {
   //TODO: Fix issue with drops getting surpressed when killed with /kill-type admin commands.
   if (verbose) {System.out.println("["+pluginName+"] Supressing drops for "+playerName);}
   for(int i=0;i<e.getDrops().size();i++) {e.getDrops().remove(i);i--;}
   if (!infiniteLives) {subtractLives(playerName,1);}//Do the subtraction of a life.
  }
  else
  {
   if (verbose) {System.out.println("["+pluginName+"] Player "+playerName+" is out of lives, drops will not be surpressed.");}
  }
 }
 
 public void onRespawn(PlayerRespawnEvent e)
 {
  //We can not give them back their stuff yet, just mark the entry as respawned and handle it in onMove...
  //We have to check the respawn because the entity can keep falling after death.
  Player player = e.getPlayer();
  
  if (!checkPermission(player,"canUse")) {player.sendMessage(accessDenied);return;}
  
  if (invStore.containsKey(player))
  {
   invStore.get(player).setIsRespawned(true);
   int lives = getLives(player.getName());
   if (infiniteLives)
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
   if (!infiniteLives) {player.sendMessage("Welcome back! It looks like you were out of lives.");}//Ideally this does not happen, but lets at least try to avoid confusing the player.
   player.sendMessage("Your stuff did not come back with you.");
   player.sendMessage("However, it might still be where you died!");
  }
  
  //Death iConomy punishment:
  if (econ.isEnabled() && deathPunishmentCost>0)
  {
   double oldBal = econ.getBalance(player);
   if (oldBal>=minBalanceForPunishment)
   {
    double toTake = deathPunishmentCost;
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
   if (!checkPermission(player,"canUse")) {player.sendMessage(accessDenied);return;}
   invStore.get(player).paste(player.getInventory());
   invStore.remove(player);
  }
 }
 
 public void onJoin(PlayerJoinEvent e)
 {
  if (verbose) {System.out.println("["+pluginName+"] Player joined! '"+e.getPlayer().getName()+"'");}
  String playerName = e.getPlayer().getName();
  if (playerExists(playerName))
  {
   if (verbose) {System.out.println("["+pluginName+"] Recognized player! They have "+getLives(playerName)+" lives!");}
  }
  else
  {
   if (verbose) {System.out.println("["+pluginName+"] Unrecognized player! Giving them "+defaultLives+" lives!");}
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
   
   targetName = searchPlayer(targetName);
   if (targetName!=playerName)
   {
    messagePrefix = targetName+" has";
    if (!checkPermission(player,"checkOthers")) {player.sendMessage(accessDenied);return true;}
   }
   else if (!checkPermission(player,"checkSelf")) {player.sendMessage(accessDenied);return true;}
   
   
   if (playerExists(targetName))
   {
    if (infiniteLives)
    {sender.sendMessage(messagePrefix+" infinite lives.");}
    else
    {sender.sendMessage(messagePrefix+" "+getLives(targetName)+" lives.");}
   }
   else {sender.sendMessage("No player named "+targetName+".");}
   return true;
  }
  else if ((commandName.compareToIgnoreCase("givelives") == 0 || commandName.compareToIgnoreCase("takelives") == 0 || commandName.compareToIgnoreCase("setlives") == 0) && sender.isOp())//Give lives to someone /givelives [playername] [lives]
  {
   if (!checkPermission(player,"change")) {player.sendMessage(accessDenied);return true;}
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
   
   targetName = searchPlayer(targetName);
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
   
   if (playerExists(targetName))
   {
    //Holdings account = iConomy.getAccount(targetName).getHoldings();
    if (econ.getBalance(targetName)<lifeCost*count)
    {
     sender.sendMessage("You do not have enough"+econ.getCurrency(true));
     sender.sendMessage("You need "+econ.format(lifeCost*count)+(count>1?"to buy "+count+" lives.":" to buy a life."));
     return true;
    }
    
    econ.subBalance(targetName,lifeCost*count);
    addLives(targetName,count);
    int numLives = getLives(targetName);
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
 
 //Permissions Integration
 boolean checkPermission(Player player,String node)
 {
  if (permissionsPlugin!=null)
  {return permissionsPlugin.has(player,"playerlives."+node);}
  else
  {
   if (node=="canUse" || node=="checkSelf")
   {return true;}//Things normal people can use.
   else//checkOthers, change, buy
   {return player.isOp();}//If not, assume op-only.
  }
 }
 final static String accessDenied = "You do not have access to that command.";
 
 //Lives manipulation
 boolean playerExists(String player) {return livesList.containsKey(player);}
 
 int getLives(String player)
 {
  if (playerExists(player))
  {
   int ret = livesList.get(player);
   if (ret<=0 && infiniteLives)
   {return 1;}//Never return <0 if infiniteLives is on
   else
   {return ret;}
  }
  else {return -1;}
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
 
 //Util
 public String searchPlayer(String targetName)
 {
  if (!playerExists(targetName))//Try to resolve the name to someone on the server...
  {
   List<Player> matches = getServer().matchPlayer(targetName);
   if (matches.size()>=1) {return matches.get(0).getName();}
  }
  return targetName;
 }
 
 //External Plugin Support
 public void onPluginEnable(PluginEnableEvent e)
 {
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
