package net.emutils.client.mixin;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.chat.ChatDisplayFormatter;
import net.emutils.client.emutils.chat.ChatHudAccess;
import net.emutils.client.emutils.chat.ChatMentionAlerts;
import net.emutils.client.emutils.chat.ChatMentionDetector;
import net.emutils.client.emutils.chat.ChatMessageMetadata;
import net.emutils.client.emutils.chat.ChatMessageTracker;
import net.emutils.client.emutils.chat.ChatTimestampFormatter;
import net.emutils.client.emutils.chat.EMUtilsChatMessages;
import net.emutils.client.emutils.chat.SmartChatFilter;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.waypoint.WaypointChatAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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

@Mixin(ChatComponent.class)
public abstract class ChatHudMixin implements WaypointChatAccess, ChatHudAccess {
	@Unique
	private final SmartChatFilter emutils$smartChatFilter = new SmartChatFilter();
 
	@Unique
	private final ChatMessageTracker emutils$messageTracker = new ChatMessageTracker();
 
	@Unique
	private boolean emutils$messageHandledByHead;
 
	@Shadow
	@Final
	private Minecraft minecraft;
 
	@Shadow
	@Final
	private List<GuiMessage> allMessages;
 
	@Shadow
	@Final
	private List<GuiMessage.Line> trimmedMessages;
 
	@Shadow
	private int chatScrollbarPos;
 
	@Invoker("refreshTrimmedMessages")
	protected abstract void emutils$invokeRefresh();
 
	@Invoker("logChatMessage")
	protected abstract void emutils$invokeLogChatMessage(GuiMessage message);
 
	@Invoker("addMessageToDisplayQueue")
	protected abstract void emutils$invokeAddVisibleMessage(GuiMessage message);
 
	@Invoker("addMessageToQueue")
	protected abstract void emutils$invokeStoreMessage(GuiMessage message);
 
	@Unique
	private boolean emutils$isChatHidden() {
		return minecraft.options.chatVisibility().get() == net.minecraft.world.entity.player.ChatVisiblity.HIDDEN;
	}
 
	@Shadow
	private int getWidth() {
		throw new AssertionError();
	}
 
	@Shadow
	private double getScale() {
		throw new AssertionError();
	}
 
	@Shadow
	private int getLineHeight() {
		throw new AssertionError();
	}
 
	@Shadow
	private int getLinesPerPage() {
		throw new AssertionError();
	}
 
	@Inject(
		method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void emutils$handleMentionAlerts(
		Component message,
		@Nullable MessageSignature signatureData,
		@Nullable GuiMessageSource source,
		@Nullable GuiMessageTag indicator,
		CallbackInfo ci
	) {
		ChatMentionAlerts.handle(minecraft, message);
	}
 
	@Inject(
		method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void emutils$processChatFeatures(
		Component message,
		@Nullable MessageSignature signatureData,
		@Nullable GuiMessageSource source,
		@Nullable GuiMessageTag indicator,
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
		GuiMessage previousLine = allMessages.isEmpty() ? null : allMessages.getFirst();
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
		ChatMessageMetadata metadata = new ChatMessageMetadata(message, duplicateCount, nowMillis, mentionsCurrentPlayer);
		Component displayMessage = ChatDisplayFormatter.format(metadata, config, emutils$currentUsername());
 
		GuiMessage line = emutils$addProcessedMessage(displayMessage, signatureData, indicator);
		emutils$messageTracker.register(line, message.copy(), duplicateCount, nowMillis, mentionsCurrentPlayer);
		if (pending != null) {
			emutils$smartChatFilter.track(pending, line);
		}
	}
 
	@Inject(
		method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
		at = @At("TAIL")
	)
	private void emutils$trackVanillaChatMessage(
		Component message,
		@Nullable MessageSignature signatureData,
		@Nullable GuiMessageSource source,
		@Nullable GuiMessageTag indicator,
		CallbackInfo ci
	) {
		if (emutils$messageHandledByHead) {
			return;
		}
 
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || EMUtilsChatMessages.isInternal(message) || allMessages.isEmpty()) {
			return;
		}
 
		long nowMillis = System.currentTimeMillis();
		boolean mentionsCurrentPlayer = emutils$mentionsCurrentPlayer(message);
		GuiMessage line = allMessages.getFirst();
		emutils$messageTracker.register(line, message.copy(), 1, nowMillis, mentionsCurrentPlayer);
		emutils$applyChatFormattingToLine(line, config);
 
		if (!config.smartChatFilters() || config.chatTimestamps()) {
			return;
		}
 
		SmartChatFilter.PendingMessage pending = emutils$smartChatFilter.prepare(
			message,
			nowMillis,
			config.duplicateMessageWindowSeconds(),
			config.duplicateMessageTimeWindow(),
			allMessages.size() > 1 ? allMessages.get(1) : null,
			allMessages.size() > 1 ? emutils$messageTracker.trackedMetadata(allMessages.get(1)) : null
		);
		if (!pending.hasDuplicates()) {
			emutils$smartChatFilter.track(pending, line);
		}
	}
 
	@Inject(method = "clearMessages", at = @At("TAIL"))
	private void emutils$clearChatFeatureState(boolean clearHistory, CallbackInfo ci) {
		emutils$smartChatFilter.clear();
		emutils$messageTracker.clear();
	}
 
	@Override
	public void emutils$removeMessageSilently(MessageSignature signature) {
		if (signature == null) {
			return;
		}
 
		boolean removed = allMessages.removeIf(line -> {
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
		String username = emutils$currentUsername();
		for (int index = 0; index < allMessages.size(); index++) {
			GuiMessage line = allMessages.get(index);
			if (EMUtilsChatMessages.isInternal(line.content())) {
				continue;
			}
 
			ChatMessageMetadata metadata = emutils$messageTracker.metadataFor(line);
			Component displayMessage = ChatDisplayFormatter.format(metadata, config, username);
			boolean forceUpdate = metadata.mentionsCurrentPlayer();
			if (!forceUpdate && displayMessage.getString().equals(line.content().getString())) {
				continue;
			}
 
			GuiMessage updatedLine = new GuiMessage(line.addedTime(), displayMessage, line.signature(), line.source(), line.tag());
			allMessages.set(index, updatedLine);
			emutils$messageTracker.replaceLine(line, updatedLine);
			changed = true;
		}
 
		if (changed) {
			emutils$invokeRefresh();
		}
	}
 
	@Override
	@Nullable
	public Component emutils$getMessageAt(double mouseX, double mouseY) {
		if (emutils$isChatHidden() || trimmedMessages.isEmpty()) {
			return null;
		}
 
		float scale = (float) getScale();
		Vector2f untransformed = new Vector2f();
		Matrix3x2f pose = new Matrix3x2f();
		pose.scale(scale, scale);
		pose.translate(4.0F, 0.0F);
		pose.invert(new Matrix3x2f()).transformPosition((float) mouseX, (float) mouseY, untransformed);
 
		int windowHeight = minecraft.getWindow().getGuiScaledHeight();
		int chatWidth = Mth.ceil(getWidth() / scale);
		int chatBottom = Mth.floor((windowHeight - 40) / scale);
		int lineHeight = getLineHeight();
		int visibleRows = Math.min(trimmedMessages.size() - chatScrollbarPos, getLinesPerPage());
 
		for (int row = visibleRows - 1; row >= 0; row--) {
			int listIndex = row + chatScrollbarPos;
			int top = chatBottom - row * lineHeight;
			int bottom = top - lineHeight;
			if (!emutils$isWithinBounds(untransformed.x, untransformed.y, -4, bottom, chatWidth + 8, top)) {
				continue;
			}
 
			return findMessageForVisibleListIndex(listIndex);
		}
 
		return null;
	}
 
	@Nullable
	private Component findMessageForVisibleListIndex(int listIndex) {
		GuiMessage line = emutils$lineForVisibleListIndex(listIndex);
		return line == null ? null : line.content();
	}
 
	@Nullable
	private GuiMessage emutils$lineForVisibleListIndex(int listIndex) {
		if (listIndex < 0 || listIndex >= trimmedMessages.size()) {
			return null;
		}
 
		Font textRenderer = minecraft.font;
		int width = Mth.floor(getWidth() / getScale());
		int current = 0;
 
		for (GuiMessage message : allMessages) {
			int lines = message.splitLines(textRenderer, width).size();
			if (listIndex >= current && listIndex < current + lines) {
				return message;
			}
 
			current += lines;
		}
 
		return null;
	}
 
	@Unique
	private boolean emutils$mentionsCurrentPlayer(Component message) {
		return minecraft != null
			&& minecraft.getUser() != null
			&& ChatMentionDetector.isMention(message, minecraft.getUser().getName());
	}
 
	@Unique
	@Nullable
	private String emutils$currentUsername() {
		return minecraft != null && minecraft.getUser() != null ? minecraft.getUser().getName() : null;
	}
 
	@Unique
	private void emutils$applyChatFormattingToLine(GuiMessage line, EMUtilsConfig config) {
		ChatMessageMetadata metadata = emutils$messageTracker.trackedMetadata(line);
		if (metadata == null) {
			return;
		}
 
		Component displayMessage = ChatDisplayFormatter.format(metadata, config, emutils$currentUsername());
		boolean forceUpdate = metadata.mentionsCurrentPlayer();
		if (!forceUpdate && displayMessage.getString().equals(line.content().getString())) {
			return;
		}
 
		int index = allMessages.indexOf(line);
		if (index < 0) {
			return;
		}
 
		GuiMessage updatedLine = new GuiMessage(line.addedTime(), displayMessage, line.signature(), line.source(), line.tag());
		allMessages.set(index, updatedLine);
		emutils$messageTracker.replaceLine(line, updatedLine);
		emutils$invokeRefresh();
	}
 
	private GuiMessage emutils$addProcessedMessage(
		Component message,
		@Nullable MessageSignature signatureData,
		@Nullable GuiMessageTag indicator
	) {
		GuiMessage line = new GuiMessage(minecraft.gui.getGuiTicks(), message, signatureData, GuiMessageSource.SYSTEM_CLIENT, indicator);
		emutils$invokeLogChatMessage(line);
		emutils$invokeAddVisibleMessage(line);
		emutils$invokeStoreMessage(line);
		return line;
	}
 
	private void emutils$removePreviousDuplicateLines(List<GuiMessage> previousLines) {
		boolean removed = false;
		for (GuiMessage previousLine : previousLines) {
			removed |= allMessages.removeIf(line -> {
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
 
	@Unique
	private static boolean emutils$isWithinBounds(float x, float y, int left, int bottom, int right, int top) {
		return x >= left && x <= right && y >= bottom && y <= top;
	}
}
