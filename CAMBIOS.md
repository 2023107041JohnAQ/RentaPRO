# Cambios de esta versión

## Generador de contratos (PDF)
- Se genera primero en memoria y luego se guarda: si el PDF ya está abierto en el visor (Windows lo bloquea) ya no falla; se guarda con un sufijo de hora.
- **Guardar copia en...**: elige dónde descargar el PDF (abre en Descargas por defecto). Doble clic en un contrato lo abre.
- El PDF se abre con `HostServices` de JavaFX (más fiable que `java.awt.Desktop`) y, si no se puede, avisa la ruta en la barra de estado.
- Diseño: encabezado reducido en la página 2, cuadro económico con recargo por retraso y **importe en letras** (`SON: ... SOLES`), acta de entrega con nivel de combustible, **estado de cuenta** al día de emisión (total, pagado, saldo y estado de pago), fecha larga en la cláusula de plazo, línea y número de página en el pie, metadatos del documento.
- Dirección y N° de licencia del cliente ahora se capturan y salen impresos (antes quedaban en blanco).

## Pagos y saldos con retraso
- El recargo por retraso se acumula día a día en reservas abiertas vencidas y **forma parte del saldo pendiente** (antes solo se contaba al registrar la devolución).
- Nueva tabla **Cuentas por cobrar** (alquiler, retraso, recargo, pagado, saldo, estado) y detalle del saldo al registrar un pago.
- Estados de pago: PAGADO, PARCIAL, PENDIENTE, EN MORA, ANULADA.
- Nuevo indicador **Por cobrar** en el encabezado (se pone rojo si hay reservas en mora).
- Cálculo centralizado en `util/Recargos.java` (lo usan pantalla, servicio y contrato).

## Disponibilidad y reservas
- Tras pagar, cancelar, devolver o generar un contrato se recarga todo de inmediato; también al cambiar de pestaña y cada 20 segundos.
- La tabla de reservas muestra estado de pago y saldo.
- Cancelar una reserva con pagos registra un reembolso (pago negativo) para que lo cobrado y los saldos cuadren.
- Refrescar ya no pisa las fechas del formulario de reserva ni el monto que se está escribiendo.

## Validaciones
- No se pueden registrar pagos en reservas canceladas.
- La fecha real de devolución no puede ser futura.

## Rediseño de interfaz (v4)
- Barra lateral oscura con íconos (reemplaza las pestañas visibles; las pestañas siguen existiendo ocultas, así la lógica no cambia).
- Barra superior clara con el título de la página y los indicadores en tarjetas.
- Barra de búsqueda destacada para consultar disponibilidad, con botón de acento.
- Color de acento único (`-accent` al inicio de `app.css`), tablas con filas alternadas, tarjetas con sombra suave.
- Cambios en código: solo `MainController` (métodos `irAPestana` y `sincronizarMenu`).

## Pulido de interfaz (v4.1)
- Buscador en Clientes (documento, nombre, teléfono o correo).
- Mensajes de tabla vacía en todas las tablas.
- Tarjetas de disponibilidad más grandes, con foto más grande.
- Avisos y confirmaciones con el mismo estilo de la aplicación.
- Ícono de la aplicación (`resources/img/icono.png`), casillas, barras de desplazamiento y ayudas con el nuevo estilo.
- Ventana inicial 1300x840 (mínimo 1180 de ancho).

## Ajustes finales (v4.2)
- Logo corregido (RentaPro con «Pro» en el color de acento).
- Menú lateral: «Reservas» en vez del texto largo, íconos con ancho fijo para alinear los textos.
- Formularios con la columna de etiquetas más ancha.
- Todas las tablas reparten sus columnas por todo el ancho de la ventana.

## Avisos por correo a clientes (v5)
- Nueva pantalla **Notificaciones**: configuración del correo de salida (SMTP), qué avisos se envían, correo de prueba e historial de envíos.
- Avisos automáticos por correo: reserva creada o cancelada, pago registrado, recordatorio de devolución (el día antes y el mismo día), retraso en la devolución y saldo pendiente (se repiten cada 3 días).
- El envío ocurre en segundo plano; la pantalla nunca espera al servidor de correo. Cada intento queda en el historial (ENVIADO, ERROR u OMITIDO si el cliente no tiene correo válido).
- La revisión de recordatorios y retrasos corre mientras la aplicación esté abierta (a los 8 segundos de abrir y luego cada 30 minutos).
- Los avisos vienen **desactivados** por defecto: se activan en Notificaciones → «Enviar avisos a los clientes».
- Nueva dependencia: `spring-boot-starter-mail` (recarga Maven al abrir el proyecto).
- Nota: la contraseña del correo se guarda en `rentapro.db` (carpeta del usuario). Usa una «contraseña de aplicación», no tu contraseña normal.

## Versión 6: respaldos, reportes, mantenimiento e instalador
- **Respaldos**: respaldo automático diario de la base de datos (se conservan los últimos 15), respaldo manual, «Guardar copia en...» (USB, escritorio) y restauración (se aplica al volver a abrir; la base anterior se conserva).
- **Reportes**: ingresos cobrados, alquileres, ocupación de la flota, ticket promedio, recargos y por cobrar para el periodo elegido; gráfico de ingresos de los últimos 6 meses; rentabilidad por vehículo; clientes en mora; exportación a Excel (CSV).
- **Mantenimiento**: SOAT, revisión técnica, seguro y servicios por vehículo, con alertas 30 días antes del vencimiento. Indicador «Alertas» en la barra superior. Al renovar un documento, el anterior pasa a «RENOVADO».
- **Instalador de Windows**: carpeta `instalador/` con `crear-instalador.bat` (ver `LEEME-INSTALADOR.txt`).
- Al eliminar un vehículo sin reservas también se eliminan sus registros de mantenimiento.
- Menú lateral reorganizado (MENÚ / GESTIÓN / SISTEMA) y nuevas dependencias internas: ninguna (solo `spring-boot-starter-mail`, ya agregada en la v5).
