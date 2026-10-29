package com.teamcanvas.common.util;

import java.util.Locale;

/** Single place that defines how e-mail addresses are normalised before they are stored or compared. */
public final class Emails {

    private Emails() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
