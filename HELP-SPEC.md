# HELP-SPEC.md — Especificación del Sistema de Ayuda Integrado
## Proyecto: Gráficas Mulberry — Sprint HELP-0

**Fecha:** 2026-06-10  
**Estado:** Especificación cerrada. Base para Sprint HELP-1 (contenidos) y HELP-2 (vista JavaFX).  
**Sin código Java en este sprint.** Solo especificación, estructura y contenidos base.

---

## 1. OBJETIVO Y PRINCIPIOS DE DISEÑO

El sistema de ayuda debe ser:

1. **Completamente offline.** No requiere internet. Todo el contenido se empaqueta con la aplicación.
2. **Contextual.** Cada pantalla puede abrir directamente el artículo relevante (F1 o botón `?`).
3. **Buscable.** El usuario puede buscar por palabras clave sin saber el nombre del módulo.
4. **Accionable.** Los errores de la aplicación incluyen un enlace al artículo que explica la solución.
5. **Operacional.** El lenguaje es práctico, no técnico. El público son los empleados de la empresa, no desarrolladores.
6. **Incremental.** Cada sprint funcional futuro incluye su artículo de ayuda correspondiente.
7. **Mantenible.** Los artículos son archivos HTML o Markdown empaquetados como recursos, editables sin recompilar.

---

## 2. ARQUITECTURA DEL SISTEMA DE AYUDA

El sistema tiene cuatro capas, implementadas en orden:

```
┌─────────────────────────────────────────────────────────────┐
│  Capa 4 — Ayuda contextual (F1 + botón ? por pantalla)      │  ← HELP-3
│  Capa 3 — Centro de ayuda (vista buscable, HelpView.java)   │  ← HELP-2
│  Capa 2 — Contenidos offline (artículos HTML en recursos)   │  ← HELP-1
│  Capa 1 — Especificación (este documento)                   │  ← HELP-0 ✓
└─────────────────────────────────────────────────────────────┘
```

### Capa 1 — Especificación (HELP-0, este documento)
Arquitectura, taxonomía, inventario de artículos, criterios de aceptación.

### Capa 2 — Contenidos offline (HELP-1)
Artículos HTML/Markdown en `src/main/resources/org/gipsybuho/help/`.
Estructura de carpetas por módulo. Sin servidor, sin JavaScript externo.
Cada artículo es un fichero `.html` autocontenido con CSS inline o enlazado local.

### Capa 3 — Centro de ayuda JavaFX (HELP-2)
Vista `HelpView.java` con:
- WebView para renderizar artículos HTML
- TreeView / ListView lateral con tabla de contenidos
- Barra de búsqueda (busca en metadatos de artículos, no full-text en primer sprint)
- Botones de navegación (atrás/adelante, inicio)
- Accesible desde el sidebar (ícono `?` en footer de navegación)

### Capa 4 — Ayuda contextual (HELP-3, después de HELP-2)
- Tecla F1 en cualquier vista → abre HelpView en el artículo del módulo activo
- Botón `?` en formularios y diálogos complejos → abre artículo relevante
- Errores de la aplicación con código → enlace directo al artículo de solución
- Tooltips avanzados en campos no obvios (descripción + ejemplo)

---

## 3. TAXONOMÍA DE CONTENIDOS

Cada artículo pertenece a una de estas categorías:

| Categoría | Código | Descripción |
|---|---|---|
| Primeros pasos | `PS` | Flujos básicos para empezar a usar el módulo |
| Tutorial | `TUT` | Guía paso a paso de una tarea concreta |
| Referencia | `REF` | Descripción de campos, estados y valores posibles |
| FAQ | `FAQ` | Preguntas frecuentes del módulo |
| Errores | `ERR` | Mensajes de error + causa + solución |
| Glosario | `GLO` | Términos propios del negocio o la app |
| Advertencias | `ADV` | Acciones irreversibles, riesgos operativos |
| Novedad | `NEW` | Funcionalidades nuevas en cada versión |

### Prioridad de categorías para HELP-1
1. `PS` — sin primeros pasos, el usuario no arranca
2. `ERR` — los errores sin solución bloquean el trabajo
3. `FAQ` — responde el 80% de las dudas del día a día
4. `REF` + `ADV` — campos y riesgos
5. `TUT` + `GLO` — más adelante, una vez lo básico funciona

---

## 4. INVENTARIO DE ARTÍCULOS POR MÓDULO

Para cada módulo se indica: artículos mínimos para HELP-1 (marcados ★) y artículos deseables para iteraciones posteriores.

---

### 4.1 Clientes (`clientes/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| CLI-PS-1 | PS | Cómo añadir un cliente nuevo | ★ |
| CLI-PS-2 | PS | Cómo editar los datos de un cliente | ★ |
| CLI-REF-1 | REF | Campos del formulario de cliente (NIF, tipo, ciudad...) | ★ |
| CLI-FAQ-1 | FAQ | ¿Por qué no aparece el botón "Guardar"? | ★ |
| CLI-ERR-1 | ERR | Error "NIF duplicado" — qué significa y cómo resolverlo | ★ |
| CLI-TUT-1 | TUT | Importar clientes desde Excel o CSV | |
| CLI-ADV-1 | ADV | Consecuencias de eliminar un cliente con presupuestos activos | |

---

### 4.2 Materiales (`materiales/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| MAT-PS-1 | PS | Cómo añadir un material nuevo | ★ |
| MAT-REF-1 | REF | Campos del formulario de material (código, categoría, unidad, stock) | ★ |
| MAT-FAQ-1 | FAQ | ¿Qué significa el campo "stock"? ¿Se actualiza automáticamente? | ★ |
| MAT-ERR-1 | ERR | Error "Código duplicado" | ★ |
| MAT-TUT-1 | TUT | Importar materiales desde Excel | |
| MAT-ADV-1 | ADV | Ajuste de stock: efecto en pedidos activos | |

---

### 4.3 Empleados (`empleados/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| EMP-PS-1 | PS | Cómo dar de alta un empleado | ★ |
| EMP-REF-1 | REF | Campos del formulario de empleado (DNI, categoría, fecha alta) | ★ |
| EMP-FAQ-1 | FAQ | ¿Qué significa "inactivo"? ¿Puedo seguir importando nóminas de un empleado inactivo? | ★ |
| EMP-ERR-1 | ERR | Error "DNI duplicado" | ★ |
| EMP-ADV-1 | ADV | Dar de baja un empleado: impacto en nóminas históricas | |

---

### 4.4 Presupuestos (`presupuestos/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| PRE-PS-1 | PS | Cómo crear un presupuesto desde cero | ★ |
| PRE-PS-2 | PS | Cómo convertir un presupuesto en factura | ★ |
| PRE-REF-1 | REF | Estados del presupuesto (borrador, enviado, aceptado, rechazado) | ★ |
| PRE-REF-2 | REF | Líneas de presupuesto: campos y cálculo de totales | ★ |
| PRE-FAQ-1 | FAQ | ¿Puedo cambiar el cliente de un presupuesto ya enviado? | ★ |
| PRE-ERR-1 | ERR | Error "Número duplicado" en presupuesto | ★ |
| PRE-ADV-1 | ADV | Eliminar un presupuesto: no se puede deshacer | |
| PRE-TUT-1 | TUT | Importar presupuestos desde CSV (formato ancho) | |

---

### 4.5 Facturas (`facturas/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| FAC-PS-1 | PS | Cómo crear una factura desde cero | ★ |
| FAC-PS-2 | PS | Cómo generar una factura desde un presupuesto aceptado | ★ |
| FAC-REF-1 | REF | Estados de factura (pendiente, pagada, anulada) | ★ |
| FAC-REF-2 | REF | Forma de pago: valores posibles y cuándo cambiarlo | ★ |
| FAC-FAQ-1 | FAQ | ¿Cómo exportar la factura a PDF? | ★ |
| FAC-ERR-1 | ERR | Error "Número duplicado" en factura | ★ |
| FAC-ADV-1 | ADV | Anular una factura: consecuencias contables | |

---

### 4.6 Albaranes (`albaranes/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| ALB-PS-1 | PS | Cómo crear un albarán | ★ |
| ALB-PS-2 | PS | Cómo generar un albarán desde una factura | ★ |
| ALB-REF-1 | REF | Estados de albarán (pendiente, entregado, cancelado) | ★ |
| ALB-FAQ-1 | FAQ | ¿Un albarán siempre tiene que estar vinculado a una factura? | ★ |
| ALB-ERR-1 | ERR | Error "Factura no encontrada" al crear desde factura | ★ |

---

### 4.7 Pedidos (`pedidos/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| PED-PS-1 | PS | Cómo crear un pedido de material a proveedor | ★ |
| PED-REF-1 | REF | Estados del pedido y flujo de trabajo | ★ |
| PED-FAQ-1 | FAQ | ¿Cómo registro la recepción de mercancía? | ★ |
| PED-ERR-1 | ERR | Error "Número duplicado" en pedido | ★ |

---

### 4.8 Nóminas (`nominas/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| NOM-PS-1 | PS | Cómo registrar una nómina mensual | ★ |
| NOM-REF-1 | REF | Campos de nómina (bruto, retención, neto, mes, año) | ★ |
| NOM-FAQ-1 | FAQ | ¿Puedo registrar la nómina de un empleado dado de baja? | ★ |
| NOM-TUT-1 | TUT | Importar nóminas desde Excel | |

---

### 4.9 Tarifas (`tarifas/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| TAR-PS-1 | PS | Cómo asignar una tarifa a un cliente para un material | ★ |
| TAR-REF-1 | REF | Qué es un tramo de tarifa y cómo funciona | ★ |
| TAR-FAQ-1 | FAQ | ¿Cómo sé qué tarifa se aplica en un presupuesto? | ★ |

---

### 4.10 Importación (`importacion/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| IMP-PS-1 | PS | Cómo importar datos desde Excel o CSV | ★ |
| IMP-REF-1 | REF | Formatos soportados: CSV, XLS, XLSX, XLSB, XLSM, JSON | ★ |
| IMP-REF-2 | REF | Diálogo de mapeo de columnas: cómo usarlo | ★ |
| IMP-REF-3 | REF | Política de duplicados: omitir, actualizar o crear nuevo | ★ |
| IMP-TUT-1 | TUT | Crear un campo personalizado durante la importación | ★ |
| IMP-ERR-1 | ERR | Error "NIF no encontrado" al importar facturas o albaranes | ★ |
| IMP-ERR-2 | ERR | Errores de formato de fecha en columnas de fecha | ★ |
| IMP-FAQ-1 | FAQ | Mi Excel tiene celdas combinadas. ¿Cómo lo importo? | ★ |
| IMP-ADV-1 | ADV | Importación con política "Actualizar existente": qué sobreescribe | ★ |
| IMP-TUT-2 | TUT | Convertir Excel complejo a CSV importable (procedimiento manual) | |

---

### 4.11 Exportación (`exportacion/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| EXP-PS-1 | PS | Cómo exportar una tabla a Excel o CSV | ★ |
| EXP-PS-2 | PS | Cómo exportar un documento a PDF | ★ |
| EXP-FAQ-1 | FAQ | ¿Puedo elegir qué columnas exportar? | ★ |

---

### 4.12 Backups (`backups/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| BAK-PS-1 | PS | Cómo hacer una copia de seguridad de la base de datos | ★ |
| BAK-PS-2 | PS | Cómo restaurar desde una copia de seguridad | ★ |
| BAK-FAQ-1 | FAQ | ¿Con qué frecuencia debo hacer backups? | ★ |
| BAK-ADV-1 | ADV | Restaurar un backup sobreescribe todos los datos actuales | ★ |
| BAK-ERR-1 | ERR | Error al importar backup: formato o versión incompatible | ★ |

---

### 4.13 IA — Asistente Ollama (`ia/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| IA-PS-1 | PS | Cómo instalar Ollama y activar el asistente | ★ |
| IA-PS-2 | PS | Cómo usar el asistente de IA en la aplicación | ★ |
| IA-REF-1 | REF | Comandos disponibles en el asistente | ★ |
| IA-FAQ-1 | FAQ | El asistente dice "Ollama no disponible". ¿Qué hago? | ★ |
| IA-ADV-1 | ADV | El asistente no accede a internet ni envía datos fuera del equipo | ★ |

---

### 4.14 Asistente Visual y TTS (`asistente/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| AST-PS-1 | PS | Cómo activar el asistente visual y la lectura en voz | ★ |
| AST-REF-1 | REF | Configuración de velocidad, voz y activación automática | |
| AST-FAQ-1 | FAQ | ¿El asistente habla aunque tenga el volumen bajo en Windows? | |

---

### 4.15 Estadísticas (`estadisticas/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| EST-PS-1 | PS | Cómo ver las estadísticas de ventas y materiales | ★ |
| EST-REF-1 | REF | Cómo filtrar por período y módulo | ★ |
| EST-FAQ-1 | FAQ | Los datos no se actualizan al instante — ¿es normal? | |

---

### 4.16 Calendario (`calendario/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| CAL-PS-1 | PS | Cómo añadir una nota o evento al calendario | ★ |
| CAL-FAQ-1 | FAQ | ¿El calendario se sincroniza con otras aplicaciones? | ★ |

---

### 4.17 Usuarios y Roles (`usuarios/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| USR-PS-1 | PS | Cómo crear un usuario nuevo | ★ |
| USR-REF-1 | REF | Permisos por rol: ADMINISTRADOR, COMERCIAL, PRODUCCION, CONTABILIDAD | ★ |
| USR-FAQ-1 | FAQ | ¿Qué puede hacer cada rol? | ★ |
| USR-ADV-1 | ADV | Solo un ADMINISTRADOR puede cambiar contraseñas o eliminar usuarios | ★ |
| USR-ERR-1 | ERR | "Sin permisos" al intentar acceder a un módulo | ★ |

---

### 4.18 Configuración (`configuracion/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| CFG-PS-1 | PS | Cómo cambiar el tema visual de la aplicación | ★ |
| CFG-REF-1 | REF | Temas disponibles (Mulberry, Oscuro, Claro, Corporativo, Minimalista) | ★ |
| CFG-FAQ-1 | FAQ | ¿La configuración se aplica a todos los usuarios o solo al mío? | ★ |

---

### 4.19 General / Transversal (`general/`)

| ID | Categoría | Título | Prioridad |
|---|---|---|---|
| GEN-PS-1 | PS | Primeros pasos con Gráficas Mulberry (onboarding) | ★ |
| GEN-REF-1 | REF | Glosario de términos del sistema | ★ |
| GEN-FAQ-1 | FAQ | Preguntas frecuentes generales | ★ |
| GEN-ADV-1 | ADV | Acciones irreversibles: qué se puede recuperar y qué no | ★ |
| GEN-NEW-1 | NEW | Novedades de la versión actual | ★ |

---

## 5. ESTRUCTURA DE ARCHIVOS — HELP-1

```
src/main/resources/org/gipsybuho/help/
├── index.json                    # Metadatos de todos los artículos (ID, título, módulo, categoría, tags)
├── help.css                      # CSS compartido por todos los artículos
├── general/
│   ├── GEN-PS-1.html
│   ├── GEN-REF-1.html            # Glosario
│   ├── GEN-FAQ-1.html
│   ├── GEN-ADV-1.html
│   └── GEN-NEW-1.html
├── clientes/
│   ├── CLI-PS-1.html
│   ├── CLI-PS-2.html
│   ├── CLI-REF-1.html
│   ├── CLI-FAQ-1.html
│   └── CLI-ERR-1.html
├── materiales/
│   └── ...
├── empleados/
│   └── ...
├── presupuestos/
│   └── ...
├── facturas/
│   └── ...
├── albaranes/
│   └── ...
├── pedidos/
│   └── ...
├── nominas/
│   └── ...
├── tarifas/
│   └── ...
├── importacion/
│   └── ...
├── exportacion/
│   └── ...
├── backups/
│   └── ...
├── ia/
│   └── ...
├── asistente/
│   └── ...
├── estadisticas/
│   └── ...
├── calendario/
│   └── ...
├── usuarios/
│   └── ...
└── configuracion/
    └── ...
```

---

## 6. FORMATO DE ARTÍCULOS HTML

Cada artículo es un fichero HTML autocontenido con esta estructura mínima:

```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <link rel="stylesheet" href="../help.css">
  <title>Título del artículo</title>
  <meta name="module" content="clientes">
  <meta name="category" content="PS">
  <meta name="tags" content="cliente, añadir, nuevo, NIF">
</head>
<body>
  <h1>Título del artículo</h1>
  <!-- Contenido: h2 para secciones, p, ul, ol, code, .note, .warning, .tip -->
</body>
</html>
```

### Clases CSS semánticas (definidas en `help.css`)
- `.note` — información adicional (fondo azul claro)
- `.warning` — advertencia operativa (fondo amarillo)
- `.danger` — acción irreversible (fondo rojo claro)
- `.tip` — consejo práctico (fondo verde claro)
- `.steps` — lista `ol` para pasos numerados
- `.field-ref` — tabla de referencia de campos
- `code` — valores literales, nombres de campo, rutas

---

## 7. FORMATO DE `index.json`

```json
[
  {
    "id": "CLI-PS-1",
    "module": "clientes",
    "category": "PS",
    "title": "Cómo añadir un cliente nuevo",
    "tags": ["cliente", "nuevo", "añadir", "crear", "NIF"],
    "path": "clientes/CLI-PS-1.html",
    "priority": 1
  }
]
```

La búsqueda en HELP-2 opera sobre `title` + `tags`. Full-text sobre el HTML queda para HELP-3.

---

## 8. ESPECIFICACIÓN TÉCNICA DE HELP-2 (HelpView.java)

### Estructura de la vista
```
BorderPane (raíz)
├── top: HBox — barra de búsqueda + botones nav (← → ⌂)
├── left: TreeView<HelpEntry> — tabla de contenidos por módulo (ancho: 220px)
└── center: WebView — renderizado del artículo HTML
```

### Clase de datos `HelpEntry`
```java
record HelpEntry(String id, String module, String category,
                 String title, List<String> tags, String path, int priority) {}
```

### `HelpService.java` (lógica de búsqueda)
- Carga `index.json` al arrancar (singleton lazy, no en el constructor de HelpView).
- `search(String query)` → `List<HelpEntry>` filtrando por `title.contains` + `tags.contains`.
- `getByModule(String module)` → todos los artículos del módulo, ordenados por `priority`.
- `getArticle(String id)` → ruta del recurso para cargarlo en WebView.

### Integración en navegación
- Icono `?` en el footer del sidebar de `MainView` → abre `HelpView` en `GEN-PS-1` (onboarding).
- `HelpView` es una vista más del router principal, no una ventana separada.
- `ModuloWindowManager` no la gestiona — se abre dentro del área central como cualquier otra vista.

---

## 9. AYUDA CONTEXTUAL — ESPECIFICACIÓN HELP-3

### Mapa vista → artículo (para implementar F1 y botón `?`)

| Vista Java | Artículo por defecto |
|---|---|
| `ClientesView` | `CLI-PS-1` |
| `MaterialesView` | `MAT-PS-1` |
| `EmpleadosView` | `EMP-PS-1` |
| `PresupuestosView` | `PRE-PS-1` |
| `FacturasView` | `FAC-PS-1` |
| `AlbaranesView` | `ALB-PS-1` |
| `PedidosView` | `PED-PS-1` |
| `NominasView` | `NOM-PS-1` |
| `TarifasView` | `TAR-PS-1` |
| `ImportView` | `IMP-PS-1` |
| `ImportBackupView` | `BAK-PS-1` |
| `ExportView` | `EXP-PS-1` |
| `IAView` | `IA-PS-1` |
| `VisualAssistantView` | `AST-PS-1` |
| `EstadisticasView` | `EST-PS-1` |
| `CalendarioView` | `CAL-PS-1` |
| `UserManagementView` | `USR-PS-1` |
| `ConfiguracionView` | `CFG-PS-1` |
| `DashboardView` | `GEN-PS-1` |
| `ColumnMappingDialog` | `IMP-REF-2` |

### Errores con enlace a artículo
La clase `ToastService` (o un nuevo `ErrorHelpService`) mapea códigos de error a `HelpEntry.id`.
Ejemplo: `"NIF_DUPLICADO"` → `CLI-ERR-1`. El Toast muestra un enlace "Ver solución" que abre HelpView.

---

## 10. CRITERIOS DE ACEPTACIÓN

### HELP-1 — Contenidos offline

- [ ] Estructura de carpetas `src/main/resources/org/gipsybuho/help/` creada.
- [ ] `index.json` con todos los artículos de prioridad ★ (al menos 60 artículos).
- [ ] `help.css` con clases semánticas `.note`, `.warning`, `.danger`, `.tip`, `.steps`, `.field-ref`.
- [ ] Artículos `GEN-PS-1` (onboarding) y `GEN-REF-1` (glosario) con contenido completo.
- [ ] Artículos `IMP-PS-1`, `IMP-REF-1`, `IMP-REF-2`, `IMP-REF-3` completos (importación es el flujo más crítico).
- [ ] Al menos 3 artículos completos por módulo principal (Clientes, Presupuestos, Facturas).
- [ ] Todos los `ERR-*` de prioridad ★ con contenido completo (errores más frecuentes resueltos).
- [ ] Los artículos se pueden abrir directamente en un navegador y se ven correctamente.

### HELP-2 — Centro de ayuda JavaFX

- [ ] `HelpView.java` compila y se puede abrir desde el sidebar.
- [ ] `HelpService.java` carga `index.json` y sirve artículos al WebView.
- [ ] La búsqueda por texto muestra resultados relevantes en menos de 200ms.
- [ ] La tabla de contenidos muestra todos los módulos con sus artículos agrupados.
- [ ] El WebView renderiza los artículos con el CSS correcto.
- [ ] Botones de navegación (atrás/adelante) funcionan dentro del historial de la sesión.
- [ ] Tests: al menos 1 test unitario para `HelpService.search()` y `getByModule()`.

### HELP-3 — Ayuda contextual

- [ ] F1 en cualquier vista principal abre HelpView en el artículo correspondiente del mapa.
- [ ] `ColumnMappingDialog` tiene botón `?` que abre `IMP-REF-2`.
- [ ] Al menos 3 errores del sistema tienen enlace "Ver solución" en el Toast/Alert.

---

## 11. CONTEO DE ARTÍCULOS ★

| Módulo | Artículos ★ |
|---|---|
| Clientes | 5 |
| Materiales | 4 |
| Empleados | 4 |
| Presupuestos | 6 |
| Facturas | 6 |
| Albaranes | 5 |
| Pedidos | 4 |
| Nóminas | 3 |
| Tarifas | 3 |
| Importación | 9 |
| Exportación | 3 |
| Backups | 5 |
| IA | 5 |
| Asistente | 2 |
| Estadísticas | 2 |
| Calendario | 2 |
| Usuarios | 5 |
| Configuración | 3 |
| General | 5 |
| **TOTAL** | **81** |

---

*Gráficas Mulberry — HELP-SPEC.md — Sprint HELP-0 — 2026-06-10*
