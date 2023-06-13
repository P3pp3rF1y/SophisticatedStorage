package net.p3pp3rf1y.sophisticatedstorage.upgrades.compression;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryPartitioner;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.MathHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CompressionInventoryPartTest {
	private static MockedStatic<RecipeHelper> recipeHelperMock;
	private static MockedStatic<SophisticatedStorage> ss;

	@BeforeAll
	public static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();

		recipeHelperMock = Mockito.mockStatic(RecipeHelper.class);

		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(Items.IRON_NUGGET, RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)).thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.IRON_INGOT), Collections.emptyList()));
		recipeHelperMock.when(() -> RecipeHelper.getCompactingResult(Items.IRON_INGOT, RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)).thenReturn(AccessHelper.initCompactingResult(new ItemStack(Items.IRON_BLOCK), Collections.emptyList()));

		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(any(Item.class))).thenReturn(Set.of(RecipeHelper.CompactingShape.NONE));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(Items.IRON_NUGGET)).thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getItemCompactingShapes(Items.IRON_INGOT)).thenReturn(Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));

		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(any(Item.class))).thenReturn(RecipeHelper.UncompactingResult.EMPTY);
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(Items.IRON_BLOCK)).thenReturn(new RecipeHelper.UncompactingResult(Items.IRON_INGOT, RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
		recipeHelperMock.when(() -> RecipeHelper.getUncompactingResult(Items.IRON_INGOT)).thenReturn(new RecipeHelper.UncompactingResult(Items.IRON_NUGGET, RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));

		ss = Mockito.mockStatic(SophisticatedStorage.class);
		ss.when(() -> SophisticatedStorage.getRL(anyString())).thenAnswer(i -> new ResourceLocation(i.getArgument(0)));
	}

	@BeforeEach
	public void testSetup() throws Exception {
		MockitoAnnotations.openMocks(this).close();
	}

	@AfterAll
	public static void tearDown() {
		recipeHelperMock.close();
		ss.close();
	}

	private InventoryHandler getFilledInventoryHandler(Map<Integer, ItemStack> slotStacks, int baseSlotLimit) {
		InventoryHandler inventoryHandler = Mockito.mock(InventoryHandler.class);
		when(inventoryHandler.getBaseStackLimit(any(ItemStack.class))).thenAnswer(i -> {
			ItemStack stack = i.getArgument(0);
			int limit = MathHelper.intMaxCappedMultiply(stack.getMaxStackSize(), (baseSlotLimit / 64));
			int remainder = baseSlotLimit % 64;
			if (remainder > 0) {
				limit = MathHelper.intMaxCappedAddition(limit, remainder * stack.getMaxStackSize() / 64);
			}
			return limit;
		});
		when(inventoryHandler.getBaseSlotLimit()).thenReturn(baseSlotLimit);

		Map<Integer, ItemStack> internalStacks = new HashMap<>();

		doAnswer(i -> {
			internalStacks.put(i.getArgument(0), i.getArgument(1));
			return null;
		}).when(inventoryHandler).setSlotStack(anyInt(), any(ItemStack.class));

		when(inventoryHandler.getSlotStack(anyInt())).thenAnswer(i -> {
			int slot = i.getArgument(0);
			return internalStacks.containsKey(slot) ? internalStacks.get(slot) : slotStacks.get(slot);
		});

		return inventoryHandler;
	}

	private static MemorySettingsCategory getMemorySettings(InventoryHandler invHandler, Map<Integer, ItemStack> slotFilterStacks) {
		MemorySettingsCategory memorySettingsCategory = Mockito.spy(new MemorySettingsCategory(() -> invHandler, new CompoundTag(), compoundTag -> {}));
		when(memorySettingsCategory.getSlotFilterStack(anyInt(), anyBoolean())).thenAnswer(i -> Optional.ofNullable(slotFilterStacks.get((int) i.getArgument(0))));
		return memorySettingsCategory;
	}

	@ParameterizedTest
	@MethodSource("compactsStacksOnInit")
	void compactsStacksOnInit(Map<Integer, ItemStack> slotStacksInput, Map<Integer, ItemStack> slotStacksModified, int baseSlotLimit) {
		InventoryHandler invHandler = getFilledInventoryHandler(slotStacksInput, baseSlotLimit);
		int minSlot = Collections.min(slotStacksInput.keySet());

		initCompressionInventoryPart(slotStacksInput, invHandler, minSlot);

		assertInternalStacks(slotStacksModified, invHandler);
	}

	private CompressionInventoryPart initCompressionInventoryPart(Map<Integer, ItemStack> slotStacksInput, InventoryHandler invHandler, Supplier<MemorySettingsCategory> getMemorySettings) {
		return initCompressionInventoryPart(invHandler, new InventoryPartitioner.SlotRange(Collections.min(slotStacksInput.keySet()), Collections.min(slotStacksInput.keySet()) + slotStacksInput.size()), getMemorySettings);
	}

	private CompressionInventoryPart initCompressionInventoryPart(Map<Integer, ItemStack> slotStacksInput, InventoryHandler invHandler, int minSlot) {
		return initCompressionInventoryPart(invHandler, new InventoryPartitioner.SlotRange(minSlot, minSlot + slotStacksInput.size()), () -> getMemorySettings(invHandler, Map.of()));
	}

	private CompressionInventoryPart initCompressionInventoryPart(InventoryHandler invHandler, InventoryPartitioner.SlotRange slotRange, Supplier<MemorySettingsCategory> getMemorySettings) {
		CompressionInventoryPart spiedPart = spy(new CompressionInventoryPart(invHandler, slotRange, getMemorySettings));
		doReturn(Optional.empty()).when(spiedPart).getDecompressionResultFromConfig(any(Item.class));
		spiedPart.onInit();
		return spiedPart;
	}

	private static void assertInternalStacks(Map<Integer, ItemStack> slotStacksModified, InventoryHandler invHandler) {
		boolean matching = true;
		Map<Integer, ItemStack> updatedInternalStacks = new LinkedHashMap<>();

		ArgumentCaptor<Integer> intCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<ItemStack> stackCaptor = ArgumentCaptor.forClass(ItemStack.class);
		verify(invHandler, times(slotStacksModified.size())).setSlotStack(intCaptor.capture(), stackCaptor.capture());
		List<ItemStack> updatedStacks = stackCaptor.getAllValues();
		List<Integer> updatedSlots = intCaptor.getAllValues();
		for (int i = 0; i < updatedSlots.size(); i++) {
			ItemStack updatedStack = updatedStacks.get(i);
			updatedInternalStacks.put(i, updatedStack);
			if (!ItemStack.matches(slotStacksModified.get(updatedSlots.get(i)), updatedStack)) {
				matching = false;
			}
		}

		if (!matching) {
			assertionFailure().message("Calculated stacks don't equal")
					.expected(slotStacksModified)
					.actual(updatedInternalStacks)
					.buildAndThrow();
		}
	}

	public static Object[][] compactsStacksOnInit() {
		return new Object[][] {
				{
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 10), 2, ItemStack.EMPTY),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 1)),
						64
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 20), 2, new ItemStack(Items.IRON_NUGGET, 40)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 6), 2, new ItemStack(Items.IRON_NUGGET, 4)),
						64
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 4000), 1, new ItemStack(Items.IRON_INGOT, 4000), 2, new ItemStack(Items.IRON_NUGGET, 4000)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 4096), 1, new ItemStack(Items.IRON_INGOT, 3580), 2, new ItemStack(Items.IRON_NUGGET, 4)),
						64 * 64
				}
		};
	}

	@ParameterizedTest
	@MethodSource("calculatedStacksCorrectOnInit")
	void calculatedStacksCorrectOnInit(Map<Integer, ItemStack> slotStacksInput, Map<Integer, ItemStack> calculatedStacks, int baseSlotLimit) {
		InventoryHandler invHandler = getFilledInventoryHandler(slotStacksInput, baseSlotLimit);
		int minSlot = Collections.min(slotStacksInput.keySet());

		Map<Integer, ItemStack> internalStacks = new HashMap<>();

		doAnswer(i -> {
			internalStacks.put(i.getArgument(0), i.getArgument(1));
			return null;
		}).when(invHandler).setSlotStack(anyInt(), any(ItemStack.class));

		when(invHandler.getSlotStack(anyInt())).thenAnswer(i -> {
			int slot = i.getArgument(0);
			return internalStacks.containsKey(slot) ? internalStacks.get(slot) : slotStacksInput.get(slot);
		});

		CompressionInventoryPart part = initCompressionInventoryPart(slotStacksInput, invHandler, minSlot);

		assertCalculatedStacks(calculatedStacks, minSlot, part);
	}

	public static Object[][] calculatedStacksCorrectOnInit() {
		return new Object[][] {
				{
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 10), 2, ItemStack.EMPTY),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 10), 2, new ItemStack(Items.IRON_NUGGET, 90)),
						64
				},
				{
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.CLAY, 10), 2, ItemStack.EMPTY),
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.CLAY, 10), 2, ItemStack.EMPTY),
						64
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 20), 2, new ItemStack(Items.IRON_NUGGET, 40)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 33), 2, new ItemStack(Items.IRON_NUGGET, 301)),
						64
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 4000), 1, new ItemStack(Items.IRON_INGOT, 4000), 2, new ItemStack(Items.IRON_NUGGET, 4000)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 4096), 1, new ItemStack(Items.IRON_INGOT, 40444), 2, new ItemStack(Items.IRON_NUGGET, 364000)),
						64 * 64
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 4096), 1, new ItemStack(Items.IRON_INGOT, 4096), 2, new ItemStack(Items.IRON_NUGGET, 4096)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 4096), 1, new ItemStack(Items.IRON_INGOT, 40960), 2, new ItemStack(Items.IRON_NUGGET, 372736)),
						64 * 64
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, new ItemStack(Items.IRON_INGOT, Integer.MAX_VALUE - 64), 2, new ItemStack(Items.IRON_NUGGET, Integer.MAX_VALUE - 64)),
						64 * 64 * 64 * 64 * 64
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, new ItemStack(Items.IRON_INGOT, 1073741824 - 48), 2, new ItemStack(Items.IRON_NUGGET, 8)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, new ItemStack(Items.IRON_INGOT, Integer.MAX_VALUE - 48), 2, new ItemStack(Items.IRON_NUGGET, Integer.MAX_VALUE - 64)),
						64 * 64 * 64 * 64 * 64
				}
		};
	}

	@ParameterizedTest
	@MethodSource("extractItemUpdatesStacks")
	void extractItemUpdatesStacks(Map<Integer, ItemStack> internalStacksBefore, int baseSlotLimit, int extractSlot, int extractAmount, ItemStack extractResult, Map<Integer, ItemStack> internalStacksUpdated, Map<Integer, ItemStack> calculatedStacksAfter) {
		InventoryHandler invHandler = getFilledInventoryHandler(internalStacksBefore, baseSlotLimit);
		int minSlot = Collections.min(internalStacksBefore.keySet());

		CompressionInventoryPart part = initCompressionInventoryPart(internalStacksBefore, invHandler, minSlot);

		ItemStack result = part.extractItem(extractSlot, extractAmount, false);

		assertStackEquals(extractResult, result, "Extract result doesn't match");
		assertCalculatedStacks(calculatedStacksAfter, minSlot, part);
		assertInternalStacks(internalStacksUpdated, invHandler);
	}

	public static Object[][] extractItemUpdatesStacks() {
		return new Object[][] {
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 1), 2, ItemStack.EMPTY),
						64,
						1,
						10,
						new ItemStack(Items.IRON_INGOT, 10),
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY),
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY)
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 4), 2, new ItemStack(Items.IRON_NUGGET, 5)),
						64,
						1,
						10,
						new ItemStack(Items.IRON_INGOT, 10),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 3)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 21), 2, new ItemStack(Items.IRON_NUGGET, 194))
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 289), 1, new ItemStack(Items.IRON_INGOT, 5), 2, new ItemStack(Items.IRON_NUGGET, 3)),
						64 * 8,
						0,
						300,
						new ItemStack(Items.IRON_BLOCK, 289),
						Map.of(0, ItemStack.EMPTY),
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 5), 2, new ItemStack(Items.IRON_NUGGET, 48))
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 4), 2, new ItemStack(Items.IRON_NUGGET, 5)),
						64,
						1,
						5,
						new ItemStack(Items.IRON_INGOT, 5),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 8)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 26), 2, new ItemStack(Items.IRON_NUGGET, 239))
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						2,
						1,
						new ItemStack(Items.IRON_NUGGET, 1),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 8), 2, new ItemStack(Items.IRON_NUGGET, 8)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 2), 1, new ItemStack(Items.IRON_INGOT, 26), 2, new ItemStack(Items.IRON_NUGGET, 242))
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 4), 2, new ItemStack(Items.IRON_NUGGET, 5)),
						64,
						0,
						4,
						new ItemStack(Items.IRON_BLOCK, 3),
						Map.of(0, ItemStack.EMPTY),
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 4), 2, new ItemStack(Items.IRON_NUGGET, 41))
				},
				{
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						0,
						4,
						ItemStack.EMPTY,
						Map.of(),
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY)
				},
				{
						Map.of(0, new ItemStack(Items.IRON_INGOT, 3), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						1,
						14,
						new ItemStack(Items.IRON_NUGGET, 14),
						Map.of(0, new ItemStack(Items.IRON_INGOT, 1), 1, new ItemStack(Items.IRON_NUGGET, 4)),
						Map.of(0, new ItemStack(Items.IRON_INGOT, 1), 1, new ItemStack(Items.IRON_NUGGET, 13), 2, ItemStack.EMPTY)
				}
		};
	}

	private static void assertStackEquals(ItemStack expected, ItemStack actual, Object message) {
		if (!ItemStack.matches(expected, actual)) {
			assertionFailure().message(message)
					.expected(expected)
					.actual(actual)
					.buildAndThrow();
		}
	}

	private static void assertCalculatedStacks(Map<Integer, ItemStack> calculatedStacksAfter, int minSlot, CompressionInventoryPart part) {
		boolean matching = true;
		Map<Integer, ItemStack> actualCalculatedStacks = new LinkedHashMap<>();
		for (int slot = minSlot; slot < minSlot + calculatedStacksAfter.size(); slot++) {
			ItemStack calculatedStack = part.getStackInSlot(slot, s -> ItemStack.EMPTY);
			actualCalculatedStacks.put(slot, calculatedStack);

			if (!ItemStack.matches(calculatedStack, calculatedStacksAfter.get(slot))) {
				matching = false;
			}
		}
		if (!matching) {
			assertionFailure().message("Calculated stacks don't equal")
					.expected(calculatedStacksAfter)
					.actual(actualCalculatedStacks)
					.buildAndThrow();
		}
	}

	@ParameterizedTest
	@MethodSource("simulatedExtractItemDoesNotUpdateStacks")
	void simulatedExtractItemDoesNotUpdateStacks(Map<Integer, ItemStack> internalStacksBefore, Map<Integer, ItemStack> calculatedStacksBefore, int baseSlotLimit, int extractSlot, int extractAmount, ItemStack extractResult) {
		InventoryHandler invHandler = getFilledInventoryHandler(internalStacksBefore, baseSlotLimit);
		int minSlot = Collections.min(internalStacksBefore.keySet());

		CompressionInventoryPart part = initCompressionInventoryPart(internalStacksBefore, invHandler, minSlot);

		ItemStack result = part.extractItem(extractSlot, extractAmount, true);

		assertStackEquals(extractResult, result, "Extract result doesn't match");
		assertCalculatedStacks(calculatedStacksBefore, minSlot, part);
		verify(invHandler, never()).setSlotStack(anyInt(), any(ItemStack.class));
	}

	public static Object[][] simulatedExtractItemDoesNotUpdateStacks() {
		return new Object[][] {
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 1), 2, ItemStack.EMPTY),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 10), 2, new ItemStack(Items.IRON_NUGGET, 90)),
						64,
						1,
						10,
						new ItemStack(Items.IRON_INGOT, 10)
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 4), 2, new ItemStack(Items.IRON_NUGGET, 5)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 31), 2, new ItemStack(Items.IRON_NUGGET, 284)),
						64,
						1,
						10,
						new ItemStack(Items.IRON_INGOT, 10)
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 289), 1, new ItemStack(Items.IRON_INGOT, 5), 2, new ItemStack(Items.IRON_NUGGET, 3)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 289), 1, new ItemStack(Items.IRON_INGOT, 2606), 2, new ItemStack(Items.IRON_NUGGET, 23457)),
						64 * 8,
						0,
						300,
						new ItemStack(Items.IRON_BLOCK, 289)
				}
		};
	}

	@ParameterizedTest
	@MethodSource("insertItemUpdatesStacks")
	void insertItemUpdatesStacks(Map<Integer, ItemStack> internalStacksBefore, int baseSlotLimit, int insertSlot, ItemStack stack, ItemStack insertResult, Map<Integer, ItemStack> internalStacksAfter, Map<Integer, ItemStack> calculatedStacksAfter) {
		InventoryHandler invHandler = getFilledInventoryHandler(internalStacksBefore, baseSlotLimit);
		int minSlot = Collections.min(internalStacksBefore.keySet());

		CompressionInventoryPart part = initCompressionInventoryPart(internalStacksBefore, invHandler, minSlot);

		ItemStack result = part.insertItem(insertSlot, stack, false, (slot, itemStack, simulate) -> ItemStack.EMPTY);

		assertStackEquals(insertResult, result, "Insert result doesn't match");
		assertCalculatedStacks(calculatedStacksAfter, minSlot, part);
		assertInternalStacks(internalStacksAfter, invHandler);
	}

	@Test
	void insertNotMatchingStackReturnsBackWithoutChanging() {
		insertItemUpdatesStacks(
				Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 2), 2, new ItemStack(Items.IRON_NUGGET, 1)),
				64, 2, new ItemStack(Items.GOLD_NUGGET, 100), new ItemStack(Items.GOLD_NUGGET, 100),
				Map.of(), Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 2), 2, new ItemStack(Items.IRON_NUGGET, 19))
		);
	}

	@Test
	void insertDecompressibleItemJustSetsItInSlotAndDoesntAffectOtherSlots() {
		insertItemUpdatesStacks(
				Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
				64, 1, new ItemStack(Items.GOLD_NUGGET, 64), ItemStack.EMPTY,
				Map.of(1, new ItemStack(Items.GOLD_NUGGET, 64)),
				Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.GOLD_NUGGET, 64), 2, ItemStack.EMPTY)
		);
	}

	public static Object[][] insertItemUpdatesStacks() {
		return new Object[][] {
				{
						Map.of(2, ItemStack.EMPTY, 1, ItemStack.EMPTY, 0, ItemStack.EMPTY),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 100),
						ItemStack.EMPTY,
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 1), 1, new ItemStack(Items.IRON_INGOT, 2), 0, new ItemStack(Items.IRON_BLOCK, 1)),
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 100), 1, new ItemStack(Items.IRON_INGOT, 11), 0, new ItemStack(Items.IRON_BLOCK, 1))
				},
				{
						Map.of(2, ItemStack.EMPTY, 1, ItemStack.EMPTY, 0, ItemStack.EMPTY),
						64,
						1,
						new ItemStack(Items.IRON_NUGGET, 100),
						ItemStack.EMPTY,
						Map.of(1, new ItemStack(Items.IRON_NUGGET, 1), 0, new ItemStack(Items.IRON_INGOT, 11)),
						Map.of(2, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_NUGGET, 100), 0, new ItemStack(Items.IRON_INGOT, 11))
				},
				{
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 8), 1, new ItemStack(Items.IRON_INGOT, 8), 0, new ItemStack(Items.IRON_BLOCK, 63)),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 1000),
						new ItemStack(Items.IRON_NUGGET, 359),
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 64), 1, new ItemStack(Items.IRON_INGOT, 64), 0, new ItemStack(Items.IRON_BLOCK, 64)),
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 5824), 1, new ItemStack(Items.IRON_INGOT, 640), 0, new ItemStack(Items.IRON_BLOCK, 64)),
				},
				{
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 5), 1, new ItemStack(Items.IRON_INGOT, 4), 0, new ItemStack(Items.IRON_BLOCK, 3)),
						64,
						1,
						new ItemStack(Items.IRON_INGOT, 32),
						ItemStack.EMPTY,
						Map.of(1, ItemStack.EMPTY, 0, new ItemStack(Items.IRON_BLOCK, 7)),
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 572), 1, new ItemStack(Items.IRON_INGOT, 63), 0, new ItemStack(Items.IRON_BLOCK, 7))
				},
				{
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 8), 1, new ItemStack(Items.IRON_INGOT, 8), 0, new ItemStack(Items.IRON_BLOCK, 73741824)),
						64 * 64 * 64 * 64 * 64,
						0,
						new ItemStack(Items.IRON_BLOCK, 1_000_000_001),
						new ItemStack(Items.IRON_BLOCK, 1),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, new ItemStack(Items.IRON_INGOT, Integer.MAX_VALUE - 64), 2, new ItemStack(Items.IRON_NUGGET, Integer.MAX_VALUE - 64))
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, new ItemStack(Items.IRON_INGOT, 73741824 - 48), 2, new ItemStack(Items.IRON_NUGGET, 8)),
						64 * 64 * 64 * 64 * 64,
						1,
						new ItemStack(Items.IRON_INGOT, 1_000_000_000),
						ItemStack.EMPTY,
						Map.of(1, new ItemStack(Items.IRON_INGOT, 1073741824 - 48)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1073741824), 1, new ItemStack(Items.IRON_INGOT, Integer.MAX_VALUE - 48), 2, new ItemStack(Items.IRON_NUGGET, Integer.MAX_VALUE - 64))
				},
				{
						Map.of(0, new ItemStack(Items.IRON_INGOT, 1), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						1,
						new ItemStack(Items.IRON_NUGGET, 9),
						ItemStack.EMPTY,
						Map.of(0, new ItemStack(Items.IRON_INGOT, 2)),
						Map.of(0, new ItemStack(Items.IRON_INGOT, 2), 1, new ItemStack(Items.IRON_NUGGET, 18), 2, ItemStack.EMPTY)
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 1),
						ItemStack.EMPTY,
						Map.of(2, new ItemStack(Items.IRON_NUGGET, 1)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 9), 2, new ItemStack(Items.IRON_NUGGET, 82))
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 7), 2, ItemStack.EMPTY),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 9),
						ItemStack.EMPTY,
						Map.of(1, new ItemStack(Items.IRON_INGOT, 8)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 17), 2, new ItemStack(Items.IRON_NUGGET, 153))
				}
		};
	}

	@Test
	void extractItemAllowsDifferentItemToBeInsertedIfExtractedFully() {
		Map<Integer, ItemStack> slotStacksInput = Map.of(0, new ItemStack(Items.IRON_BLOCK, 63), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY);
		InventoryHandler invHandler = getFilledInventoryHandler(slotStacksInput, 64);

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new InventoryPartitioner.SlotRange(0, 3), () -> getMemorySettings(invHandler, Map.of()));
		part.extractItem(0, 63, false);

		ItemStack insertResult = part.insertItem(1, new ItemStack(Items.GOLD_NUGGET, 10), false, (s, st, sim) -> ItemStack.EMPTY);

		assertEquals(ItemStack.EMPTY, insertResult);
	}

	@Test
	void extractItemDoesntAllowDifferentInMemorizedSlotsEvenIfExtractedFully() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, new ItemStack(Items.IRON_BLOCK, 32), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), 64);
		MemorySettingsCategory memorySettings = getMemorySettings(invHandler, Map.of());
		when(memorySettings.getSlotFilterStack(eq(0), anyBoolean())).thenReturn(Optional.of(new ItemStack(Items.IRON_BLOCK)));

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new InventoryPartitioner.SlotRange(0, 3), () -> memorySettings);
		part.extractItem(0, 32, false);

		assertStackEquals(new ItemStack(Items.GOLD_BLOCK, 32), part.insertItem(1, new ItemStack(Items.GOLD_BLOCK, 32), true, (s, st, sim) -> ItemStack.EMPTY), "Insert result does not equal");
	}

	@Test
	void properlyInitializesItemsBasedOnMemorizedSlots() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), 64);
		MemorySettingsCategory memorySettings = getMemorySettings(invHandler, Map.of());
		when(memorySettings.getSlotFilterStack(1, true)).thenReturn(Optional.of(new ItemStack(Items.IRON_BLOCK)));

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new InventoryPartitioner.SlotRange(0, 3), () -> memorySettings);

		assertStackEquals(new ItemStack(Items.GOLD_BLOCK, 32), part.insertItem(1, new ItemStack(Items.GOLD_BLOCK, 32), true, (s, st, sim) -> ItemStack.EMPTY), "Insert result does not equal");
		assertStackEquals(ItemStack.EMPTY, part.insertItem(1, new ItemStack(Items.IRON_BLOCK, 32), true, (s, st, sim) -> ItemStack.EMPTY), "Insert result does not equal");
	}

	@ParameterizedTest
	@MethodSource("setStackInSlotUpdatesStacks")
	void setStackInSlotUpdatesStacks(Map<Integer, ItemStack> internalStacksBefore, int baseSlotLimit, int insertSlot, ItemStack stack, Map<Integer, ItemStack> internalStacksAfter, Map<Integer, ItemStack> calculatedStacksAfter) {
		InventoryHandler invHandler = getFilledInventoryHandler(internalStacksBefore, baseSlotLimit);
		int minSlot = Collections.min(internalStacksBefore.keySet());

		CompressionInventoryPart part = initCompressionInventoryPart(internalStacksBefore, invHandler, minSlot);

		part.setStackInSlot(insertSlot, stack, (slot, itemStack) -> {});

		assertCalculatedStacks(calculatedStacksAfter, minSlot, part);
		assertInternalStacks(internalStacksAfter, invHandler);
	}

	public static Object[][] setStackInSlotUpdatesStacks() {
		return new Object[][] {
				{
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 100),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 2), 2, new ItemStack(Items.IRON_NUGGET, 1)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 1), 1, new ItemStack(Items.IRON_INGOT, 11), 2, new ItemStack(Items.IRON_NUGGET, 100))
				},
				{
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						1,
						new ItemStack(Items.IRON_NUGGET, 100),
						Map.of(0, new ItemStack(Items.IRON_INGOT, 11), 1, new ItemStack(Items.IRON_NUGGET, 1)),
						Map.of(0, new ItemStack(Items.IRON_INGOT, 11), 1, new ItemStack(Items.IRON_NUGGET, 100), 2, ItemStack.EMPTY)
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 63), 1, new ItemStack(Items.IRON_INGOT, 8), 2, new ItemStack(Items.IRON_NUGGET, 8)),
						64,
						2,
						new ItemStack(Items.IRON_NUGGET, 1000),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 12), 1, new ItemStack(Items.IRON_INGOT, 3), 2, new ItemStack(Items.IRON_NUGGET, 1)),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 12), 1, new ItemStack(Items.IRON_INGOT, 111), 2, new ItemStack(Items.IRON_NUGGET, 1000)),
				},
				{
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 3), 1, new ItemStack(Items.IRON_INGOT, 4), 2, new ItemStack(Items.IRON_NUGGET, 5)),
						64,
						1,
						new ItemStack(Items.IRON_INGOT, 1),
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 1)),
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_INGOT, 1), 2, new ItemStack(Items.IRON_NUGGET, 14))
				}
		};
	}

	@Test
	void puttingDamagedDecompressibleItemInDoesntHealIt() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY, 2, ItemStack.EMPTY), 64);
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new InventoryPartitioner.SlotRange(minSlot, minSlot + 3), () -> getMemorySettings(invHandler, Map.of()));

		ItemStack damagedItem = new ItemStack(Items.NETHERITE_AXE);
		damagedItem.setDamageValue(10);
		part.insertItem(1, damagedItem, false, (s, st, sim) -> ItemStack.EMPTY);

		assertStackEquals(damagedItem, part.getStackInSlot(1, s -> ItemStack.EMPTY), "Damaged item doesn't match");
	}

	@Test
	void initializingWithDamagedDecompressibleItemDoesntHealIt() {
		ItemStack damagedItem = new ItemStack(Items.NETHERITE_AXE);
		damagedItem.setDamageValue(10);

		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, ItemStack.EMPTY, 1, damagedItem, 2, ItemStack.EMPTY), 64);
		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new InventoryPartitioner.SlotRange(0, 3), () -> getMemorySettings(invHandler, Map.of()));

		assertStackEquals(damagedItem, part.getStackInSlot(1, s -> ItemStack.EMPTY), "Damaged item doesn't match");
	}

	@Test
	void extractingDecompressibleItemWorks() {
		ItemStack damagedItem = new ItemStack(Items.NETHERITE_AXE);
		damagedItem.setDamageValue(10);

		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, ItemStack.EMPTY, 1, damagedItem, 2, ItemStack.EMPTY), 64);
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new InventoryPartitioner.SlotRange(minSlot, minSlot + 3), () -> getMemorySettings(invHandler, Map.of()));

		ItemStack damagedItemToMatch = new ItemStack(Items.NETHERITE_AXE);
		damagedItemToMatch.setDamageValue(10);
		assertStackEquals(damagedItemToMatch, part.extractItem(1, 1, false), "Extracted item doesn't match");
	}

	@Test
	void extractingPartOfDecompressibleStackCorrectlyLeavesTheRestIn() {
		InventoryHandler invHandler = getFilledInventoryHandler(Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.COBBLESTONE, 10), 2, ItemStack.EMPTY), 64);
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new InventoryPartitioner.SlotRange(minSlot, minSlot + 3), () -> getMemorySettings(invHandler, Map.of()));

		assertStackEquals(new ItemStack(Items.COBBLESTONE, 1), part.extractItem(1, 1, false), "Extracted item doesn't match");
		assertStackEquals(new ItemStack(Items.COBBLESTONE, 9), part.getStackInSlot(1, s -> ItemStack.EMPTY), "Item left in slot doesn't match");
	}

	@ParameterizedTest
	@MethodSource("stackLimitsAreSetCorrectlyOnInit")
	void stackLimitsAreSetCorrectlyOnInit(StackLimitsAreSetCorrectlyOnInitParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.stacks(), params.baseLimit());
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new InventoryPartitioner.SlotRange(minSlot, minSlot + params.stacks().size()), () -> getMemorySettings(invHandler, Map.of()));

		params.expectedLimits().forEach((slot, stackLimit) -> assertEquals(stackLimit.getRight(), part.getStackLimit(slot, stackLimit.getLeft()), "Stack limit doesn't match"));
	}

	private record StackLimitsAreSetCorrectlyOnInitParams(Map<Integer, ItemStack> stacks, int baseLimit,
														  Map<Integer, Pair<ItemStack, Integer>> expectedLimits) {}

	private static List<StackLimitsAreSetCorrectlyOnInitParams> stackLimitsAreSetCorrectlyOnInit() {
		return List.of(
				new StackLimitsAreSetCorrectlyOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK), 1, ItemStack.EMPTY, 2, ItemStack.EMPTY),
						64,
						Map.of(0, ImmutablePair.of(new ItemStack(Items.IRON_BLOCK), 64), 1, ImmutablePair.of(new ItemStack(Items.IRON_INGOT), 9 * 64 + 64), 2, ImmutablePair.of(new ItemStack(Items.IRON_NUGGET), 9 * 9 * 64 + 9 * 64 + 64))
				),
				new StackLimitsAreSetCorrectlyOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_INGOT), 1, ItemStack.EMPTY),
						64,
						Map.of(0, ImmutablePair.of(new ItemStack(Items.IRON_INGOT), 64), 1, ImmutablePair.of(new ItemStack(Items.IRON_NUGGET), 9 * 64 + 64))
				),
				new StackLimitsAreSetCorrectlyOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_INGOT), 1, ItemStack.EMPTY),
						Integer.MAX_VALUE,
						Map.of(0, ImmutablePair.of(new ItemStack(Items.IRON_INGOT), Integer.MAX_VALUE), 1, ImmutablePair.of(new ItemStack(Items.IRON_NUGGET), Integer.MAX_VALUE))
				),
				new StackLimitsAreSetCorrectlyOnInitParams(
						Map.of(0, new ItemStack(Items.IRON_SWORD), 1, ItemStack.EMPTY),
						64,
						Map.of(0, ImmutablePair.of(new ItemStack(Items.IRON_SWORD), 1), 1, ImmutablePair.of(new ItemStack(Items.IRON_SWORD), 0))
				)
		);
	}

	@ParameterizedTest
	@MethodSource("insertingAdditionalUncompressibleItemsProperlyCalculatesCount")
	void insertingAdditionalUncompressibleItemsProperlyCalculatesCount(InsertingAdditionalUncompressibleItemsProperlyCalculatesCountParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.stacks(), params.baseLimit());
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new InventoryPartitioner.SlotRange(minSlot, minSlot + params.stacks().size()), () -> getMemorySettings(invHandler, Map.of()));

		part.insertItem(params.insertedStack.getLeft(), params.insertedStack.getRight(), false, (slot, itemStack, simulate) -> ItemStack.EMPTY);

		assertCalculatedStacks(params.expectedStacksSet(), 0, part);
		assertInternalStacks(params.expectedStacksSet(), invHandler);
	}

	private record InsertingAdditionalUncompressibleItemsProperlyCalculatesCountParams(Map<Integer, ItemStack> stacks, int baseLimit,
																					   Pair<Integer, ItemStack> insertedStack,
																					   Map<Integer, ItemStack> expectedStacksSet) {}

	private static List<InsertingAdditionalUncompressibleItemsProperlyCalculatesCountParams> insertingAdditionalUncompressibleItemsProperlyCalculatesCount() {
		return List.of(
				new InsertingAdditionalUncompressibleItemsProperlyCalculatesCountParams(
						Map.of(0, new ItemStack(Items.SAND, 23), 1, ItemStack.EMPTY),
						64,
						ImmutablePair.of(0, new ItemStack(Items.SAND, 41)),
						Map.of(0, new ItemStack(Items.SAND, 64))
				),
				new InsertingAdditionalUncompressibleItemsProperlyCalculatesCountParams(
						Map.of(0, new ItemStack(Items.SAND, 23), 1, ItemStack.EMPTY),
						256,
						ImmutablePair.of(0, new ItemStack(Items.SAND, 128)),
						Map.of(0, new ItemStack(Items.SAND, 151))
				),
				new InsertingAdditionalUncompressibleItemsProperlyCalculatesCountParams(
						Map.of(0, ItemStack.EMPTY, 1, ItemStack.EMPTY),
						256,
						ImmutablePair.of(0, new ItemStack(Items.SAND, 256)),
						Map.of(0, new ItemStack(Items.SAND, 256))
				)
		);
	}

	@ParameterizedTest
	@MethodSource("extractingFromFullyFilledSlotsProperlyCalculatesCounts")
	void extractingFromFullyFilledSlotsProperlyCalculatesCounts(ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.stacks(), params.baseLimit());
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new InventoryPartitioner.SlotRange(minSlot, minSlot + params.stacks().size()), () -> getMemorySettings(invHandler, Map.of()));

		part.extractItem(params.extractedStack.getLeft(), params.extractedStack.getRight(), false);

		assertCalculatedStacks(params.expectedCalculatedStacks(), 0, part);
	}

	private record ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams(Map<Integer, ItemStack> stacks, int baseLimit,
																				Pair<Integer, Integer> extractedStack,
																				Map<Integer, ItemStack> expectedCalculatedStacks) {}

	private static List<ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams> extractingFromFullyFilledSlotsProperlyCalculatesCounts() {
		return List.of(
				new ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 256)),
						256,
						ImmutablePair.of(1, 64),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 2496))
				),
				new ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 256)),
						256,
						ImmutablePair.of(1, 256),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 2304))
				),
				new ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 256)),
						256,
						ImmutablePair.of(1, 257),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 255), 1, new ItemStack(Items.IRON_INGOT, 2303))
				),
				new ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 256)),
						256,
						ImmutablePair.of(1, 0),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 2560))
				),
				new ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 256)),
						256,
						ImmutablePair.of(1, 256 + 10 * 9),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 246), 1, new ItemStack(Items.IRON_INGOT, 2214))
				),
				new ExtractingFromFullyFilledSlotsProperlyCalculatesCountsParams(
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 256), 2, new ItemStack(Items.IRON_NUGGET, 256)),
						256,
						ImmutablePair.of(2, 256 + 10 * 9),
						Map.of(0, new ItemStack(Items.IRON_BLOCK, 256), 1, new ItemStack(Items.IRON_INGOT, 2550), 2, new ItemStack(Items.IRON_NUGGET, 22950))
				)
		);
	}

	@ParameterizedTest
	@MethodSource("initializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacks")
	void initializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacks(InitializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksParams params) {
		InventoryHandler invHandler = getFilledInventoryHandler(params.stacks(), params.baseLimit());
		int minSlot = 0;

		CompressionInventoryPart part = initCompressionInventoryPart(invHandler, new InventoryPartitioner.SlotRange(minSlot, minSlot + params.stacks().size()), () -> getMemorySettings(invHandler, Map.of()));

		assertCalculatedStacks(params.calculatedStacks(), 0, part);
	}

	private record InitializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksParams(Map<Integer, ItemStack> stacks, int baseLimit, Map<Integer, ItemStack> calculatedStacks) {}

	private static List<InitializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksParams> initializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacks() {
		return List.of(
				new InitializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_AXE), 2, new ItemStack(Items.IRON_INGOT), 3, ItemStack.EMPTY),
						64,
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_AXE), 2, new ItemStack(Items.IRON_INGOT), 3, ItemStack.EMPTY)
				),
				new InitializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksParams(
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_AXE), 2, new ItemStack(Items.IRON_INGOT, 4), 3, new ItemStack(Items.IRON_NUGGET, 3)),
						256,
						Map.of(0, ItemStack.EMPTY, 1, new ItemStack(Items.IRON_AXE), 2, new ItemStack(Items.IRON_INGOT, 4), 3, new ItemStack(Items.IRON_NUGGET, 3))
				),
				new InitializingWithPartiallyNonCompressibleItemsDoesntCrashAndAllowsAccessToNonCompressedStacksParams(
						Map.of(0, new ItemStack(Items.IRON_INGOT, 4), 1, new ItemStack(Items.IRON_NUGGET, 3), 2, ItemStack.EMPTY, 3, new ItemStack(Items.IRON_AXE)),
						256,
						Map.of(0, new ItemStack(Items.IRON_INGOT, 4), 1, new ItemStack(Items.IRON_NUGGET, 3), 2, ItemStack.EMPTY, 3, new ItemStack(Items.IRON_AXE))
				)
		);
	}
}
