#!/usr/bin/env bash
#
# Test de integración mínimo, sin frameworks: compila, comprueba que se descubren
# los tres plugins, ejecuta un pipeline y compara con datos/esperado.csv.
#
set -euo pipefail

cd "$(dirname "$0")/.."

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

echo "[1/4] Compilando y ejecutando los tests..."
mvn -q package

echo "[2/4] Comprobando el catálogo de plugins..."
./run.sh --list -v > "$tmp/list.txt"

for id in subtract-mean minmax standardize; do
    grep -q "$id" "$tmp/list.txt" || {
        echo "ERROR: no se ha descubierto el plugin '$id'" >&2
        exit 1
    }
done

for jar in plugin-subtract-mean plugin-minmax plugin-standardize; do
    grep -q "$jar" "$tmp/list.txt" || {
        echo "ERROR: no aparece el JAR '$jar' como origen de ningún plugin" >&2
        exit 1
    }
done

echo "[3/4] Ejecutando pipeline y comparando con datos/esperado.csv..."
./run.sh --run subtract-mean,standardize datos/ejemplo.csv "$tmp/salida.csv" > /dev/null
diff -u datos/esperado.csv "$tmp/salida.csv"

echo "[4/4] Comprobando el error con un plugin inexistente..."
if ./run.sh --run no-existe datos/ejemplo.csv > "$tmp/error.txt" 2>&1; then
    echo "ERROR: el comando debía fallar y ha terminado con éxito" >&2
    exit 1
fi
grep -q "No existe el plugin" "$tmp/error.txt"
grep -q "subtract-mean" "$tmp/error.txt"

echo
echo "OK: compilación, descubrimiento, pipeline y errores verificados."
