package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.service.MusicService;
import org.gipsybuho.service.OllamaService;
import org.gipsybuho.service.TemaManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.file.FileSystems;
import java.nio.file.FileStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class ConfiguracionView extends VBox {

    // ── Datos de cada tema ─────────────────────────────────────────────────────
    private record Tema(String id, String nombre,
                        String primary, String sidebar, String bg, String accent) {}

    private static final List<Tema> TEMAS = List.of(
        new Tema("mulberry", "Mulberry",    "#6B2D5E", "#2D1A28", "#F5F0F4", "#E891D0"),
        new Tema("oscuro",   "Oscuro",      "#7B93D0", "#1A1D2E", "#242638", "#A8BEFF"),
new Tema("azul",     "Azul marino", "#1A56A6", "#0D2845", "#EFF4FB", "#64AFFF"),
        new Tema("verde",    "Verde",       "#2D6A4F", "#1B4332", "#F0F7F4", "#74C69D"),
        new Tema("rojo",     "Rojo",        "#A03030", "#4A1010", "#FBF3F3", "#FF8888"),
        new Tema("claro",    "Claro",       "#4A5568", "#2D3748", "#F7F8FA", "#90CDF4")
    );

    private static final List<String> FUENTES = List.of(
        "Segoe UI", "Arial", "Calibri", "Georgia", "Consolas", "Trebuchet MS", "Verdana"
    );

    private record TamanoFuente(String etiqueta, String px) {}
    private static final List<TamanoFuente> TAMANIOS = List.of(
        new TamanoFuente("Pequeño",     "11"),
        new TamanoFuente("Normal",      "13"),
        new TamanoFuente("Grande",      "15"),
        new TamanoFuente("Muy grande",  "17")
    );

    // ── Constructor ───────────────────────────────────────────────────────────

    public ConfiguracionView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(16);

        Label titulo = new Label("Configuración");
        titulo.getStyleClass().add("view-title");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Tab tabApariencia   = new Tab("🎨  Apariencia",  buildTabApariencia());
        Tab tabEmpresa      = new Tab("🏢  Mi empresa",  buildTabEmpresa());
        Tab tabPreferencias = new Tab("⚙  Preferencias", buildTabPreferencias());
        Tab tabMusica       = new Tab("🎵  Música",       buildTabMusica());
        Tab tabModelosIA    = new Tab("🤖  Modelos IA",   buildTabModelosIA());
        Tab tabAcercaDe     = new Tab("ℹ  Acerca de",     buildTabAcercaDe());

        tabs.getTabs().addAll(tabApariencia, tabEmpresa, tabPreferencias, tabMusica, tabModelosIA, tabAcercaDe);
        getChildren().addAll(titulo, tabs);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB 1 — APARIENCIA
    // ═════════════════════════════════════════════════════════════════════════

    private ScrollPane buildTabApariencia() {
        VBox contenido = new VBox(20);
        contenido.setPadding(new Insets(20));

        // ── Sección: tema de color ────────────────────────────────────────────
        VBox seccionTema = new VBox(10);
        Label tituloTema = new Label("Tema de color");
        tituloTema.getStyleClass().add("config-section-title");
        Label descTema = new Label("Cambia al instante el aspecto visual completo de la aplicación.");
        descTema.getStyleClass().add("config-section-desc");

        FlowPane temaCards = new FlowPane(12, 12);
        String temaActual = TemaManager.getTemaActual();
        for (Tema t : TEMAS) {
            temaCards.getChildren().add(buildTemaCard(t, t.id().equals(temaActual)));
        }

        seccionTema.getChildren().addAll(tituloTema, descTema, temaCards);

        // ── Sección: tipografía ───────────────────────────────────────────────
        VBox seccionFuente = new VBox(10);
        Label tituloFuente = new Label("Tipografía");
        tituloFuente.getStyleClass().add("config-section-title");
        Label descFuente = new Label("La fuente y el tamaño se aplican al guardar. El título y algunos elementos fijos mantienen su tamaño relativo.");
        descFuente.getStyleClass().add("config-section-desc");
        descFuente.setWrapText(true);

        // Fuente
        GridPane gridFuente = new GridPane();
        gridFuente.setHgap(12);
        gridFuente.setVgap(14);

        Label lblFuente = new Label("Fuente:");
        lblFuente.getStyleClass().add("config-form-label");
        ComboBox<String> cbFuente = new ComboBox<>();
        cbFuente.getItems().addAll(FUENTES);
        String fuenteActual = TemaManager.getFuenteActual();
        cbFuente.setValue(fuenteActual.isBlank() ? "Segoe UI" : fuenteActual);
        cbFuente.setPrefWidth(220);

        Label lblTamano = new Label("Tamaño:");
        lblTamano.getStyleClass().add("config-form-label");
        HBox tamanosBox = buildSelectorTamano();

        gridFuente.add(lblFuente, 0, 0);  gridFuente.add(cbFuente, 1, 0);
        gridFuente.add(lblTamano, 0, 1);  gridFuente.add(tamanosBox, 1, 1);

        Button btnAplicarFuente = new Button("Aplicar tipografía");
        btnAplicarFuente.getStyleClass().add("config-save-btn");
        btnAplicarFuente.setOnAction(e -> {
            if (getScene() != null) {
                TemaManager.aplicarFuente(getScene(), cbFuente.getValue(), tamanoSeleccionado);
                mostrarToast("Tipografía aplicada correctamente");
            }
        });

        seccionFuente.getChildren().addAll(tituloFuente, descFuente, gridFuente, btnAplicarFuente);

        // ── Separador visual ──────────────────────────────────────────────────
        contenido.getChildren().addAll(
            seccionTema,
            new Separator(),
            seccionFuente
        );

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private String tamanoSeleccionado = "";
    private final List<Button> botonesSize = new java.util.ArrayList<>();

    private HBox buildSelectorTamano() {
        HBox box = new HBox(8);
        String actual = TemaManager.getTamanoFuenteActual();

        for (TamanoFuente t : TAMANIOS) {
            Button btn = new Button(t.etiqueta());
            btn.getStyleClass().add("font-size-btn");
            if (t.px().equals(actual) || (actual.isBlank() && t.px().equals("13"))) {
                btn.getStyleClass().add("font-size-btn-selected");
                tamanoSeleccionado = t.px();
            }
            btn.setOnAction(e -> {
                botonesSize.forEach(b -> b.getStyleClass().remove("font-size-btn-selected"));
                btn.getStyleClass().add("font-size-btn-selected");
                tamanoSeleccionado = t.px();
            });
            botonesSize.add(btn);
            box.getChildren().add(btn);
        }
        return box;
    }

    private VBox buildTemaCard(Tema t, boolean activo) {
        VBox card = new VBox(0);
        card.getStyleClass().add("tema-card");
        if (activo) card.getStyleClass().add("tema-card-activa");
        card.setPrefWidth(148);
        card.setCursor(Cursor.HAND);

        // Preview de colores
        HBox preview = new HBox(0);
        preview.setPrefHeight(60);
        preview.setStyle("-fx-background-radius: 6 6 0 0;");

        // Franja sidebar
        VBox sidebarMini = new VBox();
        sidebarMini.setPrefWidth(44);
        sidebarMini.setStyle("-fx-background-color: " + t.sidebar() + "; -fx-background-radius: 6 0 0 0;");
        sidebarMini.setPadding(new Insets(6));
        for (int i = 0; i < 4; i++) {
            Region linea = new Region();
            linea.setPrefHeight(4);
            linea.setPrefWidth(32);
            linea.setStyle("-fx-background-color: rgba(255,255,255,0.25); -fx-background-radius: 2;");
            VBox.setMargin(linea, new Insets(0, 0, 4, 0));
            sidebarMini.getChildren().add(linea);
        }

        // Área de contenido
        VBox contentMini = new VBox(4);
        HBox.setHgrow(contentMini, Priority.ALWAYS);
        contentMini.setStyle("-fx-background-color: " + t.bg() + "; -fx-background-radius: 0 6 0 0;");
        contentMini.setPadding(new Insets(6, 6, 6, 6));

        Region barPrimary = new Region();
        barPrimary.setPrefHeight(5);
        barPrimary.setStyle("-fx-background-color: " + t.primary() + "; -fx-background-radius: 2;");

        Region barAccent = new Region();
        barAccent.setPrefHeight(3);
        barAccent.setPrefWidth(28);
        barAccent.setStyle("-fx-background-color: " + t.accent() + "; -fx-background-radius: 2;");

        Region barGray = new Region();
        barGray.setPrefHeight(3);
        barGray.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 2;");

        contentMini.getChildren().addAll(barPrimary, barAccent, barGray);
        preview.getChildren().addAll(sidebarMini, contentMini);

        // Nombre
        Label nombre = new Label(activo ? t.nombre() + "  ✓" : t.nombre());
        nombre.getStyleClass().add("tema-nombre");
        nombre.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(preview, nombre);
        card.setOnMouseClicked(e -> seleccionarTema(t));
        return card;
    }

    private void seleccionarTema(Tema t) {
        if (getScene() == null) return;
        TemaManager.aplicarTema(getScene(), t.id());
        // Refrescar la propia vista para actualizar el marcador ✓
        // (la vista ya está en la escena, el CSS cambia automáticamente,
        //  pero el texto del nombre del card no, así que la recargamos)
        getScene().getRoot().applyCss();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB 2 — MI EMPRESA
    // ═════════════════════════════════════════════════════════════════════════

    private ScrollPane buildTabEmpresa() {
        VBox contenido = new VBox(20);
        contenido.setPadding(new Insets(20));

        Label titulo = new Label("Datos de la empresa");
        titulo.getStyleClass().add("config-section-title");
        Label desc = new Label("Esta información aparece en presupuestos, facturas y nóminas.");
        desc.getStyleClass().add("config-section-desc");

        // Formulario
        Map<String, String[]> campos = new LinkedHashMap<>();
        campos.put("empresa_nombre",    new String[]{"Nombre / Razón social", "Gráficas Mulberry S.L."});
        campos.put("empresa_nif",       new String[]{"NIF / CIF",             "B12345678"});
        campos.put("empresa_direccion", new String[]{"Dirección",             "Calle Río Miño, 54 - 3F"});
        campos.put("empresa_ciudad",    new String[]{"Ciudad",                "Huercal de Almería"});
        campos.put("empresa_cp",        new String[]{"Código postal",         "04230"});
        campos.put("empresa_telefono",  new String[]{"Teléfono",              "950 30 61 64"});
        campos.put("empresa_email",     new String[]{"Email",                 "info@graficasmulberry.com"});
        campos.put("empresa_web",       new String[]{"Página web",            "www.graficasmulberry.com"});
        campos.put("empresa_iban",      new String[]{"IBAN",                  "ES00 0000 0000 0000 0000 0000"});
        campos.put("empresa_actividad", new String[]{"Actividad / CNAE",      "Serigrafía y artes gráficas"});

        Map<String, TextField> fields = new LinkedHashMap<>();
        GridPane grid = buildFormGrid(campos, fields);

        Button btnGuardar = new Button("Guardar cambios");
        btnGuardar.getStyleClass().add("config-save-btn");
        btnGuardar.setOnAction(e -> {
            fields.forEach((clave, tf) -> DatabaseManager.setConfig(clave, tf.getText().trim()));
            mostrarToast("Datos de la empresa guardados");
        });

        HBox footer = new HBox(btnGuardar);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(8, 0, 0, 0));

        VBox panel = new VBox(12, titulo, desc, new Separator(), grid, footer);
        panel.getStyleClass().add("config-panel");
        contenido.getChildren().add(panel);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private GridPane buildFormGrid(Map<String, String[]> campos, Map<String, TextField> fields) {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);

        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setMinWidth(160);
        cc0.setHalignment(javafx.geometry.HPos.RIGHT);

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setHgrow(Priority.ALWAYS);
        cc1.setFillWidth(true);

        grid.getColumnConstraints().addAll(cc0, cc1);

        int row = 0;
        for (Map.Entry<String, String[]> entry : campos.entrySet()) {
            String clave = entry.getKey();
            String label = entry.getValue()[0];
            String placeholder = entry.getValue()[1];

            Label lbl = new Label(label + ":");
            lbl.getStyleClass().add("config-form-label");

            TextField tf = new TextField(DatabaseManager.getConfig(clave));
            tf.setPromptText(placeholder);
            tf.setMaxWidth(Double.MAX_VALUE);
            fields.put(clave, tf);

            grid.add(lbl, 0, row);
            grid.add(tf,  1, row);
            row++;
        }
        return grid;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB 3 — PREFERENCIAS
    // ═════════════════════════════════════════════════════════════════════════

    private ScrollPane buildTabPreferencias() {
        VBox contenido = new VBox(20);
        contenido.setPadding(new Insets(20));

        contenido.getChildren().addAll(
            buildPanelFacturacion(),
            buildPanelRecordatorios()
        );

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private VBox buildPanelFacturacion() {
        Label titulo = new Label("Facturación y presupuestos");
        titulo.getStyleClass().add("config-section-title");
        Label desc = new Label("Valores por defecto usados al crear nuevos documentos.");
        desc.getStyleClass().add("config-section-desc");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setMinWidth(200);
        cc0.setHalignment(javafx.geometry.HPos.RIGHT);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc0, cc1);

        // IVA
        TextField tfIva = fieldFor("iva_defecto", "21");
        // Prefijos
        TextField tfPrePres = fieldFor("prefijo_presupuesto", "PRE");
        TextField tfPreFact = fieldFor("prefijo_factura",     "FAC");
        // Condiciones
        TextArea taCondiciones = new TextArea(DatabaseManager.getConfig("condiciones_defecto"));
        taCondiciones.setPromptText("Texto de condiciones que aparece en presupuestos");
        taCondiciones.setPrefRowCount(3);
        taCondiciones.setWrapText(true);

        addRow(grid, 0, "IVA por defecto (%):", tfIva);
        addRow(grid, 1, "Prefijo presupuesto:", tfPrePres);
        addRow(grid, 2, "Prefijo factura:",     tfPreFact);
        addRow(grid, 3, "Condiciones defecto:", taCondiciones);

        Button btnGuardar = new Button("Guardar preferencias de facturación");
        btnGuardar.getStyleClass().add("config-save-btn");
        btnGuardar.setOnAction(e -> {
            DatabaseManager.setConfig("iva_defecto",          tfIva.getText().trim());
            DatabaseManager.setConfig("prefijo_presupuesto",  tfPrePres.getText().trim());
            DatabaseManager.setConfig("prefijo_factura",      tfPreFact.getText().trim());
            DatabaseManager.setConfig("condiciones_defecto",  taCondiciones.getText().trim());
            mostrarToast("Preferencias de facturación guardadas");
        });

        HBox footer = new HBox(btnGuardar);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(8, 0, 0, 0));

        VBox panel = new VBox(12, titulo, desc, new Separator(), grid, footer);
        panel.getStyleClass().add("config-panel");
        return panel;
    }

    private VBox buildPanelRecordatorios() {
        Label titulo = new Label("Recordatorios del calendario");
        titulo.getStyleClass().add("config-section-title");
        Label desc = new Label("Controla con cuánta antelación se muestran avisos de eventos próximos.");
        desc.getStyleClass().add("config-section-desc");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgrow(grid, Priority.NEVER);
        grid.setHgap(14);
        grid.setVgap(12);
        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setMinWidth(200);
        cc0.setHalignment(javafx.geometry.HPos.RIGHT);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc0, cc1);

        String rawAviso = DatabaseManager.getConfig("cal_dias_aviso");
        String rawPanel = DatabaseManager.getConfig("cal_dias_panel");
        Spinner<Integer> spAviso = new Spinner<>(1, 30, parseIntSafe(rawAviso, 3));
        Spinner<Integer> spPanel = new Spinner<>(1, 60, parseIntSafe(rawPanel, 7));
        spAviso.setEditable(true);
        spPanel.setEditable(true);
        spAviso.setPrefWidth(100);
        spPanel.setPrefWidth(100);

        addRow(grid, 0, "Alerta emergente al iniciar (días):", spAviso);
        addRow(grid, 1, "Panel dashboard — mostrar eventos (días):", spPanel);

        Button btnGuardar = new Button("Guardar preferencias de recordatorios");
        btnGuardar.getStyleClass().add("config-save-btn");
        btnGuardar.setOnAction(e -> {
            DatabaseManager.setConfig("cal_dias_aviso", String.valueOf(spAviso.getValue()));
            DatabaseManager.setConfig("cal_dias_panel", String.valueOf(spPanel.getValue()));
            mostrarToast("Preferencias de recordatorios guardadas");
        });

        HBox footer = new HBox(btnGuardar);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(8, 0, 0, 0));

        VBox panel = new VBox(12, titulo, desc, new Separator(), grid, footer);
        panel.getStyleClass().add("config-panel");
        return panel;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB 4 — MÚSICA
    // ═════════════════════════════════════════════════════════════════════════

    private ScrollPane buildTabMusica() {
        VBox contenido = new VBox(20);
        contenido.setPadding(new Insets(20));

        // ── Playlist ──────────────────────────────────────────────────────────
        Label tituloPlaylist = new Label("Lista de reproducción");
        tituloPlaylist.getStyleClass().add("config-section-title");
        Label descPlaylist = new Label("Añade archivos MP3. Doble clic en una pista para reproducirla directamente.");
        descPlaylist.getStyleClass().add("config-section-desc");

        ObservableList<String> paths = FXCollections.observableArrayList(MusicService.getPlaylist());

        ListView<String> lista = new ListView<>(paths);
        lista.setPrefHeight(200);
        lista.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                setText(empty || path == null ? null : new File(path).getName());
            }
        });
        lista.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int idx = lista.getSelectionModel().getSelectedIndex();
                if (idx >= 0) MusicService.play(idx);
            }
        });

        Button btnAnadir = new Button("+ Añadir MP3");
        btnAnadir.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar archivos MP3");
            fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Audio MP3", "*.mp3", "*.MP3"));
            List<File> files = fc.showOpenMultipleDialog(
                getScene() != null ? getScene().getWindow() : null);
            if (files != null) {
                for (File f : files) {
                    String abs = f.getAbsolutePath();
                    if (!paths.contains(abs)) {
                        paths.add(abs);
                        MusicService.addTrack(abs);
                    }
                }
            }
        });

        Button btnQuitar = new Button("🗑 Quitar seleccionado");
        btnQuitar.setOnAction(e -> {
            int idx = lista.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                MusicService.removeTrack(idx);
                paths.remove(idx);
            }
        });

        HBox listButtons = new HBox(8, btnAnadir, btnQuitar);

        // ── Reproductor ───────────────────────────────────────────────────────
        Label tituloReproductor = new Label("Reproductor");
        tituloReproductor.getStyleClass().add("config-section-title");

        Label lblNowPlaying = new Label(
            MusicService.isReproduciendo() && !MusicService.getNombreActual().isEmpty()
                ? "♪ " + MusicService.getNombreActual()
                : "Sin reproducción");
        lblNowPlaying.getStyleClass().add("config-section-desc");

        Button btnAnterior  = controlBtn("⏮");
        Button btnPlayPause = controlBtn(MusicService.isReproduciendo() ? "⏸" : "⏯");
        Button btnStop      = controlBtn("⏹");
        Button btnSiguiente = controlBtn("⏭");

        MusicService.setOnTrackChange(nombre ->
            lblNowPlaying.setText("♪ " + nombre));
        MusicService.setOnEstadoChange(est -> {
            lblNowPlaying.setText(est == MusicService.Estado.PARADO
                ? "Sin reproducción" : lblNowPlaying.getText());
            btnPlayPause.setText(est == MusicService.Estado.REPRODUCIENDO ? "⏸" : "⏯");
        });

        btnAnterior.setOnAction(e  -> MusicService.anterior());
        btnPlayPause.setOnAction(e -> {
            if (MusicService.isReproduciendo()) MusicService.pause();
            else MusicService.play();
        });
        btnStop.setOnAction(e      -> MusicService.stop());
        btnSiguiente.setOnAction(e -> MusicService.siguiente());

        HBox controls = new HBox(6, btnAnterior, btnPlayPause, btnStop, btnSiguiente, lblNowPlaying);
        controls.setAlignment(Pos.CENTER_LEFT);

        // ── Opciones ──────────────────────────────────────────────────────────
        Label tituloOpciones = new Label("Opciones");
        tituloOpciones.getStyleClass().add("config-section-title");

        String volStr = DatabaseManager.getConfig("musica_volumen");
        int volInt = volStr.isBlank() ? 50 : parseIntSafe(volStr, 50);
        Slider sliderVol = new Slider(0, 100, volInt);
        sliderVol.setPrefWidth(200);
        sliderVol.setMajorTickUnit(25);
        sliderVol.setShowTickMarks(true);
        MusicService.setVolumen(volInt / 100f);

        Label lblVolVal = new Label(volInt + "%");
        lblVolVal.setStyle("-fx-text-fill: -c-text; -fx-font-weight: bold;");
        sliderVol.valueProperty().addListener((obs, ov, nv) -> {
            MusicService.setVolumen(nv.floatValue() / 100f);
            lblVolVal.setText((int) Math.round(nv.doubleValue()) + "%");
        });

        Label lblVolLabel = new Label("Volumen música:");
        lblVolLabel.setStyle("-fx-text-fill: -c-text; -fx-font-weight: bold;");

        HBox volBox = new HBox(10, lblVolLabel, sliderVol, lblVolVal);
        volBox.setAlignment(Pos.CENTER_LEFT);

        String loopStr = DatabaseManager.getConfig("musica_loop");
        CheckBox chkLoop = new CheckBox("Repetir lista en bucle");
        chkLoop.setStyle("-fx-text-fill: -c-text;");
        chkLoop.setSelected(!"0".equals(loopStr));
        chkLoop.selectedProperty().addListener((obs, ov, nv) -> MusicService.setLoop(nv));
        MusicService.setLoop(chkLoop.isSelected());

        String autoStr = DatabaseManager.getConfig("musica_autoplay");
        CheckBox chkAutoplay = new CheckBox("Reproducir automáticamente al iniciar la aplicación");
        chkAutoplay.setStyle("-fx-text-fill: -c-text;");
        chkAutoplay.setSelected("1".equals(autoStr));

        // ── Guardar ───────────────────────────────────────────────────────────
        Button btnGuardar = new Button("Guardar configuración de música");
        btnGuardar.getStyleClass().add("config-save-btn");
        btnGuardar.setOnAction(e -> {
            DatabaseManager.setConfig("musica_playlist",
                String.join("|", paths));
            DatabaseManager.setConfig("musica_volumen",
                String.valueOf((int) sliderVol.getValue()));
            DatabaseManager.setConfig("musica_loop",
                chkLoop.isSelected() ? "1" : "0");
            DatabaseManager.setConfig("musica_autoplay",
                chkAutoplay.isSelected() ? "1" : "0");
            mostrarToast("Configuración de música guardada");
        });

        HBox footer = new HBox(btnGuardar);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(8, 0, 0, 0));

        VBox panel = new VBox(12,
            tituloPlaylist, descPlaylist,
            lista, listButtons,
            new Separator(),
            tituloReproductor, controls,
            new Separator(),
            tituloOpciones, volBox, chkLoop, chkAutoplay,
            new Separator(),
            footer
        );
        panel.getStyleClass().add("config-panel");
        contenido.getChildren().add(panel);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private Button controlBtn(String texto) {
        Button b = new Button(texto);
        b.setStyle("-fx-font-size:16; -fx-padding:4 10;");
        return b;
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TextField fieldFor(String clave, String placeholder) {
        TextField tf = new TextField(DatabaseManager.getConfig(clave));
        tf.setPromptText(placeholder);
        tf.setMaxWidth(240);
        return tf;
    }

    private void addRow(GridPane grid, int row, String label, javafx.scene.Node field) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("config-form-label");
        grid.add(lbl, 0, row);
        grid.add(field, 1, row);
    }

    private void mostrarToast(String mensaje) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Configuración");
        info.setHeaderText(null);
        info.setContentText("✅  " + mensaje);
        if (getScene() != null) {
            info.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        }
        info.show();
        javafx.animation.PauseTransition pausa = new javafx.animation.PauseTransition(
            javafx.util.Duration.seconds(2));
        pausa.setOnFinished(ev -> info.close());
        pausa.play();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB 5 — MODELOS IA
    // ═════════════════════════════════════════════════════════════════════════

    private static final String[][] MODELOS_INFO = {
        {"llama4",      "~5.6 GB",  "Meta Llama 4 Scout — Multimodal, el más reciente"},
        {"llama3.2",    "~2.0 GB",  "Meta Llama 3.2 — Rápido y equilibrado (recomendado)"},
        {"llama3.1",    "~4.7 GB",  "Meta Llama 3.1 — Más preciso que 3.2"},
        {"mistral",     "~4.1 GB",  "Mistral 7B — Muy eficiente en español"},
        {"phi4",        "~9.1 GB",  "Microsoft Phi-4 — Excelente razonamiento"},
        {"phi3.5",      "~2.2 GB",  "Microsoft Phi-3.5 — Ligero y rápido"},
        {"qwen2.5",     "~4.7 GB",  "Qwen 2.5 — Multilingüe, muy bueno en español"},
        {"gemma3",      "~3.3 GB",  "Google Gemma 3 — Eficiente"},
        {"deepseek-r1", "~4.7 GB",  "DeepSeek R1 — Razonamiento avanzado"},
    };

    private ScrollPane buildTabModelosIA() {
        VBox contenido = new VBox(20);
        contenido.setPadding(new Insets(20));

        Label titulo = new Label("Modelos de Inteligencia Artificial");
        titulo.getStyleClass().add("config-section-title");
        Label desc = new Label(
            "Gestiona los modelos de IA disponibles en Ollama. " +
            "Puedes descargar nuevos modelos o eliminar los que ya no necesites para liberar espacio en disco.");
        desc.getStyleClass().add("config-section-desc");
        desc.setWrapText(true);

        // Tabla de modelos disponibles con tamaños
        Label lblTabla = new Label("Modelos disponibles:");
        lblTabla.getStyleClass().add("config-section-title");

        GridPane gridModelos = new GridPane();
        gridModelos.setHgap(20);
        gridModelos.setVgap(8);
        gridModelos.setPadding(new Insets(10));
        gridModelos.setStyle(
            "-fx-background-color:#F8F8F8; -fx-border-color:#DDD; " +
            "-fx-border-radius:6; -fx-background-radius:6;");

        ColumnConstraints cMod = new ColumnConstraints(); cMod.setMinWidth(120);
        ColumnConstraints cTam = new ColumnConstraints(); cTam.setMinWidth(80);
        ColumnConstraints cDes = new ColumnConstraints(); cDes.setHgrow(Priority.ALWAYS);
        gridModelos.getColumnConstraints().addAll(cMod, cTam, cDes);

        Label hNombre = new Label("Modelo"); hNombre.setStyle("-fx-font-weight:bold;");
        Label hTam    = new Label("Tamaño"); hTam.setStyle("-fx-font-weight:bold;");
        Label hDesc   = new Label("Descripción"); hDesc.setStyle("-fx-font-weight:bold;");
        gridModelos.addRow(0, hNombre, hTam, hDesc);

        for (int i = 0; i < MODELOS_INFO.length; i++) {
            Label lNom = new Label(MODELOS_INFO[i][0]);
            lNom.setStyle("-fx-font-weight:bold; -fx-text-fill:#6B2D5E;");
            Label lTam = new Label(MODELOS_INFO[i][1]);
            lTam.setStyle("-fx-text-fill:#F39C12; -fx-font-weight:bold;");
            Label lDes = new Label(MODELOS_INFO[i][2]);
            gridModelos.addRow(i + 1, lNom, lTam, lDes);
        }

        Button btnGestionar = new Button("⬇  Gestionar y descargar modelos");
        btnGestionar.getStyleClass().add("config-save-btn");
        btnGestionar.setOnAction(e -> {
            if (getScene() == null) return;
            Stage owner = (Stage) getScene().getWindow();
            new ModelosGestionDialog(owner, new OllamaService()).showAndWait();
        });

        VBox panel = new VBox(12, titulo, desc, new Separator(), lblTabla, gridModelos, new Separator(), btnGestionar);
        panel.getStyleClass().add("config-panel");
        contenido.getChildren().add(panel);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB 6 — ACERCA DE
    // ═════════════════════════════════════════════════════════════════════════

    private ScrollPane buildTabAcercaDe() {
        VBox contenido = new VBox(20);
        contenido.setPadding(new Insets(20));

        // Tarjeta principal de la app
        VBox cardApp = new VBox(10);
        cardApp.setAlignment(Pos.CENTER);
        cardApp.setPadding(new Insets(30, 20, 30, 20));
        cardApp.setStyle("-fx-background-color:-c-primary-dark; -fx-background-radius:12;");

        Label lblNombreApp = new Label("Gráficas Mulberry");
        lblNombreApp.setStyle("-fx-font-size:26px; -fx-font-weight:bold; -fx-text-fill:white;");

        Label lblSubtitulo = new Label("Sistema de Gestión para Artes Gráficas y Serigrafía");
        lblSubtitulo.setStyle("-fx-font-size:13px; -fx-text-fill:rgba(255,255,255,0.85);");
        lblSubtitulo.setWrapText(true);

        Label lblVersion = new Label("Versión 4.2");
        lblVersion.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:rgba(255,255,255,0.95);");

        cardApp.getChildren().addAll(lblNombreApp, lblSubtitulo, lblVersion);

        // Información detallada
        GridPane gridInfo = new GridPane();
        gridInfo.setHgap(20);
        gridInfo.setVgap(14);
        gridInfo.setPadding(new Insets(16));

        ColumnConstraints ccL = new ColumnConstraints(); ccL.setMinWidth(150);
        ccL.setHalignment(javafx.geometry.HPos.RIGHT);
        ColumnConstraints ccR = new ColumnConstraints(); ccR.setHgrow(Priority.ALWAYS);
        gridInfo.getColumnConstraints().addAll(ccL, ccR);

        addInfoRow(gridInfo, 0, "Creador:",        "Gipsybuho");
        addInfoRow(gridInfo, 1, "Año:",            "2026");
        addInfoRow(gridInfo, 2, "Tecnología:",     "Java 21 + JavaFX 21");
        addInfoRow(gridInfo, 3, "Base de datos:",  "SQLite (datos 100% locales)");
        addInfoRow(gridInfo, 4, "IA integrada:",   "Ollama (sin conexión a Internet)");

        // Descripción
        Label lblDescTitulo = new Label("Acerca de la aplicación");
        lblDescTitulo.getStyleClass().add("config-section-title");

        Label lblDesc = new Label(
            "Gráficas Mulberry es un sistema de gestión integral diseñado para empresas de artes " +
            "gráficas y serigrafía. Permite gestionar presupuestos, facturas, albaranes, materiales " +
            "en stock, empleados, nóminas y estadísticas, con un asistente de inteligencia artificial " +
            "totalmente local que protege la privacidad de tus datos.");
        lblDesc.setWrapText(true);
        lblDesc.getStyleClass().add("config-section-desc");

        // Tarjeta de información del sistema
        Label lblSistemaTitulo = new Label("Información del sistema");
        lblSistemaTitulo.getStyleClass().add("config-section-title");

        GridPane gridSistema = new GridPane();
        gridSistema.setHgap(20);
        gridSistema.setVgap(12);
        gridSistema.setPadding(new Insets(8, 16, 8, 16));
        ColumnConstraints csL = new ColumnConstraints(); csL.setMinWidth(160);
        csL.setHalignment(javafx.geometry.HPos.RIGHT);
        ColumnConstraints csR = new ColumnConstraints(); csR.setHgrow(Priority.ALWAYS);
        gridSistema.getColumnConstraints().addAll(csL, csR);

        int sysRow = 0;

        // --- Equipo ---
        String computerName = System.getenv("COMPUTERNAME");
        if (computerName != null && !computerName.isEmpty())
            addInfoRow(gridSistema, sysRow++, "Nombre del equipo:", computerName);

        // Sistema operativo — WMI para edición completa y número de build
        String osArch = System.getProperty("os.arch", "");
        Map<String, String> osWmi = wmicQuery("os", "get", "Caption,BuildNumber,OSArchitecture", "/format:value");
        String osDisplay = osWmi.getOrDefault("Caption",
            System.getProperty("os.name", "Desconocido") + " " + System.getProperty("os.version", "")).trim();
        String osBuild = osWmi.get("BuildNumber");
        if (osBuild != null && !osBuild.isEmpty()) osDisplay += "  (Build " + osBuild + ")";
        addInfoRow(gridSistema, sysRow++, "Sistema operativo:", osDisplay);
        addInfoRow(gridSistema, sysRow++, "Arquitectura:", osWmi.getOrDefault("OSArchitecture", osArch));

        // --- Procesador ---
        Map<String, String> cpuWmi = wmicQuery("cpu", "get",
            "Name,NumberOfCores,NumberOfLogicalProcessors,MaxClockSpeed", "/format:value");
        String cpuNombre = cpuWmi.getOrDefault("Name",
            System.getenv("PROCESSOR_IDENTIFIER") != null ? System.getenv("PROCESSOR_IDENTIFIER") : osArch);
        addInfoRow(gridSistema, sysRow++, "Procesador:", cpuNombre.trim());
        String nCores   = cpuWmi.getOrDefault("NumberOfCores", "?");
        String nThreads = cpuWmi.getOrDefault("NumberOfLogicalProcessors",
            String.valueOf(Runtime.getRuntime().availableProcessors()));
        addInfoRow(gridSistema, sysRow++, "Núcleos:", nCores + " físicos  ·  " + nThreads + " lógicos");
        String mhzStr = cpuWmi.get("MaxClockSpeed");
        if (mhzStr != null && !mhzStr.isEmpty()) {
            try {
                double ghz = Integer.parseInt(mhzStr.trim()) / 1000.0;
                addInfoRow(gridSistema, sysRow++, "Frecuencia máx.:", String.format("%.2f GHz", ghz));
            } catch (NumberFormatException ignored) {}
        }

        // --- Memoria RAM ---
        try {
            com.sun.management.OperatingSystemMXBean osMx =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long ramTotal = osMx.getTotalMemorySize();
            long ramLibre = osMx.getFreeMemorySize();
            long ramUsada = ramTotal - ramLibre;
            double pctUsada = (double) ramUsada / ramTotal * 100;
            addInfoRow(gridSistema, sysRow++, "RAM total:", formatBytes(ramTotal));
            addInfoRow(gridSistema, sysRow++, "RAM usada / libre:",
                formatBytes(ramUsada) + "  /  " + formatBytes(ramLibre)
                + "  (" + String.format("%.1f%%", pctUsada) + " en uso)");
        } catch (Exception ignored) {}

        // --- Almacenamiento ---
        try {
            for (FileStore store : FileSystems.getDefault().getFileStores()) {
                long total  = store.getTotalSpace();
                long libre  = store.getUsableSpace();
                long usado  = total - libre;
                if (total == 0) continue;
                double pctUsado = (double) usado / total * 100;
                String tipo = store.type() != null && !store.type().isEmpty()
                    ? "  [" + store.type() + "]" : "";
                addInfoRow(gridSistema, sysRow++,
                    "Disco " + store.name() + tipo + ":",
                    formatBytes(total) + " total  —  "
                    + formatBytes(libre) + " libres  ("
                    + String.format("%.1f%%", pctUsado) + " usado)");
            }
        } catch (Exception ignored) {}

        // --- Tarjeta gráfica ---
        Map<String, String> gpuWmi = wmicQuery("path", "win32_videocontroller", "get",
            "Caption,AdapterRAM", "/format:value");
        String gpuCaption = gpuWmi.get("Caption");
        if (gpuCaption != null && !gpuCaption.isBlank()) {
            addInfoRow(gridSistema, sysRow++, "Tarjeta gráfica:", gpuCaption.trim());
            String vramStr = gpuWmi.get("AdapterRAM");
            if (vramStr != null && !vramStr.isBlank()) {
                try {
                    long vram = Long.parseLong(vramStr.trim());
                    if (vram > 0) addInfoRow(gridSistema, sysRow++, "Memoria GPU:", formatBytes(vram));
                } catch (NumberFormatException ignored) {}
            }
        }

        // --- Pantalla ---
        try {
            Rectangle2D bounds = Screen.getPrimary().getBounds();
            addInfoRow(gridSistema, sysRow++, "Resolución pantalla:",
                (int) bounds.getWidth() + " × " + (int) bounds.getHeight() + " px"
                + "  ·  " + String.format("%.0f", Screen.getPrimary().getDpi()) + " DPI");
        } catch (Exception ignored) {}

        // --- Java ---
        String javaVersion = System.getProperty("java.version", "?");
        String javaVendor  = System.getProperty("java.vendor",  "");
        addInfoRow(gridSistema, sysRow++, "Java (JVM):", javaVersion + "  —  " + javaVendor);

        VBox panel = new VBox(16,
            cardApp,
            new Separator(),
            gridInfo,
            new Separator(),
            lblDescTitulo, lblDesc,
            new Separator(),
            lblSistemaTitulo, gridSistema
        );
        panel.getStyleClass().add("config-panel");
        contenido.getChildren().add(panel);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private Map<String, String> wmicQuery(String... args) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            String[] cmd = new String[args.length + 1];
            cmd[0] = "wmic";
            System.arraycopy(args, 0, cmd, 1, args.length);
            Process p = new ProcessBuilder(cmd).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String key = line.substring(0, eq).trim();
                        String val = line.substring(eq + 1).trim();
                        if (!val.isEmpty() && !result.containsKey(key)) result.put(key, val);
                    }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024L)                       return bytes + " B";
        if (bytes < 1024L * 1024)                return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024)         return String.format("%.1f MB", bytes / (1024.0 * 1024));
        if (bytes < 1024L * 1024 * 1024 * 1024)  return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        return String.format("%.2f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
    }

    private void addInfoRow(GridPane grid, int row, String label, String valor) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("config-form-label");
        Label val = new Label(valor);
        val.setStyle("-fx-font-weight:bold; -fx-text-fill:-c-text;");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }
}
