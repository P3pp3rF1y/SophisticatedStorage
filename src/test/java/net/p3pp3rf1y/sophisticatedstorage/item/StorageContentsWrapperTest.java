package net.p3pp3rf1y.sophisticatedstorage.item;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.EventListenerHelper;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.GameData;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryPartRegistry;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.block.IStorageBlock;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.compression.CompressionInventoryPart;
import net.p3pp3rf1y.sophisticatedstorage.upgrades.compression.CompressionUpgradeItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class StorageContentsWrapperTest {
	private static StackUpgradeItem stackUpgrade;
	private static CompressionUpgradeItem compressionUpgrade;
	private MockedStatic<RecipeHelper> recipes;
	private Item[] denominations;

	@BeforeAll
	static void setup() throws Exception {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		try (MockedStatic<ObfuscationReflectionHelper> reflection = mockStatic(ObfuscationReflectionHelper.class)) {
			Field field = ItemStack.class.getDeclaredField("capNBT");
			field.setAccessible(true);
			reflection.when(() -> ObfuscationReflectionHelper.findField(ItemStack.class, "capNBT")).thenReturn(field);
			Class.forName("net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey");
		}
		GameData.unfreezeData();
		Config.SERVER_SPEC.setConfig(CommentedConfig.inMemory());
		InventoryPartRegistry.registerFactory(CompressionInventoryPart.NAME, CompressionInventoryPart::new);
		stackUpgrade = new StackUpgradeItem(4, CreativeModeTab.TAB_MISC, Config.SERVER.maxUpgradesPerStorage);
		stackUpgrade.setRegistryName("sophisticatedstorage_test", "stack_upgrade");
		ForgeRegistries.ITEMS.register(stackUpgrade);
		compressionUpgrade = new CompressionUpgradeItem(CreativeModeTab.TAB_MISC);
		compressionUpgrade.setRegistryName("sophisticatedstorage_test", "compression_upgrade");
		ForgeRegistries.ITEMS.register(compressionUpgrade);
		try (MockedStatic<EventListenerHelper> events = mockStatic(EventListenerHelper.class)) {
			events.when(() -> EventListenerHelper.getListenerList(any(Class.class))).thenReturn(new ListenerList());
			Class.forName("net.p3pp3rf1y.sophisticatedstorage.settings.StorageSettingsHandler");
		}
	}

	@BeforeEach
	void setUpRecipes() throws Exception {
		denominations = new Item[] {Items.DIAMOND_BLOCK, Items.IRON_BLOCK, Items.IRON_INGOT, Items.IRON_NUGGET};
		recipes = Mockito.mockStatic(RecipeHelper.class);
		recipes.when(() -> RecipeHelper.getItemCompactingShapes(any(Item.class))).thenReturn(Set.of(RecipeHelper.CompactingShape.NONE));
		recipes.when(() -> RecipeHelper.getUncompactingResult(any(Item.class))).thenReturn(RecipeHelper.UncompactingResult.EMPTY);
		Constructor<RecipeHelper.CompactingResult> resultConstructor = RecipeHelper.CompactingResult.class.getDeclaredConstructor(ItemStack.class, List.class);
		resultConstructor.setAccessible(true);
		for (int i = 1; i < denominations.length; i++) {
			Item lower = denominations[i];
			Item upper = denominations[i - 1];
			RecipeHelper.CompactingShape shape = RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE;
			RecipeHelper.CompactingResult result = resultConstructor.newInstance(new ItemStack(upper), List.of());
			recipes.when(() -> RecipeHelper.getItemCompactingShapes(lower)).thenReturn(Set.of(shape));
			recipes.when(() -> RecipeHelper.getCompactingResult(lower, shape)).thenReturn(result);
			recipes.when(() -> RecipeHelper.getUncompactingResult(upper)).thenReturn(new RecipeHelper.UncompactingResult(lower, shape));
		}
	}

	@AfterEach
	void closeRecipes() {
		recipes.close();
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void showsEveryCalculatedDenominationInSlotOrder(int slots) {
		Item[] items = Arrays.copyOfRange(denominations, 4 - slots, 4);
		CompoundTag saved = getInventoryTag(slots, true, new ItemStack(items[0], 129));
		CompoundTag before = saved.copy();

		List<ItemStack> result = new StorageContentsWrapper(getBarrel(slots), saved).getContents();

		assertEquals(slots, result.size());
		int count = 129;
		for (int i = 0; i < slots; i++) {
			assertSame(items[i], result.get(i).getItem());
			assertEquals(count, result.get(i).getCount());
			count *= 9;
		}
		assertEquals(before, saved, "Preview must not modify synchronized contents");
	}

	@Test
	void calculatesRemaindersWithoutChangingSavedDataOrEarlierPreviews() {
		CompoundTag saved = getInventoryTag(3, true, new ItemStack(Items.IRON_BLOCK),
				new ItemStack(Items.IRON_INGOT, 20), new ItemStack(Items.IRON_NUGGET, 40));
		CompoundTag before = saved.copy();

		List<ItemStack> first = new StorageContentsWrapper(getBarrel(3), saved).getContents();
		assertEquals(List.of(3, 33, 301), first.stream().map(ItemStack::getCount).toList());
		first.get(0).setCount(999);
		List<ItemStack> second = new StorageContentsWrapper(getBarrel(3), saved).getContents();
		assertEquals(List.of(3, 33, 301), second.stream().map(ItemStack::getCount).toList());
		assertEquals(before, saved);
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void directSlotMutationReconcilesInternalCompressionCounts(int slots) {
		Item highest = denominations[4 - slots];
		StorageContentsWrapper preview = new StorageContentsWrapper(getBarrel(slots), getInventoryTag(slots, true, new ItemStack(highest, 2)));
		InventoryHandler handler = preview.getInventoryHandler();
		int lastSlot = slots - 1;
		int originalCount = handler.getStackInSlot(lastSlot).getCount();
		ItemStack removed = handler.getStackInSlot(lastSlot).split(1);

		handler.onContentsChanged(lastSlot);
		assertEquals(originalCount - 1, handler.getStackInSlot(lastSlot).getCount());

		handler.onContentsChanged(lastSlot);
		assertEquals(originalCount - 1, handler.getStackInSlot(lastSlot).getCount());

		ItemStack remainder = handler.extractItemIgnoringLimit(lastSlot, Integer.MAX_VALUE, false);
		assertEquals(originalCount, removed.getCount() + remainder.getCount());
		for (int slot = 0; slot < slots; slot++) {
			assertTrue(handler.getSlotStack(slot).isEmpty());
		}
	}

	@Test
	void reflectsNewContentsAfterSyncAndKeepsOtherBarrelsIndependent() {
		CompoundTag first = getInventoryTag(2, true, new ItemStack(Items.IRON_INGOT, 2));
		CompoundTag second = getInventoryTag(2, true, new ItemStack(Items.IRON_INGOT, 7));
		assertEquals(List.of(2, 18), getCounts(first, 2));
		assertEquals(List.of(7, 63), getCounts(second, 2));
		assertEquals(List.of(2, 18), getCounts(first, 2));
	}

	@Test
	void ordinarySlotsKeepTheirItemsNbtAndFullCounts() {
		ItemStack named = new ItemStack(Items.DIAMOND, 257);
		named.getOrCreateTag().putString("testMarker", "preserved");
		CompoundTag saved = getInventoryTag(4, false, new ItemStack(Items.DIRT), ItemStack.EMPTY,
				named, new ItemStack(Items.DIRT, 3));
		CompoundTag before = saved.copy();
		List<ItemStack> result = new StorageContentsWrapper(getBarrel(4), saved).getContents();
		assertEquals(3, result.size());
		assertSame(Items.DIRT, result.get(0).getItem());
		assertSame(Items.DIAMOND, result.get(1).getItem());
		assertEquals(257, result.get(1).getCount());
		assertEquals("preserved", result.get(1).getTag().getString("testMarker"));
		assertSame(Items.DIRT, result.get(2).getItem(), "Separate slots should not be merged");
		assertEquals(before, saved);
	}

	@Test
	void emptyBarrelsRemainEmptyAndUnpackedItemsAreNotHandled() {
		assertTrue(new StorageContentsWrapper(getBarrel(4), getInventoryTag(4, true)).getContents().isEmpty());
		assertTrue(StorageContentsWrapper.fromStack(new ItemStack(Items.BARREL)).isEmpty());
		assertTrue(StorageContentsWrapper.fromStack(ItemStack.EMPTY).isEmpty());
	}

	@Test
	void restoresAllSavedUpgradeSlotsAndTheBaseTimesUpgradeMultiplier() {
		CompoundTag saved = getInventoryTag(27, false, new ItemStack(Items.DIAMOND, 257));
		addUpgrades(saved);
		CompoundTag before = saved.copy();
		IStorageBlock block = getBarrel(27);
		when(block.getNumberOfUpgradeSlots()).thenReturn(2);
		when(block.getBaseStackSizeMultiplier()).thenReturn(8);

		StorageContentsWrapper preview = new StorageContentsWrapper(block, saved);
		Map<Integer, IUpgradeWrapper> upgrades = preview.getUpgradeHandler().getSlotWrappers();
		assertEquals(Set.of(1, 3), upgrades.keySet());
		assertSame(compressionUpgrade, upgrades.get(1).getUpgradeStack().getItem());
		assertSame(stackUpgrade, upgrades.get(3).getUpgradeStack().getItem());
		assertEquals("saved-upgrade", upgrades.get(3).getUpgradeStack().getTag().getString("testMarker"));
		assertEquals(32, preview.getInventoryHandler().getStackSizeMultiplier());
		assertEquals(before, saved);
		upgrades.get(3).getUpgradeStack().getOrCreateTag().putString("testMarker", "changed-preview");
		assertEquals(before, saved);
	}

	@Test
	void upgradeOnlyBarrelsRetainIconsWhenThereIsNoInventoryTag() {
		CompoundTag saved = getInventoryTag(27, false);
		saved.getCompound("contents").remove("inventory");
		addUpgrades(saved);
		StorageContentsWrapper preview = new StorageContentsWrapper(getBarrel(27), saved);
		assertTrue(preview.getContents().isEmpty());
		assertEquals(2, preview.getUpgradeHandler().getSlotWrappers().size());
	}

	@Test
	void compressedContentsAndUpgradesComeFromTheSameDetachedPreview() {
		CompoundTag saved = getInventoryTag(3, true, new ItemStack(Items.IRON_BLOCK, 129));
		addUpgrades(saved);
		CompoundTag before = saved.copy();
		StorageContentsWrapper preview = new StorageContentsWrapper(getBarrel(3), saved);
		assertEquals(List.of(129, 1161, 10449), preview.getContents().stream().map(ItemStack::getCount).toList());
		assertEquals(2, preview.getUpgradeHandler().getSlotWrappers().size());
		assertEquals(2048, preview.getInventoryHandler().getStackSizeMultiplier());
		assertEquals(before, saved);
	}

	private void addUpgrades(CompoundTag saved) {
		CompoundTag upgrades = new CompoundTag();
		upgrades.putInt("Size", 4);
		ListTag items = new ListTag();
		CompoundTag compression = new ItemStack(compressionUpgrade).save(new CompoundTag());
		compression.putInt("Slot", 1);
		items.add(compression);
		ItemStack stack = new ItemStack(stackUpgrade);
		stack.getOrCreateTag().putString("testMarker", "saved-upgrade");
		CompoundTag stacking = stack.save(new CompoundTag());
		stacking.putInt("Slot", 3);
		items.add(stacking);
		upgrades.put("Items", items);
		saved.getCompound("contents").put("upgradeInventory", upgrades);
		saved.putInt("numberOfUpgradeSlots", 4);
	}

	private List<Integer> getCounts(CompoundTag saved, int slots) {
		return new StorageContentsWrapper(getBarrel(slots), saved).getContents().stream().map(ItemStack::getCount).toList();
	}

	private IStorageBlock getBarrel(int slots) {
		IStorageBlock block = mock(IStorageBlock.class);
		when(block.getNumberOfInventorySlots()).thenReturn(slots);
		when(block.getNumberOfUpgradeSlots()).thenReturn(0);
		when(block.getBaseStackSizeMultiplier()).thenReturn(512);
		return block;
	}

	private CompoundTag getInventoryTag(int slots, boolean compression, ItemStack... stacks) {
		CompoundTag inventory = new CompoundTag();
		ListTag items = new ListTag();
		for (int i = 0; i < stacks.length; i++) {
			if (stacks[i].isEmpty()) {
				continue;
			}
			CompoundTag item = stacks[i].save(new CompoundTag());
			item.putInt("Slot", i);
			item.putInt("realCount", stacks[i].getCount());
			items.add(item);
		}
		inventory.putInt("Size", slots);
		inventory.put("Items", items);
		CompoundTag contents = new CompoundTag();
		contents.put("inventory", inventory);
		if (compression) {
			CompoundTag partitioner = new CompoundTag();
			partitioner.putIntArray("baseIndexes", new int[] {0});
			ListTag parts = new ListTag();
			parts.add(StringTag.valueOf(CompressionInventoryPart.NAME));
			partitioner.put("inventoryPartNames", parts);
			contents.put("partitioner", partitioner);
		}
		CompoundTag wrapper = new CompoundTag();
		wrapper.putInt("numberOfInventorySlots", slots);
		wrapper.putInt("numberOfUpgradeSlots", 0);
		wrapper.put("contents", contents);
		return wrapper;
	}
}
