package net.emutils.client.mixin;

import java.util.List;
import net.emutils.client.chat.ChatHudAccess;
import net.emutils.client.death.DeathWaypointChatAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin implements DeathWaypointChatAccess, ChatHudAccess {
	@Shadow
	@Final
	private MinecraftClient client;

	@Shadow
	@Final
	private List<ChatHudLine> messages;

	@Shadow
	@Final
	private List<ChatHudLine.Visible> visibleMessages;

	@Shadow
	private int scrolledLines;

	@Invoker("refresh")
	protected abstract void emutils$invokeRefresh();

	@Shadow
	private boolean isChatHidden() {
		throw new AssertionError();
	}

	@Shadow
	private int getWidth() {
		throw new AssertionError();
	}

	@Shadow
	private double getChatScale() {
		throw new AssertionError();
	}

	@Shadow
	private int getLineHeight() {
		throw new AssertionError();
	}

	@Shadow
	private int getVisibleLineCount() {
		throw new AssertionError();
	}

	@Override
	public void emutils$removeMessageSilently(MessageSignatureData signature) {
		if (signature == null) {
			return;
		}

		boolean removed = messages.removeIf(line -> signature.equals(line.signature()));
		if (removed) {
			emutils$invokeRefresh();
		}
	}

	@Override
	@Nullable
	public String emutils$getMessageAt(double mouseX, double mouseY) {
		if (isChatHidden() || visibleMessages.isEmpty()) {
			return null;
		}

		float scale = (float) getChatScale();
		Vector2f untransformed = new Vector2f();
		Matrix3x2f pose = new Matrix3x2f();
		pose.scale(scale, scale);
		pose.translate(4.0F, 0.0F);
		pose.invert(new Matrix3x2f()).transformPosition((float) mouseX, (float) mouseY, untransformed);

		int windowHeight = client.getWindow().getScaledHeight();
		int chatWidth = MathHelper.ceil(getWidth() / scale);
		int chatBottom = MathHelper.floor((windowHeight - 40) / scale);
		int lineHeight = getLineHeight();
		int visibleRows = Math.min(visibleMessages.size() - scrolledLines, getVisibleLineCount());

		for (int row = visibleRows - 1; row >= 0; row--) {
			int listIndex = row + scrolledLines;
			int top = chatBottom - row * lineHeight;
			int bottom = top - lineHeight;
			if (!DrawnTextConsumer.isWithinBounds(untransformed.x, untransformed.y, -4, bottom, chatWidth + 8, top)) {
				continue;
			}

			return findMessageTextForVisibleListIndex(listIndex);
		}

		return null;
	}

	@Nullable
	private String findMessageTextForVisibleListIndex(int listIndex) {
		if (listIndex < 0 || listIndex >= visibleMessages.size()) {
			return null;
		}

		TextRenderer textRenderer = client.textRenderer;
		int width = MathHelper.floor(getWidth() / getChatScale());
		int current = 0;

		for (ChatHudLine message : messages) {
			int lines = message.breakLines(textRenderer, width).size();
			if (listIndex >= current && listIndex < current + lines) {
				return message.content().getString();
			}

			current += lines;
		}

		return null;
	}
}
