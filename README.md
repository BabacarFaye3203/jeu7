# 🎮 Jeu de Sept - Simulation Multi-Agents JADE

Une simulation multi-agents du jeu classique "Jeu de Sept" implémentée avec la plateforme JADE et une interface JavaFX.

## 📋 Table des Matières

- [🎯 Présentation](#-présentation)
- [🎲 Règles du Jeu](#-règles-du-jeu)
- [🏗️ Architecture](#-architecture)
- [🚀 Installation](#-installation)
- [🖥️ Interface](#-interface)
- [🤖 Agents](#-agents)
- [📁 Structure du Projet](#-structure-du-projet)
- [🔧 Compilation et Exécution](#-compilation-et-exécution)
- [📊 Exemple de Déroulement](#-exemple-de-déroulement)
- [🛠️ Développement](#-développement)
- [📄 Licence](#-licence)

  

## 🎯 Présentation

Le **Jeu de Sept** est une simulation multi-agents qui met en scène :
- **Un arbitre** qui contrôle le jeu et applique les règles
- **Deux joueurs IA** qui s'affrontent avec des stratégies simples
- **Un observateur** avec interface graphique pour suivre la partie

**Objectifs pédagogiques** :
- Comprendre les systèmes multi-agents
- Implémenter des communications ACL (Agent Communication Language)
- Développer une interface graphique JavaFX
- Gérer des règles de jeu complexes

## 🎲 Règles du Jeu

### Objectif
Accumuler le plus de points sur 5 tours maximum en lançant des dés sans obtenir un total de 7.

### Règles Détaillées
1. **Tour de jeu** :
   - Chaque joueur a jusqu'à 3 lancers par tour
   - Après chaque lancer, le joueur décide de :
     - **LANCER** à nouveau (si < 3 lancers)
     - **PASSER** (conserver son score et terminer)

2. **Calcul des points** :
   - Score = somme des deux dés (2-12)
   - Les points s'accumulent pendant le tour
   - À la fin du tour, les points sont ajoutés au score total

3. **Règle spéciale du 7** :
   - Si un joueur obtient 7 :
     - **Perd tous les points du tour**
     - **Passe immédiatement la main**
     - **Ne peut plus lancer dans ce tour**

4. **Fin de partie** :
   - 5 tours maximum
   - Le joueur avec le score le plus élevé gagne
   - En cas d'égalité : match nul

## 🏗️ Architecture


graph TB
    Start[StartJade.java] --> JADE
    JADE --> Arbitre[ArbitreAgent]
    JADE --> IA1[JoueurIAAgent - IA1]
    JADE --> IA2[JoueurIAAgent - IA2]
    JADE --> Observateur[ObservateurFX]
    
    Arbitre -->|ACL Messages| IA1
    Arbitre -->|ACL Messages| IA2
    Arbitre -->|game-update| Observateur
    
    Observateur -->|game-control| Arbitre
    
    Observateur --> JavaFX[JavaFX Interface]
    
    subgraph "Interface Utilisateur"
        JavaFX --> Control[Panneau de contrôle]
        JavaFX --> Scores[Affichage scores]
        JavaFX --> Des[Affichage dés]
        JavaFX --> Graph[Graphique évolution]
        JavaFX --> Logs[Logs séparés IA1/IA2]
    end
```

## 🚀 Installation

### Prérequis
- **Java JDK 17** ou supérieur
- **JADE 4.6.0** (incluse dans le projet)
- **JavaFX 17** SDK

### Méthode 1 : Avec un IDE (Recommandé)
1. Clonez le dépôt :
   ```bash
   git clone https://github.com/votre-utilisateur/jeu-de-sept.git
   cd jeu-de-sept
   ```

2. Importez dans votre IDE (Eclipse/IntelliJ)
3. Configurez JavaFX dans les modules du projet
4. Ajoutez les librairies JADE au classpath
5. Exécutez `StartJade.java`

### Méthode 2 : Ligne de commande
```bash
# Compilation
javac -cp "lib/*:." -d bin src/com/aidc/jeu7/*.java

# Exécution
java -cp "lib/*:bin" com.aidc.jeu7.StartJade
```

## 🖥️ Interface

### Vue d'ensemble
![Interface du Jeu de Sept](docs/images/interface.png)

### Sections principales

1. **🎮 Titre** - "JEU DE SEPT - OBSERVATEUR"
2. **⚙️ Contrôles** - Boutons Démarrer/Nouvelle partie/Quitter
3. **📊 Scores** - Affichage en temps réel des scores
4. **🎲 Dés** - Visualisation des lancers
5. **📈 Graphique** - Évolution des scores tour par tour
6. **📋 Tableau** - Récapitulatif des scores par tour
7. **📝 Logs** - 3 zones séparées pour IA1, événements, IA2

### Boutons
| Bouton | Description | État initial |
|--------|-------------|--------------|
| **▶ Démarrer une partie** | Lance une nouvelle partie | Actif |
| **🔄 Nouvelle partie** | Réinitialise pour une nouvelle partie | Inactif |
| **⏹ Quitter** | Arrête le jeu | Actif |

## 🤖 Agents

### ArbitreAgent (`ArbitreAgent.java`)
**Rôle** : Maître du jeu
```java
// Responsabilités
- Gère le déroulement des tours
- Applique les règles (règle du 7)
- Calcule et met à jour les scores
- Coordonne les communications
- Annonce le vainqueur
```

### JoueurIAAgent (`JoueurIAAgent.java`)
**Rôle** : Joueur intelligent (2 instances)
```java
// Comportement
- Reçoit les requêtes de l'arbitre
- Prend des décisions (LANCER/PASSER)
- Stratégie simple : lancer 70%, passer 30%
- Envoie ses décisions à l'arbitre
```

### ObservateurFX (`ObservateurFX.java`)
**Rôle** : Interface utilisateur
```java
// Fonctionnalités
- Interface JavaFX complète
- Affichage des scores en temps réel
- Logs séparés par joueur
- Graphique d'évolution
- Contrôles utilisateur
```

## 📁 Structure du Projet

```
jeu-de-sept/
├── src/
│   └── com/aidc/jeu7/
│       ├── ArbitreAgent.java      # Contrôleur principal du jeu
│       ├── JoueurIAAgent.java     # Intelligence artificielle des joueurs
│       ├── ObservateurFX.java     # Interface graphique JavaFX
│       └── StartJade.java         # Point d'entrée de l'application
├── lib/
│   ├── jade.jar                   # Framework JADE
│   └── javafx.*.jar                       # jars pour FX
├── README.md                      # Ce fichier
└── .gitignore
```

## 🔧 Compilation et Exécution

### Configuration minimale
```properties
# Mémoire Java
-Xmx512m
-Xms256m

# Modules JavaFX
--module-path /chemin/vers/javafx-sdk-17/lib 
--add-modules javafx.controls,javafx.fxml
```

### Script de lancement (Linux/Mac)
```bash
#!/bin/bash
# lancement.sh

# Configuration JavaFX (adaptez le chemin)
JAVAFX_PATH="/usr/share/openjfx/lib"

# Compilation
javac -cp "lib/*" -d bin src/com/aidc/jeu7/*.java

# Exécution
java -cp "lib/*:bin" \
     --module-path $JAVAFX_PATH \
     --add-modules javafx.controls,javafx.fxml \
     com.aidc.jeu7.StartJade
```

### Script de lancement (Windows)
```batch
@echo off
REM lancement.bat

REM Configuration JavaFX
set JAVAFX_PATH="C:\Program Files\Java\javafx-sdk-17\lib"

REM Compilation
javac -cp "lib\*" -d bin src\com\aidc\jeu7\*.java

REM Exécution
java -cp "lib\*;bin" ^
     --module-path %JAVAFX_PATH% ^
     --add-modules javafx.controls,javafx.fxml ^
     com.aidc.jeu7.StartJade
```

## 📊 Exemple de Déroulement

### Log d'une partie typique
```
=== SYSTÈME INITIALISÉ ===
En attente du démarrage du jeu...

=== DÉBUT DU TOUR 1 ===
C'est au tour de IA1

[IA1] Décision: LANCER
[IA1] Lancer: 3 + 4 = 7 -> 7! Score perdu!
[SYSTÈME] 7 obtenu! Perte des points et passage de main.

=== DÉBUT DU TOUR 1 ===
C'est au tour de IA2
[IA2] Décision: LANCER
[IA2] Lancer: 2 + 3 = 5 -> OK (+5)
[IA2] Décision: PASSER
--- Fin tour - Score tour: 5

... (4 tours supplémentaires)

=== FIN DE PARTIE ===
🎉 IA1 GAGNE LA PARTIE !
Score final: IA1 = 42 | IA2 = 38
```

### Messages ACL utilisés
| Type | De → À | Contenu | Objectif |
|------|--------|---------|----------|
| `game-update` | Arbitre → Tous | Scores, état, événements | Synchronisation |
| `game-control` | Observateur → Arbitre | Commandes utilisateur | Contrôle du jeu |
| `player-action` | Arbitre → Joueur | Demande de décision | Tour de jeu |
| `game-end` | Arbitre → Tous | Résultats finaux | Fin de partie |

## 🛠️ Développement

### Extensions possibles

#### 1. Stratégies IA avancées
```java
public class JoueurIAAvance extends Agent {
    // Stratégie basée sur :
    // - Score actuel
    // - Score de l'adversaire
    // - Nombre de tours restants
    // - Probabilités statistiques
}
```

#### 2. Interface enrichie
- Animations 3D des dés
- Sons et effets sonores
- Historique détaillé des parties
- Options de configuration

#### 3. Fonctionnalités réseau
- Mode multijoueur en réseau
- Tournois avec plusieurs IA
- Classements et statistiques

#### 4. Règles supplémentaires
- Variantes du jeu (dés pipés, bonus, malus)
- Différents niveaux de difficulté
- Mode entraînement avec feedback

### Débogage
```java
// Activer les logs détaillés JADE
java -cp "lib/*:bin" \
     -Djava.util.logging.config.file=logging.properties \
     jade.Boot ...
```

### Tests
```bash
# Exécuter des tests automatisés
./scripts/test-strategies.sh

# Générer des statistiques
java -cp "bin" com.aidc.jeu7.Statistiques 1000
# Simule 1000 parties et analyse les stratégies
```

## 📄 Licence

Ce projet est sous licence MIT.

```text
MIT License

Copyright (c) 2024 Votre Nom

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 🤝 Contribution

Les contributions sont les bienvenues ! Pour contribuer :

1. Forkez le projet
2. Créez une branche pour votre fonctionnalité (`git checkout -b feature/AmazingFeature`)
3. Committez vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Pushez vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

## 📞 Support

Pour toute question ou problème :
- Ouvrez https://babacarfayeresume.vercel.app/fr


**Développé avec ❤️ dans le cadre d'un projet sur les systèmes multi-agents**
