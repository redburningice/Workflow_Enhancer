import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.Math.round;

public class Group {
    int groupIndex;
    int avgCoordinate;
    LinkedHashMap<String, Integer> groupMembers = new LinkedHashMap<>();

    /**
     * represents a group for pairs of csvUniqueId and coordinates
     *
     * @param groupIndex unique identifier for the group inside an ArrayList
     * @param firstEntry the first pairing of csvUniqueId and coordinates to initiate a new group
     */
    public Group(int groupIndex, Map.Entry<String, Integer> firstEntry) {
        this.groupIndex = groupIndex;
        addGroupMember(firstEntry);
    }

    public void addGroupMember(Map.Entry<String, Integer> entry) {
        groupMembers.put(entry.getKey(), entry.getValue());
    }

    /**
     * Prints every group member to the console. Only for debugging purposes.
     */
    public void printGroupMembers() {
        for (Map.Entry<String, Integer> entry : groupMembers.entrySet()) {
            System.out.println(entry.getKey() + " - " + entry.getValue());
        }
    }

    public void equalizeCoordinates() {
        for (Map.Entry<String, Integer> entry : groupMembers.entrySet()) {
            groupMembers.put(entry.getKey(), getAvgCoordinate());
        }
    }

    public int getAvgCoordinate() {
        return avgCoordinate;
    }

    public void setAvgCoordinate() {
        //FIXME algorithm is not correct, each group member gets a different coordinate
        int count = 0;
        int sum = 0;

        for (int entry : groupMembers.values()) {
            sum += entry;
            count++;
        }
        this.avgCoordinate = round(sum / count);
    }

    int getGroupIndex() {
        return groupIndex;
    }
}
