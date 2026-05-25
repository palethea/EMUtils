package net.emutils.client.inventory;

import java.util.ArrayList;
import java.util.List;

public final class InventoryToolsScopeData {
	private List<Integer> lockedSlots = new ArrayList<>();
	private List<int[]> bindings = new ArrayList<>();

	public InventoryToolsScopeData() {
	}

	public List<Integer> lockedSlots() {
		return lockedSlots;
	}

	public void setLockedSlots(List<Integer> lockedSlots) {
		this.lockedSlots = lockedSlots == null ? new ArrayList<>() : lockedSlots;
	}

	public List<int[]> bindings() {
		return bindings;
	}

	public void setBindings(List<int[]> bindings) {
		this.bindings = bindings == null ? new ArrayList<>() : bindings;
	}
}
