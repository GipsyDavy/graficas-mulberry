# -*- coding: utf-8 -*-
"""Generador del manual de instrucciones de Graficas Mulberry."""

from fpdf import FPDF
from fpdf.enums import XPos, YPos
import os

MULBERRY        = (204,   0,   0)   # rojo principal
MULBERRY_LIGHT  = (255, 153, 153)   # rojo claro
MULBERRY_BG     = (255, 235, 235)   # fondo rojo muy suave
DARK            = ( 17,  17,  17)   # negro
GRIS            = (102, 102, 102)
GRIS_CLARO      = (240, 240, 240)
BLANCO          = (255, 255, 255)
VERDE           = ( 39, 174,  96)
NARANJA         = (243, 156,  18)
ROJO            = (204,   0,   0)
AZUL            = ( 76, 155, 232)

LOGO_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)),
    "src", "main", "resources", "org", "gipsybuho", "img", "logo.jpg")


class ManualPDF(FPDF):

    def header(self):
        if self.page_no() == 1:
            return
        self.set_fill_color(*MULBERRY)
        self.rect(0, 0, 210, 12, style="F")
        self.set_font("Helvetica", "B", 9)
        self.set_text_color(*BLANCO)
        self.set_xy(10, 2)
        self.cell(100, 8, "Graficas Mulberry - Manual de la Aplicacion", align="L")
        self.set_xy(0, 2)
        self.cell(200, 8, f"Pagina {self.page_no()}", align="R")
        self.ln(10)

    def footer(self):
        if self.page_no() == 1:
            return
        self.set_y(-14)
        self.set_draw_color(*MULBERRY)
        self.set_line_width(0.5)
        self.line(10, self.get_y(), 200, self.get_y())
        self.set_font("Helvetica", "", 8)
        self.set_text_color(*GRIS)
        self.cell(0, 8,
            "www.graficasmulberry.com  |  info@graficasmulberry.com  |  Tel. 950 30 61 64",
            align="C")

    def titulo_seccion(self, texto, numero=""):
        self.ln(4)
        self.set_fill_color(*MULBERRY)
        self.set_text_color(*BLANCO)
        self.set_font("Helvetica", "B", 13)
        txt = f"  {numero}  {texto}" if numero else f"  {texto}"
        self.cell(0, 10, txt, new_x=XPos.LMARGIN, new_y=YPos.NEXT, fill=True)
        self.set_text_color(*DARK)
        self.ln(3)

    def titulo_sub(self, texto):
        self.set_fill_color(*MULBERRY_BG)
        self.set_text_color(*MULBERRY)
        self.set_font("Helvetica", "B", 11)
        self.cell(0, 8, f"  {texto}", new_x=XPos.LMARGIN, new_y=YPos.NEXT, fill=True)
        self.set_text_color(*DARK)
        self.ln(1)

    def parrafo(self, texto, indent=0):
        self.set_font("Helvetica", "", 10)
        self.set_text_color(*DARK)
        self.set_x(10 + indent)
        self.multi_cell(190 - indent, 5.5, texto,
                        new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(1)

    def vineta(self, items, color=None):
        self.set_font("Helvetica", "", 10)
        self.set_text_color(*(color or DARK))
        for item in items:
            self.set_x(14)
            self.cell(5, 5.5, "-")
            self.set_x(20)
            self.multi_cell(175, 5.5, item, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_text_color(*DARK)
        self.ln(1)

    def codigo(self, lineas):
        self.set_fill_color(30, 20, 28)
        self.set_text_color(200, 230, 200)
        self.set_font("Courier", "", 9)
        self.set_x(10)
        ancho = 190
        padding = 4
        alto_total = len(lineas) * 5.5 + padding * 2
        self.rect(10, self.get_y(), ancho, alto_total, style="F")
        self.set_xy(14, self.get_y() + padding)
        for linea in lineas:
            self.cell(ancho - 4, 5.5, linea, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            self.set_x(14)
        self.ln(4)
        self.set_text_color(*DARK)

    def tabla(self, cabeceras, filas, anchos):
        self.set_font("Helvetica", "B", 9)
        self.set_fill_color(*MULBERRY)
        self.set_text_color(*BLANCO)
        for i, cab in enumerate(cabeceras):
            self.cell(anchos[i], 8, f" {cab}", border=0, fill=True)
        self.ln()
        self.set_font("Helvetica", "", 9)
        par = False
        for fila in filas:
            bg = GRIS_CLARO if par else BLANCO
            self.set_fill_color(*bg)
            self.set_text_color(*DARK)
            y0 = self.get_y()
            for i, celda in enumerate(fila):
                self.set_xy(self.get_x(), y0)
                self.multi_cell(anchos[i], 7, f" {celda}",
                                border=0, fill=True,
                                new_x=XPos.RIGHT, new_y=YPos.TOP)
            self.ln(7)
            par = not par
        self.ln(3)

    def caja_info(self, titulo, contenido, color=None):
        col = color or AZUL
        self.set_fill_color(*col)
        self.set_text_color(*BLANCO)
        self.set_font("Helvetica", "B", 10)
        self.cell(0, 7, f"  {titulo}", fill=True,
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_fill_color(*MULBERRY_BG)
        self.set_text_color(*DARK)
        self.set_font("Helvetica", "", 9)
        self.multi_cell(0, 5.5, f"  {contenido}", fill=True,
                        new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(3)

    def paso(self, numero, texto, detalle=""):
        self.set_fill_color(*MULBERRY)
        self.set_text_color(*BLANCO)
        self.set_font("Helvetica", "B", 10)
        self.cell(8, 7, str(numero), align="C", fill=True)
        self.set_text_color(*DARK)
        self.set_font("Helvetica", "B", 10)
        self.cell(0, 7, f"  {texto}",
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        if detalle:
            self.set_font("Helvetica", "", 9)
            self.set_x(18)
            self.set_text_color(*GRIS)
            self.multi_cell(175, 5, detalle,
                            new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            self.set_text_color(*DARK)
        self.ln(2)

    def problema(self, titulo, solucion):
        self.set_font("Helvetica", "B", 10)
        self.set_text_color(*ROJO)
        self.set_x(12)
        self.cell(5, 7, ">")
        self.cell(0, 7, titulo, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_font("Helvetica", "", 9)
        self.set_text_color(*DARK)
        self.set_x(20)
        self.multi_cell(175, 5, solucion,
                        new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(2)


# ===========================================================================
def generar():
    pdf = ManualPDF(orientation="P", unit="mm", format="A4")
    pdf.set_auto_page_break(auto=True, margin=18)
    pdf.set_margins(10, 16, 10)

    # ── PORTADA ──────────────────────────────────────────────────────────────
    pdf.add_page()
    pdf.set_fill_color(*MULBERRY)
    pdf.rect(0, 0, 210, 297, style="F")

    if os.path.exists(LOGO_PATH):
        pdf.image(LOGO_PATH, x=75, y=40, w=60)
        pdf.set_y(112)
    else:
        pdf.set_y(65)

    pdf.set_text_color(*BLANCO)
    pdf.set_font("Helvetica", "B", 28)
    pdf.cell(0, 14, "GRAFICAS MULBERRY",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_font("Helvetica", "", 15)
    pdf.set_text_color(*MULBERRY_LIGHT)
    pdf.cell(0, 8, "Sistema de Gestion Empresarial",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(8)

    # Caja de modulos
    caja_y = pdf.get_y()
    pdf.set_xy(30, caja_y)
    pdf.set_fill_color(*DARK)
    pdf.rect(30, caja_y, 150, 72, style="F")
    pdf.set_xy(40, caja_y + 8)

    modulos_portada = [
        ("[Pres]", "Presupuestos y Facturas"),
        ("[Tar] ", "Tarifas de impresion"),
        ("[Mat] ", "Control de materiales"),
        ("[Nom] ", "Nominas (legislacion espanola)"),
        "[ IA ] ", "Asistente IA local (Ollama)",
    ]
    filas_portada = [
        ("[Pres]", "Presupuestos y Facturas"),
        ("[Tar] ", "Tarifas de impresion"),
        ("[Mat] ", "Control de materiales"),
        ("[Nom] ", "Nominas (legislacion espanola)"),
        ("[ IA ]", "Asistente IA local (Ollama)"),
    ]
    for marcador, texto in filas_portada:
        pdf.set_font("Courier", "B", 10)
        pdf.set_text_color(*MULBERRY_LIGHT)
        pdf.set_x(40)
        pdf.cell(14, 9, marcador)
        pdf.set_font("Helvetica", "", 11)
        pdf.set_text_color(*BLANCO)
        pdf.cell(0, 9, texto, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        pdf.set_x(40)

    pdf.ln(14)
    pdf.set_font("Helvetica", "", 9)
    pdf.set_text_color(*MULBERRY_LIGHT)
    pdf.cell(0, 6, "Huercal de Almeria  |  Almeria  |  Espana",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.cell(0, 6, "www.graficasmulberry.com  |  info@graficasmulberry.com",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    pdf.set_y(272)
    pdf.set_font("Helvetica", "", 9)
    pdf.set_text_color(*MULBERRY_LIGHT)
    pdf.cell(0, 6, "Manual de usuario  |  Version 1.0  |  2025", align="C")

    # ── INDICE ───────────────────────────────────────────────────────────────
    pdf.add_page()
    pdf.set_text_color(*DARK)
    pdf.set_fill_color(*MULBERRY)
    pdf.set_text_color(*BLANCO)
    pdf.set_font("Helvetica", "B", 18)
    pdf.cell(0, 12, "  INDICE DE CONTENIDOS",
             fill=True, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(6)

    secciones = [
        ("1.", "Requisitos del sistema",                   True),
        ("2.", "Instalacion y configuracion",               True),
        ("3.", "Primera ejecucion",                         True),
        ("4.", "Modulos de la aplicacion",                  True),
        ("  4.1", "Panel principal (Dashboard)",            False),
        ("  4.2", "Clientes",                               False),
        ("  4.3", "Presupuestos",                           False),
        ("  4.4", "Facturas",                               False),
        ("  4.5", "Tarifas",                                False),
        ("  4.6", "Control de materiales",                  False),
        ("  4.7", "Nominas",                                False),
        ("5.", "Asistente IA local (Ollama)",                True),
        ("6.", "Exportacion a PDF",                         True),
        ("7.", "Base de datos",                             True),
        ("8.", "Soporte tecnico",                           True),
    ]
    for i, (num, tit, principal) in enumerate(secciones):
        pdf.set_font("Helvetica", "B" if principal else "", 10 if principal else 9)
        bg = MULBERRY_BG if i % 2 == 0 else BLANCO
        pdf.set_fill_color(*bg)
        indent = 0 if principal else 8
        pdf.set_x(12 + indent)
        pdf.set_text_color(*(MULBERRY if principal else DARK))
        pdf.cell(185 - indent, 7, f"  {num}   {tit}",
                 fill=True, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(8)

    pdf.caja_info(
        "[ i ]  Sobre este manual",
        "Este manual cubre el uso completo del sistema de gestion de Graficas Mulberry.\n"
        "La aplicacion esta desarrollada en Java 21 con JavaFX y almacena todos los datos\n"
        "localmente en una base de datos SQLite. No requiere conexion a Internet para funcionar,\n"
        "salvo el modulo de IA que utiliza Ollama de forma completamente local.",
        color=AZUL)

    # ── 1. REQUISITOS ────────────────────────────────────────────────────────
    pdf.add_page()
    pdf.titulo_seccion("REQUISITOS DEL SISTEMA", "1")

    pdf.titulo_sub("Requisitos minimos de hardware")
    pdf.vineta([
        "Procesador: Intel/AMD de 64 bits (x86_64), 2 GHz o superior",
        "Memoria RAM: 4 GB (8 GB recomendados si se usa la IA)",
        "Espacio en disco: 500 MB para la aplicacion + espacio para datos",
        "Resolucion de pantalla: 1280x720 minimo (1920x1080 recomendado)",
    ])

    pdf.titulo_sub("Requisitos de software")
    pdf.vineta([
        "Sistema operativo: Windows 10 / Windows 11 (64 bits)",
        "Java Development Kit (JDK) 21 o superior  -- ya instalado en tu equipo",
        "Apache Maven 3.8+  -- incluido en IntelliJ IDEA o instalable por separado",
        "IntelliJ IDEA 2023+ (recomendado) o cualquier IDE compatible con Maven",
    ])

    pdf.titulo_sub("Requisitos opcionales -- para la IA")
    pdf.vineta([
        "Ollama (software gratuito) -- descargable en https://ollama.com",
        "Modelo de IA: llama3.2 (2 GB), phi4 (8 GB) u otro modelo Ollama",
        "RAM adicional: 4-8 GB segun el modelo elegido",
        "GPU NVIDIA opcional (acelera la inferencia notablemente)",
    ])

    pdf.caja_info(
        "[OK]  Verificacion rapida",
        "Abre el terminal de Windows (cmd) y ejecuta:  java -version\n"
        "Deberas ver algo como:  java version \"21.x.x\" o superior.\n"
        "Si no aparece, descarga el JDK desde https://adoptium.net",
        color=VERDE)

    # ── 2. INSTALACION ───────────────────────────────────────────────────────
    pdf.titulo_seccion("INSTALACION Y CONFIGURACION", "2")

    pdf.titulo_sub("Paso a paso -- Primera instalacion")
    pdf.paso(1, "Abrir el proyecto en IntelliJ IDEA",
             "Inicia IntelliJ IDEA  >  File  >  Open  >  selecciona la carpeta 'Graficas Mulberry'")
    pdf.paso(2, "Importar el proyecto Maven",
             "IntelliJ detecta automaticamente el pom.xml. Haz clic en 'Load Maven Project' "
             "en la notificacion que aparece en la esquina superior derecha.")
    pdf.paso(3, "Descargar dependencias",
             "Espera a que Maven descargue todas las librerias. Progreso visible en la barra "
             "inferior. Requiere conexion a Internet solo en este primer paso.")
    pdf.paso(4, "Configurar el JDK del proyecto",
             "File  >  Project Structure  >  SDK  >  selecciona JDK 21 o superior")
    pdf.paso(5, "Compilar el proyecto",
             "Build  >  Build Project (Ctrl+F9). Debe completarse sin errores.")

    pdf.titulo_sub("Dependencias incluidas automaticamente")
    pdf.tabla(
        ["Libreria", "Funcion", "Descripcion"],
        [
            ["JavaFX 21",     "Interfaz grafica",     "Controles modernos, CSS, efectos visuales"],
            ["SQLite JDBC",   "Base de datos local",  "Sin servidor, archivo .db en carpeta del proyecto"],
            ["OpenPDF",       "Generacion de PDF",    "Presupuestos, facturas y nominas en PDF"],
            ["Jackson",       "API de Ollama",        "Comunicacion JSON con el servidor de IA local"],
            ["Ikonli",        "Iconos",               "Iconos FontAwesome en la interfaz"],
        ],
        [40, 50, 97]
    )

    # ── 3. PRIMERA EJECUCION ─────────────────────────────────────────────────
    pdf.add_page()
    pdf.titulo_seccion("PRIMERA EJECUCION", "3")

    pdf.titulo_sub("Ejecutar desde IntelliJ IDEA")
    pdf.paso(1, "Localizar la clase principal",
             "En el panel de proyecto (izquierda): src  >  main  >  java  >  org.gipsybuho  >  Main.java")
    pdf.paso(2, "Ejecutar la aplicacion",
             "Clic derecho sobre Main.java  >  Run 'Main'  (o pulse Shift+F10)")
    pdf.paso(3, "Primera carga",
             "La primera vez se crea la base de datos SQLite automaticamente en la carpeta "
             "del proyecto (graficas_mulberry.db) con datos de ejemplo precargados.")

    pdf.titulo_sub("Ejecutar desde el terminal (Maven)")
    pdf.codigo([
        "# Navegar a la carpeta del proyecto",
        "cd \"C:\\Users\\[usuario]\\MAVEN\\Graficas Mulberry\"",
        "",
        "# Ejecutar la aplicacion",
        "mvn javafx:run",
    ])

    pdf.titulo_sub("Datos precargados al inicio")
    pdf.vineta([
        "10 tarifas de ejemplo: serigrafia 1-4 colores, DTF, bordado, vinilo, sublimacion, gran formato",
        "10 materiales con stock inicial: tintas, pantallas, emulsion, camisetas, vinilos, hilo...",
        "Configuracion de empresa con los datos de Graficas Mulberry",
        "Numeracion automatica: presupuestos (PRE-2025-0001) y facturas (FAC-2025-0001)",
    ])

    pdf.caja_info(
        "[Dir]  Ubicacion de la base de datos",
        "El archivo graficas_mulberry.db se crea en la carpeta raiz del proyecto.\n"
        "Para hacer una copia de seguridad, simplemente copia este archivo.\n"
        "Ruta: C:\\Users\\[usuario]\\MAVEN\\Graficas Mulberry\\graficas_mulberry.db",
        color=NARANJA)

    # ── 4. MODULOS ───────────────────────────────────────────────────────────
    pdf.add_page()
    pdf.titulo_seccion("MODULOS DE LA APLICACION", "4")

    pdf.parrafo(
        "La aplicacion se organiza en 8 modulos accesibles desde la barra lateral izquierda "
        "(color Mulberry oscuro). Cada modulo es independiente y los datos se guardan "
        "automaticamente en la base de datos local.")

    pdf.titulo_sub("4.1  Panel Principal (Dashboard)")
    pdf.parrafo(
        "La pantalla de inicio muestra un resumen en tiempo real del estado del negocio "
        "mediante cinco tarjetas de informacion:")
    pdf.vineta([
        "Clientes registrados  -- numero total de clientes en la base de datos",
        "Presupuestos pendientes  -- presupuestos en estado 'enviado' sin respuesta",
        "Facturas pendientes  -- facturas emitidas aun sin cobrar",
        "Materiales bajo stock  -- articulos que han alcanzado el stock minimo de alerta",
        "Facturado este anio  -- suma total de facturas no anuladas del anio en curso",
    ])

    pdf.titulo_sub("4.2  Clientes")
    pdf.vineta([
        "Aniadir, editar y eliminar clientes (empresas y particulares)",
        "Campos: nombre, NIF/CIF, direccion, ciudad, CP, telefono, email, notas",
        "Busqueda instantanea por nombre, NIF o email",
        "Los clientes se vinculan automaticamente a presupuestos y facturas",
    ])

    pdf.titulo_sub("4.3  Presupuestos")
    pdf.vineta([
        "Crear presupuestos con numero automatico (PRE-ANIO-XXXX)",
        "Aniadir lineas de producto con tecnica, cantidad, precio y descuento",
        "Seleccionar tarifas predefinidas directamente desde el catalogo",
        "Calculo automatico de base imponible, IVA (21%) y total",
        "Estados: borrador  >  enviado  >  aceptado / rechazado  >  facturado",
        "Exportar a PDF con el logo y datos de la empresa",
        "Convertir un presupuesto aceptado en factura con un solo clic",
    ])

    pdf.add_page()
    pdf.titulo_sub("4.4  Facturas")
    pdf.vineta([
        "Se generan automaticamente desde presupuestos (recomendado)",
        "Tambien se pueden crear manualmente",
        "Numero automatico con prefijo FAC-ANIO-XXXX",
        "Gestion de estado: pendiente  >  pagada / anulada",
        "Exportar factura a PDF cumpliendo los requisitos legales espanioles:",
        "   - Numero de factura, fecha de emision y vencimiento",
        "   - Datos fiscales completos del emisor y receptor",
        "   - Descripcion detallada, base imponible, tipo IVA y cuota de IVA",
        "Seguimiento del total facturado por anio en el panel principal",
    ])

    pdf.titulo_sub("4.5  Tarifas")
    pdf.parrafo("Gestiona el catalogo de precios por tecnica de impresion. Incluye por defecto:")
    pdf.tabla(
        ["Tecnica",             "Precio/ud.",  "Setup  |  Minimo"],
        [
            ["Serigrafia 1 tinta",    "2,50 EUR",  "35 EUR  |  12 uds"],
            ["Serigrafia 2 tintas",   "3,20 EUR",  "60 EUR  |  12 uds"],
            ["Serigrafia 4 colores",  "4,80 EUR",  "100 EUR  |  24 uds"],
            ["DTF estandar",          "3,50 EUR",  "Sin setup  |  Desde 1 ud"],
            ["Bordado basico",        "4,00 EUR",  "25 EUR  |  6 uds"],
            ["Bordado premium",       "7,00 EUR",  "40 EUR  |  6 uds"],
            ["Vinilo textil",         "2,80 EUR",  "Sin setup  |  Desde 1 ud"],
            ["Sublimacion",           "3,00 EUR",  "Sin setup  |  Desde 1 ud"],
            ["Lona PVC 510g",         "18,00 EUR/m2", "Gran formato"],
            ["Vinilo adhesivo",       "22,00 EUR/m2", "Exterior"],
        ],
        [55, 35, 97]
    )

    pdf.add_page()
    pdf.titulo_sub("4.6  Control de Materiales")
    pdf.vineta([
        "Registro de todos los materiales del taller: tintas, pantallas, sustratos, vinilos, hilo...",
        "Stock en tiempo real con indicador visual: verde (correcto) / rojo (bajo minimo)",
        "Filtro rapido 'Solo materiales con stock bajo' para gestionar reposicion",
        "Movimientos de stock: entradas (compras) y salidas (consumo)",
        "Historial de movimientos por material",
        "Alertas automaticas cuando el stock llega al minimo configurado",
        "Precio por unidad para calcular el coste de produccion",
    ])

    pdf.caja_info(
        "[!]  Consejo de uso",
        "Configura los stocks minimos con cuidado. Si tienes un pedido habitual de 50 camisetas,\n"
        "pon el stock minimo en 50 uds para que la alerta aparezca cuando debas reponer.",
        color=MULBERRY)

    pdf.titulo_sub("4.7  Nominas")
    pdf.parrafo(
        "Modulo de gestion de nominas adaptado a la legislacion laboral espaniola "
        "con cotizaciones de Seguridad Social vigentes en 2024:")
    pdf.vineta([
        "Alta de empleados con NIF, categoria, salario base e IRPF individual",
        "Calculo automatico de: salario bruto, IRPF, SS trabajador y SS empresa",
        "Soporte para: horas extra normales y festivas, complementos salariales",
        "Percepciones no salariales (dietas, ayudas)  -- no cotizan a SS",
        "Generacion de nominas en masa para todos los empleados de un mes con un clic",
        "Exportacion del recibo de nomina a PDF con todos los conceptos desglosados",
    ])

    pdf.titulo_sub("Porcentajes SS 2024 (Espania)")
    pdf.tabla(
        ["Concepto",              "Trabajador", "Empresa"],
        [
            ["Contingencias comunes", "4,70%",  "23,60%"],
            ["Desempleo",             "1,55%",  "5,50%"],
            ["Formacion profesional", "0,10%",  "0,60%"],
            ["FOGASA",                "--",     "0,20%"],
            ["TOTAL S.S.",            "6,35%",  "29,90%"],
        ],
        [90, 45, 52]
    )

    # ── 5. IA ────────────────────────────────────────────────────────────────
    pdf.add_page()
    pdf.titulo_seccion("ASISTENTE IA LOCAL (OLLAMA)", "5")

    pdf.parrafo(
        "La aplicacion incluye un asistente de inteligencia artificial que funciona "
        "integralments en tu propio ordenador. No envia ningun dato a Internet. "
        "Utiliza Ollama, un motor de IA gratuito y de codigo abierto.")

    pdf.titulo_sub("Instalar Ollama")
    pdf.paso(1, "Descargar Ollama",
             "Visita https://ollama.com y descarga el instalador para Windows.")
    pdf.paso(2, "Instalar",
             "Ejecuta el instalador descargado. Ollama arranca automaticamente como servicio "
             "en segundo plano al iniciar Windows.")
    pdf.paso(3, "Descargar un modelo de IA",
             "Abre el terminal de Windows (cmd) y ejecuta uno de estos comandos:")
    pdf.codigo([
        "# Modelo recomendado (ligero, 2 GB, muy capaz):",
        "ollama pull llama3.2",
        "",
        "# Alternativa mas potente (8 GB RAM necesaria):",
        "ollama pull phi4",
        "",
        "# Ver modelos disponibles en tu equipo:",
        "ollama list",
    ])
    pdf.paso(4, "Abrir el modulo IA en la aplicacion",
             "Haz clic en 'Asistente IA' en la barra lateral izquierda. "
             "El indicador debe mostrar '[ON] Ollama conectado'. "
             "Selecciona el modelo del desplegable y empieza a chatear.")

    pdf.titulo_sub("Casos de uso del Asistente IA")
    pdf.vineta([
        "Calcular el precio de un pedido: 'Tengo un pedido de 100 camisetas a 4 colores, cuanto cobro?'",
        "Redactar textos para presupuestos: 'Escribe una descripcion profesional para bordado corporativo'",
        "Consultar sobre materiales: 'Que diferencia hay entre tinta plastisol y acuarela?'",
        "Asesoramiento de nominas: 'Explica que es el FOGASA y cuanto aporta la empresa'",
        "Estrategia comercial: 'Sugiere como aumentar el margen en pedidos pequenios de serigrafia'",
        "Cualquier duda sobre el negocio de artes graficas e impresion",
    ])

    pdf.caja_info(
        "[*]  Privacidad total garantizada",
        "Toda la inteligencia artificial funciona en tu propio ordenador.\n"
        "Ninguna pregunta ni respuesta sale de tu red local.\n"
        "Los datos de clientes, presupuestos y nominas nunca se comparten.",
        color=VERDE)

    # ── 6. PDF ───────────────────────────────────────────────────────────────
    pdf.titulo_seccion("EXPORTACION A PDF", "6")

    pdf.parrafo(
        "La aplicacion genera documentos PDF profesionales con el logo y datos corporativos "
        "de Graficas Mulberry. Los archivos se guardan automaticamente en carpetas organizadas:")

    pdf.codigo([
        "Presupuestos:  C:\\Users\\[usuario]\\Documentos\\Mulberry\\Presupuestos\\",
        "Facturas:      C:\\Users\\[usuario]\\Documentos\\Mulberry\\Facturas\\",
        "Nominas:       C:\\Users\\[usuario]\\Documentos\\Mulberry\\Nominas\\",
    ])

    pdf.parrafo(
        "Tras generarse, el PDF se abre automaticamente con el visor de PDF predeterminado de Windows.")

    pdf.caja_info(
        "[i]  Personalizar datos de la empresa en el PDF",
        "Los datos que aparecen en el PDF (nombre, NIF, direccion, telefono, email, web)\n"
        "se almacenan en la tabla 'config' de la base de datos y se pueden editar en cualquier momento.",
        color=AZUL)

    # ── 7. BASE DE DATOS ─────────────────────────────────────────────────────
    pdf.add_page()
    pdf.titulo_seccion("BASE DE DATOS", "7")

    pdf.parrafo(
        "La aplicacion utiliza SQLite, una base de datos ligera que se almacena como un unico "
        "archivo en la carpeta del proyecto. No requiere instalacion de ninguna herramienta adicional.")

    pdf.titulo_sub("Ubicacion del archivo")
    pdf.codigo(["C:\\Users\\[usuario]\\MAVEN\\Graficas Mulberry\\graficas_mulberry.db"])

    pdf.titulo_sub("Copia de seguridad (recomendada semanalmente)")
    pdf.vineta([
        "Cierra la aplicacion antes de copiar el archivo",
        "Copia graficas_mulberry.db a un disco externo, nube (OneDrive, Google Drive) u otro PC",
        "Para restaurar, simplemente pega el archivo en la misma ubicacion",
        "El nombre del archivo incluye todos los datos: clientes, presupuestos, facturas y nominas",
    ])

    pdf.titulo_sub("Tablas incluidas en la base de datos")
    pdf.tabla(
        ["Tabla",                "Contenido",          "Descripcion"],
        [
            ["clientes",              "Datos de clientes",      "Empresa o particular, NIF, contacto"],
            ["empleados",             "Plantilla",               "Datos laborales e IRPF individual"],
            ["tarifas",               "Catalogo de precios",    "Por tecnica: serigrafia, DTF, bordado..."],
            ["materiales",            "Stock del taller",       "Con alertas de stock minimo"],
            ["presupuestos",          "Presupuestos",           "Cabecera + totales + estado"],
            ["lineas_presupuesto",    "Lineas de presupuesto",  "Cada articulo del presupuesto"],
            ["facturas",              "Facturas emitidas",      "Vinculadas a presupuestos"],
            ["lineas_factura",        "Lineas de factura",      "Desglose de la factura"],
            ["nominas",               "Recibos de nomina",      "Calculo completo SS + IRPF"],
            ["config",                "Configuracion",          "Datos empresa, prefijos, IVA..."],
        ],
        [48, 48, 91]
    )

    # ── 8. SOPORTE ───────────────────────────────────────────────────────────
    pdf.titulo_seccion("SOPORTE TECNICO", "8")

    pdf.titulo_sub("Datos de contacto")
    pdf.vineta([
        "Web:       www.graficasmulberry.com",
        "Email:     info@graficasmulberry.com",
        "Telefono:  950 30 61 64  /  605 37 16 96",
        "Direccion: Calle Rio Minyo, 54 - 3F  |  04230 Huercal de Almeria  |  Almeria",
    ])

    pdf.titulo_sub("Problemas frecuentes y soluciones")

    pdf.problema(
        "La aplicacion no arranca",
        "Verifica que Java 21+ esta instalado. Abre cmd y ejecuta: java -version")
    pdf.problema(
        "Error al compilar en IntelliJ",
        "Haz clic en el panel Maven (derecha) > boton 'Reload All Maven Projects'. "
        "Espera que se descarguen las dependencias y vuelve a compilar con Ctrl+F9.")
    pdf.problema(
        "La IA no responde",
        "Comprueba que Ollama esta en ejecucion (icono en la barra de tareas de Windows). "
        "Ejecuta en cmd: ollama list -- debe mostrar al menos un modelo instalado.")
    pdf.problema(
        "No se genera el PDF",
        "Verifica que existe la carpeta Documentos en tu usuario de Windows. "
        "La aplicacion crea la subcarpeta Mulberry automaticamente al generar el primer PDF.")
    pdf.problema(
        "Los datos desaparecen al reinstalar",
        "El archivo graficas_mulberry.db contiene absolutamente todos los datos. "
        "Copia este archivo antes de reinstalar o mover la aplicacion.")
    pdf.problema(
        "Error de charset / caracteres extranieros en el PDF",
        "Asegurate de que el archivo se ejecuta con encoding UTF-8. "
        "En IntelliJ: File > Settings > Editor > File Encodings > UTF-8.")

    # ── CONTRAPORTADA ────────────────────────────────────────────────────────
    pdf.add_page()
    # Franja superior
    pdf.set_fill_color(*MULBERRY_BG)
    pdf.rect(0, 0, 210, 180, style="F")
    # Franja inferior
    pdf.set_fill_color(*MULBERRY)
    pdf.rect(0, 180, 210, 120, style="F")

    # Contenido central
    pdf.set_y(50)
    if os.path.exists(LOGO_PATH):
        pdf.image(LOGO_PATH, x=80, y=50, w=50)
        pdf.set_y(108)

    pdf.set_text_color(*DARK)
    pdf.set_font("Helvetica", "B", 20)
    pdf.cell(0, 10, "Sistema de Gestion Empresarial",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_font("Helvetica", "", 11)
    pdf.set_text_color(*GRIS)
    pdf.cell(0, 7,
             "Presupuestos  |  Facturas  |  Tarifas  |  Materiales  |  Nominas  |  IA Local",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    pdf.set_y(192)
    pdf.set_text_color(*BLANCO)
    pdf.set_font("Helvetica", "B", 14)
    pdf.cell(0, 9, "Graficas Mulberry",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.set_font("Helvetica", "", 10)
    pdf.set_text_color(*MULBERRY_LIGHT)
    pdf.cell(0, 6, "Serigrafia | DTF | Bordado | Vinilo | Sublimacion | Gran Formato",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(4)
    pdf.set_font("Helvetica", "", 9)
    pdf.cell(0, 5, "Huercal de Almeria  |  Almeria  |  Espania",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.cell(0, 5, "www.graficasmulberry.com  |  info@graficasmulberry.com  |  Tel. 950 30 61 64",
             align="C", new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    # Guardar en la carpeta del escritorio (graficas_mulberry)
    import ctypes.wintypes, ctypes
    CSIDL_DESKTOPDIRECTORY = 0
    buf = ctypes.create_unicode_buffer(ctypes.wintypes.MAX_PATH)
    ctypes.windll.shell32.SHGetFolderPathW(0, CSIDL_DESKTOPDIRECTORY, 0, 0, buf)
    desktop = buf.value
    carpeta = os.path.join(desktop, "graficas_mulberry")
    os.makedirs(carpeta, exist_ok=True)
    out = os.path.join(carpeta, "Manual_GraficasMulberry.pdf")
    pdf.output(out)
    print(f"PDF generado correctamente: {out}")
    return out


if __name__ == "__main__":
    generar()
