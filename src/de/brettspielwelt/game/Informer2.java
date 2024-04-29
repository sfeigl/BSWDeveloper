package de.brettspielwelt.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Informer2 {

	// -------------  paar  drilling vierling

    public static List<List<Integer>> findCombinations(List<Integer> hand, int groupSize) {
        List<List<Integer>> result = new ArrayList<>();
        Map<Integer, List<Integer>> valueToCardsMap = new HashMap<>();

        for (int i = 0; i < hand.size(); i++) {
            int card = hand.get(i);
            if (!valueToCardsMap.containsKey(card)) {
                valueToCardsMap.put(card, new ArrayList<>());
            }
            valueToCardsMap.get(card).add(i);
        }
        for (int value : valueToCardsMap.keySet()) {
            List<Integer> indices = valueToCardsMap.get(value);
            if (indices.size() >= groupSize) {
                findCombinations(hand, indices, groupSize, 0, new ArrayList<>(), result);
            }
        }

        return result;
    }

    private static void findCombinations(List<Integer> hand, List<Integer> indices, int groupSize,
                                          int startIndex, List<Integer> currentCombination, List<List<Integer>> result) {
        if (currentCombination.size() == groupSize) {
            result.add(new ArrayList<>(currentCombination)); // Gruppe gefunden, füge sie zum Ergebnis hinzu
            return;
        }

        for (int i = startIndex; i <= indices.size() - (groupSize - currentCombination.size()); i++) {
            int index = indices.get(i);
            currentCombination.add(hand.get(index));

            findCombinations(hand, indices, groupSize, i + 1, currentCombination, result);

            currentCombination.remove(currentCombination.size() - 1);
        }
    }
	/// ------------- 3 Karten bilden Summe von 7 ----------------
	
	public static List<List<Integer>> findCombinationsThatSumsToSeven(List<Integer> hand) {
        if (hand.size() < 3) {
            return null; // Nicht genügend Karten für eine Kombination
        }
        List<List<Integer>> result = new ArrayList<>();
        
        // Durchlaufe alle möglichen Kombinationen von drei Karten
        for (int i = 0; i < hand.size() - 2; i++) {
            for (int j = i + 1; j < hand.size() - 1; j++) {
                for (int k = j + 1; k < hand.size(); k++) {
                    int sum = hand.get(i) + hand.get(j) + hand.get(k);
                    if (sum == 7) {
                    	List<Integer> currentCombination = new ArrayList<>();
                        currentCombination.add(hand.get(i));
                        currentCombination.add(hand.get(j));
                        currentCombination.add(hand.get(k));
                        result.add(currentCombination);
                    	// Kombination gefunden, die zu 7 summiert
                        System.out.printf("Kombination gefunden: %d, %d, %d%n", hand.get(i), hand.get(j), hand.get(k));
                    }
                }
            }
        }
        // Keine passende Kombination gefunden
        return (result.size()==0?null:result);
    }

	// ----------------- Beliebig viele Karten summieren sich zu x addieren -------------
	
    // Methode zur Suche nach allen Kombinationen
    public static List<List<Integer>> findCombinationsThatSumTo(List<Integer> hand, int x) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> currentCombination = new ArrayList<>();
        int targetSum = x;

        findCombinations(hand, targetSum, 0, currentCombination, result);

        return (result.size()==0?null:result);
    }

    // Rekursive Hilfsmethode zur Durchsuchung aller Kombinationen
    private static void findCombinations(List<Integer> hand, int targetSum, int startIndex,
                                         List<Integer> currentCombination, List<List<Integer>> result) {
        if (targetSum == 0) {
            // Zielsumme erreicht, füge aktuelle Kombination zum Ergebnis hinzu
            result.add(new ArrayList<>(currentCombination));
            return;
        }

        for (int i = startIndex; i < hand.size(); i++) {
            int currentCard = hand.get(i);

            if (currentCard <= targetSum) {
                // Füge aktuelle Karte zur aktuellen Kombination hinzu
                currentCombination.add(currentCard);

                // Rekursiv nach weiteren Kombinationen suchen
                findCombinations(hand, targetSum - currentCard, i + 1, currentCombination, result);

                // Entferne die zuletzt hinzugefügte Karte, um andere Kombinationen zu versuchen
                currentCombination.remove(currentCombination.size() - 1);
            }
        }
    }

    
    // ------------- 2 Päärchen
    
    public static List<List<Integer>> findTwoPairCombinations(List<Integer> hand) {
        List<List<Integer>> result = new ArrayList<>();
        
        // Sortiere die Karten, um eine ordentliche Paarbildung zu erleichtern
        hand.sort(Integer::compareTo);

        // Durchlaufe alle Kombinationen von Paaren
        for (int i = 0; i < hand.size() - 3; i++) {
            for (int j = i + 1; j < hand.size() - 2; j++) {
                for (int k = j + 1; k < hand.size() - 1; k++) {
                    for (int l = k + 1; l < hand.size(); l++) {
                        // Überprüfe, ob die aktuelle Kombination zwei Paare bildet
                        if (hand.get(i).equals(hand.get(j)) && hand.get(k).equals(hand.get(l))) {
                            List<Integer> pair1 = Arrays.asList(hand.get(i), hand.get(j));
                            List<Integer> pair2 = Arrays.asList(hand.get(k), hand.get(l));
                            List<Integer> combination = new ArrayList<>();
                            combination.addAll(pair1);
                            combination.addAll(pair2);
                            result.add(combination);
                        }
                    }
                }
            }
        }

        return result;
    }
    

    // --------------------------   allen Straßen der vorgegebenen Länge
    public static List<List<Integer>> findAllStreets(List<Integer> hand, int length) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> currentCombination = new ArrayList<>();
        
        hand.sort(Integer::compareTo);

        findStreets(hand, length, 0, currentCombination, result);

        return result;
    }

    private static void findStreets(List<Integer> hand, int length, int startIndex,
                                     List<Integer> currentCombination, List<List<Integer>> result) {
        if (currentCombination.size() == length) {
            if (isStreet(currentCombination)) {
                result.add(new ArrayList<>(currentCombination)); // Straße gefunden, füge sie zum Ergebnis hinzu
            }
            return;
        }

        for (int i = startIndex; i < hand.size(); i++) {
            currentCombination.add(hand.get(i));
            findStreets(hand, length, i + 1, currentCombination, result);
            currentCombination.remove(currentCombination.size() - 1);
        }
    }

    private static boolean isStreet(List<Integer> cards) {
        if (cards.size() < 2) {
            return false; // Eine Straße benötigt mindestens zwei Karten
        }
        for (int i = 1; i < cards.size(); i++) {
            if (cards.get(i) != cards.get(i - 1) + 1) {
                return false; // Nicht aufeinanderfolgende Karten, keine Straße
            }
        }
        return true; // Alle Karten sind aufeinanderfolgend, Straße gefunden
    }
    
    // --------------- Test
    
    
    public static void main(String[] args) {
        // Beispielaufruf der Methode
        List<Integer> hand = new ArrayList<>();
        hand.add(2);
        hand.add(1);
        hand.add(2);
        hand.add(3);
        hand.add(2);
        hand.add(3);
        hand.add(2);

//        List<List<Integer>> combinations = findTwoPairCombinations(hand);
        //List<List<Integer>> combinations = findAllStreets(hand,4);
        //List<List<Integer>> combinations = findCombinationsThatSumsToSeven(hand);
        List<List<Integer>> combinations = findCombinations(hand,4);
               
        if (combinations.isEmpty()) {
            System.out.println("Keine Kombinationen gefunden, die aus genau zwei Paaren bestehen.");
        } else {
            System.out.println("Folgende Kombinationen mit genau zwei Paaren wurden gefunden:");
            for (List<Integer> combination : combinations) {
                System.out.println(combination);
            }
        }
    }
}
