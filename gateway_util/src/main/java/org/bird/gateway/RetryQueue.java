package org.bird.gateway;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author bird
 * @date 2021-7-5 15:19
 **/
@Slf4j
public class RetryQueue {

    private static final BlockingQueue<String> queue = new ArrayBlockingQueue<>(1024);

    public static final void Put(String arg) throws InterruptedException {
        log.info("RetryQueue.Put ==>" + arg);
        queue.put(arg);
    }

    public static final boolean Offer(String arg) {
        return queue.offer(arg);
    }

    public static final String Take() throws InterruptedException {
        return queue.take();
    }

    public static final String Pull() {
        return queue.poll();
    }

}
