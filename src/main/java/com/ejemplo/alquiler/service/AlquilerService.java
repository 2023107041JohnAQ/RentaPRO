package com.ejemplo.alquiler.service;

import com.ejemplo.alquiler.model.*;
import com.ejemplo.alquiler.repository.*;
import com.ejemplo.alquiler.util.Recargos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Jhon Alessandro Arpi Quispe
 * @author Julio Cesar Tutacano de la Cruz
 * @author Jhunior Eric Herrera Condori
 */
@Service
@Transactional
public class AlquilerService {

    public static final String RESERVADA = "RESERVADA";
    public static final String EN_ALQUILER = "EN ALQUILER";
    public static final String DEVUELTA = "DEVUELTA";
    public static final String CANCELADA = "CANCELADA";

    public static final String PAGADO = "PAGADO";
    public static final String PARCIAL = "PARCIAL";
    public static final String PENDIENTE = "PENDIENTE";
    public static final String EN_MORA = "EN MORA";
    public static final String ANULADA = "ANULADA";

    public record ResumenDevolucion(LocalDate fechaPrevista, LocalDate fechaReal, long diasRetraso,
                                    double tarifaDiaria, double recargoPorDia, double recargo,
                                    double totalAlquiler, double pagado, double saldo) {
        public boolean conRetraso() {
            return diasRetraso > 0;
        }
    }

    public record CuentaReserva(Reserva reserva, long diasRetraso, double recargo, double totalAlquiler,
                                double pagado, double saldo, String estadoPago) {
        public double totalConRecargo() {
            return Recargos.redondear(totalAlquiler + recargo);
        }

        public boolean conRetraso() {
            return diasRetraso > 0;
        }
    }

    public record ResumenPanel(long flota, long disponiblesHoy, long reservasActivas, double ingresos,
                               double porCobrar, long reservasConRetraso) {
    }

    private final VehiculoRepository vehiculos;
    private final ClienteRepository clientes;
    private final ReservaRepository reservas;
    private final ContratoRepository contratos;
    private final PagoRepository pagos;
    private final MantenimientoRepository mantenimientos;

    @Value("${rentapro.recargo.porcentaje:0.25}")
    private double porcentajeRecargo;

    public AlquilerService(VehiculoRepository v, ClienteRepository c, ReservaRepository r, ContratoRepository co, PagoRepository p,
                          MantenimientoRepository m) {
        vehiculos = v;
        clientes = c;
        reservas = r;
        contratos = co;
        pagos = p;
        mantenimientos = m;
    }

    public double getPorcentajeRecargo() {
        return porcentajeRecargo;
    }

    @Transactional(readOnly = true)
    public List<Vehiculo> vehiculos() {
        return vehiculos.findAll(Sort.by("placa"));
    }

    @Transactional(readOnly = true)
    public List<Cliente> clientes() {
        return clientes.findAll(Sort.by("nombres"));
    }

    @Transactional(readOnly = true)
    public List<Reserva> reservas() {
        return reservas.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public List<Contrato> contratos() {
        return contratos.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public List<Pago> pagos() {
        return pagos.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public Vehiculo guardarVehiculo(Vehiculo v) {
        if (v.getPlaca() == null || v.getPlaca().isBlank()) throw new IllegalArgumentException("La placa o código es obligatorio.");
        if (v.getMarca() == null || v.getMarca().isBlank()) throw new IllegalArgumentException("La marca es obligatoria.");
        if (v.getModelo() == null || v.getModelo().isBlank()) throw new IllegalArgumentException("El modelo es obligatorio.");
        if (v.getTipo() == null || v.getTipo().isBlank()) throw new IllegalArgumentException("Selecciona el tipo (auto, moto, etc.).");
        if (v.getTarifaDiaria() <= 0) throw new IllegalArgumentException("La tarifa diaria debe ser mayor que cero.");
        boolean duplicada = vehiculos.findAll().stream()
                .anyMatch(o -> o.getPlaca().equalsIgnoreCase(v.getPlaca()) && !Objects.equals(o.getId(), v.getId()));
        if (duplicada) throw new IllegalArgumentException("Ya existe un vehículo con la placa " + v.getPlaca() + ".");
        return vehiculos.save(v);
    }

    public boolean eliminarVehiculo(Vehiculo v) {
        if (v == null || v.getId() == null) throw new IllegalArgumentException("Selecciona un vehículo de la tabla.");
        boolean conHistorial = reservas.findAll().stream().anyMatch(r -> r.getVehiculo().getId().equals(v.getId()));
        if (conHistorial) {
            Vehiculo actual = vehiculos.findById(v.getId()).orElseThrow(() -> new IllegalArgumentException("El vehículo ya no existe."));
            actual.setActivo(false);
            vehiculos.save(actual);
            return false;
        }
        mantenimientos.deleteByVehiculoId(v.getId());
        vehiculos.deleteById(v.getId());
        return true;
    }

    public Cliente guardarCliente(Cliente c) {
        if (c.getDocumento() == null || c.getDocumento().isBlank() || c.getNombres() == null || c.getNombres().isBlank())
            throw new IllegalArgumentException("Documento y nombres son obligatorios.");
        if (!c.getDocumento().matches("\\d{8}|\\d{11}"))
            throw new IllegalArgumentException("El documento debe tener 8 dígitos (DNI) u 11 dígitos (RUC).");
        if (c.getTelefono() == null || !c.getTelefono().matches("[0-9+()\\- ]{6,}"))
            throw new IllegalArgumentException("Ingresa un teléfono válido (mínimo 6 dígitos).");
        if (c.getCorreo() != null && !c.getCorreo().isBlank() && !c.getCorreo().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("El correo electrónico no tiene un formato válido.");
        boolean duplicado = clientes.findAll().stream()
                .anyMatch(o -> o.getDocumento().equals(c.getDocumento()) && !Objects.equals(o.getId(), c.getId()));
        if (duplicado) throw new IllegalArgumentException("Ya existe un cliente con el documento " + c.getDocumento() + ".");
        return clientes.save(c);
    }

    public void eliminarCliente(Cliente c) {
        if (c == null || c.getId() == null) throw new IllegalArgumentException("Selecciona un cliente de la tabla.");
        boolean conHistorial = reservas.findAll().stream().anyMatch(r -> r.getCliente().getId().equals(c.getId()));
        if (conHistorial) throw new IllegalArgumentException("No se puede eliminar: el cliente tiene reservas registradas.");
        clientes.deleteById(c.getId());
    }

    public static long dias(LocalDate inicio, LocalDate fin) {
        return ChronoUnit.DAYS.between(inicio, fin) + 1;
    }

    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) throw new IllegalArgumentException("Selecciona la fecha de inicio y la de fin.");
        if (fin.isBefore(inicio)) throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial.");
    }

    @Transactional(readOnly = true)
    public List<Vehiculo> disponibles(LocalDate inicio, LocalDate fin) {
        validarRango(inicio, fin);
        LocalDate hoy = LocalDate.now();
        List<Reserva> abiertas = reservas.findAll().stream().filter(Reserva::estaAbierta).toList();
        return vehiculos.findAll(Sort.by("placa")).stream()
                .filter(Vehiculo::isActivo)
                .filter(v -> abiertas.stream().noneMatch(r -> {
                    if (!r.getVehiculo().getId().equals(v.getId())) return false;
                    LocalDate finEfectivo = EN_ALQUILER.equals(r.getEstado()) && r.getFechaFin().isBefore(hoy) ? hoy : r.getFechaFin();
                    return !fin.isBefore(r.getFechaInicio()) && !inicio.isAfter(finEfectivo);
                }))
                .toList();
    }

    public Reserva reservar(Vehiculo v, Cliente c, LocalDate inicio, LocalDate fin) {
        if (v == null) throw new IllegalArgumentException("Selecciona un vehículo.");
        if (c == null) throw new IllegalArgumentException("Selecciona un cliente.");
        validarRango(inicio, fin);
        Vehiculo vehiculo = vehiculos.findById(v.getId()).orElseThrow(() -> new IllegalArgumentException("El vehículo ya no existe."));
        Cliente cliente = clientes.findById(c.getId()).orElseThrow(() -> new IllegalArgumentException("El cliente ya no existe."));
        if (disponibles(inicio, fin).stream().noneMatch(x -> x.getId().equals(vehiculo.getId())))
            throw new IllegalArgumentException("El vehículo no está disponible en esas fechas.");
        Reserva r = new Reserva();
        r.setVehiculo(vehiculo);
        r.setCliente(cliente);
        r.setFechaInicio(inicio);
        r.setFechaFin(fin);
        r.setEstado(RESERVADA);
        r.setTotalEstimado(dias(inicio, fin) * vehiculo.getTarifaDiaria());
        return reservas.save(r);
    }

    public double cancelarReserva(Reserva r) {
        if (r == null) throw new IllegalArgumentException("Selecciona una reserva de la tabla.");
        Reserva actual = reservas.findById(r.getId()).orElseThrow(() -> new IllegalArgumentException("La reserva ya no existe."));
        if (!RESERVADA.equals(actual.getEstado()))
            throw new IllegalArgumentException("Solo se pueden cancelar reservas en estado RESERVADA (esta está " + actual.getEstado() + ").");
        double pagado = Recargos.redondear(pagadoDe(actual.getId()));
        actual.setEstado(CANCELADA);
        reservas.save(actual);
        if (pagado > 0) {
            Pago reembolso = new Pago();
            reembolso.setReserva(actual);
            reembolso.setFechaPago(LocalDate.now());
            reembolso.setMonto(-pagado);
            reembolso.setMetodo("Reembolso");
            reembolso.setConcepto("Reembolso por cancelación de la reserva #" + actual.getId());
            pagos.save(reembolso);
        }
        return pagado;
    }

    public Contrato crearContrato(Reserva r) {
        if (r == null) throw new IllegalArgumentException("Selecciona una reserva.");
        Reserva actual = reservas.findById(r.getId()).orElseThrow(() -> new IllegalArgumentException("La reserva ya no existe."));
        if (CANCELADA.equals(actual.getEstado())) throw new IllegalArgumentException("La reserva está cancelada.");
        return contratos.findAll().stream()
                .filter(c -> c.getReserva().getId().equals(actual.getId()))
                .findFirst()
                .orElseGet(() -> {
                    Contrato c = new Contrato();
                    c.setReserva(actual);
                    c.setFechaFirma(LocalDate.now());
                    c.setCodigo(String.format("CT-%d-%05d", LocalDate.now().getYear(), actual.getId()));
                    if (RESERVADA.equals(actual.getEstado())) {
                        actual.setEstado(EN_ALQUILER);
                        reservas.save(actual);
                    }
                    return contratos.save(c);
                });
    }

    public double calcularRecargo(LocalDate fechaPrevista, LocalDate fechaReal, double tarifaDiaria) {
        return Recargos.recargo(Recargos.diasRetraso(fechaPrevista, fechaReal), tarifaDiaria, porcentajeRecargo);
    }

    private double pagadoDe(Long reservaId) {
        return pagos.findAll().stream().filter(p -> p.getReserva().getId().equals(reservaId)).mapToDouble(Pago::getMonto).sum();
    }

    private Map<Long, Double> pagadoPorReserva() {
        return pagos.findAll().stream()
                .collect(Collectors.groupingBy(p -> p.getReserva().getId(), Collectors.summingDouble(Pago::getMonto)));
    }

    private CuentaReserva cuentaDe(Reserva r, double pagadoBruto, LocalDate hoy) {
        if (CANCELADA.equals(r.getEstado())) {
            return new CuentaReserva(r, 0, 0, 0, 0, 0, ANULADA);
        }
        long retraso = 0;
        double recargo = 0;
        if (DEVUELTA.equals(r.getEstado())) {
            retraso = Recargos.diasRetraso(r.getFechaFin(), r.getFechaDevolucionReal());
            recargo = r.getRecargo();
        } else if (r.estaAbierta()) {
            retraso = Recargos.diasRetraso(r.getFechaFin(), hoy);
            recargo = Recargos.recargo(retraso, r.getVehiculo().getTarifaDiaria(), porcentajeRecargo);
        }
        double pagado = Recargos.redondear(pagadoBruto);
        double saldo = Recargos.saldo(r.getTotalEstimado(), recargo, pagado);
        String estadoPago;
        if (saldo <= 0.004) estadoPago = PAGADO;
        else if (retraso > 0) estadoPago = EN_MORA;
        else if (pagado > 0.004) estadoPago = PARCIAL;
        else estadoPago = PENDIENTE;
        return new CuentaReserva(r, retraso, recargo, r.getTotalEstimado(), pagado, saldo, estadoPago);
    }

    @Transactional(readOnly = true)
    public List<CuentaReserva> cuentas() {
        Map<Long, Double> pagado = pagadoPorReserva();
        LocalDate hoy = LocalDate.now();
        return reservas.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(r -> cuentaDe(r, pagado.getOrDefault(r.getId(), 0.0), hoy))
                .toList();
    }

    @Transactional(readOnly = true)
    public CuentaReserva cuenta(Reserva r) {
        if (r == null) throw new IllegalArgumentException("Selecciona una reserva.");
        Reserva actual = reservas.findById(r.getId()).orElseThrow(() -> new IllegalArgumentException("La reserva ya no existe."));
        return cuentaDe(actual, pagadoDe(actual.getId()), LocalDate.now());
    }

    @Transactional(readOnly = true)
    public ResumenDevolucion resumenDevolucion(Reserva r, LocalDate fechaReal) {
        if (r == null) throw new IllegalArgumentException("Selecciona una reserva.");
        Reserva actual = reservas.findById(r.getId()).orElse(r);
        LocalDate real = fechaReal == null ? LocalDate.now() : fechaReal;
        long retraso = Recargos.diasRetraso(actual.getFechaFin(), real);
        double tarifa = actual.getVehiculo().getTarifaDiaria();
        double recargo = Recargos.recargo(retraso, tarifa, porcentajeRecargo);
        double pagado = pagadoDe(actual.getId());
        double saldo = Recargos.saldo(actual.getTotalEstimado(), recargo, pagado);
        return new ResumenDevolucion(actual.getFechaFin(), real, retraso, tarifa, Recargos.redondear(tarifa * porcentajeRecargo),
                recargo, actual.getTotalEstimado(), Recargos.redondear(pagado), saldo);
    }

    public ResumenDevolucion devolver(Reserva r, LocalDate fechaReal) {
        if (r == null) throw new IllegalArgumentException("Selecciona la reserva que se devuelve.");
        if (fechaReal == null) throw new IllegalArgumentException("Ingresa la fecha real de devolución.");
        Reserva actual = reservas.findById(r.getId()).orElseThrow(() -> new IllegalArgumentException("La reserva ya no existe."));
        if (!actual.estaAbierta()) throw new IllegalArgumentException("Esta reserva ya está cerrada (" + actual.getEstado() + ").");
        if (fechaReal.isBefore(actual.getFechaInicio()))
            throw new IllegalArgumentException("La devolución no puede ser anterior al inicio del alquiler.");
        if (fechaReal.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("La fecha real de devolución no puede ser futura: registra la devolución el día en que ocurre.");
        double recargo = calcularRecargo(actual.getFechaFin(), fechaReal, actual.getVehiculo().getTarifaDiaria());
        actual.setEstado(DEVUELTA);
        actual.setFechaDevolucionReal(fechaReal);
        actual.setRecargo(recargo);
        reservas.save(actual);
        return resumenDevolucion(actual, fechaReal);
    }

    @Transactional(readOnly = true)
    public double saldoPendiente(Reserva r) {
        if (r == null) return 0;
        return cuenta(r).saldo();
    }

    public Pago registrarPago(Reserva r, double monto, String metodo, String concepto) {
        if (r == null) throw new IllegalArgumentException("Selecciona la reserva a la que corresponde el pago.");
        if (monto <= 0) throw new IllegalArgumentException("El pago debe ser mayor que cero.");
        if (metodo == null || metodo.isBlank()) throw new IllegalArgumentException("Selecciona el método de pago.");
        if (concepto == null || concepto.isBlank()) throw new IllegalArgumentException("Indica el concepto del pago.");
        Reserva actual = reservas.findById(r.getId()).orElseThrow(() -> new IllegalArgumentException("La reserva ya no existe."));
        if (CANCELADA.equals(actual.getEstado()))
            throw new IllegalArgumentException("La reserva #" + actual.getId() + " está cancelada: no admite pagos.");
        double saldo = cuentaDe(actual, pagadoDe(actual.getId()), LocalDate.now()).saldo();
        if (monto > saldo + 0.005)
            throw new IllegalArgumentException(String.format(java.util.Locale.US, "El monto excede el saldo pendiente (S/ %.2f).", saldo));
        Pago p = new Pago();
        p.setReserva(actual);
        p.setFechaPago(LocalDate.now());
        p.setMonto(Recargos.redondear(monto));
        p.setMetodo(metodo);
        p.setConcepto(concepto.trim());
        return pagos.save(p);
    }

    @Transactional(readOnly = true)
    public ResumenPanel panel() {
        LocalDate hoy = LocalDate.now();
        long flota = vehiculos.findAll().stream().filter(Vehiculo::isActivo).count();
        long libres = disponibles(hoy, hoy).size();
        List<Reserva> todas = reservas.findAll();
        long activas = todas.stream().filter(Reserva::estaAbierta).count();
        long conRetraso = todas.stream().filter(r -> r.estaVencida(hoy)).count();
        Map<Long, Double> pagado = pagadoPorReserva();
        double ingresos = pagado.values().stream().mapToDouble(Double::doubleValue).sum();
        double porCobrar = todas.stream()
                .mapToDouble(r -> cuentaDe(r, pagado.getOrDefault(r.getId(), 0.0), hoy).saldo())
                .sum();
        return new ResumenPanel(flota, libres, activas, Recargos.redondear(ingresos), Recargos.redondear(porCobrar), conRetraso);
    }
}
