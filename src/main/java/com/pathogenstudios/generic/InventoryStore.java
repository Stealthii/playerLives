package com.pathogenstudios.generic;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Inventory + Meta data needed by pathogenPlayerLives container:
public class InventoryStore {
	private ItemStack[] contents;
	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;
	private boolean isRespawned = false;//Used by onMove to allow the inventory to come back...

	public InventoryStore(PlayerInventory inv) {
		Log.d("Saving inventory...");
		contents = inv.getContents().clone();
		helmet = inv.getHelmet();
		chestplate = inv.getChestplate();
		leggings = inv.getLeggings();
		boots = inv.getBoots();
	}

	public void paste(PlayerInventory inv) {
		Log.d("Restoring inventory...");
		inv.setContents(contents);
		if (helmet != null && helmet.getTypeId() != 0) {
			inv.setHelmet(helmet);
		}
		if (chestplate != null && chestplate.getTypeId() != 0) {
			inv.setChestplate(chestplate);
		}
		if (leggings != null && leggings.getTypeId() != 0) {
			inv.setLeggings(leggings);
		}
		if (boots != null && boots.getTypeId() != 0) {
			inv.setBoots(boots);
		}
	}

	public void setIsRespawned(boolean isRespawned) {
		this.isRespawned = isRespawned;
	}

	public boolean isRespawned() {
		return isRespawned;
	}
}
