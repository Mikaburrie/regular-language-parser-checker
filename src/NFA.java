import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

public class NFA {

	ArrayList<Node> nodes;
	RegularExpression exp;
	
	public NFA(RegularExpression exp) {
		nodes = new ArrayList<>();
		this.exp = exp;
		
		Node endNode = new Node(new HashMap<>());
		endNode.expanded = true;
		endNode.epsilonShortened = true;
		
		HashSet<Integer> endNodePath = new HashSet<>();
		endNodePath.add(endNode.id);
		HashMap<RegularExpression, HashSet<Integer>> delta = new HashMap<>();
		delta.put(exp, endNodePath);
		new Node(delta);
	}
	
	// Performs expansion on the NFA until all transitions are terminals
	public void expandNodes() {
		boolean expansionDone = true;
		
		// Iterate over all nodes and determine if expansion is possible
		for (int i = 0; i < nodes.size(); i++) {
			Node node = nodes.get(i);
			if (node.expanded) {
				if (Main.LOGGING) System.out.println(node.toString());
				continue;
			}
			
			HashMap<RegularExpression, HashSet<Integer>> newDelta = new HashMap<>();
			node.expanded = true;
			
			// Loop over all transitions in a state and expand if possible
			node.delta.forEach((exp, nodes) -> {
				switch (exp.type) {
					case Union:
						// create transition to nodes on r1 / r2
						putCombined(newDelta, exp.r1, nodes);
						putCombined(newDelta, exp.r2, nodes);
						
						if (exp.r1.isExpandable() || exp.r2.isExpandable()) {
							node.expanded = false;
						}
						break;
					case Star:
						// create epsilon transition to destination
						HashMap<RegularExpression, HashSet<Integer>> middleDelta = new HashMap<>();
						putCombined(middleDelta, RegularExpression.EPSILON, nodes);
						
						// create middle node with the transition to destination
						Node middleNode = new Node(middleDelta);
						
						// add loop from middle node to middle node
						HashSet<Integer> middleNodeLoop = new HashSet<>();
						middleNodeLoop.add(middleNode.id);
						putCombined(middleDelta, exp.r1, middleNodeLoop);
						
						// create epsilon transition from origin to middle node
						HashSet<Integer> middleNodePath = new HashSet<>();
						middleNodePath.add(middleNode.id);
						putCombined(newDelta, RegularExpression.EPSILON, middleNodePath);
						
						if (exp.r1.isExpandable()) {
							node.expanded = false;
						}
						break;
					case Concatenation:
						// create r2 transition to destination
						HashMap<RegularExpression, HashSet<Integer>> middleDelta2 = new HashMap<>();
						putCombined(middleDelta2, exp.r2, nodes);
						
						// create middle node with transition to destination
						Node middleNode2 = new Node(middleDelta2);
						
						// create r1 transition from origin to middle node
						HashSet<Integer> middleNodePath2 = new HashSet<>();
						middleNodePath2.add(middleNode2.id);
						putCombined(newDelta, exp.r1, middleNodePath2);
						
						if (exp.r1.isExpandable() || exp.r2.isExpandable()) {
							node.expanded = false;
						}
						break;
					case None:
						// replace $ transition with all characters in alphabet
						if (exp.term == '$') {
							Main.alphabet.forEach(c -> {
								putCombined(newDelta, RegularExpression.fromCharacter(c), nodes);
							});
							break;
						}
						
						putCombined(newDelta, exp, nodes);
						break;
				}
			});
			
			node.delta = newDelta;
			
			expansionDone = expansionDone && node.expanded;
			
			if (Main.LOGGING) System.out.println(node.toString());
		}
		
		// Continue expanding until all nodes have terminal transitions
		if (expansionDone) return;
		if (Main.LOGGING) System.out.println();		
		expandNodes();
	}
	
	// Removes epsilon transitions from NFA
	public void shortenEpsilonTransitions() {
		boolean shorteningDone = true;
		
		// Iterate over all nodes
		for (int i = 0; i < nodes.size(); i++) {
			Node node = nodes.get(i);
			if (node.epsilonShortened) {
				if (Main.LOGGING) System.out.println(node.toString());
				continue;
			}
			
			node.epsilonShortened = true;
			
			// For all transitions from a node, determine if the destinations contain an epsilon transition
			node.delta.forEach((exp, paths) -> {
				HashSet<Integer> epsilonPaths = new HashSet<>();
				
				paths.forEach((nodeId) -> {
					Node nextNode = nodes.get(nodeId);
					if (nextNode.delta != null && nextNode.delta.containsKey(RegularExpression.EPSILON)) {
						epsilonPaths.addAll(nextNode.delta.get(RegularExpression.EPSILON));
					}
				});
				
				// If no new paths are found, the node is fully shortened
				if (!paths.containsAll(epsilonPaths)) {					
					paths.addAll(epsilonPaths);
					node.epsilonShortened = false;
				}
				
			});
			
			shorteningDone = shorteningDone && node.epsilonShortened;
			
			if (Main.LOGGING) System.out.println(node.toString());
		}
		
		// Remove all epsilon transitions once shortening is finished
		if (shorteningDone) {
			if (Main.LOGGING) {
				System.out.println("\n" + nodes.get(0));
				System.out.println(nodes.get(1));
			}
			for (int i = 2; i < nodes.size(); i++) {
				Node node = nodes.get(i);
				node.delta.remove(RegularExpression.EPSILON);
				if (Main.LOGGING) System.out.println(node);
			}
			
			return;
		}
		
		if (Main.LOGGING) System.out.println();
		shortenEpsilonTransitions();
	}
	
	// Checks if string is accepted or rejected
	public boolean processString(String in) {
		// Create set of potential states
		HashSet<Integer> state = new HashSet<>();
		state.add(1);
		
		// Add start's epsilon transitions
		HashMap<RegularExpression, HashSet<Integer>> start = nodes.get(1).delta;
		if (start.containsKey(RegularExpression.EPSILON)) {
			state.addAll(start.get(RegularExpression.EPSILON));
		}
		
		if (Main.LOGGING) System.out.printf("Start: %s%n", state);
		
		// Iterate over string and calculate next set of possible states
		for (int i = 0; i < in.length(); i++) {
			RegularExpression current = RegularExpression.fromCharacter(in.charAt(i));
			HashSet<Integer> nextState = new HashSet<>();
			
			state.forEach(nodeId -> {
				HashMap<RegularExpression, HashSet<Integer>> node = nodes.get(nodeId).delta;
				
				if (node != null && node.containsKey(current)) {
					nextState.addAll(node.get(current));
				}
			});
			
			state = nextState;
			
			if (Main.LOGGING) System.out.printf("%c: %s%n", current.term, state);
		}
		
		// Accept if final set of states contains 0 (the accept state)
		return state.contains(0);
	}
	
	public String toString() {
		String output = "";
		for (int i = 0; i < nodes.size(); i++) {
			output += nodes.get(i).toString() + "\n";
		}
		return output;
	}
	
	// Combines a set stored in a hash map with a new set
	private void putCombined(HashMap<RegularExpression, HashSet<Integer>> delta, RegularExpression exp, HashSet<Integer> nodes) {
		HashSet<Integer> old = delta.put(exp, nodes);
		if (old != null) {
			nodes.addAll(old);
		}
	}
	
	// Used to represent a state along with its transitions in the NFA
	private class Node {
		
		HashMap<RegularExpression, HashSet<Integer>> delta;
		int id;
		boolean expanded = false;
		boolean epsilonShortened = false;
		
		public Node(HashMap<RegularExpression, HashSet<Integer>> delta) {
			this.delta = delta;
			id = nodes.size();
			nodes.add(this);
		}
		
		public String toString() {
			return String.format("%d %s%s%s", id, (delta == null ? "{}" : delta.toString()), id == 0 ? " -> accept" : "", id == 1 ? " -> start" : "");
		}
		
	}
	
}
