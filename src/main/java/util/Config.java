package util;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public class Config {
    private  HashSet<String> sinkSet = new HashSet<String>();
    private  HashSet<String> sourceSet = new HashSet<String>();
    private  HashMap<String, TransferRule> transferRuleHashMap = new HashMap<String, TransferRule>();

    private static Config config;
    /**
     * read config about the source and sink function
     * @param filePath the file path of the config file
     * @return true: address the configFile successfully, otherwise false;
     */
    public void readConfig(String filePath) {
        File file = new File(filePath);
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String temp = in.readLine();
            while(temp != null) {
                String[] res = temp.split(" ", 3);
                if(res.length < 2) {
                    System.out.println("wrong config file format");
                } else {
                    if("SINK".equals(res[0]))
                        sinkSet.add(res[1]);
                    else if("SOURCE".equals(res[0]))
                        sourceSet.add(res[1]);
                    else if("PROCESS".equals(res[0])) {
                        String signature = res[1];
                        String[] rule = res[2].replace("[", "")
                                .replace("]", "")
                                .split("->");
                        String[] from = rule[0].split(",");
                        String[] des  = rule[1].split(",");
                        Vector<Integer> from_int = new Vector<Integer>(), des_int = new Vector<Integer>();
                        for(int i = 0; i < from.length; i++)
                            from_int.add(Integer.parseInt(from[i]));
                        for(int j = 0; j < des.length; j++)
                            des_int.add(Integer.parseInt(des[j]));

                        TransferRule transferRule = new TransferRule(from_int, des_int);
                        transferRuleHashMap.put(signature, transferRule);
                    }
                    System.out.println(temp);
                }
                temp = in.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readConfig(String configFile,String scopeFile,String jarFile,String classFile) {
        BufferedWriter out = null;
        if(jarFile != null) {
            File file = new File(scopeFile);
            try {
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
                out.newLine();
                out.write("Application,Java,jarFile," + jarFile);
                out.flush();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(out != null)
                        out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }
        if(classFile != null) {
            File file = new File(scopeFile);
            try {
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
                out.newLine();
                out.write("Application,Java,classFile," + classFile);
                out.flush();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(out != null)
                        out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        readConfig(configFile);

    }
    /**
     * return true if the methodSiganture is a sink point that
     * has been defined.
     * @param methodSignature method signature in format which wala uses
     * @return true if the method is a sink point, otherwise return false;
     */

    public boolean isSink(String methodSignature) {
        return sinkSet.contains(methodSignature);
    }

    /**
     * return true if the method Signature is a Source point that
     * has been defined.
     * @param methodSignature method signature in format which wala uses
     * @return true if the method is a source point, otherwise return false;
     */
    public boolean isSource(String methodSignature) {
        return sourceSet.contains(methodSignature);
    }


    /**
     * 添加传播规则
     * @param signature     函数的签名
     * @param transferRule  转移规则：from -> des, && 全局变量的转换
     */
    public void addRul(String signature, TransferRule transferRule) {
        if(!transferRuleHashMap.containsKey(signature)) {
            transferRuleHashMap.put(signature, transferRule);
        }
    }

    public TransferRule getRule(String signature) {
        if(transferRuleHashMap.containsKey(signature)) {
            return transferRuleHashMap.get(signature);
        }
        return null;
    }

    class TransferRule {
        public Vector<Integer> from, des;
        public CodeContext.DirtyData[] sfileds;

        public TransferRule(Vector<Integer> from, Vector<Integer> des) {
            this.from = from;
            this.des  = des;
        }

        public TransferRule(Vector<Integer> from, Vector<Integer> des, CodeContext.DirtyData[] sfileds) {
            this.from = from;
            this.des  = des;
            this.sfileds = sfileds;
        }
    }

    private Config() {
    }


    public static Config getInstance() {
        if( config == null) {
            config = new Config();
            return config;
        } else {
            return config;
        }
    }


    public static void main(String[] args) {
        String path = "res/pointConfig.txt";
        Config config = Config.getInstance();
        config.readConfig(path);
        System.out.println("***sink point***");
        for(String i : config.sinkSet) {
            System.out.println(i);
        }
        System.out.println("***source point***");
        for(String i : config.sourceSet) {
            System.out.println(i);
        }
    }
}
