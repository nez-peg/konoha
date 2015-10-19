package konoha.api;

import konoha.Coercion;
import konoha.Conversion;
import konoha.Method;

public class BooleanOp {
	@Method
	public final static boolean not(boolean a) {
		return !a;
	}

	@Method
	public final static boolean eq(boolean a, boolean b) {
		return a == b;
	}

	@Method
	public final static boolean ne(boolean a, boolean b) {
		return a != b;
	}

	/* cast, conversion */

	@Coercion
	public final static Object to_Object(boolean a) {
		return a;
	}

	@Coercion
	public final static Boolean to_Boolean(boolean a) {
		return a;
	}

	@Conversion
	public final static String toString(boolean a) {
		return String.valueOf(a);
	}

	@Coercion
	public final static boolean to_boolean(Boolean a) {
		return a;
	}

}
