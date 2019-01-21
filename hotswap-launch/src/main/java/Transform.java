/**
 * 563868273@qq.com Inc.
 * Copyright (c) 2010-2018 All Rights Reserved.
 */

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * <p></p>
 *
 * @author 563868273@qq.com
 * @version $Id: Test.java, v 0.1 2018-12-15 下午11:51 @yourMIS $$
 */
public class Transform {
    public static  String jarBasePath = "";
    static {
        File file = new File(Transform.class.getResource(File.separator).getFile());
        jarBasePath = file.getParentFile().getParentFile().getParent() + File.separator+"hotswap-agent"+ File.separator + "target";
    }
    public static void main(String[] args) throws Exception {
        Test1 tt = new Test1();
        installAgent();
        TestTransformer testTransformer = new TestTransformer();
        int max = 20;
        int index = 0;
        while (++index<max){
            tt.sayHello();
            testTransformer.testTrans();
            Thread.sleep(100L);
        }
    }

    /**
     * 初始化引擎
     */
    private static void installAgent() throws Exception {
        //对classLoader添加第三方Jar包
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        File file = new File(jarBasePath + "/hotswap-agent-jar-with-dependencies.jar");
        method.setAccessible(true);
        method.invoke(classLoader, file.toURI().toURL());
        final String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();

        /**
         * 获取当前的Pid
         */
        final String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
        Launcher.install(pid, jarBasePath + "/hotswap-agent.jar");
    }
}