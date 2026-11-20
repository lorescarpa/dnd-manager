package it.lorenzoscarpa.dnd.character;

import java.util.Set;

/**
 * D&D 5e (SRD 5.1) formulas for the computed fields of a character sheet (see docs/domain.md).
 * Pure functions: no Spring, no persistence, no state.
 */
public final class CharacterRules {

	private CharacterRules() {
	}

	/**
	 * ⌊(score − 10) / 2⌋. {@code Math.floorDiv} rounds down; plain {@code /} would round toward
	 * zero and give −4 instead of −5 for a score of 1.
	 */
	public static int abilityModifier(int score) {
		return Math.floorDiv(score - 10, 2);
	}

	/** 2 + ⌊(level − 1) / 4⌋. level ≥ 1, so plain integer division already rounds down. */
	public static int proficiencyBonus(int level) {
		return 2 + (level - 1) / 4;
	}

	/** modifier of the skill's ability + (proficient ? proficiencyBonus : 0). */
	public static int skill(int modifier, boolean proficient, int proficiencyBonus) {
		return modifier + (proficient ? proficiencyBonus : 0);
	}

	/** modifier of the ability + (proficient ? proficiencyBonus : 0). */
	public static int savingThrow(int modifier, boolean proficient, int proficiencyBonus) {
		return modifier + (proficient ? proficiencyBonus : 0);
	}

	/** Dexterity modifier. */
	public static int initiative(AbilityScores scores) {
		return abilityModifier(scores.dexterity());
	}

	/** 10 + skill(PERCEPTION). */
	public static int passivePerception(AbilityScores scores, int level, Set<SkillKey> skillProficiencies) {
		SkillKey perception = SkillKey.PERCEPTION;
		return 10 + skill(
				abilityModifier(scores.score(perception.ability())),
				skillProficiencies.contains(perception),
				proficiencyBonus(level));
	}
}
