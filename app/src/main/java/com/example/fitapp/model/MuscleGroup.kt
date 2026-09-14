package com.example.fitapp.model

enum class MuscleGroup(val displayName: String, val description: String) {
    CHEST("Chest", "Pectorals, upper and lower chest"),
    BACK("Back", "Lats, traps, rhomboids and erectors"),
    SHOULDERS("Shoulders", "Deltoids: anterior, lateral and rear"),
    BICEPS("Biceps", "Biceps brachii and brachialis"),
    TRICEPS("Triceps", "Triceps brachii long, lateral and medial heads"),
    FOREARMS("Forearms", "Flexors, extensors and grip strength"),
    LEGS("Legs", "Quadriceps and hamstrings"),
    ADDUCTORS("Adductors", "Inner thigh and groin"),
    CALVES("Calves", "Gastrocnemius and soleus"),
    GLUTES("Glutes", "Gluteus maximus, medius and minimus"),
    CORE("Core", "Rectus abdominis and transverse abdominis (abs)"),
    OBLIQUES("Obliques", "Internal and external obliques, side core");

    companion object {
        fun fromString(value: String): MuscleGroup? {
            return entries.firstOrNull {
                it.displayName.equals(value, ignoreCase = true) ||
                it.name.equals(value, ignoreCase = true)
            }
        }
    }
}
