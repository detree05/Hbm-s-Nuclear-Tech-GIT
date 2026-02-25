package com.hbm.inventory.container;

import com.hbm.tileentity.machine.TileEntityCoreManipulator;

import net.minecraft.entity.player.InventoryPlayer;

public class ContainerCoreManipulator extends ContainerBase {

	public ContainerCoreManipulator(InventoryPlayer player, TileEntityCoreManipulator coreManipulator) {
		super(player, coreManipulator);

		// Inventory panel is at X:56..231, Y:138..237.
		// Slot grid remains centered inside that panel.
		playerInv(player, 62, 156, 214);
	}
}
