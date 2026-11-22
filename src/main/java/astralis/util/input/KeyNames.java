package astralis.util.input;

import java.util.HashMap;
import java.util.Map;

public class KeyNames {
    private static Map<Integer, String> namesByCode;
    private static Map<String, Integer> codesByName;

    private KeyNames() {}

    public static String getKeyName(int keyCode) {
        return namesByCode.get(keyCode);
    }

    public static int getKeyCode(String keyName) {

        if (keyName == null) return -1;
        Integer value = codesByName.get(keyName.toUpperCase(java.util.Locale.ROOT));
        return value == null ? -1 : value;
    }

    static {
        namesByCode = new HashMap<>();
        codesByName = new HashMap<>();

        // GLFW key names and codes.
        String keyTable =
                "0=48;1=49;2=50;3=51;4=52;5=53;6=54;7=55;8=56;9=57;A=65;B=66;C=67;D=68;E=69;F=70;G=71;H=72;I=73;J=74;K=75;L=76;M=77;N=78;O=79;P=80;Q=81;R=82;S=83;T=84;U=85;V=86;W=87;X=88;Y=89;Z=90;F1=290;F2=291;F3=292;F4=293;F5=294;F6=295;F7=296;F8=297;F9=298;F10=299;F11=300;F12=301;NUMLOCK=282;NUMPAD0=320;NUMPAD1=321;NUMPAD2=322;NUMPAD3=323;NUMPAD4=324;NUMPAD5=325;NUMPAD6=326;NUMPAD7=327;NUMPAD8=328;NUMPAD9=329;NUMPADCOMMA=330;NUMPADENTER=335;NUMPADEQUALS=336;DOWN=264;LEFT=263;RIGHT=262;UP=265;ADD=334;APOSTROPHE=39;BACKSLASH=92;COMMA=44;EQUALS=61;GRAVE=96;LBRACKET=91;MINUS=45;MULTIPLY=332;PERIOD=46;RBRACKET=93;SEMICOLON=59;SLASH=47;SPACE=32;TAB=258;LALT=342;LCONTROL=341;LSHIFT=340;LWIN=343;RALT=346;RCONTROL=345;RSHIFT=344;RWIN=347;RETURN=257;BACKSPACE=259;DELETE=261;END=269;HOME=268;INSERT=260;PAGEDOWN=267;PAGEUP=266;CAPSLOCK=280;PAUSE=284;SCROLLLOCK=281;PRINTSCREEN=283";
        for (String entry : keyTable.split(";")) {
            String[] nameAndCode = entry.split("=");
            String name = nameAndCode[0];
            int code = Integer.parseInt(nameAndCode[1]);
            namesByCode.put(code, name);
            codesByName.put(name, code);
        }
    }
}
