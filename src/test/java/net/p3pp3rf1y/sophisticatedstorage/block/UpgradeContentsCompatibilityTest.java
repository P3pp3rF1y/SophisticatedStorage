package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.StorageContentsType;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpgradeContentsCompatibilityTest {
	@BeforeAll
	static void bootstrap() throws ReflectiveOperationException {
		StorageLinkedCapabilitiesTest.bootstrapMinecraft();
		Items.DIAMOND.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@AfterAll
	static void clearConfig() {
		StorageLinkedCapabilitiesTest.unloadServerConfig();
	}

	@ParameterizedTest
	@MethodSource("scenarios")
	void canAddUpgradeToMatchesDeclarationsAndDefaultsUnmarkedAddonsToItems(Compatibility params, boolean clientSide) {
		IStorageWrapper storage = storage(params.host());
		AddonUpgradeItem upgrade = params.addon().create();
		ItemStack stack = new ItemStack(upgrade);

		UpgradeSlotChangeResult result = upgrade.canAddUpgradeTo(storage, stack, true, clientSide);

		assertEquals(params.allowed(), result.successful());
		assertTrue(storage.getUpgradeHandler().getStackInSlot(0).isEmpty());
	}

	@ParameterizedTest
	@MethodSource("scenarios")
	void canSwapUpgradeForChecksTheReplacementDeclarationOnBothSides(Compatibility params, boolean clientSide) {
		IStorageWrapper storage = storage(params.host());
		AddonUpgradeItem installed = AddonKind.BOTH.create();
		ItemStack replacement = new ItemStack(params.addon().create());

		UpgradeSlotChangeResult result = installed.canSwapUpgradeFor(replacement, 0, storage, clientSide);

		assertEquals(params.allowed(), result.successful());
		assertTrue(storage.getUpgradeHandler().getStackInSlot(0).isEmpty());
	}

	@ParameterizedTest
	@MethodSource("scenarios")
	void insertItemAcceptsDeclaredAddonUpgradesWithoutAHandlerAllowlist(Compatibility params, boolean simulate) {
		IStorageWrapper storage = storage(params.host());
		ItemStack input = new ItemStack(params.addon().create());
		UpgradeHandler handler = storage.getUpgradeHandler();

		int inserted;
		try (Transaction tx = Transaction.openRoot()) {
			inserted = handler.insert(0, ItemResource.of(input), 1, tx);
			if (!simulate) {
				tx.commit();
			}
		}

		assertEquals(params.allowed() ? 1 : 0, inserted);
		assertEquals(params.allowed() && !simulate ? 1 : 0, handler.getStackInSlot(0).getCount());
		assertEquals(1, input.getCount());
		if (params.allowed() && !simulate) {
			assertSame(input.getItem(), handler.getStackInSlot(0).getItem());
		}
	}

	@ParameterizedTest
	@MethodSource("tankReplacements")
	void setStackInSlotRejectsItemOnlyReplacementsWithoutRemovingTheInstalledUpgrade(Compatibility params) {
		IStorageWrapper storage = storage(params.host());
		UpgradeHandler handler = storage.getUpgradeHandler();
		ItemStack installed = new ItemStack(AddonKind.FLUID.create());
		handler.setStackInSlot(0, installed);
		ItemStack replacement = new ItemStack(params.addon().create());

		if (params.allowed()) {
			handler.setStackInSlot(0, replacement);
		} else {
			assertThrows(IllegalArgumentException.class, () -> handler.setStackInSlot(0, replacement));
		}

		assertSame(params.allowed() ? replacement.getItem() : installed.getItem(), handler.getStackInSlot(0).getItem());
	}

	private static Stream<Arguments> scenarios() {
		return Stream.of(HostKind.values()).flatMap(
				host -> Stream.of(AddonKind.values()).flatMap(addon -> Stream.of(false, true).map(flag -> Arguments.of(new Compatibility(host, addon), flag))));
	}

	private static Stream<Compatibility> tankReplacements() {
		return Stream.of(AddonKind.values()).map(addon -> new Compatibility(HostKind.FLUID, addon));
	}

	private static IStorageWrapper storage(HostKind host) {
		IStorageWrapper storage = host == HostKind.ITEM ? mock(StorageWrapper.class, CALLS_REAL_METHODS) : mock(IStorageWrapper.class, CALLS_REAL_METHODS);
		if (host == HostKind.FLUID) {
			when(storage.getSupportedContents()).thenReturn(Set.of(StorageContentsType.FLUID));
		}
		doReturn(Component.literal("Test storage")).when(storage).getDisplayName();
		UpgradeHandler handler = host == HostKind.FLUID ? new TankUpgradeHandler(1, storage, new ContainerContents(), () -> {
		}, () -> {
		}) {
			@Override
			protected void onContentsChanged(int slot, ItemStack previousContents) {
			}
		} : new UpgradeHandler(1, storage, new ContainerContents(), () -> {
		}, () -> {
		}) {
			@Override
			protected void onContentsChanged(int slot, ItemStack previousContents) {
			}
		};
		doReturn(handler).when(storage).getUpgradeHandler();
		return storage;
	}

	private enum HostKind {
		DEFAULT, ITEM, FLUID
	}

	private record Compatibility(HostKind host, AddonKind addon) {
		boolean allowed() {
			return host == HostKind.DEFAULT || addon == AddonKind.BOTH || (addon == AddonKind.FLUID) == (host == HostKind.FLUID);
		}
	}

	private enum AddonKind {
		DEFAULT_ITEM, FLUID, BOTH;

		AddonUpgradeItem create() {
			AddonUpgradeItem item = mock(AddonUpgradeItem.class, CALLS_REAL_METHODS);
			@SuppressWarnings("unchecked")
			Holder.Reference<Item> holder = mock(Holder.Reference.class);
			when(holder.value()).thenReturn(item);
			when(holder.components()).thenReturn(Items.DIAMOND.components());
			doReturn(holder).when(item).builtInRegistryHolder();
			if (this != DEFAULT_ITEM) {
				doReturn(this == FLUID ? Set.of(StorageContentsType.FLUID) : Set.of(StorageContentsType.ITEM, StorageContentsType.FLUID)).when(item)
						.getSupportedContents();
			}
			return item;
		}
	}

	private static class AddonUpgradeItem extends Item implements IUpgradeItem<IUpgradeWrapper> {
		private static final UpgradeType<IUpgradeWrapper> TYPE = new UpgradeType<>((storage, stack, save) -> mock(IUpgradeWrapper.class));

		AddonUpgradeItem() {
			super(new Properties());
		}

		@Override
		public UpgradeType<IUpgradeWrapper> getType() {
			return TYPE;
		}

		@Override
		public List<UpgradeConflictDefinition> getUpgradeConflicts() {
			return List.of();
		}

		@Override
		public int getUpgradesPerStorage(String storageType) {
			return Integer.MAX_VALUE;
		}

		@Override
		public int getUpgradesInGroupPerStorage(String storageType) {
			return Integer.MAX_VALUE;
		}

		@Override
		public Component getName() {
			return Component.literal("Addon upgrade");
		}
	}
}
