package net.emutils.client.mixin;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
	private static final int BUTTON_WIDTH = 140;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_MARGIN = 8;

	@Unique
	private ButtonWidget emutils$clearWaypointsButton;

	protected GameMenuScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void emutils$init(CallbackInfo ci) {
		if (!EMUtilsClient.config().deathWaypoint()) {
			emutils$clearWaypointsButton = null;
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		emutils$clearWaypointsButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.OPTION_CLEAR_WAYPOINTS), button -> {
			EMUtilsClient.deathWaypoint().clearForCurrentWorld(client);
			button.active = false;
		}).dimensions(BUTTON_MARGIN, BUTTON_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT).build();
		emutils$clearWaypointsButton.active = EMUtilsClient.deathWaypoint().hasWaypointForCurrentWorld(client);
		addDrawableChild(emutils$clearWaypointsButton);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void emutils$tick(CallbackInfo ci) {
		if (emutils$clearWaypointsButton != null) {
			emutils$clearWaypointsButton.active = EMUtilsClient.deathWaypoint()
				.hasWaypointForCurrentWorld(MinecraftClient.getInstance());
		}
	}
}
