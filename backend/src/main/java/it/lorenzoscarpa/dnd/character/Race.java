package it.lorenzoscarpa.dnd.character;

/**
 * SRD 5.1 base races (no subraces), with their base walking speed in feet.
 * Constant names match the API codes (see Race in api.yaml).
 */
public enum Race {
	DRAGONBORN(30),
	DWARF(25),
	ELF(30),
	GNOME(25),
	HALF_ELF(30),
	HALFLING(25),
	HALF_ORC(30),
	HUMAN(30),
	TIEFLING(30);

	private final int baseSpeed;

	Race(int baseSpeed) {
		this.baseSpeed = baseSpeed;
	}

	/** Default speed (ft) when a character is created without one. */
	public int baseSpeed() {
		return baseSpeed;
	}
}
