package com.aidc.jeu7;

public class StartJade {
    public static void main(String[] args) {
        jade.Boot.main(new String[] {
                "-gui",
                "-local-port", "1099",
                "Arbitre:com.aidc.jeu7.ArbitreAgent;" +
                        "IA1:com.aidc.jeu7.JoueurIAAgent;" +
                        "IA2:com.aidc.jeu7.JoueurIAAgent;" +
                        "Observateur:com.aidc.jeu7.ObservateurFX"
        });
    }
}