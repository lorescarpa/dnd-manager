package it.lorenzoscarpa.dnd.character;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CharacterRulesTest {

    @ParameterizedTest
    @CsvSource({
            // punteggio, modificatore atteso
            "1, -5",
            "2, -4",
            "3, -4",
            "4, -3",
            "5, -3",
            "6, -2",
            "7, -2",
            "8, -1",
            "9, -1",
            "10, 0",
            "11, 0",
            "12, 1",
            "13, 1",
            "14, 2",
            "15, 2",
            "16, 3",
            "17, 3",
            "18, 4",
            "19, 4",
            "20, 5"
    })
    void abilityModifier(int score, int expected) {
        assertEquals(expected, CharacterRules.abilityModifier(score));
    }

    // proficiencyBonus
    @ParameterizedTest
    @CsvSource({
            // livello, bonus competenza atteso
            "1, 2",
            "2, 2",
            "3, 2",
            "4, 2",
            "5, 3",
            "6, 3",
            "7, 3",
            "8, 3",
            "9, 4",
            "10, 4",
            "11, 4",
            "12, 4",
            "13, 5",
            "14, 5",
            "15, 5",
            "16, 5",
            "17, 6",
            "18, 6",
            "19, 6",
            "20, 6"
    })
    void proficiencyBonus(int level, int expected) {
        assertEquals(expected, CharacterRules.proficiencyBonus(level));
    }

    // skill: con e senza competenza, con modificatore negativo
    @ParameterizedTest
    @CsvSource({
            // modificatore, competenza, bonus atteso (proficiencyBonus=2)
            "-5, false, -5",
            "-5, true, -3",
            "-4, false, -4",
            "-4, true, -2",
            "-3, false, -3",
            "-3, true, -1",
            "-2, false, -2",
            "-2, true, 0",
            "-1, false, -1",
            "-1, true, 1",
            "0, false, 0",
            "0, true, 2",
            "1, false, 1",
            "1, true, 3",
            "2, false, 2",
            "2, true, 4",
            "3, false, 3",
            "3, true, 5",
            "4, false, 4",
            "4, true, 6",
            "5, false, 5",
            "5, true, 7"
    })
    void skill(int modifier, boolean proficient, int expected) {
        assertEquals(expected, CharacterRules.skill(modifier, proficient, 2));
    }

    // savingThrow
    @ParameterizedTest
    @CsvSource({
            // modificatore, competenza, bonusdi competenza, bonus atteso
            "-2, true, 3, 1",
            "-2, false, 3, -2",
            "0, true, 2, 2",
            "0, false, 2, 0",
            "1, true, 2, 3"
    })
    void savingThrow(int modifier, boolean proficient, int proficiencyBonus, int expected) {
        assertEquals(expected, CharacterRules.savingThrow(modifier, proficient, proficiencyBonus));
    }

    // initiative
    // Tutte le caratteristiche a 10 (modificatore 0), tranne quella indicata
    private static AbilityScores scoresWithDex(int dex) {
        return new AbilityScores(10, dex, 10, 10, 10, 10); // str, dex, con, int, wis, cha
    }

    @ParameterizedTest(name = "DES {0} -> iniziativa {1}")
    @CsvSource({
            "1, -5",
            "8, -1",
            "9, -1",
            "10, 0",
            "14, 2",
            "15, 2",
            "20, 5"
    })
    void initiativeEqualsDexterityModifier(int dex, int expected) {
        assertEquals(expected, CharacterRules.initiative(scoresWithDex(dex)));
    }

    @Test
    void initiativeIgnoresOtherAbilities() {
        // DES media, tutte le altre molto alte: se il calcolo usasse un'altra
        // caratteristica, il risultato cambierebbe
        var scores = new AbilityScores(20, 10, 20, 20, 20, 20);
        assertEquals(0, CharacterRules.initiative(scores));
    }

    // passivePerception
    private static AbilityScores scoresWithWis(int wis) {
        return new AbilityScores(10, 10, 10, 10, wis, 10); // str, dex, con, int, wis, cha
    }

    @ParameterizedTest(name = "SAG {0}, non competente -> {1}")
    @CsvSource({
            "1, 5",
            "8, 9",
            "10, 10",
            "13, 11",
            "20, 15"
    })
    void withoutProficiency(int wis, int expected) {
        assertEquals(expected,
                CharacterRules.passivePerception(scoresWithWis(wis), 1, Set.of()));
    }

    @ParameterizedTest(name = "SAG {0}, livello {1}, competente -> {2}")
    @CsvSource({
            "14, 1, 14", // 10 + 2 + 2
            "14, 5, 15", // 10 + 2 + 3
            "14, 17, 18", // 10 + 2 + 6
            "8, 1, 11" // 10 - 1 + 2: modificatore negativo con competenza
    })
    void withProficiency(int wis, int level, int expected) {
        assertEquals(expected,
                CharacterRules.passivePerception(scoresWithWis(wis), level, Set.of(SkillKey.PERCEPTION)));
    }

    @Test
    void levelDoesNotMatterWithoutProficiency() {
        var scores = scoresWithWis(14);
        assertEquals(12, CharacterRules.passivePerception(scores, 1, Set.of()));
        assertEquals(12, CharacterRules.passivePerception(scores, 20, Set.of()));
    }

    @Test
    void ignoresOtherAbilities() {
        var scores = new AbilityScores(20, 20, 20, 20, 10, 20);
        assertEquals(10, CharacterRules.passivePerception(scores, 1, Set.of()));
    }

    @Test
    void ignoresProficiencyInOtherWisdomSkills() {
        // Intuizione usa anch'essa SAG: la competenza lì non deve contare per la
        // Percezione
        assertEquals(12,
                CharacterRules.passivePerception(scoresWithWis(14), 5, Set.of(SkillKey.INSIGHT)));
    }
}
