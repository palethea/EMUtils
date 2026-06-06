package net.emutils.client.emutils.chat;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public interface ChatHudAccess {
	@Nullable
	Component emutils$getMessageAt(double mouseX, double mouseY);

	void emutils$refreshDisplayedMessages();
}
