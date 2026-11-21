APP_ID = design.antics.mathinvaders

SRC := $(shell find src -name "*.java")

OUT = out
CLASSES = $(OUT)/classes
DEX = $(OUT)/classes.dex
RESZIP = $(OUT)/compiled_res.zip

UNSIGNED = unsigned.apk
ALIGNED = aligned.apk

# Plain '=' (not '?=') so a stale exported ANDROID_JAR can't drag the build
# below the target SDK. `make ANDROID_JAR=...` on the CLI still overrides.
ANDROID_JAR = /usr/lib/android-sdk/platforms/android-33/android.jar
AAPT2 = /usr/lib/android-sdk/build-tools/34.0.0/aapt2

MIN_SDK = 24
TARGET_SDK = 33

KEYSTORE = debug.keystore

# Hardcoded device addresses go stale (the tablet is on DHCP), so pick the
# first device adb already has. Override with `make run DEVICE=<ip>:<port>`
# or by exporting ANDROID_DEVICE.
ADB = ../platform-tools/adb
DEVICE ?= $(or $(ANDROID_DEVICE),$(shell $(ADB) devices | awk 'NR>1 && $$2=="device" {print $$1; exit}'))

all: $(ALIGNED)

$(DEX): $(SRC)
	rm -rf $(CLASSES)
	mkdir -p $(CLASSES)
	javac -source 8 -target 8 -classpath $(ANDROID_JAR) -d $(CLASSES) $(SRC)
	d8 --lib $(ANDROID_JAR) --output $(OUT) $$(find $(CLASSES) -name "*.class")

$(RESZIP): $(shell find res -type f)
	mkdir -p $(OUT)
	$(AAPT2) compile --dir res -o $(RESZIP)

$(UNSIGNED): $(DEX) $(RESZIP) AndroidManifest.xml
	$(AAPT2) link \
		-I $(ANDROID_JAR) \
		--manifest AndroidManifest.xml \
		--min-sdk-version $(MIN_SDK) \
		--target-sdk-version $(TARGET_SDK) \
		$(RESZIP) \
		-o $(UNSIGNED)
	zip -j -u $(UNSIGNED) $(DEX)

# -f overwrites an existing aligned.apk; without it a rebuild fails once one
# exists.
$(ALIGNED): $(UNSIGNED)
	zipalign -f -p 4 $(UNSIGNED) $(ALIGNED)
	apksigner sign \
		--ks $(KEYSTORE) \
		--ks-pass pass:android \
		--key-pass pass:android \
		$(ALIGNED)

install: $(ALIGNED)
	@test -n "$(DEVICE)" || { echo "No device: connect one, or pass DEVICE=<ip>:<port>"; exit 1; }
	$(ADB) connect $(DEVICE) || true
	$(ADB) -s $(DEVICE) install -r $(ALIGNED)

run: install
	$(ADB) -s $(DEVICE) shell am start -n $(APP_ID)/.MainActivity

keystore:
	keytool -genkeypair \
		-keystore $(KEYSTORE) \
		-alias androiddebugkey \
		-keyalg RSA \
		-keysize 2048 \
		-validity 10000 \
		-storepass android \
		-keypass android \
		-dname "CN=Android Debug,O=Android,C=US"

# Regenerate launcher icons from icon.png (legacy square for API 24-25, plus
# adaptive foreground at 62% so the art survives the circular/squircle mask;
# the background colour in res/values/colors.xml matches the art's edge).
DENSITIES = mdpi:48:108 hdpi:72:162 xhdpi:96:216 xxhdpi:144:324 xxxhdpi:192:432
# icon.png is rasterized from icon.svg (the source of truth)
icon.png: icon.svg
	convert -density 96 -background '#1F2154' icon.svg -resize 1024x1024 -alpha remove -alpha off PNG24:icon.png

icons: icon.png
	@for d in $(DENSITIES); do \
		n=$${d%%:*}; r=$${d#*:}; legacy=$${r%%:*}; fg=$${r#*:}; \
		mkdir -p res/mipmap-$$n; \
		convert icon.png -resize $${legacy}x$${legacy} res/mipmap-$$n/ic_launcher.png; \
		convert -size $${fg}x$${fg} xc:'#1F2154' \( icon.png -resize $$((fg*62/100))x$$((fg*62/100)) \) \
			-gravity center -composite res/mipmap-$$n/ic_launcher_foreground.png; \
	done

logcat:
	$(ADB) -s $(DEVICE) logcat --pid=$$($(ADB) -s $(DEVICE) shell pidof -s $(APP_ID))

clean:
	rm -rf out *.apk *.apk.idsig

.PHONY: all install run keystore icons logcat clean
