package com.hbm.inventory.container;

import com.hbm.inventory.SlotNonRetarded;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.TileEntityCoreManipulator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerCoreManipulator extends ContainerBase {

	public ContainerCoreManipulator(InventoryPlayer player, TileEntityCoreManipulator coreManipulator) {
		super(player, coreManipulator);

		this.addSlotToContainer(new SlotNonRetarded(coreManipulator, 0, 38, 107));
		this.addSlotToContainer(new SlotNonRetarded(coreManipulator, 1, 31, 82));

		// Inventory panel is at X:56..231, Y:138..237.
		// Shifted left by one slot column to match the custom GUI frame alignment.
		playerInv(player, 44, 156, 214);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		ItemStack slotOriginal = null;
		Slot slot = (Slot) this.inventorySlots.get(index);

		if(slot != null && slot.getHasStack()) {
			ItemStack slotStack = slot.getStack();
			slotOriginal = slotStack.copy();

			if(index < tile.getSizeInventory()) {
				if(!this.mergeItemStack(slotStack, tile.getSizeInventory(), this.inventorySlots.size(), true)) {
					return null;
				}
			} else {
				if(ItemBlueprints.isCoreManipulatorBlueprint(slotOriginal)) {
					if(!this.mergeItemStack(slotStack, 0, 1, false)) {
						return null;
					}
				} else if(slotOriginal.getItem() == ModItems.sat_chip) {
					if(!this.mergeItemStack(slotStack, 1, 2, false)) {
						return null;
					}
				} else {
					return null;
				}
			}

			if(slotStack.stackSize == 0) {
				slot.putStack(null);
			} else {
				slot.onSlotChanged();
			}

			slot.onPickupFromSlot(player, slotStack);
		}

		return slotOriginal;
	}
}
