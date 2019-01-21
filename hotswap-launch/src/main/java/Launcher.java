/**
 * 563868273@qq.com Inc.
 * Copyright (c) 2010-2018 All Rights Reserved.
 */

import com.sun.tools.attach.VirtualMachine;

/**
 * <p></p>
 *
 * @author 563868273@qq.com
 * @version $Id: Launcher.java, v 0.1 2018-12-15 下午11:13 @yourMIS $$
 */
public class Launcher {

    public static void install(String pid, String agentPath) throws Exception {
        VirtualMachine vmObj = null;
        try {
            vmObj = VirtualMachine.attach(pid);
            if (vmObj != null) {
                vmObj.loadAgent(agentPath, pid);
            }

        } finally {
            if (null != vmObj) {
                vmObj.detach();
            }
        }
    }
}