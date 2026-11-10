# Security Service

## Correo de bienvenida

Usá `.env.example` para configurar el proceso y `.env.test.example` para habilitar el correo contra Mailpit en desarrollo. Cuando se usa SMTP autenticado, configurá las credenciales fuera de Git y habilitá `SMTP_STARTTLS=true` para exigir TLS. El correo y sus métricas están deshabilitados por defecto.

El registro y la fila de outbox se confirman en la misma transacción. El worker consulta hasta 20 filas cada cinco segundos de forma predeterminada. Realiza hasta cinco intentos con esperas de 15, 30, 60 y 120 segundos. Si un proceso se interrumpe después de tomar una fila, otro puede reclamarla tras 120 segundos. SMTP puede aceptar el mensaje antes de que se confirme la actualización a `SENT`. El identificador de notificación se conserva en los reintentos para correlacionar una entrega duplicada.

El indicador `notificationOutbox` verifica la base. Un fallo de SMTP se observa en la cola y en los reintentos. Las métricas `logitrack.notification.outbox.pending` y `logitrack.notification.outbox.failed` se registran en Micrometer. Los logs del worker incluyen el identificador y el estado. Para revisar los totales sin consultar destinatarios, ejecutá:

```sql
SELECT status, COUNT(*) AS total
FROM outbox_notifications
GROUP BY status;
```

## Verificación

Ejecutá `bash mvnw --batch-mode clean verify` con Java 25. Para probar la entrega SMTP, iniciá Mailpit en loopback y exportá `MAILPIT_SMOKE_PORT` con el puerto SMTP antes de ejecutar Maven. La bandeja debe contener el asunto «Bienvenida a LogiTrack».

`compose.mail-test.yml` levanta Security, MySQL y Mailpit sin depender de Gateway, Eureka ni RabbitMQ. Definí `MAIL_TEST_DB_PASSWORD` con una contraseña descartable y `MAIL_TEST_KEY_DIR` con la ruta absoluta a un directorio de prueba que contenga `private.pem` (RSA PKCS#8) y `public.pem` (X.509), legibles desde el contenedor. Ejecutá `docker compose -f compose.mail-test.yml up --build -d --wait` y consultá los puertos asignados con `docker compose -f compose.mail-test.yml port security 8081` y `docker compose -f compose.mail-test.yml port mailpit 8025`.

Detené Mailpit, registrá un cliente de prueba con `POST /auth/register` y verificá el 201 y la fila `PENDING`. Volvé a iniciar Mailpit y comprobá la transición a `SENT` y el mensaje en la bandeja. Al terminar, ejecutá `docker compose -f compose.mail-test.yml down`.
