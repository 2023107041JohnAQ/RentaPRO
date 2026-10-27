package com.ejemplo.alquiler.util;

import java.util.List;

/**
 * @author Jhunior Eric Herrera Condori
 */
public final class PlantillaCorreo {

    private PlantillaCorreo() {
    }

    public record Fila(String etiqueta, String valor, boolean destacada) {
        public static Fila de(String etiqueta, String valor) {
            return new Fila(etiqueta, valor, false);
        }

        public static Fila resaltada(String etiqueta, String valor) {
            return new Fila(etiqueta, valor, true);
        }
    }

    public static String escapar(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String pie(String empresa, String telefono) {
        String base = "Este mensaje fue enviado automáticamente por " + empresa + ".";
        return telefono == null || telefono.isBlank() ? base : base + " Consultas: " + telefono.trim() + ".";
    }

    public static String html(String empresa, String titulo, String nombre, String intro, List<Fila> filas,
                              String cierre, String telefono) {
        StringBuilder f = new StringBuilder();
        for (Fila fila : filas) {
            String color = fila.destacada() ? "#d81b78" : "#1f2937";
            String peso = fila.destacada() ? "800" : "600";
            f.append("<tr><td style=\"padding:9px 0;border-bottom:1px solid #eef2f7;color:#6b7a8c;\">")
                    .append(escapar(fila.etiqueta()))
                    .append("</td><td style=\"padding:9px 0;border-bottom:1px solid #eef2f7;text-align:right;font-weight:")
                    .append(peso).append(";color:").append(color).append(";\">")
                    .append(escapar(fila.valor())).append("</td></tr>");
        }
        return "<div style=\"background:#f3f6fb;padding:24px;font-family:'Segoe UI',Arial,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width:560px;margin:0 auto;background:#ffffff;border:1px solid #e3e9f1;border-radius:12px;\">"
                + "<tr><td style=\"background:#0f1f3a;padding:18px 24px;color:#ffffff;font-size:20px;font-weight:bold;"
                + "border-radius:12px 12px 0 0;\">" + escapar(empresa) + "</td></tr>"
                + "<tr><td style=\"height:4px;background:#d81b78;font-size:0;line-height:0;\">&nbsp;</td></tr>"
                + "<tr><td style=\"padding:24px;color:#1f2937;font-size:14px;line-height:1.55;\">"
                + "<h2 style=\"margin:0 0 14px;color:#0f1f3a;font-size:19px;\">" + escapar(titulo) + "</h2>"
                + "<p style=\"margin:0 0 10px;\">Hola " + escapar(nombre) + ",</p>"
                + "<p style=\"margin:0 0 10px;\">" + escapar(intro) + "</p>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"border-collapse:collapse;margin:14px 0;font-size:14px;\">" + f + "</table>"
                + (cierre == null || cierre.isBlank() ? "" : "<p style=\"margin:10px 0 0;\">" + escapar(cierre) + "</p>")
                + "</td></tr>"
                + "<tr><td style=\"background:#f3f6fb;padding:14px 24px;color:#6b7a8c;font-size:12px;"
                + "border-radius:0 0 12px 12px;\">" + escapar(pie(empresa, telefono)) + "</td></tr>"
                + "</table></div>";
    }

    public static String texto(String empresa, String titulo, String nombre, String intro, List<Fila> filas,
                               String cierre, String telefono) {
        StringBuilder sb = new StringBuilder();
        sb.append(titulo).append("\n\nHola ").append(nombre).append(",\n\n").append(intro).append("\n\n");
        for (Fila fila : filas) sb.append(fila.etiqueta()).append(": ").append(fila.valor()).append('\n');
        if (cierre != null && !cierre.isBlank()) sb.append('\n').append(cierre).append('\n');
        sb.append("\n--\n").append(pie(empresa, telefono)).append('\n');
        return sb.toString();
    }
}
