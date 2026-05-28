import java.io.InputStream;

interface InputReader {
    String read();
}

class ConsoleInputReader implements InputReader {
    private java.util.Scanner scanner;
    private InputStream lastIn;

    @Override
    public String read() {
        if (scanner == null || System.in != lastIn) {
            lastIn = System.in;
            scanner = new java.util.Scanner(lastIn);
        }

        if (!scanner.hasNext()) {
            return null;
        }
        return scanner.next();
    }
}