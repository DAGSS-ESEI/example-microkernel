# Example Microkernel

Ejemplo mínimo y ejecutable de **arquitectura de plugins (microkernel)** en Java.

## El problema

Llega un fichero CSV con una tabla de números (por ejemplo, unidades vendidas y
precio de cada pedido) y hay que aplicarles operaciones de limpieza o
normalización antes de analizarlos: restar la media de cada columna, reescalar
los valores a `[0, 1]`, dejarlos con media 0 y desviación típica 1...

Esas operaciones:

- no son fijas: aparecen nuevas con el tiempo y cada equipo quiere las suyas;
- se combinan en cualquier orden, según el análisis;
- no deberían obligar a tocar el código que las ejecuta.

No hace falta saber estadística: cada operación es una fórmula de una línea. Lo
que nos interesa aquí es **quién conoce a quién** y **cómo se añade o se quita
una pieza sin recompilar el core**.

## La idea en un dibujo

```
                   +-------------------------------------------+
   datos.csv ----> |  CORE (microkernel)                       |
                   |                                           |
                   |  1. lee la matriz                         |
                   |  2. Registry: descubre los plugins (SPI)  |<---- contrato
                   |  3. Pipeline: los aplica en orden         |      Transformation
                   |  4. escribe el resultado                  |      (solo la interfaz)
                   +---------------------+---------------------+
                                         |  invoca sin conocer
                    +--------------------+--------------------+
                    |                    |                    |
            plugin-minmax        plugin-standardize   plugin-subtract-mean
            (JAR independiente)  (JAR independiente)  (JAR independiente)
```

El core **solo** compila contra `Transformation`. Las implementaciones se
descubren en tiempo de ejecución con `java.util.ServiceLoader` (el mecanismo SPI
del JDK): no hay dependencias externas ni reflexión "casera".

### Las tres operaciones incluidas

| `id` | Qué hace | Fórmula (por columna) |
|---|---|---|
| `subtract-mean` | Centra los datos en 0 | `x - media` |
| `minmax` | Reescala al intervalo `[0, 1]` | `(x - mínimo) / (máximo - mínimo)` |
| `standardize` | Media 0 y desviación 1 (z-score) | `(x - media) / desviación` |

## Qué demuestra este ejemplo

- Un **contrato** de extensión pequeño y explícito (`Transformation`).
- Un **registro** que descubre implementaciones sin listarlas a mano.
- **Aislamiento de compilación**: el módulo `core` no depende de ningún plugin;
  Maven haría imposible importar una clase concreta de un plugin.
- **Despliegue por artefactos**: cada plugin es un JAR que se puede quitar o
  añadir del classpath sin recompilar nada (principio abierto/cerrado).
- **Gobierno del contrato**: ids únicos, ids estables, metadatos y errores
  accionables.

## Qué NO demuestra (a propósito)

- No hay **carga en caliente** real: si añades un JAR hay que reiniciar el
  proceso (en producción esto se resuelve con OSGi, JPMS o *classloaders*
  aislados).
- Un plugin que lanza una excepción **tumba la aplicación**: no hay aislamiento de
  fallos.
- No hay versionado del contrato ni compatibilidad entre versiones de plugins.
- No hay **configuración por plugin** (factores, umbrales, rutas): los valores
  están fijados en el código. El ejercicio 4 explica de dónde vendrían.
- No es un sistema operativo: es el patrón, no la infraestructura.

## Estructura

| Ruta | Qué contiene |
|---|---|
| `core/` | El microkernel: contrato, registro, pipeline, E/S CSV y CLI |
| `plugins/plugin-subtract-mean/` | Plugin `subtract-mean`: resta la media de cada columna |
| `plugins/plugin-minmax/` | Plugin `minmax`: escala cada columna a `[0, 1]` |
| `plugins/plugin-standardize/` | Plugin `standardize`: z-score (media 0, desviación 1) |
| `datos/` | CSV de ejemplo y salida esperada para el test de integración |
| `run.sh` | Arranca el core con `core/target/classes` + `plugins/*/target/*.jar` |
| `scripts/smoke-test.sh` | Test de integración sin frameworks (build + catálogo + pipeline + error) |
| `docs/guion-demo.md` | Guion de la demo en vivo (5 minutos) |
| `docs/ejercicios.md` | Ejercicios de ampliación para el alumnado |

## Requisitos y ejecución

Java 21 y Maven 3.8+.

```bash
mvn package          # compila los 5 módulos y ejecuta los tests

./run.sh --list      # catálogo funcional
./run.sh --list -v   # catálogo + JAR del que se carga cada plugin

./run.sh --run subtract-mean,standardize datos/ejemplo.csv
./run.sh --run subtract-mean,standardize datos/ejemplo.csv salida.csv

./scripts/smoke-test.sh
```

Salida de `--list -v` (fragmento):

```
ID                 NOMBRE                           ORIGEN
--                 ------                           ------
minmax             Escala min-max                   plugin-minmax-1.0-SNAPSHOT.jar
standardize        Estandariza (z-score)            plugin-standardize-1.0-SNAPSHOT.jar
subtract-mean      Resta la media                   plugin-subtract-mean-1.0-SNAPSHOT.jar
```

Ese `ORIGEN` es la prueba visual de que cada plugin es un artefacto distinto.

### Formato de los CSV

Una fila por línea y valores separados por coma, punto y coma o tabulador. Se
ignoran las líneas vacías y las que empiezan por `#`.

**Importante**: el separador decimal es el punto (`3.5`). La coma se usa como
separador de columnas.

## Cómo añadir un plugin en 3 pasos

1. **Crear el módulo**: copia `plugins/plugin-minmax/`, cambia el `artifactId` y
   añádelo a `<modules>` del `pom.xml` raíz.
2. **Implementar el contrato**:

   ```java
   package es.uvigo.esei.dagss.microkernel.plugins.miplugin;

   public final class MiTransformacion implements Transformation {
       @Override public String id()          { return "mi-plugin"; }
       @Override public String name()        { return "Mi plugin"; }
       @Override public String description() { return "Qué hace, en una línea"; }

       @Override public Matrix apply(Matrix input) {
           double[][] values = input.toArray();  // copia: la entrada es inmutable
           // ... transformar values ...
           return Matrix.of(values);
       }
   }
   ```

3. **Declararlo** en
   `src/main/resources/META-INF/services/es.uvigo.esei.dagss.microkernel.core.api.Transformation`:

   ```
   es.uvigo.esei.dagss.microkernel.plugins.miplugin.MiTransformacion
   ```

Después, `mvn package` y el plugin aparece en `--list -v`. **No se ha tocado ni
una línea del core.**

Requisitos que impone `ServiceLoader`: clase pública, constructor público sin
argumentos e implementación del contrato.

## Decisiones de diseño

| Decisión | Por qué |
|---|---|
| `Matrix` inmutable | Un plugin no puede corromper la entrada de los siguientes pasos; los errores por efectos secundarios desaparecen por diseño. |
| Contrato sin estado ni configuración | Menos ruido y plugins seguros para uso concurrente. El ejercicio 4 trata las dos evoluciones por separado: configuración (parámetros que vienen de fuera) y estado (`fit`/`transform`, estadísticos aprendidos de los datos). |
| `PluginRegistry.register()` público | El registro es un concepto explícito, no magia del framework; además permite tests con plugins falsos. |
| Ids duplicados = error | Dos JARs reclamando el mismo id producen un comportamiento ambiguo: mejor fallar al arrancar. |
| `--list -v` muestra el JAR | Hace visible el despliegue, que es justo lo que el patrón aporta. |
| Sin JPMS (`module-info`) | El ejemplo usa el classpath a propósito: menos ceremonia para explicar el mecanismo. |
| Salida CSV determinista (`%.4f`, `Locale.ROOT`) | Permite comparar la salida en el test de integración. |

## Trade-offs arquitectónicos

Los mismos que aparecen en el banco de exámenes del Tema 3:

| Fortalezas | Debilidades |
|---|---|
| Coste y simplicidad | Naturaleza monolítica: **1 *quantum*** |
| Modularidad y extensibilidad vía plugins | *Scalability*, *elasticity* y *fault tolerance* limitadas |
| El core no cambia al añadir funcionalidad (OCP) | *Deployability* intermedia: core y plugins se despliegan juntos |
| Plugins testeables de forma aislada | *Testability* intermedia: hay que probar también la integración y el contrato |
| Contratos explícitos y gobernables | Los contratos son caros de cambiar y hay que versionarlos |

## Relación con la teoría (Tema 3)

- **Estilo**: plugin/microkernel = *core* + *contracts* (extension points) +
  *plugins* + *registry*.
- **Principios**: inversión de dependencias (el core define el contrato; los
  plugins dependen de él) y abierto/cerrado (extender sin modificar).
- **Comparación**: una capa de servicios o un pipeline resuelven "ejecutar
  pasos"; el microkernel añade **descubrimiento y despliegue independiente**.
- **Evolución**: el contrato es el punto caro. Cualquier cambio incompatible
  obliga a recompilar y redistribuir todos los plugins.

## Siguientes pasos (si quieres llevarlo más lejos)

- **JPMS**: `module-info.java` con `provides ... with ...` en cada plugin.
- **Plugins fuera del classpath**: cargar `plugins/*.jar` con un
  `URLClassLoader` propio y recargarlos sin reiniciar.
- **Metadatos en descriptor** (`plugin.properties`) para listar plugins sin
  cargar sus clases.
- **Versionado del contrato** y comprobación de compatibilidad al registrar.
- **Aislamiento de fallos**: ejecutar cada plugin en un proceso o hilo aparte,
  con *timeout* y captura de errores.
- **Seguridad**: firmar los JARs y aplicar una política de permisos.

## Documentación

- [`docs/guion-demo.md`](docs/guion-demo.md): demo en vivo paso a paso.
- [`docs/ejercicios.md`](docs/ejercicios.md): ejercicios de ampliación.
