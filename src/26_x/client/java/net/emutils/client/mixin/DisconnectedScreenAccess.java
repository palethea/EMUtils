package net.emutils.client.mixin;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.gui.screens.DisconnectedScreen")
public interface DisconnectedScreenAccess {
	@Accessor("parent")
	Screen emutils$getParentScreen();
}
