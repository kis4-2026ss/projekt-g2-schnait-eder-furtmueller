# Technische Dokumentation – „The_game_no_name"

> Hinweis für Mitwirkende: Dieses Projekt entstand 2015, ist gegen ein sehr altes Android‑SDK gebaut und enthält in vielen Stellen ungewöhnliche oder fragwürdige Konstrukte (globale Statics, hartkodierte Magic Numbers, leere `catch`‑Blöcke, halbgare `synchronized`‑Konstrukte, vertauschte X/Y‑Koordinaten, kyrillische Kommentare etc.). Diese Dokumentation beschreibt den **Ist‑Zustand** des Codes – nicht, wie er aussehen sollte. Wo das Verhalten tatsächlich von der Benennung abweicht oder wo Fallstricke lauern, wird darauf hingewiesen.

---

## 1. Projektübersicht

**Typ:** Android‑Anwendung (Java).
**Genre:** Rundenbasierter Roguelike‑/Dungeon‑Crawler mit Tile‑Map, Sichtfeld („Fog of War"), Inventar, Stats, Stairways zwischen Dungeon‑Ebenen, Nahkampf gegen Goblins.
**Build:** Gradle (`com.android.application`), `compileSdkVersion 21`, `minSdkVersion 15`, `targetSdkVersion 21`, `buildToolsVersion 21.1.2`. Einzige externe Abhängigkeit: `com.android.support:appcompat-v7:21.0.3`.
**Package:** `com.example.framaz1.myapplication`.
**Einstiegspunkt:** `MainActivity` zeigt einen Start‑Button; Klick startet `MainGameActivity`, das die Spielfläche aufbaut und den `GameThread` startet.

### Laufzeitarchitektur (drei Threads + UI)

```
                   ┌──────────────────┐
                   │   MainActivity   │  (Start‑Screen, Button)
                   └────────┬─────────┘
                            │ startActivity
                            ▼
                   ┌──────────────────┐
                   │ MainGameActivity │  (statisch sf : MySurfaceView)
                   └────────┬─────────┘
                            │
        ┌───────────────────┼───────────────────────┐
        ▼                   ▼                       ▼
   GameThread         DrawThread                AnimationThread
  (Game.gaming)   (Dauerschleife: zeichnet    (Punktuelle Anim für
   Spiellogik,    Tiles + Akteure + Menü      Bewegung/Angriff,
   Gegner‑KI,     in MySurfaceView)           blockiert DrawThread
   Pathfinding,                                 währenddessen)
   FoV)
        │                   │                       │
        └────── Sync‑Objekte: TouchAndThreadParams.{outStreamSync,
                                                    gameSync,
                                                    animationSync}
```

Spielzustand wird über statische Felder von `Game`, `Params`, `TouchAndThreadParams` und `AllBitmaps` geteilt (klassischer „God‑State"‑Stil – keine Kapselung). Synchronisation läuft über drei Object‑Locks und `wait()/notifyAll()`. Touch‑Eingaben kommen von `MySurfaceView.onTouchEvent` und werden in dieselben Statics geschrieben.

### Wichtigste Designentscheidungen / Patterns

* **Globaler Singleton‑artiger Zustand**: Sämtliche Spielobjekte (Karte, Spieler, Bitmaps, Display‑Offset, Touch‑Position, UI‑Modus‑Flags) liegen als `public static`‑Felder in den Klassen `Game`, `Params`, `TouchAndThreadParams`, `AllBitmaps`. Es gibt keine DI oder Kapselung.
* **Vorlagenklassen statt Interfaces** („Mother…" als Basisklasse): `MotherCreature`, `MotherItem`, `Mothermap` sind konkrete (nicht abstrakte) Basisklassen, die per Vererbung spezialisiert werden. Polymorphismus wird genutzt für `behavior()`, `move()`, `equip()`/`unEquip()`, `toInteract()`, `generateTheField()`.
* **Drawing‑IDs statt Bitmap‑Referenzen**: Tiles und Creatures speichern eine `int drawId`; `AllBitmaps.getPictureById(int)` mappt diese ID auf das aktuell skalierte Bitmap. So muss bei einem Resize nur der zentrale Bitmap‑Cache aktualisiert werden.
* **„String‑Commands" statt Enums**: Touch‑Auswertung (`getWhatPlayerDesired()`) und Pathfinding‑Richtungen verwenden Strings (`"Move"`, `"Inventory"`, `"Up"`, `"DownLeft"`, …) als Befehlskanal. Der `behavior()`‑Switch in `PlayCreature` reagiert darauf.
* **Cooperative Turn‑Taking** über `toWait`: Statt Echtzeit hat jede Kreatur einen Cooldown `toWait`. Pro Tick wird er heruntergezählt; bei 0 wird `behavior()` aufgerufen. So ergeben sich unterschiedliche Geschwindigkeiten je Aktion (Bewegung/Angriff/Waffenverzögerung).
* **BFS‑Pathfinding** auf 100×100‑Gitter mit 8‑Nachbarn (rückwärts vom Ziel, Distanzfeld in `Game.pathFindingHelpArray`, anschließendes Gradienten‑Walken vom Start zum Ziel).
* **Raycast‑Sichtfeld**: `Game.fieldOfView()` schießt Linien vom Spieler in die vier Kantenrichtungen des 100×100‑Gitters (Bresenham‑artig auf Basis von `y = k·x + b`) und stoppt an Wänden.

### Bekannte Auffälligkeiten (nicht Bugs, die behoben werden sollen – nur Hinweise zum Lesen des Codes)

* **Vertauschte Koordinaten**: Vielerorts wird `field[i][j]` mit `i = yCoord` und `j = xCoord` indiziert, an anderen Stellen aber mit vertauschten Indizes (`field[xCoordinates][yCoordinates]`). Insbesondere `MotherCreature.die()` und `Game.placeCreature(...)` schreiben `isMobHere` an die gespiegelte Position.
* **`displayX` / `displayY` sind invertiert** zur Erwartung: `displayX` entspricht der vertikalen Pixel‑Verschiebung (Höhe), `displayY` der horizontalen.
* **`whereToGoX` ist screenY**, `whereToGoY` ist screenX (siehe `MySurfaceView.onTouchEvent`).
* **Leere `catch`‑Blöcke** verstecken Fehler in `Game.gaming()` und an mehreren `wait()`‑Stellen.
* **Falsch gewichtete Bedingung** in `Game.pathFinding`: An einer Stelle wird `&` (bitweise) statt `&&` (logisch) verwendet.

---

## 2. Projektstruktur

```
The_game_no_name-master/
├── build.gradle, settings.gradle, gradle/...   # Top‑Level Build
└── app/
    ├── build.gradle                            # SDK‑Versionen, deps
    └── src/main/
        ├── AndroidManifest.xml                 # 2 Activities (siehe §3.1)
        ├── res/                                # drawable/, layout/, values/
        └── java/com/example/framaz1/myapplication/
            ├── MainActivity.java               # Startbildschirm
            ├── MainGameActivity.java           # Hostet das SurfaceView
            ├── GameThread.java                 # Thread‑Wrapper für Game.gaming()
            ├── Game.java                       # Spiellogik‑Singleton (statisch)
            ├── Params.java                     # UI‑Modus‑Flags + Display‑Offset
            ├── TouchAndThreadParams.java       # Sync‑Locks + Drawing‑Helfer
            ├── AllBitmaps.java                 # Bitmap‑Cache, Skalierung
            ├── Creatures/
            │   ├── MotherCreature.java         # Basisklasse aller Kreaturen
            │   ├── PlayCreature.java           # Spieler
            │   └── Goblin.java                 # Gegner
            ├── Items/
            │   ├── MotherItem.java             # Basis‑Item
            │   ├── EmptyItem.java              # Null‑Object für leere Slots
            │   ├── ItemType.java               # enum
            │   ├── RandomItem.java             # (leer)
            │   └── Weapons/
            │       ├── MeeleWeapon.java
            │       ├── RangedWeapon.java
            │       └── Meeles/WoodenSword.java
            ├── Helpers/
            │   ├── AttackHelper.java           # Datentransfer‑Objekt für Angriffe
            │   └── FindingHelper.java          # Koordinatentupel für BFS
            ├── Tiles/
            │   ├── Tile.java                   # Basis‑Tile
            │   ├── Floor.java, Wall.java
            │   ├── UpStairway.java, DownStairway.java
            │   └── StandardDungeon/
            │       ├── DungeonFloor.java
            │       ├── DungeonWall.java
            │       ├── DungeonUpStairway.java
            │       └── DungeonDownStairway.java
            ├── Map_etc/
            │   ├── Mothermap.java              # Basis‑Karte
            │   ├── RoomHelper.java             # Rechteck‑Tupel für Räume
            │   └── DungeonTypes/
            │       ├── ShallowDungeon.java     # Prozedurale Generierung
            │       └── TestDungeon.java        # Debug‑Karte
            └── Outstreams/
                ├── MySurfaceView.java          # SurfaceView + Touch
                ├── DrawThread.java             # Render‑Loop
                └── AnimationThread.java        # Move-/Attack‑Animationen
```

---

## 3. Module / Dateien im Detail

### 3.1 `MainActivity.java`

**Zweck:** Startbildschirm. Initialisiert `Params.resource` mit `getResources()`, damit `AllBitmaps` (statisch‑initialisiert) auf Drawables zugreifen kann, sobald es das erste Mal geladen wird.

* **Klasse `MainActivity extends Activity`**

  * Felder: `Button bt` – der einzige Button im Layout `activity_main`.
  * `onCreate(Bundle)`: setzt `FEATURE_NO_TITLE`, erzwingt Portrait, lädt das Layout, hängt einen `OnClickListener` an `bt`, der eine Intent‑Navigation zu `MainGameActivity` startet. **Seiteneffekt:** `Params.resource` wird gesetzt – ohne diesen Aufruf würde `AllBitmaps` mit NPE laden.
  * `onCreateOptionsMenu(Menu)` / `onOptionsItemSelected(MenuItem)`: Standard‑Boilerplate, nutzt `R.menu.menu_main`.

> Auffällig: Im `AndroidManifest.xml` ist auch `MainGameActivity` als `LAUNCHER` markiert, d. h. es erscheinen zwei App‑Icons im Launcher.

---

### 3.2 `MainGameActivity.java`

**Zweck:** Hostet die Spielfläche. Erzeugt `MySurfaceView`, startet den `GameThread` und verbindet einfache Klicks mit dem Spielzustand.

* **Klasse `MainGameActivity extends Activity`**

  * Felder:
    * `GameThread gameThread` – der Logik‑Thread (siehe §3.3).
    * `Bitmap bitmap` – ungenutzt.
    * `public static MySurfaceView sf` – globale Referenz, damit andere Klassen (z. B. `MotherCreature`) den `AnimationThread` über `MainGameActivity.sf.animThread` ansprechen können.
  * `onCreate(Bundle)`:
    1. Kein Titel, Portrait, Vollbild.
    2. Erzeugt `sf = new MySurfaceView(this)` und setzt es als Content View.
    3. Liest `DisplayMetrics` in `Params.displaySettings`.
    4. Startet `gameThread`.
    5. Setzt einen `OnClickListener` auf `sf`, der die letzten Touch‑Koordinaten in `Game.whereToGoX/Y` kopiert und `TouchAndThreadParams.gameSync.notifyAll()` aufruft, um den im `gameSync` schlafenden `GameThread`/`PlayCreature.behavior()` zu wecken.

> Hinweis: `MySurfaceView` hat parallel sein eigenes `onTouchEvent`, das ebenfalls in dieselben Statics schreibt – beide Wege koexistieren.

---

### 3.3 `GameThread.java`

**Zweck:** Trivialer `Thread`‑Wrapper, der in `run()` lediglich `Game.gaming()` aufruft. Setzt den Threadnamen auf `"GameThread"`. Hat keine eigenen Felder oder weitere Logik.

---

### 3.4 `Game.java` – zentrale Spiellogik (statisch)

Ist die „Engine". Alle Felder sind `public static`.

#### 3.4.1 Statische Felder

| Feld | Typ | Bedeutung |
|---|---|---|
| `gamedepths` | `Mothermap[100]` | Bis zu 100 Dungeon‑Ebenen. Index 0 ist die Startebene (`TestDungeon`); `1..4` werden bei Bedarf als `ShallowDungeon` generiert. |
| `layer` | `int` | Aktive Ebene; Index in `gamedepths`. |
| `timeFromStart` | `int` | Tick‑Zähler der Hauptschleife. |
| `doneMyTurn` | `boolean` | Steuert die innere Schleife in `PlayCreature.behavior()`. |
| `whereToGoX`, `whereToGoY` | `int` | Letzter Touch‑Punkt in **Bildschirm**‑Pixeln (X = vertikal, Y = horizontal – siehe Hinweis oben). `-1` heißt „kein Input". |
| `pathFindingHelpArray` | `int[100][100]` | BFS‑Distanzfeld; wird in jedem `pathFinding()`‑Aufruf zurückgesetzt. |
| `player` | `PlayCreature` | Der Spieler. |
| `isSeen` | `boolean[100][100]` | Aktuelles Sichtfeld („was ich gerade sehe"). Wird in `fieldOfView()` jedes Mal komplett neu berechnet. Tile‑Felder besitzen separat `wasSeen` für „bereits einmal gesehen". |

#### 3.4.2 `public static void gaming()`

Hauptschleife, läuft im `GameThread`.

* **Initialisierungsphase** (innerhalb `synchronized(TouchAndThreadParams.outStreamSync)`):
  * Setzt `layer = 0`, lädt `AllBitmaps.initialize()` und `AllBitmaps.changeSize()`.
  * `gamedepths[0] = new TestDungeon()` und legt darin per Hand einen Goblin, eine Wand bei `[99][99]`, einen Down‑Stairway bei `[5][1]` (mit `linker = 1`), eine Wand‑Reihe `[4][0..6]`, weitere einzelne Wände sowie ein Wooden Sword bei `[5][2]` ab.
  * Findet die erste begehbare Zelle (kein `iswall`) per Doppelschleife mit `break tototo;`‑Label und setzt den Spieler dorthin (`yCoordinates=i`, `xCoordinates=j`).
  * Verschiebt den Goblin (`creatures.peek()`) auf `(3,3)`.
  * Berechnet das Sichtfeld (`fieldOfView()`).
  * Weckt alle, die auf `outStreamSync` warten (`DrawThread`).
  * Erzeugt leere `Mothermap`‑Hülsen für Layer 1–99.
  * Der gesamte Block ist in `try { … } catch (Exception e) {}` gehüllt – Fehler werden geschluckt.

* **Hauptschleife** (`while (true)`):
  1. Wenn `player.toWait == 0`: rufe `player.behavior()` (blockiert ggf. auf `gameSync`, bis ein Touch kommt), sonst dekrementiere.
  2. Iteriere über `gamedepths[layer].creatures`: Bei `toWait == 0` → `creature.behavior()`, sonst dekrementiere.
  3. `timeFromStart++; whereToGoX = -1;`

> Die Schleife liegt geschachtelt in `synchronized(waitingThing)` und `synchronized(outStreamSync)` – `waitingThing` ist eine lokale, sonst nirgends benutzte Sperre und hat keine Wirkung, da niemand sonst auf ihr wartet. Der `outStreamSync`‑Lock wird innerhalb von `behavior()` über `wait()` immer wieder freigegeben.

#### 3.4.3 Pathfinding

```java
public static LinkedList<String> pathFinding(MotherCreature from, MotherCreature to)
public static LinkedList<String> pathFinding(MotherCreature from, int tx, int ty)
public static LinkedList<String> pathFinding(int fromX, int fromY, int toX, int toY, int max)
```

* **Was sie tun:** BFS von `(toX,toY)` aus über das `gamedepths[layer].field`. Erlaubt 8 Nachbarn, blockiert bei `iswall` und `isMobHere` (außer der Mob ist der Suchstartpunkt). Maximale Tiefe: `max` (entspricht `from.maxVision`).
* **Wie:** `pathFindingHelpArray` wird auf 100000 zurückgesetzt; Zielzelle bekommt 0; jede expandierte Zelle ihren Wert + 1. Anschließend wird, wenn `pathFindingHelpArray[fromY][fromX] < 100000`, vom Start aus immer in die jeweils kleinere Distanz gewandert. Aus dieser Wanderung entsteht die Schritt‑Liste (`"Up"`, `"Down"`, …, `"DownLeft"`).
* **Parameter:** Quelle und Ziel als Kreatur oder explizit; `max` = maximale Suchtiefe.
* **Rückgabe:** `LinkedList<String>` mit den Bewegungs‑Strings vom Start zum Ziel. Leer, wenn nicht erreichbar.
* **Seiteneffekte:** Überschreibt `pathFindingHelpArray`. Verwendet `washere[100][100]` lokal.

> Bug‑Risiko: Im „Right"‑Zweig wird `&` (bitweise) statt `&&` (kurz‑schaltend) verwendet. Funktioniert auf bool, ist aber kein Short‑Circuit – die Folgevergleiche werden immer ausgewertet. Ebenso schreibt der `Right`‑Zweig nicht in das Pathfinding‑Array, wenn die Bedingung scheitert.

#### 3.4.4 `public static int findCreatureByCoordinater(int x, int y)`

Sucht in `gamedepths[layer].creatures` die Kreatur an `(x,y)`.
* Wenn der Spieler dort steht: Rückgabe `-1`.
* Sonst: Index in der Liste oder `-3`, wenn keiner gefunden.
* Aufrufer (`MotherCreature.attack`, `PlayCreature.behavior` Move‑Case) verwenden den Sentinel `-1` als „Spieler" und `>=0` als Mob‑Index.

#### 3.4.5 `public static void changeLevel(int fromWhat, int toWhere)`

Wechselt zwischen Dungeon‑Ebenen.
* Generiert `gamedepths[toWhere]` als `ShallowDungeon`, wenn noch nicht erzeugt und `toWhere < 5`.
* Sucht im neuen Level eine Zelle mit `linker == fromWhat` (Verknüpfung der Treppe) und platziert den Spieler dort (`xCoordinates = j-1`, `yCoordinates = i-1`).
* Aktualisiert `Params.displayX/Y` (mit absichtlich vertauschten X/Y) und `Game.layer`.
* Ruft `fieldOfView()` neu auf.

> `fromWhat`‑Bedeutung: `1..5` für Down‑Treppen, `6..10` für Up‑Treppen (siehe `UpStairway/DownStairway.toInteract`).

#### 3.4.6 `public static void placeCreature(int what, int whereDepth, int whereX, int whereY)`

Erzeugt eine Kreatur per Typ‑Code (aktuell nur `102` = Goblin), setzt deren Koordinaten und legt sie in `gamedepths[whereDepth].creatures`. Markiert die Zielzelle als `isMobHere = true`.

#### 3.4.7 `public static void fieldOfView()`

Setzt `isSeen[][]` komplett auf `false` und schießt dann je nach Spielerposition Strahlen zur Top‑, Bot‑, Left‑ und Right‑Kante.

* Pro Strahl wird die Geraden‑Form `y = k·x + b` aufgestellt; je nach Steigung wird über X oder Y iteriert.
* In jedem Schritt werden `isSeen[][]` und `tile.wasSeen` auf `true` gesetzt; trifft der Strahl eine Wand → `break`.
* Sonderfall „vertikale" Strahlen (`startX - xPos == 0`): Iteration nur entlang Y.

> Auskommentierter Code referenziert eine geplante Verbreiterung des Lichtkegels für flache Winkel – derzeit nicht aktiv.

---

### 3.5 `Params.java` – UI‑Modus & Kamera

Statischer Behälter für den Render‑/UI‑Zustand.

| Feld | Typ | Bedeutung |
|---|---|---|
| `menu`, `inventory`, `map`, `gameField`, `item`, `stats` | `boolean` | Welche „Seite" aktuell angezeigt wird. Genau eine soll zur Zeit `true` sein – durchgesetzt durch die `start…()`‑Helfer. `gameField=true` initial. |
| `size` | `int = 64` | Tile‑Kantenlänge in Pixel (zoombar 32–96, siehe `MySurfaceView`). |
| `resource` | `Resources` | Wird aus `MainActivity.onCreate` gesetzt; von `AllBitmaps` verwendet. |
| `itemToShow` | `int` | Inventar‑Slot‑ID, die im Item‑View dargestellt wird. ≥ 0 = Inventarslot, `-5..-1` = Equipment (Helm/Body/Weapon/Jewel/Ring1/Ring2). |
| `displaySettings` | `DisplayMetrics` | Aufgelöste Bildschirmgröße. |
| `displayX`, `displayY` | `int` | Kamera‑/Scroll‑Offset in Pixeln. **Beachte:** `displayX` ist vertikal, `displayY` horizontal. |
| `iconOversize` | `int` | Skalierungsfaktor (in `AllBitmaps.initialize` ermittelt), damit Inventar/Icons auf dem Display passen. |

**Methoden** (alle statisch):

* `startGameField()`, `startInventory()`, `startStats()`, `startItem(int)` – einfache State‑Setter, die jeweils alle Modus‑Flags zurücksetzen und genau eines auf `true` setzen. `startItem(int)` schreibt zusätzlich `itemToShow`.
* `centerOnPlayer()` – berechnet `displayX/Y` so, dass der Spieler zentriert wird, und klemmt die Werte in die Kartengrenzen (mit Berücksichtigung der `standartIconSize` als Top‑Bar‑Reserve).

---

### 3.6 `TouchAndThreadParams.java` – Sync + Render‑Helfer

Trotz des Namens enthält diese Klasse zwei sehr unterschiedliche Verantwortlichkeiten:

1. **Synchronisationsobjekte und letzte Touch‑Daten** (Statics).
2. **Zeichenroutinen** für Menü/Inventar/Item/Stats/Tiles und Text‑zu‑Pixel.

#### 3.6.1 Statische Felder

| Feld | Bedeutung |
|---|---|
| `justClicked`, `secondFinger` | Zustand der laufenden Touch‑Geste. |
| `flastX, flastY, slastX, slastY` | Letzte / vorletzte Touch‑Koordinaten. **Achtung:** `flastX` ist `event.getY()`, `flastY` ist `event.getX()` (siehe `MySurfaceView.onTouchEvent`). |
| `used` | Ungenutzt im Code. |
| `outStreamSync` | Lock zwischen `Game`, `DrawThread`, Inventory‑Aktionen. |
| `gameSync` | Lock, auf dem `PlayCreature.behavior()` wartet, bis ein Touch kommt. |
| `animationSync` | Lock zwischen Logik‑Thread und `AnimationThread`. |
| `inventoryResize` | Konstante 2 (nicht überall genutzt). |
| `whenClicked` | `System.currentTimeMillis()` beim Touch‑Down – für Tap‑vs‑Drag. |

#### 3.6.2 `static String getWhatPlayerDesired()`

Klassifiziert den letzten Touch‑Punkt anhand des aktuellen UI‑Modus (`Params.gameField/inventory/stats/item`) zu einer der Befehlsstrings:

* Spielfeld: `"Inventory"`, `"Wait"`, `"Stats"`, `"TakeFromFloor"`, `"Interact"`, `"Move"` (Default).
* Inventar: `"ChooseItem"` oder `"Back"`.
* Stats: `"StatUp"` oder `"Back"`.
* Item: `"PushInventoryButton"`, `"Nothing"`, `"BackToInventory"`.

Verwendet die in `AllBitmaps` zwischengespeicherten Bildgrößen (`inventoryImage`, `getItemHere`, `statsView` etc.) zur Hit‑Box‑Berechnung.

> Gibt `""` zurück, wenn kein Modus passt – `PlayCreature.behavior()` läuft dann durch den `switch` ohne Treffer.

#### 3.6.3 Zeichenroutinen

Alle nehmen einen `Canvas` entgegen, schreiben darauf und geben ihn zurück.

* **`drawMenu(Canvas)`** – Top‑Bar mit `leftBar`/`centerBar`/`rightBar`, gefüllt entsprechend `health/maxHP`, `mana/maxMana`, `experience/(lvl*10)`. Unten: Wait‑Icon mittig, Inventory‑Icon rechts; oben links Stats‑Icon, oben rechts Menü‑Icon. Optional: „Item‑hier‑aufnehmen"‑Button, wenn auf der aktuellen Tile Gold/Items liegen, und „Interagieren"‑Button bei `interractable`.
* **`drawMultiLineEllipsizedText(Canvas, TextPaint, left, top, right, bottom, text)`** – baut zwei `StaticLayout`s, kürzt mit „…", wenn der Text vertikal nicht passt. Die Vorlage stammt laut Kommentar von Stack Overflow (androidseb).
* **`textToPicture(String)`** – wandelt Ziffern‑Zeichen und Spaces in `Bitmap`‑Folgen aus `AllBitmaps.originalnumbers` und `AllBitmaps.space`. Andere Zeichen werden ignoriert (TODO im Code).
* **`drawMultiLineText(Canvas, String, left, right, top, bottom, overSize)`** – wortweiser Wrap der per `textToPicture` erzeugten Bitmaps; bricht in Zeilen um und stoppt, wenn die Höhe ausgeht. Zeichnet ziffernweise. Die Variable `floatingPoint` ist ein horizontaler Cursor, kein Float.
* **`drawInventory(Canvas)`** – zeichnet die Inventar‑Hintergrundgrafik, dann fünf Equipment‑Slots (BodyWear, Weapon, Jewel, Ring1, Ring2) und das 4×5‑Item‑Grid `Game.player.items[]`. Skaliert die Item‑Sprites in 32·widthOfInv/288 Px.
* **`drawItem(Canvas)`** – Item‑Detail‑Ansicht. Wählt das Item per `Params.itemToShow`, zeigt Bild, Name (ellipsiert) und Beschreibung; blendet je nach `equipable`/`usable`/Inventar vs. Equipment Buttons (Equip/Unequip/Use/Drop) ein.
* **`drawStats(Canvas)`** – Spielerportrait, Stat‑Werte (HP, Mana, Str, Intel, Agi, lvlUps), Plus‑Buttons, wenn `Game.player.lvlUps > 0`.
* **`drawAllTilesOnTheField(Canvas)`** – Hauptkartenzeichnung. Berechnet aus `displayX/Y` und `Params.size` ein Sichtfenster (`startX/Y` … `endX/Y`) und zeichnet alle Tiles mit `wasSeen`. Für `isSeen` darüber Items/Gold; sonst eine Schatten‑Bitmap. Verwendet `AllBitmaps.getPictureById(tile.drawId)`.

---

### 3.7 `AllBitmaps.java` – Bitmap‑Cache & Skalierung

Lädt sämtliche Sprites einmalig per `BitmapFactory.decodeResource(Params.resource, …)`.

* Hält jeweils zwei Versionen pro Sprite: `originalXxx` (Originalgröße) und `xxx` (aktuell skalierte Variante). Skaliert wird in `changeSize()` zur Tile‑Größe `Params.size`, in `initialize()` zusätzlich um `iconOversize` für UI‑Elemente.
* Für animierbare Charaktere existieren `LinkedList<Bitmap>`‑Move‑/Attack‑Frames (`character_move1/2`, `character_attack1/2`; Goblin recycelt das Standbild).
* Statische Mappings:
  * **`getPictureById(int id)`**: 1=DungeonFloor, 2=DungeonWall, 3=ladderDown, 4=ladderUp, 101=Character, 102=Goblin. Sonst `null`.
  * **`getMoveAnimationById(int id)` / `getAttackAnimationById(int id)`**: Animationsframes per drawId.
* **`initialize()`**: Berechnet `iconOversize` (größtmögliches `i`, sodass Inventar‑Bitmap × i auf dem Bildschirm Platz hat), skaliert UI‑Sprites entsprechend, lädt Animationen und Ziffern‑Bitmaps.
* **`changeSize()`**: Skaliert alle Tile‑/Charakter‑Bitmaps auf `Params.size` × `Params.size`. Wird beim Pinch‑Zoom (siehe `MySurfaceView`) und beim Spielstart aufgerufen.

> Auffällig: `originalcharacterMove`/`originalcharacterAttack` werden in `changeSize()` in einer Schleife geleert (`characterMove.clear()`) und anschließend nur das aktuelle `i`‑Frame eingefügt – nach der Schleife enthält `characterMove` daher nur das letzte Frame. Dasselbe gilt für `goblinMove/Attack`. Animation funktioniert dadurch nur eingeschränkt.

---

### 3.8 Paket `Creatures/`

#### 3.8.1 `MotherCreature.java`

Basis aller Spielfiguren. Reine Datencontainer + virtuelle Verhalten‑Methoden.

**Felder (alle `public`):**

| Gruppe | Felder |
|---|---|
| Vitalwerte | `health, mana, maxHP, maxMana` |
| Identität/Render | `name`, `drawId`, `orientatedToRight` |
| Kampfstatus | `aggred`, `str, agi, intel, deffense, attack` |
| Position | `xCoordinates, yCoordinates` |
| Progression | `lvl, lvlUps, experience, expOnDeath` |
| KI | `maxVision, pathing` (LinkedList<String>) |
| Equipment | `helmet, bodyWear, weapon, ring1, ring2, jewel` (alle `MotherItem`, default `EmptyItem`) |
| Inventar | `items[20]`, alle initial `EmptyItem` |
| Bewegung | `moveDelay`, `weight`, `gold`, `toWait` |

**Konstruktor `MotherCreature()`:** Initialisiert sämtliche Equipment‑Slots mit `EmptyItem`, Inventar mit 20 `EmptyItem`s, `lvl=1`, `gold=300` (Default!), `maxMana=5`, `moveDelay=100`.

**Methoden:**

* `public void behavior()` – KI für nicht‑Spieler. Wenn `aggred`, Pfadsuche zum Spieler; bei Pfadlänge >1 ein Schritt, bei =1 Angriff. Zugriff über `outStreamSync`. Übergibt am Ende `notifyAll()` an den Outstream‑Lock.
* `protected boolean unEquip(int what)` – `what` ist `-5..-1`. Bewegt das entsprechende Equipment per `MotherItem.unEquip(this)` zurück ins Inventar; setzt das Slot‑Feld auf `EmptyItem`. Rückgabe true, wenn erfolgreich. **Kein `break`** zwischen den `case`s – Fall‑Through ist offenbar gewollt, in `MotherItem.unEquip` aber ebenfalls so → führt dazu, dass tatsächlich für mehrere Slot‑Typen Bonusse abgezogen werden.
* `protected boolean equip(int what)` – legt `items[what]` per `equip(this)` an und leert den Slot. Defragmentiert anschließend mit `makeInventoryFit()`.
* `protected void move(String str, Tile wherefrom)` – Setzt `toWait = moveDelay * wherefrom.movedelay`, markiert die Quellzelle als nicht mehr besetzt, übergibt den `AnimationThread` (`MainGameActivity.sf.animThread.setMoveAnimation`) und aktualisiert `xCoordinates/yCoordinates` je nach Richtungs‑String. Wartet ggf. auf `animationSync.wait()`. Markiert Zielzelle als `isMobHere=true`. Wird in `PlayCreature` überschrieben, um `Game.fieldOfView()` neu zu berechnen.
* `protected boolean attack(int x, int y)` – Kampfformel:
  * Gegner per `findCreatureByCoordinater(x,y)`.
  * `toWait = moveDelay * weapon.delay`, mit Strafmodifikatoren bei `weight > str`, `weapon.agiNeeded > agi`, `weapon.strNeeded > str`.
  * Trefferchance (`hitchance`): startet bei 90, abzgl. Gewichtsfaktor und ggf. Agilitätsdifferenz, untere Schranke 30.
  * Schaden (`toHit`): zufällig im Bereich `[attack/2 + 1, attack]`; Krit (× 2) mit Wahrscheinlichkeit `(str+agi)/2 %`. Verteidigung `toDef` analog aus `who.deffense`.
  * Wenn Treffer: `toHit-toDef` Schaden (mind. 1) → `who.health-=…`. Wenn Verfehlt: `helper.missed=true`.
  * Triggert die Attack‑Animation, wartet auf deren Ende, und gibt `who.health <= 0` zurück.
  * **Achtung:** Im Bug‑lastigen Code wird der Schaden im Trefferpfad **doppelt** abgezogen (einmal allgemein über `who`, einmal explizit am `creatures.get(whom)` bzw. `Game.player`).
* `protected void makeInventoryFit()` – komprimiert `items[]` durch Verschieben nach links über leere Slots.
* `protected void drop(int what)` – legt Item auf die aktuelle Zelle (`itemsHere.add`), Slot wird leer.
* `protected boolean take()` – nimmt Gold (immer) bzw. das oberste Item vom Boden ins Inventar. Stoppt, wenn `items[19]` belegt ist und kein Gold da. Anschließend `makeInventoryFit()`.
* `protected void addExp(int howMany)` – schichtet Erfahrung in Levelups. Pro Level: `experience >= lvl*10` → `lvlUps++`, `experience -= lvl*10`, `lvl++`.
* `protected void die()` – legt sämtliche Items + Gold auf die aktuelle Zelle, ruft alle fünf `unEquip`‑Slots auf und entfernt die Kreatur aus `creatures`. **Achtung:** `gamedepths[layer].field[xCoordinates][yCoordinates].isMobHere = false` verwendet vertauschte Koordinaten und leert eine andere Zelle als die, in der die Kreatur tatsächlich stand.

#### 3.8.2 `PlayCreature.java`

Spezialisierung für den Spieler. Default‑Werte: `health=10`, `maxHP=10`, `mana=10`, `maxMana=15`, `maxVision=10000` (de facto unbegrenzt), `drawId=101`, `str/intel/agi=10`, `experience=9`, `lvlUps=3`, zwei Wooden Swords im Inventar (Slots 0 und 5).

* **`@Override public void behavior()`** – Die Kommandozentrale für den Spieler:
  1. Solange `!Game.doneMyTurn`: wenn `pathing` leer ist, blockiert auf `gameSync.wait()` bis ein Touch eintrifft, dann fragt `getWhatPlayerDesired()` nach dem Befehl. Sonst (laufender Pfad) Befehl = `"Move"`.
  2. Riesiger `switch`:
     * **`Move`**: Wandelt `Game.whereToGoX/Y + Params.displayX/Y` per `Params.size` in Tile‑Koordinaten `(x,y)`. Bricht ab, wenn Ziel Wand/unbekannt ist oder die Spielfigur komplett von Mobs/Wänden umschlossen ist. Bei freiem Ziel: `pathing = Game.pathFinding(this, y, x)`, evtl. `Params.centerOnPlayer()`, dann ein Schritt. Bei direkt benachbartem Mob: `attack(y, x)`; bei tödlichem Treffer Erfahrungsgewinn und `die()`. Setzt `Game.doneMyTurn = true`.
     * **`Wait`**: Zug überspringen (`toWait = moveDelay`).
     * **`Inventory`/`Stats`/`Back`/`BackToInventory`**: nur UI‑Modus wechseln.
     * **`TakeFromFloor`**: `take()` und Zug beenden.
     * **`Interact`**: Tile‑spezifisch (`tile.toInteract(this)`), Zug beenden. **Hinweis:** Es fehlt ein `break;` → Fall‑Through nach `ChooseItem`.
     * **`ChooseItem`**: Wandelt die Touch‑Position auf dem Inventar‑Bild in Slot‑Index (`-5..-1` Equipment, `0..19` Inventar) und öffnet die Item‑Ansicht via `Params.startItem(what)`, falls dort tatsächlich ein Item liegt.
     * **`PushInventoryButton`**: Equip/Unequip (Button 1) oder Drop (Button 3); Use (Button 2) ist nicht implementiert.
     * **`StatUp`**: Erhöht passende Stat (`maxHP+=5`, `maxMana+=5`, `str/intel/agi++`) und dekrementiert `lvlUps`.
     * **`Nothing`**: keine Aktion.
  3. Am Ende der Iteration: `outStreamSync.notifyAll()` + `wait()` → Übergibt die Kontrolle an den Render‑Thread, bevor erneut auf Input gewartet wird.

* **`@Override protected void move(String str, Tile wherefrom)`** – wie in `MotherCreature`, aber: ruft am Ende **`Game.fieldOfView()`** auf, damit das Sichtfeld für den Spieler aktualisiert wird, und benutzt einen anderen Sync‑Pfad (das `if(Game.isSeen[…])`‑Gate aus der Basisklasse fehlt, weil der Spieler immer animiert werden soll).

#### 3.8.3 `Goblin.java`

Trivialer Subtyp mit fixen Startwerten: `xCoordinates=9, yCoordinates=8`, `maxVision=20`, `health=3`, `attack=2`, `str=agi=intel=3`, `deffense=1`, `weight=4`, `expOnDeath=1`, `aggred=true`, `moveDelay=150`, `drawId=102`. Erbt `behavior()` von `MotherCreature` (verfolgt den Spieler, schlägt zu, wenn benachbart).

---

### 3.9 Paket `Items/`

#### 3.9.1 `ItemType.java`

```java
enum ItemType { Helm, BodyWear, MeeleWeapon, RangedWeapon, Ring, Jewel, Consumable, Quest, Ammo }
```

Wird von `MotherItem.equip()`/`unEquip()` zur Slot‑Auswahl verwendet.

#### 3.9.2 `MotherItem.java`

Basisklasse aller Items.

**Felder:** `type` (ItemType), `name`, `description`, `picture` (Bitmap), `delay`, `strNeeded`, `agiNeeded`, `intNeeded`, `weight`, `equipable`, `usable`, `bonus`, `strBonus`, `agiBonus`, `intBonus`. `equipable=false; usable=false` per Default.

**Methoden:**

* `String getInformation()` – aktuell `""`.
* `void use()` / `boolean use(MotherCreature mc)` – Default: ruft `equip(mc)` und gibt `true` zurück.
* `boolean equip(MotherCreature creature)` – schaltet je nach `type` den entsprechenden Slot um, addiert `strBonus/intBonus/agiBonus/weight/bonus` (Defense bei Helm/BodyWear; Attack bei Waffen). Gibt `false` zurück, wenn der Slot belegt ist (Ringe versuchen erst Slot 1, dann Slot 2).
* `boolean unEquip(MotherCreature creature)` – sucht den ersten freien Inventarslot. **Achtung:** Der `switch` hat **keine `break`s**, was zur Folge hat, dass für jeden Item‑Typ die Bonus‑Subtraktion auch von darunter liegenden Cases mitläuft (z. B. Helm → BodyWear → Weapon → Jewel → Ring) – effektiv werden bei Helmen/Body‑Wear/Waffen *mehrfach* Boni abgezogen. Das ist im Code so, nicht durch diese Doku interpretierbar bereinigt.

#### 3.9.3 `EmptyItem.java`

Null‑Object‑Pattern: `name=""`, alle Boni 0. Wird überall als „leer"‑Marker verwendet (`item.name.equals("")` prüft auf Leersein – kein null‑Check).

#### 3.9.4 `RandomItem.java`

Leere Klasse (Platzhalter, derzeit ohne Verwendung).

#### 3.9.5 `Items/Weapons/MeeleWeapon.java`

Subklasse `MotherItem`, setzt `type=MeeleWeapon`, `equipable=true`.

#### 3.9.6 `Items/Weapons/RangedWeapon.java`

Subklasse `MotherItem`, setzt `type=RangedWeapon`. **Achtung:** `equipable` bleibt false (Bug).

#### 3.9.7 `Items/Weapons/Meeles/WoodenSword.java`

Konkrete Waffe: `name="Wooden Sword"`, Dummy‑Beschreibung, `picture=AllBitmaps.woodenSword`, `strNeeded=3`, `agiNeeded=2`, `intNeeded=0`, `bonus=3`, `delay=1`.

---

### 3.10 Paket `Helpers/`

#### 3.10.1 `AttackHelper.java`

DTO für eine Angriffsanimation: `attacker`, `attacked`, `damage`, `missed`, `stunned`, `scared`. Wird von `MotherCreature.attack()` befüllt und an `AnimationThread.setAttackAnimation()` übergeben.

#### 3.10.2 `FindingHelper.java`

Schlankes (x,y)‑Tupel für die BFS‑Queue in `Game.pathFinding`.

---

### 3.11 Paket `Tiles/`

`Tile` ist die Basis aller Map‑Felder.

**`Tile`‑Felder:**

| Feld | Bedeutung |
|---|---|
| `iswall` | Blockiert Bewegung/Pfad/Sicht. |
| `movedelay` | Multiplier für Bewegungs‑`toWait` (z. B. Schlamm). Default 1. |
| `isMobHere` | Steht eine Kreatur darauf. |
| `goldHere` | Gold‑Stack‑Größe. |
| `interractable` | Treppe / Schalter etc. |
| `drawId` | ID für `AllBitmaps.getPictureById`. |
| `wasSeen` | Wurde diese Zelle jemals vom Spieler gesehen (für „Fog of War"). |
| `itemsHere` | `Stack<MotherItem>` der auf der Zelle liegenden Items. |
| `linker` | Treppen‑Verknüpfung (1–5 Down‑Treppen, 6–10 Up‑Treppen, `-1` = unverknüpft). |

**Methoden:** `toInteract()` und `toInteract(MotherCreature who)` – default no‑op.

#### Subtypen

* `Floor` – `iswall=false`. Default‑Begeher.
* `Wall` – `iswall=true`.
* `UpStairway` (extends `Tile`) – `interractable=true`. `toInteract(who)`: `Game.changeLevel(linker - 5, Game.layer - 1); Params.centerOnPlayer();`
* `DownStairway` – `interractable=true`. `toInteract(who)`: `Game.changeLevel(linker + 5, Game.layer + 1); Params.centerOnPlayer();`
* `StandardDungeon/DungeonFloor` – `Floor` mit `drawId=1`.
* `StandardDungeon/DungeonWall` – `Wall` mit `drawId=2`.
* `StandardDungeon/DungeonDownStairway` – `DownStairway` mit `drawId=3`, `interractable=true`.
* `StandardDungeon/DungeonUpStairway` – `UpStairway` mit `drawId=4`.

> Konvention: Konkrete Tiles in `StandardDungeon/` sind reine Zuweisungen der `drawId` auf die generischen Basistypen.

---

### 3.12 Paket `Map_etc/`

#### 3.12.1 `Mothermap.java`

Container für eine Dungeonebene: `Tile field[100][100]`, `LinkedList<MotherCreature> creatures`, `boolean generated`, `protected int depth`. Konstruktoren erzeugen leere Felder.

`generateTheField()` – im Basistyp leer; Subklassen überschreiben.

#### 3.12.2 `RoomHelper.java`

Trivialer Rechteck‑DTO für `ShallowDungeon`: `x, y, width, heigth` (sic).

#### 3.12.3 `DungeonTypes/TestDungeon.java`

Debug‑/Initialkarte: füllt komplett mit `DungeonFloor`, setzt eine `DungeonWall` bei `[3][6]`. Konstruktor instanziiert `field` selbst und ruft direkt `generateTheField()` auf.

#### 3.12.4 `DungeonTypes/ShallowDungeon.java`

Prozedural generierte Karte für die ersten 5 Ebenen (`depth < 5`).

`generateTheField()`:
1. `n` = `Math.random()*10 + 10` (Anzahl Räume).
2. Karte komplett mit `DungeonWall` füllen.
3. Erste Raum (`RoomHelper`) zufällig im Bereich `[4..84) x [4..84)` mit Größe `[3..10)` setzen, mit `DungeonFloor` füllen.
4. Pro weiterem Raum: zufällig erzeugen, mit Floor füllen, dann L‑förmigen Korridor zum Vorgängerraum legen (erst horizontal, dann vertikal).
5. Drei Down‑Stairs platzieren (Linker 1..3) auf zufälligen Floor‑Zellen.
6. Drei Up‑Stairs platzieren (Linker 6..8).
7. Zehn Goblins über `Game.placeCreature(102, depth, x, y)` platzieren.

> Indizes der Treppen sind absichtlich asymmetrisch (`i` für Down vs `i+5` für Up), passend zu den `+5`/`-5` Berechnungen in `UpStairway/DownStairway.toInteract`.

---

### 3.13 Paket `Outstreams/`

Drei eng verzahnte Klassen für Rendering und Eingabe.

#### 3.13.1 `MySurfaceView.java`

`extends SurfaceView implements SurfaceHolder.Callback`.

* **Felder:**
  * `private DrawThread drawThread`
  * `public AnimationThread animThread` – statisch erreichbar via `MainGameActivity.sf.animThread`.
  * `private int length` – letzter Pinch‑Abstand für Zoom.
* **`onTouchEvent(MotionEvent)`** – Drei Phasen:
  * **DOWN:** merkt sich `(flastX, flastY)` (mit X=Y, Y=X), Zeitstempel `whenClicked`, `justClicked=true`, leert `Game.player.pathing` (laufenden Pfad abbrechen).
  * **MOVE:**
    * Mit einem Finger: wenn der Finger sich >3 px verschoben hat → Pan: `Params.displayY/X` werden um die Differenz aktualisiert, geclampt. `justClicked=false`.
    * Mit zwei Fingern: berechnet eine euklidische Distanz (Achtung – die Formel hat einen Klammersetzungsfehler) und nutzt die Differenz zum letzten `length` zum Zoom: `Params.size += diff/10`, dann `AllBitmaps.changeSize()`. `Params.size` wird bei 32 und 96 gekappt.
    * In beiden Pfaden wird am Ende `outStreamSync.notifyAll()` + `.wait()` ausgelöst, um den `DrawThread` einen Frame zeichnen zu lassen.
  * **UP:** wenn weniger als 250 ms vergangen sind und nicht gedrag gewesen wurde → echter Tap: `Game.whereToGoX/Y` wird gesetzt und `gameSync.notifyAll()` weckt `PlayCreature.behavior()`.
* **`surfaceCreated(holder)`** – erzeugt `drawThread` und `animThread`, startet beide.
* **`surfaceDestroyed(holder)`** – stoppt `drawThread` per `setRunning(false)` und joint ihn.
* `surfaceChanged(...)` – leer.

#### 3.13.2 `DrawThread.java`

Endlosschleife (`runFlag`‑gesteuert), max. Priorität, Threadname `"Thread_of_outstream"`.

* **Konstruktor:** Speichert `SurfaceHolder` und decodiert ein Default‑`tile`‑Bitmap (nur als Initialwert).
* **`run()`**:
  1. Initialer `outStreamSync.wait(2000)` – wartet auf das Signal aus `Game.gaming()`, dass die Initialisierung fertig ist (max. 2 s).
  2. Pro Iteration in `synchronized(surfaceHolder)`: `lockCanvas`, schwarzen Hintergrund zeichnen, dann
     * `TouchAndThreadParams.drawAllTilesOnTheField`,
     * Spieler‑Sprite (gespiegelt, falls `!orientatedToRight`),
     * alle sichtbaren (`Game.isSeen[…]`) Kreaturen,
     * `drawMenu`, ggf. `drawInventory`, `drawItem`, `drawStats`,
     * `unlockCanvasAndPost`.
  3. Anschließend `outStreamSync.wait()` – schläft, bis Logik oder Touch wieder rendert.
* **`setRunning(boolean)`** – steuert die Schleife.

#### 3.13.3 `AnimationThread.java`

Dedizierter Thread für Move‑ und Attack‑Animationen.

* **Felder:** `surfaceHolder`, `attackHelper`, `fromX/fromY` (Startzelle für Bewegung), `whom` (sich bewegende Kreatur), `whatToDo` (1 = Move, 2 = Attack).
* **`setMoveAnimation(MotherCreature, fX, fY)`** – setzt `whatToDo=1` und merkt sich Start‑Daten.
* **`setAttackAnimation(AttackHelper)`** – setzt `whatToDo=2` und den Helper.
* **`run()`**:
  1. Wartet initial auf `animationSync`.
  2. Endlosschleife in `synchronized(animationSync)`:
     * Bei `whatToDo == 1`: zeichnet 150 ms lang (`while time < 150ms`) Frames, in denen das `whom`‑Sprite linear zwischen `(fromX,fromY)` und `(xCoordinates,yCoordinates)` interpoliert wird. Verwendet `getMoveAnimationById(drawId)` für Frame‑Auswahl. Wenn `whom` der Spieler ist, läuft die Kamera (`Params.displayX/Y`) am Bildschirmrand mit, geclampt. Zeichnet danach das Menü.
     * Bei `whatToDo == 2`: zwei Phasen:
       * **Hit‑Animation** (250 ms): rendert Tiles + Charaktere + die Attack‑Frames des Angreifers.
       * **Schadens‑Pop‑up** (750 ms): rendert die Damage‑Zahlen (oder `miss.png`), die nach oben über das Ziel schweben. Position respektiert die Kartengrenzen; bei Zielen am oberen Rand wird die Zahl unterhalb gezeichnet (`reversed`).
  3. Am Ende einer Animation: `notifyAll()` + `wait()` – übergibt zurück an den Logikthread.

> Beachte: Während dieser Thread innerhalb seines `synchronized(animationSync)`‑Blocks zeichnet, blockiert die Spiellogik dort, wo sie ebenfalls auf `animationSync` synchronisiert (in `MotherCreature.move`/`attack`).

---

## 4. Datenfluss & Interaktion

### 4.1 Frame eines normalen Spielzugs

```
Touch → MySurfaceView.onTouchEvent (UP, kurz)
      → Game.whereToGoX/Y gesetzt
      → TouchAndThreadParams.gameSync.notifyAll()
      → PlayCreature.behavior() (im GameThread, schlief auf gameSync)
        → getWhatPlayerDesired() liefert "Move"
        → pathFinding(this, y, x) baut LinkedList<String>
        → move(step, currentTile)
            ├── animationSync.notifyAll() weckt AnimationThread (Move)
            ├── wait() bis Animation fertig
            └── Game.fieldOfView() (nur PlayCreature)
        → setzt Game.doneMyTurn = true
      → outStreamSync.notifyAll() weckt DrawThread für nächsten Frame
```

### 4.2 Gegnerzug

```
Game.gaming() while-Loop iteriert creatures
  → Goblin.behavior()  (geerbt aus MotherCreature)
    → if aggred: pathFinding(this, player) → move() oder attack()
    → attack() füllt AttackHelper → setAttackAnimation
        → AnimationThread spielt Hit‑Animation + Schadenszahl
    → outStreamSync.notifyAll(), wait()
```

### 4.3 Treppen‑Wechsel

```
Player tippt auf Interact‑Button
  → "Interact" → tile.toInteract(this)
    → DownStairway: Game.changeLevel(linker+5, layer+1)
      → ggf. ShallowDungeon.generateTheField()
      → Spieler auf passendes Linker-Tile (-5 Offset für gegenüber)
      → fieldOfView()
    → Params.centerOnPlayer()
```

### 4.4 UI‑Modi

`Params.menu/inventory/stats/item/gameField/map` sind als sich gegenseitig ausschließende Boolesche Flags codiert. `DrawThread.run()` prüft sie und ruft die jeweilige Zeichenmethode in `TouchAndThreadParams` auf. `getWhatPlayerDesired()` interpretiert dieselben Flags zur Klassifizierung von Touch‑Punkten.

### 4.5 Synchronisationsregeln

| Lock | Wartet drauf | Wird geweckt von |
|---|---|---|
| `outStreamSync` | `DrawThread.run()`, `PlayCreature.behavior` (am Ende), `MotherCreature.attack` | `Game.gaming()` Init, `PlayCreature` Aktionen, Touch‑Move, `MotherCreature.behavior` |
| `gameSync` | `PlayCreature.behavior()` (wenn `pathing` leer) | `MainGameActivity.OnClick`, `MySurfaceView` Tap‑Up |
| `animationSync` | `AnimationThread.run`, `MotherCreature.move`, `MotherCreature.attack` | `MotherCreature.move/attack` setzen Animation, dann notifyAll |

---

## 5. Zusammenfassung der Designentscheidungen

* **Singleton‑Engine über statische Felder**: Maximale Kopplung, einfacher Zugriff, keinerlei Testbarkeit. Pro Spielinstanz existiert genau ein Spielzustand.
* **„Mother…"‑Vererbung** (Mother{Creature, Item, Map}) als Pattern für Erweiterung. Jede Subklasse setzt nur Default‑Werte oder überschreibt 1–2 Methoden; eigentliches Verhalten liegt großteils in der Basisklasse.
* **DrawId‑Indirektion** entkoppelt Spiellogik von der Bitmap‑Größe; ermöglicht Resizing zur Laufzeit (Pinch‑Zoom).
* **Cooperative Threading** mit drei Locks: Logik (`GameThread`), Standardrendering (`DrawThread`) und Animation (`AnimationThread`) wechseln sich ab. Es gibt keinen festen FPS‑Takt, jedes `notifyAll/wait`‑Paar treibt einen Frame.
* **String‑basierte Befehle** für Touch‑Auswertung und Pfadrichtungen – einfach zu verlängern, aber nicht typsicher.
* **Prozedurale Dungeon‑Generierung** über zufällige Räume + L‑Korridore + zufällige Treppen + Mob‑Spawns; deterministisch durch `Math.random()`, ohne Seed‑Steuerung.
* **Vorberechnete Sichtfeld‑Strahlen vom Spieler** statt expliziter Cone‑/Symmetrie‑Algorithmen – einfach, aber asymmetrisch in den Eckfällen.
* **Kein Persistenzmechanismus** (kein Save/Load); jeder App‑Start beginnt im `TestDungeon`.

## 6. Hinweise für künftige Arbeit

* Vor Bugfixes empfiehlt sich, die **vertauschten X/Y‑Indizes** im Code projektweit zu auditieren und zu dokumentieren, an welchen Stellen die Konvention `field[y][x]` und an welchen `field[x][y]` gilt.
* Die **leeren `catch`‑Blöcke** in `Game.gaming()`, `MotherCreature.move`, `AnimationThread` etc. erschweren die Diagnose; bei Refactor zuerst dort Logging hinzufügen.
* Die **Animationsschleifen in `AllBitmaps.changeSize()`** verlieren alle bis auf das letzte Frame – Animation ist daher de facto Standbild. Beim Beheben sollte der `clear()`‑Aufruf vor die Schleife verschoben werden.
* Der `synchronized (waitingThing)` in `Game.gaming()` ist wirkungslos und kann entfernt werden.
* `RoomHelper.heigth` und sonstige Tippfehler in Public‑APIs sind weit verbreitet; jede Umbenennung berührt mehrere Dateien.
