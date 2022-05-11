package mstc.cloud.worker;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeoutException;

/**
 * @author dreedy
 */
public class Util {
    public static void deleteDir(File dir) throws IOException {
        Files.walk(dir.toPath())
             .sorted(Comparator.reverseOrder())
             .map(Path::toFile)
             .forEach(File::delete);
    }

    public interface Check {
        boolean check();
    }

    public static class MinIOCheck implements Check {
        @Override
        public boolean check() {
            try {
                URL url = new URL("http://localhost:9001");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.getResponseCode();
                return true;
            } catch (Exception e) {
            }
            return false;
        }
    }

    public static void check(Check checker) throws Exception {
        int retry = 0;
        boolean online = false;
        long t0 = System.currentTimeMillis();
        while (!online) {
            online = checker.check();
            if(!online) {
                retry++;
                if (retry == 10) {
                    throw new TimeoutException(checker.getClass().getSimpleName() + "failed");
                }
                Thread.sleep(1000);
            } else {
                System.out.println("Waited " + (System.currentTimeMillis() - t0) + " millis");
            }
        }
    }
}
