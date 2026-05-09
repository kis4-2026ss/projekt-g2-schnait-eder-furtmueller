# DOCUMENTATION

## 1. Projektübersicht und Zweck

Dieses Projekt ist eine **ereignisorientierte Simulation eines Supermarkts** (Discrete Event Simulation, DES), umgesetzt in Java. Modelliert wird der Ablauf an den Kassen: Kunden treffen über die Zeit verteilt ein, stellen sich an einer Kasse an, werden dort bedient – oder verlassen die Schlange wieder, falls die Wartezeit zu lange wird.

Der Zweck der Simulation ist die **Analyse von Kapazität und Wartezeiten** in einem Supermarkt unter unterschiedlichen Bedingungen. Konkret können damit Fragen beantwortet werden wie:

- Wie viele Kassen werden bei einer bestimmten Kundenfrequenz benötigt?
- Wie hoch ist die durchschnittliche Wartezeit?
- Wie stark sind die einzelnen Kassen über den Tag ausgelastet?
- Wie viele Kunden geben vor der Bedienung auf?

Das Projekt ist als **schichtweise erweiterbares System** aufgebaut: Es gibt eine schlanke Basisversion der Simulation, die um Statistik-Auswertung und um zeitabhängige Ankunftsprofile (z. B. unterschiedliche Tageszeiten / Stoßzeiten) erweitert werden kann. Damit dient das Projekt zugleich als Beispiel für eine saubere, modulare Architektur eines DES-Systems.

---

## 2. Architektur und Projektstruktur

### 2.1 Architekturprinzip

Die Architektur folgt dem klassischen **Discrete-Event-Simulation-Muster**:

- Eine zentrale Simulations-Engine verwaltet eine **Ereigniswarteschlange** (Priority Queue, sortiert nach Zeitpunkt).
- Die Simulation springt **von Ereignis zu Ereignis** in der Zeit, statt in festen Zeitschritten zu laufen.
- Jedes Ereignis kann beim Verarbeiten **neue Folgeereignisse** in die Queue einfügen.
- Auf der Engine setzt eine **Domänenschicht** (Supermarkt, Kasse, Kunde) auf.
- Darüber liegen optionale Erweiterungsschichten für **Statistik** und **Ankunftsprofile**.

Die Erweiterungen sind durch **Vererbung und Schnittstellen** sauber entkoppelt: Die Events erkennen über `instanceof`-Prüfungen, ob die aktuelle Supermarkt-Variante zusätzliche Funktionen bereitstellt, sodass dieselben Events sowohl mit dem einfachen als auch mit dem statistik-fähigen Supermarkt funktionieren.

### 2.2 Projektstruktur

```
Good_Code_example/
└── src/
    ├── main/        ← produktiver Code
    │   ├── sim/         Simulations-Engine
    │   ├── ev/          Ereignistypen
    │   ├── supm/
    │   │   ├── base/        Basis-Supermarkt + Konfiguration + Einstiegspunkt
    │   │   ├── parts/       Domänenobjekte (Kunde, Kasse)
    │   │   ├── statis/      Erweiterung um Statistik-Sammlung
    │   │   └── profiled/    Erweiterung um zeitabhängige Ankunftsraten
    │   ├── profile/     Ankunftsprofile (z. B. tageszeitabhängig)
    │   ├── stats/       Statistik-Auswertung und Ausgabe
    │   └── util/        Hilfsklassen (verstellbare Uhr)
    └── test/        ← Unit- und Integrationstests, gespiegelt zur main-Struktur
```

### 2.3 Rolle der Pakete

| Paket | Rolle |
|---|---|
| `sim` | Generische DES-Engine: Ereigniswarteschlange, Zeitfortschritt, Schritt-Schleife. Weiß nichts von Supermärkten. |
| `ev` | Konkrete Ereignistypen, die im Supermarkt-Kontext auftreten (Ankunft, Bedienungsende, Aufgabe). |
| `supm.base` | Supermarkt als spezialisierte Simulation: hält Kassen, erzeugt Zwischenankunfts- und Bedienzeiten, dient als Einstiegspunkt. |
| `supm.parts` | Die fachlichen Kernobjekte `Customer` und `Checkout`. |
| `supm.statis` | Variante des Supermarkts, die statistische Daten sammelt. |
| `supm.profiled` | Variante, die zusätzlich ein Ankunftsprofil verwendet (z. B. Öffnungszeiten, Spitzenzeiten). |
| `profile` | Abstraktion über Ankunftsraten – inklusive einer periodischen Implementierung (Tagesprofil). |
| `stats` | Sammelt Kennzahlen während der Simulation und erzeugt am Ende Auswertungen (Zusammenfassung + Auslastungstabelle). |
| `util` | Eine manuell vorrückbare Uhr, damit die Simulationszeit von der echten Systemzeit entkoppelt werden kann. |
| `test/...` | Tests, die die jeweiligen Pakete der `main`-Seite spiegeln. |

---

## 3. Hauptkomponenten und Zusammenspiel

### 3.1 Simulations-Engine (`sim`)

Die Engine ist eine generische, **wiederverwendbare DES-Schicht**. Sie kennt nur den abstrakten Begriff eines „Ereignisses“ und verwaltet dieses in einer nach Zeitstempeln sortierten Warteschlange. Sie kann Ereignisse einzeln abarbeiten oder bis zu einem definierten Endzeitpunkt durchlaufen. Beim Verarbeiten eines Ereignisses springt die interne Simulationszeit auf dessen Zeitstempel vor, wodurch Leerlaufzeiten effizient übersprungen werden.

### 3.2 Ereignisse (`ev`)

Es gibt drei konkrete Ereignistypen, die zusammen das Verhalten an der Kasse modellieren:

- **`CustomerArrivalEvent`** – Ein Kunde betritt den Supermarkt. Er wählt die kürzeste Schlange. Ist dort frei, beginnt sofort die Bedienung; andernfalls reiht er sich ein. In jedem Fall wird das nächste Ankunftsereignis des nächsten Kunden geplant, solange die Simulationsdauer noch nicht erreicht ist.
- **`ServiceEndEvent`** – Die Bedienung eines Kunden endet. Steht ein weiterer Kunde in der Schlange dieser Kasse, beginnt seine Bedienung; sonst wird die Kasse freigesetzt.
- **`CustomerAbandonEvent`** – Ein Kunde, der zu lange in der Schlange wartet, gibt auf und verlässt die Kasse. Dieses Ereignis wirkt nur, wenn der Kunde zum geplanten Zeitpunkt tatsächlich noch wartet.

Die Ereignisse erzeugen sich gegenseitig: Eine Ankunft erzeugt eine zukünftige Ankunft sowie entweder ein Bedienungsende oder ein Abbruch-Ereignis. So entsteht eine selbsttragende Ereigniskette, die den gesamten Simulationsablauf bildet.

### 3.3 Domänenobjekte (`supm.parts`)

- **`Customer`** – Repräsentiert einen Kunden mit eindeutiger ID, Ankunftszeit und – sobald bedient – einer Bedienungs-Startzeit.
- **`Checkout`** – Repräsentiert eine Kasse mit eigener Warteschlange, Status (frei/belegt) und einem Zeitstempel, wann sie zuletzt belegt wurde.

### 3.4 Supermarkt-Schichten (`supm.base`, `supm.statis`, `supm.profiled`)

Der Supermarkt ist in **drei aufeinander aufbauenden Schichten** modelliert:

1. **`Supermarket`** (Basis) – erweitert die Simulations-Engine um die supermarkt-spezifische Welt: er hält die Kassen, kennt die Konfiguration, weiß die kürzeste Schlange zu finden und liefert Zufallswerte für Zwischenankunfts- und Bedienzeiten (exponentiell bzw. normalverteilt).
2. **`SupermarketStatistic`** – erbt vom Basis-Supermarkt und delegiert ereignisrelevante Vorgänge (eingereihte Kunden, abgebrochene Kunden, Wartezeiten, Belegtzeiten der Kassen) an ein separates Statistik-Objekt.
3. **`ProfiledSupermarketStatistic`** – erbt vom statistik-fähigen Supermarkt und ersetzt die Berechnung der Zwischenankunftszeit durch eine **profil-basierte Variante**. Damit können Tageszeiten mit unterschiedlicher Frequenz oder Schließzeiten realistisch abgebildet werden.

Das saubere Schichtenmodell hat den Effekt, dass Ereignisse und Engine **unverändert** bleiben können – die jeweils tiefere Schicht bringt zusätzliche Funktionalität durch Überschreiben einzelner Methoden bzw. durch zusätzliche Hooks ein.

### 3.5 Konfiguration (`SimulationParameters`)

Sämtliche Stellschrauben einer Simulation – Anzahl Kassen, mittlere Ankunftsrate, mittlere Bedienzeit und deren Standardabweichung, maximal akzeptable Wartezeit, Simulationsdauer und ob ein Lauf-Protokoll ausgegeben werden soll – werden in einem zentralen Parameter-Objekt zusammengefasst. Dieses Objekt wird beim Aufbau eines Supermarkts übergeben.

### 3.6 Ankunftsprofile (`profile`)

- **`ArrivalProfile`** ist eine Schnittstelle, die zu einer gegebenen Uhrzeit die zugehörige Ankunftsrate liefert.
- **`PeriodicArrivalProfile`** ist eine konkrete, **periodische** Implementierung: Eine Periode (z. B. ein Tag) wird in mehrere Intervalle gleicher Länge geteilt, und jedem Intervall wird eine eigene Ankunftsrate zugewiesen. Liegen mehrere aufeinanderfolgende Intervalle bei Rate Null (z. B. nachts, geschlossen), kann das Profil den nächsten „offenen“ Zeitpunkt berechnen, sodass die Simulation diese Zeiträume überspringen kann.

### 3.7 Statistik (`stats`)

Das Statistik-Modul sammelt während des Laufs Kennzahlen pro Kasse (bediente Kunden, maximale Schlangenlänge, Belegtzeit pro Zeitintervall) sowie globale Werte (gesamte Wartezeit, Anzahl wartender Kunden, abgebrochene Kunden). Die Belegtzeit wird auf gleich lange **Auswertungsintervalle** verteilt, sodass am Ende eine **Auslastungstabelle** über den Simulationszeitraum entsteht.

Am Ende einer Simulation werden zwei Ausgaben erzeugt:

- **Zusammenfassung**: pro Kasse die Anzahl bedienter Kunden und die maximale Schlangenlänge, dazu die Anzahl der Abbrecher und die mittlere Wartezeit.
- **Auslastungstabelle**: pro Zeitintervall die prozentuale Auslastung jeder Kasse sowie der Durchschnitt über alle Kassen.

### 3.8 Hilfskomponente (`util.AdjustableClock`)

Eine manuell weiterstellbare Uhr, die der Simulations-Engine optional übergeben werden kann. Sie sorgt dafür, dass Zeitabfragen an die **Simulationszeit** und nicht an die echte Systemzeit gekoppelt sind – wichtig, damit lange Simulationszeiträume (z. B. mehrere Tage) in Sekunden Realzeit ablaufen können.

### 3.9 Zusammenspiel im Überblick

Vereinfachter Ablauf eines Simulationslaufs:

1. Ein **Client** baut ein `SimulationParameters`-Objekt und davon einen Supermarkt (Basis-, Statistik- oder Profil-Variante).
2. Der Client legt das **erste `CustomerArrivalEvent`** in die Ereigniswarteschlange.
3. `run(...)` wird aufgerufen; die Engine arbeitet die Warteschlange ab, bis der Endzeitpunkt erreicht ist.
4. Jedes Ereignis modifiziert den Zustand des Supermarkts (Kassen, Schlangen) und plant Folgeereignisse.
5. In den Statistik- und Profil-Varianten werden zusätzlich Kennzahlen aktualisiert bzw. die nächste Ankunftszeit über das Profil ermittelt.
6. Nach Simulationsende werden – sofern Statistik aktiv ist – Zusammenfassung und Auslastungstabelle ausgegeben.

---

## 4. Setup- und Nutzungsanleitung

### 4.1 Voraussetzungen

- **Java Development Kit (JDK) 21 oder neuer** (das Projekt nutzt moderne Java-Features wie Pattern-Matching für `instanceof` mit Bindung sowie `List.getFirst()`).
- Eine Java-IDE (z. B. IntelliJ IDEA – das Projekt enthält bereits eine `.iml`-Datei) oder eine Konsole mit `javac`/`java`.
- Keine externen Abhängigkeiten – das Projekt baut ausschließlich auf der Java-Standardbibliothek auf.

### 4.2 Projekt öffnen

1. Das Repository auschecken bzw. den Projektordner öffnen.
2. In IntelliJ: Projekt aus dem Ordner `Good_Code_example` öffnen. Die mitgelieferte `.iml`-Datei wird erkannt, `src/main` ist als Quellordner und `src/test` als Testordner konfiguriert.
3. Alternativ kann das Projekt manuell mit `javac` aus `src/main` heraus kompiliert werden.

### 4.3 Simulation starten

Es stehen **drei Einstiegspunkte** zur Verfügung – je nachdem, wie umfangreich die Auswertung sein soll:

| Einstiegspunkt | Zweck |
|---|---|
| `supm.base.SupermarketClient` | Reine Simulation mit Konsolen-Protokoll, ohne Statistik. Geeignet, um den Ablauf nachzuvollziehen. |
| `supm.statis.SupermarketStatisticClient` | Simulation mit konstanter Ankunftsrate, am Ende werden Zusammenfassung und Auslastungstabelle ausgegeben. |
| `supm.profiled.ProfiledSupermarketStatisticClient` | Simulation mit tageszeitabhängigem Ankunftsprofil über mehrere Tage hinweg, inklusive Statistik-Ausgabe. |

Der gewünschte Client wird wie ein normales Java-Programm gestartet (in der IDE: „Run“; auf der Konsole: über `java <vollqualifizierter-Klassenname>`).

### 4.4 Simulation konfigurieren

Die Stellgrößen werden direkt in der jeweiligen Client-Klasse über das `SimulationParameters`-Objekt gesetzt:

- **Anzahl Kassen**
- **mittlere Ankunftsrate** (Kunden pro Minute)
- **mittlere Bedienzeit** und ihre **Standardabweichung** (Minuten)
- **maximale akzeptierte Wartezeit** (Minuten, danach gibt der Kunde auf)
- **Simulationsdauer** (Minuten)
- **Protokoll-Flag** (steuert, ob jeder Schritt auf der Konsole ausgegeben wird)

Beim Statistik-Client wird zusätzlich die **Intervalllänge** für die Auslastungstabelle festgelegt. Beim Profil-Client wird darüber hinaus ein Tagesprofil als Array von Raten samt Periodendauer (typischerweise 24 h) übergeben; eine Rate von 0 in einem Intervall steht für „geschlossen“ und wird von der Simulation übersprungen.

### 4.5 Ergebnisse interpretieren

- **Protokoll** (nur bei aktivem `protocol`-Flag): chronologische Konsolenausgabe jedes einzelnen Ereignisses (Ankunft, Bedienungsstart, Bedienungsende, Aufgabe).
- **Zusammenfassung**: pro Kasse die Anzahl bedienter Kunden und die maximale Schlangenlänge, gefolgt von Anzahl Abbrecher und mittlerer Wartezeit über alle wartenden Kunden.
- **Auslastungstabelle**: zeilenweise pro Zeitintervall die prozentuale Auslastung jeder Kasse sowie der Durchschnitt – damit lassen sich Engpässe und Leerlaufzeiten direkt erkennen.

### 4.6 Tests ausführen

Im Ordner `src/test` liegen JUnit-Tests, die die Architekturschichten widerspiegeln (Engine, Events, Supermarkt-Varianten, Statistik, Profil). Sie können in der IDE über die übliche „Run Tests“-Funktion oder per Build-Tool ausgeführt werden und dienen als ausführbare Spezifikation des erwarteten Verhaltens der einzelnen Komponenten.
