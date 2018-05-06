package com.jamieswhiteshirt.jsmodels;

import com.google.common.collect.ImmutableMap;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.animation.ModelBlockAnimation;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.io.IOUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.function.Function;

@Mod.EventBusSubscriber
public class JSModelLoader implements ICustomModelLoader {
    private static final ModelBlockAnimation defaultModelBlockAnimation = new ModelBlockAnimation(ImmutableMap.of(), ImmutableMap.of());
    private static Class<?> class_VanillaModelWrapper = null;
    private static Constructor<IModel> VanillaModelWrapper_constructor = null;

    private static Class<Enum> class_VanillaLoader = null;
    private static Enum VanillaLoader_INSTANCE = null;
    private static Field VanillaLoader_loader = null;

    static {
        try {
            class_VanillaModelWrapper = Class.forName("net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper");
            VanillaModelWrapper_constructor = (Constructor<IModel>) class_VanillaModelWrapper.getDeclaredConstructor(ModelLoader.class, ResourceLocation.class, ModelBlock.class, boolean.class, ModelBlockAnimation.class);
            VanillaModelWrapper_constructor.setAccessible(true);

            class_VanillaLoader = (Class<Enum>) Class.forName("net.minecraftforge.client.model.ModelLoader$VanillaLoader");
            VanillaLoader_INSTANCE = class_VanillaLoader.getEnumConstants()[0];
            VanillaLoader_loader = class_VanillaLoader.getDeclaredField("loader");
            VanillaLoader_loader.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private final ScriptEngineManager engineManager = new ScriptEngineManager();
    private ScriptObjectMirror json;

    {
        try {
            ScriptEngine engine = engineManager.getEngineByName("nashorn");
            json = (ScriptObjectMirror) engine.eval("JSON");
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    private IResourceManager resourceManager;
    private Function<String, Object> require = resourceLocationString -> {
        ResourceLocation resourceLocation = new ResourceLocation(resourceLocationString);
        try {
            if (resourceLocationString.endsWith(".js")) {
                return loadModule(resourceLocation).exports;
            } else if (resourceLocationString.endsWith(".json")) {
                IResource resource = resourceManager.getResource(resourceLocation);
                return json.callMember("parse", IOUtils.toString(resource.getInputStream(), Charset.forName("UTF-8")));
            } else {
                return null;
            }
        } catch (IOException | ScriptException e) {
            return null;
        }
    };

    public class Module {
        public Object exports = new HashMap<>();
    }

    private Module loadModule(ResourceLocation location) throws IOException, ScriptException {
        Module module = new Module();
        IResource resource = resourceManager.getResource(location);
        ScriptEngine engine = engineManager.getEngineByName("nashorn");
        engine.put("require", require);
        engine.put("module", module);
        engine.eval(new InputStreamReader(resource.getInputStream()));
        return module;
    }

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        ResourceLocation jsLocation = new ResourceLocation(modelLocation.getResourceDomain(), modelLocation.getResourcePath() + ".js");
        try {
            resourceManager.getResource(jsLocation);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) throws Exception {
        ResourceLocation location = new ResourceLocation(modelLocation.getResourceDomain(), modelLocation.getResourcePath() + ".js");

        Module module = loadModule(location);
        String output = (String) json.callMember("stringify", module.exports);
        ModelBlock modelBlock = ModelBlock.deserialize(new StringReader(output));
        ModelLoader modelLoader = (ModelLoader) VanillaLoader_loader.get(VanillaLoader_INSTANCE);
        return VanillaModelWrapper_constructor.newInstance(modelLoader, modelLocation, modelBlock, false, defaultModelBlockAnimation);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
}
