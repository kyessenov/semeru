package edu.mit.csail.cap.util;

public abstract class Pattern {
	public abstract boolean accept(String x);

	public static Pattern create(String name) {
		if (name.equals("*"))
			return All;
		else if (name.equals(""))
			return None;
		else if (name.charAt(name.length() - 1) == '*')
			return new PrefixPattern(name.substring(0, name.length() - 1));
		else
			return new ExactPattern(name);
	}

	public static Pattern[] makePatterns(String patterns) {
		final String[] ps = patterns.split(",");
		final Pattern[] out = new Pattern[ps.length];
		for (int i = 0; i < ps.length; i++)
			out[i] = create(ps[i].trim());
		return out;
	}

	static Pattern All = new Pattern() {
		public boolean accept(String x) {
			return true;
		}

		public String toString() {
			return "*";
		}
	};

	static Pattern None = new Pattern() {
		public boolean accept(String x) {
			return false;
		}

		public String toString() {
			return "";
		}
	};

	static class ExactPattern extends Pattern {
		private final String s;

		ExactPattern(String s) {
			assert s.indexOf('*') == -1;
			this.s = s;
		}

		public boolean accept(String x) {
			return x.equals(s);
		}

		@Override
		public String toString() {
			return s;
		}
	}

	static class PrefixPattern extends Pattern {
		private final String s;

		PrefixPattern(String s) {
			assert s.indexOf('*') == -1;
			this.s = s;
		}

		public boolean accept(String x) {
			return x.startsWith(s);
		}

		@Override
		public String toString() {
			return s + '*';
		}
	}
}
