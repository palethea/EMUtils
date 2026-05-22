package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.reconnect.AutoReconnectManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screen.DisconnectedScreen")
public abstract class DisconnectedScreenMixin extends Screen {
	@Unique
	private ButtonWidget emutils$reconnectButton;

	protected DisconnectedScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void emutils$init(CallbackInfo ci) {
		AutoReconnectManager manager = EMUtilsClient.autoReconnect();
		if (!manager.enabled() || !manager.hasServer()) {
			return;
		}

		int buttonY = Math.min(height - 28, height / 2 + 58);
		emutils$reconnectButton = ButtonWidget.builder(manager.buttonText(), button -> manager.reconnectNow(MinecraftClient.getInstance(), this))
			.dimensions(width / 2 - 100, buttonY, 200, 20)
			.build();
		addDrawableChild(emutils$reconnectButton);
		manager.setReconnectButton(emutils$reconnectButton);
	}
}
