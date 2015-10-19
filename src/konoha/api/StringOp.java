package konoha.api;

import konoha.Conversion;
import konoha.Method;

public class StringOp {

	@Method
	public static final int size(String x) {
		return x.length();
	}

	@Method
	public static final String get(String x, int n) {
		return String.valueOf(x.charAt(n));
	}

	@Method
	public static final String add(String x, Object y) {
		return x + y;
	}

	@Method
	public static final int compareTo(String x, String y) {
		return x.compareTo(y);
	}

	@Method
	public static final boolean eq(String x, String y) {
		return x.equals(y);
	}

	@Method
	public static final boolean ne(String x, String y) {
		return !x.equals(y);
	}

	@Method
	public final static boolean lt(String a, String b) {
		return a.compareTo(b) < 0;
	}

	@Method
	public final static boolean gt(String a, String b) {
		return a.compareTo(b) > 0;
	}

	@Method
	public final static boolean lte(String a, String b) {
		return a.compareTo(b) <= 0;
	}

	@Method
	public final static boolean gte(String a, String b) {
		return a.compareTo(b) >= 0;
	}

	@Conversion
	public final static int toint(String x) {
		try {
			return Integer.parseInt(x);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

}
