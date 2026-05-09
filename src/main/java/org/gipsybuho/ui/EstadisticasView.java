package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.gipsybuho.service.EstadisticasService;
import org.gipsybuho.service.PDFService;
import org.gipsybuho.service.PdfPreviewService;
import org.gipsybuho.service.SoundService;

import java.awt.Desktop;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

public class EstadisticasView extends VBox {

    // Paleta de colores coherente con el tema de la aplicación
    private static final String[] PALETA = {
        "#6B2D5E","#4C9BE8","#27AE60","#F39C12","#E74C3C",
        "#9B59B6","#1ABC9C","#E67E22","#2ECC71","#3498DB"
    };

    private static final String COLOR_INGRESOS = "#27AE60";
    private static final String COLOR_GASTOS   = "#E74C3C";
    private static final String COLOR_NOMINAS  = "#F39C12";
    private static final String COLOR_PRIMARIO = "#6B2D5E";
    private static final String COLOR_AZUL     = "#4C9BE8";

    private int anio = LocalDate.now().getYear();
    private ComboBox<Integer> cbAnio;
    private Button btnExportarPDF;

    private final StackPane areaFinanciero  = new StackPane();
    private final StackPane areaMateriales  = new StackPane();
    private final StackPane areaClientes    = new StackPane();
    private final StackPane areaProduccion  = new StackPane();

    // Seguimiento de qué tabs han sido cargados
    private final Set<StackPane> cargados = new HashSet<>();

    public EstadisticasView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Estadísticas y Análisis");
        titulo.getStyleClass().add("view-title");

        Label sub = new Label("Análisis visual de ingresos, gastos, materiales, clientes y producción");
        sub.getStyleClass().add("view-subtitle");

        TabPane tabs = buildTabs();
        VBox.setVgrow(tabs, Priority.ALWAYS);

        getChildren().addAll(titulo, sub, buildHeaderBar(), tabs);

        // Carga inicial del primer tab
        cargar(areaFinanciero, this::contenidoFinanciero);
    }

    // ── Header con selector de año ────────────────────────────────────────────

    private HBox buildHeaderBar() {
        cbAnio = new ComboBox<>();
        cbAnio.getItems().addAll(EstadisticasService.aniosDisponibles());
        cbAnio.setValue(anio);
        cbAnio.setStyle("-fx-font-weight:bold;");
        cbAnio.setOnAction(e -> {
            if (cbAnio.getValue() != null) {
                anio = cbAnio.getValue();
                refrescar();
            }
        });

        btnExportarPDF = new Button("📄  Exportar PDF");
        btnExportarPDF.setStyle("-fx-padding:5 14; -fx-background-radius:4;");
        btnExportarPDF.setOnAction(e -> exportarPDF());

        Button btnPreview = new Button("👁  Previsualizar");
        btnPreview.setStyle("-fx-padding:5 14; -fx-background-radius:4; -fx-background-color:#6B2D5E; -fx-text-fill:white; -fx-font-weight:bold;");
        btnPreview.setOnAction(e -> previsualizar());

        HBox bar = new HBox(10, new Label("Año:"), cbAnio, btnExportarPDF, btnPreview);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 10, 6, 10));
        bar.setStyle("-fx-background-color:-c-tab-bg; -fx-background-radius:6;");
        return bar;
    }

    private void refrescar() {
        // Limpiar y recargar solo los tabs ya visitados
        for (StackPane area : new StackPane[]{areaFinanciero, areaMateriales, areaClientes, areaProduccion}) {
            if (cargados.contains(area)) {
                cargados.remove(area);
                area.getChildren().clear();
            }
        }
        if (cargados.contains(areaFinanciero) || !areaFinanciero.getChildren().isEmpty()) {
            cargar(areaFinanciero, this::contenidoFinanciero);
        } else {
            cargar(areaFinanciero, this::contenidoFinanciero);
        }
        if (!areaMateriales.getChildren().isEmpty())  cargar(areaMateriales, this::contenidoMateriales);
        if (!areaClientes.getChildren().isEmpty())    cargar(areaClientes,   this::contenidoClientes);
        if (!areaProduccion.getChildren().isEmpty())  cargar(areaProduccion, this::contenidoProduccion);
    }

    // ── TabPane ───────────────────────────────────────────────────────────────

    private TabPane buildTabs() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tFin  = tab("💰  Financiero",  areaFinanciero);
        Tab tMat  = tab("📦  Materiales",  areaMateriales);
        Tab tCli  = tab("👥  Clientes",    areaClientes);
        Tab tPro  = tab("🏭  Producción",  areaProduccion);

        tabs.getTabs().addAll(tFin, tMat, tCli, tPro);

        tabs.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n == tFin && areaFinanciero.getChildren().isEmpty())
                cargar(areaFinanciero, this::contenidoFinanciero);
            else if (n == tMat && areaMateriales.getChildren().isEmpty())
                cargar(areaMateriales, this::contenidoMateriales);
            else if (n == tCli && areaClientes.getChildren().isEmpty())
                cargar(areaClientes, this::contenidoClientes);
            else if (n == tPro && areaProduccion.getChildren().isEmpty())
                cargar(areaProduccion, this::contenidoProduccion);
        });
        return tabs;
    }

    private Tab tab(String texto, Node contenido) {
        Tab t = new Tab(texto, contenido);
        t.setClosable(false);
        return t;
    }

    // ── Carga asíncrona con spinner ───────────────────────────────────────────

    private void cargar(StackPane area, Runnable builder) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(60, 60);
        area.getChildren().setAll(spinner);
        Thread.ofVirtual().start(() -> {
            // El builder construye el contenido (puede acceder a la BD)
            // y luego llama a Platform.runLater internamente
            builder.run();
            cargados.add(area);
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TAB 1 — FINANCIERO
    // ════════════════════════════════════════════════════════════════════════

    private void contenidoFinanciero() {
        int a = anio;
        Map<String, Double> ingresos   = EstadisticasService.ingresosPorMes(a);
        Map<String, Double> gastosMat  = EstadisticasService.gastosMaterialPorMes(a);
        Map<String, Double> gastosNom  = EstadisticasService.gastosNominasPorMes(a);
        Map<String, Double> acumulada  = EstadisticasService.facturacionAcumulada(a);
        Map<String, Double> estFacturas = EstadisticasService.facturasPorEstado();
        Map<String, Double> estPresup  = EstadisticasService.presupuestosPorEstado();
        double totIng  = EstadisticasService.totalIngresosAnio(a);
        double totGMat = EstadisticasService.totalGastosMaterialAnio(a);
        double totNom  = EstadisticasService.totalNominasAnio(a);
        double beneficio = totIng - totGMat - totNom;

        Platform.runLater(() -> {
            // ── KPI cards ────────────────────────────────────────────────────
            HBox kpis = new HBox(12,
                kpi("Ingresos " + a,       fmt(totIng)     + " €", COLOR_INGRESOS),
                kpi("Gastos materiales",   fmt(totGMat)    + " €", COLOR_GASTOS),
                kpi("Gastos nóminas",      fmt(totNom)     + " €", COLOR_NOMINAS),
                kpi("Beneficio estimado",  fmt(beneficio)  + " €", beneficio >= 0 ? COLOR_AZUL : COLOR_GASTOS)
            );
            kpis.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

            // ── Gráfica de barras ingresos vs gastos por mes ─────────────────
            BarChart<String, Number> barMes = barChart("Ingresos vs Gastos por mes (" + a + ")", false,
                serie("Ingresos",    ingresos,  COLOR_INGRESOS),
                serie("Mat. compras", gastosMat, COLOR_GASTOS),
                serie("Nóminas",     gastosNom, COLOR_NOMINAS)
            );
            barMes.setPrefHeight(300);
            barMes.setStyle("CHART_COLOR_1:" + COLOR_INGRESOS + "; CHART_COLOR_2:" + COLOR_GASTOS + "; CHART_COLOR_3:" + COLOR_NOMINAS + ";");

            // ── LineChart facturación acumulada ───────────────────────────────
            LineChart<String, Number> lineAcum = lineChart(
                "Facturación acumulada (" + a + ")",
                serie("Acumulado", acumulada, COLOR_PRIMARIO));
            lineAcum.setPrefHeight(240);
            lineAcum.setStyle("CHART_COLOR_1:" + COLOR_PRIMARIO + ";");

            // ── PieChart estado facturas ──────────────────────────────────────
            PieChart pieFacturas = pieChart("Estado de facturas", estFacturas,
                "#27AE60","#E74C3C","#E67E22","#95A5A6","#3498DB");
            pieFacturas.setPrefHeight(260);

            // ── PieChart estado presupuestos ──────────────────────────────────
            PieChart piePresup = pieChart("Estado de presupuestos", estPresup,
                "#27AE60","#4C9BE8","#F39C12","#E74C3C","#9B59B6");
            piePresup.setPrefHeight(260);

            HBox pieRow = new HBox(16, pieFacturas, piePresup);
            HBox.setHgrow(pieFacturas, Priority.ALWAYS);
            HBox.setHgrow(piePresup,   Priority.ALWAYS);

            VBox contenido = new VBox(14, kpis, barMes, lineAcum, pieRow);
            contenido.setPadding(new Insets(12));
            areaFinanciero.getChildren().setAll(scroll(contenido));
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TAB 2 — MATERIALES
    // ════════════════════════════════════════════════════════════════════════

    private void contenidoMateriales() {
        Map<String, Double> masUsados    = EstadisticasService.materialesMasUsados(10);
        Map<String, Double> porCategoria = EstadisticasService.materialesPorCategoria();
        Map<String, Double> stock        = EstadisticasService.stockMateriales(12);

        Platform.runLater(() -> {
            // ── BarChart top 10 más consumidos ────────────────────────────────
            BarChart<String, Number> barUso = barChart(
                "Top 10 materiales más consumidos (unidades de salida)", false,
                serie("Consumo", masUsados, COLOR_PRIMARIO));
            barUso.setPrefHeight(300);
            barUso.setStyle("CHART_COLOR_1:" + COLOR_PRIMARIO + ";");

            // ── PieChart por categoría ────────────────────────────────────────
            PieChart pieCat = pieChart("Materiales por categoría", porCategoria,
                "#6B2D5E","#4C9BE8","#27AE60","#F39C12","#E74C3C","#9B59B6");
            pieCat.setPrefHeight(280);

            // ── BarChart stock actual ─────────────────────────────────────────
            BarChart<String, Number> barStock = barChart(
                "Stock actual de materiales (top 12)", false,
                serie("Stock", stock, COLOR_AZUL));
            barStock.setPrefHeight(280);
            barStock.setStyle("CHART_COLOR_1:" + COLOR_AZUL + ";");

            HBox row1 = new HBox(16, barUso, pieCat);
            HBox.setHgrow(barUso, Priority.ALWAYS);

            VBox contenido = new VBox(14, row1, barStock);
            contenido.setPadding(new Insets(12));
            areaMateriales.getChildren().setAll(scroll(contenido));
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TAB 3 — CLIENTES
    // ════════════════════════════════════════════════════════════════════════

    private void contenidoClientes() {
        int a = anio;
        Map<String, Double> porMes     = EstadisticasService.clientesPorMes(a);
        Map<String, Double> porTipo    = EstadisticasService.clientesPorTipo();
        Map<String, Double> topClients = EstadisticasService.topClientesPorFacturacion(8);
        int nuevosAnio  = EstadisticasService.totalClientesAnio(a);
        int totalAcum   = EstadisticasService.totalClientesAcumulado();

        Platform.runLater(() -> {
            // ── KPI ───────────────────────────────────────────────────────────
            HBox kpis = new HBox(12,
                kpi("Total clientes",          String.valueOf(totalAcum),  COLOR_PRIMARIO),
                kpi("Nuevos en " + a,          String.valueOf(nuevosAnio), COLOR_AZUL),
                kpi("Tipo más habitual",        primeraClave(porTipo),      "#9B59B6")
            );
            kpis.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

            // ── LineChart nuevos por mes ──────────────────────────────────────
            LineChart<String, Number> linea = lineChart(
                "Nuevos clientes por mes (" + a + ")",
                serie("Nuevos clientes", porMes, COLOR_AZUL));
            linea.setPrefHeight(260);
            linea.setStyle("CHART_COLOR_1:" + COLOR_AZUL + ";");

            // ── PieChart tipo ─────────────────────────────────────────────────
            PieChart pieTipo = pieChart("Clientes por tipo", porTipo,
                COLOR_PRIMARIO, COLOR_AZUL, "#27AE60");
            pieTipo.setPrefHeight(260);

            HBox row1 = new HBox(16, linea, pieTipo);
            HBox.setHgrow(linea, Priority.ALWAYS);

            // ── BarChart top clientes ─────────────────────────────────────────
            BarChart<String, Number> barTop = barChart(
                "Top 8 clientes por facturación (€)", false,
                serie("Facturación €", topClients, COLOR_PRIMARIO));
            barTop.setPrefHeight(280);
            barTop.setStyle("CHART_COLOR_1:" + COLOR_PRIMARIO + ";");

            VBox contenido = new VBox(14, kpis, row1, barTop);
            contenido.setPadding(new Insets(12));
            areaClientes.getChildren().setAll(scroll(contenido));
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TAB 4 — PRODUCCIÓN
    // ════════════════════════════════════════════════════════════════════════

    private void contenidoProduccion() {
        Map<String, Double> tecIngr   = EstadisticasService.tecnicasPorIngresos();
        Map<String, Double> tecVol    = EstadisticasService.tecnicasPorVolumen();
        Map<String, Double> pedEstado = EstadisticasService.pedidosPorEstado();

        Platform.runLater(() -> {
            // ── PieChart técnicas por ingresos ────────────────────────────────
            PieChart pieTecIngr = pieChart("Ingresos por técnica (€)", tecIngr,
                COLOR_PRIMARIO, COLOR_AZUL, COLOR_INGRESOS, COLOR_NOMINAS, COLOR_GASTOS, "#9B59B6","#1ABC9C","#E67E22");
            pieTecIngr.setPrefHeight(300);

            // ── PieChart pedidos por estado ───────────────────────────────────
            PieChart piePedidos = pieChart("Pedidos por estado", pedEstado,
                "#F39C12","#4C9BE8","#27AE60","#9B59B6","#E74C3C");
            piePedidos.setPrefHeight(300);

            HBox pieRow = new HBox(16, pieTecIngr, piePedidos);
            HBox.setHgrow(pieTecIngr, Priority.ALWAYS);
            HBox.setHgrow(piePedidos, Priority.ALWAYS);

            // ── BarChart técnicas por ingresos ────────────────────────────────
            BarChart<String, Number> barIngr = barChart(
                "Ingresos por técnica (€)", false,
                serie("Ingresos €", tecIngr, COLOR_PRIMARIO));
            barIngr.setPrefHeight(260);
            barIngr.setStyle("CHART_COLOR_1:" + COLOR_PRIMARIO + ";");

            // ── BarChart técnicas por volumen ─────────────────────────────────
            BarChart<String, Number> barVol = barChart(
                "Volumen de líneas por técnica (uds.)", false,
                serie("Líneas", tecVol, COLOR_AZUL));
            barVol.setPrefHeight(260);
            barVol.setStyle("CHART_COLOR_1:" + COLOR_AZUL + ";");

            HBox barRow = new HBox(16, barIngr, barVol);
            HBox.setHgrow(barIngr, Priority.ALWAYS);
            HBox.setHgrow(barVol,  Priority.ALWAYS);

            VBox contenido = new VBox(14, pieRow, barRow);
            contenido.setPadding(new Insets(12));
            areaProduccion.getChildren().setAll(scroll(contenido));
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CONSTRUCTORES DE GRÁFICAS
    // ════════════════════════════════════════════════════════════════════════

    @SafeVarargs
    private BarChart<String, Number> barChart(String titulo, boolean leyenda,
            XYChart.Series<String, Number>... series) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis   y = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setTitle(titulo);
        chart.setAnimated(false);
        chart.setLegendVisible(leyenda || series.length > 1);
        chart.setBarGap(3);
        chart.setCategoryGap(12);
        for (var s : series) chart.getData().add(s);
        return chart;
    }

    private LineChart<String, Number> lineChart(String titulo,
            XYChart.Series<String, Number> serie) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis   y = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.setTitle(titulo);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setLegendVisible(false);
        chart.getData().add(serie);
        return chart;
    }

    private PieChart pieChart(String titulo, Map<String, Double> datos, String... colores) {
        PieChart chart = new PieChart();
        chart.setTitle(titulo);
        chart.setAnimated(false);
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setStartAngle(90);

        List<PieChart.Data> slices = new ArrayList<>();
        for (Map.Entry<String, Double> e : datos.entrySet()) {
            if (e.getValue() > 0) slices.add(new PieChart.Data(e.getKey(), e.getValue()));
        }
        if (slices.isEmpty()) slices.add(new PieChart.Data("Sin datos", 1));
        chart.getData().addAll(slices);

        // Colorear slices después de que el scene construya los nodos
        Platform.runLater(() -> {
            for (int i = 0; i < chart.getData().size(); i++) {
                PieChart.Data d = chart.getData().get(i);
                String color = colores.length > 0 ? colores[i % colores.length] : PALETA[i % PALETA.length];
                if (d.getNode() != null) {
                    d.getNode().setStyle("-fx-pie-color: " + color + ";");
                }
            }
        });
        return chart;
    }

    // ── Helpers de series ─────────────────────────────────────────────────────

    private XYChart.Series<String, Number> serie(String nombre, Map<String, Double> datos, String color) {
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName(nombre);
        for (Map.Entry<String, Double> e : datos.entrySet()) {
            String etiqueta = e.getKey();
            if (etiqueta.length() > 16) etiqueta = etiqueta.substring(0, 14) + "…";
            s.getData().add(new XYChart.Data<>(etiqueta, e.getValue()));
        }
        return s;
    }

    // ── Helpers de layout ─────────────────────────────────────────────────────

    private VBox kpi(String titulo, String valor, String color) {
        Label lTit = new Label(titulo);
        lTit.setStyle("-fx-font-size:11px; -fx-text-fill:#444;");
        Label lVal = new Label(valor);
        lVal.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:" + color + ";");
        VBox card = new VBox(4, lTit, lVal);
        card.getStyleClass().add("dashboard-card");
        card.setStyle("-fx-border-color:" + color + "; -fx-border-width:0 0 0 4;");
        card.setPadding(new Insets(12, 16, 12, 16));
        return card;
    }

    private ScrollPane scroll(Node contenido) {
        ScrollPane sp = new ScrollPane(contenido);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent; -fx-background:transparent;");
        return sp;
    }

    private String fmt(double v) {
        if (Math.abs(v) >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (Math.abs(v) >= 1_000)     return String.format("%.0fK", v / 1_000);
        return String.format("%.0f", v);
    }

    private String primeraClave(Map<String, Double> map) {
        return map.isEmpty() ? "—" : map.keySet().iterator().next();
    }

    private void previsualizar() {
        setDisable(true);
        int a = anio;
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdf = PdfPreviewService.previsualizarEstadisticas(a);
                Platform.runLater(() -> {
                    setDisable(false);
                    PdfPreviewWindow.mostrar(pdf, "Previsualización — Estadísticas " + a);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setDisable(false);
                    new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage(), ButtonType.OK).showAndWait();
                });
            }
        });
    }

    private void exportarPDF() {
        btnExportarPDF.setDisable(true);
        btnExportarPDF.setText("Generando…");
        SoundService.play(SoundService.Sound.START);
        int a = anio;
        Thread.ofVirtual().start(() -> {
            try {
                Path path = new PDFService().generarEstadisticas(a);
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.COMPLETE);
                    btnExportarPDF.setDisable(false);
                    btnExportarPDF.setText("📄  Exportar PDF");
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("PDF generado");
                    alert.setHeaderText("Informe de estadísticas exportado");
                    alert.setContentText("Guardado en:\n" + path.toAbsolutePath());
                    ButtonType btnAbrir = new ButtonType("Abrir archivo");
                    alert.getButtonTypes().setAll(btnAbrir, ButtonType.OK);
                    alert.showAndWait().ifPresent(bt -> {
                        if (bt == btnAbrir && Desktop.isDesktopSupported()) {
                            try { Desktop.getDesktop().open(path.toFile()); }
                            catch (Exception ex) { /* sin app PDF disponible */ }
                        }
                    });
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.ERROR);
                    btnExportarPDF.setDisable(false);
                    btnExportarPDF.setText("📄  Exportar PDF");
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Error al generar PDF");
                    err.setHeaderText("No se pudo generar el informe");
                    err.setContentText(ex.getMessage());
                    err.showAndWait();
                });
            }
        });
    }
}
