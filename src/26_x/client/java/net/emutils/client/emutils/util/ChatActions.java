package net.emutils.client.emutils.util;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.Identifier;

/**
 * The click events of EMUtils' own chat buttons, such as Remove under a waypoint (#254). Text from a server,
 * a sign or a book can carry the same click events, so each button's payload starts with a token made new
 * every session, which only the buttons EMUtils adds itself know; a click without it does nothing.
 */
public final class ChatActions {
	private static final String TOKEN = UUID.randomUUID().toString();
	private static final String SEPARATOR = "|";

	private ChatActions() {
	}

	/** The click event of an EMUtils button that does {@code action} with {@code value}. */
	public static ClickEvent.Custom click(Identifier action, String value) {
		return new ClickEvent.Custom(action, Optional.of(StringTag.valueOf(TOKEN + SEPARATOR + value)));
	}

	/** The value of a button EMUtils made this session, or empty for anything else, such as server-sent text. */
	public static Optional<String> value(Optional<Tag> payload) {
		String prefix = TOKEN + SEPARATOR;
		return payload
			.flatMap(element -> element instanceof StringTag string ? string.asString() : Optional.empty())
			.filter(text -> text.startsWith(prefix))
			.map(text -> text.substring(prefix.length()));
	}

	/** For UI snapshot checks: a click event like an EMUtils button's, but without the token, as a server could send. */
	public static ClickEvent.Custom forgedForSnapshot(Identifier action, Optional<String> value) {
		return new ClickEvent.Custom(action, value.map(StringTag::valueOf));
	}
}
