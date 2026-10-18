#!/bin/sh
# Builds Parpt and installs it for the current user, so it can be started by typing `parpt`.
#
# The jar is copied to $PARPT_INSTALL_DIR (default: ~/.local/share/parpt) and a `parpt`
# launcher is written to $PARPT_BIN_DIR (default: ~/.local/bin). Projects are saved to
# ~/.parpt/projects.json, whichever directory Parpt is started from.
#
# Usage: ./install.sh
set -eu

SOURCE_DIR=$(cd "$(dirname "$0")" && pwd)
INSTALL_DIR=${PARPT_INSTALL_DIR:-"$HOME/.local/share/parpt"}
BIN_DIR=${PARPT_BIN_DIR:-"$HOME/.local/bin"}

if ! command -v java >/dev/null 2>&1; then
    echo "Java was not found on PATH. Install JDK 21 and run this script again." >&2
    exit 1
fi

echo "Building Parpt..."
"$SOURCE_DIR/gradlew" -p "$SOURCE_DIR" bootJar

# bootJar writes only the runnable jar, but a plain jar left by an earlier `./gradlew build`
# may sit beside it and cannot be run on its own.
JAR=""
for candidate in "$SOURCE_DIR"/build/libs/Parpt-*.jar; do
    case "$candidate" in
        *-plain.jar) ;;
        *) [ -f "$candidate" ] && JAR="$candidate" ;;
    esac
done
if [ -z "$JAR" ]; then
    echo "No runnable jar was found in $SOURCE_DIR/build/libs after the build." >&2
    exit 1
fi

mkdir -p "$INSTALL_DIR" "$BIN_DIR"
cp "$JAR" "$INSTALL_DIR/parpt.jar"

LAUNCHER="$BIN_DIR/parpt"
cat > "$LAUNCHER" <<EOF
#!/bin/sh
exec java -jar "$INSTALL_DIR/parpt.jar" "\$@"
EOF
chmod +x "$LAUNCHER"

echo "Installed $(basename "$JAR") to $INSTALL_DIR/parpt.jar"
echo "Installed the launcher to $LAUNCHER"

case ":$PATH:" in
    *":$BIN_DIR:"*) echo "Start Parpt by typing: parpt" ;;
    *)
        echo "$BIN_DIR is not on your PATH. Add this line to your shell profile (e.g. ~/.bashrc):"
        echo "    export PATH=\"$BIN_DIR:\$PATH\""
        echo "Then start Parpt by typing: parpt"
        ;;
esac
