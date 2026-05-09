# Dokumentation – Supermarkt-Simulation (Good Code Example)

## 1. Überblick

Dieses Projekt ist eine **ereignisgesteuerte Simulation (Discrete Event Simulation)** eines
Supermarkts mit mehreren Kassen. Modelliert werden ankommende Kundinnen und Kunden, die sich
an der jeweils kürzesten Kassenwarteschlange anstellen, dort bedient werden oder die
Warteschlange nach einer maximalen Wartezeit wieder verlassen.

Die Simulation läuft in drei Ausbaustufen:

1. **Basis-Simulation** – nur Ablauf, keine Statistik (`SupermarketClient`)
2. **Mit Statistiken** – Auslastung, Wartezeiten, abgebrochene Kunden (`SupermarketStatisticClient`)
3. **Mit Ankunftsprofil** – zeitabhängige Ankunftsraten (z. B. Tagesverlauf) (`ProfiledSupermarketStatisticClient`)

---

## 2. Projektstruktur

```
src/
├── main/
│   ├── sim/        Simulationskern (Event-Loop, Priority Queue)
│   ├── ev/         Event-Klassen (Ankunft, Service-Ende, Abbruch)
│   ├── supm/
│   │   ├── base/       Supermarkt-Basis, Parameter, Client
│   │   ├── parts/      Customer, Checkout
│   │   ├── statis/     Supermarkt mit Statistik-Erweiterung
│   │   └── profiled/   Supermarkt mit Ankunftsprofil
│   ├── stats/      Sammlung und Auswertung der Statistikdaten
│   ├── profile/    Ankunftsprofile (Interface + periodische Implementierung)
│   └── util/       AdjustableClock (steuerbare Uhr für die Simulation)
└── test/           JUnit-Tests, gespiegelte Paketstruktur
```

---

## 3. Architektur

### 3.1 Simulationskern (`sim.Simulation`)

Der Kern verwaltet eine **PriorityQueue** von `Event`-Objekten, sortiert nach ihrer
Ausführungszeit (`LocalDateTime`). Die wichtigsten Methoden:

- `addEvent(Event e)` – fügt ein neues Ereignis ein
- `step()` – verarbeitet das nächste Ereignis, springt in der Zeit nach vorne
- `run(LocalDateTime until)` – arbeitet alle Events bis zum gewünschten Zeitpunkt ab
- `now()` – aktuelle Simulationszeit

Die Simulation arbeitet **nicht in Echtzeit** – sie springt von Event zu Event. Wird
optional ein `AdjustableClock` übergeben, wird dessen interne Uhr ebenfalls mitgeführt.

### 3.2 Events (`ev`)

Alle Events erben von `Event` und implementieren `Comparable<Event>` (Sortierung nach Zeit).
Jedes Event implementiert `process(Simulation sim)`, in dem die Folgewirkung passiert:

| Event                  | Was passiert beim `process()`                                                                                       |
|------------------------|---------------------------------------------------------------------------------------------------------------------|
| `CustomerArrivalEvent` | Kunde sucht kürzeste Kasse, beginnt Service oder reiht sich ein. Erzeugt nächstes Ankunfts- und ggf. Abbruch-Event. |
| `ServiceEndEvent`      | Service endet, ggf. wird nächster Kunde aus der Warteschlange bedient.                                              |
| `CustomerAbandonEvent` | Kunde verlässt die Warteschlange, falls er noch nicht bedient wurde (Geduld überschritten).                         |

Die Events erkennen über `instanceof` automatisch, ob es sich um die einfache oder die
statistik-erweiterte Variante des Supermarkts handelt, und melden ggf. zusätzliche Kennzahlen.

### 3.3 Supermarkt-Hierarchie (`supm`)

```
Simulation (sim)
    └── Supermarket (supm.base)
            └── SupermarketStatistic (supm.statis)
                    └── ProfiledSupermarketStatistic (supm.profiled)
```

- **`Supermarket`** – verwaltet eine Liste von `Checkout`s, liefert die kürzeste Schlange,
  zieht zufällige Bedien- und Zwischenankunftszeiten (Exponential- bzw. Gauß-Verteilung).
- **`SupermarketStatistic`** – delegiert Statistik-Calls an `SupermarketStats`.
- **`ProfiledSupermarketStatistic`** – überschreibt `nextInterarrivalTime()`, sodass die
  aktuelle Rate aus einem `ArrivalProfile` (z. B. Tagesverlauf) gelesen wird.

### 3.4 Bestandteile (`supm.parts`)

- **`Customer`** – ID (auto-increment), Ankunftszeit, Service-Startzeit
- **`Checkout`** – ID, FIFO-Warteschlange (`LinkedList<Customer>`), Busy-Status, Beginn der
  letzten Belegung (für Auslastungsberechnung)

### 3.5 Parameter (`supm.base.SimulationParameters`)

Container für alle Eingabewerte einer Simulation:

| Feld                        | Bedeutung                                            |
|-----------------------------|------------------------------------------------------|
| `numCheckouts`              | Anzahl der Kassen                                    |
| `meanArrivalRate`           | Mittlere Ankünfte pro Minute (λ, Exponentialverteilung) |
| `meanServiceTime`           | Mittlere Bedienzeit in Minuten                       |
| `serviceTimeStdDev`         | Standardabweichung der Bedienzeit                    |
| `maxWaitTime`               | Geduld der Kunden in Minuten                         |
| `simulationDurationMinutes` | Simulierte Dauer in Minuten                          |
| `protocol`                  | `true` → Konsolen-Logging der Ereignisse             |

### 3.6 Statistik (`stats.SupermarketStats`)

Wird vom `SupermarketStatistic` benutzt und sammelt:

- bediente Kundinnen pro Kasse
- maximale Schlangenlänge pro Kasse
- abgebrochene Kunden gesamt
- aufsummierte und mittlere Wartezeit
- Belegungs-/Auslastungstabelle pro Zeitintervall (in `intervalLength` Minuten-Schritten)

Ausgabemethoden: `printSummary()` und `printUtilizationTable()`.
Vor der Auswertung sollte `finalizeBusyTimes(simEnd)` aufgerufen werden, damit zum
Simulationsende noch laufende Bedienungen korrekt mitgezählt werden.

### 3.7 Ankunftsprofile (`profile`)

- **`ArrivalProfile`** – Interface mit `getArrivalRate(LocalDateTime time)`
- **`PeriodicArrivalProfile`** – periodisches Profil (z. B. 24-h-Tagesverlauf), das ein
  Array von Raten pro Intervall verwaltet. `nextOpenTime(current)` springt über Intervalle
  mit Rate 0 (geschlossener Supermarkt) hinweg zur nächsten "offenen" Periode.

### 3.8 Hilfsklassen (`util`)

- **`AdjustableClock`** – `java.time.Clock`, deren Uhrzeit per `incrementTime(Duration)`
  manuell vorgespult werden kann. Wird benötigt, weil die Simulation in der Zeit "springt".

---

## 4. Ablauf einer Simulation

1. `SimulationParameters` werden gesetzt.
2. Eine Supermarkt-Instanz (Basis / Statistik / Profiled) wird erzeugt.
3. Erstes `CustomerArrivalEvent` wird in die Queue gelegt.
4. `market.run(endZeit)` arbeitet alle Ereignisse in chronologischer Reihenfolge ab.
5. Während des Laufs wird in jedem `CustomerArrivalEvent` automatisch die nächste Ankunft
   geplant – so lange, bis die Simulationsdauer überschritten wird.
6. Optional: Statistiken finalisieren und ausgeben.

### Beispiel: Einfache Simulation (`SupermarketClient`)

```java
SimulationParameters params = new SimulationParameters(
        2,      // Kassen
        1.5,    // Ankünfte pro Minute
        3.0,    // Mittlere Bedienzeit (min)
        1.0,    // Std-Abweichung Bedienzeit
        3.0,    // Max. Wartezeit (min)
        15,     // Simulationsdauer (min)
        true    // Protokoll ausgeben
);

Supermarket market = new Supermarket(params);
market.addEvent(new CustomerArrivalEvent(market.now(), market));
market.run(LocalDateTime.now().plusMinutes(params.simulationDurationMinutes));
```

### Beispiel: Mit Tagesprofil (`ProfiledSupermarketStatisticClient`)

```java
double[] dayProfile = {                      // Raten pro 4-Stunden-Block
        100.0/4.0/60.0,                      // 00–04 Uhr
        220.0/4.0/60.0,                      // 04–08 Uhr
        150.0/4.0/60.0,                      // 08–12 Uhr
        0.0, 0.0, 0.0                        // 12–24 Uhr geschlossen
};

ArrivalProfile profile = new PeriodicArrivalProfile(
        simStart,
        Duration.ofMinutes(60 * 24),         // Periode = 1 Tag
        dayProfile
);

ProfiledSupermarketStatistic market =
        new ProfiledSupermarketStatistic(params, intervalLength, profile);
```

---

## 5. Einstiegspunkte

| Client                                  | Zweck                                                                 |
|-----------------------------------------|-----------------------------------------------------------------------|
| `supm.base.SupermarketClient`           | Minimales Beispiel ohne Statistik – Output über `protocol = true`     |
| `supm.statis.SupermarketStatisticClient`| Simulation mit Auslastungs- und Wartezeit-Auswertung                  |
| `supm.profiled.ProfiledSupermarketStatisticClient` | Mehrtägige Simulation mit zeitabhängigen Ankunftsraten     |

Hinweis: Die `main`-Methoden sind in der Sprachvariante ohne `String[] args` deklariert
(*instance main methods*, JEP 463 / Java 21+ Preview bzw. Java 25+).

---

## 6. Tests

Die Tests liegen unter `src/test/` und spiegeln die Paketstruktur des Hauptcodes:

- **`sim/SimulationTest`** – Event-Loop, Reihenfolge, Zeitfortschritt
- **`ev/...Test`** – Verhalten der drei Event-Typen
- **`supm/base/SupermarketTest`** – Auswahl der kürzesten Schlange, Zufallsverteilungen
- **`supm/parts/CustomerTest`, `CheckoutTest`** – Datenklassen
- **`supm/statis/SupermarketStatisticTest`** – Statistik-Anbindung
- **`supm/profiled/ProfiledSupermarketStatisticTest`** – Profil-basierte Ankünfte
- **`profile/PeriodicArrivalProfileTest`** – Ratenberechnung, Sprung über Schließzeiten
- **`stats/SupermarketStatsTest`** – Aggregation, Auslastungstabelle

---

## 7. Erweiterungspunkte

- **Neuer Event-Typ:** Klasse von `Event` ableiten, `process(Simulation sim)` implementieren,
  per `sim.addEvent(...)` einreihen.
- **Neues Ankunftsprofil:** `ArrivalProfile` implementieren (z. B. `WeeklyArrivalProfile`)
  und an `ProfiledSupermarketStatistic` übergeben. Achtung: Aktuell wird in
  `ProfiledSupermarketStatistic.nextInterarrivalTime()` nach `PeriodicArrivalProfile`
  gecastet – bei einer neuen Implementierung müsste der Cast verallgemeinert werden.
- **Eigene Strategie für Schlangenwahl:** `Supermarket.findShortestQueue()` überschreiben
  (z. B. nach erwartet kürzester Wartezeit statt nach Schlangenlänge).
- **Andere Verteilungen:** `nextInterarrivalTime()` und `nextServiceTime()` in `Supermarket`
  überschreiben (aktuell Exponential- und Gauß-Verteilung).
