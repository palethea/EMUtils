package net.emutils.client.emutils.packs;

public record PackOperationResult(boolean success, String message) {
	public static PackOperationResult ok(String message) {
		return new PackOperationResult(true, message);
	}

	public static PackOperationResult error(String message) {
		return new PackOperationResult(false, message);
	}
}
