package net.p3pp3rf1y.sophisticatedstorage.compat.chipped;

import earth.terrarium.chipped.common.compat.jei.WorkbenchCategory;
import earth.terrarium.chipped.common.registry.ModRecipeTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.chipped.BlockTransformationUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.compat.chipped.BlockTransformationUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.compat.chipped.BlockTransformationUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.compat.jei.StoragePlugin;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

import java.util.function.Supplier;

public class ChippedCompat implements ICompat {

	public static final DeferredHolder<Item, BlockTransformationUpgradeItem> BOTANIST_WORKBENCH_UPGRADE = ModItems.ITEMS.register("chipped/botanist_workbench_upgrade",
			() -> new BlockTransformationUpgradeItem(ModRecipeTypes.WORKBENCH, Config.SERVER.maxUpgradesPerStorage));
	public static final DeferredHolder<Item, BlockTransformationUpgradeItem> GLASSBLOWER_UPGRADE = ModItems.ITEMS.register("chipped/glassblower_upgrade",
			() -> new BlockTransformationUpgradeItem(ModRecipeTypes.WORKBENCH, Config.SERVER.maxUpgradesPerStorage));
	public static final DeferredHolder<Item, BlockTransformationUpgradeItem> CARPENTERS_TABLE_UPGRADE = ModItems.ITEMS.register("chipped/carpenters_table_upgrade",
			() -> new BlockTransformationUpgradeItem(ModRecipeTypes.WORKBENCH, Config.SERVER.maxUpgradesPerStorage));
	public static final DeferredHolder<Item, BlockTransformationUpgradeItem> LOOM_TABLE_UPGRADE = ModItems.ITEMS.register("chipped/loom_table_upgrade",
			() -> new BlockTransformationUpgradeItem(ModRecipeTypes.WORKBENCH, Config.SERVER.maxUpgradesPerStorage));
	public static final DeferredHolder<Item, BlockTransformationUpgradeItem> MASON_TABLE_UPGRADE = ModItems.ITEMS.register("chipped/mason_table_upgrade",
			() -> new BlockTransformationUpgradeItem(ModRecipeTypes.WORKBENCH, Config.SERVER.maxUpgradesPerStorage));
	public static final DeferredHolder<Item, BlockTransformationUpgradeItem> ALCHEMY_BENCH_UPGRADE = ModItems.ITEMS.register("chipped/alchemy_bench_upgrade",
			() -> new BlockTransformationUpgradeItem(ModRecipeTypes.WORKBENCH, Config.SERVER.maxUpgradesPerStorage));
	public static final DeferredHolder<Item, BlockTransformationUpgradeItem> TINKERING_TABLE_UPGRADE = ModItems.ITEMS.register("chipped/tinkering_table_upgrade",
			() -> new BlockTransformationUpgradeItem(ModRecipeTypes.WORKBENCH, Config.SERVER.maxUpgradesPerStorage));

	@Override
	public void init(IEventBus modBus) {
		modBus.addListener(this::registerContainers);

		if (ModList.get().isLoaded(CompatModIds.JEI)) {
			((Supplier<Runnable>) () -> () -> StoragePlugin.setAdditionalCatalystRegistrar(registration -> {
				registration.addRecipeCatalyst(new ItemStack(BOTANIST_WORKBENCH_UPGRADE.get()), WorkbenchCategory.RECIPE);
				registration.addRecipeCatalyst(new ItemStack(GLASSBLOWER_UPGRADE.get()), WorkbenchCategory.RECIPE);
				registration.addRecipeCatalyst(new ItemStack(CARPENTERS_TABLE_UPGRADE.get()), WorkbenchCategory.RECIPE);
				registration.addRecipeCatalyst(new ItemStack(LOOM_TABLE_UPGRADE.get()), WorkbenchCategory.RECIPE);
				registration.addRecipeCatalyst(new ItemStack(MASON_TABLE_UPGRADE.get()), WorkbenchCategory.RECIPE);
				registration.addRecipeCatalyst(new ItemStack(ALCHEMY_BENCH_UPGRADE.get()), WorkbenchCategory.RECIPE);
				registration.addRecipeCatalyst(new ItemStack(TINKERING_TABLE_UPGRADE.get()), WorkbenchCategory.RECIPE);
			})).get().run();
		}
	}

	public void registerContainers(RegisterEvent event) {
		if (!event.getRegistryKey().equals(Registries.MENU)) {
			return;
		}
		registerUpgradeContainer(BOTANIST_WORKBENCH_UPGRADE);
		registerUpgradeContainer(GLASSBLOWER_UPGRADE);
		registerUpgradeContainer(CARPENTERS_TABLE_UPGRADE);
		registerUpgradeContainer(LOOM_TABLE_UPGRADE);
		registerUpgradeContainer(MASON_TABLE_UPGRADE);
		registerUpgradeContainer(ALCHEMY_BENCH_UPGRADE);
		registerUpgradeContainer(TINKERING_TABLE_UPGRADE);
	}

	private void registerUpgradeContainer(DeferredHolder<Item, BlockTransformationUpgradeItem> item) {
		UpgradeContainerType<BlockTransformationUpgradeWrapper, BlockTransformationUpgradeContainer> containerType = new UpgradeContainerType<>(BlockTransformationUpgradeContainer::new);
		UpgradeContainerRegistry.register(item.getId(), containerType);
		if (FMLEnvironment.dist.isClient()) {
			ChippedCompatClient.registerUpgradeTab(item.getId(), containerType);
		}
	}

	@Override
	public void setup() {
		//noop
	}
}
