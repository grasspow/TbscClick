package grasspow.tbscclick.impl;

import grasspow.tbscclick.IKeyBind;
import net.minecraft.client.KeyMapping;

/**
 * @author tbsc on 02/09/2021
 */
public class KeyBind implements IKeyBind {

    private final KeyMapping key;

    public KeyBind(KeyMapping key) {
        this.key = key;
    }

    @Override
    public void setHeld(boolean held) {
        KeyMapping.set(key.getKey(), held);
    }

    @Override
    public boolean isDown() {
        return key.isDown();
    }
}
