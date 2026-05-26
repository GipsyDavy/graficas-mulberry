package org.gipsybuho.ui;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

/**
 * Iconos SVG para la interfaz — Material Design Icons (Apache 2.0).
 * Todos los paths usan viewBox 24×24 y son fill-based.
 */
public final class Icons {

    private Icons() {}

    // ── Paths (Material Design Icons, Apache 2.0) ────────────────────────────
    public static final String HOME       = "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z";
    public static final String USERS      = "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z";
    public static final String PERSON     = "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z";
    public static final String TAG        = "M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z";
    public static final String ASSIGNMENT = "M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z";
    public static final String CART       = "M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96C5 16.1 5.9 17 7 17h12v-2H7.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63H19c.75 0 1.41-.41 1.75-1.03l3.58-6.49c.08-.14.12-.31.12-.48 0-.55-.45-1-1-1H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z";
    public static final String FILE       = "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z";
    public static final String RECEIPT    = "M18 17H6v-2h12v2zm0-4H6v-2h12v2zm0-4H6V7h12v2zM3 22l1.5-1.5L6 22l1.5-1.5L9 22l1.5-1.5L12 22l1.5-1.5L15 22l1.5-1.5L18 22l1.5-1.5L21 22V2l-1.5 1.5L18 2l-1.5 1.5L15 2l-1.5 1.5L12 2l-1.5 1.5L9 2 7.5 3.5 6 2 4.5 3.5 3 2v20z";
    public static final String LAYERS     = "M11.99 18.54l-7.37-5.73L3 14.07l9 7 9-7-1.63-1.27-7.38 5.74zM12 16l7.36-5.73L21 9l-9-7-9 7 1.63 1.27L12 16z";
    public static final String WORK       = "M20 6h-4V4c0-1.11-.89-2-2-2h-4c-1.11 0-2 .89-2 2v2H4c-1.11 0-1.99.89-1.99 2L2 19c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-6 0h-4V4h4v2z";
    public static final String BAR_CHART  = "M5 9.2h3V19H5V9.2zM10.6 5h2.8v14h-2.8V5zm5.6 8H19v6h-2.8v-6z";
    public static final String CALENDAR   = "M20 3h-1V1h-2v2H7V1H5v2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 18H4V8h16v13z";
    public static final String ROBOT      = "M20 9V7c0-1.1-.9-2-2-2h-3c0-1.66-1.34-3-3-3S9 3.34 9 5H6c-1.1 0-2 .9-2 2v2c-1.66 0-3 1.34-3 3s1.34 3 3 3v4c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2v-4c1.66 0 3-1.34 3-3s-1.34-3-3-3zM7 7h10v7H7V7zm5 9c-.83 0-1.5-.67-1.5-1.5S11.17 13 12 13s1.5.67 1.5 1.5S12.83 16 12 16zm-2.5-5c-.28 0-.5-.22-.5-.5v-1c0-.28.22-.5.5-.5s.5.22.5.5v1c0 .28-.22.5-.5.5zm5 0c-.28 0-.5-.22-.5-.5v-1c0-.28.22-.5.5-.5s.5.22.5.5v1c0 .28-.22.5-.5.5z";
    public static final String DOWNLOAD   = "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z";
    public static final String UPLOAD     = "M5 12v7h14v-7h2v7c0 1.1-.9 2-2 2H5c-1.1 0-2-.9-2-2v-7h2zm7-10l-4 4h3v6h2V6h3l-4-4z";
    public static final String SETTINGS   = "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.57 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z";
    public static final String POWER      = "M13 3h-2v10h2V3zm4.83 2.17l-1.42 1.42C17.99 7.86 19 9.81 19 12c0 3.87-3.13 7-7 7s-7-3.13-7-7c0-2.19 1.01-4.14 2.58-5.42L6.17 5.17C4.23 6.82 3 9.26 3 12c0 4.97 4.03 9 9 9s9-4.03 9-9c0-2.74-1.23-5.18-3.17-6.83z";
    public static final String COMPASS    = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-5.5-2.5l7.51-3.49L17.5 6.5 9.99 9.99 6.5 17.5zm5.5-6.6c.61 0 1.1.49 1.1 1.1s-.49 1.1-1.1 1.1-1.1-.49-1.1-1.1.49-1.1 1.1-1.1z";
    public static final String CHEVRON_R  = "M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6-6-6z";

    // ── API pública — iconos de navegación (sidebar) ─────────────────────────
    public static StackPane home()       { return navIcon(HOME); }
    public static StackPane users()      { return navIcon(USERS); }
    public static StackPane person()     { return navIcon(PERSON); }
    public static StackPane tag()        { return navIcon(TAG); }
    public static StackPane assignment() { return navIcon(ASSIGNMENT); }
    public static StackPane cart()       { return navIcon(CART); }
    public static StackPane file()       { return navIcon(FILE); }
    public static StackPane receipt()    { return navIcon(RECEIPT); }
    public static StackPane layers()     { return navIcon(LAYERS); }
    public static StackPane work()       { return navIcon(WORK); }
    public static StackPane barChart()   { return navIcon(BAR_CHART); }
    public static StackPane calendar()   { return navIcon(CALENDAR); }
    public static StackPane robot()      { return navIcon(ROBOT); }
    public static StackPane download()   { return navIcon(DOWNLOAD); }
    public static StackPane upload()     { return navIcon(UPLOAD); }
    public static StackPane settings()   { return navIcon(SETTINGS); }
    public static StackPane power()      { return navIcon(POWER); }
    public static StackPane compass()    { return navIcon(COMPASS); }

    /**
     * Flecha para los grupos de nav — rotar el StackPane devuelto 90°
     * para indicar estado expandido.
     */
    public static StackPane navArrow() {
        SVGPath path = new SVGPath();
        path.setContent(CHEVRON_R);
        path.getStyleClass().add("nav-group-arrow-icon");
        path.getTransforms().add(new Scale(10.0 / 24.0, 10.0 / 24.0, 0, 0));
        StackPane pane = new StackPane(path);
        pane.setPrefSize(10, 10);
        pane.setMaxSize(10, 10);
        pane.setMinSize(10, 10);
        return pane;
    }

    /** Icono coloreado para las tarjetas KPI del dashboard. */
    public static StackPane cardIcon(String pathData, String colorHex, double size) {
        SVGPath path = new SVGPath();
        path.setContent(pathData);
        path.setStyle("-fx-fill: " + colorHex + ";");
        path.getTransforms().add(new Scale(size / 24.0, size / 24.0, 0, 0));
        StackPane pane = new StackPane(path);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        pane.setMinSize(size, size);
        return pane;
    }

    // ── Icono estándar de sidebar (15×15, clase nav-icon para CSS) ───────────
    private static StackPane navIcon(String pathData) {
        SVGPath path = new SVGPath();
        path.setContent(pathData);
        path.getStyleClass().add("nav-icon");
        path.getTransforms().add(new Scale(15.0 / 24.0, 15.0 / 24.0, 0, 0));
        StackPane pane = new StackPane(path);
        pane.setPrefSize(15, 15);
        pane.setMaxSize(15, 15);
        pane.setMinSize(15, 15);
        return pane;
    }
}
