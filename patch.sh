#!/usr/bin/env bash
# Использование: ./patch.sh module.apk base.apk [split1.apk split2.apk ...]
# Нужен lspatch.jar рядом (https://github.com/JingMatrix/LSPatch/releases) и Java 17+.
# Результат: папка out/ с пропатченным TikTok (root не нужен).
set -euo pipefail
MODULE="${1:?укажите APK модуля}"; shift
[ $# -ge 1 ] || { echo "укажите оригинальный TikTok APK (base + splits)"; exit 1; }
[ -f lspatch.jar ] || { echo "положите lspatch.jar рядом со скриптом"; exit 1; }
mkdir -p out
# -l 2 = обход проверки подписи (TikTok её проверяет), -f = перезаписать
java -jar lspatch.jar "$@" -m "$MODULE" -l 2 -f -o out
echo "Готово. Сначала удалите оригинальный TikTok, потом:"
echo "  adb install-multiple out/*.apk"
