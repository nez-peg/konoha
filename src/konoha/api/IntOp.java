package konoha.api;

import konoha.Coercion;
import konoha.Conversion;
import konoha.Operator;

public class IntOp {

	/* int */

	/* method names come from BigInteger */

	@Operator
	public final static int negate(int a) {
		return -a;
	}

	@Operator
	public final static int add(int a, int b) {
		return a + b;
	}

	@Operator
	public final static int subtract(int a, int b) {
		return a - b;
	}

	@Operator
	public final static int multiply(int a, int b) {
		return a * b;
	}

	@Operator
	public final static int divide(int a, int b) {
		return a / b;
	}

	@Operator
	public final static int mod(int a, int b) {
		return a % b;
	}

	@Operator
	public final static int compareTo(int a, int b) {
		return Integer.compare(a, b);
	}

	@Operator
	public final static boolean eq(int a, int b) {
		return a == b;
	}

	@Operator
	public final static boolean ne(int a, int b) {
		return a != b;
	}

	@Operator
	public final static boolean lt(int a, int b) {
		return a < b;
	}

	@Operator
	public final static boolean gt(int a, int b) {
		return a > b;
	}

	@Operator
	public final static boolean lte(int a, int b) {
		return a <= b;
	}

	@Operator
	public final static boolean gte(int a, int b) {
		return a >= b;
	}

	@Operator
	public final static int shiftLeft(int a, int b) {
		return a << b;
	}

	@Operator
	public final static int shiftRight(int a, int b) {
		return a >> b;
	}

	// public final static int opLogicalRightShift(int a, int b) {
	// return a >>> b;
	// }

	@Operator
	public final static int and(int a, int b) {
		return a & b;
	}

	@Operator
	public final static int or(int a, int b) {
		return a | b;
	}

	@Operator
	public final static int xor(int a, int b) {
		return a ^ b;
	}

	@Operator
	public final static int not(int a) {
		return ~a;
	}

	/* cast */

	@Coercion
	public final static byte to_byte(int a) {
		return (byte) a;
	}

	@Coercion
	public final static char to_char(int a) {
		return (char) a;
	}

	@Coercion
	public final static short to_short(int a) {
		return (short) a;
	}

	@Coercion
	public final static int to_int(int a) {
		return a;
	}

	@Coercion
	public final static long to_long(int a) {
		return a;
	}

	@Coercion
	public final static float to_float(int a) {
		return a;
	}

	@Coercion
	public final static double to_double(int a) {
		return a;
	}

	/* object */

	@Coercion
	public final static Object to_Object(int a) {
		return a;
	}

	@Coercion
	public final static Number to_Number(int a) {
		return a;
	}

	@Coercion
	public final static Integer to_Integer(int a) {
		return a;
	}

	@Conversion
	public final static String toString(int a) {
		return String.valueOf(a);
	}

	@Coercion
	public final static int to_int(Integer a) {
		return a;
	}

}
