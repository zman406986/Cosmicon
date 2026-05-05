package data.scripts.cosmicon.prismatic;

import java.util.ArrayList;
import java.util.List;

import data.scripts.Strings;
import data.scripts.cosmicon.util.PrismaticDisplayHelper;

public class PrismaticFaceDisplay {
    
    private static final String[] FACE_LETTERS = {"A", "B", "C", "D", "E", "F"};
    
    public static String formatSingleFace(int faceIndex, int faceValue, boolean isSpecial) {
        if (faceIndex < 0 || faceIndex >= FACE_LETTERS.length) return "";
        String letter = FACE_LETTERS[faceIndex];
        String marker = isSpecial ? Strings.get("prismatic.face.special_marker") : "";
        return Strings.format("prismatic.face.rolled_label", letter, faceValue, marker);
    }
    
    public static String formatRolledResult(PrismaticDiceInstance instance) {
        if (instance == null) return "";
        return formatSingleFace(instance.faceIndex, instance.rolledFace, instance.isSpecialFace);
    }
    
    public static List<String> formatAllFaces(PrismaticDiceType type, boolean useTrueVersion) {
        if (type == null) return new ArrayList<>();
        
        List<String> results = new ArrayList<>();
        int[] faces = type.getFaces(useTrueVersion);
        
        for (int i = 0; i < faces.length && i < FACE_LETTERS.length; i++) {
            boolean isSpecial = type.isSpecialFace(i, useTrueVersion);
            results.add(formatSingleFace(i, faces[i], isSpecial));
        }
        
        return results;
    }
    
    public static String formatFaceMappingCompact(PrismaticDiceType type, boolean useTrueVersion) {
        if (type == null) return Strings.get("prismatic.face.no_dice");
        
        List<String> faces = formatAllFaces(type, useTrueVersion);
        String separator = Strings.get("prismatic.face.separator");
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < faces.size(); i++) {
            sb.append(faces.get(i));
            if (i < faces.size() - 1) sb.append(separator);
        }
        
        return sb.toString();
    }
    
    public static String getEffectDescription(PrismaticDiceType type) {
        return PrismaticDisplayHelper.getEffectDescription(type);
    }
}