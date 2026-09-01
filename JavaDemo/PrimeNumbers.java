public class PrimeNumbers {
    public static void main(String[] args) {
        final int LIMIT = 100;

        // isComposite[i] == false means i is prime (default false avoids init loop)
        boolean[] isComposite = new boolean[LIMIT + 1];
        isComposite[0] = true;
        isComposite[1] = true;

        // Sieve: only need to check up to √LIMIT
        for (int p = 2; p * p <= LIMIT; p++) {
            if (!isComposite[p]) {
                // Mark multiples starting from p² (smaller multiples already marked)
                for (int multiple = p * p; multiple <= LIMIT; multiple += p) {
                    isComposite[multiple] = true;
                }
            }
        }

        // Print all primes
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i <= LIMIT; i++) {
            if (!isComposite[i]) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(i);
            }
        }
        System.out.println(sb.toString());
    }
}
