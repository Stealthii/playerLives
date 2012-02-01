
/*
playerLives
Created by Pathogen David
http://www.pathogenstudios.com/
*/
package com.pathogenstudios.playerlives;

//~--- non-JDK imports --------------------------------------------------------

import com.nijiko.permissions.PermissionHandler;

import com.nijikokun.bukkit.Permissions.Permissions;

import com.pathogenstudios.generic.*;
import com.pathogenstudios.playerlives.dbWrappers.*;
import com.pathogenstudios.playerlives.econWrappers.*;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.List;

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Main class
public class PlayerLives extends JavaPlugin {
    private DbWrapper         db                = null;
    private PermissionHandler permissionsPlugin = null;

    // Bukkit Listeners:
    private PlayerLivesPlayerListener playerListener = new PlayerLivesPlayerListener(this);
    private PlayerLivesEntityListener entityListener = new PlayerLivesEntityListener(this);
    private PlayerLivesServerListener serverListener = new PlayerLivesServerListener(this);

    // Inernal:
    private HashMap<Player, InventoryStore> invStore = new HashMap<Player, InventoryStore>();

    // private HashMap<Player,GenericLabel> hudLabels = null;

    // Configuration and such:
    public ConfigMan    conf;
    private EconWrapper econ;
    private LangMan     lang;

    // Plugins:
    private PluginManager pluginMan;

    public PlayerLives() {
        Log.pluginName = "pathogenPlayerLives";
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Enable / Disable:
    public void onEnable() {
        Log.m("Loading Pathogen playerLives...");
        Log.verbose = true;    // Use Verbose until config is loaded so any pre-config verbose messages are still displayed for debugging purposes.
        pluginMan = getServer().getPluginManager();
        econ      = new EconWrapper();    // Dummy wrapper until a compatible econ plugin is detected.

        // Make config folder if necessary...
        getDataFolder().mkdir();

        // Set up language stuff (Do before config - config will cause all allowed languages to be loaded.)
        lang = new LangMan(this, "en");

        // Config Loading and such
        conf        = new ConfigMan(this);
        Log.verbose = conf.verbose;

        // Load Lives Db
        if (conf.dbDriver.compareTo("mysql") == 0) {
            db = new MySQL(this);
        }

        if ((db == null) ||!db.isActive()) {
            db = new Flatfile(this);
        }    // Default / fall back on flatfile.

        // Register Events
        pluginMan.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Event.Priority.Normal, this);
        pluginMan.registerEvent(Event.Type.ENTITY_DEATH, entityListener, Event.Priority.Normal, this);
        pluginMan.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
        pluginMan.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Event.Priority.Normal, this);
        pluginMan.registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Event.Priority.Normal, this);
        pluginMan.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Event.Priority.Monitor, this);

        // Simulate onJoin events for all players currently online
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

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Callbacks:
    public void onDeath(EntityDeathEvent e) {
        Player player     = (Player) e.getEntity();
        String playerName = player.getName();

        if (!checkPermission(player, "canuse")) {
            return;
        }

        if (db.get(playerName) >= 1) {

            // Save old inventory:
            invStore.put(player, new InventoryStore(player.getInventory()));

            // Suppress Drops
            Log.d("Supressing drops for " + playerName);

            for (int i = 0; i < e.getDrops().size(); i++) {
                e.getDrops().remove(i);
                i--;
            }

            if (!conf.infiniteLives) {
                db.take(playerName, 1);
            }    // Do the subtraction of a life.
        } else {
            Log.d("Player " + playerName + " is out of lives, drops will not be surpressed.");
        }
    }

    public void onRespawn(PlayerRespawnEvent e) {

        // We can not give them back their stuff yet, just mark the entry as respawned and handle it in onMove...
        // We have to check the respawn because the entity can keep falling after death.
        Player player = e.getPlayer();

        if (!checkPermission(player, "canuse")) {
            return;
        }

        String language = lang.getDefaultLanguage();

        if (invStore.containsKey(player)) {
            invStore.get(player).setIsRespawned(true);

            int lives = db.get(player.getName());

            if (conf.infiniteLives) {
                return;
            } else if (lives == 1) {
                sendPlayerNotification(player, lang.get(language, "spoutWelcomeCaption"),
                                       lang.get(language, "spoutWelcomeOneLife"), Material.DIAMOND_CHESTPLATE,
                                       lang.get(language, "welcomeOneLife"));
            } else if (lives == 0) {
                sendPlayerNotification(player, lang.get(language, "spoutWelcomeCaption"),
                                       lang.get(language, "spoutWelcomeLastLife"), Material.OBSIDIAN,
                                       lang.get(language, "welcomeLastLife"));
            } else {
                sendPlayerNotification(player, lang.get(language, "spoutWelcomeCaption"),
                                       lang.get(language, "spoutWelcomeBackGeneral", "LIVES", Integer.toString(lives)),
                                       Material.DIAMOND_CHESTPLATE,
                                       lang.get(language, "welcomeBackGeneral", "LIVES", Integer.toString(lives)));
            }
        } else {
            sendPlayerNotification(player, lang.get(language, "spoutWelcomeNoLivesCaption"),
                                   lang.get(language, "spoutWelcomeNoLives"), Material.OBSIDIAN,
                                   lang.get(language, "welcomeNoLives"));
        }

        // Death economy punishment:
        if (econ.isEnabled() && (conf.deathPunishmentCost > 0)) {
            double oldBal = econ.getBalance(player);

            if (oldBal >= conf.minBalanceForPunishment) {
                double toTake = conf.deathPunishmentCost;

                if (oldBal - toTake < 0) {
                    toTake = oldBal;
                }    // In case the economy allows debt, we avoid creating it.

                econ.subBalance(player, toTake);
                player.sendMessage(lang.get(language, "deathPunishMessage", "MONEYLOST", econ.format(toTake),
                                            "PLAYERBALANCE", econ.format(econ.getBalance(player))));
            }
        }
    }

    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();

        // Give them their stuff back (if they just respawned and have logged stuff)
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
        } else {
            Log.d("Unrecognized player! Giving them " + conf.defaultLives + " lives!");
            db.addPlayer(player, conf.defaultLives);
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Handle commands
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player      = null;
        String playerName  = "";
        String commandName = command.getName().toLowerCase();

        if (sender instanceof Player) {
            player     = (Player) sender;
            playerName = player.getName();
        }

        String language = lang.getDefaultLanguage();

        if (commandName.compareToIgnoreCase("lives") == 0)    // Check the current number of lives for a person... /lives [playername]
        {
            boolean useThirdPerson = false;
            String  targetName     = playerName;

            if (args.length > 0) {
                targetName = args[0];
            } else if (player == null) {    // Console does not have lives!
                return false;
            }

            targetName = searchPlayer(targetName);

            if (targetName != playerName) {
                useThirdPerson = true;

                if (!checkPermission(player, "checkothers")) {
                    player.sendMessage(lang.get(language, "accessDenied"));

                    return true;
                }
            } else if (!checkPermission(player, "checkself")) {
                player.sendMessage(lang.get(language, "accessDenied"));

                return true;
            }

            if (db.exists(targetName)) {
                if (conf.infiniteLives) {
                    sender.sendMessage(lang.get(language, useThirdPerson
                            ? "commandLivesPlayerHasInf"
                            : "commandLivesYouHaveInf", "PLAYER", targetName));
                } else {
                    sender.sendMessage(lang.get(language, useThirdPerson
                            ? "commandLivesPlayerHas"
                            : "commandLivesYouHave", "PLAYER", targetName, "LIVES",
                                                     Integer.toString(db.get(targetName))));
                }
            } else {
                sender.sendMessage(lang.get(language, "unknownTarget", "PLAYER", targetName));
            }

            return true;
        }

        // Give lives to someone /givelives [playername] [lives]
        else if (((commandName.compareToIgnoreCase("givelives") == 0)
                  || (commandName.compareToIgnoreCase("takelives") == 0)
                  || (commandName.compareToIgnoreCase("setlives") == 0))) {
            if (!checkPermission(player, "change")) {
                player.sendMessage(lang.get(language, "accessDenied"));

                return true;
            }

            String  targetName = playerName;
            Integer count      = 1;

            if (commandName.compareToIgnoreCase("setlives") == 0) {
                count = conf.defaultLives;
            }

            boolean useThirdPerson = false;

            for (int i = 0; i < args.length; i++) {
                if (i > 1) {
                    break;
                }    // We only have two possible arguments...

                try {
                    count = Integer.parseInt(args[i]);
                }    // Is it a number? If so, it must be the count...
                        catch (Exception e) {
                    targetName = args[i];
                }    // If not, it is probably the player name
            }

            targetName = searchPlayer(targetName);

            if (commandName.compareToIgnoreCase("takelives") == 0) {
                count = -count;
            }

            if (targetName != playerName) {
                useThirdPerson = true;
            }

            if (db.exists(targetName)) {
                if (commandName.compareToIgnoreCase("setlives") == 0) {
                    db.set(targetName, count);
                } else {
                    db.give(targetName, count);
                }

                sender.sendMessage(lang.get(language, useThirdPerson
                        ? "commandGivePlayerHas"
                        : "commandGiveYouHave", "PLAYER", targetName, "LIVES", Integer.toString(db.get(targetName))));
            } else {
                sender.sendMessage(lang.get(language, "unknownTarget", "PLAYER", targetName));
            }

            return true;
        } else if (commandName.compareToIgnoreCase("buylives") == 0) {
            if (!checkPermission(player, "buy")) {
                player.sendMessage(lang.get(language, "accessDenied"));

                return true;
            }

            String  targetName = playerName;
            Integer count      = 1;

            if (!econ.isEnabled()) {
                sender.sendMessage(lang.get(language, "commandBuyNoEconomy"));

                return true;                              // << Technically it was handled...
            }

            if (args.length >= 1) {
                try {
                    count = Integer.parseInt(args[0]);    // Is it a number? If so, it must be the count...
                } catch (Exception e) {
                    sender.sendMessage(lang.get(language, "commandBuyNotNumber"));

                    return false;    // << To avoid unexpected transactions, bail out. Use false so Bukkit will remind them of command usage.
                }

                if (count < 1) {
                    sender.sendMessage(lang.get(language, "commandBuyBadInput"));

                    return false;
                }
            }

            if (db.exists(targetName)) {
                if (econ.getBalance(targetName) < conf.lifeCost * count) {
                    sender.sendMessage(lang.get(language, "commandBuyNotEnoughMoney", "CURRENCY",
                                                econ.getCurrency(true)));
                    sender.sendMessage(lang.get(language, (count > 1)
                            ? "commandBuyYouNeedMulti"
                            : "commandBuyYouNeed", "MONEY", econ.format(conf.lifeCost * count), "LIVES",
                                                   Integer.toString(count)));

                    return true;
                }

                econ.subBalance(targetName, conf.lifeCost * count);
                db.give(targetName, count);

                int numLives = db.get(targetName);

                sender.sendMessage(lang.get(language, (numLives == 1)
                        ? "commandBuyYouNowHave"
                        : "commandBuyYouNowHaveMulti", "LIVES", Integer.toString(numLives), "MONEY",
                        econ.format(econ.getBalance(targetName))));
            } else {
                sender.sendMessage(lang.get(language, "unknownTarget", "PLAYER", targetName));
                Log.e("Player '" + targetName + "' does not exist in the databse!");
            }

            return true;
        } else if ((commandName.compareToIgnoreCase("playerlives") == 0)
                   || (commandName.compareToIgnoreCase("ppl") == 0)) {
            String subCommand = "";

            if (args.length < 1) {
                sender.sendMessage(lang.get(language, "commandPplNone"));
                subCommand = "help";
            } else {
                subCommand = args[0].toLowerCase();
            }

            if (subCommand.equalsIgnoreCase("enable")) {
                sender.sendMessage(lang.get(language, "commandPplEnable"));

                return true;
            } else if (subCommand.equalsIgnoreCase("disable")) {
                sender.sendMessage(lang.get(language, "commandPplDisable"));

                return true;
            } else if (subCommand.equalsIgnoreCase("reload")) {
                sender.sendMessage(lang.get(language, "commandPplReloadMessageStart"));
                onDisable();
                onEnable();
                sender.sendMessage(lang.get(language, "commandPplReloadMessageDone"));

                return true;
            } else if (subCommand.equalsIgnoreCase("help")) {
                sender.sendMessage(lang.get(language, "helpFirstLine"));

                return true;
            } else {
                sender.sendMessage(lang.get(language, "commandPplInvalid", "SUBCOMMAND", subCommand));

                return false;
            }
        }

        return false;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Util
    boolean checkPermission(Player player, String node) {
        node = node.toLowerCase();    // Just in case...

        if (permissionsPlugin != null) {
            return permissionsPlugin.has(player, "playerlives." + node);
        } else {
            return player.hasPermission("playerlives." + node);
        }
    }

    public String searchPlayer(String targetName) {
        if (!db.exists(targetName))    // Try to resolve the name to someone on the

        // server...
        {
            List<Player> matches = getServer().matchPlayer(targetName);

            if (matches.size() >= 1) {
                return matches.get(0).getName();
            }
        }

        return targetName;
    }

    public void sendPlayerNotification(Player player, String title, String message, Material icon,
                                       String classicMessage) {
        if (classicMessage == null) {
            classicMessage = title + " " + message;
        }

        player.sendMessage(classicMessage);
    }

    public void sendPlayerNotification(Player player, String title, String message, Material icon) {
        sendPlayerNotification(player, title, message, icon, null);
    }

    // ////////////////////////////////////////////
    // External Plugin Support
    public void onPluginEnable(PluginEnableEvent e) {

        // Economy Plugins
        if (!econ.isEnabled()) {
            cosine.boseconomy.BOSEconomy BOSEconomyPlugin =
                (cosine.boseconomy.BOSEconomy) pluginMan.getPlugin("BOSEconomy");

            if ((BOSEconomyPlugin != null) && BOSEconomyPlugin.isEnabled()) {
                Log.m("Successfully linked with BOSEconomy");
                econ = new BOSEconomy(BOSEconomyPlugin);
            } else {
                try {
                    com.iCo6.iConomy iConomy6Plugin = (com.iCo6.iConomy) pluginMan.getPlugin("iConomy");

                    if ((iConomy6Plugin != null) && iConomy6Plugin.isEnabled()) {
                        Log.m("Successfully linked with iConomy 6");
                        econ = new iConomy6();
                    }
                } catch (NoClassDefFoundError ex1) {
                    Log.m("Failed to link with iConomy 6. Trying iConomy 5...");

                    try {
                        com.iConomy.iConomy iConomy5Plugin = (com.iConomy.iConomy) pluginMan.getPlugin("iConomy");

                        if ((iConomy5Plugin != null) && iConomy5Plugin.isEnabled()) {
                            Log.m("Successfully linked with iConomy 5");
                            econ = new iConomy5();
                        }
                    } catch (NoClassDefFoundError ex2) {
                        Log.m("Failed to link with iConomy 5. Trying iConomy 4...");

                        com.nijiko.coelho.iConomy.iConomy iConomy4Plugin =
                            (com.nijiko.coelho.iConomy.iConomy) pluginMan.getPlugin("iConomy");

                        if ((iConomy4Plugin != null) && iConomy4Plugin.isEnabled()) {
                            Log.m("Successfully linked with iConomy 4");
                            econ = new iConomy4();
                        } else {
                            Log.e("Failed to link with iConomy 4 and iConomy 5!");
                        }
                    }
                }
            }
        }

        // Permissions Plugins
        if (permissionsPlugin == null) {
            Permissions tempPerm = (Permissions) pluginMan.getPlugin("Permissions");

            if (tempPerm != null) {
                permissionsPlugin = tempPerm.getHandler();
                Log.m("Successfully linked with Permissions");
            } else {
                permissionsPlugin = null;
            }
        }
    }
}
