# GESPA Backend — Documentación de desarrollo

## 1) Objetivo
Definir cómo se desarrollará el backend de **GESPA** en `backend/gespa_backend`, alineado al ERS (`documentacion/documento_ers_duoc_uc.docx`) y tomando como referencia **solo la estructura** del proyecto `backend/tejedoraypunto_backend`.

---

## 2) Alcance backend (según ERS)
El backend cubrirá el MVP con estos dominios:
- Autenticación y autorización por roles (`professional`, `patient`, `admin`).
- Gestión de pacientes y antecedentes clínicos básicos.
- Gestión de citas con estados y validaciones de horario.
- Registro de notas clínicas y documentos asociados.
- Portal paciente con acceso restringido a información autorizada.
- Consentimientos y auditoría de acciones críticas.

Reglas clave consideradas:
- Un paciente se asocia a un profesional responsable.
- Una cita no puede terminar antes de iniciar.
- Solo usuarios autorizados acceden a datos clínicos sensibles.
- Toda acción crítica debe dejar trazabilidad (auditoría).

---

## 3) Stack propuesto
- **Lenguaje:** Java 21
- **Framework:** Spring Boot 3.x
- **Persistencia:** Spring Data JPA + MySQL
- **Seguridad:** Spring Security + JWT
- **Documentación API:** springdoc-openapi (Swagger)
- **Build:** Maven
- **Testing:** JUnit 5 + Mockito + Spring Boot Test
- **Despliegue:** Railway (API) + MySQL en desarrollo local con XAMPP

---

## 4) Estructura del proyecto (base)
Estructura recomendada para `backend/gespa_backend` (siguiendo el patrón del repo guía):

```text
gespa_backend/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/tbtha/gespa_backend/
    │   │   ├── GespaBackendApplication.java
    │   │   ├── auth/
    │   │   ├── config/
    │   │   ├── controllers/
    │   │   ├── dtos/
    │   │   ├── entities/
    │   │   ├── exceptions/
    │   │   ├── repositories/
    │   │   ├── security/
    │   │   ├── services/
    │   │   └── webconfig/
    │   └── resources/
    │       ├── application.properties
    │       ├── application-dev.properties
    │       ├── application-prod.properties
    │       └── data.sql (opcional seeds)
    └── test/
        └── java/com/tbtha/gespa_backend/
```

> Nota: `dtos/` y `exceptions/` se agregan para mantener contrato API limpio y manejo uniforme de errores.

---

## 5) Modelo de capas
1. **controllers/**
   - Expone endpoints REST.
   - Valida input básico y delega a servicios.
2. **services/**
   - Implementa lógica de negocio.
   - Aplica reglas del ERS.
3. **repositories/**
   - Acceso a BD vía JPA.
4. **entities/**
   - Mapeo de tablas del modelo (usuarios, profesionales, pacientes, etc.).
5. **security/** + **auth/**
   - Login, emisión/validación de JWT, filtros, roles.
6. **config/** + **webconfig/**
   - Beans globales, CORS, serialización, timezone.

---

## 6) Módulos backend y endpoints base (MVP)

### 6.1 Auth
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

### 6.2 Usuarios / perfiles
- `GET /api/usuarios/me`
- `PUT /api/profesionales/{id}`
- `GET /api/profesionales/{id}`

### 6.3 Pacientes
- `POST /api/pacientes`
- `GET /api/pacientes`
- `GET /api/pacientes/{id}`
- `PUT /api/pacientes/{id}`
- `DELETE /api/pacientes/{id}` (borrado lógico recomendado)

### 6.4 Antecedentes
- `GET /api/pacientes/{id}/antecedentes`
- `PUT /api/pacientes/{id}/antecedentes`

### 6.5 Citas
- `POST /api/citas`
- `GET /api/citas`
- `GET /api/citas/{id}`
- `PUT /api/citas/{id}`
- `PATCH /api/citas/{id}/estado`

### 6.6 Notas clínicas
- `POST /api/pacientes/{id}/notas`
- `GET /api/pacientes/{id}/notas`
- `PUT /api/notas/{id}`

### 6.7 Evolución clínica
- `POST /api/pacientes/{id}/evolucion`
- `GET /api/pacientes/{id}/evolucion?indicador=peso`

### 6.8 Documentos
- `POST /api/pacientes/{id}/documentos`
- `GET /api/pacientes/{id}/documentos`
- `GET /api/documentos/{id}`
- `PATCH /api/documentos/{id}/compartir`

### 6.9 Consentimientos y auditoría
- `POST /api/pacientes/{id}/consentimientos`
- `PATCH /api/consentimientos/{id}/revocar`
- `GET /api/auditoria` (rol admin/profesional autorizado)

---

## 7) Seguridad y cumplimiento
- JWT con expiración corta + refresh token.
- Hash de contraseñas con BCrypt.
- Autorización por rol y por propiedad del recurso (paciente dueño / profesional asignado).
- Registro de auditoría para `LOGIN`, `EXPORT`, cambios clínicos y accesos críticos.
- CORS restringido por ambiente.
- Variables sensibles en entorno (`JWT_SECRET`, `DB_URL`, `DB_USER`, `DB_PASSWORD`).

---

## 8) Plan de desarrollo por fases

### Fase 1 — Base técnica
- Inicializar proyecto Spring Boot.
- Configurar perfiles `dev` y `prod`.
- Conexión MySQL.
- Configuración Swagger.

### Fase 2 — Seguridad
- Módulo `auth` + JWT.
- Roles y guardas de acceso.
- Endpoint `me`.

### Fase 3 — Núcleo clínico
- Pacientes, antecedentes, citas.
- Validaciones de reglas de negocio.

### Fase 4 — Registro clínico
- Notas, evolución, documentos.
- Compartición controlada para portal paciente.

### Fase 5 — Gobernanza
- Consentimientos y auditoría.
- Exportación básica y trazabilidad.

### Fase 6 — Calidad y despliegue
- Pruebas unitarias/integración.
- Hardening de seguridad.
- Deploy en Railway.

---

## 9) Criterios de terminado (Definition of Done)
Una historia backend se considera terminada cuando:
- Tiene endpoint documentado (Swagger).
- Cumple validaciones de negocio.
- Incluye control de acceso por rol.
- Registra auditoría si aplica.
- Incluye tests mínimos (unitario + integración del caso principal).
- Maneja errores con respuesta estándar (`code`, `message`, `timestamp`).

---

## 10) Convenciones recomendadas
- Prefijo rutas: `/api`.
- DTOs para request/response (no exponer entidades directamente).
- Fechas en ISO-8601 (`OffsetDateTime`).
- Errores HTTP:
  - `400` validación
  - `401` no autenticado
  - `403` sin permisos
  - `404` no encontrado
  - `409` conflicto de negocio
  - `500` error interno

---

## 11) Próximos pasos inmediatos
1. Crear el esqueleto de proyecto Maven + Spring Boot en `backend/gespa_backend`.
2. Configurar MySQL local en XAMPP y validar conexión.
3. Implementar autenticación JWT y roles.
4. Implementar módulo de pacientes y citas (prioridad MVP).
5. Conectar frontend con endpoints base y probar flujo completo.

---

## 12) Estado actual implementado
Actualmente el backend ya cuenta con:
- Esqueleto Spring Boot operativo con Maven.
- Configuración por perfiles (`dev`, `prod`, `test`).
- Seguridad base para desarrollo con usuario temporal configurable.
- Entidades JPA iniciales: `usuarios`, `profesionales`, `pacientes`, `citas`.
- Repositorios JPA para las entidades iniciales.
- Endpoints REST iniciales para:
   - `profesionales`
   - `pacientes`
   - `citas`
   - `health`
   - `auth/login` (mock temporal)
- Manejo global de errores (`400`, `404`, `409`, `500`).
- Pruebas locales con H2 para no depender de MySQL al compilar.

---

## 13) Referencias internas
- ERS: `documentacion/documento_ers_duoc_uc.docx`
- Modelo de BD: `documentacion/documento_modelo_bd.html`
- Diagrama BD: `documentacion/modelo_bd_diagrama.svg`
- Estructura guía: `backend/tejedoraypunto_backend`
