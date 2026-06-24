package net.emutils.client.emutils.inventory.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.inventory.MassDropStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MassDropScreen extends Screen {
	private final Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	private EditBox search;
	private Button modeButton;
	private ItemList list;

	public MassDropScreen(Screen parent) {
		super(Component.translatable("emutils.mass_drop.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		layout.addTitleHeader(title, font);
		layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
		layout.visitWidgets(this::addRenderableWidget);

		search = addRenderableWidget(new EditBox(font, 0, 0, 260, 20, Component.translatable("emutils.mass_drop.search")));
		search.setHint(Component.translatable("emutils.mass_drop.search"));
		search.setResponder(ignored -> refreshList());
		modeButton = addRenderableWidget(Button.builder(modeLabel(), button -> {
			store().cycleMode();
			button.setMessage(modeLabel());
		}).width(160).build());
		list = addRenderableWidget(new ItemList(this, minecraft, width, height));
		refreshList();
		repositionElements();
	}

	private MassDropStore store() {
		return EMUtilsClient.massDrop().store();
	}

	private Component modeLabel() {
		return Component.translatable("emutils.mass_drop.mode", Component.translatable(store().mode().labelKey()));
	}

	private void refreshList() {
		if (list != null) {
			list.refresh(search == null ? "" : search.getValue());
		}
	}

	@Override
	protected void repositionElements() {
		layout.arrangeElements();
		int toolbarY = layout.getHeaderHeight() + 6;
		int toolbarWidth = 260 + 6 + 160;
		int toolbarX = (width - toolbarWidth) / 2;
		if (search != null) {
			search.setPosition(toolbarX, toolbarY);
		}
		if (modeButton != null) {
			modeButton.setPosition(toolbarX + 266, toolbarY);
		}
		if (list != null) {
			int top = toolbarY + 26;
			list.updateSizeAndPosition(width, Math.max(40, height - top - layout.getFooterHeight()), 0, top);
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}

	private static final class ItemList extends ObjectSelectionList<ItemEntry> {
		private final MassDropScreen screen;

		private ItemList(MassDropScreen screen, Minecraft client, int width, int height) {
			super(client, width, height, 0, 38);
			this.screen = screen;
			centerListVertically = false;
		}

		private void refresh(String query) {
			clearEntries();
			String normalized = query.trim().toLowerCase(Locale.ROOT);
			List<Item> items = new ArrayList<>();
			for (Item item : BuiltInRegistries.ITEM) {
				if (item == Items.AIR) {
					continue;
				}
				ItemStack stack = item.getDefaultInstance();
				String id = BuiltInRegistries.ITEM.getKey(item).toString();
				String name = stack.getHoverName().getString();
				boolean selected = screen.store().contains(id);
				if ((normalized.isEmpty() && selected)
					|| (!normalized.isEmpty() && (id.toLowerCase(Locale.ROOT).contains(normalized)
					|| name.toLowerCase(Locale.ROOT).contains(normalized)))) {
					items.add(item);
				}
			}
			items.sort(Comparator.comparing(item -> item.getDefaultInstance().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER));
			for (Item item : items) {
				addEntry(new ItemEntry(screen, minecraft, item));
			}
			if (items.isEmpty()) {
				addEntry(new ItemEntry(screen, minecraft, null));
			}
		}

		@Override
		public int getRowWidth() {
			return Math.min(width - 40, 520);
		}
	}

	private static final class ItemEntry extends ObjectSelectionList.Entry<ItemEntry> {
		private final MassDropScreen screen;
		private final Minecraft client;
		private final Item item;
		private final Button toggle;

		private ItemEntry(MassDropScreen screen, Minecraft client, Item item) {
			this.screen = screen;
			this.client = client;
			this.item = item;
			this.toggle = item == null ? null : Button.builder(toggleLabel(), button -> toggle()).size(28, 20).build();
		}

		@Override
		public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			if (item == null) {
				Component empty = Component.translatable("emutils.mass_drop.empty").withStyle(ChatFormatting.GRAY);
				context.text(client.font, empty, getContentXMiddle() - client.font.width(empty) / 2, getContentY() + 12, CommonColors.GRAY);
				return;
			}
			ItemStack stack = item.getDefaultInstance();
			String id = BuiltInRegistries.ITEM.getKey(item).toString();
			int x = getContentX() + 4;
			int y = getContentY() + 3;
			context.item(stack, x, y + 5, 0);
			context.text(client.font, stack.getHoverName(), x + 24, y + 2, CommonColors.WHITE);
			context.text(client.font, Component.literal(id).withStyle(ChatFormatting.DARK_GRAY), x + 24, y + 15, CommonColors.GRAY);
			toggle.setPosition(getContentRight() - 34, y + 5);
			toggle.setMessage(toggleLabel());
			toggle.extractRenderState(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
			return toggle != null && toggle.mouseClicked(click, doubled);
		}

		@Override
		public boolean mouseReleased(MouseButtonEvent click) {
			return toggle != null && toggle.mouseReleased(click);
		}

		@Override
		public void visitWidgets(Consumer<AbstractWidget> consumer) {
			if (toggle != null) {
				consumer.accept(toggle);
			}
		}

		@Override
		public Component getNarration() {
			return item == null ? Component.translatable("emutils.mass_drop.empty") : item.getDefaultInstance().getHoverName();
		}

		private Component toggleLabel() {
			if (item == null) {
				return Component.empty();
			}
			String id = BuiltInRegistries.ITEM.getKey(item).toString();
			return Component.literal(screen.store().contains(id) ? "−" : "+");
		}

		private void toggle() {
			screen.store().toggle(BuiltInRegistries.ITEM.getKey(item).toString());
			screen.refreshList();
		}
	}
}
