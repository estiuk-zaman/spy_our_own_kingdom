package com.estiuk.studyxp;

/** Tiny English / Bangla switch. */
final class Lang {
    static boolean bn = false;
    static String t(String en, String bnText) { return bn ? bnText : en; }
    private Lang() { }
}
