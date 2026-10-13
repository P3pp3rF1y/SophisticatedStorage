package net.p3pp3rf1y.sophisticatedstorage.compat.sb;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageHostWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackSettingsHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.settings.BackpackMainSettingsCategory;
import net.p3pp3rf1y.sophisticatedbackpacks.settings.BackpackMainSettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu.ISettingsContainerFactory;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageHostFactories;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategory;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageLinkedStorageHostWrapper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.DecorationTableInputSlotPreview;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.util.DecorationHelper;

import java.util.Map;

public class SBCompat implements ICompat {
	@Override
	public void setup() {
		LinkedStorageHostFactories.registerCompatibility(StorageLinkedStorageHostWrapper.FACTORY_ID, BackpackLinkedStorageHostWrapper.FACTORY_ID);
		StorageSettingsContainerMenu.setCanonicalFactoryResolver(
				(name, handler) -> BackpackMainSettingsCategory.NAME.equals(name) && (handler == null || handler instanceof BackpackSettingsHandler)
						? (ISettingsContainerFactory<MainSettingsCategory, BackpackMainSettingsContainer>) BackpackMainSettingsContainer::new
						: null);
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> SBClientCompat::setup);
		DecorationTableInputSlotPreview.registerPreviewStack(() -> new ItemStack(ModItems.BACKPACK.get()));

		DecorationTableBlockEntity.registerItemDecorator(stack -> stack.getItem() instanceof BackpackItem, new DecorationTableBlockEntity.IItemDecorator() {
			@Override
			public boolean supportsMaterials(ItemStack input) {
				return false;
			}

			@Override
			public boolean supportsTints(ItemStack input) {
				return true;
			}

			@Override
			public boolean supportsTopInnerTrim(ItemStack input) {
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

				return result.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).map(backpackWrapper -> {
					int originalMainColor = backpackWrapper.getMainColor();
					int originalAccentColor = backpackWrapper.getAccentColor();

					backpackWrapper.setColors(mainColorToSet, accentColorToSet);
					return new DecorationTableBlockEntity.TintDecorationResult(result,
							DecorationHelper.getDyePartsNeeded(mainColorToSet, accentColorToSet, originalMainColor, originalAccentColor, 20, 4));
				}).orElse(DecorationTableBlockEntity.TintDecorationResult.EMPTY);
			}

			private boolean colorsTransparentOrSameAs(ItemStack backpack, int mainColorToSet, int accentColorToSet) {
				return backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance())
						.map(backpackWrapper -> (mainColorToSet == -1 || mainColorToSet == backpackWrapper.getMainColor())
								&& (accentColorToSet == -1 || accentColorToSet == backpackWrapper.getAccentColor()))
						.orElse(true);
			}
		});
	}
}
