package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.reconnect.AutoReconnectManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.DisconnectedScreen")
public abstract class DisconnectedScreenMixin extends Screen {
	@Shadow
	@Final
	private LinearLayout layout;

	@Unique
	private Button emutils$reconnectButton;

	protected DisconnectedScreenMixin(Component title) {
		super(title);
	}

	@Inject(
		method = "init",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;arrangeElements()V", shift = At.Shift.BEFORE)
	)
	private void emutils$addReconnectButton(CallbackInfo ci) {
		AutoReconnectManager manager = EMUtilsClient.autoReconnect();
		if (!manager.enabled() || !manager.hasServer()) {
			return;
		}

		emutils$reconnectButton = Button.builder(
			manager.buttonText(),
			button -> manager.reconnectNow(Minecraft.getInstance(), this)
		).width(200).build();
		this.layout.addChild(emutils$reconnectButton);
		manager.setReconnectButton(emutils$reconnectButton);
	}
}
