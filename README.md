# GESPA Backend

Backend Spring Boot para GESPA.

## Requisitos
- Java 21
- Maven 3.9+
- MySQL disponible

## Configuración
Revisar variables en `application.properties` / `application.yml` (BD, JWT, puerto).

Valores esperados en desarrollo:
- API: `http://localhost:8083`
- Health: `http://localhost:8083/api/health`

## Iniciar backend
Desde esta carpeta:

```bash
cd backend/gespa_backend
mvn spring-boot:run
```

También puede iniciarse desde IDE ejecutando `GespaBackendApplication`.

## Verificar que está arriba
```bash
curl -s http://localhost:8083/api/health
```
Respuesta esperada:
```json
{"status":"ok"}
```

## Cerrar ejecución
### Si corre en terminal
Presionar `Ctrl + C` en la terminal donde está ejecutándose.

### Si corre en IDE (Run/Debug)
Detener desde el botón Stop del IDE.

### Si el puerto quedó ocupado
Identificar proceso:
```bash
lsof -i :8083
```
Cerrar proceso por PID:
```bash
kill -9 <PID>
```

## Troubleshooting rápido

### 1) `Exit Code: 1` al iniciar en Debug
- Revisar consola completa del backend (stacktrace real).
- Validar que MySQL esté operativo y credenciales correctas.
- Validar que el puerto 8083 no esté tomado.
- Probar iniciar sin debug con `mvn spring-boot:run`.

### 2) Frontend muestra 401 al login/acciones
- Confirmar que el backend real de este proyecto esté activo en 8083.
- Verificar que no haya otro servicio mock respondiendo en el mismo puerto.
- Limpiar tokens del navegador (`localStorage`) y volver a iniciar sesión.

### 3) Error de endpoints no encontrados
- Confirmar prefijo `/api/...`.
- Probar endpoint directo con `curl`.

## Endpoints base útiles
- `GET /api/health`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`

## Notas de operación
- Si se cambia el puerto backend, actualizar también `VITE_API_BASE_URL` en el frontend.
- Usar siempre el mismo backend activo para evitar confusión entre instancias (real vs mock).
