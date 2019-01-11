package util;

import com.ibm.wala.classLoader.IField;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class MethodAbstract {
    // mark the object pass status change
    public Vector<Boolean> arguments = new Vector<Boolean>(3);
    // mark the static filed status change
    public Map<IField, Boolean> sFiled = new HashMap<IField, Boolean>();
}
