package test.cap.framework;

import test.cap.client.A1;
import test.cap.client.Subtypes;

//
// Framework usage example.
// F, A, B, C are framework classes
// A1, B1, C1 are client classes
//
// This procedure encode the protocol of usage.
//
public class FrameworkUsage {
	public static void main(String[] args) {
		F f = new F();
		new A1(f);

		// two framework invocations
		f.x();
		f.y();

		System.out.println("test succeeded");

		Subtypes.test1();
		Subtypes.test2();
	}
}
