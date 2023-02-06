package dev.breeze.settlements.xxxxxxreactions.config;

import lombok.Getter;

@Getter
public enum Reaction {

    SWIRL(),
    CRYSTALLIZE(),

    VAPORIZE(),
    MELT(),

    FROZEN(),
    SHATTER(),

    OVERLOAD(),
    SUPERCONDUCT(),
    ELECTRO_CHARGED(),

    BLOOM(),
    HYPERBLOOM(),
    BURGEON(),

    BURNING(),
    QUICKEN(),
    AGGRAVATE(),
    SPREAD(),
    ;

    private double amplifier;
    private double extraDamage;

}
