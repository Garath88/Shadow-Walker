package com.sakura.bot.commands;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.CaseFormat;

import commands.waifu.Roles;

public class WaifuRoles implements Roles {

    enum Waifus {
        ANNEROSE("Team Annerose"),
        ASAGI("Team Asagi"),
        ASUKA("Team Asuka"),
        INGRID("Team Ingrid"),
        KURENAI("Team Kurenai"),
        MU("Team Murasaki"),
        RINKO("Rinko is #1"),
        SAKURA("Sakura's Harbingers"),
        SHIRANUI("Team Shiranui"),
        SHIZURU("Team Shizuru"),
        TOKIKO("Team Tokiko"),
        YUKI("Team Yukikaze", "Team Yuki"),
        OBORO("Oboro Squad"),
        ZAIDAN("Zaidan's Interns"),
        ;

        private final String roleName;
        private final String teamName;

        Waifus(String teamName) {
            this.roleName = teamName;
            this.teamName = teamName;
        }

        Waifus(String roleName, String teamName) {
            this.roleName = roleName;
            this.teamName = teamName;
        }

        String getRoleName() {
            return this.roleName;
        }

        String getTeamName() {
            return teamName;
        }

        @Override
        public String toString() {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
        }
    }

    @Override
    public List<String> getRoleArguments() {
        return Stream.of(Waifus.values())
            .map(Waifus::toString)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoles() {
        return Stream.of(Waifus.values())
            .map(Waifus::getRoleName)
            .collect(Collectors.toList());
    }

    @Override
    public String getRole(String argument) {
        return Waifus.valueOf(argument.toUpperCase()).getRoleName();
    }

    @Override
    public String getRoleRepresentation(String role) {
        return Stream.of(Waifus.values())
            .filter(name -> name.getRoleName().equals(role))
            .findFirst()
            .map(Waifus::getTeamName)
            .orElseThrow(IllegalStateException::new);
    }
}