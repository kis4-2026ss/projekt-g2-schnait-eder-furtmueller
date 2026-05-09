# Technische Dokumentation – Supermarket Simulation

## 1. Projektübersicht

Das Projekt ist eine **diskrete ereignisgesteuerte Simulation (Discrete Event Simulation, DES)** eines Supermarkts mit mehreren Kassen (Checkouts). Es modelliert:

- Kundenankünfte (zufällig oder zeitabhängig nach einem Tagesprofil)
- Warteschlangen-Verhalten an Kassen
- Bedienzeiten und das Verlassen der Warteschlange bei zu langer Wartezeit (*reneging*)
- Auslastungs- und Wartezeitstatistiken

Die Simulation ist in **Java** implementiert (modernes Java mit Pattern Matching, `LocalDateTime`/`Duration`-API). Es gibt drei Einstiegspunkte (Clients), die schrittweise mehr Funktionalität aktivieren:

1. **`SupermarketClient`** – einfache Simulation mit Konsolen-Protokoll, ohne Statistik.
2. **`SupermarketStatisticClient`** – Simulation mit Aggregat-Statistiken (Wartezeit, Auslastung, Abbrüche).
3. **`ProfiledSupermarketStatisticClient`** – Statistik-Simulation mit zeitabhängiger Ankunftsrate (Tagesprofil mit Öffnungs-/Schließzeiten).

### Kernkonzepte

- **Event-driven Simulation:** Eine `PriorityQueue<Event>` enthält alle zukünftigen Ereignisse, sortiert nach Zeitstempel. In jedem Schritt wird das früheste Ereignis entnommen, die Simulationsuhr darauf vorgestellt und das Event verarbeitet. Events können beim Verarbeiten neue Events erzeugen.
- **Drei Event-Typen:** Kundenankunft, Bedienungsende und Warteschlangen-Abbruch.
- **Vererbungsbasierte Erweiterung:** `Supermarket` → `SupermarketStatistic` → `ProfiledSupermarketStatistic`. Jede Erweiterung fügt Funktionalität hinzu, ohne die Basisklasse zu modifizieren (Open/Closed-Prinzip).
- **Reproduzierbare Zufallszahlen:** Exponentialverteilte Zwischenankunftszeiten (Poisson-Prozess) und gauss-verteilte Bedienzeiten.

## 2. Projektstruktur

```
Good_Code_example/
├── Good_Code_example.iml        IntelliJ-Modulfile
└── src/
    ├── main/                    Produktiv-Code
    │   ├── sim/
    │   │   └── Simulation.java               Discrete-Event-Engine
    │   ├── ev/                                Event-Hierarchie
    │   │   ├── Event.java                    Abstrakte Basisklasse
    │   │   ├── CustomerArrivalEvent.java     Ankunft eines Kunden
    │   │   ├── ServiceEndEvent.java          Ende einer Bedienung
    │   │   └── CustomerAbandonEvent.java     Abbruch wegen Wartezeit-Überschreitung
    │   ├── supm/                              Supermarkt-Domäne
    │   │   ├── base/
    │   │   │   ├── Supermarket.java          Basis-Supermarkt (extends Simulation)
    │   │   │   ├── SupermarketClient.java    Einstiegspunkt: Basis-Simulation
    │   │   │   └── SimulationParameters.java Parameter-Container
    │   │   ├── parts/
    │   │   │   ├── Customer.java             Kundenmodell
    │   │   │   └── Checkout.java             Kassenmodell mit Queue
    │   │   ├── statis/
    │   │   │   ├── SupermarketStatistic.java        Supermarket + Statistikerfassung
    │   │   │   └── SupermarketStatisticClient.java  Einstiegspunkt: Statistik-Simulation
    │   │   └── profiled/
    │   │       ├── ProfiledSupermarketStatistic.java       Mit Ankunftsprofil
    │   │       └── ProfiledSupermarketStatisticClient.java Einstiegspunkt: Profilierte Sim.
    │   ├── profile/                           Ankunfts-Profile
    │   │   ├── ArrivalProfile.java           Interface für zeitabh. Ankunftsraten
    │   │   └── PeriodicArrivalProfile.java   Periodische Implementierung (z. B. Tag)
    │   ├── stats/
    │   │   └── SupermarketStats.java         Aggregat-Datenhaltung & Reports
    │   └── util/
    │       └── AdjustableClock.java          Manuell vorgestellbare java.time.Clock
    └── test/                                 JUnit-Tests (Spiegelstruktur zu main/)
        ├── ev/, profile/, sim/, stats/, supm/...
```

### Architekturüberblick

```
                   ┌────────────────────────┐
                   │       Simulation       │   PriorityQueue<Event>
                   │  (sim.Simulation)      │   step(), run(until)
                   └───────────┬────────────┘
                               │ extends
                   ┌───────────▼────────────┐
                   │       Supermarket      │   Checkouts, Random,
                   │   (supm.base)          │   nextInterarrivalTime(),
                   └───────────┬────────────┘   nextServiceTime()
                               │ extends
                   ┌───────────▼────────────┐
                   │  SupermarketStatistic  │   delegiert an SupermarketStats
                   │   (supm.statis)        │
                   └───────────┬────────────┘
                               │ extends
                   ┌───────────▼─────────────────┐
                   │ ProfiledSupermarketStatistic│   nutzt ArrivalProfile
                   │   (supm.profiled)           │
                   └─────────────────────────────┘

  Events lesen/ändern Supermarket-Zustand:
    CustomerArrivalEvent ──> findShortestQueue, Customer einreihen oder bedienen,
                            erzeugt ServiceEndEvent + CustomerAbandonEvent + nächstes ArrivalEvent
    ServiceEndEvent      ──> nächsten Kunden aus Queue ziehen, ggf. neues ServiceEndEvent
    CustomerAbandonEvent ──> Kunden aus Queue entfernen, Statistik aktualisieren
```

## 3. Verwendete Patterns & Designentscheidungen

| Pattern / Entscheidung | Wo? | Warum? |
|---|---|---|
| **Discrete Event Simulation** mit `PriorityQueue` | `sim.Simulation` | Klassischer DES-Ansatz: Zeit springt von Ereignis zu Ereignis statt in fixen Ticks → effizient bei seltenen Ereignissen. |
| **Template Method / Vererbungsketten** | `Supermarket` → `SupermarketStatistic` → `ProfiledSupermarketStatistic` | Schrittweise Erweiterung. `nextInterarrivalTime()` wird in `Profiled…` überschrieben. Statistik-Methoden bleiben in `Supermarket` unsichtbar; `Event#process` prüft per `instanceof` Pattern Matching, ob Statistik-Variante vorliegt. |
| **Strategy Pattern** | `ArrivalProfile` Interface, `PeriodicArrivalProfile` Implementation | Ankunftsverhalten austauschbar (z. B. konstant vs. tagesperiodisch). |
| **Pattern Matching für `instanceof`** | `CustomerArrivalEvent`, `ServiceEndEvent`, `CustomerAbandonEvent`, `Simulation.step` | Ermöglicht, dass dieselbe Event-Klasse mit allen Supermarkt-Varianten arbeitet, ohne separate Hierarchie. |
| **Composition over Inheritance** (für Statistik) | `SupermarketStatistic` *hat ein* `SupermarketStats` | Statistik-Logik ist getrennt vom Supermarkt-Kern und in `stats.SupermarketStats` gekapselt. |
| **Parameter Object** | `SimulationParameters` | Bündelt alle Konfigurationswerte und macht Konstruktor-Signaturen lesbar. |
| **Test-Clock Injection** | `AdjustableClock`, `Simulation(Clock)` | Erlaubt deterministische Tests; Simulation kann eine vom Wall-Clock entkoppelte, manuell vorstellbare Uhr nutzen. |
| **Static-Factory für IDs** | `Customer.nextId` | Inkrementierender statischer Counter generiert eindeutige Customer-IDs. |
| **Exponential-/Gauss-Verteilungen** | `Supermarket.nextInterarrivalTime`, `nextServiceTime` | Inverse-Transform Sampling für Poisson-Prozess; Normalverteilung für Bedienzeiten. |

## 4. Detaillierte Modul- & Klassenbeschreibung

### 4.1 Paket `sim`

#### `sim.Simulation`
Kern der Discrete-Event-Engine. Verwaltet eine prioritätenbasierte Event-Queue und einen Zeitcursor.

**Eigenschaften**
- `PriorityQueue<Event> queue` – Events sortiert nach `Event.time` (`compareTo`).
- `Clock clock` – Java-`Clock`-Instanz; falls `AdjustableClock`, wird sie synchron zum Simulationszeitsprung mitgeführt.
- `LocalDateTime currentTime` – aktueller Stand der Simulationszeit.

**Konstruktoren**
- `Simulation()` – nutzt `Clock.systemDefaultZone()`.
- `Simulation(Clock clock)` – Dependency Injection einer Clock (für Tests/Determinismus).

**Öffentliche Methoden**
| Methode | Zweck | Seiteneffekt |
|---|---|---|
| `LocalDateTime now()` | Liefert den aktuellen Simulationszeitpunkt. | Keiner. |
| `void addEvent(Event e)` | Stellt ein Event in die Queue ein. | Mutiert `queue`. |
| `void step()` | Holt das nächste Event aus der Queue, springt mit der Zeit darauf, schiebt ggf. die `AdjustableClock` mit, ruft `e.process(this)` auf. | Mutiert `currentTime`, `queue`, ggf. `clock`. |
| `void run(LocalDateTime until)` | Schleife: solange das nächste Event nicht hinter `until` liegt, `step()` ausführen. | Verarbeitet alle in `[currentTime, until]` liegenden Events; neue Events während der Verarbeitung können erzeugt werden. |

### 4.2 Paket `ev`

#### `ev.Event` (abstract)
Basisklasse aller Ereignisse; implementiert `Comparable<Event>` über den Zeitstempel — dadurch ordnet die `PriorityQueue` Events automatisch nach Zeit.

**Eigenschaften**
- `protected final LocalDateTime time` – Zeitpunkt der Ereignis-Verarbeitung.

**Methoden**
- `LocalDateTime getTime()` – Getter.
- `abstract void process(Simulation sim)` – wird von der Engine aufgerufen, wenn das Event an der Reihe ist.
- `int compareTo(Event other)` – Vergleich anhand `time`.

#### `ev.CustomerArrivalEvent extends Event`
Modelliert die Ankunft eines neuen Kunden. Erzeugt im Konstruktor automatisch ein neues `Customer`-Objekt.

**Eigenschaften:** `Supermarket market`, `Customer customer`.

**`process(Simulation sim)` – Was passiert:**
1. Logt die Ankunft.
2. Sucht via `market.findShortestQueue()` die kürzeste/leerste Kasse.
3. **Falls Kasse frei:** Markiert sie als belegt, setzt `startBusy`, setzt `customer.startServiceT` und plant ein `ServiceEndEvent` für `time + nextServiceTime()*60s`.
4. **Sonst:** Hängt den Kunden in die Queue, informiert die Statistik (falls `market` ein `SupermarketStatistic` ist) per Pattern Matching, plant ein `CustomerAbandonEvent` zum Zeitpunkt `time + maxWaitTime`.
5. Plant das nächste `CustomerArrivalEvent` mittels `nextInterarrivalTime()` — ruft die profilierte Variante via `instanceof ProfiledSupermarketStatistic`, sodass Tagesprofile berücksichtigt werden.
6. Stoppt die Generierung neuer Ankünfte, sobald `simulationDurationMinutes` überschritten würde.

**Seiteneffekte:** Mutation an `Checkout` (busy, queue), an Statistik, neue Events in Queue.

#### `ev.ServiceEndEvent extends Event`
Tritt ein, wenn ein Kunde an der Kasse fertig bedient ist.

**Eigenschaften:** `Supermarket market`, `Checkout checkout`, `Customer customer`.

**`process(Simulation sim)`:**
1. Logt das Ende der Bedienung.
2. Falls noch jemand in der Queue: nächsten Kunden ziehen, Service-Start setzen, neues `ServiceEndEvent` einplanen.
3. Sonst: Kasse als nicht-belegt markieren.
4. Falls Statistik-Variante: zählt bedienten Kunden, addiert Wartezeit (`startService - arrivalTime`), und sobald die Kasse leerläuft, wird die Belegtzeit (`startBusy → time`) erfasst.

**Seiteneffekte:** Mutiert `Checkout`-Queue/Busy-Status, evtl. Stats, fügt Folge-Event ein.

#### `ev.CustomerAbandonEvent extends Event`
Wird beim Einreihen geplant — falls der Kunde dann noch in der Queue steht, verlässt er sie unbedient.

**Eigenschaften:** `Supermarket market`, `Checkout cashier`, `Customer customer`.

**`process(Simulation sim)`:**
1. Versucht, den Kunden aus der Queue zu entfernen (`queue.remove(customer)` liefert `false`, falls er bereits bedient wurde — dann passiert nichts).
2. Bei Erfolg: ggf. Stats inkrementieren (`customerAbandoned`), Log-Eintrag.

**Seiteneffekte:** Mutiert Queue + Statistik. Idempotent gegenüber bereits bedienten Kunden.

### 4.3 Paket `profile`

#### `profile.ArrivalProfile` (Interface)
Ein einziger Vertrag: `double getArrivalRate(LocalDateTime time)` — die momentane Ankunftsrate (Kunden/Minute) zum gegebenen Zeitpunkt. **Strategy Pattern**: erlaubt austauschbare Tages-, Wochen- oder Konstantprofile.

#### `profile.PeriodicArrivalProfile implements ArrivalProfile`
Periodisches Profil (z. B. ein Tag), in fixe Intervalle gleicher Länge unterteilt; jedes Intervall hat eine eigene Rate.

**Eigenschaften**
- `double[] rates` – Raten pro Intervall.
- `Duration period` – Dauer einer Periode (z. B. 24 h).
- `Duration interval` – `period / rates.length` (intern berechnet).
- `LocalDateTime start` – Referenzzeitpunkt.

**Konstruktor:** validiert, dass `rates[0] > 0` (sonst `IllegalStateException`).

**Methoden**
- `getArrivalRate(LocalDateTime time)`: berechnet `(minutesSinceStart % periodMinutes) / intervalMinutes`, indiziert in `rates` und liefert die zuständige Rate.
- `nextOpenTime(LocalDateTime current)`: springt vorwärts in Intervallschritten, bis ein Intervall mit `rate > 0` gefunden wird; nützlich für „Geschäft geschlossen“-Lücken im Tagesprofil.

### 4.4 Paket `supm.base`

#### `supm.base.SimulationParameters`
Reines Daten-Container-Objekt für alle Simulationskonfigurationen. Felder sind `public` (bewusst keine Kapselung — entspricht einem klassischen Parameter Object).

| Feld | Bedeutung |
|---|---|
| `int numCheckouts` | Anzahl Kassen |
| `double meanArrivalRate` | λ (Kunden/min) für Poisson-Prozess |
| `double meanServiceTime` | µ (min), Mittelwert Bediendauer |
| `double serviceTimeStdDev` | σ (min), Standardabweichung Bediendauer |
| `double maxWaitTime` | Wartezeit-Tolranz (min); danach abandont der Kunde |
| `int simulationDurationMinutes` | Gesamtlaufzeit der Simulation |
| `boolean protocol` | aktiviert Konsolen-Logging |

#### `supm.base.Supermarket extends Simulation`
Domänenmodell des Marktes. Ist gleichzeitig die Engine — durch Vererbung können Clients direkt `addEvent`/`run` an einem `Supermarket` aufrufen.

**Eigenschaften**
- `SimulationParameters params`
- `List<Checkout> checkouts` – generiert im Konstruktor mit IDs `1..numCheckouts`.
- `Random random` – für `nextInterarrivalTime`/`nextServiceTime`.

**Konstruktor:** legt Checkouts an, prüft `maxWaitTime >= 0`.

**Öffentliche Methoden**
| Methode | Beschreibung |
|---|---|
| `getParams()` | liefert `SimulationParameters`. |
| `findShortestQueue()` | Liefert zuerst eine freie Kasse, sonst die mit kürzester Schlange. Wirft `IllegalStateException`, wenn keine Kassen vorhanden sind. |
| `log(String msg)` | gibt `msg` auf `System.out` aus, wenn `params.protocol == true`. |
| `nextInterarrivalTime()` | Inverse-Transform-Sampling: `-ln(1-u)/λ` ⇒ exponentialverteiltes Δ-t (Poisson-Prozess). Validiert `meanArrivalRate > 0`. |
| `nextServiceTime()` | `max(0.1, µ + σ·N(0,1))` — verhindert negative oder triviale Bedienzeiten. |
| `getRandom()`, `getCheckout()` | Getter (Letzterer ist der API-Name für die Kassenliste – Plural-/Singularform unverändert übernommen). |

#### `supm.base.SupermarketClient`
Einstiegspunkt der Basis-Simulation (Java 21 instance-`main`-Variante: `static void main()`). Konfiguriert Parameter, erzeugt einen `Supermarket`, plant das initiale `CustomerArrivalEvent` zum Startzeitpunkt und ruft `run(...)` auf. Es wird **keine** Statistik ausgegeben, nur Protokoll-Logs.

### 4.5 Paket `supm.parts`

#### `supm.parts.Customer`
Repräsentiert einen einzelnen Kunden.

**Eigenschaften**
- `static int nextId` – globaler Zähler für eindeutige IDs.
- `int id`, `LocalDateTime arrivalTime` (immutable).
- `LocalDateTime startServiceT` (mutable, initial `null`).

**Methoden:** `setStartServiceT`, `getStartServiceT` (mit `assert`), `getArrivalTime`, `getId`, `toString` → `"Customer <id>"`.

> **Hinweis:** Der globale Zähler ist nicht thread-safe. Da die Simulation single-threaded läuft, ist das hier akzeptabel.

#### `supm.parts.Checkout`
Repräsentiert eine Kasse mit FIFO-Warteschlange.

**Eigenschaften**
- `int id`
- `Queue<Customer> queue` (`LinkedList`).
- `boolean busy`
- `LocalDateTime startBusy` – Zeitpunkt, ab dem die Kasse durchgängig belegt ist (für Auslastungsmessung).

**Methoden:** Getter/Setter für `busy` und `startBusy`, `getQueue()` (direkter Zugriff), `queueLength()`, `getId()`, `toString()` → `"Checkout <id>"`.

### 4.6 Paket `stats`

#### `stats.SupermarketStats`
Reine Datenhaltung & Auswertung – kennt keinen `Supermarket`-Zustand außer der übergebenen Checkout-Liste.

**Eigenschaften**
- `List<Checkout> checkouts`
- `double intervalLength` (min) – Auswertungs-Bucket-Breite.
- `Map<Checkout, Integer> servedCustomers`, `maxQueueLength`.
- `int abandonedCustomers`, `double totalWaitTime`, `int numWaitedCustomers`.
- `Map<Checkout, List<Double>> busyTimePerInterval` – pro Kasse ein Vektor mit Belegtzeit (in min) je Intervall.
- `int numIntervals` = `ceil(simulationDuration / intervalLength)`.
- `double maxWaitTime`, `LocalDateTime simStart`.

**Konstruktor:** Initialisiert die Maps, füllt `busyTimePerInterval` mit Nullen.

**Update-Methoden (während der Simulation aufgerufen):**
| Methode | Wirkung |
|---|---|
| `customerServed(Checkout c)` | Inkrementiert `servedCustomers[c]`. |
| `addWaitTime(double waitTime)` | Summiert `totalWaitTime`, erhöht `numWaitedCustomers`. |
| `customerQueued(Checkout c)` | Aktualisiert `maxQueueLength[c] := max(prev, c.queueLength())`. |
| `customerAbandoned()` | Erhöht `abandonedCustomers`, `numWaitedCustomers`, addiert `maxWaitTime` zur `totalWaitTime` (Worst-Case-Approximation für Verlassene). |
| `recordBusyTime(c, start, end)` | Verteilt das `[start,end]`-Intervall auf die zugehörigen Auswertungs-Buckets via Overlap-Berechnung. |
| `finalizeBusyTimes(LocalDateTime simEnd)` | Bei Simulationsende: noch belegte Kassen mit `recordBusyTime(c.startBusy → simEnd)` abschließen. |

**Auswertungs-Methoden:**
- `getAverageWaitTime()` → `totalWaitTime / numWaitedCustomers` (oder 0).
- `getUtilizationTable()` → `List<List<Double>>`: Pro Intervall eine Zeile mit Auslastung pro Kasse plus Durchschnittsspalte am Ende; Werte sind `min(1.0, busy/intervalLength)`.
- `printSummary()` – pro Kasse Anzahl Bedienungen + max. Queue-Länge, total Abandons, mittlere Wartezeit.
- `printUtilizationTable()` – formatierte Tabelle der Auslastung über die Zeitintervalle hinweg.

### 4.7 Paket `supm.statis`

#### `supm.statis.SupermarketStatistic extends Supermarket`
Erweitert `Supermarket` um Statistikerfassung. Hält ein `SupermarketStats`-Objekt und delegiert die Update-Aufrufe an dieses.

**Konstruktor:** `SupermarketStatistic(SimulationParameters params, int intervalLength)` – baut den Stats-Container mit Simulationsdauer/Intervallen.

**Methoden** (im Wesentlichen Delegationen):
- `getStats()` – Zugriff für den Client zum Drucken der Reports.
- `customerQueued(Checkout)`, `customerServed(Checkout)`, `customerAbandoned()`.
- `addWaitTime(arrivalTime, serviceStartTime)` – berechnet `Duration.between(...).toMinutes()` und reicht weiter.
- `recordBusyTime(c, start, end)`.

Die Events erkennen diese Klasse über `instanceof SupermarketStatistic statMarket` und rufen die Statistik-Hooks nur dann auf — so bleiben Events kompatibel mit dem reinen `Supermarket`.

#### `supm.statis.SupermarketStatisticClient`
Einstiegspunkt mit Statistik. Verwendet 3 Kassen, 2 h Simulationsdauer, 30-min-Auswertungsintervall. Druckt nach dem Lauf `printSummary()` und `printUtilizationTable()`.

### 4.8 Paket `supm.profiled`

#### `supm.profiled.ProfiledSupermarketStatistic extends SupermarketStatistic`
Überschreibt die Ankunftszeit-Generierung, damit zeitabhängige Raten respektiert werden.

**Eigenschaften:** `ArrivalProfile profile`, eigene `Random random`.

**`nextInterarrivalTime()` (override):**
1. Ermittelt `nextOpen` über `((PeriodicArrivalProfile) profile).nextOpenTime(now)` — überspringt geschlossene Phasen (Rate=0).
2. Liest aktuelle Rate bei `nextOpen`.
3. Generiert ein `interarrival = -ln(1-u)/rate`.
4. Liefert `waitMinutes (bis Öffnung) + interarrival` zurück.

> **Hinweis:** `CustomerArrivalEvent` ruft diese Methode explizit über das Pattern `instanceof ProfiledSupermarketStatistic profMarket` auf, sodass der Sonderfall nicht hinter Standardpolymorphie versteckt ist. Polymorphie würde hier prinzipiell ausreichen — das explizite Pattern macht die Differenz aber lesbar im aufrufenden Event.

#### `supm.profiled.ProfiledSupermarketStatisticClient`
Definiert ein Tagesprofil (`dayProfile`) mit 6 Buckets à 4 Stunden — Geschäft hat morgens/mittags Verkehr und ist nachmittags/nachts geschlossen (Rate = 0). Simuliert 5 Tage und gibt Summary + Utilization-Tabelle aus.

### 4.9 Paket `util`

#### `util.AdjustableClock extends java.time.Clock`
Eine Clock, deren „aktuelle Zeit“ manuell vorgestellt werden kann.

**Eigenschaften:** `Instant currentTime`.

**Methoden**
- `getZone()` → `ZoneId.systemDefault()`.
- `withZone(ZoneId)` – returns `this`, falls Zone gleich; sonst eine System-Clock in der Zone.
- `instant()` → `currentTime`.
- `incrementTime(Duration)` – schiebt `currentTime` um die übergebene Duration vor.

`Simulation.step()` erkennt diese Klasse via Pattern Matching (`clock instanceof AdjustableClock adj`) und ruft `incrementTime` synchron zum Eventzeit-Sprung auf — so können Tests Clock-abhängigen Code deterministisch ausführen.

## 5. Datenfluss & Komponenteninteraktion

### 5.1 Lebenszyklus eines Kunden

```
 t0   CustomerArrivalEvent.process
        ├─ market.findShortestQueue() → Checkout c
        ├─ c frei?
        │    ja: c.busy=true, c.startBusy=t0, customer.startServiceT=t0
        │        ⇒ ServiceEndEvent(t0+service, c, customer)
        │    nein: c.queue.add(customer)
        │           stats.customerQueued(c)            (falls Statistik)
        │           ⇒ CustomerAbandonEvent(t0+maxWait, c, customer)
        └─ ⇒ CustomerArrivalEvent(t0+interarrival)     (falls noch innerhalb der Sim-Dauer)

 t1   CustomerAbandonEvent.process     (nur wenn Kunde noch in Queue)
        ├─ c.queue.remove(customer)
        └─ stats.customerAbandoned()

 t2   ServiceEndEvent.process
        ├─ c.queue leer?
        │    ja:   c.busy = false
        │          stats.recordBusyTime(c, c.startBusy, t2)
        │    nein: next = c.queue.poll(); next.startServiceT = t2
        │          ⇒ ServiceEndEvent(t2+service, c, next)
        └─ stats.customerServed(c); stats.addWaitTime(arrival, startService)
```

### 5.2 Interaktion zwischen Klassen

- **`Supermarket` ↔ `Simulation`:** Erbt die Event-Engine; alle Marktstati werden im selben Objekt gehalten, Events erhalten den `Simulation` und casten implizit (durch übergebene `Supermarket`-Referenz im Eventkonstruktor) zur Marktinstanz.
- **`Event` ↔ `Supermarket`:** Events kennen ihren Markt direkt. Statistikinformationen erhalten sie über `instanceof`-Patterns — der Markt-Typ entscheidet dynamisch.
- **`SupermarketStatistic` ↔ `SupermarketStats`:** Reine Delegation. Stats kennt seinerseits nur die übergebene Checkout-Liste und Zeitparameter, keine Events.
- **`ProfiledSupermarketStatistic` ↔ `ArrivalProfile`:** Profile-Strategie steuert die Verteilung der Ankunftsabstände. Der Markt selbst ist dafür blind.
- **`Simulation` ↔ `AdjustableClock`:** Optional injizierte Test-Clock; wird von `step()` synchron mit dem Event-Zeitsprung mitgeführt, sodass `LocalDateTime.now(clock)`-Aufrufe konsistent bleiben.

## 6. Tests

Im Paket `src/test/` liegen JUnit-Tests, deren Paketstruktur die `main`-Struktur spiegelt:

- `ev/`: Tests für jedes Event (Verhalten bei freier/besetzter Kasse, Abbruchmechanik, Folge-Events).
- `profile/`: Validierung der Indexberechnung und `nextOpenTime`-Logik im `PeriodicArrivalProfile`.
- `sim/`: `SimulationTest` – Engine-Verhalten (Reihenfolge, Zeitfortschritt, leere Queue).
- `stats/`: Aggregation, Wartezeitmittel, Utilization-Berechnung, Buckets.
- `supm/`: Marktverhalten (Queue-Auswahl, Parameter-Validierung), Customer/Checkout-Identitätstests, Statistik-/Profilvarianten.

## 7. Hinweise für Mitarbeit

- **Single-Threaded by Design.** Random, Customer-IDs und alle Event-Pfade sind nicht thread-safe — Parallelisierung würde umfangreichen Umbau verlangen.
- **Zeitarithmetik:** Die Simulation rechnet konsequent in `LocalDateTime`/`Duration`. Bedienzeiten werden als „Sekunden“ addiert (`time.plusSeconds(((long) serviceTime) * 60)`), wodurch der Bruchanteil von `serviceTime` (in Minuten) verloren geht. Vor Änderungen daran prüfen, dass Tests/Reports konsistent bleiben.
- **Erweiterung um neue Markttypen:** Neue Subklassen von `Supermarket(Statistic)` benötigen ggf. zusätzliche `instanceof`-Pfade in den Events. Alternativ kann man die Hooks (`onCustomerQueued`, `onCustomerServed`, …) in die Basisklasse hochziehen und Default-No-Ops bieten — das würde die Pattern-Matches eliminieren.
- **Neues Ankunftsprofil:** Implementiere `ArrivalProfile`. Beachte: `ProfiledSupermarketStatistic` castet derzeit hart auf `PeriodicArrivalProfile` zwecks `nextOpenTime`. Für eine generische Lösung muss `nextOpenTime` ins Interface gehoben werden.
- **Logging:** Alle Logs gehen über `Supermarket.log` und sind via `params.protocol` global an/aus schaltbar. Bei großen Läufen unbedingt deaktivieren.
