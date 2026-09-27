package net.emutils.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
public interface MouseAccess {
	@Accessor("xpos")
	double emutils$getX();

	@Accessor("ypos")
	double emutils$getY();

	@Accessor("xpos")
	void emutils$setX(double x);

	@Accessor("ypos")
	void emutils$setY(double y);

	/** Mouse movement waiting to turn the player; used by UI snapshots. */
	@Accessor("accumulatedDX")
	void emutils$setAccumulatedDX(double dx);

	/** Turns the player (or Free Camera or Freelook) by the accumulated movement; used by UI snapshots. */
	@Invoker("turnPlayer")
	void emutils$turnPlayer(double mousea);
}
