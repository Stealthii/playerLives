package com.pathogenstudios.playerlives;

import org.bukkit.entity.Player;

public class EconWrapper {
    public boolean isEnabled() {return false;}
    public String format(double amount) {return Double.toString(amount) + " " + getCurrency(amount);}
    public double getBalance(String player) {return 0.0;}
    public double getBalance(Player player) {return getBalance(player.getName());}
    public void subBalance(String player, double value) {}
    public void subBalance(Player player, double value) {subBalance(player.getName(), value);}
    public String getCurrency(boolean multi) {return multi ? "Coins" : "Coin";}
    public String getCurrency(double value) {return getCurrency(value > 1.0);}
    public String getCurrency() {return getCurrency(false);}
}
