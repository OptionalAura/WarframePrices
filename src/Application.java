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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Application {
    private final DelayedThreadQueue allItems;
    private final DelayedThreadQueue searchedItems;
    private RunOnceAfterDelayThread loadSearchedItems;
    private ApplicationWindow window;
    private boolean initialized = false;
    private static final File storageFile = new File("src\\items.bin");
    public Application() {
        allItems = new DelayedThreadQueue(0, this);
        searchedItems = new DelayedThreadQueue(0, this);
        init();
    }

    /**
     * Initializes the application
     */
    public void init() {
        if (initialized) return;
        MarketAPI.loadItems();
        window = new ApplicationWindow(1280, 720);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                shutdown();
            }
        });
        //System.out.println(Utils.listToString(window.getTableModel().getDataVector()));
        loadSearchedItems = new RunOnceAfterDelayThread(1000, () -> {
            final List<Item> dataVector = window.getTableModel().getDataVector();
            for (Item entry : dataVector) if (window.getTableModel().filter(entry)) searchedItems.pushTask(entry);
        });
        window.searchBar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                window.getTableModel().setSearchText(window.searchBar.getText());
                window.getTableModel().fireTableDataChanged();
                if (window.searchBar.getText().isBlank()) {
                    searchedItems.purgeQueue();
                    searchedItems.setPaused(true);
                    allItems.setPaused(false);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchedItems.purgeQueue();
                    loadSearchedItems.trigger();
                    searchedItems.setPaused(false);
                    allItems.setPaused(true);
                }
            }
        });

        window.styleComponents(ApplicationWindow.STYLE_DARK);
        initialized = true;
    }

    private void shutdown() {
        try {
            if (!storageFile.exists()) {
                storageFile.getParentFile().mkdirs();
                storageFile.createNewFile();
            }
            ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(storageFile)));
            oos.writeObject(window.getTableModel().getDataVector());
            oos.flush();
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        allItems.terminate();
        searchedItems.terminate();
    }

    /**
     * Initialize the application if it is not already initialized, then start it.
     */
    public void start() {
        init();
        window.setVisible(true);
        gatherInfo();
    }

    /**
     * Gathers a list of items from the API and files and begins queuing them for updates
     */
    public void gatherInfo() {
        for (int i = 0; i < MarketAPI.itemNames.size(); i++) {
            String name = MarketAPI.itemNames.get(i);
            Item temp = new Item(name, i);
            allItems.pushTask(name, i);
            window.getTableModel().addRow(temp);
        }
        try {
            if (storageFile.exists()) {
                ObjectInputStream oos = new ObjectInputStream(new GZIPInputStream(new FileInputStream(storageFile)));
                List<Item> o = (List<Item>) oos.readObject();
                o.sort(Comparator.comparing(a -> a.name));
                for (int i = 0; i < o.size(); i++) o.get(i).location = i;
                window.getTableModel().setDataVector(o);
                oos.close();

            }
            /*
            ArrayDeque<Item> queue = allItems.getQueue();
            int size = queue.size();
            for(int i = 0; i < size; i++){
                Item item = queue.();
                System.out.println(item.name);
                //sets the initialized items to be updated last. Useful for first-time setup of items
                if(item.relics != null){
                    System.out.println(item.name + " was initialized");
                    queue.removeLast();
                    queue.addFirst(item);
                }
            }*/
            //todo make this work
        } catch (Exception e) {
            e.printStackTrace();
        }
        allItems.start();
        searchedItems.start();
    }

    public ApplicationWindow getWindow() {
        return window;
    }
}
