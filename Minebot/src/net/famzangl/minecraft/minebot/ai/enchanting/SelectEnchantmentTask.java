/*******************************************************************************
 * This file is part of Minebot.
 *
 * Minebot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Minebot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Minebot.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package net.famzangl.minecraft.minebot.ai.enchanting;

import java.lang.reflect.Field;

import net.famzangl.minecraft.minebot.ai.AIHelper;
import net.famzangl.minecraft.minebot.ai.task.AITask;
import net.famzangl.minecraft.minebot.ai.task.TaskOperations;
import net.famzangl.minecraft.minebot.ai.task.error.StringTaskError;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.inventory.ContainerEnchantment;

public class SelectEnchantmentTask extends AITask {

	private static final int E_SLOT = 2;
	
	private boolean hasFailed = false;

	@Override
	public boolean isFinished(AIHelper aiHelper) {
		if (!(aiHelper.getMinecraft().currentScreen instanceof GuiEnchantment)) {
			return false;
		} else {
			final GuiEnchantment screen = (GuiEnchantment) aiHelper.getMinecraft().currentScreen;
			return screen.inventorySlots.getSlot(0).getHasStack()
					&& screen.inventorySlots.getSlot(0).getStack()
							.isItemEnchanted();
		}
	}

	@Override
	public void runTick(AIHelper aiHelper, TaskOperations taskOperations) {
		if (!(aiHelper.getMinecraft().currentScreen instanceof GuiEnchantment)) {
			System.out.println("Screen not opened.");
			taskOperations.desync(new StringTaskError("Enchantment screen is not open."));
			return;
		}
		final GuiEnchantment screen = (GuiEnchantment) aiHelper.getMinecraft().currentScreen;
		if (!screen.inventorySlots.getSlot(0).getHasStack()) {
			System.out.println("No stack in slot.");
			taskOperations.desync(new StringTaskError("No stack in enchantment table."));
			return;
		}
		if (screen.inventorySlots.getSlot(0).getStack().isItemEnchanted()) {
			System.out.println("Already enchanted.");
			return;
		}

		try {
			final Field field = GuiEnchantment.class
					.getDeclaredField("field_147075_G");
			field.setAccessible(true);
			final ContainerEnchantment enchantment = (ContainerEnchantment) field
					.get(screen);

			hasFailed = !attemptEnchanting(aiHelper, enchantment, E_SLOT);
		} catch (final Throwable e) {
			e.printStackTrace();
			taskOperations.desync(new StringTaskError("Some error... :-("));
			return;
		}
	}

	private boolean attemptEnchanting(AIHelper aiHelper, ContainerEnchantment c,
			int slot) {
		if (c.enchantLevels[slot] == 0) {
			System.out.println("No enchantment levels computed yet.");
			return false;
		}
		if (aiHelper.getMinecraft().thePlayer.experienceLevel < c.enchantLevels[slot]) {
			System.out.println("Abort enchantment, not enough levels.");
			return slot > 0 ? attemptEnchanting(aiHelper, c, slot - 1) : false;
		}
		if (c.enchantItem(aiHelper.getMinecraft().thePlayer, slot)) {
			aiHelper.getMinecraft().playerController.sendEnchantPacket(c.windowId,
					slot);
			System.out.println("Sent enchant request package.");
			return true;
		}
		return false;
	}
	
	public boolean hasFailed() {
		return hasFailed;
	}

}
