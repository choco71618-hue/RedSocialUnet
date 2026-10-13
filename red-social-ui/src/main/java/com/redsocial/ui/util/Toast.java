package com.redsocial.ui.util;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

/**
 * Toasts del sistema: inferiores a la izquierda y con auto-cierre.
 * Centraliza la presentación para garantizar duración y posición.
 */
public final class Toast {

    private static final int DURATION_MS = 3500;

    private Toast() {
    }

    public static void show(String text) {
        show(text, DURATION_MS);
    }

    public static void show(String text, int durationMs) {
        Notification notification = new Notification(text, durationMs, Position.BOTTOM_START);
        notification.open();
    }
}