package edu.uob;

public enum ConditionResult{
    NO_CONDITION(0),
    TRUE(1),
    FALSE(2),
    INVALID(3);
    
    private int hierarchy;
    
    ConditionResult(int hierarchy){
        this.hierarchy = hierarchy;
    }
    
    public int getVal(){
        return hierarchy;
    }
}
