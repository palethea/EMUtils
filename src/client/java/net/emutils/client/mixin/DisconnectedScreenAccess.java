package net.emutils.client.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.gui.screen.DisconnectedScreen")
public interface DisconnectedScreenAccess {
	@Accessor("parent")
	Screen emutils$getParentScreen();
}
