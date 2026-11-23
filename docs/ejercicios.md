# Ejercicios de ampliación

Los ejercicios van de menos a más y atacan las decisiones caras del patrón:
contrato, descubrimiento, evolución y despliegue.

Antes de empezar:

```bash
mvn package
./scripts/smoke-test.sh
```

---

## 1. Un plugin nuevo sin tocar el core

**Objetivo**: comprobar en primera persona que extender no exige modificar.

Implementa un plugin `clip` que recorte cada valor al intervalo
`[media - 2·desviación, media + 2·desviación]` de su columna (recorte de
*outliers*).

**Pasos**

1. Copia `plugins/plugin-minmax/` como `plugins/plugin-clip/` y ajusta
   `artifactId` en su `pom.xml`.
2. Añade el módulo a `<modules>` del `pom.xml` raíz.
3. Implementa `ClipTransformation` (`id = "clip"`), declara el fichero en
   `META-INF/services/` y escribe un test.
4. `mvn package` y comprueba `./run.sh --list -v`.

**Criterios de aceptación**

- `git diff --stat core/` está vacío.
- El plugin aparece con su propio JAR como origen.
- `./run.sh --run clip datos/ejemplo.csv` produce una matriz de las mismas
  dimensiones y sin valores fuera de los límites.
- `./scripts/smoke-test.sh` sigue en verde.

**Pregunta**: ¿cuántas líneas has tenido que cambiar en código existente? ¿Qué
nombre recibe esa métrica cuando es alta?

---

## 2. Un segundo *extension point*

**Objetivo**: entender que un microkernel puede tener varios contratos y que cada
uno tiene su propio registro.

Hoy la E/S está fijada a CSV dentro del core. Extrae dos contratos nuevos:

```java
public interface MatrixReader { String id(); Matrix read(Path path) throws IOException; }
public interface MatrixWriter { String id(); void write(Matrix matrix, Path path) throws IOException; }
```

Crea plugins `reader-json` y `writer-json` (puedes inventar un JSON sencillo:
`[[1,2],[3,4]]`) y permite elegir el formato por línea de comandos:

```bash
./run.sh --run subtract-mean --in datos/ejemplo.csv --in-format csv \
         --out salida.json --out-format json
```

**Criterios de aceptación**

- El core ya no importa `CsvMatrixReader`/`CsvMatrixWriter` desde `Main`.
- Cada contrato se descubre con su propio `ServiceLoader` y su propio registro.
- Un formato desconocido produce un error con el catálogo disponible.

**Preguntas**: ¿por qué no vale un único registro para los tres contratos? ¿Qué
pasa si un JAR declara un lector de CSV y otro también? ¿Cómo lo resolverías?

---

## 3. Contraste con el *fat core*

**Objetivo**: medir el coste de no tener plugins.

Esta es la versión "sin arquitectura" del mismo sistema. Añade una clase
`NaiveMain` (no la conectes al CLI, es solo para comparar):

```java
Matrix result = input;
for (String operation : operations.split(",")) {
    switch (operation) {
        case "subtract-mean" -> result = subtractMean(result);
        case "minmax"        -> result = minMax(result);
        case "standardize"   -> result = standardize(result);
        default -> throw new IllegalArgumentException("Operación desconocida: " + operation);
    }
}
```

Responde **con datos**, no con opiniones:

1. ¿Cuántas líneas hay que tocar para añadir `clip`?
2. ¿Cuántos tests existentes pueden romperse al añadirlo?
3. ¿Cuántos equipos tendrían que coordinar su despliegue?
4. ¿Qué pasa si dos clientes necesitan *conjuntos distintos* de operaciones?

**Criterios de aceptación**: una tabla comparativa *naive* vs. *plugins* con esas
cuatro filas y una conclusión de una frase.

**Conceptos**: *change amplification*, principio abierto/cerrado, acoplamiento
aferente, coste de regresión.

---

## 4. Evolucionar el contrato (la parte difícil)

**Objetivo**: comprobar que el contrato es el punto caro del patrón. Se evoluciona
en **dos direcciones independientes** que conviene no confundir:

| Necesidad | Qué es | De dónde sale |
|---|---|---|
| **Configuración** | Parámetros que el usuario elige antes de ejecutar (`factor=3`) | De la composición: CLI, fichero de despliegue, variables de entorno o UI |
| **Estado** | Datos calculados a partir de un dataset y reutilizados después (media, desviación, modelo) | De una ejecución anterior (`fit`) sobre los datos |

### 4a. Configuración: de dónde vienen los parámetros

El contrato no debe saber si el parámetro llega de la CLI, de un YAML o de una
pantalla. El core solo transporta un `Map<String, String>`; **quien interpreta
las claves es el plugin**.

Sintaxis propuesta en la CLI (coherente con `--run`):

```bash
./run.sh --run "clip(factor=3),standardize" datos/ejemplo.csv
```

Reparto de responsabilidades:

1. **Core (*composition root*)**: parsea `id(clave=valor,...)` y llama a
   `configure(...)` antes de `apply(...)`. No conoce ninguna clave.
2. **Plugin**: valida y convierte los valores (`factor` numérico y mayor que 0) y
   falla con un mensaje claro si no sirven.
3. **Fichero de despliegue** (alternativa): un `plugins.properties` con
   `clip.factor=3` permite cambiar valores sin tocar la línea de comandos. El
   core lo lee, lo fusiona con lo que venga de la CLI y esta tiene prioridad.

Añade al contrato:

```java
default void configure(Map<String, String> parameters) { /* sin configuración */ }
```

y un parámetro `factor` para `clip` (por defecto 2). Después:

1. Haz que la CLI acepte `clip(factor=3)`.
2. Registra un plugin antiguo que **no** sobrescribe `configure`.
3. Inventa un segundo cambio **incompatible**: `apply` pasa a devolver
   `Optional<Matrix>` (o se añade un método abstracto nuevo).

**Preguntas**

- ¿Quién valida los tipos: el core, el plugin o ambos? ¿Dónde se convierte
  `"3"` a `int`?
- ¿Qué haces con una clave desconocida: error, aviso o silencio? ¿Y con un valor
  fuera de rango?
- ¿Dónde se registra la configuración efectiva para poder **reproducir** un
  análisis más tarde?
- ¿Qué plugins siguen funcionando sin recompilarse en cada escenario?
- ¿Por qué un `default method` salva la compatibilidad binaria y un método
  abstracto nuevo no?
- ¿Qué debería hacer el registro si encuentra un plugin incompatible: ignorarlo,
  fallar o cargarlo en modo degradado? Justifica.

### 4b. Estado: el patrón `fit`/`transform`

Si quieres reutilizar los estadísticos de un dataset en otro (por ejemplo, aplicar
a los datos de test la media calculada con los de entrenamiento), el contrato
necesita separar "aprender" de "aplicar":

```java
void fit(Matrix reference);   // calcula y guarda media/desviación
Matrix apply(Matrix input);   // usa lo aprendido
```

**Preguntas**

- ¿Dónde vive el estado: dentro del plugin o en un objeto aparte que se pasa?
- El registro descubre instancias únicas: ¿compartes esa instancia entre
  ejecuciones o pides una nueva por ejecución?
- ¿Qué pasa si se llama a `apply` sin `fit`? ¿Y si se hace `fit` con una matriz
  distinta de la que luego se transforma?
- ¿Rompe esto la inmutabilidad o la seguridad concurrente? ¿Cómo lo resolverías?

**Criterios de aceptación**: una nota breve (media página) que distinga
configuración de estado, con la política de versionado propuesta y qué se rompe
en cada escenario.

---

## 5. Dos productos con el mismo core (líneas de producto)

**Objetivo**: ver el microkernel como mecanismo de *variabilidad*.

Construye dos despliegues distintos sin tocar el core:

```bash
# Producto A: analítica básica
java -cp "core/target/classes:plugins/plugin-subtract-mean/target/*.jar" \
     es.uvigo.esei.dagss.microkernel.core.Main --list

# Producto B: analítica avanzada
java -cp "core/target/classes:plugins/*/target/*.jar" \
     es.uvigo.esei.dagss.microkernel.core.Main --list
```

**Criterios de aceptación**

- Script `scripts/run-product-a.sh` (o similar) que arranque solo con un
  subconjunto de plugins.
- Explicar en 5 líneas qué comparten y qué cambia entre A y B, y qué implicaría
  para el despliegue y el soporte.

**Preguntas**: ¿esto es una línea de producto software (SPL)? ¿Qué falta para
poder combinar plugins de forma segura sin probar todas las combinaciones?

---

## Para nota: elige un ejercicio y defiéndelo

Criterios de evaluación sugeridos:

| Criterio | Peso |
|---|---|
| El core no se modifica (salvo en los ejercicios 2 y 4, donde es el objetivo) | 30 % |
| El contrato se respeta y los tests cubren casos límite | 25 % |
| El error es accionable (mensaje + catálogo) | 15 % |
| Se razonan los trade-offs, no solo se implementa | 20 % |
| El test de integración sigue siendo reproducible | 10 % |
