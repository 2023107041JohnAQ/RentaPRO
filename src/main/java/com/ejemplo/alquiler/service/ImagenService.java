package com.ejemplo.alquiler.service;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * @author Jhunior Eric Herrera Condori
 */
@Service
public class ImagenService {

    private static final Path CARPETA = Path.of(System.getProperty("user.home"), "RentaPro", "imagenes");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final Map<String, Image> cache = new HashMap<>();

    public String guardar(File origen) throws IOException {
        if (origen == null || !origen.isFile()) throw new IOException("No se encontró el archivo de imagen.");
        if (origen.length() > MAX_BYTES) throw new IOException("La imagen supera los 10 MB. Elige una más liviana.");
        String nombre = origen.getName();
        int punto = nombre.lastIndexOf('.');
        String extension = punto >= 0 ? nombre.substring(punto).toLowerCase(Locale.ROOT) : ".png";
        Files.createDirectories(CARPETA);
        Path destino = CARPETA.resolve(UUID.randomUUID() + extension);
        Files.copy(origen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
        return destino.toString();
    }

    public boolean existe(String ruta) {
        return ruta != null && !ruta.isBlank() && Files.isRegularFile(Path.of(ruta));
    }

    private Image cargar(String ruta, double ancho, double alto) {
        if (!existe(ruta)) return null;
        String clave = ruta + "@" + ancho + "x" + alto;
        return cache.computeIfAbsent(clave, k -> new Image(Path.of(ruta).toUri().toString(), ancho, alto, true, true, true));
    }

    public Node miniatura(String ruta, double ancho, double alto) {
        StackPane caja = new StackPane();
        caja.getStyleClass().add("thumb");
        caja.setMinSize(ancho, alto);
        caja.setPrefSize(ancho, alto);
        caja.setMaxSize(ancho, alto);
        Image imagen = cargar(ruta, ancho * 2, alto * 2);
        if (imagen != null) {
            ImageView vista = new ImageView(imagen);
            vista.setFitWidth(ancho);
            vista.setFitHeight(alto);
            vista.setPreserveRatio(true);
            vista.setSmooth(true);
            caja.getChildren().add(vista);
        } else {
            Label vacio = new Label("Sin foto");
            vacio.getStyleClass().add("thumb-empty");
            caja.getChildren().add(vacio);
        }
        return caja;
    }
}
