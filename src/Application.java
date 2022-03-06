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
    public final DelayedThreadQueue allItems;
    public final DelayedThreadQueue searchedItems;
    private RunOnceAfterDelayThread loadSearchedItems;
    private boolean paused = false;
    private boolean isSearching = false;
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

        window.searchBar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                window.getTableModel().setSearchText(window.searchBar.getText());
                window.getTableModel().fireTableDataChanged();
                if (window.searchBar.getText().isBlank() && !window.showTrackedOnly.isSelected()) {
                    isSearching = false;
                    searchedItems.purgeQueue();
                    searchedItems.setPaused(true);
                    allItems.setPaused(false);
                } else if (!window.searchBar.getText().isBlank() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    isSearching = true;
                    searchedItems.purgeQueue();
                    searchedItems.setPaused(false);
                    allItems.setPaused(true);
                    final List<Item> dataVector = window.getTableModel().getDataVector();
                    for (Item entry : dataVector) if (window.getTableModel().filter(entry)) searchedItems.pushTask(entry);
                }
            }
        });
        window.pauseButton.addActionListener((e) -> {
            paused = !paused;
            if (paused) {
                allItems.setPaused(true);
                searchedItems.setPaused(true);
                window.pauseButton.setText("Resume");
            } else {
                if (isSearching) {
                    searchedItems.setPaused(false);
                } else {
                    allItems.setPaused(false);
                }
                window.pauseButton.setText("Pause");
            }
        });
        window.showTrackedOnly.addActionListener(e -> window.getTableModel().fireTableDataChanged());
        window.updateMenuButton.addActionListener(e -> {
            int[] selectedRows = window.table.getSelectedRows();
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int row = selectedRows[i];
                if (row >= 0) {
                    Item item = window.getTableModel().getDataVector().get(window.table.convertRowIndexToModel(row));
                    if (!allItems.isPaused()) {
                        //
                        allItems.addTask(item, false);
                    } else if (!searchedItems.isPaused()) {
                        //
                        searchedItems.addTask(item, false);
                    }
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
            //e.printStackTrace();
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
        //todo fix this to make sure it has the latest updates
        for (int i = 0; i < MarketAPI.itemNames.size(); i++) {
            String name = MarketAPI.itemNames.get(i);
            Item temp = new Item(name, i);
            window.getTableModel().addRow(temp);
        }
        try {
            if (storageFile.exists()) {
                ObjectInputStream oos = new ObjectInputStream(new GZIPInputStream(new FileInputStream(storageFile)));
                List<Item> o = (List<Item>) oos.readObject();

                window.getTableModel().setDataVector(o);
                oos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        window.getTableModel().getDataVector().sort(Comparator.comparing(o -> o.name));
        //for some reason I get a NullPointerException if I do this in the initialization of the item, so I'll loop again here and do this
        for (int i = 0; i < window.getTableModel().getDataVector().size(); i++) {
            Item item = window.getTableModel().getDataVector().get(i);
            MarketAPI.Pair<Integer, Integer> itemCount = WFInfo.getItemCount(item.name);
            item.owned = itemCount.left;
            item.itemsInSet = itemCount.right;
            item.mastered = WFInfo.isMastered(item.name);
            window.getTableModel().getDataVector().set(i, item);
            allItems.pushTask(item);
        }

        for (int i = 0; i < window.getTableModel().getDataVector().size(); i++) {
            window.getTableModel().getDataVector().get(i).location = i;
        }
        window.getTableModel().fireTableDataChanged();
        allItems.start();
        searchedItems.start();
    }

    public ApplicationWindow getWindow() {
        return window;
    }
}
