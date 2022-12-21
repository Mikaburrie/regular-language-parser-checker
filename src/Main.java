import java.util.HashSet;
import java.util.Scanner;

public class Main {
	
	public static final boolean LOGGING = false;
	public static String alphabetString = "ab";
	public static HashSet<Character> alphabet;
	public static NFA nfa;
 
	public static void main(String[] args) {
		boolean running = true;
		Scanner inputScanner = new Scanner(System.in);

		changeAlphabet(alphabetString);
		
		while (running) {
			printOptions();
			if (inputScanner.hasNextInt()) {
				int selection = inputScanner.nextInt();
				inputScanner.nextLine();
				switch (selection) {
					case 1:
						System.out.print("Type all characters in alphabet: ");
						changeAlphabet(inputScanner.nextLine().trim());
						break;
					case 2:
						System.out.print("Enter expression: ");
						changeExpression(inputScanner.nextLine().trim());
						break;
					case 3:
						System.out.print("Enter string: ");
						processString(inputScanner.nextLine().trim());
						break;
					case 4:
						System.out.println("   Spaces are ignored for all inputs");
						System.out.println("1. The alphabet can consist of alphanumeric characters. Changing the alphabet will clear the current expression");
						System.out.println("2. Specify a regular expression using (), + for union, * for star, _ for epsilon, and $ for sigma");
						System.out.println("   The expression parser will inform you of any invalid syntax in your expression string");
						System.out.println("3. Processes a string. The empty string is specified by _");
						System.out.println("4. Help displays this message");
						System.out.println("5. Exists the program");
						break;
					case 5:
						running = false;
						break;
					default:
						System.out.println("Invalid option");
				}
			} else {
				System.out.println("Invalid option");
				inputScanner.next();
			}
			
			System.out.println();
		}
		
		inputScanner.close();
		
	}
	
	public static void printOptions() {
		System.out.printf("Current alphabet: %s%n", alphabetString);
		System.out.printf("Current expression: %s%n", (nfa == null || nfa.exp == null) ? "none" : nfa.exp.toString());
		System.out.println("Enter a digit to choose an option:");
		System.out.println("1. Change alphabet");
		System.out.println("2. Change expression");
		System.out.println("3. Process string");
		System.out.println("4. Help");
		System.out.println("5. Exit");
		System.out.print("Enter selection: ");
	}
	
	public static void changeAlphabet(String alpha) {
		alphabetString = "";
		alphabet = new HashSet<>();
		nfa = null;
		alpha = alpha.replace(" ", "");
		for (int i = 0; i < alpha.length(); i++) {
			char c = alpha.charAt(i);
			if (Character.isAlphabetic(c) || Character.isDigit(c)) {
				if (!alphabet.contains(c)) {
					alphabet.add(alpha.charAt(i));
					alphabetString += c;
				}
			} else {
				System.out.printf("%c cannot be in alphabet and will be excluded%n", c);
			}
		}
	}
	
	public static void changeExpression(String exp) {
		RegularExpression r = RegularExpression.fromString(exp);
		if (r == null) return;
		
		if (Main.LOGGING) System.out.printf("Reconstructed expression: %s%n%n", r.toString());
		
		nfa = new NFA(r);
		
		if (Main.LOGGING) {
			System.out.printf("Two state NFA:%n%s%n", nfa.toString());
			System.out.printf("Expanding nodes%n");
		}
		
		nfa.expandNodes();
		
		if (Main.LOGGING) System.out.printf("%nShortening epsilons%n");
		
		nfa.shortenEpsilonTransitions();
		
		if (Main.LOGGING) System.out.println();
	}
	
	public static void processString(String input) {
		if (nfa == null) {
			System.out.println("Rejected (No expression)");
			return;
		}
		
		if (Main.LOGGING) System.out.printf("Test string: %s%n", input);
		System.out.printf("%s%s%n", Main.LOGGING ? input + ": " : "", nfa.processString(input.replace(" ", "")) ? "Accepted" : "Rejected");
	}

}
