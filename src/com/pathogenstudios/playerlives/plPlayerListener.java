package com.pathogenstudios.playerlives;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
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
 
 public void onPlayerInteract(PlayerInteractEvent e)
 {
  if (e.getItem()!=null && e.getItem().getTypeId()==81 && e.getAction()==Action.RIGHT_CLICK_BLOCK)
  {
   //Simulate the placing of a cactus
   int newAmount=e.getItem().getAmount()-1;
   if (newAmount<=0)
   {
    e.getPlayer().sendMessage("You can only magic cactus when you have >1 in your stack!");
   }
   else
   {
    e.getItem().setAmount(newAmount);
    e.getClickedBlock().getRelative(e.getBlockFace()).setTypeId(81);
   }
  }
 }
}
