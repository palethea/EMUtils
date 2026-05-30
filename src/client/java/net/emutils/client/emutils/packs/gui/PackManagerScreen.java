package net.emutils.client.emutils.packs.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emhelpers.text.EmUtilsChatPrefix;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.emutils.client.emutils.compat.IrisCompat;
import net.emutils.client.emutils.packs.InstalledPack;
import net.emutils.client.emutils.packs.InstalledPackIndex;
import net.emutils.client.emutils.packs.InstalledPackScanner;
import net.emutils.client.emutils.packs.PackOperationResult;
import net.emutils.client.emutils.packs.PackType;
import net.emutils.client.emutils.packs.ResourcePackController;
import net.emutils.client.emutils.packs.modrinth.ModrinthClient;
import net.emutils.client.emutils.packs.modrinth.ModrinthSearchResult;
import net.emutils.client.emutils.screenshot.gui.GalleryIconButtonWidget;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

public final class PackManagerScreen extends Screen {

    private static final int TAB_WIDTH = 120;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_GAP = 4;
    private static final int ROW_GAP = 6;
    private static final int SEARCH_FIELD_WIDTH = 260;
    private static final int ICON_BUTTON_SIZE = 20;
    private static final int INSTALLED_POLL_TICKS = 40;

    private static final ExecutorService EXECUTOR =
        Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "EMUtils Pack Manager");
            thread.setDaemon(true);
            return thread;
        });

    private enum ViewMode {
        INSTALLED,
        MODRINTH,
    }

    private record RefreshResult(
        List<PackListItem> installed,
        @Nullable List<PackListItem> search
    ) {}

    private final Screen parent;
    private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(
        this
    );
    private final ModrinthClient modrinth = new ModrinthClient();
    private final InstalledPackIndex index = InstalledPackIndex.load();
    private PackType activeType = PackType.RESOURCE;
    private ViewMode viewMode = ViewMode.INSTALLED;
    private TextFieldWidget searchField;
    private PackSearchWidget list;
    private ButtonWidget resourceTab;
    private ButtonWidget shaderTab;
    private ButtonWidget installedTab;
    private ButtonWidget modrinthTab;
    private GalleryIconButtonWidget searchButton;
    private int refreshRequestId;
    private int installedPollTicks;
    private @Nullable String lastLoadedQuery;
    private List<PackListItem> installedItems = List.of();
    private @Nullable List<PackListItem> searchItems;

    public PackManagerScreen(Screen parent) {
        super(Text.translatable(EMUtilsTexts.SCREEN_PACK_MANAGER));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout.addHeader(title, textRenderer);
        layout.addFooter(
            ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .width(200)
                .build()
        );
        layout.forEachChild(this::addDrawableChild);

        resourceTab = addDrawableChild(
            ButtonWidget.builder(
                Text.translatable(EMUtilsTexts.PACK_TAB_RESOURCE_PACKS),
                button -> switchType(PackType.RESOURCE)
            )
                .width(TAB_WIDTH)
                .build()
        );
        shaderTab = addDrawableChild(
            ButtonWidget.builder(
                Text.translatable(EMUtilsTexts.PACK_TAB_SHADER_PACKS),
                button -> switchType(PackType.SHADER)
            )
                .width(TAB_WIDTH)
                .build()
        );
        installedTab = addDrawableChild(
            ButtonWidget.builder(
                Text.translatable(EMUtilsTexts.PACK_TAB_INSTALLED),
                button -> switchView(ViewMode.INSTALLED)
            )
                .width(TAB_WIDTH)
                .build()
        );
        modrinthTab = addDrawableChild(
            ButtonWidget.builder(
                Text.translatable(EMUtilsTexts.PACK_TAB_MODRINTH),
                button -> switchView(ViewMode.MODRINTH)
            )
                .width(TAB_WIDTH)
                .build()
        );

        searchField = addDrawableChild(
            new TextFieldWidget(
                textRenderer,
                0,
                0,
                SEARCH_FIELD_WIDTH,
                TAB_HEIGHT,
                Text.translatable(EMUtilsTexts.PACK_SEARCH_PLACEHOLDER)
            )
        );
        searchField.setPlaceholder(
            Text.translatable(EMUtilsTexts.PACK_SEARCH_PLACEHOLDER).formatted(
                Formatting.GRAY
            )
        );
        searchButton = addDrawableChild(
            GalleryIconButtonWidget.create(
                Text.translatable(EMUtilsTexts.PACK_SEARCH),
                PackIcons.SEARCH,
                PackIcons.SIZE,
                button -> search()
            )
        );

        list = addDrawableChild(
            new PackSearchWidget(
                client,
                width,
                height,
                this::download,
                this::enable,
                this::disable,
                this::delete,
                this::applyShader,
                this::disableShader
            )
        );

        refreshWidgetPositions();
        refreshTabState();
        refreshInstalledOnly();
    }

    @Override
    protected void refreshWidgetPositions() {
        layout.refreshPositions();

        int centerX = width / 2;
        int typeRowWidth = TAB_WIDTH * 2 + TAB_GAP;
        int typeRowLeft = centerX - typeRowWidth / 2;
        int y = layout.getHeaderHeight() + 8;

        resourceTab.setWidth(TAB_WIDTH);
        resourceTab.setHeight(TAB_HEIGHT);
        resourceTab.setX(typeRowLeft);
        resourceTab.setY(y);
        shaderTab.setWidth(TAB_WIDTH);
        shaderTab.setHeight(TAB_HEIGHT);
        shaderTab.setX(typeRowLeft + TAB_WIDTH + TAB_GAP);
        shaderTab.setY(y);
        y += TAB_HEIGHT + ROW_GAP;

        int viewRowLeft = centerX - typeRowWidth / 2;
        installedTab.setWidth(TAB_WIDTH);
        installedTab.setHeight(TAB_HEIGHT);
        installedTab.setX(viewRowLeft);
        installedTab.setY(y);
        modrinthTab.setWidth(TAB_WIDTH);
        modrinthTab.setHeight(TAB_HEIGHT);
        modrinthTab.setX(viewRowLeft + TAB_WIDTH + TAB_GAP);
        modrinthTab.setY(y);
        y += TAB_HEIGHT + ROW_GAP;

        boolean modrinthView = viewMode == ViewMode.MODRINTH;
        searchField.visible = modrinthView;
        searchButton.visible = modrinthView;
        if (modrinthView) {
            int toolbarWidth = SEARCH_FIELD_WIDTH + TAB_GAP + ICON_BUTTON_SIZE;
            int toolbarLeft = centerX - toolbarWidth / 2;
            searchField.setX(toolbarLeft);
            searchField.setY(y);
            searchField.setWidth(SEARCH_FIELD_WIDTH);
            searchField.setHeight(TAB_HEIGHT);
            searchButton.setX(toolbarLeft + SEARCH_FIELD_WIDTH + TAB_GAP);
            searchButton.setY(y);
            y += TAB_HEIGHT + ROW_GAP;
        }

        int listHeight = Math.max(
            40,
            height - y - layout.getFooterHeight() - 8
        );
        if (list != null) {
            list.position(width, listHeight, 0, y);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (
            viewMode == ViewMode.INSTALLED &&
            ++installedPollTicks >= INSTALLED_POLL_TICKS
        ) {
            installedPollTicks = 0;
            pollInstalled();
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (viewMode == ViewMode.MODRINTH && searchField != null) {
            if (
                (input.key() == 257 || input.key() == 335) &&
                searchField.isFocused()
            ) {
                search();
                return true;
            }
            if (searchField.keyPressed(input)) {
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        if (list != null) {
            list.close();
        }
        client.setScreen(parent);
    }

    private void switchType(PackType type) {
        if (activeType == type) {
            return;
        }
        activeType = type;
        searchItems = null;
        refreshTabState();
        if (viewMode == ViewMode.MODRINTH) {
            browse();
            return;
        }
        refreshInstalledOnly();
    }

    private void switchView(ViewMode mode) {
        if (viewMode == mode) {
            return;
        }
        viewMode = mode;
        refreshTabState();
        refreshWidgetPositions();
        if (viewMode == ViewMode.INSTALLED) {
            refreshInstalledOnly();
            return;
        }

        browse();
    }

    private void refreshTabState() {
        if (resourceTab != null) {
            resourceTab.active = activeType != PackType.RESOURCE;
        }
        if (shaderTab != null) {
            shaderTab.active =
                activeType != PackType.SHADER &&
                (IrisCompat.isIrisLoaded() ||
                    EMUtilsClient.config().packManagerShowShadersWithoutIris());
        }
        if (installedTab != null) {
            installedTab.active = viewMode != ViewMode.INSTALLED;
        }
        if (modrinthTab != null) {
            modrinthTab.active = viewMode != ViewMode.MODRINTH;
        }
    }

    private void pollInstalled() {
        int requestId = ++refreshRequestId;
        PackType type = activeType;
        CompletableFuture.supplyAsync(
            () -> scanInstalled(type),
            EXECUTOR
        ).whenComplete((items, throwable) ->
            client.execute(() -> {
                if (requestId != refreshRequestId || throwable != null) {
                    return;
                }
                if (!items.equals(installedItems)) {
                    installedItems = items;
                    if (viewMode == ViewMode.INSTALLED) {
                        updateList();
                    }
                }
            })
        );
    }

    private void browse() {
        loadModrinth("");
    }

    private void search() {
        loadModrinth(searchField == null ? "" : searchField.getText().trim());
    }

    private void loadModrinth(String query) {
        if (!EMUtilsClient.config().packManagerEnabled()) {
            showMessage(
                Text.translatable(EMUtilsTexts.OPTION_PACK_MANAGER).formatted(
                    Formatting.RED
                )
            );
            return;
        }

        lastLoadedQuery = query;
        int requestId = ++refreshRequestId;
        PackType type = activeType;
        CompletableFuture.supplyAsync(
            () ->
                new RefreshResult(
                    scanInstalled(type),
                    loadSearchItems(type, query)
                ),
            EXECUTOR
        ).whenComplete((result, throwable) ->
            client.execute(() -> {
                if (requestId != refreshRequestId) {
                    return;
                }
                if (throwable != null) {
                    setError(throwable);
                } else {
                    installedItems = result.installed();
                    searchItems = result.search();
                    updateList();
                }
            })
        );
    }

    private void refreshInstalledOnly() {
        int requestId = ++refreshRequestId;
        PackType type = activeType;
        CompletableFuture.supplyAsync(
            () -> scanInstalled(type),
            EXECUTOR
        ).whenComplete((items, throwable) ->
            client.execute(() -> {
                if (requestId != refreshRequestId) {
                    return;
                }
                if (throwable != null) {
                    setError(throwable);
                } else {
                    installedItems = items;
                    updateList();
                }
            })
        );
    }

    private void refreshAfterChange() {
        int requestId = ++refreshRequestId;
        PackType type = activeType;
        String query = lastLoadedQuery == null ? "" : lastLoadedQuery;
        boolean reloadSearch =
            viewMode == ViewMode.MODRINTH && searchItems != null;
        CompletableFuture.supplyAsync(() -> {
            List<PackListItem> installed = scanInstalled(type);
            List<PackListItem> search = reloadSearch
                ? loadSearchItems(type, query)
                : null;
            return new RefreshResult(installed, search);
        }, EXECUTOR).whenComplete((result, throwable) ->
            client.execute(() -> {
                if (requestId != refreshRequestId) {
                    return;
                }
                if (throwable != null) {
                    setError(throwable);
                } else {
                    installedItems = result.installed();
                    if (result.search() != null) {
                        searchItems = result.search();
                    }
                    updateList();
                }
            })
        );
    }

    private List<PackListItem> scanInstalled(PackType type) {
        try {
            return InstalledPackScanner.scan(client, type, index)
                .stream()
                .sorted(Comparator.comparing(InstalledPack::filename))
                .map(PackListItem::installed)
                .toList();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private List<PackListItem> loadSearchItems(PackType type, String query) {
        try {
            List<InstalledPack> installed = InstalledPackScanner.scan(
                client,
                type,
                index
            );
            List<ModrinthSearchResult> results = modrinth.search(
                type,
                query,
                SharedConstants.getGameVersion().name(),
                EMUtilsClient.config().packManagerSearchLimit()
            );
            List<PackListItem> items = new ArrayList<>();
            for (ModrinthSearchResult result : results) {
                InstalledPack matched = installed
                    .stream()
                    .filter(
                        pack ->
                            pack.record() != null &&
                            pack.record().matches(type, result.projectId())
                    )
                    .findFirst()
                    .orElse(null);
                items.add(PackListItem.result(type, result, matched));
            }
            return items;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(exception);
        }
    }

    private void download(PackListItem item) {
        if (item.result() == null) {
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                ResourcePackController.install(
                    modrinth,
                    client,
                    item.type(),
                    item.result(),
                    index
                );
                return PackOperationResult.ok("Installed pack.");
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return PackOperationResult.error(exception.getMessage());
            }
        }, EXECUTOR).thenAccept(result ->
            client.execute(() -> {
                showMessage(
                    Text.literal(result.message()).formatted(
                        result.success() ? Formatting.GREEN : Formatting.RED
                    )
                );
                if (result.success()) {
                    refreshAfterChange();
                }
            })
        );
    }

    private void enable(PackListItem item) {
        if (item.installed() == null) {
            return;
        }
        PackOperationResult result =
            ResourcePackController.setResourcePackEnabled(
                client,
                item.installed().filename(),
                true
            );
        showMessage(
            Text.literal(result.message()).formatted(
                result.success() ? Formatting.GREEN : Formatting.RED
            )
        );
        refreshAfterChange();
    }

    private void disable(PackListItem item) {
        if (item.installed() == null) {
            return;
        }
        PackOperationResult result =
            ResourcePackController.setResourcePackEnabled(
                client,
                item.installed().filename(),
                false
            );
        showMessage(
            Text.literal(result.message()).formatted(
                result.success() ? Formatting.GREEN : Formatting.RED
            )
        );
        refreshAfterChange();
    }

    private void delete(PackListItem item) {
        if (item.installed() == null) {
            return;
        }

        client.setScreen(
            new ConfirmScreen(
                confirmed -> {
                    client.setScreen(this);
                    if (confirmed) {
                        deleteConfirmed(item.installed());
                    }
                },
                Text.translatable(EMUtilsTexts.PACK_DELETE_TITLE),
                Text.translatable(
                    EMUtilsTexts.PACK_DELETE_MESSAGE,
                    item.installed().filename()
                ),
                Text.translatable(EMUtilsTexts.PACK_DELETE),
                ScreenTexts.CANCEL
            )
        );
    }

    private void deleteConfirmed(InstalledPack pack) {
        if (pack.type() == PackType.RESOURCE && pack.enabled()) {
            PackOperationResult disabled =
                ResourcePackController.setResourcePackEnabled(
                    client,
                    pack.filename(),
                    false
                );
            if (!disabled.success()) {
                showMessage(
                    Text.literal(disabled.message()).formatted(Formatting.RED)
                );
                return;
            }
        }
        CompletableFuture.supplyAsync(
            () ->
                ResourcePackController.deleteInstalledFile(client, index, pack),
            EXECUTOR
        ).thenAccept(result ->
            client.execute(() -> {
                showMessage(
                    Text.literal(result.message()).formatted(
                        result.success() ? Formatting.GREEN : Formatting.RED
                    )
                );
                if (result.success()) {
                    refreshAfterChange();
                }
            })
        );
    }

    private void applyShader(PackListItem item) {
        if (item.installed() == null) {
            return;
        }
        if (!IrisCompat.isIrisLoaded()) {
            showMessage(
                Text.translatable(
                    EMUtilsTexts.PACK_STATUS_IRIS_REQUIRED
                ).formatted(Formatting.RED)
            );
            return;
        }

        IrisCompat.applyShaderPackWithLoading(
            client,
            this,
            item.installed().filename(),
            success -> {
                if (success) {
                    showMessage(
                        Text.translatable(
                            EMUtilsTexts.PACK_SHADER_APPLIED,
                            item.title()
                        ).formatted(Formatting.GREEN)
                    );
                } else {
                    showMessage(
                        Text.translatable(
                            EMUtilsTexts.PACK_SHADER_APPLY_FAILED,
                            item.title()
                        ).formatted(Formatting.RED)
                    );
                }
                refreshAfterChange();
            }
        );
    }

    private void disableShader(PackListItem item) {
        if (item.installed() == null) {
            return;
        }
        if (!IrisCompat.isIrisLoaded()) {
            showMessage(
                Text.translatable(
                    EMUtilsTexts.PACK_STATUS_IRIS_REQUIRED
                ).formatted(Formatting.RED)
            );
            return;
        }

        IrisCompat.disableShaderPackWithLoading(client, this, success -> {
            if (success) {
                showMessage(
                    Text.translatable(
                        EMUtilsTexts.PACK_SHADER_DISABLED,
                        item.title()
                    ).formatted(Formatting.GREEN)
                );
            } else {
                showMessage(
                    Text.translatable(
                        EMUtilsTexts.PACK_SHADER_DISABLE_FAILED,
                        item.title()
                    ).formatted(Formatting.RED)
                );
            }
            refreshAfterChange();
        });
    }

    private void updateList() {
        if (list == null) {
            return;
        }

        if (viewMode == ViewMode.INSTALLED) {
            list.setItems(
                installedItems,
                PackSearchWidget.ViewMode.INSTALLED,
                installedEmptyHint()
            );
            return;
        }

        if (searchItems == null) {
            list.setItems(
                List.of(),
                PackSearchWidget.ViewMode.MODRINTH,
                Text.empty()
            );
            return;
        }

        list.setItems(
            searchItems,
            PackSearchWidget.ViewMode.MODRINTH,
            modrinthEmptyHint()
        );
    }

    private Text installedEmptyHint() {
        return Text.translatable(
            activeType == PackType.SHADER
                ? EMUtilsTexts.PACK_SECTION_INSTALLED_EMPTY_SHADERS
                : EMUtilsTexts.PACK_SECTION_INSTALLED_EMPTY_PACKS
        );
    }

    private Text modrinthEmptyHint() {
        return Text.translatable(
            activeType == PackType.SHADER
                ? EMUtilsTexts.PACK_STATUS_EMPTY_SHADERS
                : EMUtilsTexts.PACK_STATUS_EMPTY_PACKS
        );
    }

    private void setError(Throwable throwable) {
        Throwable cause =
            throwable.getCause() == null ? throwable : throwable.getCause();
        showMessage(
            Text.translatable(
                EMUtilsTexts.PACK_ERROR,
                cause.getMessage()
            ).formatted(Formatting.RED)
        );
        if (viewMode == ViewMode.MODRINTH) {
            searchItems = List.of();
            updateList();
        }
    }

    private void showMessage(Text message) {
        client.inGameHud
            .getChatHud()
            .addMessage(EmUtilsChatPrefix.chat(message), null, null);
    }
}
