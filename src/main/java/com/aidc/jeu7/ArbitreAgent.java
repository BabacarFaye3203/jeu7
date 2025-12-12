package com.aidc.jeu7;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Random;

public class ArbitreAgent extends Agent {

    private final int nbToursMax = 5;
    private final int maxLancersParTour = 3;
    private static final String AGENT_IA1 = "IA1";
    private static final String AGENT_IA2 = "IA2";
    private static final String AGENT_OBSERVATEUR = "Observateur";

    private int scoreIA1 = 0;
    private int scoreIA2 = 0;
    private int tourActuel = 1;
    private int scoreTour = 0;
    private int lancersEffectues = 0;
    private String joueurCourant = "IA1";
    private Random random = new Random();

    // Pour le contrôle étape par étape
    private boolean attenteEtape = false; // DÉSACTIVÉ pour le moment
    private boolean jeuEnPause = true;
    private boolean jeuTermine = false;
    private boolean jeuDemarre = false;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " démarré - arbitre du Jeu de Sept");

        // Envoyer un message initial à l'observateur
        sendGameUpdate("initialisation;etat=pret");

        // Comportement pour écouter les messages de contrôle
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchConversationId("game-control");
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    String content = msg.getContent();
                    String sender = msg.getSender().getLocalName();
                    System.out.println("Arbitre reçoit de " + sender + ": " + content);

                    if (content.contains("action=demarrer")) {
                        System.out.println("Arbitre: Signal de démarrage reçu!");
                        jeuEnPause = false;
                        jeuDemarre = true;
                        sendControlMessage("jeu_demarre;message=Le jeu commence!");

                        // Démarrer le jeu
                        addBehaviour(new OneShotBehaviour() {
                            @Override
                            public void action() {
                                runGame();
                            }
                        });
                    }
                    else if (content.contains("action=suivant")) {
                        System.out.println("Arbitre: Étape suivante");
                        jeuEnPause = false;
                        sendControlMessage("etape_suivante;message=Étape suivante...");
                    }
                    else if (content.contains("action=quitter")) {
                        System.out.println("Arbitre: Arrêt demandé");
                        jeuTermine = true;
                        jeuEnPause = false;
                        sendControlMessage("jeu_arrete;message=Jeu arrêté");
                    }
                } else {
                    block();
                }
            }
        });

        // Envoyer le message d'attente initial
        sendControlMessage("attente_demarrage;message=Cliquez sur Démarrer pour commencer");
    }

    private void runGame() {
        System.out.println("=== DÉMARRAGE DE LA PARTIE ===");

        try {
            while (tourActuel <= nbToursMax && !jeuTermine) {
                System.out.println("\n=== Début du tour " + tourActuel + " ===");

                // Tour IA1
                joueurCourant = "IA1";
                sendGameUpdate("debut_tour;joueur=IA1;tour=" + tourActuel);
                waitForNextStep();
                if (jeuTermine) break;
                handleTurn(AGENT_IA1, "IA1");
                if (checkFinPartie() || jeuTermine) break;

                // Tour IA2
                joueurCourant = "IA2";
                sendGameUpdate("debut_tour;joueur=IA2;tour=" + tourActuel);
                waitForNextStep();
                if (jeuTermine) break;
                handleTurn(AGENT_IA2, "IA2");
                if (checkFinPartie() || jeuTermine) break;

                tourActuel++;
            }

            if (!jeuTermine) {
                announceWinner();
            }
        } catch (Exception e) {
            System.err.println("Erreur dans runGame: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== PARTIE TERMINÉE ===");
    }

    private void waitForNextStep() {
        if (attenteEtape && jeuDemarre) {
            jeuEnPause = true;
            sendControlMessage("attente_etape;message=Cliquez sur Suivant pour continuer");

            System.out.println("Arbitre en attente de l'étape suivante...");
            while (jeuEnPause && !jeuTermine && jeuDemarre) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleTurn(String agentName, String role) {
        lancersEffectues = 0;
        scoreTour = 0;
        boolean tourTermine = false;

        System.out.println("\n--- Tour de " + agentName + " ---");
        sendGameUpdate("nouveau_tour;joueur=" + role + ";score_tour=0;lancers=0");

        while (!tourTermine && lancersEffectues < maxLancersParTour && !jeuTermine) {
            sendActionRequest(agentName, role);
            sendGameUpdate("attente_decision;joueur=" + role);

            // Petite pause pour l'affichage
            try { Thread.sleep(500); } catch (InterruptedException e) {}

            ACLMessage reply = blockingReceiveFrom(agentName, 10000);

            if (reply == null) {
                System.out.println("Pas de réponse de " + agentName + ". PASSER automatique.");
                addScoreGlobal(role, scoreTour);
                sendGameUpdate("timeout_pass;joueur=" + role + ";score_tour=" + scoreTour);
                waitForNextStep();
                if (jeuTermine) break;
                break;
            }

            String content = reply.getContent().trim().toUpperCase();
            System.out.println("Réponse de " + agentName + " : " + content);
            sendGameUpdate("decision;joueur=" + role + ";decision=" + content);

            if ("LANCER".equals(content)) {
                waitForNextStep();
                if (jeuTermine) break;

                // Petite pause dramatique avant le lancer
                try { Thread.sleep(500); } catch (InterruptedException e) {}

                lancerDes(agentName, role);
                lancersEffectues++;
                sendGameUpdate("lancers_effectues;nombre=" + lancersEffectues);

                if (lancersEffectues >= maxLancersParTour) {
                    sendGameUpdate("max_lancers_atteint;joueur=" + role);
                }
            } else {
                // PASSER ou autre
                System.out.println(agentName + " passe.");
                addScoreGlobal(role, scoreTour);
                sendGameUpdate("passe;joueur=" + role + ";score_tour=" + scoreTour);
                waitForNextStep();
                if (jeuTermine) break;
                tourTermine = true;
            }

            waitForNextStep();
            if (jeuTermine) break;
        }

        // Si atteint le max de lancers
        if (lancersEffectues >= maxLancersParTour && !tourTermine && !jeuTermine) {
            addScoreGlobal(role, scoreTour);
            sendGameUpdate("fin_tour_max_lancers;joueur=" + role + ";score_tour=" + scoreTour);
        }

        System.out.println("Fin tour " + role + ". Score tour: " + scoreTour +
                " | Total IA1: " + scoreIA1 + " | IA2: " + scoreIA2);
        sendGameUpdate("fin_tour;joueur=" + role + ";score_final_tour=" + scoreTour +
                ";total_ia1=" + scoreIA1 + ";total_ia2=" + scoreIA2);

        // Petite pause entre les tours
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
    }

    private void lancerDes(String agentName, String role) {
        int de1 = 1 + random.nextInt(6);
        int de2 = 1 + random.nextInt(6);
        int somme = de1 + de2;

        System.out.println(agentName + " lance: " + de1 + " + " + de2 + " = " + somme);
        sendGameUpdate("avant_lancer;joueur=" + role + ";de1=" + de1 + ";de2=" + de2);

        // Petite pause pour l'effet visuel
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        if (somme == 7) {
            scoreTour = 0;
            System.out.println("=> 7 obtenu! Score tour réinitialisé.");
            sendGameUpdate(String.format("lancer_resultat;joueur=%s;de1=%d;de2=%d;resultat=7;score_tour=0", role, de1, de2));
        } else {
            scoreTour += somme;
            System.out.println("Score tour " + role + ": " + scoreTour);
            sendGameUpdate(String.format("lancer_resultat;joueur=%s;de1=%d;de2=%d;resultat=ok;score_tour=%d", role, de1, de2, scoreTour));
        }
    }

    private void addScoreGlobal(String role, int scoreTourToAdd) {
        if ("IA1".equals(role)) scoreIA1 += scoreTourToAdd;
        else scoreIA2 += scoreTourToAdd;
    }

    private boolean checkFinPartie() {
        return tourActuel > nbToursMax;
    }

    private void sendGameUpdate(String extra) {
        String content = String.format("game_update;ia1=%d;ia2=%d;score_tour=%d;joueur=%s;tour=%d;%s",
                scoreIA1, scoreIA2, scoreTour, joueurCourant, tourActuel, extra);

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID(AGENT_IA1, AID.ISLOCALNAME));
        msg.addReceiver(new AID(AGENT_IA2, AID.ISLOCALNAME));
        msg.addReceiver(new AID(AGENT_OBSERVATEUR, AID.ISLOCALNAME));
        msg.setConversationId("game-update");
        msg.setContent(content);
        send(msg);
        System.out.println("Arbitre envoie: " + content);
    }

    private void sendControlMessage(String message) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID(AGENT_OBSERVATEUR, AID.ISLOCALNAME));
        msg.setConversationId("game-control");
        msg.setContent(message);
        send(msg);
        System.out.println("Arbitre contrôle: " + message);
    }

    private void announceWinner() {
        System.out.println("\n=== Partie terminée ===");
        System.out.println("Score final -> " + AGENT_IA1 + ": " + scoreIA1 + " | " + AGENT_IA2 + ": " + scoreIA2);

        String resultat;
        if (scoreIA1 > scoreIA2) {
            resultat = "IA1_GAGNE";
            System.out.println("Vainqueur: IA1");
        } else if (scoreIA2 > scoreIA1) {
            resultat = "IA2_GAGNE";
            System.out.println("Vainqueur: IA2");
        } else {
            resultat = "EGALITE";
            System.out.println("Match nul!");
        }

        ACLMessage finalMsg = new ACLMessage(ACLMessage.INFORM);
        finalMsg.addReceiver(new AID(AGENT_IA1, AID.ISLOCALNAME));
        finalMsg.addReceiver(new AID(AGENT_IA2, AID.ISLOCALNAME));
        finalMsg.addReceiver(new AID(AGENT_OBSERVATEUR, AID.ISLOCALNAME));
        finalMsg.setConversationId("game-end");
        finalMsg.setContent(String.format("FINAL;ia1=%d;ia2=%d;result=%s", scoreIA1, scoreIA2, resultat));
        send(finalMsg);
        System.out.println("Arbitre: Fin de partie envoyée");
    }

    private void sendActionRequest(String agentLocalName, String role) {
        ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
        req.addReceiver(new AID(agentLocalName, AID.ISLOCALNAME));
        req.setConversationId("player-action");
        req.setContent(String.format("YOUR_MOVE;ia1=%d;ia2=%d;scoreTour=%d;joueur=%s;tour=%d",
                scoreIA1, scoreIA2, scoreTour, role, tourActuel));
        send(req);
        System.out.println("Arbitre demande à " + agentLocalName);
    }

    private ACLMessage blockingReceiveFrom(String agentLocalName, long timeoutMillis) {
        MessageTemplate mt = MessageTemplate.MatchConversationId("player-action");
        return blockingReceive(mt, timeoutMillis);
    }
}