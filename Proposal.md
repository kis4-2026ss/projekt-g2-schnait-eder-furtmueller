# Proposal
## Goal of the Project
Ziel des Projekts ist es, herauszufinden, ob eine 100% KI-generierte Dokumentation für eine vorhandene Code-Basis brauchbar ist.

Dabei werden 3 Dokumentationen, basierend auf unterschiedlichen Prompts, für jeweils zwei existierende Projekte generiert. Zusätzlich wird dabei getestet, wie die KI mit relativ schlechter Code Qualität und unübersichtlichem Design umgeht, indem einmal ein Projekt mit guter Codequalität verwendet wird und einmal eines mit schlechter.

## Code used
#### Good Code Example
Als Beispiel für gute Code Qualität, wird eine in der Java-Uebung erstellte Hausübung aus dem 3.Semester verwendet.

Grund: Da das Projekt von einem der Teammitglieder stammt, ist es leichter festzustellen ob die generierte Dokumentation akkurat ist.

#### Bad Code Example
Als Beispiel für schlechte Code Qualität, wird das folgende, auf GitHub verfügbare, Projekt verwendet, welches den bad-code tag besitzt.
https://github.com/framaz/The_Game_No_Name 

Grund: Das gefundene Projekt ist einerseits extrem unübersichtlich, mit schlechter Code Qualität, was es der KI erschwären soll eine verständliche Dokumentation zu erstellen und andererseits veraltet (>10 Jahre), wodurch getestet werden kann, wie die KI mit teilweise veraltetem Code umgeht. Zusätzlich besitzt das Projekt mehrere Bugs, was es ermöglicht zu testen, wie die KI mit einem nicht funktionierendem Projekt umgeht und ob sie eventuell, als Teil der Dokumentation, auch die Bugs richtig dokumentiert.

## Project plan

Es soll gegenübergestellt werden, ob bzw. welchen Unterschied es macht, welchen Prompt man für die Generierung der Projekt-Dokumentation verwendet.

Tasks:
- 3 verschiedene Prompts schreiben
- Prompts in Claude über Bad Code Example laufen lassen
- Prompts in Claude über Good Code Example laufen lassen
- Ergebnisse gegenüberstellen und analysieren

## Promts
1.Prompt: (wenige Details)

    Analysiere dieses Projekt und erstelle eine Dokumentation in einer DOCUMENTATION.md Datei. 
    Die Dokumentation soll folgendes enthalten:
      - Projektübersicht und Zweck
      - Architektur und Projektstruktur (Ordner/Dateien und ihre Rolle)
      - Hauptkomponenten und wie sie zusammenspielen
      - Setup- und Nutzungsanleitung
    Geh NICHT auf einzelne Codezeilen, Methoden oder Implementierungsdetails ein. 
    Halte alles auf einem konzeptuellen, textuellen Level – als wäre es für jemanden geschrieben, 
    der das Projekt verstehen will ohne den Code zu lesen.
    
2.Prompt: (viele Details)

    Analysiere dieses Projekt vollständig und erstelle eine detaillierte technische Dokumentation 
    in einer DOCUMENTATION.md Datei. Die Dokumentation soll enthalten:
    - Projektübersicht, Architektur und Projektstruktur
    - Für jedes Modul/jede Datei: Zweck und Verantwortlichkeiten
    - Für jede Klasse: Beschreibung, Eigenschaften und Rolle im System
    - Für jede öffentliche Methode/Funktion: Was sie tut, wie sie es tut, 
      Parameter, Rückgabewerte und Seiteneffekte
    - Datenfluss und Interaktionen zwischen Komponenten
    - Wichtige Designentscheidungen und Patterns die verwendet werden
    
    Sei präzise und technisch. Die Zielgruppe sind Entwickler die am Projekt mitarbeiten.

3.Prompt: (keine genauen Anweisungen)

    Schreib eine Dokumentation zu diesem Projekt in ein File namens documentation.md.


## Team
Teammitglieder: Sebastian Schnait, Max Eder, Laura Furtmüller

