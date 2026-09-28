package net.emutils.client.emutils.hud;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.EMUtilsHudElements;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.ui.UiRasterScale;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Armor Status (#44): the worn armor and held items in a card in the HUD Overlay's look, each with how
 * much durability it has left and a durability bar. Items at or below Low Durability turn the warning
 * color, and can flash and play a sound. While an elytra is worn, a row counts the firework rockets
 * carried.
 * <p>
 * Its place in the HUD layout is sized for every row that's turned on, so it doesn't jump around as
 * armor goes on and off. The card sticks to the top of that slot, or to the bottom in the lower half of
 * the screen.
 */
public final class ArmorStatusRenderer {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "armor_status");
	private static final int ITEM_SIZE = 16;
	private static final int ITEM_GAP = 5;
	private static final int ROW_GAP = 2;
	private static final int PADDING_X = 6;
	private static final int PADDING_Y = 5;
	private static final int BAR_HEIGHT = 2;
	private static final int BAR_GAP = 2;
	private static final int MIN_TEXT_WIDTH = 22;
	private static final int RIGHT_MARGIN = 8;
	/** One flash of a low item, off and on, in milliseconds. */
	private static final float FLASH_PERIOD_MS = 900.0F;
	private static final UiText.Size TEXT_SIZE = UiText.Size.LABEL;
	private static final Map<Slot, LowState> LOW_STATES = new EnumMap<>(Slot.class);
	private static int sounds;

	private ArmorStatusRenderer() {
	}

	/** The rows the card can show, top to bottom. */
	enum Slot {
		HEAD(EquipmentSlot.HEAD, Items.DIAMOND_HELMET, 120),
		CHEST(EquipmentSlot.CHEST, Items.ELYTRA, 40),
		LEGS(EquipmentSlot.LEGS, Items.DIAMOND_LEGGINGS, 60),
		FEET(EquipmentSlot.FEET, Items.DIAMOND_BOOTS, 380),
		MAINHAND(EquipmentSlot.MAINHAND, Items.DIAMOND_PICKAXE, 900),
		OFFHAND(EquipmentSlot.OFFHAND, Items.SHIELD, 30);

		private final EquipmentSlot equipment;
		private final Item sample;
		private final int sampleDamage;

		Slot(EquipmentSlot equipment, Item sample, int sampleDamage) {
			this.equipment = equipment;
			this.sample = sample;
			this.sampleDamage = sampleDamage;
		}

		boolean enabled(EMUtilsConfig config) {
			return switch (this) {
				case HEAD -> config.armorStatusHelmet();
				case CHEST -> config.armorStatusChestplate();
				case LEGS -> config.armorStatusLeggings();
				case FEET -> config.armorStatusBoots();
				case MAINHAND -> config.armorStatusMainHand();
				case OFFHAND -> config.armorStatusOffHand();
			};
		}

		boolean hand() {
			return this == MAINHAND || this == OFFHAND;
		}

		ItemStack sample() {
			ItemStack stack = new ItemStack(sample);
			stack.setDamageValue(sampleDamage);
			return stack;
		}
	}

	/** One row of the card: an item, and its durability or how many are carried. */
	private record Row(ItemStack icon, @Nullable String text, float bar, int barColor, boolean low) {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, (context, tickCounter) -> render(context));
	}

	/** Plays the low durability sound when a shown item drops to Low Durability, or one that low is put on. */
	public static void tick(Minecraft client) {
		EMUtilsConfig config = EMUtilsClient.config();
		Player player = client.player;
		if (config == null || player == null || !config.armorStatus() || !config.armorStatusSound()) {
			LOW_STATES.clear();
			return;
		}
		// Anti-Durability Break already warns about held items with a sound of its own.
		boolean handsWarned = config.tweakAntiDurabilityBreak() && config.antiDurabilityWarning();
		boolean play = false;
		for (Slot slot : Slot.values()) {
			if (!slot.enabled(config) || (slot.hand() && handsWarned)) {
				LOW_STATES.remove(slot);
				continue;
			}
			ItemStack stack = player.getItemBySlot(slot.equipment);
			boolean low = isLow(stack, config.armorStatusLowPercent());
			String key = stack.isEmpty() ? null : stack.getItem().toString();
			LowState last = LOW_STATES.get(slot);
			if (low && (last == null || !last.low() || !Objects.equals(last.key(), key))) {
				play = true;
			}
			LOW_STATES.put(slot, new LowState(key, low));
		}
		if (play) {
			sounds++;
			client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F, 0.6F));
		}
	}

	/** How many low durability sounds have played, for UI snapshot checks. */
	public static int soundsForSnapshot() {
		return sounds;
	}

	private record LowState(@Nullable String key, boolean low) {
	}

	static boolean isLow(ItemStack stack, int lowPercent) {
		if (stack.isEmpty() || !stack.isDamageableItem()) {
			return false;
		}
		int remaining = stack.getMaxDamage() - stack.getDamageValue();
		return remaining * 100L <= (long) lowPercent * stack.getMaxDamage();
	}

	/** How many rows are turned on, which sizes the card's place in the HUD layout. */
	private static int enabledRows(EMUtilsConfig config) {
		int rows = 0;
		for (Slot slot : Slot.values()) {
			if (slot.enabled(config)) {
				rows++;
			}
		}
		return config.armorStatusFireworks() ? rows + 1 : rows;
	}

	public static int width(EMUtilsConfig config) {
		Font font = Minecraft.getInstance().font;
		int textWidth = Math.max(MIN_TEXT_WIDTH, UiText.glyphsWidth(font, config.armorStatusDisplay().widest(), TEXT_SIZE));
		return PADDING_X * 2 + ITEM_SIZE + ITEM_GAP + textWidth;
	}

	public static int slotHeight(EMUtilsConfig config) {
		return cardHeight(Math.max(1, enabledRows(config)));
	}

	private static int cardHeight(int rows) {
		return PADDING_Y * 2 + rows * ITEM_SIZE + Math.max(0, rows - 1) * ROW_GAP;
	}

	static int defaultX(int screenWidth, int width) {
		return screenWidth - width - RIGHT_MARGIN;
	}

	static int defaultY(int screenHeight, int height) {
		return (screenHeight - height) / 2;
	}

	/**
	 * The rows to draw. With {@code preview}, for the layout editor, empty slots and a missing elytra get
	 * sample items, so every row that's turned on shows.
	 */
	private static List<Row> rows(@Nullable Player player, EMUtilsConfig config, boolean preview) {
		List<Row> rows = new ArrayList<>();
		int lowPercent = config.armorStatusLowPercent();
		ArmorStatusDisplay display = config.armorStatusDisplay();
		for (Slot slot : Slot.values()) {
			if (!slot.enabled(config)) {
				continue;
			}
			ItemStack stack = player == null ? ItemStack.EMPTY : player.getItemBySlot(slot.equipment);
			if (stack.isEmpty() && preview) {
				stack = slot.sample();
			}
			if (!stack.isEmpty()) {
				rows.add(row(player, slot, stack, display, lowPercent, config.armorStatusBar()));
			}
		}
		boolean elytra = player != null && player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
		if (config.armorStatusFireworks() && (elytra || preview)) {
			int count = player == null ? 0 : count(player.getInventory(), Items.FIREWORK_ROCKET);
			if (preview && !elytra) {
				count = 24;
			}
			rows.add(new Row(new ItemStack(Items.FIREWORK_ROCKET), Integer.toString(count), -1.0F, 0, count == 0));
		}
		return rows;
	}

	private static Row row(@Nullable Player player, Slot slot, ItemStack stack, ArmorStatusDisplay display, int lowPercent, boolean bar) {
		if (stack.isDamageableItem()) {
			int max = stack.getMaxDamage();
			int remaining = max - stack.getDamageValue();
			float fraction = bar ? Math.clamp(remaining / (float) max, 0.0F, 1.0F) : -1.0F;
			return new Row(stack, display.format(remaining, max), fraction, stack.getBarColor() | 0xFF000000, isLow(stack, lowPercent));
		}
		// Held blocks, arrows and other stackables show how many are carried; worn heads and pumpkins, and
		// other items, just their icon.
		String text = null;
		if (slot.hand() && stack.isStackable()) {
			text = Integer.toString(player == null ? stack.getCount() : count(player.getInventory(), stack.getItem()));
		}
		return new Row(stack, text, -1.0F, 0, false);
	}

	/** How many of {@code item} the inventory holds, the hands and armor included. */
	private static int count(Inventory inventory, Item item) {
		int count = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/**
	 * Draws the card inside its slot at ({@code x}, {@code y}), at the slot's top, or its bottom when
	 * {@code bottom}.
	 */
	static void renderInSlot(GuiGraphicsExtractor context, Minecraft client, EMUtilsConfig config, int x, int y, boolean bottom, boolean preview, int opacityPercent) {
		List<Row> rows = rows(client.player, config, preview);
		if (rows.isEmpty()) {
			return;
		}
		int width = width(config);
		int height = cardHeight(rows.size());
		int top = bottom ? y + slotHeight(config) - height : y;
		UiTheme theme = UiTheme.current();
		HudOverlayRenderer.drawCard(context, theme, x, top, width, height, opacityPercent);
		Font font = client.font;
		boolean shadow = opacityPercent < HudOverlayRenderer.TEXT_SHADOW_BELOW_OPACITY;
		float flash = config.armorStatusFlash() ? flash() : 1.0F;
		int itemX = x + PADDING_X;
		int textX = itemX + ITEM_SIZE + ITEM_GAP;
		int textWidth = width - PADDING_X * 2 - ITEM_SIZE - ITEM_GAP;
		int rowY = top + PADDING_Y;
		for (Row row : rows) {
			context.item(row.icon(), itemX, rowY);
			int textColor = row.low() ? UiTheme.fade(theme.warning(), flash) : theme.text();
			int lineHeight = UiText.lineHeight(font, TEXT_SIZE);
			boolean bar = row.bar() >= 0.0F;
			int blockHeight = lineHeight + (bar ? BAR_GAP + BAR_HEIGHT : 0);
			int textTop = rowY + (ITEM_SIZE - blockHeight) / 2;
			if (row.text() != null) {
				if (shadow) {
					float offset = HudOverlayRenderer.shadowOffset();
					UiText.drawGlyphs(context, font, row.text(), TEXT_SIZE, textX + offset, textTop + offset, HudOverlayRenderer.shadowColor());
				}
				UiText.drawGlyphs(context, font, row.text(), TEXT_SIZE, textX, textTop, textColor);
			}
			if (bar) {
				int barY = textTop + lineHeight + BAR_GAP;
				UiShapes.pill(context, textX, barY, textWidth, BAR_HEIGHT, theme.line());
				int fill = Math.round(textWidth * row.bar());
				if (fill > 0) {
					int barColor = row.low() ? UiTheme.fade(theme.warning(), flash) : row.barColor();
					UiShapes.pill(context, textX, barY, Math.max(BAR_HEIGHT, fill), BAR_HEIGHT, barColor);
				}
			}
			rowY += ITEM_SIZE + ROW_GAP;
		}
	}

	/** Fades low items in and out, never all the way out, so they stay readable. */
	private static float flash() {
		float phase = (Util.getMillis() % (long) FLASH_PERIOD_MS) / FLASH_PERIOD_MS;
		return 0.35F + 0.65F * (0.5F + 0.5F * (float) Math.cos(phase * Math.PI * 2.0));
	}

	private static void render(GuiGraphicsExtractor context) {
		EMUtilsConfig config = EMUtilsClient.config();
		Minecraft client = Minecraft.getInstance();
		if (config == null || client == null || client.player == null || client.level == null) {
			return;
		}
		// The layout editor draws its own preview.
		if (!config.armorStatus() || HudLayoutManager.isEditing()) {
			return;
		}
		if (MinecraftClientCompat.isHudHidden(client) || client.player.isSpectator()) {
			return;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}
		if (config.armorStatusHideInContainers() && MinecraftClientCompat.screen(client) instanceof AbstractContainerScreen<?>) {
			return;
		}

		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(
			EMUtilsHudElements.ARMOR_STATUS,
			config,
			context.guiWidth(),
			context.guiHeight(),
			client
		);
		boolean bottom = layout.position().y() + layout.dimensions().height() / 2 > context.guiHeight() / 2;
		context.pose().pushMatrix();
		UiRasterScale.set(layout.scaleFactor());
		try {
			context.pose().translate(layout.position().x(), layout.position().y());
			context.pose().scale(layout.scaleFactor(), layout.scaleFactor());
			renderInSlot(context, client, config, 0, 0, bottom, false, layout.opacityPercent());
		} finally {
			UiRasterScale.reset();
			context.pose().popMatrix();
		}
	}
}
