# JaCoCo Rules

Human-maintained coverage policy lives in these text files. The Gradle script
loads them at configuration time and fails fast on missing files, duplicate
rules, backslash separators, or source/class-pattern mixups.

Format:

- One rule per line.
- Blank lines and lines starting with `#` are ignored.
- `*-class-patterns.txt` files use Gradle `fileTree` patterns against compiled class directories.
- `*-sources.txt` files are paths relative to `src/com/android/messaging/ui`.

The script still owns generated synthetic-class patterns and source-to-class pattern derivation.
Keep only human-maintained coverage policy here.
