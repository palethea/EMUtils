package net.emutils.client.mixin;

import net.emutils.client.accessor.KeyBindingAccess;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBinding.class)
public abstract class KeyBindingAccessorMixin implements KeyBindingAccess {
	@Accessor("boundKey")
	@Override
	public abstract InputUtil.Key emutils$getBoundKey();
}
