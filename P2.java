import java.io.IOException;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class P2 {

    public static int windowSize = 0;

    public static Queue<Integer> buffer;

    public static LinkedList<Bucket> buckets = new LinkedList<>();

    public static AtomicInteger ticker = new AtomicInteger(0);

    public synchronized static void addBucket(int bit, int time) {
        if (bit == 1) {
            buckets.addFirst(new Bucket(1, time));
            mergeBuckets(buckets);
        }
    }

    public static synchronized LinkedList<Bucket> getBuckets() {
        return buckets;
    }

    private static void mergeBuckets(LinkedList<Bucket> buckets) {
        int currentSize = 1;
        int currentCount = 0;
        int currentIndex = 0;
        while (true) {
            for(; currentIndex < buckets.size(); currentIndex++) {
                if (buckets.get(currentIndex).size == currentSize) {
                    currentCount++;
                }
                if (buckets.get(currentIndex).size > currentSize) {
                    break;
                }
            }
            if (currentCount > 3) {
                buckets.get(currentIndex -1).size += buckets.get(currentIndex - 2).size;
                buckets.get(currentIndex -1).time = buckets.get(currentIndex - 2).time;
                buckets.remove(currentIndex - 2);
            } else {
                break;
            }
            currentIndex--;
            currentSize *= 2;
        }
    }

    public synchronized static Queue<Integer> getBuffer() {
        return buffer;
    }

    public synchronized static void pushBuffer(Integer bit) {
        if (buffer.size() == windowSize) {
            buffer.poll();
            buffer.offer(bit);
        } else {
            buffer.offer(bit);
        }
    }

    public synchronized static void printString(String content, boolean newLine) {
        if (newLine) {
            System.out.println(content);
        } else {
            System.out.print(content);
        }
    }

    static class Bucket {
        public int size;
        public int time;
        public Bucket(int size, int time) {
            this.size = size;
            this.time = time;
        }
    }

    static class DataReader implements Runnable {

        private final String address;
        private final int port;

        public DataReader(String address, int port) {
            this.address = address;
            this.port = port;
        }

        @Override
        public void run() {
            Socket socket;
            Scanner inputStreamScanner = null;
            int data;
            try {
                socket = new Socket(this.address, this.port);
                inputStreamScanner = new Scanner(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }


            /* Test only
            while (true) {
                printString("0", false);
                try {
                    sleep(8000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            */

            while (inputStreamScanner != null && inputStreamScanner.hasNext()) {
                try {
                    data = inputStreamScanner.nextInt();

                    pushBuffer(data);
                    addBucket(data, ticker.incrementAndGet());

                    printString(Integer.valueOf(data).toString(), false);

                } catch (InputMismatchException e) {
                    continue;
                }
            }
        }
    }


    static class QueryReader implements Runnable {

        private final String NUMBER_REGEX = "[^0-9]";
        private final Pattern NUMBER_PATTERN = Pattern.compile(NUMBER_REGEX);

        @Override
        public void run() {
            Scanner queryScanner = new Scanner(System.in);
            String query;
            int k = 0;
            while (queryScanner.hasNext()) {
                query = queryScanner.nextLine();
                if (query.isEmpty()) {
                    continue;
                }
                try {
                    k = Integer.valueOf(NUMBER_PATTERN.matcher(query).replaceAll("").trim());

                    // debug only.
                    // printString(NUMBER_PATTERN.matcher(query).replaceAll("").trim(), true);
                    new Thread(new DGIM(k)).start();

                } catch (NumberFormatException e) {
                    printString("Invalid query!", true);
                    continue;
                }
            }
        }
    }


    static class DGIM implements Runnable {
        private int k;

        public DGIM(int k) {
            this.k = k;
        }

        private Integer countOne(Queue<Integer> buffer, int k) {
            int count = 0;
            List<Integer> tempList = (List)buffer;
            for (int i = buffer.size()-k; i < buffer.size(); i++) {
                count += tempList.get(i);
            }
            return count;
        }

        @Override
        public void run() {
            if (this.k <= windowSize) {
                printString(String.format("The number of ones of last %d data is exact %d.", k, countOne(getBuffer(), k)), true);
            } else {
                estimate(getBuckets(), k);
            }
        }

        private int findTargetBucket(int target) {
            for (int i = 0; i < buckets.size(); i++) {
                if (buckets.get(i).time <= target) {
                    return i;
                }
            }
            return -1;
        }

        private void estimate(LinkedList<Bucket> buckets, int k) {
            int target = buckets.get(0).time - k + 1;
            int bucketIndex = findTargetBucket(target);
            int countOne = 0;
            if (bucketIndex == -1) {
                printString("error", true);
            }

            for (int i = 0; i < bucketIndex; i++) {
                countOne += buckets.get(i).size;
            }
            if (buckets.get(bucketIndex).time == target) {
                countOne++;
                printString(String.format("The number of ones of last %d data is exact %d.", k, countOne), true);
            } else {
                countOne += buckets.get(bucketIndex).size/2;
                printString(String.format("The number of ones of last %d data is estimated %d.", k, countOne), true);
            }
        }
    }

    public static void main(String[] args) {

        String hostAddress = args[0];
        int hostPort = 0;

        System.out.println("Window size: ");
        Scanner input = new Scanner(System.in);


        try {
            hostPort = Integer.valueOf(args[1]);
            windowSize = input.nextInt();

        } catch (InputMismatchException e) {
            System.out.println("Not a valid integer!");
            System.exit(0);
        }

        if (hostPort <= 0) {
            System.out.println("Port number is invalid!");
            System.exit(0);
        }

        buffer = new LinkedList();

        if (args.length < 2) {
            System.out.println("Please input a valid server's address and port!");
            System.exit(0);
        }


        Runnable dataReaderThread = new DataReader(hostAddress, hostPort);
        Runnable queryReaderThread = new QueryReader();
        new Thread(dataReaderThread).start();
        new Thread(queryReaderThread).start();
    }


}
