package com.pathogenstudios.playerlives;


import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.PluginManager;

import com.iConomy.iConomy;
import com.iConomy.system.Holdings;

public class econWrapper
{
 private iConomy iConomyPlugin = null;
 private PluginManager pluginMan;
 
 public econWrapper(PluginManager pluginMan)
 {
  this.pluginMan = pluginMan;
 }
 
 public void onPluginEnable(PluginEnableEvent e)
 {
  if (iConomyPlugin==null)
  {
   iConomyPlugin = (iConomy)pluginMan.getPlugin("iConomy");
   
   if (iConomyPlugin!=null && iConomyPlugin.isEnabled())
   {System.out.println("["+playerLives.pluginName+"] Successfully linked with iConomy");}
   else
   {iConomyPlugin = null;}
  }
 }
 
 public boolean isEnabled()
 {
  return iConomyPlugin!=null;
 }
 
 public String format(double amount)
 {
  if (iConomyPlugin!=null) {return iConomy.format(amount);}
  else {return Double.toString(amount);}
 }
 
 public double getBalance(String player)
 {
  if (iConomyPlugin!=null) {return iConomy.getAccount(player).getHoldings().balance();}
  else {return 0.0;}
 }
 public double getBalance(Player player) {return getBalance(player.getName());}
 
 public void subBalance(String player,double value)
 {
  if (iConomyPlugin!=null) {iConomy.getAccount(player).getHoldings().subtract(value);}
 }
 public void subBalance(Player player,double value) {subBalance(player.getName(),value);}
 
 public String getCurrency(boolean multi)
 {
  if (iConomyPlugin!=null)
  {
   if (multi)
   {return iConomy.format(932).replaceFirst("932","");}//TODO: HACKY
   else
   {return iConomy.format(1).replaceFirst("1","");}//TODO: HACKY
  }
  else {return multi?"Coins":"Coin";}
 }
 public String getCurrency(double value) {return getCurrency(value>1.0);}
 public String getCurrency() {return getCurrency(false);}
}
