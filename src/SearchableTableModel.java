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
    public SearchableTableModel(String[] columnNames, int i) {
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
    public void setDataVector(List<T> dataVector) {
        this.tableData = nonNullArrayList(dataVector);
        fireTableStructureChanged();
    }
    public void setDataVector(List<T> dataVector,
                              List<String> columnIdentifiers) {
        this.tableData = nonNullArrayList(dataVector);
        this.columnNames = nonNullArrayList(columnIdentifiers);
        fireTableStructureChanged();
    }

    public final List<T> getDataVector(){
        return this.tableData;
    }
    public void addRow(T row){
        this.tableData.add(row);
        fireTableRowsInserted(tableData.size()-1, tableData.size()-1);
    }
    private static <E> List<E> nonNullArrayList(List<E> v) {
        return (v != null) ? v : new ArrayList<E>();
    }
}
