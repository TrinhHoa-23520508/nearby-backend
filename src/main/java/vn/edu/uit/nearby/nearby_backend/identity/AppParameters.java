package vn.edu.uit.nearby.nearby_backend.identity;

/**
 * Reads runtime system parameters ({@code app_parameters}). Modules wrap the keys they
 * own in a typed parameters class instead of passing key strings around. Every method
 * throws {@link IllegalStateException} when the key does not exist and
 * {@link NumberFormatException} when a numeric getter reads a non-numeric value.
 */
public interface AppParameters {

	String get(String key);

	default int getInt(String key) {
		return Integer.parseInt(get(key));
	}

	default double getDouble(String key) {
		return Double.parseDouble(get(key));
	}

}
