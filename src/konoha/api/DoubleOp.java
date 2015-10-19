package konoha.api;

import konoha.Coercion;
import konoha.Conversion;
import konoha.Method;

public class DoubleOp {
	/* double */

	/* method names come from BigInteger */

	@Method
	public final static double negate(double a) {
		return -a;
	}

	@Method
	public final static double add(double a, double b) {
		return a + b;
	}

	@Method
	public final static double subtract(double a, double b) {
		return a - b;
	}

	@Method
	public final static double multiply(double a, double b) {
		return a * b;
	}

	@Method
	public final static double divide(double a, double b) {
		return a / b;
	}

	@Method
	public final static double mod(double a, double b) {
		return a % b;
	}

	@Method
	public final static int compareTo(double a, double b) {
		return Double.compare(a, b);
	}

	@Method
	public final static boolean eq(double a, double b) {
		return a == b;
	}

	@Method
	public final static boolean ne(double a, double b) {
		return a != b;
	}

	@Method
	public final static boolean lt(double a, double b) {
		return a < b;
	}

	@Method
	public final static boolean gt(double a, double b) {
		return a > b;
	}

	@Method
	public final static boolean lte(double a, double b) {
		return a <= b;
	}

	@Method
	public final static boolean gte(double a, double b) {
		return a >= b;
	}

	/* cast */

	@Coercion
	public final static byte to_byte(double a) {
		return (byte) a;
	}

	@Coercion
	public final static char to_char(double a) {
		return (char) a;
	}

	@Coercion
	public final static short to_short(double a) {
		return (short) a;
	}

	@Coercion
	public final static int to_int(double a) {
		return (int) a;
	}

	@Coercion
	public final static long to_long(double a) {
		return (long) a;
	}

	@Coercion
	public final static float to_float(double a) {
		return (float) a;
	}

	@Coercion
	public final static double to_double(double a) {
		return a;
	}

	/* object */

	@Coercion
	public final static Object to_Object(double a) {
		return a;
	}

	@Coercion
	public final static Number to_Number(double a) {
		return a;
	}

	@Coercion
	public final static Double to_Double(double a) {
		return a;
	}

	@Conversion
	public final static String toString(double a) {
		return String.valueOf(a);
	}

	@Coercion
	public final static double to_boolean(Double a) {
		return a;
	}

}
