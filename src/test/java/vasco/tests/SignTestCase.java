package vasco.tests;

public class SignTestCase {
	static int P, Q, R;
	
	public static void main(String... args) {
		int p = five();
		int q = f(p, -3);
		int r = g(-q);
		int l = func(r);
		int m = func(p);
		P = p;
		Q = q;
		R = r;
	}
	
	public static int five() {
		return 5;
	}
	
	public static int f(int a, int b) {
		int c;
		if (a < b) {
			c = a * b;
		} else {
			c = g(10);
		}
		return c;
	}
	
	public static int g(int u) {
		int v = f(-u, u);
		return v;
	}

	public static int func(int val) {
		return 1;
	}
}
