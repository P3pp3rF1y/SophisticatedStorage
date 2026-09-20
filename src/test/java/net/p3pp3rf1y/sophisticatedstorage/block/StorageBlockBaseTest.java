package net.p3pp3rf1y.sophisticatedstorage.block;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeRenderer;
import net.p3pp3rf1y.sophisticatedcore.client.render.UpgradeRenderRegistry;
import net.p3pp3rf1y.sophisticatedcore.linkedstorage.LinkedStorageEndpointRole;
import net.p3pp3rf1y.sophisticatedcore.renderdata.IUpgradeRenderData;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.renderdata.UpgradeRenderDataType;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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
	void animateTickRendersUpgradesForPrimaryAndUnlinkedButNotSecondary() {
		StorageBlockBase block = mock(StorageBlockBase.class, Mockito.CALLS_REAL_METHODS);
		BlockState state = mock(BlockState.class);
		BlockPos pos = BlockPos.ZERO;
		Level level = mock(Level.class);
		StorageBlockEntity storage = new BarrelBlockEntity(pos, ModBlocks.BARREL.get().defaultBlockState());
		StorageWrapper storageWrapper = storage.getStorageWrapper();
		RenderInfo renderInfo = storageWrapper.getRenderInfo();
		UpgradeRenderDataType<TestUpgradeRenderData> renderDataType = new UpgradeRenderDataType<>("test", TestUpgradeRenderData.class,
				tag -> new TestUpgradeRenderData());
		TestUpgradeRenderData renderData = new TestUpgradeRenderData();
		IUpgradeRenderer<TestUpgradeRenderData> renderer = mock(IUpgradeRenderer.class);
		AtomicInteger renderCalls = new AtomicInteger();

		when(level.getBlockEntity(pos)).thenReturn(storage);
		when(state.getBlock()).thenReturn(block);
		when(block.getFacing(state)).thenReturn(Direction.NORTH);
		renderInfo.setUpgradeRenderData(renderDataType, renderData);
		doAnswer(invocation -> renderCalls.incrementAndGet()).when(renderer).render(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

		try (MockedStatic<Minecraft> minecraftMock = Mockito.mockStatic(Minecraft.class);
				MockedStatic<UpgradeRenderRegistry> renderRegistryMock = Mockito.mockStatic(UpgradeRenderRegistry.class)) {
			Minecraft minecraft = mock(Minecraft.class);
			minecraftMock.when(Minecraft::getInstance).thenReturn(minecraft);
			when(minecraft.isPaused()).thenReturn(false);
			renderRegistryMock.when(() -> UpgradeRenderRegistry.getUpgradeRenderer(renderDataType)).thenReturn(Optional.of(renderer));

			storageWrapper.setLinkedStorageEndpoint(null, LinkedStorageEndpointRole.PRIMARY);
			block.animateTick(state, level, pos, RandomSource.create());
			storageWrapper.setLinkedStorageEndpoint(null, LinkedStorageEndpointRole.SECONDARY);
			block.animateTick(state, level, pos, RandomSource.create());
			storageWrapper.setLinkedStorageEndpoint(null, null);
			block.animateTick(state, level, pos, RandomSource.create());
		}

		assertEquals(2, renderCalls.get());
	}

	private static class TestUpgradeRenderData implements IUpgradeRenderData {
		@Override
		public CompoundTag serializeNBT() {
			return new CompoundTag();
		}
	}
}
