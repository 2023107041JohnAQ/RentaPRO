# RentaPro — Alquiler de vehículos y motos

Aplicación de escritorio con JavaFX + Spring Boot + Spring Data JPA + SQLite. No usa Lombok.
Requiere JDK 17 y Maven Wrapper.
Interfaz v4: barra lateral con íconos, barra superior con indicadores y un solo color de acento (se cambia en `-accent`, al inicio de `src/main/resources/css/app.css`).

## Abrir en IntelliJ IDEA
1. Extrae el ZIP en una carpeta sencilla, por ejemplo `C:\Proyectos\alquiler-vehiculos`.
2. IntelliJ IDEA → Open → selecciona la carpeta que contiene `pom.xml`.
3. Configura Project SDK en Java 17 (File → Project Structure → Project).
4. En Maven, pulsa *Reload All Maven Projects*.
5. Ejecuta en la Terminal de IntelliJ: `mvnw.cmd clean javafx:run` (Windows) o `./mvnw clean javafx:run` (Linux/macOS).

## Módulos
- **Vehículos y motos**: alta, edición y baja; tipo (auto, camioneta, moto, scooter...), tarifa diaria, **foto** de cada unidad, búsqueda y estado activo/inactivo.
- **Clientes**: alta, edición y baja con validación de DNI (8 dígitos) / RUC (11), teléfono y correo.
- **Clientes**: además de los datos básicos se guardan **dirección** y **N° de licencia** (opcionales); salen impresos en el contrato.
- **Disponibilidad y reservas**: tarjetas con foto de los vehículos libres en el rango de fechas; al hacer clic se eligen para la reserva. Muestra el total estimado y permite cancelar reservas. La tabla de reservas muestra el **estado de pago y el saldo** de cada una. Al cancelar, el vehículo queda libre al instante y, si el cliente ya había pagado, se registra el reembolso.
- **Contrato PDF**: documento A4 de dos páginas: datos del cliente, vehículo con foto, cuadro económico (con **recargo por retraso** e **importe en letras**), acta de entrega, cláusulas, **estado de cuenta** al día de emisión y firmas. Se guarda en `RentaPro/contratos` y se abre automáticamente. El botón **«Guardar copia en...»** permite descargarlo en la carpeta que elijas. Doble clic en un contrato lo abre.
- **Devolución y pagos**: tabla de **cuentas por cobrar** con alquiler, días de retraso, recargo, pagado y saldo (el recargo se acumula día a día mientras el vehículo no se devuelve). Al elegir la reserva y la fecha real se muestran los días de retraso, el **cargo adicional**, el total y el saldo por cobrar. Registro de pagos (efectivo, Yape, Plin, tarjeta, transferencia) con historial.
- **Notificaciones**: avisos por correo a los clientes (reserva, pago, recordatorio de devolución, retraso y saldo pendiente) con historial de envíos. Se configuran en la pestaña *Notificaciones*; Gmail requiere una «contraseña de aplicación».
- **Reportes**: cifras del periodo, gráfico mensual, rentabilidad por vehículo, clientes en mora y exportación a Excel (CSV).
- **Mantenimiento**: SOAT, revisión técnica, seguro y servicios por vehículo, con alertas de vencimiento.
- **Respaldos**: respaldo diario automático de la base de datos, copia manual y restauración.

## Instalador para Windows
Para usar RentaPro sin IntelliJ ni Maven, mira `instalador/LEEME-INSTALADOR.txt`
(`instalador\crear-instalador.bat` crea una carpeta lista para usar; con `exe` crea un instalador).

## Actualización automática
Reservas, disponibilidad, saldos y cifras del encabezado se actualizan al instante tras pagar, cancelar, devolver o generar un contrato, al cambiar de pestaña y cada 20 segundos (por si cambia el día y aparecen retrasos nuevos).

## Regla de recargo
Por cada día de retraso se cobra el 25% de la tarifa diaria. Se cambia en `src/main/resources/application.properties`:
`rentapro.recargo.porcentaje=0.25`

El recargo cuenta desde el primer día vencido mientras la reserva siga abierta, de modo que el **saldo por cobrar** ya lo incluye antes de registrar la devolución. La fecha real de devolución no puede ser futura.

## Datos
- Base de datos SQLite: `rentapro.db` en la carpeta del usuario (en Windows `C:\Users\<usuario>\rentapro.db`). Hibernate agrega las columnas nuevas automáticamente; los datos anteriores se conservan.
- Fotos: `RentaPro/imagenes` dentro de la carpeta del usuario.
