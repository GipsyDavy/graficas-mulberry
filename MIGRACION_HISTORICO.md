# Migración de datos históricos — Procedimiento Vía A

README técnico sobre cómo procesar archivos históricos de la empresa cliente cuando lleguen.

**Última revisión:** 12/06/2026 (actualizado Sprint IMPORT-PARSER + MAPPING-GUARD)
**Estado:** Vía A activa y documentada. Sprint 2 (Importación CSV) cerrado. Sprint IMPORT-UPGRADE (`4bc6c9c`) cerrado. Sprint IMPORT-PARSER + MAPPING-GUARD cerrado en working tree: 288/288 archivos reales aportados por el usuario abren con `ImportService.parseFile()` (110 CSV, 177 XLSX, 1 XLSB). El problema pendiente NO es abrir archivos ni importar CSV/Excel plano, sino migrar **tablas complejas en formato humano** con secciones internas, bloques laterales, varias mini-tablas por hoja y filas decorativas. La siguiente línea de trabajo debe centrarse en limpiar/clasificar esos casos complejos.

---

## Contexto

La empresa cliente trabaja con archivos en formato humano: Excel con celdas combinadas, varias hojas por libro, cabeceras decorativas, bloques laterales, totales calculados con fórmulas. También PDFs y Words generados desde software de terceros.

Estos archivos no son importables directamente con el asistente de mapping (`ColumnMappingDialog` + `EntityImportService`) porque dicho asistente asume **CSV plano o CSV ancho con cabecera repetida**: una fila = un registro (o un grupo agrupable por clave), cabeceras en fila 1, sin estructura visual humana.

Sprint 2 cerrado por Vía A en mayo 2026:
- No se construye importador genérico capaz de tragarse formatos humanos.
- Conversión manual a CSV limpio archivo por archivo.
- Import con el asistente de Fase 1 + Fase 2 (parent-child) ya existente.
- Las 9 vistas tienen botón `📥 Importar` funcional cableado contra `EntityImportService`. Ya no hay Alerts "Funcionalidad próximamente".

**Aclaración para próximas sesiones:** si el usuario pregunta por "Excel", "tablas complejas",
"migración de archivos" o "lo que quedó por la 3.9", NO arrancar Refactor B2 ni DOC/HELP.
Primero retomar esta línea: analizar archivos reales, definir conversión a CSV limpio o plantilla
específica, y documentar el procedimiento.

---

## Procedimiento general

Para cada archivo que llegue:

1. **Inspeccionar estructura.** Abrir el archivo, identificar qué entidades del sistema contiene, cómo están organizados los datos.
2. **Identificar campos relevantes por entidad.** Qué columnas son cabeceras reales (no decorativas), qué celdas combinadas hay, qué bloques laterales.
3. **Generar CSV limpio.** Un CSV por entidad. Para entidades planas: una fila = un registro. Para entidades parent-child (Presupuesto, Factura, Albarán): CSV ancho con cabecera repetida en cada línea, agrupable por `numero`. Cabeceras en fila 1. Sin combinaciones, sin fórmulas, decimales con punto, UTF-8.
4. **Mapear contra el `IMPORT_SPEC`.** Las claves del CSV deben encajar con las claves del `IMPORT_SPEC` del modelo correspondiente. Ver sección "Estado actual del importador" abajo.
5. **Importar desde la app.** Botón Importar de la vista → seleccionar CSV → `ColumnMappingDialog` resuelve el mapping casi todo automático → confirmar → revisar `ImportResult` (importadas, descartadas, errores).
6. **Validar visualmente.** Abrir la tabla en la app, comprobar que los registros aparecen correctamente.

---

## Próximos pasos obligatorios — Sprint MIGRACION-COMPLEJA

Objetivo: resolver la migración de tablas complejas reales, no reabrir el importador CSV/Excel plano ya cerrado.

### Inventario real recibido el 2026-06-12

Rutas aportadas y probadas:
- `C:\Users\Gipsy Dávy\Desktop\CSV`
- `C:\Users\Gipsy Dávy\Desktop\excel`
- `C:\Users\Gipsy Dávy\Desktop\EXCEL_SEPARADO`
- `C:\Users\Gipsy Dávy\Desktop\files`
- `C:\Users\Gipsy Dávy\Desktop\TARIFAS_SEPARADAS`
- `C:\Users\Gipsy Dávy\Desktop\TARIFAS_SEPARADAS 1`
- `C:\Users\Gipsy Dávy\Desktop\todas_las_tarifas`

Resultado técnico:
- `ImportService.parseFile()` abre 288/288 archivos soportados.
- Recuento: 110 CSV, 177 XLSX, 1 XLSB.
- 34 archivos están vacíos o sin columnas reales; abrirlos no debe considerarse fallo.
- CSV de `Desktop\files` son los más limpios para Materiales y se importan bien en dry-run.
- `01_TARJETAS_DE_VISITA.csv/xlsx`, `17_DISEÑOS.xlsx`, `19_OVALOS.xlsx`, `40_IMANES.xlsx` son buenos casos de Tarifas para smoke test.
- `NUEVAS TARIFAS (2) (version 1).xlsb` abre y recupera 1020 filas, pero contiene múltiples secciones internas; requiere limpieza o estrategia específica.
- `PRECIOS PAPEL PROVEEDORES Formulas.xlsx` abre pero contiene filas separadoras/bloques humanos; dry-run simple importó 39/76 filas.

Correcciones ya aplicadas en código:
- XLSB se lee con `XSSFBEventBasedExcelExtractor`.
- Parser detecta cabecera real, conserva tablas laterales, infiere cabeceras vacías y salta cabeceras repetidas/separadores.
- `mapearCampos()` siempre ejecuta fallback local aunque Ollama falle o devuelva todo `null`.
- Wizard bloquea avance si faltan campos obligatorios.

No repetir como trabajo pendiente:
- “Hacer que los archivos abran”: ya está validado.
- “Mapeo básico de tarifas/materiales”: ya está mejorado.
- “Soporte XLSB”: ya existe para extracción tabulada; lo pendiente es limpiar libros con secciones internas.

### Paso 1 — Inventario de archivos reales

La muestra inicial ya existe en las rutas anteriores. El siguiente agente debe convertir el inventario en una tabla de clasificación por archivo:
- Excel `.xlsx/.xls/.xlsm/.xlsb` con tablas humanas.
- PDFs con tablas seleccionables o escaneadas.
- Word `.docx/.doc` con tablas.
- Cualquier exportación antigua del software previo.

Para cada archivo anotar:
- entidad destino: cliente, material, presupuesto, factura, albarán, pedido, nómina, tarifa, empleado;
- número de hojas o tablas;
- fila real de cabecera;
- presencia de celdas combinadas;
- fórmulas;
- bloques laterales;
- totales calculados;
- volumen aproximado de filas;
- periodicidad: puntual o recurrente.

### Paso 2 — Clasificación por vía

Clasificar cada archivo en una de estas vías:

| Vía | Cuándo usarla | Resultado |
|---|---|---|
| A1 — Conversión manual simple | Archivo puntual o pequeño | CSV limpio generado a mano |
| A2 — Script específico | Archivo recurrente con estructura estable | Script Python por plantilla de origen |
| B — Importador nativo específico | Alto volumen mensual y estructura estable | Código Java/servicio dedicado |
| C — OCR/manual asistido | PDF escaneado o documento libre | Extracción manual revisada |

La vía por defecto sigue siendo A1/A2. No construir B hasta demostrar que el coste manual supera
el coste de mantener código específico.

### Paso 3 — Plantilla de CSV destino por entidad

Para cada entidad afectada, crear una plantilla de columnas destino compatible con `IMPORT_SPEC`.
Para parent-child usar CSV ancho con cabecera repetida y agrupación por `numero`.

Entidades parent-child:
- Presupuesto: cabecera + líneas, agrupado por `numero`.
- Factura: cabecera + líneas, agrupado por `numero`.
- Albarán: cabecera + líneas, agrupado por `numero`.

Entidades planas:
- Cliente, Material, Empleado, Tarifa, Nómina, Pedido.

### Paso 4 — Prototipo de conversión

Para el primer archivo real de cada familia:
- inspeccionar estructura;
- generar CSV limpio manualmente o con script;
- importar en una base de prueba;
- revisar `ImportResult`;
- verificar visualmente en la app;
- documentar el procedimiento exacto aquí.

### Paso 5 — Decisión de automatización

Solo después del prototipo decidir:
- mantener conversión manual;
- crear script Python específico;
- añadir soporte Java nativo;
- aplazar por bajo volumen.

### Criterio de cierre del sprint

- Al menos un archivo real complejo convertido e importado correctamente en entorno de prueba.
- Procedimiento documentado en este archivo.
- Plantilla CSV destino documentada para la entidad afectada.
- Limitaciones anotadas.
- No tocar código Java salvo autorización explícita posterior.

---

## Convenciones del CSV destino

Reglas que evitan errores comunes al importar:

- Una tabla por archivo. Si el Excel original tiene varias mini-tablas en horizontal, separarlas en CSVs distintos.
- Cabeceras en fila 1. Sin títulos encima, sin filas vacías intermedias, sin sub-cabeceras.
- Sin celdas combinadas. Si las hay en el original, deshacer y rellenar valor hacia abajo donde corresponda.
- Sin fórmulas. Copiar todo, pegar como valores.
- Sin filas ni columnas totalmente vacías.
- Tipos consistentes por columna. Precios solo números, sin `€`, sin texto adicional, sin `"N/A"`. Decimales con punto.
- Nombres de columna planos: `tipo_papel`, `precio_resma`, no `Tipo de papel (€/resma)`.
- Encoding UTF-8 al guardar.
- Fechas en ISO `YYYY-MM-DD`. El `ImportService` normaliza fechas Excel (números seriales) a ISO antes de pasarlas a `EntityImportService`, pero si exportas a CSV manualmente, escribe directamente ISO para evitar ambigüedad.

---

## Estado actual del importador

Las 9 entidades del sistema soportan importación desde CSV, XLSX, XLS, XLSB, XLSM, XLTX, XLTM y JSON. Cada entidad tiene un `IMPORT_SPEC` público estático en su modelo.

### Formatos soportados (Sprint IMPORT-UPGRADE, `4bc6c9c`)

`ImportService.parseFile()` detecta la extensión y enruta:
- `.csv` / `.txt` → `parseCSV()`
- `.json` → `parseJSON()`
- `.xlsx` / `.xls` / `.xlsb` / `.xlsm` / `.xltx` / `.xltm` → `parseExcel()` via `WorkbookFactory.create(file, null, true)`

`WorkbookFactory` (Apache POI `poi-ooxml-full`) gestiona todos los formatos Excel de forma transparente. Ya no hay código separado para HSSF/XSSF.

### Creación de campos nuevos desde el diálogo de importación

`ColumnMappingDialog` tiene un botón "➕ Nuevo campo…" (visible solo para entidades planas con `tableName()` no nulo). Permite crear una columna dinámica en la tabla SQLite (`ALTER TABLE … ADD COLUMN`) y registrarla en `column_configs` directamente desde el asistente de importación, sin salir del flujo. Los valores de esa columna se escriben vía `DynamicColumnValueDAO.updateValues()` tras cada INSERT/UPDATE. **Entidades parent-child (Presupuesto, Factura, Albarán) no tienen este botón.**

### Tabla de entidades importables

| Entidad | Tipo | Clave de duplicado | Política default |
|---|---|---|---|
| Cliente | plana | `nif` | `SKIP_IF_EXISTS` |
| Material | plana | `codigo` | `SKIP_IF_EXISTS` |
| Empleado | plana | `dni` | `SKIP_IF_EXISTS` |
| Tarifa | plana | `(material_id, cliente_id)` | `SKIP_IF_EXISTS` |
| Nómina | plana | `(empleado_id, mes, anio)` | `SKIP_IF_EXISTS` |
| Pedido | plana | `numero` | `SKIP_IF_EXISTS` |
| Presupuesto | **parent-child** | `numero` | `SKIP_IF_EXISTS` |
| Factura | **parent-child** | `numero` | `SKIP_IF_EXISTS` |
| Albarán | **parent-child** | `numero` | `SKIP_IF_EXISTS` |

Política `SKIP_IF_EXISTS`: si la clave ya existe en BD, la fila se descarta como duplicado (sin error). `UPDATE_EXISTING` y `CREATE_NEW` están **bloqueadas** para parent-child (lanzan `IllegalArgumentException` al inicio de `importar()`).

### Modelo CSV ancho con cabecera repetida (parent-child)

Presupuesto, Factura y Albarán son entidades parent-child: una cabecera tiene N líneas. El modelo CSV es **ancho con cabecera repetida**: cada fila del CSV contiene los campos de cabecera + los de una línea. Las filas con el mismo `numero` se agrupan en un único registro parent + N líneas.

Ejemplo simplificado (Presupuesto, 2 cabeceras con 2 líneas cada una):

````
numero,cliente_nif,fecha,descripcion,cantidad,precio_unitario
PRES-001,B12345678,2026-05-01,Cartulina A4,100,0.15
PRES-001,B12345678,2026-05-01,Encuadernación,1,25.00
PRES-002,A87654321,2026-05-02,Folleto díptico,500,0.30
PRES-002,A87654321,2026-05-02,Diseño gráfico,1,80.00
````

**Política de coherencia intra-grupo (D1):** todos los campos de cabecera declarados en `spec.campos()` deben coincidir entre filas del mismo `numero`. Si una fila del grupo trae `fecha=2026-05-01` y otra `fecha=2026-05-02` para el mismo `numero`, el grupo se rechaza con `RowError` de tipo `INCONSISTENCIA_GRUPO`.

### FKs opcionales por número del CSV (D5)

Para evitar exigir IDs internos en los CSVs históricos, el importador resuelve FKs opcionales buscando por su número natural:

| Entidad | Columna CSV | FK resuelta | Comportamiento si vacío | Comportamiento si número no encontrado |
|---|---|---|---|---|
| Factura | `presupuesto_numero` | `presupuesto_id` | `setNull` (sin FK) | ERROR `FK_NO_ENCONTRADA` |
| Albarán | `factura_numero` | `factura_id` | `setNull` (sin FK) | ERROR `FK_NO_ENCONTRADA` |
| Albarán | `pedido_numero` | `pedido_id` | `setNull` (sin FK) | ERROR `FK_NO_ENCONTRADA` |

La FK de cliente NO usa este patrón: se resuelve por `nif` (match exacto requerido) o, si `nif` viene vacío, por `nombre+apellidos` (match único o ERROR). Empleado se resuelve por `nombre+apellidos`.

### Defaults aplicados en Java por entidad

Cuando una columna del CSV viene vacía y el campo tiene un default conocido, el importador lo aplica en Java durante `ensamblarX()`, antes del `save()`. Esto evita depender de los `DEFAULT` del DDL de SQLite, que **no se aplican cuando el DAO pasa `NULL` explícito** (lección aprendida en 5a.3, Deuda 20).

| Entidad | Campo | Default Java |
|---|---|---|
| Presupuesto | `fecha` | `LocalDate.now().toString()` |
| Presupuesto | `iva_porcentaje` | `21.0` |
| Presupuesto | `estado` | `'borrador'` |
| Factura | `fecha` | `LocalDate.now().toString()` |
| Factura | `iva_porcentaje` | `21.0` |
| Factura | `estado` | `'pendiente'` |
| Factura | `forma_pago` | (no se aplica en Java; el DDL aplica `'Transferencia bancaria'` solo si la columna se omite del INSERT) |
| Albarán | `fecha` | `LocalDate.now().toString()` |
| Albarán | `estado` | `'pendiente'` |

Si tu CSV trae un valor explícito, gana sobre el default.

### Limitaciones conocidas relevantes para el operador

- **Empleados inactivos:** corregido en Sprint C (`1851216`). La resolución de empleado para Nómina ya no filtra `activo = 1`, por lo que las nóminas históricas de empleados dados de baja pueden importarse si el empleado existe en la base de datos.
- **`cantidad` no se valida como numérico (Deuda 19):** si un CSV trae `cantidad=N/A` o similar en una línea de Presupuesto/Factura/Albarán/Pedido, persiste como `0` en BD sin error. Verificar manualmente con `Select-String -Pattern '[^0-9.,;]' archivo.csv` antes de importar.
- **Totales recalculados (no respetados):** los totales del CSV en Presupuesto/Factura se ignoran. El importador llama a `calcularTotales()` tras montar cabecera+líneas. Albarán no tiene totales (no hay precios ni IVA).
- **Tildes en match por nombre (Deuda 3):** SQLite `lower()` no normaliza tildes, así que `"García"` y `"Garcia"` no matchean. Si el match por `nombre+apellidos` falla con un nombre que sospechas existe, revisar acentuación literal en BD.

---

## Ejemplo concreto: xlsx con múltiples bloques (Fase 1)

Caso real procesado al inicio del proyecto: `PRECIOS_PAPEL_PROVEEDORES_Formulas.xlsx`. Excel del proveedor con 7 hojas, cada hoja con varias mini-tablas pegadas en horizontal (precios principales + sub-tablas por gramaje), celdas combinadas, fórmulas, filas en blanco como separadores.

**Diagnóstico de los problemas:**

- Múltiples mini-tablas por hoja (ESTUCADOS, OFFSET, SOPORSET, CORAL BOOK) pegadas a derecha con columnas vacías entre ellas.
- Cabeceras en fila 3 (la 1 es título, la 2 está vacía).
- 33 rangos de celdas combinadas por hoja.
- Fórmulas como `=B6/500`, `=H3*H13`.
- Filas vacías como separadores de grupo.
- Valores mezclados con texto en la misma columna (`"0,530 UNIDAD"`).

**Solución aplicada:**

Script Python con `openpyxl` que recorre cada hoja, detecta cabeceras conocidas (`TIPO DE PAPEL`, `TAMAÑOS`), extrae cada mini-tabla a un CSV plano normalizado, añade columna `proveedor`, redondea decimales, descarta filas sin nombre.

Resultado: 8 CSVs limpios, una entidad por archivo, importables directamente con el asistente.

**Plantilla de script (Python):**

```python
import openpyxl
import csv
from pathlib import Path

# data_only=True es indispensable: lee valores calculados, no fórmulas
wb = openpyxl.load_workbook('archivo.xlsx', data_only=True)

OUT = Path('salida_csvs')
OUT.mkdir(exist_ok=True)

def cell(ws, row, col):
    """Lee una celda y devuelve None si está vacía o es solo espacios."""
    v = ws.cell(row=row, column=col).value
    if v is None:
        return None
    if isinstance(v, str):
        v = v.strip()
        return v if v else None
    return v

def num(v):
    """Convierte a número o None."""
    if v is None:
        return None
    if isinstance(v, (int, float)):
        return float(v)
    try:
        return float(str(v).replace(',', '.'))
    except ValueError:
        return None

# Para cada hoja del libro
for sheet_name in wb.sheetnames:
    ws = wb[sheet_name]
    print(f"Hoja: {sheet_name}, {ws.max_row}x{ws.max_column}")

    # Inspeccionar primero las primeras 15 filas para entender la estructura
    for r in range(1, min(15, ws.max_row) + 1):
        row = [cell(ws, r, c) for c in range(1, ws.max_column + 1)]
        if any(v is not None for v in row):
            print(f"  R{r}: {row}")

# Una vez identificada la estructura, extraer la tabla principal
# Ejemplo: tabla en columnas A-D, cabeceras en fila 3, datos desde fila 5
rows = []
ws = wb['UNIÓN PAPELERA']
for r in range(5, ws.max_row + 1):
    tipo = cell(ws, r, 1)
    precio = cell(ws, r, 2)
    if tipo is None or precio is None:
        continue  # saltar separadores y categorías
    if str(tipo).upper().startswith('TIPO DE PAPEL'):
        continue  # saltar cabeceras repetidas
    rows.append([
        'UNION_PAPELERA',  # columna proveedor añadida
        str(tipo),
        round(num(precio), 4) if num(precio) is not None else None,
        round(num(cell(ws, r, 3)), 4) if num(cell(ws, r, 3)) is not None else None,
    ])

# Escribir CSV limpio
with open(OUT / 'precios_papel.csv', 'w', newline='', encoding='utf-8') as f:
    w = csv.writer(f)
    w.writerow(['proveedor', 'tipo_papel', 'precio_resma', 'precio_pliego'])
    w.writerows(rows)
```

**Trucos útiles:**

- `data_only=True` al cargar el workbook resuelve fórmulas a valores. Sin esto se leen como texto literal `=B3+C3`.
- Para detectar bloques laterales, recorrer todas las celdas y buscar cabeceras conocidas como sentinelas (ej. `TAMAÑOS`, `TIPO DE PAPEL`).
- `ws.merged_cells.ranges` lista los rangos combinados si necesitas saber dónde están.
- Redondear decimales a 4 cifras al escribir el CSV evita ruido tipo `61.71999999999999`.
- Para los 7-8 separadores decimales mezclados (algunos con coma, otros con punto), normalizar siempre a punto en el CSV destino.

**Verificación post-conversión:**

Antes de importar, abrir el CSV resultante en un editor de texto plano (no Excel, que oculta problemas) y comprobar:

- Cabecera en línea 1, una sola línea, sin filas vacías encima.
- Una coma como separador en cada línea (mismo número de comas en todas las filas).
- Decimales con punto, no coma.
- Sin caracteres raros tipo `\x84` (indica encoding incorrecto).
- Sin filas vacías al final.

---

## Otros formatos (xlsb, pdf, docx)

Estado 2026-06-12:

- **xlsb:** ya hay soporte Java parcial/útil vía `XSSFBEventBasedExcelExtractor`. Para `NUEVAS TARIFAS (2) (version 1).xlsb` se recupera texto tabulado y cabeceras reales, pero el libro contiene muchas secciones internas. Si se requiere importación perfecta, clasificar secciones y decidir si limpiar a CSV por script o crear plantilla específica.
- **pdf con tablas seleccionables:** probar `pdfplumber` o `tabula-py`. Aceptar que la extracción nunca es perfecta, siempre habrá filas malformadas que corregir.
- **pdf escaneado:** requiere OCR (Tesseract). Caso a evaluar si aparece.
- **docx con tablas:** `python-docx` puede leerlas.
- **docx texto libre:** inviable automatizar, copiar a mano.

Documentar aquí el procedimiento concreto cuando se procese el primer archivo de cada tipo. No diseñar a ciegas.

---

## Cuándo reevaluar Vía B (importador genérico nativo)

El soporte parent-child añadido en Sprint 2 ya cubre el caso de "Excel ancho con cabecera repetida". La Vía B sigue significando "importador capaz de tragarse Excel con estructura visual humana sin conversión manual previa". Posibles desencadenantes:

- Que el procesamiento manual supere las 10 horas/mes de forma sostenida.
- Que la empresa empiece a importar archivos recurrentes (mensuales) en formato siempre igual de un mismo origen. En ese caso, codificar plantilla de parseo específica para ese origen amortiza pronto.
- Que aparezcan formatos pdf/docx con estructura uniforme y suficiente volumen.

Si ninguno de los tres, mantener Vía A indefinidamente.

---

## Referencias del código

- `EntityImportService` (`src/main/java/org/gipsybuho/service/`): servicio de importación con 3 fases para entidades planas (mapeo, validación, transacción única) más Fase 2.5 (agrupación) y Fase 3 bifurcada (`insertarFilas` + `insertarGrupos`) para parent-child. Acepta `EntityImportSpec`, `List<Map<String, String>>` (filas parseadas), `mapping` y `DuplicatePolicy`.
- `ColumnMappingDialog` (`src/main/java/org/gipsybuho/ui/`): asistente visual de mapping de columnas.
- `ImportService` (`src/main/java/org/gipsybuho/service/`): parseo de CSV/Excel/JSON con detección estricta de encoding UTF-8/windows-1252 y normalización de fechas Excel a ISO.
- `EntityImportSpec` (`src/main/java/org/gipsybuho/service/importer/`): record con 7 campos. Los 3 últimos (`claveAgrupacion`, `campoLineas`, `specLinea`) son `null` para entidades planas y no-`null` para parent-child. Helper `esParentChild()` discrimina.
- **`IMPORT_SPEC` por entidad:** `Cliente.IMPORT_SPEC`, `Material.IMPORT_SPEC`, `Empleado.IMPORT_SPEC`, `Tarifa.IMPORT_SPEC`, `Nomina.IMPORT_SPEC`, `Pedido.IMPORT_SPEC`, `Presupuesto.IMPORT_SPEC`, `Factura.IMPORT_SPEC`, `Albaran.IMPORT_SPEC`. Las 3 últimas son parent-child y declaran `specLinea` con los campos de `LineaPresupuesto`/`LineaFactura`/`LineaAlbaran`.
