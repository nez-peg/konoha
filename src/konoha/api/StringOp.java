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
		if (x == null || y == null) {
			return x == y;
		}
		return x.equals(y);
	}

	@Method
	public static final boolean ne(String x, String y) {
		if (x == null || y == null) {
			return x != y;
		}
		return !x.equals(y);
	}

	@Method
	public final static boolean lt(String a, String b) {
		if (a != null) {
			return a.compareTo(b) < 0;
		}
		return b != null;
	}

	@Method
	public final static boolean gt(String a, String b) {
		if (a != null) {
			return a.compareTo(b) > 0;
		}
		return false;
	}

	@Method
	public final static boolean lte(String a, String b) {
		if (a != null) {
			return a.compareTo(b) <= 0;
		}
		return true;
	}

	@Method
	public final static boolean gte(String a, String b) {
		if (a != null) {
			return a.compareTo(b) >= 0;
		}
		return b == null;
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
