/*
playerLives
Created by Pathogen David
http://www.pathogenstudios.com/
*/
package com.pathogenstudios.playerlives;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.getspout.spoutapi.SpoutManager;
//import org.getspout.spoutapi.gui.*;
//import org.getspout.spoutapi.player.SpoutPlayer;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import com.pathogenstudios.playerlives.dbWrappers.*;
import com.pathogenstudios.playerlives.econWrappers.*;
import com.pathogenstudios.generic.*;

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Main class
public class PlayerLives extends JavaPlugin {
    public PlayerLives() {
	Log.pluginName = "pathogenPlayerLives";
    }

    //Bukkit Listeners:
    private PlayerLivesPlayerListener playerListener = new PlayerLivesPlayerListener(this);
    private PlayerLivesEntityListener entityListener = new PlayerLivesEntityListener(this);
    private PlayerLivesServerListener serverListener = new PlayerLivesServerListener(this);

    //Plugins:
    private PluginManager pluginMan;
    private PermissionHandler permissionsPlugin = null;
    private EconWrapper econ;
    private DbWrapper db = null;
    private SpoutManager spout = null;

    //Inernal:
    private HashMap<Player, inventoryStore> invStore = new HashMap<Player, inventoryStore>();
    //private HashMap<Player,GenericLabel> hudLabels = null;

    //Configuration:
    public ConfigMan conf;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Enable / Disable:
    public void onEnable() {
	Log.m("Loading Pathogen playerLives...");

	pluginMan = getServer().getPluginManager();
	econ = new EconWrapper();//Dummy wrapper until a compatible econ plugin is detected.

	//Make config folder if necessary...
	getDataFolder().mkdir();//boolean newInstall =

	//Config Loading and such
	conf = new ConfigMan(this);
	Log.verbose = conf.verbose;

	//Load Lives Db
	if (conf.dbDriver.compareTo("mysql") == 0) {
	    db = new MySQL(this);
	}
	if (db == null || !db.isActive()) {
	    db = new Flatfile(this);
	}//Default / fall back on flatfile.

	//Register Events
	pluginMan.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Event.Priority.Normal, this);
	pluginMan.registerEvent(Event.Type.ENTITY_DEATH, entityListener, Event.Priority.Normal, this);
	pluginMan.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
	pluginMan.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Event.Priority.Normal, this);
	pluginMan.registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Event.Priority.Normal, this);
	pluginMan.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Event.Priority.Monitor, this);

	//Simulate onJoin events for all players currently online
	Player[] onlinePlayers = getServer().getOnlinePlayers();

	for (Player onlinePlayer : onlinePlayers) {
	    playerListener.onPlayerJoin(new PlayerJoinEvent(onlinePlayer, ""));
	}

	Log.m("Done loading Pathogen playerLives.");
    }

    public void onDisable() {
	Log.m("Unloading Pathogen playerLives...");
	db.close();
	Log.v("I'm not even angry...");
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Event Callbacks:
    public void onDeath(EntityDeathEvent e) {
	Player player = (Player) e.getEntity();
	String playerName = player.getName();

	if (!checkPermission(player, "canuse")) {
	    return;
	}

	if (db.get(playerName) >= 1) {
	    //Save old inventory:
	    invStore.put(player, new inventoryStore(player.getInventory()));

	    //Suppress Drops
	    Log.d("Supressing drops for " + playerName);
	    for (int i = 0; i < e.getDrops().size(); i++) {
		e.getDrops().remove(i);
		i--;
	    }

	    if (!conf.infiniteLives) {
		db.take(playerName, 1);
	    }//Do the subtraction of a life.
	}
	else {
	    Log.d("Player " + playerName + " is out of lives, drops will not be surpressed.");
	}
    }

    public void onRespawn(PlayerRespawnEvent e) {
	//We can not give them back their stuff yet, just mark the entry as
	//respawned and handle it in onMove...
	//We have to check the respawn because the entity can keep falling
	//after death.
	Player player = e.getPlayer();

	if (!checkPermission(player, "canuse")) {
	    return;
	}

	if (invStore.containsKey(player)) {
	    invStore.get(player).setIsRespawned(true);
	    int lives = db.get(player.getName());
	    if (conf.infiniteLives) {}//Don't display anything.
				      //pathogenPlayerLives is now a static
				      //game mechanic.
	    else if (lives == 1) {
		//player.sendMessage("Welcome back! You only have one more life! Be careful!");
		sendPlayerNotification(player, "Welcome back!", "You have one more life!", Material.DIAMOND_CHESTPLATE, "Welcome back! You only have one more life! Be careful!");
	    }
	    else if (lives == 0) {
		//player.sendMessage("Welcome back! You have no lives left! Be careful!");
		sendPlayerNotification(player, "Welcome back!", "That was your last life!", Material.OBSIDIAN, "Welcome back! You have no lives left! Be careful!");
	    }
	    else {
		//player.sendMessage("Welcome back! You have "+lives+" lives left.");
		sendPlayerNotification(player, "Welcome back!", "You have " + lives + " lives.", Material.DIAMOND_CHESTPLATE, "Welcome back! You have " + lives + " lives left.");
	    }
	}
	else {
	    /*if (!conf.infiniteLives) {player.sendMessage("Welcome back! It looks like you were out of lives.");}//Ideally this does not happen, but lets at least try to avoid confusing the player.
	    player.sendMessage("Your stuff did not come back with you.");
	    player.sendMessage("However, it might still be where you died!");*/
	    sendPlayerNotification(player, "You are out of lives.", "You lost your stuff.", Material.OBSIDIAN, "You ran out of lives, so you lost all your stuff.");
	}

	//Death economy punishment:
	if (econ.isEnabled() && conf.deathPunishmentCost > 0) {
	    double oldBal = econ.getBalance(player);
	    if (oldBal >= conf.minBalanceForPunishment) {
		double toTake = conf.deathPunishmentCost;
		if (oldBal - toTake < 0) {
		    toTake = oldBal;
		}//In case the economy allows debt, we avoid creating it.
		econ.subBalance(player, toTake);
		player.sendMessage("You also lost " + econ.format(toTake) + " leaving you with " + econ.format(econ.getBalance(player)) + ".");
	    }
	}
    }

    public void onMove(PlayerMoveEvent e) {
	Player player = e.getPlayer();

	//Give them their stuff back (if they just respawned and have logged
	//stuff)
	if (invStore.containsKey(player) && invStore.get(player).isRespawned()) {
	    if (!checkPermission(player, "canuse")) {
		return;
	    }
	    invStore.get(player).paste(player.getInventory());
	    invStore.remove(player);
	}
    }

    public void onJoin(PlayerJoinEvent e) {
	Log.d("Player joined! '" + e.getPlayer().getName() + "'");
	Player player = e.getPlayer();
	if (db.exists(player)) {
	    Log.d("Recognized player! They have " + db.get(player.getName()) + " lives!");
	}
	else {
	    Log.d("Unrecognized player! Giving them " + conf.defaultLives + " lives!");
	    db.addPlayer(player, conf.defaultLives);
	}

	if (spout != null) {
	    //TODO: Make this not NEED spout but allow spout.
	    /*if (hudLabels == null)
	    {
	     hudLabels = new HashMap<Player,GenericLabel>();
	    }
	    
	    GenericLabel newLbl = new GenericLabel("");
	    newLbl.setHexColor(Integer.parseInt("FFFFFF", 16)).setX(10).setY(10);
	    newLbl.setText("");
	    newLbl.setDirty(true);
	    
	    //SpoutPlayer sp = SpoutManager.getPlayer(player);
	    //sp.getMainScreen().attachWidget(newLbl);
	    
	    hudLabels.put(player,newLbl);*/
	    onNumLivesChange(player.getName());
	}
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Internal callbacks
    public void onNumLivesChange(String playerName) {
	//Player player = getServer().getPlayer(playerName);

	//TODO: Make this not NEED spout but allow spout.
	/*if (spout == null || hudLabels.get(player)==null)
	{
	 Log.d("No player in hud label list or spout is off.");
	 return;
	}
	Log.d("Updating player " + playerName + "'s Spout status label.");
	
	((GenericLabel)hudLabels.get(player)).setText("Lives: " + db.get(player)).setDirty(true);
	SpoutManager.getPlayer(player).getMainScreen().setDirty(true);*/
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Handle commands
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	Player player = null;
	String playerName = "";
	String commandName = command.getName().toLowerCase();
	if (sender instanceof Player) {
	    player = (Player) sender;
	    playerName = player.getName();
	}

	if (commandName.compareToIgnoreCase("lives") == 0)//Check the current
							  //number of lives for
							  //a person... /lives
							  //[playername]
	{
	    String messagePrefix = "You have";
	    String targetName = playerName;
	    if (args.length > 0) {
		targetName = args[0];
	    }
	    else if (player == null) {
		return false;
	    }//Console does not have lives!

	    targetName = searchPlayer(targetName);
	    if (targetName != playerName) {
		messagePrefix = targetName + " has";
		if (!checkPermission(player, "checkothers")) {
		    player.sendMessage(accessDenied);
		    return true;
		}
	    }
	    else if (!checkPermission(player, "checkself")) {
		player.sendMessage(accessDenied);
		return true;
	    }

	    if (db.exists(targetName)) {
		if (conf.infiniteLives) {
		    sender.sendMessage(messagePrefix + " infinite lives.");
		}
		else {
		    sender.sendMessage(messagePrefix + " " + db.get(targetName) + " lives.");
		}
	    }
	    else {
		sender.sendMessage("No player named " + targetName + ".");
	    }
	    return true;
	}
	//Give lives to someone /givelives [playername] [lives]
	else if ((commandName.compareToIgnoreCase("givelives") == 0 || commandName.compareToIgnoreCase("takelives") == 0 || commandName.compareToIgnoreCase("setlives") == 0))
	{
	    if (!checkPermission(player, "change")) {
		player.sendMessage(accessDenied);
		return true;
	    }
	    String targetName = playerName;
	    Integer count = 1;
	    if (commandName.compareToIgnoreCase("setlives") == 0) {
		count = conf.defaultLives;
	    }
	    String messagePrefix = "You now have";

	    for (int i = 0; i < args.length; i++) {
		if (i > 1) {
		    break;
		}//We only have two possible arguments...
		try {
		    count = Integer.parseInt(args[i]);
		}//Is it a number? If so, it must be the count...
		catch (Exception e) {
		    targetName = args[i];
		}//If not, it is probably the player name
	    }

	    targetName = searchPlayer(targetName);
	    if (commandName.compareToIgnoreCase("takelives") == 0) {
		count = -count;
	    }
	    if (targetName != playerName) {
		messagePrefix = targetName + " now has";
	    }

	    if (db.exists(targetName)) {
		if (commandName.compareToIgnoreCase("setlives") == 0) {
		    db.set(targetName, count);
		}
		else {
		    db.give(targetName, count);
		}

		sender.sendMessage(messagePrefix + " " + db.get(targetName) + " lives.");
	    }
	    else {
		sender.sendMessage("No player named " + targetName + ".");
	    }
	    return true;
	}
	else if (commandName.compareToIgnoreCase("buylives") == 0) {
	    if (!checkPermission(player, "buy")) {
		player.sendMessage(accessDenied);
		return true;
	    }
	    String targetName = playerName;
	    Integer count = 1;

	    if (!econ.isEnabled()) {
		sender.sendMessage("Server needs an economy to enable buying lives!");
		return true;//<< Technically it was handled...
	    }

	    if (args.length >= 1) {
		try {
		    count = Integer.parseInt(args[0]);
		}//Is it a number? If so, it must be the count...
		catch (Exception e) {
		    sender.sendMessage("Expected a number for count.");
		    return false;//<< To avoid unexpected transactions, bail out. Use false so Bukkit will remind them of command usage.
		}

		if (count < 1) {
		    sender.sendMessage("Invalid count.");
		    return false;
		}
	    }

	    if (db.exists(targetName)) {
		//Holdings account =
		//iConomy.getAccount(targetName).getHoldings();
		if (econ.getBalance(targetName) < conf.lifeCost * count) {
		    sender.sendMessage("You do not have enough " + econ.getCurrency(true));
		    sender.sendMessage("You need " + econ.format(conf.lifeCost * count) + (count > 1 ? " to buy " + count + " lives." : " to buy a life."));
		    return true;
		}

		econ.subBalance(targetName, conf.lifeCost * count);
		db.give(targetName, count);
		int numLives = db.get(targetName);
		sender.sendMessage("You now have " + numLives + (numLives == 1 ? " life" : " lives") + " and " + econ.format(econ.getBalance(targetName)) + ".");
	    }
	    else {
		sender.sendMessage("No player named " + targetName + ".");
		Log.e("Player '" + targetName + "' does not exist!");
	    }
	    return true;
	}
	else if (commandName.compareToIgnoreCase("playerlives") == 0 || commandName.compareToIgnoreCase("ppl") == 0) {
	    String subCommand = "";
	    if (args.length < 1) {
		sender.sendMessage("You must specifiy a subcommand.");
		subCommand = "help";
	    }
	    else {
		subCommand = args[0].toLowerCase();
	    }

	    if (subCommand.equalsIgnoreCase("enable")) {
		sender.sendMessage("Unimplemented");
		return true;
	    }
	    else if (subCommand.equalsIgnoreCase("disable")) {
		sender.sendMessage("Unimplemented");
		return true;
	    }
	    else if (subCommand.equalsIgnoreCase("reload")) {
		sender.sendMessage("Reloading the plugin...");
		onDisable();
		onEnable();
		sender.sendMessage("Pathogen Player Lives was successfully reloaded.");
		return true;
	    }
	    else if (subCommand.equalsIgnoreCase("help")) {
		sender.sendMessage("Mmmmmm....so you're a bitch. that likes...BANANAS?!");
		return true;
	    }
	    else {
		sender.sendMessage("Invalid subcommand '" + subCommand + "'");
		return false;
	    }
	}

	return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Util
    boolean checkPermission(Player player, String node) {
	node = node.toLowerCase();//Just in case...
	if (permissionsPlugin != null) {
	    return permissionsPlugin.has(player, "playerlives." + node);
	}
	else {
	    return player.hasPermission("playerlives." + node);
	}
    }

    final static String accessDenied = "You do not have access to that command.";

    public String searchPlayer(String targetName) {
	if (!db.exists(targetName))//Try to resolve the name to someone on the
				   //server...
	{
	    List<Player> matches = getServer().matchPlayer(targetName);
	    if (matches.size() >= 1) {
		return matches.get(0).getName();
	    }
	}
	return targetName;
    }

    public void sendPlayerNotification(Player player, String title, String message, Material icon, String classicMessage) {
	if (classicMessage == null) {
	    classicMessage = title + " " + message;
	}

	if (spout != null && SpoutManager.getPlayer(player).isSpoutCraftEnabled()) {
	    SpoutManager.getPlayer(player).sendNotification(title, message, icon);
	}
	else {
	    player.sendMessage(classicMessage);
	}
    }

    public void sendPlayerNotification(Player player, String title, String message, Material icon) {
	sendPlayerNotification(player, title, message, icon, null);
    }

    //////////////////////////////////////////////
    //External Plugin Support
    public void onPluginEnable(PluginEnableEvent e) {
	//Economy Plugins
	if (!econ.isEnabled()) {
	    cosine.boseconomy.BOSEconomy BOSEconomyPlugin = (cosine.boseconomy.BOSEconomy) pluginMan.getPlugin("BOSEconomy");
	    if (BOSEconomyPlugin != null && BOSEconomyPlugin.isEnabled()) {
		Log.m("Successfully linked with BOSEconomy");
		econ = new BOSEconomy(BOSEconomyPlugin);
	    }
	    else {
		try {
		    com.iCo6.iConomy iConomy6Plugin = (com.iCo6.iConomy) pluginMan.getPlugin("iConomy");
		    if (iConomy6Plugin != null && iConomy6Plugin.isEnabled()) {
			Log.m("Successfully linked with iConomy 6");
			econ = new iConomy6();
		    }
		}
		catch (NoClassDefFoundError ex1) {
		    Log.m("Failed to link with iConomy 6. Trying iConomy 5...");
		    try {
			com.iConomy.iConomy iConomy5Plugin = (com.iConomy.iConomy) pluginMan.getPlugin("iConomy");
			if (iConomy5Plugin != null && iConomy5Plugin.isEnabled()) {
			    Log.m("Successfully linked with iConomy 5");
			    econ = new iConomy5();
			}
		    }
		    catch (NoClassDefFoundError ex2) {
			Log.m("Failed to link with iConomy 5. Trying iConomy 4...");
			com.nijiko.coelho.iConomy.iConomy iConomy4Plugin = (com.nijiko.coelho.iConomy.iConomy) pluginMan.getPlugin("iConomy");

			if (iConomy4Plugin != null && iConomy4Plugin.isEnabled()) {
			    Log.m("Successfully linked with iConomy 4");
			    econ = new iConomy4();
			}
			else {
			    Log.e("Failed to link with iConomy 4 and iConomy 5!");
			}
		    }
		}
	    }
	}

	//Permissions Plugins
	if (permissionsPlugin == null) {
	    Permissions tempPerm = (Permissions) pluginMan.getPlugin("Permissions");

	    if (tempPerm != null) {
		permissionsPlugin = tempPerm.getHandler();
		Log.m("Successfully linked with Permissions");
	    }
	    else {
		permissionsPlugin = null;
	    }
	}

	//Spout!
	if (spout == null && pluginMan.getPlugin("Spout") != null) {
	    spout = SpoutManager.getInstance();
	    if (spout != null) {
		Log.m("Successfully linked with Spout");
	    }
	}
    }
}
