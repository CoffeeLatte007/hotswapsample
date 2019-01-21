package com.lz;
/**
 * 563868273@qq.com Inc.
 * Copyright (c) 2010-2018 All Rights Reserved.
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

/**
 * <p></p>
 *
 * @author 563868273@qq.com
 * @version $Id: com.TestAgentBootstrap.java, v 0.1 2018-12-15 下午10:42 @yourMIS $$
 */
public class TestAgentBootstrap {
    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        doRefine(inst);
        doRetransFrom(inst);
    }

    private static void doRetransFrom(Instrumentation inst) {
        //必须设置true，才能进行多次retrans
        inst.addTransformer(new SampleTransformer(), true);
        new Thread(() -> {
            while (true) {
                try {
                    for(Class<?> clazz : inst.getAllLoadedClasses()){
                        if (clazz.getName().equals("TestTransformer")) {
                            inst.retransformClasses(clazz);
                        }
                    }
                    Thread.sleep(100L);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 一次交换Test1
     * @param inst
     */
    private static void doRefine(Instrumentation inst) {
        new Thread(() -> {
            try {
                List<byte[]> bytess = getBytesList();
                int index = 0;
                for (Class<?> clazz : inst.getAllLoadedClasses()) {
                    if (clazz.getName().equals("Test1")) {
                        while (true) {
                            ClassDefinition classDefinition = new ClassDefinition(clazz, getIndexBytes(index, bytess));
                            // redefindeClass Test1
                            inst.redefineClasses(classDefinition);
                            Thread.sleep(100L);
                            index++;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static List<byte[]>  getBytesList() {
        RandomAccessFile file1 = null;
        RandomAccessFile file2 = null;
        RandomAccessFile file3 = null;
        List<byte[]> bytess = new ArrayList<>();
        try {
            File directory = new File("");// 参数为空
            String courseFile = directory.getCanonicalPath();
            String basePath = courseFile + "/";
            // 输出Test1的class
            file1 = new RandomAccessFile(basePath + "Test1.class", "r");
            // 输出Test2的class
            file2 = new RandomAccessFile(basePath + "Test2.class", "r");
            // 输出Test3的class
            file3 = new RandomAccessFile(basePath + "Test3.class", "r");
            bytess.add(getFileBytes(file1));
            bytess.add(getFileBytes(file2));
            bytess.add(getFileBytes(file3));

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                file1.close();
                file2.close();
                file3.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return bytess;
    }

    private static byte[] getIndexBytes(int index, List<byte[]> bytess) {
        if (index % 3 == 0) {
            return bytess.get(0);
        } else if (index % 3 == 1) {
            return bytess.get(1);
        } else {
            return bytess.get(2);
        }
    }

    private static byte[] getFileBytes(RandomAccessFile file) throws IOException {
        final byte[] bytes = new byte[(int) file.length()];
        file.readFully(bytes);
        return bytes;
    }

}