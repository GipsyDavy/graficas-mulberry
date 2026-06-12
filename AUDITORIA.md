# AUDITORIA.md — Gráficas Mulberry
## Auditoría técnica completa: código, arquitectura, seguridad y calidad

> **Nota de vigencia 2026-06-12:** este documento es histórico. Para continuar un sprint, leer primero `MACRO-PROMPT-GRAFICAS-MULBERRY.md`, `continuar.md`, `CLAUDE.md`, `interfaz.md` y `Resumen.md`. Estado vigente: HEAD `979cd06`, working tree no limpio, sprints COLUMN-FORMAT + IMPORT-REPAIR e IMPORT-PARSER + MAPPING-GUARD cerrados en working tree, `.\mvnw.cmd test` esperado 110/110.

**Fecha:** 2026-06-02  
**Auditor:** Claude Code (Fase 4 del plan de auditoría — ver FASES.md)  
**Archivos auditados:** `AuthService.java`, `UserDAO.java`, `DatabaseManager.java`, `App.java`,
`UserRole.java`, `UserPermissions.java`, `AppConstants.java`, `ExportService.java` (parcial),
`ImportBackupService.java` (completo), `OllamaService.java` (parcial), `LoginView.java`  
**Tests:** 72/72 verdes en HEAD `1fb1904`

> **Nota de vigencia:** documento histórico de auditoría del 2026-06-02. Varios hallazgos
> aquí descritos fueron corregidos posteriormente en Sprint SEC, Sprint COD y Sprints UI-A/B/C/D.
> Para estado vigente usar `Resumen.md`, `continuar.md`, `interfaz.md` y
> `MACRO-PROMPT-GRAFICAS-MULBERRY.md`.

---

## RESUMEN EJECUTIVO

El proyecto tiene una base técnica sólida. El stack (Java 21 + JavaFX + SQLite JDBC) es coherente,
las queries usan PreparedStatements, BCrypt está correctamente aplicado y el sistema de permisos
está bien diseñado. Sin embargo, se han identificado **16 hallazgos** distribuidos en tres categorías:
seguridad, calidad de código y arquitectura.

**No hay vulnerabilidades críticas** que comprometan la integridad de los datos en el uso normal.
Los hallazgos más importantes son un bug de diseño en el patrón singleton de la conexión (ARCH-1),
la ausencia de verificación de contraseña actual en `changePassword` (SEC-2), y dead code con SQL
incorrecto en `AppConstants` (COD-2).

---

## HALLAZGOS DE SEGURIDAD

### SEC-1 — UserDAO cierra la conexión singleton 🟠 MEDIO

**Archivo:** `UserDAO.java` — todos los métodos públicos  
**Problema:** `UserDAO` usa `try (Connection conn = DatabaseManager.getConnection())` con
try-with-resources. En Java, try-with-resources llama `conn.close()` al salir del bloque.
Esto cierra la conexión singleton de `DatabaseManager`. Los DAOs transaccionales del Sprint B
(PresupuestoDAO, FacturaDAO, etc.) NO cierran la conexión — dependen de que permanezca abierta
para mantener la transacción. Si `UserDAO` se llama dentro de un contexto transaccional de otro
DAO, cierra la conexión y rompe la transacción silenciosamente.

**Por qué no falla en la práctica:** `DatabaseManager.getConnection()` reabre la conexión si está
cerrada. En el flujo actual, `UserDAO` solo se usa durante auth (antes de la app principal) y en
`UserManagementView`. Si nunca se llama desde dentro de una transacción de otro DAO, no explota.
Pero es una bomba de relojería.

**Corrección:** En `UserDAO`, reemplazar `try (Connection conn = ...)` por
`Connection conn = DatabaseManager.getConnection()` sin try-with-resources, igual que hacen los DAOs
transaccionales. Solo cerrar `PreparedStatement` y `ResultSet` con try-with-resources.

**Impacto si se corrige ahora:** Bajo — cambio mecánico. No toca tests existentes.

---

### SEC-2 — changePassword sin verificar contraseña actual 🔴 ALTO *(elevado por Gemini)*

**Archivo:** `AuthService.java:46-51`  
**Problema:**
```java
public boolean changePassword(int userId, String newPassword) {
    return userDAO.findById(userId).map(u -> {
        u.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        return userDAO.updateUser(u);
    }).orElse(false);
}
```
Cualquier código con acceso a un `userId` puede cambiar la contraseña sin conocer la actual.
Si hay un bug de autorización en la UI, se puede cambiar la contraseña de cualquier usuario,
incluido un ADMINISTRADOR.

**Evaluación Gemini (segunda opinión):** Riesgo crítico. "Un usuario que deje su sesión abierta
podría ver su contraseña cambiada por otra persona con acceso físico al equipo. La restricción
de la UI es una capa de defensa, pero la capa de servicio siempre debe aplicar las reglas
de seguridad." Gemini indica expresamente: **no debe haber un overload sin verificación**.

**Corrección definitiva (Gemini confirma):**
```java
public boolean changePassword(int userId, String oldPassword, String newPassword) {
    return userDAO.findById(userId)
        .filter(u -> BCrypt.checkpw(oldPassword, u.getPasswordHash()))
        .map(u -> {
            u.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            return userDAO.updateUser(u);
        }).orElse(false);
}
```
Para cambio forzado por ADMIN (reset sin conocer la contraseña actual), crear un método
separado explícito `resetPasswordAdmin(int userId, String newPassword)` con anotación
o guard de rol, no un overload sin verificación.

---

### SEC-3 — Enumeración de usuarios en LoginView 🟡 BAJO

**Archivo:** `LoginView.java:188-191`  
**Problema:** La pantalla de login carga TODOS los usuarios en un ComboBox visible sin
autenticación previa. Cualquier persona que abra la aplicación puede ver todos los nombres
de usuario existentes. El diálogo de recuperación de contraseña también lista todos los usuarios.

**Contexto:** Es una app de escritorio local. La "amenaza" es alguien con acceso físico al PC.
En ese contexto el riesgo es bajo. Pero para una empresa con empleados, exponer los usernames
sin auth es un descuido de diseño.

**Corrección opcional:** Cambiar a un `TextField` de username libre en lugar de ComboBox.
El usuario escribe su nombre y solo entonces se comprueba. Esto elimina la enumeración.
Evaluar si el cambio de UX merece el coste.

---

### SEC-4 — Contraseña mínima 6 caracteres 🟡 BAJO

**Archivo:** `LoginView.java:151`  
**Problema:** El diálogo de recuperación valida `newPw.length() < 6`. Seis caracteres es
insuficiente para una contraseña de uso empresarial. La recomendación estándar es 8 mínimo.

**Corrección:** Cambiar la constante a 8. Añadir la misma validación en `AdminSetupView`
y en `UserManagementView` si aún no la tienen.

---

### SEC-5 — runMigrations swallows ALL SQLException 🟡 BAJO

**Archivo:** `DatabaseManager.java:105-111`  
**Problema:**
```java
} catch (SQLException ignored) {
    // Ignorar errores si la columna ya existe (ej. al ejecutar varias veces)
}
```
El comentario dice "si la columna ya existe" pero el catch captura TODOS los `SQLException`.
Un error real en una migración (sintaxis incorrecta, constraint violation) se ignora silenciosamente.

**Riesgo:** Si una migración falla, la BD puede quedar en estado inconsistente sin ningún aviso.

**Corrección:** Filtrar solo el error específico de SQLite para "column already exists":
```java
} catch (SQLException e) {
    if (!e.getMessage().contains("duplicate column name") &&
        !e.getMessage().contains("already exists")) {
        System.err.println("[Migration warning] " + e.getMessage());
    }
}
```

---

### SEC-6 — Fallback de rol a COMERCIAL en UserDAO 🟡 BAJO

**Archivo:** `UserDAO.java:143-146`  
**Problema:**
```java
try {
    role = UserRole.valueOf(rs.getString("role"));
} catch (IllegalArgumentException e) {
    role = UserRole.COMERCIAL;  // ← fallback a rol con permisos reales
}
```
Si la BD tiene un valor de rol desconocido o corrupto, el usuario obtiene permisos de COMERCIAL
en lugar de que la operación falle. El principio de mínimo privilegio exige que un rol desconocido
no otorgue ningún permiso.

**Corrección:** Relanzar la excepción o usar el rol más restrictivo:
```java
} catch (IllegalArgumentException e) {
    throw new SQLException("Rol desconocido en BD: " + rs.getString("role"), e);
}
```

---

## HALLAZGOS DE CALIDAD DE CÓDIGO

### COD-1 — AppConstants es una god class 🟠 MEDIO

**Archivo:** `AppConstants.java` — 168 líneas  
**Problema:** Una única clase mezcla sin relación:
- Configuración de Ollama/IA (URL, modelo, timeout, prompts)
- Nombres de empresa y claves de config
- Paleta de colores en hexadecimal (JavaFX)
- Colores AWT para PDFs
- Etiquetas de UI, estados de documentos, formatos
- Constantes SQL
- Strings de estilos CSS inline
- Constantes debug

Esto viola SRP (Single Responsibility Principle) y hace el archivo imposible de navegar.
Aunque es baja prioridad refactorizarlo, cualquier búsqueda en este archivo es difícil.

**Hallazgo adicional:** `DEBUG_GESTION_MODELOS = "DEBUG: Solicitada apertura..."` y
`DEBUG_REDIR_OLLAMA = "Redirigiendo..."` — constantes con prefijo DEBUG que deberían estar en logs,
no en producción.

---

### COD-2 — SQL_SELECT_LOW_STOCK con columna inexistente 🟠 MEDIO

**Archivo:** `AppConstants.java:148`  
**Problema:**
```java
public static final String SQL_SELECT_LOW_STOCK =
    "SELECT * FROM " + TABLE_MATERIALES + " WHERE stock <= stock_minimo";
```
La columna real en la tabla `materiales` es `stock_actual`, no `stock` (verificado en
`DatabaseManager.createTables()`). Este SQL fallaría en SQLite con "no such column: stock".

**Estado actual:** `EstadisticasService` usa su propio SQL inline (`stock_actual`) y no consume
esta constante. `SQL_SELECT_LOW_STOCK` es **dead code** con SQL incorrecto.

**Corrección:** Eliminar la constante o corregirla a `WHERE stock_actual <= stock_minimo`.
Si no se usa en ningún sitio, eliminar (más limpio).

---

### COD-3 — Inline styles en LoginView y AppConstants bypasando CSS 🟡 BAJO

**Archivo:** `LoginView.java:54`, `AppConstants.java:134-137`  
**Problema:**  
En `LoginView`:
```java
forgotLabel.setStyle("-fx-cursor: hand; -fx-text-fill: #3498db; -fx-font-size: 11px;");
msgLabel.setStyle("-fx-text-fill: #c0392b;");
```
En `AppConstants`:
```java
public static final String STYLE_BURBUJA_USUARIO = "-fx-background-color:#0078D7; ...";
```
Estos estilos bypass el sistema de temas CSS. En modo oscuro o con otros temas, los colores
hardcodeados no cambian. El azul `#3498db` de `forgotLabel` puede tener bajo contraste en
algunos temas.

**Corrección:** Crear clases CSS (`.link-label`, `.msg-error`, `.chat-bubble-user`) en `styles.css`
y usar `getStyleClass().add(...)` en su lugar.

---

### COD-4 — SELECT * en constante SQL 🟡 BAJO

**Archivo:** `AppConstants.java:147`  
```java
public static final String SQL_SELECT_ALL_CLIENTES =
    "SELECT * FROM " + TABLE_CLIENTES + " ORDER BY nombre ASC";
```
`SELECT *` en código hardcodeado es frágil ante cambios de esquema. Si se añaden columnas grandes
(datos binarios, textos largos), esta query traerá datos innecesarios.

**Estado:** No usado actualmente (misma situación que `SQL_SELECT_LOW_STOCK`). Dead code.

---

### COD-5 — Comentario de clase innecesario en AppConstants 🟢 INFO

**Archivo:** `AppConstants.java:5-8`  
```java
/**
 * AppConstants: Centraliza todas las constantes del sistema ERP y el Asistente IA.
 * VERSIÓN DE ÚLTIMA GENERACIÓN: Fusión total (Negocio + IA + SQL + UI + API).
 */
```
"VERSIÓN DE ÚLTIMA GENERACIÓN: Fusión total" — comentario no técnico y que no aporta valor.
Ruido en el código.

---

## HALLAZGOS DE ARQUITECTURA

### ARCH-1 — Inconsistencia en el ciclo de vida de la conexión singleton 🟠 MEDIO

**Archivos:** `UserDAO.java` (cierra), `PresupuestoDAO.java`, `FacturaDAO.java`, etc. (no cierran)  
**Problema:** El diseño del singleton `Connection` en `DatabaseManager` asume que la conexión
permanece abierta. Los DAOs del Sprint B respetan esto. `UserDAO` la cierra.

Esta inconsistencia es la manifestación superficial del **Refactor B2** (Deuda Técnica):
inyectar `Connection` en DAOs en lugar del singleton estático. Mientras ese refactor no se haga,
la corrección táctica es que `UserDAO` no use try-with-resources en la `Connection`.

**Ver también:** SEC-1 (mismo hallazgo desde perspectiva de seguridad).

---

### ARCH-2 — TABLAS duplicadas en Export e Import sin fuente única de verdad 🟡 BAJO

**Archivos:** `ExportService.java:54-59` (array `TABLAS`), `ImportBackupService.java:19-25`
(array `TABLAS_ORDEN`)  
**Problema:** Dos listas de tablas que deben estar en sync. Si se añade una tabla nueva al
proyecto, hay que actualizarla en dos sitios. Si se olvida uno, el backup/restore es incompleto.

**Corrección futura:** Mover la lista a `DatabaseManager` como constante estática pública.
Bajo coste, pero requiere tocar dos servicios de producción.

---

### ARCH-3 — generarNumero* no es atómico 🟢 INFO

**Archivo:** `DatabaseManager.java:523-553`  
```java
int sig = parseConfigInt("siguiente_presupuesto", 1); // READ
setConfig("siguiente_presupuesto", String.valueOf(sig + 1)); // WRITE
return String.format(...);
```
Read-then-write sin transacción ni lock. En teoría, dos llamadas simultáneas generarían el mismo
número. En la práctica: JavaFX es single-thread (Event Thread) para la UI, así que dos llamadas
simultáneas son imposibles en el flujo normal. **No es un bug real para esta app.**

---

### ARCH-4 — OllamaService conecta a localhost sin validación de URL 🟢 INFO

**Archivo:** `AppConstants.java:17-18`, `OllamaService.java`  
```java
public static final String OLLAMA_BASE_URL = "http://localhost:11434";
```
URL hardcodeada a localhost. Si el usuario cambia el puerto de Ollama o usa una instancia remota,
no hay forma de configurarlo sin recompilar. No es un riesgo de seguridad (solo afecta localhost),
pero limita la flexibilidad. Podría leer de la config de BD como las demás preferencias.

---

## VALIDACIÓN GEMINI — SEGUNDA OPINIÓN (2026-06-02)

| Hallazgo | Veredicto Gemini | Cambio de severidad |
|----------|-----------------|---------------------|
| SEC-1 | Confirmado. Fix inmediato: quitar try-with-resources del Connection en UserDAO | Sin cambio (🟠 MEDIO) |
| SEC-2 | **Elevado a CRÍTICO.** "La capa de servicio siempre debe aplicar las reglas de seguridad." Sin overload sin verificación. | 🟠 MEDIO → 🔴 ALTO |
| COD-2 | Confirmado dead code. Riesgo de uso por reflexión "extremadamente bajo" en este stack. Eliminar. | Sin cambio (🟠 MEDIO) |
| ARCH-1 | Confirmado. Fix intermedio: DatabaseManager gestiona ciclo de vida, ningún DAO cierra la conexión. Refactor B2 como solución definitiva. | Sin cambio (🟠 MEDIO) |

**Gemini añade sobre SEC-2:** "No debe haber un overload sin verificación. Para admin-reset,
crear método separado explícito `resetPasswordAdmin`." — Cambia el diseño propuesto: no dos
overloads, sino `changePassword` (verifica old) + `resetPasswordAdmin` (solo para ADMIN).

---

## PLAN DE ACCIÓN PRIORIZADO *(actualizado con validación Gemini)*

### Sprint SEC — Seguridad (máxima prioridad)

| ID | Acción | Archivo | Esfuerzo | Gemini |
|----|--------|---------|----------|--------|
| SEC-2 | Refactorizar `changePassword` para requerir `oldPassword`. Añadir `resetPasswordAdmin` separado. | `AuthService.java` | 45 min | ✅ Elevado |
| SEC-1 | UserDAO: quitar try-with-resources de `Connection` (no de Statement/ResultSet) | `UserDAO.java` | 30 min | ✅ Confirmado |
| SEC-5 | `runMigrations`: filtrar solo "column already exists" en SQLException | `DatabaseManager.java` | 15 min | — |
| SEC-6 | `UserDAO.map()` fallback: relanzar excepción en rol desconocido | `UserDAO.java` | 10 min | — |
| SEC-4 | Contraseña mínima 8 chars en recovery + AdminSetup + UserManagement | `LoginView.java` | 15 min | — |

### Sprint COD — Limpieza de código (bajo riesgo, alta legibilidad)

| ID | Acción | Archivo | Esfuerzo | Gemini |
|----|--------|---------|----------|--------|
| COD-2 | Eliminar `SQL_SELECT_LOW_STOCK` (columna incorrecta + dead code) | `AppConstants.java` | 5 min | ✅ Confirmado |
| COD-4 | Eliminar `SQL_SELECT_ALL_CLIENTES` (dead code, SELECT *) | `AppConstants.java` | 5 min | — |
| COD-3 | Mover inline styles de `LoginView` a clases CSS | `LoginView.java`, `styles.css` | 30 min | — |
| COD-3b | Mover `STYLE_BURBUJA_*` de AppConstants a clases CSS en `styles.css` | `AppConstants.java`, `styles.css` | 30 min | — |

### Sprint ARCH — Arquitectura (coste medio, posterior a SEC y COD)

| ID | Acción | Archivo | Esfuerzo | Gemini |
|----|--------|---------|----------|--------|
| ARCH-1 | **Intermedio**: asegurar que ningún DAO llame `conn.close()` directamente | Todos los DAOs | 1h | ✅ Confirmado |
| ARCH-2 | Centralizar lista de tablas en `DatabaseManager` como constante estática | Export + Import + DB | Medio | — |
| SEC-3 | Cambiar ComboBox de login a TextField (eliminar enumeración de usuarios) | `LoginView.java` | 1h | — |

### Refactor B2 — Largo plazo (solo cuando los anteriores estén cerrados)

| ID | Acción | Esfuerzo |
|----|--------|----------|
| ARCH-1 def | Inyectar Connection en todos los DAOs via constructor o método | Alto |
| COD-1 | Dividir AppConstants en clases por dominio | Alto |

---

## HALLAZGOS POSITIVOS — LO QUE ESTÁ BIEN

| Aspecto | Evaluación |
|---------|-----------|
| BCrypt con gensalt() en cada hash | ✅ Correcto |
| Respuesta de seguridad también hasheada con BCrypt | ✅ Correcto |
| Normalización `trim().toLowerCase()` antes de BCrypt | ✅ Correcto |
| Todos los DAOs usan PreparedStatements | ✅ Sin inyección SQL |
| `DatabaseManager.quoteIdentifier()` + `requireSqlIdentifier()` para SQL dinámico | ✅ Bien protegido |
| `TABLAS_PERMITIDAS` whitelist en ImportBackupService | ✅ Correcto |
| `STATEMENT_SEGURO` regex para filtrar SQL en restore .sql | ✅ Bien diseñado |
| SingleInstanceLock en App.java | ✅ Previene instancias múltiples |
| Transacciones explícitas en DAOs clave (Sprint B) | ✅ Coherentes |
| `PRAGMA foreign_keys = ON` en cada apertura de conexión | ✅ Integridad referencial |
| Patrón `externalTx` en DAOs transaccionales | ✅ Correcto |
| `stop()` en App.java: cierra música, BD y lock | ✅ Limpio |
| OllamaService con timeout de 10s en conexión | ✅ No bloquea indefinidamente |
| Permisos granulares por módulo en UserRole | ✅ Bien diseñado |

---

## FASE 5 — AUDITORÍA UI/UX: DISEÑO, ANIMACIONES Y FRONTEND

**Archivos auditados:** `MainView.java`, `DashboardView.java`, `FacturasView.java`, `IAView.java`,
`LoginView.java`, `styles.css`, 5 archivos de temas CSS, `AppConstants.java` (sección UI)

---

### ESTADO GENERAL — LO QUE FUNCIONA BIEN ✅

| Aspecto | Detalle |
|---------|---------|
| Transición entre vistas | `mostrarVista()` usa `FadeTransition(150ms)` con `opacity 0→1` ✅ |
| Botones de toolbar | `btn()` helper aplica `.btn-toolbar` correctamente. Emojis se eliminan con regex ✅ |
| Estado activo sidebar | `nav-btn-active` CSS aplicado y eliminado correctamente en clicks ✅ |
| Grupos de navegación | Expand/collapse con `RotateTransition(180ms)` en la flecha ✅ |
| Footer contextual | Right-click en footer → "Abrir en ventana aparte" via `ModuloWindowManager` ✅ |
| Búsqueda sidebar | Ctrl+K abre buscador. Filtrado reactivo en tiempo real con `textProperty` listener ✅ |
| Sonidos | Hover/navigate/click/window-open en todos los elementos interactivos ✅ |
| Asistente visual | Integrado en navegación: dice el módulo al entrar, contexto en grupos ✅ |
| Status badges | `Icons.statusBadge(valor, variante)` en columna Estado de Facturas ✅ |
| Empty states | `tabla.setPlaceholder(Icons.emptyState(...))` en FacturasView ✅ |
| Columnas dinámicas | `DynamicColumnRuntime` en FacturasView — el usuario puede configurar columnas ✅ |
| Multi-select en tabla | `SelectionMode.MULTIPLE` en TableView ✅ |
| Tooltips | En todos los botones de navegación y toolbar ✅ |
| Dashboard cards | `TranslateTransition(200ms, Y:-2)` en hover — lift effect ✅ |
| Dialog con tema | `confirmacion.getDialogPane().getStylesheets().addAll(...)` — diálogo hereda tema ✅ |
| CONSTRAINED_RESIZE_POLICY | Columnas de tabla llenan el ancho disponible ✅ |

---

### HALLAZGOS UI/UX

#### UI-1 — Transición entre vistas demasiado rápida 🟡 BAJO
**Archivo:** `MainView.java:560`
```java
FadeTransition ft = new FadeTransition(Duration.millis(150), vista);
```
150ms es perceptualmente muy rápido — casi imperceptible. Gemini recomendó 200-250ms
para transiciones entre módulos. El cambio es de una constante numérica.

**Corrección:** Cambiar a `Duration.millis(220)`.

---

#### UI-2 — Parámetro `color` ignorado en `btn()` — ruido en 9 vistas 🟡 BAJO
**Archivo:** `FacturasView.java:763-768` (y equivalente en las otras 8 vistas con CommandBar)
```java
private Button btn(String t, String color, Runnable r) {
    String label = t.replaceFirst("^\\P{L}+", "").strip();
    Button b = new Button(label);
    b.getStyleClass().add("btn-toolbar");
    b.setOnAction(e -> r.run()); return b;  // ← color nunca se usa
}
```
El parámetro `color` se pasa en todas las llamadas pero nunca se utiliza.
Todos los colores hardcodeados en las llamadas (`"#F39C12"`, `"#27AE60"`, etc.)
son dead code. El botón solo obtiene `.btn-toolbar`.

**Estado actual:** Funcionalmente correcto — los botones se ven bien con `.btn-toolbar`.
El problema es visual únicamente a nivel de código: ruido que confunde sobre el propósito
del color.

**Corrección:** Eliminar el parámetro `color` del método `btn()` y de todas las llamadas.
O, si se quiere recuperar la intención original, añadir un `.btn-toolbar-danger`,
`.btn-toolbar-warning` etc. para botones destructivos (Borrar, Anular).

---

#### UI-3 — Versión hardcodeada en sidebar 🟡 BAJO
**Archivo:** `MainView.java:245`
```java
Label version = new Label("v13.5.0 · Almería, España");
```
La versión está hardcodeada en el código Java, no se lee de ninguna constante ni de `pom.xml`.
La memoria del proyecto indica v3.3. Hay discrepancia entre la versión en el sidebar (`v13.5.0`),
la versión en `Resumen.md` (`v3.9.1` como handoff), y la versión del instalador (`v3.3`).

**Corrección:** Leer la versión desde una constante en `AppConstants.APP_VERSION`
que sea la fuente única de verdad, o inyectarla desde el `pom.xml` via properties file.

---

#### UI-4 — DashboardView bypass del sistema de temas con colores hardcodeados 🟠 MEDIO
**Archivo:** `DashboardView.java:55-66`
```java
card.setStyle("-fx-border-color: " + color + "; -fx-border-width: 0 0 0 4;");
val.setStyle("-fx-text-fill: " + color + ";");
```
Los 5 KPI del dashboard usan colores hardcodeados (`#4C9BE8`, `#F39C12`, `#E74C3C`,
`#9B59B6`, `#27AE60`). Estos no respetan el sistema de temas CSS. En modo oscuro
o en el tema rojo, pueden generar bajo contraste o choque visual.

**Contexto:** Son colores semánticos (azul=clientes, naranja=presupuestos, rojo=facturas,
violeta=stock, verde=cobrado). No necesitan seguir el tema primario — pero sí deberían
ser legibles en dark mode.

**Corrección mínima:** Verificar contraste en dark mode. Si hay problema, añadir
variables CSS específicas para estas métricas (`.root { -c-kpi-clients: #4C9BE8 }`) y
override en `.modo-oscuro`.

---

#### UI-5 — IAView sin CommandBar ni empty state ⚠️ CONFIRMADO 🟠 MEDIO
**Archivo:** `IAView.java` — confirmado en auditoría (Codex, Fase 3)

`IAView` es el único módulo sin `CommandBar` estandarizado. Tiene:
- Título + subtítulo ✅
- Barra de estado (Ollama conectado/desconectado) ✅
- Área de chat ✅
- Área de input ✅

Pero NO tiene:
- Botones `.btn-toolbar` en un `HBox.command-bar` ❌
- Empty state cuando el historial de chat está vacío ❌
- Integración con el patrón del resto de módulos ❌

El módulo tiene su propia UX específica (chat), por lo que el patrón
CommandBar no aplica 1:1. Sin embargo, algunos botones de acción
(Limpiar, Exportar, Modelos) podrían estar en una `command-bar`.

**Corrección:** Crear una `HBox.command-bar` con los botones de acción
superiores (limpiar historial, exportar, gestionar modelos). Los botones
de estado (Ollama status, modelo activo) pueden permanecer en su barra propia.

---

## FASE 7 — VALIDACIÓN CODEX: VERIFICACIÓN TÉCNICA LOCAL

**Ejecutado:** 2026-06-02 · **Tests:** 72/72 ✅ · **Build:** SUCCESS

---

### Hallazgos confirmados por Codex

#### SEC-1 — CONFIRMADO ✅
`UserDAO.java` — los **9 métodos** abren `DatabaseManager.getConnection()` con try-with-resources
y cierran la conexión singleton al salir del bloque:
`findByUsername`, `findById`, `createUser`, `updateUser`, `deleteUser`,
`getAllUsers`, `updateSecurityQuestion`, `updateLastLogin`, `hasAdmin`.

---

#### SEC-2 — CONFIRMADO ✅ — Código exacto verificado
`AuthService.java` — `changePassword` recibe solo `(int userId, String newPassword)`.
No existe `resetPasswordAdmin`. El método más parecido es `resetPasswordWithAnswer`
(valida respuesta de seguridad). Código exacto:

```java
public boolean changePassword(int userId, String newPassword) {
    return userDAO.findById(userId).map(u -> {
        u.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        return userDAO.updateUser(u);
    }).orElse(false);
}
```

Cualquier módulo con acceso a `AuthService` puede cambiar la contraseña de cualquier usuario
sin verificar quién lo solicita ni cuál era la contraseña anterior.

---

#### SEC-4 — CONFIRMADO ✅ — Solo texto de prompt, sin validación lógica
`AdminSetupView.java` — en las primeras 80 líneas, el único control de longitud es:
```java
passwordField.setPromptText("Contraseña (mín. 6 caracteres)");
```
No hay ninguna validación ejecutable. Un usuario puede crear la contraseña de admin con 1 carácter.

---

#### UI-3 — Aclaración tras verificar pom.xml
`pom.xml` declara `<version>13.5.0</version>`, que **coincide** con el hardcoded del sidebar
(`Label("v13.5.0 · Almería, España")`). No hay discrepancia entre pom y sidebar.
El problema real es más amplio: hay **tres sistemas de versionado distintos** sin sincronía:
- `pom.xml` / sidebar: `13.5.0`
- Instalador `output/`: `v3.3`
- Handoff `Resumen.md`: `v3.9.1`

La fuente única de verdad debería ser `pom.xml`. Los otros dos deberían derivarse de él.

---

#### UI-6 — IAView usa inline styles en lugar de clases CSS 🟡 BAJO
**Archivo:** `IAView.java:81`, `IAView.java:93`
```java
lblEstado.setStyle("-fx-text-fill: #333333; -fx-font-weight: bold;");
btnInstalarOllama.setStyle("-fx-background-color: #6B2D5E; -fx-text-fill: white;");
```
Y desde `AppConstants`:
```java
val.setStyle(AppConstants.STYLE_BURBUJA_USUARIO);  // "-fx-background-color:#0078D7; ..."
val.setStyle(AppConstants.STYLE_BURBUJA_IA);       // "-fx-background-color:#F1F1F1; ..."
```
El color de las burbujas de chat (`#0078D7` azul, `#F1F1F1` gris) y el botón de
instalación (`#6B2D5E` mulberry) son hardcodeados. En dark mode, el `#F1F1F1`
(casi blanco) será invisible sobre un fondo oscuro.

**Corrección:** Añadir a `styles.css`:
- `.chat-bubble-user` (fondo `-c-primary`, texto blanco)
- `.chat-bubble-ia` (fondo `-c-card-bg`, borde `-c-border`)
- `.btn-install-ollama` (extiende `.config-save-btn`)

---

#### UI-7 — Grupos de navegación sin animación de altura en expand/collapse 🟡 BAJO
**Archivo:** `MainView.java:488-498`
```java
contenido.setVisible(expand);
contenido.setManaged(expand);
// Sin animación de altura — aparece/desaparece abruptamente
```
La flecha rota suavemente (RotateTransition 180ms) pero el contenido del grupo
aparece/desaparece de golpe (visible/managed toggle). La coherencia pediría
una animación de altura o fade en el contenido.

**Corrección:** Añadir `FadeTransition(150ms)` en el contenido al expandir.
El colapso puede ser inmediato (para evitar que el layout quede bloqueado).

---

#### UI-8 — Sidebar version discrepancy 🟡 BAJO
**Archivo:** `MainView.java:245`

Adicionalmente al UI-3, el label muestra "v13.5.0" pero el `pom.xml`
tiene su propia versión. Al construir un instalador, el número visible
en la UI no se actualiza automáticamente con el build. Ambos deben estar
sincronizados.

---

#### UI-9 — LoginView: "Mostrar contraseña" sin binding limpio 🟢 INFO
**Archivo:** `LoginView.java:168-186`

El sistema de toggle de contraseña (PasswordField ↔ TextField con binding bidireccional)
funciona correctamente pero el `TextField` overlay comparte el mismo `maxWidth` que el
`PasswordField`, lo que puede causar que el `StackPane` tenga ancho incorrecto si el
`PasswordField` no tiene `maxWidth` definido.

**Estado:** Funciona. Riesgo bajo de regresión visual en ventanas muy pequeñas.

---

### HALLAZGOS POSITIVOS — ANIMACIONES Y MICRO-INTERACCIONES

| Elemento | Animación | Estado |
|----------|-----------|--------|
| Cambio de módulo | FadeTransition 150ms | ✅ Implementado |
| Hover en KPI cards | TranslateTransition(200ms, Y:-2) | ✅ Implementado |
| Colapso sidebar (flecha) | RotateTransition(180ms) | ✅ Implementado |
| Expand/collapse grupos nav (flecha) | RotateTransition(180ms) | ✅ Implementado |
| Expand/collapse contenido grupo | — (toggle inmediato) | 🟡 Sin animación |
| Toast notifications | FadeTransition + auto-dismiss | ✅ Implementado |
| Sidebar collapse CSS | Clase `.sidebar-collapsed` vía CSS | ✅ Implementado |
| Formularios con error | `.input-error` (borde rojo) | ✅ Implementado |

---

### PLAN DE ACCIÓN UI/UX PRIORIZADO

#### Sprint UI-A inmediato (solo CSS — ver interfaz.md)
- UA-1: Tooltip hex hardcodeados → variables CSS
- UA-2: Status badges → variables CSS
- UA-3: `.btn-toolbar-active`
- UA-9: Sanear `theme-mulberry.css`

#### Sprint UI-B: correcciones de código rápidas
| Tarea | Archivo | Esfuerzo |
|-------|---------|----------|
| UI-1: Transición 150→220ms | `MainView.java:560` | 5 min |
| UI-2: Eliminar param `color` en `btn()` | 9 vistas | 30 min |
| UI-3: Versión leída desde AppConstants | `MainView.java:245` | 20 min |
| UI-7: FadeTransition en expand grupo nav | `MainView.java:488` | 20 min |

#### Sprint UI-C: IAView completa
| Tarea | Descripción |
|-------|-------------|
| UI-5 | Añadir `HBox.command-bar` con Limpiar/Exportar/Modelos en `.btn-toolbar` |
| UI-6 | Mover bubble styles a clases CSS `.chat-bubble-user`, `.chat-bubble-ia` |

#### Sprint UI-D: Verificación visual pendiente
Ver sección 2.3 y Sprint UI-C en `interfaz.md` para los módulos [VERIFICAR].

---

### CORRECCIÓN INMEDIATA DETECTADA — interfaz.md desactualizado

La sección "Sprint UI-B" de `interfaz.md` indicaba que no había transición entre vistas.
Esto era incorrecto — `mostrarVista()` ya tiene `FadeTransition(150ms)`.
La propuesta de Sprint UI-B original debe actualizarse: no crear la transición
(ya existe), sino ajustar su duración de 150ms a 220ms.

---

*AUDITORIA.md — Fases 4 y 5 completadas — 2026-06-02*

---

## BLOQUE CODEX — FASE 7: VERIFICACIÓN TÉCNICA ⏳ PENDIENTE

Pegar en **Codex** del IDE. Respuesta necesaria antes de elaborar el informe final (Fase 8).

```
## Contexto del proyecto
Gráficas Mulberry — ERP de escritorio Java 21 + JavaFX 21 + SQLite.
Sin Lombok, sin Spring. Raíz: C:\Users\GipsyDavy\MAVEN\Graficas Mulberry
Build: .\mvnw.cmd test

## Tu tarea concreta
Solo lectura y verificación. NO modificar ningún archivo. Responde a cada punto:

1. Lee src/main/java/org/gipsybuho/dao/UserDAO.java
   - ¿Todos los métodos usan try (Connection conn = ...) con try-with-resources?
   - ¿Hay algún método que NO cierre la conexión?
   - Lista los métodos que sí cierran la conexión con try-with-resources.

2. Lee src/main/java/org/gipsybuho/service/AuthService.java
   - ¿Existe algún método changePassword que verifique la contraseña actual?
   - ¿Existe algún método resetPasswordAdmin o similar?
   - Copia el método changePassword exactamente como está.

3. Lee src/main/java/org/gipsybuho/ui/AdminSetupView.java (primeras 80 líneas)
   - ¿Qué validación de longitud mínima de contraseña hay?
   - ¿Cuál es el mínimo de caracteres exigido?

4. Lee pom.xml (solo las primeras 15 líneas)
   - ¿Cuál es la versión declarada en el pom.xml?

5. Ejecuta: .\mvnw.cmd test
   - Reporta el número de tests y si todos pasan.

## Restricciones
- Solo lectura. NO modificar ningún archivo.
- Reportar resultados claros para cada punto numerado.
```

📋 **Pega este bloque en el chat de Codex**
