package net.emutils.client.mixin;

import net.emhelpers.client.accessor.KeyBindingAccess;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public abstract class KeyBindingAccessorMixin implements KeyBindingAccess {
	@Accessor("key")
	@Override
	public abstract InputConstants.Key emhelpers$getBoundKey();
}
