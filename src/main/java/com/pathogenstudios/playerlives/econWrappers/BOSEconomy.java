package com.pathogenstudios.playerlives.econWrappers;

//~--- non-JDK imports --------------------------------------------------------

import com.pathogenstudios.playerlives.EconWrapper;

public class BOSEconomy extends EconWrapper {
    cosine.boseconomy.BOSEconomy bose;

    public BOSEconomy(cosine.boseconomy.BOSEconomy bose) {
        super();
        this.bose = bose;
    }

    public boolean isEnabled() {
        return true;
    }

    public String format(double amount) {
        return bose.getMoneyFormatted(amount) + " " + bose.getMoneyNameProper(amount);
    }

    public double getBalance(String player) {
        return bose.getPlayerMoneyDouble(player);
    }

    public void subBalance(String player, double value) {
        bose.addPlayerMoney(player, -value, false);
    }

    public String getCurrency(boolean multi) {
        return multi
               ? bose.getMoneyNamePlural()
               : bose.getMoneyName();
    }
}
