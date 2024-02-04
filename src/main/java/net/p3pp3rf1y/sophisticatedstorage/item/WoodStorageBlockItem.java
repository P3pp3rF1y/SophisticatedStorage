package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
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
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
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
	public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
		if (isPacked(stack)) {
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
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		if (!isPacked(stack)) {
			return Optional.empty();
		}

		AtomicReference<TooltipComponent> ret = new AtomicReference<>(null);
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			Minecraft mc = Minecraft.getInstance();
			if (Screen.hasShiftDown() || (mc.player != null && !mc.player.containerMenu.getCarried().isEmpty())) {
				ret.set(new StorageContentsTooltip(stack));
			}
		});
		return Optional.ofNullable(ret.get());
	}

	@Override
	public void setMainColor(ItemStack storageStack, int mainColor) {
		if (StorageBlockItem.getAccentColorFromStack(storageStack).isPresent()) {
			removeWoodType(storageStack);
		}
		super.setMainColor(storageStack, mainColor);
	}

	@Override
	public void setAccentColor(ItemStack storageStack, int accentColor) {
		if (StorageBlockItem.getMainColorFromStack(storageStack).isPresent()) {
			removeWoodType(storageStack);
		}
		super.setAccentColor(storageStack, accentColor);
	}

	private void removeWoodType(ItemStack storageStack) {
		storageStack.getOrCreateTag().remove(WoodStorageBlockItem.WOOD_TYPE_TAG);
	}

	public static Optional<WoodType> getWoodType(ItemStack storageStack) {
		return NBTHelper.getString(storageStack, WOOD_TYPE_TAG)
				.flatMap(woodType -> WoodType.values().filter(wt -> wt.name().equals(woodType)).findFirst());
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
					StorageWrapper storageWrapper = new StackStorageWrapper(stack) {
						@Override
						public String getStorageType() {
							return "wood_storage"; //isn't really relevant because wooden storage can't have its gui open when in item form
						}

						@Override
						public Component getDisplayName() {
							return TextComponent.EMPTY; //isn't really relevant because wooden storage can't have its gui open when in item form
						}

						@Override
						protected boolean isAllowedInStorage(ItemStack stack) {
							return false;
						}
					};
					if (uuid != null) {
						CompoundTag compoundtag = ItemContentsStorage.get().getOrCreateStorageContents(uuid).getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG);
						storageWrapper.load(compoundtag);
						storageWrapper.setContentsUuid(uuid); //setting here because client side the uuid isn't in contentsnbt before this data is synced from server and it would create a new one otherwise
					}
					wrapper = storageWrapper;
				}
			}
		};
	}

	public static ItemStack setWoodType(ItemStack storageStack, WoodType woodType) {
		storageStack.getOrCreateTag().putString(WOOD_TYPE_TAG, woodType.name());
		return storageStack;
	}

	@Override
	public Component getName(ItemStack stack) {
		return getDisplayName(getDescriptionId(), getWoodType(stack).orElse(null));
	}

	public static Component getDisplayName(String descriptionId, @Nullable WoodType woodType) {
		if (woodType == null) {
			return new TranslatableComponent(descriptionId, "", "");
		}
		return new TranslatableComponent(descriptionId, new TranslatableComponent("wood_name.sophisticatedstorage." + woodType.name().toLowerCase(Locale.ROOT)), " ");
	}
}
