package com.gtnewhorizon.gtnhlib.client.ResourcePackUpdater;

final class UpdaterMeta {

    final String packName;
    final String packVersion;
    final String packGameVersion;
    final String sourceType;
    final String owner;
    final String repo;

    UpdaterMeta(String packName, String packVersion, String packGameVersion, String sourceType, String owner,
            String repo) {
        this.packName = packName;
        this.packVersion = packVersion;
        this.packGameVersion = packGameVersion;
        this.sourceType = sourceType;
        this.owner = owner;
        this.repo = repo;
    }
}
