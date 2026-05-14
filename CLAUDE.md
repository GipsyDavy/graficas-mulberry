# CLAUDE.md - Reglas del Proyecto

Reglas obligatorias inspiradas en Andrej Karpathy, adaptadas a este proyecto Java de escritorio.  
Estas reglas deben respetar siempre las instrucciones explícitas del usuario, las políticas de seguridad, los permisos del entorno y las capacidades reales de las herramientas disponibles.

## CONTEXTO DEL PROYECTO

- Aplicación de escritorio Java 21.
- Build con Maven.
- Interfaz JavaFX 21.
- Base de datos local SQLite mediante JDBC.
- Autenticación con BCrypt.
- Integración local con Ollama cuando esté disponible.
- Tests con JUnit 5.
- Empaquetado Windows mediante Maven, jpackage/Inno Setup y scripts PowerShell existentes.
- Comandos habituales:
  - `mvn test`
  - `mvn package`
  - `mvn javafx:run`
  - `mvn package -Ppackage-windows`

## ESTRUCTURA DEL PROYECTO

- `src/main/java/org/gipsybuho/dao`: acceso a datos y consultas SQLite/JDBC.
- `src/main/java/org/gipsybuho/model`: modelos de dominio.
- `src/main/java/org/gipsybuho/service`: lógica de servicios e integraciones.
- `src/main/java/org/gipsybuho/ui`: pantallas, controladores y componentes JavaFX.
- `src/main/resources/org/gipsybuho`: estilos, FXML, temas e imágenes.
- `src/test/java`: tests JUnit 5.
- `installer/`, `output/` y `target/`: artefactos de build, empaquetado o salida generada.

## 1. Piensa antes de programar (Modo Estricto)
- Declara explícitamente todas tus suposiciones.
- Si una ambigüedad bloquea la tarea o puede provocar cambios incorrectos, detente y pregunta.
- Si la ambigüedad es menor, asume la opción más simple y segura, y declara la suposición.
- Propón siempre primero la solución más simple y directa (YAGNI).
- Expón claramente los trade-offs cuando sea relevante.

## 2. Simplicidad y Java (Filosofía Karpathy)
- Código mínimo viable. Evita over-engineering y abstracciones prematuras.
- No introducir Lombok ni nuevas dependencias salvo petición expresa o justificación técnica clara.
- Prefiere código legible por sí mismo (buenos nombres > comentarios excesivos).
- Manejo de errores simple, claro y coherente con el código existente.
- Usar excepciones verificadas o no verificadas según el contexto, sin ocultar errores relevantes.

## 3. Cambios Quirúrgicos
- Modifica **solo** lo estrictamente necesario.
- Validación de Contexto: Antes de modificar un archivo, lee sus dependencias directas para asegurar compatibilidad.
- Respeta al 100% el estilo, convenciones y estructura del proyecto existente.
- No refactorices código que no esté roto.
- Limpia solamente el código que tú mismo estás modificando.
- No modificar `target/`, `output/`, instaladores `.exe` ni artefactos empaquetados salvo que la tarea sea explícitamente de build, empaquetado o instalador.

## 4. Ejecución orientada a objetivos
- Convierte cada tarea en criterios de éxito claros y verificables.
- Criterio de Calidad: escribir o actualizar un test con JUnit 5 que valide el cambio antes de finalizar la tarea, cuando el cambio lo requiera.
- Avanza de forma iterativa, verificando cada paso antes de continuar.

## ADAPTACIÓN Y PREFERENCIAS

- Analiza automáticamente la versión de Java, sistema de build y frameworks del proyecto.
- **Base de Datos**: SQLite es la base de datos principal del proyecto. No introducir MySQL, MongoDB u otra base de datos salvo petición expresa.
- Adáptate completamente al estilo existente.
- Mantener la arquitectura actual de aplicación JavaFX de escritorio.
- No introducir Spring Boot, servidores HTTP ni arquitectura web salvo petición expresa.
- Investigación y segunda opinión: usar Gemini, Claude Code o Codex según la sección Multi-IA cuando aporte valor y el coste de cuota lo justifique.

## DOCUMENTACIÓN

- JavaDoc en clases/interfaces públicas y métodos con lógica compleja.
- No añadir Swagger/OpenAPI salvo que el proyecto incorpore una API HTTP real.
- Preferir código auto-documentado.

## GIT Y COMMITS (Convenciones Estrictas)

### Reglas de Commits:
- Cada commit debe ser **atómico** (un solo cambio lógico y completo).
- Mensajes claros, en imperativo y en español.
- Formato recomendado:
  ```bash
  tipo: descripción corta y clara
  
  Descripción detallada cuando sea necesario. Explicar motivo e impacto.
  ```
- Tipos permitidos: feat, fix, refactor, docs, style, test, chore, perf, security
- Preferir commits pequeños y frecuentes.
- Evitar commits grandes o con cambios mezclados.
- Evitar commits temporales (“WIP”, “temp”, etc.) en la rama principal.

### Branching y Pull Requests:
- Usar prefijos claros: feat/, fix/, refactor/, docs/, chore/, etc.
- Mantener las ramas actualizadas con la principal.
- Título claro y descriptivo.
- Descripción que explique qué cambia, por qué y cómo se probó.
- Incluir evidencia (capturas o tests) cuando corresponda.

## SEGURIDAD Y CALIDAD (SONAR)
- Aplicar principios OWASP cuando haya entradas externas, autenticación, permisos, ficheros, red o datos sensibles. 
- Validar y sanitizar todas las entradas de usuario y externas. 
- Realizar revisión de seguridad explícita en funcionalidades críticas (auth, pagos, datos sensibles).
- Priorizar la resolución de vulnerabilidades y Code Smells detectados por SonarQube.
- Principio de mínimo privilegio.
- Proteger contra inyecciones, Broken Access Control, SSRF, XSS, etc.

## HERRAMIENTAS DISPONIBLES

Usar herramientas locales, CLI, IDE o MCP cuando estén disponibles y aporten valor real:

Esta lista es orientativa y no exhaustiva:

- filesystem: Leer, escribir y explorar archivos locales.
- git: Operaciones git locales.
- github: Interactuar con repositorios, PRs e issues de GitHub.
- sequential-thinking: Razonamiento paso a paso estructurado.
- memory: Memoria persistente entre sesiones.
- playwright: Automatización web y pruebas.
- e2b / fetch: Ejecución y peticiones externas.

### REGLA DE SEGURIDAD Y ACCESO:
- Verificar disponibilidad de herramientas, claves API y permisos de lectura/escritura antes de iniciar tareas que los requieran.
- No asumir que todas las herramientas listadas están instaladas o autenticadas.

### Uso Proactivo:
- Usaré estas herramientas de forma proactiva cuando ayuden a resolver mejor la tarea, especialmente para análisis de código, investigación o validación.

## AGENTES IA - MULTI-IA OBLIGATORIO

### Principio
- Claude Code, Codex y Gemini forman un sistema Multi-IA obligatorio para este proyecto.
- La integración debe funcionar desde cualquier punto de entrada: chat de Claude Code, chat de Codex, chat de Gemini, IDE, CLI local, MCP, terminal o transferencia manual de contexto.
- Cualquier agente puede solicitar apoyo, revisión, planificación, análisis, validación o ejecución a cualquiera de los otros cuando aporte valor técnico real.
- La colaboración entre agentes debe iniciarse sin pedir intervención humana, salvo que haga falta autorización de seguridad, permisos del sistema, acceso a secretos, acciones destructivas o una decisión funcional ambigua.
- Si una IA no puede invocar directamente a otra por limitaciones del entorno, debe usar el mecanismo equivalente disponible: CLI local, integración del IDE, MCP, terminal, bloque de instrucciones autocontenido o transferencia manual de contexto.
- No se debe invocar otro agente para tareas triviales, repetitivas o cuando la verificación local sea suficiente.
- Si un agente no está disponible por cuota, autenticación, red, permisos o limitaciones del IDE, se continuará con los agentes disponibles y se informará al usuario.

### Roles
- Claude Code: agente principal preferente para implementación, revisión final, calidad, seguridad, testing y cumplimiento de estas reglas.
- Codex: agente principal alternativo o apoyo para edición local, ejecución de comandos, parches quirúrgicos, inspección de archivos y validación inmediata.
- Gemini / Gemini Code Assist: apoyo para contexto amplio, arquitectura, planificación, investigación técnica, análisis de alternativas y segunda opinión.

### Suplencia por cuota o disponibilidad
- Si Claude Code no está operativo por cuota, autenticación, red, permisos o bloqueo temporal, Codex puede asumir provisionalmente el rol principal.
- Si Codex no está operativo, Claude Code o Gemini deben continuar con el mecanismo disponible y dejar constancia de la limitación.
- Si Gemini no está operativo, Claude Code o Codex deben continuar sin bloquear la tarea salvo que el usuario haya pedido expresamente su intervención.
- Cuando un agente vuelva a estar disponible, puede reincorporarse para revisión, validación o continuidad si aporta valor y el coste de cuota lo justifica.

### Capacidades de edición
- Claude Code, Codex y Gemini pueden leer, analizar y editar archivos del proyecto cuando estén autorizados por el contexto de la tarea y sus herramientas lo permitan.
- Las ediciones deben seguir siempre las reglas de cambios quirúrgicos, mínima modificación necesaria, respeto del estilo existente y protección de cambios no relacionados.
- Ningún agente debe revertir cambios ajenos sin petición explícita del usuario.

### Criterio de uso de otros agentes
El uso de otros agentes debe decidirse por una combinación de:
- Valor técnico esperado.
- Riesgo del cambio.
- Tamaño/contexto de la tarea.
- Disponibilidad real del agente.
- Coste de cuota o límite de uso.

#### Requerir apoyo Multi-IA salvo justificación por cuota/disponibilidad cuando:
- El cambio afecte autenticación, permisos, seguridad, datos sensibles, base de datos o lógica crítica de negocio.
- La tarea sea amplia, ambigua o requiera comparar alternativas técnicas.
- Haya que revisar una implementación relevante antes de cerrar.
- El agente líder detecte incertidumbre técnica importante.
- El usuario solicite explícitamente revisión, segunda opinión o colaboración Multi-IA.

#### Usar otro agente opcionalmente cuando:
- La tarea sea mediana y una revisión paralela pueda ahorrar tiempo.
- Exista riesgo de regresión pero el cambio no sea crítico.
- Sea útil repartir trabajo entre análisis, implementación y validación.
- Gemini pueda ayudar con contexto amplio o investigación.
- Codex pueda ejecutar pruebas, inspeccionar archivos o aplicar parches rápidos.

#### No usar otro agente cuando:
- La tarea sea pequeña, local y de bajo riesgo.
- El cambio sea mecánico y verificable con tests o compilación.
- La consulta consumiría cuota sin aportar una mejora clara.
- El agente líder ya puede validar el resultado de forma objetiva.
- Otro agente no esté operativo o requiera autenticación/configuración adicional.

### Ahorro de cuota
La cuota es un recurso técnico del proyecto. Por tanto:
- No se debe gastar cuota de Claude Code, Codex o Gemini en comprobaciones repetitivas, tareas triviales o revisiones sin riesgo.
- No se debe hacer ping a todos los agentes en cada tarea.
- Se debe reutilizar la información de disponibilidad obtenida durante la misma sesión.
- Si Claude Code tiene cuota limitada, reservarlo para revisión final, cambios críticos o decisiones de calidad.
- Si Gemini tiene cuota limitada, reservarlo para contexto amplio, arquitectura, investigación o segunda opinión.
- Si Codex tiene cuota limitada, reservarlo para edición local, ejecución de comandos, pruebas y validación.
- Cuando no se use otro agente por ahorro de cuota, debe indicarse brevemente en el resumen final si la tarea era relevante.

### Regla práctica de decisión
Antes de invocar otro agente, responder internamente:
1. ¿Qué aportará este agente que no pueda verificarse localmente?
2. ¿El riesgo del cambio justifica gastar cuota?
3. ¿La tarea requiere una segunda opinión real o solo una comprobación rutinaria?
4. ¿Hay una validación objetiva más barata, como `mvn test`, compilación o inspección directa?

Si la respuesta no justifica claramente el coste, no se invoca otro agente y se documenta el motivo.

### Comprobación de disponibilidad
Antes de delegar o depender de un agente, comprobar solo lo necesario:
- Claude Code instalado: `claude --version`
- Claude Code operativo: `claude -p "ping"`
- Codex CLI instalado: `codex --version`
- Codex CLI autenticado: `codex login status`
- Codex CLI operativo: `$null | codex exec "ping" -s read-only --ephemeral`
- Gemini CLI instalado: `gemini --version`
- Gemini operativo: `gemini -p "ping"` con timeout mínimo de 3 minutos cuando la tarea requiera Gemini.
- Integraciones del IDE activas para Claude Code, Gemini Code Assist y Codex, si se van a usar desde el IDE.

No repetir estas comprobaciones en cada paso si ya se han realizado durante la misma sesión y el agente está funcionando.

### Comunicación entre agentes
- Claude Code puede llamar a Codex mediante CLI, IDE, MCP, terminal o bloque de instrucciones autocontenido.
- Claude Code puede llamar a Gemini mediante CLI, IDE, MCP, terminal o bloque de instrucciones autocontenido.
- Codex puede llamar a Claude Code mediante CLI, IDE, MCP, terminal o bloque de instrucciones autocontenido.
- Codex puede llamar a Gemini mediante CLI, IDE, MCP, terminal o bloque de instrucciones autocontenido.
- Gemini puede llamar a Claude Code o Codex mediante el canal disponible en el entorno o mediante instrucciones autocontenidas para el usuario/agente intermedio.
- Cuando un agente llame a otro, debe enviar contexto mínimo suficiente, ruta del proyecto, objetivo concreto, restricciones relevantes, archivos implicados y resultado esperado.
- Si la llamada directa falla, usar el mecanismo alternativo disponible sin bloquear la tarea salvo que la colaboración sea obligatoria por riesgo.

### Asignación según tarea
- Implementación localizada, edición de archivos, ejecución de tests o comandos: usar el agente que tenga acceso directo al workspace y menor fricción operativa.
- Revisión de seguridad, auth, permisos, datos sensibles o cambios críticos: solicitar segunda revisión a otro agente disponible.
- Cambios amplios, arquitectura, migraciones o decisiones con varias alternativas: consultar Gemini o Claude Code para planificación/revisión.
- Cambios pequeños y claros: un solo agente puede ejecutar. En tareas relevantes, debe dejar constancia de que Multi-IA no se invocó por no aportar valor técnico adicional.

### Flujo obligatorio
1. Declarar brevemente qué agente lidera la fase actual.
2. Determinar si la tarea requiere apoyo Multi-IA real.
3. Si requiere apoyo, comprobar disponibilidad del agente necesario.
4. Delegar o consultar con instrucciones concretas, contexto mínimo suficiente y objetivo verificable.
5. Ejecutar cambios quirúrgicos respetando el estado actual del workspace.
6. Validar con `mvn test`, compilación, ejecución manual o prueba equivalente según el cambio.
7. Para cambios relevantes, solicitar revisión final a Claude Code si está operativo; si no lo está, Codex o Gemini harán la revisión disponible.
8. Informar al final qué agentes participaron, qué aportó cada uno y qué validación se ejecutó.

### Mecanismo CLI / IDE
- Si el CLI del agente está operativo, usarlo directamente con una instrucción concreta y acotada.
- Si el CLI no está operativo pero existe integración IDE, preparar un bloque autocontenido para pegar en el IDE.
- Si ningún mecanismo está disponible, continuar con el agente actual e informar de la limitación.

### Interacción con Codex
- Codex debe poder ser invocado por Claude Code, Gemini o el usuario cuando aporte valor técnico real.
- **Método CLI** (Codex operativo): enviar instrucciones directamente vía `codex exec "..." -s workspace-write -C "<ruta-proyecto>"`.
- **Método read-only** (consulta o ping): usar `$null | codex exec "..." -s read-only --ephemeral`.
- **Método IDE** (Codex sin CLI operativo, sin cuota, sin red, sin autenticación o sin permisos): generar un bloque de instrucciones formateado con contexto del proyecto, ruta del archivo objetivo, cambios previos realizados por otros agentes y tarea concreta a ejecutar.
- El bloque para Codex debe ser autocontenido y suficiente para actuar sin contexto adicional.

### Seguridad
- No usar flags de bypass de permisos, modo YOLO o aprobación automática sin autorización explícita.
- No enviar secretos, claves API, credenciales, datos personales ni información sensible a agentes externos.
- No ejecutar comandos destructivos sin autorización explícita.
- Ningún agente debe revertir cambios ajenos sin petición expresa del usuario.
- La comunicación entre agentes debe limitarse al contexto necesario para la tarea.
- Mantener trazabilidad: indicar cuando una decisión viene de Claude Code, Codex, Gemini o criterio propio del agente que ejecuta.

### Trazabilidad
Al finalizar una tarea relevante, indicar:
- Agente líder.
- Agentes consultados o usados.
- Motivo de uso o motivo de no uso.
- Validación realizada.
- Limitaciones encontradas: cuota, autenticación, red, permisos o integración.

## REGLA DE INICIO

Toda respuesta conversacional sobre este proyecto debe comenzar preferentemente con:

**_Gipsybuho_**, procedo a **ejecutar** las instrucciones dadas:

Esta regla no debe aplicarse cuando interfiera con formatos técnicos obligatorios, mensajes de commit, bloques autocontenidos para otros agentes, salidas de comandos, parches, JSON, código o documentación generada.
