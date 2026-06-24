package net.emutils.client.mixin;

import java.util.function.BooleanSupplier;
import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftFreeCameraInputMixin {
	@Shadow
	@Nullable
	public HitResult hitResult;

	@Shadow
	@Nullable
	public Entity crosshairPickEntity;

	@Shadow
	public abstract Entity getCameraEntity();

	@Shadow
	public abstract void setCameraEntity(Entity entity);

	@Shadow
	private void pick(float tickDelta) {
	}

	@Shadow
	private boolean startAttack() {
		return false;
	}

	@Shadow
	private void continueAttack(boolean leftClick) {
	}

	@Shadow
	private void startUseItem() {
	}

	@Unique
	private boolean emutils$replayingFreeCameraInput;

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void emutils$handleFreeCameraAttack(CallbackInfoReturnable<Boolean> cir) {
		if (active() && !emutils$replayingFreeCameraInput) {
			cir.setReturnValue(emutils$runWithPlayerTarget(this::startAttack));
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void emutils$handleFreeCameraAttackHold(boolean leftClick, CallbackInfo ci) {
		if (active() && !emutils$replayingFreeCameraInput) {
			emutils$runWithPlayerTarget(() -> continueAttack(leftClick));
			ci.cancel();
		}
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void emutils$handleFreeCameraUse(CallbackInfo ci) {
		if (active() && !emutils$replayingFreeCameraInput) {
			emutils$runWithPlayerTarget(this::startUseItem);
			ci.cancel();
		}
	}

	@Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
	private void emutils$preventFreeCameraPick(CallbackInfo ci) {
		if (active()) {
			ci.cancel();
		}
	}

	private static boolean active() {
		return EMUtilsClient.tweaks() != null && EMUtilsClient.tweaks().freeCamera().isActive();
	}

	@Unique
	private boolean emutils$runWithPlayerTarget(BooleanSupplier action) {
		Entity player = ((Minecraft) (Object) this).player;
		if (player == null) {
			return false;
		}

		Entity previousCamera = getCameraEntity();
		HitResult previousHitResult = hitResult;
		Entity previousPickEntity = crosshairPickEntity;
		emutils$replayingFreeCameraInput = true;
		try {
			setCameraEntity(player);
			pick(1.0F);
			return action.getAsBoolean();
		} finally {
			setCameraEntity(previousCamera == null ? player : previousCamera);
			hitResult = previousHitResult;
			crosshairPickEntity = previousPickEntity;
			emutils$replayingFreeCameraInput = false;
		}
	}

	@Unique
	private void emutils$runWithPlayerTarget(Runnable action) {
		emutils$runWithPlayerTarget(() -> {
			action.run();
			return true;
		});
	}
}
