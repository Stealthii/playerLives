package com.pathogenstudios.playerlives;

import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

public class PlayerLivesServerListener extends ServerListener {
	PlayerLives parent;

	public PlayerLivesServerListener(PlayerLives parent) {
		this.parent = parent;
	}

	public void onPluginEnable(PluginEnableEvent e) {
		parent.onPluginEnable(e);
	}
}
