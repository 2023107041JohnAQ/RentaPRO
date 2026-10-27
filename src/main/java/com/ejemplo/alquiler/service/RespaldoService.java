package com.ejemplo.alquiler.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author Jhon Alessandro Arpi Quispe
 */
@Service
public class RespaldoService {

    public static final int MAX_AUTOMATICOS = 15;

    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
    private static final Pattern NOMBRE = Pattern.compile("rentapro-(\\d{4}-\\d{2}-\\d{2}_\\d{6})(-manual)?\\.db");

    public record Respaldo(Path archivo, LocalDateTime fecha, long bytes, boolean manual) {
    }

    private final JdbcTemplate jdbc;
    private ScheduledExecutorService reloj;

    public RespaldoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void iniciar() {
        reloj = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "respaldo-automatico");
            t.setDaemon(true);
            return t;
        });
        reloj.scheduleWithFixedDelay(this::respaldoDelDia, 20, 6 * 3600L, TimeUnit.SECONDS);
    }

    @PreDestroy
    void detener() {
        if (reloj != null) reloj.shutdownNow();
    }

    public static Path carpetaRespaldos() {
        return Path.of(System.getProperty("user.home"), "RentaPro", "respaldos");
    }

    public Path carpeta() {
        return carpetaRespaldos();
    }

    private static Path rutaBaseDatos() {
        return Path.of(System.getProperty("rentapro.db.path"));
    }

    private static Path rutaPendiente() {
        Path bd = rutaBaseDatos();
        return bd.resolveSibling(bd.getFileName() + ".restaurar");
    }

    private void respaldoDelDia() {
        try {
            LocalDateTime ahora = LocalDateTime.now();
            boolean hecho = listar().stream().anyMatch(r -> !r.manual() && r.fecha().toLocalDate().equals(ahora.toLocalDate()));
            if (!hecho) crear(false);
        } catch (Exception e) {
            System.err.println("No se pudo crear el respaldo automático: " + e.getMessage());
        }
    }

    public Path crear(boolean manual) throws IOException {
        Files.createDirectories(carpeta());
        Path destino = carpeta().resolve("rentapro-" + SELLO.format(LocalDateTime.now()) + (manual ? "-manual" : "") + ".db");
        volcar(destino);
        if (!manual) depurar();
        return destino;
    }

    public void exportar(Path destino) throws IOException {
        volcar(destino);
    }

    private void volcar(Path destino) throws IOException {
        Files.deleteIfExists(destino);
        String ruta = destino.toAbsolutePath().toString().replace('\\', '/').replace("'", "''");
        try {
            jdbc.execute("VACUUM INTO '" + ruta + "'");
        } catch (Exception e) {
            throw new IOException("No se pudo copiar la base de datos: " + e.getMessage(), e);
        }
    }

    private void depurar() throws IOException {
        List<Respaldo> automaticos = listar().stream().filter(r -> !r.manual()).toList();
        for (int i = MAX_AUTOMATICOS; i < automaticos.size(); i++) Files.deleteIfExists(automaticos.get(i).archivo());
    }

    public List<Respaldo> listar() {
        Path carpeta = carpeta();
        if (!Files.isDirectory(carpeta)) return List.of();
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos
                    .filter(Files::isRegularFile)
                    .map(this::aRespaldo)
                    .filter(r -> r != null)
                    .sorted(Comparator.comparing(Respaldo::fecha).reversed())
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private Respaldo aRespaldo(Path archivo) {
        Matcher m = NOMBRE.matcher(archivo.getFileName().toString());
        if (!m.matches()) return null;
        try {
            return new Respaldo(archivo, LocalDateTime.parse(m.group(1), SELLO), Files.size(archivo), m.group(2) != null);
        } catch (Exception e) {
            return null;
        }
    }

    public void programarRestauracion(Path origen) throws IOException {
        validar(origen);
        Files.copy(origen, rutaPendiente(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void validar(Path origen) throws IOException {
        byte[] cabecera = new byte[16];
        try (InputStream in = Files.newInputStream(origen)) {
            if (in.readNBytes(cabecera, 0, 16) < 16 || !new String(cabecera, 0, 15, StandardCharsets.US_ASCII).equals("SQLite format 3"))
                throw new IOException("El archivo elegido no es una base de datos de RentaPro.");
        }
        String ruta = origen.toAbsolutePath().toString().replace('\\', '/');
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + ruta);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name IN ('vehiculos','clientes','reservas','pagos')")) {
            if (!rs.next() || rs.getInt(1) < 4)
                throw new IOException("El archivo elegido no parece un respaldo de RentaPro (faltan tablas).");
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("No se pudo leer el archivo elegido: " + e.getMessage(), e);
        }
    }

    public static void aplicarRestauracionPendiente() {
        try {
            Path bd = rutaBaseDatos();
            Path pendiente = rutaPendiente();
            if (!Files.exists(pendiente)) return;
            Files.createDirectories(carpetaRespaldos());
            if (Files.exists(bd)) {
                Files.move(bd, carpetaRespaldos().resolve("rentapro-antes-de-restaurar-" + SELLO.format(LocalDateTime.now()) + ".db"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            for (String sufijo : new String[]{"-journal", "-wal", "-shm"})
                Files.deleteIfExists(bd.resolveSibling(bd.getFileName() + sufijo));
            Files.move(pendiente, bd, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("No se pudo aplicar la restauración pendiente: " + e.getMessage());
        }
    }
}
