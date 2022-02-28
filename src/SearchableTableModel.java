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
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;


@SuppressWarnings({"SerializableHasSerializationMethods", "serial"})
public abstract class SearchableTableModel<T> extends AbstractTableModel {
    //todo serialize tableData
    private List<T> tableData;
    //private Vector<Vector> tableText;
    private List<String> columnNames;
    private String searchText = "";
    public void setSearchText(String text){
        this.searchText = text;
    }
    public String getSearchText(){
        return this.searchText;
    }
    public SearchableTableModel(){
        init();
    }

    public final RowFilter<SearchableTableModel<T>, Integer> filter = new RowFilter<>() {
        @Override
        public boolean include(Entry<? extends SearchableTableModel<T>, ? extends Integer> entry) {
            T item = tableData.get(entry.getIdentifier());
            return filter(item);
        }
    };

    public abstract boolean filter(T object);

    public SearchableTableModel(String[] columnNames) {
        setDataVector(null, List.of(columnNames));
        init();
    }

    public String getColumnName(int column) {
        String id = null;

        if (column < columnNames.size() && (column >= 0)) {
            id = columnNames.get(column);
        }
        return (id == null) ? super.getColumnName(column)
                : id;
    }
    private void init(){

    }

    /**
     * Returns the number of rows in the model. A
     * <code>JTable</code> uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see #getColumnCount
     */
    @Override
    public int getRowCount() {
        return tableData.size();
    }

    /**
     * Returns the number of columns in the model. A
     * <code>JTable</code> uses this method to determine how many columns it
     * should create and display by default.
     *
     * @return the number of columns in the model
     * @see #getRowCount
     */
    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public abstract Class<?> getColumnClass(int columnIndex);

    /**
     * Returns the value for the cell at <code>columnIndex</code> and
     * <code>rowIndex</code>.
     *
     * @param rowIndex    the row whose value is to be queried
     * @param columnIndex the column whose value is to be queried
     * @return the value Object at the specified cell
     */
    @Override
    public abstract Object getValueAt(int rowIndex, int columnIndex);

    public void setDataVector(List<T> dataVector, List<String> columnIdentifiers) {
        this.tableData = nonNullArrayList(dataVector);
        this.columnNames = nonNullArrayList(columnIdentifiers);
        fireTableDataChanged();
        fireTableStructureChanged();
    }

    public final List<T> getDataVector() {
        return this.tableData;
    }

    public void setDataVector(List<T> dataVector) {
        this.tableData = nonNullArrayList(dataVector);
        fireTableDataChanged();
    }

    public void addRow(T row) {
        this.tableData.add(row);
        fireTableRowsInserted(tableData.size() - 1, tableData.size() - 1);
    }

    private static <E> List<E> nonNullArrayList(List<E> v) {
        return (v != null) ? v : new ArrayList<>();
    }
}
