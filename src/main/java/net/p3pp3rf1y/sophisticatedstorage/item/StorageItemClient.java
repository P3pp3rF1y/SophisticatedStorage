package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.ClientLinkedStorageContents;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.RequestLinkedStorageContentsPayload;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;

import javax.annotation.Nullable;

import java.util.Optional;

public class StorageItemClient {
	private StorageItemClient() {
	}

	@Nullable
	public static TooltipComponent getTooltipImage(ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		Optional<LinkedStorageTooltip> linkedStorageTooltip = StorageBlockEntity.getLinkedStorageEndpointData(stack)
				.flatMap(endpoint -> StorageBlockEntity.getLinkedStorageEndpointRole(stack).map(role -> new LinkedStorageTooltip(role, endpoint.groupId())));
		if (!Minecraft.getInstance().hasShiftDown() && (mc.player == null || mc.player.containerMenu.getCarried().isEmpty())) {
			linkedStorageTooltip.filter(
					tooltip -> mc.player != null && ClientLinkedStorageContents.shouldRequestSnapshot(tooltip.groupId(), mc.player.level().getGameTime()))
					.ifPresent(tooltip -> ClientPacketDistributor.sendToServer(new RequestLinkedStorageContentsPayload(tooltip.groupId(),
							ClientLinkedStorageContents.getRevision(tooltip.groupId()).orElse(-1L))));
			return linkedStorageTooltip.<TooltipComponent>map(tooltip -> tooltip).orElse(null);
		}
		if (Minecraft.getInstance().hasShiftDown() || (mc.player != null && !mc.player.containerMenu.getCarried().isEmpty())) {
			return new StorageContentsTooltip(stack, linkedStorageTooltip.orElse(null));
		}
		return null;
	}
}
