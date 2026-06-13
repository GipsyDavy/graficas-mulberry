"""Limpia 5c_material_otros.csv:
- Descarta filas sin nombre real o sin precio extraíble
- Recupera precio de campo longitud_o_unidad cuando precio está vacío pero contiene número
- Genera 5c_material_otros_limpio.csv con columnas compatibles con Material.IMPORT_SPEC
"""
import csv, re
from pathlib import Path

SRC = Path(r"C:\Users\Gipsy Dávy\Desktop\files\5c_material_otros.csv")
OUT = Path(r"C:\Users\Gipsy Dávy\Desktop\files\5c_material_otros_limpio.csv")

NOTAS = {
    "0,22 m/lineal de plástico",
    "5 € de arranque de maquina para  el plastificado",
}


def extraer_numero(s):
    """Extrae el primer número de un string como '0,530 UNIDAD' → 0.53"""
    if s is None:
        return None
    m = re.search(r"[\d]+[,.][\d]+|[\d]+", str(s).replace(",", "."))
    return round(float(m.group().replace(",", ".")), 4) if m else None


def main():
    rows_in, rows_out, descartadas = 0, 0, 0
    with open(SRC, encoding="utf-8") as f:
        reader = csv.DictReader(f)
        out_rows = []
        for row in reader:
            rows_in += 1
            nombre = (row.get("modelo") or "").strip()
            if not nombre or nombre.lower() in NOTAS:
                print(f"  DESCARTADO (nota): {nombre!r}")
                descartadas += 1
                continue

            precio_raw = (row.get("precio") or "").strip()
            unidad_raw = (row.get("longitud_o_unidad") or "").strip()

            # Intentar precio directo
            precio = extraer_numero(precio_raw) if precio_raw else None

            # Si precio vacío, intentar recuperar de longitud_o_unidad
            if precio is None and unidad_raw:
                precio = extraer_numero(unidad_raw)
                if precio is not None:
                    print(f"  RECUPERADO precio de longitud_o_unidad: {nombre!r} -> {precio}")
                    unidad_raw = ""  # no poner el texto mezclado como unidad

            if precio is None:
                print(f"  DESCARTADO (sin precio): {nombre!r}")
                descartadas += 1
                continue

            out_rows.append({
                "nombre": nombre,
                "categoria": "consumibles",
                "unidad": unidad_raw if unidad_raw and not re.search(r"[€$]|ROLLO|UNIDAD", unidad_raw.upper()) else "ud",
                "precio_unidad": precio,
                "proveedor": (row.get("proveedor") or "CODIAL").strip(),
            })
            rows_out += 1

    with open(OUT, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["nombre", "categoria", "unidad", "precio_unidad", "proveedor"])
        writer.writeheader()
        writer.writerows(out_rows)

    print(f"\nEntrada: {rows_in} | Importables: {rows_out} | Descartadas: {descartadas}")
    print(f"Salida: {OUT}")
    for r in out_rows:
        print(f"  {r['nombre'][:50]:<50} | {r['precio_unidad']:>10} | {r['proveedor']}")


if __name__ == "__main__":
    main()
