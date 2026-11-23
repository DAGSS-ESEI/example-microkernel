# Guion de la demo en vivo (5 minutos)

**Mensaje que debe quedar claro al terminar**: el core define el contrato y no
conoce a los plugins; añadir o quitar funcionalidad es añadir o quitar un JAR,
sin recompilar el core.

## Antes de clase (1 minuto, fuera del aula)

```bash
cd example-microkernel
mvn -q package
./run.sh --list -v        # comprobar que aparecen 3 plugins
```

Ten a mano una terminal con fuente grande y el árbol de directorios abierto (o
`tree -L 3`). Deja también abierto `core/pom.xml` para enseñar que **no** hay
dependencias a plugins.

## Demo (minuto a minuto)

### 0:00 — Situar el problema (30 s)

> "Tenemos un sistema que procesa matrices. Cada operación es una pieza
> independiente: restar la media, escalar, estandarizar... La pregunta de
> arquitectura no es cómo se calcula, sino **quién tiene que cambiar cuando
> aparece una operación nueva**."

### 0:30 — El catálogo y su origen (1 min)

```bash
./run.sh --list -v
```

Salida esperada:

```
Plugins descubiertos: 3

ID                 NOMBRE                           ORIGEN
--                 ------                           ------
minmax             Escala min-max                   plugin-minmax-1.0-SNAPSHOT.jar
standardize        Estandariza (z-score)            plugin-standardize-1.0-SNAPSHOT.jar
subtract-mean      Resta la media                   plugin-subtract-mean-1.0-SNAPSHOT.jar
```

> "Fijaos en la última columna: cada plugin viene de un JAR distinto. El core no
> los importa; los descubre al arrancar mediante `ServiceLoader`."

Enseña el contrato (`Transformation.java`, 4 métodos) y `PluginRegistry`.

### 1:30 — Componer un pipeline (1 min)

```bash
./run.sh --run subtract-mean,standardize datos/ejemplo.csv
```

Salida esperada:

```
-0.6799,0.0000,-0.4981
0.2914,-1.4951,0.3321
-1.1655,1.3755,-1.4666
1.7483,-0.5980,1.5773
-0.1943,0.7176,0.0553
```

> "El orden importa y lo decide el usuario, no el código. El core solo ejecuta
> los ids que le pasamos."

### 2:30 — **El momento clave**: quitar un plugin (1 min)

```bash
mkdir -p /tmp/plugins-off
mv plugins/plugin-minmax/target/plugin-minmax-1.0-SNAPSHOT.jar /tmp/plugins-off/
./run.sh --list
```

Salida esperada:

```
Plugins descubiertos: 2
...
standardize        Estandariza (z-score) ...
subtract-mean      Resta la media ...
```

> "No he recompilado nada. No he tocado el core. La funcionalidad ha desaparecido
> porque su JAR no está en el classpath. Eso es el principio abierto/cerrado,
> pero a nivel de despliegue."

Devuélvelo y repite `--list`:

```bash
mv /tmp/plugins-off/plugin-minmax-1.0-SNAPSHOT.jar plugins/plugin-minmax/target/
./run.sh --list
```

### 3:30 — El contrato también sirve para fallar bien (30 s)

```bash
./run.sh --run no-existe datos/ejemplo.csv
```

Salida esperada (código de salida 2):

```
Error: No existe el plugin 'no-existe'. Disponibles: minmax, standardize, subtract-mean
```

> "Un buen contrato incluye buenos errores: el sistema te dice qué había
> disponible. Eso es gobierno del contrato."

### 4:00 — Enganchar con la teoría (1 min)

- El core **define** el contrato; los plugins **lo implementan**. Nunca al revés.
- Un solo despliegue y una sola base de datos: **monolito, 1 *quantum***.
- Fortalezas: coste, simplicidad, extensibilidad. Debilidades: *scalability*,
  *elasticity*, *fault tolerance*; *deployability* y *testability* intermedias.
- Puente a la clase: ¿qué pasa si un plugin tarda o falla? ¿y si hay dos
  versiones del contrato? (ver `docs/ejercicios.md`).

## Preguntas para lanzar a la clase

1. Si mañana el equipo de negocio pide una operación nueva, ¿qué ficheros se
   tocan? ¿Cuántos equipos?
2. ¿Qué impide que dos plugins usen el mismo `id`?
3. ¿Por qué el core no puede importar `MinMaxTransformation`? ¿Qué ganamos?
4. Si quitamos un JAR en producción, ¿qué pasa con las peticiones en curso?
5. ¿Esto escala horizontalmente? ¿Por qué no? ¿Qué habría que cambiar?

## Plan B si algo falla

- Si `run.sh` dice que no está compilado: `mvn package` (30 s).
- Si no aparecen plugins: comprueba `plugins/*/target/*.jar` y que cada JAR tiene
  `META-INF/services/es.uvigo.esei.dagss.microkernel.core.api.Transformation`.
- Si el pipeline falla por columna constante: usa `datos/ejemplo.csv` (tiene
  varianza en las tres columnas).
- Todo el guion está automatizado en `./scripts/smoke-test.sh`, que puedes usar
  como red de seguridad antes de entrar al aula.
