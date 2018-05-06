package com.jamieswhiteshirt.jsmodels;

import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = JSModels.MODID,
    version = JSModels.VERSION,
    clientSideOnly = true
)
@Mod.EventBusSubscriber
public class JSModels {
    public static final String MODID = "jsmodels";
    public static final String VERSION = "1.12.2-1.0";

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        ModelLoaderRegistry.registerLoader(new JSModelLoader());
    }
}
