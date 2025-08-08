
package net.p3pp3rf1y.sophisticatedstorage.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.pip.OversizedItemRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import net.p3pp3rf1y.sophisticatedcore.util.Easing;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.DecorationTableMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.util.DecorationHelper;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class DecorationTableScreen extends AbstractContainerScreen<DecorationTableMenu> {
	public static final ResourceLocation GUI_BACKGROUND = SophisticatedStorage.getRL("textures/gui/decoration_table.png");
	public static final ResourceLocation GUI_DECORATION_TABLE_ELEMENTS = SophisticatedStorage.getRL("textures/gui/decoration_table_elements.png");
	public static final Dimension SQUARE_64 = new Dimension(64, 64);
	public static final Dimension SQUARE_8 = new Dimension(8, 8);
	public static final TextureBlitData TOP_INNER_TRIM_HIGHLIGHT = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(0, 0), Dimension.SQUARE_16);
	public static final TextureBlitData TOP_TRIM_HIGHLIGHT = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(0, 16), Dimension.SQUARE_16);
	public static final TextureBlitData SIDE_TRIM_HIGHLIGHT = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(0, 32), Dimension.SQUARE_16);
	public static final TextureBlitData BOTTOM_TRIM_HIGHLIGHT = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(0, 48), Dimension.SQUARE_16);
	public static final TextureBlitData TOP_CORE_HIGHLIGHT = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(16, 16), Dimension.SQUARE_16);
	public static final TextureBlitData SIDE_CORE_HIGHLIGHT = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(16, 32), Dimension.SQUARE_16);
	public static final TextureBlitData BOTTOM_CORE_HIGHLIGHT = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(16, 48), Dimension.SQUARE_16);
	public static final TextureBlitData ACCENT_TINT_HIGHLIGHT = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(32, 48), Dimension.SQUARE_16);
	public static final TextureBlitData MAIN_TINT_HIGHLIGHT = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(48, 48), Dimension.SQUARE_16);
	public static final TextureBlitData STORAGE_INFO = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(32, 16), Dimension.SQUARE_16);

	private static final TextureBlitData VERTICAL_ARROW_BACKGROUND = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(56, 0), SQUARE_8);
	private static final TextureBlitData VERTICAL_ARROW_HOVERED_BACKGROUND = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(48, 0), SQUARE_8);
	private static final TextureBlitData HORIZONTAL_ARROW_BACKGROUND = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(56, 8), SQUARE_8);
	private static final TextureBlitData HORIZONTAL_ARROW_HOVERED_BACKGROUND = new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(48, 8), SQUARE_8);

	private static final ButtonDefinition.Toggle<Boolean> VERTICAL_INHERITANCE_ARROW = new ButtonDefinition.Toggle<>(SQUARE_8, VERTICAL_ARROW_BACKGROUND, Map.of(
			true, new ToggleButton.StateData(new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(32, 0), SQUARE_8),
					Component.translatable(StorageTranslationHelper.INSTANCE.translButton("decoration_inheritance_on"))),
			false, new ToggleButton.StateData(new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(40, 0), SQUARE_8),
					Component.translatable(StorageTranslationHelper.INSTANCE.translButton("decoration_inheritance_off")))
	), VERTICAL_ARROW_HOVERED_BACKGROUND);

	private static final ButtonDefinition.Toggle<Boolean> HORIZONTAL_INHERITANCE_ARROW = new ButtonDefinition.Toggle<>(SQUARE_8, HORIZONTAL_ARROW_BACKGROUND, Map.of(
			true, new ToggleButton.StateData(new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(32, 8), SQUARE_8),
					Component.translatable(StorageTranslationHelper.INSTANCE.translButton("decoration_inheritance_on"))),
			false, new ToggleButton.StateData(new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(40, 8), SQUARE_8),
					Component.translatable(StorageTranslationHelper.INSTANCE.translButton("decoration_inheritance_off")))
	), HORIZONTAL_ARROW_HOVERED_BACKGROUND);

	private BlockPreview blockPreview;
	private long lastRotationSetTime = 0;

	@Nullable
	private ColorPicker colorPicker;

	private final List<Component> resultPartsNeededTooltip = new ArrayList<>();

	public DecorationTableScreen(DecorationTableMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		imageWidth = 250;
		imageHeight = 226;
		inventoryLabelX = 45;
		getMenu().setSlotChangedListener(this::updatePreviewStacks);
	}

	private void updatePreviewStacks() {
		if (blockPreview != null) {
			blockPreview.setPreviewStacks(getMenu().getDecoratedPreviewStacks());
		}
	}

	@Override
	protected void init() {
		super.init();
		inventoryLabelY = getMenu().getSlot(DecorationTableBlockEntity.BOTTOM_TRIM_SLOT).y + 18 + 2;
		int lastDyeSlotIndex = getMenu().getDyeSlotRange().firstSlot() + getMenu().getDyeSlotRange().numberOfSlots() - 1;
		Slot lastDyeSlot = getMenu().getSlot(lastDyeSlotIndex);
		Slot resultSlot = menu.getResultSlot();
		blockPreview = new BlockPreview(new Position(leftPos + lastDyeSlot.x + 16 + 1 + 8 + 1, topPos + lastDyeSlot.y), new Dimension(80, resultSlot.y - lastDyeSlot.y + 16 + 4));
		updatePreviewStacks();

		addRenderableWidget(blockPreview);
		addVerticalInheritanceArrow(DecorationTableBlockEntity.PartSlot.TOP_TRIM);
		addVerticalInheritanceArrow(DecorationTableBlockEntity.PartSlot.SIDE_TRIM);
		addVerticalInheritanceArrow(DecorationTableBlockEntity.PartSlot.BOTTOM_TRIM);

		addInheritanceArrow(DecorationTableBlockEntity.PartSlot.TOP_CORE, -11, 4, HORIZONTAL_INHERITANCE_ARROW);

		addVerticalInheritanceArrow(DecorationTableBlockEntity.PartSlot.SIDE_CORE);
		addVerticalInheritanceArrow(DecorationTableBlockEntity.PartSlot.BOTTOM_CORE);

		addPartHint(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT, TOP_INNER_TRIM_HIGHLIGHT, "top_inner_trim");
		addPartHint(DecorationTableBlockEntity.TOP_TRIM_SLOT, TOP_TRIM_HIGHLIGHT, "top_trim");
		addPartHint(DecorationTableBlockEntity.SIDE_TRIM_SLOT, SIDE_TRIM_HIGHLIGHT, "side_trim");
		addPartHint(DecorationTableBlockEntity.BOTTOM_TRIM_SLOT, BOTTOM_TRIM_HIGHLIGHT, "bottom_trim");
		addPartHint(DecorationTableBlockEntity.TOP_CORE_SLOT, TOP_CORE_HIGHLIGHT, "top");
		addPartHint(DecorationTableBlockEntity.SIDE_CORE_SLOT, SIDE_CORE_HIGHLIGHT, "side");
		addPartHint(DecorationTableBlockEntity.BOTTOM_CORE_SLOT, BOTTOM_CORE_HIGHLIGHT, "bottom");

		Slot slot = menu.getSlot(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT);
		addRenderableWidget(new PartStorageInfo(new Position(leftPos + slot.x + 57, topPos + slot.y), menu::getPartsStored));

		addDyeElements();

		if (colorPicker != null) {
			colorPicker.setPosition(new Position(leftPos + (imageWidth - ColorPicker.DIMENSIONS.width()) / 2, topPos + (imageHeight - ColorPicker.DIMENSIONS.height()) / 2));
		}
	}

	private void addDyeElements() {
		Slot greenDyeSlot = menu.getSlot(menu.getDyeSlotRange().firstSlot() + 1);
		Slot topTrimSlot = menu.getSlot(DecorationTableBlockEntity.TOP_TRIM_SLOT);
		Slot sideTrimSlot = menu.getSlot(DecorationTableBlockEntity.SIDE_TRIM_SLOT);

		ColorButton mainColorButton = new ColorButton(new Position(leftPos + greenDyeSlot.x - 1, topPos + topTrimSlot.y - 1), new Dimension(18, 18), menu::getMainColor,
				button -> openColorPicker(menu.getMainColor(), menu::setMainColor), Component.translatable(StorageTranslationHelper.INSTANCE.translButton("pick_color")));
		addRenderableWidget(mainColorButton);
		ColorButton accentColorButton = new ColorButton(new Position(leftPos + greenDyeSlot.x - 1, topPos + sideTrimSlot.y - 1), new Dimension(18, 18), menu::getAccentColor,
				button -> openColorPicker(menu.getAccentColor(), menu::setAccentColor), Component.translatable(StorageTranslationHelper.INSTANCE.translButton("pick_color")));
		addRenderableWidget(accentColorButton);

		addRenderableWidget(new PartIcon(new Position(mainColorButton.getX() + mainColorButton.getWidth() + 1, mainColorButton.getY() + 1), MAIN_TINT_HIGHLIGHT, Component.translatable(StorageTranslationHelper.INSTANCE.translGui("tint.main"))));
		addRenderableWidget(new PartIcon(new Position(accentColorButton.getX() + accentColorButton.getWidth() + 1, accentColorButton.getY() + 1), ACCENT_TINT_HIGHLIGHT, Component.translatable(StorageTranslationHelper.INSTANCE.translGui("tint.accent"))));
	}

	private void openColorPicker(int color, IntConsumer colorSetter) {
		colorPicker = new ColorPicker(this, new Position(leftPos + (imageWidth - ColorPicker.DIMENSIONS.width()) / 2, topPos + (imageHeight - ColorPicker.DIMENSIONS.height()) / 2),
				color, c -> {
			colorSetter.accept(c);
			colorPicker = null;
			blockPreview.setVisible(true);
			updatePreviewStacks();
		});
		blockPreview.setVisible(false);
	}

	private void addVerticalInheritanceArrow(DecorationTableBlockEntity.PartSlot slot) {
		addInheritanceArrow(slot, 4, -11, VERTICAL_INHERITANCE_ARROW);
	}

	private void addInheritanceArrow(DecorationTableBlockEntity.PartSlot partSlot, int xOffset, int yOffset, ButtonDefinition.Toggle<Boolean> arrowDefinition) {
		Slot slot = menu.getSlot(partSlot.getSlotIndex());
		addRenderableWidget(new ToggleButton<>(new Position(leftPos + slot.x + xOffset, topPos + slot.y + yOffset), arrowDefinition,
				button -> {
					resultPartsNeededTooltip.clear();
					getMenu().setSlotMaterialInheritance(partSlot, !getMenu().isSlotMaterialInherited(partSlot));
					updatePreviewStacks();
				}, () -> getMenu().isSlotMaterialInherited(partSlot)) {
			@Override
			public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				super.render(guiGraphics, mouseX, mouseY, partialTicks);
				if (isMouseOver(mouseX, mouseY)) {
					Vec2 rotations = SLOT_PREVIEW_ROTATIONS.get(partSlot);
					if (rotations != null) {
						setPreviewRotations((int) rotations.x, (int) rotations.y);
					}
				}
			}
		});
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

		renderDyeSlotsOverlays(guiGraphics);

		if (colorPicker != null) {
			colorPicker.renderBg(guiGraphics, minecraft, i, i1);
		}
	}

	private void renderDyeSlotsOverlays(GuiGraphics guiGraphics) {
		Matrix3x2fStack pose = guiGraphics.pose();
		pose.pushMatrix();
		pose.translate(leftPos, topPos);
		Slot redSlot = getMenu().getSlot(getMenu().getDyeSlotRange().firstSlot());
		renderSlotOverlay(guiGraphics, redSlot, 0x33_FF0000);
		Slot greenSlot = getMenu().getSlot(getMenu().getDyeSlotRange().firstSlot() + 1);
		renderSlotOverlay(guiGraphics, greenSlot, 0x33_00FF00);
		Slot blueSlot = getMenu().getSlot(getMenu().getDyeSlotRange().firstSlot() + 2);
		renderSlotOverlay(guiGraphics, blueSlot, 0x33_0000FF);
		pose.popMatrix();
	}

	private void renderSlotOverlay(GuiGraphics guiGraphics, Slot slot, int slotColor) {
		guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, slotColor);
	}

	private void addPartHint(int slotIndex, TextureBlitData texture, String barrelPart) {
		Slot slot = menu.getSlot(slotIndex);
		addRenderableWidget(new PartIcon(new Position(leftPos + slot.x + 18, topPos + slot.y), texture, Component.translatable(StorageTranslationHelper.INSTANCE.translGui("barrel_part." + barrelPart))));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		updatePreviewRotation(mouseX, mouseY);
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		if (colorPicker != null) {
			renderTransparentBackground(guiGraphics);
			colorPicker.render(guiGraphics, mouseX, mouseY, partialTick);
			colorPicker.renderTooltip(this, guiGraphics, mouseX, mouseY);
		} else {
			renderTooltip(guiGraphics, mouseX, mouseY);
		}
	}

	@Override
	protected void renderSlotContents(GuiGraphics guiGraphics, ItemStack itemstack, Slot slot, @Nullable String countString) {
		if (colorPicker != null) {
			return;
		}

		super.renderSlotContents(guiGraphics, itemstack, slot, countString);
	}

	@Override
	protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
		super.renderSlot(guiGraphics, slot);
		if (slot.getItem().isEmpty() && getMenu().isSlotMaterialInherited(slot.index)) {
			ItemStack inheritedItem = getMenu().getInheritedItem(slot.index);
			if (!inheritedItem.isEmpty()) {
				guiGraphics.renderItem(inheritedItem, slot.x, slot.y, slot.x + slot.y * imageWidth);
				guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiHelper.GUI_CONTROLS, slot.x, slot.y, 77, 0, 16, 16, 256, 256);
			}
		}
	}

	@Override
	protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
		List<Component> tooltip = super.getTooltipFromContainerItem(stack);
		if (hoveredSlot == getMenu().getResultSlot() && !hoveredSlot.getItem().isEmpty() && hoveredSlot.getItem().getItem() != ModItems.PAINTBRUSH.get()) {
			tooltip.addAll(getResultPartsNeededTooltip());
		} else if (!resultPartsNeededTooltip.isEmpty()) {
			resultPartsNeededTooltip.clear();
		}
		return tooltip;
	}

	private List<Component> getResultPartsNeededTooltip() {
		if (!resultPartsNeededTooltip.isEmpty()) {
			return resultPartsNeededTooltip;
		}

		Map<ResourceLocation, Integer> partsNeeded = getMenu().getPartsNeeded();
		addPartCountInfo(partsNeeded, resultPartsNeededTooltip, location -> getMenu().getMissingDyes().contains(location) ? ChatFormatting.RED : ChatFormatting.DARK_GRAY);
		return resultPartsNeededTooltip;
	}

	private static void addPartCountInfo(Map<ResourceLocation, Integer> partCounts, List<Component> tooltip, Function<ResourceLocation, ChatFormatting> getPartFormatting) {
		Map<ItemStack, Tuple<ResourceLocation, Integer>> itemCounts = new LinkedHashMap<>();
		partCounts.forEach((part, count) -> {
			if (BuiltInRegistries.ITEM.containsKey(part)) {
				Item item = BuiltInRegistries.ITEM.getValue(part);
				itemCounts.put(new ItemStack(item), new Tuple<>(part, count));
			} else {
				BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, part))
						.flatMap(set -> set.stream().findFirst()).ifPresent(dye -> itemCounts.put(new ItemStack(dye), new Tuple<>(part, count)));
			}
		});

		itemCounts.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().getHoverName().getString())).forEach(entry -> {
			ItemStack itemStack = entry.getKey();
			ResourceLocation location = entry.getValue().getA();
			int count = entry.getValue().getB();
			MutableComponent partCountText = Component.literal(count + "/" + DecorationHelper.BLOCK_TOTAL_PARTS + " (" + String.format("%.0f%%", (float) count / DecorationHelper.BLOCK_TOTAL_PARTS * 100) + ") of ");
			tooltip.add(partCountText.append(itemStack.getHoverName()).withStyle(getPartFormatting.apply(location)));
		});
	}

	private static final Map<Integer, Vec2> SLOT_PREVIEW_ROTATIONS = Map.of(
			0, new Vec2(90, 180),
			1, new Vec2(90, 180),
			4, new Vec2(90, 180),
			2, new Vec2(0, 180),
			5, new Vec2(0, 180),
			3, new Vec2(-90, 180),
			6, new Vec2(-90, 180)
	);

	private void updatePreviewRotation(int mouseX, int mouseY) {
		SLOT_PREVIEW_ROTATIONS.forEach(
				(slotIndex, rotation) -> updatePreviewRotationForSlot(slotIndex, mouseX, mouseY, (int) rotation.x, (int) rotation.y)
		);
		if (lastRotationSetTime != 0 && System.currentTimeMillis() - lastRotationSetTime > 1000) {
			blockPreview.resetToDefaultRotation();
			lastRotationSetTime = 0;
		}
	}

	private void updatePreviewRotationForSlot(int slotIndex, int mouseX, int mouseY, int xAxisRotation, int yAxisRotation) {
		Slot slot = getMenu().getSlot(slotIndex);
		int slotLeft = leftPos + slot.x;
		int slotTop = topPos + slot.y;
		if (leftPos + slot.x <= mouseX && mouseX < slotLeft + 16 + 18 && slotTop <= mouseY && mouseY < slotTop + 16) {
			setPreviewRotations(xAxisRotation, yAxisRotation);
		}
	}

	private void setPreviewRotations(int xAxisRotation, int yAxisRotation) {
		blockPreview.setTargetRotations(xAxisRotation, yAxisRotation);
		lastRotationSetTime = System.currentTimeMillis();
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (colorPicker != null) {
			return colorPicker.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}

		for (GuiEventListener child : children()) {
			if (child.isMouseOver(mouseX, mouseY) && child.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
				if (child instanceof BlockPreview) {
					lastRotationSetTime = System.currentTimeMillis() + 100_000;
				}

				return true;
			}
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
		super.renderTooltip(guiGraphics, x, y);

		renderables.forEach(renderable -> {
			if (renderable instanceof WidgetBase widget) {
				widget.renderTooltip(this, guiGraphics, x, y);
			}
		});
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (colorPicker != null) {
			return colorPicker.mouseClicked(mouseX, mouseY, button);
		}
		GuiEventListener focused = getFocused();
		if (focused != null && !focused.isMouseOver(mouseX, mouseY) && (focused instanceof WidgetBase widgetBase)) {
			widgetBase.setFocused(false);
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256 && colorPicker != null) {
			colorPicker = null;
			blockPreview.setVisible(true);
			updatePreviewStacks();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private static class PartIcon extends WidgetBase {
		private final TextureBlitData texture;
		private final Component tooltip;

		protected PartIcon(Position position, TextureBlitData texture, Component tooltip) {
			super(position, new Dimension(texture.getWidth(), texture.getHeight()));
			this.texture = texture;
			this.tooltip = tooltip;
		}

		@Override
		protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
			GuiHelper.blit(guiGraphics, x, y, texture);
		}

		@Override
		protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
			//noop
		}

		@Override
		public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
			if (isMouseOver(mouseX, mouseY)) {
				guiGraphics.setTooltipForNextFrame(screen.getMinecraft().font, tooltip, mouseX, mouseY);
			}
		}
	}

	private static class PartStorageInfo extends WidgetBase {
		private final List<Component> partStorageTooltip = new ArrayList<>();
		private final Supplier<Map<ResourceLocation, Integer>> getPartsStored;

		protected PartStorageInfo(Position position, Supplier<Map<ResourceLocation, Integer>> getPartsStored) {
			super(position, Dimension.SQUARE_16);
			this.getPartsStored = getPartsStored;
		}

		@Override
		protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
			if (hasNoPartsToShow()) {
				return;
			}

			GuiHelper.blit(guiGraphics, x, y, STORAGE_INFO);
		}

		private boolean hasNoPartsToShow() {
			return getPartsStored.get().isEmpty();
		}

		@Override
		protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
			//noop
		}

		@Override
		public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
			if (hasNoPartsToShow()) {
				return;
			}

			if (isMouseOver(mouseX, mouseY)) {
				guiGraphics.setTooltipForNextFrame(screen.getMinecraft().font, getPartStorageTooltip(), Optional.empty(), mouseX, mouseY);
			} else if (!partStorageTooltip.isEmpty()) {
				partStorageTooltip.clear();
			}
		}

		private List<Component> getPartStorageTooltip() {
			if (!partStorageTooltip.isEmpty() || hasNoPartsToShow()) {
				return partStorageTooltip;
			}

			partStorageTooltip.add(Component.translatable(StorageTranslationHelper.INSTANCE.translGuiTooltip("parts_stored")));
			DecorationTableScreen.addPartCountInfo(getPartsStored.get(), partStorageTooltip, location -> ChatFormatting.GRAY);
			return partStorageTooltip;
		}
	}

	private static class StackButton extends ButtonBase {
		private static final TextureBlitData BUTTON_HOVER = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(63, 42), Dimension.SQUARE_18);
		private final Supplier<ItemStack> stackSupplier;

		protected StackButton(Position position, IntConsumer onClick, Supplier<ItemStack> stackSupplier) {
			super(position, Dimension.SQUARE_18, onClick);
			this.stackSupplier = stackSupplier;
		}

		@Override
		protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {

		}

		@Override
		protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
			ItemStack stack = stackSupplier.get();
			if (stack.isEmpty()) {
				return;
			}

			guiGraphics.renderItem(stack, x + 1, y + 1);
			if (isMouseOver(mouseX, mouseY)) {
				GuiHelper.blit(guiGraphics, x, y, BUTTON_HOVER);
			}
		}
	}

	private static class BlockPreview extends CompositeWidgetBase<WidgetBase> {
		private final List<ItemStack> previewStacks = new ArrayList<>();
		private float xAxisRotation = 30;
		private float yAxisRotation = 45;

		private float fromXAxisRotation = xAxisRotation;
		private float fromYAxisRotation = yAxisRotation;
		private float targetXAxisRotation = xAxisRotation;
		private float targetYAxisRotation = yAxisRotation;
		private long lastTargetSetTime = 0;
		private int selectedPreview = 0;
		private final List<StackButton> previewStackButtons = new ArrayList<>();

		protected BlockPreview(Position position, Dimension dimension) {
			super(position, dimension);
		}

		public void setPreviewStacks(List<ItemStack> previewStacks) {
			this.previewStacks.clear();
			this.previewStacks.addAll(previewStacks);
			selectedPreview = 0;
			updatePreviewStackButtons();
			resetToDefaultRotation();
		}

		private void updatePreviewStackButtons() {
			previewStackButtons.forEach(children::remove);
			previewStackButtons.clear();

			if (previewStacks.size() < 2) {
				return;
			}

			int x = this.x + (getWidth() - previewStacks.size() * (18 + 2)) / 2;
			for (int i = 0; i < previewStacks.size(); i++) {
				ItemStack stack = previewStacks.get(i);
				int finalI = i;
				previewStackButtons.add(new StackButton(new Position(x + i * 20, y + getHeight() - 19), button -> {
					selectedPreview = finalI;
					resetToDefaultRotation();
				}, () -> stack));
			}
			previewStackButtons.forEach(this::addChild);
		}

		public void resetToDefaultRotation() {
			if (previewStacks.isEmpty()) {
				return;
			}

			ItemStack previewStack = previewStacks.get(selectedPreview);
			if (previewStack.isEmpty()) {
				return;
			}

			ItemStackRenderState renderState = new ItemStackRenderState();
			resolveModel(previewStack, renderState, ItemDisplayContext.GUI);

			if (renderState.layers.length < 1) {
				return;
			}

			ItemTransform guiTransform = renderState.layers[0].transform;
			setTargetRotations((int) guiTransform.rotation().x(), (int) guiTransform.rotation().y());
		}

		private void resolveModel(ItemStack previewStack, ItemStackRenderState renderState, ItemDisplayContext displayContext) {
			minecraft.getItemModelResolver().updateForTopItem(renderState, previewStack, displayContext, null, null, 0);
		}

		public void setTargetRotations(int xAxisRotation, int yAxisRotation) {
			if ((targetXAxisRotation == xAxisRotation && targetYAxisRotation == yAxisRotation)) {
				return;
			}

			fromXAxisRotation = this.xAxisRotation;
			fromYAxisRotation = this.yAxisRotation;
			targetXAxisRotation = xAxisRotation;
			targetYAxisRotation = yAxisRotation;
			lastTargetSetTime = System.currentTimeMillis();
		}

		@Override
		protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
			guiGraphics.fill(x, y, x + getWidth(), y + getHeight(), 0xFF_000000);
		}

		@Override
		protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
			super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
			if (previewStacks.isEmpty()) {
				return;
			}

			ItemStack previewStack = previewStacks.get(selectedPreview);

			if (previewStack.isEmpty()) {
				return;
			}

			updateRotations();

			TrackingItemStackRenderState renderState = new TrackingItemStackRenderState();
			resolveModel(previewStack, renderState, ItemDisplayContext.NONE);
			ItemTransform transform = renderState.layers[0].transform;
			renderState.layers[0].transform = new ItemTransform(new Vector3f(xAxisRotation, yAxisRotation, 0), transform.translation(), new Vector3f(3, 3, 3));
			renderState.setOversizedInGui(true);
			renderState.appendModelIdentityElement(xAxisRotation);
			renderState.appendModelIdentityElement(yAxisRotation);
			guiGraphics.submitPictureInPictureRenderState(new OversizedItemRenderState(new GuiItemRenderState(previewStack.getItem().getName().toString(),
					new Matrix3x2f(guiGraphics.pose()),
					renderState,
					x,
					y,
					guiGraphics.peekScissorStack()), x, y, x + getWidth(), y + getHeight()));
		}

		private void updateRotations() {
			float secondsDuration = 1;
			long currentTime = System.currentTimeMillis();
			if (currentTime - lastTargetSetTime <= secondsDuration * 1000) {
				float ratio = (currentTime - lastTargetSetTime) / (secondsDuration * 1000);
				ratio = Easing.EASE_IN_OUT_CUBIC.ease(ratio);
				xAxisRotation = (fromXAxisRotation + (targetXAxisRotation - fromXAxisRotation) * ratio);
				yAxisRotation = (fromYAxisRotation + (targetYAxisRotation - fromYAxisRotation) * ratio);
			} else {
				xAxisRotation = targetXAxisRotation;
				yAxisRotation = targetYAxisRotation;
			}
		}

		@Override
		public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
			if (!previewStackButtons.isEmpty() && mouseY > y + getHeight() - 20) {
				return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
			}

			yAxisRotation += (float) (2 * dragX);
			yAxisRotation = yAxisRotation % 360;
			xAxisRotation += (float) (2 * dragY);
			xAxisRotation = xAxisRotation % 360;
			targetXAxisRotation = xAxisRotation;
			targetYAxisRotation = yAxisRotation;
			return true;
		}
	}
}
