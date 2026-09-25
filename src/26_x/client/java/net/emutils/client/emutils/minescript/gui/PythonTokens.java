package net.emutils.client.emutils.minescript.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Splits a line of Python into the pieces the script editors color: keywords, strings, numbers, comments. */
final class PythonTokens {
	static final Set<String> KEYWORDS = Set.of(
		"and", "as", "assert", "break", "class", "continue", "def", "del", "elif", "else", "except", "finally", "for", "from", "global",
		"if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield",
		"True", "False", "None"
	);

	enum Kind {
		TEXT,
		KEYWORD,
		STRING,
		NUMBER,
		COMMENT
	}

	/** Characters {@code start} (inclusive) to {@code end} (exclusive) of the line. */
	record Token(int start, int end, Kind kind) {
	}

	private PythonTokens() {
	}

	/** The line's tokens in order, covering all of it; runs of plain text are merged into one token. */
	static List<Token> tokenize(String line) {
		List<Token> tokens = new ArrayList<>();
		int index = 0;
		while (index < line.length()) {
			char c = line.charAt(index);
			if (c == '#') {
				add(tokens, index, line.length(), Kind.COMMENT);
				break;
			}
			if (c == '"' || c == '\'') {
				int end = index + 1;
				while (end < line.length() && line.charAt(end) != c) {
					if (line.charAt(end) == '\\') {
						end++;
					}
					end++;
				}
				end = Math.min(line.length(), end + 1);
				add(tokens, index, end, Kind.STRING);
				index = end;
				continue;
			}
			if (Character.isDigit(c)) {
				int end = index + 1;
				while (end < line.length() && (Character.isDigit(line.charAt(end)) || line.charAt(end) == '.')) {
					end++;
				}
				add(tokens, index, end, Kind.NUMBER);
				index = end;
				continue;
			}
			if (Character.isJavaIdentifierStart(c)) {
				int end = index + 1;
				while (end < line.length() && Character.isJavaIdentifierPart(line.charAt(end))) {
					end++;
				}
				add(tokens, index, end, KEYWORDS.contains(line.substring(index, end)) ? Kind.KEYWORD : Kind.TEXT);
				index = end;
				continue;
			}
			add(tokens, index, index + 1, Kind.TEXT);
			index++;
		}
		return tokens;
	}

	private static void add(List<Token> tokens, int start, int end, Kind kind) {
		if (kind == Kind.TEXT && !tokens.isEmpty()) {
			Token last = tokens.get(tokens.size() - 1);
			if (last.kind() == Kind.TEXT && last.end() == start) {
				tokens.set(tokens.size() - 1, new Token(last.start(), end, Kind.TEXT));
				return;
			}
		}
		tokens.add(new Token(start, end, kind));
	}
}
