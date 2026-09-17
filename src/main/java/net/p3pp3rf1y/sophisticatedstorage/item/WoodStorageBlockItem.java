package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.RequestLinkedStorageContentsMessage;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.util.GenericWoodStorageHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class WoodStorageBlockItem extends StorageBlockItem {
	public static final String WOOD_TYPE_TAG = "woodType";
	public static final String PACKED_TAG = "packed";

	public WoodStorageBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static void setPacked(ItemStack storageStack, boolean packed) {
		storageStack.getOrCreateTag().putBoolean(PACKED_TAG, packed);
	}

	public static boolean isPacked(ItemStack storageStack) {
		return NBTHelper.getBoolean(storageStack, PACKED_TAG).orElse(false);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		if (isPacked(stack) || StorageBlockEntity.hasLinkedStorageEndpoint(stack)) {
			if (flag == TooltipFlag.ADVANCED && isPacked(stack)) {
				stack.getCapability(CapabilityStorageWrapper.getCapabilityInstance()).ifPresent(wrapper -> wrapper.getContentsUuid()
						.ifPresent(uuid -> tooltip.add(Component.literal("UUID: " + uuid).withStyle(ChatFormatting.DARK_GRAY))));
			}
			if (!Screen.hasShiftDown()) {
				tooltip.add(Component
						.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".press_for_contents",
								Component.translatable(TranslationHelper.INSTANCE.translItemTooltip("storage") + ".shift").withStyle(ChatFormatting.AQUA))
						.withStyle(ChatFormatting.GRAY));
			}
		}
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		if (!isPacked(stack) && !StorageBlockEntity.hasLinkedStorageEndpoint(stack)) {
			return Optional.empty();
		}
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
						storageWrapper.load(ItemContentsStorage.get().getOrCreateStorageContents(uuid).getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG));
						storageWrapper.setContentsUuid(uuid);
					});
				}
				wrapper = storageWrapper;
			}
		};
	}

	public static Optional<WoodType> getWoodType(ItemStack storageStack) {
		return NBTHelper.getString(storageStack, WOOD_TYPE_TAG).flatMap(woodType -> WoodType.values().filter(wt -> wt.name().equals(woodType)).findFirst());
	}

	public static ItemStack setWoodType(ItemStack storageStack, WoodType woodType) {
		storageStack.getOrCreateTag().putString(WOOD_TYPE_TAG, woodType.name());
		return storageStack;
	}

	@Override
	public Component getName(ItemStack stack) {
		return getDisplayName(getDescriptionId(), isFullyTinted(stack) ? null : getWoodType(stack).orElse(null));
	}

	private static boolean isFullyTinted(ItemStack stack) {
		return StorageBlockItem.getMainColorFromStack(stack).isPresent() && StorageBlockItem.getAccentColorFromStack(stack).isPresent();
	}

	public static Component getDisplayName(String descriptionId, @Nullable WoodType woodType) {
		return woodType == null
				? Component.translatable(descriptionId, "", "")
				: Component.translatable(descriptionId, GenericWoodStorageHelper.getWoodDisplayName(woodType), " ");
	}
}
