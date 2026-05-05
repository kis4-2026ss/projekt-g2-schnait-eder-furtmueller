# Proposal
## Goal of the Project
Ziel des Projekts ist es, herauszufinden, ob eine 100% KI-generierte Dokumentation für eine vorhandene Code-Basis brauchbar ist.

Dabei werden 3 Dokumentationen, basierend aud unterschiedlichen Prompts, für jeweils zwei existierende Projekte generiert. Zusätzlich wird dabei getestet, wie die KI mit relativ schlechter Code Qualität und unübersichtlichem Design umgeht, indem einmal ein Projekt mit guter Codequalität verwendet wird und einmal eines mit schlechter.

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

    Schreib eine Dokumentation zu diesem Projekt.


## Teamwork and responsibilities
Teammitglieder: Sebastian Schnait, Max Eder, Laura Furtmüller

Aufteilung: idk
