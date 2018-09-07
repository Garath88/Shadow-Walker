package com.sakura.bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Permissions {
    MODERATOR("Kunoichi", "Taimanin"),
    FAN(Collections.singletonList(MODERATOR), "Hentai Master");

    private final List<String> values;

    Permissions(String... values) {
        this.values = Arrays.asList(values);
    }

    Permissions(List<Permissions> roles, String... values) {
        List<String> tempList = new ArrayList<>(Arrays.asList(values));
        roles.forEach(role -> tempList.addAll(role.getValues()));
        this.values = tempList;
    }

    public List<String> getValues() {
        return values;
    }
    }