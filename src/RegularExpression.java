import java.util.ArrayList;
import java.util.HashMap;

public class RegularExpression {
	
	// Storage for expressions with only a character to prevent excessive memory use
	public static final RegularExpression EPSILON = new RegularExpression(null, null, OpType.None, '_');
	public static final RegularExpression WILDCARD = new RegularExpression(null, null, OpType.None, '$');
	public static HashMap<Character, RegularExpression> chars = new HashMap<>();
	
	static {
		chars.put('_', EPSILON);
		chars.put('$', WILDCARD);
	}

	public RegularExpression r1;
	public RegularExpression r2;
	OpType type;
	char term;
	
	protected enum OpType {
		Union,
		Concatenation,
		Star,
		None // Used for anything that isn't an operator
	}
	
	// Public constructor for manually building regular expression parse trees
	public RegularExpression(RegularExpression r1, RegularExpression r2, OpType type, char term) {
		this.r1 = r1;
		this.r2 = r2;
		this.type = type;
		this.term = term;
	}
	
	// Private constructor for building parse tree from string
	private RegularExpression(String exp, int expStart, int expEnd, ArrayList<Op> ops, int opsStart, int opsEnd, int prevLevel) {
		// Base case for terminals
		if (opsEnd - opsStart <= 0) {
			type = OpType.None;
			term = exp.charAt(expStart + (expEnd - expStart - 1)/2);
			return;
		}
		
		// Gets root operator for the expression tree
		int topIndex = opsStart;
		int middleIndex = (opsStart + opsEnd) / 2;
		Op rootOp = ops.get(topIndex);
		for (int i = opsStart + 1; i < opsEnd; i++) {
			Op temp = ops.get(i);
			if (temp.compareTo(rootOp) < 0) {
				topIndex = i;
				rootOp = temp;
			} else if (temp.compareTo(rootOp) == 0) {
				if (Math.abs(middleIndex - i) < Math.abs(middleIndex - topIndex)) {
					topIndex = i;
					rootOp = temp;
				}
			}
		}
		
		type = rootOp.type;
		int numParentheses = rootOp.level - prevLevel;

		// Calculate indexes to split the expression
		int r1ExpStart = expStart + numParentheses;
		int r1ExpEnd = rootOp.index;
		int r2ExpStart = rootOp.index + (rootOp.type == OpType.Union ? 1 : 0);
		int r2ExpEnd = expEnd - numParentheses;
		
		// Prints how the expression is split at each step
		if (Main.LOGGING) {
			System.out.printf("%s -> %s%s%n", exp.substring(expStart, expEnd), exp.substring(r1ExpStart, r1ExpEnd),
					(type == OpType.Star) ? "" : " | " + exp.subSequence(r2ExpStart, r2ExpEnd));			
		}
		
		
		// Recursively build tree
		r1 = new RegularExpression(exp, r1ExpStart, r1ExpEnd, ops, opsStart, topIndex, rootOp.level);
		r2 = (type == OpType.Star) ? EPSILON : new RegularExpression(exp, r2ExpStart, r2ExpEnd, ops, topIndex + 1, opsEnd, rootOp.level);
	}
	
	public boolean isExpandable() {
		return type != RegularExpression.OpType.None || term == '$';
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof RegularExpression)) return false;
		RegularExpression r = (RegularExpression) o;
		if (type != OpType.None || r.type != OpType.None) return toString().equals(r.toString());
		return term == r.term;
	}
	
	public int hashCode() {
		if (type == OpType.None) return term;
		return toString().hashCode();
	}

	public String toString() {
		return toString(OpType.Union);
	}
	
	private String toString(OpType parent) {
		// Format recursively
		boolean p;
		switch (type) {
			case Star:
				p = (r1.type != OpType.None);
				return String.format("%s%s%s*", p ? "(" : "", r1.toString(type), p ? ")" : "");
			case Concatenation:
				return String.format("%s%s", r1.toString(type), r2.toString(type));
			case Union:
				p = (parent != OpType.Concatenation);
				return String.format("%s%s + %s%s", p ? "" : "(", r1.toString(type), r2.toString(type), p ? "" : ")");
			case None:
				return Character.toString(term);
		}
		return null;
	}
	
	public static RegularExpression fromString(String exp) {
		String expNoWs = exp.replace(" ", "");
		ArrayList<Op> ops = getOperators(expNoWs);
		if (ops == null) return null;
		if (Main.LOGGING) System.out.printf("Operators: %s%n%n", ops);
		return new RegularExpression(expNoWs, 0, expNoWs.length(), ops, 0, ops.size(), 0);
	}
	
	public static RegularExpression fromCharacter(char ch) {
		RegularExpression out = chars.get(ch);
		
		if (out == null) {
			out = new RegularExpression(null, null, OpType.None, ch);
			chars.put(ch, out);
		}
		
		return out;
	}
	
	// Gets a list of operators based on an expression string
	private static ArrayList<Op> getOperators(String exp) {
		ArrayList<Op> ops = new ArrayList<>();
		int level = 0;
		OpType prevOp = OpType.None;
		
		for (int i = 0; i < exp.length(); i++) {
			switch (exp.charAt(i)) {
				case '(':
					if (prevOp == OpType.Concatenation || prevOp == OpType.Star) {
						ops.add(new Op(i, level, OpType.Concatenation));
					}
					
					level++;
					prevOp = OpType.None;
					break;
					
				case ')':
					if (prevOp == OpType.Union) {
						System.out.println("Invalid expression: '+)'");
						return null;
					} else if (i > 0 && prevOp == OpType.None) {
						System.out.println("Invalid expression: '()'");
						return null;
					}
					
					level--;
					if (level < 0) {
						System.out.println("Mismatched parentheses");
						return null;
					}
					
					prevOp = OpType.Concatenation;
					break;
					
				case '+':
					if (prevOp == OpType.Union) {
						System.out.println("Invalid expression: '++'");
						return null;
					} else if (prevOp == OpType.None) {
						System.out.println("Invalid expression: '(+'");
						return null;
					}
					
					ops.add(new Op(i, level, OpType.Union));
					prevOp = OpType.Union;
					break;
					
				case '*':
					if (prevOp == OpType.Star) {
						System.out.println("Redundant star operator: '**'");
						break;
					} else if (prevOp == OpType.None) {
						System.out.println("Invalid expression: '(*'");
						return null;
					} else if (prevOp == OpType.Union) {
						System.out.println("Invalid token: '+*'");
						return null;
					}
					
					ops.add(new Op(i, level, OpType.Star));
					prevOp = OpType.Star;
					break;
					
				default:
					char term = exp.charAt(i);
					if (!isTerminal(term)) {
						boolean alphabetPossible = Character.isAlphabetic(term) || Character.isDigit(term);
						System.out.printf("%s: %c%n", alphabetPossible ? "Symbol not in alphabet" : "Invalid symbol", term);
						return null;
					}
					
					if (prevOp == OpType.Concatenation || prevOp == OpType.Star) {
						ops.add(new Op(i, level, OpType.Concatenation));
					}
					
					prevOp = OpType.Concatenation;

			}
		}
		
		return ops;
	}
	
	private static boolean isTerminal(char c) {
		return Main.alphabet.contains(c) || c == '_' || c == '$';
	}
	
	// Private class used to represent operators in the expression string
	private static class Op implements Comparable<Op> {
		int index;
		int level;
		OpType type;
		
		Op(int index, int level, OpType type) {
			this.index = index;
			this.level = level;
			this.type = type;
		}
		
		public String toString() {
			return String.format("Op[Level=%d, Index=%d, Type=%s]", level, index, type.toString());
		}

		public int compareTo(Op o) {
			if (level != o.level) {
				return level - o.level;
			} else if (type != o.type) {
				return type.ordinal() - o.type.ordinal();
			}
			return 0;
		}
	}
	
}
