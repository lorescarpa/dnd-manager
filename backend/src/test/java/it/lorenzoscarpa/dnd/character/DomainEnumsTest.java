package it.lorenzoscarpa.dnd.character;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The mapper converts between domain and generated API enums by name, so both must define the
 * same constants. This test fails when api.yaml and the domain enums drift apart.
 */
class DomainEnumsTest {

	static Stream<Arguments> enumPairs() {
		return Stream.of(
				Arguments.of(Race.class, it.lorenzoscarpa.dnd.api.model.Race.class),
				Arguments.of(CharacterClass.class, it.lorenzoscarpa.dnd.api.model.CharacterClass.class),
				Arguments.of(Alignment.class, it.lorenzoscarpa.dnd.api.model.Alignment.class),
				Arguments.of(AbilityKey.class, it.lorenzoscarpa.dnd.api.model.AbilityKey.class),
				Arguments.of(SkillKey.class, it.lorenzoscarpa.dnd.api.model.SkillKey.class));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("enumPairs")
	void domainEnumMatchesApiEnum(Class<? extends Enum<?>> domain, Class<? extends Enum<?>> api) {
		assertEquals(names(api), names(domain));
	}

	@Test
	void baseSpeedIs25ForSmallRacesAnd30ForOthers() {
		List<Race> slow = List.of(Race.DWARF, Race.GNOME, Race.HALFLING);
		for (Race race : Race.values()) {
			assertEquals(slow.contains(race) ? 25 : 30, race.baseSpeed(), race.name());
		}
	}

	private static List<String> names(Class<? extends Enum<?>> type) {
		return Arrays.stream(type.getEnumConstants()).map(Enum::name).sorted().toList();
	}
}
