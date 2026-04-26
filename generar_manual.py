# -*- coding: utf-8 -*-
"""Manual de usuario v3 - Graficas Mulberry (cobertura completa de todos los modulos)."""

from fpdf import FPDF
from fpdf.enums import XPos, YPos
import os

# ── Paleta corporativa real de la aplicacion ────────────────────────────────
MULBERRY       = (107,  45,  94)
MULBERRY_DARK  = ( 45,  26,  40)
MULBERRY_LIGHT = (196, 168, 188)
MULBERRY_BG    = (245, 240, 244)
DARK           = ( 30,  22,  38)
GRIS           = (100, 100, 110)
GRIS_CLARO     = (238, 238, 242)
BLANCO         = (255, 255, 255)
VERDE          = ( 39, 174,  96)
NARANJA        = (243, 156,  18)
ROJO           = (231,  76,  60)
AZUL           = ( 76, 155, 232)
PURPURA        = (155,  89, 182)

LOGO_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)),
    "src", "main", "resources", "org", "gipsybuho", "img", "logo.jpg")


class ManualPDF(FPDF):

    def header(self):
        if self.page_no() == 1:
            return
        self.set_fill_color(*MULBERRY)
        self.rect(0, 0, 210, 12, style="F")
        self.set_font("Helvetica", "B", 8)
        self.set_text_color(*BLANCO)
        self.set_xy(10, 2)
        self.cell(130, 8, "Graficas Mulberry - Manual de Usuario v3", align="L")
        self.set_xy(0, 2)
        self.cell(200, 8, f"Pag. {self.page_no()}", align="R")
        self.ln(10)

    def footer(self):
        if self.page_no() == 1:
            return
        self.set_y(-14)
        self.set_draw_color(*MULBERRY)
        self.set_line_width(0.4)
        self.line(10, self.get_y(), 200, self.get_y())
        self.set_font("Helvetica", "", 7)
        self.set_text_color(*GRIS)
        self.cell(0, 8,
            "www.graficasmulberry.com  |  info@graficasmulberry.com  |  Tel. 950 30 61 64  |  Huercal de Almeria",
            align="C")

    # ── Elementos de estructura ───────────────────────────────────────────────

    def capitulo(self, numero, texto):
        self.add_page()
        self.set_fill_color(*MULBERRY_DARK)
        self.rect(0, 14, 210, 22, style="F")
        self.set_fill_color(*MULBERRY)
        self.rect(0, 36, 210, 3, style="F")
        self.set_text_color(*MULBERRY_LIGHT)
        self.set_font("Helvetica", "", 9)
        self.set_xy(10, 16)
        self.cell(0, 6, f"CAPITULO {numero}")
        self.set_text_color(*BLANCO)
        self.set_font("Helvetica", "B", 16)
        self.set_xy(10, 22)
        self.cell(0, 12, texto.upper())
        self.set_text_color(*DARK)
        self.ln(32)

    def titulo_seccion(self, texto, numero=""):
        self.check_page_break(14)
        self.ln(4)
        self.set_fill_color(*MULBERRY)
        self.set_text_color(*BLANCO)
        self.set_font("Helvetica", "B", 12)
        txt = f"  {numero}  {texto}" if numero else f"  {texto}"
        self.cell(0, 10, txt, new_x=XPos.LMARGIN, new_y=YPos.NEXT, fill=True)
        self.set_text_color(*DARK)
        self.ln(3)

    def titulo_sub(self, texto):
        self.check_page_break(12)
        self.set_fill_color(*MULBERRY_BG)
        self.set_text_color(*MULBERRY)
        self.set_font("Helvetica", "B", 10)
        self.cell(0, 7, f"  {texto}", new_x=XPos.LMARGIN, new_y=YPos.NEXT, fill=True)
        self.set_text_color(*DARK)
        self.ln(1)

    def parrafo(self, texto, indent=0):
        self.check_page_break(8)
        self.set_font("Helvetica", "", 9.5)
        self.set_text_color(*DARK)
        self.set_x(10 + indent)
        self.multi_cell(190 - indent, 5.2, texto,
                        new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(1)

    def vineta(self, items, color=None, icono="-"):
        self.set_font("Helvetica", "", 9.5)
        self.set_text_color(*(color or DARK))
        for item in items:
            self.check_page_break(7)
            self.set_x(14)
            self.cell(6, 5.2, icono)
            self.set_x(20)
            self.multi_cell(174, 5.2, item, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_text_color(*DARK)
        self.ln(1)

    def paso(self, numero, texto, detalle=""):
        self.check_page_break(12)
        self.set_fill_color(*MULBERRY)
        self.set_text_color(*BLANCO)
        self.set_font("Helvetica", "B", 9)
        self.cell(7, 6, str(numero), align="C", fill=True)
        self.set_text_color(*DARK)
        self.set_font("Helvetica", "B", 9.5)
        self.cell(0, 6, f"  {texto}", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        if detalle:
            self.set_font("Helvetica", "", 9)
            self.set_x(18)
            self.set_text_color(*GRIS)
            self.multi_cell(174, 5, detalle, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            self.set_text_color(*DARK)
        self.ln(2)

    def caja(self, icono, titulo, contenido, color=None):
        self.check_page_break(16)
        col = color or AZUL
        self.set_fill_color(*col)
        self.set_text_color(*BLANCO)
        self.set_font("Helvetica", "B", 9)
        self.cell(0, 7, f"  {icono}  {titulo}", fill=True,
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_fill_color(*MULBERRY_BG)
        self.set_text_color(*DARK)
        self.set_font("Helvetica", "", 9)
        self.multi_cell(0, 5.2, f"  {contenido}", fill=True,
                        new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(3)

    def tabla(self, cabeceras, filas, anchos):
        self.check_page_break(10 + len(filas) * 7)
        self.set_font("Helvetica", "B", 8.5)
        self.set_fill_color(*MULBERRY)
        self.set_text_color(*BLANCO)
        for i, cab in enumerate(cabeceras):
            self.cell(anchos[i], 7, f" {cab}", border=0, fill=True)
        self.ln()
        self.set_font("Helvetica", "", 8.5)
        par = False
        for fila in filas:
            self.check_page_break(7)
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
        self.ln(3)

    def problema(self, titulo, solucion):
        self.check_page_break(14)
        self.set_font("Helvetica", "B", 9.5)
        self.set_text_color(*ROJO)
        self.set_x(12)
        self.cell(5, 6, "!")
        self.cell(0, 6, titulo, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_font("Helvetica", "", 9)
        self.set_text_color(*DARK)
        self.set_x(20)
        self.multi_cell(173, 5, solucion, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(2)

    def separador(self):
        self.ln(2)
        self.set_draw_color(*MULBERRY_LIGHT)
        self.set_line_width(0.3)
        self.line(10, self.get_y(), 200, self.get_y())
        self.ln(3)

    def check_page_break(self, h):
        if self.get_y() + h > self.page_break_trigger:
            self.add_page()

    def flujo_doc(self, pasos):
        """Renderiza un flujo horizontal de estados conectados."""
        self.check_page_break(14)
        x_ini = 12
        ancho = int((190 - (len(pasos) - 1) * 4) / len(pasos))
        y = self.get_y()
        for i, (estado, color) in enumerate(pasos):
            x = x_ini + i * (ancho + 4)
            self.set_fill_color(*color)
            self.set_xy(x, y)
            self.set_text_color(*BLANCO)
            self.set_font("Helvetica", "B", 7.5)
            self.cell(ancho, 8, estado, align="C", fill=True)
            if i < len(pasos) - 1:
                self.set_xy(x + ancho, y)
                self.set_text_color(*MULBERRY)
                self.set_font("Helvetica", "B", 10)
                self.cell(4, 8, ">", align="C")
        self.set_text_color(*DARK)
        self.ln(12)


# ===========================================================================
def generar():
    pdf = ManualPDF(orientation="P", unit="mm", format="A4")
    pdf.set_auto_page_break(auto=True, margin=18)
    pdf.set_margins(10, 16, 10)

    # ════════════════════════════════════════════════════════════════════════
    # PORTADA
    # ════════════════════════════════════════════════════════════════════════
    pdf.add_page()
    pdf.set_fill_color(*MULBERRY_DARK)
    pdf.rect(0, 0, 210, 297, style="F")
    pdf.set_fill_color(*MULBERRY)
    pdf.rect(0, 0, 210, 8, style="F")
    pdf.rect(0, 289, 210, 8, style="F")

    if os.path.exists(LOGO_PATH):
        pdf.image(LOGO_PATH, x=75, y=35, w=62)
        pdf.set_y(108)
    else:
        pdf.set_y(70)

    pdf.set_text_color(*BLANCO)
    pdf.set_font("Helvetica", "B", 30)
    pdf.cell(0, 14, "GRAFICAS MULBERRY",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_font("Helvetica", "", 14)
    pdf.set_text_color(*MULBERRY_LIGHT)
    pdf.cell(0, 8, "Sistema de Gestion Empresarial",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(6)

    # Banda central con modulos
    pdf.set_fill_color(20, 10, 18)
    pdf.rect(20, pdf.get_y(), 170, 98, style="F")
    pdf.set_fill_color(*MULBERRY)
    pdf.rect(20, pdf.get_y(), 4, 98, style="F")

    y_caja = pdf.get_y() + 6
    modulos = [
        ("Panel Principal",    "Resumen y KPIs del negocio en tiempo real"),
        ("Clientes",           "Gestion completa de cartera de clientes"),
        ("Presupuestos",       "Creacion y seguimiento de presupuestos"),
        ("Facturas",           "Emision, cobro y exportacion a PDF"),
        ("Albaranes",          "Documentos de entrega y confirmacion de firma"),
        ("Pedidos",            "Control de pedidos y fraccionamiento de cobros"),
        ("Tarifas",            "Catalogo de precios por tecnica de impresion"),
        ("Materiales",         "Stock, consumo y compras a proveedores"),
        ("Empleados y Nominas","Plantilla, calculo SS/IRPF y recibos PDF"),
        ("Estadisticas",       "Analisis visual financiero, materiales, clientes"),
        ("Calendario",         "Recordatorios y avisos en el dashboard"),
        ("Asistente IA",       "Chat local con Ollama (100% privado)"),
        ("Importar / Exportar","Backup SQLite, CSV, SQL, JSON"),
        ("Configuracion",      "Temas, empresa, tipografia, musica"),
    ]
    for i, (mod, desc) in enumerate(modulos):
        pdf.set_xy(30, y_caja + i * 6.5)
        pdf.set_font("Courier", "B", 8)
        pdf.set_text_color(*MULBERRY_LIGHT)
        pdf.cell(46, 6, mod)
        pdf.set_font("Helvetica", "", 8)
        pdf.set_text_color(*BLANCO)
        pdf.cell(0, 6, desc, new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    pdf.set_y(270)
    pdf.set_font("Helvetica", "B", 11)
    pdf.set_text_color(*BLANCO)
    pdf.cell(0, 7, "MANUAL DE USUARIO", align="C",
             new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_font("Helvetica", "", 9)
    pdf.set_text_color(*MULBERRY_LIGHT)
    pdf.cell(0, 6, "Version 3.0  |  2025  |  Almeria, Espana",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    # ════════════════════════════════════════════════════════════════════════
    # INDICE
    # ════════════════════════════════════════════════════════════════════════
    pdf.add_page()
    pdf.set_fill_color(*MULBERRY)
    pdf.set_text_color(*BLANCO)
    pdf.set_font("Helvetica", "B", 18)
    pdf.cell(0, 13, "  INDICE DE CONTENIDOS",
             fill=True, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(4)

    capitulos_idx = [
        ("1", "Requisitos e instalacion",                        True),
        ("2", "Primeros pasos y navegacion",                      True),
        ("3", "Panel Principal (Dashboard)",                      True),
        ("4", "Clientes",                                         True),
        ("5", "Presupuestos",                                     True),
        ("6", "Facturas",                                         True),
        ("7", "Albaranes",                                        True),
        ("8", "Pedidos y Control de Cobros",                      True),
        ("9", "Tarifas",                                          True),
        ("10","Materiales: Stock, Consumo y Compras",              True),
        ("11","Empleados",                                        True),
        ("12","Nominas",                                          True),
        ("13","Estadisticas y Analisis",                          True),
        ("14","Calendario y Recordatorios",                       True),
        ("15","Asistente IA (Ollama)",                            True),
        ("16","Importar Datos",                                   True),
        ("17","Exportar y Copias de Seguridad",                   True),
        ("18","Configuracion de la Aplicacion",                   True),
        ("19","Resolucion de Problemas",                          True),
    ]
    for i, (num, tit, _) in enumerate(capitulos_idx):
        bg = MULBERRY_BG if i % 2 == 0 else BLANCO
        pdf.set_fill_color(*bg)
        pdf.set_text_color(*DARK)
        pdf.set_font("Helvetica", "B", 9)
        pdf.set_x(12)
        pdf.cell(14, 7, f" Cap. {num}", fill=True)
        pdf.set_font("Helvetica", "", 9)
        pdf.cell(170, 7, f"  {tit}", fill=True,
                 new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    pdf.ln(4)
    pdf.caja("i", "Sobre este manual",
        "Esta guia cubre el uso completo del sistema de gestion Graficas Mulberry v3.\n"
        "La aplicacion funciona sin conexion a Internet (excepto el modulo de IA).\n"
        "Todos los datos se guardan localmente en una base de datos SQLite.\n"
        "Los iconos de aviso usados en este manual:\n"
        "  [!] Advertencia importante\n"
        "  [*] Consejo util\n"
        "  [i] Informacion adicional",
        color=AZUL)

    # ════════════════════════════════════════════════════════════════════════
    # CAP 1: REQUISITOS E INSTALACION
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("1", "Requisitos e Instalacion")

    pdf.titulo_seccion("Requisitos de hardware")
    pdf.vineta([
        "Procesador: Intel/AMD de 64 bits (x86_64), 2 GHz o superior",
        "Memoria RAM: 4 GB minimo (8 GB recomendados si se usa el modulo de IA)",
        "Espacio en disco: 500 MB para la aplicacion + espacio para la base de datos",
        "Pantalla: resolucion minima 1280 x 720 (recomendado 1920 x 1080)",
    ])

    pdf.titulo_seccion("Requisitos de software")
    pdf.vineta([
        "Windows 10 / Windows 11 de 64 bits",
        "Java JDK 21 o superior (necesario para ejecutar la aplicacion)",
        "Apache Maven 3.8+ (para compilar desde IntelliJ IDEA)",
        "IntelliJ IDEA 2023+ o cualquier IDE compatible con proyectos Maven",
    ])

    pdf.caja("*", "Verificar Java instalado",
        "Abre el terminal de Windows (cmd) y ejecuta:  java -version\n"
        "Deberas ver:  java version \"21.x.x\" o superior.\n"
        "Si no aparece, descarga el JDK desde https://adoptium.net",
        color=VERDE)

    pdf.titulo_seccion("Instalacion paso a paso")
    pdf.paso(1, "Abrir el proyecto en IntelliJ IDEA",
             "Inicia IntelliJ IDEA > File > Open > selecciona la carpeta 'Graficas Mulberry'")
    pdf.paso(2, "Importar dependencias Maven",
             "IntelliJ detecta el pom.xml. Haz clic en 'Load Maven Project' (esquina superior derecha)."
             " Espera a que se descarguen todas las librerias (requiere Internet solo esta vez).")
    pdf.paso(3, "Configurar JDK",
             "File > Project Structure > SDK > selecciona JDK 21 o superior")
    pdf.paso(4, "Compilar",
             "Build > Build Project (Ctrl + F9). Debe completarse sin errores.")
    pdf.paso(5, "Ejecutar",
             "Clic derecho sobre Main.java (src/main/java/org/gipsybuho/) > Run 'Main'"
             " o pulsa Shift + F10")
    pdf.paso(6, "Primera carga",
             "La primera vez se crea automaticamente la base de datos 'graficas_mulberry.db'"
             " con datos de ejemplo precargados (tarifas, materiales, configuracion de empresa).")

    pdf.titulo_seccion("Alternativa: ejecutar desde el terminal")
    pdf.set_font("Courier", "", 9)
    pdf.set_fill_color(28, 16, 26)
    pdf.set_text_color(190, 230, 190)
    lineas_cmd = [
        "cd \"C:\\Users\\[usuario]\\MAVEN\\Graficas Mulberry\"",
        "mvn javafx:run",
    ]
    alto = len(lineas_cmd) * 5.5 + 8
    pdf.rect(10, pdf.get_y(), 190, alto, style="F")
    pdf.set_xy(14, pdf.get_y() + 4)
    for l in lineas_cmd:
        pdf.cell(185, 5.5, l, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        pdf.set_x(14)
    pdf.set_text_color(*DARK)
    pdf.ln(6)

    pdf.titulo_seccion("Ubicacion de la base de datos")
    pdf.parrafo(
        "El archivo 'graficas_mulberry.db' contiene TODOS los datos de la aplicacion: "
        "clientes, presupuestos, facturas, nominas, materiales y configuracion. "
        "Se crea en la carpeta raiz del proyecto la primera vez que se ejecuta la aplicacion.")
    pdf.caja("!", "Copia de seguridad",
        "Para hacer una copia de seguridad, simplemente copia el archivo .db a un disco "
        "externo, OneDrive o USB. Para restaurar, pega el archivo en la misma ubicacion "
        "antes de iniciar la aplicacion. Se recomienda hacer copias semanalmente.",
        color=NARANJA)

    # ════════════════════════════════════════════════════════════════════════
    # CAP 2: PRIMEROS PASOS Y NAVEGACION
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("2", "Primeros Pasos y Navegacion")

    pdf.titulo_seccion("La interfaz principal")
    pdf.parrafo(
        "Al abrir la aplicacion se muestra una ventana dividida en dos zonas: "
        "la barra lateral izquierda (menu de navegacion) y el area de contenido principal "
        "(zona central derecha donde se trabaja con cada modulo).")

    pdf.titulo_sub("Barra lateral (menu principal)")
    pdf.vineta([
        "Muestra el logo de Graficas Mulberry y el nombre de la empresa configurada.",
        "Contiene 16 botones de navegacion, uno por cada modulo de la aplicacion.",
        "El boton activo se resalta en color Mulberry con borde lateral.",
        "En la parte inferior: control de volumen con icono dinamico y numero de version.",
    ])

    pdf.titulo_sub("Botones de navegacion disponibles")
    pdf.tabla(
        ["Icono", "Modulo",              "Funcion principal"],
        [
            ["Panel",  "Panel Principal",   "Resumen ejecutivo del negocio"],
            ["Cli.",   "Clientes",          "Gestion de clientes"],
            ["Pres.",  "Presupuestos",      "Crear y gestionar presupuestos"],
            ["Fact.",  "Facturas",          "Emitir y controlar facturas"],
            ["Alb.",   "Albaranes",         "Documentos de entrega"],
            ["Ped.",   "Pedidos",           "Gestion de pedidos y cobros"],
            ["Tar.",   "Tarifas",           "Catalogo de precios"],
            ["Mat.",   "Materiales",        "Control de stock y compras"],
            ["Emp.",   "Empleados",         "Alta y baja de trabajadores"],
            ["Nom.",   "Nominas",           "Calculo y exportacion de nominas"],
            ["Est.",   "Estadisticas",      "Graficas y analisis de datos"],
            ["IA",     "Asistente IA",      "Chat con inteligencia artificial local"],
            ["Cal.",   "Calendario",        "Recordatorios y eventos"],
            ["Imp.",   "Importar datos",    "Cargar datos desde CSV/Excel/JSON"],
            ["Exp.",   "Exportar/Backup",   "Copia de seguridad y exportacion"],
            ["Cfg.",   "Configuracion",     "Ajustes de la aplicacion"],
        ],
        [15, 42, 130]
    )

    pdf.titulo_sub("Acciones comunes en todos los modulos")
    pdf.vineta([
        "+ Nuevo / Nuevo: abre el formulario para crear un registro.",
        "Editar (icono lapiz): modifica el registro seleccionado en la tabla.",
        "Borrar (icono papelera): elimina el registro tras confirmar en un dialogo.",
        "Doble clic sobre una fila de la tabla: abre el formulario de edicion directamente.",
        "Buscador (campo de texto en la parte superior): filtra los resultados en tiempo real.",
        "Todos los cambios se guardan automaticamente en la base de datos al confirmar.",
    ])

    pdf.caja("i", "Campos obligatorios",
        "Los campos marcados con (*) en los formularios son obligatorios. "
        "El boton 'Guardar' o 'OK' permanece deshabilitado hasta que se completen. "
        "El resto de campos son opcionales y se pueden rellenar posteriormente.",
        color=AZUL)

    # ════════════════════════════════════════════════════════════════════════
    # CAP 3: PANEL PRINCIPAL
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("3", "Panel Principal (Dashboard)")

    pdf.parrafo(
        "El panel principal es la pantalla de inicio de la aplicacion. "
        "Muestra un resumen ejecutivo actualizado en tiempo real del estado del negocio. "
        "No requiere ninguna accion: los datos se cargan automaticamente al abrirlo.")

    pdf.titulo_seccion("Tarjetas de informacion (KPIs)")
    pdf.parrafo("La parte superior muestra cinco tarjetas con los indicadores mas importantes:")
    pdf.tabla(
        ["Tarjeta",                    "Color",    "Informacion mostrada"],
        [
            ["Clientes",               "Azul",     "Numero total de clientes registrados"],
            ["Presupuestos pendientes", "Naranja",  "Presupuestos en estado 'enviado' sin respuesta"],
            ["Facturas pendientes",    "Rojo",     "Facturas emitidas aun sin cobrar"],
            ["Bajo stock",             "Purpura",  "Materiales con stock igual o por debajo del minimo"],
            ["Facturado este anio",    "Verde",    "Suma de todas las facturas cobradas del anio actual (euros)"],
        ],
        [52, 18, 117]
    )

    pdf.titulo_seccion("Panel de proximos recordatorios")
    pdf.parrafo(
        "La parte inferior muestra las notas del calendario que vencen en los proximos dias "
        "(configurable en Configuracion > Preferencias). Cada aviso se muestra con un badge "
        "de color indicando la urgencia:")
    pdf.vineta([
        "HOY (rojo): la nota vence hoy mismo.",
        "+1d, +2d... (naranja): vence en los proximos 2 dias.",
        "+Nd (azul): vence en los proximos N dias.",
    ])
    pdf.caja("*", "Buen uso del dashboard",
        "Revisa el dashboard al inicio de cada jornada para tener una vision rapida "
        "del estado de la empresa. Presta atencion a las facturas pendientes (en rojo) "
        "y a los materiales bajo stock (en purpura) para actuar con tiempo.",
        color=VERDE)

    # ════════════════════════════════════════════════════════════════════════
    # CAP 4: CLIENTES
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("4", "Clientes")

    pdf.parrafo(
        "El modulo de Clientes permite gestionar toda la cartera de clientes de la empresa, "
        "tanto empresas como particulares. Los clientes son la base del resto de modulos: "
        "presupuestos, facturas, albaranes y pedidos se vinculan a un cliente.")

    pdf.titulo_seccion("Buscar y filtrar clientes")
    pdf.vineta([
        "Usa el campo 'Buscar...' (parte superior) para filtrar en tiempo real.",
        "La busqueda funciona por: nombre, apellido, NIF/CIF o email.",
        "La tabla se actualiza automaticamente mientras escribes.",
    ])

    pdf.titulo_seccion("Crear un nuevo cliente")
    pdf.paso(1, "Haz clic en el boton '+ Nuevo'",
             "Se abre el formulario de alta de cliente.")
    pdf.paso(2, "Rellena los campos del formulario",
             "Nombre (*) es obligatorio. El resto son opcionales.")
    pdf.paso(3, "Selecciona el tipo de cliente",
             "'empresa' o 'particular'. Esto afecta al tipo de NIF esperado (CIF/NIF).")
    pdf.paso(4, "Haz clic en 'Guardar'",
             "El cliente queda guardado y aparece en la tabla.")

    pdf.titulo_seccion("Campos del formulario de cliente")
    pdf.tabla(
        ["Campo",     "Obligatorio", "Descripcion"],
        [
            ["Nombre",        "Si (*)",   "Nombre o razon social del cliente"],
            ["Apellido",      "No",       "Apellido (solo para particulares)"],
            ["Tipo",          "No",       "'empresa' o 'particular'"],
            ["NIF/CIF",       "No",       "Numero de identificacion fiscal"],
            ["Telefono",      "No",       "Telefono de contacto principal"],
            ["Email",         "No",       "Correo electronico del cliente"],
            ["Ciudad",        "No",       "Ciudad (valor predeterminado: Almeria)"],
            ["Codigo Postal", "No",       "Codigo postal"],
            ["Direccion",     "No",       "Calle, numero, piso..."],
            ["Notas",         "No",       "Observaciones internas sobre el cliente"],
        ],
        [38, 26, 123]
    )

    pdf.titulo_seccion("Editar un cliente")
    pdf.vineta([
        "Selecciona el cliente en la tabla y haz clic en 'Editar', o haz doble clic sobre la fila.",
        "Modifica los campos necesarios y haz clic en 'Guardar'.",
        "Los cambios se reflejan automaticamente en todos los documentos vinculados.",
    ])

    pdf.titulo_seccion("Eliminar un cliente")
    pdf.vineta([
        "Selecciona el cliente y haz clic en el boton 'Borrar'.",
        "Aparecera un dialogo de confirmacion. Haz clic en 'Aceptar' para confirmar.",
        "Solo se pueden eliminar clientes que no tengan presupuestos ni facturas asociadas.",
    ])
    pdf.caja("!", "Antes de eliminar",
        "Si el cliente tiene presupuestos o facturas, primero deberas eliminarlos. "
        "En general, es mejor no eliminar clientes sino dejarlos inactivos en las notas.",
        color=ROJO)

    # ════════════════════════════════════════════════════════════════════════
    # CAP 5: PRESUPUESTOS
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("5", "Presupuestos")

    pdf.parrafo(
        "Los presupuestos son el punto de entrada del flujo comercial de la empresa. "
        "Aqui se crea la oferta economica para el cliente, con lineas de producto, "
        "precios, descuentos e IVA. Una vez aceptado por el cliente, se convierte en factura.")

    pdf.titulo_seccion("Ciclo de vida de un presupuesto")
    pdf.flujo_doc([
        ("Borrador",  (149, 165, 166)),
        ("Enviado",   (243, 156,  18)),
        ("Aceptado",  ( 39, 174,  96)),
        ("Facturado", (107,  45,  94)),
    ])
    pdf.flujo_doc([
        ("Borrador",  (149, 165, 166)),
        ("Enviado",   (243, 156,  18)),
        ("Rechazado", (231,  76,  60)),
    ])

    pdf.titulo_seccion("Crear un presupuesto")
    pdf.paso(1, "Haz clic en '+ Nuevo'",
             "Se abre el dialogo de creacion con dos pestanias: 'Datos generales' y 'Lineas'.")
    pdf.paso(2, "Rellena los datos generales",
             "Selecciona el cliente (*), ajusta el IVA (defecto: 21%), fecha y fecha de validez "
             "(defecto: 30 dias). El numero se genera automaticamente (PRE-ANIO-XXXX).")
    pdf.paso(3, "Anade lineas de producto",
             "Ve a la pestania 'Lineas' y haz clic en '+ Aniadir linea'. "
             "Cada linea representa un producto o servicio del presupuesto.")
    pdf.paso(4, "Rellena cada linea",
             "Descripcion (*), tecnica (serigrafia, DTF, bordado...), cantidad, precio unitario "
             "y descuento (%). El total se calcula automaticamente.")
    pdf.paso(5, "Usa tarifas predefinidas (opcional)",
             "En el dialogo de linea, el selector 'Tarifa' rellena automaticamente descripcion, "
             "tecnica y precio desde el catalogo de tarifas configurado.")
    pdf.paso(6, "Guarda el presupuesto",
             "Haz clic en 'Guardar'. El presupuesto queda en estado 'borrador'.")

    pdf.titulo_seccion("Cambiar el estado del presupuesto")
    pdf.tabla(
        ["Estado",      "Cuando usarlo",                       "Como cambiarlo"],
        [
            ["Borrador",  "En preparacion, no enviado aun",    "Estado inicial por defecto"],
            ["Enviado",   "Cuando se ha mandado al cliente",   "Editar presupuesto > cambiar estado"],
            ["Aceptado",  "El cliente ha dicho que si",        "Editar presupuesto > cambiar estado"],
            ["Rechazado", "El cliente ha declinado la oferta", "Editar presupuesto > cambiar estado"],
            ["Facturado", "Se ha generado la factura",         "Automatico al crear la factura"],
        ],
        [26, 74, 87]
    )

    pdf.titulo_seccion("Convertir un presupuesto en factura")
    pdf.paso(1, "Selecciona el presupuesto aceptado en la tabla",
             "El presupuesto debe estar en estado 'aceptado' (o se pedira confirmacion).")
    pdf.paso(2, "Haz clic en 'Crear Factura'",
             "Se genera automaticamente una factura con todos los datos y lineas del presupuesto.")
    pdf.paso(3, "El presupuesto pasa a estado 'facturado'",
             "Esto impide crear una segunda factura por accidente.")

    pdf.titulo_seccion("Exportar presupuesto a PDF")
    pdf.vineta([
        "Selecciona el presupuesto y haz clic en 'Exportar PDF'.",
        "El PDF se genera con el logo y datos de la empresa.",
        "Se guarda en:  Documentos\\Mulberry\\Presupuestos\\[numero].pdf",
        "Se abre automaticamente con el visor PDF de Windows.",
    ])

    # ════════════════════════════════════════════════════════════════════════
    # CAP 6: FACTURAS
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("6", "Facturas")

    pdf.parrafo(
        "Las facturas son los documentos legales de cobro emitidos al cliente. "
        "Lo mas habitual es crearlas desde un presupuesto aceptado, aunque tambien "
        "se pueden crear manualmente. La aplicacion cumple con los requisitos "
        "de facturacion para pymes espaniolas.")

    pdf.titulo_seccion("Ciclo de vida de una factura")
    pdf.flujo_doc([
        ("Pendiente", (243, 156,  18)),
        ("Pagada",    ( 39, 174,  96)),
    ])
    pdf.flujo_doc([
        ("Pendiente", (243, 156,  18)),
        ("Anulada",   (149, 165, 166)),
    ])

    pdf.titulo_seccion("Crear una factura manualmente")
    pdf.paso(1, "Haz clic en '+ Nueva'",
             "Se abre el formulario. El numero se asigna automaticamente (FAC-ANIO-XXXX).")
    pdf.paso(2, "Selecciona el cliente y rellena los datos",
             "Fecha de emision, fecha de vencimiento, forma de pago, IVA y notas.")
    pdf.paso(3, "Anade las lineas",
             "Igual que en presupuestos: descripcion, tecnica, cantidad, precio, descuento.")
    pdf.paso(4, "Guarda la factura",
             "El estado inicial es 'pendiente' (pendiente de cobro).")

    pdf.titulo_seccion("Gestionar el estado de las facturas")
    pdf.tabla(
        ["Accion",         "Boton / Opcion",      "Resultado"],
        [
            ["Registrar cobro",  "Marcar pagada",    "Estado: PAGADA (verde)"],
            ["Cancelar factura", "Anular",           "Estado: ANULADA (gris), pide confirmacion"],
            ["Eliminar",         "Borrar",           "Elimina definitivamente (pide confirmacion)"],
        ],
        [40, 45, 102]
    )

    pdf.titulo_seccion("Exportar factura a PDF")
    pdf.vineta([
        "Selecciona la factura y haz clic en 'Exportar PDF'.",
        "El PDF incluye: numero, fecha, datos fiscales de empresa y cliente, lineas "
        "detalladas con precios, base imponible, IVA y total.",
        "Se guarda en:  Documentos\\Mulberry\\Facturas\\[numero].pdf",
        "Los datos de la empresa que aparecen en el PDF se configuran en "
        "Configuracion > Mi empresa.",
    ])

    pdf.titulo_seccion("Crear albaran desde factura")
    pdf.vineta([
        "Selecciona la factura y haz clic en 'Crear Albaran'.",
        "Se abre el modulo de Albaranes con los datos de la factura pre-rellenos.",
        "Completa las lineas de entrega y guarda.",
    ])

    pdf.caja("i", "Indicadores de color en la tabla de facturas",
        "PAGADA: verde  |  VENCIDA: rojo (vencimiento superado y sin pagar)  "
        "|  ANULADA: gris  |  PENDIENTE: naranja",
        color=AZUL)

    # ════════════════════════════════════════════════════════════════════════
    # CAP 7: ALBARANES
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("7", "Albaranes")

    pdf.parrafo(
        "Un albaran es un documento de entrega que acredita que la mercancia ha sido "
        "entregada al cliente. Se puede generar desde una factura o crear manualmente. "
        "Una vez firmado por el cliente, sirve como justificante de entrega.")

    pdf.titulo_seccion("Ciclo de vida de un albaran")
    pdf.flujo_doc([
        ("Pendiente", (243, 156,  18)),
        ("Entregado", ( 76, 155, 232)),
        ("Firmado",   ( 39, 174,  96)),
    ])

    pdf.titulo_seccion("Crear un albaran")
    pdf.paso(1, "Haz clic en '+ Nuevo' o crea desde Facturas > 'Crear Albaran'",
             "Si lo creas desde factura, se pre-rellenan el cliente y la referencia.")
    pdf.paso(2, "Rellena los datos generales",
             "Cliente (*), estado, fecha de entrega y observaciones.")
    pdf.paso(3, "Anade los articulos a entregar",
             "En la pestania 'Articulos': descripcion (*), cantidad y unidad.")
    pdf.paso(4, "Guarda el albaran",
             "El estado inicial es 'pendiente'.")

    pdf.titulo_seccion("Campos del formulario")
    pdf.tabla(
        ["Campo",             "Descripcion"],
        [
            ["Numero",            "Generado automaticamente, no editable"],
            ["Cliente (*)",       "Cliente al que se entrega la mercancia"],
            ["Estado",            "pendiente / entregado / firmado"],
            ["Fecha entrega",     "Fecha en que se entrega la mercancia"],
            ["Factura ref.",      "Numero de factura vinculada (si aplica)"],
            ["Pedido ref.",       "Numero de pedido vinculado (si aplica)"],
            ["Observaciones",     "Notas internas sobre la entrega"],
            ["Articulos",         "Lista de productos entregados con cantidad y unidad"],
        ],
        [42, 145]
    )

    pdf.titulo_seccion("Acciones disponibles")
    pdf.vineta([
        "Marcar firmado: cambia el estado a 'firmado' cuando el cliente ha aceptado y firmado.",
        "Exportar PDF: genera el albaran con espacio para firma del cliente y de la empresa.",
        "Borrar: elimina el albaran (pide confirmacion previa).",
    ])

    # ════════════════════════════════════════════════════════════════════════
    # CAP 8: PEDIDOS Y CONTROL DE COBROS
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("8", "Pedidos y Control de Cobros")

    pdf.parrafo(
        "El modulo de Pedidos es uno de los mas completos de la aplicacion. "
        "Permite registrar pedidos de clientes, asociarles pagos fraccionados "
        "y hacer un seguimiento completo del estado del cobro. "
        "Tiene dos pestanias: 'Pedidos' y 'Control de Pagos'.")

    pdf.titulo_seccion("Pestania Pedidos")
    pdf.titulo_sub("Tarjetas de resumen")
    pdf.vineta([
        "Activos: numero de pedidos en curso (no entregados ni cancelados).",
        "Total facturado: suma de importes de todos los pedidos activos.",
        "Cobrado: suma de pagos marcados como cobrados.",
        "Pendiente de cobro: diferencia entre total facturado y cobrado.",
        "Pagos vencidos: numero de pagos cuya fecha de vencimiento ya ha pasado.",
    ])

    pdf.titulo_sub("Filtros de pedidos")
    pdf.vineta([
        "Botones de filtro: Todos / Pendientes / En proceso / Listos / Entregados.",
        "Campo de busqueda: filtra por nombre de cliente, numero de pedido o descripcion.",
    ])

    pdf.titulo_sub("Indicadores de color en la tabla")
    pdf.tabla(
        ["Color",    "Estado del pedido"],
        [
            ["Azul",    "Pendiente (aun no se ha iniciado)"],
            ["Naranja", "En proceso (en produccion)"],
            ["Verde",   "Listo (terminado, pendiente de entrega)"],
            ["Purpura", "Entregado"],
            ["Gris",    "Cancelado"],
        ],
        [25, 162]
    )

    pdf.titulo_seccion("Crear un pedido")
    pdf.paso(1, "Haz clic en '+ Nuevo pedido'",
             "Se abre el formulario de creacion.")
    pdf.paso(2, "Rellena los datos obligatorios",
             "Cliente (*), numero de pedido (*) e importe total (*) son obligatorios.")
    pdf.paso(3, "Completa el resto de campos",
             "Fecha pedido, entrega prevista, estado inicial, descripcion, IVA y notas.")
    pdf.paso(4, "Guarda el pedido",
             "El pedido aparece en la tabla.")

    pdf.titulo_seccion("Gestionar pagos de un pedido")
    pdf.parrafo(
        "Selecciona un pedido en la tabla. En el panel inferior aparecen los pagos "
        "asociados a ese pedido con su estado de cobro.")
    pdf.paso(1, "Aniadir un pago manualmente",
             "Haz clic en '+ Aniadir pago'. Rellena concepto, importe, vencimiento y forma de pago.")
    pdf.paso(2, "Fraccionar el importe en plazos",
             "Haz clic en 'Fraccionar'. Elige el numero de plazos (2-24), el primer vencimiento, "
             "la periodicidad (15/30/60/90 dias) y la forma de pago. El ultimo plazo absorbe "
             "el residuo de redondeo automaticamente.")
    pdf.paso(3, "Marcar un pago como cobrado",
             "Selecciona el pago y haz clic en 'Marcar cobrado'. "
             "Se pedira la fecha real de cobro para el registro.")
    pdf.paso(4, "Editar o eliminar pagos",
             "Doble clic en el pago para editarlo. Boton 'Eliminar' para borrarlo.")

    pdf.titulo_seccion("Pestania Control de Pagos")
    pdf.parrafo(
        "Muestra TODOS los pagos de TODOS los pedidos en una sola vista consolidada. "
        "Ideal para la gestion diaria de tesoreria.")
    pdf.titulo_sub("Filtros disponibles")
    pdf.vineta([
        "Todos: muestra todos los pagos sin filtrar.",
        "Pendientes: pagos aun no cobrados y no vencidos.",
        "Vencidos: pagos cuyo vencimiento ya ha pasado y no estan cobrados.",
        "Proximos: pagos que vencen en los proximos 7 dias.",
        "Cobrados: pagos ya registrados como cobrados.",
    ])

    pdf.caja("!", "Atencion a los pagos vencidos",
        "Revisa periodicamente los pagos vencidos (filtro 'Vencidos'). "
        "Aparecen en rojo con los dias de retraso. "
        "Cuando se cobre, marca el pago como cobrado para mantener la contabilidad al dia.",
        color=ROJO)

    # ════════════════════════════════════════════════════════════════════════
    # CAP 9: TARIFAS
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("9", "Tarifas")

    pdf.parrafo(
        "El modulo de Tarifas gestiona el catalogo de precios por tecnica de impresion. "
        "Cada tarifa define el precio por unidad, el coste de preparacion (setup) y "
        "el minimo de unidades. Las tarifas se usan directamente al crear lineas de presupuesto.")

    pdf.titulo_seccion("Tecnicas disponibles")
    pdf.vineta([
        "Serigrafia: impresion con pantallas, ideal para grandes tiradas.",
        "DTF (Direct to Film): transfer digital sin minimos, valido desde 1 unidad.",
        "Bordado: logo en hilo, especial para ropa de trabajo y uniformes.",
        "Vinilo: corte y aplicacion de vinilo textil o adhesivo.",
        "Sublimacion: impresion en tejidos sinteticos con calidad fotografica.",
        "Gran Formato: lonas, carteles, vinilos para exteriores.",
        "Offset: impresion offset (opcional).",
        "Otros: cualquier otro servicio personalizado.",
    ])

    pdf.titulo_seccion("Crear una tarifa")
    pdf.paso(1, "Haz clic en '+ Nueva tarifa'",
             "Se abre el formulario de alta.")
    pdf.paso(2, "Selecciona la tecnica (*) e introduce el nombre (*)",
             "El nombre debe ser descriptivo, por ejemplo: 'Serigrafia 2 colores - Camiseta'.")
    pdf.paso(3, "Define el precio",
             "Precio por unidad (*) en euros. Este es el precio que se aplica a cada unidad "
             "del producto en la linea del presupuesto.")
    pdf.paso(4, "Define el setup (opcional)",
             "Coste inicial de preparacion (fotolitos, configuracion, etc.). "
             "Se cobra una sola vez por pedido, independientemente de la cantidad.")
    pdf.paso(5, "Define el minimo de unidades",
             "Minimo de unidades requeridas para esta tarifa. Por defecto: 1.")

    pdf.titulo_seccion("Tarifas predeterminadas incluidas")
    pdf.tabla(
        ["Tecnica",              "Nombre ejemplo",       "Precio/ud.",  "Setup",    "Min."],
        [
            ["Serigrafia",        "1 tinta",              "2,50 EUR",   "35 EUR",   "12 uds"],
            ["Serigrafia",        "2 tintas",             "3,20 EUR",   "60 EUR",   "12 uds"],
            ["Serigrafia",        "4 colores",            "4,80 EUR",   "100 EUR",  "24 uds"],
            ["DTF",               "Estandar hasta 30cm",  "3,50 EUR",   "Sin setup","1 ud"],
            ["Bordado",           "Logo basico",          "4,00 EUR",   "25 EUR",   "6 uds"],
            ["Bordado",           "Logo premium",         "7,00 EUR",   "40 EUR",   "6 uds"],
            ["Vinilo textil",     "Corte simple",         "2,80 EUR",   "Sin setup","1 ud"],
            ["Sublimacion",       "Tela sintetica",       "3,00 EUR",   "Sin setup","1 ud"],
            ["Gran Formato",      "Lona PVC 510g",        "18 EUR/m2",  "Variable", "1 m2"],
            ["Gran Formato",      "Vinilo adhesivo",      "22 EUR/m2",  "Variable", "1 m2"],
        ],
        [28, 42, 26, 22, 18]
    )

    # ════════════════════════════════════════════════════════════════════════
    # CAP 10: MATERIALES
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("10", "Materiales: Stock, Consumo y Compras")

    pdf.parrafo(
        "El modulo de Materiales tiene tres pestanias independientes que cubren "
        "el control completo del almacen: gestion de stock, reglas de consumo "
        "por tecnica de impresion, y registro de compras a proveedores.")

    pdf.titulo_seccion("Pestania 1: Stock")
    pdf.parrafo(
        "Muestra el inventario actual de todos los materiales del taller: "
        "tintas, pantallas, sustratos, vinilos, hilo de bordado, consumibles, etc.")

    pdf.titulo_sub("Ver el estado del stock")
    pdf.vineta([
        "Punto verde (izquierda): el stock actual esta por encima del minimo configurado.",
        "Punto rojo: el stock actual es igual o inferior al minimo. Hay que reponer.",
        "Filtro rapido: checkbox 'Solo materiales con stock bajo' para ver solo las alertas.",
        "Columnas: nombre, referencia, categoria, stock actual, stock minimo, precio/ud., proveedor.",
    ])

    pdf.titulo_sub("Registrar una entrada de material")
    pdf.paso(1, "Selecciona el material en la tabla",
             "Haz clic sobre la fila del material que has recibido.")
    pdf.paso(2, "Haz clic en 'Entrada'",
             "Se abre un dialogo pidiendo la cantidad recibida.")
    pdf.paso(3, "Introduce la cantidad",
             "La aplicacion suma la cantidad al stock actual y registra el movimiento.")

    pdf.titulo_sub("Registrar una salida de material")
    pdf.vineta([
        "Selecciona el material y haz clic en 'Salida'.",
        "Introduce la cantidad consumida. Se descuenta del stock automaticamente.",
        "Esta accion registra un movimiento de tipo 'salida manual'.",
    ])

    pdf.titulo_sub("Crear o editar un material")
    pdf.tabla(
        ["Campo",         "Descripcion"],
        [
            ["Nombre (*)",     "Nombre descriptivo del material (ej. 'Tinta plastisol negro')"],
            ["Referencia",     "Codigo del fabricante o referencia interna"],
            ["Categoria",      "tintas / pantallas / sustratos / vinilos / consumibles / bordado / gran formato"],
            ["Unidad",         "Unidad de medida: ud, m2, m, kg, L..."],
            ["Stock actual",   "Cantidad disponible en este momento"],
            ["Stock minimo",   "Si el stock baja de este valor, aparece alerta en rojo"],
            ["Precio/ud.",     "Coste de compra por unidad (euros)"],
            ["Proveedor",      "Nombre del proveedor habitual"],
        ],
        [38, 149]
    )

    pdf.titulo_seccion("Pestania 2: Consumo por tecnica")
    pdf.parrafo(
        "Define cuanto material se consume por unidad producida en cada tecnica. "
        "Estas reglas permiten calcular el coste de materiales de cada pedido.")
    pdf.vineta([
        "Ejemplo: 'Serigrafia 1 tinta consume 0,05 L de tinta plastisol negro por unidad'.",
        "Haz clic en '+ Aniadir regla' para crear una nueva relacion tecnica-material.",
        "Selecciona la tecnica, el material y la cantidad consumida por unidad producida.",
        "Edita o elimina reglas existentes con los botones correspondientes.",
    ])

    pdf.titulo_seccion("Pestania 3: Compras a proveedores")
    pdf.parrafo(
        "Registra todas las compras de materiales con sus condiciones de pago. "
        "Permite controlar los pagos pendientes a proveedores y sus vencimientos.")

    pdf.titulo_sub("Tarjetas de resumen")
    pdf.vineta([
        "Total pendiente: suma de todas las compras aun no pagadas.",
        "Vencidos: numero y suma de facturas a proveedor cuyo plazo ya ha expirado.",
        "Proximos 7 dias: pagos a proveedores que vencen esta semana.",
    ])

    pdf.titulo_sub("Registrar una compra a proveedor")
    pdf.paso(1, "Haz clic en '+ Nueva compra'",
             "Se abre el formulario de compra.")
    pdf.paso(2, "Selecciona el material (*)",
             "El campo Proveedor se rellena automaticamente con el proveedor habitual del material.")
    pdf.paso(3, "Introduce el numero de factura del proveedor, fecha de compra e importe (*)",
             "El importe es el total de la factura del proveedor (obligatorio).")
    pdf.paso(4, "Selecciona la forma de pago",
             "Contado, 30 dias, 60 dias, etc. El vencimiento se calcula automaticamente.")
    pdf.paso(5, "Marca como pagado cuando se realice el pago",
             "Selecciona la compra y haz clic en 'Marcar pagado'.")

    pdf.titulo_sub("Indicadores de estado en compras")
    pdf.tabla(
        ["Color",    "Estado"],
        [
            ["Verde",    "Pagado"],
            ["Rojo",     "Vencido (fecha de pago superada)"],
            ["Naranja",  "Proximo (vence en menos de 7 dias)"],
            ["Azul",     "Pendiente (plazo aun no vencido)"],
        ],
        [25, 162]
    )

    # ════════════════════════════════════════════════════════════════════════
    # CAP 11: EMPLEADOS
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("11", "Empleados")

    pdf.parrafo(
        "El modulo de Empleados gestiona la plantilla de la empresa. "
        "Permite dar de alta trabajadores, registrar sus datos laborales "
        "y gestionar sus altas y bajas. Los empleados estan vinculados al modulo de Nominas.")

    pdf.titulo_seccion("Tipos de empleado")
    pdf.vineta([
        "Operario, Oficial de 1a, Oficial de 2a, Auxiliar",
        "Tecnico, Administrativo, Encargado, Gerente",
    ])

    pdf.titulo_seccion("Dar de alta un empleado")
    pdf.paso(1, "Haz clic en '+ Nuevo'",
             "Se abre el formulario con valores predeterminados: "
             "salario base 1.200 EUR, IRPF 15%, activo=si, fecha alta=hoy.")
    pdf.paso(2, "Rellena los datos obligatorios",
             "Nombre (*) es el unico campo obligatorio.")
    pdf.paso(3, "Completa los datos laborales",
             "NIF, categoria, salario base, IRPF personal, telefono, email, IBAN y direccion.")
    pdf.paso(4, "Guarda",
             "El empleado queda activo y disponible para generar nominas.")

    pdf.titulo_seccion("Dar de baja a un empleado")
    pdf.vineta([
        "Selecciona el empleado activo en la tabla.",
        "Haz clic en 'Dar de baja'.",
        "El sistema registra la fecha de baja automaticamente (fecha del dia).",
        "El empleado pasa a estado BAJA y deja de aparecer en la seleccion de nominas.",
        "Para ver empleados de baja: activa el checkbox 'Mostrar empleados dados de baja'.",
    ])

    pdf.titulo_seccion("Reactivar un empleado")
    pdf.vineta([
        "Activa el filtro 'Mostrar empleados dados de baja'.",
        "Selecciona el empleado y haz clic en 'Reactivar'.",
        "El empleado vuelve a estado ACTIVO.",
    ])

    pdf.titulo_seccion("Campos del formulario de empleado")
    pdf.tabla(
        ["Campo",          "Descripcion"],
        [
            ["Nombre (*)",     "Nombre del empleado"],
            ["Apellido",       "Apellidos"],
            ["NIF",            "Numero de identificacion fiscal"],
            ["Categoria",      "Puesto de trabajo (ver lista arriba)"],
            ["Fecha alta",     "Fecha de incorporacion a la empresa"],
            ["Salario base",   "Salario bruto base mensual en euros"],
            ["IRPF (%)",       "Porcentaje de retencion de IRPF individual"],
            ["Telefono",       "Telefono de contacto"],
            ["Email",          "Correo electronico"],
            ["IBAN",           "Cuenta bancaria para el ingreso de la nomina"],
            ["Direccion",      "Domicilio del trabajador"],
            ["Activo",         "Checkbox: si esta marcado el empleado esta activo"],
        ],
        [38, 149]
    )

    # ════════════════════════════════════════════════════════════════════════
    # CAP 12: NOMINAS
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("12", "Nominas")

    pdf.parrafo(
        "El modulo de Nominas calcula automaticamente los recibos de salario "
        "siguiendo la legislacion laboral espaniola, con las cotizaciones a la "
        "Seguridad Social vigentes en 2024 y el calculo de IRPF individual. "
        "Los recibos se pueden exportar a PDF.")

    pdf.titulo_seccion("Como funciona el calculo")
    pdf.tabla(
        ["Concepto",                    "Porcentaje",  "Quien lo deduce"],
        [
            ["Contingencias comunes",   "4,70%",       "Trabajador (descuenta del bruto)"],
            ["Desempleo",               "1,55%",       "Trabajador"],
            ["Formacion profesional",   "0,10%",       "Trabajador"],
            ["IRPF",                    "Variable",    "Trabajador (segun tipo personal)"],
            ["SS empresa (total)",      "aprox. 31,4%","Empresa (coste adicional)"],
        ],
        [72, 26, 89]
    )

    pdf.titulo_seccion("Crear una nomina")
    pdf.paso(1, "Haz clic en '+ Nueva nomina'",
             "Se abre el formulario de calculo.")
    pdf.paso(2, "Selecciona el empleado (*) y el periodo (mes y anio)",
             "El sistema avisa si ya existe una nomina para ese empleado y periodo.")
    pdf.paso(3, "Revisa el salario base e IRPF",
             "Se copian automaticamente del empleado. Puedes modificarlos para este mes.")
    pdf.paso(4, "Introduce complementos y percepciones (si aplica)",
             "Complementos salariales en euros. Percepciones no salariales (dietas, etc.).")
    pdf.paso(5, "Introduce horas extra (si aplica)",
             "Horas extra normales (precio por defecto: 15 EUR/h) y "
             "festivas (22 EUR/h por defecto).")
    pdf.paso(6, "Consulta el panel de calculo en tiempo real",
             "A la derecha se muestra el desglose: total bruto, deducciones, "
             "LIQUIDO NETO (lo que cobra el empleado) y coste total para la empresa.")
    pdf.paso(7, "Guarda la nomina",
             "La nomina queda registrada. Desde la tabla puedes exportarla a PDF.")

    pdf.titulo_seccion("Generar nominas para todos los empleados")
    pdf.vineta([
        "Haz clic en el boton 'Generar mes para todos'.",
        "Selecciona el mes y el anio.",
        "La aplicacion crea automaticamente una nomina para cada empleado activo.",
        "Si ya existe una nomina para algun empleado ese mes, se omite (no se duplica).",
    ])

    pdf.titulo_seccion("Exportar nomina a PDF")
    pdf.vineta([
        "Selecciona la nomina en la tabla y haz clic en 'Exportar PDF'.",
        "El PDF incluye: datos del empleado y empresa, percepciones, deducciones y liquido.",
        "Se guarda en:  Documentos\\Mulberry\\Nominas\\[anio-mes_empleado].pdf",
    ])

    pdf.caja("*", "IBAN en la nomina",
        "Para que el IBAN del empleado aparezca en el PDF de la nomina, "
        "debes haberlo introducido en el formulario del empleado (modulo Empleados).",
        color=VERDE)

    # ════════════════════════════════════════════════════════════════════════
    # CAP 13: ESTADISTICAS
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("13", "Estadisticas y Analisis")

    pdf.parrafo(
        "El modulo de Estadisticas ofrece un dashboard analitico con graficas interactivas "
        "que muestran la evolucion del negocio. Los datos se cargan de forma asincrona "
        "(con un spinner mientras se procesan). Tiene cuatro pestanias.")

    pdf.titulo_seccion("Selector de anio y actualizacion")
    pdf.vineta([
        "Usa el combo 'Anio' (parte superior) para cambiar el periodo de analisis.",
        "Boton 'Actualizar' (icono flecha): recarga los datos de las pestanias ya visitadas.",
        "Boton 'Exportar PDF': genera un informe PDF completo con todos los datos del anio.",
        "El PDF se guarda en: Documentos\\Mulberry\\Estadisticas\\Estadisticas_[anio].pdf",
    ])

    pdf.titulo_seccion("Pestania: Financiero")
    pdf.vineta([
        "4 tarjetas KPI: Ingresos del anio, Gastos materiales, Gastos nominas, Beneficio estimado.",
        "Grafica de barras: ingresos vs gastos por mes (3 series: ingresos, materiales, nominas).",
        "Grafica de linea: facturacion acumulada mes a mes.",
        "Dos graficas de sectores: estado de facturas y estado de presupuestos.",
    ])

    pdf.titulo_seccion("Pestania: Materiales")
    pdf.vineta([
        "Top 10 materiales mas consumidos (unidades de salida registradas).",
        "Distribucion de materiales por categoria (grafica de sectores).",
        "Stock actual de los 12 materiales con mas inventario.",
    ])

    pdf.titulo_seccion("Pestania: Clientes")
    pdf.vineta([
        "KPIs: total clientes acumulado, nuevos clientes en el anio, tipo mas habitual.",
        "Evolucion de nuevos clientes por mes.",
        "Distribucion de clientes por tipo (empresa / particular).",
        "Top 8 clientes por volumen de facturacion.",
    ])

    pdf.titulo_seccion("Pestania: Produccion")
    pdf.vineta([
        "Ingresos por tecnica de impresion (serigrafia, DTF, bordado...).",
        "Volumen de lineas de produccion por tecnica.",
        "Estado actual de los pedidos (pendiente, en proceso, listo, entregado, cancelado).",
    ])

    pdf.titulo_seccion("Exportar informe de estadisticas a PDF")
    pdf.paso(1, "Selecciona el anio en el combo",
             "Por defecto se muestra el anio actual.")
    pdf.paso(2, "Haz clic en 'Exportar PDF'",
             "El boton muestra 'Generando...' mientras se crea el informe.")
    pdf.paso(3, "Al terminar aparece un dialogo",
             "Muestra la ruta del archivo guardado. Puedes abrirlo directamente "
             "haciendo clic en 'Abrir archivo'.")

    pdf.caja("i", "El informe PDF incluye",
        "Pagina 1: Financiero (KPIs, tabla mensual y estados de documentos)\n"
        "Pagina 2: Materiales (consumo, categorias, stock)\n"
        "Pagina 3: Clientes (KPIs, top clientes, tipos)\n"
        "Pagina 4: Produccion (tecnicas, pedidos)",
        color=AZUL)

    # ════════════════════════════════════════════════════════════════════════
    # CAP 14: CALENDARIO Y RECORDATORIOS
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("14", "Calendario y Recordatorios")

    pdf.parrafo(
        "El modulo de Calendario permite crear recordatorios y notas vinculadas a fechas "
        "concretas. Los avisos con fechas proximas aparecen automaticamente en el Panel "
        "Principal (Dashboard) cada vez que se abre la aplicacion.")

    pdf.titulo_seccion("Navegar por el calendario")
    pdf.vineta([
        "Botones '<' y '>': cambian al mes anterior / siguiente.",
        "Boton 'Hoy': vuelve al mes actual.",
        "Dias con notas: muestran un punto de color como indicador visual.",
        "Dia de hoy: aparece resaltado con el color principal de la aplicacion.",
    ])

    pdf.titulo_seccion("Crear una nota o recordatorio")
    pdf.paso(1, "Haz clic sobre el dia que deseas",
             "Se abre el dialogo de notas para ese dia.")
    pdf.paso(2, "En la seccion 'Nueva nota', introduce el titulo",
             "El titulo es el texto que aparecera en el dashboard como aviso.")
    pdf.paso(3, "Anade un detalle opcional",
             "Informacion adicional que solo aparece en el dialogo del dia.")
    pdf.paso(4, "Haz clic en 'Guardar'",
             "La nota queda guardada. El dia mostrara un punto indicador en el calendario.")

    pdf.titulo_seccion("Ver o eliminar notas existentes")
    pdf.vineta([
        "Haz clic sobre un dia que tenga el punto indicador.",
        "El dialogo muestra todas las notas guardadas para ese dia.",
        "Para eliminar una nota, haz clic en el boton 'x' que aparece junto a ella.",
        "Un dia puede tener multiples notas.",
    ])

    pdf.titulo_seccion("Avisos en el Dashboard")
    pdf.parrafo(
        "El panel de 'Proximos recordatorios' del Dashboard muestra automaticamente "
        "las notas cuya fecha cae dentro del plazo configurado (por defecto 7 dias). "
        "Para cambiar este plazo, ve a Configuracion > Preferencias > "
        "'Panel dashboard mostrar eventos (dias)'.")

    # ════════════════════════════════════════════════════════════════════════
    # CAP 15: ASISTENTE IA (OLLAMA)
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("15", "Asistente IA (Ollama)")

    pdf.parrafo(
        "La aplicacion incluye un asistente de inteligencia artificial que funciona "
        "COMPLETAMENTE en tu propio ordenador. Ninguna pregunta ni respuesta sale de "
        "tu red local. Utiliza Ollama, un motor de IA gratuito y de codigo abierto.")

    pdf.titulo_seccion("Instalar Ollama")
    pdf.paso(1, "Haz clic en 'Instalar Ollama' en la vista IA",
             "Se abre el asistente de instalacion automatica. Alternativamente, "
             "puedes descargar el instalador manualmente desde: https://ollama.com")
    pdf.paso(2, "Sigue el asistente de instalacion",
             "El asistente descarga e instala Ollama y el modelo llama3.2 automaticamente. "
             "Este proceso requiere Internet y puede tardar varios minutos (modelo ~2 GB).")
    pdf.paso(3, "Verificar que Ollama esta activo",
             "El indicador de estado en la vista IA debe mostrar 'Ollama listo'. "
             "Ollama arranca automaticamente con Windows una vez instalado.")

    pdf.titulo_seccion("Usar el Asistente IA")
    pdf.paso(1, "Selecciona el modelo en el desplegable",
             "Por defecto aparece llama3.2. Puedes descargar otros modelos en 'Modelos'.")
    pdf.paso(2, "Escribe tu pregunta en el cuadro de texto",
             "Puedes usar Enter para enviar o Shift + Enter para nueva linea.")
    pdf.paso(3, "O haz clic en una sugerencia rapida",
             "Hay chips con preguntas frecuentes sobre impresion y gestion.")
    pdf.paso(4, "Lee la respuesta en el chat",
             "La respuesta aparece caracter a caracter (streaming). "
             "Un cursor intermitente indica que el modelo esta generando texto.")

    pdf.titulo_seccion("Casos de uso recomendados")
    pdf.vineta([
        "Calcular precios: 'Cuanto deberia cobrar por 200 camisetas de serigrafia a 3 colores?'",
        "Redactar textos: 'Escribe una descripcion profesional para un presupuesto de bordado'.",
        "Dudas tecnicas: 'Que diferencia hay entre DTF y serigrafia para pedidos de 50 uds?'",
        "Asesoramiento: 'Que complementos salariales cotizan a la Seguridad Social?'",
        "Marketing: 'Sugiere como vender impresion textil a empresas locales'.",
        "Cualquier consulta relacionada con la gestion del taller de impresion.",
    ])

    pdf.titulo_seccion("Gestionar modelos de IA")
    pdf.paso(1, "Haz clic en el boton 'Modelos' (junto al selector de modelo)",
             "Se abre el gestor de modelos instalados.")
    pdf.paso(2, "Ver modelos instalados",
             "La lista muestra todos los modelos disponibles con su tamano en MB.")
    pdf.paso(3, "Descargar un nuevo modelo",
             "Haz clic en uno de los chips de modelos populares (llama3.2, mistral, phi4...)"
             " o escribe el nombre manualmente y haz clic en 'Descargar'.")
    pdf.paso(4, "Eliminar un modelo",
             "Haz clic en el icono papelera junto al modelo para liberarlo (confirmacion previa).")

    pdf.titulo_sub("Modelos recomendados")
    pdf.tabla(
        ["Modelo",         "Tamano aprox.", "Perfil"],
        [
            ["llama3.2",   "~2 GB",         "Recomendado: rapido y muy capaz para uso general"],
            ["phi4",       "~8 GB",         "Muy preciso, requiere 16 GB de RAM"],
            ["mistral",    "~4 GB",         "Buena relacion calidad/tamano"],
            ["phi3.5",     "~2 GB",         "Ligero y eficiente"],
            ["qwen2.5",    "~4 GB",         "Excelente en matematicas y analisis"],
        ],
        [30, 28, 129]
    )

    pdf.caja("*", "Privacidad garantizada",
        "Toda la IA funciona en tu ordenador. Ningun dato (clientes, precios, nominas) "
        "sale de tu equipo ni de tu red local. Es un sistema 100% privado.",
        color=VERDE)

    # ════════════════════════════════════════════════════════════════════════
    # CAP 16: IMPORTAR DATOS
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("16", "Importar Datos")

    pdf.parrafo(
        "El modulo de importacion permite cargar datos de clientes, materiales, "
        "empleados o tarifas desde archivos externos (CSV, Excel, JSON). "
        "El proceso sigue 4 pasos guiados con asistencia de IA para detectar "
        "y mapear los campos automaticamente.")

    pdf.titulo_seccion("Formatos de archivo soportados")
    pdf.tabla(
        ["Formato",  "Extension",  "Uso tipico"],
        [
            ["CSV",     ".csv",       "Exportaciones de Excel, Google Sheets, cualquier gestor"],
            ["Excel",   ".xlsx/.xls", "Hojas de calculo de Microsoft Excel"],
            ["JSON",    ".json",      "Exportaciones de aplicaciones, APIs"],
            ["Texto",   ".txt",       "Archivos de texto separados por coma o punto y coma"],
        ],
        [20, 25, 142]
    )

    pdf.titulo_seccion("Proceso de importacion paso a paso")
    pdf.paso(1, "Seleccionar archivo",
             "Haz clic en 'Elegir archivo...' y selecciona el archivo desde el explorador. "
             "La aplicacion muestra el nombre del archivo y su tamano.")
    pdf.paso(2, "Detectar tipo de datos",
             "La aplicacion detecta las columnas del archivo. Selecciona el tipo de datos "
             "que deseas importar: Clientes, Materiales, Empleados o Tarifas. "
             "Si Ollama esta disponible, la IA detecta automaticamente el tipo.")
    pdf.paso(3, "Mapear campos",
             "La tabla de mapeo muestra cada columna del archivo con un selector "
             "del campo de destino en la aplicacion. "
             "La IA mapea automaticamente los campos mas probables. "
             "Puedes ajustar manualmente cualquier mapeo. "
             "Las columnas sin equivalente se pueden marcar como '(ignorar)'.")
    pdf.paso(4, "Vista previa e importar",
             "Se muestran los primeros 8 registros para verificar que los datos son correctos. "
             "Haz clic en 'Importar X registros' para iniciar la importacion. "
             "Una barra de progreso muestra el avance.")

    pdf.titulo_seccion("Despues de importar")
    pdf.vineta([
        "Aparece un dialogo confirmando el numero de registros importados.",
        "El boton 'Abrir seccion' lleva directamente al modulo correspondiente.",
        "El log inferior registra cada paso del proceso con hora exacta.",
    ])

    pdf.caja("!", "Antes de importar",
        "Asegurate de que el archivo no tiene filas de cabecera duplicadas ni celdas vacias "
        "en los campos obligatorios (el campo 'nombre' siempre es obligatorio). "
        "Guarda una copia de seguridad de la base de datos antes de importar datos masivos.",
        color=NARANJA)

    # ════════════════════════════════════════════════════════════════════════
    # CAP 17: EXPORTAR Y COPIAS DE SEGURIDAD
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("17", "Exportar y Copias de Seguridad")

    pdf.parrafo(
        "El modulo de exportacion permite hacer copias de seguridad de la base de datos "
        "y exportar los datos en diferentes formatos para uso externo. "
        "Se recomienda hacer una copia de seguridad semanal.")

    pdf.titulo_seccion("Informacion de la base de datos")
    pdf.parrafo(
        "La parte superior muestra: ruta del archivo de base de datos, "
        "tamano actual, y un recuento de los registros principales "
        "(clientes, facturas, presupuestos, etc.). "
        "El boton 'Abrir carpeta' abre el explorador en la ubicacion del archivo.")

    pdf.titulo_seccion("Tipos de exportacion disponibles")
    pdf.tabla(
        ["Tipo",                  "Formato",  "Uso recomendado"],
        [
            ["Copia de seguridad", ".db",      "Restaurar en otro PC. Copia identica de toda la BD."],
            ["CSV (comprimido)",   ".zip",     "Abrir en Excel, LibreOffice o Google Sheets."],
            ["Volcado SQL",        ".sql",     "Importar en otra base de datos SQLite o compatible."],
            ["JSON estructurado",  ".json",    "Integracion con otras aplicaciones o APIs."],
        ],
        [40, 20, 127]
    )

    pdf.titulo_seccion("Como exportar")
    pdf.paso(1, "Haz clic en el boton del tipo de exportacion deseado",
             "Ejemplo: 'Exportar .db' para copia de seguridad.")
    pdf.paso(2, "Selecciona donde guardar el archivo",
             "Se abre el explorador de archivos. El nombre sugerido incluye fecha y hora "
             "(ej. 'GraficasMulberry_20250426_143022.db').")
    pdf.paso(3, "Espera a que se complete",
             "El log inferior muestra el progreso en tiempo real. "
             "Al terminar aparece un mensaje con el tamano del archivo exportado.")
    pdf.paso(4, "Verifica el resultado",
             "Aparece un dialogo con la opcion de abrir la carpeta donde se guardo el archivo.")

    pdf.titulo_seccion("Restaurar una copia de seguridad (.db)")
    pdf.paso(1, "Cierra la aplicacion completamente",
             "Es importante que no este en ejecucion durante la restauracion.")
    pdf.paso(2, "Localiza el archivo .db de copia que quieres restaurar",
             "El archivo que guardaste con la exportacion de tipo 'Copia de seguridad'.")
    pdf.paso(3, "Renombralo a 'graficas_mulberry.db'",
             "Elimina (o renombra) el archivo actual y pon en su lugar la copia.")
    pdf.paso(4, "Vuelve a abrir la aplicacion",
             "Los datos se cargaran desde la copia restaurada.")

    # ════════════════════════════════════════════════════════════════════════
    # CAP 18: CONFIGURACION DE LA APLICACION
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("18", "Configuracion de la Aplicacion")

    pdf.parrafo(
        "El modulo de Configuracion permite personalizar la aplicacion segun "
        "las necesidades de la empresa. Tiene cuatro pestanias: "
        "Apariencia, Mi empresa, Preferencias y Musica.")

    pdf.titulo_seccion("Pestania: Apariencia")
    pdf.titulo_sub("Temas de color")
    pdf.parrafo(
        "Hay 6 temas de color disponibles. Haz clic en la tarjeta del tema para aplicarlo "
        "al instante (sin necesidad de reiniciar la aplicacion):")
    pdf.tabla(
        ["Tema",         "Descripcion"],
        [
            ["Mulberry",  "Tema predeterminado: purpura oscuro y granate (colores corporativos)"],
            ["Oscuro",    "Azul muy oscuro, ideal para trabajo nocturno"],
            ["Azul",      "Azul marino profesional"],
            ["Verde",     "Verde natural y tierra"],
            ["Rojo",      "Rojo intenso con contrastes oscuros"],
            ["Claro",     "Fondo blanco con acentos en gris, maxima legibilidad"],
        ],
        [25, 162]
    )

    pdf.titulo_sub("Tipografia")
    pdf.vineta([
        "Fuente: Segoe UI (recomendada), Arial, Calibri, Georgia, Consolas, Trebuchet, Verdana.",
        "Tamano: Pequenio (11px), Normal (13px, defecto), Grande (15px), Muy grande (17px).",
        "Haz clic en 'Aplicar tipografia' para que el cambio surta efecto.",
    ])

    pdf.titulo_seccion("Pestania: Mi empresa")
    pdf.parrafo(
        "Estos datos aparecen en todos los documentos PDF generados "
        "(presupuestos, facturas, albaranes, nominas). "
        "Es importante rellenarlos correctamente antes de emitir cualquier documento.")
    pdf.tabla(
        ["Campo",              "Descripcion"],
        [
            ["Nombre/Razon social", "Nombre legal de la empresa o autonomo"],
            ["NIF/CIF",             "Numero de identificacion fiscal"],
            ["Direccion",           "Calle y numero del establecimiento"],
            ["Ciudad",              "Ciudad o municipio"],
            ["Codigo postal",       "CP del establecimiento"],
            ["Telefono",            "Telefono principal de contacto"],
            ["Email",               "Correo electronico comercial"],
            ["Pagina web",          "URL de la web (aparece en el pie del PDF)"],
            ["IBAN",                "Cuenta bancaria de la empresa"],
            ["Actividad/CNAE",      "Descripcion de la actividad economica"],
        ],
        [42, 145]
    )
    pdf.vineta(["Haz clic en 'Guardar cambios' para que los datos se actualicen en los documentos."])

    pdf.titulo_seccion("Pestania: Preferencias")
    pdf.titulo_sub("Facturacion")
    pdf.tabla(
        ["Preferencia",                "Valor defecto", "Descripcion"],
        [
            ["IVA por defecto (%)",    "21%",           "Se aplica en nuevos presupuestos y facturas"],
            ["Prefijo presupuesto",    "PRE",           "Ej: PRE-2025-0001"],
            ["Prefijo factura",        "FAC",           "Ej: FAC-2025-0001"],
            ["Condiciones por defecto","(texto)",       "Texto que aparece en 'Condiciones' de presupuestos"],
        ],
        [55, 24, 108]
    )

    pdf.titulo_sub("Recordatorios del Calendario")
    pdf.vineta([
        "Alerta emergente al iniciar (dias): muestra un dialogo al arrancar si hay notas "
        "en los proximos N dias. Defecto: 3 dias.",
        "Panel dashboard mostrar eventos (dias): cuantos dias hacia adelante muestra "
        "el panel de recordatorios del Dashboard. Defecto: 7 dias.",
    ])

    pdf.titulo_seccion("Pestania: Musica")
    pdf.parrafo(
        "La aplicacion puede reproducir musica de fondo mientras trabajas. "
        "Soporta archivos MP3 organizados en una lista de reproduccion.")
    pdf.vineta([
        "'+ Aniadir MP3': abre el explorador para seleccionar uno o varios archivos MP3.",
        "Doble clic en la cancion: empieza a reproducir inmediatamente.",
        "Controles: anterior, play/pausa, stop, siguiente.",
        "Slider de volumen: 0% a 100%.",
        "Repetir lista en bucle: cuando acaba la lista, vuelve a empezar.",
        "Reproducir automaticamente al iniciar: si esta marcado, la musica empieza al abrir la app.",
        "Haz clic en 'Guardar configuracion de musica' para conservar tus ajustes.",
    ])
    pdf.caja("i", "Control de volumen global",
        "El control de volumen de la barra lateral (esquina inferior izquierda) "
        "afecta a toda la musica y sonidos de la aplicacion. "
        "El icono cambia segun el nivel: silenciado / bajo / medio / alto. "
        "Haz clic en el icono para silenciar/activar rapidamente.",
        color=AZUL)

    # ════════════════════════════════════════════════════════════════════════
    # CAP 19: RESOLUCION DE PROBLEMAS
    # ════════════════════════════════════════════════════════════════════════
    pdf.capitulo("19", "Resolucion de Problemas")

    pdf.titulo_seccion("Problemas de instalacion y arranque")

    pdf.problema(
        "La aplicacion no arranca / error 'java: command not found'",
        "Java 21 no esta instalado o no esta en el PATH del sistema. "
        "Descarga el JDK desde https://adoptium.net, instalalo y reinicia el ordenador. "
        "Verifica con:  java -version  en el terminal (cmd).")

    pdf.problema(
        "Error 'UnsupportedClassVersionError' al compilar",
        "El proyecto requiere Java 21 o superior. Tu JDK instalado es una version anterior. "
        "Actualiza el JDK y en IntelliJ ve a: File > Project Structure > SDK > selecciona JDK 21+")

    pdf.problema(
        "Error de dependencias Maven al compilar",
        "Las librerias no se han descargado. Abre el panel Maven (derecha en IntelliJ), "
        "haz clic en 'Reload All Maven Projects'. Requiere conexion a Internet la primera vez.")

    pdf.titulo_seccion("Problemas con los modulos")

    pdf.problema(
        "No aparece el logo en los PDF generados",
        "Verifica que existe el archivo logo.jpg en: "
        "src/main/resources/org/gipsybuho/img/logo.jpg")

    pdf.problema(
        "Los PDF no se abren automaticamente",
        "Windows no tiene configurado un visor PDF predeterminado. "
        "Instala Adobe Acrobat Reader o el visor PDF integrado de Windows (Edge). "
        "Tambien puedes abrir el archivo manualmente desde la carpeta Documentos\\Mulberry\\")

    pdf.problema(
        "No puedo eliminar un cliente (error o boton deshabilitado)",
        "El cliente tiene presupuestos, facturas u otros documentos vinculados. "
        "Elimina primero esos documentos, o simplemente deja el cliente inactivo.")

    pdf.problema(
        "El calculo de la nomina no coincide con lo esperado",
        "Verifica el porcentaje de IRPF y el salario base en el formulario del empleado. "
        "Los porcentajes de SS aplicados son los vigentes en 2024 para Espania: "
        "4,70% + 1,55% + 0,10% = 6,35% trabajador.")

    pdf.titulo_seccion("Problemas con la IA")

    pdf.problema(
        "El asistente IA muestra 'Ollama no disponible' o el indicador esta en rojo",
        "1. Verifica que Ollama esta instalado (icono en la bandeja del sistema de Windows).\n"
        "2. Abre el terminal y ejecuta:  ollama list  (debe mostrar al menos un modelo).\n"
        "3. Si no hay ningun modelo, ejecuta:  ollama pull llama3.2\n"
        "4. Reinicia la aplicacion y espera hasta 15 segundos a que se detecte Ollama.")

    pdf.problema(
        "La descarga del modelo tarda mucho o se interrumpe",
        "Los modelos de IA pesan entre 2 y 8 GB. Con conexion lenta puede tardar horas. "
        "Si se interrumpe, vuelve a ejecutar:  ollama pull [nombre_modelo]  (retoma donde paro).")

    pdf.problema(
        "El asistente da respuestas muy lentas",
        "El rendimiento depende del hardware disponible. Con una CPU de 4 nucleos sin GPU, "
        "el modelo llama3.2 tarda entre 2 y 10 segundos por respuesta. "
        "Si tienes GPU NVIDIA, Ollama la usa automaticamente (mucho mas rapido). "
        "Prueba el modelo phi3.5 (mas ligero y rapido).")

    pdf.titulo_seccion("Problemas con los datos")

    pdf.problema(
        "Los datos desaparecen o hay errores al abrir",
        "La base de datos puede estar corrupta. Restaura la ultima copia de seguridad: "
        "copia tu archivo .db de backup sobre graficas_mulberry.db y reinicia la aplicacion.")

    pdf.problema(
        "La importacion de datos no funciona correctamente",
        "Verifica que el archivo CSV usa UTF-8 con BOM para acentos espanioles. "
        "En Excel: Guardar como > CSV UTF-8 (con BOM). "
        "Asegurate de que la columna 'nombre' (o equivalente) no tiene celdas vacias.")

    pdf.titulo_seccion("Contacto y soporte")
    pdf.vineta([
        "Web:       www.graficasmulberry.com",
        "Email:     info@graficasmulberry.com",
        "Telefono:  950 30 61 64  /  605 37 16 96",
        "Direccion: Calle Rio Minyo, 54 - 3F  |  04230 Huercal de Almeria  |  Almeria",
    ])

    # ════════════════════════════════════════════════════════════════════════
    # CONTRAPORTADA
    # ════════════════════════════════════════════════════════════════════════
    pdf.add_page()
    pdf.set_fill_color(*MULBERRY_BG)
    pdf.rect(0, 0, 210, 175, style="F")
    pdf.set_fill_color(*MULBERRY_DARK)
    pdf.rect(0, 175, 210, 122, style="F")
    pdf.set_fill_color(*MULBERRY)
    pdf.rect(0, 173, 210, 4, style="F")

    pdf.set_y(40)
    if os.path.exists(LOGO_PATH):
        pdf.image(LOGO_PATH, x=78, y=38, w=54)
        pdf.set_y(100)

    pdf.set_text_color(*DARK)
    pdf.set_font("Helvetica", "B", 18)
    pdf.cell(0, 10, "Sistema de Gestion Empresarial",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_font("Helvetica", "", 10)
    pdf.set_text_color(*GRIS)
    pdf.cell(0, 7,
             "Presupuestos  |  Facturas  |  Albaranes  |  Pedidos  |  Materiales  |  Nominas",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.cell(0, 7,
             "Estadisticas  |  Calendario  |  Asistente IA  |  Importar  |  Exportar",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    pdf.set_y(185)
    pdf.set_text_color(*BLANCO)
    pdf.set_font("Helvetica", "B", 16)
    pdf.cell(0, 9, "Graficas Mulberry",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_font("Helvetica", "", 9)
    pdf.set_text_color(*MULBERRY_LIGHT)
    pdf.cell(0, 6, "Serigrafia  |  DTF  |  Bordado  |  Vinilo  |  Sublimacion  |  Gran Formato",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(6)
    pdf.set_font("Helvetica", "", 8.5)
    pdf.cell(0, 5, "Huercal de Almeria  |  Almeria  |  Espana",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.cell(0, 5, "www.graficasmulberry.com  |  info@graficasmulberry.com  |  Tel. 950 30 61 64",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(5)
    pdf.set_font("Helvetica", "B", 9)
    pdf.set_text_color(*MULBERRY_LIGHT)
    pdf.cell(0, 5, "Manual de Usuario  |  Version 3.0  |  2025",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    # ── Guardar en la raiz del proyecto Y en el escritorio ──────────────────
    proyecto = os.path.dirname(os.path.abspath(__file__))
    out_proyecto = os.path.join(proyecto, "Manual_GraficasMulberry.pdf")
    pdf.output(out_proyecto)
    print(f"PDF generado: {out_proyecto}")

    try:
        import ctypes.wintypes, ctypes
        buf = ctypes.create_unicode_buffer(ctypes.wintypes.MAX_PATH)
        ctypes.windll.shell32.SHGetFolderPathW(0, 0, 0, 0, buf)
        desktop = buf.value
        carpeta = os.path.join(desktop, "graficas_mulberry")
        os.makedirs(carpeta, exist_ok=True)
        out_desk = os.path.join(carpeta, "Manual_GraficasMulberry.pdf")
        import shutil
        shutil.copy2(out_proyecto, out_desk)
        print(f"Copia en escritorio: {out_desk}")
    except Exception as e:
        print(f"(No se pudo copiar al escritorio: {e})")

    return out_proyecto


if __name__ == "__main__":
    generar()
