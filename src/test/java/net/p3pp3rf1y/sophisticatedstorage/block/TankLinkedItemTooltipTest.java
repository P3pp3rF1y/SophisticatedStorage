package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.*;
import net.p3pp3rf1y.sophisticatedcore.network.ItemStackReference;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.LinkedStorageTooltip;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageItemClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TankLinkedItemTooltipTest {
	@BeforeAll
	static void bootstrap() {
		StorageTankTestBootstrap.bootstrapMinecraft();
	}

	@AfterAll
	static void clearConfig() {
		StorageTankTestBootstrap.clearConfig();
	}

	@Test
	void getLinkedStorageTooltipImageKeepsThePrimaryIndicatorWhileItsSnapshotIsPending() {
		UUID groupId = UUID.randomUUID();
		ItemStack tank = new ItemStack(ModBlocks.GOLD_TANK_ITEM.get());
		LinkedStorageStackData.setEndpoint(tank, new LinkedStorageEndpointData(groupId, UUID.randomUUID()));
		LinkedStorageStackData.setPrimaryEndpoint(tank, true);
		Minecraft minecraft = mock(Minecraft.class);
		LocalPlayer player = mock(LocalPlayer.class);
		ClientLevel level = mock(ClientLevel.class);
		minecraft.player = player;
		when(player.level()).thenReturn(level);
		ItemStackReference reference = new ItemStackReference(ItemStackReference.PLAYER_INVENTORY, 0, "", "");
		ClientLinkedStorageContents.clear();
		ClientLinkedStorageContents.shouldRequestSnapshot(groupId, 0);
		try (var client = mockStatic(Minecraft.class); var references = mockStatic(ItemStackReference.class)) {
			client.when(Minecraft::getInstance).thenReturn(minecraft);
			references.when(() -> ItemStackReference.find(player, tank)).thenReturn(Optional.of(reference));

			TooltipComponent tooltip = StorageItemClient.getLinkedStorageTooltipImage(tank);

			assertEquals(new LinkedStorageTooltip(LinkedStorageEndpointRole.PRIMARY, groupId), tooltip);
		} finally {
			ClientLinkedStorageContents.clear();
		}
	}
}
