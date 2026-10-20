#!/bin/bash
# Extract @Table + @Column names from legacy JPA entities
cd /mnt/d/ihms/src/main/java/com/ohum/models || exit 1
for f in "$@"; do
  echo "=== $f ==="
  table=$(grep -oE '@Table\(name *= *"[^"]*"\)' "$f" | head -1)
  echo "  $table"
  grep -oE '@Column\(name *= *"[^"]*"\)' "$f" | sed -E 's/@Column\(name *= *"([^"]*)"\)/\1/' | tr '\n' ',' | sed 's/,/, /g'
  echo
done
