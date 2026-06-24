package net.emutils.client.mixin;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientLevel.class)
public abstract class ClientLevelAmbientParticleMixin {
	@Redirect(
		method = "doAnimateTick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/core/BlockPos;)Ljava/lang/Object;"
		)
	)
	private Object emutils$removeNetherAmbientParticles(
		EnvironmentAttributeSystem attributes,
		EnvironmentAttribute<?> attribute,
		BlockPos position
	) {
		ClientLevel level = (ClientLevel) (Object) this;
		if (attribute == EnvironmentAttributes.AMBIENT_PARTICLES
			&& level.dimension() == Level.NETHER
			&& EMUtilsClient.config() != null
			&& EMUtilsClient.config().tweakNoNetherParticles()) {
			return List.of();
		}
		return getValue(attributes, attribute, position);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Object getValue(
		EnvironmentAttributeSystem attributes,
		EnvironmentAttribute<?> attribute,
		BlockPos position
	) {
		return attributes.getValue((EnvironmentAttribute) attribute, position);
	}
}
