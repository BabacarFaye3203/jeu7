package com.aidc.jeu7;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class JoueurIAAgent extends Agent {

    private String monNom;

    @Override
    protected void setup() {
        monNom = getLocalName();
        System.out.println(monNom + " démarré (IA)");

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchConversationId("player-action");
                ACLMessage msg = myAgent.receive(mt);

                if (msg != null) {
                    // Décision simple: lancer 2 fois puis passer
                    String decision = "LANCER"; // Stratégie très simple

                    // Parfois passer pour varier
                    if (Math.random() < 0.3) {
                        decision = "PASSER";
                    }

                    System.out.println(monNom + " décide: " + decision);

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setConversationId("player-action");
                    reply.setContent(decision);
                    send(reply);
                } else {
                    block();
                }
            }
        });
    }
}