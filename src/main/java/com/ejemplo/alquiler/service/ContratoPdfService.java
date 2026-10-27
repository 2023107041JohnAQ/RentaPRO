package com.ejemplo.alquiler.service;

import com.ejemplo.alquiler.model.Cliente;
import com.ejemplo.alquiler.model.Contrato;
import com.ejemplo.alquiler.model.Reserva;
import com.ejemplo.alquiler.model.Vehiculo;
import com.ejemplo.alquiler.service.AlquilerService.CuentaReserva;
import com.ejemplo.alquiler.util.Formato;
import com.ejemplo.alquiler.util.NumeroLetras;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * @author Julio Cesar Tutacano de la Cruz
 */
@Service
public class ContratoPdfService {

    private static final Color AZUL = new Color(0x14, 0x2D, 0x4E);
    private static final Color ACENTO = new Color(0x17, 0x69, 0xAA);
    private static final Color GRIS = new Color(0xF1, 0xF5, 0xF9);
    private static final Color BORDE = new Color(0xC3, 0xD0, 0xDE);
    private static final Color TEXTO = new Color(0x1F, 0x29, 0x37);
    private static final Color SUAVE = new Color(0xD4, 0xE1, 0xEF);
    private static final Color ROJO = new Color(0xB3, 0x26, 0x1E);
    private static final Color VERDE = new Color(0x1B, 0x6B, 0x3A);
    private static final Color ROJO_FONDO = new Color(0xFD, 0xEC, 0xEC);
    private static final Color VERDE_FONDO = new Color(0xE6, 0xF6, 0xEC);

    private static final Font F_MARCA = fuente(24, Font.BOLD, Color.WHITE);
    private static final Font F_MARCA_MINI = fuente(14, Font.BOLD, Color.WHITE);
    private static final Font F_LEMA = fuente(9, Font.NORMAL, SUAVE);
    private static final Font F_DOC = fuente(15, Font.BOLD, Color.WHITE);
    private static final Font F_DOC_DATO = fuente(9, Font.NORMAL, SUAVE);
    private static final Font F_SECCION = fuente(10, Font.BOLD, Color.WHITE);
    private static final Font F_ETIQUETA = fuente(8.5f, Font.BOLD, AZUL);
    private static final Font F_VALOR = fuente(9.5f, Font.NORMAL, TEXTO);
    private static final Font F_CLAUSULA = fuente(8.5f, Font.NORMAL, TEXTO);
    private static final Font F_CLAUSULA_B = fuente(8.5f, Font.BOLD, AZUL);
    private static final Font F_NOTA = fuente(8, Font.ITALIC, new Color(0x55, 0x65, 0x77));
    private static final Font F_TOTAL = fuente(12, Font.BOLD, AZUL);
    private static final Font F_PIE = fuente(8, Font.NORMAL, new Color(0x6B, 0x7A, 0x8C));

    private static final DateTimeFormatter HORA_ARCHIVO = DateTimeFormatter.ofPattern("HHmmss");

    @Value("${rentapro.recargo.porcentaje:0.25}")
    private double porcentajeRecargo;

    private static Font fuente(float tamano, int estilo, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, tamano, estilo, color);
    }

    public Path carpeta() {
        return Path.of(System.getProperty("user.home"), "RentaPro", "contratos");
    }

    public Path generar(Contrato contrato, CuentaReserva cuenta) throws Exception {
        byte[] datos = generarBytes(contrato, cuenta);
        Files.createDirectories(carpeta());
        Path archivo = carpeta().resolve(contrato.getCodigo() + ".pdf");
        try {
            Files.write(archivo, datos);
            return archivo;
        } catch (IOException bloqueado) {
            Path alterno = carpeta().resolve(contrato.getCodigo() + "-" + LocalTime.now().format(HORA_ARCHIVO) + ".pdf");
            Files.write(alterno, datos);
            return alterno;
        }
    }

    public Path exportar(Contrato contrato, CuentaReserva cuenta, Path destino) throws Exception {
        byte[] datos = generarBytes(contrato, cuenta);
        Path padre = destino.toAbsolutePath().getParent();
        if (padre != null) Files.createDirectories(padre);
        Files.write(destino, datos);
        return destino;
    }

    public String nombreSugerido(Contrato contrato) {
        return "Contrato " + contrato.getCodigo() + ".pdf";
    }

    public byte[] generarBytes(Contrato contrato, CuentaReserva cuenta) throws Exception {
        if (contrato == null || cuenta == null) throw new IllegalArgumentException("Falta el contrato o el estado de cuenta.");
        Reserva reserva = contrato.getReserva();
        Cliente cliente = reserva.getCliente();
        Vehiculo vehiculo = reserva.getVehiculo();
        LocalDate hoy = LocalDate.now();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 52);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, salida);
            writer.setPageEvent(new Pie(contrato.getCodigo()));
            doc.addTitle("Contrato de alquiler " + contrato.getCodigo());
            doc.addSubject("Contrato de alquiler de " + vehiculo.nombreCompleto() + " (" + vehiculo.getPlaca() + ")");
            doc.addAuthor("RentaPro");
            doc.addCreator("RentaPro");
            doc.open();

            doc.add(cabecera(contrato, reserva));
            doc.add(espacio(10));

            doc.add(seccion("I.  DATOS DEL ARRENDATARIO (CLIENTE)"));
            doc.add(grilla(new String[][]{
                    {"Nombres / Razón social", cliente.getNombres(), "DNI / RUC", cliente.getDocumento()},
                    {"Teléfono", cliente.getTelefono(), "Correo electrónico", vacio(cliente.getCorreo())},
                    {"Licencia de conducir N°", texto(cliente.getLicencia()), "Categoría / Vigencia", ""},
                    {"Dirección", texto(cliente.getDireccion())}
            }, 22));
            doc.add(espacio(10));

            doc.add(seccion("II.  DATOS DEL VEHÍCULO"));
            doc.add(bloqueVehiculo(vehiculo));
            doc.add(espacio(10));

            doc.add(seccion("III.  PERIODO Y CONDICIONES ECONÓMICAS"));
            doc.add(grilla(new String[][]{
                    {"Fecha de inicio", Formato.fecha(reserva.getFechaInicio()), "Fecha de devolución pactada", Formato.fecha(reserva.getFechaFin())},
                    {"Duración", reserva.dias() + " día(s)", "Fecha de firma", Formato.fecha(contrato.getFechaFirma())}
            }, 22));
            doc.add(cuadroEconomico(reserva, vehiculo, cuenta));
            doc.add(espacio(10));

            doc.add(seccion("IV.  ACTA DE ENTREGA DEL VEHÍCULO"));
            doc.add(grilla(new String[][]{
                    {"Kilometraje inicial", "", "Kilometraje de devolución", ""},
                    {"Nivel de combustible", "[   ] 1/4     [   ] 1/2     [   ] 3/4     [   ] Lleno"},
                    {"Accesorios entregados", "", "Daños previos", ""}
            }, 24));

            doc.newPage();
            doc.add(cabeceraCompacta(contrato));
            doc.add(espacio(8));
            doc.add(seccion("V.  CLÁUSULAS Y CONDICIONES"));
            doc.add(espacio(6));
            agregarClausulas(doc, reserva, vehiculo);
            doc.add(espacio(6));
            doc.add(seccion("VI.  ESTADO DE CUENTA AL " + Formato.fecha(hoy)));
            doc.add(estadoDeCuenta(cuenta));
            doc.add(firmas(cliente));
        } finally {
            if (doc.isOpen()) doc.close();
        }
        return salida.toByteArray();
    }

    private PdfPTable cabecera(Contrato contrato, Reserva reserva) {
        PdfPTable tabla = new PdfPTable(new float[]{3f, 2.4f});
        tabla.setWidthPercentage(100);

        PdfPCell izquierda = new PdfPCell();
        izquierda.setBackgroundColor(AZUL);
        izquierda.setBorder(Rectangle.NO_BORDER);
        izquierda.setPadding(14);
        izquierda.addElement(new Paragraph("RentaPro", F_MARCA));
        izquierda.addElement(new Paragraph("Alquiler de vehículos y motos", F_LEMA));

        PdfPCell derecha = new PdfPCell();
        derecha.setBackgroundColor(AZUL);
        derecha.setBorder(Rectangle.NO_BORDER);
        derecha.setPadding(14);
        Paragraph titulo = new Paragraph("CONTRATO DE ALQUILER", F_DOC);
        titulo.setAlignment(Element.ALIGN_RIGHT);
        Paragraph codigo = new Paragraph("N° " + contrato.getCodigo(), F_DOC_DATO);
        codigo.setAlignment(Element.ALIGN_RIGHT);
        Paragraph fecha = new Paragraph("Fecha de firma: " + Formato.fecha(contrato.getFechaFirma()) + "   ·   Reserva #" + reserva.getId(), F_DOC_DATO);
        fecha.setAlignment(Element.ALIGN_RIGHT);
        derecha.addElement(titulo);
        derecha.addElement(codigo);
        derecha.addElement(fecha);

        tabla.addCell(izquierda);
        tabla.addCell(derecha);

        PdfPCell franja = new PdfPCell(new Phrase(" ", fuente(2, Font.NORMAL, Color.WHITE)));
        franja.setColspan(2);
        franja.setBackgroundColor(ACENTO);
        franja.setBorder(Rectangle.NO_BORDER);
        franja.setFixedHeight(5);
        tabla.addCell(franja);
        return tabla;
    }

    private PdfPTable cabeceraCompacta(Contrato contrato) {
        PdfPTable tabla = new PdfPTable(new float[]{3f, 2.4f});
        tabla.setWidthPercentage(100);

        PdfPCell izquierda = new PdfPCell(new Phrase("RentaPro", F_MARCA_MINI));
        izquierda.setBackgroundColor(AZUL);
        izquierda.setBorder(Rectangle.NO_BORDER);
        izquierda.setPadding(8);
        izquierda.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell derecha = new PdfPCell(new Phrase("CONTRATO DE ALQUILER  ·  N° " + contrato.getCodigo(), F_DOC_DATO));
        derecha.setBackgroundColor(AZUL);
        derecha.setBorder(Rectangle.NO_BORDER);
        derecha.setPadding(8);
        derecha.setHorizontalAlignment(Element.ALIGN_RIGHT);
        derecha.setVerticalAlignment(Element.ALIGN_MIDDLE);

        tabla.addCell(izquierda);
        tabla.addCell(derecha);

        PdfPCell franja = new PdfPCell(new Phrase(" ", fuente(2, Font.NORMAL, Color.WHITE)));
        franja.setColspan(2);
        franja.setBackgroundColor(ACENTO);
        franja.setBorder(Rectangle.NO_BORDER);
        franja.setFixedHeight(4);
        tabla.addCell(franja);
        return tabla;
    }

    private PdfPTable seccion(String titulo) {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell(new Phrase(titulo, F_SECCION));
        celda.setBackgroundColor(ACENTO);
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPaddingTop(5);
        celda.setPaddingBottom(5);
        celda.setPaddingLeft(8);
        tabla.addCell(celda);
        return tabla;
    }

    private PdfPTable grilla(String[][] filas, float altoMinimo) {
        PdfPTable tabla = new PdfPTable(new float[]{1.35f, 2f, 1.45f, 2f});
        tabla.setWidthPercentage(100);
        for (String[] fila : filas) {
            tabla.addCell(celdaEtiqueta(fila[0], altoMinimo));
            PdfPCell valor = celdaValor(fila[1], altoMinimo);
            if (fila.length == 2) valor.setColspan(3);
            tabla.addCell(valor);
            if (fila.length == 4) {
                tabla.addCell(celdaEtiqueta(fila[2], altoMinimo));
                tabla.addCell(celdaValor(fila[3], altoMinimo));
            }
        }
        return tabla;
    }

    private PdfPTable bloqueVehiculo(Vehiculo v) {
        PdfPTable externa = new PdfPTable(new float[]{1.5f, 3.6f});
        externa.setWidthPercentage(100);
        externa.addCell(celdaFoto(v));

        PdfPTable datos = grilla(new String[][]{
                {"Placa / código", v.getPlaca(), "Tipo", v.getTipo()},
                {"Marca", v.getMarca(), "Modelo", v.getModelo()},
                {"Tarifa diaria", Formato.moneda(v.getTarifaDiaria()), "Categoría", v.esMoto() ? "Motocicleta" : "Vehículo"}
        }, 30);
        PdfPCell contenedor = new PdfPCell(datos);
        contenedor.setBorder(Rectangle.NO_BORDER);
        contenedor.setPadding(0);
        externa.addCell(contenedor);
        return externa;
    }

    private PdfPCell celdaFoto(Vehiculo v) {
        PdfPCell celda = new PdfPCell();
        celda.setBorderColor(BORDE);
        celda.setBorderWidth(0.6f);
        celda.setPadding(6);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        try {
            if (v.getImagenPath() != null && Files.isRegularFile(Path.of(v.getImagenPath()))) {
                Image imagen = Image.getInstance(v.getImagenPath());
                imagen.scaleToFit(150, 100);
                imagen.setAlignment(Element.ALIGN_CENTER);
                celda.addElement(imagen);
                return celda;
            }
        } catch (Exception ignorada) {
        }
        Paragraph sinFoto = new Paragraph("\n\nSin fotografía", F_PIE);
        sinFoto.setAlignment(Element.ALIGN_CENTER);
        celda.addElement(sinFoto);
        return celda;
    }

    private PdfPTable cuadroEconomico(Reserva reserva, Vehiculo vehiculo, CuentaReserva cuenta) {
        PdfPTable tabla = new PdfPTable(new float[]{4.2f, 1.2f, 1.8f, 1.8f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(6);

        for (String titulo : new String[]{"Descripción", "Días", "Precio por día", "Subtotal"}) {
            PdfPCell c = celda(titulo, fuente(8.5f, Font.BOLD, Color.WHITE), AZUL, Element.ALIGN_CENTER);
            tabla.addCell(c);
        }
        tabla.addCell(celda("Alquiler de " + vehiculo.getTipo().toLowerCase(Locale.ROOT) + " " + vehiculo.nombreCompleto()
                + " (" + vehiculo.getPlaca() + ")", F_VALOR, Color.WHITE, Element.ALIGN_LEFT));
        tabla.addCell(celda(String.valueOf(reserva.dias()), F_VALOR, Color.WHITE, Element.ALIGN_CENTER));
        tabla.addCell(celda(Formato.moneda(vehiculo.getTarifaDiaria()), F_VALOR, Color.WHITE, Element.ALIGN_RIGHT));
        tabla.addCell(celda(Formato.moneda(reserva.getTotalEstimado()), F_VALOR, Color.WHITE, Element.ALIGN_RIGHT));

        double porDia = vehiculo.getTarifaDiaria() * porcentajeRecargo;
        boolean conRecargo = cuenta.recargo() > 0;
        if (conRecargo) {
            tabla.addCell(celda("Recargo por devolución tardía (" + Math.round(porcentajeRecargo * 100) + "% de la tarifa diaria por día de retraso)",
                    fuente(9.5f, Font.NORMAL, ROJO), ROJO_FONDO, Element.ALIGN_LEFT));
            tabla.addCell(celda(String.valueOf(cuenta.diasRetraso()), fuente(9.5f, Font.NORMAL, ROJO), ROJO_FONDO, Element.ALIGN_CENTER));
            tabla.addCell(celda(Formato.moneda(porDia), fuente(9.5f, Font.NORMAL, ROJO), ROJO_FONDO, Element.ALIGN_RIGHT));
            tabla.addCell(celda(Formato.moneda(cuenta.recargo()), fuente(9.5f, Font.NORMAL, ROJO), ROJO_FONDO, Element.ALIGN_RIGHT));
        }

        PdfPCell etiquetaTotal = celda(conRecargo ? "TOTAL A PAGAR (ALQUILER + RECARGO)" : "TOTAL ESTIMADO DEL ALQUILER", F_ETIQUETA, GRIS, Element.ALIGN_RIGHT);
        etiquetaTotal.setColspan(3);
        tabla.addCell(etiquetaTotal);
        tabla.addCell(celda(Formato.moneda(cuenta.totalConRecargo()), F_TOTAL, GRIS, Element.ALIGN_RIGHT));

        PdfPCell son = celda("SON: " + NumeroLetras.soles(cuenta.totalConRecargo()), F_ETIQUETA, Color.WHITE, Element.ALIGN_LEFT);
        son.setColspan(4);
        tabla.addCell(son);

        PdfPCell nota = new PdfPCell(new Phrase("Recargo por devolución tardía: " + Math.round(porcentajeRecargo * 100) + "% de la tarifa diaria por cada día de retraso ("
                + Formato.moneda(porDia) + " por día). Los importes están expresados en soles (S/).", F_NOTA));
        nota.setColspan(4);
        nota.setBorder(Rectangle.NO_BORDER);
        nota.setPaddingTop(4);
        tabla.addCell(nota);
        return tabla;
    }

    private PdfPTable estadoDeCuenta(CuentaReserva cuenta) {
        boolean debe = cuenta.saldo() > 0.004;
        Color colorSaldo = debe ? ROJO : VERDE;
        Color fondoSaldo = debe ? ROJO_FONDO : VERDE_FONDO;

        PdfPTable tabla = new PdfPTable(new float[]{1f, 1f, 1f, 1f});
        tabla.setWidthPercentage(100);
        for (String titulo : new String[]{"Total a pagar", "Pagado a la fecha", "Saldo pendiente", "Estado de pago"}) {
            tabla.addCell(celda(titulo, F_ETIQUETA, GRIS, Element.ALIGN_CENTER));
        }
        tabla.addCell(celda(Formato.moneda(cuenta.totalConRecargo()), F_TOTAL, Color.WHITE, Element.ALIGN_CENTER));
        tabla.addCell(celda(Formato.moneda(cuenta.pagado()), F_TOTAL, Color.WHITE, Element.ALIGN_CENTER));
        tabla.addCell(celda(Formato.moneda(cuenta.saldo()), fuente(12, Font.BOLD, colorSaldo), fondoSaldo, Element.ALIGN_CENTER));
        tabla.addCell(celda(cuenta.estadoPago(), fuente(10, Font.BOLD, colorSaldo), fondoSaldo, Element.ALIGN_CENTER));

        String detalle = cuenta.conRetraso()
                ? "Se registran " + cuenta.diasRetraso() + " día(s) de retraso; el recargo de " + Formato.moneda(cuenta.recargo()) + " está incluido en el total."
                : "Sin recargos por retraso a la fecha de emisión.";
        PdfPCell nota = new PdfPCell(new Phrase(detalle + " Este estado de cuenta se actualiza cada vez que se emite el documento.", F_NOTA));
        nota.setColspan(4);
        nota.setBorder(Rectangle.NO_BORDER);
        nota.setPaddingTop(3);
        tabla.addCell(nota);
        return tabla;
    }

    private void agregarClausulas(Document doc, Reserva reserva, Vehiculo vehiculo) throws Exception {
        String pct = Math.round(porcentajeRecargo * 100) + "%";
        String[][] clausulas = {
                {"PRIMERA - OBJETO.", "El ARRENDADOR entrega en alquiler al ARRENDATARIO el vehículo descrito en la sección II, que recibe conforme y en las condiciones consignadas en el acta de entrega."},
                {"SEGUNDA - PLAZO.", "El alquiler rige desde el " + Formato.fechaLarga(reserva.getFechaInicio()) + " hasta el " + Formato.fechaLarga(reserva.getFechaFin())
                        + ", ambos días inclusive. Toda ampliación deberá solicitarse y confirmarse antes del vencimiento."},
                {"TERCERA - PRECIO Y PAGO.", "El ARRENDATARIO pagará el importe indicado en la sección III, según la forma y las fechas acordadas con el ARRENDADOR. Los pagos se registran en el sistema y el saldo vigente se detalla en la sección VI."},
                {"CUARTA - USO DEL VEHÍCULO.", "El vehículo será conducido únicamente por el ARRENDATARIO o por conductores autorizados con licencia vigente. Queda prohibido subarrendarlo, usarlo en competencias, para transporte oneroso o con fines ilícitos."},
                {"QUINTA - DEVOLUCIÓN Y RECARGO.", "El vehículo se devolverá en la fecha, hora y lugar pactados. Por cada día de retraso se aplicará un recargo equivalente al " + pct
                        + " de la tarifa diaria (" + Formato.moneda(vehiculo.getTarifaDiaria() * porcentajeRecargo) + " por día), sin perjuicio de las demás responsabilidades."},
                {"SEXTA - CUIDADO Y DAÑOS.", "El ARRENDATARIO responde por los daños, pérdidas, multas e infracciones ocurridos durante el alquiler, y se obliga a devolver el vehículo en el mismo estado en que lo recibió, salvo el desgaste normal por el uso."},
                {"SÉPTIMA - ACCIDENTES Y ROBO.", "En caso de accidente, robo o avería, el ARRENDATARIO debe avisar de inmediato al ARRENDADOR y a las autoridades competentes, y entregar copia de la denuncia o del parte policial."},
                {"OCTAVA - CONTROVERSIAS.", "Las partes procurarán resolver amistosamente cualquier diferencia. De no lograrlo, se someten a la jurisdicción de los jueces y tribunales competentes del lugar de celebración de este contrato."}
        };
        for (String[] c : clausulas) {
            Paragraph p = new Paragraph();
            p.add(new Chunk(c[0] + " ", F_CLAUSULA_B));
            p.add(new Chunk(c[1], F_CLAUSULA));
            p.setAlignment(Element.ALIGN_JUSTIFIED);
            p.setLeading(11.5f);
            p.setSpacingAfter(5);
            doc.add(p);
        }
        Paragraph cierre = new Paragraph("Leído el presente contrato y estando ambas partes conformes, lo firman por duplicado en señal de aceptación.", F_CLAUSULA);
        cierre.setSpacingBefore(2);
        doc.add(cierre);
    }

    private PdfPTable firmas(Cliente cliente) {
        PdfPTable tabla = new PdfPTable(new float[]{4f, 1f, 4f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(8);

        for (int i = 0; i < 3; i++) {
            PdfPCell espacio = new PdfPCell(new Phrase(" "));
            espacio.setBorder(Rectangle.NO_BORDER);
            espacio.setFixedHeight(62);
            tabla.addCell(espacio);
        }
        tabla.addCell(celdaFirma("EL ARRENDATARIO", cliente.getNombres(), "DNI / RUC: " + cliente.getDocumento()));
        PdfPCell separador = new PdfPCell(new Phrase(" "));
        separador.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(separador);
        tabla.addCell(celdaFirma("EL ARRENDADOR", "RentaPro", "Representante autorizado"));
        return tabla;
    }

    private PdfPCell celdaFirma(String rol, String nombre, String detalle) {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(Rectangle.TOP);
        celda.setBorderColor(AZUL);
        celda.setBorderWidth(1f);
        celda.setPaddingTop(6);
        Paragraph r = new Paragraph(rol, F_ETIQUETA);
        r.setAlignment(Element.ALIGN_CENTER);
        Paragraph n = new Paragraph(nombre, F_VALOR);
        n.setAlignment(Element.ALIGN_CENTER);
        Paragraph d = new Paragraph(detalle, F_PIE);
        d.setAlignment(Element.ALIGN_CENTER);
        celda.addElement(r);
        celda.addElement(n);
        celda.addElement(d);
        return celda;
    }

    private PdfPCell celda(String texto, Font fuente, Color fondo, int alineacion) {
        PdfPCell c = new PdfPCell(new Phrase(texto == null ? "" : texto, fuente));
        c.setBackgroundColor(fondo);
        c.setBorderColor(BORDE);
        c.setBorderWidth(0.6f);
        c.setPadding(6);
        c.setHorizontalAlignment(alineacion);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    private PdfPCell celdaEtiqueta(String texto, float altoMinimo) {
        PdfPCell c = celda(texto, F_ETIQUETA, GRIS, Element.ALIGN_LEFT);
        c.setMinimumHeight(altoMinimo);
        return c;
    }

    private PdfPCell celdaValor(String texto, float altoMinimo) {
        PdfPCell c = celda(texto, F_VALOR, Color.WHITE, Element.ALIGN_LEFT);
        c.setMinimumHeight(altoMinimo);
        return c;
    }

    private Paragraph espacio(float alto) {
        Paragraph p = new Paragraph(" ", fuente(1, Font.NORMAL, Color.WHITE));
        p.setLeading(alto);
        return p;
    }

    private static String vacio(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }

    private static String texto(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private static class Pie extends PdfPageEventHelper {
        private final String codigo;

        Pie(String codigo) {
            this.codigo = codigo;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.setLineWidth(0.5f);
            cb.setColorStroke(BORDE);
            cb.moveTo(document.left(), document.bottom() - 10);
            cb.lineTo(document.right(), document.bottom() - 10);
            cb.stroke();
            Phrase texto = new Phrase("RentaPro  ·  Contrato " + codigo + "  ·  Página " + writer.getPageNumber(), F_PIE);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, texto,
                    (document.right() + document.left()) / 2, document.bottom() - 24, 0);
        }
    }
}
