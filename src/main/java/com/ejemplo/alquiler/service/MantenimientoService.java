package com.ejemplo.alquiler.service;

import com.ejemplo.alquiler.model.Mantenimiento;
import com.ejemplo.alquiler.model.Vehiculo;
import com.ejemplo.alquiler.repository.MantenimientoRepository;
import com.ejemplo.alquiler.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Jhon Alessandro Arpi Quispe
 */
@Service
@Transactional
public class MantenimientoService {

    public static final String VENCIDO = "VENCIDO";
    public static final String POR_VENCER = "POR VENCER";
    public static final String VIGENTE = "VIGENTE";
    public static final String REGISTRADO = "REGISTRADO";
    public static final String RENOVADO = "RENOVADO";

    public static final long DIAS_AVISO = 30;

    public record Registro(Mantenimiento mantenimiento, String estado, Long dias) {
        public boolean esAlerta() {
            return VENCIDO.equals(estado) || POR_VENCER.equals(estado);
        }
    }

    private final MantenimientoRepository repo;
    private final VehiculoRepository vehiculos;

    public MantenimientoService(MantenimientoRepository repo, VehiculoRepository vehiculos) {
        this.repo = repo;
        this.vehiculos = vehiculos;
    }

    private static String clave(Mantenimiento m) {
        return m.getVehiculo().getId() + "|" + m.getTipo().trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public List<Registro> registros() {
        LocalDate hoy = LocalDate.now();
        List<Mantenimiento> todos = repo.findAll();
        Map<String, LocalDate> ultimoVencimiento = new HashMap<>();
        for (Mantenimiento m : todos) {
            if (m.getVencimiento() == null) continue;
            ultimoVencimiento.merge(clave(m), m.getVencimiento(), (a, b) -> a.isAfter(b) ? a : b);
        }
        return todos.stream()
                .map(m -> registro(m, hoy, ultimoVencimiento.get(clave(m))))
                .sorted(Comparator.comparing((Registro r) -> r.esAlerta() ? 0 : 1)
                        .thenComparing(r -> r.dias() == null ? Long.MAX_VALUE : r.dias())
                        .thenComparing(r -> r.mantenimiento().getId(), Comparator.reverseOrder()))
                .toList();
    }

    private Registro registro(Mantenimiento m, LocalDate hoy, LocalDate ultimo) {
        if (m.getVencimiento() == null) return new Registro(m, REGISTRADO, null);
        long dias = ChronoUnit.DAYS.between(hoy, m.getVencimiento());
        if (ultimo != null && m.getVencimiento().isBefore(ultimo)) return new Registro(m, RENOVADO, dias);
        String estado = dias < 0 ? VENCIDO : (dias <= DIAS_AVISO ? POR_VENCER : VIGENTE);
        return new Registro(m, estado, dias);
    }

    @Transactional(readOnly = true)
    public List<Registro> alertas() {
        return registros().stream()
                .filter(Registro::esAlerta)
                .filter(r -> r.mantenimiento().getVehiculo().isActivo())
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarAlertas() {
        return alertas().size();
    }

    public Mantenimiento guardar(Mantenimiento m) {
        if (m.getVehiculo() == null || m.getVehiculo().getId() == null) throw new IllegalArgumentException("Selecciona el vehículo.");
        Vehiculo v = vehiculos.findById(m.getVehiculo().getId()).orElseThrow(() -> new IllegalArgumentException("El vehículo ya no existe."));
        if (m.getTipo() == null || m.getTipo().isBlank()) throw new IllegalArgumentException("Indica qué se registra (SOAT, revisión técnica, cambio de aceite...).");
        if (m.getFecha() == null) throw new IllegalArgumentException("Indica la fecha en que se hizo o se emitió.");
        if (m.getVencimiento() != null && m.getVencimiento().isBefore(m.getFecha()))
            throw new IllegalArgumentException("El vencimiento no puede ser anterior a la fecha de emisión.");
        if (m.getKilometraje() != null && m.getKilometraje() < 0) throw new IllegalArgumentException("El kilometraje no puede ser negativo.");
        if (m.getCosto() < 0) throw new IllegalArgumentException("El costo no puede ser negativo.");
        m.setVehiculo(v);
        m.setTipo(m.getTipo().trim());
        return repo.save(m);
    }

    public void eliminar(Mantenimiento m) {
        if (m == null || m.getId() == null) throw new IllegalArgumentException("Selecciona un registro de la tabla.");
        repo.deleteById(m.getId());
    }
}
