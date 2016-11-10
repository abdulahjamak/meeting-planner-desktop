package meetings;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TextField;

import java.util.Comparator;

public class FilteredSortedList<T extends Participant> {
    private final TextField search = new TextField();
    final SortedList<T> sortedData;

    public FilteredSortedList(ObservableList<T> list) {
        FilteredList<T> filteredData = new FilteredList<>(list, p -> true);
        search.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(person -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                return person.getName().toLowerCase().contains(newValue.toLowerCase());
            });
        });

        sortedData = new SortedList<>(filteredData);
    }

    public void bind(ObservableValue<Comparator<T>> comparator) {
        sortedData.comparatorProperty().bind(comparator);
    }

    public TextField getField() {
        return search;
    }
}
