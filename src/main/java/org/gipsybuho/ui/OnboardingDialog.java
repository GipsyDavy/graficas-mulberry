package org.gipsybuho.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import org.gipsybuho.service.PreferenceService;

/**
 * Diálogo de bienvenida y primer arranque. Se muestra una sola vez tras el login.
 * Permite al usuario elegir modo principiante y resume los primeros pasos.
 */
public class OnboardingDialog extends Dialog<Void> {

    private static final int TOTAL = 3;

    private int paso = 0;
    private CheckBox chkBeginner;
    private final StackPane contenido = new StackPane();
    private Button btnAnterior;
    private Button btnSiguiente;
    private Button btnEmpezar;
    private final VBox[] pasos = new VBox[TOTAL];

    public OnboardingDialog() {
        setTitle("Bienvenido a Gráficas Mulberry");
        setHeaderText(null);
        getDialogPane().getStylesheets().add(
            getClass().getResource("/org/gipsybuho/styles.css").toExternalForm());
        getDialogPane().getStyleClass().add("onboarding-dialog");
        getDialogPane().setPrefWidth(520);

        construirPasos();
        construirBotones();
        getDialogPane().setContent(contenido);
        actualizarVista();
        // Garantiza que first_run_completed se marca independientemente de cómo se cierre el diálogo.
        setOnHiding(e -> aplicarPreferencias());
    }

    private void construirPasos() {
        pasos[0] = buildPaso0();
        pasos[1] = buildPaso1();
        pasos[2] = buildPaso2();
    }

    private VBox buildPaso0() {
        Label titulo = lbl("Bienvenido a\nGráficas Mulberry", "onboarding-titulo");
        Label desc = lbl(
            "Gestiona clientes, pedidos, facturas y almacén en un solo lugar.\n\n"
            + "Esta guía rápida te ayuda a dar los primeros pasos.",
            "onboarding-desc");
        return paso(titulo, desc);
    }

    private VBox buildPaso1() {
        Label titulo = lbl("¿Cómo prefieres empezar?", "onboarding-titulo");
        chkBeginner = new CheckBox("Activar modo principiante");
        chkBeginner.setSelected(true);
        chkBeginner.getStyleClass().add("onboarding-check");
        Label desc = lbl(
            "El modo principiante muestra sugerencias en cada pantalla.\n"
            + "Puedes cambiarlo en Configuración → Preferencias.",
            "onboarding-desc");
        return paso(titulo, chkBeginner, desc);
    }

    private VBox buildPaso2() {
        Label titulo = lbl("Tus primeros pasos", "onboarding-titulo");
        Label desc = lbl(
            "1.  Configura tu empresa en Configuración → Mi empresa.\n"
            + "2.  Añade tus clientes desde el módulo Clientes.\n"
            + "3.  Crea un pedido y genera tu primera factura.\n\n"
            + "Pulsa F1 en cualquier pantalla para abrir la ayuda contextual.",
            "onboarding-desc");
        desc.setTextAlignment(TextAlignment.LEFT);
        return paso(titulo, desc);
    }

    private Label lbl(String text, String styleClass) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setTextAlignment(TextAlignment.CENTER);
        l.setMaxWidth(420);
        l.getStyleClass().add(styleClass);
        return l;
    }

    private VBox paso(Node... nodos) {
        VBox box = new VBox(20, nodos);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(36, 48, 28, 48));
        box.getStyleClass().add("onboarding-paso");
        return box;
    }

    private void construirBotones() {
        ButtonType btSaltar    = new ButtonType("Saltar",       ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType btAnterior  = new ButtonType("← Anterior",  ButtonBar.ButtonData.BACK_PREVIOUS);
        ButtonType btSiguiente = new ButtonType("Siguiente →",  ButtonBar.ButtonData.NEXT_FORWARD);
        ButtonType btEmpezar   = new ButtonType("¡Empezar!",    ButtonBar.ButtonData.FINISH);

        getDialogPane().getButtonTypes().addAll(btSaltar, btAnterior, btSiguiente, btEmpezar);

        btnAnterior  = (Button) getDialogPane().lookupButton(btAnterior);
        btnSiguiente = (Button) getDialogPane().lookupButton(btSiguiente);
        btnEmpezar   = (Button) getDialogPane().lookupButton(btEmpezar);
        Button btnSaltar = (Button) getDialogPane().lookupButton(btSaltar);

        btnSiguiente.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            paso++;
            actualizarVista();
        });
        btnAnterior.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            paso--;
            actualizarVista();
        });
        // No action filter needed — setOnHiding covers all close paths.
    }

    private void actualizarVista() {
        contenido.getChildren().setAll(pasos[paso]);
        boolean esUltimo = paso == TOTAL - 1;
        btnAnterior.setVisible(paso > 0);
        btnAnterior.setManaged(paso > 0);
        btnSiguiente.setVisible(!esUltimo);
        btnSiguiente.setManaged(!esUltimo);
        btnEmpezar.setVisible(esUltimo);
        btnEmpezar.setManaged(esUltimo);
    }

    private void aplicarPreferencias() {
        PreferenceService prefs = PreferenceService.getInstance();
        if (chkBeginner != null) prefs.setBeginnerMode(chkBeginner.isSelected());
        prefs.markFirstRunCompleted();
    }
}
