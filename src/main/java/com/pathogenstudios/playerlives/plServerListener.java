package com.pathogenstudios.playerlives;

import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

public class plServerListener extends ServerListener
{
 playerLives parent;
 public plServerListener(playerLives parent) {this.parent = parent;}
 public void onPluginEnable(PluginEnableEvent e) {parent.onPluginEnable(e);}
}
