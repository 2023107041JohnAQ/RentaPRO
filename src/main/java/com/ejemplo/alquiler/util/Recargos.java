package com.ejemplo.alquiler.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * @author Jhon Alessandro Arpi Quispe
 */
public final class Recargos {

    private Recargos() {
    }

    public static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public static long diasRetraso(LocalDate fechaFin, LocalDate referencia) {
        if (fechaFin == null || referencia == null) return 0;
        return Math.max(0, ChronoUnit.DAYS.between(fechaFin, referencia));
    }

    public static double recargo(long diasRetraso, double tarifaDiaria, double porcentaje) {
        return redondear(Math.max(0, diasRetraso) * tarifaDiaria * porcentaje);
    }

    public static double saldo(double totalAlquiler, double recargo, double pagado) {
        return Math.max(0, redondear(totalAlquiler + recargo - pagado));
    }
}
