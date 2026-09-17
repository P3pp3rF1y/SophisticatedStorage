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

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class StorageBlockBaseTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void animateTickRendersUpgradesForPrimaryAndUnlinkedButNotSynchronizedSecondary() throws ReflectiveOperationException {
		StorageBlockBase block = mock(StorageBlockBase.class, Mockito.CALLS_REAL_METHODS);
		BlockState state = mock(BlockState.class);
		BlockPos pos = BlockPos.ZERO;
		Level level = mock(Level.class);
		StorageBlockEntity storage = new BarrelBlockEntity(pos, ModBlocks.BARREL.get().defaultBlockState());
		UpgradeClientDataType<TestUpgradeClientData> renderDataType = new UpgradeClientDataType<>("test", TestUpgradeClientData.class, null, null);
		TestUpgradeClientData renderData = new TestUpgradeClientData();
		IUpgradeClientTickHandler<TestUpgradeClientData> renderer = mock(IUpgradeClientTickHandler.class);
		AtomicInteger renderCalls = new AtomicInteger();

		when(level.getBlockEntity(pos)).thenReturn(storage);
		when(state.getBlock()).thenReturn(block);
		when(block.getFacing(state)).thenReturn(Direction.NORTH);
		storage.getStorageWrapper().getRenderDataHandler().setUpgradeClientData(renderDataType, renderData);
		doAnswer(invocation -> renderCalls.incrementAndGet()).when(renderer).onClientTick(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

		try (MockedStatic<Minecraft> minecraftMock = Mockito.mockStatic(Minecraft.class);
				MockedStatic<UpgradeClientRegistry> renderRegistryMock = Mockito.mockStatic(UpgradeClientRegistry.class)) {
			Minecraft minecraft = mock(Minecraft.class);
			minecraftMock.when(Minecraft::getInstance).thenReturn(minecraft);
			when(minecraft.isPaused()).thenReturn(false);
			renderRegistryMock.when(() -> UpgradeClientRegistry.getUpgradeClientTickHandler(renderDataType)).thenReturn(Optional.of(renderer));

			setLinkedStorageEndpointRole(storage, LinkedStorageEndpointRole.PRIMARY);
			block.animateTick(state, level, pos, RandomSource.create());
			setLinkedStorageEndpointRole(storage, LinkedStorageEndpointRole.SECONDARY);
			block.animateTick(state, level, pos, RandomSource.create());
			setLinkedStorageEndpointRole(storage, null);
			block.animateTick(state, level, pos, RandomSource.create());
		}

		assertEquals(2, renderCalls.get());
	}

	private static void setLinkedStorageEndpointRole(StorageBlockEntity storage, LinkedStorageEndpointRole endpointRole) throws ReflectiveOperationException {
		Field field = StorageBlockEntity.class.getDeclaredField("linkedStorageEndpointRole");
		field.setAccessible(true);
		field.set(storage, endpointRole);
	}

	private static class TestUpgradeClientData implements IUpgradeClientData {
		@Override
		public IUpgradeClientData copy() {
			return this;
		}
	}
}
