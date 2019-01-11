package util;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;


import java.io.File;
import java.io.IOException;
import java.util.*;

public class GraphBuilder {

    private String NOT_INCLUDE = "D:\\simpleWala\\walalab\\res\\Exclusion.txt";

    // 标记基本块是否已经被访问
    private Map<Integer, Boolean> mark = new HashMap<Integer, Boolean>();

    // dfs's stack
    private Stack<CodeContext> stack = new Stack<CodeContext>();

    private Map<MethodReference, HashSet<Integer>> visit = new HashMap<MethodReference, HashSet<Integer>>();

    // 用于处理对象的域
    private ClassHierarchy cha = null;

    // 分析过程中缓存,其中包含ir
    private IAnalysisCacheView cache = null;

    // cg图
    private CallGraph cg = null;

    private boolean selfCheck = false;
    private boolean selfTainted = false;
    private boolean retDirty = false;

    /**
     * @param scopeFile 符合wala要求的scope文件，分析文件不能为源代码
     * @param mainClass the signature of the class. eg:Ljava/lang/Obejct
     * @throws ClassHierarchyException
     * @throws CallGraphBuilderCancelException
     * @throws IOException
     */
    public GraphBuilder(String scopeFile, String mainClass) throws ClassHierarchyException, CallGraphBuilderCancelException, IOException {
        buildCG_CFG(scopeFile, mainClass);
    }

    /***
     *
     * @param scopeFile  符合wala要求的scope文件，分析文件不能为源代码
     * @param mainClass  the signature of the class. eg:Ljava/lang/Obejct
     * @throws IOException
     * @throws CallGraphBuilderCancelException
     * @throws ClassHierarchyException
     */

    public void printfCG(String scopeFile, String mainClass) throws IOException, CallGraphBuilderCancelException, ClassHierarchyException {
        System.out.println("start");

        // 使用exclude文件加载scope文件
        File exFile = new FileProvider().getFile(NOT_INCLUDE);
        AnalysisScope scope = AnalysisScopeReader.readJavaScope(scopeFile, exFile,
                ClassLoader.getSystemClassLoader());

        // 加载类到wala中
        cha = ClassHierarchyFactory.make(scope);


        // 测试入口的主类是否存在
        TypeReference mainClassRef = TypeReference.findOrCreate(ClassLoaderReference.Application, mainClass);
        if (cha.lookupClass(mainClassRef) == null) {
            System.out.println("error: can't find the main class");
            return;
        }

        // 打印当前加载的类的个数
        System.out.println(cha.getNumberOfClasses() + " classes");

        // 设置程序分析入口
        Iterable<Entrypoint> e = Util.makeMainEntrypoints(scope, cha, mainClass);

        // 设置分析选项，不分析程序中的反射
        AnalysisOptions o = new AnalysisOptions(scope, e);
        o.setReflectionOptions(AnalysisOptions.ReflectionOptions.NONE);

        // 生成CG图,并将分析缓存放入cache中
        cache = new AnalysisCacheImpl();
        CallGraphBuilder builder = Util.makeZeroOneCFABuilder(Language.JAVA, o, cache, cha, scope);
        CallGraph cg = builder.makeCallGraph(o, null);
//      System.out.println(cg.toString());
        System.out.println(CallGraphStats.getStats(cg));
//        TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<CGNode, Integer>> result = reachingDefs.analyze();
//        ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph = reachingDefs.getSupergraph();
//        System.out.println(supergraph.toString());
//        TabulationDomain<Pair<CGNode, Integer>, BasicBlockInContext<IExplodedBasicBlock>> domain =  reachingDefs.getDomain();
//        System.out.println("domain size:" + domain.getSize());
//         输出由Application 加载器加载的类的cfg图
        for (IClass clazz : cha) {
            if (scope.isApplicationLoader(clazz.getClassLoader())) {
                for (IMethod method : clazz.getAllMethods()) {
                    IR ir = cache.getIR(method);
                    if (ir != null) {
                        System.out.println(ir.toString());
                        ir.getSymbolTable();
                    }
                }
            }
        }
    }

    /**
     * after calling this function, get cha, cg, cache for the java classed in the scopeFile
     *
     * @param scopeFile 符合wala要求的scope文件，分析文件不能为源代码
     * @param mainClass the signature of the class. eg:Ljava/lang/Obejct
     * @throws IOException
     * @throws CallGraphBuilderCancelException
     * @throws ClassHierarchyException
     */
    public void buildCG_CFG(String scopeFile, String mainClass) throws IOException, CallGraphBuilderCancelException, ClassHierarchyException {
        System.out.println("start");
        System.out.println(scopeFile);
        // 使用exclude文件加载scope文件
        File exFile = new FileProvider().getFile(NOT_INCLUDE);
        AnalysisScope scope = AnalysisScopeReader.readJavaScope(scopeFile, exFile,
                ClassLoader.getSystemClassLoader());

        // 加载类到wala中
        cha = ClassHierarchyFactory.make(scope);


        // 测试入口的主类是否存在
        TypeReference mainClassRef = TypeReference.findOrCreate(ClassLoaderReference.Application, mainClass);
        if (cha.lookupClass(mainClassRef) == null) {
            System.out.println("error: can't find the main class");
            return;
        }

        // 打印当前加载的类的个数
        System.out.println(cha.getNumberOfClasses() + " classes");

        // 设置程序分析入口
        Iterable<Entrypoint> e = Util.makeMainEntrypoints(scope, cha, mainClass);

        // 设置分析选项，不分析程序中的反射
        AnalysisOptions o = new AnalysisOptions(scope, e);
        o.setReflectionOptions(AnalysisOptions.ReflectionOptions.NONE);

        // 生成CG图,并将分析缓存放入cache中
        cache = new AnalysisCacheImpl();
        CallGraphBuilder builder = Util.makeZeroOneCFABuilder(Language.JAVA, o, cache, cha, scope);
        cg = builder.makeCallGraph(o, null);
    }

    public SSACFG getCFG(MethodReference methodReference) {
        if (cha == null || cache == null || cg == null)
            return null;
        IMethod method = cha.resolveMethod(methodReference);
        IR ir = cache.getIR(method);

        if (ir != null) {
            return ir.getControlFlowGraph();
        }
        return null;
    }

    /**
     * get IR according to the methodReference, should be called after build the CG
     *
     * @param methodReference
     * @return null or the IR
     */
    public IR getIR(MethodReference methodReference) {
        if (cha == null || cache == null || cg == null)
            return null;
        IMethod method = cha.resolveMethod(methodReference);
        return cache.getIR(method);
    }

    public SSACFG getFakeRootCFG() {
        if (cha == null || cache == null || cg == null)
            return null;
        return cg.getFakeRootNode().getIR().getControlFlowGraph();
    }

    public CGNode getEntryCGNode() {
        if (cha == null || cache == null || cg == null)
            return null;
        // get the entry of the java application, for the simple
        // test, the number of the entry is only one;
        for (CGNode i : cg.getEntrypointNodes()) {
            if (i != null) {
                return i;
            }
        }
        return null;
    }


    public FieldReference getFieldReference(SSAInstruction instruction) {
        if (instruction instanceof SSAPutInstruction) {
            return ((SSAPutInstruction) instruction).getDeclaredField();
        } else if (instruction instanceof SSAGetInstruction) {
            return ((SSAGetInstruction) instruction).getDeclaredField();
        } else if (instruction instanceof SSAFieldAccessInstruction) {
            return ((SSAFieldAccessInstruction) instruction).getDeclaredField();
        }

        else {
            LogUtil.i("getFieldReference", "resolve field reference error");
            return null;
        }
    }

    /**
     * dfs the cfg, during the travel, we can do the calculation
     *
     * @param now    method, CG node we are in now
     * @param ssacfg the ssacfg we are traveling
     * @param v      basicblock we should visit
     */
    private void dfs(MethodReference now, SSACFG ssacfg, ISSABasicBlock v) {
        String tag = "method dfs";


        LogUtil.i(tag, "method: " + now.getSignature() + " block " + ssacfg.getNumber(v) + ": ");
        CodeContext context = null;

        for (SSAInstruction instruction : ((SSACFG.BasicBlock) v).getAllInstructions()) {
            context = stack.peek(); // get CodeContext

            if (instruction instanceof SSAPutInstruction) {
                SSAPutInstruction putInstr = (SSAPutInstruction) instruction;
                IField field = cha.resolveField(putInstr.getDeclaredField());

                System.out.println("[" + instruction.iindex + "]: " + instruction.toString());

                int vOrder = putInstr.getVal();

                LogUtil.i(tag, putInstr.getNumberOfUses() + " " + putInstr.getUse(0) + " " +
                        putInstr.getUse(1) + " vorder is " + vOrder);

                // LogUtil.i(tag, "vfrom is " + vfrom);
                if (field != null) {
                    // 添加污染变量
                    LogUtil.i("tttttttttttttt", field.toString());

                    if(context.isTainted(vOrder, now.getSignature())) {
                        context.addTainted(field, vOrder, now.getSignature());
                    }
                }
            } else if (instruction instanceof SSAGetInstruction) {
            // e.g. 14 = getstatic < Application, Ljava/lang/System, out, <Application,Ljava/io/PrintStream> >
                System.out.println("[" + instruction.iindex + "]: " + instruction.toString());

                SSAGetInstruction getInstr = (SSAGetInstruction) instruction;
                IField field = cha.resolveField(getInstr.getDeclaredField());

                int vOrder = getInstr.getDef();
                LogUtil.i(tag, getInstr.getNumberOfUses() + "vorder is " + vOrder);

                if (field != null) {
                    if(getInstr.getNumberOfUses() > 0) {
                        if (context.isTainted(field, getInstr.getUse(0), now.getSignature())) {
                            context.addTainted(field, vOrder, now.getSignature());
                        }
                    }
                }
            } else if (instruction instanceof SSABinaryOpInstruction) {
                // 只要操作数中有被污染的直接认为其被污染。
                LogUtil.i(tag, "next instruction is an BinaryOpInstruction");

                final SSABinaryOpInstruction opInstruction = (SSABinaryOpInstruction) instruction;
                int defs = opInstruction.getNumberOfDefs();
                int uses = opInstruction.getNumberOfUses();
                boolean flag = false;


                for (int i = 0; i < uses; i++) {
                    int index = opInstruction.getUse(i);
                    if(context.isTainted(index, now.getSignature())) {
                        flag = true;
                        break;
                    }
                }

                if (flag) {
                    for (int i = 0; i < defs; i++) {
                        int index = opInstruction.getDef(i);
                        context.addTainted(null, index, now.getSignature());
                        System.out.println(opInstruction.getDef(i));
                    }
                }
            } else if (instruction instanceof SSAInvokeInstruction) {
            // 该条指令为invoke指令
            // e.g. [3]: 6 = invokestatic < Application, Ljava/lang/System, getProperty(Ljava/lang/String;)Ljava/lang/String; > 4 @3 exception:5
            // 仅进入类加载器为Application的方法
            
                // print instructions in the bb
                System.out.println("[" + instruction.iindex + "]: " + instruction.toString() + "\n");

                SSAInvokeInstruction callInstruction = (SSAInvokeInstruction) instruction;
                int retV = callInstruction.getNumberOfReturnValues();


//                LogUtil.i(tag, "getUse is " + callInstruction.getUse(0)
//                        + "getDef is " + callInstruction.getDef());


                // getUse() 获得函数调用中使用的参数
                // callInstruction.getReturnValue(1));   1 是第几个返回值

                IMethod nextMethod = cha.resolveMethod(callInstruction.getDeclaredTarget());


                if (nextMethod != null && cache.getIR(nextMethod) != null) {
                    LogUtil.i(tag, "the method is " + nextMethod.getSignature());
                    if (Config.getInstance().isSource(nextMethod.getSignature())) {
                        // 参数
                        for (int i = 0; i < callInstruction.getNumberOfUses(); i++) {
                            int index = callInstruction.getUse(i);
                            context.addTainted(null, index, now.getSignature());
                        }
                        for(int j = 0; j < callInstruction.getNumberOfReturnValues(); j++) {
                            context.addTainted(null, callInstruction.getReturnValue(j), now.getSignature());
                        }
                        continue;
                    }
                    if (Config.getInstance().isSink(nextMethod.getSignature())) {
                        // 参数
                        for (int i = 0; i < callInstruction.getNumberOfUses(); i++) {
                            int index = callInstruction.getUse(i);
                            if(context.isTainted(index, now.getSignature())) {
                                System.out.println("fing a path from source to sink, the path is ");
                                printPath();
                            }
                        }
                        continue;
                    }
                    if (Config.getInstance().getRule(nextMethod.getSignature()) != null) {
                        Vector<Integer> from = Config.getInstance().getRule(nextMethod.getSignature()).from;
                        Vector<Integer> des = Config.getInstance().getRule(nextMethod.getSignature()).des;

                        boolean pFlag = false;      // 参数中是否有污染变量
                        for(Integer i : from) {
                            if(i < callInstruction.getNumberOfPositionalParameters()) {
                                if(context.isTainted(null, callInstruction.getUse(i), now.getSignature())) {
                                    pFlag = true;
                                    break;
                                }
                            }
                        }

                        if(pFlag) {
                            for(Integer i : des) {
                                if(i == -1) {
                                    context.addTainted(null, callInstruction.getReturnValue(0), now.getSignature());
                                }
                                else
                                    context.addTainted(null,callInstruction.getUse(i), now.getSignature());
                            }
                        }
                        continue;
                    }


                    String clazzLooader = nextMethod.getDeclaringClass().getClassLoader().toString();
//                  LogUtil.i("class Loader", nextMethod.getDeclaringClass().getClassLoader().toString());

                    if (!clazzLooader.equals("Application")) {
                          continue;
                    }

                        // step into  the function
                    MethodReference reference = nextMethod.getReference();
                    SSACFG nextCFG = getCFG(reference);
//
//                       // add new CodeContext into the stack
//                        CodeContext nc = stack.peek().deepCopy();
//                        DefUse du = cache.getDefUse(getIR(reference));

//                    nc.iStatus.clear();
//                    stack.push(nc);
//                    LogUtil.i("stack", stack.size());
//                    // step into the method

                    // 进入下一个函数内部，传递污染变量
                    for(int i =0; i < callInstruction.getNumberOfPositionalParameters(); i++) {
                        if(context.isTainted(callInstruction.getUse(i), now.getSignature())) {
                            context.addTainted(null, i + 1, nextMethod.getSignature());
                        }
                    }
                    if(!callInstruction.isStatic()) {
                        selfCheck   = true;
                    }

                    dfs(reference, nextCFG, nextCFG.entry());

                    if(retDirty) {
                        context.addTainted(null, callInstruction.getReturnValue(0), now.getSignature());
                        retDirty =false;
                    }

                    if(selfTainted) {
                        context.addTainted(null, callInstruction.getUse(0), now.getSignature());
                        selfCheck = false;
                    }
//                        get method abstract from the stack top
//                        CodeContext preContext = stack.pop();

                }
            }  else if (instruction instanceof SSAReturnInstruction) {
                SSAReturnInstruction retInstruction = (SSAReturnInstruction) instruction;
                LogUtil.i("ret", retInstruction.toString() + ";;" + retInstruction.getResult());
                int ret = retInstruction.getResult();
                if(ret != -1) {
                    if(context.isTainted(ret, now.getSignature()))
                        retDirty = true;
                }
                if(context.isTainted(1, now.getSignature()))
                    selfTainted = true;
            } else {
                // print instructions in the bb
                System.out.println("[" + instruction.iindex + "]: " + instruction.toString());
            }
        }

        System.out.println(ssacfg.getSuccNodeCount(v));
        if(ssacfg.getSuccNodeCount(v) <= 2) {
            Iterator<ISSABasicBlock> nextBlock = ssacfg.getSuccNodes(v);
            while (nextBlock.hasNext()) {
                ISSABasicBlock b = nextBlock.next();
                if(b.equals(ssacfg.exit()))
                    continue;
                dfs(now, ssacfg, b);
            }
            return;
        }

            // visit other bb more than one branch
        Iterator<ISSABasicBlock> nextBlock = ssacfg.getSuccNodes(v);
        while (nextBlock.hasNext()) {
            ISSABasicBlock b = nextBlock.next();
            if(b.equals(ssacfg.exit()))
                continue;
            stack.push(stack.peek().deepCopy());
            dfs(now, ssacfg, b);
            stack.pop();
        }
        return;
    }

    public void printPath() {

    }

    public void analysis () throws ClassHierarchyException, CallGraphBuilderCancelException, IOException {
        CGNode entryNode = getEntryCGNode();
        if (entryNode != null) {
            System.out.println(entryNode.getIR().toString());
            SSACFG cfg = entryNode.getIR().getControlFlowGraph();
            MethodReference reference = entryNode.getMethod().getReference();
            ISSABasicBlock basicBlock = cfg.entry();

            CodeContext context = new CodeContext();
            DefUse du = cache.getDefUse(entryNode.getIR());
            stack.push(context);
            LogUtil.i("stack", stack.size());
            dfs(reference, cfg, basicBlock);
        }
    }
}
