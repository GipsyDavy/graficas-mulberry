"""
Importa los CSVs de materiales pendientes directamente en la BD SQLite.
Equivalente al wizard JavaFX con politica SKIP_IF_EXISTS por (nombre, proveedor).

Uso:
    python scripts/importar_materiales.py [--dry-run]

Orden de importacion:
    1. 1_precios_papel_proveedor.csv       (84 filas)
    2. 2_precios_papel_por_gramaje.csv     (330 filas)
    3. 3_union_papelera_otros_productos.csv (34 filas)
    4. 5a_material_tintas.csv              (4 filas)
    5. 5b_material_plastico.csv            (17 filas)
    6. 5c_material_otros_limpio.csv        (5 filas)
"""

import sqlite3
import csv
import os
import sys
from pathlib import Path

DRY_RUN = "--dry-run" in sys.argv

DB_PATH = Path(os.environ.get("APPDATA", "")).parent / "Local" / "GraficasMulberry" / "graficas_mulberry.db"
FILES_DIR = Path(os.environ.get("USERPROFILE", "")) / "Desktop" / "files"


def to_float(v):
    if v is None or str(v).strip() == "":
        return None
    try:
        return float(str(v).replace(",", ".").strip())
    except ValueError:
        return None


def load_csv(path, encoding="utf-8"):
    rows = []
    with open(path, encoding=encoding, newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append({k.strip(): v.strip() if v else "" for k, v in row.items()})
    return rows


def insert_material(cur, nombre, categoria, unidad, precio_unidad, proveedor, referencia=None, dry_run=False):
    if not nombre:
        return "SKIP_EMPTY"

    # Dedup por (nombre, proveedor)
    prov = proveedor or ""
    cur.execute(
        "SELECT id FROM materiales WHERE nombre = ? AND COALESCE(proveedor,'') = ?",
        (nombre, prov)
    )
    if cur.fetchone():
        return "SKIP_DUP"

    if not dry_run:
        cur.execute(
            """INSERT INTO materiales (nombre, referencia, categoria, unidad, precio_unidad, proveedor)
               VALUES (?, ?, ?, ?, ?, ?)""",
            (
                nombre,
                referencia or None,
                categoria or "consumibles",
                unidad or "ud",
                precio_unidad if precio_unidad is not None else 0.0,
                proveedor or None,
            )
        )
    return "INSERT"


def run():
    con = sqlite3.connect(str(DB_PATH))
    cur = con.cursor()

    totals = {}

    def process(label, rows_fn):
        results = {"INSERT": 0, "SKIP_DUP": 0, "SKIP_EMPTY": 0, "ERROR": 0}
        for r in rows_fn():
            res = r
            results[res] = results.get(res, 0) + 1
        totals[label] = results
        mode = "[DRY-RUN] " if DRY_RUN else ""
        print(f"{mode}{label}: insert={results['INSERT']} dup={results['SKIP_DUP']} vacio={results['SKIP_EMPTY']}")

    # ── 1. 1_precios_papel_proveedor.csv ─────────────────────────────
    def csv1():
        rows = load_csv(FILES_DIR / "1_precios_papel_proveedor.csv")
        for r in rows:
            nombre = r.get("tipo_papel", "")
            proveedor = r.get("proveedor", "")
            precio = to_float(r.get("precio_resma"))
            yield insert_material(cur, nombre, "papel proveedor", "resma", precio, proveedor, dry_run=DRY_RUN)

    process("1_precios_papel_proveedor", csv1)

    # ── 2. 2_precios_papel_por_gramaje.csv ───────────────────────────
    # nombre descriptivo: familia + gramaje + tamano para distinguir variantes
    def csv2():
        rows = load_csv(FILES_DIR / "2_precios_papel_por_gramaje.csv")
        for r in rows:
            familia = r.get("familia", "")
            gramaje = r.get("gramaje_g", "")
            tamano = r.get("tamano", "")
            if not familia:
                yield "SKIP_EMPTY"
                continue
            parts = [p for p in [familia, (gramaje + "g") if gramaje else "", tamano] if p]
            nombre = " ".join(parts)
            proveedor = r.get("proveedor", "")
            precio = to_float(r.get("eur_resma"))
            yield insert_material(cur, nombre, "papel gramaje", "resma", precio, proveedor, dry_run=DRY_RUN)

    process("2_precios_papel_por_gramaje", csv2)

    # ── 3. 3_union_papelera_otros_productos.csv ──────────────────────
    def csv3():
        rows = load_csv(FILES_DIR / "3_union_papelera_otros_productos.csv")
        for r in rows:
            nombre = r.get("producto", "")
            proveedor = r.get("proveedor", "")
            categoria = r.get("categoria", "")
            referencia = r.get("sref", "") or None
            precio = to_float(r.get("precio_pliego_o_unidad")) or to_float(r.get("precio_resma_o_millar"))
            yield insert_material(cur, nombre, categoria or "otros", "ud", precio, proveedor, referencia=referencia, dry_run=DRY_RUN)

    process("3_union_papelera_otros_productos", csv3)

    # ── 4. 5a_material_tintas.csv ────────────────────────────────────
    def csv4():
        rows = load_csv(FILES_DIR / "5a_material_tintas.csv")
        for r in rows:
            nombre = r.get("modelo", "")
            proveedor = r.get("proveedor", "")
            precio = to_float(r.get("precio_unidad"))
            yield insert_material(cur, nombre, "tintas", "ud", precio, proveedor, dry_run=DRY_RUN)

    process("5a_material_tintas", csv4)

    # ── 5. 5b_material_plastico.csv ──────────────────────────────────
    def csv5():
        rows = load_csv(FILES_DIR / "5b_material_plastico.csv")
        for r in rows:
            nombre = r.get("modelo", "")
            proveedor = r.get("proveedor", "")
            precio = to_float(r.get("precio_unidad")) or to_float(r.get("precio"))
            longitud = r.get("longitud", "")
            unidad = ("m " + longitud).strip() if longitud else "ud"
            yield insert_material(cur, nombre, "plastico", unidad, precio, proveedor, dry_run=DRY_RUN)

    process("5b_material_plastico", csv5)

    # ── 6. 5c_material_otros_limpio.csv ──────────────────────────────
    def csv6():
        rows = load_csv(FILES_DIR / "5c_material_otros_limpio.csv")
        for r in rows:
            nombre = r.get("nombre", "")
            proveedor = r.get("proveedor", "")
            categoria = r.get("categoria", "")
            unidad = r.get("unidad", "")
            precio = to_float(r.get("precio_unidad"))
            yield insert_material(cur, nombre, categoria, unidad, precio, proveedor, dry_run=DRY_RUN)

    process("5c_material_otros_limpio", csv6)

    # ── Commit y resumen ─────────────────────────────────────────────
    if not DRY_RUN:
        con.commit()

    con.close()

    total_insert = sum(v["INSERT"] for v in totals.values())
    total_dup = sum(v["SKIP_DUP"] for v in totals.values())
    print()
    print(f"TOTAL: {total_insert} insertados, {total_dup} omitidos por duplicado")
    if DRY_RUN:
        print("(dry-run: ninguna fila escrita en BD)")


if __name__ == "__main__":
    if not DB_PATH.exists():
        print(f"ERROR: BD no encontrada en {DB_PATH}")
        sys.exit(1)
    print(f"BD: {DB_PATH}")
    print(f"CSVs: {FILES_DIR}")
    print(f"Modo: {'DRY-RUN' if DRY_RUN else 'REAL'}")
    print()
    run()
