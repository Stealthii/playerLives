package com.pathogenstudios.playerlives;

import org.bukkit.event.server.PluginEvent;
import org.bukkit.event.server.ServerListener;

public class plServerListener extends ServerListener
{
 playerLives parent;
 public plServerListener(playerLives parent) {this.parent = parent;}
 public void onPluginEnabled(PluginEvent e) {parent.onPluginEnabled(e);}
}
