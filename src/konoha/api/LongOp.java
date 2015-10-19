package konoha.api;

import konoha.Coercion;
import konoha.Conversion;
import konoha.Method;

public class LongOp {

	/* method names come from BigInteger */

	@Method
	public final static long negate(long a) {
		return -a;
	}

	@Method
	public final static long add(long a, long b) {
		return a + b;
	}

	@Method
	public final static long subtract(long a, long b) {
		return a - b;
	}

	@Method
	public final static long multiply(long a, long b) {
		return a * b;
	}

	@Method
	public final static long divide(long a, long b) {
		return a / b;
	}

	@Method
	public final static long mod(long a, long b) {
		return a % b;
	}

	@Method
	public final static int compareTo(long a, long b) {
		return Long.compare(a, b);
	}

	@Method
	public final static boolean eq(long a, long b) {
		return a == b;
	}

	@Method
	public final static boolean ne(long a, long b) {
		return a != b;
	}

	@Method
	public final static boolean lt(long a, long b) {
		return a < b;
	}

	@Method
	public final static boolean gt(long a, long b) {
		return a > b;
	}

	@Method
	public final static boolean lte(long a, long b) {
		return a <= b;
	}

	@Method
	public final static boolean gte(long a, long b) {
		return a >= b;
	}

	@Method
	public final static long shiftLeft(long a, long b) {
		return a << b;
	}

	@Method
	public final static long shiftRight(long a, long b) {
		return a >> b;
	}

	// public final static long opLogicalRightShift(long a, long b) {
	// return a >>> b;
	// }

	@Method
	public final static long and(long a, long b) {
		return a & b;
	}

	@Method
	public final static long or(long a, long b) {
		return a | b;
	}

	@Method
	public final static long xor(long a, long b) {
		return a ^ b;
	}

	@Method
	public final static long not(long a) {
		return ~a;
	}

	/* cast */

	@Coercion
	public final static byte to_byte(long a) {
		return (byte) a;
	}

	@Coercion
	public final static char to_char(long a) {
		return (char) a;
	}

	@Coercion
	public final static short to_short(long a) {
		return (short) a;
	}

	@Coercion
	public final static long to_int(long a) {
		return (int) a;
	}

	@Coercion
	public final static long to_long(long a) {
		return a;
	}

	@Coercion
	public final static float to_float(long a) {
		return a;
	}

	@Coercion
	public final static double to_double(long a) {
		return a;
	}

	/* object */

	@Coercion
	public final static Object to_Object(long a) {
		return a;
	}

	@Coercion
	public final static Number to_Number(long a) {
		return a;
	}

	@Coercion
	public final static Long to_Long(long a) {
		return a;
	}

	@Conversion
	public final static String toString(long a) {
		return String.valueOf(a);
	}

	@Coercion
	public final static long to_boolean(Long a) {
		return a;
	}
}
