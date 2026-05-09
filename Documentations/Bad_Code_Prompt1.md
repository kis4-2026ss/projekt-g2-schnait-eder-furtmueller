# Dokumentation: The Game No Name

## 1. Projektübersicht und Zweck

**The Game No Name** ist ein **rundenbasiertes Dungeon-Crawler-Spiel** (Roguelike) für Android-Geräte. Der Spieler steuert einen Helden durch zufallsgenerierte Dungeons, kämpft gegen Monster, sammelt Gold und Ausrüstung und versucht, immer tiefere Dungeon-Ebenen zu erreichen.

Das Projekt orientiert sich an klassischen Genre-Vertretern wie *NetHack* oder *Dungeon Crawl* und setzt deren Mechaniken in einer mobilen, touch-bedienbaren Oberfläche um.

**Eckdaten:**
- **Plattform:** Android (nativ)
- **Programmiersprache:** Java
- **Build-System:** Gradle (mit Gradle Wrapper)
- **Grafik:** Eigene 2D-Sprites, gerendert über `SurfaceView`
- **Steuerung:** Touch-Eingabe (Tippen zur Bewegung, Wischen für die Kamera)
- **Genre:** Rundenbasiertes RPG / Roguelike
- **Ziel-API:** Android 4.0+ (minSdkVersion 15, targetSdkVersion 21)

Das Projekt ist ein **Studienprojekt / Prototyp** – es zeigt die Grundstruktur eines kompletten kleinen Spiels (Charaktersystem, Inventar, Karten, Gegner, Kämpfe), ohne den vollen Umfang eines fertigen kommerziellen Titels zu erreichen. Funktionen wie Sound, Speichern/Laden oder ein vollständig ausbalanciertes Progressionssystem fehlen.

---

## 2. Architektur und Projektstruktur

Das Projekt folgt der typischen **Android-Gradle-Projektstruktur** mit einem einzigen Anwendungsmodul (`app`).

### Verzeichnisstruktur (Hauptebene)

| Ordner / Datei | Rolle |
|----------------|-------|
| `app/` | Hauptmodul – enthält den gesamten Quellcode, Ressourcen und das `AndroidManifest.xml` |
| `gradle/` | Gradle Wrapper – sorgt dafür, dass auf jedem Rechner die gleiche Gradle-Version verwendet wird |
| `gradlew`, `gradlew.bat` | Skripte zum Aufruf von Gradle unter macOS/Linux bzw. Windows |
| `build.gradle` (Root) | Projektweite Build-Konfiguration |
| `settings.gradle` | Gibt an, welche Module zum Build gehören (hier nur `app`) |
| `gradle.properties` | Gradle-Einstellungen (z. B. JVM-Speicher) |
| `asdasdasd/` | Historisches Artefakt (zusätzliches Git-Repository), für die Anwendung nicht relevant |
| `.idea/` | Konfigurationsdateien von IntelliJ / Android Studio |

### Inhalt des `app/`-Moduls

| Pfad | Rolle |
|------|-------|
| `app/build.gradle` | Modulbezogene Build-Konfiguration (Abhängigkeiten, Build-Typen) |
| `app/src/main/AndroidManifest.xml` | Beschreibt die Activities, Berechtigungen und das Verhalten der App |
| `app/src/main/java/...` | Sämtliche Java-Klassen (Spielelogik, Rendering, Steuerung) |
| `app/src/main/res/drawable/` | Sprite-Grafiken (PNG): Held, Goblin, Münzen, Wände, Böden, Treppen, Items |
| `app/src/main/res/layout/` | XML-Oberflächen für Menü und Spielfeld |
| `app/src/main/res/values/` | Strings, Farben, Stile |

### Java-Paketstruktur

Der Code ist nach Verantwortlichkeiten in mehrere Unterpakete gegliedert:

| Paket | Aufgabenbereich |
|-------|-----------------|
| `myapplication` (Wurzel) | Einstiegspunkte (Activities), Game-Loop, Bitmap-Verwaltung, gemeinsame Parameter |
| `Creatures/` | Spielfigur und Gegner (Held, Goblin, gemeinsame Basisklasse) |
| `Items/` | Inventar-Items, Waffen, leere Slots, Item-Typen |
| `Items/Weapons/` | Nahkampf- und Fernkampfwaffen, konkrete Waffentypen (z. B. Holzschwert) |
| `Map_etc/` | Karten-Repräsentation, Raumgenerator, Dungeon-Varianten |
| `Map_etc/DungeonTypes/` | Konkrete Dungeon-Layouts (Test-Dungeon, flacher Dungeon) |
| `Tiles/` | Spielfeld-Kacheln (Boden, Wand, Treppe nach oben/unten) |
| `Tiles/StandardDungeon/` | Konkrete Kachelvarianten für den Standard-Dungeon |
| `Outstreams/` | Rendering und Eingabe: Surface-View, Zeichnen-Thread, Animations-Thread |
| `Helpers/` | Hilfslogik für Kampf (Trefferberechnung) und Pathfinding |

---

## 3. Hauptkomponenten und ihr Zusammenspiel

Das Spiel besteht aus mehreren großen Bausteinen, die ineinandergreifen, um aus statischen Bildern ein interaktives, rundenbasiertes Spielerlebnis zu machen.

### 3.1 Einstiegspunkte (Activities)

- **MainActivity** – Startbildschirm mit dem Titel und einem „Play"-Button. Sie bereitet die Anwendung vor und übergibt die Kontrolle ans eigentliche Spiel.
- **MainGameActivity** – Hier läuft das Spielgeschehen. Sie hält die Spielfläche (`SurfaceView`), startet die Threads und nimmt die Touch-Eingaben entgegen.

### 3.2 Spielwelt (Karten, Räume, Kacheln)

Die Spielwelt ist als **gitterbasierte Karte** organisiert. Jede Karte besteht aus vielen Kacheln (`Tile`), die unterschiedliche Bedeutungen haben können:

- **Boden** – begehbar
- **Wand** – blockiert
- **Treppe nach oben / nach unten** – wechselt die Dungeon-Ebene

Ein **Dungeon-Generator** (`Mothermap` mit Hilfsklassen wie `RoomHelper`) erzeugt daraus Räume und Verbindungen. Es existieren mehrere **Dungeon-Typen** (z. B. ein Test-Dungeon und ein „flacher" Dungeon), die unterschiedliche Layout-Varianten anbieten.

### 3.3 Kreaturen (Spieler und Gegner)

Alle lebenden Wesen erben von einer gemeinsamen Basisklasse (`MotherCreature`), die Eigenschaften wie Lebenspunkte, Mana, Stärke, Rüstung, Erfahrung usw. bündelt:

- **PlayCreature** – die vom Spieler gesteuerte Figur. Sie wartet auf eine Eingabe und führt dann den entsprechenden Zug aus (Bewegen, Angreifen, Treppe nutzen).
- **Goblin** – ein Beispielgegner mit eigener Verhaltenslogik, der den Spieler verfolgt und angreift.

### 3.4 Items und Ausrüstung

Alle Items leiten sich von einer Basisklasse (`MotherItem`) ab. Es gibt:

- **Leere Slots** als Platzhalter für noch nicht belegte Inventarplätze
- **Zufalls-Items** für die zufällige Vergabe von Ausrüstung
- **Waffen**, getrennt nach Nah- und Fernkampf, mit konkreten Ausprägungen (z. B. Holzschwert)

Die Spielfigur trägt ein **Inventar mit mehreren Slots**, in denen Waffen, Rüstung, Ringe etc. liegen können.

### 3.5 Rendering und Eingabe

Die Darstellung läuft nicht über das normale Android-View-System, sondern über eine **eigene Zeichenfläche** (`MySurfaceView`), die von zwei Hintergrund-Threads versorgt wird:

- **DrawThread** – zeichnet permanent den aktuellen Spielzustand (Karte, Figur, Gegner, Items).
- **AnimationThread** – kümmert sich um Bewegungs- und Effektanimationen.

Touch-Events des Nutzers werden vom `SurfaceView` aufgenommen und an die Spiellogik weitergereicht, wo sie als Bewegungs- oder Aktionsbefehl interpretiert werden.

### 3.6 Spiel-Loop und Synchronisation

Den Kern bildet ein **Game-Thread** (`GameThread`), der die Hauptklasse `Game` ausführt:

1. Initialisierung der Bitmaps, des ersten Dungeons und der Spielfigur.
2. Platzieren von Spieler und Gegnern auf zufälligen begehbaren Kacheln.
3. **Rundenschleife**: Der Spielzug der `PlayCreature` wartet auf Eingabe → führt Aktion aus → anschließend handeln die Gegner.
4. Zwischen Spiel-, Zeichen- und Animations-Thread sorgt eine **Synchronisationsklasse** (`TouchAndThreadParams`) dafür, dass die Threads sich nicht in die Quere kommen.

### 3.7 Hilfsklassen

- **AllBitmaps** – lädt alle Sprite-Grafiken einmal zentral und stellt sie der Anwendung skaliert zur Verfügung.
- **AttackHelper** – kapselt die Berechnung von Angriffen (Schaden, Treffer).
- **FindingHelper** – liefert einfache Pathfinding-Logik für Spieler- und Monsterbewegung.
- **Params** – gemeinsame, projektweit genutzte Parameter und Konstanten.

### 3.8 Wie alles zusammenspielt – im Überblick

```
   MainActivity ──Play──▶ MainGameActivity
                              │
                              ▼
                          MySurfaceView ◀── Touch-Eingaben
                              │
              ┌───────────────┼────────────────┐
              ▼               ▼                ▼
         GameThread      DrawThread      AnimationThread
              │
              ▼
            Game ──────▶ Mothermap (Karte, Kacheln, Räume)
              │      ──▶ PlayCreature + Gegner
              │      ──▶ Items / Inventar
              │      ──▶ Helpers (Angriff, Pathfinding)
              │      ──▶ AllBitmaps (Sprites)
              ▼
       TouchAndThreadParams (Synchronisation)
```

Das Zusammenspiel ist klassisch: Eine Karte hält die Welt, Kreaturen und Items leben auf der Karte, ein Spiel-Thread treibt die Logik voran, ein Zeichen-Thread macht das Ergebnis sichtbar, und Touch-Events des Nutzers fließen über die Surface zurück in die Logik.

---

## 4. Setup- und Nutzungsanleitung

### 4.1 Voraussetzungen

- **Android Studio** (empfohlen) oder eine kompatible IDE mit Android-Unterstützung
- **Java Development Kit (JDK)** – passend zur verwendeten Android-Studio-Version
- **Android SDK** mit installierter API-Plattform (mind. API 15, ideal API 21)
- Ein **Android-Gerät** (mit aktiviertem USB-Debugging) **oder** ein **Android-Emulator**

> Hinweis: Das Projekt nutzt eine **sehr alte Version** des Android-Gradle-Plugins. Bei aktuellen Android-Studio-Versionen kann ein Upgrade des Gradle-Wrappers und des Plugins nötig sein, bevor das Projekt gebaut werden kann.

### 4.2 Projekt öffnen

1. Repository / Ordner auf den lokalen Rechner kopieren.
2. In Android Studio über **„Open an existing project"** den Ordner `The_Game_No_Name-master` auswählen.
3. Android Studio das Projekt synchronisieren lassen (Gradle Sync). Eventuell auftretende Aufforderungen zum Update von Gradle oder Plugins akzeptieren.

### 4.3 Build über die Kommandozeile

Im Projektwurzelverzeichnis lassen sich folgende Befehle ausführen:

| Befehl | Wirkung |
|--------|---------|
| `./gradlew build` (macOS/Linux) bzw. `gradlew.bat build` (Windows) | Baut das Projekt komplett |
| `./gradlew assembleDebug` | Erzeugt das Debug-APK unter `app/build/outputs/apk/` |
| `./gradlew installDebug` | Installiert die Debug-Version auf einem angeschlossenen Gerät / Emulator |

### 4.4 Spiel starten

1. Android-Gerät anschließen oder Emulator starten.
2. In Android Studio auf **Run** klicken oder `./gradlew installDebug` ausführen.
3. Die App **„The Game No Name"** auf dem Gerät öffnen.
4. Im Startbildschirm auf **„Play"** tippen, um in das Spielfeld zu wechseln.

### 4.5 Steuerung im Spiel

- **Tippen auf eine Kachel:** Spielfigur bewegt sich dorthin (oder greift an, wenn dort ein Gegner steht).
- **Wischen:** Kamera über das Spielfeld verschieben.
- **Treppen:** Durch Bewegen auf eine Treppe wechselt der Held die Dungeon-Ebene.

### 4.6 Bekannte Einschränkungen

- **Kein Speichern/Laden:** Wird das Spiel beendet, geht der Fortschritt verloren.
- **Kein Sound** vorgesehen.
- **Begrenzter Inhalt:** Nur ein Gegnertyp (Goblin) und wenige Itemtypen sind ausgearbeitet.
- **Veraltetes Build-Setup:** Auf modernen Toolchains kann ein manuelles Upgrade der Gradle- und Plugin-Versionen erforderlich sein.

---

*Diese Dokumentation beschreibt das Projekt auf konzeptueller Ebene. Für Implementierungsdetails siehe den Quellcode unter `app/src/main/java/`.*
