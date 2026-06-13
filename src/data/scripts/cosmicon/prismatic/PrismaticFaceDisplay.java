package data.scripts.cosmicon.prismatic;



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

    public static String formatFaceMappingCompact(PrismaticDiceType type, boolean useTrueVersion) {
        if (type == null) return Strings.get("prismatic.face.no_dice");

        int[] faces = type.getFaces(useTrueVersion);
        String separator = Strings.get("prismatic.face.separator");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < faces.length && i < FACE_LETTERS.length; i++) {
            if (i > 0) sb.append(separator);
            sb.append(formatSingleFace(i, faces[i], type.isSpecialFace(i, useTrueVersion)));
        }

        return sb.toString();
    }

    public static String getEffectDescription(PrismaticDiceType type) {
        return PrismaticDisplayHelper.getEffectDescription(type);
    }
}