package com.pathogenstudios.playerlives.econWrappers;

import com.pathogenstudios.playerlives.econWrapper;
import com.iConomy.iConomy;

public class iConomy5 extends econWrapper
{
 
 public iConomy5() {super();}
 
 public boolean isEnabled() {return true;}
 public String format(double amount) {return iConomy.format(amount);}
 public double getBalance(String player) {return iConomy.getAccount(player).getHoldings().balance();}
 public void subBalance(String player,double value) {iConomy.getAccount(player).getHoldings().subtract(value);}
 public String getCurrency(boolean multi)
 {
  if (multi)
  {return format(932).replaceFirst("932","");}//TODO: HACKY
  else
  {return format(1).replaceFirst("1","");}//TODO: HACKY
 }
}
