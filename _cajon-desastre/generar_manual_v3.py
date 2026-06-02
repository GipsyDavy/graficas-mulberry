# -*- coding: utf-8 -*-
"""Manual de usuario v3 - Graficas Mulberry - Todos los modulos."""

from fpdf import FPDF
from fpdf.enums import XPos, YPos
import os

MULBERRY       = (107,  45,  94)
MULBERRY_LIGHT = (180, 130, 170)
MULBERRY_BG    = (245, 235, 243)
DARK           = ( 20,  20,  25)
GRIS           = (100, 100, 110)
GRIS_CLARO     = (242, 242, 245)
BLANCO         = (255, 255, 255)
VERDE          = ( 34, 153,  84)
VERDE_BG       = (230, 250, 238)
NARANJA        = (230, 126,  34)
NARANJA_BG     = (254, 245, 231)
ROJO           = (192,  57,  43)
AZUL           = ( 52, 120, 200)
AZUL_BG        = (235, 243, 255)

LOGO_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)),
    "src", "main", "resources", "org", "gipsybuho", "img", "logo.jpg")


class ManualPDF(FPDF):

    def header(self):
        if self.page_no() == 1:
            return
        self.set_fill_color(*MULBERRY)
        self.rect(0, 0, 210, 11, style="F")
        self.set_font("Helvetica", "B", 8)
        self.set_text_color(*BLANCO)
        self.set_xy(10, 1.5)
        self.cell(140, 8, "Graficas Mulberry  -  Manual de Usuario v3", align="L")
        self.set_xy(0, 1.5)
        self.cell(198, 8, f"Pagina {self.page_no()}", align="R")
        self.set_text_color(*DARK)
        self.ln(12)

    def footer(self):
        if self.page_no() == 1:
            return
        self.set_y(-13)
        self.set_draw_color(*MULBERRY)
        self.set_line_width(0.4)
        self.line(10, self.get_y(), 200, self.get_y())
        self.set_font("Helvetica", "", 7.5)
        self.set_text_color(*GRIS)
        self.cell(0, 7,
            "Graficas Mulberry  |  www.graficasmulberry.com  |  info@graficasmulberry.com  |  Tel. 950 30 61 64",
            align="C")

    # ── helpers ──────────────────────────────────────────────────────────────

    def titulo_seccion(self, texto, numero=""):
        self.ln(3)
        self.set_fill_color(*MULBERRY)
        self.set_text_color(*BLANCO)
        self.set_font("Helvetica", "B", 12)
        txt = f"  {numero}   {texto}" if numero else f"  {texto}"
        self.cell(0, 10, txt, new_x=XPos.LMARGIN, new_y=YPos.NEXT, fill=True)
        self.set_text_color(*DARK)
        self.ln(2)

    def titulo_sub(self, texto):
        self.ln(1)
        self.set_fill_color(*MULBERRY_BG)
        self.set_draw_color(*MULBERRY)
        self.set_text_color(*MULBERRY)
        self.set_font("Helvetica", "B", 10)
        self.cell(0, 7, f"  {texto}", new_x=XPos.LMARGIN, new_y=YPos.NEXT, fill=True)
        self.set_text_color(*DARK)
        self.set_draw_color(*DARK)
        self.ln(1)

    def parrafo(self, texto, indent=0):
        self.set_font("Helvetica", "", 9.5)
        self.set_text_color(*DARK)
        self.set_x(10 + indent)
        self.multi_cell(190 - indent, 5.2, texto, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(1)

    def vineta(self, items, color=None, indent=0):
        self.set_font("Helvetica", "", 9.5)
        col = color or DARK
        self.set_text_color(*col)
        for item in items:
            self.set_x(14 + indent)
            self.cell(6, 5.2, "•")
            self.set_x(20 + indent)
            self.multi_cell(174 - indent, 5.2, item, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_text_color(*DARK)
        self.ln(1)

    def codigo(self, lineas):
        self.set_fill_color(30, 22, 30)
        self.set_text_color(200, 235, 200)
        self.set_font("Courier", "", 8.5)
        self.set_x(10)
        ancho = 190
        pad = 3.5
        alto = len(lineas) * 5 + pad * 2
        self.rect(10, self.get_y(), ancho, alto, style="F")
        self.set_xy(14, self.get_y() + pad)
        for ln in lineas:
            self.cell(ancho - 4, 5, ln, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            self.set_x(14)
        self.ln(3.5)
        self.set_text_color(*DARK)

    def tabla(self, cabeceras, filas, anchos):
        self.set_font("Helvetica", "B", 8.5)
        self.set_fill_color(*MULBERRY)
        self.set_text_color(*BLANCO)
        for i, cab in enumerate(cabeceras):
            self.cell(anchos[i], 7, f" {cab}", border=0, fill=True)
        self.ln()
        self.set_font("Helvetica", "", 8.5)
        par = False
        for fila in filas:
            bg = GRIS_CLARO if par else BLANCO
            self.set_fill_color(*bg)
            self.set_text_color(*DARK)
            y0 = self.get_y()
            for i, celda in enumerate(fila):
                self.set_xy(self.get_x(), y0)
                self.multi_cell(anchos[i], 6.5, f" {celda}",
                                border=0, fill=True,
                                new_x=XPos.RIGHT, new_y=YPos.TOP)
            self.ln(6.5)
            par = not par
        self.ln(2)

    def caja(self, titulo, cuerpo, color=None, bg=None):
        col = color or AZUL
        fondo = bg or AZUL_BG
        self.set_fill_color(*col)
        self.set_text_color(*BLANCO)
        self.set_font("Helvetica", "B", 9)
        self.cell(0, 6.5, f"  {titulo}", fill=True,
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_fill_color(*fondo)
        self.set_text_color(*DARK)
        self.set_font("Helvetica", "", 9)
        self.multi_cell(0, 5.2, f"  {cuerpo}", fill=True,
                        new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(2.5)

    def paso(self, num, texto, detalle=""):
        self.set_fill_color(*MULBERRY)
        self.set_text_color(*BLANCO)
        self.set_font("Helvetica", "B", 9.5)
        self.cell(7, 7, str(num), align="C", fill=True)
        self.set_text_color(*DARK)
        self.set_font("Helvetica", "B", 9.5)
        self.cell(0, 7, f"  {texto}", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        if detalle:
            self.set_font("Helvetica", "", 8.5)
            self.set_x(17)
            self.set_text_color(*GRIS)
            self.multi_cell(177, 5, detalle, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            self.set_text_color(*DARK)
        self.ln(1.5)

    def problema(self, titulo, solucion):
        self.set_font("Helvetica", "B", 9.5)
        self.set_text_color(*ROJO)
        self.set_x(12)
        self.cell(5, 7, ">")
        self.cell(0, 7, titulo, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_font("Helvetica", "", 9)
        self.set_text_color(*DARK)
        self.set_x(20)
        self.multi_cell(174, 5, solucion, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(1.5)

    def modulo_titulo(self, icono, nombre, descripcion):
        self.ln(2)
        self.set_fill_color(*MULBERRY)
        self.set_text_color(*BLANCO)
        self.set_font("Helvetica", "B", 11)
        self.cell(0, 9, f"  {icono}  {nombre}", fill=True,
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_font("Helvetica", "I", 9)
        self.set_text_color(*GRIS)
        self.cell(0, 6, f"  {descripcion}", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_text_color(*DARK)
        self.ln(1)

    def estado_badge(self, estados):
        self.set_font("Helvetica", "B", 8.5)
        colores = {
            "borrador":   (GRIS,    GRIS_CLARO),
            "enviado":    (AZUL,    AZUL_BG),
            "aceptado":   (VERDE,   VERDE_BG),
            "rechazado":  (ROJO,    (255,230,228)),
            "facturado":  (MULBERRY, MULBERRY_BG),
            "pendiente":  (NARANJA, NARANJA_BG),
            "pagada":     (VERDE,   VERDE_BG),
            "vencida":    (ROJO,    (255,230,228)),
            "anulada":    (GRIS,    GRIS_CLARO),
            "firmado":    (VERDE,   VERDE_BG),
            "entregado":  (AZUL,    AZUL_BG),
            "activo":     (VERDE,   VERDE_BG),
            "baja":       (ROJO,    (255,230,228)),
        }
        self.set_x(10)
        for estado in estados:
            key = estado.lower().replace(" ", "")
            fg, bg2 = colores.get(key, (DARK, GRIS_CLARO))
            self.set_fill_color(*bg2)
            self.set_text_color(*fg)
            self.cell(len(estado)*2.8 + 4, 6, f" {estado} ", fill=True)
            self.cell(3, 6, "")
        self.ln(9)
        self.set_text_color(*DARK)


# ============================================================================
def generar():
    pdf = ManualPDF(orientation="P", unit="mm", format="A4")
    pdf.set_auto_page_break(auto=True, margin=16)
    pdf.set_margins(10, 15, 10)

    # ════════════════════════════════════════════════════════════════════════
    # PORTADA
    # ════════════════════════════════════════════════════════════════════════
    pdf.add_page()
    pdf.set_fill_color(*MULBERRY)
    pdf.rect(0, 0, 210, 297, style="F")

    # Franja decorativa
    pdf.set_fill_color(*MULBERRY_LIGHT)
    pdf.rect(0, 78, 210, 1.5, style="F")
    pdf.rect(0, 195, 210, 1.5, style="F")

    if os.path.exists(LOGO_PATH):
        pdf.image(LOGO_PATH, x=75, y=18, w=60)
        pdf.set_y(85)
    else:
        pdf.set_y(55)

    pdf.set_text_color(*BLANCO)
    pdf.set_font("Helvetica", "B", 30)
    pdf.cell(0, 14, "GRAFICAS MULBERRY", align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_font("Helvetica", "", 14)
    pdf.set_text_color(*MULBERRY_LIGHT)
    pdf.cell(0, 7, "Sistema de Gestion Empresarial", align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(5)
    pdf.set_font("Helvetica", "B", 13)
    pdf.set_text_color(*BLANCO)
    pdf.cell(0, 7, "MANUAL DE USUARIO", align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_font("Helvetica", "", 10)
    pdf.set_text_color(*MULBERRY_LIGHT)
    pdf.cell(0, 6, "Version 3  |  Edicion completa  |  2025", align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    pdf.ln(8)
    # Caja de modulos
    caja_y = pdf.get_y()
    pdf.set_fill_color(20, 10, 18)
    pdf.rect(25, caja_y, 160, 90, style="F")

    modulos_info = [
        ("[CLIENTES]  ", "Gestion de clientes y base de datos"),
        ("[PRES/FACT] ", "Presupuestos, Facturas y Albaranes"),
        ("[PEDIDOS]   ", "Pedidos y control de pagos"),
        ("[MATERIALES]", "Stock, consumo y proveedores"),
        ("[NOMINAS]   ", "Nominas con SS espanola 2025"),
        ("[ESTADIST.] ", "Graficas financieras y de produccion"),
        ("[CALENDARIO]", "Notas y recordatorios"),
        ("[ ASIST. IA]", "Inteligencia artificial local (Ollama)"),
        ("[IMPORTAR]  ", "Importacion CSV / Excel / JSON"),
        ("[BACKUP]    ", "Exportacion y copias de seguridad"),
        ("[CONFIG]    ", "Temas, empresa y preferencias"),
    ]
    pdf.set_xy(33, caja_y + 5)
    for marcador, texto in modulos_info:
        pdf.set_font("Courier", "B", 8.5)
        pdf.set_text_color(*MULBERRY_LIGHT)
        pdf.set_x(33)
        pdf.cell(34, 7.5, marcador)
        pdf.set_font("Helvetica", "", 9)
        pdf.set_text_color(*BLANCO)
        pdf.cell(0, 7.5, texto, new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    pdf.set_y(200)
    pdf.set_text_color(*MULBERRY_LIGHT)
    pdf.set_font("Helvetica", "", 8.5)
    pdf.cell(0, 5, "Huercal de Almeria  |  Almeria  |  Espana", align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.cell(0, 5, "www.graficasmulberry.com  |  info@graficasmulberry.com  |  Tel. 950 30 61 64",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    # ════════════════════════════════════════════════════════════════════════
    # INDICE
    # ════════════════════════════════════════════════════════════════════════
    pdf.add_page()
    pdf.set_fill_color(*MULBERRY)
    pdf.set_text_color(*BLANCO)
    pdf.set_font("Helvetica", "B", 16)
    pdf.cell(0, 11, "  INDICE DE CONTENIDOS", fill=True,
             new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(4)

    secciones = [
        ("1.", "Introduccion y primeros pasos",          True),
        ("  1.1", "Requisitos del sistema",              False),
        ("  1.2", "Instalacion",                         False),
        ("  1.3", "Primera ejecucion",                   False),
        ("2.", "Interfaz general",                       True),
        ("3.", "Modulos de la aplicacion",               True),
        ("  3.1", "Panel principal (Dashboard)",         False),
        ("  3.2", "Clientes",                            False),
        ("  3.3", "Presupuestos",                        False),
        ("  3.4", "Facturas",                            False),
        ("  3.5", "Albaranes",                           False),
        ("  3.6", "Pedidos y Control de pagos",          False),
        ("  3.7", "Tarifas",                             False),
        ("  3.8", "Materiales y Stock",                  False),
        ("  3.9", "Empleados",                           False),
        ("  3.10", "Nominas",                            False),
        ("  3.11", "Estadisticas y Analisis",            False),
        ("  3.12", "Calendario y Recordatorios",         False),
        ("  3.13", "Asistente IA (Ollama)",              False),
        ("  3.14", "Importacion de datos",               False),
        ("  3.15", "Exportacion y Copias de seguridad",  False),
        ("  3.16", "Configuracion",                      False),
        ("4.", "Flujos de trabajo principales",          True),
        ("5.", "Generacion de documentos PDF",           True),
        ("6.", "Base de datos",                          True),
        ("7.", "Solucion de problemas frecuentes",       True),
        ("8.", "Soporte tecnico",                        True),
    ]

    for i, (num, tit, principal) in enumerate(secciones):
        pdf.set_font("Helvetica", "B" if principal else "", 10 if principal else 9)
        bg = MULBERRY_BG if (i % 2 == 0 and principal) else (GRIS_CLARO if i % 2 == 0 else BLANCO)
        pdf.set_fill_color(*bg)
        indent = 0 if principal else 8
        pdf.set_x(10 + indent)
        pdf.set_text_color(*(MULBERRY if principal else DARK))
        pdf.cell(185 - indent, 6.5, f"  {num}   {tit}",
                 fill=True, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(5)

    pdf.caja(
        "Sobre este manual",
        "Este manual cubre la version 3 del Sistema de Gestion de Graficas Mulberry.\n"
        "Describe todos los modulos disponibles: gestion comercial, produccion, personal,\n"
        "estadisticas, importacion/exportacion, inteligencia artificial local y configuracion.\n"
        "La aplicacion funciona completamente sin conexion a Internet (modo offline).",
        color=AZUL, bg=AZUL_BG)

    # ════════════════════════════════════════════════════════════════════════
    # 1. INTRODUCCION Y PRIMEROS PASOS
    # ════════════════════════════════════════════════════════════════════════
    pdf.add_page()
    pdf.titulo_seccion("INTRODUCCION Y PRIMEROS PASOS", "1")

    pdf.titulo_sub("1.1  Requisitos del sistema")
    pdf.tabla(
        ["Requisito", "Minimo", "Recomendado"],
        [
            ["Sistema operativo",    "Windows 10 (64 bits)",         "Windows 11"],
            ["Procesador",           "Intel/AMD 2 GHz dual-core",    "4 nucleos o mas"],
            ["Memoria RAM",          "4 GB",                         "8 GB (16 GB con IA)"],
            ["Espacio en disco",     "500 MB",                       "2 GB"],
            ["Resolucion pantalla",  "1280 x 720",                   "1920 x 1080"],
            ["Java",                 "JDK 21 o superior",            "JDK 21 LTS"],
            ["Internet",             "Solo instalacion inicial",      "No necesario"],
        ],
        [50, 65, 72]
    )

    pdf.titulo_sub("1.2  Instalacion")
    pdf.paso(1, "Abrir el instalador",
             "Ejecuta el archivo GraficasMulberry_Setup_v3.exe y sigue el asistente de instalacion.")
    pdf.paso(2, "Elegir la carpeta de destino",
             "Por defecto se instala en C:\\Program Files\\Graficas Mulberry\\. Puedes cambiarlo.")
    pdf.paso(3, "Finalizar la instalacion",
             "Haz clic en 'Instalar' y despues en 'Finalizar'. Se creara un acceso directo en el escritorio.")

    pdf.titulo_sub("1.3  Primera ejecucion")
    pdf.paso(1, "Iniciar la aplicacion",
             "Haz doble clic en el acceso directo 'Graficas Mulberry' del escritorio.")
    pdf.paso(2, "Base de datos inicial",
             "La primera vez se crea automaticamente la base de datos con datos de ejemplo:\n"
             "10 tarifas predefinidas (serigrafia, DTF, bordado...), 10 materiales con stock inicial\n"
             "y la configuracion de empresa de Graficas Mulberry.")
    pdf.paso(3, "Configurar los datos de tu empresa",
             "Ve a Configuracion (icono de engranaje en la barra lateral) > pestana 'Mi Empresa'\n"
             "y rellena tus datos reales: nombre, NIF, direccion, telefono, email y logo.")
    pdf.paso(4, "Cargar el logo",
             "En Configuracion > Mi Empresa, haz clic en 'Seleccionar logo'. El logo aparecera\n"
             "en la cabecera de todos los documentos PDF generados.")

    pdf.caja("Consejo  -  Primer uso",
             "Antes de empezar a trabajar, rellena tus datos de empresa en Configuracion.\n"
             "Esto garantiza que todos los documentos PDF se generen con tu informacion correcta.",
             color=VERDE, bg=VERDE_BG)

    # ════════════════════════════════════════════════════════════════════════
    # 2. INTERFAZ GENERAL
    # ════════════════════════════════════════════════════════════════════════
    pdf.add_page()
    pdf.titulo_seccion("INTERFAZ GENERAL", "2")

    pdf.parrafo(
        "La interfaz de Graficas Mulberry se divide en dos zonas principales: la barra lateral\n"
        "de navegacion (izquierda) y el area de contenido (centro y derecha).")

    pdf.titulo_sub("Barra lateral de navegacion")
    pdf.parrafo("La barra lateral muestra todos los modulos con iconos descriptivos:")
    pdf.tabla(
        ["Icono / Nombre", "Modulo"],
        [
            ["Inicio / Dashboard",      "Resumen general del negocio"],
            ["Clientes",                "Gestion de clientes"],
            ["Presupuestos",            "Crear y gestionar presupuestos"],
            ["Facturas",                "Facturacion y cobros"],
            ["Albaranes",               "Control de entregas"],
            ["Pedidos",                 "Pedidos a proveedores y control de pagos"],
            ["Tarifas",                 "Catalogo de precios por tecnica"],
            ["Materiales",              "Stock, consumo y pagos a proveedores"],
            ["Empleados",               "Gestion de personal"],
            ["Nominas",                 "Calculo y generacion de nominas"],
            ["Estadisticas",            "Graficas de ingresos, gastos y produccion"],
            ["Calendario",              "Recordatorios y notas"],
            ["Asistente IA",            "Chat con inteligencia artificial local"],
            ["Importar",                "Importacion desde CSV, Excel o JSON"],
            ["Exportar / Backup",       "Copias de seguridad y exportacion de datos"],
            ["Configuracion",           "Apariencia, empresa y preferencias"],
        ],
        [70, 118]
    )

    pdf.titulo_sub("Elementos comunes de cada modulo")
    pdf.vineta([
        "Barra de botones superior: Nuevo, Editar, Exportar PDF, Eliminar (segun modulo)",
        "Campo de busqueda: filtra los registros en tiempo real al escribir",
        "Tabla central: lista todos los registros del modulo con columnas ordenables",
        "Panel de detalle (en algunos modulos): muestra informacion ampliada del elemento seleccionado",
        "Botones con colores: azul=crear, naranja=editar, rojo=eliminar, verde=confirmar/guardar",
    ])

    pdf.titulo_sub("Atajos de teclado")
    pdf.tabla(
        ["Tecla", "Accion"],
        [
            ["Enter (en tabla)",     "Abrir dialogo de edicion del elemento seleccionado"],
            ["Supr / Delete",        "Eliminar el elemento seleccionado (pide confirmacion)"],
            ["Esc",                  "Cerrar el dialogo actual sin guardar"],
            ["Ctrl+N",               "Nuevo registro (en modulos que lo soportan)"],
            ["Ctrl+S",               "Guardar cambios del dialogo abierto"],
        ],
        [45, 143]
    )

    # ════════════════════════════════════════════════════════════════════════
    # 3. MODULOS
    # ════════════════════════════════════════════════════════════════════════
    pdf.add_page()
    pdf.titulo_seccion("MODULOS DE LA APLICACION", "3")

    # ── 3.1 DASHBOARD ──────────────────────────────────────────────────────
    pdf.modulo_titulo("[ Inicio ]", "3.1  Panel Principal  -  Dashboard",
                      "Resumen visual del estado del negocio en tiempo real")

    pdf.parrafo(
        "Al abrir la aplicacion, el Dashboard muestra de un vistazo el estado actual\n"
        "del negocio mediante tarjetas de informacion y un panel de recordatorios.")

    pdf.titulo_sub("Tarjetas de resumen")
    pdf.tabla(
        ["Tarjeta", "Que muestra"],
        [
            ["Clientes",              "Total de clientes registrados en la base de datos"],
            ["Presupuestos pend.",    "Presupuestos en estado 'enviado' sin respuesta del cliente"],
            ["Facturas pendientes",   "Facturas emitidas aun sin cobrar"],
            ["Bajo stock",            "Materiales cuyo stock actual esta por debajo del minimo"],
            ["Facturado este ano",    "Suma total de facturas no anuladas del ano en curso (euros)"],
        ],
        [55, 133]
    )

    pdf.titulo_sub("Panel de recordatorios")
    pdf.parrafo(
        "Muestra los proximos recordatorios del calendario (configurable: por defecto 7 dias).\n"
        "Los recordatorios aparecen con codigo de color: HOY (rojo), MANANA (naranja), PROXIMO (azul).\n"
        "Haciendo clic en un recordatorio se abre el Calendario en esa fecha.")

    # ── 3.2 CLIENTES ───────────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Clientes ]", "3.2  Clientes",
                      "Base de datos de clientes: empresas y particulares")

    pdf.titulo_sub("Funciones disponibles")
    pdf.vineta([
        "Crear nuevo cliente: boton '+ Nuevo' o Ctrl+N",
        "Editar cliente existente: seleccionarlo en la tabla y pulsar 'Editar' o hacer doble clic",
        "Eliminar cliente: seleccionarlo y pulsar 'Borrar' (pide confirmacion)",
        "Buscar: escribe en el campo de busqueda para filtrar por nombre, apellido, NIF o email",
    ])

    pdf.titulo_sub("Campos del formulario de cliente")
    pdf.tabla(
        ["Campo", "Descripcion", "Obligatorio"],
        [
            ["Nombre",        "Nombre o razon social",                      "Si"],
            ["Apellido",      "Apellido (para particulares)",                "No"],
            ["Tipo",          "Empresa o Particular",                        "Si"],
            ["NIF / CIF",     "Numero de identificacion fiscal",              "No"],
            ["Direccion",     "Calle, numero, piso...",                       "No"],
            ["Ciudad",        "Ciudad o localidad",                           "No"],
            ["Codigo Postal", "Codigo postal (5 digitos)",                    "No"],
            ["Telefono",      "Numero de telefono de contacto",               "No"],
            ["Email",         "Correo electronico",                           "No"],
            ["Notas",         "Observaciones internas (no aparecen en PDF)",  "No"],
        ],
        [38, 105, 34]
    )

    pdf.caja("Vinculacion automatica",
             "Al crear un presupuesto o factura, podras seleccionar el cliente desde este listado.\n"
             "Los datos del cliente (nombre, NIF, direccion) se copian automaticamente al documento.",
             color=AZUL, bg=AZUL_BG)

    # ── 3.3 PRESUPUESTOS ───────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Presupuestos ]", "3.3  Presupuestos",
                      "Crear, gestionar y exportar presupuestos a clientes")

    pdf.titulo_sub("Funciones disponibles")
    pdf.vineta([
        "Crear nuevo presupuesto con numero automatico (formato: PRE-2025-0001)",
        "Editar presupuesto existente (siempre que no este facturado)",
        "Cambiar el estado del presupuesto manualmente",
        "Exportar a PDF: genera documento profesional con logo y datos de empresa",
        "Crear factura: convierte un presupuesto aceptado en factura con un clic",
        "Eliminar presupuesto: solo si esta en borrador o rechazado",
    ])

    pdf.titulo_sub("Estados del presupuesto")
    pdf.estado_badge(["Borrador", "Enviado", "Aceptado", "Rechazado", "Facturado"])
    pdf.parrafo("Flujo normal: Borrador -> Enviado (al cliente) -> Aceptado -> Facturado")

    pdf.titulo_sub("Crear un nuevo presupuesto  -  paso a paso")
    pdf.paso(1, "Seleccionar cliente", "Desplegable con busqueda integrada. Si el cliente no existe, crealo primero en el modulo Clientes.")
    pdf.paso(2, "Elegir fecha de emision y validez", "La validez es el plazo en que el precio es valido (por defecto 30 dias).")
    pdf.paso(3, "Anadir lineas de producto/servicio",
             "Para cada linea: descripcion, tecnica de impresion (serigrafia, DTF, bordado...),\n"
             "cantidad, precio unitario y descuento (%). El total de linea se calcula solo.")
    pdf.paso(4, "Usar una tarifa predefinida",
             "Boton 'Insertar tarifa': abre el catalogo de Tarifas y rellena la linea automaticamente.")
    pdf.paso(5, "Revisar totales",
             "La aplicacion calcula automaticamente: base imponible, IVA (21%) y total final.")
    pdf.paso(6, "Guardar y exportar",
             "Pulsa 'Guardar'. Luego 'Exportar PDF' para generar el documento que enviaras al cliente.")

    pdf.titulo_sub("Campos del formulario")
    pdf.tabla(
        ["Campo", "Descripcion"],
        [
            ["Cliente",          "Cliente al que va dirigido el presupuesto"],
            ["Fecha emision",    "Fecha de creacion del presupuesto"],
            ["Fecha validez",    "Hasta cuando es valido el precio ofertado"],
            ["Notas",            "Texto libre que aparece en el PDF (condiciones, plazos...)"],
            ["Condiciones",      "Texto de condiciones generales (aparece al pie del PDF)"],
            ["IVA (%)",          "Porcentaje de IVA aplicado (por defecto 21%)"],
            ["Estado",           "Borrador / Enviado / Aceptado / Rechazado / Facturado"],
        ],
        [40, 148]
    )

    # ── 3.4 FACTURAS ───────────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Facturas ]", "3.4  Facturas",
                      "Gestion de facturacion y cobros con exportacion legal a PDF")

    pdf.titulo_sub("Formas de crear una factura")
    pdf.vineta([
        "Desde un presupuesto aceptado: en el modulo Presupuestos, boton 'Crear factura' (recomendado)",
        "Manualmente: boton '+ Nueva' en el modulo Facturas (util para trabajos sin presupuesto previo)",
    ])

    pdf.titulo_sub("Estados de la factura")
    pdf.estado_badge(["Pendiente", "Pagada", "Vencida", "Anulada"])
    pdf.parrafo(
        "Las facturas vencidas (sin pagar y con fecha de vencimiento superada) se resaltan\n"
        "en rojo en la tabla. Las pagadas en verde. Las anuladas en gris.")

    pdf.titulo_sub("Acciones disponibles")
    pdf.tabla(
        ["Accion", "Descripcion"],
        [
            ["Exportar PDF",       "Genera la factura en PDF con todos los datos legales requeridos"],
            ["Marcar como Pagada", "Registra el cobro y cambia el estado a 'Pagada'"],
            ["Crear Albaran",      "Genera un albaran de entrega vinculado a esta factura"],
            ["Anular",             "Marca la factura como nula. No se elimina (queda en el registro)"],
            ["Borrar",             "Elimina definitivamente la factura (solo facturas no anuladas)"],
        ],
        [40, 148]
    )

    pdf.titulo_sub("Campos del formulario de factura")
    pdf.tabla(
        ["Campo", "Descripcion"],
        [
            ["Numero de factura",   "Autogenerado: FAC-2025-0001. No editable."],
            ["Cliente",             "Cliente al que se factura"],
            ["Fecha emision",       "Fecha de la factura (dia, mes, ano)"],
            ["Fecha vencimiento",   "Plazo de pago (ej: 30 dias desde emision)"],
            ["Forma de pago",       "Transferencia, Efectivo, Tarjeta, Cheque, Pagare, Domiciliacion"],
            ["Lineas de factura",   "Descripcion, cantidad, precio unitario, descuento, total"],
            ["Base imponible",      "Suma de todas las lineas (sin IVA). Calculado automaticamente."],
            ["IVA (%)",             "Porcentaje de IVA aplicado (por defecto 21%)"],
            ["Total",               "Base imponible + cuota de IVA. Calculado automaticamente."],
        ],
        [42, 146]
    )

    # ── 3.5 ALBARANES ──────────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Albaranes ]", "3.5  Albaranes",
                      "Control de entrega de mercancias y servicios")

    pdf.parrafo(
        "El albaran es el documento que acredita la entrega de un pedido o servicio al cliente.\n"
        "Puede generarse directamente desde una factura o de forma independiente.")

    pdf.titulo_sub("Estados del albaran")
    pdf.estado_badge(["Pendiente", "Firmado", "Entregado"])

    pdf.titulo_sub("Funciones disponibles")
    pdf.vineta([
        "Crear nuevo albaran (vinculado a una factura o de forma independiente)",
        "Editar albaran existente (mientras no este firmado)",
        "Marcar como firmado: confirma que el cliente ha recibido el pedido",
        "Exportar a PDF: genera el documento para que el cliente firme en papel",
        "Borrar albaran (solo si esta en estado 'Pendiente')",
    ])

    pdf.titulo_sub("Campos del formulario de albaran")
    pdf.tabla(
        ["Campo", "Descripcion"],
        [
            ["Numero albaran",     "Autogenerado. Formato: ALB-2025-0001"],
            ["Factura vinculada",  "Factura a la que se asocia el albaran (opcional)"],
            ["Cliente",            "Cliente que recibe la entrega"],
            ["Fecha",              "Fecha de entrega"],
            ["Lineas",             "Descripcion, cantidad, referencia de cada articulo entregado"],
            ["Notas",              "Observaciones de entrega (condiciones, lugar, responsable)"],
        ],
        [42, 146]
    )

    pdf.caja("Uso del albaran",
             "Genera el PDF del albaran y llevalo impreso cuando entregues el pedido.\n"
             "El cliente firma en el papel. Luego marca el albaran como 'Firmado' en la aplicacion\n"
             "y archiva el papel firmado para tu registro.",
             color=NARANJA, bg=NARANJA_BG)

    # ── 3.6 PEDIDOS ────────────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Pedidos ]", "3.6  Pedidos y Control de Pagos",
                      "Gestion de pedidos a proveedores y seguimiento de pagos")

    pdf.parrafo(
        "Este modulo tiene dos pestanas: Pedidos (gestion de pedidos a proveedores) y\n"
        "Control de Pagos (seguimiento consolidado de todos los pagos pendientes).")

    pdf.titulo_sub("Pestana: Pedidos")
    pdf.vineta([
        "Crear, editar y eliminar pedidos a proveedores",
        "Registrar la forma de pago: Transferencia, Efectivo, Tarjeta, Cheque, Pagare, Domiciliacion",
        "Panel inferior: muestra los pagos registrados del pedido seleccionado",
        "Boton 'Marcar pagado': cambia el estado del pedido a pagado",
        "Anadir pagos parciales o totales al pedido seleccionado",
    ])

    pdf.titulo_sub("Pestana: Control de Pagos")
    pdf.vineta([
        "Vista consolidada de TODOS los pagos de todos los pedidos",
        "Filtros rapidos: Todos | Pendiente | Vencido | Proximo (proximos 7 dias)",
        "Los pagos vencidos se muestran en rojo; los proximos en naranja",
    ])

    pdf.titulo_sub("Tarjetas de resumen del modulo")
    pdf.tabla(
        ["Tarjeta", "Que muestra"],
        [
            ["Pedidos activos",       "Numero de pedidos no completados"],
            ["Total facturado",       "Suma de todos los pedidos (euros)"],
            ["Cobrado",               "Total ya pagado (euros)"],
            ["Pendiente de cobro",    "Lo que queda por pagar (euros)"],
            ["Vencidos",              "Pagos cuya fecha de vencimiento ya paso"],
        ],
        [55, 133]
    )

    pdf.titulo_sub("Registrar un pago a un pedido")
    pdf.paso(1, "Seleccionar el pedido en la tabla",
             "Haz clic sobre el pedido. Aparecera el historial de pagos en el panel inferior.")
    pdf.paso(2, "Pulsar 'Anadir pago'",
             "Introduce el importe, la fecha y la forma de pago del pago parcial o total.")
    pdf.paso(3, "Guardar",
             "El pago queda registrado. Si el total pagado iguala el importe del pedido,\n"
             "el pedido pasa automaticamente a estado 'Pagado'.")

    # ── 3.7 TARIFAS ────────────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Tarifas ]", "3.7  Tarifas",
                      "Catalogo de precios por tecnica de impresion")

    pdf.parrafo(
        "Las tarifas son los precios unitarios de cada tecnica de impresion. Desde el formulario\n"
        "de presupuestos puedes insertar una tarifa directamente para rellenar la linea de forma rapida.")

    pdf.titulo_sub("Tecnicas disponibles")
    pdf.vineta([
        "Serigrafia  (1 color, 2 colores, 4 colores, mas colores)",
        "DTF  (Direct to Film  -  impresion digital directa)",
        "Bordado  (basico y premium, por numero de puntos)",
        "Vinilo textil  (corte y transfer)",
        "Sublimacion  (poliester y materiales sublimables)",
        "Gran Formato  (lonas, vinilos adhesivos, impresion digital)",
        "Offset  (impresion offset para grandes tiradas)",
        "Otros  (cualquier tecnica no listada anteriormente)",
    ])

    pdf.titulo_sub("Campos de una tarifa")
    pdf.tabla(
        ["Campo", "Descripcion"],
        [
            ["Tecnica",            "Tipo de impresion (serigrafia, DTF, bordado...)"],
            ["Nombre",             "Descripcion corta (ej: Serigrafia 2 tintas basica)"],
            ["Descripcion",        "Detalle opcional para el presupuesto"],
            ["Precio unitario",    "Precio por unidad (euros)"],
            ["Precio de setup",    "Coste fijo de preparacion/fotolito (euros)"],
            ["Minimo unidades",    "Cantidad minima para aplicar este precio"],
        ],
        [40, 148]
    )

    pdf.titulo_sub("Tarifas de ejemplo incluidas por defecto")
    pdf.tabla(
        ["Tecnica",              "Precio/ud", "Setup",     "Minimo"],
        [
            ["Serigrafia 1 tinta",    "2,50 EUR",  "35,00 EUR",  "12 uds"],
            ["Serigrafia 2 tintas",   "3,20 EUR",  "60,00 EUR",  "12 uds"],
            ["Serigrafia 4 colores",  "4,80 EUR",  "100,00 EUR", "24 uds"],
            ["DTF estandar",          "3,50 EUR",  "-",           "1 ud"],
            ["Bordado basico",        "4,00 EUR",  "25,00 EUR",  "6 uds"],
            ["Bordado premium",       "7,00 EUR",  "40,00 EUR",  "6 uds"],
            ["Vinilo textil",         "2,80 EUR",  "-",           "1 ud"],
            ["Sublimacion",           "3,00 EUR",  "-",           "1 ud"],
            ["Lona PVC 510g",         "18,00/m2",  "-",           "1 m2"],
            ["Vinilo adhesivo",       "22,00/m2",  "-",           "1 m2"],
        ],
        [55, 35, 50, 48]
    )

    # ── 3.8 MATERIALES ─────────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Materiales ]", "3.8  Materiales y Stock",
                      "Gestion de inventario, consumo por tecnica y pagos a proveedores")

    pdf.parrafo("Este modulo tiene tres pestanas: Stock, Consumo por Tecnica y Pagos a Proveedores.")

    pdf.titulo_sub("Pestana: Stock")
    pdf.vineta([
        "Lista de todos los materiales con su stock actual y stock minimo",
        "Indicador visual de alerta: fondo rojo si el stock esta por debajo del minimo",
        "Filtro rapido: checkbox 'Solo materiales bajo alerta' para ver solo los que necesitan reposicion",
        "Filtro por categoria: Tintas, Pantallas, Sustratos, Vinilos, Consumibles, Bordado, Gran Formato",
        "Crear, editar y eliminar materiales",
        "Registrar movimientos: entrada de stock (compra) o salida (consumo/merma)",
    ])

    pdf.titulo_sub("Campos de un material")
    pdf.tabla(
        ["Campo", "Descripcion"],
        [
            ["Nombre",         "Nombre del material (ej: Tinta plastisol negra 1kg)"],
            ["Referencia/SKU", "Codigo interno o del proveedor"],
            ["Categoria",      "Tipo de material (ver lista de categorias arriba)"],
            ["Stock actual",   "Cantidad actual en almacen (en la unidad configurada)"],
            ["Stock minimo",   "Cantidad minima antes de lanzar la alerta de reposicion"],
            ["Unidad",         "Kg, litros, metros, unidades, rollos..."],
            ["Precio unitario","Coste de compra por unidad (para calcular coste de produccion)"],
            ["Proveedor",      "Nombre del proveedor habitual"],
        ],
        [38, 150]
    )

    pdf.titulo_sub("Pestana: Consumo por Tecnica")
    pdf.parrafo(
        "Registra cuanto material se consume por cada tecnica de impresion.\n"
        "Util para calcular el coste real de cada trabajo y detectar mermas.")
    pdf.vineta([
        "Ver historial de consumos: fecha, tecnica, material, cantidad consumida",
        "Registrar nuevo consumo: selecciona el material y la tecnica utilizada",
        "Filtrar por tecnica o por material para analizar tendencias",
    ])

    pdf.titulo_sub("Pestana: Pagos a Proveedores")
    pdf.vineta([
        "Registra las facturas de compra de materiales a proveedores",
        "Filtros rapidos: Todos | Pendiente | Vencido | Proximo | Pagado",
        "Tarjetas de resumen: Total pendiente, Vencidos, Proximos a vencer",
        "Marcar pagos como realizados",
    ])

    pdf.caja("Alerta de bajo stock en el Dashboard",
             "Cuando el stock de un material baja del minimo configurado, el Dashboard\n"
             "lo cuenta en la tarjeta 'Bajo stock'. Para ver cuales son, usa el filtro\n"
             "del modulo Materiales: checkbox 'Solo materiales bajo alerta'.",
             color=NARANJA, bg=NARANJA_BG)

    # ── 3.9 EMPLEADOS ──────────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Empleados ]", "3.9  Empleados",
                      "Gestion de datos de personal y estado laboral")

    pdf.titulo_sub("Funciones disponibles")
    pdf.vineta([
        "Crear nuevo empleado con todos sus datos laborales",
        "Editar datos de empleado existente",
        "Dar de baja: marca al empleado como inactivo (no se elimina, queda en el historial)",
        "Reactivar empleado: devuelve al estado activo",
        "Checkbox 'Mostrar empleados dados de baja': ver tambien los inactivos",
    ])

    pdf.titulo_sub("Estados del empleado")
    pdf.estado_badge(["Activo", "Baja"])

    pdf.titulo_sub("Campos del formulario de empleado")
    pdf.tabla(
        ["Campo", "Descripcion"],
        [
            ["Nombre / Apellido",  "Datos de identificacion del trabajador"],
            ["NIF",                "Numero de Identificacion Fiscal"],
            ["Categoria",          "Operario, Oficial 1a, Oficial 2a, Auxiliar, Tecnico, Administrativo, Encargado, Gerente"],
            ["Salario base",       "Salario bruto mensual (euros). Base para el calculo de nominas"],
            ["Fecha de alta",      "Fecha de incorporacion a la empresa"],
            ["IBAN",               "Cuenta bancaria para transferencia de nomina"],
            ["% IRPF",             "Porcentaje de retencion de IRPF individual del trabajador"],
            ["Telefono / Email",   "Datos de contacto"],
            ["Direccion",          "Domicilio del empleado"],
        ],
        [42, 146]
    )

    pdf.caja("Consejo sobre el IRPF",
             "El porcentaje de IRPF es individual para cada empleado y lo comunica la Agencia Tributaria.\n"
             "Verifica los porcentajes cada inicio de ano o cuando el empleado te lo comunique.",
             color=AZUL, bg=AZUL_BG)

    # ── 3.10 NOMINAS ───────────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Nominas ]", "3.10  Nominas",
                      "Calculo y gestion de nominas conforme a la legislacion espanola")

    pdf.parrafo(
        "El modulo de nominas calcula automaticamente todos los conceptos de cotizacion\n"
        "segun los porcentajes vigentes de la Seguridad Social espanola.")

    pdf.titulo_sub("Funciones disponibles")
    pdf.vineta([
        "Crear nomina individual para un empleado y mes",
        "Generar nominas para todos los empleados activos de un mes con un clic",
        "Editar nomina (ajustar complementos, horas extra, dietas...)",
        "Exportar nomina a PDF: recibo oficial con todos los conceptos desglosados",
        "Borrar nomina",
    ])

    pdf.titulo_sub("Como generar las nominas del mes")
    pdf.paso(1, "Pulsar 'Generar mes para todos'",
             "Introduce el mes y el ano (formato MM/AAAA, ej: 01/2025).")
    pdf.paso(2, "Revision automatica",
             "El sistema crea una nomina por cada empleado activo con el salario base configurado.")
    pdf.paso(3, "Ajustes individuales (si procede)",
             "Si algun empleado tiene complementos, horas extra o incidencias, edita su nomina\n"
             "y modifica los campos correspondientes.")
    pdf.paso(4, "Exportar a PDF",
             "Selecciona cada nomina y pulsa 'Exportar PDF'. Genera el recibo de salario completo.")

    pdf.titulo_sub("Conceptos calculados automaticamente")
    pdf.tabla(
        ["Concepto", "Descripcion", "Porcentaje 2025"],
        [
            ["Salario bruto",           "Salario base + complementos + horas extra",           "-"],
            ["Cotizacion SS (trabajador)", "Descontado del bruto del empleado",                "6,35%"],
            ["  - Contingencias comunes", "Mayor parte de la cotizacion SS",                   "4,70%"],
            ["  - Desempleo",            "",                                                    "1,55%"],
            ["  - Formacion profesional","",                                                    "0,10%"],
            ["IRPF",                    "Retencion fiscal individual del empleado",             "variable"],
            ["Neto a percibir",         "Bruto - SS trabajador - IRPF",                        "-"],
            ["SS empresa",              "Coste adicional de la empresa a la Seguridad Social",  "29,90%"],
            ["Coste empresa total",     "Bruto + SS empresa",                                  "-"],
        ],
        [55, 90, 43]
    )

    # ── 3.11 ESTADISTICAS ──────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Estadisticas ]", "3.11  Estadisticas y Analisis",
                      "Graficas de ingresos, gastos, materiales, clientes y produccion")

    pdf.parrafo(
        "El modulo de estadisticas organiza los datos de negocio en graficas visuales distribuidas\n"
        "en cuatro pestanas. Cada pestana se carga al acceder a ella (carga perezosa para mayor rapidez).")

    pdf.titulo_sub("Pestana 1: Financiero")
    pdf.vineta([
        "Grafica mensual: Ingresos (verde) vs Gastos de materiales (rojo) vs Nominas (naranja)",
        "Selector de ano: analiza cualquier ejercicio fiscal anterior",
        "Fuente de datos: facturas no anuladas, pagos de materiales y nominas calculadas",
        "Boton de actualizacion manual para refrescar si has modificado datos",
    ])

    pdf.titulo_sub("Pestana 2: Materiales")
    pdf.vineta([
        "Materiales mas consumidos (ranking por unidades/importe)",
        "Distribucion del consumo por categoria de material",
        "Stock actual de todos los materiales (grafica de barras)",
    ])

    pdf.titulo_sub("Pestana 3: Clientes")
    pdf.vineta([
        "Numero de clientes por estado: potencial, activo, inactivo",
        "Clientes con mayor volumen de facturacion (ranking)",
        "Frecuencia de pedidos por cliente",
    ])

    pdf.titulo_sub("Pestana 4: Produccion")
    pdf.vineta([
        "Volumen de trabajo por tecnica de impresion (serigrafia, DTF, bordado...)",
        "Evolucion mensual de la produccion",
        "Eficiencia y rentabilidad por tecnica",
    ])

    # ── 3.12 CALENDARIO ────────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Calendario ]", "3.12  Calendario y Recordatorios",
                      "Gestion de notas, eventos y recordatorios con integracion en el Dashboard")

    pdf.titulo_sub("Navegacion por el calendario")
    pdf.vineta([
        "Botones '<' y '>' para ir al mes anterior o siguiente",
        "Boton 'Hoy' para volver rapidamente al mes actual",
        "Vista de calendario mensual con 7 columnas (lunes a domingo)",
        "Los dias con recordatorios se resaltan visualmente (punto de color)",
    ])

    pdf.titulo_sub("Crear un recordatorio")
    pdf.paso(1, "Hacer clic en el dia deseado",
             "Se resalta el dia seleccionado y aparece el panel lateral de notas.")
    pdf.paso(2, "Pulsar '+ Anadir nota'",
             "Se abre el formulario con: titulo (obligatorio), descripcion (opcional) y fecha.")
    pdf.paso(3, "Guardar",
             "El recordatorio queda guardado y el dia se marca visualmente en el calendario.")

    pdf.titulo_sub("Integracion con el Dashboard")
    pdf.parrafo(
        "Los recordatorios de los proximos N dias (configurable en Configuracion > Preferencias)\n"
        "aparecen automaticamente en el panel de recordatorios del Dashboard al iniciar la aplicacion.\n"
        "Color: HOY (rojo), MANANA (naranja), PROXIMO (azul).")

    pdf.titulo_sub("Gestion de recordatorios existentes")
    pdf.vineta([
        "Ver todos los recordatorios de un dia: clic en el dia del calendario",
        "Editar un recordatorio: seleccionarlo en la lista y pulsar 'Editar'",
        "Eliminar un recordatorio: seleccionarlo y pulsar 'Eliminar'",
    ])

    # ── 3.13 ASISTENTE IA ──────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Asistente IA ]", "3.13  Asistente IA Local  -  Ollama",
                      "Chat con inteligencia artificial que funciona sin Internet en tu propio ordenador")

    pdf.parrafo(
        "El asistente de IA utiliza Ollama, un motor de IA de codigo abierto que corre\n"
        "completamente en tu PC. Ningun dato sale de tu red local. Es 100% privado.")

    pdf.titulo_sub("Instalar Ollama (solo la primera vez)")
    pdf.paso(1, "Descargar Ollama",
             "Visita https://ollama.com desde tu navegador y descarga el instalador de Windows.")
    pdf.paso(2, "Instalar",
             "Ejecuta el instalador. Ollama arranca como servicio en segundo plano automaticamente.")
    pdf.paso(3, "Descargar un modelo de IA (en cmd de Windows):")
    pdf.codigo([
        "# Modelo recomendado - ligero (2 GB), rapido y capaz:",
        "ollama pull llama3.2",
        "",
        "# Alternativa mas potente (requiere 8 GB de RAM):",
        "ollama pull phi4",
        "",
        "# Ver modelos disponibles en tu equipo:",
        "ollama list",
    ])
    pdf.paso(4, "Abrir el modulo IA en la aplicacion",
             "Haz clic en 'Asistente IA' en la barra lateral. El indicador debe mostrar '[ON] Ollama conectado'.")

    pdf.titulo_sub("Como usar el chat")
    pdf.vineta([
        "Escribe tu pregunta en el campo de texto inferior y pulsa Enter o el boton 'Enviar'",
        "La respuesta aparece en streaming (palabra a palabra conforme la genera la IA)",
        "Selector de modelo: cambia el modelo de IA segun tus necesidades",
        "Boton 'Limpiar chat': borra el historial de la conversacion actual",
    ])

    pdf.titulo_sub("Ejemplos de uso")
    pdf.vineta([
        "Calcular precio: 'Tengo un pedido de 100 camisetas a 2 colores, cuanto cobro?'",
        "Redactar textos para presupuestos: 'Escribe una descripcion para bordado corporativo'",
        "Materiales: 'Que diferencia hay entre tinta plastisol y tinta acuarela?'",
        "Nominas: 'Explica el calculo del IRPF en una nomina espanola'",
        "Estrategia: 'Como aumentar el margen en pedidos pequenos de serigrafia?'",
        "Marketing: 'Escribe un email ofreciendo nuestros servicios de DTF a empresas'",
    ])

    pdf.caja("Privacidad garantizada",
             "Toda la IA funciona en tu ordenador. Ningun mensaje, ninguna pregunta\n"
             "ni ningun dato de clientes, facturas o nominas sale de tu red local.\n"
             "Puedes usar la IA incluso sin conexion a Internet una vez descargado el modelo.",
             color=VERDE, bg=VERDE_BG)

    # ── 3.14 IMPORTACION ───────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Importar ]", "3.14  Importacion de Datos",
                      "Importa clientes, materiales, empleados y tarifas desde CSV, Excel o JSON")

    pdf.parrafo(
        "El modulo de importacion permite cargar datos masivos desde archivos externos.\n"
        "Sigue un flujo de 4 pasos guiados con deteccion automatica del tipo de dato.")

    pdf.titulo_sub("Formatos soportados")
    pdf.vineta([
        "CSV: delimitado por comas (,) o punto y coma (;). UTF-8 recomendado.",
        "Excel: formatos .xlsx (Excel 2007 o superior) y .xls (Excel 97-2003)",
        "JSON: array de objetos con clave-valor (formato de exportacion de muchos sistemas)",
    ])

    pdf.titulo_sub("Flujo de importacion  -  4 pasos")
    pdf.paso(1, "Seleccionar archivo",
             "Pulsa 'Examinar' y navega hasta el archivo CSV, Excel o JSON que quieres importar.")
    pdf.paso(2, "Deteccion automatica del tipo",
             "La aplicacion analiza las columnas del archivo e intenta detectar automaticamente si\n"
             "contiene Clientes, Materiales, Empleados o Tarifas. Puedes corregir la deteccion manualmente.")
    pdf.paso(3, "Mapear campos",
             "Asigna cada columna del archivo a su campo correspondiente en la base de datos.\n"
             "El sistema propone el mapeo automaticamente. Verifica y corrige si es necesario.")
    pdf.paso(4, "Importar",
             "Pulsa 'Importar'. El sistema muestra el progreso y un resumen de cuantos registros\n"
             "se importaron correctamente y cuales dieron error.")

    pdf.titulo_sub("Entidades importables y sus campos principales")
    pdf.tabla(
        ["Entidad",    "Campos disponibles para mapeo"],
        [
            ["Clientes",   "nombre, apellido, tipo, nif, direccion, ciudad, cp, telefono, email, notas"],
            ["Materiales", "nombre, referencia, categoria, stock_actual, stock_minimo, unidad, precio, proveedor"],
            ["Empleados",  "nombre, apellido, nif, categoria, salario_base, fecha_alta, iban, irpf, telefono, email"],
            ["Tarifas",    "tecnica, nombre, descripcion, precio_unit, precio_setup, minimo_unidades"],
        ],
        [30, 158]
    )

    pdf.caja("Consejo para la importacion",
             "Si migras datos desde otro programa, exportalos a CSV primero.\n"
             "Comprueba que el archivo tiene cabecera (primera fila con nombres de columnas).\n"
             "La codificacion UTF-8 evita problemas con caracteres especiales (tildes, enes).",
             color=AZUL, bg=AZUL_BG)

    # ── 3.15 EXPORTACION ───────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Exportar ]", "3.15  Exportacion y Copias de Seguridad",
                      "Exporta todos los datos de la aplicacion en multiples formatos")

    pdf.parrafo(
        "Este modulo permite exportar la base de datos completa para hacer copias de seguridad\n"
        "o para compartir los datos con otros sistemas.")

    pdf.titulo_sub("Informacion de la base de datos")
    pdf.vineta([
        "Ruta del archivo de base de datos (para copias manuales)",
        "Tamano actual del archivo en MB",
        "Numero de registros principales (clientes, facturas, empleados, etc.)",
        "Boton 'Abrir carpeta': abre el explorador de Windows en la carpeta de la BD",
    ])

    pdf.titulo_sub("Formatos de exportacion disponibles")
    pdf.tabla(
        ["Formato",      "Extension", "Descripcion", "Mejor para"],
        [
            ["Copia SQLite",  ".db",   "Copia exacta de la base de datos",          "Backup principal. Restaurable directamente"],
            ["ZIP con CSV",   ".zip",  "Un CSV por tabla, comprimidos juntos",       "Compatibilidad maxima. Abrir en Excel."],
            ["JSON",          ".json", "Estructura jerarquica completa",             "Integracion con otros sistemas."],
            ["Excel",         ".xlsx", "Libro con una hoja por tabla",               "Compartir datos. Analisis en Excel."],
        ],
        [28, 22, 68, 70]
    )

    pdf.titulo_sub("Como hacer una copia de seguridad")
    pdf.paso(1, "Ir al modulo Exportar (barra lateral)",
             "Ves el tamano y la ruta de tu base de datos.")
    pdf.paso(2, "Seleccionar el formato",
             "Para backup: elige 'Copia SQLite (.db)'. Es la forma mas completa y rapida de restaurar.")
    pdf.paso(3, "Elegir la carpeta de destino",
             "Pulsa 'Seleccionar destino' y elige una carpeta. Recomendado: disco externo o nube (OneDrive).")
    pdf.paso(4, "Exportar",
             "Pulsa 'Exportar'. El log muestra el progreso en tiempo real.\n"
             "Al terminar, la carpeta de destino se abre automaticamente.")

    pdf.titulo_sub("Tablas incluidas en la exportacion completa")
    pdf.parrafo(
        "La exportacion incluye TODOS los datos: configuracion, clientes, empleados, tarifas,\n"
        "materiales, consumos, movimientos, pagos a proveedores, presupuestos, lineas de\n"
        "presupuesto, facturas, lineas de factura, albaranes, pedidos, pagos de pedidos,\n"
        "nominas y notas del calendario. Nada se omite.")

    pdf.caja("Frecuencia recomendada de backups",
             "Copia de seguridad diaria si facturas habitualmente.\n"
             "Minimo semanal si el volumen de datos es menor.\n"
             "Guarda siempre la copia en un sitio diferente al PC (disco externo, nube).",
             color=ROJO, bg=(255, 235, 232))

    # ── 3.16 CONFIGURACION ─────────────────────────────────────────────────
    pdf.add_page()
    pdf.modulo_titulo("[ Configuracion ]", "3.16  Configuracion",
                      "Personaliza la apariencia, los datos de empresa y las preferencias")

    pdf.parrafo("La configuracion tiene cuatro pestanas independientes.")

    pdf.titulo_sub("Pestana 1: Apariencia")
    pdf.vineta([
        "Tema de color: 6 esquemas predefinidos (Mulberry, Oscuro, Azul marino, Verde, Rojo, Claro)",
        "Tipografia: Segoe UI, Arial, Calibri, Georgia, Consolas, Trebuchet MS, Verdana",
        "Tamano de fuente: Pequeno (11px), Normal (13px), Grande (15px), Muy grande (17px)",
        "Los cambios se aplican en tiempo real al pulsar 'Aplicar'",
        "Las preferencias se guardan automaticamente y se restauran al abrir la aplicacion",
    ])

    pdf.titulo_sub("Pestana 2: Mi Empresa")
    pdf.parrafo("Estos datos aparecen en la cabecera de todos los documentos PDF generados:")
    pdf.tabla(
        ["Campo", "Descripcion"],
        [
            ["Nombre de empresa",  "Nombre completo de tu empresa o razon social"],
            ["NIF / CIF",          "Numero de identificacion fiscal"],
            ["Direccion",          "Calle, numero y piso"],
            ["Codigo Postal",      "CP de la localidad"],
            ["Ciudad",             "Ciudad o localidad"],
            ["Telefono",           "Numero de contacto principal"],
            ["Email",              "Correo electronico de contacto"],
            ["Logo",               "Imagen del logo (JPG, PNG). Aparece en la cabecera de los PDFs"],
        ],
        [42, 146]
    )

    pdf.titulo_sub("Pestana 3: Preferencias")
    pdf.tabla(
        ["Preferencia", "Descripcion", "Valor por defecto"],
        [
            ["Dias de aviso recordatorios",    "Cuantos dias antes se muestra la alerta",             "3 dias"],
            ["Dias en panel Dashboard",        "Cuantos dias de recordatorios se muestran en el inicio", "7 dias"],
            ["IVA por defecto (%)",            "IVA que se aplica al crear nuevos presupuestos/facturas", "21%"],
            ["Prefijo presupuestos",           "Texto antes del numero (ej: PRE-2025-0001)",             "PRE"],
            ["Prefijo facturas",               "Texto antes del numero (ej: FAC-2025-0001)",             "FAC"],
            ["Ruta de guardado PDFs",          "Carpeta donde se guardan los PDFs generados",           "Documentos\\Mulberry"],
        ],
        [55, 88, 45]
    )

    pdf.titulo_sub("Pestana 4: Musica y Sonidos")
    pdf.vineta([
        "Volumen de efectos de sonido (slider 0-100%) y boton Silenciar/Activar",
        "Musica de fondo: seleccionar archivos MP3 del ordenador para crear una lista de reproduccion",
        "Volumen de musica independiente del volumen de efectos",
        "Loop automatico: la lista se repite indefinidamente",
        "Autoplay: inicia la musica automaticamente al abrir la aplicacion",
    ])

    # ════════════════════════════════════════════════════════════════════════
    # 4. FLUJOS DE TRABAJO
    # ════════════════════════════════════════════════════════════════════════
    pdf.add_page()
    pdf.titulo_seccion("FLUJOS DE TRABAJO PRINCIPALES", "4")

    pdf.titulo_sub("Flujo 1: Presupuesto -> Factura -> Cobro (flujo comercial completo)")
    pdf.paso(1, "Crear el cliente", "Si no existe, anadelo en Clientes con sus datos fiscales.")
    pdf.paso(2, "Crear el presupuesto", "En Presupuestos: nuevo, selecciona el cliente, aniade lineas de trabajo,\nexporta a PDF y envia al cliente.")
    pdf.paso(3, "Cambiar estado a 'Enviado'", "Actualiza el estado del presupuesto para llevar el seguimiento.")
    pdf.paso(4, "Presupuesto aceptado", "Cuando el cliente acepte, cambia el estado a 'Aceptado'.")
    pdf.paso(5, "Crear la factura", "Boton 'Crear factura' en el presupuesto aceptado. La factura hereda todos los datos.")
    pdf.paso(6, "Registrar el cobro", "Cuando cobres, abre la factura y pulsa 'Marcar como pagada'.")
    pdf.paso(7, "Crear albaran (opcional)", "Si necesitas acreditar la entrega, genera el albaran desde la factura.")

    pdf.titulo_sub("Flujo 2: Gestion de inventario y reposicion")
    pdf.paso(1, "Configurar stock minimo", "En cada material, establece el stock minimo de alerta.")
    pdf.paso(2, "Registrar consumo", "Tras cada produccion, aniade el consumo en Materiales > Consumo por Tecnica.")
    pdf.paso(3, "Detectar alertas", "El Dashboard muestra la tarjeta 'Bajo stock'. En Materiales, activa el filtro.")
    pdf.paso(4, "Crear pago al proveedor", "En Materiales > Pagos a proveedores, registra la compra.")
    pdf.paso(5, "Actualizar el stock", "Al recibir la mercancia, actualiza el stock del material.")

    pdf.titulo_sub("Flujo 3: Nomina mensual")
    pdf.paso(1, "Verificar datos de empleados", "Comprueba salarios base e IRPF en el modulo Empleados.")
    pdf.paso(2, "Generar nominas del mes", "En Nominas: 'Generar mes para todos'. Introduce el mes y ano.")
    pdf.paso(3, "Revisar y ajustar", "Edita las nominas que tengan complementos, horas extra o incidencias.")
    pdf.paso(4, "Exportar PDFs", "Selecciona cada nomina y exporta a PDF para el empleado.")

    pdf.titulo_sub("Flujo 4: Copias de seguridad")
    pdf.paso(1, "Ir a Exportar", "Modulo Exportar en la barra lateral.")
    pdf.paso(2, "Copia SQLite", "Selecciona 'Copia SQLite (.db)' y una carpeta segura (disco externo o nube).")
    pdf.paso(3, "Exportar", "El archivo .db es tu copia completa. Guardalo con la fecha en el nombre.")
    pdf.paso(4, "Exportacion anual a Excel", "Cada fin de ano, exporta a Excel para tener el archivo del ejercicio fiscal.")

    # ════════════════════════════════════════════════════════════════════════
    # 5. GENERACION DE PDFs
    # ════════════════════════════════════════════════════════════════════════
    pdf.add_page()
    pdf.titulo_seccion("GENERACION DE DOCUMENTOS PDF", "5")

    pdf.parrafo(
        "La aplicacion genera documentos PDF profesionales con el logo y datos corporativos\n"
        "de tu empresa. Los archivos se guardan en la carpeta configurada (por defecto: Documentos\\Mulberry).")

    pdf.titulo_sub("Documentos PDF disponibles")
    pdf.tabla(
        ["Documento",    "Como generarlo", "Contenido"],
        [
            ["Presupuesto",  "Presupuestos > seleccionar > 'Exportar PDF'",       "Logo, datos empresa y cliente, lineas de trabajo, totales, condiciones"],
            ["Factura",      "Facturas > seleccionar > 'Exportar PDF'",            "Logo, datos fiscales completos, lineas, base, IVA, total, forma de pago"],
            ["Albaran",      "Albaranes > seleccionar > 'Exportar PDF'",           "Logo, datos entrega, lineas, espacio para firma del cliente"],
            ["Nomina",       "Nominas > seleccionar > 'Exportar PDF'",             "Todos los conceptos de cotizacion, SS empleado y empresa, neto"],
        ],
        [25, 60, 103]
    )

    pdf.titulo_sub("Personalizar la cabecera de los PDFs")
    pdf.vineta([
        "Ve a Configuracion > Mi Empresa",
        "Rellena todos los campos: nombre, NIF, direccion, telefono, email",
        "Carga el logo de tu empresa (JPG o PNG, minimo 200x200 px recomendado)",
        "Los cambios se aplican a todos los PDFs generados a partir de ese momento",
    ])

    pdf.titulo_sub("Abrir el PDF tras generarlo")
    pdf.parrafo(
        "Tras generar el PDF, la aplicacion lo abre automaticamente con el visor de PDF\n"
        "predeterminado de Windows (Adobe Acrobat Reader, Edge u otro).\n"
        "El archivo queda guardado en la carpeta de PDFs para uso posterior.")

    # ════════════════════════════════════════════════════════════════════════
    # 6. BASE DE DATOS
    # ════════════════════════════════════════════════════════════════════════
    pdf.add_page()
    pdf.titulo_seccion("BASE DE DATOS", "6")

    pdf.parrafo(
        "La aplicacion usa SQLite, una base de datos que se guarda como un unico archivo .db\n"
        "en el ordenador. No requiere servidor ni instalacion adicional.")

    pdf.titulo_sub("Ubicacion del archivo")
    pdf.codigo([
        "Carpeta de instalacion: C:\\Program Files\\Graficas Mulberry\\",
        "Nombre del archivo:     graficas_mulberry.db",
        "Ruta completa:          C:\\Program Files\\Graficas Mulberry\\graficas_mulberry.db",
    ])

    pdf.titulo_sub("Tablas incluidas en la base de datos")
    pdf.tabla(
        ["Tabla", "Contenido"],
        [
            ["config",                    "Configuracion de empresa y preferencias (clave-valor)"],
            ["clientes",                  "Datos de clientes (empresa y particulares)"],
            ["empleados",                 "Datos de personal con datos laborales"],
            ["tarifas",                   "Catalogo de precios por tecnica de impresion"],
            ["materiales",                "Inventario del taller con alertas de stock"],
            ["consumo_material_tecnica",  "Registro de consumo de material por tecnica"],
            ["movimientos_material",      "Entradas y salidas de stock"],
            ["pagos_material",            "Pagos a proveedores de materiales"],
            ["presupuestos",              "Cabecera de presupuestos (cliente, fecha, estado, totales)"],
            ["lineas_presupuesto",        "Lineas de detalle de cada presupuesto"],
            ["facturas",                  "Cabecera de facturas (cliente, fecha, estado, totales)"],
            ["lineas_factura",            "Lineas de detalle de cada factura"],
            ["albaranes",                 "Albaranes de entrega"],
            ["lineas_albaran",            "Lineas de detalle de cada albaran"],
            ["pedidos",                   "Pedidos a proveedores"],
            ["pagos_pedido",              "Pagos registrados por pedido"],
            ["nominas",                   "Recibos de nomina con todos los conceptos calculados"],
            ["notas_calendario",          "Recordatorios y notas del calendario"],
        ],
        [55, 133]
    )

    pdf.titulo_sub("Restaurar una copia de seguridad")
    pdf.paso(1, "Cerrar la aplicacion", "Asegurate de que Graficas Mulberry no esta en ejecucion.")
    pdf.paso(2, "Reemplazar el archivo",
             "Copia el archivo .db de la copia de seguridad y pegalo en la carpeta de instalacion,\n"
             "sobreescribiendo el archivo graficas_mulberry.db existente.")
    pdf.paso(3, "Abrir la aplicacion",
             "Al iniciar, la aplicacion cargara automaticamente los datos del archivo restaurado.")

    # ════════════════════════════════════════════════════════════════════════
    # 7. SOLUCION DE PROBLEMAS
    # ════════════════════════════════════════════════════════════════════════
    pdf.add_page()
    pdf.titulo_seccion("SOLUCION DE PROBLEMAS FRECUENTES", "7")

    pdf.problema(
        "La aplicacion no arranca",
        "Verifica que Java 21 o superior esta instalado. Abre cmd y ejecuta: java -version\n"
        "Si no aparece, descarga el JDK desde https://adoptium.net e instalalo.")

    pdf.problema(
        "El asistente IA no responde / aparece 'Ollama desconectado'",
        "Ollama debe estar ejecutandose en segundo plano. Busca su icono en la barra de tareas.\n"
        "Si no esta, abre 'Ollama' desde el menu Inicio. Luego vuelve al modulo IA.")

    pdf.problema(
        "La IA responde muy lento",
        "Esto es normal en PC sin GPU. El modelo llama3.2 es el mas rapido.\n"
        "Evita usar modelos grandes (phi4, llama3.1:70b) en PC con menos de 16 GB de RAM.")

    pdf.problema(
        "El PDF no se genera o no se abre",
        "Verifica que tienes permisos de escritura en la carpeta de destino de PDFs (Configuracion > Preferencias).\n"
        "Comprueba que tienes instalado un visor de PDF (Adobe Reader o Microsoft Edge puede abrirlos).")

    pdf.problema(
        "Los caracteres especiales salen mal en el PDF (tildes, enes)",
        "Asegurate de que el logo y los campos de empresa no tienen caracteres problematicos.\n"
        "La aplicacion usa la fuente Helvetica para maximo de compatibilidad en PDF.")

    pdf.problema(
        "La importacion de CSV falla o importa datos incorrectos",
        "Comprueba que el archivo CSV usa UTF-8 como codificacion y que tiene fila de cabecera.\n"
        "Abre el CSV en el Bloc de notas > Guardar como > selecciona UTF-8 en la codificacion.")

    pdf.problema(
        "Los datos desaparecieron o la BD parece vacia",
        "No borres ni muevas el archivo graficas_mulberry.db. Si lo hiciste por error,\n"
        "restaura la ultima copia de seguridad siguiendo el procedimiento del apartado 6.")

    pdf.problema(
        "La aplicacion va lenta al cargar muchos registros",
        "Esto puede ocurrir con miles de registros. Usa los filtros de busqueda para acotar\n"
        "los resultados. Si persiste, considera hacer una copia y limpiar datos muy antiguos.")

    pdf.problema(
        "La musica de fondo no suena",
        "Verifica que has anadido archivos MP3 en Configuracion > Musica y Sonidos.\n"
        "Comprueba que el volumen de musica no esta en 0 y que el boton Silenciar no esta activo.")

    pdf.problema(
        "Las estadisticas no muestran datos recientes",
        "Pulsa el boton de actualizacion manual dentro del modulo Estadisticas.\n"
        "Verifica que el ano seleccionado en el selector es el correcto.")

    # ════════════════════════════════════════════════════════════════════════
    # 8. SOPORTE TECNICO
    # ════════════════════════════════════════════════════════════════════════
    pdf.titulo_seccion("SOPORTE TECNICO", "8")

    pdf.titulo_sub("Datos de contacto")
    pdf.tabla(
        ["Canal", "Detalle"],
        [
            ["Web",       "www.graficasmulberry.com"],
            ["Email",     "info@graficasmulberry.com"],
            ["Telefono",  "950 30 61 64  |  605 37 16 96"],
            ["Direccion", "Calle Rio Minyo, 54  -  3F  |  04230 Huercal de Almeria  |  Almeria"],
            ["Horario",   "Lunes a viernes: 9:00 - 14:00 y 16:00 - 20:00"],
        ],
        [28, 160]
    )

    pdf.titulo_sub("Actualizaciones de la aplicacion")
    pdf.parrafo(
        "Las actualizaciones de Graficas Mulberry se distribuyen como nuevos instaladores.\n"
        "Antes de actualizar: haz siempre una copia de seguridad de la base de datos (.db).\n"
        "La actualizacion conserva todos los datos existentes.")

    pdf.caja("Licencia y uso",
             "Este software es propiedad de Graficas Mulberry y esta licenciado para uso interno.\n"
             "Queda prohibida su redistribucion, modificacion o uso fuera de la empresa\n"
             "sin autorizacion expresa por escrito.",
             color=GRIS, bg=GRIS_CLARO)

    # ════════════════════════════════════════════════════════════════════════
    # CONTRAPORTADA
    # ════════════════════════════════════════════════════════════════════════
    pdf.add_page()
    pdf.set_fill_color(*MULBERRY_BG)
    pdf.rect(0, 0, 210, 175, style="F")
    pdf.set_fill_color(*MULBERRY)
    pdf.rect(0, 175, 210, 122, style="F")

    pdf.set_y(25)
    if os.path.exists(LOGO_PATH):
        pdf.image(LOGO_PATH, x=80, y=25, w=50)
        pdf.set_y(82)

    pdf.set_text_color(*MULBERRY)
    pdf.set_font("Helvetica", "B", 19)
    pdf.cell(0, 10, "Sistema de Gestion Empresarial", align="C",
             new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_font("Helvetica", "", 10)
    pdf.set_text_color(*GRIS)
    pdf.cell(0, 6,
             "Clientes  |  Presupuestos  |  Facturas  |  Albaranes  |  Pedidos",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.cell(0, 6,
             "Materiales  |  Nominas  |  Estadisticas  |  Calendario  |  IA  |  Importar  |  Exportar",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    pdf.ln(18)
    pdf.set_fill_color(*MULBERRY)
    pdf.set_text_color(*BLANCO)
    pdf.set_font("Helvetica", "B", 10)
    pdf.cell(0, 8, "  Modulos incluidos en la version 3", fill=True,
             new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_font("Helvetica", "", 9)
    pdf.set_fill_color(*MULBERRY_BG)
    modulos_lista = [
        "Panel Principal (Dashboard)     Clientes     Presupuestos     Facturas",
        "Albaranes     Pedidos y Control de Pagos     Tarifas     Materiales y Stock",
        "Empleados     Nominas     Estadisticas y Analisis     Calendario",
        "Asistente IA (Ollama)     Importacion de Datos     Exportacion / Backup     Configuracion",
    ]
    for m in modulos_lista:
        pdf.set_text_color(*DARK)
        pdf.cell(0, 7, f"  {m}", fill=True, new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    pdf.set_y(183)
    pdf.set_text_color(*BLANCO)
    pdf.set_font("Helvetica", "B", 14)
    pdf.cell(0, 8, "Graficas Mulberry", align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_font("Helvetica", "", 10)
    pdf.set_text_color(*MULBERRY_LIGHT)
    pdf.cell(0, 6, "Serigrafia | DTF | Bordado | Vinilo | Sublimacion | Gran Formato",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(5)
    pdf.set_font("Helvetica", "", 8.5)
    pdf.cell(0, 5, "Huercal de Almeria  |  Almeria  |  Espana",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.cell(0, 5, "www.graficasmulberry.com  |  info@graficasmulberry.com  |  Tel. 950 30 61 64",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(5)
    pdf.set_font("Helvetica", "B", 9)
    pdf.set_text_color(*BLANCO)
    pdf.cell(0, 5, "Manual de Usuario  |  Version 3  |  2025", align="C")

    # ── guardar ──────────────────────────────────────────────────────────
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                       "Manual_GraficasMulberry_v3.pdf")
    pdf.output(out)
    print(f"PDF generado: {out}")
    return out


if __name__ == "__main__":
    generar()
