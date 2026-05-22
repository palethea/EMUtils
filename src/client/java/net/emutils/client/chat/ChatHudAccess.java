package net.emutils.client.chat;

import org.jspecify.annotations.Nullable;

public interface ChatHudAccess {
	@Nullable
	String emutils$getMessageAt(double mouseX, double mouseY);
}
