package com.gtnewhorizon.gtnhlib.eventbus;

import static org.objectweb.asm.Opcodes.*;

import org.apache.logging.log4j.ThreadContext;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.IEventListener;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;

public class StaticASMEventHandler implements IEventListener {

    private static int IDs = 0;
    private static final String HANDLER_DESC = Type.getInternalName(IEventListener.class);
    private static final String HANDLER_FUNC_DESC = Type
            .getMethodDescriptor(IEventListener.class.getDeclaredMethods()[0]);
    private static final ASMClassLoader LOADER = new ASMClassLoader();
    private static final Object2ObjectMap<String, Class<?>> CACHE = new Object2ObjectOpenHashMap<>();
    private static final boolean GETCONTEXT = Boolean.parseBoolean(System.getProperty("fml.LogContext", "false"));

    private final IEventListener handler;
    private final ModContainer owner;
    private final String readable;
    private final boolean receiveCanceled;
    @Getter
    private final EventPriority priority;

    StaticASMEventHandler(ModContainer owner, MethodInfo method) throws Exception {
        this.owner = owner;
        handler = (IEventListener) createWrapper(method).getDeclaredConstructor().newInstance();
        readable = "ASM: " + method.getDeclaringClass() + " " + method.getName() + method.getDesc();
        receiveCanceled = method.receiveCanceled;
        priority = method.getPriority();
    }

    @Override
    public void invoke(Event event) {
        if (owner != null && GETCONTEXT) {
            ThreadContext.put("mod", owner.getName());
        } else if (GETCONTEXT) {
            ThreadContext.put("mod", "");
        }
        if (handler != null) {
            if (!event.isCancelable() || !event.isCanceled() || receiveCanceled) {
                handler.invoke(event);
            }
        }
        if (GETCONTEXT) ThreadContext.remove("mod");
    }

    public Class<?> createWrapper(MethodInfo method) {
        Class<?> cached = CACHE.get(method.getKey());
        if (cached != null) return cached;

        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        String name = getUniqueName(method);
        String desc = name.replace('.', '/');
        String instType = method.getDeclaringClass().replace('.', '/');
        String eventType = EventBusUtil.getParameterClassInternal(method.getDesc());

        cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, desc, null, "java/lang/Object", new String[] { HANDLER_DESC });

        cw.visitSource(".dynamic", null);
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "invoke", HANDLER_FUNC_DESC, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, eventType);
            mv.visitMethodInsn(INVOKESTATIC, instType, method.name, method.desc, false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        cw.visitEnd();
        Class<?> ret = LOADER.define(name, cw.toByteArray());
        CACHE.put(method.getKey(), ret);
        return ret;
    }

    private String getUniqueName(MethodInfo method) {
        String param = EventBusUtil.getParameterClassName(method.getDesc());
        String declaring = method.getDeclaringClass();
        return String.format(
                "%s_%d_%s_%s_%s",
                getClass().getName(),
                IDs++,
                EventBusUtil.getSimpleClassName(declaring),
                method.getName(),
                EventBusUtil.getSimpleClassName(param));
    }

    private static class ASMClassLoader extends ClassLoader {

        private ASMClassLoader() {
            super(ASMClassLoader.class.getClassLoader());
        }

        public Class<?> define(String name, byte[] data) {
            return defineClass(name, data, 0, data.length);
        }
    }

    public String toString() {
        return readable;
    }
}
