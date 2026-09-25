package net.emutils.client.emutils.spotify;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Just enough of the Windows Runtime to call WinRT APIs from Java through JNA: activation factories,
 * calling interface methods by their vtable slot, HSTRINGs and waiting for async operations. Slots
 * count from the start of the vtable, so an interface's own methods start at 6, after IUnknown's 3
 * and IInspectable's 3; they follow the method order in the Windows SDK's .idl files.
 */
final class WinRt {
	private static final int RO_INIT_MULTITHREADED = 1;
	private static final int RPC_E_CHANGED_MODE = 0x80010106;
	private static final int ASYNC_STARTED = 0;
	private static final int ASYNC_COMPLETED = 1;
	private static final Pointer IID_IASYNC_INFO = iid("00000036-0000-0000-C000-000000000046");

	private WinRt() {
	}

	/** Joins the calling thread to the multithreaded apartment; once per thread, before any other call. */
	static void initThread() {
		int result = Combase.INSTANCE.RoInitialize(RO_INIT_MULTITHREADED);
		// Already in a single-threaded apartment still works: the objects used here are agile.
		if (result < 0 && result != RPC_E_CHANGED_MODE) {
			throw new WinRtException("RoInitialize", result);
		}
	}

	/** A GUID in its in-memory layout, for passing as an IID. */
	static Pointer iid(String value) {
		UUID uuid = UUID.fromString(value);
		long high = uuid.getMostSignificantBits();
		long low = uuid.getLeastSignificantBits();
		Memory memory = new Memory(16);
		memory.setInt(0, (int) (high >>> 32));
		memory.setShort(4, (short) (high >>> 16));
		memory.setShort(6, (short) high);
		for (int i = 0; i < 8; i++) {
			memory.setByte(8 + i, (byte) (low >>> (56 - i * 8)));
		}
		return memory;
	}

	/** The activation factory of a runtime class, as the interface {@code iid}, such as its statics. */
	static Pointer factory(String runtimeClass, Pointer iid) {
		Pointer name = createString(runtimeClass);
		try {
			PointerByReference factory = new PointerByReference();
			check("RoGetActivationFactory " + runtimeClass, Combase.INSTANCE.RoGetActivationFactory(name, iid, factory));
			return factory.getValue();
		} finally {
			Combase.INSTANCE.WindowsDeleteString(name);
		}
	}

	/** Calls the method in vtable slot {@code slot} of {@code object} and returns its HRESULT. */
	static int invoke(Pointer object, int slot, Object... args) {
		Pointer vtable = object.getPointer(0);
		Function function = Function.getFunction(vtable.getPointer((long) slot * Native.POINTER_SIZE), Function.ALT_CONVENTION);
		Object[] withThis = new Object[args.length + 1];
		withThis[0] = object;
		System.arraycopy(args, 0, withThis, 1, args.length);
		return function.invokeInt(withThis);
	}

	/** Calls a method whose last parameter receives an interface pointer, which may be null. */
	static @Nullable Pointer object(Pointer object, int slot, Object... args) {
		PointerByReference out = new PointerByReference();
		call(object, slot, append(args, out));
		return out.getValue();
	}

	static long int64(Pointer object, int slot) {
		Memory out = new Memory(8);
		call(object, slot, out);
		return out.getLong(0);
	}

	static int int32(Pointer object, int slot) {
		Memory out = new Memory(4);
		call(object, slot, out);
		return out.getInt(0);
	}

	/** Calls a method whose last parameter receives an HSTRING, and frees it. */
	static String string(Pointer object, int slot) {
		PointerByReference out = new PointerByReference();
		call(object, slot, out);
		Pointer value = out.getValue();
		if (value == null) {
			return "";
		}
		try {
			IntByReference length = new IntByReference();
			Pointer chars = Combase.INSTANCE.WindowsGetStringRawBuffer(value, length);
			return chars == null ? "" : new String(chars.getCharArray(0, length.getValue()));
		} finally {
			Combase.INSTANCE.WindowsDeleteString(value);
		}
	}

	static Pointer queryInterface(Pointer object, Pointer iid) {
		PointerByReference out = new PointerByReference();
		check("QueryInterface", invoke(object, 0, iid, out));
		return out.getValue();
	}

	static void release(@Nullable Pointer object) {
		if (object != null) {
			invoke(object, 2);
		}
	}

	/**
	 * Waits for an {@code IAsyncOperation} to finish, then releases it. The result goes into
	 * {@code result} through {@code GetResults} (slot 8): a {@link PointerByReference} for an object,
	 * or a small {@link Memory} for a number or boolean.
	 */
	static void await(Pointer operation, long timeoutMs, Object result) {
		Pointer info = queryInterface(operation, IID_IASYNC_INFO);
		try {
			long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
			int status;
			while ((status = int32(info, 7)) == ASYNC_STARTED) {
				if (System.nanoTime() > deadline) {
					invoke(info, 9);
					throw new WinRtException("Timed out waiting for a WinRT operation", 0);
				}
				try {
					Thread.sleep(2L);
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					invoke(info, 9);
					throw new WinRtException("Interrupted waiting for a WinRT operation", 0);
				}
			}
			if (status != ASYNC_COMPLETED) {
				throw new WinRtException("WinRT operation ended with status " + status, int32(info, 8));
			}
			call(operation, 8, result);
		} finally {
			release(info);
			release(operation);
		}
	}

	/** Waits for an operation that returns an object, which may be null. */
	static @Nullable Pointer awaitObject(Pointer operation, long timeoutMs) {
		PointerByReference out = new PointerByReference();
		await(operation, timeoutMs, out);
		return out.getValue();
	}

	private static void call(Pointer object, int slot, Object... args) {
		check("WinRT call to slot " + slot, invoke(object, slot, args));
	}

	private static void check(String what, int hresult) {
		if (hresult < 0) {
			throw new WinRtException(what, hresult);
		}
	}

	private static Object[] append(Object[] args, Object last) {
		Object[] all = new Object[args.length + 1];
		System.arraycopy(args, 0, all, 0, args.length);
		all[args.length] = last;
		return all;
	}

	private static Pointer createString(String value) {
		PointerByReference out = new PointerByReference();
		check("WindowsCreateString", Combase.INSTANCE.WindowsCreateString(new WString(value), value.length(), out));
		return out.getValue();
	}

	static final class WinRtException extends RuntimeException {
		WinRtException(String what, int hresult) {
			super(hresult == 0 ? what : what + " failed: 0x" + Integer.toHexString(hresult));
		}
	}

	private interface Combase extends Library {
		Combase INSTANCE = Native.load("combase", Combase.class);

		int RoInitialize(int type);

		int RoGetActivationFactory(Pointer classId, Pointer iid, PointerByReference factory);

		int WindowsCreateString(WString source, int length, PointerByReference string);

		int WindowsDeleteString(Pointer string);

		Pointer WindowsGetStringRawBuffer(Pointer string, IntByReference length);
	}
}
