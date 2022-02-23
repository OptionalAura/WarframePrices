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

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ApplicationWindow extends JFrame {
    /**
     * A dark theme meant to be easy on the eyes
     */
    public static final int STYLE_DARK = 0;
    /**
     * A light theme. This is not the default because light theme is trash.
     */
    public static final int STYLE_LIGHT = 1;
    /**
     * Hashmap used to store color palettes for each theme
     */
    private static final HashMap<Integer, HashMap<String, Color>> themes;

    static {
        themes = new HashMap<>();
        HashMap<String, Color> dark = new HashMap<>();
        dark.put("primaryBackground", Color.decode("#313131"));
        dark.put("primaryForeground", Color.decode("#E5E5E5"));
        dark.put("secondaryBackground", Color.decode("#161616"));
        dark.put("secondaryForeground", Color.decode("#FFFFFF"));
        dark.put("tableBackground", dark.get("secondaryBackground"));
        dark.put("tableForeground", dark.get("primaryForeground"));
        dark.put("tableBorders", dark.get("primaryBackground"));

        themes.put(STYLE_DARK, dark);
        HashMap<String, Color> light = new HashMap<>();
        light.put("primaryBackground", Color.decode("#FFFFFF"));
        light.put("primaryForeground", Color.decode("#000000"));
        light.put("secondaryBackground", Color.decode("#E5E5E5"));
        light.put("secondaryForeground", Color.decode("#313131"));
        light.put("tableBackground", light.get("secondaryBackground"));
        light.put("tableForeground", light.get("primaryForeground"));
        light.put("tableBorders", light.get("primaryBackground"));
        themes.put(STYLE_LIGHT, light);
    }

    public JTextField searchBar;
    public TableRowSorter<SearchableTableModel<Item>> tableSorter;
    GridBagLayout gbl;
    GridBagConstraints gbc;
    private JPanel mainPanel;
    JButton saveButton;
    JCheckBox showTrackedOnly;
    JPopupMenu popupMenu;
    JMenuItem trackItem;
    JMenuItem trackValue;
    //TODO Rename this to something actually memorable
    private JTable table;
    private SearchableTableModel<Item> tableModel;

    /**
     * Creates an application window with the specified width and height
     *
     * @param width
     * @param height
     */
    public ApplicationWindow(final int width, int height) {
        init(width, height);
        initComponents();
        layoutComponents();
    }

    /**
     * Initializes this window with its specified dimensions
     */
    private void init(final int width, int height) {
        this.setPreferredSize(new Dimension(width, height));
        this.setSize(new Dimension(width, height));
        this.setMinimumSize(new Dimension(100, 80));
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    /**
     * Initializes all components
     */
    private void initComponents() {
        mainPanel = new JPanel();
        searchBar = new JTextField();
        saveButton = new JButton("Save");
        showTrackedOnly = new JCheckBox();
        tableModel = new SearchableTableModel<Item>(new String[]{"Name", "Buy Price", "Sell Price", "Profit", "Average Price (48h)",
                "Average Price " + "(90d)", "Trend", "Orders", "Profitable?", "Relics", "Tags", "Ducats", "Ducats/Plat"}, 0) {
            //todo allow permissive filters rather than exclusive (i.e show things that match A & B, but also show things that match A || B)
            @Override
            public boolean filter(Item item) {
                if (showTrackedOnly.isSelected()) return item.tracked;
                if (getSearchText().isBlank()) return true;
                boolean shouldShow = true;
                String[] conditions = getSearchText().trim().split(",");
                for (String value : conditions) {
                    String condition = value.trim();
                    if (condition.isBlank()) continue;
                    boolean inverted = condition.charAt(0) == '!';
                    String firstTrim = inverted ? condition.substring(1) : condition;
                    boolean searchTagOnly = (firstTrim.length() > 1) && firstTrim.charAt(0) == '#';
                    final String searchText = searchTagOnly ? firstTrim.substring(1) : firstTrim;
                    boolean valid = false;
                    if (!searchTagOnly && Utils.containsIgnoreCase(item.name, searchText)) valid = true;
                    else if (item.tags != null && Arrays.stream(item.tags).anyMatch((s) -> s.equalsIgnoreCase(searchText))) valid = true;
                    else if (!searchTagOnly && item.relics != null && Arrays.stream(item.relics).anyMatch((s) -> s.equalsIgnoreCase(searchText)))
                        valid = true;
                    if (shouldShow) shouldShow = valid != inverted;
                }

                return shouldShow;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return Item.getColumnClass(columnIndex);
            }

            public Item getItemAt(int rowIndex) {
                return getItemFromAbsoluteIndex(table.convertRowIndexToModel(rowIndex));
            }

            public Item getItemFromAbsoluteIndex(int rowIndex) {
                return getDataVector().get(rowIndex);
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return getDataVector().get(rowIndex).getValueAt(columnIndex);
            }
        };

        tableSorter = new TableRowSorter<>(tableModel);
        saveButton.addActionListener(e -> {
            List<Item> items = tableModel.getDataVector().stream().filter(item -> tableModel.filter(item)).toList();
            try {
                File f = new File("search.csv");
                CSVWriter writer = new CSVWriter(f);
                for (Item i : items) writer.writeObject(i);
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        table = new JTable(tableModel);
        table.setRowSorter(tableSorter);
        popupMenu = new JPopupMenu();
        trackValue = new JMenuItem("Tracked Value: ");
        trackItem = new JMenuItem("Track");
        trackItem.addActionListener(e -> {
            int rowAtPoint = table.getSelectedRow();
            if (rowAtPoint >= 0) {
                Item item = tableModel.getDataVector().get(table.convertRowIndexToModel(rowAtPoint));
                if (item.tracked) item.tracked = false;
                else {
                    item.tracked = true;
                    String input;
                    do input = JOptionPane.showInputDialog("Enter Target Price"); while (input == null || input.isBlank() ||
                            !input.matches("^[0-9]*$"));
                    item.trackValue = Integer.parseInt(input);
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JTable source = (JTable) e.getSource();
                    int row = source.rowAtPoint(e.getPoint());
                    int column = source.columnAtPoint(e.getPoint());

                    if (!source.isRowSelected(row)) source.changeSelection(row, column, false, false);

                    Item item = tableModel.getDataVector().get(table.convertRowIndexToModel(table.rowAtPoint(e.getPoint())));
                    if (item.tracked) {
                        trackValue.setVisible(true);
                        trackValue.setText("Tracked Value: " + item.trackValue);
                        trackItem.setText("Untrack");
                    } else {
                        trackValue.setVisible(false);
                        trackItem.setText("Track");
                    }
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        popupMenu.add(trackValue);
        popupMenu.add(trackItem);
        tableSorter.setRowFilter(tableModel.filter);
    }

    private void layoutComponents() {
        this.add(mainPanel);
        //set base properties of the layout
        gbl = new GridBagLayout();
        gbc = new GridBagConstraints();
        gbl.columnWeights = new double[]{1.0, 0, 0.3};
        gbl.rowWeights = new double[]{0.0, 1.0};
        gbl.rowHeights = new int[]{30};
        mainPanel.setLayout(gbl);
        gbl.setConstraints(mainPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;

        //add components
        mainPanel.add(searchBar, gbc);
        gbc.gridx++;
        mainPanel.add(saveButton, gbc);
        gbc.gridx++;
        mainPanel.add(showTrackedOnly, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(new JScrollPane(table), gbc);
        //mainPanel.add(table, gbc);
    }

    /**
     * Style all components based on a specified theme
     *
     * @see #STYLE_DARK
     * @see #STYLE_LIGHT
     */
    public void styleComponents(int style) {
        this.setBackground(themes.get(style).get("primaryBackground"));
        this.setBackground(themes.get(style).get("primaryForeground"));

        mainPanel.setBackground(themes.get(style).get("primaryBackground"));
        mainPanel.setForeground(themes.get(style).get("primaryForeground"));


        searchBar.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        searchBar.setBackground(themes.get(style).get("secondaryBackground"));
        searchBar.setForeground(themes.get(style).get("secondaryForeground"));
        table.setBackground(themes.get(style).get("tableBackground"));
        table.setForeground(themes.get(style).get("tableForeground"));
        table.setGridColor(themes.get(style).get("tableBorders"));
    }

    public SearchableTableModel<Item> getTableModel() {
        return tableModel;
    }
}