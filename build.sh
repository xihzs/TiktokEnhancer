#!/usr/bin/env bash
# ==============================================================================
# TikTok Enhancer - Build Script (DexKit 2.3.0 Engine + Android APK)
# ==============================================================================
set -e

BOLD='\033[1m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
GRAY='\033[0;90m'
NC='\033[0m'

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

BUILD_DEBUG=true
BUILD_RELEASE=false
DO_CLEAN=false
DO_INSTALL=false

show_help() {
    echo -e "${BOLD}Usage:${NC} ./build.sh [OPTIONS]"
    echo ""
    echo -e "${BOLD}Build Modes:${NC}"
    echo "  --debug            Build Debug APK [default]"
    echo "  --release          Build Release APK"
    echo "  --all              Build both Debug and Release APKs"
    echo ""
    echo -e "${BOLD}Options:${NC}"
    echo "  --clean            Clean build caches before compiling"
    echo "  --install          Install the built APK onto connected device via ADB"
    echo "  -h, --help         Show this help message"
    echo ""
    echo -e "${BOLD}Examples:${NC}"
    echo "  ./build.sh                  # Build Debug APK"
    echo "  ./build.sh --release        # Build Release APK"
    echo "  ./build.sh --all            # Build both Debug and Release APKs"
    echo "  ./build.sh --install        # Compile and install directly to device via ADB"
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --debug)
            BUILD_DEBUG=true
            BUILD_RELEASE=false
            shift
            ;;
        --release)
            BUILD_DEBUG=false
            BUILD_RELEASE=true
            shift
            ;;
        --all)
            BUILD_DEBUG=true
            BUILD_RELEASE=true
            shift
            ;;
        --clean)
            DO_CLEAN=true
            shift
            ;;
        --install)
            DO_INSTALL=true
            shift
            ;;
        -h|--help)
            show_help
            ;;
        *)
            echo -e "${RED}[ERROR] Unknown option: $1${NC}"
            show_help
            ;;
    esac
done

echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}${BOLD}  TikTok Enhancer - DexKit 2.3.0 Build Pipeline                   ${NC}"
echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GRAY}Target: DexKit 2.3.0 (Native C++ Dex Scan) • No Rust engine needed${NC}"
echo ""

if [ "$DO_CLEAN" = true ]; then
    echo -e "${YELLOW}Cleaning build caches...${NC}"
    "$DIR/gradlew" clean
    echo -e "${GREEN}[OK] Clean complete${NC}"
    echo ""
fi

# Build Debug
if [ "$BUILD_DEBUG" = true ]; then
    echo -e "${BLUE}${BOLD}[1/2] Building Debug APK...${NC}"
    "$DIR/gradlew" assembleDebug
    DEBUG_APK="$DIR/app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$DEBUG_APK" ]; then
        SIZE=$(du -h "$DEBUG_APK" | cut -f1)
        echo -e "${GREEN}[OK] Debug APK built successfully:${NC} $DEBUG_APK (${SIZE})"
    fi
fi

# Build Release
if [ "$BUILD_RELEASE" = true ]; then
    echo -e "${BLUE}${BOLD}[2/2] Building Release APK...${NC}"
    "$DIR/gradlew" assembleRelease
    if [ -f "$DIR/app/build/outputs/apk/release/app-release.apk" ]; then
        RELEASE_APK="$DIR/app/build/outputs/apk/release/app-release.apk"
    else
        RELEASE_APK="$DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
    fi
    if [ -f "$RELEASE_APK" ]; then
        SIZE=$(du -h "$RELEASE_APK" | cut -f1)
        echo -e "${GREEN}[OK] Release APK built successfully:${NC} $RELEASE_APK (${SIZE})"
    fi
fi

# Install via ADB if requested
if [ "$DO_INSTALL" = true ]; then
    APK_TO_INSTALL="$DEBUG_APK"
    if [ "$BUILD_RELEASE" = true ] && [ "$BUILD_DEBUG" = false ]; then
        APK_TO_INSTALL="$RELEASE_APK"
    fi

    if [ -f "$APK_TO_INSTALL" ]; then
        echo ""
        echo -e "${CYAN}Installing onto connected device via ADB...${NC}"
        adb install -r "$APK_TO_INSTALL"
        echo -e "${GREEN}[OK] Installed successfully on device!${NC}"
    else
        echo -e "${RED}[ERROR] Target APK not found for install: $APK_TO_INSTALL${NC}"
        exit 1
    fi
fi

echo ""
echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}${BOLD}  Build finished successfully!                                    ${NC}"
echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
