package com.hbm.inventory.container;

import com.hbm.tileentity.machine.TileEntityCoreManipulator;

import net.minecraft.entity.player.InventoryPlayer;

public class ContainerCoreManipulator extends ContainerBase {

	public ContainerCoreManipulator(InventoryPlayer player, TileEntityCoreManipulator coreManipulator) {
		super(player, coreManipulator);

		// Inventory panel is at X:24..199, Y:138..237.
		// Slot grid inside that panel follows the vanilla offset (+8, +18).
		playerInv(player, 32, 156, 214);
	}
}
