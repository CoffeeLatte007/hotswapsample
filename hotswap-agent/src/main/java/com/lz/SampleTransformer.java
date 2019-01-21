/**
 * 563868273@qq.com Inc.
 * Copyright (c) 2010-2019 All Rights Reserved.
 */
package com.lz;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Random;

/**
 * <p></p>
 *
 * @author 563868273@qq.com
 * @version $Id: SampleTransformer.java, v 0.1 2019-01-21 下午1:04 @yourMIS $$
 */
public class SampleTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!"TestTransformer".equals(className)){
            //返回Null代表不进行处理
            return null;
        }
        //进行随机输出testTrans + random.nextInt(3)
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor classVisitor = new SampleClassVistor(Opcodes.ASM5,classWriter);
        reader.accept(classVisitor,ClassReader.SKIP_DEBUG);
        return classWriter.toByteArray();
    }
}
class SampleClassVistor extends ClassVisitor {

    public SampleClassVistor(int i, ClassVisitor classVisitor) {
        super(i, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (!"testTrans".equals(name)){
            return mv;
        }
        return new SampleMethodVisitor(Opcodes.ASM5, mv);
    }
}
class SampleMethodVisitor extends MethodVisitor{
    public SampleMethodVisitor(int i, MethodVisitor methodVisitor) {
        super(i, methodVisitor);
    }

    @Override
    public void visitLdcInsn(Object o) {
        super.visitLdcInsn("testTrans" + String.valueOf(new Random().nextInt(3)));
    }
}