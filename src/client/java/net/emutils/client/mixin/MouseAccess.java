package net.emutils.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.client.Mouse;

@Mixin(Mouse.class)
public interface MouseAccess {
	@Accessor("x")
	void emutils$setX(double x);

	@Accessor("y")
	void emutils$setY(double y);

	@Invoker("onCursorPos")
	void emutils$onCursorPos(long window, double x, double y);
}
