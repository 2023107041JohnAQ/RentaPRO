package com.ejemplo.alquiler.util;

/**
 * @author Julio Cesar Tutacano de la Cruz
 */
public final class NumeroLetras {

    private static final String[] UNIDADES = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
            "VEINTE", "VEINTIUNO", "VEINTIDÓS", "VEINTITRÉS", "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS", "VEINTISIETE",
            "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENAS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private NumeroLetras() {
    }

    public static String soles(double monto) {
        double positivo = Math.abs(monto);
        long entero = (long) Math.floor(positivo + 1e-9);
        int centimos = (int) Math.round((positivo - entero) * 100);
        if (centimos >= 100) {
            entero++;
            centimos = 0;
        }
        return convertir(entero) + " CON " + String.format("%02d", centimos) + "/100 SOLES";
    }

    public static String convertir(long n) {
        if (n == 0) return "CERO";
        StringBuilder sb = new StringBuilder();
        long millones = n / 1_000_000;
        long resto = n % 1_000_000;
        if (millones > 0) {
            sb.append(millones == 1 ? "UN MILLÓN" : hasta999999(millones, true) + " MILLONES");
        }
        if (resto > 0) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(hasta999999(resto, false));
        }
        return sb.toString();
    }

    private static String hasta999999(long n, boolean apocoparFinal) {
        StringBuilder sb = new StringBuilder();
        long miles = n / 1000;
        int resto = (int) (n % 1000);
        if (miles > 0) sb.append(miles == 1 ? "MIL" : menorQueMil((int) miles, true) + " MIL");
        if (resto > 0) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(menorQueMil(resto, apocoparFinal));
        }
        return sb.toString();
    }

    private static String menorQueMil(int n, boolean apocopar) {
        if (n == 100) return "CIEN";
        StringBuilder sb = new StringBuilder();
        int c = n / 100;
        int r = n % 100;
        if (c > 0) sb.append(CENTENAS[c]);
        if (r > 0) {
            if (sb.length() > 0) sb.append(' ');
            if (r < 30) {
                sb.append(UNIDADES[r]);
            } else {
                sb.append(DECENAS[r / 10]);
                if (r % 10 > 0) sb.append(" Y ").append(UNIDADES[r % 10]);
            }
        }
        String s = sb.toString();
        if (apocopar) {
            if (s.endsWith("VEINTIUNO")) return s.substring(0, s.length() - 9) + "VEINTIÚN";
            if (s.endsWith("UNO")) return s.substring(0, s.length() - 3) + "UN";
        }
        return s;
    }
}
