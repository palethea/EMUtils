package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.reconnect.AutoReconnectManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screen.DisconnectedScreen")
public abstract class DisconnectedScreenMixin extends Screen {
	@Shadow
	@Final
	private DirectionalLayoutWidget grid;

	@Unique
	private ButtonWidget emutils$reconnectButton;

	protected DisconnectedScreenMixin(Text title) {
		super(title);
	}

	@Inject(
		method = "init",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;refreshPositions()V", shift = At.Shift.BEFORE)
	)
	private void emutils$addReconnectButton(CallbackInfo ci) {
		AutoReconnectManager manager = EMUtilsClient.autoReconnect();
		if (!manager.enabled() || !manager.hasServer()) {
			return;
		}

		emutils$reconnectButton = ButtonWidget.builder(
			manager.buttonText(),
			button -> manager.reconnectNow(MinecraftClient.getInstance(), this)
		).width(200).build();
		this.grid.add(emutils$reconnectButton);
		manager.setReconnectButton(emutils$reconnectButton);
	}
}
