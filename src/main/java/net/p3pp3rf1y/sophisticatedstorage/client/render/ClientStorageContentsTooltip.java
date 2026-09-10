package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.world.WorldEvent;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.item.CapabilityStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageContentsTooltip;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageContentsWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.network.RequestStorageContentsMessage;
import net.p3pp3rf1y.sophisticatedstorage.network.StoragePacketHandler;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientStorageContentsTooltip extends net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltip {
	private static final Pattern BARREL_TIER = Pattern.compile("^(?:limited_)?(?:(copper|iron|gold|diamond|netherite)_)?barrel(?:_[1-4])?$");
	private final ItemStack storageItem;

	@SuppressWarnings("unused") //parameter needs to be there so that addListener logic would know which event this method listens to
	public static void onWorldLoad(WorldEvent.Load event) {
		refreshContents();
		lastRequestTime = 0;
	}

	@Override
	public void renderImage(Font font, int leftX, int topY, PoseStack poseStack, ItemRenderer itemRenderer, int blitOffset) {
		storageItem.getCapability(CapabilityStorageWrapper.getCapabilityInstance()).ifPresent(wrapper -> renderTooltip(wrapper, font, leftX, topY, poseStack, itemRenderer, blitOffset));
	}

	public ClientStorageContentsTooltip(StorageContentsTooltip tooltip) {
		storageItem = tooltip.getStorageItem();
	}

	@Override
	protected IStorageWrapper refreshContentsWrapper(IStorageWrapper wrapper) {
		return StorageContentsWrapper.fromStack(storageItem).orElseGet(() -> super.refreshContentsWrapper(wrapper));
	}

	@Override
	protected List<ItemStack> getContents(IStorageWrapper wrapper) {
		if (!(wrapper instanceof StorageContentsWrapper preview) || !(storageItem.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof LimitedBarrelBlock)) {
			return super.getContents(wrapper);
		}
		return preview.getContents();
	}

	@Override
	protected void addTooltipLines(IStorageWrapper wrapper, List<Component> lines) {
		if (!WoodStorageBlockItem.isPacked(storageItem) || !(storageItem.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof BarrelBlock barrel) || barrel.getRegistryName() == null) {
			return;
		}
		Matcher matcher = BARREL_TIER.matcher(barrel.getRegistryName().getPath());
		if (matcher.matches()) {
			String tier = matcher.group(1) == null ? "wood" : matcher.group(1);
			lines.add(new TranslatableComponent("item.sophisticatedstorage.barrel.tooltip.tier",
					new TranslatableComponent("storage_tier.sophisticatedstorage." + tier).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	protected void sendInventorySyncRequest(UUID uuid) {
		StoragePacketHandler.INSTANCE.sendToServer(new RequestStorageContentsMessage(uuid));
	}
}
