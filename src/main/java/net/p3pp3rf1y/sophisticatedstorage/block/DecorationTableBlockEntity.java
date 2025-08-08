package net.p3pp3rf1y.sophisticatedstorage.block;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ValueIOHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.PaintbrushItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.util.DecorationHelper;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DecorationTableBlockEntity extends BlockEntity {
	private static final Codec<Map<PartSlot, Boolean>> SLOT_INHERITANCE_CODEC =
			Codec.unboundedMap(PartSlot.CODEC, Codec.BOOL);

	private static final Codec<Map<ResourceLocation, Integer>> REMAINING_PARTS_CODEC =
			Codec.simpleMap(ResourceLocation.CODEC, Codec.INT, StringRepresentable.keys(PartSlot.values())).codec();

	public static final int TOP_INNER_TRIM_SLOT = 0;
	public static final int TOP_TRIM_SLOT = 1;
	public static final int SIDE_TRIM_SLOT = 2;
	public static final int BOTTOM_TRIM_SLOT = 3;
	public static final int TOP_CORE_SLOT = 4;
	public static final int SIDE_CORE_SLOT = 5;
	public static final int BOTTOM_CORE_SLOT = 6;
	public static final int RED_DYE_SLOT = 0;
	public static final int GREEN_DYE_SLOT = 1;
	public static final int BLUE_DYE_SLOT = 2;
	public static final Set<Item> STORAGES_WIHOUT_TOP_INNER_TRIM = Set.of(ModBlocks.BARREL_ITEM.get(), ModBlocks.COPPER_BARREL_ITEM.get(), ModBlocks.IRON_BARREL_ITEM.get(), ModBlocks.GOLD_BARREL_ITEM.get(), ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.NETHERITE_BARREL_ITEM.get(),
			ModBlocks.LIMITED_BARREL_1_ITEM.get(), ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(), ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(), ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get(), ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get(), ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get());

	private static final Map<Predicate<ItemStack>, IItemDecorator> ITEM_DECORATORS = new LinkedHashMap<>();

	public static void registerItemDecorator(Predicate<ItemStack> itemMatcher, IItemDecorator itemDecorator) {
		ITEM_DECORATORS.put(itemMatcher, itemDecorator);
	}

	private final Map<ResourceLocation, Integer> remainingParts = new HashMap<>();

	private final ItemStackHandler decorativeBlocks = new ItemStackHandler(7) {
		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			updateResultAndSetChanged();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.getItem() instanceof BlockItem blockItem && !(stack.getItem() instanceof StorageBlockItem) && Block.isShapeFullBlock(blockItem.getBlock().defaultBlockState().getShape(level, BlockPos.ZERO));
		}
	};

	private final ItemStackHandler dyes = new ItemStackHandler(3) {
		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			updateResultAndSetChanged();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return switch (slot) {
				case RED_DYE_SLOT -> stack.is(Tags.Items.DYES_RED);
				case GREEN_DYE_SLOT -> stack.is(Tags.Items.DYES_GREEN);
				case BLUE_DYE_SLOT -> stack.is(Tags.Items.DYES_BLUE);
				default -> false;
			};
		}
	};

	private ItemStack result = ItemStack.EMPTY;

	private final Map<PartSlot, Boolean> slotMaterialInheritance = new HashMap<>();
	private int accentColor = -1;
	private int mainColor = -1;

	private final Set<ResourceLocation> missingDyes = new HashSet<>();

	public void updateResultAndSetChanged() {
		updateResult();
		setChanged();
	}

	private final ItemStackHandler storageBlock = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			updateResultAndSetChanged();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return ITEM_DECORATORS.keySet().stream().anyMatch(predicate -> predicate.test(stack));
		}
	};

	private void updateResult() {
		missingDyes.clear();
		result = ItemStack.EMPTY;

		ItemStack input = storageBlock.getStackInSlot(0);
		if (input.isEmpty()) {
			return;
		}

		getItemDecorator(input).ifPresent(itemDecorator -> {
			TintDecorationResult decorationResult = decorateItem(itemDecorator, input);
			result = decorationResult.result();
			missingDyes.addAll(calculateMissingDyes(decorationResult.requiredDyeParts()));
		});
	}

	private static Optional<IItemDecorator> getItemDecorator(ItemStack input) {
		return ITEM_DECORATORS.entrySet().stream().filter(e -> e.getKey().test(input)).findFirst().map(Map.Entry::getValue);
	}

	private TintDecorationResult decorateItem(IItemDecorator itemDecorator, ItemStack input) {
		if (itemDecorator.supportsMaterials(input)) {
			Map<BarrelMaterial, ResourceLocation> materialsToApply = getMaterialsToApply(itemDecorator.supportsTopInnerTrim(input));
			if (!materialsToApply.isEmpty()) {
				return new TintDecorationResult(itemDecorator.decorateWithMaterials(input, materialsToApply), Collections.emptyMap());
			}
		}
		if (itemDecorator.supportsTints(input)) {
			return itemDecorator.decorateWithTints(input, mainColor, accentColor);
		}
		return TintDecorationResult.EMPTY;
	}

	public boolean hasMaterials() {
		return !InventoryHelper.isEmpty(decorativeBlocks);
	}

	public List<ItemStack> getDecoratedPreviewStacks() {
		ItemStack input = storageBlock.getStackInSlot(0);
		return getItemDecorator(input).map(itemDecorator -> {
			List<ItemStack> previewStacks = new ArrayList<>();
			itemDecorator.getPreviewStackInputs(input, hasMaterials()).forEach(stack -> getItemDecorator(stack).ifPresent(inputItemDecorator -> {
				TintDecorationResult decorationResult = decorateItem(inputItemDecorator, stack);
				if (!decorationResult.result().isEmpty()) {
					previewStacks.add(decorationResult.result());
				}
			}));
			return previewStacks;
		}).orElse(Collections.emptyList());
	}

	public static boolean allMaterialsMatch(Map<BarrelMaterial, ResourceLocation> newMaterials, Map<BarrelMaterial, ResourceLocation> currentMaterials) {
		if (newMaterials.size() != currentMaterials.size()) {
			return false;
		}

		for (Map.Entry<BarrelMaterial, ResourceLocation> entry : newMaterials.entrySet()) {
			if (!entry.getValue().equals(currentMaterials.get(entry.getKey()))) {
				return false;
			}
		}

		return true;
	}

	private Set<ResourceLocation> calculateMissingDyes(Map<TagKey<Item>, Integer> requiredDyeParts) {
		Set<ResourceLocation> missingDyes = new HashSet<>();
		if (!dyes.getStackInSlot(RED_DYE_SLOT).isEmpty() && !dyes.getStackInSlot(GREEN_DYE_SLOT).isEmpty() && !dyes.getStackInSlot(BLUE_DYE_SLOT).isEmpty()) {
			return missingDyes;
		}

		Map<ResourceLocation, Integer> partsNeeded =
				requiredDyeParts.entrySet().stream().map(entry -> Map.entry(entry.getKey().location(), entry.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		for (Map.Entry<ResourceLocation, Integer> entry : partsNeeded.entrySet()) {
			if (entry.getKey().equals(Tags.Items.DYES_RED.location()) && dyes.getStackInSlot(RED_DYE_SLOT).isEmpty()) {
				missingDyes.add(entry.getKey());
			} else if (entry.getKey().equals(Tags.Items.DYES_GREEN.location()) && dyes.getStackInSlot(GREEN_DYE_SLOT).isEmpty()) {
				missingDyes.add(entry.getKey());
			} else if (entry.getKey().equals(Tags.Items.DYES_BLUE.location()) && dyes.getStackInSlot(BLUE_DYE_SLOT).isEmpty()) {
				missingDyes.add(entry.getKey());
			}
		}
		return missingDyes;
	}

	private static Map<TagKey<Item>, Integer> calculateRequiredDyes(int mainColorToSet, int accentColorToSet, int currentMainColor, int currentAccentColor) {
		return DecorationHelper.getDyePartsNeeded(mainColorToSet, accentColorToSet, currentMainColor, currentAccentColor);
	}

	public Set<ResourceLocation> getMissingDyes() {
		return missingDyes;
	}

	private void setMaterialsFromDecorativeBlocks(Map<BarrelMaterial, ResourceLocation> materials, boolean supportsInnerTrim) {
		ResourceLocation topInnerTrimMaterialLocation = setMaterialFromBlock(PartSlot.TOP_INNER_TRIM, null, materials, BarrelMaterial.TOP_INNER_TRIM, supportsInnerTrim);
		ResourceLocation topTrimMaterialLocation = setMaterialFromBlock(PartSlot.TOP_TRIM, topInnerTrimMaterialLocation, materials, BarrelMaterial.TOP_TRIM, true);
		ResourceLocation sideTrimMaterialLocation = setMaterialFromBlock(PartSlot.SIDE_TRIM, topTrimMaterialLocation, materials, BarrelMaterial.SIDE_TRIM, true);
		setMaterialFromBlock(PartSlot.BOTTOM_TRIM, sideTrimMaterialLocation, materials, BarrelMaterial.BOTTOM_TRIM, true);
		ResourceLocation topMaterialLocation = setMaterialFromBlock(PartSlot.TOP_CORE, topTrimMaterialLocation, materials, BarrelMaterial.TOP, true);
		ResourceLocation sideMaterialLocation = setMaterialFromBlock(PartSlot.SIDE_CORE, topMaterialLocation, materials, BarrelMaterial.SIDE, true);
		setMaterialFromBlock(PartSlot.BOTTOM_CORE, sideMaterialLocation, materials, BarrelMaterial.BOTTOM, true);
	}

	@Nullable
	private ResourceLocation setMaterialFromBlock(PartSlot slot, @Nullable ResourceLocation defaultMaterialLocation, Map<BarrelMaterial, ResourceLocation> materials, BarrelMaterial material, boolean addToMaterials) {
		ItemStack decorativeBlock = decorativeBlocks.getStackInSlot(slot.getSlotIndex());
		ResourceLocation materialLocation = DecorationHelper.getMaterialLocation(decorativeBlock).orElse(isSlotMaterialInherited(slot) ? defaultMaterialLocation : null);
		if (materialLocation != null) {
			if (addToMaterials) {
				materials.put(material, materialLocation);
			}
			return materialLocation;
		}
		return null;
	}

	public DecorationTableBlockEntity(BlockPos pos, BlockState blockState) {
		super(ModBlocks.DECORATION_TABLE_BLOCK_ENTITY_TYPE.get(), pos, blockState);
	}

	public ItemStackHandler getDecorativeBlocks() {
		return decorativeBlocks;
	}

	public ItemStackHandler getDyes() {
		return dyes;
	}

	public ItemStackHandler getStorageBlock() {
		return storageBlock;
	}

	public ItemStack getResult() {
		return result;
	}

	public ItemStack extractResult(int count) {
		ItemStack result = getResult();
		if (result.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack extracted = result.copy();
		extracted.setCount(count);
		if (count >= result.getCount()) {
			this.result = ItemStack.EMPTY;
		} else {
			result.shrink(count);
		}
		setChanged();
		return extracted;
	}

	public boolean isSlotMaterialInherited(PartSlot slot) {
		return slotMaterialInheritance.getOrDefault(slot, true);
	}

	public ItemStack getInheritedItem(PartSlot childSlot) {
		while (isSlotMaterialInherited(childSlot)) {
			PartSlot parentSlot = getSlotInheritedFrom(childSlot);
			if (parentSlot == null) {
				return ItemStack.EMPTY;
			}
			if (!decorativeBlocks.getStackInSlot(parentSlot.getSlotIndex()).isEmpty()) {
				return decorativeBlocks.getStackInSlot(parentSlot.getSlotIndex());
			}
			childSlot = parentSlot;
		}
		return ItemStack.EMPTY;
	}

	@Nullable
	public PartSlot getSlotInheritedFrom(PartSlot slot) {
		return switch (slot) {
			case TOP_INNER_TRIM -> null;
			case TOP_TRIM -> PartSlot.TOP_INNER_TRIM;
			case SIDE_TRIM -> PartSlot.TOP_TRIM;
			case BOTTOM_TRIM -> PartSlot.SIDE_TRIM;
			case TOP_CORE -> PartSlot.TOP_TRIM;
			case SIDE_CORE -> PartSlot.TOP_CORE;
			case BOTTOM_CORE -> PartSlot.SIDE_CORE;
		};
	}

	public void setSlotMaterialInheritance(PartSlot slot, boolean value) {
		if (value) {
			slotMaterialInheritance.remove(slot);
		} else {
			slotMaterialInheritance.put(slot, false);
		}
		updateResultAndSetChanged();
	}

	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return super.getUpdateTag(registries).merge(ValueIOHelper.collectOutputToTag(registries, this::saveData));
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		in.child("decorativeBlocks").ifPresent(decorativeBlocks::deserialize);
		in.child("dyes").ifPresent(dyes::deserialize);
		in.child("storageBlock").ifPresent(storageBlock::deserialize);
		result = in.read("result", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		slotMaterialInheritance.clear();
		in.read("slotMaterialInheritance", SLOT_INHERITANCE_CODEC).ifPresent(slotMaterialInheritance::putAll);

		remainingParts.clear();
		in.read("remainingParts", REMAINING_PARTS_CODEC).ifPresent(remainingParts::putAll);

		mainColor = in.getIntOr("mainColor", -1);
		accentColor = in.getIntOr("accentColor", -1);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		saveData(out);
	}

	private void saveData(ValueOutput out) {
		out.putChild("decorativeBlocks", decorativeBlocks);
		out.putChild("dyes", dyes);
		out.putChild("storageBlock", storageBlock);
		if (!result.isEmpty()) {
			out.store("result", ItemStack.CODEC, result);
		}
		out.store("slotMaterialInheritance", SLOT_INHERITANCE_CODEC, slotMaterialInheritance);
		out.store("remainingParts", REMAINING_PARTS_CODEC, remainingParts);
		out.putInt("mainColor", mainColor);
		out.putInt("accentColor", accentColor);
	}

	public void consumeIngredientsOnCraft() {
		ItemStack input = storageBlock.getStackInSlot(0);

		getItemDecorator(input).ifPresent(itemDecorator -> {
			if (!itemDecorator.consumesIngredientsOnCraft()) {
				return;
			}

			boolean emptyDecorativeBlocks = InventoryHelper.isEmpty(decorativeBlocks);
			if ((emptyDecorativeBlocks || !itemDecorator.supportsMaterials(input)) && itemDecorator.supportsTints(input)) {
				DecorationHelper.consumeDyes(mainColor, accentColor, remainingParts, List.of(dyes), StorageBlockItem.getMainColorFromComponentHolder(input).orElse(-1), StorageBlockItem.getAccentColorFromComponentHolder(input).orElse(-1), false);
			} else if (!emptyDecorativeBlocks && itemDecorator.supportsMaterials(input)) {
				Map<BarrelMaterial, ResourceLocation> originalMaterials = BarrelBlockItem.getUncompactedMaterials(input);
				DecorationHelper.consumeMaterials(remainingParts, List.of(decorativeBlocks), originalMaterials, getMaterialsToApply(!STORAGES_WIHOUT_TOP_INNER_TRIM.contains(input.getItem())), false);
			}

			setChanged();
			WorldHelper.notifyBlockUpdate(this);
		});

	}

	public Map<ResourceLocation, Integer> getPartsNeeded() {
		Map<ResourceLocation, Integer> partsNeeded = new HashMap<>();
		ItemStack storageStack = storageBlock.getStackInSlot(0);
		if (InventoryHelper.isEmpty(decorativeBlocks) || !(storageStack.getItem() instanceof BarrelBlockItem)) {
			DecorationHelper.getDyePartsNeeded(mainColor, accentColor, StorageBlockItem.getMainColorFromComponentHolder(storageStack).orElse(-1), StorageBlockItem.getAccentColorFromComponentHolder(storageStack).orElse(-1))
					.forEach((tag, parts) -> partsNeeded.put(tag.location(), parts));
		} else {
			partsNeeded.putAll(DecorationHelper.getMaterialPartsNeeded(BarrelBlockItem.getUncompactedMaterials(storageStack), getMaterialsToApply(!STORAGES_WIHOUT_TOP_INNER_TRIM.contains(storageStack.getItem()))));
		}

		return partsNeeded;
	}

	private Map<BarrelMaterial, ResourceLocation> getMaterialsToApply(boolean supportsInnerTrim) {
		Map<BarrelMaterial, ResourceLocation> materialsToApply = new EnumMap<>(BarrelMaterial.class);
		setMaterialsFromDecorativeBlocks(materialsToApply, supportsInnerTrim);
		return materialsToApply;
	}

	public int getMainColor() {
		return mainColor;
	}

	public void setMainColor(int mainColor) {
		this.mainColor = mainColor;
		updateResultAndSetChanged();
	}

	public int getAccentColor() {
		return accentColor;
	}

	public void setAccentColor(int accentColor) {
		this.accentColor = accentColor;
		updateResultAndSetChanged();
	}

	public Map<ResourceLocation, Integer> getPartsStored() {
		return remainingParts;
	}

	public void dropContents() {
		InventoryHelper.dropItems(decorativeBlocks, level, worldPosition);
		InventoryHelper.dropItems(dyes, level, worldPosition);
		InventoryHelper.dropItems(storageBlock, level, worldPosition);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		dropContents();
	}

	public record TintDecorationResult(ItemStack result, Map<TagKey<Item>, Integer> requiredDyeParts) {
		public static final TintDecorationResult EMPTY = new TintDecorationResult(ItemStack.EMPTY, Collections.emptyMap());
	}

	public interface IItemDecorator {
		default boolean consumesIngredientsOnCraft() {
			return true;
		}

		boolean supportsMaterials(ItemStack input);

		boolean supportsTints(ItemStack input);

		boolean supportsTopInnerTrim(ItemStack input);

		ItemStack decorateWithMaterials(ItemStack input, Map<BarrelMaterial, ResourceLocation> materialsToApply);

		TintDecorationResult decorateWithTints(ItemStack input, int mainColorToSet, int accentColorToSet);

		default List<ItemStack> getPreviewStackInputs(ItemStack input, boolean materialsInCraftingSlots) {
			return List.of(input);
		}
	}

	private static boolean isTintedStorage(ItemStack storage) {
		return StorageBlockItem.getMainColorFromComponentHolder(storage).isPresent() || StorageBlockItem.getAccentColorFromComponentHolder(storage).isPresent();
	}

	private static boolean colorsTransparentOrSameAs(ItemStack storage, int mainColorToSet, int accentColorToSet) {
		return (mainColorToSet == -1 || mainColorToSet == StorageBlockItem.getMainColorFromComponentHolder(storage).orElse(-1)) && (accentColorToSet == -1 || accentColorToSet == StorageBlockItem.getAccentColorFromComponentHolder(storage).orElse(-1));
	}

	public static final IItemDecorator STORAGE_DECORATOR = new IItemDecorator() {
		@Override
		public boolean supportsMaterials(ItemStack input) {
			return input.getItem() instanceof BarrelBlockItem && !isTintedStorage(input);
		}

		@Override
		public boolean supportsTints(ItemStack input) {
			return !(input.getItem() instanceof BarrelBlockItem) || BarrelBlockItem.getMaterials(input).isEmpty();
		}

		@Override
		public boolean supportsTopInnerTrim(ItemStack input) {
			return !STORAGES_WIHOUT_TOP_INNER_TRIM.contains(input.getItem());
		}

		@Override
		public ItemStack decorateWithMaterials(ItemStack input, Map<BarrelMaterial, ResourceLocation> materialsToApply) {
			if (materialsToApply.isEmpty()) {
				return ItemStack.EMPTY;
			}

			Map<BarrelMaterial, ResourceLocation> materials = new EnumMap<>(BarrelMaterial.class);
			materials.putAll(BarrelBlockItem.getMaterials(input));
			BarrelBlockItem.uncompactMaterials(materials);

			materials.putAll(materialsToApply);

			BarrelBlockItem.compactMaterials(materials);

			if (allMaterialsMatch(materials, BarrelBlockItem.getMaterials(input))) {
				return ItemStack.EMPTY;
			}

			ItemStack result = input.copyWithCount(1);

			BarrelBlockItem.removeCoveredTints(result, materials);
			BarrelBlockItem.setMaterials(result, materials);

			return result;
		}

		@Override
		public TintDecorationResult decorateWithTints(ItemStack input, int mainColorToSet, int accentColorToSet) {
			if (colorsTransparentOrSameAs(input, mainColorToSet, accentColorToSet)) {
				return TintDecorationResult.EMPTY;
			}

			ItemStack result = input.copyWithCount(1);

			if (result.getItem() instanceof BlockItem blockItem && blockItem instanceof ITintableBlockItem tintableBlockItem) {
				if (mainColorToSet != -1) {
					tintableBlockItem.setMainColor(result, mainColorToSet);
				}
				if (accentColorToSet != -1) {
					tintableBlockItem.setAccentColor(result, accentColorToSet);
				}
			}

			return new TintDecorationResult(result, calculateRequiredDyes(mainColorToSet, accentColorToSet, StorageBlockItem.getMainColorFromComponentHolder(input).orElse(-1), StorageBlockItem.getAccentColorFromComponentHolder(input).orElse(-1)));

		}
	};

	static {
		ITEM_DECORATORS.put(stack -> stack.getItem() instanceof StorageBlockItem, STORAGE_DECORATOR);
		ITEM_DECORATORS.put(stack -> stack.getItem() instanceof PaintbrushItem, new IItemDecorator() {
			@Override
			public boolean consumesIngredientsOnCraft() {
				return false;
			}

			@Override
			public boolean supportsMaterials(ItemStack input) {
				return true;
			}

			@Override
			public boolean supportsTints(ItemStack input) {
				return true;
			}

			@Override
			public boolean supportsTopInnerTrim(ItemStack input) {
				return true;
			}

			@Override
			public ItemStack decorateWithMaterials(ItemStack input, Map<BarrelMaterial, ResourceLocation> materialsToApply) {
				BarrelBlockItem.compactMaterials(materialsToApply);

				if (allMaterialsMatch(materialsToApply, BarrelBlockItem.getMaterials(input))) {
					return ItemStack.EMPTY;
				}

				ItemStack result = input.copyWithCount(1);
				PaintbrushItem.setBarrelMaterials(result, materialsToApply);
				return result;
			}

			@Override
			public TintDecorationResult decorateWithTints(ItemStack input, int mainColorToSet, int accentColorToSet) {
				if ((mainColorToSet == -1 && accentColorToSet == -1) || (mainColorToSet == PaintbrushItem.getMainColor(input) && accentColorToSet == PaintbrushItem.getAccentColor(input))) {
					return TintDecorationResult.EMPTY;
				}

				ItemStack result = input.copyWithCount(1);
				if (mainColorToSet != -1) {
					PaintbrushItem.setMainColor(result, mainColorToSet);
				}
				if (accentColorToSet != -1) {
					PaintbrushItem.setAccentColor(result, accentColorToSet);
				}
				return new TintDecorationResult(result, Collections.emptyMap());

			}

			@Override
			public List<ItemStack> getPreviewStackInputs(ItemStack input, boolean materialsInCraftingSlots) {
				if (materialsInCraftingSlots) {
					return List.of(new ItemStack(ModBlocks.LIMITED_BARREL_3_ITEM.get()));
				}

				return List.of(new ItemStack(ModBlocks.LIMITED_BARREL_3_ITEM.get()), new ItemStack(ModBlocks.CHEST_ITEM.get()), new ItemStack(ModBlocks.SHULKER_BOX_ITEM.get()));
			}
		});
		ITEM_DECORATORS.put(stack -> stack.is(ItemTags.DYEABLE), new IItemDecorator() {
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
			public TintDecorationResult decorateWithTints(ItemStack input, int mainColorToSet, int accentColorToSet) {
				int currentColor = DyedItemColor.getOrDefault(input, -1);
				if (mainColorToSet == -1 || (currentColor == mainColorToSet)) {
					return TintDecorationResult.EMPTY;
				}

				ItemStack result = input.copyWithCount(1);
				result.set(DataComponents.DYED_COLOR, new DyedItemColor(ARGB.color(0, ARGB.red(mainColorToSet), ARGB.green(mainColorToSet), ARGB.blue(mainColorToSet))));

				return new TintDecorationResult(result, DecorationHelper.getDyePartsNeeded(mainColorToSet, -1, currentColor, -1, 24, 0));
			}
		});
	}

	public enum PartSlot implements StringRepresentable {
		TOP_INNER_TRIM("top_inner_trim", TOP_INNER_TRIM_SLOT),
		TOP_TRIM("top_trim", TOP_TRIM_SLOT),
		SIDE_TRIM("side_trim", SIDE_TRIM_SLOT),
		BOTTOM_TRIM("bottom_trim", BOTTOM_TRIM_SLOT),
		TOP_CORE("top_core", TOP_CORE_SLOT),
		SIDE_CORE("side_core", SIDE_CORE_SLOT),
		BOTTOM_CORE("bottom_core", BOTTOM_CORE_SLOT);

		private final String name;
		private final int slotIndex;

		public static final Codec<PartSlot> CODEC = StringRepresentable.fromEnum(PartSlot::values);

		PartSlot(String name, int slotIndex) {
			this.name = name;
			this.slotIndex = slotIndex;
		}

		private static final Map<String, PartSlot> NAME_VALUES = Arrays.stream(PartSlot.values())
				.collect(Collectors.toMap(PartSlot::getSerializedName, partSlot -> partSlot));
		private static final Map<Integer, PartSlot> INDEX_VALUES = Arrays.stream(PartSlot.values())
				.collect(Collectors.toMap(PartSlot::getSlotIndex, partSlot -> partSlot));

		public static PartSlot fromName(String name) {
			return NAME_VALUES.getOrDefault(name, TOP_INNER_TRIM);
		}

		public static PartSlot fromSlotIndex(int slotIndex) {
			return INDEX_VALUES.getOrDefault(slotIndex, TOP_INNER_TRIM);
		}

		public int getSlotIndex() {
			return slotIndex;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
