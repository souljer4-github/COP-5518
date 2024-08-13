import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MTCollatz class runs a multi-threaded Collatz Conjecture stopping time calculation.
 */
public class MTCollatz {
    private static final int MAX_STOPPING_TIME = 1000; // Maximum stopping time to consider
    private static int[] histogram = new int[MAX_STOPPING_TIME + 1]; // Histogram to count stopping times
    private static int counter = 1; // Counter for the current number to process
    private static int N; // Upper limit of numbers to process
    private static boolean useLock = true; // Flag to determine whether to use locking
    private static ReentrantLock lock = new ReentrantLock(); // Lock for synchronizing counter access

    /**
     * Main method to execute the multi-threaded Collatz Conjecture calculation.
     *
     * @param args Command line arguments: <N> <T> [-nolock]
     */
    public static void main(String[] args) {
        // Validate input arguments
        if (args.length < 2) {
            System.err.println("Usage: java MTCollatz <N> <T> [-nolock]");
            System.exit(1);
        }

        N = Integer.parseInt(args[0]); // Parse the upper limit N
        int T = Integer.parseInt(args[1]); // Parse the number of threads T

        // Check for the -nolock flag
        if (args.length > 2 && args[2].equals("-nolock")) {
            useLock = false; // Disable locking if -nolock is provided
        }

        Thread[] threads = new Thread[T]; // Array to hold thread references
        Instant start = Instant.now(); // Record start time

        // Create and start threads
        for (int i = 0; i < T; i++) {
            threads[i] = new Thread(new CollatzWorker());
            threads[i].start();
        }

        // Wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Instant end = Instant.now(); // Record end time
        double elapsedTime = (end.toEpochMilli() - start.toEpochMilli()) / 1000.0; // Calculate elapsed time

        // Print the histogram to stdout
        printHistogram();

        // Print the timing information to stdout at the end
        System.out.printf("%d,%d,%.9f%n", N, T, elapsedTime);

        // Append the histogram to the CSV file
        appendHistogramToCSV("collatz_histogram.csv");
    }

    /**
     * CollatzWorker class implements Runnable to perform the Collatz calculation.
     */
    static class CollatzWorker implements Runnable {
        @Override
        public void run() {
            while (true) {
                int num;
                if (useLock) {
                    lock.lock(); // Acquire lock
                    try {
                        if (counter > N) return; // Exit if counter exceeds N
                        num = counter++; // Get the current counter value and increment
                    } finally {
                        lock.unlock(); // Release lock
                    }
                } else {
                    if (counter > N) return; // Exit if counter exceeds N
                    num = counter++; // Get the current counter value and increment without locking
                }

                int stoppingTime = calculateStoppingTime(num); // Calculate stopping time for the number
                synchronized (histogram) {
                    histogram[stoppingTime]++; // Update histogram with stopping time
                }
            }
        }
    }

    /**
     * Calculates the stopping time for the Collatz Conjecture.
     *
     * @param n The number for which to calculate the stopping time.
     * @return The stopping time for the given number.
     */
    private static int calculateStoppingTime(int n) {
        int steps = 0;
        long num = n; // Use long to prevent overflow for large numbers
        while (num != 1) {
            num = (num % 2 == 0) ? num / 2 : 3 * num + 1; // Apply Collatz transformation
            steps++;
            if (steps > MAX_STOPPING_TIME) {
                return MAX_STOPPING_TIME; // Cap at MAX_STOPPING_TIME
            }
        }
        return steps;
    }

    /**
     * Prints the histogram of stopping times to standard output.
     */
    private static void printHistogram() {
        for (int i = 1; i <= MAX_STOPPING_TIME; i++) {
            if (histogram[i] > 0) {
                System.out.println(i + " " + histogram[i]); // Print non-zero histogram entries
            }
        }
    }

    /**
     * Appends the histogram of stopping times to a CSV file.
     *
     * @param fileName The name of the CSV file.
     */
    private static void appendHistogramToCSV(String fileName) {
        try (FileWriter writer = new FileWriter(fileName, true)) {
            for (int i = 1; i <= MAX_STOPPING_TIME; i++) {
                if (histogram[i] > 0) {
                    writer.append(String.format("%d,%d\n", i, histogram[i]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
