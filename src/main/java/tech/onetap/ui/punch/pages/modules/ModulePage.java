package tech.onetap.ui.punch.pages.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import tech.onetap.ui.punch.feature.Feature;
import tech.onetap.ui.punch.feature.FeatureCategory;
import tech.onetap.ui.punch.feature.FeatureManager;
import tech.onetap.ui.punch.core.MenuConfigStore;
import tech.onetap.ui.punch.core.MenuOverlayState;
import tech.onetap.ui.punch.core.MenuPage;
import tech.onetap.ui.punch.ui.Component;
import tech.onetap.ui.punch.ui.SmoothScroll;
import tech.onetap.ui.punch.color.ColorUtil;
import tech.onetap.ui.punch.friends.FriendManager;
import tech.onetap.ui.punch.textures.Textures;
import tech.onetap.ui.punch.theme.Theme;
import tech.onetap.ui.punch.gui.MsdfFont;
import tech.onetap.ui.punch.gui.Render2DUtil;
import tech.onetap.ui.punch.gui.UiFontStyle;
import tech.onetap.ui.punch.gui.UiFonts;
import tech.onetap.ui.punch.text.NameProtectUtil;
import tech.onetap.ui.punch.text.StringUtil;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class ModulePage extends Component {

    public enum NitamaTab {
        COMBAT("Combat", "Combat", FeatureCategory.COMBAT, Textures.Icons.SWORDS),
        MOVEMENT("Combat", "Movement", FeatureCategory.MOVEMENT, Textures.Icons.PERSON_STANDING),
        ESP("Visuals", "ESP", FeatureCategory.VISUAL, Textures.Icons.BOXES),
        RENDER("Visuals", "Render", FeatureCategory.VISUAL, Textures.Icons.EYE),
        PLAYER("Visuals", "Player", FeatureCategory.PLAYER, Textures.Icons.USER_ROUND),
        COLOR_THEMES("Other", "Color Themes", FeatureCategory.MISC, Textures.Icons.SPARKLES),
        CONFIGS("Other", "Configs", FeatureCategory.MISC, Textures.Icons.HARD_DRIVE),
        FRIENDS("Other", "Friends", FeatureCategory.MISC, Textures.Icons.USER_ROUND),
        ABOUT_US("Other", "About us", FeatureCategory.MISC, Textures.Icons.TRIANGLE_ALERT);

        private final String section;
        private final String title;
        private final FeatureCategory category;
        private final Identifier icon;

        NitamaTab(String section, String title, FeatureCategory category, Identifier icon) {
            this.section = section;
            this.title = title;
            this.category = category;
            this.icon = icon;
        }

        public String getSection() { return section; }
        public String getTitle() { return title; }
        public FeatureCategory getCategory() { return category; }
        public Identifier getIcon() { return icon; }
    }

    // Design Pixel Constants (DESIGN_WIDTH = 1024, DESIGN_HEIGHT = 640)
    private static final int SIDEBAR_WIDTH = 220;
    private static final int RIGHT_PANEL_X = 220;
    private static final int RIGHT_PANEL_WIDTH = 804;
    private static final int HEADER_HEIGHT = 48;
    private static final int CONTENT_PADDING = 16;
    private static final int CONTENT_X = RIGHT_PANEL_X + CONTENT_PADDING; // 236
    private static final int CONTENT_Y = HEADER_HEIGHT + 10; // 58
    private static final int CONTENT_WIDTH = RIGHT_PANEL_WIDTH - (CONTENT_PADDING * 2); // 772
    private static final int CONTENT_HEIGHT = 640 - CONTENT_Y - 14; // 568

    private NitamaTab activeTab = NitamaTab.COMBAT;
    private final Map<NitamaTab, List<ModuleCard>> tabCards = new LinkedHashMap<>();
    private final Map<NitamaTab, SmoothScroll> tabScrolls = new LinkedHashMap<>();
    private final Map<NitamaTab, Integer> tabMaxContent = new HashMap<>();

    private MenuOverlayState state;
    private MenuPage displayedPage = MenuPage.NONE;
    private int mouseX;
    private int mouseY;

    private String searchQuery = "";
    private boolean searchFocused = false;

    // Friends & Config Input States
    private String friendInputText = "";
    private boolean friendInputFocused = false;
    private String configInputText = "";
    private boolean configInputFocused = false;
    private String activeConfigName = "default.json";
    private final List<String> configList = new ArrayList<>(List.of("default.json", "legit_pvp.json", "rage_hvh.json"));

    public ModulePage() {
        syncSavedConfigs();
        rebuildAllTabs();
    }

    private void syncSavedConfigs() {
        try {
            List<String> saved = FeatureManager.INSTANCE.listNamedConfigs();
            if (saved != null) {
                for (String cfg : saved) {
                    if (!this.configList.contains(cfg)) {
                        this.configList.add(cfg);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public FeatureCategory selectedCategory() {
        return activeTab.getCategory();
    }

    private void rebuildAllTabs() {
        this.tabCards.clear();
        for (NitamaTab tab : NitamaTab.values()) {
            if (tab == NitamaTab.COLOR_THEMES || tab == NitamaTab.CONFIGS || tab == NitamaTab.FRIENDS || tab == NitamaTab.ABOUT_US) {
                this.tabCards.put(tab, List.of());
                continue;
            }
            List<ModuleCard> cards = new ArrayList<>();
            List<Feature> features = new ArrayList<>(FeatureManager.INSTANCE.getFeatures(tab.getCategory()));
            if (tab == NitamaTab.ESP) {
                features.removeIf(f -> !f.getName().toLowerCase().contains("esp") && !f.getName().toLowerCase().contains("tracer") && !f.getName().toLowerCase().contains("cham"));
            }
            for (Feature feature : features) {
                if (!this.searchQuery.isEmpty() && !feature.getName().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                    continue;
                }
                cards.add(new ModuleCard(feature));
            }
            this.tabCards.put(tab, cards);
            this.tabScrolls.putIfAbsent(tab, new SmoothScroll());
            this.tabMaxContent.put(tab, 500);
        }
    }

    public void layout(Component frame, MenuOverlayState state, int mouseX, int mouseY) {
        attach(frame, frame.x(), frame.y(), frame.width(), frame.height());
        this.state = state;
        this.displayedPage = state.displayPage();
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        // Layout Module Cards in 2 Columns using Design Pixels (cardW = 378 design px)
        List<ModuleCard> cards = this.tabCards.get(this.activeTab);
        if (cards != null && !cards.isEmpty()) {
            SmoothScroll scroll = this.tabScrolls.computeIfAbsent(this.activeTab, k -> new SmoothScroll());
            int maxContent = this.tabMaxContent.getOrDefault(this.activeTab, 500);
            float scrollOffset = scroll.update(Math.max(0, maxContent - CONTENT_HEIGHT));

            int cols = 2;
            int gap = 16;
            int cardW = (CONTENT_WIDTH - gap) / cols;

            int[] colHeights = new int[cols];
            Arrays.fill(colHeights, 0);

            for (ModuleCard card : cards) {
                int col = (colHeights[0] <= colHeights[1]) ? 0 : 1;
                int cardX = CONTENT_X + col * (cardW + gap);
                int cardY = Math.round(CONTENT_Y + colHeights[col] - scrollOffset);

                card.place(this, cardX, cardY, cardW, 1.0F, mouseX, mouseY, 0, CONTENT_Y + CONTENT_HEIGHT);
                colHeights[col] += card.designHeight() + gap;
            }

            int totalHeight = Math.max(colHeights[0], colHeights[1]);
            this.tabMaxContent.put(this.activeTab, Math.max(totalHeight, CONTENT_HEIGHT));
        }
    }

    public boolean handleMouseButton(int mouseX, int mouseY, int button) {
        // Sidebar Click Detection
        if (hit(mouseX, mouseY, 0, 0, SIDEBAR_WIDTH, 640)) {
            float startY = 64;
            float itemH = 30;
            float currY = startY;
            String lastSec = "";

            for (NitamaTab tab : NitamaTab.values()) {
                if (!tab.getSection().equals(lastSec)) {
                    lastSec = tab.getSection();
                    currY += 22;
                }
                if (hit(mouseX, mouseY, 14, currY, 192, itemH)) {
                    this.activeTab = tab;
                    rebuildAllTabs();
                    return true;
                }
                currY += itemH + 2;
            }

            // Search Box Click (Y: 535, W: 192, H: 30)
            if (hit(mouseX, mouseY, 14, 535, 192, 30)) {
                this.searchFocused = true;
                this.friendInputFocused = false;
                this.configInputFocused = false;
                return true;
            } else {
                this.searchFocused = false;
            }
        }

        // Color Themes Swatch Clicks
        if (this.activeTab == NitamaTab.COLOR_THEMES && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            float swatchY = 110;
            float size = 26;
            float gap = 8;
            float startX = RIGHT_PANEL_X + 24;
            for (int i = 0; i < Theme.accentCount(); i++) {
                float sx = startX + i * (size + gap);
                if (hit(mouseX, mouseY, sx, swatchY, size, size)) {
                    Theme.setAccentIndex(i);
                    MenuConfigStore.saveAccentIndex(i);
                    return true;
                }
            }
        }

        // Friends Page Clicks
        if (this.activeTab == NitamaTab.FRIENDS && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            float inputX = RIGHT_PANEL_X + 24;
            float inputY = 110;
            if (hit(mouseX, mouseY, inputX, inputY, 220, 32)) {
                this.friendInputFocused = true;
                this.searchFocused = false;
                this.configInputFocused = false;
                return true;
            } else {
                this.friendInputFocused = false;
            }

            float btnX = inputX + 230;
            if (hit(mouseX, mouseY, btnX, inputY, 100, 32)) {
                if (!this.friendInputText.trim().isEmpty()) {
                    FriendManager.INSTANCE.add(this.friendInputText.trim());
                    this.friendInputText = "";
                }
                return true;
            }

            List<String> friends = new ArrayList<>(FriendManager.INSTANCE.getFriends());
            float listY = 170;
            for (int i = 0; i < friends.size(); i++) {
                float itemY = listY + i * 44;
                float removeBtnX = RIGHT_PANEL_X + 480;
                if (hit(mouseX, mouseY, removeBtnX, itemY + 6, 74, 26)) {
                    FriendManager.INSTANCE.remove(friends.get(i));
                    return true;
                }
            }
        }

        // Configs Page Clicks
        if (this.activeTab == NitamaTab.CONFIGS && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            float inputX = RIGHT_PANEL_X + 24;
            float inputY = 110;
            if (hit(mouseX, mouseY, inputX, inputY, 220, 32)) {
                this.configInputFocused = true;
                this.searchFocused = false;
                this.friendInputFocused = false;
                return true;
            } else {
                this.configInputFocused = false;
            }

            float btnX = inputX + 230;
            if (hit(mouseX, mouseY, btnX, inputY, 110, 32)) {
                saveNewConfig();
                return true;
            }

            // Click Config Items (Load or Delete)
            float listY = 170;
            float cardW = RIGHT_PANEL_WIDTH - 48;
            for (int i = 0; i < this.configList.size(); i++) {
                String cfg = this.configList.get(i);
                float itemY = listY + i * 46;

                // Load Config Button
                float loadBtnX = RIGHT_PANEL_X + cardW - 140;
                if (hit(mouseX, mouseY, loadBtnX, itemY + 6, 60, 26)) {
                    this.activeConfigName = cfg;
                    try { FeatureManager.INSTANCE.loadNamedConfig(cfg); } catch (Exception ignored) {}
                    return true;
                }

                // Delete Config Button
                float delBtnX = RIGHT_PANEL_X + cardW - 74;
                if (hit(mouseX, mouseY, delBtnX, itemY + 6, 60, 26)) {
                    try { FeatureManager.INSTANCE.deleteNamedConfig(cfg); } catch (Exception ignored) {}
                    this.configList.remove(cfg);
                    if (this.activeConfigName.equals(cfg) && !this.configList.isEmpty()) {
                        this.activeConfigName = this.configList.get(0);
                    }
                    return true;
                }
            }
        }

        // Module Cards Clicks
        List<ModuleCard> cards = this.tabCards.get(this.activeTab);
        if (cards != null) {
            for (ModuleCard card : cards) {
                if (card.handleBindPopupClick(mouseX, mouseY, button)) return true;
                if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && card.handleMiddleClick(mouseX, mouseY)) return true;
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && card.handleRightClick(mouseX, mouseY)) return true;
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    if (card.handleDropdownPopupClick(mouseX, mouseY)) return true;
                    if (card.handleClick(mouseX, mouseY)) return true;
                }
            }
        }

        return true;
    }

    private void saveNewConfig() {
        if (!this.configInputText.trim().isEmpty()) {
            String newCfg = this.configInputText.trim();
            if (!newCfg.endsWith(".json")) newCfg += ".json";
            if (!this.configList.contains(newCfg)) {
                this.configList.add(newCfg);
            }
            this.activeConfigName = newCfg;
            try {
                FeatureManager.INSTANCE.saveNamedConfig(newCfg);
            } catch (Exception ignored) {}
            this.configInputText = "";
        }
    }

    public void drag(int mouseX) {
        List<ModuleCard> cards = this.tabCards.get(this.activeTab);
        if (cards != null) {
            for (ModuleCard card : cards) card.drag(mouseX);
        }
    }

    public void releasePointer() {
        for (List<ModuleCard> cards : this.tabCards.values()) {
            for (ModuleCard card : cards) card.releasePointer();
        }
    }

    public boolean handleKey(int key) {
        if (this.searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.searchQuery.isEmpty()) {
                    this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                    rebuildAllTabs();
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                this.searchFocused = false;
                return true;
            }
        }

        if (this.friendInputFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.friendInputText.isEmpty()) this.friendInputText = this.friendInputText.substring(0, this.friendInputText.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) {
                if (!this.friendInputText.trim().isEmpty()) {
                    FriendManager.INSTANCE.add(this.friendInputText.trim());
                    this.friendInputText = "";
                }
                this.friendInputFocused = false;
                return true;
            }
        }

        if (this.configInputFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.configInputText.isEmpty()) this.configInputText = this.configInputText.substring(0, this.configInputText.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) {
                saveNewConfig();
                this.configInputFocused = false;
                return true;
            }
        }

        List<ModuleCard> cards = this.tabCards.get(this.activeTab);
        if (cards != null) {
            for (ModuleCard card : cards) {
                if (card.handleKey(key)) return true;
            }
        }
        return false;
    }

    public boolean isCapturingBind() {
        List<ModuleCard> cards = this.tabCards.get(this.activeTab);
        if (cards != null) {
            for (ModuleCard card : cards) {
                if (card.isCapturingBind()) return true;
            }
        }
        return false;
    }

    public boolean handleCharacter(int codePoint) {
        if (this.searchFocused && codePoint >= 32) {
            this.searchQuery += new String(Character.toChars(codePoint));
            rebuildAllTabs();
            return true;
        }

        if (this.friendInputFocused && codePoint >= 32) {
            this.friendInputText += new String(Character.toChars(codePoint));
            return true;
        }

        if (this.configInputFocused && codePoint >= 32) {
            this.configInputText += new String(Character.toChars(codePoint));
            return true;
        }

        List<ModuleCard> cards = this.tabCards.get(this.activeTab);
        if (cards != null) {
            for (ModuleCard card : cards) {
                if (card.handleCharacter(codePoint)) return true;
            }
        }
        return false;
    }

    public void handleScroll(int mouseX, int mouseY, double vertical) {
        SmoothScroll scroll = this.tabScrolls.get(this.activeTab);
        int maxContent = this.tabMaxContent.getOrDefault(this.activeTab, 500);
        if (scroll != null) {
            scroll.scroll(vertical, Math.max(0, maxContent - CONTENT_HEIGHT));
        }
    }

    public boolean isSearchOpen() { return this.searchFocused; }
    public boolean isSearchFocused() { return this.searchFocused; }
    public void focusSearch() { this.searchFocused = true; }
    public void appendCodePoint(int codePoint) { handleCharacter(codePoint); }
    public void backspace() { handleKey(GLFW.GLFW_KEY_BACKSPACE); }

    @Override
    public void render(MinecraftClient minecraft, DrawContext context) {
        if (this.displayedPage != MenuPage.NONE) return;

        MsdfFont font = UiFonts.sfProDisplay();
        int accent = Theme.getAccent();

        // 1. LEFT SIDEBAR BACKGROUND (#0b0c10)
        Render2DUtil.rect(sx(0), sy(0), px(SIDEBAR_WIDTH), px(640))
                .color(ColorUtil.rgba(11, 12, 16, 255))
                .radius(px(16), 0.0F, 0.0F, px(16))
                .draw();

        Render2DUtil.rect(sx(SIDEBAR_WIDTH), sy(0), px(1.0F), px(640))
                .color(ColorUtil.rgba(26, 27, 36, 255))
                .draw();

        // 2. LOGO HEADER ("P" Punch Emblem!)
        Render2DUtil.text(sx(95), font.centeredTextY(sy(28), px(24.0F)), px(24.0F), "P")
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.WHITE)
                .draw();

        Render2DUtil.rect(sx(14), sy(50), px(192), px(1.0F))
                .color(ColorUtil.rgba(26, 27, 36, 255))
                .draw();

        // 3. NAVIGATION SECTIONS
        float currY = 64;
        float itemH = 30;
        String lastSec = "";

        for (NitamaTab tab : NitamaTab.values()) {
            if (!tab.getSection().equals(lastSec)) {
                lastSec = tab.getSection();
                Render2DUtil.text(sx(20), sy(currY + 6), px(11.0F), lastSec)
                        .style(UiFontStyle.MEDIUM)
                        .color(ColorUtil.rgb(88, 89, 102))
                        .draw();
                currY += 22;
            }

            boolean active = (tab == this.activeTab);
            float itemX = 14;
            float itemW = 192;

            if (active) {
                Render2DUtil.rect(sx(itemX), sy(currY), px(itemW), px(itemH))
                        .color(ColorUtil.rgba(28, 29, 39, 255))
                        .radius(px(10))
                        .border(px(1.0F), ColorUtil.rgba(43, 44, 61, 255))
                        .draw();
            }

            float iconSize = px(15);
            Render2DUtil.texture(sx(itemX + 12), sy(currY) + (px(itemH) - iconSize) / 2.0F, iconSize, iconSize, tab.getIcon())
                    .color(active ? ColorUtil.WHITE : ColorUtil.rgb(133, 134, 148))
                    .draw();

            Render2DUtil.text(sx(itemX + 36), font.centeredTextY(sy(currY) + px(itemH) / 2.0F, px(13.0F)), px(13.0F), tab.getTitle())
                    .style(active ? UiFontStyle.SEMIBOLD : UiFontStyle.MEDIUM)
                    .color(active ? ColorUtil.WHITE : ColorUtil.rgb(153, 154, 166))
                    .draw();

            currY += itemH + 2;
        }

        // 4. SEARCH INPUT BOX
        float searchX = 14;
        float searchY = 535;
        float searchW = 192;
        float searchH = 30;

        Render2DUtil.rect(sx(searchX), sy(searchY), px(searchW), px(searchH))
                .color(ColorUtil.rgba(20, 21, 30, 255))
                .radius(px(15))
                .border(px(1.0F), this.searchFocused ? accent : ColorUtil.rgba(31, 32, 44, 255))
                .draw();

        Render2DUtil.texture(sx(searchX + 10), sy(searchY) + (px(searchH) - px(14)) / 2.0F, px(14), px(14), Textures.Header.SEARCH)
                .color(ColorUtil.rgb(93, 94, 112))
                .draw();

        String searchDisp = this.searchQuery.isEmpty() ? "Search..." : this.searchQuery;
        Render2DUtil.text(sx(searchX + 32), font.centeredTextY(sy(searchY) + px(searchH) / 2.0F, px(12.0F)), px(12.0F), searchDisp)
                .style(UiFontStyle.REGULAR)
                .color(this.searchQuery.isEmpty() ? ColorUtil.rgb(74, 75, 91) : ColorUtil.WHITE)
                .draw();

        // 5. FOOTER COPYRIGHT TEXT (Punch client)
        Render2DUtil.text(sx(searchX + 4), sy(572), px(9.5F), "Punch client 2026 all rights")
                .style(UiFontStyle.REGULAR)
                .color(ColorUtil.rgb(82, 83, 98))
                .draw();
        Render2DUtil.text(sx(searchX + 4), sy(584), px(9.5F), "reserved. Subscription plan : Beta.")
                .style(UiFontStyle.REGULAR)
                .color(ColorUtil.rgb(82, 83, 98))
                .draw();

        // 6. USER PROFILE CARD (fre1m)
        float profileY = 595;
        float profileH = 38;

        Render2DUtil.rect(sx(searchX), sy(profileY), px(searchW), px(profileH))
                .color(ColorUtil.rgba(18, 19, 27, 255))
                .radius(px(12))
                .border(px(1.0F), ColorUtil.rgba(28, 29, 41, 255))
                .draw();

        Render2DUtil.texture(sx(searchX + 6), sy(profileY + 5), px(28), px(28), Textures.Header.DEV_AVATAR)
                .draw();

        String username = NameProtectUtil.protect(minecraft.getSession().getUsername());
        if (username.length() > 8) username = username.substring(0, 8);
        Render2DUtil.text(sx(searchX + 40), sy(profileY + 8), px(12.5F), username.isEmpty() ? "fre1m" : username)
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.WHITE)
                .draw();

        Render2DUtil.text(sx(searchX + 40), sy(profileY + 22), px(10.0F), "User")
                .style(UiFontStyle.MEDIUM)
                .color(ColorUtil.rgb(217, 56, 56))
                .draw();

        float logoutW = 60;
        float logoutH = 22;
        float logoutX = searchX + searchW - logoutW - 6;
        float logoutY = profileY + (profileH - logoutH) / 2.0F;

        Render2DUtil.rect(sx(logoutX), sy(logoutY), px(logoutW), px(logoutH))
                .color(ColorUtil.rgba(45, 21, 24, 255))
                .radius(px(10))
                .border(px(1.0F), ColorUtil.rgba(94, 29, 36, 255))
                .draw();

        Render2DUtil.text(sx(logoutX + 6), font.centeredTextY(sy(logoutY) + px(logoutH) / 2.0F, px(9.5F)), px(9.5F), "[-> Log Out")
                .style(UiFontStyle.MEDIUM)
                .color(ColorUtil.rgb(238, 68, 68))
                .draw();

        // 7. RIGHT MAIN CONTENT PANEL BACKGROUND (#0d0e12)
        Render2DUtil.rect(sx(RIGHT_PANEL_X), sy(0), px(RIGHT_PANEL_WIDTH), px(640))
                .color(ColorUtil.rgba(13, 14, 18, 255))
                .radius(0.0F, px(16), px(16), 0.0F)
                .draw();

        // Top Header Breadcrumb Bar
        Render2DUtil.rect(sx(RIGHT_PANEL_X), sy(0), px(RIGHT_PANEL_WIDTH), px(HEADER_HEIGHT))
                .color(ColorUtil.rgba(11, 12, 16, 255))
                .radius(0.0F, px(16), 0.0F, 0.0F)
                .draw();

        Render2DUtil.rect(sx(RIGHT_PANEL_X), sy(HEADER_HEIGHT), px(RIGHT_PANEL_WIDTH), px(1.0F))
                .color(ColorUtil.rgba(26, 27, 36, 255))
                .draw();

        // Breadcrumb Trail: "Visuals / 🧊 Render"
        Render2DUtil.text(sx(RIGHT_PANEL_X + 20), font.centeredTextY(sy(HEADER_HEIGHT / 2.0F), px(13.0F)), px(13.0F), this.activeTab.getSection())
                .style(UiFontStyle.MEDIUM)
                .color(ColorUtil.rgb(132, 133, 150))
                .draw();

        Render2DUtil.text(sx(RIGHT_PANEL_X + 72), font.centeredTextY(sy(HEADER_HEIGHT / 2.0F), px(13.0F)), px(13.0F), "/")
                .style(UiFontStyle.REGULAR)
                .color(ColorUtil.rgb(74, 75, 91))
                .draw();

        Render2DUtil.texture(sx(RIGHT_PANEL_X + 88), sy((HEADER_HEIGHT - 16) / 2.0F), px(16), px(16), this.activeTab.getIcon())
                .color(ColorUtil.WHITE)
                .draw();

        Render2DUtil.text(sx(RIGHT_PANEL_X + 110), font.centeredTextY(sy(HEADER_HEIGHT / 2.0F), px(13.0F)), px(13.0F), this.activeTab.getTitle())
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.WHITE)
                .draw();

        // 8. RENDER CUSTOM PAGES (Color Themes, Configs, Friends, About us)
        if (this.activeTab == NitamaTab.COLOR_THEMES) {
            renderColorThemesPage();
            return;
        }

        if (this.activeTab == NitamaTab.FRIENDS) {
            renderFriendsPage();
            return;
        }

        if (this.activeTab == NitamaTab.CONFIGS) {
            renderConfigsPage();
            return;
        }

        if (this.activeTab == NitamaTab.ABOUT_US) {
            renderAboutUsPage();
            return;
        }

        // Center Crosshair Icon 'x'
        List<ModuleCard> currentCards = this.tabCards.get(this.activeTab);
        if (currentCards == null || currentCards.isEmpty()) {
            float centerX = sx(RIGHT_PANEL_X + RIGHT_PANEL_WIDTH / 2.0F);
            float centerY = sy(HEADER_HEIGHT + (640 - HEADER_HEIGHT) / 2.0F);
            Render2DUtil.text(centerX, font.centeredTextY(centerY, px(14.0F)), px(14.0F), "x")
                    .style(UiFontStyle.REGULAR)
                    .align(tech.onetap.ui.punch.gui.TextAlign.CENTER)
                    .color(ColorUtil.rgb(74, 75, 91))
                    .draw();
        }

        // 9. RENDER MODULE CARDS WITH SCISSOR IN DESIGN PIXEL BOUNDS
        if (currentCards != null && !currentCards.isEmpty()) {
            Render2DUtil.pushScissor(sx(CONTENT_X), sy(CONTENT_Y), px(CONTENT_WIDTH), px(CONTENT_HEIGHT));
            for (ModuleCard card : currentCards) {
                card.render(minecraft, context);
            }
            Render2DUtil.popScissor();

            for (ModuleCard card : currentCards) {
                card.renderOverlay(minecraft, context);
            }
        }
    }

    private void renderColorThemesPage() {
        MsdfFont font = UiFonts.sfProDisplay();
        int currentAccent = Theme.getAccent();

        float cardX = RIGHT_PANEL_X + 24;
        float cardY = 64;
        float cardW = RIGHT_PANEL_WIDTH - 48;
        float cardH = 130;

        Render2DUtil.rect(sx(cardX), sy(cardY), px(cardW), px(cardH))
                .color(ColorUtil.rgba(17, 18, 24, 255))
                .radius(px(14))
                .border(px(1.0F), ColorUtil.rgba(26, 27, 36, 255))
                .draw();

        Render2DUtil.text(sx(cardX + 16), sy(cardY + 18), px(14.0F), "Accent Color Presets")
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.WHITE)
                .draw();

        Render2DUtil.text(sx(cardX + 16), sy(cardY + 36), px(11.0F), "Select primary accent color for ClickGUI buttons, toggles and highlights")
                .style(UiFontStyle.REGULAR)
                .color(ColorUtil.rgb(133, 134, 148))
                .draw();

        float swatchY = cardY + 70;
        float swatchSize = 26;
        float swatchGap = 10;
        for (int i = 0; i < Theme.accentCount(); i++) {
            float sx = cardX + 16 + i * (swatchSize + swatchGap);
            int color = Theme.accent(i);
            boolean selected = (Theme.accentIndex() == i);

            Render2DUtil.rect(sx(sx), sy(swatchY), px(swatchSize), px(swatchSize))
                    .color(color)
                    .radius(px(6))
                    .draw();

            if (selected) {
                Render2DUtil.rect(sx(sx - 2), sy(swatchY - 2), px(swatchSize + 4), px(swatchSize + 4))
                        .color(ColorUtil.TRANSPARENT)
                        .radius(px(8))
                        .border(px(1.5F), ColorUtil.WHITE)
                        .draw();
            }
        }
    }

    private void renderFriendsPage() {
        MsdfFont font = UiFonts.sfProDisplay();
        int accent = Theme.getAccent();

        float cardX = RIGHT_PANEL_X + 24;
        float cardY = 64;
        float cardW = RIGHT_PANEL_WIDTH - 48;

        Render2DUtil.rect(sx(cardX), sy(cardY), px(cardW), px(100))
                .color(ColorUtil.rgba(17, 18, 24, 255))
                .radius(px(14))
                .border(px(1.0F), ColorUtil.rgba(26, 27, 36, 255))
                .draw();

        Render2DUtil.text(sx(cardX + 16), sy(cardY + 16), px(14.0F), "Friends Manager")
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.WHITE)
                .draw();

        Render2DUtil.text(sx(cardX + 16), sy(cardY + 34), px(11.0F), "Add players to friend list to prevent targeting in Combat modules")
                .style(UiFontStyle.REGULAR)
                .color(ColorUtil.rgb(133, 134, 148))
                .draw();

        float inputX = cardX + 16;
        float inputY = cardY + 56;
        float inputW = 220;
        float inputH = 32;

        Render2DUtil.rect(sx(inputX), sy(inputY), px(inputW), px(inputH))
                .color(ColorUtil.rgba(20, 21, 30, 255))
                .radius(px(8))
                .border(px(1.0F), this.friendInputFocused ? accent : ColorUtil.rgba(31, 32, 44, 255))
                .draw();

        String disp = this.friendInputText.isEmpty() ? "Add player by name..." : this.friendInputText;
        Render2DUtil.text(sx(inputX + 10), font.centeredTextY(sy(inputY + inputH / 2.0F), px(12.0F)), px(12.0F), disp)
                .style(UiFontStyle.REGULAR)
                .color(this.friendInputText.isEmpty() ? ColorUtil.rgb(74, 75, 91) : ColorUtil.WHITE)
                .draw();

        float btnX = inputX + inputW + 10;
        Render2DUtil.rect(sx(btnX), sy(inputY), px(100), px(inputH))
                .color(accent)
                .radius(px(8))
                .draw();

        Render2DUtil.text(sx(btnX + 18), font.centeredTextY(sy(inputY + inputH / 2.0F), px(12.0F)), px(12.0F), "+ Add Friend")
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.WHITE)
                .draw();

        List<String> friends = new ArrayList<>(FriendManager.INSTANCE.getFriends());
        float listY = cardY + 116;

        if (friends.isEmpty()) {
            Render2DUtil.text(sx(cardX + 16), sy(listY + 20), px(12.0F), "No friends added yet.")
                    .style(UiFontStyle.REGULAR)
                    .color(ColorUtil.rgb(133, 134, 148))
                    .draw();
        } else {
            for (int i = 0; i < friends.size(); i++) {
                String friendName = friends.get(i);
                float itemY = listY + i * 44;

                Render2DUtil.rect(sx(cardX), sy(itemY), px(cardW), px(38))
                        .color(ColorUtil.rgba(17, 18, 24, 255))
                        .radius(px(10))
                        .border(px(1.0F), ColorUtil.rgba(26, 27, 36, 255))
                        .draw();

                Render2DUtil.text(sx(cardX + 16), font.centeredTextY(sy(itemY + 19), px(13.0F)), px(13.0F), friendName)
                        .style(UiFontStyle.SEMIBOLD)
                        .color(ColorUtil.WHITE)
                        .draw();

                float removeX = cardX + cardW - 84;
                Render2DUtil.rect(sx(removeX), sy(itemY + 6), px(74), px(26))
                        .color(ColorUtil.rgba(45, 21, 24, 255))
                        .radius(px(6))
                        .border(px(1.0F), ColorUtil.rgba(94, 29, 36, 255))
                        .draw();

                Render2DUtil.text(sx(removeX + 14), font.centeredTextY(sy(itemY + 19), px(10.0F)), px(10.0F), "Remove")
                        .style(UiFontStyle.MEDIUM)
                        .color(ColorUtil.rgb(238, 68, 68))
                        .draw();
            }
        }
    }

    private void renderConfigsPage() {
        MsdfFont font = UiFonts.sfProDisplay();
        int accent = Theme.getAccent();

        float cardX = RIGHT_PANEL_X + 24;
        float cardY = 64;
        float cardW = RIGHT_PANEL_WIDTH - 48;

        Render2DUtil.rect(sx(cardX), sy(cardY), px(cardW), px(100))
                .color(ColorUtil.rgba(17, 18, 24, 255))
                .radius(px(14))
                .border(px(1.0F), ColorUtil.rgba(26, 27, 36, 255))
                .draw();

        Render2DUtil.text(sx(cardX + 16), sy(cardY + 16), px(14.0F), "Configs Manager")
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.WHITE)
                .draw();

        Render2DUtil.text(sx(cardX + 16), sy(cardY + 34), px(11.0F), "Save, load and share your feature preset configurations")
                .style(UiFontStyle.REGULAR)
                .color(ColorUtil.rgb(133, 134, 148))
                .draw();

        float inputX = cardX + 16;
        float inputY = cardY + 56;
        float inputW = 220;
        float inputH = 32;

        Render2DUtil.rect(sx(inputX), sy(inputY), px(inputW), px(inputH))
                .color(ColorUtil.rgba(20, 21, 30, 255))
                .radius(px(8))
                .border(px(1.0F), this.configInputFocused ? accent : ColorUtil.rgba(31, 32, 44, 255))
                .draw();

        String disp = this.configInputText.isEmpty() ? "Config Name..." : this.configInputText;
        Render2DUtil.text(sx(inputX + 10), font.centeredTextY(sy(inputY + inputH / 2.0F), px(12.0F)), px(12.0F), disp)
                .style(UiFontStyle.REGULAR)
                .color(this.configInputText.isEmpty() ? ColorUtil.rgb(74, 75, 91) : ColorUtil.WHITE)
                .draw();

        float btnX = inputX + inputW + 10;
        Render2DUtil.rect(sx(btnX), sy(inputY), px(110), px(inputH))
                .color(accent)
                .radius(px(8))
                .draw();

        Render2DUtil.text(sx(btnX + 18), font.centeredTextY(sy(inputY + inputH / 2.0F), px(12.0F)), px(12.0F), "+ Save Config")
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.WHITE)
                .draw();

        // Config Items List (Live Config Display)
        float listY = cardY + 116;
        for (int i = 0; i < this.configList.size(); i++) {
            String cfg = this.configList.get(i);
            float itemY = listY + i * 46;
            boolean active = cfg.equals(this.activeConfigName);

            Render2DUtil.rect(sx(cardX), sy(itemY), px(cardW), px(40))
                    .color(ColorUtil.rgba(17, 18, 24, 255))
                    .radius(px(10))
                    .border(px(1.0F), active ? accent : ColorUtil.rgba(26, 27, 36, 255))
                    .draw();

            Render2DUtil.text(sx(cardX + 16), font.centeredTextY(sy(itemY + 20), px(13.0F)), px(13.0F), cfg)
                    .style(UiFontStyle.SEMIBOLD)
                    .color(ColorUtil.WHITE)
                    .draw();

            if (active) {
                Render2DUtil.text(sx(cardX + 160), font.centeredTextY(sy(itemY + 20), px(10.0F)), px(10.0F), "[ACTIVE]")
                        .style(UiFontStyle.MEDIUM)
                        .color(accent)
                        .draw();
            }

            // Load Button
            float loadBtnX = cardX + cardW - 140;
            Render2DUtil.rect(sx(loadBtnX), sy(itemY + 7), px(60), px(26))
                    .color(ColorUtil.rgba(28, 29, 39, 255))
                    .radius(px(6))
                    .border(px(1.0F), ColorUtil.rgba(43, 44, 61, 255))
                    .draw();

            Render2DUtil.text(sx(loadBtnX + 16), font.centeredTextY(sy(itemY + 20), px(10.0F)), px(10.0F), "Load")
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.WHITE)
                    .draw();

            // Delete Button
            float delBtnX = cardX + cardW - 74;
            Render2DUtil.rect(sx(delBtnX), sy(itemY + 7), px(60), px(26))
                    .color(ColorUtil.rgba(45, 21, 24, 255))
                    .radius(px(6))
                    .border(px(1.0F), ColorUtil.rgba(94, 29, 36, 255))
                    .draw();

            Render2DUtil.text(sx(delBtnX + 14), font.centeredTextY(sy(itemY + 20), px(10.0F)), px(10.0F), "Delete")
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.rgb(238, 68, 68))
                    .draw();
        }
    }

    private void renderAboutUsPage() {
        MsdfFont font = UiFonts.sfProDisplay();

        float cardX = RIGHT_PANEL_X + 24;
        float cardY = 64;
        float cardW = RIGHT_PANEL_WIDTH - 48;

        Render2DUtil.rect(sx(cardX), sy(cardY), px(cardW), px(140))
                .color(ColorUtil.rgba(17, 18, 24, 255))
                .radius(px(14))
                .border(px(1.0F), ColorUtil.rgba(26, 27, 36, 255))
                .draw();

        Render2DUtil.text(sx(cardX + 20), sy(cardY + 24), px(22.0F), "Punch Client 2026")
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.WHITE)
                .draw();

        Render2DUtil.text(sx(cardX + 20), sy(cardY + 50), px(12.0F), "Advanced Utility Client for MinecraftClient 1.21.4 (Fabric)")
                .style(UiFontStyle.MEDIUM)
                .color(ColorUtil.rgb(133, 134, 148))
                .draw();

        Render2DUtil.text(sx(cardX + 20), sy(cardY + 75), px(11.0F), "Subscription Plan: Beta / Lifetime")
                .style(UiFontStyle.REGULAR)
                .color(ColorUtil.rgb(217, 56, 56))
                .draw();

        Render2DUtil.text(sx(cardX + 20), sy(cardY + 95), px(11.0F), "Runtime: Java 21+ | Fabric Loom 1.17.12 | All rights reserved 2026")
                .style(UiFontStyle.REGULAR)
                .color(ColorUtil.rgb(82, 83, 98))
                .draw();

        float statsY = cardY + 156;
        Render2DUtil.rect(sx(cardX), sy(statsY), px(cardW), px(80))
                .color(ColorUtil.rgba(17, 18, 24, 255))
                .radius(px(14))
                .border(px(1.0F), ColorUtil.rgba(26, 27, 36, 255))
                .draw();

        Render2DUtil.text(sx(cardX + 20), sy(statsY + 20), px(14.0F), "Client Statistics")
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.WHITE)
                .draw();

        int totalFeatures = FeatureManager.INSTANCE.getFeatures().size();
        Render2DUtil.text(sx(cardX + 20), sy(statsY + 44), px(12.0F), "Registered Modules: " + totalFeatures + " features active")
                .style(UiFontStyle.REGULAR)
                .color(ColorUtil.rgb(133, 134, 148))
                .draw();
    }
}
