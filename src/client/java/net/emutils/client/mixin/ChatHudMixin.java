package net.emutils.client.mixin;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.chat.ChatDisplayFormatter;
import net.emutils.client.chat.ChatHudAccess;
import net.emutils.client.chat.ChatMentionAlerts;
import net.emutils.client.chat.ChatMentionDetector;
import net.emutils.client.chat.ChatMessageMetadata;
import net.emutils.client.chat.ChatMessageTracker;
import net.emutils.client.chat.ChatTimestampFormatter;
import net.emutils.client.chat.EMUtilsChatMessages;
import net.emutils.client.chat.SmartChatFilter;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.death.DeathWaypointChatAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin implements DeathWaypointChatAccess, ChatHudAccess {
	@Unique
	private static final int EMUTILS_MENTION_HIGHLIGHT_COLOR = 0x00FFD54A;
	@Unique
	private static final int EMUTILS_MENTION_HIGHLIGHT_ALPHA = 64;

	@Unique
	private final SmartChatFilter emutils$smartChatFilter = new SmartChatFilter();

	@Unique
	private final ChatMessageTracker emutils$messageTracker = new ChatMessageTracker();

	@Unique
	private boolean emutils$messageHandledByHead;

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

	@Invoker("logChatMessage")
	protected abstract void emutils$invokeLogChatMessage(ChatHudLine message);

	@Invoker("addVisibleMessage")
	protected abstract void emutils$invokeAddVisibleMessage(ChatHudLine message);

	@Invoker("addMessage")
	protected abstract void emutils$invokeStoreMessage(ChatHudLine message);

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

	@Inject(
		method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
		at = @At("HEAD")
	)
	private void emutils$handleMentionAlerts(
		Text message,
		@Nullable MessageSignatureData signatureData,
		@Nullable MessageIndicator indicator,
		CallbackInfo ci
	) {
		ChatMentionAlerts.handle(client, message);
	}

	@Inject(
		method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void emutils$processChatFeatures(
		Text message,
		@Nullable MessageSignatureData signatureData,
		@Nullable MessageIndicator indicator,
		CallbackInfo ci
	) {
		emutils$messageHandledByHead = false;
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || EMUtilsChatMessages.isInternal(message)) {
			return;
		}

		boolean timestamps = config.chatTimestamps();
		boolean smartFilters = config.smartChatFilters();
		if (!smartFilters) {
			emutils$smartChatFilter.clear();
		}

		if (!timestamps && !smartFilters) {
			return;
		}

		long nowMillis = System.currentTimeMillis();
		boolean mentionsCurrentPlayer = emutils$mentionsCurrentPlayer(message);
		ChatHudLine previousLine = messages.isEmpty() ? null : messages.getFirst();
		ChatMessageMetadata previousMetadata = previousLine == null ? null : emutils$messageTracker.trackedMetadata(previousLine);
		SmartChatFilter.PendingMessage pending = smartFilters
			? emutils$smartChatFilter.prepare(
				message,
				nowMillis,
				config.duplicateMessageWindowSeconds(),
				config.duplicateMessageTimeWindow(),
				previousLine,
				previousMetadata
			)
			: null;
		boolean duplicate = pending != null && pending.hasDuplicates();
		if (!timestamps && !duplicate) {
			return;
		}

		emutils$messageHandledByHead = true;
		ci.cancel();
		if (duplicate) {
			emutils$removePreviousDuplicateLines(pending.previousLines());
		}

		int duplicateCount = pending != null ? pending.duplicateCount() : 1;
		Text displayMessage = duplicate ? SmartChatFilter.withDuplicateCount(message, duplicateCount) : message;
		if (timestamps) {
			displayMessage = ChatTimestampFormatter.prependTimestamp(displayMessage, config.chatTimestamp24Hour(), nowMillis);
		}

		ChatHudLine line = emutils$addProcessedMessage(displayMessage, signatureData, indicator);
		emutils$messageTracker.register(line, message.copy(), duplicateCount, nowMillis, mentionsCurrentPlayer);
		if (pending != null) {
			emutils$smartChatFilter.track(pending, line);
		}
	}

	@Inject(
		method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
		at = @At("TAIL")
	)
	private void emutils$trackVanillaChatMessage(
		Text message,
		@Nullable MessageSignatureData signatureData,
		@Nullable MessageIndicator indicator,
		CallbackInfo ci
	) {
		if (emutils$messageHandledByHead) {
			return;
		}

		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || EMUtilsChatMessages.isInternal(message) || messages.isEmpty()) {
			return;
		}

		long nowMillis = System.currentTimeMillis();
		ChatHudLine line = messages.getFirst();
		emutils$messageTracker.register(line, message.copy(), 1, nowMillis, emutils$mentionsCurrentPlayer(message));

		if (!config.smartChatFilters() || config.chatTimestamps()) {
			return;
		}

		SmartChatFilter.PendingMessage pending = emutils$smartChatFilter.prepare(
			message,
			nowMillis,
			config.duplicateMessageWindowSeconds(),
			config.duplicateMessageTimeWindow(),
			messages.size() > 1 ? messages.get(1) : null,
			messages.size() > 1 ? emutils$messageTracker.trackedMetadata(messages.get(1)) : null
		);
		if (!pending.hasDuplicates()) {
			emutils$smartChatFilter.track(pending, line);
		}
	}

	@Inject(method = "clear", at = @At("TAIL"))
	private void emutils$clearChatFeatureState(boolean clearHistory, CallbackInfo ci) {
		emutils$smartChatFilter.clear();
		emutils$messageTracker.clear();
	}

	@Inject(
		method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/hud/ChatHud;forEachVisibleLine(Lnet/minecraft/client/gui/hud/ChatHud$OpacityRule;Lnet/minecraft/client/gui/hud/ChatHud$LineConsumer;)I",
			ordinal = 1
		)
	)
	private void emutils$renderMentionHighlights(ChatHud.Backend drawer, int windowHeight, int currentTick, boolean expanded, CallbackInfo ci) {
		EMUtilsConfig config = EMUtilsClient.config();
		if (
			config == null
				|| !config.chatMentionHighlight()
				|| isChatHidden()
				|| visibleMessages.isEmpty()
				|| scrolledLines >= visibleMessages.size()
		) {
			return;
		}

		float scale = (float) getChatScale();
		int chatWidth = MathHelper.ceil(getWidth() / scale);
		int chatBottom = MathHelper.floor((windowHeight - 40) / scale);
		double lineSpacing = client.options.getChatLineSpacing().getValue();
		int lineHeight = (int) (9 * (lineSpacing + 1.0));
		float chatOpacity = client.options.getChatOpacity().getValue().floatValue() * 0.9F + 0.1F;
		int visibleRows = Math.min(visibleMessages.size() - scrolledLines, getVisibleLineCount());

		for (int row = visibleRows - 1; row >= 0; row--) {
			int listIndex = row + scrolledLines;
			ChatHudLine line = emutils$lineForVisibleListIndex(listIndex);
			if (line == null || !emutils$messageTracker.metadataFor(line).mentionsCurrentPlayer()) {
				continue;
			}

			ChatHudLine.Visible visible = visibleMessages.get(listIndex);
			float opacity = expanded ? 1.0F : emutils$lineOpacity(visible, currentTick);
			if (opacity <= 1.0E-5F) {
				continue;
			}

			int top = chatBottom - row * lineHeight;
			int bottom = top - lineHeight;
			drawer.fill(-4, bottom, chatWidth + 8, top, emutils$highlightColor(opacity * chatOpacity));
		}
	}

	@Override
	public void emutils$removeMessageSilently(MessageSignatureData signature) {
		if (signature == null) {
			return;
		}

		boolean removed = messages.removeIf(line -> {
			if (signature.equals(line.signature())) {
				emutils$messageTracker.removeLine(line);
				return true;
			}

			return false;
		});
		if (removed) {
			emutils$invokeRefresh();
		}
	}

	@Override
	public void emutils$refreshDisplayedMessages() {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null) {
			return;
		}

		boolean changed = false;
		for (int index = 0; index < messages.size(); index++) {
			ChatHudLine line = messages.get(index);
			if (EMUtilsChatMessages.isInternal(line.content())) {
				continue;
			}

			ChatMessageMetadata metadata = emutils$messageTracker.metadataFor(line);
			Text displayMessage = ChatDisplayFormatter.format(metadata, config);
			if (displayMessage.getString().equals(line.content().getString())) {
				continue;
			}

			ChatHudLine updatedLine = new ChatHudLine(line.creationTick(), displayMessage, line.signature(), line.indicator());
			messages.set(index, updatedLine);
			emutils$messageTracker.replaceLine(line, updatedLine);
			changed = true;
		}

		if (changed) {
			emutils$invokeRefresh();
		}
	}

	@Override
	@Nullable
	public Text emutils$getMessageAt(double mouseX, double mouseY) {
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

			return findMessageForVisibleListIndex(listIndex);
		}

		return null;
	}

	@Nullable
	private Text findMessageForVisibleListIndex(int listIndex) {
		ChatHudLine line = emutils$lineForVisibleListIndex(listIndex);
		return line == null ? null : line.content();
	}

	@Nullable
	private ChatHudLine emutils$lineForVisibleListIndex(int listIndex) {
		if (listIndex < 0 || listIndex >= visibleMessages.size()) {
			return null;
		}

		TextRenderer textRenderer = client.textRenderer;
		int width = MathHelper.floor(getWidth() / getChatScale());
		int current = 0;

		for (ChatHudLine message : messages) {
			int lines = message.breakLines(textRenderer, width).size();
			if (listIndex >= current && listIndex < current + lines) {
				return message;
			}

			current += lines;
		}

		return null;
	}

	@Unique
	private boolean emutils$mentionsCurrentPlayer(Text message) {
		return client != null
			&& client.getSession() != null
			&& ChatMentionDetector.isMention(message, client.getSession().getUsername());
	}

	@Unique
	private static float emutils$lineOpacity(ChatHudLine.Visible line, int currentTick) {
		int ticksVisible = currentTick - line.addedTime();
		double opacity = ticksVisible / 200.0;
		opacity = 1.0 - opacity;
		opacity *= 10.0;
		opacity = MathHelper.clamp(opacity, 0.0, 1.0);
		opacity *= opacity;
		return (float) opacity;
	}

	@Unique
	private static int emutils$highlightColor(float opacity) {
		int alpha = MathHelper.clamp(Math.round(EMUTILS_MENTION_HIGHLIGHT_ALPHA * opacity), 0, EMUTILS_MENTION_HIGHLIGHT_ALPHA);
		return (alpha << 24) | EMUTILS_MENTION_HIGHLIGHT_COLOR;
	}

	private ChatHudLine emutils$addProcessedMessage(
		Text message,
		@Nullable MessageSignatureData signatureData,
		@Nullable MessageIndicator indicator
	) {
		ChatHudLine line = new ChatHudLine(client.inGameHud.getTicks(), message, signatureData, indicator);
		emutils$invokeLogChatMessage(line);
		emutils$invokeAddVisibleMessage(line);
		emutils$invokeStoreMessage(line);
		return line;
	}

	private void emutils$removePreviousDuplicateLines(List<ChatHudLine> previousLines) {
		boolean removed = false;
		for (ChatHudLine previousLine : previousLines) {
			removed |= messages.removeIf(line -> {
				if (line == previousLine) {
					emutils$messageTracker.removeLine(line);
					return true;
				}

				return false;
			});
		}

		if (removed) {
			emutils$invokeRefresh();
		}
	}
}
