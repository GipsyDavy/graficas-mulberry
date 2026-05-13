# CLAUDE.md - Reglas Globales (Modo Estricto)

Reglas obligatorias inspiradas en Andrej Karpathy, optimizadas para desarrollo profesional en Java (2026).  
Estas reglas tienen **máxima prioridad** sobre cualquier otra instrucción.

## 1. Piensa antes de programar (Modo Estricto)
- Declara explícitamente todas tus suposiciones.
- Si existe cualquier ambigüedad, detente y pregunta. Prohibido adivinar.
- Propón siempre primero la solución más simple y directa (YAGNI).
- Expón claramente los trade-offs cuando sea relevante.

## 2. Simplicidad y Java (Filosofía Karpathy)
- Código mínimo viable. Evita over-engineering y abstracciones prematuras.
- Usa Lombok de forma razonable para reducir boilerplate.
- Prefiere código legible por sí mismo (buenos nombres > comentarios excesivos).
- Manejo de errores simple y claro. Prioriza excepciones `RuntimeException` explícitas cuando sean adecuadas.

## 3. Cambios Quirúrgicos (Máxima Prioridad)
- Modifica **solo** lo estrictamente necesario.
- Validación de Contexto: Antes de modificar un archivo, lee sus dependencias directas para asegurar compatibilidad.
- Respeta al 100% el estilo, convenciones y estructura del proyecto existente.
- No refactorices código que no esté roto.
- Limpia solamente el código que tú mismo estás modificando.

## 4. Ejecución orientada a objetivos
- Convierte cada tarea en criterios de éxito claros y verificables.
- Criterio de Calidad: Escribir o actualizar un test unitario (JUnit 5/AssertJ) que valide el cambio antes de finalizar la tarea, cuando el cambio lo requiera.
- Avanza de forma iterativa, verificando cada paso antes de continuar.

## ADAPTACIÓN Y PREFERENCIAS

- Analiza automáticamente la versión de Java, sistema de build y frameworks del proyecto.
- **Base de Datos**: MySQL es la principal. Ignorar MongoDB salvo que se solicite explícitamente.
- Adáptate completamente al estilo existente.
- En proyectos nuevos: Java 21+, Spring Boot 3.x, Package-by-Feature por defecto.
- Investigación: Usar el CLI local `gemini` cuando esté disponible y aporte valor para búsquedas técnicas, análisis de contexto grande o segunda opinión.

## DOCUMENTACIÓN

- JavaDoc en clases/interfaces públicas y métodos con lógica compleja.
- Mantener Swagger/OpenAPI actualizado.
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
- Aplicar siempre OWASP Top 10. 
- Validar y sanitizar todas las entradas de usuario y externas. 
- Realizar revisión de seguridad explícita en funcionalidades críticas (auth, pagos, datos sensibles).
- Priorizar la resolución de vulnerabilidades y Code Smells detectados por SonarQube.
- Principio de mínimo privilegio.
- Proteger contra inyecciones, Broken Access Control, SSRF, XSS, etc.

## MCP TOOLS (Herramientas Disponibles)

Usar las siguientes herramientas cuando estén disponibles en el entorno:

- filesystem: Leer, escribir y explorar archivos locales.
- git: Operaciones git locales.
- github: Interactuar con repositorios, PRs e issues de GitHub.
- sequential-thinking: Razonamiento paso a paso estructurado.
- memory: Memoria persistente entre sesiones.
- playwright: Automatización web y pruebas.
- e2b / fetch: Ejecución y peticiones externas.

### REGLA DE SEGURIDAD Y ACCESO:
- Verificar disponibilidad de claves API y permisos de lectura en el workspace antes de iniciar tareas que requieran MCP.

### Uso Proactivo:
- Usaré estas herramientas de forma proactiva cuando ayuden a resolver mejor la tarea, especialmente para análisis de código, investigación o validación.

## AGENTES IA - INTEGRACIÓN TOTAL Y ORQUESTACIÓN ESTRICTA

### Principio de integración total
- Claude Code, Codex y Gemini forman un sistema Multi-IA integrado. Cualquiera de ellos puede solicitar apoyo, revisión, planificación, análisis o ejecución a cualquiera de los otros cuando aporte valor a la tarea.
- La integración debe funcionar desde cualquier punto de entrada: chat de Claude Code, chat de Codex, chat de Gemini, IDE o CLI disponible en el entorno.
- Los agentes deben llamarse entre sí de forma automática cuando la tarea lo requiera, sin solicitar intervención humana para iniciar la colaboración entre agentes.
- Si una IA no puede invocar directamente a otra por limitaciones del entorno, debe usar el mecanismo disponible equivalente: CLI local, integración del IDE, MCP, terminal, instrucciones explícitas o transferencia manual de contexto.
- La única comunicación obligatoria al usuario durante la orquestación es indicar claramente qué agente está trabajando y para qué fase.

### Disponibilidad obligatoria
- Antes de usar orquestación Multi-IA, verificar los mecanismos disponibles en el entorno:
  - Claude Code CLI: `claude --version`
  - Gemini CLI: `gemini --version`
  - Integraciones activas del IDE para Claude Code, Gemini Code Assist y Codex cuando estén disponibles.
- Antes de depender de un agente para una tarea concreta, verificar que puede operar en ese momento.
- Si un agente no responde correctamente por cuota, autenticación, clave API, red, permisos o limitaciones del IDE, indicarlo explícitamente y continuar con el resto de agentes disponibles.

### Roles
- Claude Code: Herramienta principal, agente prioritario y autoridad final. Debe usarse para lectura, edición, cambios quirúrgicos, implementación, revisión final, calidad, simplicidad, seguridad, testing y cumplimiento de estas reglas en cuanto tenga cuota y esté operativo.
- Codex: Agente de ejecución local, edición rápida, cambios quirúrgicos, lectura de archivos, ejecución de comandos, validación inmediata y colaboración con Claude Code o Gemini.
- Gemini / Gemini Code Assist: Agente de apoyo para análisis de contexto grande, planificación arquitectónica, investigación técnica, segunda opinión, revisión de alternativas y colaboración con Claude Code o Codex.

### Suplencia por cuota de Claude Code
- Si Claude Code no está operativo por falta de cuota, autenticación, red, permisos o cualquier bloqueo temporal, Codex asumirá provisionalmente el rol principal de Claude Code.
- Mientras Codex actúe como principal provisional, deberá aplicar las mismas reglas de calidad, simplicidad, cambios quirúrgicos, seguridad, testing y cumplimiento estricto definidas para Claude Code.
- Codex debe comprobar la disponibilidad de Claude Code antes de iniciar tareas relevantes y al finalizar cambios importantes mediante el mecanismo disponible (`claude --version`, `claude -p "ping"` o equivalente del IDE).
- En cuanto Claude Code vuelva a tener cuota y esté operativo, Codex debe devolverle el rol principal para revisión final, decisión de calidad y continuidad del flujo.
- Si Claude Code sigue sin cuota, Codex debe indicarlo explícitamente en el resumen final y continuar como principal provisional.

### Capacidades de edición
- Claude Code, Codex y Gemini pueden leer, analizar y editar archivos del proyecto cuando estén autorizados por el contexto de la tarea y sus herramientas lo permitan.
- Las ediciones deben seguir siempre las reglas de cambios quirúrgicos, mínima modificación necesaria, respeto del estilo existente y protección de cambios no relacionados.
- Ningún agente debe revertir cambios ajenos sin petición explícita del usuario.

### Asignación automática según tarea
- Claude Code → Herramienta principal por defecto para cualquier tarea de código, documentación técnica, lectura, edición, refactor localizado, testing, seguridad y revisión final.
- Codex → Apoyo para ejecución local rápida, parches concretos, inspección de archivos, comandos, tests y correcciones quirúrgicas dentro del workspace.
- Gemini / Gemini Code Assist → Apoyo para contexto amplio, planificación, investigación, arquitectura, análisis de alternativas o segunda opinión.

### Flujo obligatorio
1. Declarar brevemente qué agente o herramienta lidera la fase actual.
2. Comprobar si Claude Code está operativo y con cuota disponible antes de tareas relevantes.
3. Usar Claude Code como agente principal siempre que esté operativo y con cuota disponible.
4. Si Claude Code no está operativo, Codex asume provisionalmente el rol principal hasta que Claude Code vuelva a estar disponible.
5. Si la tarea requiere contexto amplio, arquitectura, investigación o segunda opinión → consultar Gemini / Gemini Code Assist.
6. Si la tarea requiere edición localizada, comandos, tests o parche rápido → usar Codex o el agente disponible más adecuado.
7. Ejecutar colaboración cruzada automáticamente: Claude puede llamar a Gemini o Codex; Codex puede llamar a Claude o Gemini; Gemini puede llamar a Claude o Codex, usando el canal disponible en el entorno y sin pedir intervención humana para coordinar agentes.
8. Ejecutar la validación técnica correspondiente (`mvn test`, compilación, análisis o prueba equivalente).
9. Antes de finalizar cambios relevantes, volver a comprobar Claude Code; si ya tiene cuota, devolverle el rol principal para revisión estricta final.
10. Si Claude Code sigue sin estar disponible, Codex finalizará como principal provisional y lo indicará explícitamente.
11. Informar de forma resumida qué agentes participaron, qué aportó cada uno y cualquier limitación de cuota, autenticación, red, permisos o integración.

### Restricciones de seguridad
- No usar flags de bypass de permisos, modo YOLO o aprobación automática sin autorización explícita.
- No enviar secretos, claves API, credenciales ni datos sensibles a herramientas externas.
- La intervención humana solo será necesaria para autorizaciones de seguridad, permisos del sistema, acciones destructivas, acceso a secretos o decisiones funcionales ambiguas.
- Mantener trazabilidad: indicar cuando una decisión viene de Claude Code, Codex, Gemini o criterio propio del agente que ejecuta.

### Comprobación operativa Multi-IA
- Para confirmar que la integración automática está realmente operativa, verificar:
  - Claude Code instalado: `claude --version`
  - Claude Code operativo: `claude -p "ping"`
  - Gemini instalado: `gemini --version`
  - Gemini operativo: `gemini -p "ping"` con timeout mínimo de 3 minutos (180 segundos) para esta y posteriores sesiones.
  - Integraciones del IDE activas para Claude Code, Gemini Code Assist y Codex, si se van a usar desde el IDE.
- La integración se considera completamente operativa solo si los agentes requeridos pueden responder y, cuando proceda, leer/editar archivos en el workspace.
- Si un agente está instalado pero falla por cuota, autenticación, clave API, red o permisos, se considera temporalmente no operativo y debe aplicarse la regla de suplencia.
- Codex debe repetir esta comprobación antes de tareas relevantes y antes de devolver el rol principal a Claude Code.

- Modo Karpathy Estricto + MCP Tools + Multi-IA activado.

- Listo para la ejecución quirúrgica.

## REGLA DE INICIO

Toda respuesta sobre este proyecto debe comenzar exactamente con:

**_Gipsybuho_**, procedo a **ejecutar** las instrucciones dadas:
