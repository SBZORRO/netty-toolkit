package tcp.client;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public abstract class BatchQ implements Closeable {

  public static final Map<String, ArrayList<String>> BATCHQ = new ConcurrentHashMap<>();
  public static final Map<String, Thread> BATCHQ_CONSUMER = new ConcurrentHashMap<>();
  public static final ReentrantReadWriteLock BATCHQ_LOCK = new ReentrantReadWriteLock();
  public static final WriteLock BATCHQ_WRITE_LOCK = BATCHQ_LOCK.writeLock();

  public static final ScheduledExecutorService EXE = Executors
      .newSingleThreadScheduledExecutor();

  public static void enq(String host, ArrayList<String> li) {
    BATCHQ_WRITE_LOCK.lock();
    if (BATCHQ.containsKey(host)) {
      BATCHQ.get(host).addAll(li);
    } else {
      BATCHQ.put(host, li);
    }
    BATCHQ_WRITE_LOCK.unlock();
  }

  public static void consume(String host, Runnable r) {
    BATCHQ_CONSUMER.computeIfAbsent(host, (key) -> {
      Thread t = new Thread(r);
      t.start();
      return t;
    });
  }

  public static void end(String host) {
    BATCHQ_WRITE_LOCK.lock();
    if (BATCHQ.containsKey(host) && BATCHQ.get(host).size() > 0) {
      BATCHQ_WRITE_LOCK.unlock();
      return;
    }
    BATCHQ_CONSUMER.computeIfPresent(host, (key, val) -> {
      return null;
    });
    BATCHQ_WRITE_LOCK.unlock();
  }
}
