package com.pathogenstudios.playerlives;

//~--- non-JDK imports --------------------------------------------------------

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;

public class PlayerLivesEntityListener extends EntityListener {
    PlayerLives parent;

    public PlayerLivesEntityListener(PlayerLives parent) {
        this.parent = parent;
    }

    // Just call back to the parent
    // public void onEntityDamage(EntityDamageEvent e) {if (e.getEntity() instanceof Player) {parent.onDamage(e);}}
    public void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player) {
            parent.onDeath(e);
        }
    }
}
