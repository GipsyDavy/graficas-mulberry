# CLAUDE.md - Reglas Globales (Modo Estricto)

Reglas obligatorias inspiradas en Andrej Karpathy, optimizadas para Java Profesional (2026).
Estas reglas tienen **máxima prioridad** sobre cualquier otra instrucción general.

## 1. Piensa antes de programar (Modo Estricto)
- **Suposiciones:** Antes de proponer código, declara explícitamente tus suposiciones.
- **Ambigüedad:** Si algo no está claro al 100%, **detente y pregunta**. Prohibido adivinar.
- **Simplicidad:** Propón siempre la solución más directa y minimalista ("YAGNI" - You Ain't Gonna Need It).
- **Trade-offs:** Expón claramente los beneficios y contras de la solución elegida (ej. rendimiento vs. legibilidad).

## 2. Simplicidad y Java (Filosofía Karpathy)
- **Cero Over-engineering:** Prohibidas las interfaces de una sola implementación o patrones complejos si una clase simple resuelve el problema.
- **Código Mínimo:** Usa **Lombok** para eliminar boilerplate (getters, setters, constructores) siempre que sea posible.
- **Manejo de Errores:** Evita bloques try-catch vacíos o lógica de reintento compleja para casos improbables. Prioriza `RuntimeExceptions` claras en los puntos de entrada.
- **Legibilidad:** El código debe ser tan limpio que los comentarios sean casi innecesarios.

## 3. Cambios Quirúrgicos (Máxima Prioridad)
- **Impacto Mínimo:** Modifica **únicamente** las líneas necesarias para cumplir el objetivo. No toques lo que no está roto.
- **Respeto al Estilo:** Adáptate al 100% a las convenciones, nombres y estructura del proyecto existente, aunque no te gusten.
- **Limpieza Localizada:** Solo limpia o refactoriza el código que estás modificando directamente.

## === ADAPTACIÓN Y PREFERENCIAS ===

### Análisis de Proyecto Existente
- Antes de actuar, identifica: Versión de Java, sistema de construcción (Maven/Gradle) y frameworks activos (Spring Boot, Quarkus, etc.).
- Si el proyecto usa estándares antiguos (ej. JUnit 4 o Java 8), respétalos a menos que se pida una migración.

### Proyectos Nuevos (Greenfield)
- **Stack:** Java 21+, Spring Boot 3.x, Maven.
- **Arquitectura:** Inicia con **Package-by-Feature** (Simplicidad). Solo escala a **Arquitectura Hexagonal/Clean** si la lógica de negocio es muy compleja y el usuario lo confirma explícitamente.
- **Testing:** JUnit 5 + AssertJ + Mockito. Testcontainers para integración.

## === DOCUMENTACIÓN Y GIT ===
- **JavaDoc:** Solo en APIs públicas o lógica compleja. No documentar lo obvio.
- **Git Atómico:** Un commit = Un cambio lógico.
- **Mensajes de Commit:** `tipo: descripción corta en español` (ej: `feat: login con JWT`).
- **Tipos:** `feat`, `fix`, `refactor`, `docs`, `style`, `test`, `chore`, `perf`, `security`.

## === INTEGRACIÓN CON GEMINI (Protocolo Multi-IA) ===

Claude Code actúa como implementador y Gemini como estratega de contexto.

1. **Contexto Masivo:** Si el archivo supera las 500 líneas o el cambio afecta a más de 5 archivos, Claude debe sugerir: *"Esto requiere visión global. Por favor, pide a Gemini que analice [archivos] y pégame aquí su plan"*.
2. **Segunda Opinión:** Para cambios críticos en seguridad o refactors grandes, Claude solicitará: *"He preparado el código; se recomienda validación de Gemini antes de aplicar"*.
3. **Flujo CLI:** Si el usuario tiene `gemini-cli`, Claude puede pedir ejecutar: `gemini "analiza [archivo] para detectar bugs"` y procesar el resultado.

---
**Modo Karpathy + Integración Gemini activado.**
Listo para la ejecución quirúrgica.
