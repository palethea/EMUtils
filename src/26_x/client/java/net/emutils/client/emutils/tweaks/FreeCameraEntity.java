package net.emutils.client.emutils.tweaks;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

final class FreeCameraEntity extends Entity {
	FreeCameraEntity(Entity source, Level level) {
		super(source.getType(), level);
		noPhysics = true;
		setPos(source.getX(), source.getEyeY() - getEyeHeight(), source.getZ());
		setYRot(source.getYRot());
		setXRot(source.getXRot());
		setOldPosAndRot();
	}

	@Override
	public boolean isSpectator() {
		return true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
