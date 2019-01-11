import util.Config;
import util.GraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;

import java.io.IOException;

public class Entry {

    /*
    * 参数：-j jarFilePath -c classFilePath -m MainClassName
    * */
    public static void main(String[] args) throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {

        String scopeFile = "res\\myscope";
        String configFile = "res\\pointConfig.txt";

        String jarFile = null, classFile = null, mainClassName = "LTest/example/Test";
        for(int i =0; i < args.length; i++) {
            if(args[i].equalsIgnoreCase("-j")) {
                if(i < args.length - 1)
                    jarFile = args[i+1];
                i++;
            } else if (args[i].equalsIgnoreCase("-c")) {
                if(i < args.length - 1)
                    classFile = args[i+1];
                i++;
            } else if (args[i].equalsIgnoreCase("-m")) {
                if(i < args.length - 1)
                    mainClassName = args[i+1];
                i++;
            }
         }
         if(mainClassName == null) {
            System.out.println("mainClass Name can't be null");
            return;
         }
        Config.getInstance().readConfig(configFile, scopeFile, jarFile, classFile);
        GraphBuilder graphBuilder = new GraphBuilder(scopeFile, mainClassName);
        graphBuilder.analysis();
    }
}
