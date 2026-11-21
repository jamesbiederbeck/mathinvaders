# Math Invaders

A question sits at the top and four invaders each carry a candidate answer. Drag to aim the cannon and release to fire one shot. The invaders descend toward the red line (default 10% of the fall per second; adjustable in the parents menu). Of the three wrong ones, one parks at 25%, one at 50% and one at 75%; the right one keeps falling. Shoot it for the next question; if it reaches the line you lose one of three lives. For primary-school learners.

Built with Make — no Gradle, no IDE. Package `design.antics.mathinvaders`,
minSdk 24 / targetSdk 33.

## Dependencies

```
sudo apt install \
  google-android-platform-33-installer \
  google-android-build-tools-34.0.0-installer \
  openjdk-17-jdk \
  apksigner \
  zipalign
```

`adb` comes from `../platform-tools/adb`.

## Build

```bash
make keystore              # one-time: generate debug.keystore
make                       # build signed APK (aligned.apk)
make install               # build + adb install
make run                   # install + launch
make logcat                # tail this app's log
make clean
```

`DEVICE` auto-detects the first device `adb` sees. Override with
`make run DEVICE=<ip>:<port>` or by exporting `ANDROID_DEVICE`.

## Problems and parents menu

Questions come from the same pack system as `blocksubtractor` (`Packs.java`, 23
packs from tiny addition to times tables, positive numbers only, some with a
blank such as `8 + ? = 14`). Hamburger top-left -> PIN (default **1234**) -> toggle
packs, reset the best score, change the PIN. Shared look: see `DESIGN.md`.
