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
import org.gipsybuho.service.SoundService;
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
    // ── Datos de cada tema (Sincronizados con los archivos CSS) ────────────────
    private record Tema(String id, String nombre,
                        String primary, String sidebar, String bg, String accent) {}

    private static final List<Tema> TEMAS = List.of(
            new Tema("mulberry", "Mulberry",    "#6B2D5E", "#2D1A28", "#F5F0F4", "#E891D0"),
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

    private final VisualAssistantView assistant;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ConfiguracionView(VisualAssistantView assistant) {
        this.assistant = assistant;
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
        Tab tabSonido       = new Tab("🔊  Sonido",       buildTabSonido());
        Tab tabAsistente    = new Tab("🧭  Asistente",    buildTabAsistente());
        Tab tabDocumentos   = new Tab("📄  Documentos",   buildTabDocumentos());
        Tab tabAcercaDe     = new Tab("ℹ  Acerca de",     buildTabAcercaDe());
        Tab tabDiagnostico  = new Tab("🖥  Diagnóstico",  buildTabDiagnostico());

        tabs.getTabs().addAll(tabApariencia, tabEmpresa, tabPreferencias,
            tabSonido, tabAsistente, tabDocumentos, tabAcercaDe, tabDiagnostico);
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
        seccionTema.getStyleClass().add("config-panel");
        seccionTema.setPadding(new Insets(15));

        // ── Sección: modo oscuro ─────────────────────────────────────────────
        VBox seccionModo = new VBox(10);
        Label tituloModo = new Label("Modo oscuro");
        tituloModo.getStyleClass().add("config-section-title");
        CheckBox chkModoOscuro = new CheckBox("Ejecutar la aplicación en modo oscuro");
        chkModoOscuro.setSelected(TemaManager.isModoOscuroActivo());
        chkModoOscuro.setOnAction(e -> {
            if (getScene() != null) {
                TemaManager.aplicarModoOscuro(getScene(), chkModoOscuro.isSelected());
                mostrarToast(chkModoOscuro.isSelected() ? "Modo oscuro activado" : "Modo oscuro desactivado");
            }
        });
        seccionModo.getChildren().addAll(tituloModo, chkModoOscuro);
        seccionModo.getStyleClass().add("config-panel");
        seccionModo.setPadding(new Insets(15));

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
        seccionFuente.getStyleClass().add("config-panel");
        seccionFuente.setPadding(new Insets(15));
        // ── Separador visual ──────────────────────────────────────────────────
        contenido.getChildren().addAll(seccionTema, seccionModo, seccionFuente);

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
        SoundService.play(SoundService.Sound.CLICK);
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
        TextField tfPreAlb  = fieldFor("prefijo_albaran",     "ALB");
        TextField tfPrePed  = fieldFor("prefijo_pedido",      "PED");
        // Condiciones
        TextArea taCondiciones = new TextArea(DatabaseManager.getConfig("condiciones_defecto"));
        taCondiciones.setPromptText("Texto de condiciones que aparece en presupuestos");
        taCondiciones.setPrefRowCount(3);
        taCondiciones.setWrapText(true);
        // Vencimiento y forma de pago
        Spinner<Integer> spVencimiento = new Spinner<>(0, 365,
            parseIntSafe(DatabaseManager.getConfig("factura_vencimiento_dias"), 30));
        spVencimiento.setEditable(true);
        spVencimiento.setPrefWidth(100);
        ComboBox<String> cbFormaPago = new ComboBox<>();
        cbFormaPago.getItems().addAll("Transferencia", "Efectivo", "Cheque", "Domiciliación", "Tarjeta");
        String formaPagoActual = DatabaseManager.getConfig("factura_forma_pago");
        cbFormaPago.setValue(formaPagoActual.isBlank() ? "Transferencia" : formaPagoActual);

        addRow(grid, 0, "IVA por defecto (%):",        tfIva);
        addRow(grid, 1, "Prefijo presupuesto:",         tfPrePres);
        addRow(grid, 2, "Prefijo factura:",             tfPreFact);
        addRow(grid, 3, "Prefijo albarán:",             tfPreAlb);
        addRow(grid, 4, "Prefijo pedido:",              tfPrePed);
        addRow(grid, 5, "Condiciones defecto:",         taCondiciones);
        addRow(grid, 6, "Vencimiento factura (días):",  spVencimiento);
        addRow(grid, 7, "Forma de pago defecto:",       cbFormaPago);

        Button btnGuardar = new Button("Guardar preferencias de facturación");
        btnGuardar.getStyleClass().add("config-save-btn");
        btnGuardar.setOnAction(e -> {
            DatabaseManager.setConfig("iva_defecto",               tfIva.getText().trim());
            DatabaseManager.setConfig("prefijo_presupuesto",       tfPrePres.getText().trim());
            DatabaseManager.setConfig("prefijo_factura",           tfPreFact.getText().trim());
            DatabaseManager.setConfig("prefijo_albaran",           tfPreAlb.getText().trim());
            DatabaseManager.setConfig("prefijo_pedido",            tfPrePed.getText().trim());
            DatabaseManager.setConfig("condiciones_defecto",       taCondiciones.getText().trim());
            DatabaseManager.setConfig("factura_vencimiento_dias",  String.valueOf(spVencimiento.getValue()));
            DatabaseManager.setConfig("factura_forma_pago",        cbFormaPago.getValue());
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

    private ScrollPane buildTabSonido() {
        VBox contenido = new VBox(20);
        contenido.setPadding(new Insets(20));

        // ── Efectos de interfaz UI ────────────────────────────────────────────
        Label tituloEfectos = new Label("Efectos de interfaz");
        tituloEfectos.getStyleClass().add("config-section-title");
        Label descEfectos = new Label("Sonidos de navegación, clicks y notificaciones del sistema.");
        descEfectos.getStyleClass().add("config-section-desc");

        CheckBox chkSonidosActivos = new CheckBox("Activar efectos de sonido");
        chkSonidosActivos.setStyle("-fx-text-fill: -c-text;");
        chkSonidosActivos.setSelected(!SoundService.isMuted());

        int uiVolInt = parseIntSafe(DatabaseManager.getConfig("audio_volumen"), 65);
        Slider sliderUiVol = new Slider(0, 100, uiVolInt);
        sliderUiVol.setPrefWidth(200);
        sliderUiVol.setMajorTickUnit(25);
        sliderUiVol.setShowTickMarks(true);
        Label lblUiVolVal = new Label(uiVolInt + "%");
        lblUiVolVal.setStyle("-fx-text-fill: -c-text; -fx-font-weight: bold;");
        Label lblUiVolLabel = new Label("Volumen efectos:");
        lblUiVolLabel.setStyle("-fx-text-fill: -c-text; -fx-font-weight: bold;");
        HBox uiVolBox = new HBox(10, lblUiVolLabel, sliderUiVol, lblUiVolVal);
        uiVolBox.setAlignment(Pos.CENTER_LEFT);

        Button btnProbar = new Button("▶  Probar sonido");
        btnProbar.setOnAction(e -> SoundService.play(SoundService.Sound.SUCCESS));

        chkSonidosActivos.selectedProperty().addListener((obs, ov, activo) -> {
            SoundService.setMuted(!activo);
            DatabaseManager.setConfig("audio_muted", activo ? "0" : "1");
        });
        sliderUiVol.valueProperty().addListener((obs, ov, nv) -> {
            SoundService.setVolume(nv.floatValue() / 100f);
            int val = (int) Math.round(nv.doubleValue());
            lblUiVolVal.setText(val + "%");
            if (!sliderUiVol.isValueChanging()) {
                DatabaseManager.setConfig("audio_volumen", String.valueOf(val));
            }
        });
        sliderUiVol.valueChangingProperty().addListener((obs, wasChanging, changing) -> {
            if (!changing) {
                DatabaseManager.setConfig("audio_volumen",
                    String.valueOf((int) Math.round(sliderUiVol.getValue())));
            }
        });

        VBox panelEfectos = new VBox(12, tituloEfectos, descEfectos,
            chkSonidosActivos, uiVolBox, btnProbar);
        panelEfectos.getStyleClass().add("config-panel");
        panelEfectos.setPadding(new Insets(15));
        contenido.getChildren().add(panelEfectos);

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
            int valor = (int) Math.round(nv.doubleValue());
            lblVolVal.setText(valor + "%");
            if (!sliderVol.isValueChanging()) {
                DatabaseManager.setConfig("musica_volumen", String.valueOf(valor));
            }
        });
        sliderVol.valueChangingProperty().addListener((obs, wasChanging, changing) -> {
            if (!changing) {
                DatabaseManager.setConfig("musica_volumen",
                    String.valueOf((int) Math.round(sliderVol.getValue())));
            }
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
                String.valueOf((int) Math.round(sliderVol.getValue())));
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
        SoundService.play(SoundService.Sound.SUCCESS);
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
    // TAB 5 — ACERCA DE
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

        Label lblVersion = new Label("Versión 13.5.0");
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

        VBox panel = new VBox(16,
            cardApp,
            new Separator(),
            gridInfo,
            new Separator(),
            lblDescTitulo, lblDesc
        );
        panel.getStyleClass().add("config-panel");
        contenido.getChildren().add(panel);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB 5 — ASISTENTE VISUAL
    // ═════════════════════════════════════════════════════════════════════════

    private ScrollPane buildTabAsistente() {
        ScrollPane scroll = new ScrollPane(new VisualAssistantConfigView(assistant));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB — DOCUMENTOS
    // ═════════════════════════════════════════════════════════════════════════

    private ScrollPane buildTabDocumentos() {
        VBox contenido = new VBox(20);
        contenido.setPadding(new Insets(20));

        // ── Logo de la empresa ────────────────────────────────────────────────
        Label tituloLogo = new Label("Logo de la empresa");
        tituloLogo.getStyleClass().add("config-section-title");
        Label descLogo = new Label("El logo aparecerá en las cabeceras de presupuestos, facturas y albaranes.");
        descLogo.getStyleClass().add("config-section-desc");

        TextField tfLogoPath = new TextField(DatabaseManager.getConfig("doc_logo_path"));
        tfLogoPath.setPromptText("Ruta del archivo de imagen (PNG, JPG)");
        tfLogoPath.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tfLogoPath, Priority.ALWAYS);

        Button btnSeleccionarLogo = new Button("📂 Seleccionar...");
        btnSeleccionarLogo.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar logo");
            fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.PNG", "*.jpg", "*.JPG", "*.jpeg", "*.JPEG"));
            File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) tfLogoPath.setText(f.getAbsolutePath());
        });

        HBox logoBox = new HBox(8, tfLogoPath, btnSeleccionarLogo);
        logoBox.setAlignment(Pos.CENTER_LEFT);

        Button btnGuardarLogo = new Button("Guardar logo");
        btnGuardarLogo.getStyleClass().add("config-save-btn");
        btnGuardarLogo.setOnAction(e -> {
            DatabaseManager.setConfig("doc_logo_path", tfLogoPath.getText().trim());
            mostrarToast("Logo guardado");
        });

        HBox footerLogo = new HBox(btnGuardarLogo);
        footerLogo.setAlignment(Pos.CENTER_RIGHT);

        VBox panelLogo = new VBox(12, tituloLogo, descLogo, logoBox, footerLogo);
        panelLogo.getStyleClass().add("config-panel");

        // ── Pie legal ─────────────────────────────────────────────────────────
        Label tituloPie = new Label("Pie de página legal");
        tituloPie.getStyleClass().add("config-section-title");
        Label descPie = new Label("Texto que aparece al pie de todos los documentos generados.");
        descPie.getStyleClass().add("config-section-desc");

        TextArea taPieLegal = new TextArea(DatabaseManager.getConfig("doc_pie_legal"));
        taPieLegal.setPromptText("Ej: Inscrita en el Registro Mercantil de Almería...");
        taPieLegal.setPrefRowCount(3);
        taPieLegal.setWrapText(true);

        Button btnGuardarPie = new Button("Guardar pie legal");
        btnGuardarPie.getStyleClass().add("config-save-btn");
        btnGuardarPie.setOnAction(e -> {
            DatabaseManager.setConfig("doc_pie_legal", taPieLegal.getText().trim());
            mostrarToast("Pie legal guardado");
        });

        HBox footerPie = new HBox(btnGuardarPie);
        footerPie.setAlignment(Pos.CENTER_RIGHT);

        VBox panelPie = new VBox(12, tituloPie, descPie, taPieLegal, footerPie);
        panelPie.getStyleClass().add("config-panel");

        // ── Textos por defecto ────────────────────────────────────────────────
        Label tituloTextos = new Label("Textos por defecto en documentos");
        tituloTextos.getStyleClass().add("config-section-title");
        Label descTextos = new Label("Texto introductorio que se inserta automáticamente al crear cada tipo de documento.");
        descTextos.getStyleClass().add("config-section-desc");
        descTextos.setWrapText(true);

        TextArea taTextoPresupuesto = new TextArea(DatabaseManager.getConfig("doc_texto_presupuesto"));
        taTextoPresupuesto.setPromptText("Texto por defecto en presupuestos");
        taTextoPresupuesto.setPrefRowCount(3);
        taTextoPresupuesto.setWrapText(true);

        TextArea taTextoFactura = new TextArea(DatabaseManager.getConfig("doc_texto_factura"));
        taTextoFactura.setPromptText("Texto por defecto en facturas");
        taTextoFactura.setPrefRowCount(3);
        taTextoFactura.setWrapText(true);

        TextArea taTextoAlbaran = new TextArea(DatabaseManager.getConfig("doc_texto_albaran"));
        taTextoAlbaran.setPromptText("Texto por defecto en albaranes");
        taTextoAlbaran.setPrefRowCount(3);
        taTextoAlbaran.setWrapText(true);

        GridPane gridTextos = new GridPane();
        gridTextos.setHgap(14);
        gridTextos.setVgap(12);
        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setMinWidth(140);
        cc0.setHalignment(javafx.geometry.HPos.RIGHT);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setHgrow(Priority.ALWAYS);
        cc1.setFillWidth(true);
        gridTextos.getColumnConstraints().addAll(cc0, cc1);
        Label lblPres = new Label("Presupuesto:");
        lblPres.getStyleClass().add("config-form-label");
        Label lblFact = new Label("Factura:");
        lblFact.getStyleClass().add("config-form-label");
        Label lblAlb = new Label("Albarán:");
        lblAlb.getStyleClass().add("config-form-label");
        gridTextos.add(lblPres, 0, 0); gridTextos.add(taTextoPresupuesto, 1, 0);
        gridTextos.add(lblFact, 0, 1); gridTextos.add(taTextoFactura, 1, 1);
        gridTextos.add(lblAlb,  0, 2); gridTextos.add(taTextoAlbaran,  1, 2);

        Button btnGuardarTextos = new Button("Guardar textos");
        btnGuardarTextos.getStyleClass().add("config-save-btn");
        btnGuardarTextos.setOnAction(e -> {
            DatabaseManager.setConfig("doc_texto_presupuesto", taTextoPresupuesto.getText().trim());
            DatabaseManager.setConfig("doc_texto_factura",    taTextoFactura.getText().trim());
            DatabaseManager.setConfig("doc_texto_albaran",    taTextoAlbaran.getText().trim());
            mostrarToast("Textos de documentos guardados");
        });

        HBox footerTextos = new HBox(btnGuardarTextos);
        footerTextos.setAlignment(Pos.CENTER_RIGHT);

        VBox panelTextos = new VBox(12, tituloTextos, descTextos, gridTextos, footerTextos);
        panelTextos.getStyleClass().add("config-panel");

        contenido.getChildren().addAll(panelLogo, panelPie, panelTextos);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB — DIAGNÓSTICO DEL SISTEMA
    // ═════════════════════════════════════════════════════════════════════════

    private ScrollPane buildTabDiagnostico() {
        VBox contenido = new VBox(20);
        contenido.setPadding(new Insets(20));

        Label lblTitulo = new Label("Información del sistema");
        lblTitulo.getStyleClass().add("config-section-title");
        Label lblDesc = new Label("Detalles de hardware y software. Útil para soporte técnico.");
        lblDesc.getStyleClass().add("config-section-desc");

        GridPane gridSistema = new GridPane();
        gridSistema.setHgap(20);
        gridSistema.setVgap(12);
        gridSistema.setPadding(new Insets(8, 16, 8, 16));
        ColumnConstraints csL = new ColumnConstraints();
        csL.setMinWidth(160);
        csL.setHalignment(javafx.geometry.HPos.RIGHT);
        ColumnConstraints csR = new ColumnConstraints();
        csR.setHgrow(Priority.ALWAYS);
        gridSistema.getColumnConstraints().addAll(csL, csR);

        int sysRow = 0;

        String computerName = System.getenv("COMPUTERNAME");
        if (computerName != null && !computerName.isEmpty())
            addInfoRow(gridSistema, sysRow++, "Nombre del equipo:", computerName);

        String osArch = System.getProperty("os.arch", "");
        Map<String, String> osWmi = wmicQuery("os", "get", "Caption,BuildNumber,OSArchitecture", "/format:value");
        String osDisplay = osWmi.getOrDefault("Caption",
            System.getProperty("os.name", "Desconocido") + " " + System.getProperty("os.version", "")).trim();
        String osBuild = osWmi.get("BuildNumber");
        if (osBuild != null && !osBuild.isEmpty()) osDisplay += "  (Build " + osBuild + ")";
        addInfoRow(gridSistema, sysRow++, "Sistema operativo:", osDisplay);
        addInfoRow(gridSistema, sysRow++, "Arquitectura:", osWmi.getOrDefault("OSArchitecture", osArch));

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

        try {
            Rectangle2D bounds = Screen.getPrimary().getBounds();
            addInfoRow(gridSistema, sysRow++, "Resolución pantalla:",
                (int) bounds.getWidth() + " × " + (int) bounds.getHeight() + " px"
                + "  ·  " + String.format("%.0f", Screen.getPrimary().getDpi()) + " DPI");
        } catch (Exception ignored) {}

        String javaVersion = System.getProperty("java.version", "?");
        String javaVendor  = System.getProperty("java.vendor",  "");
        addInfoRow(gridSistema, sysRow++, "Java (JVM):", javaVersion + "  —  " + javaVendor);

        VBox panel = new VBox(16, lblTitulo, lblDesc, gridSistema);
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
