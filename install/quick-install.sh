#!/bin/bash
#
# AI Minecraft Players - Quick Install Script
# ============================================
#
# This script helps you quickly install AI Minecraft Players on your server.
#

set -e

echo "=========================================="
echo "AI Minecraft Players - Quick Installer"
echo "=========================================="
echo ""

# Check if running as root
if [ "$EUID" -eq 0 ]; then
    echo "⚠️  Warning: Running as root. Consider using a regular user for Minecraft servers."
    echo ""
fi

# Get server directory
read -p "Enter your Minecraft server directory [/opt/minecraft]: " SERVER_DIR
SERVER_DIR=${SERVER_DIR:-/opt/minecraft}

if [ ! -d "$SERVER_DIR" ]; then
    echo "❌ Error: Directory $SERVER_DIR does not exist!"
    echo "Please create your Minecraft server directory first."
    exit 1
fi

echo ""
echo "📁 Server directory: $SERVER_DIR"

# Check if mods directory exists
if [ ! -d "$SERVER_DIR/mods" ]; then
    echo "⚠️  Warning: $SERVER_DIR/mods directory not found!"
    read -p "Create mods directory? (y/n): " CREATE_MODS
    if [ "$CREATE_MODS" = "y" ]; then
        mkdir -p "$SERVER_DIR/mods"
        echo "✅ Created $SERVER_DIR/mods"
    else
        echo "❌ Cannot continue without mods directory."
        exit 1
    fi
fi

# Check if config directory exists
if [ ! -d "$SERVER_DIR/config" ]; then
    mkdir -p "$SERVER_DIR/config"
    echo "✅ Created $SERVER_DIR/config"
fi

# Find JAR file
JAR_FILE=$(ls ai-minecraft-player-*.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "❌ Error: Could not find ai-minecraft-player-*.jar in current directory!"
    echo "Please ensure the mod JAR file is in the install directory."
    exit 1
fi

echo "📦 Found mod: $JAR_FILE"

# Copy JAR file
echo ""
echo "Installing mod..."
cp "$JAR_FILE" "$SERVER_DIR/mods/"
echo "✅ Copied mod to $SERVER_DIR/mods/"

# Copy config
echo ""
echo "Installing configuration..."
if [ -f "$SERVER_DIR/config/aiplayer-config.json" ]; then
    echo "⚠️  Configuration file already exists!"
    read -p "Backup and replace? (y/n): " REPLACE_CONFIG
    if [ "$REPLACE_CONFIG" = "y" ]; then
        cp "$SERVER_DIR/config/aiplayer-config.json" "$SERVER_DIR/config/aiplayer-config.json.backup"
        echo "✅ Backed up existing config to aiplayer-config.json.backup"
        cp config/aiplayer-config.json "$SERVER_DIR/config/"
        echo "✅ Installed new configuration"
    else
        echo "⏭️  Skipped config installation"
    fi
else
    cp config/aiplayer-config.json "$SERVER_DIR/config/"
    echo "✅ Installed configuration to $SERVER_DIR/config/"
fi

# Copy role presets
if [ -d config/roles ]; then
    mkdir -p "$SERVER_DIR/config/roles"
    cp -r config/roles/* "$SERVER_DIR/config/roles/"
    echo "✅ Installed personality role presets to $SERVER_DIR/config/roles/"
fi

echo ""
echo "=========================================="
echo "✅ Installation Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo ""
echo "1. Configure your LLM provider:"
echo "   Edit: $SERVER_DIR/config/aiplayer-config.json"
echo ""
echo "   For OpenAI:"
echo "     - Set 'provider' to 'openai'"
echo "     - Set 'apiKey' to your OpenAI API key"
echo ""
echo "   For local Ollama (FREE):"
echo "     - Install Ollama: curl -fsSL https://ollama.com/install.sh | sh"
echo "     - Run: ollama pull mistral && ollama serve &"
echo "     - Set 'provider' to 'local'"
echo ""
echo "   For Simple Mode (no LLM):"
echo "     - Leave 'apiKey' empty"
echo ""
echo "2. Start your Minecraft server:"
echo "   cd $SERVER_DIR"
echo "   ./start.sh   (or however you start your server)"
echo ""
echo "3. In-game, spawn an AI player:"
echo "   /aiplayer spawn AISteve"
echo ""
echo "4. Talk to your AI:"
echo "   @AISteve hello!"
echo ""
echo "For detailed documentation, see:"
echo "  - docs/README.md (features and quick start)"
echo "  - docs/INSTALL.md (detailed installation guide)"
echo "  - docs/LLM_SETUP.md (LLM configuration details)"
echo ""
echo "Need help? Visit:"
echo "  https://github.com/your-username/AI_Minecraft_Players/issues"
echo ""
echo "Happy AI Minecrafting! 🤖⛏️"
echo ""
