package com.pathogenstudios.playerlives;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class plPlayerListener extends PlayerListener
{
 playerLives parent;
 
 public plPlayerListener(playerLives parent)
 {
  this.parent = parent;
 }
 
 public void onPlayerRespawn(PlayerRespawnEvent e) {parent.onRespawn(e);}
 public void onPlayerMove(PlayerMoveEvent e) {parent.onMove(e);}
 public void onPlayerJoin(PlayerJoinEvent e) {parent.onJoin(e);}
}
