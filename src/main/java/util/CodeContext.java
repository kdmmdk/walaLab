package util;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.rits.cloning.Cloner;

import java.util.HashSet;

public class CodeContext {
    private static final String TAG = "CodeContext";
    public HashSet<DirtyData> status = new HashSet<DirtyData>();

    // return a deepcopy and replace it with a

    public CodeContext deepCopy() {
        Cloner cloner = new Cloner();
        CodeContext copy = cloner.deepClone(this);
        return copy;
    }

    class  DirtyData {
        IField  field = null;
        int     register;
        String method;
        public DirtyData(IField field, int register, String method) {
            this.field = field;
            this.register = register;
            this.method = method;
        }
    }

    public boolean isTainted(IField iField, int register, String method) {
        for(DirtyData data : status) {
            if((iField != null && iField.equals(data.field)) ||
                    (data.register == register && method.equals(data.method)))
                return true;
        }
        return false;
    }

    public boolean isTainted(int register, String method) {
        for(DirtyData data : status) {
            if(register == data.register && data.method.equals(method))
                return true;
        }
        return false;
    }

    public boolean isTainted(IField iField) {
        for(DirtyData data : status) {
            if(iField.equals(data.field))
                return true;
        }
        return false;
    }

    public void addTainted(IField field, int register, String method) {
        DirtyData dirtyData = new DirtyData(field, register, method);
        if(!status.contains(dirtyData)) {
            status.add(dirtyData);
        } else {
            LogUtil.i(TAG, "this has been added.");
        }
    }
}


