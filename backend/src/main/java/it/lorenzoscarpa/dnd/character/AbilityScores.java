package it.lorenzoscarpa.dnd.character;

/**
 * The six ability scores of a character (each 1–20; the range is validated at the API boundary
 * and by the database, not here).
 */
public record AbilityScores(
		int strength,
		int dexterity,
		int constitution,
		int intelligence,
		int wisdom,
		int charisma) {

	public int score(AbilityKey ability) {
		return switch (ability) {
			case STR -> strength;
			case DEX -> dexterity;
			case CON -> constitution;
			case INT -> intelligence;
			case WIS -> wisdom;
			case CHA -> charisma;
		};
	}
}
