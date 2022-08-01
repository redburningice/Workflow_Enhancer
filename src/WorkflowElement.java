public class WorkflowElement {

    String csvuniqueId;
    Point point;

    public WorkflowElement(String csvuniqueId, int xCoordinate, int yCoordinate) {
        this.csvuniqueId = csvuniqueId;
        this.point = new Point(xCoordinate, yCoordinate);
    }

    public String getCsvuniqueId() {
        return csvuniqueId;
    }

    public String toString() {
        return csvuniqueId + " | " + getPoint().getxCoordinate() + ", " + getPoint().getyCoordinate();
    }

    public Point getPoint() {
        return point;
    }
}
