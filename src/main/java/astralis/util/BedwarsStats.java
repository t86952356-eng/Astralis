package astralis.util;

public record BedwarsStats(
        int stars,
        double fkdr,
        int finalKills,
        int finalDeaths,
        int wins,
        int losses,
        int winstreak,
        boolean nicked,
        String uuid,
        int kills,
        int deaths) {

    public BedwarsStats(
            int stars,
            double fkdr,
            int finalKills,
            int finalDeaths,
            int wins,
            int losses,
            int winstreak,
            boolean nicked,
            String uuid) {
        this(stars, fkdr, finalKills, finalDeaths, wins, losses, winstreak, nicked, uuid, 0, 0);
    }

    public int getLosses() {
        return this.losses;
    }

    public String getUuid() {
        return this.uuid;
    }

    public int getKills() {
        return this.kills;
    }

    public int getStars() {
        return this.stars;
    }

    public int getFinalDeaths() {
        return this.finalDeaths;
    }

    public int getDeaths() {
        return this.deaths;
    }

    public boolean isNicked() {
        return this.nicked;
    }

    public int getWinstreak() {
        return this.winstreak;
    }

    public double getKdr() {
        return this.deaths > 0 ? (double) this.kills / this.deaths : this.kills;
    }

    public int getFinalKills() {
        return this.finalKills;
    }

    public int getWins() {
        return this.wins;
    }
}
