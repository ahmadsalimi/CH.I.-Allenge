package client.model;

public class CellInformation {
    public boolean isLayerSet;
    public int layerNumber;
    public boolean isReservedForDodge = false;

    public void setCellInfo(int distance) {
        isLayerSet = true;
        layerNumber = distance;
    }
}
