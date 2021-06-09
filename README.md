## Praktikum Compiler

## Entwicklung einrichten

Hinweis: Diese Einrichtung gilt für die Intellij IDE!

[Hier JavaCup runterladen](http://www2.cs.tum.edu/projects/cup/index.php)

Dann java-cup-11b.jar mit einem Archiver öffnen und in das Rootverzeichnis extrahieren

Dannach die java-cup-11b-runtime.jar in das Projektverzeichnis kopieren.

In Intellij dann einfach die .jar Datei als Library einbinden


## Jflex installieren

Manjaro
````
sudo pamac install jflex 
````
Windows:

[siehe Jflex Dokumentation](https://jflex.de/)

## Make ausführen zum kompelieren

````
make
````


## Clean up
make generiert viele .class Dateien.

Für den fall das man den Überblick verliert:

````
make clean
````

## Lexer testen
P2 Main ausführen. P2 erwartet ein Argument: "Eigabedatei.sim".

## Parser testen

in IntelliJ eine Configuration anlegen die P3 ausführt!

P3 erwartet zwei Argumente: "Eingabedatei.sim" und "Parserausgabe.txt"

Dann einfach auf run klicken!
