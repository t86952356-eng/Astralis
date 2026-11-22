package astralis.social.party;

public final class PartyWaypoint {
    public final String id;
    public final String creatorName;
    public final String label;
    public final String serverAddress;
    public final String serverFingerprint;
    public final String dimension;
    public final int color;
    public final double x;
    public final double y;
    public final double z;
    public final long timestamp;

    public PartyWaypoint(
            String id,
            String creatorName,
            String label,
            String serverAddress,
            String serverFingerprint,
            String dimension,
            int color,
            double x,
            double y,
            double z,
            long timestamp) {
        this.id = id;
        this.creatorName = creatorName;
        this.label = label;
        this.serverAddress = serverAddress;
        this.serverFingerprint = serverFingerprint;
        this.dimension = dimension;
        this.color = color;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof PartyWaypoint other)) {
            return false;
        } else {
            if (this.getColor() != other.getColor()) {
                return false;
            }

            if (Double.compare(this.getX(), other.getX()) != 0) {
                return false;
            }

            if (Double.compare(this.getY(), other.getY()) != 0) {
                return false;
            }

            if (Double.compare(this.getZ(), other.getZ()) != 0) {
                return false;
            }

            if (this.getTimestamp() != other.getTimestamp()) {
                return false;
            }

            String thisId = this.getId();
            String otherId = other.getId();
            if (thisId == null ? otherId == null : thisId.equals(otherId)) {
                String thisCreatorName = this.getCreatorName();
                String otherCreatorName = other.getCreatorName();
                if (thisCreatorName == null
                        ? otherCreatorName == null
                        : thisCreatorName.equals(otherCreatorName)) {
                    String thisLabel = this.getLabel();
                    String otherLabel = other.getLabel();
                    if (thisLabel == null ? otherLabel == null : thisLabel.equals(otherLabel)) {
                        String thisServerAddress = this.getServerAddress();
                        String otherServerAddress = other.getServerAddress();
                        if (thisServerAddress == null
                                ? otherServerAddress == null
                                : thisServerAddress.equals(otherServerAddress)) {
                            String thisServerFingerprint = this.getServerFingerprint();
                            String otherServerFingerprint = other.getServerFingerprint();
                            if (thisServerFingerprint == null
                                    ? otherServerFingerprint == null
                                    : thisServerFingerprint.equals(otherServerFingerprint)) {
                                String thisDimension = this.getDimension();
                                String otherDimension = other.getDimension();
                                return thisDimension == null
                                        ? otherDimension == null
                                        : thisDimension.equals(otherDimension);
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public String toString() {

        String idValue = this.getId();
        String creatorNameValue = this.getCreatorName();
        String labelValue = this.getLabel();
        String serverAddressValue = this.getServerAddress();
        String serverFingerprintValue = this.getServerFingerprint();
        String dimensionValue = this.getDimension();
        int colorValue = this.getColor();
        double xValue = this.getX();
        double yValue = this.getY();
        long timestamp = this.getTimestamp();
        double z = this.getZ();
        double y = yValue;
        double x = xValue;
        int color = colorValue;
        String dimension = dimensionValue;
        String serverFingerprint = serverFingerprintValue;
        String serverAddress = serverAddressValue;
        String label = labelValue;
        String creatorName = creatorNameValue;
        String id = idValue;

        return "PartyStatePacket.Waypoint(id="
                + id
                + ", creatorName="
                + creatorName
                + ", label="
                + label
                + ", serverAddress="
                + serverAddress
                + ", serverFingerprint="
                + serverFingerprint
                + ", dimension="
                + dimension
                + ", color="
                + color
                + ", x="
                + x
                + ", y="
                + y
                + ", z="
                + z
                + ", timestamp="
                + timestamp
                + ")";
    }

    @Override
    public int hashCode() {

        int result;

        result = 59 + this.getColor();
        long xBits = Double.doubleToLongBits(this.getX());

        int scaledForX = result * 59;

        result = scaledForX + (int) (xBits >>> 32 ^ xBits);
        long yBits = Double.doubleToLongBits(this.getY());

        int scaledForY = result * 59;

        result = scaledForY + (int) (yBits >>> 32 ^ yBits);
        long zBits = Double.doubleToLongBits(this.getZ());

        int scaledForZ = result * 59;

        result = scaledForZ + (int) (zBits >>> 32 ^ zBits);
        long timestampValue = this.getTimestamp();

        int scaledForTimestamp = result * 59;

        result = scaledForTimestamp + (int) (timestampValue >>> 32 ^ timestampValue);
        String idValue = this.getId();

        int scaledForId = result * 59;
        int idHash;
        if (idValue == null) {

            idHash = 43;
        } else {
            idHash = idValue.hashCode();
        }

        result = scaledForId + idHash;
        String creatorNameValue = this.getCreatorName();

        int scaledForCreatorName = result * 59;
        int creatorNameHash;
        if (creatorNameValue == null) {

            creatorNameHash = 43;
        } else {
            creatorNameHash = creatorNameValue.hashCode();
        }

        result = scaledForCreatorName + creatorNameHash;
        String labelValue = this.getLabel();

        int scaledForLabel = result * 59;
        int labelHash;
        if (labelValue == null) {

            labelHash = 43;
        } else {
            labelHash = labelValue.hashCode();
        }

        result = scaledForLabel + labelHash;
        String serverAddressValue = this.getServerAddress();

        int scaledForServerAddress = result * 59;
        int serverAddressHash;
        if (serverAddressValue == null) {

            serverAddressHash = 43;
        } else {
            serverAddressHash = serverAddressValue.hashCode();
        }

        result = scaledForServerAddress + serverAddressHash;
        String serverFingerprintValue = this.getServerFingerprint();

        int scaledForServerFingerprint = result * 59;
        int serverFingerprintHash;
        if (serverFingerprintValue == null) {

            serverFingerprintHash = 43;
        } else {
            serverFingerprintHash = serverFingerprintValue.hashCode();
        }

        result = scaledForServerFingerprint + serverFingerprintHash;
        String dimensionValue = this.getDimension();

        int scaledForDimension = result * 59;
        int dimensionHash;
        if (dimensionValue == null) {

            dimensionHash = 43;
        } else {
            dimensionHash = dimensionValue.hashCode();
        }

        return scaledForDimension + dimensionHash;
    }

    public int getColor() {
        return this.color;
    }

    public String getDimension() {
        return this.dimension;
    }

    public double getX() {
        return this.x;
    }

    public String getCreatorName() {
        return this.creatorName;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public String getServerAddress() {
        return this.serverAddress;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public String getServerFingerprint() {
        return this.serverFingerprint;
    }

    public String getId() {
        return this.id;
    }

    public String getLabel() {
        return this.label;
    }
}
