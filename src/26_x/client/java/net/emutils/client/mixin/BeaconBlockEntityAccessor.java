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

	/** The beacon's primary effect, synced to the client with the block entity; null until one is picked. */
	@Accessor("primaryPower")
	@Nullable Holder<MobEffect> emutils$getPrimaryPower();
}
