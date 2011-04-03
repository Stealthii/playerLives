package com.pathogenstudios.playerlives;

import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.entity.Player;

public class plEntityListener extends EntityListener
{
 playerLives parent;
 
 public plEntityListener(playerLives parent) {this.parent = parent;}
 
 //Just call back to the parent
 public void onEntityDamage(EntityDamageEvent e) {if (e.getEntity() instanceof Player) {parent.onDamage(e);}}
 public void onEntityDeath(EntityDeathEvent e) {if (e.getEntity() instanceof Player) {parent.onDeath(e);}}
}
