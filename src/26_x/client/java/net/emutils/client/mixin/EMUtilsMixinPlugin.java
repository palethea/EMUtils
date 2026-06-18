package net.emutils.client.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class EMUtilsMixinPlugin implements IMixinConfigPlugin {
	private static final Set<String> SUPPORTED_SODIUM_26_X = Set.of("26.1", "26.1.1", "26.1.2", "26.2");

	private String minecraftVersion = "unknown";

	@Override
	public void onLoad(String mixinPackage) {
		minecraftVersion = FabricLoader.getInstance()
			.getModContainer("minecraft")
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.endsWith(".SodiumWorldRendererMixin")) {
			return isSupportedSodiumTarget() && FabricLoader.getInstance().isModLoaded("sodium");
		}

		return true;
	}

	private boolean isSupportedSodiumTarget() {
		return SUPPORTED_SODIUM_26_X.contains(minecraftVersion);
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
