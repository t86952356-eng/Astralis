package astralis.resourcepack;

import lombok.Generated;

import java.util.List;

public final class PvprpPage {
    private final List<PvprpPack> packs;
    private final boolean hasNextPage;

    public PvprpPage(List<PvprpPack> packs, boolean hasNextPage) {
        this.packs = List.copyOf(packs);
        this.hasNextPage = hasNextPage;
    }

    @Generated
    public List<PvprpPack> getPacks() {
        return this.packs;
    }

    @Generated
    public boolean hasNextPage() {
        return this.hasNextPage;
    }

    @Generated
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof PvprpPage other)) {
            return false;
        } else {
            if (this.hasNextPage() != other.hasNextPage()) {
                return false;
            }

            List thisPacks = this.getPacks();
            List otherPacks = other.getPacks();
            return thisPacks == null ? otherPacks == null : thisPacks.equals(otherPacks);
        }
    }

    @Generated
    @Override
    public int hashCode() {

        int result;

        byte nextPageHash;
        if (this.hasNextPage()) {

            nextPageHash = 79;
        } else {

            nextPageHash = 97;
        }

        result = 59 + nextPageHash;
        List packs = this.getPacks();

        int packsSeed = result * 59;
        int packsHash;
        if (packs == null) {

            packsHash = 43;
        } else {
            packsHash = packs.hashCode();
        }

        return packsSeed + packsHash;
    }

    @Generated
    @Override
    public String toString() {
        String packs = String.valueOf(this.getPacks());
        boolean hasNextPage = this.hasNextPage();

        return "PvprpPage(packs=" + packs + ", hasNextPage=" + hasNextPage + ")";
    }
}
