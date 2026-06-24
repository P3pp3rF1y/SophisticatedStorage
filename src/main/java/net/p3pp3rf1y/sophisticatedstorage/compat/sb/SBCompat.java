package net.p3pp3rf1y.sophisticatedstorage.compat.sb;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.DecorationTableInputSlotPreview;
import net.p3pp3rf1y.sophisticatedstorage.util.DecorationHelper;

import java.util.Map;

public class SBCompat implements ICompat {
	@Override
	public void setup() {
		DecorationTableInputSlotPreview.registerPreviewStack(() -> new ItemStack(ModItems.BACKPACK.get()));

		DecorationTableBlockEntity.registerItemDecorator(BackpackItem.class::isInstance, new DecorationTableBlockEntity.IItemDecorator() {
			@Override
			public boolean supportsMaterials(ItemResource input) {
				return false;
			}

			@Override
			public boolean supportsTints(ItemResource input) {
				return true;
			}

			@Override
			public boolean supportsTopInnerTrim(ItemResource input) {
				return false;
			}

			@Override
			public ItemStack decorateWithMaterials(ItemStack input, Map<BarrelMaterial, ResourceLocation> materialsToApply) {
				return ItemStack.EMPTY;
			}

			@Override
			public DecorationTableBlockEntity.TintDecorationResult decorateWithTints(ItemStack input, int mainColorToSet, int accentColorToSet) {
				if (colorsTransparentOrSameAs(input, mainColorToSet, accentColorToSet)) {
					return DecorationTableBlockEntity.TintDecorationResult.EMPTY;
				}

				ItemStack result = input.copyWithCount(1);

				IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(result);
				int originalMainColor = backpackWrapper.getMainColor();
				int originalAccentColor = backpackWrapper.getAccentColor();

				backpackWrapper.setColors(mainColorToSet, accentColorToSet);
				return new DecorationTableBlockEntity.TintDecorationResult(result,
						DecorationHelper.getDyePartsNeeded(mainColorToSet, accentColorToSet, originalMainColor, originalAccentColor, 20, 4));
			}

			private boolean colorsTransparentOrSameAs(ItemStack backpack, int mainColorToSet, int accentColorToSet) {
				IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
				return (mainColorToSet == -1 || mainColorToSet == backpackWrapper.getMainColor())
						&& (accentColorToSet == -1 || accentColorToSet == backpackWrapper.getAccentColor());
			}
		});
	}
}
