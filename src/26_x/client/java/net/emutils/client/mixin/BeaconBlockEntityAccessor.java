package net.emutils.client.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BeaconBlockEntity.class)
public interface BeaconBlockEntityAccessor {
	@Accessor("levels")
	int emutils$getLevels();

	/**
	 * The beacon's primary effect, or null before one is picked. Clients get it when the chunk loads,
	 * not when it changes, so {@code BeaconRadiusRenderer} also sets it when this player picks one.
	 */
	@Accessor("primaryPower")
	@Nullable Holder<MobEffect> emutils$getPrimaryPower();

	@Accessor("primaryPower")
	void emutils$setPrimaryPower(@Nullable Holder<MobEffect> effect);
}
