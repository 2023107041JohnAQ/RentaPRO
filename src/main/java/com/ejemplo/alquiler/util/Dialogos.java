package com.ejemplo.alquiler.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.net.URL;

/**
 * @author Jhunior Eric Herrera Condori
 */
public final class Dialogos {

    private Dialogos() {
    }

    public static boolean confirmar(String encabezado, String texto, String textoBoton) {
        ButtonType si = new ButtonType(textoBoton, ButtonBar.ButtonData.OK_DONE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, texto, si, ButtonType.CANCEL);
        alerta.setHeaderText(encabezado);
        estilizar(alerta);
        return alerta.showAndWait().filter(b -> b == si).isPresent();
    }

    public static void aviso(String encabezado, String texto) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, texto, ButtonType.OK);
        alerta.setHeaderText(encabezado);
        estilizar(alerta);
        alerta.showAndWait();
    }

    public static void error(String texto) {
        Alert alerta = new Alert(Alert.AlertType.WARNING, texto == null ? "Revisa los datos ingresados." : texto, ButtonType.OK);
        alerta.setHeaderText("No se pudo completar la operación");
        estilizar(alerta);
        alerta.showAndWait();
    }

    private static void estilizar(Alert alerta) {
        URL css = Dialogos.class.getResource("/css/app.css");
        if (css != null) alerta.getDialogPane().getStylesheets().add(css.toExternalForm());
    }
}
