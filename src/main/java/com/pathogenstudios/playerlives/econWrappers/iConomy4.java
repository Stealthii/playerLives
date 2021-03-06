package com.pathogenstudios.playerlives.econWrappers;

//~--- non-JDK imports --------------------------------------------------------

import com.nijiko.coelho.iConomy.iConomy;

import com.pathogenstudios.playerlives.EconWrapper;

public class iConomy4 extends EconWrapper {
    public iConomy4() {
        super();
    }

    public boolean isEnabled() {
        return true;
    }

    public String format(double amount) {
        return iConomy.getBank().format(amount);
    }

    public double getBalance(String player) {
        return iConomy.getBank().getAccount(player).getBalance();
    }

    public void subBalance(String player, double value) {
        iConomy.getBank().getAccount(player).subtract(value);
    }

    public String getCurrency(boolean multi) {
        if (multi) {
            return format(932).replaceFirst("932", "");
        }    // TODO: HACKY
                else {
            return format(1).replaceFirst("1", "");
        }    // TODO: HACKY
    }
}
