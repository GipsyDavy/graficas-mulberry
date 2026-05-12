# CLAUDE.md - Reglas Globales (Modo Estricto)

Reglas obligatorias inspiradas en Andrej Karpathy, optimizadas para desarrollo profesional en Java (2026).  
Estas reglas tienen **máxima prioridad** sobre cualquier otra instrucción.

## 1. Piensa antes de programar (Modo Estricto)
- Declara explícitamente todas tus suposiciones.
- Si existe cualquier ambigüedad, detente y pregunta. Prohibido adivinar.
- Propón siempre primero la solución más simple y directa.
- Expón claramente los trade-offs cuando sea relevante.

## 2. Simplicidad y Java (Filosofía Karpathy)
- Código mínimo viable. Evita over-engineering y abstracciones prematuras.
- Usa Lombok de forma razonable para reducir boilerplate.
- Prefiere código legible por sí mismo (buenos nombres > comentarios excesivos).
- Manejo de errores simple y claro.

## 3. Cambios Quirúrgicos (Máxima Prioridad)
- Modifica **solo** lo estrictamente necesario.
- Validación de Contexto: Antes de modificar un archivo, lee sus dependencias directas para asegurar compatibilidad de tipos y métodos.
- Respeta al 100% el estilo, convenciones y estructura del proyecto existente.
- No refactorices código que no esté roto.
- Limpia solamente el código que tú mismo estás modificando.

## 4. Ejecución orientada a objetivos
- Convierte cada tarea en criterios de éxito claros y verificables.
- Criterio de Calidad: Escribir o actualizar un test unitario (JUnit 5/AssertJ) que valide el cambio antes de finalizar la tarea.
- Avanza de forma iterativa, verificando cada paso antes de continuar.

## === ADAPTACIÓN Y PREFERENCIAS ===

- Analiza automáticamente la versión de Java, sistema de build y frameworks del proyecto.
- Build System: Priorizar Maven (pom.xml) o Gradle (build.gradle) según se detecte en la raíz.
- Adáptate completamente al estilo existente.
- En proyectos nuevos: Java 21+, Spring Boot 3.x, Package-by-Feature por defecto.

## === DOCUMENTACIÓN ===

- JavaDoc en clases/interfaces públicas y métodos con lógica compleja.
- Mantener Swagger/OpenAPI actualizado en APIs.
- Preferir código auto-documentado.
- Actualizar README.md cuando sea necesario.

## === GIT Y COMMITS (Convenciones Estrictas) ===

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

Branching:

- Usar prefijos claros: feat/, fix/, refactor/, docs/, chore/, etc.
- Mantener las ramas actualizadas con la principal.

Pull Requests:

- Título claro y descriptivo.
- Descripción que explique qué cambia, por qué y cómo se probó.
- Incluir evidencia (capturas o tests) cuando corresponda.

=== SEGURIDAD (Modo Estricto) ===

- Aplicar siempre OWASP Top 10.
- Validar y sanitizar todas las entradas de usuario y externas.
- Principle of Least Privilege.
- Proteger contra inyecciones, Broken Access Control, SSRF, XSS, etc.
- Realizar revisión de seguridad explícita en funcionalidades críticas (auth, pagos, datos sensibles).

=== MCP TOOLS (Herramientas Disponibles) ===

Puedo utilizar las siguientes herramientas MCP cuando sea necesario:

- filesystem: Leer, escribir y explorar archivos locales.
- git: Operaciones git locales.
- github: Interactuar con repositorios, PRs e issues de GitHub.
- sequential-thinking: Razonamiento paso a paso estructurado.
- memory: Memoria persistente entre sesiones.
- playwright: Automatización web y pruebas.
- e2b / fetch: Ejecución y peticiones externas.

Regla: Usaré estas herramientas de forma proactiva cuando ayuden a resolver mejor la tarea, especialmente para análisis de código, investigación o validación.

=== INTEGRACIÓN CON GEMINI (Multi-IA) ===

Flujo Inteligente Automático:

- Gemini se usará preferentemente para:
 * Análisis de codebase grande o múltiples archivos
 * Planificación inicial y estrategias
 * Investigación y segunda opinión

- Claude es responsable final de:
 * Implementación del código
 * Aplicación de reglas Karpathy
 * Cambios quirúrgicos
 * Revisión de calidad y seguridad


Flujo recomendado:

1. Gemini → Análisis / Planificación (cuando el contexto sea grande)
2. Claude → Implementación precisa y quirúrgica
3. Claude → Revisión final + Seguridad + Documentación

Siempre indicaré claramente cuando consulte a Gemini.

Modo Karpathy Estricto + MCP Tools + Multi-IA activado.

Listo para la ejecución quirúrgica.

REGLA DE PRUEBA: Cada vez que respondas a una pregunta sobre este proyecto, debes empezar la respuesta con la palabra 'En respuesta a tu pregunta Gipsybuho:'