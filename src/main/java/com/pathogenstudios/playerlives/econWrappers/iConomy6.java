package com.pathogenstudios.playerlives.econWrappers;

import com.pathogenstudios.playerlives.EconWrapper;
import com.iCo6.iConomy;
import com.iCo6.system.Accounts;

public class iConomy6 extends EconWrapper {
    public iConomy6() {super();}
    public boolean isEnabled() {return true;}
    public String format(double amount) {return iConomy.format(amount);}
    public double getBalance(String player) {return (new Accounts()).get(player).getHoldings().getBalance();}
    public void subBalance(String player, double value) {(new Accounts()).get(player).getHoldings().subtract(value);}
    
    public String getCurrency(boolean multi) {
	if (multi) {return format(932).replaceFirst("932", "");}//TODO: HACKY
	else {return format(1).replaceFirst("1", "");}//TODO: HACKY
    }
}
