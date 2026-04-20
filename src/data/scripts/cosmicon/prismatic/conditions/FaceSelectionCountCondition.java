package data.scripts.cosmicon.prismatic.conditions;

import data.scripts.Strings;
import data.scripts.cosmicon.prismatic.AvailabilityCondition;

public class FaceSelectionCountCondition implements AvailabilityCondition {
    
    private final int faceValue;
    private final int requiredCount;
    
    public FaceSelectionCountCondition(int faceValue, int requiredCount) {
        this.faceValue = faceValue;
        this.requiredCount = requiredCount;
    }
    
    public static FaceSelectionCountCondition faceSelected(int face, int count) {
        return new FaceSelectionCountCondition(face, count);
    }
    
    @Override
    public boolean isAvailable(ConditionContext context) {
        int selectedCount = context.getFaceSelectionCount(faceValue);
        return selectedCount >= requiredCount;
    }
    
    @Override
    public String getDescription() {
        return Strings.format("prismatic.condition.face_selection", faceValue, requiredCount);
    }
}