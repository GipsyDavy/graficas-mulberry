# Migración de datos históricos — Procedimiento Vía A

README técnico sobre cómo procesar archivos históricos de la empresa cliente cuando lleguen.

**Última revisión:** 17/05/2026
**Estado:** Vía A activa. Vía B/C reevaluables tras ver los primeros archivos reales.

---

## Contexto

La empresa cliente trabaja con archivos en formato humano: Excel con celdas combinadas, varias hojas por libro, cabeceras decorativas, bloques laterales, totales calculados con fórmulas. También PDFs y Words generados desde software de terceros.

Estos archivos no son importables directamente con el asistente de mapping (`ColumnMappingDialog` + `EntityImportService`) porque dicho asistente asume **CSV plano**: una fila = un registro, cabeceras en fila 1, sin estructura visual humana.

Sprint 2 cerrado por Vía A en mayo 2026:
- No se construye importador genérico capaz de tragarse formatos humanos.
- Conversión manual a CSV limpio archivo por archivo.
- Import con el asistente de Fase 1 ya existente.
- Los botones "Importar" de Albaranes/Facturas/Pedidos/Nóminas/Presupuestos muestran Alert "Funcionalidad próximamente" en lugar de intentar importar.

---

## Procedimiento general

Para cada archivo que llegue:

1. **Inspeccionar estructura.** Abrir el archivo, identificar qué entidades del sistema contiene, cómo están organizados los datos.
2. **Identificar campos relevantes por entidad.** Qué columnas son cabeceras reales (no decorativas), qué celdas combinadas hay, qué bloques laterales.
3. **Generar CSV limpio.** Un CSV por entidad. Una fila = un registro. Cabeceras en fila 1. Sin combinaciones, sin fórmulas, decimales con punto, UTF-8.
4. **Mapear contra el IMPORT_SPEC.** Las claves del CSV deben encajar con las claves del `IMPORT_SPEC` del modelo correspondiente. Ver `Material.IMPORT_SPEC`, `Cliente.IMPORT_SPEC`, etc. en `src/main/java/org/gipsybuho/model/`.
5. **Importar desde la app.** Botón Importar de la vista → seleccionar CSV → `ColumnMappingDialog` resuelve el mapping casi todo automático → confirmar → revisar `ImportResult` (importadas, descartadas, errores).
6. **Validar visualmente.** Abrir la tabla en la app, comprobar que los registros aparecen correctamente.

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

Pendiente de ver archivos reales para decidir herramienta. Cuando lleguen:

- **xlsb:** evaluar si convertir a xlsx con LibreOffice antes de procesar (lo más simple) o usar `pyxlsb` directamente. `pyxlsb` no resuelve fórmulas, así que si el archivo las tiene, convertir primero.
- **pdf con tablas seleccionables:** probar `pdfplumber` o `tabula-py`. Aceptar que la extracción nunca es perfecta, siempre habrá filas malformadas que corregir.
- **pdf escaneado:** requiere OCR (Tesseract). Caso a evaluar si aparece.
- **docx con tablas:** `python-docx` puede leerlas.
- **docx texto libre:** inviable automatizar, copiar a mano.

Documentar aquí el procedimiento concreto cuando se procese el primer archivo de cada tipo. No diseñar a ciegas.

---

## Cuándo reevaluar Vía B (importador genérico nativo)

Posibles desencadenantes:

- Que el procesamiento manual supere las 10 horas/mes de forma sostenida.
- Que la empresa empiece a importar archivos recurrentes (mensuales) en formato siempre igual de un mismo origen. En ese caso, codificar plantilla de parseo específica para ese origen amortiza pronto.
- Que aparezcan formatos pdf/docx con estructura uniforme y suficiente volumen.

Si ninguno de los tres, mantener Vía A indefinidamente.

---

## Referencias del código

- `EntityImportService` (`src/main/java/org/gipsybuho/service/`): servicio de importación con 3 fases (mapeo, validación, transacción única). Acepta `EntityImportSpec`, `List<Map<String, String>>` (filas parseadas), `mapping` y `DuplicatePolicy`.
- `ColumnMappingDialog` (`src/main/java/org/gipsybuho/ui/`): asistente visual de mapping de columnas.
- `ImportService` (`src/main/java/org/gipsybuho/service/`): parseo de CSV/Excel/JSON con detección estricta de encoding UTF-8/windows-1252 y normalización de fechas Excel a ISO.
- `Material.IMPORT_SPEC`, `Cliente.IMPORT_SPEC`, `Empleado.IMPORT_SPEC`, `Tarifa.IMPORT_SPEC`: especificaciones de campos importables por entidad plana. Definir nuevos `IMPORT_SPEC` solo cuando se decida abordar entidades parent-child (Sprint futuro Vía B/C).
