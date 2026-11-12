package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.item.TankBlockItem;

public final class TankItemContents {
	private TankItemContents() {
	}

	public record Contents(FluidStack fluid, ItemContainerContents upgrades, ItemContainerContents containers, int upgradeSlots, SortBy sortBy) {
	}

	public static Contents read(ItemStack stack) {
		int upgradeSlots = stack.getOrDefault(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS,
				((TankBlock) ((TankBlockItem) stack.getItem()).getBlock()).getTier().upgradeSlots());
		FluidStack fluid = stack.getOrDefault(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY).copy();
		return new Contents(fluid, stack.getOrDefault(ModDataComponents.TANK_UPGRADES, ItemContainerContents.EMPTY),
				stack.getOrDefault(ModDataComponents.TANK_CONTAINERS, ItemContainerContents.EMPTY), upgradeSlots,
				stack.getOrDefault(ModCoreDataComponents.SORT_BY, SortBy.NAME));
	}

	public static void write(ItemStack stack, Contents contents) {
		if (contents.fluid().isEmpty()) {
			stack.remove(ModCoreDataComponents.FLUID_CONTENTS);
		} else {
			stack.set(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.copyOf(contents.fluid()));
		}
		stack.set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, contents.upgradeSlots());
		stack.set(ModCoreDataComponents.SORT_BY, contents.sortBy());
		if (contents.upgrades().equals(ItemContainerContents.EMPTY)) {
			stack.remove(ModDataComponents.TANK_UPGRADES);
		} else {
			stack.set(ModDataComponents.TANK_UPGRADES, contents.upgrades());
		}
		if (contents.containers().equals(ItemContainerContents.EMPTY)) {
			stack.remove(ModDataComponents.TANK_CONTAINERS);
		} else {
			stack.set(ModDataComponents.TANK_CONTAINERS, contents.containers());
		}
	}

	public static boolean hasContents(ItemStack stack) {
		return !stack.getOrDefault(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY).isEmpty()
				|| stack.getOrDefault(ModDataComponents.TANK_UPGRADES, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().findAny().isPresent()
				|| stack.getOrDefault(ModDataComponents.TANK_CONTAINERS, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().findAny().isPresent();
	}

	public static void clearContents(ItemStack stack) {
		stack.remove(ModCoreDataComponents.FLUID_CONTENTS);
		stack.remove(ModDataComponents.TANK_UPGRADES);
		stack.remove(ModDataComponents.TANK_CONTAINERS);
	}

	public static boolean hasPortableData(ItemStack stack) {
		return stack.has(ModDataComponents.TANK_HEIGHT) || stack.has(ModCoreDataComponents.STORAGE_UUID)
				|| stack.has(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS) || hasContents(stack) || stack.has(ModDataComponents.LOCKED)
				|| stack.has(ModDataComponents.LOCK_VISIBLE) || stack.has(ModDataComponents.TIER_VISIBLE) || stack.has(ModDataComponents.UPGRADES_VISIBLE)
				|| stack.has(ModDataComponents.PACKED) || stack.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT);
	}
}
