package org.gipsybuho.service;

import java.io.IOException;

public final class TextToSpeechService {

    private static Process currentProcess;

    private TextToSpeechService() {}

    public static synchronized void speak(String text) {
        if (text == null || text.isBlank()) return;

        stop();

        ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            "Add-Type -AssemblyName System.Speech; "
                + "$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$speaker.Rate = 0; "
                + "$speaker.Volume = 90; "
                + "$speaker.Speak($env:GM_TTS_TEXT);"
        );
        pb.environment().put("GM_TTS_TEXT", normalizarTexto(text));
        pb.redirectErrorStream(true);

        try {
            currentProcess = pb.start();
        } catch (IOException e) {
            currentProcess = null;
            System.err.println("No se pudo iniciar la voz del asistente IA: " + e.getMessage());
        }
    }

    public static synchronized void stop() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroy();
        }
        currentProcess = null;
    }

    private static String normalizarTexto(String text) {
        return text
            .replaceAll("(?s)```.*?```", " bloque de código ")
            .replaceAll("[#*_`>\\[\\](){}|]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
