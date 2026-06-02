# INFORME-FINAL.md — Gráficas Mulberry
## Auditoría técnica completa · Informe ejecutivo consolidado

**Fecha:** 2026-06-02  
**Fases ejecutadas:** 1–7 (8 fases totales)  
**Auditor principal:** Claude Code  
**Agentes de apoyo:** Gemini (análisis arquitectónico), Codex (verificación técnica local)  
**Tests al cierre:** 72/72 ✅ · BUILD SUCCESS  
**Referencia completa:** `AUDITORIA.md`

---

## VEREDICTO GENERAL

El proyecto tiene una **base técnica sólida**. Stack coherente (Java 21 + JavaFX + SQLite JDBC), sin ORM ni dependencias innecesarias, PreparedStatements en todos los DAOs, BCrypt correctamente aplicado, sistema de permisos bien diseñado y transacciones explícitas en Sprint B. Los tests pasan en su totalidad.

**No hay vulnerabilidades críticas de pérdida de datos en el uso normal.**  
Sin embargo, existen **fallos de diseño de seguridad que deben corregirse** antes de que la aplicación sea utilizada por múltiples personas en un entorno de empresa real.

---

## HALLAZGOS POR PRIORIDAD

### P0 — Crítico (corregir antes del próximo uso multiusuario)

| ID | Hallazgo | Archivo | Riesgo real |
|----|----------|---------|-------------|
| **SEC-2** | `changePassword(userId, newPassword)` no verifica la contraseña actual. Cualquier código con acceso a un `userId` puede cambiar la contraseña de cualquier usuario, incluido un ADMINISTRADOR, sin saber la actual. | `AuthService.java:46` | Un usuario con sesión abierta puede ver su contraseña cambiada por quien tenga acceso físico al equipo. En empresa con empleados, esto es inaceptable. |

**Corrección exacta (validada por Gemini):**
```java
// Cambio por el propio usuario — requiere contraseña actual
public boolean changePassword(int userId, String oldPassword, String newPassword) {
    return userDAO.findById(userId)
        .filter(u -> BCrypt.checkpw(oldPassword, u.getPasswordHash()))
        .map(u -> {
            u.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            return userDAO.updateUser(u);
        }).orElse(false);
}

// Reset forzado por ADMINISTRADOR — método separado explícito
public boolean resetPasswordAdmin(int userId, String newPassword) {
    // Solo llamar desde flujos autorizados de ADMINISTRADOR
    return userDAO.findById(userId).map(u -> {
        u.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        return userDAO.updateUser(u);
    }).orElse(false);
}
```

---

### P1 — Alto (Sprint SEC — próxima semana)

| ID | Hallazgo | Archivo | Esfuerzo |
|----|----------|---------|----------|
| **SEC-1** | `UserDAO` usa `try (Connection conn = DatabaseManager.getConnection())`. Los 9 métodos cierran la conexión singleton al salir. Los DAOs transaccionales asumen que permanece abierta. Si `UserDAO` se llama en contexto transaccional → transacción rota silenciosamente. | `UserDAO.java` — 9 métodos | 30 min |
| **SEC-4** | Contraseña mínima: solo texto de prompt (`"mín. 6 caracteres"`), sin validación ejecutable. Un admin puede crearse con 1 carácter. El mínimo empresarial es 8. | `AdminSetupView.java`, `LoginView.java` | 15 min |
| **SEC-5** | `runMigrations()` silencia **todas** las `SQLException`. Un fallo real de migración pasa desapercibido, dejando la BD en estado inconsistente sin aviso. | `DatabaseManager.java:105` | 15 min |
| **SEC-6** | `UserDAO.map()` hace fallback a `UserRole.COMERCIAL` para roles desconocidos. Un valor corrupto en BD otorga permisos reales. Principio de mínimo privilegio: debe fallar, no asumir rol. | `UserDAO.java:143` | 10 min |

---

### P2 — Medio (Sprints COD + UI — planificar)

| ID | Hallazgo | Categoría | Archivo | Esfuerzo |
|----|----------|-----------|---------|----------|
| **ARCH-1** | Inconsistencia en ciclo de vida de la conexión singleton. `UserDAO` la cierra; los otros DAOs no. Corrección táctica: quitar try-with-resources del `Connection` en `UserDAO`. Solución definitiva: Refactor B2 (inyección de Connection). | Arquitectura | `UserDAO.java` | 30 min (táctica) |
| **COD-2** | `SQL_SELECT_LOW_STOCK` usa columna `stock` — la real es `stock_actual`. SQL incorrecto y dead code. Eliminar. | Código | `AppConstants.java:148` | 5 min |
| **UI-4** | `DashboardView` bypasa el sistema de temas con 5 colores hardcodeados inline. En modo oscuro pueden generar bajo contraste o choque visual. | UI/UX | `DashboardView.java:55-66` | 30 min |
| **UI-5** | `IAView` es el único módulo sin `CommandBar` estandarizado ni empty state. Los botones de acción (Limpiar, Exportar, Modelos) están dispersos o ausentes. | UI/UX | `IAView.java` | 1h |
| **SEC-3** | `LoginView` expone todos los usernames en un ComboBox sin autenticación. Enumeración de usuarios. En app local el riesgo es bajo pero es un descuido de diseño. | Seguridad | `LoginView.java:188` | 1h (cambio de UX) |

---

### P3 — Bajo / Backlog

| ID | Hallazgo | Categoría | Acción |
|----|----------|-----------|--------|
| **COD-1** | `AppConstants` es una god class (constantes SQL, IA, UI, PDF, colores, debug mezclados) | Código | Dividir en clases por dominio en Refactor futuro |
| **COD-3** | Inline styles en `LoginView` y `AppConstants.STYLE_BURBUJA_*` bypasan el sistema de temas | Código/UI | Mover a clases CSS en `styles.css` |
| **COD-4** | `SQL_SELECT_ALL_CLIENTES` — dead code con `SELECT *`. Eliminar junto con COD-2 | Código | 5 min |
| **UI-1** | Transición entre vistas `FadeTransition(150ms)` — perceptualmente muy rápida | UI/UX | Cambiar a `220ms` en `MainView.java:560` |
| **UI-2** | Parámetro `color` en `btn()` nunca se usa — dead code en 9 vistas | UI/UX | Eliminar parámetro `color` del método y todas las llamadas |
| **UI-3** | Tres sistemas de versión sin sincronía: pom/sidebar `13.5.0`, instalador `v3.3`, handoff `v3.9.1` | UI/UX | Leer versión desde `AppConstants.APP_VERSION` derivado de `pom.xml` |
| **AP-1** | `theme-mulberry.css` contiene overrides de componentes en lugar de solo variables — anti-patrón CSS (confirmado Gemini) | UI/CSS | Separar variables puras de overrides en Sprint UI-A |
| **ARCH-2** | Lista de tablas duplicada entre `ExportService` e `ImportBackupService` sin fuente única de verdad | Arquitectura | Mover a `DatabaseManager` como constante pública |
| **ARCH-4** | URL de Ollama hardcodeada (`http://localhost:11434`) — no configurable sin recompilar | Arquitectura | Leer de config de BD como las demás preferencias |
| **SEC-5** | Constantes `DEBUG_*` en `AppConstants` — ruido en producción | Código | Eliminar o mover a logger |

---

## LO QUE ESTÁ BIEN ✅

| Aspecto | Detalle |
|---------|---------|
| BCrypt con gensalt() | Correcto en cada hash — sin salt reutilizado |
| Respuesta de seguridad hasheada | BCrypt también en preguntas de seguridad |
| PreparedStatements en todos los DAOs | Sin vectores de inyección SQL |
| `quoteIdentifier()` + `requireSqlIdentifier()` | SQL dinámico correctamente protegido |
| `TABLAS_PERMITIDAS` + regex `STATEMENT_SEGURO` | Restauración de SQL bien filtrada |
| Transacciones explícitas Sprint B | Patrón `externalTx` correcto y coherente |
| `PRAGMA foreign_keys = ON` | Integridad referencial garantizada |
| SingleInstanceLock | Sin instancias múltiples accidentales |
| Sistema de temas CSS completo | 5 temas × 16 variables × light/dark — todos verificados |
| Tooltips en navegación y toolbar | Accesibilidad básica cubierta |
| FadeTransition en cambio de vista | Transiciones implementadas (150ms, ajustable) |
| Permisos granulares por módulo | 16 permisos × 4 roles bien diseñados |
| Exportación e importación robusta | CSV, PDF, Excel — sin inyecciones detectadas |
| OllamaService con timeout | No bloquea indefinidamente |
| 72/72 tests verdes | Suite de integración sólida |

---

## PLAN DE SPRINTS RECOMENDADO

### Sprint SEC *(máxima prioridad — 2h total)*

```
1. AuthService.java  — SEC-2: refactorizar changePassword + añadir resetPasswordAdmin
2. UserDAO.java      — SEC-1 / ARCH-1: quitar try-with-resources del Connection
3. UserDAO.java      — SEC-6: relanzar excepción en rol desconocido
4. DatabaseManager.java — SEC-5: filtrar solo "column already exists" en runMigrations
5. LoginView.java + AdminSetupView.java — SEC-4: validación mínimo 8 chars ejecutable
```

Criterio de éxito: `.\mvnw.cmd test` → 72/72 verdes + revisión manual de los 5 puntos.

---

### Sprint COD *(bajo riesgo, 45 min)*

```
1. AppConstants.java — COD-2: eliminar SQL_SELECT_LOW_STOCK
2. AppConstants.java — COD-4: eliminar SQL_SELECT_ALL_CLIENTES
3. AppConstants.java — COD-5: eliminar comentario "VERSIÓN DE ÚLTIMA GENERACIÓN"
4. AppConstants.java — eliminar DEBUG_GESTION_MODELOS y DEBUG_REDIR_OLLAMA
```

Criterio de éxito: `.\mvnw.cmd test` → 72/72 verdes (cambios solo eliminan dead code).

---

### Sprint UI-A *(solo CSS — sin riesgo de regresión en lógica, 1.5h)*

```
1. styles.css — añadir clases CSS: .link-label, .msg-error, .chat-bubble-user, .chat-bubble-ia
2. LoginView.java — sustituir setStyle() por getStyleClass().add()
3. theme-mulberry.css — separar variables de overrides de componentes (anti-patrón AP-1)
4. DashboardView.java — variables CSS -c-kpi-* para los 5 KPIs + override en .modo-oscuro
```

Criterio de éxito: los 5 temas mantienen apariencia correcta + dark mode sin colores chocantes.

---

### Sprint UI-B *(correcciones Java rápidas — 1h)*

```
1. MainView.java:560  — UI-1: FadeTransition 150 → 220ms
2. FacturasView.java + 8 vistas más — UI-2: eliminar parámetro color de btn()
3. MainView.java:245  — UI-3: leer versión de AppConstants.APP_VERSION (derivado de pom)
4. AppConstants.java  — añadir constante APP_VERSION
```

Criterio de éxito: `.\mvnw.cmd clean compile` sin errores + navegación visual correcta.

---

### Sprint UI-C *(IAView — 1.5h)*

```
1. IAView.java — añadir HBox.command-bar con botones: Limpiar historial, Exportar, Modelos
2. IAView.java — añadir empty state cuando historial de chat está vacío
3. AppConstants.java / styles.css — mover STYLE_BURBUJA_* a clases CSS
```

Criterio de éxito: IAView con patrón CommandBar como el resto de módulos + empty state visible.

---

### Refactor B2 *(largo plazo — solo cuando los anteriores estén cerrados)*

```
Inyectar Connection en todos los DAOs via constructor o parámetro de método.
Elimina definitivamente ARCH-1 y el patrón singleton compartido.
Coste: alto. Prerequisito: todos los sprints anteriores completados.
```

---

## SECUENCIA RECOMENDADA

```
D-ter 1d (pendiente) → Sprint SEC → Sprint COD → Sprint UI-A → Sprint UI-B → Sprint UI-C → Refactor B2
```

El Sprint D-ter 1d (ClienteDAO.setBase con DEFAULT DDL) ya estaba en curso antes de esta auditoría.  
Sprint SEC primero: los fallos de seguridad no deben esperar a los sprints de UI.

---

## TRAZABILIDAD MULTI-IA

| Fase | Agente | Aportación |
|------|--------|-----------|
| 1–2 | Claude Code | Lectura de proyectos referencia y exploración del proyecto |
| 3 | Claude Code | Creación de MACRO-PROMPT, interfaz.md, continuar.md — adaptados al stack real |
| 4 | Claude Code | Auditoría técnica completa: seguridad, código, arquitectura |
| 5 | Claude Code | Auditoría UI/UX: diseño, animaciones, CSS, patrón de módulos |
| 6 | Gemini (IDE) | Segunda opinión arquitectónica: elevó SEC-2 a ALTO, confirmó SEC-1/ARCH-1/COD-2, validó anti-patrón CSS |
| 7 | Codex (IDE) | Verificación técnica local: confirmó SEC-1 (9 métodos), código exacto SEC-2, SEC-4 sin validación ejecutable, UI-3 aclarado, 72/72 tests |
| 8 | Claude Code | Este informe consolidado |

**Limitaciones encontradas:** Codex y Gemini no pueden invocarse por CLI desde Claude Code en este entorno. El usuario los ejecutó manualmente pegando bloques IDE en cada sesión. No se detectaron errores de cuota ni de autenticación durante la sesión.

---

*INFORME-FINAL.md — Gráficas Mulberry — 2026-06-02*
