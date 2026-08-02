package dev.nullkeeper.strengthsmptrollitems.ravager;

import org.bukkit.entity.Ravager;

@FunctionalInterface
interface RavagerDefaultVisibility {
    void hide(Ravager ravager);

    static RavagerDefaultVisibility paper() {
        return ravager -> ravager.setVisibleByDefault(false);
    }
}
