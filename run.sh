#!/usr/bin/env bash
#
# Arranca el core con los plugins empaquetados en plugins/*/target/*.jar.
#
# El classpath es la clave de la demo: el core está en core/target/classes y cada
# plugin entra como un JAR independiente. Si un JAR no está en el classpath, el
# plugin simplemente no existe para la aplicación.
#
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -d core/target/classes ]]; then
    echo "Error: el proyecto no está compilado." >&2
    echo "Ejecuta primero: mvn package" >&2
    exit 1
fi

CP="core/target/classes"

shopt -s nullglob
jars=(plugins/*/target/*.jar)
shopt -u nullglob

if (( ${#jars[@]} == 0 )); then
    echo "Aviso: no hay JARs de plugins en plugins/*/target/*.jar." >&2
    echo "Ejecuta 'mvn package' si esperabas ver plugins." >&2
fi

for jar in "${jars[@]}"; do
    CP="$CP:$jar"
done

exec java -cp "$CP" es.uvigo.esei.dagss.microkernel.core.Main "$@"
