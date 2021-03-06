package io.anuke.mindustry.tools;

import io.anuke.arc.*;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.g2d.TextureAtlas.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;
import org.reflections.*;
import org.reflections.scanners.*;
import org.reflections.util.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class ScriptStubGenerator{

    public static void main(String[] args){
        String base = "io.anuke.mindustry";
        Array<String> blacklist = Array.with("plugin", "mod", "net", "io", "tools", "gen");
        Array<String> nameBlacklist = Array.with("ClientLauncher", "NetClient", "NetServer", "ClassAccess");
        Array<Class<?>> whitelist = Array.with(Draw.class, Fill.class, Lines.class, Core.class, TextureAtlas.class, TextureRegion.class, Time.class, System.class, PrintStream.class, AtlasRegion.class, String.class, Mathf.class, Angles.class, Color.class);
        Array<String> nopackage = Array.with("io.anuke.arc.func", "java.lang", "java");
        Array<String> imported = Array.with("io.anuke.mindustry.type", "io.anuke.mindustry.world");

        String fileTemplate = "package io.anuke.mindustry.mod;\n" +
        "\n" +
        "import io.anuke.arc.collection.*;\n" +
        "//obviously autogenerated, do not touch\n" +
        "public class ClassAccess{\n" +
        //"\tstatic final Array<Class<?>> allowedClasses = Array.with($ALLOWED_CLASSES$);\n" +
        "\tpublic static final ObjectSet<String> allowedClassNames = ObjectSet.with($ALLOWED_CLASS_NAMES$);\n" +
        "}";

        List<ClassLoader> classLoadersList = new LinkedList<>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());

        Reflections reflections = new Reflections(new ConfigurationBuilder()
        .setScanners(new SubTypesScanner(false), new ResourcesScanner())
        .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
        .filterInputsBy(new FilterBuilder()
        .include(FilterBuilder.prefix("io.anuke.mindustry"))
        .include(FilterBuilder.prefix("io.anuke.arc.func"))
        .include(FilterBuilder.prefix("io.anuke.arc.collection"))
        .include(FilterBuilder.prefix("io.anuke.arc.scene"))
        ));

        Array<Class<?>> classes = Array.with(reflections.getSubTypesOf(Object.class));
        classes.addAll(reflections.getSubTypesOf(Enum.class));
        classes.addAll(whitelist);
        classes.sort(Structs.comparing(Class::getName));

        classes.removeAll(type -> type.isSynthetic() || type.isAnonymousClass() || type.getCanonicalName() == null || Modifier.isPrivate(type.getModifiers())
        || blacklist.contains(s -> type.getName().startsWith(base + "." + s + ".")) || nameBlacklist.contains(type.getSimpleName()));
        classes.distinct();
        ObjectSet<String> used = ObjectSet.with();

        StringBuilder result = new StringBuilder("//Generated class. Do not modify.\n");
        result.append("\n").append(new FileHandle("core/assets/scripts/base.js").readString()).append("\n");
        for(Class type : classes){
            if(used.contains(type.getPackage().getName()) || nopackage.contains(s -> type.getName().startsWith(s))) continue;
            result.append("importPackage(Packages.").append(type.getPackage().getName()).append(")\n");
            used.add(type.getPackage().getName());
        }

        //Log.info(result);

        new FileHandle("core/assets/scripts/global.js").writeString(result.toString());
        new FileHandle("core/src/io/anuke/mindustry/mod/ClassAccess.java").writeString(fileTemplate
            .replace("$ALLOWED_CLASSES$", classes.toString(", ", type -> type.getName() + ".class"))
            .replace("$ALLOWED_CLASS_NAMES$", classes.toString(", ", type -> "\"" + type.getName() + "\"")));
    }
}
