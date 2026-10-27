package com.ejemplo.alquiler;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Jhon Alessandro Arpi Quispe
 * @author Julio Cesar Tutacano de la Cruz
 * @author Jhunior Eric Herrera Condori
 */
public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext context;
    private static HostServices servicios;

    public static HostServices hostServices() {
        return servicios;
    }

    @Override
    public void init() {
        String ruta = Path.of(System.getProperty("user.home"), "rentapro.db").toString().replace('\\', '/');
        System.setProperty("rentapro.db.path", ruta);
        com.ejemplo.alquiler.service.RespaldoService.aplicarRestauracionPendiente();
        context = new SpringApplicationBuilder(AlquilerVehiculosApplication.class).web(WebApplicationType.NONE).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        servicios = getHostServices();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));
        loader.setControllerFactory(context::getBean);
        Scene scene = new Scene(loader.load(), 1300, 840);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());
        stage.setTitle("RentaPro | Alquiler de vehículos y motos");
        stage.setMinWidth(1180);
        stage.setMinHeight(700);
        var icono = getClass().getResourceAsStream("/img/icono.png");
        if (icono != null) stage.getIcons().add(new Image(icono));
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (context != null) context.close();
        Platform.exit();
    }
}
