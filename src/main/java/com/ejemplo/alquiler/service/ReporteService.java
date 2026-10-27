package com.ejemplo.alquiler.service;

import com.ejemplo.alquiler.model.Pago;
import com.ejemplo.alquiler.model.Reserva;
import com.ejemplo.alquiler.model.Vehiculo;
import com.ejemplo.alquiler.service.AlquilerService.CuentaReserva;
import com.ejemplo.alquiler.util.Recargos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jhunior Eric Herrera Condori
 */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"};

    public record Resumen(double ingresosCobrados, long alquileres, long cancelaciones, double facturado, double recargos,
                          double porCobrar, double ocupacion, double ticketPromedio, long flotaActiva, long dias) {
    }

    public record FilaVehiculo(String vehiculo, String tipo, long reservas, long diasAlquilados, double ocupacion, double facturado) {
    }

    public record Mes(String etiqueta, double monto) {
    }

    public record FilaMora(String cliente, String telefono, String vehiculo, long diasRetraso, double recargo, double saldo) {
    }

    public record Reporte(LocalDate desde, LocalDate hasta, Resumen resumen, List<FilaVehiculo> vehiculos, List<Mes> meses,
                          List<FilaMora> mora) {
    }

    private final AlquilerService alquiler;

    public ReporteService(AlquilerService alquiler) {
        this.alquiler = alquiler;
    }

    public Reporte generar(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) throw new IllegalArgumentException("Selecciona las fechas «Desde» y «Hasta».");
        if (hasta.isBefore(desde)) throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial.");
        LocalDate hoy = LocalDate.now();
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;

        List<Vehiculo> vehiculos = alquiler.vehiculos();
        List<CuentaReserva> cuentas = alquiler.cuentas();
        List<Pago> pagos = alquiler.pagos();

        long flota = vehiculos.stream().filter(Vehiculo::isActivo).count();

        double facturado = 0;
        double recargos = 0;
        double porCobrar = 0;
        long alquileres = 0;
        long cancelaciones = 0;
        Map<Long, double[]> porVehiculo = new HashMap<>();
        for (CuentaReserva c : cuentas) {
            Reserva r = c.reserva();
            boolean enPeriodo = !r.getFechaInicio().isBefore(desde) && !r.getFechaInicio().isAfter(hasta);
            if (!enPeriodo) continue;
            if (AlquilerService.CANCELADA.equals(r.getEstado())) {
                cancelaciones++;
                continue;
            }
            alquileres++;
            double monto = c.totalAlquiler() + c.recargo();
            facturado += monto;
            recargos += c.recargo();
            porCobrar += c.saldo();
            double[] acum = porVehiculo.computeIfAbsent(r.getVehiculo().getId(), k -> new double[2]);
            acum[0] += 1;
            acum[1] += monto;
        }

        Map<Long, Long> diasPorVehiculo = new HashMap<>();
        long diasTotales = 0;
        for (CuentaReserva c : cuentas) {
            Reserva r = c.reserva();
            if (AlquilerService.CANCELADA.equals(r.getEstado())) continue;
            long d = solapados(r.getFechaInicio(), finEfectivo(r, hoy), desde, hasta);
            if (d <= 0) continue;
            diasTotales += d;
            diasPorVehiculo.merge(r.getVehiculo().getId(), d, Long::sum);
        }

        double cobrado = 0;
        for (Pago p : pagos) {
            if (!p.getFechaPago().isBefore(desde) && !p.getFechaPago().isAfter(hasta)) cobrado += p.getMonto();
        }

        double ocupacion = flota == 0 ? 0 : Math.min(100.0, diasTotales * 100.0 / (flota * dias));
        double ticket = alquileres == 0 ? 0 : facturado / alquileres;
        Resumen resumen = new Resumen(Recargos.redondear(cobrado), alquileres, cancelaciones, Recargos.redondear(facturado),
                Recargos.redondear(recargos), Recargos.redondear(porCobrar), Math.round(ocupacion * 10.0) / 10.0,
                Recargos.redondear(ticket), flota, dias);

        List<FilaVehiculo> filas = new ArrayList<>();
        for (Vehiculo v : vehiculos) {
            double[] acum = porVehiculo.get(v.getId());
            long d = diasPorVehiculo.getOrDefault(v.getId(), 0L);
            if (!v.isActivo() && acum == null && d == 0) continue;
            double ocup = Math.min(100.0, d * 100.0 / dias);
            filas.add(new FilaVehiculo(v.toString(), v.getTipo(), acum == null ? 0 : (long) acum[0], d,
                    Math.round(ocup * 10.0) / 10.0, Recargos.redondear(acum == null ? 0 : acum[1])));
        }
        filas.sort(Comparator.comparingDouble(FilaVehiculo::facturado).reversed().thenComparing(FilaVehiculo::vehiculo));

        Map<YearMonth, Double> porMes = new LinkedHashMap<>();
        YearMonth ultimo = YearMonth.from(hasta);
        for (int i = 5; i >= 0; i--) porMes.put(ultimo.minusMonths(i), 0.0);
        for (Pago p : pagos) porMes.computeIfPresent(YearMonth.from(p.getFechaPago()), (k, v) -> v + p.getMonto());
        List<Mes> meses = new ArrayList<>();
        porMes.forEach((k, v) -> meses.add(new Mes(MESES[k.getMonthValue() - 1] + " " + k.getYear(), Recargos.redondear(v))));

        List<FilaMora> mora = cuentas.stream()
                .filter(c -> AlquilerService.EN_MORA.equals(c.estadoPago()))
                .map(c -> new FilaMora(c.reserva().getCliente().getNombres(), c.reserva().getCliente().getTelefono(),
                        c.reserva().getVehiculo().toString(), c.diasRetraso(), c.recargo(), c.saldo()))
                .sorted(Comparator.comparingLong(FilaMora::diasRetraso).reversed())
                .toList();

        return new Reporte(desde, hasta, resumen, filas, meses, mora);
    }

    private static LocalDate finEfectivo(Reserva r, LocalDate hoy) {
        if (AlquilerService.DEVUELTA.equals(r.getEstado()) && r.getFechaDevolucionReal() != null) {
            return r.getFechaDevolucionReal().isBefore(r.getFechaInicio()) ? r.getFechaInicio() : r.getFechaDevolucionReal();
        }
        if (r.estaAbierta() && r.getFechaFin().isBefore(hoy)) return hoy;
        return r.getFechaFin();
    }

    private static long solapados(LocalDate ini, LocalDate fin, LocalDate desde, LocalDate hasta) {
        LocalDate a = ini.isAfter(desde) ? ini : desde;
        LocalDate b = fin.isBefore(hasta) ? fin : hasta;
        return b.isBefore(a) ? 0 : ChronoUnit.DAYS.between(a, b) + 1;
    }
}
