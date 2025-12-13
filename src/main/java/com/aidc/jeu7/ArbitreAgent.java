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
    private boolean jeuTermine = false;
    private boolean jeuDemarre = false;
    private boolean enCoursExecution = false; // Pour éviter les exécutions parallèles

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " démarré - arbitre du Jeu de Sept");

        // Envoyer un message initial à l'observateur
        sendGameUpdate("initialisation", "etat=pret");

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
                        if (!enCoursExecution) {
                            System.out.println("Arbitre: Signal de démarrage reçu!");
                            jeuDemarre = true;
                            jeuTermine = false;
                            enCoursExecution = true;

                            // Réinitialiser le jeu si nécessaire
                            if (tourActuel > nbToursMax) {
                                reinitialiserJeu();
                            }

                            sendControlMessage("jeu_demarre;message=Le jeu commence!");

                            // Démarrer le jeu dans un nouveau comportement
                            addBehaviour(new OneShotBehaviour() {
                                @Override
                                public void action() {
                                    runGame();
                                }
                            });
                        } else {
                            System.out.println("Arbitre: Jeu déjà en cours, démarrage ignoré");
                        }
                    }
                    else if (content.contains("action=quitter")) {
                        System.out.println("Arbitre: Arrêt demandé");
                        jeuTermine = true;
                        sendControlMessage("jeu_arrete;message=Jeu arrêté");
                    }
                    else if (content.contains("action=reinitialiser")) {
                        System.out.println("Arbitre: Réinitialisation demandée");
                        reinitialiserJeu();
                        sendControlMessage("jeu_reinitialise;message=Jeu réinitialisé - Prêt pour une nouvelle partie");
                    }
                } else {
                    block();
                }
            }
        });

        // Envoyer le message d'attente initial
        sendControlMessage("attente_demarrage;message=Cliquez sur Démarrer pour commencer");
    }

    private void reinitialiserJeu() {
        scoreIA1 = 0;
        scoreIA2 = 0;
        tourActuel = 1;
        scoreTour = 0;
        lancersEffectues = 0;
        joueurCourant = "IA1";
        jeuTermine = false;
        jeuDemarre = false;
        enCoursExecution = false;

        // Envoyer un message de réinitialisation à tous
        sendGameUpdate("reinitialisation", "etat=pret");
        System.out.println("Arbitre: Jeu réinitialisé");
    }

    private void runGame() {
        System.out.println("=== DÉMARRAGE DE LA PARTIE ===");

        try {
            while (tourActuel <= nbToursMax && !jeuTermine) {
                System.out.println("\n=== Début du tour " + tourActuel + " ===");

                // Tour IA1
                joueurCourant = "IA1";
                sendGameUpdate("debut_tour", "joueur=IA1;tour=" + tourActuel);
                handleTurn(AGENT_IA1, "IA1");
                if (checkFinPartie() || jeuTermine) break;

                // Tour IA2
                joueurCourant = "IA2";
                sendGameUpdate("debut_tour", "joueur=IA2;tour=" + tourActuel);
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
        } finally {
            enCoursExecution = false;
        }

        System.out.println("=== PARTIE TERMINÉE ===");
    }

    private void handleTurn(String agentName, String role) {
        lancersEffectues = 0;
        scoreTour = 0;
        boolean tourTermine = false;
        boolean septObtenu = false; // Flag pour suivre si un 7 a été obtenu

        System.out.println("\n--- Tour de " + agentName + " ---");
        sendGameUpdate("nouveau_tour", "joueur=" + role + ";score_tour=0;lancers=0");

        while (!tourTermine && lancersEffectues < maxLancersParTour && !jeuTermine && !septObtenu) {
            sendActionRequest(agentName, role);
            sendGameUpdate("attente_decision", "joueur=" + role);

            // PAUSE de 1 seconde avant de demander la décision
            try { Thread.sleep(1000); } catch (InterruptedException e) {}

            ACLMessage reply = blockingReceiveFrom(agentName, 10000);

            if (reply == null) {
                System.out.println("Pas de réponse de " + agentName + ". PASSER automatique.");
                addScoreGlobal(role, scoreTour);
                sendGameUpdate("timeout_pass", "joueur=" + role + ";score_tour=" + scoreTour);

                // PAUSE de 3 secondes avant de continuer
                try { Thread.sleep(3000); } catch (InterruptedException e) {}

                break;
            }

            String content = reply.getContent().trim().toUpperCase();
            System.out.println("Réponse de " + agentName + " : " + content);
            sendGameUpdate("decision", "joueur=" + role + ";decision=" + content);

            if ("LANCER".equals(content)) {
                // PAUSE de 3 secondes avant le lancer (dramatisation)
                try { Thread.sleep(3000); } catch (InterruptedException e) {}

                // CORRECTION : Appeler la méthode qui retourne si un 7 a été obtenu
                septObtenu = lancerDes(agentName, role);
                lancersEffectues++;
                sendGameUpdate("lancers_effectues", "nombre=" + lancersEffectues);

                // CORRECTION : Si 7 obtenu, terminer le tour immédiatement
                if (septObtenu) {
                    System.out.println("7 obtenu! " + agentName + " perd ses points et passe la main.");
                    addScoreGlobal(role, 0); // Ajouter 0 au score global (car scoreTour = 0)
                    sendGameUpdate("sept_obtenu", "joueur=" + role + ";message=7 obtenu! Perte des points et passage de main.");

                    // PAUSE de 3 secondes avant de passer au joueur suivant
                    try { Thread.sleep(3000); } catch (InterruptedException e) {}
                    break; // Sortir immédiatement de la boucle
                }

                if (lancersEffectues >= maxLancersParTour) {
                    sendGameUpdate("max_lancers_atteint", "joueur=" + role);
                }
            } else {
                // PASSER ou autre
                System.out.println(agentName + " passe.");
                addScoreGlobal(role, scoreTour);
                sendGameUpdate("passe", "joueur=" + role + ";score_tour=" + scoreTour);

                // PAUSE de 3 secondes avant de continuer
                try { Thread.sleep(3000); } catch (InterruptedException e) {}

                tourTermine = true;
            }
        }

        // Si atteint le max de lancers ET pas de 7 obtenu
        if (lancersEffectues >= maxLancersParTour && !tourTermine && !jeuTermine && !septObtenu) {
            addScoreGlobal(role, scoreTour);
            sendGameUpdate("fin_tour_max_lancers", "joueur=" + role + ";score_tour=" + scoreTour);
        }

        // Afficher le résumé seulement si pas de 7
        if (!septObtenu) {
            System.out.println("Fin tour " + role + ". Score tour: " + scoreTour +
                    " | Total IA1: " + scoreIA1 + " | IA2: " + scoreIA2);
            sendGameUpdate("fin_tour", "joueur=" + role + ";score_final_tour=" + scoreTour +
                    ";total_ia1=" + scoreIA1 + ";total_ia2=" + scoreIA2);
        } else {
            // Si 7 obtenu, afficher un message spécial
            System.out.println("Fin tour " + role + " (7 obtenu). Score tour: 0" +
                    " | Total IA1: " + scoreIA1 + " | IA2: " + scoreIA2);
            sendGameUpdate("fin_tour_sept", "joueur=" + role + ";score_final_tour=0;sept_obtenu=true" +
                    ";total_ia1=" + scoreIA1 + ";total_ia2=" + scoreIA2);
        }

        // PAUSE de 3 secondes entre les tours
        try { Thread.sleep(3000); } catch (InterruptedException e) {}
    }

    private boolean lancerDes(String agentName, String role) {
        int de1 = 1 + random.nextInt(6);
        int de2 = 1 + random.nextInt(6);
        int somme = de1 + de2;

        System.out.println(agentName + " lance: " + de1 + " + " + de2 + " = " + somme);
        sendGameUpdate("avant_lancer", "joueur=" + role + ";de1=" + de1 + ";de2=" + de2);

        // Petite pause pour l'effet visuel
        try { Thread.sleep(300); } catch (InterruptedException e) {}

        if (somme == 7) {
            scoreTour = 0; // Perte de tous les points du tour
            System.out.println("=> 7 obtenu! Score tour réinitialisé et passage de main.");
            sendGameUpdate("lancer_resultat", String.format("joueur=%s;de1=%d;de2=%d;resultat=7;score_tour=0", role, de1, de2));
            return true; // Retourne true pour indiquer qu'un 7 a été obtenu
        } else {
            scoreTour += somme;
            System.out.println("Score tour " + role + ": " + scoreTour);
            sendGameUpdate("lancer_resultat", String.format("joueur=%s;de1=%d;de2=%d;resultat=ok;score_tour=%d", role, de1, de2, scoreTour));
            return false; // Retourne false car pas de 7
        }
    }

    private void addScoreGlobal(String role, int scoreTourToAdd) {
        if ("IA1".equals(role)) scoreIA1 += scoreTourToAdd;
        else scoreIA2 += scoreTourToAdd;
    }

    private boolean checkFinPartie() {
        return tourActuel > nbToursMax;
    }

    private void sendGameUpdate(String type, String extra) {
        String content = String.format("game_update;type=%s;ia1=%d;ia2=%d;score_tour=%d;joueur=%s;tour=%d;%s",
                type, scoreIA1, scoreIA2, scoreTour, joueurCourant, tourActuel, extra);

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

        // Envoyer un message supplémentaire pour indiquer que le jeu peut être redémarré
        sendControlMessage("fin_partie;message=Partie terminée - Cliquez sur Nouvelle partie pour recommencer");
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