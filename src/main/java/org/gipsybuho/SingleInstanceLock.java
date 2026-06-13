package org.gipsybuho;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class SingleInstanceLock {

    private static final int LOCK_PORT = 54321; // Un puerto arbitrario para el bloqueo
    private static ServerSocket lockSocket;

    /**
     * Intenta adquirir un bloqueo para asegurar que solo una instancia de la aplicación se esté ejecutando.
     *
     * @return true si el bloqueo se adquirió con éxito (esta es la primera instancia), false en caso contrario.
     */
    public static boolean acquireLock() {
        try {
            // Enlazar solo a loopback evita exponer el puerto de bloqueo en la red.
            lockSocket = new ServerSocket();
            lockSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), LOCK_PORT));
            System.out.println("Bloqueo de instancia única adquirido en el puerto " + LOCK_PORT);
            return true;
        } catch (IOException e) {
            // Si el puerto ya está en uso, significa que otra instancia ya está corriendo.
            System.err.println("No se pudo adquirir el bloqueo de instancia única. Otra instancia de la aplicación ya está en ejecución.");
            return false;
        }
    }

    /**
     * Libera el bloqueo de instancia única cerrando el ServerSocket.
     * Debe llamarse cuando la aplicación se cierra.
     */
    public static void releaseLock() {
        if (lockSocket != null && !lockSocket.isClosed()) {
            try {
                lockSocket.close();
                System.out.println("Bloqueo de instancia única liberado.");
            } catch (IOException e) {
                System.err.println("Error al liberar el bloqueo de instancia única: " + e.getMessage());
            }
        }
    }
}
