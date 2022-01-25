public class RunOnceAfterDelayThread extends Thread{
    Runnable r;
    volatile boolean started = false;
    volatile boolean running = false;
    long buffer;
    volatile long timer;
    //todo figure out wtf I'm doing with this class because it could be made so much better
    /**
     * Constructor
     * @param buffer
     * @param r
     */
    public RunOnceAfterDelayThread(long buffer, Runnable r){
        this.buffer = buffer;
        this.r = r;
    }

    /**
     * Trigger this thread to run after its delay if it has not already been triggered, or reset the delay if it has.
     */
    public void trigger(){
        timer = System.currentTimeMillis();
        if(!started) {
            start();
            started = true;
        } else {
            run();
        }
    }

    @Override
    public void run() {
        if(running)
            return;
        running = true;
        long curTime = System.currentTimeMillis();
        while(curTime - timer < buffer){
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            curTime = System.currentTimeMillis();
        }
        if (r != null) {
            r.run();
        }
        running = false;
    }
}
