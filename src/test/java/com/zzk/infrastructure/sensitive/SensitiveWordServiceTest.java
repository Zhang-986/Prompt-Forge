package com.zzk.infrastructure.sensitive;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

public class SensitiveWordServiceTest {

    @Test
    public void testBuildDFAWithConflict() throws Exception {
        SensitiveWordService service = new SensitiveWordService();
        
        // Use reflection to access private sensitiveWords set
        java.lang.reflect.Field field = service.getClass().getDeclaredField("sensitiveWords");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> sensitiveWords = (Set<String>) field.get(service);
        
        // Add conflicting words
        // "is" ends at 's'. if "isEnd" was used (starts with 'i'), it might conflict with "issue" if implementation was wrong?
        // Actually the bug was "isEnd" string used with charAt(0) -> 'i'.
        // So if we have a word like "pic" and "p", and the end flag is 'i', it might be tricky?
        // The reported bug was a ClassCastException.
        // It happens when 'i' is regular char AND end flag.
        // Example: word "i" exists. Then 'i' -> {isEnd: true}.
        // Word "is" exists. Then 'i' -> map -> 's'.
        // Wait, "isEnd" is a string key in the map?
        // No, the code was: map.put(END_FLAG.charAt(0), true); where END_FLAG="isEnd". So key was 'i'.
        
        // Scenario triggering bug:
        // Word 1: "Xi"
        // 'X' -> map
        //        'i' -> { 'i'(endFlag): true }  <-- if "Xi" is end.
        // Word 2: "Xijinping"
        // 'X' -> map
        //        'i' -> map (Collision! 'i' was previously a Boolean or map with boolean?)
        
        // The collision specifically happens if we treat the value as Map but it is Boolean, or vice versa.
        // In the original code:
        // currentMap.put(END_FLAG.charAt(0), true); -> puts 'i' -> Boolean(true)
        
        // If we also have a word that continues with 'i', e.g. "ai" and "aid".
        // "ai": 'a' -> 'i' -> { 'i'(end): true }
        // "aid": 'a' -> 'i' (get 'i' from map) -> returns Map? No, returns the map for 'i'.
        
        // The issue is when the character itself IS 'i'.
        // Suppose words: "ab", "abc".
        // 'a' -> 'b' -> { 'i'(end): true } (if end flag is 'i')
        // 'a' -> 'b' -> 'c' ...
        // 'b' map has 'i'->true. And 'b' map has 'c'->newMap. strict collision? No.
        
        // Collision happens if the character in the word is SAME as end flag.
        // End flag = 'i'.
        // Word = "pi".
        // 'p' -> 'i' -> { 'i'(end): true }
        // Word = "pig".
        // 'p' -> 'i' -> we get the map for 'i'.
        // Inside that map, we look for 'g'.
        // Wait.
        // 'p' -> map1.
        // map1 has 'i' -> map2 (next char node).
        // map2 has 'i' -> true (end of "pi").
        
        // But what if the word is "i"?
        // 'i' -> map3.
        // map3 has 'i'(end) -> true.
        // Correct.
        
        // What if the word IS "i" (the character itself)?
        // Root map -> 'i' -> map_i.
        // map_i -> 'i'(end) -> true.
        
        // The Crash happened at:
        // currentMap = (Map<Character, Object>) obj;
        // checking sensitive word:
        // Object obj = currentMap.get(c);
        
        // It crashed at buildDFA too?
        // yes: SensitiveWordService.java:112 -> currentMap = (Map<Character, Object>) obj;
        
        // This means obj was NOT null, but was NOT a Map. It was a Boolean.
        // So currentMap.get(c) returned a Boolean.
        // When does currentMap.get(c) return a boolean?
        // Only if c == END_FLAG.
        // So the character in the word being built IS the END_FLAG ('i').
        // So if we have a word "...i...", and we process char 'i'.
        // We do currentMap.get('i').
        // If 'i' was previously put as an END FLAG for the *previous* character node? No.
        // The end flag is an entry IN the node of the last character.
        
        // Example: Word "Hi".
        // Root -> 'H' -> mapH.
        // mapH -> 'i' -> mapi.
        // mapi -> 'i'(end) -> true.
        
        // Example: Word "H".
        // Root -> 'H' -> mapH.
        // mapH -> 'i'(end) -> true.
        
        // Now add "Hi".
        // Processing 'H'. Get mapH.
        // Processing 'i'. Get mapH.get('i').
        // mapH has 'i' mapped to TRUE (from "H" ending).
        // Code sees obj (TRUE) is not null.
        // Code casts obj to Map. CLASSCASH!
        
        // So the conflict is: A word is a prefix of another word, AND the next character of the longer word is 'i'.
        // Case: "H" and "Hi".
        // 1. Add "H". Root -> 'H' -> { 'i': true }.
        // 2. Add "Hi".
        //    'H' -> get('H') -> mapH.
        //    'i' -> mapH.get('i') -> returns TRUE.
        //    (Map) TRUE -> Boom.
        
        // Our fix changes end key to \u0000.
        // 1. Add "H". Root -> 'H' -> { \u0000: true }.
        // 2. Add "Hi".
        //    'H' -> get('H') -> mapH.
        //    'i' -> mapH.get('i') -> returns null (unless 'i' is there).
        //    Create new map for 'i'.
        // Good.
        
        sensitiveWords.add("H");
        sensitiveWords.add("Hi");
        
        // Also check "pig" and "pi"
        sensitiveWords.add("pi");
        sensitiveWords.add("pig");
        
        // Rebuild DFA
        Method buildDFA = service.getClass().getDeclaredMethod("buildDFA");
        buildDFA.setAccessible(true);
        buildDFA.invoke(service);
    }
}
