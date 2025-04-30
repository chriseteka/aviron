package com.github.jlangch.aviron.util;


public class OS {

    public static OsType type() {
        final String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("windows")) {
            return OsType.Windows;
        }
        else if (osName.startsWith("mac os x")) {
            return OsType.MacOSX;
        }
        else if (osName.startsWith("linux")) {
            return OsType.Linux;
        }
        else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix"))  {
            return OsType.Unix;
        }
        else {
            return OsType.Unknown;
        }
    }

    public static boolean isMacOSX() {
        return OsType.MacOSX == type();
    }

    public static boolean isLinux() {
        return OsType.Linux == type();
    }

    public static boolean isWindows() {
        return OsType.Windows == type();
    }


    public static enum OsType { MacOSX, Unix, Linux, Windows, Unknown };
}
