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

## Despliegue en Railway + Supabase

### Estado actual para despliegue
- Desarrollo local sigue usando MySQL.
- Producción quedó preparada para usar PostgreSQL por variables de entorno.
- El puerto ya toma `PORT`, compatible con Railway.

### Variables recomendadas en Railway
- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL=jdbc:postgresql://<host-supabase>:5432/postgres?sslmode=require`
- `DB_USER=<usuario-supabase>`
- `DB_PASSWORD=<password-supabase>`
- `JWT_SECRET=<secreto-largo-y-unico>`
- `CORS_ALLOWED_ORIGINS=https://tu-frontend.com`
- `JPA_DDL_AUTO=update`

Opcionales:
- `JWT_EXPIRATION_SECONDS=3600`
- `JWT_REFRESH_EXPIRATION_SECONDS=1209600`
- `DB_POOL_MAX_SIZE=5`
- `DB_POOL_MIN_IDLE=1`

### Qué conexión usar en Supabase
- Preferir el pooler de Supabase para producción.
- Convertir la cadena a formato JDBC.
- Ejemplo:

`jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require`

Si Supabase entrega usuario con sufijo de proyecto, usarlo completo en `DB_USER`.

### Flujo sugerido
1. Subir este repositorio a GitHub.
2. En Railway, crear un proyecto desde el repo.
3. Configurar las variables anteriores.
4. Definir healthcheck en `/api/health`.
5. Desplegar.
6. Probar:
	- `/api/health`
	- `/swagger-ui.html`

### Recomendación importante
Para el primer despliegue usar `JPA_DDL_AUTO=update`.
Cuando el esquema ya esté estable, cambiar a `validate`.

### Problemas locales detectados
- `mvn spring-boot:run` devolvió código `127`: en tu Mac falta `mvn` en PATH.
- Si quieres ejecutar localmente antes de desplegar, instala Maven o agrega Maven Wrapper.
- El fallo al ejecutar desde IDE probablemente está relacionado con configuración de base de datos o variables faltantes.
