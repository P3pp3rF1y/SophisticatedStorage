package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.fml.DistExecutor;
import net.p3pp3rf1y.sophisticatedcore.api.IStashStorageItem;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.RequestLinkedStorageContentsMessage;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ShulkerBoxItemRenderer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ShulkerBoxItem extends StorageBlockItem implements IStashStorageItem {
	public ShulkerBoxItem(Block block) {
		this(block, new Properties().stacksTo(1));
	}

	public ShulkerBoxItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private final NonNullLazy<BlockEntityWithoutLevelRenderer> ister = NonNullLazy
					.of(() -> new ShulkerBoxItemRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()));

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return ister.get();
			}
		});
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
		if (flagIn == TooltipFlag.ADVANCED && !StorageBlockEntity.hasLinkedStorageEndpoint(stack)) {
			stack.getCapability(CapabilityStorageWrapper.getCapabilityInstance())
					.ifPresent(w -> w.getContentsUuid().ifPresent(uuid -> tooltip.add(Component.literal("UUID: " + uuid).withStyle(ChatFormatting.DARK_GRAY))));
		}
		if (!Screen.hasShiftDown()) {
			tooltip.add(Component
					.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".press_for_contents",
							Component.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".shift").withStyle(ChatFormatting.AQUA))
					.withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		AtomicReference<TooltipComponent> ret = new AtomicReference<>();
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ret.set(getTooltipImage(stack, Minecraft.getInstance())));
		return Optional.ofNullable(ret.get());
	}

	@Nullable
	private static TooltipComponent getTooltipImage(ItemStack stack, Minecraft minecraft) {
		Optional<LinkedStorageTooltip> linkedTooltip = StorageBlockEntity.getLinkedStorageEndpointData(stack)
				.flatMap(endpoint -> StorageBlockEntity.getLinkedStorageEndpointRole(stack).map(role -> new LinkedStorageTooltip(role, endpoint.groupId())));
		if (!Screen.hasShiftDown() && (minecraft.player == null || minecraft.player.containerMenu.getCarried().isEmpty())) {
			linkedTooltip
					.filter(tooltip -> minecraft.player != null
							&& ClientLinkedStorageContents.shouldRequestSnapshot(tooltip.groupId(), minecraft.player.level().getGameTime()))
					.ifPresent(tooltip -> PacketHandler.INSTANCE.sendToServer(new RequestLinkedStorageContentsMessage(tooltip.groupId(),
							ClientLinkedStorageContents.getRevision(tooltip.groupId()).orElse(-1L))));
			return linkedTooltip.orElse(null);
		}
		return new StorageContentsTooltip(stack, linkedTooltip.orElse(null));
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}

	@Override
	public void onDestroyed(ItemEntity itemEntity) {
		Level level = itemEntity.level();
		if (level.isClientSide) {
			return;
		}
		ItemStack itemstack = itemEntity.getItem();
		getStashWrapper(itemstack).ifPresent(storageWrapper -> {
			InventoryHelper.dropItems(storageWrapper.getInventoryHandler(), level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
			InventoryHelper.dropItems(storageWrapper.getUpgradeHandler(), level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
		});
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return new ICapabilityProvider() {
			private IStorageWrapper wrapper;

			@Nonnull
			@Override
			public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
				if (stack.getCount() == 1 && cap == CapabilityStorageWrapper.getCapabilityInstance()) {
					initWrapper();
					return LazyOptional.of(() -> wrapper).cast();
				}
				return LazyOptional.empty();
			}

			private void initWrapper() {
				if (wrapper != null) {
					return;
				}
				StorageWrapper storageWrapper = new StackStorageWrapper(stack);
				if (!StorageBlockEntity.hasLinkedStorageEndpoint(stack)) {
					getContentsUuid(stack).ifPresent(uuid -> {
						CompoundTag contents = ItemContentsStorage.get().getOrCreateStorageContents(uuid).getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG);
						storageWrapper.load(contents);
						storageWrapper.setContentsUuid(uuid);
					});
				}
				wrapper = storageWrapper;
			}
		};
	}

	@Override
	public Optional<TooltipComponent> getInventoryTooltip(ItemStack stack) {
		LinkedStorageTooltip linkedStorageTooltip = StorageBlockEntity.getLinkedStorageEndpointData(stack)
				.flatMap(endpoint -> StorageBlockEntity.getLinkedStorageEndpointRole(stack).map(role -> new LinkedStorageTooltip(role, endpoint.groupId())))
				.orElse(null);
		return Optional.of(new StorageContentsTooltip(stack, linkedStorageTooltip));
	}

	public ItemStack stash(ItemStack storageStack, ItemStack stack, boolean simulate) {
		return getStashWrapper(storageStack).map(wrapper -> {
			if (wrapper instanceof StackStorageWrapper stackStorageWrapper && stackStorageWrapper.getContentsUuid().isEmpty()) {
				stackStorageWrapper.ensureContentsUuid();
			}
			return wrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
		}).orElse(stack);
	}

	@Override
	public StashResult getItemStashable(ItemStack storageStack, ItemStack stack) {
		return getStashWrapper(storageStack).map(wrapper -> {
			if (wrapper.getInventoryForUpgradeProcessing().insertItem(stack, true).getCount() == stack.getCount()) {
				return StashResult.NO_SPACE;
			}
			if (wrapper.getInventoryHandler().getSlotTracker().getItems().contains(stack.getItem())
					|| wrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).matchesFilter(stack)) {
				return StashResult.MATCH_AND_SPACE;
			}
			return StashResult.SPACE;
		}).orElse(StashResult.NO_SPACE);
	}

	private static Optional<IStorageWrapper> getStashWrapper(ItemStack storageStack) {
		Optional<StorageWrapper> linkedHost = StorageLinkedStorageResolver.resolveServerCanonicalHost(storageStack);
		if (linkedHost.isPresent()) {
			return Optional.of(linkedHost.get());
		}
		return storageStack.getCapability(CapabilityStorageWrapper.getCapabilityInstance()).resolve().map(wrapper -> (IStorageWrapper) wrapper);
	}

	@Override
	public boolean overrideStackedOnOther(ItemStack storageStack, Slot slot, ClickAction action, Player player) {
		if (hasCreativeScreenContainerOpen(player) || storageStack.getCount() > 1 || !slot.mayPickup(player) || slot.getItem().isEmpty()
				|| action != ClickAction.SECONDARY) {
			return super.overrideStackedOnOther(storageStack, slot, action, player);
		}

		ItemStack stackToStash = slot.getItem();
		ItemStack stashResult = stash(storageStack, stackToStash, true);
		if (stashResult.getCount() < stackToStash.getCount()) {
			int countToTake = stackToStash.getCount() - stashResult.getCount();
			ItemStack takeResult = slot.safeTake(countToTake, countToTake, player);
			stash(storageStack, takeResult, false);
			return true;
		}

		return super.overrideStackedOnOther(storageStack, slot, action, player);
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack storageStack, ItemStack otherStack, Slot slot, ClickAction action, Player player,
			SlotAccess carriedAccess) {
		if (hasCreativeScreenContainerOpen(player) || storageStack.getCount() > 1 || !slot.mayPlace(storageStack) || action != ClickAction.SECONDARY) {
			return super.overrideOtherStackedOnMe(storageStack, otherStack, slot, action, player, carriedAccess);
		}

		ItemStack result = stash(storageStack, otherStack, false);
		if (result.getCount() != otherStack.getCount()) {
			carriedAccess.set(result);
			slot.set(storageStack);
			return true;
		}

		return super.overrideOtherStackedOnMe(storageStack, otherStack, slot, action, player, carriedAccess);
	}

	private boolean hasCreativeScreenContainerOpen(Player player) {
		return player.level().isClientSide() && player.containerMenu instanceof CreativeModeInventoryScreen.ItemPickerMenu;
	}
}
