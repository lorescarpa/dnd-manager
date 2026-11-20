package it.lorenzoscarpa.dnd.character;

/**
 * The 18 SRD 5.1 skills, each with its fixed ability (reference data, not editable).
 * Constant names match the API codes (see SkillKey in api.yaml).
 */
public enum SkillKey {
	ACROBATICS(AbilityKey.DEX),
	ANIMAL_HANDLING(AbilityKey.WIS),
	ARCANA(AbilityKey.INT),
	ATHLETICS(AbilityKey.STR),
	DECEPTION(AbilityKey.CHA),
	HISTORY(AbilityKey.INT),
	INSIGHT(AbilityKey.WIS),
	INTIMIDATION(AbilityKey.CHA),
	INVESTIGATION(AbilityKey.INT),
	MEDICINE(AbilityKey.WIS),
	NATURE(AbilityKey.INT),
	PERCEPTION(AbilityKey.WIS),
	PERFORMANCE(AbilityKey.CHA),
	PERSUASION(AbilityKey.CHA),
	RELIGION(AbilityKey.INT),
	SLEIGHT_OF_HAND(AbilityKey.DEX),
	STEALTH(AbilityKey.DEX),
	SURVIVAL(AbilityKey.WIS);

	private final AbilityKey ability;

	SkillKey(AbilityKey ability) {
		this.ability = ability;
	}

	public AbilityKey ability() {
		return ability;
	}
}
