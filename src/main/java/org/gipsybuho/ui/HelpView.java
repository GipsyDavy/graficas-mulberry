package org.gipsybuho.ui;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.gipsybuho.model.HelpEntry;
import org.gipsybuho.service.HelpService;
import org.gipsybuho.service.SoundService;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HelpView extends BorderPane {

    private static final String DEFAULT_ARTICLE = "GEN-PS-1";

    private static final String[] MODULE_IDS = {
        "general", "clientes", "materiales", "empleados", "presupuestos",
        "facturas", "albaranes", "pedidos", "nominas", "tarifas",
        "importacion", "exportacion", "backups", "ia", "asistente",
        "estadisticas", "calendario", "usuarios", "configuracion"
    };

    private static final Map<String, String> MODULE_LABELS;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("general",       "General");
        m.put("clientes",      "Clientes");
        m.put("materiales",    "Materiales");
        m.put("empleados",     "Empleados");
        m.put("presupuestos",  "Presupuestos");
        m.put("facturas",      "Facturas");
        m.put("albaranes",     "Albaranes");
        m.put("pedidos",       "Pedidos");
        m.put("nominas",       "Nóminas");
        m.put("tarifas",       "Tarifas");
        m.put("importacion",   "Importación");
        m.put("exportacion",   "Exportación");
        m.put("backups",       "Copias de seguridad");
        m.put("ia",            "Asistente IA");
        m.put("asistente",     "Asistente Visual");
        m.put("estadisticas",  "Estadísticas");
        m.put("calendario",    "Calendario");
        m.put("usuarios",      "Usuarios y Roles");
        m.put("configuracion", "Configuración");
        MODULE_LABELS = Collections.unmodifiableMap(m);
    }

    private final HelpService service = HelpService.getInstance();
    private final Map<String, HelpEntry> idToEntry = new HashMap<>();

    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();
    private final TreeView<String> treeView = new TreeView<>();
    private final TextField tfSearch = new TextField();
    private final Button btnBack    = new Button("←");
    private final Button btnForward = new Button("→");

    /** Historial de navegación dentro de la sesión (IDs de artículos). */
    private final List<String> history = new ArrayList<>();
    private int histIdx = -1;
    /** Suprime el listener de selección durante navegación programática. */
    private boolean programmaticSelection = false;

    public HelpView() {
        init(DEFAULT_ARTICLE);
    }

    /** Abre la ayuda mostrando el primer artículo del módulo indicado. */
    public HelpView(String moduleId) {
        String firstId = service.getByModule(moduleId).stream()
            .findFirst().map(HelpEntry::id).orElse(DEFAULT_ARTICLE);
        init(firstId);
    }

    private HelpView(String articleId, boolean byArticleId) {
        init(articleId);
    }

    /** Abre la ayuda mostrando el artículo con el ID exacto indicado. */
    public static HelpView forArticle(String articleId) {
        boolean exists = HelpService.getInstance().getAll().stream()
            .anyMatch(e -> e.id().equals(articleId));
        return new HelpView(exists ? articleId : DEFAULT_ARTICLE, true);
    }

    private void init(String initialArticleId) {
        getStyleClass().add("help-view");
        for (HelpEntry e : service.getAll()) {
            idToEntry.put(e.id(), e);
        }
        setTop(buildTopBar());
        setLeft(buildLeftPanel());
        setCenter(webView);
        populateTree(null);
        openArticle(initialArticleId);
    }

    // ── Barra superior ────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        btnBack.getStyleClass().add("btn-toolbar");
        btnBack.setTooltip(new Tooltip("Artículo anterior"));
        btnBack.setDisable(true);
        btnBack.setOnAction(e -> navigateBack());

        btnForward.getStyleClass().add("btn-toolbar");
        btnForward.setTooltip(new Tooltip("Artículo siguiente"));
        btnForward.setDisable(true);
        btnForward.setOnAction(e -> navigateForward());

        Button btnHome = new Button();
        btnHome.setGraphic(Icons.home());
        btnHome.getStyleClass().add("btn-toolbar");
        btnHome.setTooltip(new Tooltip("Inicio de la ayuda"));
        btnHome.setOnAction(e -> openArticle(DEFAULT_ARTICLE));

        tfSearch.setPromptText("Buscar en la ayuda…");
        tfSearch.getStyleClass().add("sidebar-search");
        tfSearch.setPrefWidth(280);
        tfSearch.textProperty().addListener((obs, old, val) -> populateTree(val));

        Separator sep = new Separator(Orientation.VERTICAL);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(6, btnBack, btnForward, btnHome, sep, tfSearch, spacer);
        bar.getStyleClass().add("help-topbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 12, 8, 12));
        return bar;
    }

    // ── Panel izquierdo (índice de contenidos) ────────────────────────────────

    private VBox buildLeftPanel() {
        Label lblTitle = new Label("Índice de ayuda");
        lblTitle.getStyleClass().add("help-panel-title");
        lblTitle.setPadding(new Insets(12, 12, 6, 12));

        treeView.setShowRoot(false);
        treeView.getStyleClass().add("help-tree");
        VBox.setVgrow(treeView, Priority.ALWAYS);

        treeView.setCellFactory(tv -> new TreeCell<String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("help-tree-article", "help-tree-group");
                if (empty || value == null) {
                    setText(null);
                } else if (idToEntry.containsKey(value)) {
                    setText(idToEntry.get(value).title());
                    getStyleClass().add("help-tree-article");
                } else {
                    setText(value);
                    getStyleClass().add("help-tree-group");
                }
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (programmaticSelection) return;
            if (sel != null && sel.isLeaf() && idToEntry.containsKey(sel.getValue())) {
                SoundService.play(SoundService.Sound.NAVIGATE);
                openArticle(sel.getValue());
            }
        });

        VBox panel = new VBox(lblTitle, treeView);
        panel.setPrefWidth(220);
        panel.getStyleClass().add("help-left-panel");
        return panel;
    }

    // ── Árbol de contenidos ───────────────────────────────────────────────────

    private void populateTree(String query) {
        TreeItem<String> root = new TreeItem<>("");
        root.setExpanded(true);

        boolean searching = query != null && !query.isBlank();

        if (searching) {
            List<HelpEntry> results = service.search(query);
            String groupLabel = "Resultados (" + results.size() + ")";
            TreeItem<String> resultsGroup = new TreeItem<>(groupLabel);
            resultsGroup.setExpanded(true);
            for (HelpEntry e : results) {
                resultsGroup.getChildren().add(new TreeItem<>(e.id()));
            }
            root.getChildren().add(resultsGroup);
        } else {
            for (String moduleId : MODULE_IDS) {
                List<HelpEntry> moduleEntries = service.getByModule(moduleId);
                if (moduleEntries.isEmpty()) continue;
                String label = MODULE_LABELS.getOrDefault(moduleId, moduleId);
                TreeItem<String> group = new TreeItem<>(label);
                group.setExpanded("general".equals(moduleId));
                for (HelpEntry e : moduleEntries) {
                    group.getChildren().add(new TreeItem<>(e.id()));
                }
                root.getChildren().add(group);
            }
        }

        treeView.setRoot(root);
    }

    // ── Carga de artículo ─────────────────────────────────────────────────────

    private void openArticle(String id) {
        HelpEntry entry = idToEntry.get(id);
        if (entry == null) return;

        // Truncar historial hacia adelante y añadir nueva entrada
        while (history.size() > histIdx + 1) {
            history.remove(history.size() - 1);
        }
        history.add(id);
        histIdx++;

        loadHtml(entry.path());
        selectInTree(id);
        updateNavButtons();
    }

    private void navigateBack() {
        if (histIdx > 0) {
            histIdx--;
            String id = history.get(histIdx);
            HelpEntry entry = idToEntry.get(id);
            if (entry != null) loadHtml(entry.path());
            selectInTree(id);
            updateNavButtons();
        }
    }

    private void navigateForward() {
        if (histIdx < history.size() - 1) {
            histIdx++;
            String id = history.get(histIdx);
            HelpEntry entry = idToEntry.get(id);
            if (entry != null) loadHtml(entry.path());
            selectInTree(id);
            updateNavButtons();
        }
    }

    /**
     * Carga el HTML del artículo en el WebView.
     * Reemplaza el href relativo del CSS por la URL absoluta del classpath
     * para garantizar que el estilo se aplica correctamente desde el JAR.
     */
    private void loadHtml(String path) {
        try (InputStream is = getClass().getResourceAsStream("/org/gipsybuho/help/" + path)) {
            if (is == null) {
                engine.loadContent("<p>Artículo no encontrado.</p>");
                return;
            }
            String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            URL cssUrl = getClass().getResource("/org/gipsybuho/help/help.css");
            if (cssUrl != null) {
                html = html.replace("href=\"../help.css\"", "href=\"" + cssUrl.toExternalForm() + "\"");
            }
            engine.loadContent(html, "text/html");
        } catch (Exception e) {
            engine.loadContent("<p>Error al cargar el artículo.</p>");
        }
    }

    // ── Helpers de navegación ─────────────────────────────────────────────────

    private void updateNavButtons() {
        btnBack.setDisable(histIdx <= 0);
        btnForward.setDisable(histIdx >= history.size() - 1);
    }

    private void selectInTree(String id) {
        programmaticSelection = true;
        try {
            TreeItem<String> root = treeView.getRoot();
            if (root == null) return;
            TreeItem<String> found = findTreeItem(root, id);
            if (found != null) {
                if (found.getParent() != null) found.getParent().setExpanded(true);
                treeView.getSelectionModel().select(found);
                int row = treeView.getRow(found);
                if (row >= 0) treeView.scrollTo(row);
            }
        } finally {
            programmaticSelection = false;
        }
    }

    private TreeItem<String> findTreeItem(TreeItem<String> node, String id) {
        if (id.equals(node.getValue())) return node;
        for (TreeItem<String> child : node.getChildren()) {
            TreeItem<String> found = findTreeItem(child, id);
            if (found != null) return found;
        }
        return null;
    }
}
