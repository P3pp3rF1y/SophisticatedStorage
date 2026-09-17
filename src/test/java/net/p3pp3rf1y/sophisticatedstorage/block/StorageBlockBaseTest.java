package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeClientTickHandler;
import net.p3pp3rf1y.sophisticatedcore.client.render.UpgradeClientRegistry;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointRole;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeClientData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.UpgradeClientDataType;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageBlockBaseTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void animateTickRendersUpgradesForPrimaryAndUnlinkedButNotSecondary() {
		StorageBlockBase block = mock(StorageBlockBase.class, Mockito.CALLS_REAL_METHODS);
		BlockState state = mock(BlockState.class);
		BlockPos pos = BlockPos.ZERO;
		Level level = mock(Level.class);
		StorageBlockEntity storage = new BarrelBlockEntity(pos, ModBlocks.BARREL.get().defaultBlockState());
		StorageWrapper storageWrapper = storage.getStorageWrapper();
		UpgradeClientDataType<TestUpgradeClientData> clientDataType = new UpgradeClientDataType<>("test", TestUpgradeClientData.class, null, null);
		IUpgradeClientTickHandler<TestUpgradeClientData> tickHandler = mock(IUpgradeClientTickHandler.class);
		AtomicInteger tickCalls = new AtomicInteger();

		when(level.getBlockEntity(pos)).thenReturn(storage);
		when(state.getBlock()).thenReturn(block);
		when(block.getFacing(state)).thenReturn(Direction.NORTH);
		storageWrapper.getRenderDataHandler().setUpgradeClientData(clientDataType, new TestUpgradeClientData());
		doAnswer(invocation -> tickCalls.incrementAndGet()).when(tickHandler).onClientTick(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

		try (MockedStatic<Minecraft> minecraftMock = Mockito.mockStatic(Minecraft.class);
				MockedStatic<UpgradeClientRegistry> registryMock = Mockito.mockStatic(UpgradeClientRegistry.class)) {
			Minecraft minecraft = mock(Minecraft.class);
			minecraftMock.when(Minecraft::getInstance).thenReturn(minecraft);
			when(minecraft.isPaused()).thenReturn(false);
			registryMock.when(() -> UpgradeClientRegistry.getUpgradeClientTickHandler(clientDataType)).thenReturn(Optional.of(tickHandler));

			storageWrapper.setLinkedStorageEndpoint(null, LinkedStorageEndpointRole.PRIMARY);
			block.animateTick(state, level, pos, RandomSource.create());
			storageWrapper.setLinkedStorageEndpoint(null, LinkedStorageEndpointRole.SECONDARY);
			block.animateTick(state, level, pos, RandomSource.create());
			storageWrapper.setLinkedStorageEndpoint(null, null);
			block.animateTick(state, level, pos, RandomSource.create());
		}

		assertEquals(2, tickCalls.get());
	}

	private static class TestUpgradeClientData implements IUpgradeClientData {
		@Override
		public IUpgradeClientData copy() {
			return new TestUpgradeClientData();
		}
	}
}
