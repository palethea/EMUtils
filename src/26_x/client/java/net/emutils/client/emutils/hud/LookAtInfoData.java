package net.emutils.client.emutils.hud;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * What Look-At Info (#45) shows about the block or entity in the crosshair: a header with its item,
 * name and ID, then the lines turned on in its settings. Collected once a tick from the client's own
 * hit result, so it follows the player's reach.
 */
public record LookAtInfoData(ItemStack icon, String name, String id, List<HudOverlayLine> lines) {
	private static final String[] TIERS = {"wood", "stone", "copper", "iron", "diamond", "netherite"};
	/** Every tool type, weakest tier first, so the first one that gets drops is the lowest tier that does. */
	private static final List<ToolType> TOOL_TYPES = List.of(
		new ToolType("pickaxe", Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.COPPER_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE),
		new ToolType("axe", Items.WOODEN_AXE, Items.STONE_AXE, Items.COPPER_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE),
		new ToolType("shovel", Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.COPPER_SHOVEL, Items.IRON_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL),
		new ToolType("hoe", Items.WOODEN_HOE, Items.STONE_HOE, Items.COPPER_HOE, Items.IRON_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE),
		new ToolType("sword", Items.WOODEN_SWORD, Items.STONE_SWORD, Items.COPPER_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD),
		new ToolType("shears", Items.SHEARS)
	);

	private static @Nullable LookAtInfoData current;

	public static @Nullable LookAtInfoData current() {
		return current;
	}

	public static void tick(Minecraft client, @Nullable EMUtilsConfig config) {
		current = config == null || client.player == null || client.level == null ? null : collect(client, config);
	}

	private static @Nullable LookAtInfoData collect(Minecraft client, EMUtilsConfig config) {
		HitResult hit = client.hitResult;
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK && config.lookAtInfoBlocks()) {
			return block(client, config, blockHit.getBlockPos());
		}
		if (hit instanceof EntityHitResult entityHit && hit.getType() == HitResult.Type.ENTITY && config.lookAtInfoEntities()) {
			return entity(client, config, entityHit.getEntity());
		}
		return null;
	}

	/** Stone at a made-up position, for the layout editor while nothing is targeted. */
	public static LookAtInfoData preview(EMUtilsConfig config) {
		List<HudOverlayLine> lines = new ArrayList<>();
		if (config.lookAtInfoPosition()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_LOOK_AT_POSITION, "128 64 -32", HudOverlayLine.icon("coords")));
		}
		if (config.lookAtInfoHardness()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_LOOK_AT_HARDNESS, "1.5", HubIcons.HAMMER));
		}
		if (config.lookAtInfoBreakTime()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_LOOK_AT_BREAK_TIME, "0.40 s", HudOverlayLine.icon("real_time")));
		}
		if (config.lookAtInfoTool()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_LOOK_AT_TOOL, toolValue("pickaxe", "wood"), HubIcons.PICKAXE));
		}
		if (config.lookAtInfoHarvest()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_LOOK_AT_HARVEST, yesNo(true), HubIcons.CHECK, HudOverlayLine.Tone.GOOD));
		}
		ItemStack stone = new ItemStack(Items.STONE);
		return new LookAtInfoData(stone, stone.getHoverName().getString(), "minecraft:stone", lines);
	}

	private static LookAtInfoData block(Minecraft client, EMUtilsConfig config, BlockPos pos) {
		BlockState state = client.level.getBlockState(pos);
		List<HudOverlayLine> lines = new ArrayList<>();
		if (config.lookAtInfoPosition()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_LOOK_AT_POSITION, pos.getX() + " " + pos.getY() + " " + pos.getZ(), HudOverlayLine.icon("coords")));
		}
		float hardness = state.getDestroySpeed(client.level, pos);
		if (config.lookAtInfoHardness()) {
			String value = hardness < 0.0F ? text(EMUtilsTexts.HUD_LOOK_AT_UNBREAKABLE) : number(hardness);
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_LOOK_AT_HARDNESS, value, HubIcons.HAMMER));
		}
		if (config.lookAtInfoBreakTime()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_LOOK_AT_BREAK_TIME, breakTime(client, state, pos, hardness), HudOverlayLine.icon("real_time")));
		}
		if (config.lookAtInfoTool()) {
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_LOOK_AT_TOOL, tool(state), HubIcons.PICKAXE));
		}
		// Only for blocks that need a certain tool to drop anything; every other block always drops.
		if (config.lookAtInfoHarvest() && hardness >= 0.0F && state.requiresCorrectToolForDrops()) {
			boolean harvest = client.player.hasCorrectToolForDrops(state);
			lines.add(new HudOverlayLine(EMUtilsTexts.HUD_LOOK_AT_HARVEST, yesNo(harvest), HubIcons.CHECK, harvest ? HudOverlayLine.Tone.GOOD : HudOverlayLine.Tone.BAD));
		}
		Item item = state.getBlock().asItem();
		ItemStack icon = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
		return new LookAtInfoData(icon, state.getBlock().getName().getString(), BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), lines);
	}

	private static LookAtInfoData entity(Minecraft client, EMUtilsConfig config, Entity entity) {
		List<HudOverlayLine> lines = new ArrayList<>();
		String name = entity.getDisplayName().getString();
		ItemStack icon = entity.getPickResult();
		if (entity instanceof ItemEntity itemEntity) {
			ItemStack stack = itemEntity.getItem();
			icon = stack;
			name = stack.getHoverName().getString() + (stack.getCount() > 1 ? " ×" + stack.getCount() : "");
		}
		if (entity instanceof LivingEntity living) {
			if (config.lookAtInfoHealth()) {
				lines.add(health(living));
			}
			if (config.lookAtInfoArmor() && living.getArmorValue() > 0) {
				lines.add(new HudOverlayLine(EMUtilsTexts.HUD_LOOK_AT_ARMOR, Integer.toString(living.getArmorValue()), HubIcons.SHIELD));
			}
			if (config.lookAtInfoEffects()) {
				float tickRate = client.level.tickRateManager().tickrate();
				for (MobEffectInstance effect : effects(client, living)) {
					lines.add(effect(effect, tickRate));
				}
			}
		}
		return new LookAtInfoData(icon == null ? ItemStack.EMPTY : icon, name, EntityType.getKey(entity.getType()).toString(), lines);
	}

	private static HudOverlayLine health(LivingEntity living) {
		float health = living.getHealth();
		float max = living.getMaxHealth();
		String value = number(health) + " / " + number(max);
		if (living.getAbsorptionAmount() > 0.0F) {
			value += " +" + number(living.getAbsorptionAmount());
		}
		float fraction = max <= 0.0F ? 1.0F : health / max;
		HudOverlayLine.Tone tone = fraction > 0.5F ? HudOverlayLine.Tone.GOOD : fraction > 0.25F ? HudOverlayLine.Tone.SLOW : HudOverlayLine.Tone.BAD;
		return new HudOverlayLine(EMUtilsTexts.HUD_LOOK_AT_HEALTH, value, HubIcons.HEART, tone);
	}

	/** One line per effect: its name, then its level (from II up) and the time left, green when it helps. */
	private static HudOverlayLine effect(MobEffectInstance effect, float tickRate) {
		String duration = MobEffectUtil.formatDuration(effect, 1.0F, tickRate).getString();
		int level = effect.getAmplifier() + 1;
		String value = level > 1 ? level(level) + " · " + duration : duration;
		HudOverlayLine.Tone tone = effect.getEffect().value().isBeneficial() ? HudOverlayLine.Tone.GOOD : HudOverlayLine.Tone.BAD;
		return new HudOverlayLine(effect.getDescriptionId(), value, HubIcons.SPARKLES, tone);
	}

	/**
	 * A server only sends a player their own effects and those of what they ride, so other mobs'
	 * effects come from the integrated server in singleplayer, and aren't known on a server.
	 */
	private static Collection<MobEffectInstance> effects(Minecraft client, LivingEntity living) {
		if (!living.getActiveEffects().isEmpty()) {
			return List.copyOf(living.getActiveEffects());
		}
		IntegratedServer server = client.getSingleplayerServer();
		if (server == null) {
			return List.of();
		}
		ServerLevel level = server.getLevel(client.level.dimension());
		if (level == null) {
			return List.of();
		}
		try {
			// Read from the client thread while the server ticks, so a copy may fail; the next tick tries again.
			if (level.getEntity(living.getUUID()) instanceof LivingEntity serverLiving) {
				return List.copyOf(serverLiving.getActiveEffects());
			}
		} catch (RuntimeException ignored) {
		}
		return List.of();
	}

	/** How long breaking the block takes with what's in hand, counting enchantments, effects and being in water or midair. */
	private static String breakTime(Minecraft client, BlockState state, BlockPos pos, float hardness) {
		if (hardness < 0.0F) {
			return text(EMUtilsTexts.HUD_LOOK_AT_NEVER);
		}
		float progress = state.getDestroyProgress(client.player, client.level, pos);
		if (progress <= 0.0F) {
			return text(EMUtilsTexts.HUD_LOOK_AT_NEVER);
		}
		if (progress >= 1.0F) {
			return text(EMUtilsTexts.HUD_LOOK_AT_INSTANT);
		}
		int ticks = (int) Math.ceil(1.0F / progress);
		float tickRate = client.level.tickRateManager().tickrate();
		return String.format(Locale.ENGLISH, "%.2f s", ticks / (tickRate <= 0.0F ? 20.0F : tickRate));
	}

	/**
	 * The tool that mines the block fastest, and the lowest tier that gets drops when it needs one.
	 * Worked out from the vanilla tools themselves, so it matches the game's own rules for every block.
	 */
	private static String tool(BlockState state) {
		ToolType fastest = null;
		float fastestSpeed = 1.0F;
		for (ToolType type : TOOL_TYPES) {
			float speed = type.best().getDestroySpeed(state);
			if (speed > fastestSpeed) {
				fastest = type;
				fastestSpeed = speed;
			}
		}
		if (!state.requiresCorrectToolForDrops()) {
			return fastest == null ? text(EMUtilsTexts.HUD_LOOK_AT_ANY_TOOL) : toolValue(fastest.name(), null);
		}
		// Blocks that need a tool name the fastest one that works, or any that works.
		List<ToolType> candidates = new ArrayList<>(TOOL_TYPES);
		if (fastest != null) {
			candidates.remove(fastest);
			candidates.addFirst(fastest);
		}
		for (ToolType type : candidates) {
			int tier = type.lowestTier(state);
			if (tier >= 0) {
				// Shears have no tiers.
				return toolValue(type.name(), type.tiers().size() == 1 ? null : TIERS[tier]);
			}
		}
		return text(EMUtilsTexts.HUD_LOOK_AT_ANY_TOOL);
	}

	/** The Tool line's value for {@code state}, for the UI snapshot checks. */
	public static String toolForSnapshot(BlockState state) {
		return tool(state);
	}

	private static String toolValue(String type, @Nullable String tier) {
		String tool = text("emutils.hud.look_at.tool." + type);
		return tier == null ? tool : Component.translatable(EMUtilsTexts.HUD_LOOK_AT_TOOL_TIER, tool, text("emutils.hud.look_at.tier." + tier)).getString();
	}

	private static String level(int level) {
		String key = "enchantment.level." + level;
		return level <= 10 ? text(key) : Integer.toString(level);
	}

	private static String yesNo(boolean yes) {
		return text(yes ? EMUtilsTexts.HUD_YES : EMUtilsTexts.HUD_NO);
	}

	private static String text(String key) {
		return Component.translatable(key).getString();
	}

	/** Whole numbers without a fraction, others with one decimal: 20, 7.5. */
	private static String number(float value) {
		return value == Math.floor(value) ? Integer.toString((int) value) : String.format(Locale.ENGLISH, "%.1f", value);
	}

	/** Holds items rather than stacks: stacks can only be made once the item components are loaded. */
	private record ToolType(String name, List<Item> tiers) {
		ToolType(String name, Item... tiers) {
			this(name, List.of(tiers));
		}

		ItemStack best() {
			return new ItemStack(tiers.getLast());
		}

		/** The index of the weakest tier that gets drops, or -1 if none does. */
		int lowestTier(BlockState state) {
			for (int i = 0; i < tiers.size(); i++) {
				if (new ItemStack(tiers.get(i)).isCorrectToolForDrops(state)) {
					return i;
				}
			}
			return -1;
		}
	}
}
