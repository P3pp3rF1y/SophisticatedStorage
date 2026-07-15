package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.IItemDisplaySettingsPreviewProvider;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsTab;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.ShulkerBoxBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.client.render.StorageBlockPreviewRenderState;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;
import org.joml.Matrix3x2f;

import java.util.Optional;

public final class StorageItemDisplaySettingsPreviewProvider implements IItemDisplaySettingsPreviewProvider {
	public static final StorageItemDisplaySettingsPreviewProvider INSTANCE = new StorageItemDisplaySettingsPreviewProvider();

	private StorageItemDisplaySettingsPreviewProvider() {
	}

	@Override
	public Optional<ItemStack> getItemDisplaySettingsPreviewStack(SettingsScreen screen, ItemDisplaySettingsContainer container, int selectedSlot) {
		ItemStack wrappedStorageStack = screen.getMenu().getStorageWrapper().getWrappedStorageStack().copy();
		return wrappedStorageStack.isEmpty() ? Optional.empty() : Optional.of(wrappedStorageStack);
	}

	@Override
	public boolean renderItemDisplaySettingsPreview(ItemDisplaySettingsTab tab, SettingsScreen screen, GuiGraphics guiGraphics, int x, int y, int width,
			int height, ItemDisplaySettingsContainer container, int selectedSlot, float xAxisRotation, float yAxisRotation, float partialTicks) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return false;
		}

		Optional<StorageBlockEntity> storageBlockEntity = WorldHelper.getBlockEntity(minecraft.level, screen.getMenu().getBlockPosition(),
				StorageBlockEntity.class);
		storageBlockEntity.ifPresent(blockEntity -> submitStorageBlockPreview(guiGraphics, x, y, width, height, xAxisRotation, yAxisRotation, partialTicks,
				blockEntity, Optional.empty()));
		if (storageBlockEntity.isPresent()) {
			return true;
		}

		return createPreviewStorage(screen.getMenu().getStorageWrapper()).map(previewStorage -> {
			submitStorageBlockPreview(guiGraphics, x, y, width, height, xAxisRotation, yAxisRotation, partialTicks, previewStorage.blockEntity(),
					previewStorage.otherChest());
			return true;
		}).orElse(false);
	}

	private Optional<PreviewStorage> createPreviewStorage(IStorageWrapper sourceWrapper) {
		ItemStack wrappedStorageStack = sourceWrapper.getWrappedStorageStack();
		if (!(wrappedStorageStack.getItem() instanceof BlockItem blockItem)) {
			return Optional.empty();
		}

		Block block = blockItem.getBlock();
		if (!(block instanceof StorageBlockBase storageBlock)) {
			return Optional.empty();
		}

		if (wrappedStorageStack.getItem() instanceof ChestBlockItem && ChestBlockItem.isDoubleChest(wrappedStorageStack)) {
			return Optional.of(createPreviewDoubleChest(sourceWrapper, storageBlock, wrappedStorageStack));
		}

		BlockState previewState = getPreviewBlockState(storageBlock);
		StorageBlockEntity previewBlockEntity = storageBlock.newBlockEntity(BlockPos.ZERO, previewState);
		copyPreviewStorage(sourceWrapper, previewBlockEntity, wrappedStorageStack);
		return Optional.of(new PreviewStorage(previewBlockEntity, Optional.empty()));
	}

	private PreviewStorage createPreviewDoubleChest(IStorageWrapper sourceWrapper, StorageBlockBase storageBlock, ItemStack wrappedStorageStack) {
		BlockState rightState = storageBlock.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT);
		ChestBlockEntity rightChest = (ChestBlockEntity) storageBlock.newBlockEntity(BlockPos.ZERO, rightState);
		copyPreviewStorage(sourceWrapper, rightChest, wrappedStorageStack);

		BlockState leftState = storageBlock.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT);
		ChestBlockEntity leftChest = (ChestBlockEntity) storageBlock.newBlockEntity(new BlockPos(-1, 0, 0), leftState);
		copyPreviewRenderProperties(sourceWrapper, leftChest, wrappedStorageStack);
		return new PreviewStorage(rightChest, Optional.of(leftChest));
	}

	private BlockState getPreviewBlockState(StorageBlockBase storageBlock) {
		BlockState state = storageBlock.defaultBlockState();
		if (state.getBlock() instanceof LimitedBarrelBlock) {
			state = state.setValue(LimitedBarrelBlock.HORIZONTAL_FACING, Direction.NORTH).setValue(LimitedBarrelBlock.VERTICAL_FACING, VerticalFacing.NO);
		} else if (state.hasProperty(BarrelBlock.FACING)) {
			state = state.setValue(BarrelBlock.FACING, Direction.NORTH);
		} else if (state.hasProperty(ShulkerBoxBlock.FACING)) {
			state = state.setValue(ShulkerBoxBlock.FACING, Direction.NORTH);
		} else if (state.hasProperty(ChestBlock.FACING)) {
			state = state.setValue(ChestBlock.FACING, Direction.NORTH);
		}
		if (state.hasProperty(ChestBlock.TYPE)) {
			state = state.setValue(ChestBlock.TYPE, ChestType.SINGLE);
		}
		if (state.hasProperty(BarrelBlock.OPEN)) {
			state = state.setValue(BarrelBlock.OPEN, false);
		}
		return state;
	}

	private void copyPreviewStorage(IStorageWrapper sourceWrapper, StorageBlockEntity previewBlockEntity, ItemStack wrappedStorageStack) {
		copyPreviewRenderProperties(sourceWrapper, previewBlockEntity, wrappedStorageStack);
		IStorageWrapper targetWrapper = previewBlockEntity.getStorageWrapper();
		copyInventory(sourceWrapper.getInventoryHandler(), targetWrapper.getInventoryHandler());
		copyItemDisplaySettings(sourceWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class),
				targetWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class));
	}

	private void copyPreviewRenderProperties(IStorageWrapper sourceWrapper, StorageBlockEntity previewBlockEntity, ItemStack wrappedStorageStack) {
		previewBlockEntity.getStorageWrapper().setColors(
				StorageBlockItem.getMainColorFromComponentHolder(wrappedStorageStack).orElse(sourceWrapper.getMainColor()),
				StorageBlockItem.getAccentColorFromComponentHolder(wrappedStorageStack).orElse(sourceWrapper.getAccentColor()));
		if (previewBlockEntity instanceof WoodStorageBlockEntity woodStorageBlockEntity) {
			WoodStorageBlockItem.getWoodType(wrappedStorageStack).ifPresent(woodStorageBlockEntity::setWoodType);
		}
		if (previewBlockEntity instanceof BarrelBlockEntity barrelBlockEntity && wrappedStorageStack.getItem() instanceof BarrelBlockItem) {
			barrelBlockEntity.setMaterials(BarrelBlockItem.getMaterials(wrappedStorageStack));
		}
	}

	private void copyInventory(InventoryHandler sourceInventory, InventoryHandler targetInventory) {
		int slots = Math.min(sourceInventory.getSlots(), targetInventory.getSlots());
		for (int slot = 0; slot < slots; slot++) {
			targetInventory.setStackInSlot(slot, sourceInventory.getStackInSlot(slot).copy());
		}
	}

	private void copyItemDisplaySettings(ItemDisplaySettingsCategory sourceCategory, ItemDisplaySettingsCategory targetCategory) {
		for (int slot : sourceCategory.getSlots()) {
			targetCategory.selectSlot(slot);
			int rotation = sourceCategory.getRotation(slot);
			for (int i = 0; i < rotation / 45; i++) {
				targetCategory.rotate(slot, true);
			}
			targetCategory.setZOffset(slot, sourceCategory.getZOffset(slot));
		}
		targetCategory.setDisplaySide(sourceCategory.getDisplaySide());
		targetCategory.setColor(sourceCategory.getColor());
	}

	private void submitStorageBlockPreview(GuiGraphics guiGraphics, int x, int y, int width, int height, float xAxisRotation, float yAxisRotation,
			float partialTicks, StorageBlockEntity storageBlockEntity, Optional<ChestBlockEntity> previewOtherChest) {
		StorageBlockEntity renderBlockEntity = storageBlockEntity instanceof ChestBlockEntity chestBlockEntity
				? chestBlockEntity.getMainChestBlockEntity()
				: storageBlockEntity;
		if (renderBlockEntity == null) {
			return;
		}

		BlockPos renderBlockPos = renderBlockEntity.getBlockPos();
		Optional<ChestBlockEntity> otherChest = previewOtherChest.or(() -> getOtherChest(renderBlockEntity));
		Optional<BlockPos> otherChestOffset = otherChest.map(chest -> chest.getBlockPos().subtract(renderBlockPos));
		float scale = otherChest.isPresent() ? (Math.min(width, height) - 16) / 2F : Math.min(width, height) - 16;
		if (otherChest.isEmpty() && shouldScaleStoragePreviewDown(renderBlockEntity)) {
			scale *= 0.75F;
		}

		guiGraphics.submitPictureInPictureRenderState(new StorageBlockPreviewRenderState(renderBlockEntity, otherChest.orElse(null),
				otherChestOffset.orElse(null), xAxisRotation, getStoragePreviewYAxisRotation(renderBlockEntity, yAxisRotation), scale / 16F, partialTicks,
				new Matrix3x2f(guiGraphics.pose()), guiGraphics.peekScissorStack(), x, y, x + width, y + height));
	}

	private float getStoragePreviewYAxisRotation(StorageBlockEntity storageBlockEntity, float yAxisRotation) {
		if (storageBlockEntity instanceof ChestBlockEntity || storageBlockEntity instanceof BarrelBlockEntity
				|| storageBlockEntity instanceof ShulkerBoxBlockEntity) {
			return yAxisRotation + 180;
		}

		return yAxisRotation;
	}

	private boolean shouldScaleStoragePreviewDown(StorageBlockEntity storageBlockEntity) {
		return storageBlockEntity instanceof ChestBlockEntity || storageBlockEntity instanceof BarrelBlockEntity
				|| storageBlockEntity instanceof ShulkerBoxBlockEntity;
	}

	private Optional<ChestBlockEntity> getOtherChest(StorageBlockEntity storageBlockEntity) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(storageBlockEntity instanceof ChestBlockEntity chestBlockEntity) || minecraft.level == null) {
			return Optional.empty();
		}

		BlockState state = chestBlockEntity.getBlockState();
		if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
			return Optional.empty();
		}

		return WorldHelper.getBlockEntity(minecraft.level, chestBlockEntity.getBlockPos().relative(ChestBlock.getConnectedDirection(state)),
				ChestBlockEntity.class);
	}

	private record PreviewStorage(StorageBlockEntity blockEntity, Optional<ChestBlockEntity> otherChest) {
	}
}
