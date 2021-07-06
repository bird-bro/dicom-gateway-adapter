package org.bird.gateway;

import java.util.Timer;
import java.util.TimerTask;

import lombok.SneakyThrows;
import org.bird.gateway.retry.RetryService.TaskProcessor;
/**
 * @author bird
 * @date 2021-7-5 15:17
 **/
public class RetryService {

    private static RetryService retryService = null;
    private Timer timer = new Timer();
    private TimerTask timerTask = null;
    private TaskProcessor processor;
    private boolean Busy = false;


    private RetryService(TaskProcessor processor) {
        this.processor = processor;
    }

    public static final RetryService getInstance(TaskProcessor processor) {
        if(retryService == null) {
            retryService = new RetryService(processor);
        }
        return retryService;
    }

    public static final void Start() {
        if(retryService != null) {
            retryService.Execute();
        }
    }


    public static final boolean Running() {
        if(retryService != null) {
            return retryService.Busy();
        }
        return false;
    }


    public void Execute() {
        if(Busy()) return;
        timerTask = new TimerTask() {
            @SneakyThrows
            @Override
            public void run() {
                try {
                    Busy = true;
                    if (processor != null) {
                        // Subscribe to message
                        while (true) {
                            String filePath = RetryQueue.Pull();
                            if(filePath == null) {break;}
                            processor.process(filePath);
                        }
                    }
                    this.cancel();
                    timerTask = null;
                } finally {
                    Busy = false;
                }
            }
        };
        timer.schedule(timerTask, 100L);
    }

    public boolean Busy() {
        timer.purge();
        if(!Busy) {
            timerTask = null;
        }
        return Busy;
    }

    public interface TaskProcessor {
        void process(String filePath) throws Exception;
    }


}
