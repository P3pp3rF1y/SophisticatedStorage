package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.IItemRenderProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.fml.DistExecutor;
import net.p3pp3rf1y.sophisticatedcore.api.IStashStorageItem;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.IStorageBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.client.render.ShulkerBoxItemRenderer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ShulkerBoxItem extends StorageBlockItem implements IStashStorageItem {
	public ShulkerBoxItem(Block block) {
		super(block, new Properties().stacksTo(1));
	}

	@Override
	public void initializeClient(Consumer<IItemRenderProperties> consumer) {
		consumer.accept(new IItemRenderProperties() {
			private final NonNullLazy<BlockEntityWithoutLevelRenderer> ister = NonNullLazy.of(() -> new ShulkerBoxItemRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()));

			@Override
			public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
				return ister.get();
			}
		});
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
		if (flagIn == TooltipFlag.Default.ADVANCED) {
			stack.getCapability(CapabilityStorageWrapper.getCapabilityInstance())
					.ifPresent(w -> w.getContentsUuid().ifPresent(uuid -> tooltip.add(new TextComponent("UUID: " + uuid).withStyle(ChatFormatting.DARK_GRAY))));
		}
		if (!Screen.hasShiftDown()) {
			tooltip.add(new TranslatableComponent(
					TranslationHelper.INSTANCE.translItemTooltip("storage") + ".press_for_contents",
					new TranslatableComponent(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".shift").withStyle(ChatFormatting.AQUA)
			).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		AtomicReference<TooltipComponent> ret = new AtomicReference<>(null);
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			Minecraft mc = Minecraft.getInstance();
			if (Screen.hasShiftDown() || (mc.player != null && !mc.player.containerMenu.getCarried().isEmpty())) {
				ret.set(new ContentsTooltip(stack));
			}
		});
		return Optional.ofNullable(ret.get());
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}

	@Override
	public void onDestroyed(ItemEntity itemEntity) {
		Level level = itemEntity.level;
		if (level.isClientSide) {
			return;
		}
		ItemStack itemstack = itemEntity.getItem();
		itemstack.getCapability(CapabilityStorageWrapper.getCapabilityInstance()).ifPresent(storageWrapper -> {
			InventoryHelper.dropItems(storageWrapper.getInventoryHandler(), level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
			InventoryHelper.dropItems(storageWrapper.getUpgradeHandler(), level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
		});
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return new ICapabilityProvider() {
			private IStorageWrapper wrapper = null;

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
				if (wrapper == null) {
					UUID uuid = NBTHelper.getUniqueId(stack, "uuid").orElse(null);
					StorageWrapper storageWrapper = new StackStorageWrapper(stack, () -> {
						UUID newUuid = UUID.randomUUID();
						NBTHelper.setUniqueId(stack, "uuid", newUuid);
						CompoundTag mainTag = new CompoundTag();
						CompoundTag storageWrapperTag = new CompoundTag();
						storageWrapperTag.put("contents", new CompoundTag());
						mainTag.put(StorageBlockEntity.STORAGE_WRAPPER_TAG, storageWrapperTag);
						ShulkerBoxStorage.get().setShulkerBoxContents(newUuid, mainTag);
						return newUuid;
					});
					if (uuid != null) {
						CompoundTag compoundtag = ShulkerBoxStorage.get().getOrCreateShulkerBoxContents(uuid).getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG);
						storageWrapper.load(compoundtag);
						storageWrapper.setContentsUuid(uuid); //setting here because client side the uuid isn't in contentsnbt before this data is synced from server and it would create a new one otherwise
					}
					wrapper = storageWrapper;
				}
			}
		};
	}

	@Override
	public Optional<TooltipComponent> getInventoryTooltip(ItemStack stack) {
		return Optional.of(new ContentsTooltip(stack));
	}

	@Override
	public ItemStack stash(ItemStack storageStack, ItemStack stack) {
		return storageStack.getCapability(CapabilityStorageWrapper.getCapabilityInstance()).map(wrapper -> wrapper.getInventoryForUpgradeProcessing().insertItem(stack, false)).orElse(stack);
	}

	@Override
	public boolean isItemStashable(ItemStack storageStack, ItemStack stack) {
		return storageStack.getCapability(CapabilityStorageWrapper.getCapabilityInstance()).map(wrapper -> wrapper.getInventoryForUpgradeProcessing().isItemValid(0, stack)).orElse(false);
	}

	public record ContentsTooltip(ItemStack shulkerItem) implements TooltipComponent {
		public ItemStack getShulkerItem() {
			return shulkerItem;
		}
	}

	public void setNumberOfInventorySlots(ItemStack shulkerBoxStack, int numberOfInventorySlots) {
		NBTHelper.putInt(shulkerBoxStack.getOrCreateTag(), "numberOfInventorySlots", numberOfInventorySlots);
	}

	public int getNumberOfInventorySlots(ItemStack shulkerBoxStack) {
		return NBTHelper.getInt(shulkerBoxStack, "numberOfInventorySlots").orElse(shulkerBoxStack.getCapability(CapabilityStorageWrapper.getCapabilityInstance()).map(StorageWrapper::getDefaultNumberOfInventorySlots).orElse(1));
	}

	public int getNumberOfUpgradeSlots(ItemStack shulkerBoxStack) {
		return NBTHelper.getInt(shulkerBoxStack, "numberOfUpgradeSlots").orElse(shulkerBoxStack.getCapability(CapabilityStorageWrapper.getCapabilityInstance()).map(StorageWrapper::getDefaultNumberOfUpgradeSlots).orElse(1));
	}

	public void setNumberOfUpgradeSlots(ItemStack shulkerBoxStack, int numberOfUpgradeSlots) {
		NBTHelper.putInt(shulkerBoxStack.getOrCreateTag(), "numberOfUpgradeSlots", numberOfUpgradeSlots);
	}

	private class StackStorageWrapper extends StorageWrapper {
		private final ItemStack shulkerBoxStack;
		private final Supplier<UUID> getNewUuid;

		public StackStorageWrapper(ItemStack shulkerBoxStack, Supplier<UUID> getNewUuid) {
			super(() -> () -> {}, () -> {}, () -> {});
			this.shulkerBoxStack = shulkerBoxStack;
			this.getNewUuid = getNewUuid;
		}

		@Override
		public Optional<UUID> getContentsUuid() {
			return Optional.ofNullable(contentsUuid);
		}

		@Override
		protected CompoundTag getContentsNbt() {
			if (contentsUuid == null) {
				contentsUuid = getNewUuid.get();
			}
			//noinspection ConstantConditions - uuid is initiated above if null
			return ShulkerBoxStorage.get().getOrCreateShulkerBoxContents(contentsUuid).getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG).getCompound("contents");
		}

		@Override
		protected void onUpgradeRefresh() {
			//noop - there should be no upgrade refresh happening here
		}

		@Override
		public int getDefaultNumberOfInventorySlots() {
			return getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfInventorySlots() : 0;
		}

		@Override
		protected void loadSlotNumbers(CompoundTag tag) {
			numberOfInventorySlots = NBTHelper.getInt(shulkerBoxStack, "numberOfInventorySlots").orElse(0);
			numberOfUpgradeSlots = NBTHelper.getInt(shulkerBoxStack, "numberOfUpgradeSlots").orElse(0);
		}

		@Override
		protected boolean isAllowedInStorage(ItemStack stack) {
			//TODO add config with other things that can't go in
			//TODO add backpacks compat so that they can't go in
			Block block = Block.byItem(stack.getItem());
			return !(block instanceof ShulkerBoxBlock) && !(block instanceof net.minecraft.world.level.block.ShulkerBoxBlock);
		}

		@Override
		public int getDefaultNumberOfUpgradeSlots() {
			return getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfUpgradeSlots() : 0;
		}
	}
}
