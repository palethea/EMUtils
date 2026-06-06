package net.emutils.client.emutils.chat;

import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public interface ChatHudAccess {
	@Nullable
	Text emutils$getMessageAt(double mouseX, double mouseY);

	void emutils$refreshDisplayedMessages();
}
