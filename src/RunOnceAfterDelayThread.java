/*
 * Copyright 2022 Daniel Allen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class RunOnceAfterDelayThread extends Thread{
    Runnable r;
    volatile boolean started = false;
    volatile boolean running = false;
    long buffer;
    volatile long timer;
    //todo rewrite this entire class because it's poorly written
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
