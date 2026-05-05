# The Game No Name — Projektdokumentation

## 1. Überblick

**The Game No Name** ist ein in Java geschriebenes Android-Spiel im Stil eines klassischen
Roguelike-Dungeon-Crawlers. Der Spieler bewegt sich rundenbasiert über ein
prozedural generiertes 100×100-Kachelfeld, kämpft gegen Monster (z. B. Goblins),
sammelt Items, rüstet Ausrüstung aus und steigt über Treppen zwischen
Dungeon-Ebenen auf bzw. ab.

Das Projekt stammt ursprünglich von **framaz1** (laut Quell-Header, ab 16.02.2015)
und wurde im Rahmen einer Lehrveranstaltung (FH, 4. Semester, KIS) verwendet.
Die UI-Sprache der Code-Kommentare ist teilweise Russisch.

## 2. Technischer Stack

| Komponente            | Wert                                  |
|-----------------------|---------------------------------------|
| Plattform             | Android                               |
| Sprache               | Java                                  |
| Build-System          | Gradle 1.0.0                          |
| `compileSdkVersion`   | 21 (Android 5.0 Lollipop)             |
| `minSdkVersion`       | 15 (Android 4.0.3)                    |
| `targetSdkVersion`    | 21                                    |
| Build Tools           | 21.1.2                                |
| Application-ID        | `com.example.framaz1.myapplication`   |
| Abhängigkeiten        | `com.android.support:appcompat-v7:21.0.3` |

> Hinweis: Das Projekt nutzt einen sehr alten Gradle- und SDK-Stand. Für einen
> aktuellen Build wären Anpassungen an `build.gradle` (Plugin-Version, SDK-Stufe,
> AndroidX-Migration) erforderlich.

## 3. Projektstruktur

```
The_Game_No_Name-master/
├── app/
│   ├── build.gradle                 → App-Build-Konfiguration
│   ├── app-debug.apk                → vorgebaute Debug-APK
│   └── src/main/
│       ├── AndroidManifest.xml      → Activity-Deklarationen
│       ├── java/com/example/framaz1/myapplication/
│       │   ├── MainActivity.java         → Start-Bildschirm mit Button
│       │   ├── MainGameActivity.java     → eigentliche Spiel-Activity
│       │   ├── Game.java                 → zentrale Spielzustands- und Logikklasse
│       │   ├── GameThread.java           → Thread, der Game.gaming() ausführt
│       │   ├── Params.java               → globale UI-/Display-Parameter
│       │   ├── AllBitmaps.java           → zentrale Bitmap-Verwaltung
│       │   ├── TouchAndThreadParams.java → Touch-Eingabe + Zeichenroutinen
│       │   ├── Creatures/                → Spieler & Monster
│       │   ├── Items/                    → Gegenstände inkl. Waffen
│       │   ├── Tiles/                    → Kacheltypen (Boden, Wand, Treppen)
│       │   ├── Map_etc/                  → Karten- und Dungeon-Generierung
│       │   ├── Outstreams/               → SurfaceView, Render-/Animations-Threads
│       │   └── Helpers/                  → kleine Hilfsklassen
│       └── res/
│           ├── drawable/            → Sprites (Charakter, Goblin, Tiles, UI)
│           ├── layout/              → activity_main.xml, game_field.xml
│           └── values/              → Strings, Dimens, Styles
├── build.gradle                     → Top-Level-Build
├── gradle/                          → Gradle Wrapper
├── gradle.properties
├── gradlew, gradlew.bat
└── settings.gradle
```

## 4. Architektur

### 4.1 Activities

- **`MainActivity`** — Startbildschirm. Liest die Display-Resources, zeigt einen
  Button und startet bei Klick `MainGameActivity`. Setzt das Spiel fest auf
  Hochformat.
- **`MainGameActivity`** — Lädt das `MySurfaceView` als Content-View, liest die
  Display-Metriken in `Params.displaySettings`, startet den `GameThread` und
  registriert einen `OnClickListener`, der einzelne Tap-Eingaben an
  `Game.whereToGoX/Y` durchreicht.

### 4.2 Threading-Modell

Drei Threads laufen parallel und synchronisieren über drei statische
Lock-Objekte in `TouchAndThreadParams`:

| Thread             | Aufgabe                                                                  |
|--------------------|--------------------------------------------------------------------------|
| `GameThread`       | Spiellogik: Spielerzug, KI-Züge, Pfadfindung, Sichtfeld                  |
| `DrawThread`       | Render-Loop in der `SurfaceView`. Zeichnet Tiles, Kreaturen, UI          |
| `AnimationThread`  | Übernimmt Bewegungs- und Angriffs-Animationen Frame für Frame            |

Synchronisationsobjekte:

- `gameSync` — wartet auf Spielereingaben (Tap)
- `outStreamSync` — koordiniert Spiellogik ↔ Renderer
- `animationSync` — koordiniert Logik ↔ Animation

Das Spiel ist **rundenbasiert mit Wartezeiten**: Jede Kreatur hat einen
`toWait`-Zähler. Solange er > 0 ist, wartet sie. Aktionen (Bewegen, Angriff)
erhöhen `toWait` abhängig von `moveDelay`, Waffen-`delay`, Gewicht und Stats.

### 4.3 Zentrale Klasse: `Game`

Statischer Container für den globalen Spielzustand:

- `gamedepths[100]` — Array von `Mothermap`-Instanzen (die Dungeon-Ebenen)
- `layer` — aktuelle Tiefe
- `player` — Singleton-Instanz von `PlayCreature`
- `isSeen[100][100]` — aktuelles Sichtfeld
- `pathFindingHelpArray[100][100]` — Distanzkarte für Pfadsuche

Wichtige Methoden:

- `gaming()` — Hauptschleife. Initialisiert Bitmaps, baut den Test-Dungeon,
  setzt Spieler/Goblin, ruft danach in Endlosschleife `behavior()` von Spieler
  und allen Kreaturen.
- `pathFinding(...)` — BFS auf dem Tile-Feld in 8 Richtungen. Erzeugt eine
  `LinkedList<String>` mit Bewegungskommandos (`Up`, `DownLeft`, …).
- `fieldOfView()` — Raycasting in alle vier Bildschirmkanten, setzt
  `isSeen[][]` und `Tile.wasSeen`.
- `changeLevel(fromWhat, toWhere)` — Treppen-Logik. Generiert die neue Ebene
  bei Bedarf (über `ShallowDungeon` für Tiefen 0–4) und sucht den passenden
  `linker`, um den Spieler dort zu platzieren.
- `placeCreature(...)` — spawnt z. B. Goblins (ID 102).

## 5. Spielwelt

### 5.1 Karten (`Map_etc`)

- **`Mothermap`** — Basisklasse. Hält ein `Tile[100][100]` und eine
  `LinkedList<MotherCreature>`. Das Flag `generated` verhindert
  Mehrfachgenerierung beim Treppenwechsel.
- **`TestDungeon`** — Komplett begehbarer Boden mit einer Wand zu
  Demo-Zwecken; wird beim Spielstart geladen.
- **`ShallowDungeon`** — Prozedurale Generierung:
  1. Karte komplett mit `DungeonWall` füllen.
  2. Eine zufällige Anzahl Räume (10–19) mit zufälliger Position/Größe in
     `DungeonFloor` umwandeln.
  3. Jede neue Raum-Mitte mit der vorherigen über horizontale + vertikale
     Korridore verbinden.
  4. 3 zufällige Felder als `DungeonDownStairway` (linker 1–3) und
     3 als `DungeonUpStairway` (linker 6–8) markieren.
  5. 10 Goblins zufällig auf begehbaren Feldern platzieren.
- **`RoomHelper`** — kleines Wertobjekt für Raum-Position und -Größe.

### 5.2 Tiles (`Tiles`)

- `Tile` — Basisklasse mit Flags `iswall`, `isMobHere`, `wasSeen`,
  `interractable`, einem `goldHere`-Counter, einem `Stack<MotherItem>` für
  abgelegte Items, einer `movedelay` und der Treppen-Verknüpfung `linker`.
- `Floor`, `Wall` — Basistypen für begehbar/blockierend.
- `UpStairway`, `DownStairway` — implementieren `toInteract(...)`, das
  `Game.changeLevel(...)` aufruft.
- `StandardDungeon/*` — konkrete Sprites (Drawable-IDs) für den Standardlook.

### 5.3 Kreaturen (`Creatures`)

- **`MotherCreature`** — Basisklasse aller Kreaturen. Felder u. a.:
  - Stats: `health`, `maxHP`, `mana`, `maxMana`, `str`, `agi`, `intel`,
    `attack`, `deffense`, `weight`
  - Position: `xCoordinates`, `yCoordinates`
  - Inventar: `items[20]`, fünf Equip-Slots (`helmet`, `bodyWear`, `weapon`,
    `ring1`, `ring2`, `jewel`)
  - Zeitsteuerung: `moveDelay`, `toWait`
  - Sicht: `maxVision`
  - Methoden: `move(...)`, `attack(...)`, `equip(...)`, `unEquip(...)`,
    `take()`, `drop(...)`, `addExp(...)`, `die()`, `behavior()` (KI)

- **`PlayCreature`** — Erbt von `MotherCreature` und überschreibt `behavior()`
  zu einem Eingabe-getriebenen Zustandsautomaten. Mögliche Aktionen:
  `Move`, `Wait`, `Inventory`, `Stats`, `TakeFromFloor`, `Interact`,
  `ChooseItem`, `Back`, `PushInventoryButton`, `BackToInventory`, `StatUp`.
  Startwerte: 10 HP, 10/15 Mana, alle Grundstats 10, zwei `WoodenSword` im
  Inventar, `maxVision=10000` (de facto unbegrenzt).

- **`Goblin`** — Standard-Gegner. 3 HP, Stats 3/3/3, `aggred=true`, gibt 1 EP.

### 5.4 Items (`Items`)

- **`MotherItem`** — Basisklasse. Felder: `type` (`ItemType`-Enum), `name`,
  `description`, `picture`, `delay`, Bedarfswerte (`strNeeded`, `agiNeeded`,
  `intNeeded`), Bonuswerte (`strBonus`, `agiBonus`, `intBonus`, `bonus`),
  Gewicht und die Flags `equipable`, `usable`. Implementiert `equip(...)` und
  `unEquip(...)` mit slot-spezifischer Stat-Anwendung.
- **`ItemType`** — Enum: `Helm`, `BodyWear`, `MeeleWeapon`, `RangedWeapon`,
  `Ring`, `Jewel`, `Consumable`, `Quest`, `Ammo`.
- **`EmptyItem`** — Platzhalter für leere Inventarslots.
- **`RandomItem`** — Stub (noch nicht implementiert).
- **`Weapons/MeeleWeapon`**, **`Weapons/RangedWeapon`** — Waffen-Subtypen.
- **`Weapons/Meeles/WoodenSword`** — einzige konkrete Waffe; `bonus=3`,
  `strNeeded=3`, `agiNeeded=2`, `delay=1`.

### 5.5 Helpers

- `AttackHelper` — Datentransfer-Objekt für Angriff-Animationen
  (`attacker`, `attacked`, `damage`, `missed`, `stunned`, `scared`).
- `FindingHelper` — einfaches `(x, y)`-Wertobjekt für die BFS-Queue der
  Pfadsuche.

## 6. Kampfsystem

In `MotherCreature.attack(int x, int y)` umgesetzt. Ablauf je Angriff:

1. **Wartezeit (`toWait`)** wird basierend auf `weapon.delay`, Spieler-Gewicht
   und Mindest-Stats der Waffe skaliert.
2. **Trefferchance** beginnt bei 90 %, wird durch das Verhältnis
   Verteidiger-Gewicht/Stärke und Differenz der Agility reduziert (Minimum 30 %).
3. **Schaden** = `attack/2 + Random*attack/2 + 1`,
   **Verteidigung** analog aus `deffense`.
4. **Krit-Chance** = `(str + agi) / 2` % → verdoppelter Schaden.
5. Wenn Treffer: `damage = max(toHit - toDef, 1)` wird vom `health` abgezogen.
6. Animation per `AnimationThread.setAttackAnimation(helper)`.
7. Bei `health <= 0` ruft der Aufrufer `die()` auf — die Kreatur lässt alle
   Items und ihr Gold auf der Kachel fallen, ihr Eintrag wird aus
   `gamedepths[layer].creatures` entfernt.

## 7. Pfadsuche & Sichtfeld

### 7.1 Pfadsuche (`Game.pathFinding`)

BFS auf dem Tile-Gitter mit 8 Nachbarn. Gefüllt wird `pathFindingHelpArray`,
beginnend beim Zielfeld mit Distanz 0. Begrenzt durch `from.maxVision`. Walls
und besetzte Felder werden ausgeschlossen — außer das Zielfeld ist genau das
Startfeld der suchenden Kreatur. Der Rückweg vom Start zum Ziel wird durch
absteigendes Folgen der Distanzwerte rekonstruiert und als Liste von
Richtungs-Strings zurückgegeben.

### 7.2 Sichtfeld (`Game.fieldOfView`)

Raycasting: Für jede der vier Bildschirmkanten werden Strahlen vom Spieler aus
abgeschossen (Geradengleichung `y = k*x + b`). Jeder Strahl markiert Felder
in `isSeen` und setzt `Tile.wasSeen=true`, bis er auf eine Wand trifft.
Bereits einmal gesehene Felder werden im DrawThread mit `shadow.png`
verdunkelt dargestellt.

## 8. Rendering & Eingabe

### 8.1 `MySurfaceView`

Erbt von `SurfaceView` und implementiert `SurfaceHolder.Callback`. In
`surfaceCreated()` werden `DrawThread` und `AnimationThread` gestartet.

`onTouchEvent` interpretiert:

- **Einzel-Tap** (kurzer Druck < 250 ms, kaum Bewegung) →
  `Game.whereToGoX/Y` setzen, `gameSync.notifyAll()`.
- **Drag mit einem Finger** → Kamera (`Params.displayX/Y`) verschieben.
- **Pinch (zwei Finger)** → `Params.size` (Tile-Größe) zwischen 32 und 96 px
  skalieren; `AllBitmaps.changeSize()` skaliert alle Bitmaps neu.

### 8.2 `DrawThread`

Rendert in einer Endlosschleife auf das `Canvas`:

1. Schwarzer Hintergrund.
2. `TouchAndThreadParams.drawAllTilesOnTheField(canvas)` — alle sichtbaren
   Tiles im aktuellen Viewport plus Items/Gold auf der jeweiligen Kachel.
3. Spieler und Kreaturen (gespiegelt, falls
   `orientatedToRight == false`).
4. UI-Overlay: Wait-/Magic-/Inventar-/Stats-Icon, HP/Mana/EP-Balken oben.
5. Bedingt eingeblendet: Inventar (`drawInventory`), Item-Detail
   (`drawItem`), Stats-View (`drawStats`).

### 8.3 `AnimationThread`

Wird von `MotherCreature.move(...)` und `attack(...)` über
`setMoveAnimation(...)` / `setAttackAnimation(...)` getriggert. Animiert
Bewegung Pixel für Pixel zwischen alten und neuen Koordinaten und überlagert
bei Angriffen Schadenszahlen / Miss-Sprite.

## 9. UI-States

`Params` verwaltet sechs sich gegenseitig ausschließende UI-Modi über boolesche
Flags und Setter:

- `gameField` — Standardansicht (Karte)
- `inventory` — Inventar-Raster mit Equip-Slots
- `item` — Detailansicht eines einzelnen Items mit Buttons
  Equip/Unequip, Use, Drop
- `stats` — Charakterwerte mit Plus-Buttons (`StatUp`) zum Ausgeben von
  `lvlUps`-Punkten
- `menu`, `map` — definiert, aber im Code nicht aktiv genutzt

Die zentrale Methode `TouchAndThreadParams.getWhatPlayerDesired()` mappt die
Tap-Koordinaten je nach aktivem UI-Modus auf einen Aktions-String, den
`PlayCreature.behavior()` per Switch verarbeitet.

## 10. Bekannte Bugs

> Alle Stellen wurden im Code verifiziert. **Hier sind keine Fixes
> implementiert** — der Abschnitt dokumentiert nur, was im aktuellen Stand
> beobachtbar ist.

### Bug 1 — `MotherItem.unEquip()`: Switch-Fall-Through zieht Boni mehrfach ab

**Datei:** `app/src/main/java/com/example/framaz1/myapplication/Items/MotherItem.java`
**Zeilen:** 104–134

```java
switch (type) {
    case Helm:
        creature.str -= strBonus;
        creature.intel -= intBonus;
        creature.agi -= agiBonus;
        creature.weight -= weight;
        creature.deffense -= bonus;
    case BodyWear:
        creature.str -= strBonus;
        ...
    case MeeleWeapon:
    case RangedWeapon:
        ...
    case Jewel:
        ...
    case Ring:
        ...
}
```

**Symptom:** Es gibt keine `break`-Statements. Beim Ablegen eines `Helm`
fallen alle nachfolgenden Cases (`BodyWear`, `MeeleWeapon`, `RangedWeapon`,
`Jewel`, `Ring`) durch, sodass `str`, `intel`, `agi` und `weight` bis zu
fünfmal abgezogen werden, dazu `deffense` zweimal und `attack` einmal —
obwohl nur ein einzelner Helm abgelegt wurde.
Beim Anlegen (`equip(...)` in derselben Datei) ist das korrekt mit `return`
gelöst, beim Unequip nicht.

**Konsequenz:** Stats werden negativ, Spieler kann unbeabsichtigt einen
HP-Bonus durch das Ent-Ausrüsten erhalten/verlieren. Cumulativer Drift bei
mehrfachem An-/Ablegen.

---

### Bug 2 — `MotherCreature.unEquip(int what)`: Switch-Fall-Through trifft falschen Slot

**Datei:** `app/src/main/java/com/example/framaz1/myapplication/Creatures/MotherCreature.java`
**Zeilen:** 66–102

```java
switch (what) {
    case -5:
        if (!bodyWear.name.equals("") && bodyWear.unEquip(this)) {
            bodyWear = new EmptyItem();
            return true;
        }
    case -4:
        if (!weapon.name.equals("") && weapon.unEquip(this)) { ... return true; }
    case -3:
        if (!jewel.name.equals("") && jewel.unEquip(this))   { ... return true; }
    case -2:
        if (!ring1.name.equals("") && ring1.unEquip(this))   { ... return true; }
    case -1:
        if (!ring2.name.equals("") && ring2.unEquip(this))   { ... return true; }
}
```

**Symptom:** Auch hier fehlen die `break`s nach dem `if`. Wenn der Spieler
z. B. `case -5` (BodyWear) auswählt und der BodyWear-Slot leer ist, springt
die Kontrolle in `case -4` und versucht, die ausgerüstete Waffe abzulegen.
Statt einer No-Op wird also der nächste belegte Slot „nach unten" geleert.

**Konsequenz:** Im Inventar-UI führt ein Klick auf einen leeren Equip-Slot
dazu, dass irgendein nachfolgender Slot ausgerüstet wird — kombiniert mit
Bug 1 ein doppelter Treffer: falscher Slot wird geleert **und** Stat-Boni
fallen falsch ab.

---

### Bug 3 — `PlayCreature.behavior()`: `case "Interact"` ohne `break`

**Datei:** `app/src/main/java/com/example/framaz1/myapplication/Creatures/PlayCreature.java`
**Zeilen:** 126–132

```java
case "Interact":
    Game.gamedepths[Game.layer].field[yCoordinates][xCoordinates].toInteract(this);
    toWait = (int)(moveDelay);
    Game.doneMyTurn = true;
//In Inventory

case "ChooseItem":
    int widthOfInv = AllBitmaps.inventoryImage.getWidth();
    ...
```

**Symptom:** Kein `break` nach `Game.doneMyTurn = true;`. Nach jedem
Treppen-/Interact-Event fällt der Switch in `case "ChooseItem"` durch und
führt Inventar-Klick-Logik mit den letzten `whereToGoX/Y`-Werten aus dem
GameField aus. Dabei werden negative Indizes oder zufällige Slots berechnet.

**Konsequenz:** Beim Treppensteigen können unbeabsichtigt Item-Detail-Views
geöffnet werden, oder es kommt zu `ArrayIndexOutOfBoundsException` in der
Slot-Berechnung. Direkt nach einem erfolgreichen `Interact` ist außerdem
`Game.doneMyTurn` schon `true`, und der Code in `ChooseItem` ändert den
Zustand erneut.

---

### Bug 4 — `MotherCreature.die()`: Vertauschte Koordinaten

**Datei:** `app/src/main/java/com/example/framaz1/myapplication/Creatures/MotherCreature.java`
**Zeile:** 313

```java
Game.gamedepths[Game.layer].field[xCoordinates][yCoordinates].isMobHere = false;
int whom = Game.findCreatureByCoordinater(xCoordinates, yCoordinates);
```

**Symptom:** Der Rest der Klasse — `move()`, `take()`, `drop()`, `attack()` —
adressiert das Feld konsistent als `field[yCoordinates][xCoordinates]`
(Zeile-vor-Spalte). In `die()` ist die Reihenfolge invertiert. Dadurch wird
beim Tod einer Kreatur das `isMobHere`-Flag auf einer falschen Kachel
zurückgesetzt — solange die Kreatur nicht auf der Diagonale (`x == y`) starb.

**Konsequenz:** Auf der tatsächlichen Sterbe-Kachel bleibt `isMobHere = true`
hängen → Pfadsuche behandelt das Feld als blockiert, und keine andere
Kreatur (auch nicht der Spieler) kann es betreten oder dort liegende Items /
Gold abräumen. Auf der vertauschten Kachel wird hingegen ein eventuell
existierendes `isMobHere` gelöscht, das dort hingehört.

---

### Bug 5 — `MotherCreature.attack()`: Integer-Division `(1/4)`

**Datei:** `app/src/main/java/com/example/framaz1/myapplication/Creatures/MotherCreature.java`
**Zeile:** 198

```java
int hitchance = 90 - 10 * ((int)(who.weight / ((1/4) * who.str * 10)));
```

**Symptom:** `1/4` ist in Java Integer-Division und ergibt `0`. Damit ist
der Divisor `0 * who.str * 10 = 0`. `who.weight` ist `double`, also gibt
`weight / 0` `Double.POSITIVE_INFINITY`, und `(int) Infinity` =
`Integer.MAX_VALUE`. `90 - 10 * Integer.MAX_VALUE` läuft über (Integer
Overflow), das Resultat ist faktisch zufällig.

**Konsequenz:** Die anschließende Klammer `if (hitchance < 30)
hitchance = 30;` rettet die Berechnung in den meisten Fällen auf den
Mindestwert 30 %. Effektiv ist die ganze gewichts-/stärkeabhängige
Trefferchance-Formel deaktiviert — jeder Verteidiger hat de facto immer
30 % Ausweich-Bonus, unabhängig von seinen Stats. Vermutlich war
`(1.0/4)` oder eine echte Stärke-Skalierung gemeint.

---

### Bug 6 — `Game.pathFinding()`: bitweises `&` statt logisches `&&`

**Datei:** `app/src/main/java/com/example/framaz1/myapplication/Game.java`
**Zeile:** 115 (im „Right"-Zweig der BFS)

```java
if (obj.x < 99
    && !gamedepths[layer].field[obj.y][obj.x+1].iswall
    && (!gamedepths[layer].field[obj.y][obj.x+1].isMobHere
        || (obj.x+1 == fromX && obj.y == fromY))
    && !washere[obj.y][obj.x+1]
    & pathFindingHelpArray[obj.y][obj.x+1] == 100000
    & pathFindingHelpArray[obj.y][obj.x] < max)
```

**Symptom:** Die letzten beiden Verknüpfungen verwenden `&` (bitweises AND)
statt `&&`. Damit verschwindet das Short-Circuit-Verhalten, und dank der
Operator-Präzedenz (`==` und `<` binden stärker als `&`) bleibt der
Wahrheitswert glücklicherweise meist korrekt. Die anderen sieben
Richtungs-Zweige verwenden konsistent `&&`.

**Konsequenz:** Aktuell sichtbar funktional, aber inkonsistent zur
Umgebung und potenziell fragil bei Refactoring. Auch werden beide Seiten
des `&` immer ausgewertet, statt früh abzubrechen — minimaler
Performance-Nachteil.

---

### Bug 7 — `AndroidManifest.xml`: zwei Launcher-Activities

**Datei:** `app/src/main/AndroidManifest.xml`
**Zeilen:** 11–28

Beide deklarierten Activities haben einen `intent-filter` mit
`android.intent.action.MAIN` **und** `android.intent.category.LAUNCHER`:

```xml
<activity android:name=".MainActivity"> <intent-filter>...LAUNCHER... </activity>
<activity android:name=".MainGameActivity"> <intent-filter>...LAUNCHER... </activity>
```

**Symptom:** Android legt im App-Drawer zwei Icons unter demselben Namen an
und zeigt beim Installieren möglicherweise einen Auswahldialog.

**Konsequenz:** Wenn der Nutzer das zweite Icon (für `MainGameActivity`)
direkt startet, läuft das Spiel ohne den Init-Pfad in `MainActivity`. In
`MainActivity.onCreate(...)` wird `Params.resource = getResources();`
gesetzt — dieses Feld wird in `AllBitmaps` für `BitmapFactory.decodeResource`
benötigt. Direktstart der `MainGameActivity` führt daher zu
`NullPointerException` beim Bitmap-Laden.

---

### Bug 8 — `Game.changeLevel()`: keine Implementierung für Tiefe ≥ 5

**Datei:** `app/src/main/java/com/example/framaz1/myapplication/Game.java`
**Zeilen:** 272–297

```java
if (gamedepths[toWhere].generated == false) {
    if (toWhere < 5) {
        gamedepths[toWhere] = new ShallowDungeon(toWhere);
        gamedepths[toWhere].generateTheField();
    }
    //TODO more levels
}
```

**Symptom:** Für Tiefen ≥ 5 wird nichts generiert. Der nachfolgende Code
sucht trotzdem nach einem Tile mit `linker == fromWhat` auf dem leeren
`Mothermap`-Feld — `field` enthält dort nur `null`-Einträge.

**Konsequenz:** Das Betreten der 6. Treppe nach unten produziert eine
`NullPointerException` in der Suchschleife (`field[i][j].linker`). Das
TODO-Kommentar bestätigt, dass diese Funktionalität nie fertiggestellt
wurde.

---

### Bug 9 — `RandomItem`: leerer Stub

**Datei:** `app/src/main/java/com/example/framaz1/myapplication/Items/RandomItem.java`

```java
public class RandomItem extends MotherItem {
}
```

**Symptom:** Klasse ist komplett leer; sie wird im aktuellen Code nicht
instanziiert. Vermutlich war eine zufällige Item-Generierung für gefundene
Loot-Drops angedacht.

**Konsequenz:** Funktional kein Bug, da nicht aufgerufen. Tote Code-
Fragmente sind aber bei einem Dependency-Update oder Refactoring leicht
übersehene Falle.

---

### Bug 10 — `PlayCreature`: Debug-Werte im Konstruktor

**Datei:** `app/src/main/java/com/example/framaz1/myapplication/Creatures/PlayCreature.java`
**Zeilen:** 27–28

```java
experience = 9;        //TODO remove this
lvlUps = 3;
```

**Symptom:** Der Spieler startet mit 9 Erfahrungspunkten und drei nicht
verteilten Stat-Punkten. Der `// TODO remove this`-Kommentar markiert das
ausdrücklich als Test-Code.

**Konsequenz:** Kein Crash, aber Game-Balance ist verfälscht — der erste
Goblin-Kill hebt den Spieler sofort auf Level 2.

---

### Bug 11 — Tippfehler `findCreatureByCoordinater`

**Datei:** `app/src/main/java/com/example/framaz1/myapplication/Game.java`
**Zeile:** 257

Methodenname `findCreatureByCoordinater`. Korrekt wäre
`findCreatureByCoordinates` oder `findCreatureByCoordinator`. Reiner
Stilfehler, kein funktionales Problem.

---

### Sonstige Mängel

- `compileSdkVersion 21` / `targetSdkVersion 21` (Android 5.0) ist
  weit veraltet — moderne Geräte / Play Store-Richtlinien fordern API 33+.
- `Mothermap` (Default-Konstruktor) belegt `field` mit `null`-Einträgen.
  Wer eine `Mothermap` direkt instanziiert (passiert in `Game.gaming()`
  für Tiefen 1–99) und dort hineinläuft, ohne `generateTheField()`
  aufzurufen, bekommt NPEs.
- `AllBitmaps.initialize()` lädt Ressourcen, die teilweise schon in den
  Feld-Initialisierern (`BitmapFactory.decodeResource(...)`) entstehen —
  doppelte I/O.
- `Goblin.behavior()` (geerbt) hat keinen Branch für
  `pathing.size() == 0` (Spieler unerreichbar). Der Goblin handelt dann
  einfach nicht und sein `toWait` bleibt 0, sodass `Game.gaming()` ihn
  in jedem Tick neu berechnet — unnötige CPU-Last.

## 11. Offene Stellen / TODOs

- `// TODO more levels` in `Game.changeLevel` (siehe Bug 8).
- `// TODO remove this` in `PlayCreature` (siehe Bug 10).
- `RandomItem` (siehe Bug 9).
- `// TODO all others` in `TouchAndThreadParams.textToPicture` — die
  Bitmap-basierte Textausgabe unterstützt nur Ziffern und Leerzeichen.

## 12. Build & Ausführung

```bash
# Wrapper ausführbar machen (einmalig)
chmod +x gradlew

# Debug-APK bauen
./gradlew assembleDebug

# Auf einem angeschlossenen Gerät installieren
adb install -r app/build/outputs/apk/app-debug.apk
```

Eine vorgebaute Variante liegt unter `app/app-debug.apk`. Aufgrund des alten
Gradle/SDK-Stands kann ein Build mit aktuellen Android-Studio-Versionen
fehlschlagen — eine Migration auf neuere Plugin-/SDK-Versionen ist dann
nötig.

## 13. Steuerung im Spiel

| Geste                                        | Aktion                                       |
|----------------------------------------------|----------------------------------------------|
| Tap auf eine Kachel                          | Hingehen / angreifen / interagieren          |
| Drag mit einem Finger                        | Kamera verschieben                           |
| Pinch mit zwei Fingern                       | Zoom (Tile-Größe 32–96 px)                   |
| Tap unten Mitte (Wait-Icon)                  | Runde überspringen                           |
| Tap unten rechts (Inventar-Icon)             | Inventar öffnen                              |
| Tap oben links (Stats-Icon)                  | Charakter-Stats öffnen                       |
| Tap auf „Get Item Here" (rechts)             | Gold/Items aufheben                          |
| Tap auf „Interact" (rechts, bei Treppe)      | Ebene wechseln                               |
| Tap auf Item im Inventar                     | Item-Detailansicht                           |
| Item-Detail: Equip / Use / Drop              | Entsprechende Aktion                         |
| Stats-Plus (sofern `lvlUps > 0`)             | HP / Mana / Str / Int / Agi um 1 erhöhen     |
