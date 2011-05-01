package com.pathogenstudios.playerlives;

import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;

public class plBlockListener extends BlockListener
{
 playerLives parent;
 
 public plBlockListener(playerLives parent) {this.parent = parent;}
 
 public void onBlockPhysics(BlockPhysicsEvent e)
 {
  if (e.getBlock().getTypeId()==81)
  {
   e.setCancelled(true);
  }
 }
}
