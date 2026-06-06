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

	@Invoker("onMove")
	void emutils$onCursorPos(long window, double x, double y);
}
