package com.ejemplo.alquiler.util;

import com.ejemplo.alquiler.JavaFxApplication;

import java.awt.Desktop;
import java.nio.file.Path;

/**
 * @author Julio Cesar Tutacano de la Cruz
 */
public final class Abrir {

    private Abrir() {
    }

    public static boolean ruta(Path ruta) {
        try {
            var servicios = JavaFxApplication.hostServices();
            if (servicios != null) {
                servicios.showDocument(ruta.toUri().toString());
                return true;
            }
        } catch (Exception ignorada) {
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(ruta.toFile());
                return true;
            }
        } catch (Exception ignorada) {
        }
        return false;
    }
}
