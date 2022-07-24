/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */

/**
 * Provides classes and interfaces for scaling Java AWT Images using xBRZ.
 */
module io.github.stanio.xbrz.awt {

    exports io.github.stanio.xbrz.awt;
    exports io.github.stanio.xbrz.awt.util;

    requires transitive io.github.stanio.xbrz.core;
    requires transitive java.desktop;

}
