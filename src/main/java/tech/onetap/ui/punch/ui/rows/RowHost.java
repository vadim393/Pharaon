package tech.onetap.ui.punch.ui.rows;

public interface RowHost {

    void closeOtherRows(SettingRow except);

    int popupViewportMaxY();
}
