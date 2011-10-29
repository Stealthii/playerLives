package com.pathogenstudios.generic;

public class Log {
    public static String pluginName = "PathogenGeneric";
    public static boolean verbose = false;
    
    public static void m(String msg) {System.out.println("[" + pluginName + "] " + msg);}
    public static void e(String msg) {System.err.println("[" + pluginName + "] " + msg);}
    public static void w(String msg) {e(msg);}
    public static void d(String msg) {if (verbose) {m(msg);}}
    public static void v(String msg) {d(msg);}
}
