# 如何运行
1. 首先对hotswap-agent项目进行打包。在hotswap-agent目录下面执行命令:
```
    mvn package
```
检查target目录下面是否有对应的jar包生成,hostswap-agent.jar和hotswap-agent-jar-with-dependeies.jar

2. 然后进入hotswap-launch中的Transform这个类中，执行main方法。

# 4.自己实现字节码动态替换

不论上我们的Arthas还是我们的jvm-sandbox无外乎使用的就是下面几种技术:
- ASM
- Instrumentation(核心)
- VirtualMachine

## 4.1 ASM
对于ASM字节码修改技术可以参考我之前写的几篇文章:
- [字节码也能做有趣的事
](https://mp.weixin.qq.com/s/Nk4lP7723XIbu5AWXaswmw)
- [字节码也能做有趣的事之ASM](https://mp.weixin.qq.com/s/8yvMSwdjPklcJOyZAlDESw)
- [教你用Java字节码做日志脱敏工具](https://mp.weixin.qq.com/s/I-MIkZ2s57ft14Cul9i2IA)
 

对于ASM修改字节码的技术这里就不做多余阐述。
## 4.2 Instrumentation
Instrumentation是JDK1.6用来构建Java代码的类。Instrumentation是在方法中添加字节码来达到收集数据或者改变流程的目的。当然他也提供了一些额外功能，比如获取当前JVM中所有加载的Class等。

#### 4.2.1获取Instrumentation
Java提供了两种方法获取Instrumentation，下面介绍一下这两种:

##### 4.2.1.1 premain 

在启动的时候，会调用preMain方法:

```
public static void premain(String agentArgs, Instrumentation inst) {
    }

```
需要在启动时添加额外命令

```
java -javaagent:jar 文件的位置 [= 传入 premain 的参数 ] 

```
也需要在maven中配置PreMainClass。

在[教你用Java字节码做日志脱敏工具](https://mp.weixin.qq.com/s/I-MIkZ2s57ft14Cul9i2IA)中很详细的介绍了premain
##### 4.2.1.2 agentmain 
premain是Java SE5开始就提供的代理方式，给了开发者诸多惊喜，不过也有些须不变，由于其必须在命令行指定代理jar，并且代理类必须在main方法前启动。因此，要求开发者在应用前就必须确认代理的处理逻辑和参数内容等等，在有些场合下，这是比较困难的。比如正常的生产环境下，一般不会开启代理功能，所有java SE6之后提供了agentmain，用于我们动态的进行修改，而不需要在设置代理。在 JavaSE6文档当中，开发者也许无法在 java.lang.instrument包相关的文档部分看到明确的介绍，更加无法看到具体的应用 agnetmain 的例子。不过，在 Java SE 6 的新特性里面，有一个不太起眼的地方，揭示了 agentmain 的用法。这就是 Java SE 6 当中提供的 Attach API。

Attach API 不是Java的标准API，而是Sun公司提供的一套扩展 API，用来向目标JVM”附着”（Attach）代理工具程序的。有了它，开发者可以方便的监控一个JVM，运行一个外加的代理程序。

在VirtualMachine中提供了attach的接口

## 4.3 实现HotSwap
本文实现的HotSwap的代码均在https://github.com/lzggsimida123/hotswapsample中，下面简单介绍一下:
#### 4.3.1 redefineClasses
redefineClasses允许我们重新替换JVM中的类,我们现在利用它实现一个简单的需求，我们有下面一个类:

```
public class Test1 implements T1 {

    public void sayHello(){
        System.out.println("Test1");
    }
}
```
在sayHello中打印Test1,然后我们在main方法中循环调用sayHello:

```
 public static void main(String[] args) throws Exception {
        Test1 tt = new Test1();
        int max = 20;
        int index = 0;
        while (++index<max){
            Thread.sleep(100L);
        }
    }
```
如果我们不做任何处理，那么肯定打印出20次Test1。如果我们想完成一个需求，这20次打印是交替打印出Test1,Test2,Test3。那么我们可以借助redefineClass。

```
        //获取Test1,Test2,Test3的字节码
        List<byte[]> bytess = getBytesList();
        int index = 0;
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (clazz.getName().equals("Test1")) {
                while (true) {                
                    //根据index获取本次对应的字节码
                    ClassDefinition classDefinition = new ClassDefinition(clazz, getIndexBytes(index, bytess));
                    // redefindeClass Test1
                    inst.redefineClasses(classDefinition);
                    Thread.sleep(100L);
                    index++;
                }
            }
        }
```
可以看见我们获取了三个calss的字节码，在我们根目录下面有，然后调用redefineClasses替换我们对应的字节码,可以看见我们的结果，将Test1,Test2,Test3打印出来。

![](https://user-gold-cdn.xitu.io/2019/1/21/168701c42bb7236d?w=562&h=630&f=png&s=39548)

####  4.3.2 retransformClasses
redefineClasses直接将字节码做了交换，导致原始字节码丢失，局限较大。使用retransformClasses配合我们的Transformer进行转换字节码。同样的我们有下面这个类:

```
public class TestTransformer {

    public void testTrans() {
        System.out.println("testTrans1");
    }
}
```
在testTrans中打印testTrans1,我们有下面一个main方法:

```
 public static void main(String[] args) throws Exception {
        TestTransformer testTransformer = new TestTransformer();
        int max = 20;
        int index = 0;
        while (++index<max){
            testTransformer.testTrans();
            Thread.sleep(100L);
        }
```
如果我们不做任何操作，那么肯定打印的是testTrans1，接下来我们使用retransformClasses：

```
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
```
这里只是将我们对应的类尝试去retransform，但是需要Transformer:

```
//必须设置true，才能进行多次retrans
        inst.addTransformer(new SampleTransformer(), true);
```
上面添加了一个Transformer,如果设置为false，这下次retransform一个类的时候他不会执行，而是直接返回他已经执行完之后的代码。如果设置为true，那么只要有retransform的调用就会执行。

```
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
}
```
这里的SampleTransFormer使用ASM去对代码进行替换，进行随机输出testTrans + random.nextInt(3)。可以看有下面的结果:

![](https://user-gold-cdn.xitu.io/2019/1/21/168702c77ed4c0b7?w=689&h=567&f=png&s=46543)


![](https://user-gold-cdn.xitu.io/2018/7/22/164c2ad786c7cfe4?w=500&h=375&f=jpeg&s=215163)
