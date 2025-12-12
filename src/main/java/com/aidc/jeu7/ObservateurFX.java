package com.aidc.jeu7;

import jade.core.Agent;
import jade.core.AID; // AJOUTEZ CET IMPORT
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.FontWeight;
import javafx.geometry.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class ObservateurFX extends Agent {

    private ObservateurUI uiInstance;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " démarré - Observateur JavaFX");

        // Initialiser JavaFX toolkit d'abord
        try {
            // Cette méthode initialise JavaFX toolkit
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // JavaFX est déjà initialisé, c'est bon
        }

        // Créer l'interface dans le thread JavaFX
        Platform.runLater(() -> {
            try {
                uiInstance = new ObservateurUI(this);
                Scene scene = uiInstance.createScene();
                Stage stage = new Stage();
                stage.setScene(scene);
                stage.setTitle("Jeu de Sept - Contrôle Étape par Étape");
                stage.show();
                uiInstance.logMessage("=== SYSTÈME INITIALISÉ ===");
                uiInstance.logMessage("En attente du démarrage du jeu...");
            } catch (Exception e) {
                System.err.println("Erreur lors du démarrage de JavaFX: " + e.getMessage());
                e.printStackTrace();
            }
        });

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                // Écouter les mises à jour du jeu
                MessageTemplate mt1 = MessageTemplate.MatchConversationId("game-update");
                ACLMessage msg1 = receive(mt1);
                if (msg1 != null) {
                    final String content = msg1.getContent();
                    Platform.runLater(() -> {
                        if (uiInstance != null) {
                            uiInstance.updateGameState(content);
                        }
                    });
                }

                // Écouter les messages de contrôle
                MessageTemplate mt2 = MessageTemplate.MatchConversationId("game-control");
                ACLMessage msg2 = receive(mt2);
                if (msg2 != null) {
                    final String content = msg2.getContent();
                    Platform.runLater(() -> {
                        if (uiInstance != null) {
                            uiInstance.updateControlMessage(content);
                        }
                    });
                }

                // Écouter la fin de partie
                MessageTemplate mt3 = MessageTemplate.MatchConversationId("game-end");
                ACLMessage msg3 = receive(mt3);
                if (msg3 != null) {
                    final String content = msg3.getContent();
                    Platform.runLater(() -> {
                        if (uiInstance != null) {
                            uiInstance.showGameResult(content);
                        }
                    });
                }

                if (msg1 == null && msg2 == null && msg3 == null) {
                    block();
                }
            }
        });
    }

    public void envoyerCommande(String commande) {
        System.out.println("Observateur envoie commande: " + commande + " à Arbitre");
        try {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            AID arbitreAID = new AID("Arbitre", AID.ISLOCALNAME);
            msg.addReceiver(arbitreAID);
            msg.setConversationId("game-control");
            msg.setContent(commande);
            send(msg);
            System.out.println("Observateur: Commande envoyée à " + arbitreAID.getLocalName() +
                    " avec conversationId: " + msg.getConversationId());
        } catch (Exception e) {
            System.err.println("Erreur envoi commande: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Classe UI JavaFX
    public class ObservateurUI {
        private ObservateurFX agent;
        private TextArea logArea;
        private Label lblScoreIA1, lblScoreIA2, lblTour, lblJoueurCourant, lblScoreTour, lblLancersRestants;
        private Label lblMessageStatut;
        private BarChart<String, Number> scoreChart;
        private XYChart.Series<String, Number> seriesIA1, seriesIA2;
        private VBox root;
        private Button btnDemarrer, btnSuivant, btnQuitter, btnModeAuto;
        private Label lblDe1, lblDe2, lblResultat;
        private int currentTour = 0;
        private boolean modeAuto = false;

        public ObservateurUI(ObservateurFX agent) {
            this.agent = agent;
        }

        public Scene createScene() {
            // Création de l'interface
            root = new VBox(10);
            root.setPadding(new Insets(15));
            root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");

            // Titre
            Label title = new Label("🎮 JEU DE SEPT - ÉTAPE PAR ÉTAPE");
            title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
            title.setTextFill(Color.WHITE);
            title.setAlignment(Pos.CENTER);

            // Panneau de contrôle
            HBox controlPanel = createControlPanel();

            // Panneau des scores
            GridPane scorePanel = createScorePanel();

            // Panneau des dés
            HBox dicePanel = createDicePanel();

            // Graphique des scores
            scoreChart = createScoreChart();

            // Zone de log
            logArea = new TextArea();
            logArea.setEditable(false);
            logArea.setPrefHeight(200);
            logArea.setStyle("-fx-control-inner-background: #0f3460; -fx-text-fill: white; " +
                    "-fx-font-family: 'Monospaced'; -fx-font-size: 12px;");

            // Message de statut
            lblMessageStatut = new Label("Prêt à démarrer...");
            lblMessageStatut.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            lblMessageStatut.setTextFill(Color.LIGHTYELLOW);
            lblMessageStatut.setAlignment(Pos.CENTER);
            lblMessageStatut.setMaxWidth(Double.MAX_VALUE);

            // Ajout des composants
            root.getChildren().addAll(title, controlPanel, scorePanel, dicePanel,
                    scoreChart, lblMessageStatut, logArea);

            // Scène
            Scene scene = new Scene(root, 1000, 800);

            return scene;
        }

        private HBox createControlPanel() {
            HBox panel = new HBox(10);
            panel.setAlignment(Pos.CENTER);
            panel.setPadding(new Insets(10));
            panel.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 10;");

            btnDemarrer = createButton("▶ Démarrer", Color.LIMEGREEN);
            btnSuivant = createButton("⏭ Suivant", Color.DODGERBLUE);
            btnQuitter = createButton("⏹ Quitter", Color.INDIANRED);
            btnModeAuto = createButton("🤖 Mode Auto", Color.GOLD);

            btnSuivant.setDisable(true);
            btnModeAuto.setDisable(true);

            // Actions des boutons
            btnDemarrer.setOnAction(e -> {
                logMessage("Démarrage du jeu...");
                btnDemarrer.setDisable(true);
                btnSuivant.setDisable(false);
                btnModeAuto.setDisable(false);
                agent.envoyerCommande("action=demarrer");
            });

            btnSuivant.setOnAction(e -> {
                agent.envoyerCommande("action=suivant");
            });

            btnQuitter.setOnAction(e -> {
                agent.envoyerCommande("action=quitter");
                btnDemarrer.setDisable(false);
                btnSuivant.setDisable(true);
                btnModeAuto.setDisable(true);
            });

            btnModeAuto.setOnAction(e -> {
                modeAuto = !modeAuto;
                if (modeAuto) {
                    btnModeAuto.setText("⏸ Mode Manuel");
                    btnModeAuto.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
                    logMessage("Mode automatique activé");
                } else {
                    btnModeAuto.setText("🤖 Mode Auto");
                    btnModeAuto.setStyle("-fx-background-color: gold; -fx-text-fill: black;");
                    logMessage("Mode manuel activé");
                }
            });

            panel.getChildren().addAll(btnDemarrer, btnSuivant, btnModeAuto, btnQuitter);
            return panel;
        }

        private GridPane createScorePanel() {
            GridPane grid = new GridPane();
            grid.setHgap(15);
            grid.setVgap(10);
            grid.setPadding(new Insets(15));
            grid.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 10;");

            // IA1
            Label ia1Title = new Label("🤖 IA1");
            ia1Title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            ia1Title.setTextFill(Color.LIGHTBLUE);

            lblScoreIA1 = new Label("0");
            lblScoreIA1.setFont(Font.font("Arial", FontWeight.BOLD, 32));
            lblScoreIA1.setTextFill(Color.LIGHTBLUE);

            // Tour
            Label tourTitle = new Label("🔄 TOUR");
            tourTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            tourTitle.setTextFill(Color.WHITE);

            lblTour = new Label("0/5");
            lblTour.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            lblTour.setTextFill(Color.GOLD);

            // Joueur courant
            Label joueurTitle = new Label("🎯 JOUEUR ACTUEL");
            joueurTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            joueurTitle.setTextFill(Color.WHITE);

            lblJoueurCourant = new Label("---");
            lblJoueurCourant.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            lblJoueurCourant.setTextFill(Color.LIGHTCORAL);

            // Score tour
            Label scoreTourTitle = new Label("📊 SCORE TOUR");
            scoreTourTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            scoreTourTitle.setTextFill(Color.WHITE);

            lblScoreTour = new Label("0");
            lblScoreTour.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            lblScoreTour.setTextFill(Color.LIGHTGREEN);

            // Lancers restants
            Label lancersTitle = new Label("🎲 LANCERS");
            lancersTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            lancersTitle.setTextFill(Color.WHITE);

            lblLancersRestants = new Label("3/3");
            lblLancersRestants.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            lblLancersRestants.setTextFill(Color.LIGHTCYAN);

            // IA2
            Label ia2Title = new Label("🤖 IA2");
            ia2Title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            ia2Title.setTextFill(Color.LIGHTPINK);

            lblScoreIA2 = new Label("0");
            lblScoreIA2.setFont(Font.font("Arial", FontWeight.BOLD, 32));
            lblScoreIA2.setTextFill(Color.LIGHTPINK);

            // Ajout au grid
            grid.add(ia1Title, 0, 0);
            grid.add(lblScoreIA1, 0, 1);

            grid.add(tourTitle, 1, 0);
            grid.add(lblTour, 1, 1);

            grid.add(joueurTitle, 2, 0);
            grid.add(lblJoueurCourant, 2, 1);

            grid.add(scoreTourTitle, 3, 0);
            grid.add(lblScoreTour, 3, 1);

            grid.add(lancersTitle, 4, 0);
            grid.add(lblLancersRestants, 4, 1);

            grid.add(ia2Title, 5, 0);
            grid.add(lblScoreIA2, 5, 1);

            return grid;
        }

        private HBox createDicePanel() {
            HBox panel = new HBox(20);
            panel.setAlignment(Pos.CENTER);
            panel.setPadding(new Insets(10));
            panel.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10;");

            Label diceTitle = new Label("🎲 DÉS:");
            diceTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            diceTitle.setTextFill(Color.WHITE);

            // Labels pour afficher les dés
            lblDe1 = createDiceLabel("?");
            lblDe2 = createDiceLabel("?");

            Label plusLabel = new Label("+");
            plusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            plusLabel.setTextFill(Color.WHITE);

            Label equalsLabel = new Label("=");
            equalsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            equalsLabel.setTextFill(Color.WHITE);

            lblResultat = new Label("?");
            lblResultat.setFont(Font.font("Arial", FontWeight.BOLD, 32));
            lblResultat.setTextFill(Color.GOLD);

            panel.getChildren().addAll(diceTitle, lblDe1, plusLabel, lblDe2, equalsLabel, lblResultat);
            return panel;
        }

        private Label createDiceLabel(String value) {
            Label label = new Label(value);
            label.setFont(Font.font("Arial", FontWeight.BOLD, 32));
            label.setTextFill(Color.WHITE);
            label.setStyle("-fx-background-color: #4a4a4a; -fx-padding: 15px; -fx-background-radius: 10px; " +
                    "-fx-min-width: 60px; -fx-alignment: center;");
            return label;
        }

        private BarChart<String, Number> createScoreChart() {
            // Axes
            CategoryAxis xAxis = new CategoryAxis();
            xAxis.setLabel("Tours");
            xAxis.setTickLabelFill(Color.WHITE);

            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Score");
            yAxis.setTickLabelFill(Color.WHITE);

            // Chart
            BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
            chart.setTitle("📈 Évolution des scores");
            chart.setLegendVisible(true);
            chart.setPrefHeight(200);
            chart.setStyle("-fx-background-color: rgba(255,255,255,0.05);");

            // Série de données
            seriesIA1 = new XYChart.Series<>();
            seriesIA1.setName("IA1");

            seriesIA2 = new XYChart.Series<>();
            seriesIA2.setName("IA2");

            chart.getData().addAll(seriesIA1, seriesIA2);

            return chart;
        }

        private Button createButton(String text, Color color) {
            Button btn = new Button(text);
            btn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            btn.setTextFill(Color.WHITE);
            btn.setStyle(String.format("-fx-background-color: %s; -fx-padding: 10px 20px; -fx-background-radius: 5px;",
                    color.toString().replace("0x", "#")));
            btn.setCursor(javafx.scene.Cursor.HAND);
            return btn;
        }

        public void updateGameState(String content) {
            try {
                System.out.println("UI reçoit: " + content);
                String[] parts = content.split(";");

                // Variables par défaut
                int ia1Score = 0, ia2Score = 0, scoreTour = 0, tour = 0, lancers = 0;
                String joueur = "", message = "", etat = "";
                int de1 = 0, de2 = 0;
                String resultat = "";

                for (String p : parts) {
                    p = p.trim();
                    String[] keyValue = p.split("=", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = keyValue[1];

                        switch (key) {
                            case "ia1": ia1Score = Integer.parseInt(value); break;
                            case "ia2": ia2Score = Integer.parseInt(value); break;
                            case "score_tour": scoreTour = Integer.parseInt(value); break;
                            case "tour": tour = Integer.parseInt(value); break;
                            case "joueur": joueur = value; break;
                            case "de1": de1 = Integer.parseInt(value); break;
                            case "de2": de2 = Integer.parseInt(value); break;
                            case "resultat": resultat = value; break;
                            case "lancers": lancers = Integer.parseInt(value); break;
                            case "message": message = value; break;
                            case "etat": etat = value; break;
                        }
                    }
                }

                // Mettre à jour l'interface
                int finalIa1Score = ia1Score;
                int finalIa2Score = ia2Score;
                int finalTour = tour;
                String finalJoueur = joueur;
                int finalScoreTour = scoreTour;
                int finalLancers = lancers;
                String finalMessage = message;
                int finalDe = de1;
                int finalDe1 = de2;
                String finalResultat = resultat;
                int finalDe2 = de1;
                int finalDe3 = de2;
                Platform.runLater(() -> {
                    lblScoreIA1.setText(String.valueOf(finalIa1Score));
                    lblScoreIA2.setText(String.valueOf(finalIa2Score));
                    lblTour.setText(finalTour + "/5");
                    lblJoueurCourant.setText(finalJoueur);
                    lblScoreTour.setText(String.valueOf(finalScoreTour));
                    lblLancersRestants.setText((3 - finalLancers) + "/3");

                    // Mettre à jour le message de statut
                    if (!finalMessage.isEmpty()) {
                        lblMessageStatut.setText(finalMessage);
                    }

                    // Mettre à jour les dés si un lancer a eu lieu
                    if (finalDe > 0 && finalDe1 > 0) {
                        updateDice(finalDe, finalDe1, finalResultat);
                    }

                    // Mettre à jour le graphique
                    updateChart(finalTour, finalIa1Score, finalIa2Score);

                    // Log des événements importants
                    if (content.contains("debut_tour")) {
                        logMessage("=== DÉBUT DU TOUR " + finalTour + " ===");
                        logMessage("C'est au tour de " + finalJoueur);
                    } else if (content.contains("lancer_resultat")) {
                        String result = finalResultat.equals("7") ? "7 - Score perdu!" : "OK (+" + (finalDe + finalDe1) + ")";
                        logMessage(finalJoueur + " lance: " + finalDe + " + " + finalDe1 + " = " + (finalDe2 + finalDe3) + " -> " + result);
                    } else if (content.contains("decision")) {
                        String decision = content.contains("decision=LANCER") ? "LANCER" : "PASSER";
                        logMessage(finalJoueur + " décide: " + decision);
                    } else if (content.contains("fin_tour")) {
                        logMessage("Fin du tour de " + finalJoueur + ": Score tour = " + finalScoreTour);
                    }

                    // Si mode auto, déclencher automatiquement l'étape suivante
                    if (modeAuto && !content.contains("attente")) {
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000); // Pause d'1 seconde entre les étapes
                                Platform.runLater(() -> {
                                    btnSuivant.fire();
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                });

            } catch (Exception e) {
                logMessage("[ERREUR] Parsing: " + content);
                e.printStackTrace();
            }
        }

        private void updateDice(int de1, int de2, String resultat) {
            Platform.runLater(() -> {
                lblDe1.setText(String.valueOf(de1));
                lblDe2.setText(String.valueOf(de2));
                int somme = de1 + de2;
                lblResultat.setText(String.valueOf(somme));

                // Changer la couleur selon le résultat
                if (resultat.equals("7")) {
                    lblResultat.setTextFill(Color.RED);
                    lblDe1.setTextFill(Color.RED);
                    lblDe2.setTextFill(Color.RED);
                } else {
                    lblResultat.setTextFill(Color.GREEN);
                    lblDe1.setTextFill(Color.WHITE);
                    lblDe2.setTextFill(Color.WHITE);
                }
            });
        }

        private void updateChart(int tour, int ia1Score, int ia2Score) {
            if (tour > currentTour) {
                currentTour = tour;
                seriesIA1.getData().add(new XYChart.Data<>("T" + tour, ia1Score));
                seriesIA2.getData().add(new XYChart.Data<>("T" + tour, ia2Score));
            } else if (!seriesIA1.getData().isEmpty() && !seriesIA2.getData().isEmpty()) {
                seriesIA1.getData().get(seriesIA1.getData().size() - 1).setYValue(ia1Score);
                seriesIA2.getData().get(seriesIA2.getData().size() - 1).setYValue(ia2Score);
            }
        }

        public void updateControlMessage(String content) {
            Platform.runLater(() -> {
                if (content.contains("message=")) {
                    String message = content.split("message=")[1];
                    lblMessageStatut.setText(message);
                    logMessage("[CONTROLE] " + message);

                    if (content.contains("attente_demarrage")) {
                        btnDemarrer.setDisable(false);
                    } else if (content.contains("jeu_demarre")) {
                        btnDemarrer.setDisable(true);
                        btnSuivant.setDisable(false);
                    }
                }
            });
        }

        public void showGameResult(String content) {
            Platform.runLater(() -> {
                try {
                    String[] parts = content.split(";");
                    int ia1Score = 0, ia2Score = 0;
                    String result = "";

                    for (String p : parts) {
                        p = p.trim();
                        if (p.startsWith("ia1=")) ia1Score = Integer.parseInt(p.split("=")[1]);
                        else if (p.startsWith("ia2=")) ia2Score = Integer.parseInt(p.split("=")[1]);
                        else if (p.startsWith("result=")) result = p.split("=")[1];
                    }

                    String message;
                    Color color;

                    if (result.contains("IA1_GAGNE")) {
                        message = "🎉 IA1 GAGNE LA PARTIE !";
                        color = Color.LIGHTBLUE;
                    } else if (result.contains("IA2_GAGNE")) {
                        message = "🎉 IA2 GAGNE LA PARTIE !";
                        color = Color.LIGHTPINK;
                    } else {
                        message = "🤝 MATCH NUL !";
                        color = Color.GOLD;
                    }

                    logMessage("\n=== FIN DE PARTIE ===");
                    logMessage(message);
                    logMessage("Score final: IA1 = " + ia1Score + " | IA2 = " + ia2Score);

                    // Désactiver les boutons
                    btnSuivant.setDisable(true);
                    btnModeAuto.setDisable(true);

                    // Afficher une alerte
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Fin de Partie");
                    alert.setHeaderText(message);
                    alert.setContentText("Score final:\nIA1: " + ia1Score + "\nIA2: " + ia2Score);
                    alert.showAndWait();

                } catch (Exception e) {
                    logMessage("[ERREUR] Parsing fin de partie: " + content);
                }
            });
        }

        public void logMessage(String message) {
            Platform.runLater(() -> {
                logArea.appendText(message + "\n");
                logArea.setScrollTop(Double.MAX_VALUE);
            });
        }
    }
}