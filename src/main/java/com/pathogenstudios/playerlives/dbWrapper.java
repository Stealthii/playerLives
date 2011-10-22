package com.pathogenstudios.playerlives;

import org.bukkit.entity.Player;

public abstract class dbWrapper
{
 protected playerLives parent;
 public dbWrapper(playerLives parent) {this.parent = parent;}
 public boolean isActive() {return false;}
 
 //Abstracts:
 public abstract void load();
 public abstract void save();
 public abstract boolean addPlayer(String player,int lives);
 public abstract boolean exists(String player);
 public abstract boolean set(String player,int lives);
 public abstract int get(String player);
 
 //"Redundant" functions:
 public void close() {save();}
 public boolean addPlayer(Player player,int lives) {return addPlayer(player.getName(),lives);}
 
 public boolean exists(Player player) {return exists(player.getName());}
 public int get(Player player) {return get(player.getName());}
 
 public boolean set(Player player,int lives) {return set(player.getName(),lives);}
 public boolean give(String player,int lives) {return set(player,get(player)+lives);}
 public boolean give(Player player,int lives) {return give(player.getName(),lives);}
 public boolean take(String player,int lives) {return give(player,-lives);}
 public boolean take(Player player,int lives) {return take(player.getName(),lives);}
}
