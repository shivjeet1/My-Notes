# My Notes — command-line build helpers
#
# Designed for Arch Linux without Android Studio. The same targets work on any
# Linux distribution once JDK 17 and the Android command-line tools are present.

SHELL := bash
.DEFAULT_GOAL := help

# ---------------------------------------------------------------------------
# Toolchain configuration (override on the command line when needed)
# ---------------------------------------------------------------------------
JAVA_HOME ?= /usr/lib/jvm/java-17-openjdk
export JAVA_HOME
JAVA := $(JAVA_HOME)/bin/java

# Prefer ANDROID_SDK_ROOT if the caller already uses it, otherwise ~/Android/Sdk.
ANDROID_HOME ?= $(or $(ANDROID_SDK_ROOT),$(HOME)/Android/Sdk)
export ANDROID_HOME
export ANDROID_SDK_ROOT := $(ANDROID_HOME)

SDKMANAGER := $(ANDROID_HOME)/cmdline-tools/latest/bin/sdkmanager
ADB := $(shell command -v adb 2>/dev/null || printf '%s' "$(ANDROID_HOME)/platform-tools/adb")
GRADLEW := ./gradlew

# Versions are pinned to match app/build.gradle.kts (compileSdk 34, AGP 8.7.2).
SDK_PACKAGES ?= "platform-tools" "platforms;android-34" "build-tools;34.0.0"
CMDLINE_TOOLS_URL ?= https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip

APP_ID := com.shivjeet1.mynotes
MAIN_ACTIVITY := $(APP_ID)/.MainActivity
DEBUG_APK := app/build/outputs/apk/debug/app-debug.apk
RELEASE_APK := app/build/outputs/apk/release/app-release-unsigned.apk
RELEASE_AAB := app/build/outputs/bundle/release/app-release.aab
LINT_REPORT := app/build/reports/lint-results-debug.html

# ---------------------------------------------------------------------------
# Help
# ---------------------------------------------------------------------------
help: ## Show this help
	@printf 'My Notes — command-line build targets\n\n'
	@awk 'BEGIN {FS = ":.*## "}; /^[a-zA-Z0-9_-]+:.*## / {printf "  %-22s %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@printf '\nFirst run:\n  make setup\n  make doctor\n  make debug\n\n'
	@printf 'Override examples:\n  make debug JAVA_HOME=/path/to/jdk17\n  make install ANDROID_HOME=/path/to/android-sdk\n  make local-properties FORCE=1\n'

# ---------------------------------------------------------------------------
# One-time setup
# ---------------------------------------------------------------------------
setup: ## One-time: bootstrap SDK tools, accept licenses, write local.properties
	@$(MAKE) --no-print-directory sdk-bootstrap
	@$(MAKE) --no-print-directory sdk
	@$(MAKE) --no-print-directory local-properties
	@printf '\nSetup complete. Next: make doctor && make debug\n'

sdk-bootstrap: ## Download Android command-line tools into the SDK root
	@if [ -x "$(SDKMANAGER)" ]; then \
		printf '  sdkmanager already present: %s\n' "$(SDKMANAGER)"; \
		exit 0; \
	fi; \
	set -euo pipefail; \
	command -v curl >/dev/null || { printf 'curl is required (sudo pacman -S curl)\n' >&2; exit 1; }; \
	command -v unzip >/dev/null || { printf 'unzip is required (sudo pacman -S unzip)\n' >&2; exit 1; }; \
	tmp=$$(mktemp -d); \
	trap 'rm -rf "$$tmp"' EXIT; \
	printf '  downloading %s\n' "$(CMDLINE_TOOLS_URL)"; \
	curl -fL --retry 3 -o "$$tmp/cmdline-tools.zip" "$(CMDLINE_TOOLS_URL)"; \
	unzip -q "$$tmp/cmdline-tools.zip" -d "$$tmp"; \
	mkdir -p "$(ANDROID_HOME)/cmdline-tools"; \
	rm -rf "$(ANDROID_HOME)/cmdline-tools/latest"; \
	mv "$$tmp/cmdline-tools" "$(ANDROID_HOME)/cmdline-tools/latest"; \
	test -x "$(SDKMANAGER)"; \
	printf '  installed sdkmanager at %s\n' "$(SDKMANAGER)"

sdk: ## Accept SDK licenses and install platform 34 + build-tools 34
	@if [ ! -x "$(SDKMANAGER)" ]; then \
		printf 'sdkmanager not found. Run: make sdk-bootstrap\n' >&2; \
		exit 1; \
	fi
	@yes | "$(SDKMANAGER)" --licenses >/dev/null 2>&1 || true
	@"$(SDKMANAGER)" --install $(SDK_PACKAGES)

local-properties: ## Write local.properties (use FORCE=1 to overwrite)
	@if [ -f local.properties ] && [ "$(FORCE)" != "1" ]; then \
		current=$$(sed -n 's/^sdk\.dir=//p' local.properties | head -n1); \
		if [ "$$current" = "$(ANDROID_HOME)" ]; then \
			printf '  local.properties already points to %s\n' "$(ANDROID_HOME)"; \
		else \
			printf '  local.properties exists (sdk.dir=%s); leaving it unchanged\n' "$$current"; \
			printf '  run "make local-properties FORCE=1" to use %s\n' "$(ANDROID_HOME)"; \
		fi; \
	else \
		printf 'sdk.dir=%s\n' "$(ANDROID_HOME)" > local.properties; \
		printf '  wrote local.properties (sdk.dir=%s)\n' "$(ANDROID_HOME)"; \
	fi

# ---------------------------------------------------------------------------
# Build / run
# ---------------------------------------------------------------------------
debug: local-properties ## Build the debug APK
	@$(GRADLEW) assembleDebug
	@printf '  APK: %s\n' "$(DEBUG_APK)"

release: local-properties ## Build the unsigned release APK
	@$(GRADLEW) assembleRelease
	@printf '  APK: %s (sign before distributing)\n' "$(RELEASE_APK)"

bundle: local-properties ## Build the unsigned release App Bundle
	@$(GRADLEW) bundleRelease
	@printf '  AAB: %s (sign before uploading)\n' "$(RELEASE_AAB)"

test: local-properties ## Run JVM unit tests
	@$(GRADLEW) test

lint: local-properties ## Run Android lint and print the report path
	@$(GRADLEW) lintDebug
	@printf '  Report: %s\n' "$(LINT_REPORT)"

install: debug ## Install the debug APK on the connected device
	@"$(ADB)" install -r "$(DEBUG_APK)"

run: install ## Install and launch the app
	@"$(ADB)" shell am start -n "$(MAIN_ACTIVITY)"

uninstall: ## Uninstall the app from the connected device
	@"$(ADB)" uninstall "$(APP_ID)"

devices: ## List connected devices
	@"$(ADB)" devices -l

logcat: ## Stream device logcat (Ctrl+C to stop)
	@"$(ADB)" logcat

clean: ## Delete Gradle build outputs
	@$(GRADLEW) clean

# ---------------------------------------------------------------------------
# Diagnostics
# ---------------------------------------------------------------------------
doctor: ## Check JDK, SDK packages, adb, and local.properties
	@printf 'My Notes — toolchain check\n'; \
	status=0; \
	printf '  SDK root: %s\n' "$(ANDROID_HOME)"; \
	if [ -x "$(JAVA)" ]; then \
		version=$$("$(JAVA)" -version 2>&1 | head -n1); \
		major=$$(printf '%s\n' "$$version" | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n1); \
		printf '  Java: %s\n' "$$version"; \
		if [ -n "$$major" ] && [ "$$major" -ge 17 ] && [ "$$major" -le 22 ]; then \
			printf '  [ok]   JDK major version %s is supported\n' "$$major"; \
		else \
			printf '  [fail] JDK major version %s; AGP 8.7 / Gradle 8.9 need JDK 17-22\n' "$${major:-unknown}"; \
			printf '         sudo pacman -S jdk17-openjdk && sudo archlinux-java set java-17-openjdk\n'; \
			status=1; \
		fi; \
	else \
		printf '  [fail] JDK not found at %s\n' "$(JAVA)"; \
		printf '         sudo pacman -S jdk17-openjdk\n'; \
		status=1; \
	fi; \
	if [ -x "$(SDKMANAGER)" ]; then \
		printf '  [ok]   sdkmanager found\n'; \
	else \
		printf '  [fail] sdkmanager missing (run: make sdk-bootstrap)\n'; \
		status=1; \
	fi; \
	if [ -d "$(ANDROID_HOME)/platforms/android-34" ]; then \
		printf '  [ok]   Android platform 34 installed\n'; \
	else \
		printf '  [fail] Android platform 34 missing (run: make sdk)\n'; \
		status=1; \
	fi; \
	if [ -d "$(ANDROID_HOME)/build-tools/34.0.0" ]; then \
		printf '  [ok]   Build-tools 34.0.0 installed\n'; \
	else \
		printf '  [fail] Build-tools 34.0.0 missing (run: make sdk)\n'; \
		status=1; \
	fi; \
	if [ -x "$(ADB)" ]; then \
		printf '  [ok]   adb found: %s\n' "$(ADB)"; \
	else \
		printf '  [warn] adb not found; install android-tools or run make sdk\n'; \
	fi; \
	if [ -f local.properties ] && grep -q '^sdk\.dir=' local.properties; then \
		printf '  [ok]   local.properties configured\n'; \
	else \
		printf '  [warn] local.properties missing (run: make local-properties)\n'; \
	fi; \
	if [ -x "$(GRADLEW)" ]; then \
		printf '  [ok]   Gradle wrapper present\n'; \
	else \
		printf '  [fail] ./gradlew is not executable\n'; \
		status=1; \
	fi; \
	printf '\n'; \
	if [ "$$status" -eq 0 ]; then \
		printf 'All required checks passed. Run: make debug\n'; \
	else \
		printf 'Fix the items marked [fail], then re-run: make doctor\n'; \
	fi; \
	exit $$status

# ---------------------------------------------------------------------------
.PHONY: help setup sdk-bootstrap sdk local-properties debug release bundle \
        test lint install run uninstall devices logcat clean doctor
