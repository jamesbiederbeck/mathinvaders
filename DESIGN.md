# Candy Arcade - shared look for the kid games

Used by `blocksubtractor`, `lettersnake` and `mathinvaders`. There is no shared
code in this tree, so `Theme.java` and `ParentsMenu.java` are copied into each
app (only the `package` line differs). Change them in all three together.

## Look
- **Palette** (from the Block Subtractor icon): navy `#1F2154` background,
  lighter navy `#2E3170` for surfaces; tiles coral `#FF6B4A`, sunshine `#FFC31F`,
  violet `#A96CF5`; mint `#5BF7C0` for the player and "success"; cream `#FFF6E5`
  for balls, bullets and highlights. White text on navy, navy text on tiles.
- **Shapes**: flat, fully rounded tiles (corner radius = 30% of the short side),
  no outlines, no gradients. One lighter arc ("gloss") in the top-left corner.
- **Type**: `sans-serif-black`, big and centred. Numbers and letters are the hero.
- **Motion**: hits "pop" into a few small squares in the tile's colour; moving
  balls leave a short fading trail. No screen shake, no sound.
- **HUD**: hamburger top-left, then `Score n`; a centre label; `Best n` on the right.
- **Icons**: navy field, 2-3 glossy tiles, one hero object. Hand-written
  `icon.svg` -> `icon.png` -> `make icons` (legacy + adaptive mipmaps).

## Behaviour rules (so the games feel like one family)
- **Pace is gentle and parent-tunable.** Block Subtractor and Letter Snake pace
  themselves from the player's actions (a wrong letter) or a slow tick. Math
  Invaders descends at a parents-menu speed (default 10% of the fall per second);
  wrong invaders park at 25/50/75%, so only the right answer can reach the line.
- **Parents menu** is the same everywhere: hamburger -> 4-digit PIN (default
  1234, a speed bump only) -> pack toggles (at least one stays on), reset best
  score, change PIN. Opening it pauses the game.
- **Packs** are the difficulty control. Parents toggle them; each question or
  word is drawn from a random enabled pack. Packs live in `Packs.java` and expose
  `NAMES`, `EXAMPLES`, `COUNT`, `ALL`, `DEFAULT_MASK`.
- **All numbers are positive** (>= 1), including blanks, results and decoys.
- Portrait, fullscreen, immersive. No permissions, no network.
