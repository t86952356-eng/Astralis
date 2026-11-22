package astralis.resourcepack;

import lombok.Generated;

public final class PvprpPack {
    private final String name;
    private final String author;
    private final String resolution;
    private final String iconUrl;
    private final String authorProfileUrl;
    private final String views;
    private final String downloads;
    private final String pageUrl;

    @Generated
    public PvprpPack(
            String name,
            String author,
            String resolution,
            String iconUrl,
            String authorProfileUrl,
            String views,
            String downloads,
            String pageUrl) {
        this.name = name;
        this.author = author;
        this.resolution = resolution;
        this.iconUrl = iconUrl;
        this.authorProfileUrl = authorProfileUrl;
        this.views = views;
        this.downloads = downloads;
        this.pageUrl = pageUrl;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getAuthor() {
        return this.author;
    }

    @Generated
    public String getResolution() {
        return this.resolution;
    }

    @Generated
    public String getIconUrl() {
        return this.iconUrl;
    }

    @Generated
    public String getAuthorProfileUrl() {
        return this.authorProfileUrl;
    }

    @Generated
    public String getViews() {
        return this.views;
    }

    @Generated
    public String getDownloads() {
        return this.downloads;
    }

    @Generated
    public String getPageUrl() {
        return this.pageUrl;
    }

    @Generated
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof PvprpPack other)) {
            return false;
        } else {
            String thisName = this.getName();
            String otherName = other.getName();
            if (thisName == null ? otherName == null : thisName.equals(otherName)) {
                String thisAuthor = this.getAuthor();
                String otherAuthor = other.getAuthor();
                if (thisAuthor == null ? otherAuthor == null : thisAuthor.equals(otherAuthor)) {
                    String thisResolution = this.getResolution();
                    String otherResolution = other.getResolution();
                    if (thisResolution == null
                            ? otherResolution == null
                            : thisResolution.equals(otherResolution)) {
                        String thisIconUrl = this.getIconUrl();
                        String otherIconUrl = other.getIconUrl();
                        if (thisIconUrl == null
                                ? otherIconUrl == null
                                : thisIconUrl.equals(otherIconUrl)) {
                            String thisAuthorProfileUrl = this.getAuthorProfileUrl();
                            String otherAuthorProfileUrl = other.getAuthorProfileUrl();
                            if (thisAuthorProfileUrl == null
                                    ? otherAuthorProfileUrl == null
                                    : thisAuthorProfileUrl.equals(otherAuthorProfileUrl)) {
                                String thisViews = this.getViews();
                                String otherViews = other.getViews();
                                if (thisViews == null
                                        ? otherViews == null
                                        : thisViews.equals(otherViews)) {
                                    String thisDownloads = this.getDownloads();
                                    String otherDownloads = other.getDownloads();
                                    if (thisDownloads == null
                                            ? otherDownloads == null
                                            : thisDownloads.equals(otherDownloads)) {
                                        String thisPageUrl = this.getPageUrl();
                                        String otherPageUrl = other.getPageUrl();
                                        return thisPageUrl == null
                                                ? otherPageUrl == null
                                                : thisPageUrl.equals(otherPageUrl);
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
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Generated
    @Override
    public int hashCode() {

        int result;
        String name = this.getName();

        int nameHash;
        if (name == null) {

            nameHash = 43;
        } else {
            nameHash = name.hashCode();
        }

        result = 59 + nameHash;
        String author = this.getAuthor();

        int authorSeed = result * 59;
        int authorHash;
        if (author == null) {

            authorHash = 43;
        } else {
            authorHash = author.hashCode();
        }

        result = authorSeed + authorHash;
        String resolution = this.getResolution();

        int resolutionSeed = result * 59;
        int resolutionHash;
        if (resolution == null) {

            resolutionHash = 43;
        } else {
            resolutionHash = resolution.hashCode();
        }

        result = resolutionSeed + resolutionHash;
        String iconUrl = this.getIconUrl();

        int iconUrlSeed = result * 59;
        int iconUrlHash;
        if (iconUrl == null) {

            iconUrlHash = 43;
        } else {
            iconUrlHash = iconUrl.hashCode();
        }

        result = iconUrlSeed + iconUrlHash;
        String authorProfileUrl = this.getAuthorProfileUrl();

        int authorProfileUrlSeed = result * 59;
        int authorProfileUrlHash;
        if (authorProfileUrl == null) {

            authorProfileUrlHash = 43;
        } else {
            authorProfileUrlHash = authorProfileUrl.hashCode();
        }

        result = authorProfileUrlSeed + authorProfileUrlHash;
        String views = this.getViews();

        int viewsSeed = result * 59;
        int viewsHash;
        if (views == null) {

            viewsHash = 43;
        } else {
            viewsHash = views.hashCode();
        }

        result = viewsSeed + viewsHash;
        String downloads = this.getDownloads();

        int downloadsSeed = result * 59;
        int downloadsHash;
        if (downloads == null) {

            downloadsHash = 43;
        } else {
            downloadsHash = downloads.hashCode();
        }

        result = downloadsSeed + downloadsHash;
        String pageUrl = this.getPageUrl();

        int pageUrlSeed = result * 59;
        int pageUrlHash;
        if (pageUrl == null) {

            pageUrlHash = 43;
        } else {
            pageUrlHash = pageUrl.hashCode();
        }

        return pageUrlSeed + pageUrlHash;
    }

    @Generated
    @Override
    public String toString() {

        String nameValue = this.getName();
        String authorValue = this.getAuthor();
        String resolutionValue = this.getResolution();
        String iconUrlValue = this.getIconUrl();
        String authorProfileUrlValue = this.getAuthorProfileUrl();
        String viewsValue = this.getViews();
        String pageUrl = this.getPageUrl();
        String downloads = this.getDownloads();
        String views = viewsValue;
        String authorProfileUrl = authorProfileUrlValue;
        String iconUrl = iconUrlValue;
        String resolution = resolutionValue;
        String author = authorValue;
        String name = nameValue;

        return "PvprpPack(name="
                + name
                + ", author="
                + author
                + ", resolution="
                + resolution
                + ", iconUrl="
                + iconUrl
                + ", authorProfileUrl="
                + authorProfileUrl
                + ", views="
                + views
                + ", downloads="
                + downloads
                + ", pageUrl="
                + pageUrl
                + ")";
    }
}
